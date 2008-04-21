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

import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxContentVersionInfo;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxTreeException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.SequencerEngine;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.value.FxString;

import java.sql.Connection;
import java.util.List;

/**
 * Tree storage interface.
 * The tree used is an enhanced nested set model tree based on the article found at
 * http://dev.mysql.com/tech-resources/articles/hierarchical-data.html enhanced by using spacing
 * for performance, and adds the parent and depth columns to even more simplify queries.
 * Nested Set Models are optimized for read/query operations, but slow on update/move/create operations. This
 * implementation uses "spacing" between nodes to minimize slow operations - only when a level runs out of "space" a
 * reorganization on a part of the tree is performed (this can take quite a few seconds, dependong on the amount of
 * affected nodes), otherwise the update operation will be almost as fast as when using a standard Adjacency List Model
 * (pure id&lt;->parent based) tree.
 * Since count(*) can be very slow the total childcount of every node is stored within its row.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface TreeStorage {

    public final static long ROOT_NODE = 1;

    /**
     * Get various information about a tree node
     *
     * @param con    an open and valid connection
     * @param mode   Live or Edit mode
     * @param nodeId id of the node to examine
     * @return FxTreeNodeInfo
     * @throws FxApplicationException on errors
     */
    FxTreeNodeInfo getTreeNodeInfo(Connection con, FxTreeMode mode, long nodeId) throws FxApplicationException;

    /**
     * Create a new node
     *
     * @param con          an open and valid Connection
     * @param seq          reference to the sequencer
     * @param ce           reference to the content engine
     * @param mode         tree mode
     * @param nodeId       use this id unless it is < 0 then generate a new one
     * @param parentNodeId id of the parent node
     * @param name         name (will only be used if no FQN property is available in the reference)
     * @param label        label for Caption property (only used if new reference is created)
     * @param position     position
     * @param reference    referenced content id
     * @param template     optional template to assign @return id of the created node
     * @return id of the node created
     * @throws FxApplicationException on errors
     */
    long createNode(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, long nodeId, long parentNodeId, String name,
                    FxString label, int position, FxPK reference, String template) throws FxApplicationException;

    /**
     * Create a set of nodes and attach new Folder instances to them
     *
     * @param con          an open and valid connection
     * @param seq          reference to the sequencer
     * @param ce           reference to the content engine
     * @param mode         tree mode
     * @param parentNodeId id of the parent node
     * @param path         the path to create (separated by "/")
     * @param position     position within each fqn
     * @return array of the id's
     * @throws FxApplicationException on errors
     */
    long[] createNodes(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, long parentNodeId, String path, int position) throws FxApplicationException;

    /**
     * Removes all tree entries and creates new root nodes
     *
     * @param con  an open and valid Connection
     * @param ce   ContentEngine reference to create a new folder
     * @param mode the tree to operate on
     * @throws FxApplicationException on errors
     */
    void clearTree(Connection con, ContentEngine ce, FxTreeMode mode) throws FxApplicationException;

    /**
     * Get the id of rightmost node in a path described by the nodes FQNs
     *
     * @param con       an open and valid connection
     * @param mode      tree mode
     * @param startNode the start node id
     * @param path      requested path consisting of FQNs
     * @return id of the rightmost node in the path
     * @throws FxApplicationException on errors
     */
    long getIdByFQNPath(Connection con, FxTreeMode mode, long startNode, String path) throws FxApplicationException;

    /**
     * Get the id of rightmost node in a path described by the nodes Labels/Captions
     *
     * @param con       an open and valid connection
     * @param mode      tree mode
     * @param startNode the start node id
     * @param path      requested path consisting of Labels/Captions
     * @return id of the rightmost node in the path
     * @throws FxApplicationException on errors
     */
    long getIdByLabelPath(Connection con, FxTreeMode mode, long startNode, String path) throws FxApplicationException;

    /**
     * Load a single node (without and childnodes!)
     *
     * @param con    an open and valid connection
     * @param mode   tree mode
     * @param nodeId node id
     * @return node without any preloaded or chained children
     * @throws FxApplicationException on errors
     */
    FxTreeNode getNode(Connection con, FxTreeMode mode, long nodeId) throws FxApplicationException;

    /**
     * Load a (sub)tree.
     * Loading a tree with all data takes a lot of time!
     * If <code>loadPartial</code> is set to <code>true</code> the position and path of the nodes is not initialized and
     * the labels are only loaded in the language requested with <code>partialLoadLanguage</code>.
     * Incase the position, path or (detailed) label is needed the node can be reloaded using <code>getNode()</code>
     *
     * @param con                 an open and valid connection
     * @param ce                  reference to ContentEngine
     * @param mode                tree mode
     * @param nodeId              start node id
     * @param depth               depth to load
     * @param loadPartial         load all data?
     * @param partialLoadLanguage language to load for labels if not loading fully
     * @return start node with preloaded and chained children
     * @throws FxApplicationException on errors
     */
    FxTreeNode getTree(Connection con, ContentEngine ce, FxTreeMode mode, long nodeId, int depth,
                       boolean loadPartial, FxLanguage partialLoadLanguage) throws FxApplicationException;

    /**
     * Returns the path for a specified id.
     *
     * @param con  an open and valid connection
     * @param mode tree mode
     * @param id   the id to get the path for
     * @return the path for the requested id
     * @throws FxApplicationException on errors
     */
    String getPathById(Connection con, FxTreeMode mode, long id) throws FxApplicationException;

    /**
     * Check if a node with the requested id exists
     *
     * @param con  an open and valid Connection
     * @param mode tree mode
     * @param id   node id to check
     * @return if a node with the requested id exists
     * @throws FxApplicationException on errors
     */
    boolean exists(Connection con, FxTreeMode mode, long id) throws FxApplicationException;

    /**
     * Callback from the ContentEngine (actually the HierarchicalStorage implementation) if aFQN
     * property has changed to reflect changes back into the tree
     *
     * @param con         an open and valid connection
     * @param referenceId id of the referenced content
     * @param maxVersion  change affects the max version?
     * @param liveVersion change affects the live version
     * @param name        the new name
     * @throws FxApplicationException on errors
     */
    void syncFQNName(Connection con, long referenceId, boolean maxVersion, boolean liveVersion, String name) throws FxApplicationException;

    /**
     * Update the name of a tree node.
     * Will update the FQN of the referenced content as well if assigned.
     *
     * @param con    an open and valid connection
     * @param mode   tree mode
     * @param ce     reference to the content engine
     * @param nodeId node id to update
     * @param name   new name
     * @throws FxApplicationException on errors
     */
    void updateName(Connection con, FxTreeMode mode, ContentEngine ce, long nodeId, String name) throws FxApplicationException;

    /**
     * Update a nodes reference
     *
     * @param con         an open and valid connection
     * @param mode        tree mode
     * @param nodeId      node id to update
     * @param referenceId new reference id
     * @throws FxApplicationException on errors
     */
    void updateReference(Connection con, FxTreeMode mode, long nodeId, long referenceId) throws FxApplicationException;

    /**
     * Returns a list of paths made up of Caption's for the given id's.
     * If there is no caption propery found in the instance, the FQN will be used.
     * The root node will be excluded.
     * <p/>
     * Example: input ids = [12,4]<br>
     * Result: ["/DescriptionForNode1/DescriptionForNode12","/DescriptionForNode1/DescriptionForNode4"]
     *
     * @param con             an open and valid connection
     * @param mode            tree mode to use (Live or Edit tree)
     * @param labelPropertyId propertyId of the label
     * @param language        desired result language
     * @param stripNodeInfos  remove node specific meta information in result
     * @param nodeIds         the id's of the nodes to get the path to the root node for
     * @return a list with all paths made up of Caption's
     * @throws FxApplicationException on errors
     */
    List<String> getLabels(Connection con, FxTreeMode mode, long labelPropertyId, FxLanguage language, boolean stripNodeInfos, long... nodeIds) throws FxApplicationException;

    /**
     * Moves a node to the specified parent and the specified position.
     *
     * @param con         an open and valid connection
     * @param seq         reference to the sequencer
     * @param mode        tree mode
     * @param nodeId      the node to move
     * @param newParentId the new parent
     * @param newPosition the new position in the new parents children, 0 based
     * @throws FxApplicationException on errors
     */
    void move(Connection con, SequencerEngine seq, FxTreeMode mode, long nodeId, long newParentId, int newPosition) throws FxApplicationException;

    /**
     * Remove a node
     *
     * @param con            an open and valid connection
     * @param mode           tree mode
     * @param ce             reference to the content engine
     * @param nodeId         the node to remove
     * @param removeChildren if true all nodes that are inside the subtree of the given node are
     *                       deleted as well, if false the subtree is moved one level up (to the parent of the specified
     *                       node)
     * @throws FxApplicationException on errors
     */
    void removeNode(Connection con, FxTreeMode mode, ContentEngine ce, long nodeId, boolean removeChildren) throws FxApplicationException;

    /**
     * Returns all ids from the given node up to the root.
     *
     * @param con    an open and valid connection
     * @param mode   tree mode
     * @param nodeId the node id to start with
     * @return all ids from the given node up to the root or null if the node was not found
     * @throws FxApplicationException on errors
     */
    long[] getIdChain(Connection con, FxTreeMode mode, long nodeId) throws FxApplicationException;

    /**
     * Sets the template of the node.
     *
     * @param con      an open and valid connection
     * @param mode     tree mode
     * @param nodeId   the node id
     * @param template the tamplate, or null for no template
     * @throws FxApplicationException on errors
     */
    void setTemplate(Connection con, FxTreeMode mode, long nodeId, String template) throws FxApplicationException;

    /**
     * Activate a single node (and all its parents if necessary)
     *
     * @param con    an open and valid connections
     * @param seq    reference to the sequencer
     * @param ce     reference to the content engine
     * @param mode   tree mode
     * @param nodeId id of the node to activate
     * @throws FxApplicationException on errors
     */
    void activateNode(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, long nodeId) throws FxApplicationException;

    /**
     * Activate a node, its subtree and its parents up to the root node
     *
     * @param con    an open and valid connection
     * @param seq    reference to the sequencer
     * @param ce     reference to the content engine
     * @param mode   tree mode
     * @param nodeId node id
     * @throws FxApplicationException on errors
     */
    void activateSubtree(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode, long nodeId) throws FxApplicationException;

    /**
     * Activate all nodes in a tree
     *
     * @param con  an open and valid connection
     * @param mode tree mode
     * @throws com.flexive.shared.exceptions.FxTreeException
     *          on errors
     */
    void activateAll(Connection con, FxTreeMode mode) throws FxTreeException;

    /**
     * Copy a node and all its children to a new parent node
     *
     * @param con               an open and valid connection
     * @param seq               reference to the sequencer
     * @param mode              tree mode
     * @param srcNodeId         source node id
     * @param dstParentNodeId   destination parent node id
     * @param dstPosition       destination position
     * @param deepReferenceCopy perform a deep reference copy, cloning all references
     * @param copyOfPrefix      prefix to set for FQN if deep reference copy is performed
     * @return id of the copied (new) tree node
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on errors
     */
    long copy(Connection con, SequencerEngine seq, FxTreeMode mode, long srcNodeId, long dstParentNodeId,
              int dstPosition, boolean deepReferenceCopy, String copyOfPrefix) throws FxApplicationException;

    /**
     * Get a list of all nodes that are referencing a requested content id
     *
     * @param con         an open and valid connection
     * @param mode        tree mode
     * @param referenceId the referenced content id
     * @return list of all nodes that are referencing a requested content id
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on errors
     */
    List<FxTreeNode> getNodesWithReference(Connection con, FxTreeMode mode, long referenceId)
            throws FxApplicationException;

    /**
     * Populate the tree with test data.
     * This function takes quite some time to complete
     *
     * @param con  an open and valid connection
     * @param seq  reference to the sequencer
     * @param ce   reference to the content engine
     * @param mode tree mode
     * @throws FxApplicationException on errors
     */
    void populate(Connection con, SequencerEngine seq, ContentEngine ce, FxTreeMode mode) throws FxApplicationException;

    /**
     * Callback when a content is removed to replace it with a folder or remove the node(s)
     *
     * @param con                    an open and valid connection
     * @param contentId              referenced content id
     * @param liveVersionRemovedOnly if just the live version was removed (other versions have no impact) @throws FxApplicationException on errors
     * @throws FxApplicationException on errors
     */
    void contentRemoved(Connection con, long contentId, boolean liveVersionRemovedOnly) throws FxApplicationException;

    /**
     * Call for housekeeping before a content version is removed
     *
     * @param con     an open and valid connection
     * @param id      content id
     * @param version content version
     * @param cvi     content version information
     * @return String array containing edit and live nodes affected (comma separated)
     * @throws FxApplicationException on errors
     * @see TreeStorage#afterContentVersionRemoved
     */
    String[] beforeContentVersionRemoved(Connection con, long id, int version, FxContentVersionInfo cvi) throws FxApplicationException;

    /**
     * Call for housekeeping after a content version is removed
     *
     * @param nodes   array containing edit and live nodes affected (comma separated)
     * @param con     an open and valid connection
     * @param id      content id
     * @param version content version
     * @param cvi     content version information
     * @throws FxApplicationException on errors
     * @see TreeStorage#beforeContentVersionRemoved
     */
    void afterContentVersionRemoved(String[] nodes, Connection con, long id, int version, FxContentVersionInfo cvi) throws FxApplicationException;

    /**
     * Perform a complete check on the given tree if checks are enabled
     *
     * @param con  an open and valid connection
     * @param mode the tree to check
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on errors
     */
    void checkTreeIfEnabled(Connection con, FxTreeMode mode) throws FxApplicationException;
}
