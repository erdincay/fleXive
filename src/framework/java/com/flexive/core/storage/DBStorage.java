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
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.TypeStorageMode;

import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Database vendor specific storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface DBStorage {

    /**
     * Get the name of the storage vendor (DB vendor name)
     *
     * @return name of the storage vendor (DB vendor name)
     */
    String getStorageVendor();

    /**
     * Can this storage handle the requested database?
     *
     * @param dbm database meta data
     * @return if storage can handle the requested database
     */
    boolean canHandle(DatabaseMetaData dbm);

    /**
     * Get the ContentStorage singleton instance
     *
     * @param mode used storage mode
     * @return ContentStorage singleton instance
     * @throws FxNotFoundException if no implementation was found
     */
    ContentStorage getContentStorage(TypeStorageMode mode) throws FxNotFoundException;

    /**
     * Get the EnvironmentLoader singleton instance
     *
     * @return EnvironmentLoader singleton instance
     */
    EnvironmentLoader getEnvironmentLoader();

    /**
     * Get the SequencerStorage singleton instance
     *
     * @return SequencerStorage singleton instance
     */
    SequencerStorage getSequencerStorage();

    /**
     * Get the TreeStorage singleton instance
     *
     * @return TreeStorage singleton instance
     */
    TreeStorage getTreeStorage();

    /**
     * Get the LockStorage singleton instance
     *
     * @return LockStorage singleton instance
     */
    LockStorage getLockStorage();

    /**
     * Get a data selector for a sql search
     *
     * @param search current SqlSearch to operate on
     * @return data selector
     * @throws FxSqlSearchException on errors
     */
    DataSelector getDataSelector(SqlSearch search) throws FxSqlSearchException;

    /**
     * Get a data filter for a sql search
     *
     * @param con    an open and valid connection
     * @param search current SqlSearch to operate on
     * @return DataFilter
     * @throws FxSqlSearchException on errors
     */
    DataFilter getDataFilter(Connection con, SqlSearch search) throws FxSqlSearchException;

    /**
     * Get the CMIS SQL Dialect implementation
     *
     * @param environment      environment
     * @param contentEngine    content engine in use
     * @param query            query
     * @param returnPrimitives return primitives?
     * @return CMIS SQL Dialect implementation
     */
    SqlDialect getCmisSqlDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives);

    /**
     * Get a database vendor specific "IF" function
     *
     * @return database vendor specific "IF" function
     */
    String getIfFunction();

    /**
     * Get the database vendor specific statement to enable or disable referential integrity checks
     *
     * @param enable enable or disable checks?
     * @return database vendor specific statement to enable or disable referential integrity checks
     */
    String getReferentialIntegrityChecksStatement(boolean enable);

    /**
     * Get the sql code of the statement to fix referential integrity when removing selectlist items
     *
     * @return sql code of the statement to fix referential integrity when removing selectlist items
     */
    String getSelectListItemReferenceFixStatement();

    /**
     * Get a database vendor specific timestamp of the current time in milliseconds as Long
     *
     * @return database vendor specific timestamp of the current time in milliseconds as Long
     */
    String getTimestampFunction();

    /**
     * Returns true if the SqlError is a foreign key violation.
     *
     * @param exc the exception
     * @return true if the SqlError is a foreign key violation
     */
    boolean isForeignKeyViolation(Exception exc);

    /**
     * Returns true if the given exception was caused by a query timeout.
     *
     * @param e the exception to be examined
     * @return true if the given exception was caused by a query timeout
     * @since 3.1
     */
    boolean isQueryTimeout(Exception e);

    /**
     * Returns true if the SqlError is a unique constraint violation.
     *
     * @param exc the exception
     * @return true if the SqlError is a unique constraint violation
     */
    boolean isUniqueConstraintViolation(Exception exc);

    /**
     * When the database schema is used in queries, do we have to escape it? (eg postgres requires this)
     *
     * @return database schema needs to be escaped
     */
    boolean escapeSchema();

    /**
     * Get a connection to the database using provided parameters and (re)create the database and/or schema
     *
     * @param database          name of the database
     * @param schema            name of the schema
     * @param jdbcURL           JDBC connect URL
     * @param jdbcURLParameters optional JDBC URL parameters
     * @param user              name of the db user
     * @param password          password
     * @param createDB          create the database?
     * @param createSchema      create the schema?
     * @param dropIfExist       drop the database if it exists?
     * @return an open connection to the database with the schema set as default
     * @throws Exception on errors
     */
    Connection getConnection(String database, String schema, String jdbcURL, String jdbcURLParameters, String user, String password, boolean createDB, boolean createSchema, boolean dropIfExist) throws Exception;

    /**
     * Initialize a configuration schema
     *
     * @param con         an open and valid connection to the database
     * @param schema      the schema to create
     * @param dropIfExist drop the schema if it exists?
     * @return success
     * @throws Exception on errors
     */
    boolean initConfiguration(Connection con, String schema, boolean dropIfExist) throws Exception;

    /**
     * Initialize a division
     *
     * @param con         an open and valid connection to the database
     * @param schema      the schema to create
     * @param dropIfExist drop the schema if it exists?
     * @return success
     * @throws Exception on errors
     */
    boolean initDivision(Connection con, String schema, boolean dropIfExist) throws Exception;
}
