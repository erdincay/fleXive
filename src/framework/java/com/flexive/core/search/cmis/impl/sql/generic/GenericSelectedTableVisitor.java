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

import com.flexive.core.search.cmis.impl.sql.SelectedTableVisitor;
import com.flexive.core.search.cmis.impl.sql.SqlDialect;
import com.flexive.core.search.cmis.model.ColumnReference;
import com.flexive.core.search.cmis.model.JoinedTableReference;
import com.flexive.core.search.cmis.model.SingleTableReference;
import com.flexive.core.search.cmis.model.TableReference;
import com.flexive.shared.exceptions.FxCmisQueryException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;

/**
 * Visitor to build the SELECT, FROM and WHERE clause to select all tables for a CMIS table or
 * CMIS join. The visitor accumulates the SQL fragments in internal lists and can be executed on several
 * tables.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class GenericSelectedTableVisitor implements SelectedTableVisitor {
    private static final Log LOG = LogFactory.getLog(GenericSelectedTableVisitor.class);

    protected final String tableAliasPrefix;
    protected final Map<TableReference, String> tableAliases = new HashMap<TableReference, String>();
    protected final List<String> conditions = new ArrayList<String>();
    protected final List<String> typeFilterConditions = new ArrayList<String>();
    // the columns used for joining
    protected final Map<TableReference, ColumnReference> joinColumns = new HashMap<TableReference, ColumnReference>();
    protected final SqlDialect sqlDialect;
    protected boolean usesJoins = false;

    public GenericSelectedTableVisitor(GenericSqlDialect sqlDialect, String tableAliasPrefix) {
        this.sqlDialect = sqlDialect;
        this.tableAliasPrefix = tableAliasPrefix;
    }

    /** {@inheritDoc} */
    public String getSelect() {
        final List<String> result = new ArrayList<String>(tableAliases.size());
        for (Map.Entry<TableReference, String> entry : tableAliases.entrySet()) {
            result.add(selectTableAlias(entry.getKey(), entry.getValue()));
        }
        return StringUtils.join(result, ", ");
    }

    /** {@inheritDoc} */
    public String getSelectForSingleTable(TableReference selectOnlyFrom) {
        final List<String> result = new ArrayList<String>(tableAliases.size());
        for (Map.Entry<TableReference, String> entry : tableAliases.entrySet()) {
            if (entry.getKey().equals(selectOnlyFrom)) {
                result.add(selectTableAlias(entry.getKey(), entry.getValue()));
            } else {
                // select null placeholders to create uniform result sets
                result.add(
                        sqlDialect.getEmptyId() + " AS " + entry.getKey().getIdFilterColumn() + ", "
                                + sqlDialect.getEmptyVersion() + " AS " + entry.getKey().getVersionFilterColumn()
                );
            }
        }
        return StringUtils.join(result, ", ");
    }

    /** {@inheritDoc} */
    public String getFrom() {
        if (!usesJoins && tableAliases.size() == 1) {
            // only a single table - can select from the main content table to speed up security checks
            return sqlDialect.fromContentTable(tableAliases.values().iterator().next());
        }
        final List<String> result = new ArrayList<String>(tableAliases.size());
        for (Map.Entry<TableReference, String> entry : tableAliases.entrySet()) {
            if (joinColumns.containsKey(entry.getKey())) {
                // table used in a JOIN
                result.add(
                        // selected table is determined by the column reference used for the join
                        joinColumns.get(entry.getKey()).getPropertyEntry().getTableName()
                        // add alias
                        + " " + entry.getValue()
                );
            } else {
                result.add(sqlDialect.fromContentTable(entry.getValue()));
            }
        }
        return StringUtils.join(result, ", ");
    }

    /** {@inheritDoc} */
    public List<String> getConditions() {
        return Collections.unmodifiableList(conditions);
    }

    /** {@inheritDoc} */
    public Map<TableReference, String> getTableAliases() {
        return Collections.unmodifiableMap(tableAliases);
    }

    /** {@inheritDoc} */
    public String getTableAliasPrefix() {
        return tableAliasPrefix;
    }

    /** {@inheritDoc} */
    public String getTableAlias(TableReference table) {
        if (!tableAliases.containsKey(table)) {
            throw new IllegalArgumentException("Unknown table reference: " + table);
        }
        return tableAliases.get(table);
    }

    /** {@inheritDoc} */
    public String outerJoin(String tableAlias, SelectedTableVisitor subtables) {
        assert tableAliases.size() == subtables.getTableAliases().size();
        final List<String> result = new ArrayList<String>(tableAliases.size());
        for (Map.Entry<TableReference, String> entry : tableAliases.entrySet()) {
            // "other" table already is a joined view, thus we need to use the proper column aliases
            final String alias = entry.getValue();
            final TableReference table = entry.getKey();
            result.add(
                    "(" + tableAlias + "." + table.getIdFilterColumn() + " IS NULL OR "
                            + "(" + alias + ".id = " + tableAlias + "." + table.getIdFilterColumn()
                            + " AND " + alias + ".ver = " + tableAlias + "." + table.getVersionFilterColumn()
                            + "))"
            );
        }
        // only one id/ver will be matched, since conditions are always specified for
        // single tables
        return "(" + StringUtils.join(result, " AND ") + ")";
    }

    /** {@inheritDoc} */
    public void visit(SingleTableReference singleTable) {
        final String alias = tableAliasPrefix + singleTable.getAlias();
        tableAliases.put(singleTable, alias);

        // add security filter for this table
        conditions.add(
                sqlDialect.getSecurityFilter(singleTable, alias, !usesJoins)
        );
        // add version filter
        conditions.add(
                sqlDialect.getVersionFilter(alias)
        );
        // limit table by content type
        typeFilterConditions.add(
                sqlDialect.getTypeFilter(alias + ".tdef", singleTable.getReferencedTypes())
        );
    }

    /** {@inheritDoc} */
    public void visit(JoinedTableReference joinedTable) {
        usesJoins = true;
        final ColumnReference column1 = joinedTable.getFirstTableColumn();
        final ColumnReference column2 = joinedTable.getSecondTableColumn();

        // add join assignment constraints
        addColumnAssignmentConstraint(column1);
        addColumnAssignmentConstraint(column2);

        // add join condition
        conditions.add(
                joinTableColumn(column1) + " = " + joinTableColumn(column2)
        );

        if (hasFilterTable(column1) && hasFilterTable(column2)) {
            // remember join columns
            joinColumns.put(joinedTable.getFirstTable(), column1);
            joinColumns.put(joinedTable.getSecondTable(), column2);
        } else {
            throw new FxCmisQueryException(LOG, "ex.cmis.search.query.join.table",
                    hasFilterTable(column1)
                            ? column2.getAlias()
                            : column1.getAlias()
            ).asRuntimeException();
        }
    }

    protected boolean hasFilterTable(ColumnReference column) {
        return column.getPropertyEntry().getTableType() != null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUsesJoins() {
        return usesJoins;
    }

    /**
     * Adds an assignment constraint to {@code conditions} for the assignments referenced by the given column.
     *
     * @param column    the column reference
     */
    protected void addColumnAssignmentConstraint(ColumnReference column) {
        final String assignmentFilter = sqlDialect.getAssignmentFilter(
                column.getFilterTableType(),
                tableAliasPrefix + column.getTableReference().getAlias(),
                false, column.getReferencedAssignments()
        );
        if (StringUtils.isNotBlank(assignmentFilter)) {
            conditions.add(assignmentFilter);
        }
    }

    /**
     * Return the (id, version) select columns for the given table and alias.
     *
     * @param table the table reference, which is used to determine the unique column aliases
     * @param alias the table alias
     * @return  the (id, version) select columns for the given table and alias.
     */
    protected String selectTableAlias(TableReference table, String alias) {
        return alias + ".id AS " + table.getIdFilterColumn() + ", "
                + alias + ".ver AS " + table.getVersionFilterColumn();
    }

    /**
     * Returns the SQL to return the column value that is used for joining this table.
     *
     * @param column    the column to be used for joining
     * @return  the SQL to return the column value that is used for joining this table.
     */
    protected String joinTableColumn(ColumnReference column) {
        return column.getConditionColumnMapper(sqlDialect.getSqlMapperFactory())
                .getConditionColumn(sqlDialect.getSqlMapperFactory(), column,
                        tableAliasPrefix + column.getTableReference().getAlias());
    }

}
