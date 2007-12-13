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

import com.flexive.core.DatabaseConst;
import com.flexive.core.sqlSearchEngines.*;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxArrayUtils;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.sqlParser.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * MySQL specific data selector
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class MySQLDataSelector extends DataSelector {
    private static final transient Log LOG = LogFactory.getLog(MySQLDataSelector.class);

    /**
     * All field selectors supported by this implementation
     */
    private final static Hashtable<String, FxFieldSelector> SELECTORS =
            new Hashtable<String, FxFieldSelector>(5);

    static {
        SELECTORS.put("MANDATOR", new MySQLGenericSelector(DatabaseConst.TBL_MANDATORS, "id"));
        SELECTORS.put("CREATED_BY", new MySQLGenericSelector(DatabaseConst.TBL_ACCOUNTS, "id"));
        SELECTORS.put("MODIFIED_BY", new MySQLGenericSelector(DatabaseConst.TBL_ACCOUNTS, "id"));
        SELECTORS.put("ACL", new MySQLACLSelector());
        SELECTORS.put("STEP", new MySQLStepSelector());
    }

    private final static String CONTENT_DIRECT_SELECT[] = {"ID", "VERSION", "MAINLANG"};
    private final static String CONTENT_DIRECT_SELECT_PROP[] = {"ID", "VER", "MAINLANG"};
    private FxSearch search;


    /**
     * Constructor.
     *
     * @param search the search object
     */
    public MySQLDataSelector(FxSearch search) {
        this.search = search;
    }

    @Override
    public Hashtable<String, FxFieldSelector> getSelectors() {
        return SELECTORS;
    }

    private String FILTER_ALIAS = "filter";
    private String SUBSEL_ALIAS = "sub";

    @Override
    public String build(final Connection con) throws FxSqlSearchException {
        StringBuffer select = new StringBuffer(2000);
        buildColumnSelectList(select);
        if (LOG.isDebugEnabled()) {
            LOG.debug(search.getFxStatement().printDebug());
        }
        return select.toString();
    }

    @Override
    public void cleanup(Connection con) throws FxSqlSearchException {
        // nothing to do
    }

    /**
     * Builds the column select.
     * <p/>
     * Example result: "x.oid,x.ver,(select ..xx.. from ...),..."
     *
     * @param select the stringbuffer to write to
     * @throws FxSqlSearchException if the function fails
     */
    private void buildColumnSelectList(final StringBuffer select) throws FxSqlSearchException {

        // Build all value selectors
        int pos = 0;
        FxSubSelectValues values[] = new FxSubSelectValues[search.getFxStatement().getSelectedValues().size()];
        for (SelectedValue __sel : search.getFxStatement().getSelectedValues()) {
            // Prepare all values
            Value sel = __sel.getValue();
            PropertyResolver.Entry entry = null;
            if (sel instanceof Property) {
                PropertyResolver pr = search.getPropertyResolver();
                entry = pr.get(search.getFxStatement(), (Property) sel);
                pr.addResultSetColumn(entry);
            }
            values[pos] = selectFromTbl(entry, sel, pos);
            pos++;
        }

        // Build the final select statement

        select.append("select \n");
        select.append(FILTER_ALIAS).append(".rownr,").
                append(FILTER_ALIAS).append(".id, ").
                append(FILTER_ALIAS).append(".ver\n");
        for (FxSubSelectValues ssv : values) {
            for (FxSubSelectValues.Item item : ssv.getItems()) {
                if (ssv.isSorted() && item.isOrderBy()) {
                    select.append(",").append(FILTER_ALIAS).append(".").append(item.getAlias()).append("\n");
                } else {
                    select.append(",").append(item.getSelect()).append(" ").append(item.getAlias()).append("\n");
                }
            }
        }
        select.append("from (");
        select.append(_buildSourceTable(values));
        select.append(") ").append(FILTER_ALIAS).append(" order by 1");

    }

    /**
     * Builds the source table.
     *
     * @param values the selected values
     * @return a select statement which builds the source table
     */
    private String _buildSourceTable(final FxSubSelectValues values[]) {

        // Prepare type filter
        String typeFilter = "";
        if (search.getTypeFilter() != null) {
            typeFilter = " and " + FILTER_ALIAS + ".tdef=" + search.getTypeFilter().getId() + " ";
        }

        StringBuffer orderBy = new StringBuffer(1024);
        String orderByNumbers = "";
        int orderByPos = 5;
        orderBy.append("select @rownr:=@rownr+1 rownr,").
                append(("concat(concat(concat(concat(concat(t.name,'[@pk=')," + FILTER_ALIAS + ".id),'.')," + FILTER_ALIAS + ".ver),']') xpathPref,")).
                append(FILTER_ALIAS).append(".id,").append(FILTER_ALIAS).append(".ver\n");

        for (FxSubSelectValues ssv : values) {
            if (ssv.isSorted()) {
                for (FxSubSelectValues.Item item : ssv.getItems()) {
                    if (item.isOrderBy()) {
                        orderBy.append(",").append(item.getSelect()).append(" ").append(item.getAlias()).append("\n");
                        orderByNumbers += (orderByNumbers.length() == 0 ? "" : ",") + orderByPos + " " +
                                (ssv.isSortedAscending() ? "asc" : "desc");
                    } else {
                        orderBy.append(",null\n");
                    }
                    orderByPos++;
                }
            }
        }
        orderBy.append(("from " + search.getCacheTable() + " filter, " + DatabaseConst.TBL_STRUCT_TYPES + " t " +
                "where SEARCH_ID=" + search.getSearchId() + " and " + FILTER_ALIAS + ".tdef=t.id " +
                typeFilter + " "));

        // No order by specified = order by id and version
        if (orderByNumbers.length() == 0) {
            orderByNumbers = "2 asc, 3 asc";
        }
        orderBy.append(("ORDER BY " + orderByNumbers));

        // Evaluate the order by, then limit the result by the desired range if needed
        String result = orderBy.toString();
        if (search.getStartIndex() > 0 && search.getFetchRows() < Integer.MAX_VALUE) {
            result = "select * from (" + result + ") tmp limit " + search.getStartIndex() + "," + search.getFetchRows();
        }
        return result;

    }

    /**
     * Returns null if the pos is not part of the orderBy, or true if it is sorted ascending or
     * false if it is ordered descending.
     *
     * @param pos the position to check
     * @return null if the pos is not part of the orderBy, or true if it is sorted ascending or
     *         false if it is ordered descending
     */
    private Boolean _isOrderByPos(int pos) {
        ArrayList<OrderByValue> obvs = search.getFxStatement().getOrderByValues();
        for (OrderByValue obv : obvs) {
            if (obv.getColumnIndex() == pos) return obv.isAscending();
        }
        return null;
    }

    /**
     * Build the subselect query needed to get any data from the CONTENT table.
     *
     * @param entry     the entry to select
     * @param prop      the property
     * @param resultPos the position of the property in the select statement
     * @return the FxSubSelectValues
     * @throws FxSqlSearchException if anything goes wrong
     */
    private FxSubSelectValues selectFromTbl(PropertyResolver.Entry entry, Value prop, int resultPos) throws FxSqlSearchException {

        Boolean isOrderBy = _isOrderByPos(resultPos);
        FxSubSelectValues result = new FxSubSelectValues(resultPos, isOrderBy);

        if (prop instanceof Constant || entry == null) {
            result.addItem(prop.getValue().toString(), resultPos, false);
        } else if (entry.getType() == PropertyResolver.Entry.TYPE.NODE_POSITION) {
            long root = FxContext.get().getNodeId();
            if (root == -1) root = FxTreeNode.ROOT_NODE;
            String sel = "(select tree_nodeIndex(" + root + "," + FILTER_ALIAS + ".id,false))";   // TODO: LIVE/EDIT
            result.addItem(sel, resultPos, false);
        } else if (entry.getType() == PropertyResolver.Entry.TYPE.PATH) {
            long propertyId = CacheAdmin.getEnvironment().getProperty("CAPTION").getId();
            String sel = "(select tree_FTEXT1024_Paths(" + FILTER_ALIAS + ".id," +
                    search.getLanguage().getId() + "," + propertyId + ",false))"; // TODO: LIVE/EDIT
            result.addItem(sel, resultPos, false);
        } else {
            switch (entry.getTableType()) {
                case T_CONTENT:
                    for (String column : entry.getReadColumns()) {
                        int directSelect = FxArrayUtils.indexOf(CONTENT_DIRECT_SELECT, column, true);
                        if (directSelect > -1) {
                            String val = FILTER_ALIAS + "." + CONTENT_DIRECT_SELECT_PROP[directSelect];
                            result.addItem(val, resultPos, false);
                        } else {
                            String sVal = "(SELECT " + SUBSEL_ALIAS + "." + column + " FROM " + DatabaseConst.TBL_CONTENT +
                                    " " + SUBSEL_ALIAS + " WHERE " + SUBSEL_ALIAS + ".id=" + FILTER_ALIAS + ".id AND " +
                                    SUBSEL_ALIAS + ".ver=" + FILTER_ALIAS + ".ver)";
                            result.addItem(sVal, resultPos, false);
                        }
                    }
                    break;
                case T_CONTENT_DATA:
                    for (String column : entry.getReadColumns()) {
                        String sVal = _hlp_content_data(column, entry);
                        result.addItem(sVal, resultPos, false);
                    }
                    String sValXpath = "concat(filter.xpathPref," + _hlp_content_data("XPATHMULT", entry) + ")";
                    result.addItem(sValXpath, resultPos, true);
                    break;
                default:
                    throw new FxSqlSearchException("ex.sqlSearch.table.typeNotSupported", entry.getTable());
            }
        }

        return result.prepare(this, prop, entry);

    }

    /**
     * Helper function.
     *
     * @param column the column that is being procesed
     * @param entry  the entry
     * @return the subselect string for the value
     */
    private String _hlp_content_data(String column, PropertyResolver.Entry entry) {
        // TODO: lang fallback
        return "(SELECT " + SUBSEL_ALIAS + "." + column +
                " FROM " + DatabaseConst.TBL_CONTENT_DATA + " " +
                SUBSEL_ALIAS + " WHERE " +
                SUBSEL_ALIAS + ".id=" +
                FILTER_ALIAS + ".id AND " +
                SUBSEL_ALIAS + ".ver=" +
                FILTER_ALIAS + ".ver AND " +
                (entry.isAssignment()
                        ? "ASSIGN=" + entry.getAssignment().getId()
                        : "TPROP=" + entry.getProperty().getId()) +
                " LIMIT 1 " + ")";
    }


}
