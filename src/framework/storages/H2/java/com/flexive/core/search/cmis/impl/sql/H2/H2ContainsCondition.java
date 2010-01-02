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
package com.flexive.core.search.cmis.impl.sql.H2;

import com.flexive.core.DatabaseConst;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.ResultScore;
import com.flexive.core.search.cmis.impl.sql.ColumnIndex;
import com.flexive.core.search.cmis.impl.sql.SelectedTableVisitor;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.generic.mapper.condition.GenericContainsCondition;
import com.flexive.core.search.cmis.model.ContainsCondition;
import com.flexive.shared.FxFormatUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * H2 fulltext search implementation. Currently this is a dummy implementation that does not
 * utilize fulltext indices, and always returns "1.0" when the fulltext SCORE() is selected.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class H2ContainsCondition extends GenericContainsCondition {
    private static final H2ContainsCondition INSTANCE = new H2ContainsCondition();

    /**
     * {@inheritDoc}
     */
    @Override
    public String selectColumn(SqlMapperFactory sqlMapperFactory, CmisSqlQuery query, ResultScore column, long languageId, boolean xpath, boolean includeResultAlias, ColumnIndex index) {
        // check if we have a fulltext query
        findContainsCondition(query);
        // since H2 doesn't support scoring, we always return 1.0 and don't select from the result
        index.increment();
        return "1.0";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object decodeResultValue(SqlMapperFactory factory, ResultSet rs, ResultScore column, long languageId) throws SQLException {
        return 1.0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConditionSql(SqlMapperFactory sqlMapperFactory, ContainsCondition condition, SelectedTableVisitor joinedTables) {
        final String alias = joinedTables.getTableAlias(condition.getTableReference());
        return "(SELECT DISTINCT " + joinedTables.getSelectForSingleTable(condition.getTableReference()) + " FROM "
                + DatabaseConst.TBL_CONTENT_DATA_FT + " " + alias + " "
                + "WHERE value LIKE "
                + FxFormatUtils.escapeForSql("%" + condition.getExpression().toUpperCase() + "%")
                + sqlMapperFactory.getSqlDialect().limitSubquery()
                + ")";

    }

    public static H2ContainsCondition getInstance() {
        return INSTANCE;
    }
}
