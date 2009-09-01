/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
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
package com.flexive.core.search.genericSQL;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.search.*;
import com.flexive.core.storage.FxTreeNodeInfo;
import com.flexive.core.storage.StorageManager;
import com.flexive.core.storage.genericSQL.GenericTreeStorage;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.Pair;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.search.DateFunction;
import com.flexive.shared.search.FxFoundType;
import com.flexive.shared.search.query.VersionFilter;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxFlatStorageMapping;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.sqlParser.*;
import static com.flexive.sqlParser.Condition.ValueComparator;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
 * @version $Rev$
 */
public class GenericSQLDataFilter extends DataFilter {
    private static final Log LOG = LogFactory.getLog(GenericSQLDataFilter.class);

//    private static final String NO_MATCH = "(SELECT DISTINCT null id,null ver,null lang FROM dual where 1=2)";

    /** 
     * The maximum rows returned by a subquery (property or assignement search)
     * Deliberately set to -1 by default to guarantee correct results - if the query gets too
     * slow, it will timeout anyway.
     */
    private static final int SUBQUERY_LIMIT = -1;

    private final String tableMain;
    private final String tableContentData;
    private final String tableFulltext;
    private final List<FxFoundType> contentTypes = new ArrayList<FxFoundType>(10);

    private int foundEntryCount;
    private boolean truncated;
    private Connection con;

    public GenericSQLDataFilter(Connection con, SqlSearch search) throws FxSqlSearchException {
        super(con, search);
        this.con = con;
        final long[] briefcaseIds = getStatement().getBriefcaseFilter();
        if (briefcaseIds.length == 0) {
            tableMain = DatabaseConst.TBL_CONTENT;
            tableContentData = DatabaseConst.TBL_CONTENT_DATA;
            tableFulltext = DatabaseConst.TBL_CONTENT_DATA_FT;
        } else {
            // TODO: Add briefcase security (may the user access the specified briefcases?)
            // TODO: Filter out undesired / desired languages
            final String briefcaseIdFilter = "SELECT id from " + DatabaseConst.TBL_BRIEFCASE_DATA +
                    " WHERE briefcase_id " +
                    (getStatement().getBriefcaseFilter().length == 1 ? ("=" + briefcaseIds[0]) : "IN (" +
                            StringUtils.join(ArrayUtils.toObject(briefcaseIds), ',') + ")");
            tableContentData = "(SELECT * FROM " + DatabaseConst.TBL_CONTENT_DATA +
                    " WHERE id IN (" + briefcaseIdFilter + ") " + getVersionFilter(null) + ")";
            tableMain = "(SELECT * FROM " + DatabaseConst.TBL_CONTENT +
                    " WHERE id IN (" + briefcaseIdFilter + ") " + getVersionFilter(null) +
                    getDeactivatedTypesFilter(null) + getInactiveMandatorsFilter(null) + ")";
            tableFulltext = "(SELECT * FROM " + DatabaseConst.TBL_CONTENT_DATA_FT +
                    " WHERE id IN (" + briefcaseIdFilter + "))"; //versions are matched against content_data table
        }
    }

    /**
     * Cleanup all temporary data used for the search.
     *
     * @throws FxSqlSearchException if the cleanup failed
     */
    @Override
    public void cleanup() throws FxSqlSearchException {
        if (search.getCacheTable().equals(DatabaseConst.TBL_SEARCHCACHE_MEMORY)) {
            Statement stmt = null;
            try {
                stmt = getConnection().createStatement();
                stmt.executeUpdate("DELETE FROM " + DatabaseConst.TBL_SEARCHCACHE_MEMORY +
                        " WHERE search_id=" + search.getSearchId());
            } catch (Throwable t) {
                throw new FxSqlSearchException(LOG, t, "ex.sqlSearch.err.failedToClearTempSearchResult", search.getSearchId());
            } finally {
                Database.closeObjects(GenericSQLDataSelector.class, null, stmt);
            }
        }
    }

