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
package com.flexive.shared.search.query;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.search.*;
import com.flexive.shared.search.query.QueryOperatorNode.Operator;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.value.FxValue;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Query Builder for flexive SQL queries. Supports incremental adding
 * of conditions, filter and order by columns.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SqlQueryBuilder implements Serializable {
    static final long serialVersionUID = -4805627533111750389L;

    private static final String[] BASIC_SELECTS = new String[]{"@pk", "mandator",
            "created_by", "acl", "step", "*"};
    /**
     * Start column for user defined properties
     */
    public static final int COL_USERPROPS = ArrayUtils.indexOf(BASIC_SELECTS, "*");

    private final ResultLocation location;
    private ResultViewType viewType;

    private final List<String> selectColumns;
    private final List<String> filters;
    private final StringBuilder whereConditions;
    private final List<String> orderBy;
    private final Set<Table> tables;
    private final Stack<Operator> operatorStack;
    private final Stack<Integer> expressionCounter;

    private int startRow;
    private int maxRows = -1;

    private boolean includeBasicSelects;
    private boolean frozen;     // when the query is frozen, no more conditions may be added. Getting the query or the search conditions freezes the query builder.

    /**
     * Instantiates an empty query builder.
     */
    public SqlQueryBuilder() {
        this(AdminResultLocations.DEFAULT, ResultViewType.LIST);
    }

    /**
     * Instantiates an empty query builder for the specified result location and view type.
     *
     * @param location  the result location
     * @param viewType  the view type
     */
    public SqlQueryBuilder(ResultLocation location, ResultViewType viewType) {
        this(new ArrayList<String>(), new ArrayList<String>(), new StringBuilder(), new ArrayList<String>(),
                new HashSet<Table>(), new Stack<Operator>(), new Stack<Integer>(), true, false, location, viewType);
    }

    /**
     * Copy constructor. Creates an independent query builder based on the query builder <code>other</code>
     *
     * @param other the query builder to be copied
     */
    public SqlQueryBuilder(SqlQueryBuilder other) {
        //noinspection unchecked
        this(new ArrayList<String>(other.selectColumns), new ArrayList<String>(other.filters),
                new StringBuilder(other.whereConditions), new ArrayList<String>(other.orderBy),
                new HashSet<Table>(other.tables), (Stack<Operator>) other.operatorStack.clone(),
                (Stack<Integer>) other.expressionCounter.clone(),
                other.includeBasicSelects, other.frozen, other.location, other.viewType);
    }
    
    private SqlQueryBuilder(List<String> selectColumns, List<String> filters, StringBuilder whereConditions,
                            List<String> orderBy, Set<Table> tables,
                            Stack<Operator> operatorStack, Stack<Integer> expressionCounter,
                            boolean includeBasicSelects, boolean frozen,
                            ResultLocation location, ResultViewType viewType) {
        this.selectColumns = selectColumns;
        this.filters = filters;
        this.orderBy = orderBy;
        this.whereConditions = whereConditions;
        this.tables = tables;
        this.operatorStack = operatorStack;
        this.expressionCounter = expressionCounter;
        this.includeBasicSelects = includeBasicSelects;
        this.frozen = frozen;
        this.location = location;
        this.viewType = viewType;
    }

    /**
     * Returns the complete query generated by the builder.
     *
     * @return the SQL search query
     */
    public String getQuery() {
        final String conditions = getConditions();
        if (tables.size() == 0 && StringUtils.isNotBlank(conditions) && !"()".equals(conditions)) {
            throw new FxInvalidStateException("ex.sqlQueryBuilder.tables.empty").asRuntimeException();
        } else if (tables.size() == 0 && StringUtils.isBlank(conditions)) {
            // default query: select all content
            tables.add(Table.CONTENT);
        }
        StringBuilder query = new StringBuilder();
        // build select
        query.append("SELECT ").append(Table.CONTENT.getAlias()).append(".");
        query.append(StringUtils.join(getColumnNames().iterator(), ", " + Table.CONTENT.getAlias() + "."));
        // build from
        query.append("\nFROM ");
        for (Table table : tables) {
            query.append(table.getName()).append(' ').append(table.getAlias());
            query.append(',');
        }
        query.setCharAt(query.length() - 1, ' ');    // replace last comma
        // build filters
        if (!filters.isEmpty()) {
            query.append("\nFILTER ").append(getFilters());
        }
        if (conditions.length() > 0) {
            // build conditions
            query.append("\nWHERE ").append(conditions);
        }
        if (!orderBy.isEmpty()) {
            query.append("\nORDER BY ").append(getOrderBy());
        }
        return query.toString();
    }

    /**
     * Return the filters defined for this query (if any).
     *
     * @return  the filters defined for this query   
     */
     public String getFilters() {
         return StringUtils.join(filters.iterator(), ", ");
     }

    /**
     * Return the order by columns defined for this query.
     *
     * @return  the order by columns defined for this query.
     */
    public String getOrderBy() {
        return orderBy.isEmpty() ? "" : Table.CONTENT.getAlias() + "."
                + StringUtils.join(orderBy.iterator(), ", " + Table.CONTENT.getAlias());
    }

    /**
     * Render a condition for a common FxAssignment query.
     *
     * @param assignment the assignment
     * @param comparator the comparator to be used
     * @param value      the value to be compared against
     * @return this
     */
    public SqlQueryBuilder condition(FxAssignment assignment, PropertyValueComparator comparator,
                                        FxValue<?, ?> value) {
        renderCondition(comparator.getSql(assignment, value));
        tables.add(Table.CONTENT);
        return this;
    }

    /**
     * Render a condition for a common FxProperty query.
     *
     * @param property   the property
     * @param comparator the comparator to be used
     * @param value      the value to be compared against
     * @return this
     */
    public SqlQueryBuilder condition(FxProperty property, PropertyValueComparator comparator,
                                        FxValue<?, ?> value) {
        renderCondition(comparator.getSql(property, value));
        tables.add(Table.CONTENT);
        return this;
    }

    /**
     * Render a condition for a FxProperty query.
     *
     * @param propertyName   the property name
     * @param comparator the comparator to be used
     * @param value      the value to be compared against
     * @return this
     */
    public SqlQueryBuilder condition(String propertyName, PropertyValueComparator comparator, FxValue<?, ?> value) {
        renderCondition(comparator.getSql(Table.CONTENT.getColumnName(propertyName), value));
        tables.add(Table.CONTENT);
        return this;
    }

    /**
     * Render a condition for a FxProperty query.
     *
     * @param propertyName   the property name
     * @param comparator the comparator to be used
     * @param value      the value to be compared against
     * @return this
     */
    public SqlQueryBuilder condition(String propertyName, PropertyValueComparator comparator, Object value) {
        renderCondition(comparator.getSql(Table.CONTENT.getColumnName(propertyName), value));
        tables.add(Table.CONTENT);
        return this;
    }

    /**
     * Adds a condition that selects only objects of the given type. Note that this is different
     * to {@link #filterType(String)}, since filtering is applied on the result that also may contain
     * other types, but this is a standard search condition that restrains the search result.
     *
     * @param typeName  the type name to be selected
     * @return  this
     */
    public SqlQueryBuilder type(String typeName) {
        condition("typedef", PropertyValueComparator.EQ, CacheAdmin.getEnvironment().getType(typeName).getId());
        return this;
    }

    /**
     * Adds a condition that selects only objects of the given type. Note that this is different
     * to {@link #filterType(long)}, since filtering is applied on the result that also may contain
     * other types, but this is a standard search condition that restrains the search result.
     *
     * @param typeId the type id to be selected
     * @return  this
     */
    public SqlQueryBuilder type(long typeId) {
        condition("typedef", PropertyValueComparator.EQ, typeId);
        return this;
    }

    /**
     * Limits the (sub-)query to children of the given node.
     *
     * @param nodeId    the root node for the (sub-)query
     * @return  this
     */
    public SqlQueryBuilder isChild(long nodeId) {
        renderCondition("IS CHILD OF " + nodeId);
        tables.add(Table.CONTENT);
        return this;
    }

    /**
     * Limits the (sub-)query to the direct children of the given node.
     *
     * @param nodeId    the root node for the (sub-)query
     * @return  this
     */
    public SqlQueryBuilder isDirectChild(long nodeId) {
        renderCondition("IS DIRECT CHILD OF " + nodeId);
        tables.add(Table.CONTENT);
        return this;
    }

    /**
     * Return the conditional ("WHERE") statement(s) contained in this query. If the initial
     * subquery scope is still open, it will be closed. 
     *
     * @return the conditional statement(s)
     */
    public String getConditions() {
        if (operatorStack.size() == 1) {
            // close outmost scope automatically
            closeSub();
        }
        assertValidQuery();
        frozen = true;
        return whereConditions.toString();
    }

    /**
     * Return the selected column names.
     *
     * @return the selected column names.
     */
    public List<String> getColumnNames() {
        final List<String> names = new ArrayList<String>();
        if (includeBasicSelects) {
            names.addAll(Arrays.asList(BASIC_SELECTS));
        }
        names.addAll(selectColumns);
        return names;

    }

    /**
     * Select one or more columns.
     *
     * @param columns   columns to be selected
     * @return  this
     */
    public SqlQueryBuilder select(String... columns) {
        selectColumns.addAll(Arrays.asList(columns));
        if (columns.length > 0) {
            includeBasicSelects = false;
        }
        return this;
    }

    /**
     * Return only objects of the given briefcase.
     *
     * @param briefcaseId   the briefcase ID
     * @return  this
     */
    public SqlQueryBuilder filterBriefcase(long briefcaseId) {
        return uniqueFilter("briefcase", briefcaseId);
    }

    /**
     * Uses a content type filter for the given content type name.
     *
     * @param name  the content type name
     * @return  this
     */
    public SqlQueryBuilder filterType(String name) {
        return setTypeFilter(name);
    }

    /**
     * Uses a content type filter for the given content type.
     *
     * @param typeId the content type ID, or -1 to disable the content type filter
     * @return  this
     */
    public SqlQueryBuilder filterType(long typeId) {
        return setTypeFilter(typeId != -1 ? typeId : null);
    }

    private SqlQueryBuilder setTypeFilter(Object value) {
        final String filter = Table.CONTENT.getAlias() + ".TYPE";
        removeFilter(filter);
        if (value != null) {
            uniqueFilter(filter, value);
        }
        return this;
    }

    private SqlQueryBuilder uniqueFilter(String base, Object value) {
        assertNoFilterStartsWith(base);
        filters.add(base + "=" + value);
        return this;
    }

    private void assertNoFilterStartsWith(String substring) {
        for (String filter: filters) {
            if (filter.startsWith(substring)) {
                throw new FxInvalidParameterException("FILTER", "ex.sqlQueryBuilder.filter.unique",
                        filter, substring).asRuntimeException();
            }
        }
    }

    private SqlQueryBuilder removeFilter(String name) {
        for (Iterator<String> iterator = filters.iterator(); iterator.hasNext(); ) {
            if (iterator.next().startsWith(name + "=")) {
                iterator.remove();
                return this;
            }
        }
        return this;
    }

    /**
     * Order the results by the given column. Any previously set order by columns
     * are removed.
     *
     * @param column    the column to be sorted
     * @param direction the sort direction
     * @return  this
     */
    public SqlQueryBuilder orderBy(String column, SortDirection direction) {
        orderBy.clear();
        final int columnIndex = FxSharedUtils.getColumnIndex(getColumnNames(), column);
        if (columnIndex == -1) {
            throw new FxInvalidParameterException("column", "ex.sqlQueryBuilder.column.invalid", column).asRuntimeException();
        }
        orderBy.add(getColumnNames().get(columnIndex - 1) + direction.getSqlSuffix());
        return this;
    }

    /**
     * Order the results by the given column (1-based). Any previously set order by columns
     * are removed.
     *
     * @param columnIndex    the 1-based index of the column to be sorted
     * @param direction the sort direction
     * @return  this
     */
    public SqlQueryBuilder orderBy(int columnIndex, SortDirection direction) {
        orderBy.clear();
        orderBy.add(getColumnNames().get(columnIndex - 1) + direction.getSqlSuffix());
        return this;
    }

    /**
     * Returns true if the predefined columns are selected by the condition.
     *
     * @return  true if the predefined columns are selected by the condition.
     */
    public boolean isIncludeBasicSelects() {
        return includeBasicSelects;
    }

    /**
     * Enables or disables the default select columns. By default they are included
     * only if no select clause was specified by {@link #select(String[])}.
     *
     * @param includeBasicSelects   true if the pre-defined selects should be included
     * @return  this
     */
    public SqlQueryBuilder setIncludeBasicSelects(boolean includeBasicSelects) {
        this.includeBasicSelects = includeBasicSelects;
        return this;
    }

    /**
     * Create a new subquery scope with the given operator. Subsequent conditions will
     * be added to that scope. Not that all subscopes (except the first one) must be closed
     * before the query can be generated.
     *
     * @param operator the operator to be used inside the new scope
     * @return this
     */
    public SqlQueryBuilder enterSub(Operator operator) {
        assertNotFrozen();
        if (expressionCounter.size() > 0 && expressionCounter.peek() > 0) {
            whereConditions.append(" ").append(operatorStack.peek()).append(" ");
        }
        whereConditions.append("(");
        operatorStack.add(operator);
        expressionCounter.add(0);
        return this;
    }

    /**
     * Shorthand for {@link #enterSub(Operator)} with {@link Operator#AND} as the operator.
     * @return this
     */
    public SqlQueryBuilder andSub() {
        return enterSub(Operator.AND);
    }

    /**
     * Shorthand for {@link #enterSub(Operator)} with {@link Operator#OR} as the operator.
     * @return this
     */
    public SqlQueryBuilder orSub() {
        return enterSub(Operator.OR);
    }

    /**
     * Close a scope.
     *
     * @return this
     */
    public SqlQueryBuilder closeSub() {
        assertStackNotEmpty();
        assertNotFrozen();
        operatorStack.pop();
        expressionCounter.pop();
        if (expressionCounter.size() > 0) {
            expressionCounter.push(expressionCounter.pop() + 1);
        }
        whereConditions.append(')');
        if (operatorStack.isEmpty()) {
            // no more conditions can be rendered after closing the last scope
            frozen = true;
        }
        return this;
    }

    /**
     * Return true if this query builder is frozen and cannot be further modified.
     * A query builder freezes when the query or the query conditions are queried
     * with the {@link #getQuery()} and {@link #getConditions()} methods.
     * A frozen query builder does not guarantee, however, that the query itself is valid.
     *
     * @return  true if this query builder is frozen and cannot be further modified. 
     */
    public boolean isFrozen() {
        return frozen;
    }

    /**
     * Return the result location used in this query builder.
     *
     * @return  the result location used in this query builder.
     */
    public ResultLocation getLocation() {
        return location;
    }

    /**
     * Return the result view type used in this query builder.
     *
     * @return  the result view type used in this query builder.
     */
    public ResultViewType getViewType() {
        return viewType;
    }

    public SqlQueryBuilder setViewType(ResultViewType viewType) {
        this.viewType = viewType;
        return this;
    }

    public int getStartRow() {
        return startRow;
    }

    public SqlQueryBuilder setStartRow(int startRow) {
        this.startRow = startRow;
        return this;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public SqlQueryBuilder setMaxRows(int maxRows) {
        this.maxRows = maxRows;
        return this;
    }

    /**
     * Convenience method for executing a search query using this query builder.
     *
     * @return  the search result for the current query
     * @throws FxApplicationException   if the search failed
     */
    public FxResultSet getResult() throws FxApplicationException {
        return EJBLookup.getSearchEngine().search(getQuery(), startRow, maxRows != -1 ? maxRows : null, null, location, viewType);
    }

    /**
     * Render the given condition in the current scope.
     *
     * @param condition the condition to be rendered.
     */
    private void renderCondition(String condition) {
        if (operatorStack.isEmpty() && whereConditions.length() == 0) {
            // implicitly open first scope
            andSub();
        }
        assertStackNotEmpty();
        assertNotFrozen();
        if (expressionCounter.peek() == 0) {
            // first condition, open scope
/*            int level = expressionCounter.size() - 1;
            while (level >= 0 && expressionCounter.get(level) == 0) {
                // nested condition, add operator
                if (level > 0 && expressionCounter.get(level - 1) > 0) {
                    whereConditions.append(' ')
                            .append(operatorStack.get(level - 1).getSqlRepresentation())
                            .append(' ');
                }
                whereConditions.append('(');
                level--;
            }*/
        } else {
            // render operator between nodes
            whereConditions.append(' ').append(operatorStack.peek().getSqlRepresentation()).append(' ');
        }
        whereConditions.append(condition);
        expressionCounter.add(expressionCounter.pop() + 1);
    }

    private void assertStackNotEmpty() {
        if (operatorStack.isEmpty()) {
            throw new FxInvalidStateException("ex.sqlQueryBuilder.operatorStack.empty").asRuntimeException();
        }
        if (expressionCounter.isEmpty()) {
            throw new FxInvalidStateException("ex.sqlQueryBuilder.counterStack.empty").asRuntimeException();
        }
    }

    private void assertValidQuery() {
        if (!operatorStack.isEmpty() || !expressionCounter.isEmpty()) {
            throw new FxInvalidStateException("ex.sqlQueryBuilder.query.incomplete", operatorStack.size()).asRuntimeException();
		}
	}

    private void assertNotFrozen() {
        if (frozen) {
            throw new FxInvalidStateException("ex.sqlQueryBuilder.query.frozen").asRuntimeException();
        }
    }
}
