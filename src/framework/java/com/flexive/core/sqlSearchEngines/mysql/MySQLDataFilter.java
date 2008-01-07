/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.core.sqlSearchEngines.mysql;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.sqlSearchEngines.DataFilter;
import com.flexive.core.sqlSearchEngines.FxFoundTypeImpl;
import com.flexive.core.sqlSearchEngines.FxSearch;
import com.flexive.core.sqlSearchEngines.PropertyResolver;
import com.flexive.core.storage.FxTreeNodeInfo;
import com.flexive.core.storage.StorageManager;
import com.flexive.core.storage.genericSQL.GenericTreeStorage;
import com.flexive.ejb.beans.TreeEngineBean;
import com.flexive.shared.FxArrayUtils;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.search.FxFoundType;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.value.FxValueConverter;
import com.flexive.sqlParser.*;
import static com.flexive.sqlParser.Condition.Comparator;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;

// NVL --> IFNULL((select sub.id from FX_CONTENT_DATA sub where sub.id=filter.id),1)

/**
 * Builds a select statement selecting all id's and versions that the
 * search should return as resultset.
 * <p/>
 * Example result statement:<br>
 * SELECT tbl6.id,tbl6.ver from <br>
 * (SELECT DISTINCT ID,VER,LANG FROM FX_CONTENT_DATA CO WHERE CO.id=1 and ISMAX_VER=true)  tbl6<br>
 * ,(SELECT * from (<br>
 * (SELECT DISTINCT ID,VER,LANG FROM FX_CONTENT_DATA CO WHERE CO.id=1 and ISMAX_VER=true)<br>
 * UNION<br>
 * (SELECT DISTINCT ID,VER,LANG FROM FX_CONTENT_DATA CO WHERE CO.id=2 and ISMAX_VER=true) ) unInner7) tbl7<br>
 * WHERE  tbl6.id=tbl7.id and tbl6.ver=tbl7.ver and tbl6.lang=tbl7.lang
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class MySQLDataFilter extends DataFilter {
    private static final transient Log LOG = LogFactory.getLog(MySQLDataFilter.class);

    final static String CSQ_NO_MATCH = "(SELECT DISTINCT null id,null ver,null lang FROM dual where 1=2)";

    // The maximum rows returned by a subquery (property or assignement search)
    // May be -1 to use all found rows (not recommended, since MySQL will be VERY slow in this case).
    private final static int SUBQUERY_LIMIT = 10000;

    private String TBL_MAIN;
    private String TBL_DATA;
    private String TBL_FT;
    private boolean USE_VIEWS;
    private int foundEntryCount;
    private boolean truncated;
    private ArrayList<FxFoundType> contentTypes = new ArrayList<FxFoundType>(10);
    private Connection con;

    public MySQLDataFilter(Connection con, FxSearch search) throws FxSqlSearchException {
        super(con, search);
        this.con = con;
        long briefcaseIds[] = getStatement().getBriefcaseFilter();
        if (briefcaseIds.length == 0) {
            TBL_MAIN = DatabaseConst.TBL_CONTENT;
            TBL_DATA = DatabaseConst.TBL_CONTENT_DATA;
            TBL_FT = DatabaseConst.TBL_CONTENT_DATA_FT;
            USE_VIEWS = false;
        } else {
            Statement stmt = null;
            USE_VIEWS = true;
            try {
                TBL_MAIN = "FILTER_MVIEW_" + System.currentTimeMillis();
                TBL_DATA = "FILTER_DVIEW_" + System.currentTimeMillis();
                TBL_FT = "FILTER_FVIEW_" + System.currentTimeMillis();
                FxStatement fxstmt = getStatement();

                stmt = con.createStatement();
                // TODO: Add briefcase security (may the user access the specified briefcases?)
                // TODO: Filter out undesired / desired languages

                String sIdFilter = "select id from " + DatabaseConst.TBL_BRIEFCASE_DATA +
                        " where briefcase_id " +
                        (getStatement().getBriefcaseFilter().length == 1 ? ("=" + briefcaseIds[0]) : "in (" +
                                FxArrayUtils.toSeparatedList(briefcaseIds, ',') + ")");
                stmt.addBatch("CREATE VIEW " + TBL_DATA + " AS select * from " + DatabaseConst.TBL_CONTENT_DATA +
                        " where id in (" + sIdFilter + ") " + _getVersionFilter(fxstmt, null));
                stmt.addBatch("CREATE VIEW " + TBL_MAIN + " AS select * from " + DatabaseConst.TBL_CONTENT +
                        " where id in (" + sIdFilter + ") " + _getVersionFilter(fxstmt, null));
                stmt.addBatch("CREATE VIEW " + TBL_FT + " AS select * from " + DatabaseConst.TBL_CONTENT_DATA_FT +
                        " where id in (" + sIdFilter + ")");    // TODO: version filter!!
                stmt.executeBatch();
            } catch (Throwable t) {
                throw new FxSqlSearchException(t, "ex.sqlSearch.failedToBuildFilterViews", t.getMessage());
            } finally {
                Database.closeObjects(MySQLDataFilter.class, null, stmt);
            }
        }
    }

    /**
     * Cleanup all temporary data used for the search.
     *
     * @throws FxSqlSearchException if the cleanup failed
     */
    @Override
    public void cleanup() throws FxSqlSearchException {
        if (USE_VIEWS) {
            Statement stmt = null;
            try {
                stmt = getConnection().createStatement();
                stmt.addBatch("DROP VIEW " + TBL_MAIN);
                stmt.addBatch("DROP VIEW " + TBL_DATA);
                stmt.addBatch("DROP VIEW " + TBL_FT);
                stmt.executeBatch();
            } catch (Throwable t) {
                throw new FxSqlSearchException(t, "ex.sqlSearch.failedToDropFilterViews");
            } finally {
                Database.closeObjects(MySQLDataFilter.class, null, stmt);
            }
        }
        if (search.getCacheTable().equals(DatabaseConst.TBL_SEARCHCACHE_MEMORY)) {
            Statement stmt = null;
            try {
                stmt = getConnection().createStatement();
                stmt.executeUpdate("DELETE FROM " + DatabaseConst.TBL_SEARCHCACHE_MEMORY +
                        " WHERE SEARCH_ID=" + search.getSearchId());
            } catch (Throwable t) {
                throw new FxSqlSearchException(t, "ex.sqlSearch.err.failedToClearTempSearchResult", search.getSearchId());
            } finally {
                Database.closeObjects(MySQLDataSelector.class, null, stmt);
            }
        }
    }

    /**
     * Builds the filter.
     *
     * @throws FxSqlSearchException if the build failed
     */
    @Override
    public void build() throws FxSqlSearchException {
        Statement stmt = null;
        final long MAX_ROWS = search.getFxStatement().getMaxResultRows() + 1;
        try {
            String dataSelect;
            if (getStatement().getType() == FxStatement.TYPE.ALL) {
                // The statement will not filter the data
                dataSelect = "select " + search.getSearchId() + " search_id,id,ver,tdef from " + TBL_MAIN + " LIMIT " +
                        search.getFxStatement().getMaxResultRows();
            } else {
                // The statement filters the data
                StringBuilder result = new StringBuilder(5000);
                build(result, getStatement().getRootBrace());
                // Remove leading and ending brace
                result.deleteCharAt(0);
                result.deleteCharAt(result.length() - 1);
                // Finalize the select
                final UserTicket ticket = FxContext.get().getTicket();
                dataSelect = "select * from (select distinct " + search.getSearchId() +
                        " search_id,data.id,data.ver,main.tdef \n" +
                        "from (" + result.toString() + ") data, " + DatabaseConst.TBL_CONTENT + " main\n" +
                        "where data.ver=main.ver and data.id=main.id) data2\n" +
                        // Security
                        (ticket.isGlobalSupervisor() ? "" :
                                "where mayReadInstance2(data2.id,data2.ver," + ticket.getUserId() + "," +
                                        ticket.getMandatorId() + "," + ticket.isMandatorSupervisor() + "," + ticket.isGlobalSupervisor() + ")\n") +
                        // Limit by the specified max items
                        "LIMIT " + MAX_ROWS;
            }
            // Find all matching data enties and store them
            if (LOG.isDebugEnabled()) {
                LOG.debug("SQL getResult: \n" + dataSelect + "\n");
            }
            String sSql = "insert into " + search.getCacheTable() + " (" + dataSelect + ")";
            stmt = getConnection().createStatement();
            stmt.setQueryTimeout(search.getParams().getQueryTimeout());
            stmt.executeUpdate(sSql);
            analyzeResult();
        } catch (FxSqlSearchException exc) {
            throw exc;
        } catch (Throwable t) {
            throw new FxSqlSearchException(t, "ex.sqlSearch.failedToBuildDataFilter", t.getMessage(), search.getQuery());
        } finally {
            Database.closeObjects(MySQLDataFilter.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public int getFoundEntries() {
        return foundEntryCount;
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * Analyzes the result and stores the info in object variables.
     *
     * @throws FxSqlSearchException if the function fails
     */
    private void analyzeResult() throws FxSqlSearchException {
        Statement stmt = null;
        try {
            stmt = getConnection().createStatement();
            ResultSet rs = stmt.executeQuery("select count(*),tdef from " + search.getCacheTable() +
                    " where search_id=" + search.getSearchId() + " group by tdef");
            foundEntryCount = 0;
            while (rs != null && rs.next()) {
                FxFoundTypeImpl type = new FxFoundTypeImpl(rs.getLong(2), rs.getInt(1));
                foundEntryCount += type.getFoundEntries();
                contentTypes.add(type);
            }
            contentTypes.trimToSize();

            if (foundEntryCount > search.getFxStatement().getMaxResultRows()) {
                foundEntryCount = search.getFxStatement().getMaxResultRows();
                truncated = true;
            } else {
                truncated = false;
            }
            /*rs = stmt.executeQuery("SELECT COUNT(*) FROM "+search.getCacheTable()+" WHERE SEARCH_ID="+search.getSearchId());
            long totalFiltered = 0;
            if( rs != null && rs.next() )
                totalFiltered = rs.getLong(1);
            System.out.println("Total filtered: "+totalFiltered);*/
        } catch (Throwable t) {
            throw new FxSqlSearchException(t, "ex.sqlSearch.failedToCountFoundEntries", search.getSearchId());
        } finally {
            Database.closeObjects(MySQLDataFilter.class, null, stmt);
        }
    }

    /**
     * Returns a list of all content types that are part of the resultset.
     *
     * @return a list of all content types that are part of the resultset
     */
    @Override
    public ArrayList<FxFoundType> getContentTypes() {
        return this.contentTypes;
    }

    private void build(StringBuilder sb, Brace br) throws FxSqlSearchException {
        if (br.isAnd()) {
            buildAnd(sb, br);
        } else {
            buildOr(sb, br);
        }
    }

    /**
     * Builds an 'OR' condition.
     *
     * @param sb the string buffer to use
     * @param br the brace
     * @throws FxSqlSearchException if the build failed
     */
    private void buildOr(StringBuilder sb, Brace br) throws FxSqlSearchException {
        // Start OR
        sb.append("(SELECT * from (\n");
        int pos = 0;
        for (BraceElement be : br.getElements()) {
            if (pos > 0) {
                sb.append("\nUNION\n");
            }
            if (be instanceof Condition) {
                sb.append(getConditionSubQuery(br.getStatement(), (Condition) be));
            } else if (be instanceof Brace) {
                build(sb, (Brace) be);
            } else {
                System.err.println("Invalid BraceElement: " + be);
            }
            pos++;
        }
        sb.append(")");
        // Add virtual table name (need by mysql)
        sb.append((" unInner" + br.getId()));
        // Close OR
        sb.append(")");
    }

    /**
     * Builds an 'AND' condition.
     *
     * @param sb the StringBuilder to use
     * @param br the brace
     * @throws FxSqlSearchException if the build failed
     */
    private void buildAnd(StringBuilder sb, Brace br) throws FxSqlSearchException {
        // Start AND
        int pos = 0;
        String sCombine = "";
        int firstId = -1;
        for (BraceElement be : br.getElements()) {
            if (pos == 0) {
                firstId = be.getId();
                sb.append(("(SELECT tbl" + firstId + ".id,tbl" + firstId + ".ver,tbl" + firstId + ".lang from\n"));
            } else {
                sb.append(",");
                sCombine += ((pos > 1) ? " and" : "") + " " +
                        "tbl" + firstId + ".id=tbl" + be.getId() + ".id and " +
                        "tbl" + firstId + ".ver=tbl" + be.getId() + ".ver and " +
                        "(tbl" + firstId + ".lang=0 or tbl" + firstId + ".lang is null or " +
                        "tbl" + be.getId() + ".lang=0 or tbl" + be.getId() + ".lang is null or " +
                        "tbl" + firstId + ".lang=tbl" + be.getId() + ".lang)";
            }

            if (be instanceof Condition) {
                sb.append(getConditionSubQuery(br.getStatement(), (Condition) be));
            } else if (be instanceof Brace) {
                build(sb, (Brace) be);
            } else {
                System.err.println("Invalid BraceElement: " + be);
            }
            sb.append((" tbl" + be.getId() + "\n"));
            pos++;
        }
        // Where links the tables together
        sb.append(" WHERE ");
        sb.append(sCombine);
        // Close AND
        sb.append(")");
    }

    /**
     * Helper function for getConditionSubQuery(__).
     *
     * @param cond the condition
     * @param mode tree mode
     * @return the filter sub-statement
     */
    public String _getTreeFilter(Condition cond, FxTreeMode mode) {
        boolean direct = cond.getComperator() == Comparator.IS_DIRECT_CHILD_OF;
//        String table = DatabaseConst.TBL_TREE+(mode == FxTreeMode.Live ?"L":"");

        long parentNode;
        try {
            parentNode = Long.valueOf(cond.getConstant().getValue());
        } catch (Exception e) {
            try {
                // Cut away leading and ending "'" character
                String path = cond.getConstant().getValue().substring(1, cond.getConstant().getValue().length() - 1);
                // Lookup the path in the tree
                parentNode = new TreeEngineBean().getIdByPath(mode, path);
            } catch (Exception t) {
                throw new FxLoadException("Invalid tree path: " + cond.getConstant().getValue() + ", exc:" + t.getMessage()).asRuntimeException();
            }
        }

        if (parentNode == -1) {
            return CSQ_NO_MATCH;
        }


        FxTreeNodeInfo nodeInfo;
        try {
            nodeInfo = StorageManager.getTreeStorage().getTreeNodeInfo(con, mode, parentNode);
        } catch (FxApplicationException nf) {
            // Node does not exist -> empty resultset
            return CSQ_NO_MATCH;
        }
        return "(SELECT DISTINCT cd.ID,cd.VER,NULL LANG FROM " + TBL_MAIN + " cd WHERE " +
                "cd.ID IN(SELECT REF FROM " + GenericTreeStorage.getTable(mode) + " WHERE " +
                "LFT>" + nodeInfo.getLeft() + " AND RGT<" + nodeInfo.getRight() + " AND REF IS NOT NULL " +
                (direct ? " AND DEPTH=" + (nodeInfo.getDepth() + 1) : "") +
                "))";

    }

    /**
     * Builds a condition sub-query.
     *
     * @param stmt the statement
     * @param cond the condition to build
     * @return the sub query
     * @throws com.flexive.shared.exceptions.FxSqlSearchException
     *          if the function failed
     */
    private String getConditionSubQuery(FxStatement stmt, Condition cond) throws FxSqlSearchException {
        Table tblContent = stmt.getTableByType(Table.TYPE.CONTENT);
        if (tblContent == null) {
            throw new FxSqlSearchException("ex.sqlSearch.contentTableMissing");
        }
        Property prop = cond.getProperty();
        Constant constant = cond.getConstant();


        if (cond.getComperator() == Comparator.IS_CHILD_OF ||
                cond.getComperator() == Comparator.IS_DIRECT_CHILD_OF) {
            // In case of VERSION.ALL we will look up the LIVE and EDIT tree
            String result = "";
            int count = 0;
            if (stmt.getVersionFilter() == Filter.VERSION.LIVE ||
                    stmt.getVersionFilter() == Filter.VERSION.ALL) {
                result += _getTreeFilter(cond, FxTreeMode.Live);
                count++;
            }
            if (stmt.getVersionFilter() == Filter.VERSION.MAX ||
                    stmt.getVersionFilter() == Filter.VERSION.ALL) {
                if (result.length() > 0) {
                    result += "\n UNION \n";
                }
                result += _getTreeFilter(cond, FxTreeMode.Edit);
                count++;
            }
            return (count < 2) ? result : "(" + result + ")";
        } else if (prop.getPropertyName().equals("*")) {
            // Fulltext search
            return "(select distinct ft.ID,ft.VER,ft.LANG " +
                    "from " + TBL_FT + " ft," + DatabaseConst.TBL_CONTENT + " cd where\n" +
                    "cd.ver=ft.ver and cd.id=ft.id and\n" +
                    "MATCH (value) against (" + constant.getValue() + ")\n" +
                    _getLanguageFilter() +
                    _getVersionFilter(stmt, "cd") +
                    _getSubQueryLimit() +
                    ")";
            /*} else if (prop.getPropertyName().charAt(0)=='#') {
                long assignmentId = Long.valueOf(prop.getPropertyName().substring(1));
                FxAssignment fx_ass;
                try {
                    fx_ass = CacheAdmin.getEnvironment().getAssignment(assignmentId);
                } catch(Throwable t) {
                    boolean notFound = ((FxRuntimeException)t).getConverted() instanceof FxNotFoundException;
                    if (notFound) {
                        throw new FxSqlSearchException(t,"ex.sqlSearch.query.unknownAssignment",assignmentId);
                    } else {
                        throw new FxSqlSearchException(t,"ex.sqlSearch.query.failedToResolveAssignment",
                                assignmentId,t.getMessage());
                    }
                }
                if (!(fx_ass instanceof FxPropertyAssignment)) {
                    // Return no match for group assignments
                    return CSQ_NO_MATCH;
                }
                FxProperty fx_prop = ((FxPropertyAssignment)fx_ass).getProperty();
                Property _prop = new Property(prop.getTableAlias(),fx_prop.getName(),prop.getField());
                return _buildCondSubProp(stmt,cond,_prop,(FxPropertyAssignment)fx_ass);
            */
        } else {
            return _buildCondSubProp(stmt, cond, cond.getProperty());
        }
    }

    /**
     * Helper function.
     *
     * @param stmt the statement
     * @param cond the condition
     * @param prop the property
     * @return the subquery
     * @throws FxSqlSearchException if a error occured
     */
    private String _buildCondSubProp(FxStatement stmt, Condition cond, Property prop)
            throws FxSqlSearchException {

        Constant constant = cond.getConstant();
        PropertyResolver.Entry entry = getPropertyResolver().get(stmt, prop);
        final String filter = prop.isAssignment()
                ? "ASSIGN=" + entry.getAssignment().getId()
                : "TPROP=" + entry.getProperty().getId();

        if (entry.getTable().equals(DatabaseConst.TBL_CONTENT_DATA) &&
                cond.getComperator().equals(Condition.Comparator.IS) &&
                cond.getConstant().isNull()) {
            // IS NULL is a specical case for the content data table:
            // a property is null if no entry is present in the table, which means we must
            // find the entries by using a join on the main table

            //mp: wrong properties selected for a join/union and braces missing for limit statement
            //    was:        return "select distinct da.assign,da.tprop,ct.id,ct.ver from "+TBL_MAIN+" ct\n" +
            return "(SELECT DISTINCT da.ID,da.VER,da.LANG FROM " + TBL_MAIN + " ct\n" +
                    "LEFT JOIN " + TBL_DATA + " da ON (ct.ID=da.ID AND ct.VER=da.VER AND da." + filter + ")\n" +
                    "WHERE da.TPROP IS NULL " +
                    _getVersionFilter(stmt, "ct") +
                    // Assignment search: we are only interrested in the type that the assignment belongs to
                    (prop.isAssignment()
                            ? "AND ct.TDEF=" + entry.getAssignment().getAssignedType().getId() + " "
                            : "") +
                    _getSubQueryLimit() + ")";
        }

        String column = prop.getFunctionsStart() +
                entry.getFilterColumn() +
                prop.getFunctionsEnd();
        String value;
        switch (entry.getProperty().getDataType()) {
            case String1024:
            case Text:
            case HTML:
                value = constant.getValue();
                if (value == null) {
                    value = "NULL";
                } else {
                    // First remove surrounding "'" characters
                    value = FxFormatUtils.unquote(value);
                    // Escape all remaining "'" characters
                    value = value.replaceAll("'", "\\\\'");
                    // Convert back to an SQL string
                    value = "'" + value + "'";
                }
                break;
            case LargeNumber:
                value = "" + FxValueConverter.toLong(constant.getValue());
                break;
            case Number:
                value = "" + FxValueConverter.toInteger(constant.getValue());
                break;
            case Double:
                value = "" + FxValueConverter.toDouble(constant.getValue());
                break;
            case Float:
                value = "" + FxValueConverter.toFloat(constant.getValue());
                break;
            case SelectOne:
                if (StringUtils.isNumeric(constant.getValue())) {
                    //list item id, nothing to lookup
                    value = constant.getValue();
                } else {
                    //list item data (of first matching item)
                    value = "" + entry.getProperty().getReferencedList().
                            getItemByData(FxFormatUtils.unquote(constant.getValue())).getId();
                }
                break;
            case SelectMany:
                if (Comparator.EQUAL.equals(cond.getComperator()) || Comparator.NOT_EQUAL.equals(cond.getComperator())) {
                    // exact match, so we use the text column that stores the comma-separated list of selected items
                    column = "FTEXT1024";
                    value = "'" + StringUtils.join(constant.iterator(), ',') + "'";
                } else {
                    throw new FxSqlSearchException("ex.sqlSearch.reader.type.invalidOperator",
                            entry.getProperty().getDataType(), cond.getComperator()); 
                }
                break;
            case Boolean:
                value = FxValueConverter.toBoolean(constant.getValue()) ? "1" : "0";
                break;
            case Date:
            case DateRange:
                value = constant.getValue() == null ? "NULL" : "'" + FxFormatUtils.getDateFormat().format(FxValueConverter.toDate(constant.getValue())) + "'";
                break;
            case DateTime:
            case DateTimeRange:
                value = constant.getValue() == null ? "NULL" : "'" + FxFormatUtils.getDateTimeFormat().format(FxValueConverter.toDateTime(constant.getValue())) + "'";
                break;
            case Binary:
                if (cond.getComperator().equals(Comparator.IS_NOT) && constant.isNull()) {
                    value = "NULL";
                    break;
                }
                throw new FxSqlSearchException("ex.sqlSearch.reader.type.invalidOperator",
                        entry.getProperty().getDataType(), cond.getComperator());
            case Reference:
                value = String.valueOf(FxPK.fromString(constant.getValue()).getId());
                break;
            default:
                throw new FxSqlSearchException("ex.sqlSearch.reader.unknownPropertyColumnType",
                        entry.getProperty().getDataType(), prop.getPropertyName());
        }

        // IGNORE_CASE Filter
        if (stmt.getIgnoreCase()) {
            value = value.toUpperCase();
        }

        // Apply all Functions
        value = constant.getFunctionsStart() + value + constant.getFunctionsEnd();

        // Build the final filter statement
        if (entry.getTable().equals(DatabaseConst.TBL_CONTENT)) {
            return (" (SELECT DISTINCT cd.ID,cd.VER,null lang FROM " + TBL_MAIN + " cd WHERE " +
                    column +
                    cond.getSqlComperator() +
                    value +
                    _getVersionFilter(stmt, "cd") +
                    _getSubQueryLimit() +
                    ") ");
        } else {
            return " (SELECT DISTINCT cd.ID,cd.VER,cd.LANG FROM " + TBL_DATA + " cd WHERE " +
                    column +
                    cond.getSqlComperator() +
                    value +
                    " AND " + filter +
                    _getVersionFilter(stmt, "cd") +
                    _getLanguageFilter() +
                    _getSubQueryLimit() +
                    ") ";
        }
    }

    private String _getVersionFilter(FxStatement stmt, String tblAlias) {
        String tbl = (tblAlias == null || tblAlias.length() == 0) ? "" : tblAlias + ".";
        if (stmt.getVersionFilter() == Filter.VERSION.ALL) {
            return "";
        } else if (stmt.getVersionFilter() == Filter.VERSION.LIVE) {
            return " AND " + tbl + "ISLIVE_VER=true ";
        } else {
            return " AND " + tbl + "ISMAX_VER=true ";
        }
    }

    private String _getSubQueryLimit() {
        return SUBQUERY_LIMIT == -1 ? "" : " LIMIT " + SUBQUERY_LIMIT + " ";
    }

    private String _getLanguageFilter() {
        return search.getLanguage() == null ? "" : " and lang in (0," + search.getLanguage().getId() + ") ";
    }


}
