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

import com.flexive.core.Database;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.ResultColumn;
import com.flexive.core.search.cmis.impl.ResultColumnReference;
import com.flexive.core.search.cmis.impl.ResultScore;
import com.flexive.core.search.cmis.impl.sql.Capabilities;
import com.flexive.core.search.cmis.impl.sql.SelectedTableVisitor;
import com.flexive.core.search.cmis.impl.sql.generic.GenericConditionTableBuilder;
import com.flexive.core.search.cmis.impl.sql.generic.GenericInnerJoinConditionTableBuilder;
import com.flexive.core.search.cmis.impl.sql.generic.GenericSqlDialect;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionColumnMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;
import com.flexive.core.search.cmis.model.ColumnReference;
import com.flexive.core.search.cmis.model.ContainsCondition;
import com.flexive.core.search.cmis.model.Selectable;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class PostgreSQLDialect extends GenericSqlDialect {
    private static final Log LOG = LogFactory.getLog(PostgreSQLDialect.class);

    /**
     * MySQL capabilities.
     */
    private static final Capabilities CAPABILITIES = new Capabilities() {

        public boolean supportsFulltextScoring() {
            return true;
        }

        public boolean normalizedFulltextScore() {
            return false;
        }

        public boolean supportsPaging() {
            return true;
        }

    };

    public PostgreSQLDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives) {
        super(environment, contentEngine, query, returnPrimitives);
    }

    @Override
    protected String limitQuery(String sql) {
        return sql + " LIMIT "
                + query.getStartRow()
                + (query.getMaxRows() != -1
                ? ", " + (query.getStartRow() + query.getMaxRows())
                : "");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void prepareConnection(Connection con) throws SQLException {
        super.prepareConnection(con);
        for (ResultColumn column : query.getUserColumns()) {
            if (column.getSelectedObject() instanceof Selectable
                    && ((Selectable) column.getSelectedObject()).isMultivalued()) {

                // multivalued queries use GROUP_CONCAT, which is limited to 1024 characters
                // by default - increase limit to maximum allowed packet size
                Statement stmt = null;
                try {
                    if (LOG.isTraceEnabled()) {
                        LOG.trace("Multivalued properties with PostgreSQL: SET group_concat_max_len := @@max_allowed_packet");
                    }
                    stmt = con.createStatement();
                    stmt.executeQuery("SET group_concat_max_len := @@max_allowed_packet");
                    break;
                } finally {
                    Database.closeObjects(PostgreSQLDialect.class, null, stmt);
                }

            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected GenericConditionTableBuilder createConditionNodeVisitor(StringBuilder out, SelectedTableVisitor joinedTables) {
        return new GenericInnerJoinConditionTableBuilder(this, out, joinedTables);
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

    @Override
    public ResultColumnMapper<ResultColumnReference> selectColumnReference() {
        return PostgreSQLColumnReference.getInstance();
    }

    @Override
    public ConditionColumnMapper<ColumnReference> filterColumnReference() {
        return PostgreSQLColumnReference.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Capabilities getCapabilities() {
        return CAPABILITIES;
    }

}
