package com.flexive.ejb.beans.configuration;

import com.flexive.core.configuration.GenericConfigurationImpl;
import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import static com.flexive.core.DatabaseConst.TBL_USER_CONFIG;
import static com.flexive.core.DatabaseConst.TBL_APPLICATION_CONFIG;
import com.flexive.shared.interfaces.ApplicationConfigurationEngine;
import com.flexive.shared.interfaces.ApplicationConfigurationEngineLocal;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.FxContext;

import javax.ejb.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;

/**
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Stateless(name = "ApplicationConfigurationEngine", mappedName="ApplicationConfigurationEngine")
public class ApplicationConfigurationEngineBean
        extends GenericConfigurationImpl
        implements ApplicationConfigurationEngine, ApplicationConfigurationEngineLocal {
    /**
     * Application config cache root.
     */
    private static final String CACHE_ROOT = "/applicationConfig";

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection getConnection() throws SQLException {
        return Database.getDbConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getSelectStatement(Connection conn, String path, String key) throws SQLException {
        String sql = "SELECT cvalue FROM " + TBL_APPLICATION_CONFIG + " WHERE application_id=? AND cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, getApplicationId());
        stmt.setString(2, path);
        stmt.setString(3, key);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getSelectStatement(Connection conn, String path) throws SQLException {
        String sql = "SELECT ckey, cvalue FROM " + TBL_APPLICATION_CONFIG + " WHERE application_id=? AND cpath=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, getApplicationId());
        stmt.setString(2, path);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getUpdateStatement(Connection conn, String path, String key, String value) throws SQLException, FxNoAccessException {
        if (!FxContext.getUserTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.configuration.update.perm.application");
        }
        String sql = "UPDATE " + TBL_APPLICATION_CONFIG + " SET cvalue=? WHERE application_id=? AND cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, value);
        stmt.setString(2, getApplicationId());
        stmt.setString(3, path);
        stmt.setString(4, key);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getInsertStatement(Connection conn, String path, String key, String value) throws SQLException, FxNoAccessException {
        if (!FxContext.getUserTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.configuration.update.perm.application");
        }
        String sql = "INSERT INTO " + TBL_APPLICATION_CONFIG + "(application_id, cpath, ckey, cvalue) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, getApplicationId());
        stmt.setString(2, path);
        stmt.setString(3, key);
        stmt.setString(4, value);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getDeleteStatement(Connection conn, String path, String key) throws SQLException, FxNoAccessException {
        if (!FxContext.getUserTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.configuration.delete.perm.application");
        }
        String sql = "DELETE FROM " + TBL_APPLICATION_CONFIG + " WHERE application_id=? AND cpath=? "
            + (key != null ? " AND ckey=?" : "");
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, getApplicationId());
        stmt.setString(2, path);
        if (key != null) {
            stmt.setString(3, key);
        }
        return stmt;
    }

    @Override
    protected String getCachePath(String path) {
        return CACHE_ROOT + path;
    }

    private String getApplicationId() {
        return FxContext.get().getApplicationId();
    }
}
