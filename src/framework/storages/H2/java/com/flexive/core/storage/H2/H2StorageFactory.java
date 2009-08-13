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
package com.flexive.core.storage.H2;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_SELECTLIST_ITEM;
import com.flexive.core.search.DataFilter;
import com.flexive.core.search.DataSelector;
import com.flexive.core.search.H2.H2SQLDataFilter;
import com.flexive.core.search.H2.H2SQLDataSelector;
import com.flexive.core.search.SqlSearch;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.sql.H2.H2Dialect;
import com.flexive.core.search.cmis.impl.sql.SqlDialect;
import com.flexive.core.storage.*;
import com.flexive.core.storage.genericSQL.GenericLockStorage;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.TypeStorageMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * Factory for the MySQL storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class H2StorageFactory implements DBStorage {
    private static final Log LOG = LogFactory.getLog(H2StorageFactory.class);
    private final static String VENDOR = "H2";

    /**
     * {@inheritDoc}
     */
    public String getStorageVendor() {
        return VENDOR;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canHandle(DatabaseMetaData dbm) {
        try {
            return VENDOR.equals(dbm.getDatabaseProductName());
        } catch (SQLException e) {
            LOG.error(e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ContentStorage getContentStorage(TypeStorageMode mode) throws FxNotFoundException {
        if (mode == TypeStorageMode.Hierarchical)
            return H2HierarchicalStorage.getInstance();
        throw new FxNotFoundException("ex.structure.typeStorageMode.notImplemented", mode);
    }

    /**
     * {@inheritDoc}
     */
    public EnvironmentLoader getEnvironmentLoader() {
        return H2EnvironmentLoader.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public SequencerStorage getSequencerStorage() {
        return H2SequencerStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public TreeStorage getTreeStorage() {
        return H2TreeStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public LockStorage getLockStorage() {
        return GenericLockStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public DataSelector getDataSelector(SqlSearch search) throws FxSqlSearchException {
        return new H2SQLDataSelector(search);
    }

    /**
     * {@inheritDoc}
     */
    public DataFilter getDataFilter(Connection con, SqlSearch search) throws FxSqlSearchException {
        return new H2SQLDataFilter(con, search);
    }

    /**
     * {@inheritDoc}
     */
    public SqlDialect getCmisSqlDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives) {
        return new H2Dialect(environment, contentEngine, query, returnPrimitives);
    }

    /**
     * {@inheritDoc}
     */
    public String getIfFunction() {
        return "CASEWHEN";
    }

    /**
     * {@inheritDoc}
     */
    public String getReferentialIntegrityChecksStatement(boolean enable) {
        return "SET REFERENTIAL_INTEGRITY " + (enable ? "TRUE" : "FALSE");
    }

    /**
     * {@inheritDoc}
     */
    public String getSelectListItemReferenceFixStatement() {
        return "UPDATE " + TBL_SELECTLIST_ITEM + " SET PARENTID=? WHERE PARENTID IN (SELECT p.ID FROM " +
                TBL_SELECTLIST_ITEM + " p WHERE p.LISTID=?)";
    }

    /**
     * {@inheritDoc}
     */
    public String getTimestampFunction() {
        return "TIMEMILLIS(NOW())";
    }

    /**
     * {@inheritDoc}
     */
    public boolean isForeignKeyViolation(Exception exc) {
        final int errorCode = Database.getSqlErrorCode(exc);
        //see http://h2database.com/javadoc/org/h2/constant/ErrorCode.html#c23002
        return errorCode == 23002 || errorCode == 23003;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isQueryTimeout(Exception e) {
        final int errorCode = Database.getSqlErrorCode(e);
        //see http://h2database.com/javadoc/org/h2/constant/ErrorCode.html
        return errorCode == 90051;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUniqueConstraintViolation(Exception exc) {
        final int sqlErr = Database.getSqlErrorCode(exc);
        //see http://h2database.com/javadoc/org/h2/constant/ErrorCode.html
        return sqlErr == 23001;
    }
}