    /**
     * Builds the filter.
     *
     * @throws FxSqlSearchException if the build failed
     */
    @Override
    public void build() throws FxSqlSearchException, SQLException {
        final UserTicket ticket = FxContext.getUserTicket();
        final long maxRows = search.getFxStatement().getMaxResultRows();
        Statement stmt = null;
        String sql = null;
        try {
            final String dataSelect;
            final String securityFilter = getSecurityFilter(ticket);
            if (getStatement().getType() == FxStatement.Type.ALL) {
                // The statement will not filter the data
                final String filters = StringUtils.defaultIfEmpty(securityFilter, "1=1")
                        + (getStatement().getBriefcaseFilter().length == 0 ? getVersionFilter("data2") : "");
                dataSelect = "SELECT " + search.getSearchId() + " search_id,id,ver,tdef,created_by FROM " + tableMain + " data2\n"
                        + (StringUtils.isNotBlank(filters) ? "WHERE " + filters + "\n" : "")
                        + " LIMIT " + maxRows;
            } else {
                // The statement filters the data
                StringBuilder result = new StringBuilder(5000);
                build(result, getStatement().getRootBrace());
                // Remove leading and ending brace
                result.deleteCharAt(0);
                result.deleteCharAt(result.length() - 1);
                // Finalize the select
                dataSelect = "SELECT * FROM (SELECT DISTINCT " + search.getSearchId() +
                        " search_id,data.id,data.ver,main.tdef,main.created_by \n" +
                        "FROM (" + result.toString() + ") data, " + DatabaseConst.TBL_CONTENT + " main\n" +
                        "WHERE data.ver=main.ver AND data.id=main.id) data2\n"
                        + (StringUtils.isNotBlank(securityFilter) ? "WHERE " + securityFilter : "") +
                        // Limit by the specified max items
                        "LIMIT " + maxRows;
            }
            // Find all matching data enties and store them
            sql = "INSERT INTO " + search.getCacheTable() + " " + dataSelect;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Filter SQL: " + sql);
            }
            stmt = getConnection().createStatement();
            if (isQueryTimeoutSupported())
                stmt.setQueryTimeout(search.getParams().getQueryTimeout());
            stmt.executeUpdate(sql);
            analyzeResult();
        } catch (SQLException exc) {
            throw exc;
        } catch (Exception e) {
            throw new FxSqlSearchException(LOG, e, "ex.sqlSearch.failedToBuildDataFilter", e.getMessage(), search.getQuery());
        } finally {
            Database.closeObjects(GenericSQLDataFilter.class, null, stmt);
        }
    }

    private String getSecurityFilter(UserTicket ticket) {
        return ticket.isGlobalSupervisor() ? "" :
                "mayReadInstance2(data2.id,data2.ver," + ticket.getUserId() + "," +
                        ticket.getMandatorId() + "," + ticket.isMandatorSupervisor() + "," + ticket.isGlobalSupervisor() + ")\n";
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
            final ResultSet rs = stmt.executeQuery("SELECT count(*), tdef FROM " + search.getCacheTable() +
                    " WHERE search_id=" + search.getSearchId() + " GROUP BY tdef");
            foundEntryCount = 0;
            while (rs != null && rs.next()) {
                final FxFoundType type = new FxFoundType(search.getEnvironment().getType(rs.getLong(2)), rs.getInt(1));
                foundEntryCount += type.getFoundEntries();
                contentTypes.add(type);
            }

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
            throw new FxSqlSearchException(LOG, t, "ex.sqlSearch.failedToCountFoundEntries", search.getSearchId());
        } finally {
            Database.closeObjects(GenericSQLDataFilter.class, null, stmt);
        }
    }

    /**
     * Returns a list of all content types that are part of the resultset.
     *
     * @return a list of all content types that are part of the resultset
     */
    @Override
    public List<FxFoundType> getContentTypes() {
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
        if (br.getElements().length > 1) {
            final Multimap<String, ConditionTableInfo> tables = getUsedContentTables(br);
            // for "OR" we can only always optimize flat storage queries,
            // as long as only one flat storage table is used and we don't have a nested 'and'
            if (tables.keySet().size() == 1
                    && tables.values().iterator().next().isFlatStorage()
                    && !br.containsAnd()) {
                sb.append(
                        getOptimizedFlatStorageSubquery(br, tables.keySet().iterator().next())
                );
                return;
            }
        }

        sb.append("(SELECT * FROM (\n");
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
                throw new FxSqlSearchException(LOG, "ex.sqlSearch.filter.invalidBrace", be);
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
        if (br.getElements().length > 1) {
            final Multimap<String, ConditionTableInfo> tables = getUsedContentTables(br);
            // for "AND" we can only optimize when ALL flatstorage conditions are not multi-lang and on the same level,
            // i.e. that table must have exactly one flat-storage entry
            if (tables.size() == 1 && tables.values().iterator().next().isFlatStorage()) {
                sb.append(
                        getOptimizedFlatStorageSubquery(br, tables.keySet().iterator().next())
                );
                return;
            }
        }
        int pos = 0;
        final StringBuilder combinedConditions = new StringBuilder();
        int firstId = -1;
        for (BraceElement be : br.getElements()) {
            if (pos == 0) {
                firstId = be.getId();
                // TODO: do we need .lang here?
                sb.append(("(SELECT tbl" + firstId + ".id,tbl" + firstId + ".ver,tbl" + firstId + ".lang FROM\n"));
            } else {
                sb.append(",");
                combinedConditions.append((pos > 1) ? " AND " : " ")
                        .append("tbl").append(firstId).append(".id=tbl").append(be.getId()).append(".id AND ")
                        .append("tbl").append(firstId).append(".ver=tbl").append(be.getId()).append(".ver AND ")
                        .append("(tbl").append(firstId).append(".lang=0 or tbl").append(firstId).append(".lang IS NULL OR ")
                        .append("tbl").append(be.getId()).append(".lang=0 OR tbl").append(be.getId()).append(".lang IS NULL OR ")
                        .append("tbl").append(firstId).append(".lang=tbl").append(be.getId()).append(".lang)");
            }

            if (be instanceof Condition) {
                sb.append(getConditionSubQuery(br.getStatement(), (Condition) be));
            } else if (be instanceof Brace) {
                build(sb, (Brace) be);
            } else {
                throw new FxSqlSearchException(LOG, "ex.sqlSearch.filter.invalidBrace", be);
            }
            sb.append(" tbl").append(be.getId()).append("\n");
            pos++;
        }
        // Where links the tables together
        sb.append(" WHERE ");
        sb.append(combinedConditions);
        // Close AND
        sb.append(")");
    }

    private String getOptimizedFlatStorageSubquery(Brace br, String flatStorageTable) throws FxSqlSearchException {
        return "(SELECT DISTINCT cd.id, cd.ver, null as lang FROM \n"
                + flatStorageTable + " cd WHERE "
                + getOptimizedFlatStorageConditions(br)
                + getSubQueryLimit()
                + ")";
    }

    private String getOptimizedFlatStorageConditions(Brace br) throws FxSqlSearchException {
        final StringBuilder out = new StringBuilder();
        boolean first = true;
        for (BraceElement be : br.getElements()) {
            if (!first) {
                out.append(' ').append(br.getType()).append(' ');   // add and/or
            }
            if (be instanceof Condition) {
                // add conditions of the flat storage query
                final Condition cond = (Condition) be;
                out.append(
                        buildPropertyCondition(br.getStatement(), cond, cond.getProperty(), true)
                );
            } else if (be instanceof Brace) {
                // recurse
                out.append('(').append(getOptimizedFlatStorageConditions((Brace) be)).append(')');
            } else {
                throw new IllegalArgumentException("Unexpected brace element " + be);
            }
            first = false;
        }
        return out.toString();
    }

    /**
     * Return the content tables used by the given (sub-)condition. The key contains the (SQL) table name,
     * the value information about the modes in which the table is accessed. Depending on the number of
     * different accesses (e.g. multilang/no multilang), a query may or may not be optimized.
     *
     * @param br    the condition
     * @return      the table infos
     */
    private Multimap<String, ConditionTableInfo> getUsedContentTables(Brace br) throws FxSqlSearchException {
        final Multimap<String, ConditionTableInfo> tables = HashMultimap.create();
        for (BraceElement be : br.getElements()) {
            if (be instanceof Condition) {
                final PropertyEntry entry = getPropertyResolver().get(getStatement(), ((Condition) be).getProperty());
                if (entry.getAssignment() != null && entry.getAssignment().isFlatStorageEntry()) {
                    final FxFlatStorageMapping mapping = entry.getAssignment().getFlatStorageMapping();
                    tables.put(
                            mapping.getStorage(),
                            new ConditionTableInfo(
                                    true,
                                    entry.getAssignment().isMultiLang(),
                                    mapping.getLevel()
                            )
                    );
                } else {
                    tables.put(
                            entry.getTableType().getTableName(),
                            new ConditionTableInfo(
                                    false,
                                    entry.getAssignment() != null && entry.getAssignment().isMultiLang(),
                                    -1
                            )
                    );
                }
            } else if (be instanceof Brace) {
                tables.putAll(getUsedContentTables((Brace) be));
            }
        }
        return tables;
    }

    /**
     * Helper function for getConditionSubQuery(__).
     *
     * @param cond the condition
     * @param mode tree mode
     * @return the filter sub-statement
     * @throws com.flexive.shared.exceptions.FxSqlSearchException
     *          if the tree node was invalid or could not be found
     */
    public String getTreeFilter(Condition cond, FxTreeMode mode) throws FxSqlSearchException {
        boolean direct = cond.getComperator() == ValueComparator.IS_DIRECT_CHILD_OF;
//        String table = DatabaseConst.TBL_TREE+(mode == FxTreeMode.Live ?"L":"");

        long parentNode;
        final String value = cond.getConstant().getValue();
        try {
            parentNode = Long.valueOf(value);
        } catch (NumberFormatException e) {
            final String path = FxSharedUtils.stripQuotes(value, '\'');
            try {
                // Lookup the path in the tree
                parentNode = search.getTreeEngine().getIdByPath(mode, path);
            } catch (FxApplicationException e2) {
                throw new FxSqlSearchException(LOG, e2, "ex.sqlSearch.filter.invalidTreePath", path);
            }
            if (parentNode == -1) {
                throw new FxSqlSearchException(LOG, "ex.sqlSearch.filter.invalidTreePath", path);
            }
        }
        if (parentNode < 0) {
            throw new FxSqlSearchException(LOG, "ex.sqlSearch.filter.invalidTreeId", parentNode);
        }

        final FxTreeNodeInfo nodeInfo;
        try {
            nodeInfo = StorageManager.getTreeStorage().getTreeNodeInfo(con, mode, parentNode);
        } catch (FxApplicationException e) {
            throw new FxSqlSearchException(LOG, e, "ex.sqlSearch.filter.loadTreeNode", parentNode, e.getMessage());
        }
        String mandators = search.getEnvironment().getInactiveMandatorList();
        final String mandatorFilter;
        if (mandators.length() > 0)
            mandatorFilter = " AND cd.mandator NOT IN(" + mandators + ")";
        else
            mandatorFilter = mandators; //empty
        String types = search.getEnvironment().getDeactivatedTypesList();
        final String typeFilter;
        if (types.length() > 0)
            typeFilter = " AND cd.tdef NOT IN(" + types + ")";
        else
            typeFilter = types; //empty
        final String versionFilter = " AND cd." + (mode.equals(FxTreeMode.Edit) ? "ismax_ver" : "islive_ver") + "=true";
        return "(SELECT DISTINCT cd.id,cd.ver,null lang FROM " + tableMain + " cd WHERE " +
                "cd.id IN (SELECT ref FROM " + GenericTreeStorage.getTable(mode) + " WHERE " +
                "LFT>" + nodeInfo.getLeft() + " AND RGT<" + nodeInfo.getRight() + " AND ref IS NOT NULL " +
                (direct ? " AND depth=" + (nodeInfo.getDepth() + 1) : "") +
                ")" + mandatorFilter + typeFilter + versionFilter + ")";

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
        final Table tblContent = stmt.getTableByType(Table.TYPE.CONTENT);
        if (tblContent == null) {
            throw new FxSqlSearchException(LOG, "ex.sqlSearch.contentTableMissing");
        }
        final Property prop = cond.getProperty();
        final Constant constant = cond.getConstant();


        if (cond.getComperator() == ValueComparator.IS_CHILD_OF ||
                cond.getComperator() == ValueComparator.IS_DIRECT_CHILD_OF) {
            // In case of VERSION.ALL we will look up the LIVE and EDIT tree
            String result = "";
            int count = 0;
            if (stmt.getVersionFilter() == VersionFilter.LIVE ||
                    stmt.getVersionFilter() == VersionFilter.ALL) {
                result += getTreeFilter(cond, FxTreeMode.Live);
                count++;
            }
            if (stmt.getVersionFilter() == VersionFilter.MAX ||
                    stmt.getVersionFilter() == VersionFilter.ALL) {
                if (result.length() > 0) {
                    result += "\n UNION \n";
                }
                result += getTreeFilter(cond, FxTreeMode.Edit);
                count++;
            }
            return (count < 2) ? result : "(" + result + ")";
        } else if (prop.getPropertyName().equals("*")) {
            // Fulltext search
            return "(SELECT DISTINCT ft.id,ft.ver,ft.lang " +
                    "FROM " + tableFulltext + " ft," + DatabaseConst.TBL_CONTENT + " cd WHERE\n" +
                    "cd.ver=ft.ver AND cd.id=ft.id AND\n" +
                    fulltextMatch("value", constant.getValue()) + " " +
                    getLanguageFilter() +
                    getVersionFilter("cd") +
                    getDeactivatedTypesFilter("cd") +
                    getInactiveMandatorsFilter("cd") +
                    getSubQueryLimit() +
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
                    return NO_MATCH;
                }
                FxProperty fx_prop = ((FxPropertyAssignment)fx_ass).getProperty();
                Property _prop = new Property(prop.getTableAlias(),fx_prop.getName(),prop.getField());
                return buildPropertyCondition(stmt,cond,_prop,(FxPropertyAssignment)fx_ass);
            */
        } else {
            return buildPropertyCondition(stmt, cond, cond.getProperty(), false);
        }
    }

    /**
     * Perform a fulltext match
     *
     * @param column column to match
     * @param expr   expression to match against
     * @return sql snippet
     */
    protected String fulltextMatch(String column, String expr) {
        return "MATCH (" + column + ") AGAINST (" + expr + ")";
    }

    /**
     * Helper function.
     *
     * @param stmt the statement
     * @param cond the condition
     * @param prop the property
     * @param optimizedFlatStorage  if only the flat storage condition should be returned (used for optimization)
     * @return the subquery
     * @throws FxSqlSearchException if a error occured
     */
    private String buildPropertyCondition(FxStatement stmt, Condition cond, Property prop, boolean optimizedFlatStorage)
            throws FxSqlSearchException {

        final Constant constant = cond.getConstant();
        final PropertyEntry entry = getPropertyResolver().get(stmt, prop);
        final String filter;
        if (prop.isAssignment()) {
            filter = "ASSIGN IN ("
                    + StringUtils.join(FxSharedUtils.getSelectableObjectIdList(entry.getAssignmentWithDerived()), ',')
                    + ")";
        } else if (entry.getProperty() != null) {
            filter = "TPROP=" + entry.getProperty().getId();
        } else {
            throw new FxSqlSearchException("ex.sqlSearch.filter.condition.structure", prop.getPropertyName());
        }

        if (cond.getComperator() == Condition.ValueComparator.IS && cond.getConstant().isNull()) {
            // IS NULL is a special case for the content data/flat storage table:
            // a property is null if no entry is present in the table, which means we must
            // find the entries by using a join on the main table

            if (entry.getTableType() == PropertyResolver.Table.T_CONTENT_DATA) {
                return isNullSelect(entry, prop, tableContentData, filter, "tprop");
            } else if (entry.getTableType() == PropertyResolver.Table.T_CONTENT_DATA_FLAT) {
                final FxFlatStorageMapping mapping = entry.getAssignment().getFlatStorageMapping();
                return isNullSelect(entry, prop, mapping.getStorage(),
                        SearchUtils.getFlatStorageAssignmentFilter(search.getEnvironment(), "da", entry.getAssignment()),
                        mapping.getColumn()
                );
            }
        }


        // Apply all Functions
        final Pair<String, String> select = getValueCondition(prop, constant, entry, cond.getComperator());
        if (select == null) {
            throw new FxSqlSearchException(LOG, "ex.sqlSearch.reader.unknownPropertyColumnType",
                    entry.getProperty().getDataType(), prop.getPropertyName());
        }
        final String column = prop.getFunctionsStart() + select.getFirst() + prop.getFunctionsEnd();
        final String value = constant.getFunctionsStart()
                + (stmt.getIgnoreCase() ? select.getSecond().toUpperCase() : select.getSecond())
                + constant.getFunctionsEnd();

        // Build the final filter statement
        switch(entry.getTableType()) {
            case T_CONTENT:
                return (" (SELECT DISTINCT cd.id,cd.ver,null lang FROM " + tableMain + " cd WHERE " +
                        column +
                        cond.getSqlComperator() +
                        value +
                        getVersionFilter("cd") +
                        getDeactivatedTypesFilter("cd") +
                        getInactiveMandatorsFilter("cd") +
                        getSubQueryLimit() +
                        ") ");
            case T_CONTENT_DATA:
                // the FInt column is required for "NOT IN" matches of SelectMany
                final boolean usesFInt = entry.getProperty().getDataType() == FxDataType.SelectMany && cond.getComperator() == ValueComparator.NOT_IN;
                return " (SELECT DISTINCT cd.id,cd.ver,cd.lang" +
                        (usesFInt ? ",cd.fint" : "") +
                        " FROM " + tableContentData + " cd WHERE " +
                        column +
                        cond.getSqlComperator() +
                        value +
                        " AND " + filter +
                        getVersionFilter("cd") +
                        getLanguageFilter() +
                        getGroupByFilter("cd", cond, entry, value) +
                        getSubQueryLimit() +
                        ") ";
            case T_CONTENT_DATA_FLAT:
                final FxFlatStorageMapping mapping = entry.getAssignment().getFlatStorageMapping();
                final String comparison = entry.getFilterColumn() + cond.getSqlComperator() + value;
                final String condition =
                        SearchUtils.getFlatStorageAssignmentFilter(search.getEnvironment(), "cd", entry.getAssignment()) +
                                " AND " + comparison +
                        getVersionFilter("cd") +
                        getLanguageFilter();

                if (optimizedFlatStorage) {
                    // don't do a subselect, only return the condition
                    return "(" + condition + ")";
                } else {
                    return " (SELECT DISTINCT cd.id, cd.ver, cd.lang " +
                            "FROM " + mapping.getStorage() + " cd " +
                            "WHERE " 
                            + condition
                            + getSubQueryLimit()
                            + ")";
                }
            default:
                throw new FxSqlSearchException(LOG, "ex.sqlSearch.err.unknownPropertyTable", entry.getProperty().getName(), entry.getTableName());
        }
    }

    private String isNullSelect(PropertyEntry entry, Property prop, String dataTable, String dataFilter, String dataJoinColumn) {
        return "(SELECT DISTINCT ct.id, ct.ver, null AS lang FROM " + tableMain + " ct\n" +
                "LEFT JOIN " + dataTable + " da ON (ct.id=da.id AND ct.ver=da.ver AND " + dataFilter + ")\n" +
                "WHERE da." + dataJoinColumn + " IS NULL " +
                getVersionFilter("ct") +
                getDeactivatedTypesFilter("ct") +
                getInactiveMandatorsFilter("ct") +
                // Assignment search: we are only interrested in the type that the assignment belongs to
                (prop.isAssignment()
                        ? "AND ct.TDEF=" + entry.getAssignment().getAssignedType().getId() + " "
                        : "") +
                getSubQueryLimit() + ")";
    }

    private String getGroupByFilter(String tableAlias, Condition cond, PropertyEntry entry, String value) {
        if (entry.getProperty().getDataType() == FxDataType.SelectMany
                && (cond.getComperator() == ValueComparator.IN || cond.getComperator() == ValueComparator.NOT_IN)) {
            // IN/NOT IN for SelectMany: ensure that ALL listed options are matched by an object, otherwise
            // the result would get very noisy when many options are selected
            return " GROUP BY " + tableAlias + ".id, " + tableAlias + ".ver, " + tableAlias + ".lang " +
                    "HAVING COUNT(*) = " +
                    (cond.getComperator() == ValueComparator.IN
                            // IN: match number of IDs
                            ? value.split(",").length
                            // NOT IN: match results that got no rows removed (FINT stores the number of selected items)
                            : tableAlias + ".FINT");
        } else {
            return "";
        }
    }

    protected Pair<String, String> getValueCondition(Property prop, Constant constant, PropertyEntry entry, ValueComparator comparator) throws FxSqlSearchException {
        String column = entry.getFilterColumn();
        String value = null;
        switch (entry.getProperty().getDataType()) {
            case SelectMany:
                if (comparator == ValueComparator.EQUAL || comparator == ValueComparator.NOT_EQUAL) {
                    // exact match, so we use the text column that stores the comma-separated list of selected items
                    column = "FTEXT1024";
                    value = "'" + StringUtils.join(constant.iterator(), ',') + "'";
                } else if (comparator != ValueComparator.IN && comparator != ValueComparator.NOT_IN){
                    throw new FxSqlSearchException(LOG, "ex.sqlSearch.reader.type.invalidOperator",
                            entry.getProperty().getDataType(), comparator);
                }
                break;
            case Date:
            case DateRange:
                if (isWithDateFunction(prop, constant)) {
                    // use a date function - currently all MySQL date functions may be used
                    value = constant.getValue();
                }
                break;
            case DateTime:
            case DateTimeRange:
                // CREATED_AT and MODIFIED_AT store the date in a "long" column with millisecond precision

                if (isWithDateFunction(prop, constant)) {
                    // use a date function - currently all MySQL date functions may be used
                    value = constant.getValue();
                    if (PropertyEntry.isDateMillisColumn(entry.getFilterColumn())) {
                        // need to convert from milliseconds
                        column = toDBTime(entry.getFilterColumn());
                    }
                }
                break;
            case Binary:
                if (comparator.equals(ValueComparator.IS_NOT) && constant.isNull()) {
                    value = "NULL";
                    break;
                }
                throw new FxSqlSearchException(LOG, "ex.sqlSearch.reader.type.invalidOperator",
                        entry.getProperty().getDataType(), comparator);
        }
        return value != null ? new Pair<String, String>(column, value) : entry.getComparisonCondition(constant.getValue());
    }

    /**
     * Convert a column with a timestamp in long format to a database understandable timestamp
     *
     * @param expr long column to convert
     * @return database time compliant expression
     */
    protected String toDBTime(String expr) {
        return "FROM_UNIXTIME(" + expr + "/1000)";
    }

    private boolean isWithDateFunction(Property property, Constant constant) {
        // check if only valid date functions are present
        for (String function : property.getSqlFunctions()) {
            DateFunction.getBySqlName(function);
        }
        return StringUtils.isNumeric(constant.getValue()) && !property.getSqlFunctions().isEmpty();
    }

    /**
     * Get a filter for the version
     *
     * @param tblAlias table alias
     * @return filter statement
     */
    private String getVersionFilter(String tblAlias) {
        String tbl = (tblAlias == null || tblAlias.length() == 0) ? "" : tblAlias + ".";
        if (getStatement().getVersionFilter() == VersionFilter.ALL) {
            return "";
        } else if (getStatement().getVersionFilter() == VersionFilter.LIVE) {
            return " AND " + tbl + "islive_ver=TRUE ";
        } else {
            return " AND " + tbl + "ismax_ver=TRUE ";
        }
    }

    /**
     * Get a filter for (in)active mandators
     *
     * @param tblAlias table alias
     * @return filter statement
     */
    private String getInactiveMandatorsFilter(String tblAlias) {
        String tbl = (tblAlias == null || tblAlias.length() == 0) ? "" : tblAlias + ".";
        String mandators = search.getEnvironment().getInactiveMandatorList();
        if (mandators.length() > 0)
            return " AND " + tbl + "mandator NOT IN(" + mandators + ")";
        else
            return ""; //empty
    }

    /**
     * Get a filter for deactivated FxType's
     *
     * @param tblAlias table alias
     * @return filter statement
     */
    private String getDeactivatedTypesFilter(String tblAlias) {
        String tbl = (tblAlias == null || tblAlias.length() == 0) ? "" : tblAlias + ".";
        String types = search.getEnvironment().getDeactivatedTypesList();
        if (types.length() > 0)
            return " AND " + tbl + "tdef NOT IN(" + types + ")";
        else
            return ""; //empty
    }

    protected String getSubQueryLimit() {
        return SUBQUERY_LIMIT == -1 ? "" : " LIMIT " + SUBQUERY_LIMIT + " ";
    }

    private String getLanguageFilter() {
        return search.getSearchLanguage() == null ? "" : " AND lang IN (0," + search.getSearchLanguage().getId() + ") ";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isQueryTimeoutSupported() {
        //defaults to true, implementations for databases that do not support timeouts have to override this method
        return true;
    }

    /**
     * Helper class for {@link com.flexive.core.search.genericSQL.GenericSQLDataFilter#getOptimizedFlatStorageConditions(com.flexive.sqlParser.Brace)}.
     * Holds information about the attributes required for addressing an assignment in a condition. 
     */
    private static class ConditionTableInfo {
        private final boolean flatStorage;
        private final boolean multiLang;
        private final int level;

        private ConditionTableInfo(boolean flatStorage, boolean multiLang, int level) {
            this.flatStorage = flatStorage;
            this.multiLang = multiLang;
            this.level = level;
        }

        public boolean isFlatStorage() {
            return flatStorage;
        }

        public boolean isMultiLang() {
            return multiLang;
        }

        public int getLevel() {
            return level;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final ConditionTableInfo that = (ConditionTableInfo) o;
            return flatStorage == that.flatStorage && level == that.level && multiLang == that.multiLang;
        }

        @Override
        public int hashCode() {
            int result =  flatStorage ? 1 : 0;
            result = 31 * result + (multiLang ? 1 : 0);
            result = 31 * result + level;
            return result;
        }
    }
}
