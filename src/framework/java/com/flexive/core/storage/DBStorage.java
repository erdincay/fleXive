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
import com.flexive.core.search.cmis.impl.sql.SqlDialect;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.structure.TypeStorageMode;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.interfaces.ContentEngine;

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
    public String getStorageVendor();

    /**
     * Can this storage handle the requested database?
     *
     * @param dbm database meta data
     * @return if storage can handle the requested database
     */
    public boolean canHandle(DatabaseMetaData dbm);

    /**
     * Get the ContentStorage singleton instance
     *
     * @param mode used storage mode
     * @return ContentStorage singleton instance
     * @throws FxNotFoundException if no implementation was found
     */
    public ContentStorage getContentStorage(TypeStorageMode mode) throws FxNotFoundException;

    /**
     * Get the EnvironmentLoader singleton instance
     *
     * @return EnvironmentLoader singleton instance
     */
    public EnvironmentLoader getEnvironmentLoader();

    /**
     * Get the SequencerStorage singleton instance
     *
     * @return SequencerStorage singleton instance
     */
    public SequencerStorage getSequencerStorage();

    /**
     * Get the TreeStorage singleton instance
     *
     * @return TreeStorage singleton instance
     */
    public TreeStorage getTreeStorage();

    /**
     * Get a data selector for a sql search
     *
     * @param search current SqlSearch to operate on
     * @return data selector
     * @throws FxSqlSearchException on errors
     */
    public DataSelector getDataSelector(SqlSearch search) throws FxSqlSearchException;

    /**
     * Get a data filter for a sql search
     *
     * @param con    an open and valid connection
     * @param search current SqlSearch to operate on
     * @return DataFilter
     * @throws FxSqlSearchException on errors
     */
    public DataFilter getDataFilter(Connection con, SqlSearch search) throws FxSqlSearchException;

    /**
     * Get the CMIS SQL Dialect implementation
     *
     * @param environment      environment
     * @param contentEngine    content engine in use
     * @param query            query
     * @param returnPrimitives return primitives?
     * @return CMIS SQL Dialect implementation
     */
    public SqlDialect getCmisSqlDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives);

    /**
     * Get a database vendor specific "IF" function
     *
     * @return database vendor specific "IF" function
     */
    public String getIfFunction();

    /**
     * Get the database vendor specific statement to enable or disable referential integrity checks
     *
     * @param enable enable or disable checks?
     * @return database vendor specific statement to enable or disable referential integrity checks
     */
    public String getReferentialIntegrityChecksStatement(boolean enable);

    /**
     * Get the sql code of the statement to fix referential integrity when removing selectlist items
     *
     * @return sql code of the statement to fix referential integrity when removing selectlist items
     */
    public String getSelectListItemReferenceFixStatement();

    /**
     * Get a database vendor specific timestamp of the current time in milliseconds as Long
     *
     * @return database vendor specific timestamp of the current time in milliseconds as Long
     */
    public String getTimestampFunction();

    /**
     * Returns true if the SqlError is a foreign key violation.
     *
     * @param exc the exception
     * @return true if the SqlError is a foreign key violation
     */
    public boolean isForeignKeyViolation(Exception exc);

    /**
     * Returns true if the given exception was caused by a query timeout.
     *
     * @param e the exception to be examined
     * @return true if the given exception was caused by a query timeout
     * @since 3.1
     */
    public boolean isQueryTimeout(Exception e);

    /**
     * Returns true if the SqlError is a unique constraint violation.
     *
     * @param exc the exception
     * @return true if the SqlError is a unique constraint violation
     */
    public boolean isUniqueConstraintViolation(Exception exc);
}
