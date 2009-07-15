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
package com.flexive.core.storage.h2;

import com.flexive.core.storage.TreeStorage;
import com.flexive.core.storage.StorageManager;
import com.flexive.core.storage.mySQL.MySQLTreeStorage;
import com.flexive.core.storage.genericSQL.GenericTreeStorageSpreaded;
import com.flexive.shared.interfaces.SequencerEngine;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxTreeException;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.DBVendor;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * H2 specific tree storage implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class H2TreeStorage extends GenericTreeStorageSpreaded {
    private static final H2TreeStorage instance = new H2TreeStorage();

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
    public void activateNode(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, long nodeId) throws FxApplicationException {
        if (mode == FxTreeMode.Live) //Live tree can not be activated!
            return;
        long ids[] = getIdChain(con, mode, nodeId);
        for (long id : ids) {
            if (id == ROOT_NODE) continue;
            FxTreeNode srcNode = getNode(con, mode, id);
            //check if the node already exists in the live tree
            if (exists(con, FxTreeMode.Live, id)) {
                //Move and setData will not do anything if the node is already in its correct place and
                move(con, seq, FxTreeMode.Live, id, srcNode.getParentNodeId(), srcNode.getPosition());
                setData(con, FxTreeMode.Live, id, srcNode.getData());
            } else {
                createNode(con, seq, ce, FxTreeMode.Live, srcNode.getId(), srcNode.getParentNodeId(),
                        srcNode.getName(), srcNode.getLabel(), srcNode.getPosition(),
                        srcNode.getReference(), srcNode.getData());
            }

            // Remove all deleted direct child nodes
            Statement stmt = null;
            Statement stmt2 = null;
            try {
                stmt = con.createStatement();
                stmt2 = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT DISTINCT a.ID FROM (SELECT ID FROM " + getTable(FxTreeMode.Live) + " WHERE PARENT=" +
                        nodeId + ") a LEFT " +
                        "JOIN (SELECT ID FROM " + getTable(FxTreeMode.Live) + " WHERE PARENT=" + nodeId + ") b WHERE b.ID IS NULL");
                while (rs != null && rs.next()) {
                    long deleteId = rs.getLong(1);
                    stmt2.addBatch(StorageManager.getReferentialIntegrityChecksStatement(false));
                    try {
                        stmt2.addBatch("DELETE FROM " + getTable(FxTreeMode.Live) + " WHERE ID=" + deleteId);
                    } finally {
                        stmt2.addBatch(StorageManager.getReferentialIntegrityChecksStatement(true));
                    }
                }
                stmt2.addBatch("UPDATE " + getTable(FxTreeMode.Live) + " SET MODIFIED_AT=" + System.currentTimeMillis());
                stmt2.executeBatch();
            } catch (SQLException e) {
                throw new FxTreeException("ex.tree.activate.failed", nodeId, false, e.getMessage());
            } finally {
                try {
                    if (stmt != null) stmt.close();
                } catch (Exception exc) {
                    //ignore
                }
                try {
                    if (stmt2 != null) stmt2.close();
                } catch (Exception exc) {
                    //ignore
                }
            }
            clearDirtyFlag(con, mode, nodeId);
        }
    }
}