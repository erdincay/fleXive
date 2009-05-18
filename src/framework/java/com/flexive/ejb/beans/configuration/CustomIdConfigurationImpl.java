package com.flexive.ejb.beans.configuration;

import com.flexive.core.Database;
import com.flexive.shared.exceptions.FxNoAccessException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Extension of {@link GenericConfigurationImpl} configurations with an arbitrary ID field
 * (e.g. user ID).
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public abstract class CustomIdConfigurationImpl extends GenericConfigurationImpl {
    private final String configurationName;
    private final String idColumn;
    private final String tableName;
    private final boolean enableCaching;

    protected CustomIdConfigurationImpl(String configurationName, String tableName, String idColumn, boolean enableCaching) {
        this.configurationName = configurationName;
        this.idColumn = idColumn;
        this.tableName = tableName;
        this.enableCaching = enableCaching;
    }

    protected abstract void setId(PreparedStatement stmt, int column) throws SQLException;

    protected abstract boolean mayUpdate();

    @Override
    protected final Connection getConnection() throws SQLException {
        return Database.getDbConnection();
    }

    @Override
    protected final PreparedStatement getSelectStatement(Connection conn, String path, String key) throws SQLException {
        String sql = "SELECT cvalue FROM " + tableName + " WHERE " + idColumn + "=? AND cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        setId(stmt, 1);
        stmt.setString(2, path);
        stmt.setString(3, key);
        return stmt;
    }

    @Override
    protected final PreparedStatement getSelectStatement(Connection conn, String path) throws SQLException {
        String sql = "SELECT ckey, cvalue FROM " + tableName + " WHERE " + idColumn + "=? AND cpath=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        setId(stmt, 1);
        stmt.setString(2, path);
        return stmt;
    }

    @Override
    protected final PreparedStatement getUpdateStatement(Connection conn, String path, String key, String value) throws SQLException, FxNoAccessException {
        if (!mayUpdate()) {
            throw new FxNoAccessException("ex.configuration.update.perm", configurationName);
        }
        String sql = "UPDATE " + tableName + " SET cvalue=? WHERE " + idColumn + "=? AND cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, value);
        setId(stmt, 2);
        stmt.setString(3, path);
        stmt.setString(4, key);
        return stmt;
    }

    @Override
    protected final PreparedStatement getInsertStatement(Connection conn, String path, String key, String value) throws SQLException, FxNoAccessException {
        if (!mayUpdate()) {
            throw new FxNoAccessException("ex.configuration.update.perm", configurationName);
        }
        String sql = "INSERT INTO " + tableName + "(" + idColumn + ", cpath, ckey, cvalue) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        setId(stmt, 1);
        stmt.setString(2, path);
        stmt.setString(3, key);
        stmt.setString(4, value);
        return stmt;
    }

    @Override
    protected final PreparedStatement getDeleteStatement(Connection conn, String path, String key) throws SQLException, FxNoAccessException {
        if (!mayUpdate()) {
            throw new FxNoAccessException("ex.configuration.delete.perm.application");
        }
        String sql = "DELETE FROM " + tableName + " WHERE " + idColumn + "=? AND cpath=? "
            + (key != null ? " AND ckey=?" : "");
        PreparedStatement stmt = conn.prepareStatement(sql);
        setId(stmt, 1);
        stmt.setString(2, path);
        if (key != null) {
            stmt.setString(3, key);
        }
        return stmt;
    }

    @Override
    protected String getCachePath(String path) {
        if (enableCaching) {
            return "/" + configurationName + "Config";
        } else {
            return null;
        }
    }
}
