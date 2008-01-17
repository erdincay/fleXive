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
package com.flexive.core.storage.genericSQL;

import com.flexive.core.storage.FxTreeNodeInfo;
import com.flexive.core.storage.FxTreeNodeInfoSimple;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxTreeException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.SequencerEngine;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;

/**
 * Generic tree storage implementation using a simple nested set tree
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GenericTreeStorageSimple extends GenericTreeStorage {
    private static final transient Log LOG = LogFactory.getLog(GenericTreeStorageSimple.class);

    /**
     * {@inheritDoc}
     */
    @Override
    protected void wipeTree(FxTreeMode mode, Statement stmt, FxPK rootPK) throws SQLException {
        stmt.execute("SET FOREIGN_KEY_CHECKS=0");
        stmt.executeUpdate("DELETE FROM " + getTable(mode));
        stmt.executeUpdate("INSERT INTO " + getTable(mode) + " (ID,NAME,MODIFIED_AT,DIRTY,PARENT,DEPTH,CHILDCOUNT,TOTAL_CHILDCOUNT,REF,TEMPLATE,LFT,RGT) " +
                "VALUES (" + ROOT_NODE + ",'Root',SYSDATE(),FALSE,NULL,1,0,0," + rootPK.getId() +
                ",NULL,1,2)");
        stmt.executeUpdate("SET FOREIGN_KEY_CHECKS=1");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void afterNodeRemoved(Connection con, FxTreeNodeInfo nodeInfo, boolean removeChildren) throws FxApplicationException {
        //close the gap between left and right:
        PreparedStatement ps = null;
        try {
            if (!removeChildren) {
                ps = con.prepareStatement("UPDATE " + getTable(nodeInfo.getMode()) + " SET LFT=LFT-1, RGT=RGT-1 WHERE LFT BETWEEN ? AND ?");
                ps.setLong(1, nodeInfo.getLeft().longValue());
                ps.setLong(2, nodeInfo.getRight().longValue());
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("UPDATE " + getTable(nodeInfo.getMode()) + " SET LFT=LFT-2 WHERE LFT>?");
                ps.setLong(1, nodeInfo.getRight().longValue());
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("UPDATE " + getTable(nodeInfo.getMode()) + " SET RGT=RGT-2 WHERE RGT>?");
                ps.setLong(1, nodeInfo.getRight().longValue());
                ps.executeUpdate();
            } else {
                final long gap = nodeInfo.getRight().longValue() - nodeInfo.getLeft().longValue() + 1;
                ps = con.prepareStatement("UPDATE " + getTable(nodeInfo.getMode()) + " SET LFT=LFT-? WHERE LFT>?");
                ps.setLong(1, gap);
                ps.setLong(2, nodeInfo.getRight().longValue());
                ps.executeUpdate();
                ps.close();
                ps = con.prepareStatement("UPDATE " + getTable(nodeInfo.getMode()) + " SET RGT=RGT-? WHERE RGT>?");
                ps.setLong(1, gap);
                ps.setLong(2, nodeInfo.getRight().longValue());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new FxTreeException(e, "ex.tree.closeGap.error", nodeInfo.getId(), nodeInfo.getLeft(), nodeInfo.getRight(), e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOG.error(e, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public FxTreeNodeInfo getTreeNodeInfo(Connection con, FxTreeMode mode, long nodeId) throws FxApplicationException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(mode == FxTreeMode.Live ? TREE_LIVE_NODEINFO : TREE_EDIT_NODEINFO);
            ps.setLong(1, nodeId);
            ResultSet rs = ps.executeQuery();
            if (rs == null || !rs.next())
                throw new FxNotFoundException("ex.tree.node.notFound", nodeId, mode);
            long _acl = rs.getLong(14);
            FxType _type = CacheAdmin.getEnvironment().getType(rs.getLong(15));
            long _stepACL = CacheAdmin.getEnvironment().getStep(rs.getLong(17)).getAclId();
            long _createdBy = rs.getLong(18);
            long _mandator = rs.getLong(19);
            boolean[] perms = FxPermissionUtils.getPermissions(_acl, _type, _stepACL, _createdBy, _mandator);
            return new FxTreeNodeInfoSimple(rs.getLong(1), rs.getLong(2), rs.getLong(5),
                    rs.getLong(6), rs.getInt(4), rs.getInt(8), rs.getInt(7), rs.getLong(3),
                    nodeId, rs.getString(12), new FxPK(rs.getLong(9), rs.getInt(16)),
                    _acl, mode, rs.getInt(13), rs.getString(10), rs.getTimestamp(11).getTime(),
                    perms[0], perms[4], perms[2], perms[1], perms[3]);
        } catch (SQLException e) {
            throw new FxTreeException(e, "ex.tree.nodeInfo.sqlError", nodeId, e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                LOG.error(e, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long createNode(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, long nodeId,
                           long parentNodeId, String name, FxString label, int position, FxPK reference, String template) throws FxApplicationException {
        checkTemplateValue(template);
        NodeCreateInfo nci = getNodeCreateInfo(mode, seq, ce, nodeId, name, label, reference);
        FxTreeNodeInfo parentNode = getTreeNodeInfo(con, mode, parentNodeId);
        long left, right;

        if (position < 0)
            position = 0;
        else if (position > parentNode.getDirectChildCount())
            position = parentNode.getDirectChildCount();

        // Create the node
        PreparedStatement ps = null;
        try {
            if (position == 0) {
                //first entry
                left = parentNode.getLeft().longValue() + 1;
            } else if (parentNode.getDirectChildCount() >= position) {
                //last entry
                left = parentNode.getRight().longValue();
            } else {
                //inbetween - calculate needed left/right slot
                ps = con.prepareStatement("SELECT RGT FROM " + getTable(mode) + " WHERE PARENT=? ORDER BY LFT LIMIT " + position + ",1");
                ps.setLong(1, parentNodeId);
                ResultSet rs = ps.executeQuery();
                if (rs == null || !rs.next()) {
                    throw new FxTreeException("ex.tree.create.failed.positioning", parentNodeId, position);
                }
                left = rs.getLong(1);
                ps.close();
            }
            right = left + 1;
            //"move" nodes to the right to make space
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET RGT=RGT+2 WHERE RGT>=?");
            ps.setLong(1, left);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET LFT=LFT+2 WHERE LFT>?");
            ps.setLong(1, right);
            ps.executeUpdate();
            ps.close();
            //insert the new node
            ps = con.prepareStatement("INSERT INTO " + getTable(mode) + " (ID,PARENT,DEPTH,DIRTY,REF,LFT,RGT," +
                    "TOTAL_CHILDCOUNT,CHILDCOUNT,NAME,MODIFIED_AT,TEMPLATE) VALUES " +
                    "(" + nci.id + "," + parentNodeId + "," + (parentNode.getDepth() + 1) +
                    ",?," + nci.reference.getId() + ",?,?,0,0,?,SYSDATE(),?)");
            ps.setBoolean(1, mode != FxTreeMode.Live);
            ps.setLong(2, left);
            ps.setLong(3, right);
            ps.setString(4, nci.name);
            if (StringUtils.isEmpty(template)) {
                ps.setNull(5, java.sql.Types.VARCHAR);
            } else {
                ps.setString(6, template);
            }
            ps.executeUpdate();
            ps.close();

            // Update all affected childcounts
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET TOTAL_CHILDCOUNT=TOTAL_CHILDCOUNT+1 WHERE " +
                    " LFT<" + left + " AND RGT>" + right);
            ps.executeUpdate();
            ps.close();
            //update the parents childcount
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET CHILDCOUNT=CHILDCOUNT+1 WHERE ID=" + parentNodeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new FxTreeException(e, "ex.db.sqlError", e.getMessage());
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (Throwable t) {
                /*ignore*/
            }
        }
        return nci.id;
    }

    /**
     * Get a node attached to the given parent at the requested position
     *
     * @param con        an open and valid connection
     * @param mode       tree mode
     * @param parentNode parent node
     * @param position   position
     * @return FxTreeNodeInfo
     * @throws FxApplicationException on errors
     */
    protected FxTreeNodeInfo getTreeNodeInfoAt(Connection con, FxTreeMode mode, FxTreeNodeInfo parentNode, int position) throws FxApplicationException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ID FROM " + getTable(mode) + " WHERE LFT>? AND RGT<? LIMIT " + position + ",1");
            ps.setLong(1, parentNode.getLeft().longValue());
            ps.setLong(2, parentNode.getRight().longValue());
            ResultSet rs = ps.executeQuery();
            if (rs == null || !rs.next())
                throw new FxTreeException("ex.tree.nodeNotFound.position", position, parentNode.getId(), mode.name());
            return getTreeNodeInfo(con, mode, rs.getLong(1));
        } catch (SQLException e) {
            throw new FxTreeException(e, "ex.db.sqlError", e.getMessage());
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (Throwable t) {
                /*ignore*/
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void move(Connection con, SequencerEngine seq, FxTreeMode mode, long nodeId, long newParentId, int newPosition) throws FxApplicationException {
        FxTreeNodeInfo node = getTreeNodeInfo(con, mode, nodeId);
        final FxTreeNodeInfo parentNode = getTreeNodeInfo(con, mode, node.getParentId());
        if (newPosition < 0)
            newPosition = 0;
        else if (newPosition > parentNode.getDirectChildCount())
            newPosition = parentNode.getDirectChildCount() - 1;
        final boolean sameParent = parentNode.getId() == newParentId;
        if (sameParent && node.getPosition() == newPosition)
            return;  //nothing to do

        int depthDelta = 0;
        FxTreeNodeInfo newParentNode;
        if (!sameParent) {
            newParentNode = getTreeNodeInfo(con, mode, newParentId);
            depthDelta = (newParentNode.getDepth() + 1) - node.getDepth();
        } else
            newParentNode = parentNode;

        //make room for the node at destination position
        long destLeft, destRight;
        if (!newParentNode.hasChildren() || newPosition == 0) {
            //first
            destLeft = newParentNode.getLeft().longValue() + 1;
        } else if (newParentNode.getDirectChildCount() - 1 == newPosition) {
            //last
            destLeft = newParentNode.getRight().longValue()/* - 2*/;
        } else {
            //middle
            destLeft = getTreeNodeInfoAt(con, mode, newParentNode, newPosition).getLeft().longValue();
        }
        //dest right = dest left + "width" of node
        final long nodeWidth = node.getRight().longValue() - node.getLeft().longValue() + 1;
        destRight = destLeft + nodeWidth - 2;

        PreparedStatement ps = null;
        Statement stmt = null;
        try {
            //open enough space to hold the node
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET LFT=LFT+? WHERE LFT>=?");
            ps.setLong(1, nodeWidth);
            ps.setLong(2, destLeft);
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET RGT=RGT+? WHERE RGT>=?");
            ps.setLong(1, nodeWidth);
            ps.setLong(2, destRight);
            ps.executeUpdate();
            ps.close();
            //node may have been moved as well, refetch it
            FxTreeNodeInfo movedNode = getTreeNodeInfo(con, mode, nodeId);
            //move the node into the created gap
            final long delta = movedNode.getLeft().longValue() - destLeft;

            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET LFT=LFT-(?), RGT=RGT-(?) WHERE LFT>=? AND RGT<=?");
            ps.setLong(1, delta);
            ps.setLong(2, delta);
            ps.setLong(3, movedNode.getLeft().longValue());
            ps.setLong(4, movedNode.getRight().longValue());
            ps.executeUpdate();
            //close the gap from the original node
            ps.close();
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET RGT=RGT-? WHERE RGT>?");
            ps.setLong(1, nodeWidth);
            ps.setLong(2, movedNode.getRight().longValue());
            ps.executeUpdate();
            ps.close();
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET LFT=LFT-? WHERE LFT>?");
            ps.setLong(1, nodeWidth);
            ps.setLong(2, movedNode.getRight().longValue());
            ps.executeUpdate();

            // Update the parent of the node
            stmt = con.createStatement();
            stmt.addBatch("UPDATE " + getTable(mode) + " SET PARENT=" + newParentId + " WHERE ID=" + nodeId);
            if (mode != FxTreeMode.Live)
                stmt.addBatch("UPDATE " + getTable(mode) + " SET DIRTY=TRUE WHERE ID=" + nodeId);
            stmt.executeBatch();
            stmt.close();

            // Update the childcount of the new and old parent if needed + set dirty flag
            if (!sameParent) {
                FxTreeNodeInfo nodeOldParent = getTreeNodeInfo(con, mode, node.getParentId());
                node = getTreeNodeInfo(con, mode, nodeId);
                stmt = con.createStatement();
                stmt.addBatch("UPDATE " + getTable(mode) + " SET TOTAL_CHILDCOUNT=TOTAL_CHILDCOUNT+" + (node.getTotalChildCount() + 1) +
                        " WHERE (LFT<" + node.getLeft().longValue() + " AND RGT>" + node.getRight().longValue() + ")");
                stmt.addBatch("UPDATE " + getTable(mode) + " SET TOTAL_CHILDCOUNT=TOTAL_CHILDCOUNT-" + (node.getTotalChildCount() + 1) +
                        " WHERE (LFT<=" + nodeOldParent.getLeft() + " AND RGT>=" + nodeOldParent.getRight() + ")");
                stmt.addBatch("UPDATE " + getTable(mode) + " SET CHILDCOUNT=CHILDCOUNT+1 WHERE ID=" + newParentId);
                stmt.addBatch("UPDATE " + getTable(mode) + " SET CHILDCOUNT=CHILDCOUNT-1 WHERE ID=" + nodeOldParent.getId());
                if (mode != FxTreeMode.Live) {
                    stmt.addBatch("UPDATE " + getTable(mode) + " SET DIRTY=TRUE, DEPTH=DEPTH+(" + depthDelta + ") WHERE LFT>=" + node.getLeft().longValue() + " AND " +
                            "RGT<=" + node.getRight().longValue());
                    stmt.addBatch("UPDATE " + getTable(mode) + " SET DIRTY=TRUE WHERE ID IN(" + node.getParentId() + "," + newParentId + ")");
                }
                stmt.executeBatch();
                stmt.close();
            }
        } catch (SQLException e) {
            throw new FxTreeException(e, "ex.tree.move.failed", node.getId(), newParentId, newPosition, e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                //ignore
            }
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void activateNode(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, long nodeId) throws FxApplicationException {
        //TODO: code me!
    }

    /**
     * {@inheritDoc}
     */
    public void activateSubtree(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, long nodeId) throws FxApplicationException {
        //TODO: code me!
    }

    /**
     * {@inheritDoc}
     */
    public long copy(Connection con, SequencerEngine seq, FxTreeMode mode, long srcNodeId, long dstParentNodeId, int dstPosition, boolean deepReferenceCopy, String copyOfPrefix) throws FxApplicationException {
        //TODO: code me!
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void checkTree(Connection con, FxTreeMode mode) throws FxApplicationException {
        Statement stmt = null;
        Statement stmt2 = null;
        long nodes = 0;
        try {
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT ID FROM " + getTable(mode));
            while (rs.next()) {
                Long id = rs.getLong(1);
                FxTreeNodeInfo node = getTreeNodeInfo(con, mode, id);
                stmt2 = con.createStatement();
                ResultSet rs2 = stmt2.executeQuery("SELECT MAX(LFT),MAX(RGT),MIN(LFT),MIN(RGT) FROM " +
                        getTable(mode) + " WHERE PARENT=" + id);
                rs2.next();
                long maxLft = rs2.getLong(1);
                long maxRgt = rs2.getLong(2);
                long minLft = rs2.getLong(3);
                long minRgt = rs2.getLong(4);
                stmt2.close();
                if (maxLft != 0 && maxRgt != 0 && minLft != 0 && minRgt != 0) {
                    long max = Math.max(maxLft, Math.max(maxRgt, Math.max(minLft, minRgt)));
                    long min = Math.min(maxLft, Math.min(maxRgt, Math.min(minLft, minRgt)));
                    if (max > node.getRight().longValue())
                        throw new FxTreeException(LOG, "ex.tree.check.failed", mode, "#" + id + " out of bounds (right: " +
                                node.getRight().longValue() + " should be <= (" +
                                maxLft + "," + maxRgt + "," + minLft + "," + minRgt + "))");
                    if (min < node.getLeft().longValue())
                        throw new FxTreeException(LOG, "ex.tree.check.failed", mode, "#" + id + " out of bounds (left: " +
                                node.getLeft().longValue() + " should be >= (" +
                                maxLft + "," + maxRgt + "," + minLft + "," + minRgt + "))");
                }

                // Total child count check
                stmt2 = con.createStatement();
                rs2 = stmt2.executeQuery("SELECT COUNT(*) FROM " +
                        getTable(mode) + " WHERE LFT>" + node.getLeft().longValue() + " AND RGT<" + node.getRight().longValue());
                rs2.next();
                int totalChilds = rs2.getInt(1);
                if (totalChilds != node.getTotalChildCount())
                    throw new FxTreeException(LOG, "ex.tree.check.failed", mode, "#" + id + " invalid total child count [" + totalChilds + "!=" + node.getTotalChildCount() + "]");
                stmt2.close();

                // Direct child count check
                stmt2 = con.createStatement();
                rs2 = stmt2.executeQuery("SELECT COUNT(*) FROM " + getTable(mode) + " WHERE PARENT=" + id);
                rs2.next();
                int directChilds = rs2.getInt(1);
                if (directChilds != node.getDirectChildCount())
                    throw new FxTreeException(LOG, "ex.tree.check.failed", mode, "#" + id + " invalid direct child count [" + directChilds + "!=" + node.getDirectChildCount() + "]");
                if (directChilds > node.getTotalChildCount())
                    throw new FxTreeException(LOG, "ex.tree.check.failed", mode, "#" + id +
                            " more direct than total children! [direct:" + directChilds + ">total:" + node.getTotalChildCount() + "]");
                stmt2.close();

                // Depth check
                if (!node.isRoot()) {
                    stmt2 = con.createStatement();
                    rs2 = stmt2.executeQuery("SELECT DEPTH FROM " + getTable(mode) + " WHERE ID=" + node.getParentId());
                    rs2.next();
                    int depth = rs2.getInt(1);
                    stmt2.close();
                    if ((node.getDepth() - 1) != depth)
                        throw new FxTreeException(LOG, "ex.tree.check.failed", mode, "#" + id + " invalid depth: " + node.getDepth() + ", parent depth=" + depth);
                }
                nodes++;
            }
            if (LOG.isDebugEnabled())
                LOG.debug("Successfully checked [" + nodes + "] tree nodes in mode [" + mode.name() + "]!");
        } catch (SQLException e) {
            throw new FxTreeException(LOG, e, "ex.tree.check.failed", mode, e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception exc) {/*ignore*/}
            try {
                if (stmt2 != null) stmt2.close();
            } catch (Exception exc) {/*ignore*/}
        }
    }
}
