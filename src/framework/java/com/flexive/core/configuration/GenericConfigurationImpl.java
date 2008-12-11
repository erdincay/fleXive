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
package com.flexive.core.configuration;

import com.flexive.core.Database;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.ParameterData;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.GenericConfigurationEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.SerializationUtils;

import java.io.Serializable;
import java.sql.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Abstract base class for configuration methods. Implements templated getter/setter
 * methods for configuration classes that may add custom behavior like caching.
 * <p>
 * An implementor must create SQL statements for reading, updating and deleting
 * parameters, and a method for obtaining a database connection for the
 * configuration table.
 * </p>
 * <p>
 * The <code>setParameter/getParameter</code> methods may be overridden
 * to implement custom behavior, e.g. caching of parameter values.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public abstract class GenericConfigurationImpl implements GenericConfigurationEngine {
    private static final Log LOG = LogFactory.getLog(GenericConfigurationImpl.class);

    /**
     * Helper class to place unset parameters in the configuration cache.
     */
    protected static class UnsetParameter implements Serializable {
        private static final long serialVersionUID = -289868789320707584L;
    }

    /**
     * Helper class to represent null value in the cache (since an unset
     * parameter would also be null).
     */
    protected static class NullParameter implements Serializable {
        private static final long serialVersionUID = -3959509316291057862L;
    }

    /**
     * Return a (new or existing) Connection to the configuration table.
     *
     * @throws SQLException if the connection could not be retrieved
     * @return a Connection to the configuration table.
     */
    protected abstract Connection getConnection() throws SQLException;

    /**
     * Return a select statement that selects the given path and key
     * and returns the stored value.
     * <p>
     * Required SELECT arguments:
     * <ol>
     * <li>value</li>
     * </ol>
     * </p>
     *
     * @param conn the current connection
     * @param path the requested path
     * @param key  the requested key
     * @throws SQLException if a database error occurs
     * @return a PreparedStatement selecting the value for the given path/key combination
     */
    protected abstract PreparedStatement getSelectStatement(Connection conn, String path, String key)
            throws SQLException;

    /**
     * Return a select statement that selects all keys and values for the given path.
     * <p>
     * Required SELECT arguments:
     * <ol>
     * <li>key</li>
     * <li>value</li>
     * </ol>
     * </p>
     *
     * @param conn the current connection
     * @param path the requested path
     * @throws SQLException if a database error occurs
     * @return a PreparedStatement selecting all keys and values for the given path
     */
    protected abstract PreparedStatement getSelectStatement(Connection conn, String path) throws SQLException;

    /**
     * Return an update statement that updates the value for the given
     * path/key combination.
     *
     * @param conn  the current connection
     * @param path  path of the parameter
     * @param key   key to be updated
     * @param value the new value to be stored
     * @throws SQLException        if a database error occurs
     * @throws FxNoAccessException if the caller is not permitted to update the given parameter
     * @return a PreparedStatement updating the given row
     */
    protected abstract PreparedStatement getUpdateStatement(Connection conn, String path, String key, String value)
            throws SQLException, FxNoAccessException;

    /**
     * Return an insert statement that inserts a new row for the given
     * path, key and value.
     *
     * @param conn  the current connection
     * @param path  path of the new row
     * @param key   key of the new row
     * @param value value of the new row
     * @throws SQLException        if a database error occurs
     * @throws FxNoAccessException if the caller is not permitted to create the given parameter
     * @return a PreparedStatement for inserting the given path/key/value
     */
    protected abstract PreparedStatement getInsertStatement(Connection conn, String path, String key, String value)
            throws SQLException, FxNoAccessException;

    /**
     * Return a delete statement to delete the given parameter.
     *
     * @param conn the current connection
     * @param path path of the row to be deleted
     * @param key  key of the row to be deleted. If null, all keys under the given path should be deleted.
     * @return a PreparedStatement for deleting the given path/key
     * @throws SQLException        if a database error occurs
     * @throws FxNoAccessException if the caller is not permitted to delete the given parameter
     */
    protected abstract PreparedStatement getDeleteStatement(Connection conn, String path, String key)
            throws SQLException, FxNoAccessException;

    /**
     * Return the cache path for the given configuration parameter path.
     * If this method returns null (like the default implementation), caching
     * is disabled. Be aware that you have to add the context to your cache path,
     * e.g. the user ID for user settings.
     *
     * @param path the parameter path to be mapped
     * @return the mapped parameter path, or null to disable caching
     */
    protected String getCachePath(String path) {
        return null;
    }

    /**
     * Wrapper for simple cache stats. May be used as hook
     * for adding cache logging or as an aspectj pointcut.
     *
     * @param path the parameter path that caused the cache hit
     * @param key  the parameter key that caused the cache hit
     */
    protected void logCacheHit(String path, String key) {
        // no cache stats by default
    }

    /**
     * Wrapper for simple cache stats. May be used as hook
     * for adding cache logging or as an aspectj pointcut.
     *
     * @param path the parameter path that caused the cache hit
     * @param key  the parameter key that caused the cache hit
     */
    protected void logCacheMiss(String path, String key) {
        // no cache stats by default
    }


    /**
     * {@inheritDoc}
     */
    public <T> void put(Parameter<T> parameter, String key, T value)
            throws FxApplicationException {

        if (!parameter.isValid(value)) {
            throw new FxUpdateException("ex.configuration.parameter.value", parameter, value);
        }

        // put into DB config table
        Connection conn = null;
        PreparedStatement stmt = null;
        ParameterData<T> data = parameter.getData();
        try {
            conn = getConnection();
            stmt = getSelectStatement(conn, data.getPath().getValue(), key);
            ResultSet rs = stmt.executeQuery();
            boolean valueExists = rs.next();
            stmt.close();
            stmt = null;
            if (valueExists) {
                // update existing record
                stmt = getUpdateStatement(conn, data.getPath().getValue(), key,
                        value != null ? parameter.getDatabaseValue(value) : null);
            } else {
                // create new record
                stmt = getInsertStatement(conn, data.getPath().getValue(), key,
                        value != null ? parameter.getDatabaseValue(value) : null);
            }
            stmt.executeUpdate();

            // update cache?
            String cachePath = getCachePath(data.getPath().getValue());
            if (cachePath != null) {
                putCache(cachePath, key, value != null ?
                        (Serializable) SerializationUtils.clone((Serializable) value)
                        : new NullParameter());
            }
        } catch (SQLException se) {
            FxUpdateException ue = new FxUpdateException(LOG, se, "ex.db.sqlError", se.getMessage());
            LOG.error(ue, se);
            throw ue;
        } finally {
            Database.closeObjects(GenericConfigurationImpl.class, conn, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T> void put(Parameter<T> parameter, T value) throws FxApplicationException {
        put(parameter, parameter.getData().getKey(), value);
    }

    /**
     * Get a configuration parameter identified by a path and a key.
     *
     * @param parameter the actual parameter instance
     * @param path path of the parameter
     * @param key  key of the parameter
     * @return the parameter value
     * @throws FxLoadException     if the parameter could not be loaded
     * @throws FxNotFoundException if the parameter does not exist
     */
    protected <T> Object getParameter(Parameter<T> parameter, String path, String key) throws FxLoadException, FxNotFoundException {
        String cachePath = getCachePath(path);
        if (cachePath != null) {
            // try cache first
            try {
                Object value = getCache(cachePath, key);
                if (value != null) {
                    logCacheHit(path, key);
                    if (value instanceof UnsetParameter) {
                        // check for null object
                        throw new FxNotFoundException("ex.configuration.parameter.notfound", path, key);
                    } else if (value instanceof NullParameter) {
                        return null;
                    } else {
                        return value;
                    }
                }
            } catch (FxCacheException e) {
                LOG.error("Cache failure (ignored): " + e.getMessage(), e);
            }
        }
        // load parameter from config table
        logCacheMiss(path, key);
        Serializable value = loadParameterFromDb(path, key);
        if (cachePath != null) {
            // add value to cache
            putCache(cachePath, key, (Serializable) parameter.getValue(value));
        }
        return value;
    }


    /**
     * {@inheritDoc}
     */
    public <T> T get(Parameter<T> parameter) throws FxApplicationException {
        return get(parameter, parameter.getData().getKey());
    }

    /**
     * {@inheritDoc}
     */
    public <T> T get(Parameter<T> parameter, String key)
            throws FxApplicationException {
        try {
            return parameter.getValue(getParameter(parameter, parameter.getPath().getValue(), key));
        } catch (FxNotFoundException e) {
            if (parameter.getDefaultValue() != null) {
                return parameter.getDefaultValue();
            } else {
                throw e;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T> T get(Parameter<T> parameter, String key, boolean ignoreDefault)
            throws FxApplicationException {
        try {
            return parameter.getValue(getParameter(parameter, parameter.getPath().getValue(), key));
        } catch (FxNotFoundException e) {
            if (!ignoreDefault && parameter.getDefaultValue() != null) {
                return parameter.getDefaultValue();
            } else {
                throw e;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T> Map<String, T> getAll(Parameter<T> parameter) throws FxApplicationException {
        Connection conn = null;
        PreparedStatement stmt = null;
        ParameterData<T> data = parameter.getData();
        HashMap<String, T> parameters = new HashMap<String, T>();
        try {
            conn = getConnection();
            stmt = getSelectStatement(conn, data.getPath().getValue());
            ResultSet rs = stmt.executeQuery();
            while (rs != null && rs.next()) {
                // retrieve parameters and put them in hashmap
                parameters.put(rs.getString(1), parameter.getValue(rs.getString(2)));
            }
            return parameters;
        } catch (SQLException se) {
            throw new FxLoadException(LOG, se, "ex.db.sqlError", se.getMessage());
        } finally {
            Database.closeObjects(GenericConfigurationImpl.class, conn, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T> Collection<String> getKeys(Parameter<T> parameter) throws FxApplicationException {
        return getAll(parameter).keySet();
    }

    /**
     * {@inheritDoc}
     */
    public <T> void remove(Parameter<T> parameter, String key)
            throws FxApplicationException {
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = getDeleteStatement(conn, parameter.getPath().getValue(), key);
            stmt.executeUpdate();
            String cachePath = getCachePath(parameter.getPath().getValue());
            if (cachePath != null) {
                // also remove from cache
                if (key == null) {
                    // clear entire cache path
                    deleteCache(cachePath);
                } else {
                    // clear single value
                    deleteCache(cachePath, key);
                }
            }
        } catch (SQLException e) {
            throw new FxRemoveException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(GenericConfigurationImpl.class, conn, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    public <T> void remove(Parameter<T> parameter)
            throws FxApplicationException {
        remove(parameter, parameter.getKey());
    }

    /**
     * {@inheritDoc}
     */
    public <T> void removeAll(Parameter<T> parameter)
            throws FxApplicationException {
        remove(parameter, null);
    }

    /**
     * Loads the given parameter from the database. Helper method for implementors.
     *
     * @param parameter the parameter to be loaded
     * @param <T>       value type of the parameter
     * @return the parameter value
     * @throws FxNotFoundException if the parameter does not exist
     * @throws FxLoadException     if the parameter could not be loaded
     */
    protected <T> T loadParameterFromDb(Parameter<T> parameter) throws FxNotFoundException, FxLoadException {
        return parameter.getValue(loadParameterFromDb(parameter.getPath().getValue(),
                parameter.getData().getKey()));
    }

    /**
     * Loads the given parameter from the database. Helper method for implementors.
     *
     * @param path path of the parameter
     * @param key  key of the parameter
     * @return the parameter value
     * @throws FxLoadException     if the parameter could not be loaded
     * @throws FxNotFoundException if the parameter does not exist
     */
    protected Serializable loadParameterFromDb(String path, String key) throws FxLoadException, FxNotFoundException {
        // get from DB
        Connection conn = null;
        PreparedStatement stmt = null;
        try {
            conn = getConnection();
            stmt = getSelectStatement(conn, path, key);
            ResultSet rs = stmt.executeQuery();
            if (rs != null && rs.next()) {
                return rs.getString(1);
            } else {
                String cachePath = getCachePath(path);
                if (cachePath != null) {
                    // store null object in cache to avoid hitting the DB every time
                    putCache(cachePath, key, new UnsetParameter());
                }
                throw new FxNotFoundException("ex.configuration.parameter.notfound", path, key);
            }
        } catch (SQLException se) {
            throw new FxLoadException(LOG, se, "ex.db.sqlError", se.getMessage());
        } finally {
            Database.closeObjects(GenericConfigurationImpl.class, conn, stmt);
        }
    }


    /**
     * Store the given value in the cache.
     *
     * @param path  the parameter path
     * @param key   the parameter key
     * @param value the serializable value to be stored
     */
    protected void putCache(String path, String key, Serializable value) {
        try {
            CacheAdmin.getInstance().put(path, key, value);
        } catch (FxCacheException e) {
            LOG.error("Failed to update cache (ignored): " + e.getMessage());
        }
    }

    /**
     * Delete the given parameter from the cache
     *
     * @param path path of the parameter to be removed
     * @param key  key  of the parameter to be removed
     */
    protected void deleteCache(String path, String key) {
        try {
            CacheAdmin.getInstance().remove(path, key);
        } catch (FxCacheException e) {
            LOG.error("Failed to update cache (ignored): " + e.getMessage());
        }
    }

    /**
     * Delete the given path from the cache
     *
     * @param path the path to be removed
     */
    protected void deleteCache(String path) {
        try {
            CacheAdmin.getInstance().remove(path);
        } catch (FxCacheException e) {
            LOG.error("Failed to update cache (ignored): " + e.getMessage());
        }
    }

    /**
     * Returns the cached value of the given parameter
     *
     * @param path the parameter path
     * @param key  the parameter key
     * @return the cached value of the given parameter
     * @throws FxCacheException if a cache exception occured
     */
    protected Serializable getCache(String path, String key) throws FxCacheException {
        return (Serializable) SerializationUtils.clone((Serializable) CacheAdmin.getInstance().get(path, key));
    }
}
