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
package com.flexive.core.search.cmis.impl.sql.generic.mapper.condition;

import com.flexive.core.search.PropertyResolver;
import com.flexive.core.search.cmis.impl.sql.SelectedTableVisitor;
import com.flexive.core.search.cmis.impl.sql.SqlDialect;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionColumnMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionMapper;
import com.flexive.core.search.cmis.model.ColumnReference;
import com.flexive.core.search.cmis.model.Condition;
import com.flexive.core.search.cmis.model.TableReference;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * Base class for conditions that filter the values of a column based on some condition (e.g. an IN
 * or LIKE query).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public abstract class AbstractColumnReferenceCondition<T extends Condition> implements ConditionMapper<T> {

    public String getConditionSql(SqlMapperFactory sqlMapperFactory, T condition, SelectedTableVisitor joinedTables) {
        final ColumnReference ref = getColumnReference(condition);
        final TableReference table = ref.getTableReference();
        final String tableAlias = joinedTables.getTableAlias(table);
        final ConditionColumnMapper<? super ColumnReference> mapper = ref.getConditionColumnMapper(sqlMapperFactory);

        final String matchConditions;
        final SqlDialect dialect = sqlMapperFactory.getSqlDialect();
        final String filterTableName;
        if (negationQuery(condition)) {
            // we select the difference between all types of this table AND the selected ones (i.e. a MINUS query)
            final String assignmentFilter = dialect.getAssignmentFilter(ref.getFilterTableType(), "sub", true, ref.getReferencedAssignments());
            filterTableName = PropertyResolver.Table.T_CONTENT.getTableName();
            matchConditions = "id NOT IN (SELECT id FROM "
                    + ref.getFilterTableName()
                    + " sub WHERE "
                    + (isNotBlank(assignmentFilter) ? assignmentFilter + " AND " : "")
                    + mapper.getConditionColumn(sqlMapperFactory, ref, "sub")
                    + " " + getColumnCondition(condition)
                    + " AND " + dialect.getVersionFilter("sub")
                    + ")"
                    // limit on type, otherwise we'd select all instances without this property
                    + " AND " + dialect.getTypeFilter(tableAlias + ".tdef", table.getReferencedTypes());
        } else {
            // simply select all matching rows
            filterTableName = ref.getFilterTableName();
            final String assignmentFilter = dialect.getAssignmentFilter(ref.getFilterTableType(), tableAlias, true, ref.getReferencedAssignments());
            matchConditions = (isNotBlank(assignmentFilter) ? assignmentFilter + " AND " : "")
                    + mapper.getConditionColumn(sqlMapperFactory, ref, tableAlias)
                    + " " + getColumnCondition(condition)
                    + " AND " + dialect.getVersionFilter(tableAlias);
        }
        return "(SELECT DISTINCT " + joinedTables.getSelectForSingleTable(table)
                + additionalColumns(table, condition)
                + " FROM " + filterTableName + " " + tableAlias
                + " WHERE "
                + matchConditions + " "
                + (filterTypes(condition) ?
                // add type filter, otherwise we'd select all instances that do not have this property at all
                " AND id IN (SELECT id FROM " + PropertyResolver.Table.T_CONTENT.getTableName() + " WHERE "
                        + dialect.getTypeFilter("tdef", table.getReferencedTypes()) + ")"
                // TODO: add version filter
                : "")
                + getGroupBy(table, tableAlias, condition)
                + dialect.limitSubquery()
                + ")";
    }

    private String additionalColumns(TableReference table, T condition) {
        final String cols = getAdditionalColumns(table, condition);
        return cols != null ? ", " + cols : ""; 
    }

    protected String getAdditionalColumns(TableReference table, T condition) {
        return null;
    }

    protected String getGroupBy(TableReference table, String tableAlias, T condition) {
        return "";
    }

    protected abstract ColumnReference getColumnReference(T condition);

    protected abstract String getColumnCondition(T condition);

    protected abstract boolean negationQuery(T condition);

    protected abstract boolean filterTypes(T condition);
}
