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
package com.flexive.core.search.cmis.impl.sql.generic;

import com.flexive.core.search.PropertyResolver;
import com.flexive.core.search.SearchUtils;
import com.flexive.core.search.cmis.impl.*;
import com.flexive.core.search.cmis.impl.sql.*;
import com.flexive.core.search.cmis.impl.sql.generic.mapper.condition.*;
import com.flexive.core.search.cmis.impl.sql.generic.mapper.select.*;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionColumnMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;
import com.flexive.core.search.cmis.model.*;
import com.flexive.core.DatabaseConst;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.Pair;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.cmis.search.CmisResultValue;
import com.flexive.shared.cmis.search.CmisResultColumnDefinition;
import com.flexive.shared.cmis.search.CmisResultRow;
import com.flexive.shared.cmis.search.*;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.ACLPermission;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.Workflow;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class GenericSqlDialect implements SqlMapperFactory, SqlDialect {
    /**
     * Alias of the filter table from which the result values will be selected.
     */
    public static final String FILTER_ALIAS = "filter";
    /**
     * Stores the feature set of the generic SQL implementation.
     */
    private static final Capabilities CAPABILITIES = new Capabilities() {
        public boolean supportsFulltextScoring() {
            return false;
        }

        public boolean normalizedFulltextScore() {
            return false;
        }

        public boolean supportsPaging() {
            return false;
        }
    };
    /**
     * The maximum rows returned by a subquery.
     * Deliberately set to -1 by default to guarantee correct results - if the query gets too
     * slow, it will timeout anyway.
     */
    private static final int SUBQUERY_LIMIT = -1;

    protected final CmisSqlQuery query;
    protected final ContentEngine contentEngine;
    protected final FxEnvironment environment;
    protected final boolean returnPrimitives;
    protected final Map<FxPK, FxContent> contents = new HashMap<FxPK, FxContent>();   // cache contents during postprocessing
    protected final Map<ResultColumn, Boolean> propertyPermissionsEnabled = new HashMap<ResultColumn, Boolean>();
    protected final Map<String, Boolean> xpathAllowedByPropertyPermissions = new HashMap<String, Boolean>();

    public GenericSqlDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives) {
        this.query = query;
        this.contentEngine = contentEngine;
        this.environment = environment;
        this.returnPrimitives = returnPrimitives;
    }

    /**
     * {@inheritDoc}
     */
    public String getSql() {
        final StringBuilder out = new StringBuilder();

        // select final result values from filtered tables
        out.append("SELECT ");
        buildSelectedColumns(out);

        // select from subquery
        out.append("\nFROM\n").append('(');
        buildFilterSelect(out);
        out.append(") ").append(FILTER_ALIAS);

        buildOrderBy(out);

        return limitQuery(out.toString());
    }

    /**
     * Add query row limits (paging) to the final SQL query.
     *
     * @param sql the final SQL query
     * @return the query with paging limits.
     *         When query limits were applied {@link com.flexive.core.search.cmis.impl.sql.Capabilities#supportsPaging()}
     *         must return true.
     */
    protected String limitQuery(String sql) {
        // nothing to do, default dialect does not support SQL query paging
        return sql;
    }

    /**
     * {@inheritDoc}
     */
    public void prepareConnection(Connection con) throws SQLException {
        // nothing to do
    }

    /**
     * {@inheritDoc}
     */
    public CmisResultSet processResultSet(ResultSet rs) throws SQLException {
        final List<CmisResultColumnDefinition> columnAliases = Lists.newArrayListWithCapacity(query.getUserColumns().size());
        for (ResultColumn column : query.getUserColumns()) {
            if (column.getSelectedObject() instanceof ColumnReference) {
                final List<FxPropertyAssignment> assignments = ((ColumnReference) column.getSelectedObject()).getReferencedAssignments();
                columnAliases.add(
                        new CmisResultColumnDefinition(
                                column.getAlias(),
                                assignments.isEmpty() ? -1 : assignments.get(0).getId()
                        )
                );

            } else {
                columnAliases.add(
                        new CmisResultColumnDefinition(
                                column.getAlias(), -1
                        )
                );
            }
        }
        final CmisResultSet result = new CmisResultSet(columnAliases);

        // process result set, keep rows outside resultset for further processing
        final List<CmisResultRow> rows = new ArrayList<CmisResultRow>();
        if (!getCapabilities().supportsPaging()) {
            // manually loop to first result row
            int index = 0;
            while (index++ < query.getStartRow()) {
                if (!rs.next()) {
                    break;
                }
            }
        }
        while (rs.next()) {
            final CmisResultRow row = result.newRow();
            int column = 1;
            for (ResultColumn selectedColumn : query.getUserColumns()) {
                row.setValue(column++,
                        selectedColumn.decodeResultValue(
                                this, rs, query.getResultLanguageId()
                        )
                );
            }
            rows.add(row);
            if (!getCapabilities().supportsPaging() && rows.size() >= query.getMaxRows()) {
                break;
            }
        }

        // apply operations that span multiple rows
        normalizeScoreColumns(rows);
        resolveMultivaluedProperties(rows);

        // populate result set
        for (CmisResultRow row : rows) {
            result.addRow(row);
        }
        return result.freeze();
    }

    /**
     * <p>
     * Normalize any SCORE() columns in this column set. This method is called after all rows
     * have been retrieved if the SQL dialect returns false in
     * {@link com.flexive.core.search.cmis.impl.sql.Capabilities#normalizedFulltextScore()}.
     * </p>
     * <p>
     * Note that this "client-side" normalization will lead to wrong scores when used with paging.
     * In this case, the maximum score of the selected row range will always be 1, although
     * a global sort by row scores can still be applied.
     * </p>
     *
     * @param rows the rows of the current result set.
     */
    protected void normalizeScoreColumns(List<CmisResultRow> rows) {
        if (getCapabilities().normalizedFulltextScore()) {
            return;
        }
        for (int scoreColumnIndex : query.getScoreColumnIndices()) {
            // result contains a score, but values are not normalized - normalize by maximum value
            double max = 1.0;
            for (CmisResultRow row : rows) {
                final double score = row.getColumn(scoreColumnIndex).getDouble();
                if (score > max) {
                    max = score;
                }
            }
            for (CmisResultRow row : rows) {
                // scale score to [0, 1], use Math.min to avoid FP wobbles and use "1.0" as maximum value
                row.setValue(scoreColumnIndex, Math.min(1.0, row.getColumn(scoreColumnIndex).getDouble() / max));
            }
        }
    }

    /**
     * Resolve multivalued properties in the result set that have not been returned by the SQL query.
     * <p>
     * Depending on the DBMS, some (or all) multivalued properties cannot be selected into a single
     * column value, thus it is necessary to populate them after the result set was created.
     * </p>
     *
     * @param rows the result set rows
     */
    @SuppressWarnings({"unchecked"})
    protected void resolveMultivaluedProperties(List<CmisResultRow> rows) {
        final ResultColumnMapper<ResultColumnReference> columnMapper = selectColumnReference();
        final List<Pair<ResultColumn, Integer>> mvColumns = getMultivaluedForPostProcessing(columnMapper);
        for (CmisResultRow row : rows) {
            for (Pair<ResultColumn, Integer> column : mvColumns) {
                final Integer index = column.getSecond();
                final CmisResultValue value = row.getColumn(index);
                if (!(value.getValue() instanceof FxPK)) {
                    throw new IllegalArgumentException("Search must return FxPK for multivalued properties that " +
                            "cannot be selected directly.");
                }
                row.setValue(index,
                        resolveMultivaluedProperty((ColumnReference) column.getFirst().getSelectedObject(),
                                (CmisResultValue<FxPK>) value)
                );
            }
        }
    }

    /**
     * Return all result columns and their indices that are multivalued properties and require postprocessing.
     *
     * @param columnMapper the column mapper of the dialect
     * @return all result columns and their indices that are multivalued properties and require postprocessing.
     */
    private List<Pair<ResultColumn, Integer>> getMultivaluedForPostProcessing(ResultColumnMapper<ResultColumnReference> columnMapper) {
        final List<Pair<ResultColumn, Integer>> mvColumns = new ArrayList<Pair<ResultColumn, Integer>>();
        int index = 0;
        for (ResultColumn column : query.getUserColumns()) {
            index++;
            if (!(column instanceof ResultColumnReference)) {
                continue;
            }
            final ColumnReference col = (ColumnReference) column.getSelectedObject();
            if (col.isMultivalued()
                    && !columnMapper.isDirectSelectForMultivalued(this, (ResultColumnReference) column, col.getPropertyEntry().getProperty().getDataType())) {
                // value not selected, must process manually
                mvColumns.add(new Pair<ResultColumn, Integer>(column, index));
            }
        }
        return mvColumns;
    }

    /**
     * Resolve the multivalued property for the given column.
     *
     * @param column           the selected column
     * @param selectedInstance the PK returned by the query. Use this PK to get the multivalued property values from.
     * @return the multivalued property for the given column.
     */
    protected List<Object> resolveMultivaluedProperty(ColumnReference column, CmisResultValue<FxPK> selectedInstance) {
        // very generic lookup of multivalued properties through the content engine - slow but reliable

        // get FxContent instance
        final FxPK pk = selectedInstance.getValue();
        if (!contents.containsKey(pk)) {
            try {
                contents.put(pk, contentEngine.load(pk));
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }
        final FxContent content = contents.get(pk);

        // accumulate queries
        if (column.getBaseAssignment() == null) {
            throw new IllegalStateException("A column without a baseAssignment cannot be multivalued.");
        }
        final String xpath = "/" + column.getBaseAssignment().getAlias();
        int index = 0;
        final List<Object> result = new ArrayList<Object>();
        while (content.containsValue(xpath + "[" + (++index) + "]")) {
            final FxValue value = content.getValue(xpath + "[" + index + "]");
            result.add(
                    isReturnPrimitives()
                            ? value.getBestTranslation()
                            : value
            );
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Capabilities getCapabilities() {
        return CAPABILITIES;
    }

    /**
     * Select the final result values for the CMIS "SELECT" clause from the filter table
     * that can be referenced with {@link #FILTER_ALIAS}.
     *
     * @param out the output builder
     */
    protected void buildSelectedColumns(StringBuilder out) {
        final ColumnIndex index = new ColumnIndex();
        final List<String> columnsSql = new ArrayList<String>(query.getResultColumns().size());
        for (ResultColumn column : query.getResultColumns()) {
            columnsSql.add(
                    column.selectSql(this, query, query.getResultLanguageId(), false, true, index)
            );
        }
        out.append(StringUtils.join(columnsSql, ",\n"));
    }

    /**
     * Build a SELECT statement that selects all IDs and Versions from the selected tables,
     * applying the conditions of the query.
     * <p/>
     * <p>
     * One row from this SELECT will result in one row of the final result set. Thus this statement
     * must perform the JOINs of the CMIS SQL query: a table in CMIS SQL is always mapped on
     * FX_CONTENT_DATA. The join condition is then used to join two FX_CONTENT_DATA tables
     * to themselves, resulting in the ID tuples representing the actual join result.
     * </p>
     *
     * @param out the output builder
     */
    protected void buildFilterSelect(StringBuilder out) {

        // Determine our "main table". This is the first selected table, and all
        // joined subtables will be linked to this table
        final TableReference mainTable = query.getStatement().getTables().get(0);
        final String mainTableAlias = mainTable.getReferencedAliases().get(0);

        // select one instance of the content table for each table selected in the CMIS query
        if (isSelectAll()) {
            // no JOIN conditions and no conditions - select from main content table and
            // add a type filter, otherwise all instances would be returned
            buildTypeFilter(out, mainTable, mainTableAlias);
        } else {
            buildConditionFilter(out);
        }
    }

    /**
     * Returns true if the query selects all contents from a <strong>single</strong> table
     * without conditions.
     *
     * @return true if the query selects all contents from a <strong>single</strong> table
     *         without conditions.
     */
    protected final boolean isSelectAll() {
        final Statement stmt = query.getStatement();
        return stmt.getRootCondition() == null
                && stmt.getTables().size() == 1
                && stmt.getTables().get(0).getReferencedAliases().size() == 1;
    }


    /**
     * Build a filter statement that returns all instances of the selected table type(s) in the CMIS query.
     * This cannot be used with more than one table, since this would imply a join.
     *
     * @param out        the output writer where the SELECT should be appended
     * @param table      the table reference that determines the type(s) to be selected
     * @param tableAlias the desired table alias
     */
    protected void buildTypeFilter(StringBuilder out, TableReference table, String tableAlias) {
        out.append("SELECT DISTINCT ")
                // select ID
                .append(tableAlias).append(".id AS ").append(table.getIdFilterColumn()).append(", ")

                // select version
                .append(tableAlias).append(".ver AS ").append(table.getVersionFilterColumn()).append(" FROM ")

                // from FX_CONTENT
                .append(fromContentTable(tableAlias)).append(" WHERE ")

                // add type filter
                .append(getTypeFilter(tableAlias + ".tdef", query.getStatement().getReferencedTypes())).append(" AND ")

                // add security filter
                .append(getSecurityFilter(table, tableAlias, true)).append(" AND ")

                // add version filter
                .append(getVersionFilter(tableAlias));
    }

    /**
     * Build a filter statement that filters the instances using the conditions of the CMIS query.
     *
     * @param out the output stream where the SELECT query should be written
     */
    protected void buildConditionFilter(StringBuilder out) {
        final SelectedTableVisitor joins = joinTables("");

        // select (id, version) for every joined table
        out.append("SELECT DISTINCT ").append(joins.getSelect());
        out.append("\nFROM\n").append(joins.getFrom());

        // build conditions for the "WHERE" clause
        final List<String> conditions = new ArrayList<String>();

        // add conditions for joining the selected tables
        conditions.addAll(joins.getConditions());

        if (query.getStatement().getRootCondition() != null) {
            // got query conditions, select from detail tables
            out.append(',');
            // build condition table, add join conditions
            conditions.add(joinConditionsTable(out, joins));
        }

        if (!conditions.isEmpty()) {
            out.append(" WHERE ").append(StringUtils.join(conditions, " AND "));
        }
    }

    /**
     * Create a join object for all selected tables of the query, where every table will have the given
     * prefix for its alias (to enforce unique table aliases in subqueries).
     *
     * @param tableAliasPrefix the table alias prefix
     * @return a join object for all selected tables of the query
     */
    protected SelectedTableVisitor joinTables(String tableAliasPrefix) {
        final SelectedTableVisitor joins = createSelectedTableVisitor(tableAliasPrefix);
        for (TableReference tableReference : query.getStatement().getTables()) {
            tableReference.accept(joins);
        }
        return joins;
    }

    /**
     * {@inheritDoc}
     */
    public String fromContentTable(String alias) {
        return PropertyResolver.Table.T_CONTENT.getTableName() + ' ' + alias;
    }

    /**
     * {@inheritDoc}
     */
    public String getTypeFilter(String column, Collection<FxType> types) {
        return column + " IN (" +
                StringUtils.join(
                        FxSharedUtils.getSelectableObjectIdList(types),
                        ","
                ) + ")";
    }

    /**
     * Render the conditions table representing the query conditions. The condition table will be appended to
     * {@code out}, and the conditions used for joining the condition table with the main table(s) will
     * be returned.
     *
     * @param out          output writer for the conditions table
     * @param joinedTables the main tables, against which the conditions table will be joined
     * @return the conditions for joining the resulting conditions table with the tables in {@code joinedTables}.
     */
    protected String joinConditionsTable(StringBuilder out, SelectedTableVisitor joinedTables) {
        final SelectedTableVisitor subTables = joinTables("sub");
        // add constraints based on the CMIS query conditions
        final GenericConditionTableBuilder visitor = createConditionNodeVisitor(out, subTables);
        query.getStatement().getRootCondition().accept(visitor);
        out.append(" __conditions");

        return joinedTables.outerJoin("__conditions", subTables);
    }

    /**
     * {@inheritDoc}
     */
    public String getSecurityFilter(TableReference table, String tableAlias, boolean contentTable) {
        final UserTicket ticket = FxContext.getUserTicket();
        if (ticket.isGlobalSupervisor()) {
            return "1=1";
        }
        if (!contentTable) {
            // not selected from content table, main table properties for optimized security filter are not available
            return "mayReadInstance2(" + tableAlias + ".id," + tableAlias + ".ver," + ticket.getUserId() + ","
                    + ticket.getMandatorId() + "," + ticket.isMandatorSupervisor() + "," + ticket.isGlobalSupervisor() + ")";
        }
        // create security filters per type - the conditions will be joined with "OR"
        // property permissions are not handled by this filter, this is the responsibility of the
        // methods that return data from selected properties
        final List<String> conditions = new ArrayList<String>();
        if (ticket.isMandatorSupervisor()) {
            conditions.add(tableAlias + ".mandator=" + ticket.getMandatorId());
        }
        for (FxType type : table.getReferencedTypes()) {
            final String tdef = tableAlias + ".tdef=" + type.getId();
            if (!type.isUsePermissions()) {
                // no filtering at all needed for this type
                conditions.add(tdef);
                continue;
            }
            final List<String> typeFilter = new ArrayList<String>();
            typeFilter.add(tdef);
            if (type.isUseTypePermissions() && !ticket.mayReadACL(type.getACL().getId(), -1)) {
                if (!ticket.mayReadACL(type.getACL().getId(), ticket.getUserId())) {
                    // neither general nor private read permissions - skip all instances of this type
                    continue;
                }
                // ACL cannot be read, but private permissions set - select only own instances
                typeFilter.add(tableAlias + ".created_by=" + ticket.getUserId());
            }
            if (type.isUseInstancePermissions()) {
                // collect all readable instance ACLs
                final List<Long> readable = new ArrayList<Long>(
                        Arrays.asList(ticket.getACLsId(-1, ACLCategory.INSTANCE, ACLPermission.READ))
                );
                // collect all ACLs that can be read if the calling user is the owner
                final List<Long> privateReadable = new ArrayList<Long>(
                        Arrays.asList(ticket.getACLsId(ticket.getUserId(), ACLCategory.INSTANCE, ACLPermission.READ))
                );
                // remove all ACLs that are readable regardless of the owner
                privateReadable.removeAll(readable);
                // add ACL filter
                typeFilter.add(
                        contentFilterWithPrivate(ticket, tableAlias,
                                contentAclFilter(tableAlias, readable),
                                contentAclFilter(tableAlias, privateReadable)
                        )
                );
            }
            if (type.isUseStepPermissions()) {
                // collect all readable workflow steps
                final List<Long> readable = new ArrayList<Long>();
                final List<Long> privateReadable = new ArrayList<Long>();
                for (Workflow workflow : environment.getWorkflows()) {
                    for (Step step : workflow.getSteps()) {
                        if (ticket.mayReadACL(step.getAclId(), -1)) {
                            // readable ACL
                            readable.add(step.getId());
                        } else if (ticket.mayReadACL(step.getAclId(), ticket.getUserId())) {
                            // ACL readable only when the calling user is the owner
                            privateReadable.add(step.getAclId());
                        }
                    }
                }
                // add step filter
                typeFilter.add(
                        contentFilterWithPrivate(ticket, tableAlias, readable, privateReadable, "step")
                );
            }
            conditions.add("(" + StringUtils.join(typeFilter, " AND ") + ")");
        }
        return conditions.isEmpty() ? "1=0" : "(" + StringUtils.join(conditions, " OR ") + ")";
    }

    protected String contentAclFilter(String contentTableAlias, List<Long> acls) {
        return acls.isEmpty() ? null : "EXISTS(" +
                contentAclFilter(contentTableAlias, DatabaseConst.TBL_CONTENT, acls) +
                " UNION " +
                contentAclFilter(contentTableAlias, DatabaseConst.TBL_CONTENT_ACLS, acls)
                + ")";
    }

    protected String contentAclFilter(String contentTableAlias, String table, List<Long> acls) {
        return "SELECT c.acl FROM " + table + " c WHERE c.id=" + contentTableAlias + ".id AND c.ver=" + contentTableAlias + ".ver " +
                " AND c.acl IN (" + StringUtils.join(acls, ',') + ")";
    }

    protected String contentFilterWithPrivate(UserTicket ticket, String tableAlias, List<Long> readableIds, List<Long> privateReadableIds, String column) {
        return contentFilterWithPrivate(
                ticket,
                tableAlias,
                tableAlias + "." + column + " IN (" + StringUtils.join(readableIds, ',') + ")",
                privateReadableIds.isEmpty() ? null :
                        tableAlias + "." + column + " IN (" + StringUtils.join(privateReadableIds, ',') + ")"
        );
    }

    private String contentFilterWithPrivate(UserTicket ticket, String contentTableAlias, String readableFilter, String privateReadableFilter) {
        final String result;
        if (isBlank(privateReadableFilter)) {
            // no private permissions have to be checked
            result = readableFilter;
        } else {
            // must match readable column OR owner and private readable column
            result = "(" + readableFilter + " OR ("
                    + contentTableAlias + ".created_by=" + ticket.getUserId() + " AND "
                    + privateReadableFilter
                    + "))";
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getAssignmentFilter(PropertyResolver.Table tableType, String tableAlias, Collection<? extends FxPropertyAssignment> assignments) {
        switch (tableType) {
            case T_CONTENT:
                return "";  // no assignment filter necessary, the selected column determines the assignment
            case T_CONTENT_DATA:
                // add constraint on ASSIGN column
                if (assignments == null || assignments.isEmpty()) {
                    return "";
                } else if (assignments.size() == 1) {
                    return tableAlias + ".assign = " + assignments.iterator().next().getId();
                }
                return tableAlias + ".assign IN ("
                        + StringUtils.join(FxSharedUtils.getSelectableObjectIdList(assignments), ',') + ")";
            case T_CONTENT_DATA_FLAT:
                // the column determines the assignment (the first assignment is always the base assignment)
                return SearchUtils.getFlatStorageAssignmentFilter(environment, tableAlias, assignments.iterator().next());
            default:
                throw new IllegalArgumentException("Unsupported table type: " + tableType);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getVersionFilter(String tableAlias) {
        // TODO: how to specify the version filter? Currently this limits the query to the max version.
        return (tableAlias != null ? (tableAlias + ".") : "")
                + "ISMAX_VER=true";
    }

    /**
     * Add the ORDER BY clause to the output.
     *
     * @param out the output writer
     */
    protected void buildOrderBy(StringBuilder out) {
        final List<SortSpecification> columns = query.getStatement().getOrderByColumns();
        if (columns.isEmpty()) {
            return;
        }
        out.append("\nORDER BY ");
        final List<String> result = new ArrayList<String>(columns.size());
        for (SortSpecification column : columns) {
            result.add(
                    query.getResultColumn(column.getColumn()).getResultSetAlias()
                            + (SortDirection.DESCENDING.equals(column.getDirection()) ? " DESC" : "")
            );
        }
        out.append(StringUtils.join(result, ", "));
    }

    /**
     * {@inheritDoc}
     */
    public boolean isReturnPrimitives() {
        return returnPrimitives;
    }

    /**
     * {@inheritDoc}
     */
    public FxEnvironment getEnvironment() {
        return environment;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isPropertyPermissionsEnabled(ResultColumn<? extends ColumnReference> column) {
        if (!propertyPermissionsEnabled.containsKey(column)) {
            boolean enabled = false;
            // enable property permissions if at least one of the table types
            // and one of the assignments has property permissions enabled
            if (column.getSelectedObject().getTableReference().isPropertySecurityEnabled()) {
                for (FxPropertyAssignment assignment : column.getSelectedObject().getReferencedAssignments()) {
                    if (assignment.getACL() != null && !assignment.isSystemInternal()) {
                        enabled = true;
                        break;
                    }
                }
            }
            propertyPermissionsEnabled.put(
                    column,
                    enabled
            );
        }
        return propertyPermissionsEnabled.get(column);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isAllowedByPropertyPermissions(ResultSet rs, ResultColumn<? extends ColumnReference> column, String xpath) throws SQLException {
        final ResultColumn typeIdColumn = getTypeIdColumn(column.getSelectedObject().getTableReference());
        // add the type of the returned instance to the XPath
        final FxType type = environment.getType(rs.getLong(typeIdColumn.getColumnStart()));
        final String fullXPath;
        if (xpath.startsWith("/")) {
            // XPath was selected from FX_CONTENT_DATA, misses type prefix
            fullXPath = type.getName() + xpath;
        } else {
            // XPath already contains type information
            fullXPath = xpath;
        }

        // cache permission information by xpath
        if (!xpathAllowedByPropertyPermissions.containsKey(fullXPath)) {
            final ACL acl = environment.getPropertyAssignment(fullXPath).getACL();
            // TODO: private permissions are ignored because the owner is not known 
            xpathAllowedByPropertyPermissions.put(
                    fullXPath,
                    acl == null || FxContext.getUserTicket().mayReadACL(acl.getId(), -1)
            );
        }
        return xpathAllowedByPropertyPermissions.get(fullXPath);
    }

    /**
     * {@inheritDoc}
     */
    public ResultColumn getTypeIdColumn(TableReference tableReference) {
        final ResultColumn result = query.getTypeIdColumn(tableReference);
        if (result == null) {
            throw new IllegalArgumentException("No type ID was selected for table " + tableReference.getAlias() + ".");
        }
        return result;
    }

    /**
     * Create a new visitor for building the condition table.
     *
     * @param out          the output passed to the condition visitor
     * @param joinedTables the joined tables of the main query
     * @return a new visitor for building the condition table.
     */
    protected GenericConditionTableBuilder createConditionNodeVisitor(StringBuilder out, SelectedTableVisitor joinedTables) {
        return joinedTables.isUsesJoins()
                // cannot use basic INTERSECT-based builder for joins (see javadoc)
                ? new GenericInnerJoinConditionTableBuilder(this, out, joinedTables)
                // use fast generic UNION/INTERSECT variant for queries without joins
                : new GenericConditionTableBuilder(this, out, joinedTables);
    }

    /**
     * Create a new table visitor for representing the selected (and joined) tables of the CMIS query.
     *
     * @param tableAliasPrefix a prefix to be added to all table aliases
     * @return a new table visitor for representing the selected (and joined) tables of the CMIS query.
     */
    protected SelectedTableVisitor createSelectedTableVisitor(String tableAliasPrefix) {
        return new GenericSelectedTableVisitor(this, tableAliasPrefix);
    }

    /**
     * {@inheritDoc}
     */
    public ResultColumnMapper<ResultRowNumber> selectRowNumber() {
        return GenericRowNumber.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ResultColumnMapper<ResultColumnReference> selectColumnReference() {
        return GenericColumnReference.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ResultColumnMapper<ResultColumnReference> selectPath() {
        return GenericObjectPath.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ConditionColumnMapper<ColumnReference> filterColumnReference() {
        return GenericColumnReference.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ConditionMapper<ComparisonCondition> conditionCompare() {
        return GenericComparisonCondition.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ConditionColumnMapper<Literal> filterLiteral() {
        return GenericLiteral.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ResultColumnMapper<ResultColumnFunction> selectColumnFunction() {
        return GenericColumnFunction.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ResultColumnMapper<ResultScore> selectScore() {
        return GenericContainsCondition.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ConditionColumnMapper<ColumnValueFunction> filterColumnFunction() {
        return GenericColumnFunction.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ConditionMapper<ContainsCondition> conditionContain() {
        return GenericContainsCondition.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ConditionMapper<LikeCondition> conditionLike() {
        return GenericLikeColumnReferenceCondition.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ConditionMapper<InCondition> conditionIn() {
        return GenericInColumnReferenceCondition.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public ConditionMapper<NullCondition> conditionNull() {
        return GenericNullColumnReferenceCondition.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public SqlDialect getSqlDialect() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public SqlMapperFactory getSqlMapperFactory() {
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public String limitSubquery() {
        return SUBQUERY_LIMIT == -1 ? "" : " LIMIT " + SUBQUERY_LIMIT + " ";
    }
}
