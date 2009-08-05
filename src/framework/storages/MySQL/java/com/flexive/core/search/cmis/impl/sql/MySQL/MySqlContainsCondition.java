/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.core.search.cmis.impl.sql.MySQL;

import com.flexive.core.DatabaseConst;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.ResultScore;
import com.flexive.core.search.cmis.impl.sql.ColumnIndex;
import com.flexive.core.search.cmis.impl.sql.SelectedTableVisitor;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import static com.flexive.core.search.cmis.impl.sql.generic.GenericSqlDialect.FILTER_ALIAS;
import com.flexive.core.search.cmis.impl.sql.generic.mapper.condition.GenericContainsCondition;
import com.flexive.core.search.cmis.model.ContainsCondition;
import com.flexive.shared.FxFormatUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * MySQL fulltext query/scoring provider. The values returned by SCORE() are not normalized, thus the
 * {@link com.flexive.shared.search.cmis.CmisResultSet} must apply normalization when the entire result is loaded.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class MySqlContainsCondition extends GenericContainsCondition {
    private static final MySqlContainsCondition INSTANCE = new MySqlContainsCondition();

    @Override
    public String selectColumn(SqlMapperFactory sqlMapperFactory, CmisSqlQuery query, ResultScore column, long languageId, boolean xpath, boolean includeResultAlias, ColumnIndex index) {
        final ContainsCondition contains = findContainsCondition(query);
        index.increment();
        return "(SELECT MAX(" + getFulltextMatch("ftsub", contains) + ") FROM "
                + DatabaseConst.TBL_CONTENT_DATA_FT + " ftsub "
                + "WHERE ftsub.id=" + FILTER_ALIAS + "." + contains.getTableReference().getIdFilterColumn()
                + "  AND ftsub.ver=" + FILTER_ALIAS + "." + contains.getTableReference().getVersionFilterColumn() + ") "
                + (includeResultAlias ? column.getResultSetAlias() : "");
    }

    @Override
    public Object decodeResultValue(SqlMapperFactory factory, ResultSet rs, ResultScore column, long languageId) throws SQLException {
        return rs.getDouble(column.getColumnStart());
    }

    @Override
    public String getConditionSql(SqlMapperFactory sqlMapperFactory, ContainsCondition condition, SelectedTableVisitor joinedTables) {
        final String alias = joinedTables.getTableAlias(condition.getTableReference());
        final String cond = getFulltextMatch(alias, condition);
        // TODO: assignment filter
        return "(SELECT DISTINCT " + joinedTables.getSelectForSingleTable(condition.getTableReference()) + " FROM "
                + DatabaseConst.TBL_CONTENT_DATA_FT + " " + alias + " "  
                + "WHERE " + cond + sqlMapperFactory.getSqlDialect().limitSubquery() + ")";
    }

    public static MySqlContainsCondition getInstance() {
        return INSTANCE;
    }

    private String getFulltextMatch(String tableAlias, ContainsCondition condition) {
        return "MATCH(" + tableAlias + ".value) AGAINST (" + FxFormatUtils.escapeForSql(condition.getExpression()) + ")";
    }

}
