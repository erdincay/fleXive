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
package com.flexive.core.search.cmis.impl.sql.PostgreSQL;

import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.ResultRowNumber;
import com.flexive.core.search.cmis.impl.ResultScore;
import com.flexive.core.search.cmis.impl.sql.Capabilities;
import com.flexive.core.search.cmis.impl.sql.generic.GenericSqlDialect;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;
import com.flexive.core.search.cmis.model.ContainsCondition;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxEnvironment;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class PostgreSQLDialect extends GenericSqlDialect {
//    private static final Log LOG = LogFactory.getLog(PostgreSQLDialect.class);

    /**
     * PostgreSQL capabilities.
     */
    private static final Capabilities CAPABILITIES = new Capabilities() {
        public boolean supportsFulltextScoring() {
            return true;
        }

        public boolean normalizedFulltextScore() {
            return true;
        }

        public boolean supportsPaging() {
            return true;
        }
    };

    public PostgreSQLDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives) {
        super(environment, contentEngine, query, returnPrimitives);
    }

    @Override
    protected String limitAndOrderQuery(StringBuilder sql) {
        buildOrderBy(sql);
        if (query.getMaxRows() != -1)
            sql.append(" LIMIT ").append((query.getStartRow() + query.getMaxRows()));
        if( query.getStartRow() != 0)
            sql.append(" OFFSET ").append(query.getStartRow());
        return sql.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionMapper<ContainsCondition> conditionContain() {
        return PostgreSQLContainsCondition.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultColumnMapper<ResultScore> selectScore() {
        return PostgreSQLContainsCondition.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ResultColumnMapper<ResultRowNumber> selectRowNumber() {
        return PostgreSQLRowNumber.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Capabilities getCapabilities() {
        return CAPABILITIES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEmptyVersion() {
        return "CAST(null AS INTEGER)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEmptyId() {
        return "CAST (null AS BIGINT)";
    }
}
