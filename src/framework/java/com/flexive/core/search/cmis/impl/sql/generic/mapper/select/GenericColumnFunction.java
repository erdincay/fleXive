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
package com.flexive.core.search.cmis.impl.sql.generic.mapper.select;

import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.ResultColumnFunction;
import com.flexive.core.search.cmis.impl.sql.ColumnIndex;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionColumnMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;
import com.flexive.core.search.cmis.model.ColumnReference;
import com.flexive.core.search.cmis.model.ColumnValueFunction;
import com.flexive.core.search.cmis.model.ValueFunction;
import com.flexive.shared.structure.FxDataType;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps functions that are applied on a column argument (e.g. UPPER(prop)).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class GenericColumnFunction implements ResultColumnMapper<ResultColumnFunction>, ConditionColumnMapper<ColumnValueFunction> {
    private static final GenericColumnFunction INSTANCE = new GenericColumnFunction();

    /**
     * {@inheritDoc}
     */
    public String selectColumn(SqlMapperFactory sqlMapperFactory, CmisSqlQuery query, ResultColumnFunction column, long languageId, boolean xpath, boolean includeResultAlias, ColumnIndex index) {
        final ValueFunction.Functions function = column.getFunction().getFunction();
        return  // render SQL function (basic functions (LOWER, UPPER) can be mapped directly to SQL)
                function.getSqlName()
                        + "("
                        // add column where the function should be applied
                        + (column.getResultColumn() != null
                        ? column.getResultColumn().selectSql(sqlMapperFactory, query, languageId, xpath, false, index)
                        : "")
                        + ") "
                        // add optional alias
                        + (includeResultAlias ? " AS " + column.getResultColumn().getResultSetAlias() : "");
    }

    /**
     * {@inheritDoc}
     */
    public Object decodeResultValue(SqlMapperFactory factory, ResultSet rs, ResultColumnFunction column, long languageId) throws SQLException {
        // string functions do not alter the result type, thus we can forward to the column's decode method
        return column.getResultColumn().decodeResultValue(factory, rs, languageId);
    }

    /**
     * {@inheritDoc}
     */
    public String getConditionColumn(SqlMapperFactory sqlMapperFactory, ColumnValueFunction expression, String tableAlias) {
        // the function wraps the actual column, so we add our function SQL name and include the
        // outpout of the wrapped column
        final ColumnReference column = expression.getColumnReference();
        return expression.getFunction().getSqlName()
                + "("
                + column.getConditionColumnMapper(sqlMapperFactory).getConditionColumn(sqlMapperFactory, column, tableAlias)
                + ")";
    }

    public boolean isDirectSelectForMultivalued(SqlMapperFactory factory, ResultColumnFunction column, FxDataType dataType) {
        return column.getResultColumn().getSqlMapper(factory).isDirectSelectForMultivalued(factory, column.getResultColumn(), dataType);
    }

    public static GenericColumnFunction getInstance() {
        return INSTANCE;
    }
}
