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

import com.flexive.core.Database;
import com.flexive.core.search.DataFilter;
import com.flexive.core.search.DataSelector;
import com.flexive.core.search.H2.H2SQLDataFilter;
import com.flexive.core.search.H2.H2SQLDataSelector;
import com.flexive.core.search.SqlSearch;
import com.flexive.core.search.genericSQL.GenericSQLDataFilter;
import com.flexive.core.search.genericSQL.GenericSQLDataSelector;
import com.flexive.core.storage.h2.H2EnvironmentLoader;
import com.flexive.core.storage.h2.H2HierarchicalStorage;
import com.flexive.core.storage.h2.H2SequencerStorage;
import com.flexive.core.storage.h2.H2TreeStorage;
import com.flexive.core.storage.mySQL.MySQLEnvironmentLoader;
import com.flexive.core.storage.mySQL.MySQLHierarchicalStorage;
import com.flexive.core.storage.mySQL.MySQLSequencerStorage;
import com.flexive.core.storage.mySQL.MySQLTreeStorage;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.DBVendor;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.structure.TypeStorageMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton Facade for the various storage implementations
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class StorageManager {
    private static final Log LOG = LogFactory.getLog(StorageManager.class);

    /**
     * Get concrete content storage implementation for the given type storage mode
     *
     * @param mode used storage mode
     * @return DataSelector for the given storage mode
     * @throws FxNotFoundException if no implementation was found
     */
    public static ContentStorage getContentStorage(TypeStorageMode mode) throws FxNotFoundException {
        DBVendor vendor;
        vendor = FxContext.get().getDivisionData().getDbVendor();
        switch (mode) {
            case Hierarchical:
                switch (vendor) {
                    case MySQL:
                        return MySQLHierarchicalStorage.getInstance();
                    case H2:
                        return H2HierarchicalStorage.getInstance();
                    default:
                        throw new FxNotFoundException("ex.db.contentStorage.undefined", vendor, mode);
                }
            default:
                throw new FxNotFoundException("ex.structure.typeStorageMode.notImplemented", mode);
        }
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
        switch (mode) {
            case Hierarchical:
                switch (data.getDbVendor()) {
                    case MySQL:
                        return MySQLHierarchicalStorage.getInstance();
                    case H2:
                        return H2HierarchicalStorage.getInstance();
                    default:
                        throw new FxNotFoundException("ex.db.contentStorage.undefined", data.getDbVendor(), mode);
                }
            default:
                throw new FxNotFoundException("ex.structure.typeStorageMode.notImplemented", mode);
        }
    }

    /**
     * Get concrete tree storage implementation for the used database
     *
     * @return TreeStorage
     * @throws FxNotFoundException if no implementation was found
     */
    public static TreeStorage getTreeStorage() throws FxNotFoundException {
        DBVendor vendor;
        vendor = FxContext.get().getDivisionData().getDbVendor();
        switch (vendor) {
            case MySQL:
                return MySQLTreeStorage.getInstance();
            case H2:
                return H2TreeStorage.getInstance();
            default:
                throw new FxNotFoundException("ex.db.treeStorage.undefined", vendor);
        }
    }

    /**
     * Get a concrete environment loader instance for a division
     *
     * @param dd DivisionData for the requested Division
     * @return EnvironmentLoader instance
     * @throws FxNotFoundException if no implementation was found
     */
    public static EnvironmentLoader getEnvironmentLoader(DivisionData dd) throws FxNotFoundException {
        DBVendor vendor = dd.getDbVendor();
        switch (vendor) {
            case MySQL:
                return MySQLEnvironmentLoader.getInstance();
            case H2:
                return H2EnvironmentLoader.getInstance();
            default:
                throw new FxNotFoundException("ex.db.environmentLoader.undefined", vendor);
        }
    }

    /**
     * Get concrete tree storage implementation for the used database
     *
     * @return TreeStorage
     * @throws FxNotFoundException if no implementation was found
     */
    public static SequencerStorage getSequencerStorage() throws FxNotFoundException {
        DBVendor vendor;
        vendor = FxContext.get().getDivisionData().getDbVendor();
        switch (vendor) {
            case MySQL:
                return MySQLSequencerStorage.getInstance();
            case H2:
                return H2SequencerStorage.getInstance();
            default:
                throw new FxNotFoundException("ex.db.treeStorage.undefined", vendor);
        }
    }

    /**
     * Get a database vendor specific timestamp of the current time in milliseconds as Long
     *
     * @return database vendor specific timestamp of the current time in milliseconds as Long
     */
    public static String getTimestampFunction() {
        switch (FxContext.get().getDivisionData().getDbVendor()) {
            case MySQL:
                return "UNIX_TIMESTAMP()*1000";
            case H2:
                return "TIMEMILLIS(NOW())";
        }
        //default fallback
        return "UNIX_TIMESTAMP()*1000";
    }

    /**
     * Get a database vendor specific "IF" function
     *
     * @return database vendor specific "IF" function
     */
    public static String getIfFunction() {
        switch (FxContext.get().getDivisionData().getDbVendor()) {
            case MySQL:
                return "IF";
            case H2:
                return "CASEWHEN";
        }
        //default fallback
        return "IF";
    }

    /**
     * Get the database vendor specific statement to enable or disable referential integrity checks
     *
     * @param enable enable or disable checks?
     * @return database vendor specific statement to enable or disable referential integrity checks
     */
    public static String getReferentialIntegrityChecksStatement(boolean enable) {
        switch (FxContext.get().getDivisionData().getDbVendor()) {
            case MySQL:
                return "SET FOREIGN_KEY_CHECKS=" + (enable ? 1 : 0);
            case H2:
                return "SET REFERENTIAL_INTEGRITY " + (enable ? "TRUE" : "FALSE");
        }
        //default fallback: nothing
        return "";
    }

    /**
     * Returns true if the SqlError is a unique constraint violation.
     *
     * @param exc the exception
     * @return true if the SqlError is a unique constraint violation
     */
    public static boolean isUniqueConstraintViolation(Exception exc) {
        final int sqlErr = Database.getSqlErrorCode(exc);
        switch (FxContext.get().getDivisionData().getDbVendor()) {
            case MySQL:
                //see http://dev.mysql.com/doc/refman/5.1/en/error-messages-server.html
                // 1582 Example error: Duplicate entry 'ABSTRACT' for key 'UK_TYPEPROPS_NAME'
                return (sqlErr == 1062 || sqlErr == 1582);
            case H2:
                return sqlErr == 23001;

        }

        // final String sMsg = (exc.getMessage() == null) ? "" : exc.getMessage().toLowerCase();

        // Oracle:
        // msg: "unique constraint (XXXX) violated"
        // return sMsg.indexOf("unique constraint") != -1 && sMsg.indexOf("violated") != -1;

        // MySQL5:
        // msg="Duplicate key or integrity constraint violation message from server: "Cannot delete or update a
        // parent row: a foreign key constraint fails"
        // SQLState: 23000
        return ((SQLException) exc).getSQLState().equalsIgnoreCase("23000");
    }

    /**
     * Returns true if the SqlError is a foreign key violation.
     *
     * @param exc the exception
     * @return true if the SqlError is a foreign key violation
     */
    public static boolean isForeignKeyViolation(Exception exc) {
        final int errorCode = Database.getSqlErrorCode(exc);
        switch (FxContext.get().getDivisionData().getDbVendor()) {
            case MySQL:
                //see http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html
                return errorCode == 1451 || errorCode == 1217;
            case H2:
                //see http://h2database.com/javadoc/org/h2/constant/ErrorCode.html#c23002
                return errorCode == 23002 || errorCode == 23003;
            default:
                return false;
        }
    }

    /**
     * Returns true if the given exception was caused by a query timeout.
     *
     * @param e the exception to be examined
     * @return true if the given exception was caused by a query timeout
     * @since 3.1
     */
    public static boolean isQueryTimeout(Exception e) {
        final int errorCode = Database.getSqlErrorCode(e);
        switch (FxContext.get().getDivisionData().getDbVendor()) {
            case MySQL:
                //see http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html
                return errorCode == 1317 || errorCode == 1028
                        || e.getClass().getName().equals("com.mysql.jdbc.exceptions.MySQLTimeoutException");
            case H2:
                //see http://h2database.com/javadoc/org/h2/constant/ErrorCode.html#c23002
                return errorCode == 90051;
            default:
                return false;
        }
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
        final DBVendor vendor = FxContext.get().getDivisionData().getDbVendor();
        switch (vendor) {
            case MySQL:
                return new GenericSQLDataFilter(con, sqlSearch);
            case H2:
                return new H2SQLDataFilter(con, sqlSearch);
            default:
                throw new FxSqlSearchException(LOG, "ex.db.filter.undefined", vendor);
        }
    }

    /**
     * Get the DataSelector for the sql searchengine based on the used DB
     *
     * @param sqlSearch the sql search instance to operate on
     * @return DataSelector the data selecttor implementation
     * @throws FxSqlSearchException if the function fails
     */
    public static DataSelector getDataSelector(SqlSearch sqlSearch) throws FxSqlSearchException {
        final DBVendor vendor = FxContext.get().getDivisionData().getDbVendor();
        switch (vendor) {
            case H2:
                return new H2SQLDataSelector(sqlSearch);
            case MySQL:
                return new GenericSQLDataSelector(sqlSearch);
            default:
                throw new FxSqlSearchException(LOG, "ex.db.selector.undefined", vendor);
        }
    }

}
