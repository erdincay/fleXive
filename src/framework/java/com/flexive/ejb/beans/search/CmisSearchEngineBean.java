package com.flexive.ejb.beans.search;

import com.flexive.core.Database;
import com.flexive.core.storage.StorageManager;
import com.flexive.core.storage.ContentStorage;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.sql.SqlDialect;
import com.flexive.core.search.cmis.impl.sql.SqlDialectFactory;
import com.flexive.core.search.cmis.parser.CmisSqlUtils;
import static com.flexive.shared.FxContext.getUserTicket;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxCmisQueryException;
import com.flexive.shared.interfaces.CmisSearchEngine;
import com.flexive.shared.interfaces.CmisSearchEngineLocal;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.search.cmis.CmisResultSet;
import com.flexive.shared.TimestampRecorder;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.structure.TypeStorageMode;
import com.flexive.shared.structure.FxEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.EJB;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Stateless(name = "CmisSearchEngine")
@javax.ejb.TransactionManagement(javax.ejb.TransactionManagementType.CONTAINER)
public class CmisSearchEngineBean implements CmisSearchEngine, CmisSearchEngineLocal {
    private static final Log LOG = LogFactory.getLog(CmisSearchEngineBean.class);

    @EJB
    private ContentEngine contentEngine;

    public CmisResultSet search(String query) throws FxApplicationException {
        return search(query, true, 0, 2000);
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public CmisResultSet search(String query, boolean returnPrimitiveValues, int startRow, int maxRows) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        String sql = null;
        try {
            final FxEnvironment environment = CacheAdmin.getEnvironment();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing CMIS search query: \n" + query);
            }
            con = Database.getDbConnection();
            final ContentStorage storage = StorageManager.getContentStorage(TypeStorageMode.Hierarchical);

            final TimestampRecorder tsr = new TimestampRecorder();
            tsr.begin();
            final CmisSqlQuery cmisQuery = new CmisSqlQuery(
                    environment,
                    storage,
                    CmisSqlUtils.buildStatement(storage, query),
                    getUserTicket().getLanguage().getId(),
                    startRow,
                    maxRows,
                    returnPrimitiveValues
            );
            tsr.timestamp("Parsing, AST generation");

            final SqlDialect sqlDialect = SqlDialectFactory.getInstance(
                    environment,
                    contentEngine,
                    cmisQuery, 
                    returnPrimitiveValues
            );
            sql = sqlDialect.getSql();
            tsr.timestamp("Generate SQL");

            sqlDialect.prepareConnection(con);
            stmt = con.createStatement();
            final ResultSet rs = stmt.executeQuery(sql);
            tsr.timestamp("Execute query");

            final CmisResultSet result = sqlDialect.processResultSet(rs);
            tsr.timestamp("Process result set");
            result.setTimestampRecorder(tsr);

            if (LOG.isDebugEnabled()) {
                LOG.debug(tsr);
            }
            return result;
        } catch (SQLException e) {
            if (sql == null) {
                throw new FxCmisQueryException(LOG, e, "ex.cmis.search.query.general", e.getMessage());
            } else {
                throw new FxCmisQueryException(LOG, e, "ex.cmis.search.query.withSql", e.getMessage(), sql);
            }
        } finally {
            Database.closeObjects(CmisSearchEngineBean.class, con, stmt);
            if (LOG.isDebugEnabled() && sql != null) {
                LOG.debug("Generated SQL query:\n" + sql);
            }
        }
    }

}
