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

import com.flexive.core.search.cmis.model.ColumnReference;
import com.flexive.core.search.cmis.model.InCondition;
import com.flexive.core.search.cmis.model.Literal;
import com.flexive.core.search.cmis.model.TableReference;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.structure.FxDataType;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Condition mapper for IN and NOT IN conditions.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class GenericInColumnReferenceCondition extends AbstractColumnReferenceCondition<InCondition> {
    private static final GenericInColumnReferenceCondition INSTANCE = new GenericInColumnReferenceCondition();

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnReference getColumnReference(InCondition condition) {
        return condition.getColumnReference();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getColumnCondition(InCondition condition) {
        final List<String> values = new ArrayList<String>(condition.getValues().size());
        for (Literal literal : condition.getValues()) {
            values.add(FxFormatUtils.escapeForSql(literal.getValue()));
        }
        return (condition.isNegated() ? "NOT " : "") + "IN ("
                + StringUtils.join(values, ',')
                + ")";
    }

    @Override
    protected String getAdditionalColumns(TableReference table, InCondition condition) {
        return isSelectMany(condition) && condition.isNegated() ? "FINT" : null;
    }

    @Override
    protected String getGroupBy(TableReference table, String tableAlias, InCondition condition) {
        return isSelectMany(condition)
                ? "GROUP BY " + table.getIdFilterColumn()
                + ", " + table.getVersionFilterColumn()
                + (condition.isNegated() ? ", FINT" : "")
                + " HAVING count(*) = "
                // IN: match length of query IDs, NOT IN: match stored length of selected IDs (all must be returned)
                + (condition.isNegated() ? "FINT" : condition.getValues().size())
                : "";
    }

    protected boolean isSelectMany(InCondition condition) {
        return condition.getColumnReference().getPropertyEntry().getProperty().getDataType() == FxDataType.SelectMany;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean negationQuery(InCondition condition) {
        // to obey SQL semantics for NULL values, they are never returned for NOT IN conditions
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean filterTypes(InCondition condition) {
        // to obey SQL semantics for NULL values, they are never returned for NOT IN conditions
        return false;
    }

    public static GenericInColumnReferenceCondition getInstance() {
        return INSTANCE;
    }
}
