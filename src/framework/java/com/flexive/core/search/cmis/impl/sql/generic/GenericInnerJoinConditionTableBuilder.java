/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.model.*;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * Condition table builder that emulates the INTERSECT statement with inner joins. For example: instead of
 *
 * <p>
 * <code>SELECT tbl1.id AS data_id, tbl1.data_ver FROM tbl1 WHERE ...<br/>
 * INTERSECT<br/>
 * SELECT tbl2.id AS data_id, tbl2.ver AS data_ver FROM tbl2 WHERE ...<br/>
 * </code>
 * </p>
 *
 * <p>
 * this builder will generate a query similar to the following:
 * </p>
 *
 * <p>
 * <code>SELECT inner1.data_id, inner1.data_ver<br/>
 * FROM<br/>
 * (SELECT tbl1.id AS data_id, tbl1.ver AS data_ver FROM tbl1 WHERE ...) inner1,<br/>
 * (SELECT tbl2.id AS data_id, tbl2.ver AS data_ver FROM tbl2 WHERE ...) inner2 <br/>
 * WHERE (inner1.data_id = inner2.data_id AND inner1.data_ver = inner2.data_ver)<br/>
 * </code>
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
*/
public class GenericInnerJoinConditionTableBuilder extends GenericConditionTableBuilder {
    protected final Stack<List<String>> intersectTableAliases = new Stack<List<String>>();
    protected final Stack<Map<String, List<TableReference>>> intersectTableReferences = new Stack<Map<String, List<TableReference>>>();
    private int tableAliasCounter;
    private final Set<TableReference> currentConjunctionTables = new HashSet<TableReference>();  // accumulates table reference of the last condition in an AND condition

    public GenericInnerJoinConditionTableBuilder(SqlMapperFactory factory, StringBuilder out, SelectedTableVisitor joinedTables) {
        super(factory, out, joinedTables);
    }

    /** {@inheritDoc} */
    @Override
    protected void onEnterSubCondition(ConditionList.Connective type) {
        if (ConditionList.Connective.AND.equals(type)) {
            // enter "intersect" subquery, need to perform an inner join because MySQL does not support INTERSECT
            intersectTableAliases.push(new ArrayList<String>());
            intersectTableReferences.push(new HashMap<String, List<TableReference>>());
            currentConjunctionTables.clear();
        } else {
            super.onEnterSubCondition(type);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void onLeaveSubCondition(ConditionList.Connective type) {
        if (ConditionList.Connective.AND.equals(type)) {
            assert intersectTableAliases.peek().size() > 1 : "AND subcondition must contain at least two conditions";

            // create select from subtables - need to do this after the conditions have been rendered,
            // because now we know which subcondition maps to which content table (for conditions
            // in joined tables)

            final Map<TableReference, String> selectedTables = selectFromConjunction();

            // append join conditions

            boolean hasPrevious = false;
            for (Map.Entry<String, List<TableReference>> entry : intersectTableReferences.peek().entrySet()) {
                final String alias = entry.getKey();
                if (selectedTables.containsValue(alias)) {
                    // this table is already selected, thus it does not need to be joined
                    continue;
                }
                for (TableReference reference : entry.getValue()) {
                    if (hasPrevious) {
                        getOut().append(" AND ");
                    } else {
                        getOut().append(" WHERE ");
                        hasPrevious = true;
                    }
                    // link subcondition tables through the first table
                    final List<String> columns = new ArrayList<String>(joinedTables.getTableAliases().size() * 2);

                    // link to selected table
                    final String table = selectedTables.get(reference);
                    if (table != null) {
                        // if table was null, the corresponding table reference is not present in this subcondition
                        // and does not need to / cannot be joined
                        // TODO: this breaks with nested subconditions, because their selected tables do not appear in intersectTableReferences
                        assert !table.equals(alias) : "Table cannot be joined to itself";

                        columns.add(joinCondition(table, alias, reference.getIdFilterColumn()));
                        columns.add(joinCondition(table, alias, reference.getVersionFilterColumn()));
                    }
                    getOut().append(StringUtils.join(columns, " AND "));
                }
            }
            getOut().append(')');
            intersectTableAliases.pop();
            intersectTableReferences.pop();
        } else {
            super.onLeaveSubCondition(type);
        }
    }

    protected Map<TableReference, String> selectFromConjunction() {
        final StringBuilder select = new StringBuilder();
        select.append("(SELECT DISTINCT ");
        final List<String> columns = new ArrayList<String>(joinedTables.getTableAliases().size() * 2);
        final Map<TableReference, String> selectedTables = new HashMap<TableReference, String>();
        for (TableReference reference : joinedTables.getTableAliases().keySet()) {
            // find first table that selects a non-null value for this join table
            String firstTableSubCondition = null;
            for (Map.Entry<String, List<TableReference>> entry : intersectTableReferences.peek().entrySet()) {
                if (entry.getValue().contains(reference)) {
                    firstTableSubCondition = entry.getKey();
                    break;
                }
            }
            if (firstTableSubCondition == null) {
                // if firstTableSubCondition is null, the corresponding join table is never selected
                columns.add(factory.getSqlDialect().getEmptyId() + " AS " + reference.getIdFilterColumn());
                columns.add(factory.getSqlDialect().getEmptyVersion() + " AS " + reference.getVersionFilterColumn());
            } else {
                columns.add(firstTableSubCondition + "." + reference.getIdFilterColumn());
                columns.add(firstTableSubCondition + "." + reference.getVersionFilterColumn());
                selectedTables.put(reference, firstTableSubCondition);
            }
        }
        select.append(StringUtils.join(columns, ", "));
        select.append(" FROM ");
        getOut().insert(0, select);
        return selectedTables;
    }

    protected String joinCondition(String firstTable, String alias, String filterColumn) {
        return firstTable + "." + filterColumn
                + "=" + alias + "." + filterColumn;
    }

    /** {@inheritDoc} */
    @Override
    protected void onTableVisited(TableReference tableReference) {
        currentConjunctionTables.add(tableReference);
    }

    /** {@inheritDoc} */
    @Override
    protected void onStatementAdded() {
        super.onStatementAdded();
        if (!typeStack.isEmpty() && ConditionList.Connective.AND.equals(typeStack.peek())) {
            // We're in the FROM clause of the select started in onEnterSubCondition - add a table alias
            final String alias = getNextTableAlias();
            tableAliasCounter++;
            intersectTableAliases.peek().add(alias);
            if (!currentConjunctionTables.isEmpty()) {
                final Map<String, List<TableReference>> aliasMap = intersectTableReferences.peek();
                if (!aliasMap.containsKey(alias)) {
                    aliasMap.put(alias, new ArrayList<TableReference>());
                }
                aliasMap.get(alias).addAll(currentConjunctionTables);
                currentConjunctionTables.clear();
            }
            getOut().append(' ').append(alias);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected String joinConditionsAnd() {
        return ", ";
    }

    private String getNextTableAlias() {
        return "tbl_intersect_" + tableAliasCounter;
    }

}
