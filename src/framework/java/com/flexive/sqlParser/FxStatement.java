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
package com.flexive.sqlParser;

import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.search.query.VersionFilter;
import com.flexive.shared.FxSharedUtils;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Statement.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxStatement {
    private static final Log LOG = LogFactory.getLog(FxStatement.class);

    public static enum Type {
        /** the statement filter the data */
        FILTER,
        /** the statement will always return a empty resultset */
        EMPTY,
        /** The statement will always return the whole data from the DB */
        ALL
    }

    private HashMap<String, Table> tables;
    private Brace currentBrace;
    private Brace rootBrace;
    private Map<Filter.TYPE, Filter> filters;
    private boolean debug = false;
    private int braceElementIdGenerator = 1;
    private Type type = Type.FILTER;
    private List<SelectedValue> selected;
    private List<OrderByValue> order;
    private int parserExecutionTime = -1;
    private int maxResultRows = -1;
    private String cacheKey;
    private boolean distinct;
    private boolean ignoreCase = true;
    private VersionFilter versionFilter = null;
    private long[] briefcaseFilter = null;
    private String contentType;

    protected FxStatement() throws SqlParserException {
        this.rootBrace = new Brace(this);
        this.currentBrace = this.rootBrace;
        this.tables = new HashMap<String, Table>(10);
        this.filters = new HashMap<Filter.TYPE, Filter>(5);
        this.selected = new ArrayList<SelectedValue>(50);
        this.order = new ArrayList<OrderByValue>(5);
        addTable(new Table("content", "co"));
    }

    protected void setBriefcaseFilter(long[] bf) {
        briefcaseFilter = bf;
    }

    /**
     * Returns a empty array if the filter is not set, or the id's of all briefcases to search in.
     *
     * @return a empty array if the filter is not set, or the id's of all briefcases to search in.
     */
    public long[] getBriefcaseFilter() {
        return briefcaseFilter == null ? new long[0] : briefcaseFilter;
    }

    public boolean hasVersionFilter() {
        return versionFilter != null;
    }

    public VersionFilter getVersionFilter() {
        return versionFilter == null ? VersionFilter.MAX : versionFilter;
    }


    public void setVersionFilter(VersionFilter filter) {
        this.versionFilter = filter;
    }

    public boolean getIgnoreCase() {
        return ignoreCase;
    }

    protected void setIgnoreCase(boolean ignoreCase) {
        this.ignoreCase = ignoreCase;
    }

    /**
     * Gets the maximum rows returned by the search.
     *
     * @return the maximum rows returned by the search
     */
    public int getMaxResultRows() {
        return maxResultRows == -1 ? 2000 : maxResultRows; // TODO
    }

    /**
     * Sets the maximum rows returned by the search.
     *
     * @param maxResultRows the maximum rows returned by the search
     */
    protected void setMaxResultRows(int maxResultRows) {
        this.maxResultRows = maxResultRows;
    }

    /**
     * Sets the contentname to filter by, may be null to indicate that the filter is not set
     *
     * @param contentName the content name
     */
    protected void setContentTypeFilter(String contentName) {
        this.contentType = contentName.trim().length() == 0 ? null : contentName.trim().toUpperCase();
    }

    /**
     * Returns the contentname to filter by, or null if this filter option is not set.
     *
     * @return the contentname or null
     */
    public String getContentTypeFilter() {
        return contentType;
    }

    /**
     * Returns true if the content type filter is set.
     *
     * @return true if the content type filter is set
     */
    public boolean hasContentTypeFilter() {
        return contentType != null;
    }


    /**
     * Generates a new statement scope unique brace id.
     *
     * @return a new brace id
     */
    protected int getNewBraceElementId() {
        return braceElementIdGenerator++;
    }

    /**
     * Add a Value to the selected elements.
     *
     * @param vi    the element
     * @param alias the alias
     */
    protected void addSelectedValue(final Value vi, String alias) {
        selected.add(new SelectedValue(vi, alias));
    }

    /**
     * Returns the selected values in the correct order.
     *
     * @return the selected values
     */
    public List<SelectedValue> getSelectedValues() {
        return selected;
    }

    /**
     * Returns the selected value matching the given alias, or null if no match can be found.
     * <p/>
     * If a alias is used more than one time the first match will be returned.
     *
     * @param alias the alias to look for
     * @return the matching selected value, or null
     */
    public SelectedValue getSelectedValueByAlias(String alias) {
        for (SelectedValue sv : selected) {
            if (sv.getAlias().equalsIgnoreCase(alias)) return sv;
        }
        return null;
    }

    /**
     * Overrides the selected values in the given order.
     *
     * @param values the new list
     */
    public void setSelectedValues(ArrayList<SelectedValue> values) {
        this.selected = values;
    }

    /**
     * Adds a new order by condition.
     *
     * @param vi the order by value (column)
     * @throws com.flexive.sqlParser.SqlParserException
     *          if the column cannot be used for ordering because it is not selected
     */
    public void addOrderByValue(final OrderByValue vi) throws SqlParserException {
        computeOrderByColumn(vi);
        order.add(0, vi);
    }

    /**
     * Returns the sort order elements.
     *
     * @return the sort order values
     */
    public List<OrderByValue> getOrderByValues() {
        return order;
    }

    /**
     * Returns a table used by the statement by its alias.
     *
     * @param alias the alias to look for
     * @return a table used by the statement by its alias
     */
    public Table getTableByAlias(String alias) {
        return this.tables.get(alias.toUpperCase());
    }

    public Table getTableByType(Table.TYPE type) {
        for (Table tbl : getTables()) {
            if (tbl.getType() == type) return tbl;
        }
        return null;
    }

    /**
     * Returns all tables that were specified in the 'from' section of the statement.
     *
     * @return all tables
     */
    public Table[] getTables() {
        Table[] result = new Table[tables.size()];
        int pos = 0;
        for (String key : this.tables.keySet()) {
            result[pos++] = this.tables.get(key);
        }
        return result;
    }

    /**
     * Returns the root brace.
     * <p/>
     * The root brace will be null if the getType is TYPE.ALL or TYPE.EMPTY
     *
     * @return the root brace
     */
    public Brace getRootBrace() {
        return this.rootBrace;
    }

    /**
     * Returns the statements type.
     * <p/>
     * TYPE.FILTER: there are conditions<br>
     * TYPE.EMPTY: the statement will not deliver any results<br>
     * TYPE.ALL: the statements will deliver all data from the selected sources (no filter set)
     *
     * @return the statements type
     */
    public Type getType() {
        return this.type;
    }


    /**
     * Parses a statement.
     *
     * @param query the query to process
     * @return the statement
     * @throws SqlParserException ifthe function fails
     */
    public static FxStatement parseSql(String query) throws SqlParserException {
        try {
            long startTime = System.currentTimeMillis();
            query = cleanupQueryString(query);
            ByteArrayInputStream byis = new ByteArrayInputStream(query.getBytes("UTF-8"));
            FxStatement stmt = new SQL(byis, "UTF-8").statement();
            byis.close();
            stmt.setParserExecutionTime((int) (System.currentTimeMillis() - startTime));
            return stmt;
        } catch (TokenMgrError exc) {
            throw new SqlParserException(exc, query);
        } catch (ParseException exc) {
            throw new SqlParserException(exc, query);
        } catch (SqlParserException exc) {
            throw exc;
        } catch (Exception exc) {
            throw new SqlParserException(exc.getMessage());
        }
    }

    protected void setParserExecutionTime(int ms) {
        this.parserExecutionTime = ms;
    }

    /**
     * Returns the execution time needed by the parser in ms.
     *
     * @return the execution time needed by the parser in ms
     */
    public int getParserExecutionTime() {
        return this.parserExecutionTime;
    }

    protected void addFilter(Filter f) {
        this.filters.put(f.getType(), f);
    }

    /**
     * Returns the desired filter, or null if the filter was not specified and has no
     * default value.
     *
     * @param t the filter to get
     * @return the filter
     */
    public Filter getFilter(Filter.TYPE t) {
        return filters.get(t);
    }


    protected void addTable(Table table) {
        if (debug) System.out.println("Adding table: " + table);
        this.tables.put(table.getAlias(), table);
    }
    
    protected boolean isTableAlias(String value) {
        for (String alias: this.tables.keySet()) {
            if (StringUtils.equalsIgnoreCase(alias, value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Notifies selected properties when all table aliases have been set.
     * parser
     */
    private void publishTableAliases() {
        for (SelectedValue selectedValue: selected) {
            if (selectedValue.getValue() instanceof Property) {
                ((Property) selectedValue.getValue()).publishTableAliases(this.tables.keySet());
            }
        }
    }


    protected Brace getCurrentBrace() {
        return this.currentBrace;
    }

    protected Brace startSubBrace() throws SqlParserException {
        Brace br = new Brace(this);
        currentBrace.addElement(br);
        currentBrace = br;
        return currentBrace;
    }

    protected Brace endSubBrace() throws SqlParserException {
        Brace parent = currentBrace.getParent();
        if (currentBrace.size() == 1) {
            // Brace with only one element, remove it and move its element to the parent
            BraceElement ele = currentBrace.removeLastElement();
            parent.removeElement(currentBrace);
            parent.addElement(ele);
        }
        currentBrace = parent;
        return currentBrace;
    }

    /**
     * Removes empty braces and handles conditions that are always false or true.
     * Also checks if all aliases are defined.
     *
     * @throws SqlParserException if the function fails
     */
    protected void cleanup() throws SqlParserException {
        publishTableAliases();
        // Check if all referenced tables are present
        for (SelectedValue val : selected) {
            if (!(val.getValue() instanceof Property)) continue;
            Property prop = (Property) val.getValue();
            if (this.getTableByAlias(prop.getTableAlias()) == null) {
                String stables = "";
                for (String alias : tables.keySet()) {
                    stables += ((stables.length() > 0) ? "," : "") + alias;
                }
                throw new SqlParserException("ex.sqlSearch.filter.unknownTableAlias", prop.getTableAlias(), stables);
            }
        }

        // Check order by
        for (OrderByValue ov : getOrderByValues()) {
            computeOrderByColumn(ov);
        }

        // If no where clause is set at all
        if (this.rootBrace == null || this.rootBrace.size() == 0) {
            rootBrace = null;
            type = Type.ALL;
            return;
        }

        // Cleanup where clause
        cleanup(this.rootBrace);

        if (rootBrace != null) {
            // Nothing left and TYPE=FILTER has to be set to TYPE.EMPTY
            if (this.rootBrace.size() == 0 && this.getType() == Type.FILTER) {
                this.type = Type.EMPTY;
                this.rootBrace = null;
            }
            // Only one condition at top level: type has to be null and not 'or'/'and'
            else if (this.rootBrace.size() == 1) {
                this.rootBrace.setType(null);
            }
        }
    }

    public boolean isWildcardSelected() {
        for (SelectedValue sv: selected) {
            if (sv.getValue() instanceof Property) {
                final Property prop = (Property) sv.getValue();
                if (prop.isWildcard() || prop.isUserPropsWildcard()) {
                    return true;
                }
            }
        }
        return false;
    }

    private void computeOrderByColumn(OrderByValue ov) throws SqlParserException {
        if (StringUtils.isNumeric(ov.getValue())) {
            // column selected by index (1-based)
            final int index = Integer.valueOf(ov.getValue()) - 1;
            if (index >= 0 && selected.size() > index) {
                ov.setSelectedValue(index);
            } else if (isWildcardSelected()) {
                ov.setSelectedValue(0 - index);
            } else {
                throw new SqlParserException("ex.sqlSearch.invalidOrderByIndex", ov.getValue(), selected.size());
            }
        } else {
            // column selected by alias
            boolean found = false;
            int pos = 0;
            for (SelectedValue sv : selected) {
                if (ov.isUsableForSorting(sv)) {
                    found = true;
                    ov.setSelectedValue(pos);
                    break;
                }
                pos++;
            }
            if (!found) {
                throw new SqlParserException("ex.sqlSearch.invalidOrderByValue", ov.getValue());
            }
        }
    }

    /**
     * Compute a cache key for the statement.
     * <p/>
     * Statements with the same cache key will produce the same resultset.
     *
     * @return the cacheKey.
     * @throws SqlParserException if the cache key could not be computed
     */
    protected String getCacheKey() throws SqlParserException {
        try {
            // Only build once
            if (cacheKey != null) {
                return cacheKey;
            }

            StringBuffer key = new StringBuffer(512);

            if (type == Type.FILTER) {
                computeCacheKey(key, this.rootBrace);
            } else {
                key.append(type);
            }

            for (String table : this.tables.keySet()) {
                Table t = this.tables.get(table);
                Filter vf = t.getFilter(Filter.TYPE.VERSION);
                String langs = "";
                for (String lang : t.getSearchLanguages()) {
                    langs += "|" + lang;
                }
                key.append("_").append(t.getAlias()).append(";").append(t.getType()).append(";").append(langs).
                        append(";").append(vf == null ? "null" : vf.getValue());
            }
            key.append("_").
                    append("_").append(this.getMaxResultRows()).
                    append("_").append(this.isDistinct() ? "D" : "A");

            cacheKey = key.toString();
            return cacheKey;
        } catch (Throwable t) {
            System.err.println(t.getMessage());
            t.printStackTrace();
            throw new SqlParserException("ex.sqlSearch.unbableToBuildCachekey", t);
        }
    }

    /**
     * Sets the distinct condition of the statement.
     *
     * @param value the condition
     */
    protected void setDistinct(boolean value) {
        this.distinct = value;
    }

    /**
     * Returns if the statements resultset is distinct.
     *
     * @return true if the statements resultset is distinct
     */
    public boolean isDistinct() {
        return this.distinct;
    }

    /**
     * Helper function for computeCacheKey (recursion).
     *
     * @param sb the string buffer to write to
     * @param br the current brace
     */
    private void computeCacheKey(StringBuffer sb, Brace br) {
        String brType = (br.getType() == null ? "N" : (br.getType().equalsIgnoreCase("and") ? "A" : "O"));
        sb.append("(").append(brType);
        for (BraceElement be : br.getElements()) {
            if (be instanceof Condition) {
                sb.append((" " + be.toString()));
            } else {
                computeCacheKey(sb, (Brace) be);
            }
        }
        sb.append(")");
    }


    private void removeWholeBrace(Brace br, boolean alwaysTrue) {
        try {
            if (debug) System.out.println("Removing whole brace because of always " + alwaysTrue + " cond");
        } catch (Exception exc) {
            System.err.println("###Y" + exc.getMessage());
        }

        if (br.getParent() == null) {
            br.removeAllElements();
            rootBrace = null;
            type = alwaysTrue ? Type.ALL : Type.EMPTY;
        } else {
            Brace parent = br.getParent();
            try {
                Condition cd = new Condition(this, new Constant("1"), Condition.Comparator.EQUAL, new Constant(alwaysTrue ? "1" : "0"));
                parent.addElement(cd);
            } catch (Exception exc) {
                System.err.println("###Y" + exc.getMessage());
            }
            parent.removeElement(br);
        }
    }


    /**
     * Perform an cleanup on the statement.
     *
     * @param br the brace to work on
     * @throws SqlParserException if the function fails
     */
    private void cleanup(Brace br) throws SqlParserException {

        // Move down the brace tree (recursive) ..
        for (BraceElement be : br.getElements()) {
            if (be instanceof Brace) {
                cleanup((Brace) be);
            }
        }

        // ... now start cleanup from bottom up
        if (br.isOr()) {
            for (BraceElement be : br.getElements()) {
                if (!(be instanceof Condition)) continue;
                Condition cond = (Condition) be;
                if (cond.isAlwaysTrue()) {
                    // Whole brace will be true, remove it and terminate loop
                    removeWholeBrace(br, true);
                    break;
                }
                if (cond.isAlwaysFalse()) {
                    // Condition always false, so remove it
                    br.removeElement(cond);
                }
            }
            // If the whole OR is empty we need to add an ALWAYS false to the parent
            if (br.size() == 0 && br.getParent() != null) {
                removeWholeBrace(br, false);
            }

        } else if (br.isAnd()) {
            for (BraceElement be : br.getElements()) {
                if (!(be instanceof Condition)) continue;
                Condition cond = (Condition) be;
                if (cond.isAlwaysTrue()) {
                    // Condition always true, so remove it
                    br.removeElement(cond);
                }
                if (cond.isAlwaysFalse()) {
                    // Whole brace will be false, remove it and terminate loop
                    removeWholeBrace(br, false);
                    break;
                }
            }
            // If the whole AND is empty we need to add an ALWAYS true to the parent
            if (br.size() == 0 && br.getParent() != null) {
                removeWholeBrace(br, true);
            }
        } else if (br.getSize() > 0) {
            // Just one single condition is set, examine it!
            BraceElement be = br.getElementAt(0);
            if (be instanceof Condition) {
                Condition cond = (Condition) be;
                if (cond.isAlwaysTrue()) {
                    // Everything is selected!
                    br.removeAllElements();
                    this.rootBrace = null;
                    this.type = Type.ALL;
                } else if (cond.isAlwaysFalse()) {
                    // No result at all!
                    br.removeAllElements();
                    this.rootBrace = null;
                    this.type = Type.EMPTY;
                }
            }
        }

        // Cleanup empty braces, and braces that contain just one element
        if (br.size() == 1) {
            Brace parent = br.getParent();
            if (parent != null) {
                BraceElement be = br.removeLastElement();
                if (debug)
                    System.out.println("Moving element to parent since its the only one left in the brace: " + be);
                parent.removeElement(br);
                parent.addElement(be);
            } else {
                // Only one brace in root brace -> make it the root brace
                if (br.getElementAt(0) instanceof Brace) {
                    rootBrace = (Brace) br.removeLastElement();
                }
            }
        } else if (br.size() == 0 && br.getParent() != null) {
            if (debug) System.out.println("Removing empty brace: " + br);
            br.getParent().removeElement(br);
        }
    }

    /**
     * Generates a debug string.
     *
     * @return a debug string of the statement
     * @throws com.flexive.shared.exceptions.FxSqlSearchException
     *          if the function failed
     */
    public String printDebug() throws FxSqlSearchException {
        try {
            StringBuffer result = new StringBuffer(1024);
            if (this.rootBrace == null) {
                return String.valueOf(this.getType());
            } else {
                printDebug(result, this.rootBrace);
                result = result.deleteCharAt(0);
            }
            result.append(("\n##################################################\n"));
            int pos = 0;
            for (SelectedValue v : getSelectedValues()) {
                result.append(("Selected[" + (pos++) + "]: " + v.toString() + "\n"));
            }

            pos = 0;
            result.append("Order by: ");
            for (Value v : getOrderByValues()) {
                if ((pos++) > 0) result.append(",");
                result.append(v.toString());
            }
            if (pos == 0) {
                result.append(("Order: n/a\n"));
            }
            result.append("\n");

            result.append("Search Languages: ");
            pos = 0;
            for (String v : getTableByType(Table.TYPE.CONTENT).getSearchLanguages()) {
                if ((pos++) > 0) result.append(",");
                result.append(v);
            }
            result.append("\n");
            result.append("Cache Key: ");
            try {
                result.append(getCacheKey());
            } catch (Throwable t) {
                result.append(t.getMessage());
            }
            result.append("\n");
            result.append(("Parser execution time: " + this.getParserExecutionTime() + " ms\n"));
            return result.toString();
        } catch (Throwable t) {
            throw new FxSqlSearchException(LOG, t, "ex.sqlSearch.printDebugFailed");
        }
    }

    /**
     * Generates a debug string for the statement.
     *
     * @param sb the StringBuffer to write to
     * @param br the current bracer (recursion)
     */
    protected void printDebug(StringBuffer sb, Brace br) {
        String sPrefix = "\n";
        for (int i = 0; i < br.getLevel(); i++) {
            sPrefix += "+";
        }
        sPrefix += (br.getType() == null ? "N" : (br.getType().equalsIgnoreCase("and") ? "A" : "O")) + "  ";
        sb.append((sPrefix + "("));
        boolean hadSubs = false;
        for (BraceElement be : br.getElements()) {
            if (be instanceof Condition) {
                sb.append((" " + be.toString() + " "));
            } else {
                printDebug(sb, (Brace) be);
                hadSubs = true;
            }
        }
        if (hadSubs) {
            sb.append((sPrefix + ")"));
        } else {
            sb.append(" )");
        }
    }

    // Private section

    /**
     * Cleanup the query string.
     * <p/>
     * This function removes comments from the query.
     *
     * @param query the query to cleanup
     * @return the processed query string
     * @throws SqlParserException ifthe function fails
     */
    private static String cleanupQueryString(String query) throws SqlParserException {
        // Make sure the statement has the EOF charater at the end, also add one
        // '\n' to ensure that the last line comment ("--") is closed
        query = query.trim();
        query += "\n";
        if (query.charAt(query.length() - 1) != ';') query += ";";
        return query;
    }
}
