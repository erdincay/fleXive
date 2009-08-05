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
package com.flexive.core.storage;

import com.flexive.core.search.DataFilter;
import com.flexive.core.search.DataSelector;
import com.flexive.core.search.SqlSearch;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.sql.SqlDialect;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.exceptions.FxDbException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.TypeStorageMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton Facade for the various storage implementations
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class StorageManager {
    private static final Log LOG = LogFactory.getLog(StorageManager.class);

    /**
     * Mapping vendor -> storage implementation
     */
    private static final Map<String, DBStorage> storages = new HashMap<String, DBStorage>(10);

    static {
        LOG.info("Scanning available storage implementations ...");
        for (String storage : FxSharedUtils.getStorageImplementations()) {
            try {
                DBStorage impl = (DBStorage) Class.forName(storage).newInstance();
                LOG.info("Adding storage for vendor [" + impl.getStorageVendor() + "]");
                storages.put(impl.getStorageVendor(), impl);
            } catch (Exception e) {
                LOG.error("Could not instantiate storage factory class [" + storage + "]: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Get the storage implementation for the db vendor of the current division
     *
     * @return storage implementation for the db vendor of the current division
     */
    public static DBStorage getStorageImpl() {
        DBStorage storage = storages.get(FxContext.get().getDivisionData().getDbVendor());
        if (storage == null)
            //noinspection ThrowableInstanceNeverThrown
            throw new FxDbException("ex.db.storage.undefined", FxContext.get().getDivisionData().getDbVendor()).asRuntimeException();
        return storage;
    }

    /**
     * Get the storage implementation for the db vendor of the requested division data
     *
     * @param data division data to get the db vendor from
     * @return storage implementation for the db vendor of the requested division data
     */
    private static DBStorage getStorageImpl(DivisionData data) {
        DBStorage storage = storages.get(data.getDbVendor());
        if (storage == null)
            //noinspection ThrowableInstanceNeverThrown
            throw new FxDbException("ex.db.storage.undefined", data.getDbVendor()).asRuntimeException();
        return storage;
    }

    /**
     * Get concrete content storage implementation for the given type storage mode
     *
     * @param mode used storage mode
     * @return DataSelector for the given storage mode
     * @throws FxNotFoundException if no implementation was found
     */
    public static ContentStorage getContentStorage(TypeStorageMode mode) throws FxNotFoundException {
        return getStorageImpl().getContentStorage(mode);
    }

    /**
     * Get concrete content storage implementation for the given type storage mode.
     * This variant is only to be used from a division free context like in a timer service or mbean
     *
     * @param data DivisionData
     * @param mode used storage mode
     * @return DataSelector for the given storage mode
     * @throws FxNotFoundException if no implementation was found
     */
    public static ContentStorage getContentStorage(DivisionData data, TypeStorageMode mode) throws FxNotFoundException {
        return getStorageImpl(data).getContentStorage(mode);
    }

    /**
     * Get concrete tree storage implementation for the used database
     *
     * @return TreeStorage
     * @throws FxNotFoundException if no implementation was found
     */
    public static TreeStorage getTreeStorage() throws FxNotFoundException {
        return getStorageImpl().getTreeStorage();
    }

    /**
     * Get a concrete environment loader instance for a division
     *
     * @param dd DivisionData for the requested Division
     * @return EnvironmentLoader instance
     * @throws FxNotFoundException if no implementation was found
     */
    public static EnvironmentLoader getEnvironmentLoader(DivisionData dd) throws FxNotFoundException {
        return getStorageImpl(dd).getEnvironmentLoader();
    }

    /**
     * Get concrete tree storage implementation for the used database
     *
     * @return TreeStorage
     * @throws FxNotFoundException if no implementation was found
     */
    public static SequencerStorage getSequencerStorage() throws FxNotFoundException {
        return getStorageImpl().getSequencerStorage();
    }

    /**
     * Get the CMIS SQL Dialect implementation
     *
     * @param environment      environment
     * @param contentEngine    content engine in use
     * @param query            query
     * @param returnPrimitives return primitives?
     * @return CMIS SQL Dialect implementation
     */
    public static SqlDialect getCmisSqlDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives) {
        return getStorageImpl().getCmisSqlDialect(environment, contentEngine, query, returnPrimitives);
    }

    /**
     * Get a database vendor specific timestamp of the current time in milliseconds as Long
     *
     * @return database vendor specific timestamp of the current time in milliseconds as Long
     */
    public static String getTimestampFunction() {
        return getStorageImpl().getTimestampFunction();
    }

    /**
     * Get a database vendor specific "IF" function
     *
     * @return database vendor specific "IF" function
     */
    public static String getIfFunction() {
        return getStorageImpl().getIfFunction();
    }

    /**
     * Get the database vendor specific statement to enable or disable referential integrity checks
     *
     * @param enable enable or disable checks?
     * @return database vendor specific statement to enable or disable referential integrity checks
     */
    public static String getReferentialIntegrityChecksStatement(boolean enable) {
        return getStorageImpl().getReferentialIntegrityChecksStatement(enable);
    }

    /**
     * Get the sql code of the statement to fix referential integrity when removing selectlist items
     *
     * @return sql code of the statement to fix referential integrity when removing selectlist items
     */
    public static String getSelectListItemReferenceFixStatement() {
        return getStorageImpl().getSelectListItemReferenceFixStatement();
    }

    /**
     * Returns true if the SqlError is a unique constraint violation.
     *
     * @param exc the exception
     * @return true if the SqlError is a unique constraint violation
     */
    public static boolean isUniqueConstraintViolation(Exception exc) {
        return getStorageImpl().isUniqueConstraintViolation(exc);
    }

    /**
     * Returns true if the SqlError is a foreign key violation.
     *
     * @param exc the exception
     * @return true if the SqlError is a foreign key violation
     */
    public static boolean isForeignKeyViolation(Exception exc) {
        return getStorageImpl().isForeignKeyViolation(exc);
    }

    /**
     * Returns true if the given exception was caused by a query timeout.
     *
     * @param e the exception to be examined
     * @return true if the given exception was caused by a query timeout
     * @since 3.1
     */
    public static boolean isQueryTimeout(Exception e) {
        return getStorageImpl().isQueryTimeout(e);
    }

    /**
     * Get the DataSelector for the sql searchengine based on the used DB
     *
     * @param con       the connection to use
     * @param sqlSearch the sql search instance to operate on
     * @return DataSelector the data selecttor implementation
     * @throws FxSqlSearchException if the function fails
     */
    public static DataFilter getDataFilter(Connection con, SqlSearch sqlSearch) throws FxSqlSearchException {
        return getStorageImpl().getDataFilter(con, sqlSearch);
    }

    /**
     * Get the DataSelector for the sql searchengine based on the used DB
     *
     * @param sqlSearch the sql search instance to operate on
     * @return DataSelector the data selecttor implementation
     * @throws FxSqlSearchException if the function fails
     */
    public static DataSelector getDataSelector(SqlSearch sqlSearch) throws FxSqlSearchException {
        return getStorageImpl().getDataSelector(sqlSearch);
    }
}
