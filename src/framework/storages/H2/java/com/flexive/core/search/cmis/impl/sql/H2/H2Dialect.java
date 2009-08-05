package com.flexive.core.search.cmis.impl.sql.H2;

import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.ResultScore;
import com.flexive.core.search.cmis.impl.sql.generic.GenericSqlDialect;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;
import com.flexive.core.search.cmis.impl.sql.Capabilities;
import com.flexive.core.search.cmis.model.ContainsCondition;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxEnvironment;

/**
 * H2 database dialect for CMIS-SQL.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class H2Dialect extends GenericSqlDialect {
    private static final Capabilities CAPABILITIES = new Capabilities() {
        public boolean supportsFulltextScoring() {
            return false;
        }

        public boolean normalizedFulltextScore() {
            return false;
        }

        public boolean supportsPaging() {
            return true;
        }
    };

    public H2Dialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives) {
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

    @Override
    public ResultColumnMapper<ResultScore> getScoreSqlMapper() {
        return H2ContainsCondition.getInstance();
    }

    @Override
    public ConditionMapper<ContainsCondition> getContainsConditionSqlMapper() {
        return H2ContainsCondition.getInstance();
    }

    @Override
    public Capabilities getCapabilities() {
        return CAPABILITIES;
    }
}
