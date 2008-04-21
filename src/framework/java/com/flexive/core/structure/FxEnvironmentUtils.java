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
package com.flexive.core.structure;

import com.flexive.core.DatabaseConst;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.interfaces.GlobalConfigurationEngine;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Environment helper functions (core)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
final class FxEnvironmentUtils {
    private static transient Log LOG = LogFactory.getLog(FxEnvironmentUtils.class);

    /**
     * Includes the division id into the path.
     *
     * @param divisionId the division
     * @param path       the path to encode
     * @return the encoded path
     * @throws FxCacheException if the division id could not be resolved
     */
    private static String divisionEncodePath(int divisionId, final String path) throws FxCacheException {
        if (!DivisionData.isValidDivisionId(divisionId)) {
            throw new FxCacheException("Division ID missing");
        }
        return "#" + divisionId + (path.startsWith("/") ? "" : "/") + path;
//        return "/Division" + divisionId + (path.startsWith("/") ? "" : "/") + path;
    }

    /**
     * Get an object from the cache
     *
     * @param divisionId division
     * @param path       path
     * @param key        key
     * @return requested object or <code>null</code> if not found
     * @throws FxCacheException on errors
     */
    protected static Object cacheGet(int divisionId, String path, Object key) throws FxCacheException {
        return CacheAdmin.getInstance().get(divisionEncodePath(divisionId, path), key);
    }

    /**
     * Put an object into the cache
     *
     * @param divisionId division
     * @param path       path
     * @param key        key
     * @param value      value
     * @throws FxCacheException on errors
     */
    protected static void cachePut(int divisionId, String path, Object key, Object value) throws FxCacheException {
        if (CacheAdmin.ENVIRONMENT_RUNTIME.equals(key)) {
            CacheAdmin.environmentChanged();
        }
        CacheAdmin.getInstance().put(divisionEncodePath(divisionId, path), key, value);
    }

    /**
     * Remove a path from the cache
     *
     * @param divisionId division
     * @param path       path
     * @throws FxCacheException on errors
     */
    protected static void cacheRemove(int divisionId, String path) throws FxCacheException {
        CacheAdmin.getInstance().remove(divisionEncodePath(divisionId, path));
    }

    /**
     * Remove an object from the cache
     *
     * @param divisionId the division
     * @param path       path in cache
     * @param key        key
     * @throws FxCacheException on errors
     */
    protected static void cacheRemove(int divisionId, String path, String key) throws FxCacheException {
        CacheAdmin.getInstance().remove(divisionEncodePath(divisionId, path), key);
    }

    /**
     * Get a DB Connection for the given division
     *
     * @param divisionId the division
     * @return connection
     * @throws SQLException on errors
     */
    protected static Connection getDbConnection(int divisionId) throws SQLException {
        // Check division
        if (!DivisionData.isValidDivisionId(divisionId)) {
            throw new SQLException("Unable to obtain connection: Division not defined");
        }
        // Try to obtain a connection
        String finalDsName = null;
        try {
            Context c = EJBLookup.getInitialContext();
            if (divisionId == DivisionData.DIVISION_GLOBAL) {
                // Special case: global config database
                finalDsName = DatabaseConst.DS_GLOBAL_CONFIG;
            } else {
                // else: get data source from global configuration
                GlobalConfigurationEngine globalConfiguration = EJBLookup.getGlobalConfigurationEngine();
                finalDsName = globalConfiguration.getDivisionData(divisionId).getDataSource();
            }
            try {
                return ((DataSource) c.lookup(finalDsName)).getConnection();
            } catch (NamingException e) {
                //last exit: geronimo global scope
                String name = finalDsName;
                if (name.startsWith("jdbc/"))
                    name = name.substring(5);
                Object o = c.lookup("jca:/console.dbpool/" + name + "/JCAManagedConnectionFactory/" + name);
                try {
                    return ((DataSource) o.getClass().getMethod("$getResource").invoke(o)).getConnection();
                } catch (Exception e1) {
                    String sErr = "Failed to load datasource: " + e1.getMessage();
                    LOG.error(sErr);
                    throw new SQLException(sErr);
                }
            }
        } catch (NamingException exc) {
            String sErr = "Naming Exception, unable to retrieve Connection to [" + finalDsName +
                    "]: " + exc.getMessage();
            LOG.error(sErr);
            throw new SQLException(sErr);
        } catch (FxNotFoundException exc) {
            String sErr = "Failed to retrieve datasource for division " + divisionId + " (not configured).";
            LOG.error(sErr);
            throw new SQLException(sErr);
        } catch (FxApplicationException exc) {
            String sErr = "Failed to load datasource: " + exc.getMessage();
            LOG.error(sErr);
            throw new SQLException(sErr);
        }
    }
}
