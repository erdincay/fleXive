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
package com.flexive.core.search.cmis.impl.sql.generic.mapper.condition;

import com.flexive.core.search.cmis.impl.sql.SelectedTableVisitor;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionMapper;
import com.flexive.core.search.cmis.model.ComparisonCondition;
import com.flexive.core.search.cmis.model.ConditionalExpressionPart;
import com.flexive.core.search.cmis.model.TableReference;
import com.flexive.shared.search.query.PropertyValueComparator;
import org.apache.commons.lang.StringUtils;

/**
 * Creates a subselect for a single comparison condition. Since the left-hand-side of a condition can be any
 * value expression (i.e. a column reference or a function like UPPER()), this method does not extend
 * {@link com.flexive.core.search.cmis.impl.sql.generic.mapper.condition.AbstractColumnReferenceCondition}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class GenericComparisonCondition implements ConditionMapper<ComparisonCondition> {
    private static final GenericComparisonCondition INSTANCE = new GenericComparisonCondition();

    public String getConditionSql(SqlMapperFactory sqlMapperFactory, ComparisonCondition condition, SelectedTableVisitor joinedTables) {
        final TableReference table = condition.getLhs().getTableReference();
        final String tableAlias = joinedTables.getTableAlias(table);

        final String lhs = getConditionColumn(sqlMapperFactory, tableAlias, condition.getLhs());
        final String rhs = getConditionColumn(sqlMapperFactory, tableAlias, condition.getRhs());

        final String cond = lhs + " " + mapComparator(condition.getComparator()) + " " + rhs;
        final String assignmentFilter = sqlMapperFactory.getSqlDialect().getAssignmentFilter(
                                    condition.getLhs().getFilterTableType(),
                                    tableAlias,
                                    condition.getLhs().getReferencedAssignments());
        return "(SELECT DISTINCT " + joinedTables.getSelectForSingleTable(table) + " FROM "
                + condition.getLhs().getFilterTableName()
                + " " + tableAlias
                + " WHERE " + cond + (StringUtils.isNotBlank(assignmentFilter) ? " AND " + assignmentFilter : "") 
                + " AND " + sqlMapperFactory.getSqlDialect().getVersionFilter(tableAlias)
                + sqlMapperFactory.getSqlDialect().limitSubquery()
                + ")";
    }

    @SuppressWarnings({"unchecked"})
    protected String getConditionColumn(SqlMapperFactory sqlMapperFactory, String tableAlias, ConditionalExpressionPart column) {
        return column.getConditionColumnMapper(sqlMapperFactory)
                .getConditionColumn(sqlMapperFactory, column, tableAlias);
    }

    protected String mapComparator(PropertyValueComparator comparator) {
        switch (comparator) {
            case EQ:
                return "=";
            case GE:
                return ">=";
            case GT:
                return ">";
            case LE:
                return "<=";
            case LT:
                return "<";
            case NE:
                return "<>";
            default:
                throw new IllegalArgumentException("Unknown comparator: " + comparator);
        }
    }

    public static GenericComparisonCondition getInstance() {
        return INSTANCE;
    }
}
