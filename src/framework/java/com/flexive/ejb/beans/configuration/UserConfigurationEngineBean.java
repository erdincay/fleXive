/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.ejb.beans.configuration;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_USER_CONFIG;
import com.flexive.core.configuration.GenericConfigurationImpl;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.interfaces.UserConfigurationEngine;
import com.flexive.shared.interfaces.UserConfigurationEngineLocal;

import javax.ejb.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * User configuration. Currently no security checks are included - a user
 * may always update/delete own parameters (and never modify parameters of other users).
 *  
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Stateless(name = "UserConfigurationEngine")
public class UserConfigurationEngineBean extends GenericConfigurationImpl implements UserConfigurationEngine, UserConfigurationEngineLocal {
    /**
     * User config cache root. Be sure to set timeout for this cache
     * region, otherwise user configuration data will never expire from the cache.
     */
    private static final String CACHE_ROOT = "/userConfig/";


    /** {@inheritDoc} */
    @Override
    protected Connection getConnection() throws SQLException {
        return Database.getDbConnection();
    }

    /** {@inheritDoc} */
    @Override
    protected PreparedStatement getInsertStatement(Connection conn, String path, String key, String value)
            throws SQLException, FxNoAccessException {
        String sql = "INSERT INTO " + TBL_USER_CONFIG + "(user_id, cpath, ckey, cvalue) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, FxContext.get().getTicket().getUserId());
        stmt.setString(2, path);
        stmt.setString(3, key);
        stmt.setString(4, value);
        return stmt;
    }

    /** {@inheritDoc} */
    @Override
    protected PreparedStatement getSelectStatement(Connection conn, String path, String key) throws SQLException {
        String sql = "SELECT cvalue FROM " + TBL_USER_CONFIG + " WHERE user_id=? AND cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, FxContext.get().getTicket().getUserId());
        stmt.setString(2, path);
        stmt.setString(3, key);
        return stmt;
    }

    /** {@inheritDoc} */
    @Override
    protected PreparedStatement getSelectStatement(Connection conn, String path) throws SQLException {
        String sql = "SELECT ckey, cvalue FROM " + TBL_USER_CONFIG + " WHERE user_id=? AND cpath=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, FxContext.get().getTicket().getUserId());
        stmt.setString(2, path);
        return stmt;
    }

    /** {@inheritDoc} */
    @Override
    protected PreparedStatement getUpdateStatement(Connection conn, String path, String key, String value)
            throws SQLException, FxNoAccessException {
        String sql = "UPDATE " + TBL_USER_CONFIG + " SET cvalue=? WHERE user_id=? AND cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, value);
        stmt.setLong(2, FxContext.get().getTicket().getUserId());
        stmt.setString(3, path);
        stmt.setString(4, key);
        return stmt;
    }

    /** {@inheritDoc} */
    @Override
    protected PreparedStatement getDeleteStatement(Connection conn, String path, String key)
            throws SQLException, FxNoAccessException {
        String sql = "DELETE FROM " + TBL_USER_CONFIG + " WHERE user_id=? AND cpath=? "
            + (key != null ? " AND ckey=?" : "");
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setLong(1, FxContext.get().getTicket().getUserId());
        stmt.setString(2, path);
        if (key != null) {
            stmt.setString(3, key);
        }
        return stmt;
    }

    /** {@inheritDoc} */
    @Override
    protected String getCachePath(String path) {
        return CACHE_ROOT + FxContext.get().getTicket().getUserId() + path;
    }
}
