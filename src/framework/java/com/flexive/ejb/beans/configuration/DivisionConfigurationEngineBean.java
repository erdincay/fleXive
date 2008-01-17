/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.ejb.beans.configuration;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_DIVISION_CONFIG;
import com.flexive.core.configuration.GenericConfigurationImpl;
import com.flexive.core.storage.ContentStorage;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxDbException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.interfaces.DivisionConfigurationEngine;
import com.flexive.shared.interfaces.DivisionConfigurationEngineLocal;
import com.flexive.shared.structure.TypeStorageMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Division configuration implementation.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

@TransactionManagement(TransactionManagementType.CONTAINER)
@Stateless(name = "DivisionConfigurationEngine")
public class DivisionConfigurationEngineBean extends GenericConfigurationImpl implements DivisionConfigurationEngine, DivisionConfigurationEngineLocal {
    private static final transient Log LOG = LogFactory.getLog(DivisionConfigurationEngineBean.class);
    /**
     * Division config cache root.
     */
    private static final String CACHE_ROOT = "/divisionConfig";

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
    protected PreparedStatement getInsertStatement(Connection conn, String path, String key, String value)
            throws SQLException, FxNoAccessException {
        if (!FxContext.get().getTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.configuration.update.perm.division");
        }
        String sql = "INSERT INTO " + TBL_DIVISION_CONFIG + " (cpath, ckey, cvalue) VALUES (?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, path);
        stmt.setString(2, key);
        stmt.setString(3, value);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getSelectStatement(Connection conn, String path, String key) throws SQLException {
        String sql = "SELECT cvalue FROM " + TBL_DIVISION_CONFIG + " WHERE cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, path);
        stmt.setString(2, key);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getSelectStatement(Connection conn, String path) throws SQLException {
        String sql = "SELECT ckey, cvalue FROM " + TBL_DIVISION_CONFIG + " WHERE cpath=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, path);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getUpdateStatement(Connection conn, String path, String key, String value)
            throws SQLException, FxNoAccessException {
        if (!FxContext.get().getTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.configuration.update.perm.division");
        }
        String sql = "UPDATE " + TBL_DIVISION_CONFIG + " SET cvalue=? WHERE cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, value);
        stmt.setString(2, path);
        stmt.setString(3, key);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getDeleteStatement(Connection conn, String path, String key)
            throws SQLException, FxNoAccessException {
        if (!FxContext.get().getTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.configuration.delete.perm.division");
        }
        String sql = "DELETE FROM " + TBL_DIVISION_CONFIG + " WHERE cpath=? "
                + (key != null ? " AND ckey=?" : "");
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, path);
        if (key != null) {
            stmt.setString(2, key);
        }
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCachePath(String path) {
        return CACHE_ROOT + path;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void installBinary(long binaryId, String resourceName) throws FxApplicationException {
        Connection con = null;
        try {
            ContentStorage storage = StorageManager.getContentStorage(TypeStorageMode.Hierarchical);
            String binaryName = resourceName;
            String subdir = "";
            if (binaryName.indexOf('/') > 0) {
                binaryName = binaryName.substring(binaryName.lastIndexOf('/') + 1);
                subdir = resourceName.substring(0, resourceName.lastIndexOf('/')+1);
            }
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            con = getConnection();
            long length = 0;
            String[] files = FxSharedUtils.loadFromInputStream(cl.getResourceAsStream("fxresources/binaries/"+subdir+"resourceindex.flexive"), -1).
                replaceAll("\r", "").split("\n");
            for( String file: files ) {
                if( file.startsWith(binaryName+"|")) {
                    length = Long.parseLong(file.split("\\|")[1]);
                    break;
                }
            }
            //TODO: check if length is still 0 or exception
            storage.storeBinary(con, binaryId, 1, 1, binaryName, length, cl.getResourceAsStream("fxresources/binaries/" + resourceName));
        } catch (SQLException e) {
            throw new FxDbException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }
}
