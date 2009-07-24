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

import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.ResultScore;
import com.flexive.core.search.cmis.impl.sql.ColumnIndex;
import com.flexive.core.search.cmis.impl.sql.SelectedTableVisitor;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;
import com.flexive.core.search.cmis.model.ContainsCondition;
import com.flexive.core.search.cmis.model.NopConditionNodeVisitor;
import com.flexive.shared.exceptions.FxCmisQueryException;
import com.flexive.shared.structure.FxDataType;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Support for SCORE()/CONTAINS() fulltext queries. SCORE() can only be used in the SELECT and ORDER BY clauses,
 * CONTAINS() only in the WHERE clause.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class GenericContainsCondition implements ConditionMapper<ContainsCondition>, ResultColumnMapper<ResultScore> {
    private static final GenericContainsCondition INSTANCE = new GenericContainsCondition();

    public String selectColumn(SqlMapperFactory sqlMapperFactory, CmisSqlQuery query, ResultScore column, long languageId, boolean xpath, boolean includeResultAlias, ColumnIndex index) {
        throw new UnsupportedOperationException("SCORE() not supported by generic SQL dialect.");
    }

    public Object decodeResultValue(SqlMapperFactory factory, ResultSet rs, ResultScore column, long languageId) throws SQLException {
        throw new UnsupportedOperationException("SCORE() not supported by generic SQL dialect.");
    }

    public String getConditionSql(SqlMapperFactory sqlMapperFactory, ContainsCondition condition, SelectedTableVisitor joinedTables) {
        throw new UnsupportedOperationException("CONTAINS() not supported by generic SQL dialect.");
    }

    public boolean isDirectSelectForMultivalued(SqlMapperFactory factory, ResultScore column, FxDataType dataType) {
        return false;
    }

    /**
     * Find the contains condition of the query that should be used for matching a SCORE() function
     * in the SELECT clause.
     *
     * @param query the query
     * @return  the contains condition of the query that should be used for matching a SCORE() function
     * in the SELECT clause.
     */
    protected ContainsCondition findContainsCondition(CmisSqlQuery query) {
        final FindComparisonVisitor visitor = new FindComparisonVisitor();
        query.getStatement().getRootCondition().accept(visitor);
        if (visitor.getCondition() == null) {
            throw new FxCmisQueryException("ex.cmis.search.query.score.noContains").asRuntimeException();
        }
        return visitor.getCondition();
    }

    public static GenericContainsCondition getInstance() {
        return INSTANCE;
    }

    private static class FindComparisonVisitor extends NopConditionNodeVisitor {
        private ContainsCondition condition;

        @Override
        public void visit(ContainsCondition contains) {
            if (condition != null) {
                throw new FxCmisQueryException("ex.cmis.search.query.score.ambiguous").asRuntimeException();
            }
            condition = contains;
        }

        public ContainsCondition getCondition() {
            return condition;
        }
    }
}
