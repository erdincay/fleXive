/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.core.storage.MySQL;

import com.flexive.core.storage.TreeStorage;
import com.flexive.core.storage.genericSQL.GenericTreeStorageSpreaded;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.tree.FxTreeMode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * MySQL specific tree storage implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class MySQLTreeStorage extends GenericTreeStorageSpreaded {
    private static final Log LOG = LogFactory.getLog(MySQLTreeStorage.class);

    private static final MySQLTreeStorage instance = new MySQLTreeStorage();

    /**
     * Singleton getter
     *
     * @return TreeStorage
     */
    public static TreeStorage getInstance() {
        return instance;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getLabels(Connection con, FxTreeMode mode, long labelPropertyId, FxLanguage language, boolean stripNodeInfos, long... nodeIds) throws FxApplicationException {
        List<String> ret = new ArrayList<String>(nodeIds.length);
        if (nodeIds.length == 0)
            return ret;

        PreparedStatement ps = null;
        ResultSet rs;
        try {
            ps = con.prepareStatement("SELECT tree_FTEXT1024_Chain(?,?,?,?)");
            ps.setInt(2, (int) language.getId());
            ps.setLong(3, labelPropertyId);
            ps.setBoolean(4, mode == FxTreeMode.Live);
            for (long id : nodeIds) {
                ps.setLong(1, id);
                try {
                    rs = ps.executeQuery();
                    if (rs != null && rs.next()) {
                        final String path = rs.getString(1);
                        if (!StringUtils.isEmpty(path))
                            ret.add(stripNodeInfos ? stripNodeInfos(path) : path);
                        else
                            addUnknownNodeId(ret, id);
                    } else
                        addUnknownNodeId(ret, id);
                } catch (SQLException e) {
                    if ("22001".equals(e.getSQLState())) {
                        //invalid node id in MySQL
                        addUnknownNodeId(ret, id);
                    } else
                        throw e;
                }
            }
            return ret;
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    //commented out since using shared locks for the tree causes more troubles than it helps
    /**
     * {@inheritDoc}
     *
    @Override
    protected String getForUpdateClause() {
        //see: http://dev.mysql.com/doc/refman/5.1/en/innodb-deadlocks.html
        return " LOCK IN SHARE MODE";
    }
    */
}
