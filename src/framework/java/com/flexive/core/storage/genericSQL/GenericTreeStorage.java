/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.core.storage.genericSQL;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.LifeCycleInfoImpl;
import com.flexive.core.storage.DBStorage;
import com.flexive.core.storage.FxTreeNodeInfo;
import com.flexive.core.storage.StorageManager;
import com.flexive.core.storage.TreeStorage;
import com.flexive.shared.*;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.content.*;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.interfaces.SequencerEngine;
import com.flexive.shared.scripting.FxScriptBinding;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLPermission;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.TypeState;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.value.FxString;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;

import static com.flexive.core.DatabaseConst.*;

/**
 * Generic tree storage implementation for nested set model trees
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public abstract class GenericTreeStorage implements TreeStorage {
    /**
     * Partition size to be used for large IN(...) queries to workaround limitations of DBMS drivers
     */
    protected static final int SQL_IN_PARTSIZE = 500;

    /**
     * Information to be processed when creating a new node (return value helper)
     */
    protected static class NodeCreateInfo {
        final long id;
        final String name;
        final FxPK reference;

        public NodeCreateInfo(long id, String name, FxPK reference) {
            this.id = id;
            this.name = name;
            this.reference = reference;
        }
    }


    private static final Log LOG = LogFactory.getLog(GenericTreeStorage.class);

    private static final String TREE_TOTAL_COUNT_LIVE = "(SELECT COUNT(*) FROM " + getTable(FxTreeMode.Live) + " WHERE LFT > t.LFT AND RGT < t.RGT)";
    private static final String TREE_TOTAL_COUNT_EDIT = "(SELECT COUNT(*) FROM " + getTable(FxTreeMode.Edit) + " WHERE LFT > t.LFT AND RGT < t.RGT)";
    /**
     * Placeholder for dynamic child count
     */
    protected static final String COMPUTE_TOTAL_CHILDCOUNT = "__TOTAL_CHILD_COUNT";
    //                                                         1   2   3      4
    protected static final String TREE_LIVE_NODEINFO = "SELECT LFT,RGT,PARENT,TOTAL_CHILDCOUNT," +
            //                                                                        5
            "(SELECT LFT FROM " + getTable(FxTreeMode.Live) + " WHERE ID=NODE.PARENT) AS PARENTLEFT, " +
            //                                                                        6
            "(SELECT RGT FROM " + getTable(FxTreeMode.Live) + " WHERE ID=NODE.PARENT) AS PARENTRIGHT," +
            //7    8          9   10       11          12   13                               14  15   16  17   18         19
            "DEPTH,CHILDCOUNT,REF,TEMPLATE,MODIFIED_AT,NAME,tree_getPosition(?,ID,PARENT),ACL,TDEF,VER,STEP,CREATED_BY,MANDATOR " +
            "FROM (SELECT t.LFT,t.RGT,t.PARENT,t.DEPTH," + COMPUTE_TOTAL_CHILDCOUNT + ",t.CHILDCOUNT,t.REF,t.TEMPLATE,t.MODIFIED_AT,t.NAME,t.ID,c.ACL,c.TDEF,c.VER,c.STEP,c.CREATED_BY,c.MANDATOR FROM " +
            getTable(FxTreeMode.Live) + " t, " + TBL_CONTENT + " c WHERE t.ID=? AND c.ID=t.REF AND c.ISLIVE_VER=?) NODE";

    protected static final String TREE_NODEINFO_ACLS =
            "SELECT acl FROM " + TBL_CONTENT + " WHERE id=? AND ver=? and acl != " + ACL.NULL_ACL_ID +
                    " UNION " +
                    "SELECT acl FROM " + TBL_CONTENT_ACLS + " WHERE id=? AND ver=? ";

    //                                                       1   2   3      4
    protected static final String TREE_EDIT_NODEINFO = "SELECT LFT,RGT,PARENT,TOTAL_CHILDCOUNT," +
            //                                                                        5
            "(SELECT LFT FROM " + getTable(FxTreeMode.Edit) + " WHERE ID=NODE.PARENT) AS PARENTLEFT, " +
            //                                                                        6
            "(SELECT RGT FROM " + getTable(FxTreeMode.Edit) + " WHERE ID=NODE.PARENT) AS PARENTRIGHT," +
            //7    8          9   10       11          12   13                                14  15   16  17   18         19
            "DEPTH,CHILDCOUNT,REF,TEMPLATE,MODIFIED_AT,NAME,tree_getPosition(?,ID,PARENT),ACL,TDEF,VER,STEP,CREATED_BY,MANDATOR " +
            "FROM (SELECT t.LFT,t.RGT,t.PARENT,t.DEPTH," + COMPUTE_TOTAL_CHILDCOUNT + ",t.CHILDCOUNT,t.REF,t.TEMPLATE,t.MODIFIED_AT,t.NAME,t.ID,c.ACL,c.TDEF,c.VER,c.STEP,c.CREATED_BY,c.MANDATOR FROM " +
            getTable(FxTreeMode.Edit) + " t, " + TBL_CONTENT + " c WHERE t.ID=? AND c.ID=t.REF AND c.ISMAX_VER=?) NODE";

    //                                                        1    2     3       4                             5            6      7        8       9          10          11                                         12    13     14    15     16           17       18        19         20           21           22           23            24             25
    private static final String TREE_LIVE_GETNODE = "SELECT t.ID,t.REF,t.DEPTH," + TREE_TOTAL_COUNT_LIVE + ",t.CHILDCOUNT,t.NAME,t.PARENT,t.DIRTY,t.TEMPLATE,t.MODIFIED_AT,tree_getPosition(?,t.ID,t.PARENT) AS POS,c.ACL,c.TDEF,c.VER,c.STEP,c.CREATED_BY,c.MANDATOR,l.USER_ID,l.LOCKTYPE,l.CREATED_AT,l.EXPIRES_AT,c.CREATED_AT,c.MODIFIED_BY,c.MODIFIED_AT,tree_idToPath(t.id, ?) " +
            "FROM " + getTable(FxTreeMode.Live) + " t, " + TBL_CONTENT + " c LEFT JOIN " + TBL_LOCKS + " l ON (l.LOCK_ID=c.ID AND l.LOCK_VER=c.VER)WHERE t.ID=? AND c.ID=t.REF AND c.ISLIVE_VER=?";
    //                                                        1    2     3     4   5            6      7        8       9          10          11                                          12    13     14    15     16           17       18        19         20           21           22           23            24             25
    private static final String TREE_EDIT_GETNODE = "SELECT t.ID,t.REF,t.DEPTH,0,t.CHILDCOUNT,t.NAME,t.PARENT,t.DIRTY,t.TEMPLATE,t.MODIFIED_AT,tree_getPosition(?,t.ID,t.PARENT) AS POS,c.ACL,c.TDEF,c.VER,c.STEP,c.CREATED_BY,c.MANDATOR,l.USER_ID,l.LOCKTYPE,l.CREATED_AT,l.EXPIRES_AT,c.CREATED_AT,c.MODIFIED_BY,c.MODIFIED_AT,tree_idToPath(t.id, ?) " +
            "FROM " + getTable(FxTreeMode.Edit) + " t, " + TBL_CONTENT + " c LEFT JOIN " + TBL_LOCKS + " l ON (l.LOCK_ID=c.ID AND l.LOCK_VER=c.VER) WHERE t.ID=? AND c.ID=t.REF AND c.ISMAX_VER=?";

    //                                                            1    2     3     4  5            6      7        8       9          10          11                                         12    13     14    15     16           17       18        19         20           21           22           23            24
    private static final String TREE_LIVE_GETNODE_REF = "SELECT t.ID,t.REF,t.DEPTH,0,t.CHILDCOUNT,t.NAME,t.PARENT,t.DIRTY,t.TEMPLATE,t.MODIFIED_AT,tree_getPosition(?,t.ID,t.PARENT) AS POS,c.ACL,c.TDEF,c.VER,c.STEP,c.CREATED_BY,c.MANDATOR,l.USER_ID,l.LOCKTYPE,l.CREATED_AT,l.EXPIRES_AT,c.CREATED_AT,c.MODIFIED_BY,c.MODIFIED_AT " +
            "FROM " + getTable(FxTreeMode.Live) + " t, " + TBL_CONTENT + " c LEFT JOIN " + TBL_LOCKS + " l ON (l.LOCK_ID=c.ID AND l.LOCK_VER=c.VER) WHERE t.REF=? AND c.ID=t.REF AND c.ISLIVE_VER=?";
    //                                                            1    2     3     4  5            6      7        8       9          10          11                                          12    13     14    15     16           17       18        19         20           21           22           23            24
    private static final String TREE_EDIT_GETNODE_REF = "SELECT t.ID,t.REF,t.DEPTH,0,t.CHILDCOUNT,t.NAME,t.PARENT,t.DIRTY,t.TEMPLATE,t.MODIFIED_AT,tree_getPosition(?,t.ID,t.PARENT) AS POS,c.ACL,c.TDEF,c.VER,c.STEP,c.CREATED_BY,c.MANDATOR,l.USER_ID,l.LOCKTYPE,l.CREATED_AT,l.EXPIRES_AT,c.CREATED_AT,c.MODIFIED_BY,c.MODIFIED_AT " +
            "FROM " + getTable(FxTreeMode.Edit) + " t, " + TBL_CONTENT + " c LEFT JOIN " + TBL_LOCKS + " l ON (l.LOCK_ID=c.ID AND l.LOCK_VER=c.VER)WHERE t.REF=? AND c.ID=t.REF AND c.ISMAX_VER=?";

    //1=id
    private static final String TREE_REF_USAGE_EDIT = "SELECT DISTINCT c.TDEF, t.ID FROM " + TBL_CONTENT + " c," + getTable(FxTreeMode.Edit) + " t WHERE c.ID=? AND t.REF=c.ID";
    //1=id
    private static final String TREE_REF_USAGE_LIVE = "SELECT DISTINCT c.TDEF, t.ID FROM " + TBL_CONTENT + " c," + getTable(FxTreeMode.Live) + " t WHERE c.ID=? AND t.REF=c.ID";

    /**
     * Get the tree table for the requested mode
     *
     * @param mode requested mode
     * @return database table to use
     */
    public static String getTable(FxTreeMode mode) {
        if (mode == FxTreeMode.Live)
            return TBL_TREE + "_LIVE";
        else
            return TBL_TREE;
    }

    /**
     * Check if data contains valid characters
     *
     * @param data the data to check
     * @throws FxTreeException on errors
     */
    protected void checkDataValue(String data) throws FxTreeException {
        if (!StringUtils.isEmpty(data) && data.indexOf(',') >= 0) {
            throw new FxTreeException("Data may not contain the comma character [,]");
        }
    }

    /**
     * Create a new folder instance to be used as reference for nodes
     *
     * @param ce    a reference to the ContentEngine
     * @param name  name
     * @param label label
     * @return FxPK
     * @throws FxApplicationException on errors
     */
    protected FxPK createFolderInstance(ContentEngine ce, String name, FxString label) throws FxApplicationException {
        FxPK reference;
        FxType type = CacheAdmin.getEnvironment().getType(FxType.FOLDER);
        FxContent content = ce.initialize(type.getId());
        List<FxPropertyAssignment> fqns = type.getAssignmentsForProperty(EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_FQN_PROPERTY));
        if (fqns.size() > 0)
            content.setValue(fqns.get(0).getXPath(), new FxString(fqns.get(0).isMultiLang(), name));
        List<FxPropertyAssignment> captions = type.getAssignmentsForProperty(EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_CAPTION_PROPERTY));
        if (captions.size() > 0) {
            FxString captionValue;
            if (label != null) {
                if (captions.get(0).isMultiLang() && label.isMultiLanguage()) {
                    captionValue = label;
                } else if (!captions.get(0).isMultiLang() && label.isMultiLanguage()) {
                    captionValue = new FxString(false, label.getBestTranslation());
                } else if (captions.get(0).isMultiLang() && !label.isMultiLanguage()) {
                    captionValue = new FxString(true, label.getBestTranslation());
                } else
                    captionValue = new FxString(captions.get(0).isMultiLang(), name); //unreachable fallback
            } else
                captionValue = new FxString(captions.get(0).isMultiLang(), name);
            content.setValue(captions.get(0).getXPath(), captionValue);
        }
        //make sure we have a live version
        content.setStepId(type.getWorkflow().getLiveStep().getId());
        reference = ce.save(content);
        return reference;
    }

    /**
     * Resolve information about a node to be created like creating a folder if no reference is provided or check
     * permissions on existing references and availability of live instance if operated on the live tree
     *
     * @param mode            tree mode
     * @param seq             reference to the sequencer
     * @param ce              reference to the content engine
     * @param nodeId          desired node id
     * @param name            name
     * @param label           label
     * @param reference       reference
     * @param activateContent change the step of contents that have no live step to live in the max version?
     * @return NodeCreateInfo
     * @throws FxApplicationException on errors
     */
    protected NodeCreateInfo getNodeCreateInfo(FxTreeMode mode, SequencerEngine seq, ContentEngine ce,
                                               long nodeId, String name, FxString label, FxPK reference, boolean activateContent) throws FxApplicationException {
        if (nodeId < 0)
            nodeId = seq.getId(mode.getSequencer());
        if (StringUtils.isEmpty(name) || "_".equals(name)) {
            if (!StringUtils.isEmpty(label.getBestTranslation()))
                name = label.getBestTranslation();
            else if (reference != null && !reference.isNew()) {
                try {
                    final FxString caption = ce.load(reference).getCaption();
                    if (caption != null && !StringUtils.isEmpty(caption.getBestTranslation()))
                        name = caption.getBestTranslation();
                    else name = "" + nodeId;
                } catch (FxApplicationException e) {
                    name = "" + nodeId;
                }
            } else
                name = "" + nodeId;
        }
        name = FxFormatUtils.escapeTreePath(name);
        if (name.contains("/")) {
            throw new FxTreeException("ex.tree.create.failed.name", name);
        }
        //check or optionally create the reference
        if (reference == null || reference.isNew()) {
            //create a folder instance if no reference is supplied
            reference = createFolderInstance(ce, name, label);
        } else {
            //check read permission
            FxContent co = ce.load(reference);
            if (mode == FxTreeMode.Live) {
                //if mode==Live(activation), check if a live version exists and if not try to change the step of the max version to live (create a new version?)
                FxContentVersionInfo versionInfo = ce.getContentVersionInfo(reference);
                if (versionInfo.hasLiveVersion()) {
                    //check read permission
                    FxContentSecurityInfo si = ce.getContentSecurityInfo(new FxPK(reference.getId(), versionInfo.getLiveVersion()));
                    FxPermissionUtils.checkPermission(FxContext.getUserTicket(), ACLPermission.READ, si, true);
                } else {
                    if (activateContent) {
                        //create a Live version
                        reference = createContentLiveVersion(ce, co);
                        LOG.info("Created new live version " + reference + " to activate node " + nodeId);
                    } else {
                        throw new FxTreeException("ex.tree.activate.failed.noLiveContent", nodeId);
                    }
                }
            }
            List<FxPropertyData> fqns = co.getPropertyData(EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_FQN_PROPERTY), false);
            if (fqns.size() > 0)
                name = "" + fqns.get(0).getValue().getBestTranslation();
        }
        return new NodeCreateInfo(nodeId, name, reference);
    }

    /**
     * Create a live version of the given content, taking over existing locks and unlocking them after the version is created
     *
     * @param ce ContentEngine reference
     * @param co FxContent
     * @return primary key of the new version
     * @throws FxApplicationException on errors
     */
    protected FxPK createContentLiveVersion(ContentEngine ce, FxContent co) throws FxApplicationException {
        FxPK pk;
        if (co.isLocked())
            ce.takeOverLock(co.getLock());
        co.setStepId(CacheAdmin.getEnvironment().getType(co.getTypeId()).getWorkflow().getLiveStep().getId());
        //save will throw an exception if we are not allowed to save a live version
        pk = ce.save(co);
        if (co.isLocked())
            ce.unlock(pk);
        return pk;
    }

    /**
     * {@inheritDoc}
     */
    public long[] createNodes(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, long parentNodeId, String path, int position, boolean activateContent) throws FxApplicationException {
        if ("/".equals(path))
            return new long[]{FxTreeNode.ROOT_NODE};
        final List<Long> result = new ArrayList<Long>();
        final Scanner scanner = new Scanner(path);
        long currentParent = parentNodeId;
        scanner.useDelimiter("/");
        if (parentNodeId != -1) {
            acquireLocksForUpdate(con, getTreeNodeInfo(con, mode, parentNodeId), false);
        }
        while (scanner.hasNext()) {
            String name = scanner.next();
            final FxString label = new FxString(true, name);
            name = FxFormatUtils.escapeTreePath(name);
            if (StringUtils.isEmpty(name))
                continue;
            long nodeId = getIdByFQNPath(con, mode, currentParent, "/" + name);
            if (nodeId == -1)
                nodeId = createNode(con, seq, ce, mode, nodeId, currentParent, name, label, position, null, null, activateContent);
            result.add(nodeId);
            currentParent = nodeId;
        }
        return ArrayUtils.toPrimitive(result.toArray(new Long[result.size()]));
    }

    /**
     * {@inheritDoc}
     */
    public void updateName(Connection con, FxTreeMode mode, ContentEngine ce, long nodeId, String name) throws FxApplicationException {
        FxTreeNode node = getNode(con, mode, nodeId);
        name = FxFormatUtils.escapeTreePath(name);
        if (node.getName().equals(name))
            return;

        FxContent co = ce.load(node.getReference());
        FxType type = CacheAdmin.getEnvironment().getType(co.getTypeId());
        List<FxPropertyAssignment> fqns = type.getAssignmentsForProperty(EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_FQN_PROPERTY));
        if (fqns.size() > 0) {
            co.setValue(fqns.get(0).getXPath(), new FxString(fqns.get(0).isMultiLang(), name));
            ce.save(co);
            //we're done here since the content engine itself will trigger the tree table update
            return;
        }
        //no fqn - update the name column
        PreparedStatement ps = null;
        try {
            //                                                                1        2          3
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET NAME=?, DIRTY=? WHERE ID=?");
            ps.setString(1, name);
            ps.setBoolean(2, mode != FxTreeMode.Live);
            ps.setLong(3, nodeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new FxUpdateException(LOG, e, "ex.tree.update.failed", mode.name(), e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void updateReference(Connection con, FxTreeMode mode, long nodeId, long referenceId) throws FxApplicationException {
        PreparedStatement ps = null;
        try {
            //                                                               1        2           3
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET REF=?, DIRTY=? WHERE ID=?");
            ps.setLong(1, referenceId);
            ps.setBoolean(2, mode != FxTreeMode.Live); //live tree is never dirty
            ps.setLong(3, nodeId);
            ps.executeUpdate();
            FxContext.get().setTreeWasModified();
        } catch (SQLException e) {
            throw new FxUpdateException(LOG, e, "ex.tree.update.failed", mode.name(), e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void syncFQNName(Connection con, long referenceId, boolean maxVersion, boolean liveVersion, String name) throws FxApplicationException {
//        if (StringUtils.isEmpty(name))
//            return;
        if (liveVersion)
            updateName(con, FxTreeMode.Live, referenceId, name);
        if (maxVersion || liveVersion)
            updateName(con, FxTreeMode.Edit, referenceId, name);
    }

    /**
     * Update all nodes that reference the given id and set the name
     *
     * @param con         an open and valid connection
     * @param mode        tree mode
     * @param referenceId id of the referenced content
     * @param name        the new name
     * @throws FxApplicationException on errors
     */
    private void updateName(Connection con, FxTreeMode mode, long referenceId, String name) throws FxApplicationException {
        PreparedStatement ps = null;
        try {
            //                                                                1        2           3
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET NAME=?, DIRTY=? WHERE REF=?");
            ps.setString(1, FxFormatUtils.escapeTreePath(name));
            ps.setBoolean(2, mode != FxTreeMode.Live); //live tree is never dirty
            ps.setLong(3, referenceId);
            ps.executeUpdate();
            FxContext.get().setTreeWasModified();
        } catch (SQLException e) {
            throw new FxUpdateException(LOG, e, "ex.tree.update.failed", mode.name(), e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    /**
     * Clear a tree nodes dirty flag
     *
     * @param con    an open and valid connection
     * @param mode   tree mode
     * @param nodeId node id
     * @throws FxUpdateException on errors
     */
    protected void clearDirtyFlag(Connection con, FxTreeMode mode, long nodeId) throws FxUpdateException {
        PreparedStatement ps = null;
        try {
            //                                                                 1                                                              2
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET DIRTY=?,MODIFIED_AT=" + StorageManager.getTimestampFunction() + " WHERE ID=?");
            ps.setBoolean(1, false); //live tree is never dirty
            ps.setLong(2, nodeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new FxUpdateException(LOG, e, "ex.tree.update.failed", mode.name(), e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    /**
     * Flag a tree node dirty
     *
     * @param con    an open and valid connection
     * @param mode   tree mode
     * @param nodeId node id
     * @throws FxUpdateException on errors
     */
    protected void flagDirty(Connection con, FxTreeMode mode, long nodeId) throws FxUpdateException {
        PreparedStatement ps = null;
        try {
            //                                                                 1                                                              2
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET DIRTY=?,MODIFIED_AT=" + StorageManager.getTimestampFunction() + " WHERE ID=?");
            ps.setBoolean(1, mode != FxTreeMode.Live); //live tree is never dirty
            ps.setLong(2, nodeId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new FxUpdateException(LOG, e, "ex.tree.update.failed", mode.name(), e.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getIdByFQNPath(Connection con, FxTreeMode mode, long startNode, String path) throws FxApplicationException {
        path = path.replaceAll("/+", "/");
        if ("/".equals(path))
            return FxTreeNode.ROOT_NODE;

        PreparedStatement ps = null;
        try {
            final DBStorage storage = StorageManager.getStorageImpl();
            ps = con.prepareStatement("SELECT tree_pathToID(?,?,?) " + storage.getFromDual());
            ps.setLong(1, startNode);
            ps.setString(2, FxFormatUtils.escapeTreePath(path));
            ps.setBoolean(3, mode == FxTreeMode.Live);
            final ResultSet rs = ps.executeQuery();
            long result = -1;
            if (rs.next()) {
                result = rs.getLong(1);
                if (rs.wasNull()) {
                    result = -1;
                }
            }
            return result;
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

    /**
     * {@inheritDoc}
     */
    public long getIdByLabelPath(Connection con, FxTreeMode mode, long startNode, String path) throws FxApplicationException {
        path = path.replaceAll("/+", "/");
        if ("/".equals(path))
            return FxTreeNode.ROOT_NODE;

        PreparedStatement ps = null;
        try {
            final DBStorage storage = StorageManager.getStorageImpl();
            ps = con.prepareStatement("SELECT tree_captionPathToID(?, ?, ?, ?, ?) " + storage.getFromDual());
            ps.setLong(1, startNode);
            ps.setString(2, path);
            ps.setLong(3, EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_CAPTION_PROPERTY));
            ps.setInt(4, 1);
            ps.setBoolean(5, mode == FxTreeMode.Live);

            final ResultSet rs = ps.executeQuery();
            long result = -1;
            if (rs.next()) {
                result = rs.getLong(1);
                if (rs.wasNull())
                    result = -1;
            }
            return result;
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

    /**
     * {@inheritDoc}
     */
    public List<String> getLabels(Connection con, FxTreeMode mode, long labelPropertyId, FxLanguage language,
                                  boolean stripNodeInfos, long... nodeIds) throws FxApplicationException {
        List<String> ret = new ArrayList<String>(nodeIds.length);
        if (nodeIds.length == 0)
            return ret;

        PreparedStatement ps = null;
        ResultSet rs;
        try {
            ps = con.prepareStatement("SELECT tree_FTEXT1024_Chain(?,?,?,?)" + StorageManager.getFromDual());
            ps.setInt(2, (int) language.getId());
            ps.setLong(3, labelPropertyId);
            ps.setBoolean(4, mode == FxTreeMode.Live);
            for (long id : nodeIds) {
                ps.setLong(1, id);
                rs = ps.executeQuery();
                if (rs != null && rs.next()) {
                    final String path = rs.getString(1);
                    if (!StringUtils.isEmpty(path))
                        ret.add(stripNodeInfos ? stripNodeInfos(path) : path);
                    else
                        addUnknownNodeId(ret, id);
                } else
                    addUnknownNodeId(ret, id);
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

    /**
     * Helper to add an unknown node id
     *
     * @param ret result list
     * @param id  unknown node id
     */
    protected void addUnknownNodeId(List<String> ret, long id) {
        ret.add("/<unknown:" + id + ">");
    }

    /**
     * Strip any node infos from the path.
     * Node infos have the format path:nodeId:ref:tdef - strip all but path which can not contain ':' as it is replaced
     * with a space in the stored procedure.
     *
     * @param path path to strip
     * @return stripped path
     */
    protected String stripNodeInfos(String path) {
        if (StringUtils.isEmpty(path))
            return "";
        StringBuilder sbRet = new StringBuilder(path.length());
        for (String p : path.split("\\/"))
            if (p.indexOf(':') > 0)
                sbRet.append('/').append(p.substring(0, p.indexOf(':')));
        return sbRet.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getPathById(Connection con, FxTreeMode mode, long id) throws FxTreeException {
        Statement ps = null;
        try {
            DBStorage storage = StorageManager.getStorageImpl();
            ps = con.createStatement();
            ResultSet rs = ps.executeQuery("SELECT tree_idToPath(" + id + "," +
                    storage.getBooleanExpression(mode == FxTreeMode.Live) + ")" + storage.getFromDual());
            String result = null;
            if (rs.next()) {
                result = rs.getString(1);
                if (rs.wasNull()) result = "";
            }
            return result != null ? FxFormatUtils.escapeTreePath(result) : "";
        } catch (SQLException e) {
            throw new FxTreeException(LOG, "ex.db.sqlError", e.getMessage());
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (Exception exc) {
                //ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public long[] getIdChain(Connection con, FxTreeMode mode, long id) throws FxTreeException {
        Statement ps = null;
        try {
            ps = con.createStatement();
            DBStorage storage = StorageManager.getStorageImpl();
            ResultSet rs = ps.executeQuery("SELECT tree_idchain(" + id + "," +
                    storage.getBooleanExpression(mode == FxTreeMode.Live) + ")" + storage.getFromDual());
            String result = "";
            if (rs.next()) {
                result = rs.getString(1);
                if (rs.wasNull()) result = "";
            }
            if (result.length() == 0) {
                return null;
            }
            String ids[] = result.split("/");
            long[] lres = new long[ids.length - 1];
            int pos = 0;
            for (String sid : ids) {
                if (pos != 0)
                    lres[pos - 1] = Long.parseLong(sid);
                pos++;
            }
            return lres;
        } catch (SQLException e) {
            throw new FxTreeException(LOG, "ex.db.sqlError", e.getMessage());
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (Exception exc) {
                //ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(Connection con, FxTreeMode mode, long id) throws FxApplicationException {
        try {
            getTreeNodeInfo(con, mode, id);
        } catch (FxNotFoundException e) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public FxTreeNode getNode(Connection con, FxTreeMode mode, long nodeId) throws FxApplicationException {
        PreparedStatement ps = null;
        final FxEnvironment env = CacheAdmin.getEnvironment();
        try {
            ps = con.prepareStatement(prepareSql(mode, mode == FxTreeMode.Live ? TREE_LIVE_GETNODE : TREE_EDIT_GETNODE));
            ps.setBoolean(1, mode == FxTreeMode.Live);
            ps.setBoolean(2, mode == FxTreeMode.Live);
            ps.setLong(3, nodeId);
            ps.setBoolean(4, true);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long _id = rs.getLong(1);
                FxPK _ref = new FxPK(rs.getLong(2), rs.getInt(14));
                int _depth = rs.getInt(3);
                //int _totalChilds = rs.getInt(4);
                int _directChilds = rs.getInt(5);
                String _name = rs.getString(6);
                long _parent = rs.getLong(7);
                boolean _dirty = rs.getBoolean(8);
                String _data = rs.getString(9);
                if (rs.wasNull())
                    _data = null;
                long _modified = rs.getLong(10);
                int _pos = rs.getInt(11);
                long _acl = rs.getLong(12);
                FxType _type = env.getType(rs.getLong(13));
                long _stepACL = env.getStep(rs.getLong(15)).getAclId();
                long _createdBy = rs.getLong(16);
                long _mandator = rs.getLong(17);
                UserTicket ticket = FxContext.getUserTicket();
                boolean _edit;
                boolean _create;
                boolean _delete;
                boolean _export;
                boolean _relate;
                final boolean _system = FxContext.get().getRunAsSystem() || ticket.isGlobalSupervisor();
                final List<Long> acls = fetchACLs(con, _ref, _acl);
                FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.READ, _type, _stepACL, acls, true);
                FxPermissionUtils.checkMandatorExistance(_mandator);
                FxPermissionUtils.checkTypeAvailable(_type.getId(), true);
                if (_system || ticket.isMandatorSupervisor() && _mandator == ticket.getMandatorId() ||
                        !_type.isUsePermissions() || ticket.isInGroup((int) UserGroup.GROUP_OWNER) && _createdBy == ticket.getUserId()) {
                    _edit = _create = _delete = _export = _relate = true;
                } else {
                    //throw exception if read is forbidden
                    FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.READ, _type, _stepACL, acls, true);
                    _edit = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.EDIT, _type, _stepACL, acls, false);
                    _relate = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.RELATE, _type, _stepACL, acls, false);
                    _delete = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.DELETE, _type, _stepACL, acls, false);
                    _export = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.EXPORT, _type, _stepACL, acls, false);
                    _create = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.CREATE, _type, _stepACL, acls, false);
                }
                final String TRUE = StorageManager.getBooleanTrueExpression();
                FxString label = Database.loadContentDataFxString(con, "FTEXT1024", "ID=" + _ref.getId() + " AND " +
                        (mode == FxTreeMode.Live ? "ISMAX_VER=" + TRUE : "ISLIVE_VER=" + TRUE) + " AND TPROP=" +
                        EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_CAPTION_PROPERTY));
                final FxLock lock;
                final long lockUser = rs.getLong(18);
                if (rs.wasNull())
                    lock = FxLock.noLockPK();
                else
                    lock = new FxLock(FxLockType.getById(rs.getInt(19)), rs.getLong(20), rs.getLong(21), lockUser, _ref);
                return new FxTreeNode(mode, lock, _id, _parent, _ref, LifeCycleInfoImpl.load(rs, 16, 22, 23, 24), _type.getId(), acls, _name, rs.getString(25), label,
                        _pos, new ArrayList<FxTreeNode>(0), new ArrayList<Long>(0), _depth, _directChilds,
                        _directChilds == 0, _dirty, _modified, _data, _edit, _create, _delete, _relate, _export);
            } else
                throw new FxLoadException("ex.tree.node.notFound", nodeId, mode);
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "ex.tree.load.failed.node", nodeId, mode, exc.getMessage());
        } finally {
            Database.closeObjects(GenericTreeStorage.class, null, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<FxTreeNode> getNodesWithReference(Connection con, FxTreeMode mode, long referenceId)
            throws FxApplicationException {
        PreparedStatement ps = null;
        List<FxTreeNode> ret = new ArrayList<FxTreeNode>(20);
        long _id = -1;
        try {
            ps = con.prepareStatement(prepareSql(mode, mode == FxTreeMode.Live ? TREE_LIVE_GETNODE_REF : TREE_EDIT_GETNODE_REF));
            List<Long> acls = null;
            ps.setBoolean(1, mode == FxTreeMode.Live);
            ps.setLong(2, referenceId);
            ps.setBoolean(3, true);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                _id = rs.getLong(1);
                FxPK _ref = new FxPK(rs.getLong(2), rs.getInt(14));
                int _depth = rs.getInt(3);
                int _totalChilds = rs.getInt(4);
                int _directChilds = rs.getInt(5);
                String _name = rs.getString(6);
                long _parent = rs.getLong(7);
                boolean _dirty = rs.getBoolean(8);
                String _data = rs.getString(9);
                if (rs.wasNull())
                    _data = null;
                long _modified = rs.getLong(10);
                int _pos = rs.getInt(11);
                long _acl = rs.getLong(12);
                FxType _type = CacheAdmin.getEnvironment().getType(rs.getLong(13));
                long _stepACL = CacheAdmin.getEnvironment().getStep(rs.getLong(15)).getAclId();
                long _createdBy = rs.getLong(16);
                long _mandator = rs.getLong(17);
                if (acls == null) {
                    // fetch ACLs only once, since they are only dependent on the reference instance
                    acls = fetchACLs(con, _ref, _acl);
                }
                UserTicket ticket = FxContext.getUserTicket();
                boolean _read;
                boolean _edit;
                boolean _create;
                boolean _delete;
                boolean _export;
                boolean _relate;
                final boolean _system = FxContext.get().getRunAsSystem() || ticket.isGlobalSupervisor();
                FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.READ, _type, _stepACL, acls, true);
                if (_system || ticket.isMandatorSupervisor() && _mandator == ticket.getMandatorId() ||
                        !_type.isUsePermissions() || ticket.isInGroup((int) UserGroup.GROUP_OWNER) && _createdBy == ticket.getUserId()) {
                    _read = _edit = _create = _delete = _export = _relate = true;
                } else {
                    //throw exception if read is forbidden
                    _read = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.READ, _type, _stepACL, acls, false);
                    _edit = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.EDIT, _type, _stepACL, acls, false);
                    _relate = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.RELATE, _type, _stepACL, acls, false);
                    _delete = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.DELETE, _type, _stepACL, acls, false);
                    _export = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.EXPORT, _type, _stepACL, acls, false);
                    _create = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.CREATE, _type, _stepACL, acls, false);
                }
                if (!_read)
                    continue;
                final String TRUE = StorageManager.getBooleanTrueExpression();
                FxString label = Database.loadContentDataFxString(con, "FTEXT1024", "ID=" + _ref.getId() + " AND " +
                        (mode == FxTreeMode.Live ? "ISMAX_VER=" + TRUE : "ISLIVE_VER=" + TRUE) + " AND TPROP=" +
                        EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_CAPTION_PROPERTY));
                final FxLock lock;
                final long lockUser = rs.getLong(18);
                if (rs.wasNull())
                    lock = FxLock.noLockPK();
                else
                    lock = new FxLock(FxLockType.getById(rs.getInt(19)), rs.getLong(20), rs.getLong(21), lockUser, _ref);
                FxTreeNode node = new FxTreeNode(mode, lock, _id, _parent, _ref, LifeCycleInfoImpl.load(rs, 16, 22, 23, 24),
                        _type.getId(), acls, _name, getPathById(con, mode, _id), label,
                        _pos, new ArrayList<FxTreeNode>(0), new ArrayList<Long>(0), _depth, _directChilds,
                        _directChilds == 0, _dirty, _modified, _data, _edit, _create, _delete, _relate, _export);
                ret.add(node);
            }
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "ex.tree.load.failed.node", _id, mode, exc.getMessage());
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (Exception exc) {/*ignore*/}
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    public FxTreeNode getTree(Connection con, ContentEngine ce, FxTreeMode mode, long nodeId, int depth,
                              boolean loadPartial, FxLanguage partialLoadLanguage) throws FxApplicationException {
        Statement ps = null;
//        long time = System.currentTimeMillis();
//        long nodes = 0;
        try {
            final long captionProperty = EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_CAPTION_PROPERTY);
            final DBStorage storage = StorageManager.getStorageImpl();
            final String TRUE = storage.getBooleanTrueExpression();
            final String FALSE = storage.getBooleanFalseExpression();
            final String version = mode == FxTreeMode.Live ? "ISLIVE_VER=" + TRUE : "ISMAX_VER=" + TRUE;
            String label_pos = loadPartial
                    ? "COALESCE(COALESCE(" +
                    "(SELECT f.FTEXT1024 FROM " + TBL_CONTENT_DATA + " f WHERE f.TPROP=" +
                    captionProperty + " AND LANG IN(" + partialLoadLanguage.getId() + ",0)AND f." + version + " AND f.ID=n.REF " + storage.getLimit(true, 1) + ")," +
                    "(SELECT f.FTEXT1024 from " + TBL_CONTENT_DATA + " f where f.tprop=" +
                    captionProperty + " AND ISMLDEF=" + TRUE + " AND f." + version + " AND f.ID=n.REF " + storage.getLimit(true, 1) + ")" +
                    "),n.NAME)"
                    : "tree_getPosition(" + (mode == FxTreeMode.Live ? TRUE : FALSE) + ",n.ID,n.PARENT)";
            //                     1    2     3       4                  5            6      7        8       9          10              11              12    13     14    15     16           17       18        19         20           21           22           23            24
            String sql = "SELECT n.ID,n.REF,n.DEPTH,-1 AS TOTAL_CHILDCOUNT,n.CHILDCOUNT,n.NAME,n.PARENT,n.DIRTY,n.TEMPLATE,n.MODIFIED_AT," + label_pos + ",c.ACL,c.TDEF,c.VER,c.STEP,c.CREATED_BY,c.MANDATOR,l.USER_ID,l.LOCKTYPE,l.CREATED_AT,l.EXPIRES_AT,c.CREATED_AT,c.MODIFIED_BY,c.MODIFIED_AT " +
                    "FROM " + getTable(mode) + " r, " + getTable(mode) + " n, " + TBL_CONTENT + " c LEFT JOIN " + TBL_LOCKS + " l ON (l.LOCK_ID=c.ID AND l.LOCK_VER=c.VER) WHERE r.ID=" + nodeId + " AND n.LFT>=r.LFT AND n.LFT<=r.RGT AND n.DEPTH<=(r.DEPTH+" + depth + ") AND c.ID=n.REF AND c." + version + " ORDER BY n.LFT";

            ps = con.createStatement();
            ResultSet rs = ps.executeQuery(sql);
            Map<Long, FxTreeNode> data = new HashMap<Long, FxTreeNode>(100, 2.0f);
            //prepare infos needed to compute permissions
            UserTicket ticket = FxContext.getUserTicket();
            final boolean _system = FxContext.get().getRunAsSystem() || ticket.isGlobalSupervisor();
            //StepId->step ACL Id lookup
            Map<Long, Long> step2stepACLId = new HashMap<Long, Long>(10);
            //typeId->FxType lookup
            Map<Long, FxType> typeId2Type = new HashMap<Long, FxType>(10);
            while (rs.next()) {
//                nodes++;
                long _id = rs.getLong(1);
                FxPK _ref = new FxPK(rs.getLong(2), rs.getInt(14));
                int _depth = rs.getInt(3);
                int _totalChilds = rs.getInt(4);
                int _directChilds = rs.getInt(5);
                String _name = rs.getString(6);
                long _parent = rs.getLong(7);
                boolean _dirty = rs.getBoolean(8);
                String _data = rs.getString(9);
                if (rs.wasNull())
                    _data = null;
                long _modified = rs.getLong(10);
                int _pos;
                FxString label;
                if (loadPartial) {
                    _pos = FxTreeNode.PARTIAL_LOADED_POS;
                    label = new FxString(true, partialLoadLanguage.getId(), rs.getString(11));
                } else {
                    _pos = rs.getInt(11);
                    label = Database.loadContentDataFxString(con, "FTEXT1024", "ID=" + _ref.getId() + " AND " +
                            version + " AND TPROP=" + EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_CAPTION_PROPERTY));
                }
                if (label.isEmpty())
                    label = new FxString(true, _name); //TODO: check if label should be multilanguage!
                long _acl = rs.getLong(12);
                long _tdef = rs.getLong(13);
                long _step = rs.getLong(15);
                long _createdBy = rs.getLong(16);
                long _mandator = rs.getLong(17);
                final List<Long> acls = fetchACLs(con, _ref, _acl);

                boolean _read;
                boolean _edit;
                boolean _create;
                boolean _delete;
                boolean _export;
                boolean _relate;

                FxType type = typeId2Type.get(_tdef);
                final FxEnvironment env = CacheAdmin.getEnvironment();
                if (type == null) {
                    type = env.getType(_tdef);
                    typeId2Type.put(_tdef, type);
                }
                Long _stepACL = step2stepACLId.get(_step);
                if (_stepACL == null) {
                    _stepACL = env.getStep(_step).getAclId();
                    step2stepACLId.put(_step, _stepACL);
                }
                if (_system || ticket.isMandatorSupervisor() && _mandator == ticket.getMandatorId() ||
                        !type.isUsePermissions() || ticket.isInGroup((int) UserGroup.GROUP_OWNER) && _createdBy == ticket.getUserId()) {
                    _read = _edit = _create = _delete = _export = _relate = true;
                } else {
                    _read = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.READ, type, _stepACL, acls, false);
                    _edit = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.EDIT, type, _stepACL, acls, false);
                    _relate = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.RELATE, type, _stepACL, acls, false);
                    _delete = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.DELETE, type, _stepACL, acls, false);
                    _export = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.EXPORT, type, _stepACL, acls, false);
                    _create = FxPermissionUtils.checkPermission(ticket, _createdBy, ACLPermission.CREATE, type, _stepACL, acls, false);
                }
                if (_read && (!env.getMandator(_mandator).isActive() || type.getState() == TypeState.Unavailable))
                    _read = false; //filter out inactive mandators and unavailable types
                if (_read) {
                    final FxLock lock;
                    final long lockUser = rs.getLong(18);
                    if (rs.wasNull())
                        lock = FxLock.noLockPK();
                    else
                        lock = new FxLock(FxLockType.getById(rs.getInt(19)), rs.getLong(20), rs.getLong(21), lockUser, _ref);
                    FxTreeNode node = new FxTreeNode(mode, lock, _id, _parent, _ref, LifeCycleInfoImpl.load(rs, 16, 22, 23, 24),
                            _tdef, acls, _name, FxTreeNode.PATH_NOT_LOADED, label,
                            _pos, new ArrayList<FxTreeNode>(10), new ArrayList<Long>(10), _depth, _directChilds,
                            _directChilds == 0, _dirty, _modified, _data, _edit, _create, _delete, _relate, _export);
                    FxTreeNode parent = data.get(node.getParentNodeId());
                    data.put(_id, node);
                    if (parent != null)
                        parent._addChild(node);
                }
            }
            final FxTreeNode root = data.get(nodeId);
            if (root == null)
                throw new FxNotFoundException(LOG, "ex.tree.node.notFound", nodeId, mode);
            FxTreeNode _root = getNode(con, mode, root.getId());
            root._applyPath(_root.getPath());
            root._applyPosition(_root.getPosition());
            return root;
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "ex.tree.load.failed.tree", nodeId, mode, exc.getMessage(), depth);
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (Exception exc) {
                //ignore
            }
//            System.out.println(nodes + " nodes loaded in " + (System.currentTimeMillis() - time) + "[ms]. Partial:" + loadPartial);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clearTree(Connection con, ContentEngine ce, FxTreeMode mode) throws FxApplicationException {
        Statement stmt = null;
        try {
            FxPK rootPK;

            FxTreeMode rootCheckMode = mode == FxTreeMode.Live ? FxTreeMode.Edit : FxTreeMode.Live;
//            Savepoint sp = null;
//            final boolean needSavePoints = StorageManager.isRollbackOnConstraintViolation();
            try {
//                if (needSavePoints) sp = con.setSavepoint();
                rootPK = getTreeNodeInfo(con, rootCheckMode, FxTreeNode.ROOT_NODE).getReference();
//                if (needSavePoints) con.releaseSavepoint(sp);
            } catch (FxApplicationException e) {
                LOG.info("Creating a new root node for the " + mode.name() + " tree.");
//                if (needSavePoints && sp != null) con.rollback(sp);
                //create the root folder instance
                FxType type = CacheAdmin.getEnvironment().getType(FxType.FOLDER);
                FxContent content = ce.initialize(type.getId());
                List<FxPropertyAssignment> captions =
                        type.getAssignmentsForProperty(EJBLookup.getConfigurationEngine().
                                get(SystemParameters.TREE_CAPTION_PROPERTY));
                if (captions.size() > 0) {
                    FxPropertyAssignment paCaption = captions.get(0);
                    FxString label = new FxString(paCaption.isMultiLang(), "Root");
                    content.setValue(paCaption.getXPath(), label);
                }
                rootPK = ce.save(content);
            }

            stmt = con.createStatement();
            List<Long> folderIds = new ArrayList<Long>(50);

            //fetch all referenced folders
            ResultSet rs = stmt.executeQuery("SELECT DISTINCT c.ID FROM " + TBL_CONTENT + " c WHERE c.TDEF=" + CacheAdmin.getEnvironment().getType(FxType.FOLDER).getId() + " AND c.ID IN (SELECT DISTINCT t.REF FROM " + getTable(mode) + " t)");
            while (rs != null && rs.next()) {
                if (rootPK.getId() != rs.getLong(1))
                    folderIds.add(rs.getLong(1));
            }

            wipeTree(mode, stmt, rootPK);

            //reset sequencer
            StorageManager.getSequencerStorage().setSequencerId(mode.getSequencer().getSequencerName(), 1);
            if (mode == FxTreeMode.Live) {
                //flag all edit nodes back to dirty
                stmt.executeUpdate("UPDATE " + getTable(FxTreeMode.Edit) + " SET DIRTY=" +
                        StorageManager.getBooleanTrueExpression() + " WHERE ID<>" + ROOT_NODE);
            }
//            stmt.executeBatch();
            if (folderIds.size() > 0) {
                //remove all referenced folders (or try to at least)
                int removed = 0;
                for (long folderId : folderIds) {
                    if (folderId == rootPK.getId())
                        continue; //no need to test reference count for the root node as it IS used
                    FxPK folderPK = new FxPK(folderId);
                    if (ce.getReferencedContentCount(folderPK) == 0) {
                        ce.remove(folderPK);
                        removed++;
                    }
                }
                LOG.info("Removed " + removed + "/" + folderIds.size() + " Folder instances");
            }
        } catch (Exception e) {
            throw new FxTreeException(LOG, e, "ex.tree.clear.failed", mode.name(), e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Throwable t) {
                /*ignore*/
            }
        }
    }

    /**
     * Remove all tree nodes and create a new root node
     *
     * @param mode   tree mode
     * @param stmt   statement
     * @param rootPK primary key of the root content instance
     * @throws SQLException on errors
     */
    protected abstract void wipeTree(FxTreeMode mode, Statement stmt, FxPK rootPK) throws SQLException;

    /**
     * Lock tree nodes for updates.
     *
     * @param con     an existing connection
     * @param table   the table name to be locked
     * @param nodeIds the node ID(s) to be locked. If null or empty, the entire table will be locked.
     * @return true if a lock could be required, false if the DBMS indicated a deadlock
     * @throws FxDbException on non-deadlock DB errors
     */
    protected abstract boolean lockForUpdate(Connection con, String table, Iterable<Long> nodeIds) throws FxDbException;

    /**
     * Lock tree nodes for updates.
     *
     * @param con     an existing connection
     * @param table   the table name to be locked
     * @param referenceIds the reference ID(s) to be locked. If null or empty, the entire table will be locked.
     * @return true if a lock could be required, false if the DBMS indicated a deadlock
     * @throws FxDbException on non-deadlock DB errors
     */
    protected abstract boolean lockForUpdateReference(Connection con, String table, Iterable<Long> referenceIds) throws FxDbException;

    /**
     * Acquire update locks for updating tree nodes. To lock the entire tree, pass {@code null}
     * or an empty list for {@code nodeIds}.
     * <p>
     * If (some of) the nodes could not be locked due to a rollback by the DBMS, the method waits for a
     * short period of time before it tries to acquire the lock again. Non-deadlock exceptions
     * are not caught and are thrown to the caller.
     * </p>
     *
     * @param con     an existing connection
     * @param mode    the tree mode
     * @param nodeIds the tree node IDs
     * @throws FxDbException on database errors
     */
    protected void acquireLocksForUpdate(Connection con, FxTreeMode mode, Iterable<Long> nodeIds) throws FxDbException {
        while (!lockForUpdate(con, getTable(mode), nodeIds)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Failed to lock " + getTable(mode) + ", waiting 100ms and retrying...");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * Acquire update locks for a node and all of its parents and/or children.
     * <p>
     * If the subtree could not be locked due to a rollback by the DBMS, the method waits for a
     * short period of time before it tries to acquire the lock again. Non-deadlock exceptions
     * are not caught and are thrown to the called.
     * </p>
     *
     * @param con        an existing connection
     * @param nodeInfo   the node to be locked
     * @param lockParent if the node's parent should also be locked
     * @throws FxDbException on database errors
     */
    protected void acquireLocksForUpdate(Connection con, FxTreeNodeInfo nodeInfo, boolean lockParent) throws FxDbException {
        final List<Long> ids = new ArrayList<Long>(2);
        ids.add(nodeInfo.getId());
        if (lockParent) {
            ids.add(nodeInfo.getParentId());
        }
        acquireLocksForUpdate(con,
                nodeInfo.getMode(),
                ids
        );
    }

    protected void acquireLocksByReference(Connection con, FxTreeMode mode, Iterable<Long> referenceIds) throws FxDbException {
        while (!lockForUpdateReference(con, getTable(mode), referenceIds)) {
            if (LOG.isInfoEnabled()) {
                LOG.info("Failed to lock " + getTable(mode) + " for reference list "
                        + referenceIds + ", waiting 100ms and retrying...");
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
    }

    /**
     * Acquire update locks for the entire node tree.
     * <p>
     * If the node could not be locked due to a rollback by the DBMS, the method waits for a
     * short period of time before it tries to acquire the lock again. Non-deadlock exceptions
     * are not caught and are thrown to the called.
     * </p>
     *
     * @param con  an existing connection
     * @param mode the tree mode (edit or live)
     * @throws FxDbException on database errors
     */
    protected void acquireLocksForUpdate(Connection con, FxTreeMode mode) throws FxDbException {
        acquireLocksForUpdate(con, mode, null);
    }

    /**
     * {@inheritDoc}
     */
    public void removeNode(Connection con, FxTreeMode mode, ContentEngine ce, long nodeId, boolean removeChildren) throws FxApplicationException {
        if (mode == FxTreeMode.Live)
            removeChildren = true; //always delete child nodes in live mode
        Statement stmt = null;
        if (nodeId == FxTreeNode.ROOT_NODE)
            throw new FxNoAccessException("ex.tree.delete.root");

        FxTreeNodeInfo nodeInfo = getTreeNodeInfo(con, mode, nodeId);
        ScriptingEngine scripting = EJBLookup.getScriptingEngine();
        final List<Long> scriptBeforeIds = scripting.getByScriptEvent(FxScriptEvent.BeforeTreeNodeRemoved);
        final List<Long> scriptAfterIds = scripting.getByScriptEvent(FxScriptEvent.AfterTreeNodeRemoved);
        //warning: removedNodes will only be available if script mappings for event AfterTreeNodeRemoved exist!
        List<FxTreeNode> removedNodes = scriptAfterIds.size() > 0 ? new ArrayList<FxTreeNode>(100) : null;
        final String TRUE = StorageManager.getBooleanTrueExpression();
        try {
            stmt = con.createStatement();
            if (StorageManager.isDisableIntegrityTransactional()) {
                stmt.execute(StorageManager.getReferentialIntegrityChecksStatement(false));
            }
            List<FxPK> references = new ArrayList<FxPK>(50);
            UserTicket ticket = FxContext.getUserTicket();

            // lock all affected rows
            final List<Long> removeNodeIds = selectAllChildNodeIds(con, mode, nodeInfo.getLeft(), nodeInfo.getRight(), true);
            acquireLocksForUpdate(con, mode, Iterables.concat(removeNodeIds, Arrays.asList(nodeInfo.getParentId())));
            final Map<FxPK, FxContentSecurityInfo> securityInfos = Maps.newHashMapWithExpectedSize(removeNodeIds.size());

            if (removeChildren) {
                //FX-102: edit permission checks on references
                ResultSet rs = stmt.executeQuery("SELECT DISTINCT REF FROM " + getTable(mode) + " WHERE "
                        + " LFT>=" + nodeInfo.getLeft() + " AND RGT<=" + nodeInfo.getRight() + " ");
                while (rs != null && rs.next()) {
                    try {
                        if (ce != null) {
                            final FxPK pk = new FxPK(rs.getLong(1));
                            final FxContentSecurityInfo info = ce.getContentSecurityInfo(pk);
                            FxPermissionUtils.checkPermission(ticket, ACLPermission.EDIT, info, true);
                            securityInfos.put(pk, info);
                        }
                        references.add(new FxPK(rs.getLong(1)));
                    } catch (FxLoadException e) {
                        //ignore, might have been removed meanwhile
                    }
                }
                // call BeforeTreeNodeRemoved scripts
                if (scriptBeforeIds.size() > 0 || scriptAfterIds.size() > 0) {
                    final FxScriptBinding binding = new FxScriptBinding();
                    for (long removedId : removeNodeIds) {
                        final FxTreeNode n = getNode(con, mode, removedId);
                        if (removedNodes != null)
                            removedNodes.add(n);
                        for (long scriptId : scriptBeforeIds) {
                            binding.setVariable("node", n);
                            scripting.runScript(scriptId, binding);
                        }
                    }
                }

                for (List<Long> removeIds : Iterables.partition(removeNodeIds, SQL_IN_PARTSIZE)) {
                    stmt.addBatch(
                            "DELETE FROM " + getTable(mode)
                                    + " WHERE id IN ("
                                    + StringUtils.join(removeIds, ',')
                                    + ")"
                    );
                }
            } else {
                //FX-102: edit permission checks on references
                try {
                    if (ce != null) {
                        final FxContentSecurityInfo info = ce.getContentSecurityInfo(nodeInfo.getReference());
                        FxPermissionUtils.checkPermission(FxContext.getUserTicket(), ACLPermission.EDIT, info, true);
                        securityInfos.put(nodeInfo.getReference(), info);
                    }
                    references.add(nodeInfo.getReference());
                } catch (FxLoadException e) {
                    //ignore, might have been removed meanwhile
                }
                stmt.addBatch("UPDATE " + getTable(mode) + " SET PARENT=" + nodeInfo.getParentId() + " WHERE PARENT=" + nodeId);
                for (List<Long> part : Iterables.partition(removeNodeIds, SQL_IN_PARTSIZE)) {
                    stmt.addBatch("UPDATE " + getTable(mode) + " SET DEPTH=DEPTH-1,DIRTY=" +
                            StorageManager.getBooleanExpression(mode != FxTreeMode.Live) +
                            " WHERE id IN (" + StringUtils.join(part, ',') + ") AND DEPTH>0");
                }
                stmt.addBatch("DELETE FROM " + getTable(mode) + " WHERE ID=" + nodeId);
            }

            // Update the childcount of the parents
            if (removeChildren) {
                stmt.addBatch("UPDATE " + getTable(mode) + " SET CHILDCOUNT=CHILDCOUNT-1 WHERE ID=" + nodeInfo.getParentId());
            } else {
                stmt.addBatch("UPDATE " + getTable(mode) + " SET CHILDCOUNT=CHILDCOUNT+" +
                        (nodeInfo.getDirectChildCount() - 1) + " WHERE ID=" + nodeInfo.getParentId());
            }

            // Set the dirty flag for the parent if needed
            if (mode != FxTreeMode.Live) {
                stmt.addBatch("UPDATE " + getTable(mode) + " SET DIRTY=" + TRUE + " WHERE ID=" + nodeInfo.getParentId());
            }

            if (mode == FxTreeMode.Live && exists(con, FxTreeMode.Edit, nodeId)) {
                //check if a node with the same id that has been removed in the live tree exists in the edit tree,
                //the node and all its children will be flagged as dirty in the edit tree
                FxTreeNodeInfo editNode = getTreeNodeInfo(con, FxTreeMode.Edit, nodeId);
                List<Long> editNodeIds = selectAllChildNodeIds(con, FxTreeMode.Edit, editNode.getLeft(), editNode.getRight(), true);

                acquireLocksForUpdate(con, FxTreeMode.Edit, editNodeIds);
                for (List<Long> part : Iterables.partition(editNodeIds, SQL_IN_PARTSIZE)) {
                    stmt.addBatch("UPDATE " + getTable(FxTreeMode.Edit) + " SET DIRTY=" + TRUE +
                            " WHERE ID IN (" + StringUtils.join(part, ',') + ")");
                }
            }
            stmt.executeBatch();
            if (ce != null) {
                //if the referenced content is a folder, remove it
                final Set<Long> folderTypeIds = Sets.newHashSet(FxSharedUtils.getSelectableObjectIdList(
                        CacheAdmin.getEnvironment().getType(FxType.FOLDER).getDerivedTypes(true, true)
                ));
                for (FxPK ref : references) {
                    FxContentSecurityInfo si = securityInfos.get(ref);
                    if (si == null) {
                        si = ce.getContentSecurityInfo(ref);
                    }
                    if (folderTypeIds.contains(si.getTypeId())) {
                        final int contentCount = ce.getReferencedContentCount(si.getPk());
                        if (contentCount == 0) {
                            ce.remove(ref);
                        }
                    }
                }
            }
            afterNodeRemoved(con, nodeInfo, removeChildren);
            if (removedNodes != null) {
                final FxScriptBinding binding = new FxScriptBinding();
                for (long scriptId : scriptAfterIds) {
                    for (FxTreeNode n : removedNodes) {
                        binding.setVariable("node", n);
                        scripting.runScript(scriptId, binding);
                    }

                }
            }
        } catch (SQLException exc) {
            String next = "";
            if (exc.getNextException() != null)
                next = " next:" + exc.getNextException().getMessage();
            throw new FxRemoveException(LOG, exc, "ex.tree.delete.failed", nodeId, exc.getMessage() + next);
        } finally {
            try {
                if (stmt != null) {
                    if (StorageManager.isDisableIntegrityTransactional()) {
                        try {
                            stmt.execute(StorageManager.getReferentialIntegrityChecksStatement(true));
                        } catch (SQLException e) {
                            LOG.error(e);
                        }
                    }
                    stmt.close();
                }
            } catch (Exception exc) {
                //ignore
            }
        }
    }

    /**
     * Get all node ids between the left and right boundaries
     *
     * @param con   an open and valid connection
     * @param mode  tree mode
     * @param left  left boundary
     * @param right right boundary
     * @param includeRoot if the root node (of the subtree) itself should be included
     * @return node id's between the boundaries
     * @throws SQLException on errors
     */
    protected List<Long> selectAllChildNodeIds(Connection con, FxTreeMode mode, Number left, Number right, boolean includeRoot) throws SQLException {
        PreparedStatement stmt = null;
        try {
            final String rangeIncl = includeRoot ? "=" : "";
            stmt = con.prepareStatement("SELECT id FROM " + getTable(mode) 
                    + " WHERE lft >" + rangeIncl + " ? AND rgt <" + rangeIncl + " ?");
            setNodeBounds(stmt, 1, new BigInteger(left.toString()));
            setNodeBounds(stmt, 2, new BigInteger(right.toString()));
            return collectNodeIds(stmt);
        } finally {
            Database.closeObjects(GenericTreeStorage.class, null, stmt);
        }
    }

    /**
     * Get all node ids that are a direct child of <code>parentId</code>
     *
     * @param con           an open and valid connection
     * @param mode          tree mode
     * @param parentId      the parent node id
     * @param includeParent include the parent id in the returned node id list?
     * @return found node id's
     * @throws SQLException on errors
     */
    protected List<Long> selectDirectChildNodeIds(Connection con, FxTreeMode mode, long parentId, boolean includeParent) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(
                    "SELECT id FROM " + getTable(mode) + " WHERE PARENT=? "
                            + (includeParent ? "OR ID=?" : "")
            );
            stmt.setLong(1, parentId);
            if (includeParent) {
                stmt.setLong(2, parentId);
            }
            return collectNodeIds(stmt);
        } finally {
            Database.closeObjects(GenericTreeStorage.class, null, stmt);
        }
    }

    /**
     * Collect all long values of the first column in the result set as a list of long's
     *
     * @param stmt the prepared statement that contains the query, ready to be executed
     * @return list of found values
     * @throws SQLException on errors
     */
    private List<Long> collectNodeIds(PreparedStatement stmt) throws SQLException {
        final ResultSet rs = stmt.executeQuery();
        final List<Long> ids = new ArrayList<Long>();
        while (rs.next()) {
            ids.add(rs.getLong(1));
        }
        return ids;
    }

    /**
     * Perform needed cleanups like closing gaps after a node has been removed
     *
     * @param con            an open and valid connection
     * @param nodeInfo       info about the removed node
     * @param removeChildren children removed or moved up a level?
     * @throws FxApplicationException on errors
     */
    protected abstract void afterNodeRemoved(Connection con, FxTreeNodeInfo nodeInfo, boolean removeChildren) throws FxApplicationException;

    /**
     * {@inheritDoc}
     */
    public void setData(Connection con, FxTreeMode mode, long nodeId, String data) throws FxApplicationException {
        checkDataValue(data);

        FxTreeNodeInfo node = getTreeNodeInfo(con, mode, nodeId);
        // Any changes at all?
        if (node.hasData(data)) return;
        // Set the data
        PreparedStatement ps = null;
        try {
            DBStorage st = StorageManager.getStorageImpl();
            ps = con.prepareStatement("UPDATE " + getTable(mode) + " SET TEMPLATE=?,DIRTY=" + st.getBooleanTrueExpression() +
                    ",MODIFIED_AT=" + st.getTimestampFunction() + " WHERE ID=" + nodeId);
            if (data == null)
                ps.setNull(1, java.sql.Types.VARCHAR);
            else
                ps.setString(1, data);
            ps.executeUpdate();
        } catch (Throwable t) {
            throw new FxUpdateException(LOG, "ex.tree.setData.failed", data, nodeId, t.getMessage());
        } finally {
            try {
                if (ps != null) ps.close();
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void activateAll(Connection con, FxTreeMode mode) throws FxTreeException {
        Statement stmt = null;
        PreparedStatement ps = null;
        try {
            final String FALSE = StorageManager.getBooleanFalseExpression();
            acquireLocksForUpdate(con, FxTreeMode.Live);
            stmt = con.createStatement();
            stmt.addBatch(StorageManager.getReferentialIntegrityChecksStatement(false));
            stmt.addBatch("DELETE FROM " + getTable(FxTreeMode.Live));
            stmt.addBatch("INSERT INTO " + getTable(FxTreeMode.Live) + " SELECT * FROM " + getTable(mode));
            stmt.addBatch("UPDATE " + getTable(mode) + " SET DIRTY=" + FALSE);
            stmt.addBatch("UPDATE " + getTable(FxTreeMode.Live) + " SET DIRTY=" + FALSE);
            stmt.addBatch(StorageManager.getReferentialIntegrityChecksStatement(true));
            stmt.executeBatch();
            //FX-793: activate nodes that are not in the live version
            ps = con.prepareStatement("SELECT DISTINCT c.ID, c.MAX_VER FROM " + getTable(FxTreeMode.Live) +
                    " l, " + DatabaseConst.TBL_CONTENT + " c WHERE l.REF IS NOT NULL AND c.ID=l.REF AND c.LIVE_VER=0");
            ResultSet rs = ps.executeQuery();
            final ContentEngine ce = EJBLookup.getContentEngine();
            while (rs != null && rs.next()) {
                FxPK pk = new FxPK(rs.getLong(1), rs.getInt(2));
                FxContent co = ce.load(pk);
                //create a Live version
                pk = createContentLiveVersion(ce, co);
                LOG.info("Created new live version " + pk + " during tree activation");
            }
            if (rs != null)
                rs.close();
        } catch (Throwable t) {
            throw new FxTreeException(LOG, t, "ex.tree.activate.all.failed", mode.name(), t.getMessage());
        } finally {
            Database.closeObjects(GenericTreeStorage.class, stmt, ps);
        }
    }

    /**
     * Gets the next CopyOf___(Id)
     *
     * @param con          an open and valid connection
     * @param mode         tree mode
     * @param copyOfPrefix the prefix to search for
     * @param parentNodeId parent node id
     * @param nodeId       node id
     * @return the CopyOf number
     * @throws FxTreeException on errors
     */
    protected int getCopyOfCount(Connection con, FxTreeMode mode, String copyOfPrefix, long parentNodeId, long nodeId) throws FxTreeException {
        Statement stmt = null;
        try {
            // Update the childcount of the new parents
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + getTable(mode) + " WHERE PARENT=" + parentNodeId + " AND NAME LIKE " +
//                    "(SELECT CONCAT(CONCAT('" + copyOfPrefix + "%',NAME),'%') FROM " + getTable(mode) + " WHERE ID=" + nodeId + ")");
                    "(SELECT " + StorageManager.concat("'" + copyOfPrefix + "%'", "NAME", "'%'") + " FROM " + getTable(mode) + " WHERE ID=" + nodeId + ")");
            int result = 0;
            if (rs.next()) {
                result = rs.getInt(1);
            }
            return result;
        } catch (SQLException e) {
            throw new FxTreeException(LOG, "ex.tree.copyOf.failed", nodeId, parentNodeId, mode.name(), copyOfPrefix, e.getMessage());
        } finally {
            try {
                if (stmt != null) stmt.close();
            } catch (Exception exc) {
                //ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void populate(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, int maxNodeChildren) throws FxApplicationException {
        try {
            for (int i = 0; i < maxNodeChildren; i++) {
                LOG.info("Creating level [" + (i + 1) + "/" + maxNodeChildren + "]");
                String name = "Level2_[" + i + "]";
                FxString desc = new FxString(true, "Level2 Node " + i);
                long newIdLevel1 = createNode(con, seq, ce, mode, -1, ROOT_NODE, name, desc, 0, null, null, true);
                for (int y = 0; y < 10; y++) {
                    String name2 = "Level3_[" + i + "|" + y + "]";
                    FxString desc2 = new FxString(true, "Level3 Node " + y);
                    long newIdLevel2 = createNode(con, seq, ce, mode, -1, newIdLevel1, name2, desc2, 0, null, null, true);
                    for (int t = 0; t < 100; t++) {
                        String name3 = "Level4_[" + i + "|" + y + "|" + t + "]";
                        FxString desc3 = new FxString(true, "Level4 Node " + y);
                        createNode(con, seq, ce, mode, -1, newIdLevel2, name3, desc3, 0, null, null, true);
                    }
                }
                LOG.info("Created level [" + (i + 1) + "/" + maxNodeChildren + "]");
            }
        } catch (Exception e) {
            throw new FxApplicationException(LOG, e, "ex.tree.populate", mode.name(), e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void contentRemoved(Connection con, long contentId, boolean liveVersionRemovedOnly) throws FxApplicationException {
        PreparedStatement ps = null;
        try {
            // lock affected nodes
            acquireLocksByReference(con, FxTreeMode.Edit, Arrays.asList(contentId));
            acquireLocksByReference(con, FxTreeMode.Live, Arrays.asList(contentId));

            List<Long> edit = new ArrayList<Long>(10), live = new ArrayList<Long>(10);
            long typeId = -1;
            ps = con.prepareStatement(TREE_REF_USAGE_EDIT);
            ps.setLong(1, contentId);
            ResultSet rs = ps.executeQuery();
            while (rs != null && rs.next()) {
                edit.add(rs.getLong(2));
                if (typeId == -1) typeId = rs.getLong(1);
            }
            ps.close();
            ps = con.prepareStatement(TREE_REF_USAGE_LIVE);
            ps.setLong(1, contentId);
            rs = ps.executeQuery();
            while (rs != null && rs.next()) {
                live.add(rs.getLong(2));
                if (typeId == -1) typeId = rs.getLong(1);
            }
            if (typeId == -1)
                return; //nothing found
            ContentEngine ce = EJBLookup.getContentEngine();
            FxPK folderPK = null;
            for (long liveNode : live) {
                boolean inEdit = edit.contains(liveNode);
                if (liveVersionRemovedOnly || !inEdit)
                    try {
                        removeNode(con, FxTreeMode.Live, ce, liveNode, true);
                    } catch (FxNotFoundException e) {
                        //ok, may be an already removed childnode
                    }
                else {
                    //if the node is a leaf then remove the node, else replace the referenced content with a new folder instance
                    try {
                        if (inEdit)
                            folderPK = handleContentDeleted(con, ce, folderPK, typeId, getTreeNodeInfo(con, FxTreeMode.Edit, liveNode));
                    } catch (FxNotFoundException e) {
                        //ok, may be an already removed childnode
                    }
                    try {
                        folderPK = handleContentDeleted(con, ce, folderPK, typeId, getTreeNodeInfo(con, FxTreeMode.Live, liveNode));
                    } catch (FxNotFoundException e) {
                        try {
                            //try to remove even if not found since the node might exist but the
                            //content may not exist in the live step
                            removeNode(con, FxTreeMode.Live, ce, liveNode, true);
                        } catch (FxApplicationException e1) {
                            //ignore
                        }
                        //ok, may be an already removed childnode
                    }
                }
            }
            if (!liveVersionRemovedOnly) {
                edit.removeAll(live);
                //remaining nodes only exist in edit tree
                for (long editNode : edit)
                    try {
                        folderPK = handleContentDeleted(con, ce, folderPK, typeId, getTreeNodeInfo(con, FxTreeMode.Edit, editNode));
                    } catch (FxNotFoundException e) {
                        //ok, may be an already removed childnode
                    }
            }
        } catch (SQLException se) {
            throw new FxTreeException(LOG, "ex.db.sqlError", se.getMessage());
        } finally {
            FxContext.get().setTreeWasModified();
            try {
                if (ps != null) ps.close();
            } catch (Throwable t) {
                //ignore
            }
        }
    }

    /**
     * Handle removal of a content instance
     *
     * @param con      an open and valid connection
     * @param ce       a reference to the content engine
     * @param folderPK folder pk (if null a new one will be created if needed)
     * @param typeId   type of the content
     * @param nodeInfo node info
     * @return folder PK
     * @throws FxApplicationException on errors
     */
    protected FxPK handleContentDeleted(Connection con, ContentEngine ce, FxPK folderPK, long typeId, FxTreeNodeInfo nodeInfo) throws FxApplicationException {
        if (!nodeInfo.hasChildren()) {
            //leaf node - remove it
            removeNode(con, nodeInfo.getMode(), null, nodeInfo.getId(), false);
            return folderPK;
        }
        //if the referenced content is a folder, remove it without removing its children in edit mode and remove it with children in live mode
        if (CacheAdmin.getEnvironment().getType(typeId).isDerivedFrom(FxType.FOLDER)) {
            removeNode(con, nodeInfo.getMode(), null, nodeInfo.getId(), nodeInfo.getMode() == FxTreeMode.Live);
            return folderPK;
        }
        //No folder and no leaf - replace the content with a new folder instance
        FxTreeNode node = getNode(con, nodeInfo.getMode(), nodeInfo.getId());
        if (folderPK == null)
            folderPK = createFolderInstance(ce, node.getName(), node.getLabel());
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE " + getTable(nodeInfo.getMode()) + " SET REF=? WHERE ID=?");
            ps.setLong(1, folderPK.getId());
            ps.setLong(2, node.getId());
            ps.executeUpdate();
        } catch (SQLException se) {
            throw new FxTreeException(LOG, "ex.db.sqlError", se.getMessage());
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                //ignore
            }
        }

        //scripting
        ScriptingEngine scripting = EJBLookup.getScriptingEngine();
        List<Long> scriptIds = scripting.getByScriptEvent(FxScriptEvent.AfterTreeNodeFolderReplacement);
        if (scriptIds.size() == 0)
            return folderPK;
        FxScriptBinding binding = new FxScriptBinding();
        binding.setVariable("node", getNode(con, nodeInfo.getMode(), nodeInfo.getId()));
        binding.setVariable("content", nodeInfo.getReference());
        for (long scriptId : scriptIds)
            scripting.runScript(scriptId, binding);

        return folderPK;
    }

    /**
     * {@inheritDoc}
     */
    public String[] beforeContentVersionRemoved(Connection con, long id, int version, FxContentVersionInfo cvi) throws FxApplicationException {
//        if (cvi.hasLiveVersion() && version == cvi.getLiveVersion()) {
//            contentRemoved(con, id, version != cvi.getMaxVersion(), true);
//        }
        if (cvi.getVersionCount() > 1) {
            //deactivate ref. integrity to allow removal
            Statement stmt = null;
            try {
                stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT DISTINCT ID FROM " + getTable(FxTreeMode.Edit) + " WHERE REF=" + id);
                StringBuilder sb = new StringBuilder(SQL_IN_PARTSIZE);
                while (rs != null && rs.next()) {
                    if (sb.length() > 0)
                        sb.append(",");
                    sb.append(rs.getLong(1));
                }
                String[] nodes = new String[2];
                nodes[0] = sb.toString();
                sb.setLength(0);
                rs = stmt.executeQuery("SELECT DISTINCT ID FROM " + getTable(FxTreeMode.Live) + " WHERE REF=" + id);
                while (rs != null && rs.next()) {
                    if (sb.length() > 0)
                        sb.append(",");
                    sb.append(rs.getLong(1));
                }
                nodes[1] = sb.toString();
                if (nodes[0].length() == 0 && nodes[1].length() == 0)
                    return null;
                stmt.executeUpdate("UPDATE " + getTable(FxTreeMode.Edit) + " SET REF=(SELECT MIN(ID) FROM " +
                        TBL_CONTENT + ") WHERE REF=" + id);
                stmt.executeUpdate("UPDATE " + getTable(FxTreeMode.Live) + " SET REF=(SELECT MIN(ID) FROM " +
                        TBL_CONTENT + ") WHERE REF=" + id);
                return nodes;
            } catch (SQLException e) {
                throw new FxDbException(LOG, e, "ex.db.sqlError", e.getMessage());
            } finally {
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException e) {
                    //ignore
                }
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void afterContentVersionRemoved(String[] nodes, Connection con, long id, int version, FxContentVersionInfo cvi) throws FxApplicationException {
        if (nodes == null || nodes.length == 0)
            return;
        if (nodes.length == 2) {
            //reactivate ref. integrity
            Statement stmt = null;
            try {
                stmt = con.createStatement();
                if (!StringUtils.isEmpty(nodes[0]))
                    stmt.executeUpdate("UPDATE " + getTable(FxTreeMode.Edit) + " SET REF=" + id +
                            " WHERE ID IN (" + nodes[0] + ")");
                if (!StringUtils.isEmpty(nodes[1]))
                    stmt.executeUpdate("UPDATE " + getTable(FxTreeMode.Live) + " SET REF=" + id +
                            " WHERE ID IN (" + nodes[1] + ")");
            } catch (SQLException e) {
                throw new FxDbException(LOG, e, "ex.db.sqlError", e.getMessage());
            } finally {
                try {
                    if (stmt != null)
                        stmt.close();
                } catch (SQLException e) {
                    //ignore
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void checkTreeIfEnabled(Connection con, FxTreeMode mode) throws FxApplicationException {
        if (!EJBLookup.getDivisionConfigurationEngine().get(SystemParameters.TREE_CHECKS_ENABLED))
            return;
        checkTree(con, mode);
    }

    protected List<Long> fetchACLs(Connection con, FxPK reference, long mainTableACL) throws SQLException {
        final List<Long> acls;
        if (mainTableACL == ACL.NULL_ACL_ID) {
            acls = fetchNodeACLs(con, reference);
        } else {
            acls = Arrays.asList(mainTableACL);
        }
        return acls;
    }

    protected List<Long> fetchNodeACLs(Connection con, FxPK reference) throws SQLException {
        PreparedStatement ps2 = null;
        try {
            ps2 = con.prepareStatement(TREE_NODEINFO_ACLS);
            ps2.setLong(1, reference.getId());
            ps2.setInt(2, reference.getVersion());
            ps2.setLong(3, reference.getId());
            ps2.setInt(4, reference.getVersion());
            final ResultSet rs2 = ps2.executeQuery();
            final List<Long> aclIds = Lists.newArrayList();
            while (rs2.next()) {
                aclIds.add(rs2.getLong(1));
            }
            return aclIds;
        } finally {
            Database.closeObjects(GenericTreeStorage.class, null, ps2);
        }
    }


    /**
     * Check a tree for invalid nodes, will throw a FxTreeException on errors
     *
     * @param con  an open and valid connection
     * @param mode the tree to check
     * @throws com.flexive.shared.exceptions.FxTreeException
     *          on errors
     */
    public abstract void checkTree(Connection con, FxTreeMode mode) throws FxApplicationException;

    /**
     * Replace dynamic placeholders in the given select statement.
     *
     * @param sql the SQL statement
     * @return the final SQL statement
     * @since 3.1
     */
    protected String prepareSql(FxTreeMode mode, String sql) {
        String result = sql;
        if (sql.contains(COMPUTE_TOTAL_CHILDCOUNT)) {
            // compute total child count of a node only when childcount > 0
            result = sql.replace(
                    COMPUTE_TOTAL_CHILDCOUNT,
                    StorageManager.getIfFunction(
                            "CHILDCOUNT = 0",
                            "0",
                            mode == FxTreeMode.Live ? TREE_TOTAL_COUNT_LIVE : TREE_TOTAL_COUNT_EDIT
                    )
                            + " AS TOTAL_CHILDCOUNT"
            );
        }
        return result;
    }


    protected BigInteger getNodeBounds(ResultSet rs, int index) throws SQLException {
        final String value = rs.getString(index);
        return value == null ? BigInteger.ZERO : new BigInteger(value);
    }

    protected void setNodeBounds(PreparedStatement stmt, int index, BigInteger value) throws SQLException {
        // Postgres 8.x does not perform automatic conversion from String (unlike other DBMS)
        stmt.setBigDecimal(index, new BigDecimal(value.toString()));
    }

}
