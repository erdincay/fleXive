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
package com.flexive.shared.interfaces;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.tree.FxTreeNodeEdit;
import com.flexive.shared.tree.FxTreeRemoveOp;
import com.flexive.shared.value.FxReference;

import javax.ejb.Remote;
import java.util.List;

/**
 * Tree Interface
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface TreeEngine {

    //----------------------
    //- Create/Update/Remove
    //----------------------

    /**
     * Create a new or save an existing node
     *
     * @param node the node to save
     * @return id of the saved node
     * @throws FxApplicationException on errors
     */
    long save(FxTreeNodeEdit node) throws FxApplicationException;

    /**
     * Remove a node and optionally its children.
     * Referenced contents will only be removed if removedReferencedContent is set to true and the referenced
     * content is not referenced elsewhere.
     * The only exception is if the referenced content is of type FOLDER, then the folder is removed if it is not referenced
     * from anywhere else.
     *
     * @param node                    the node to removed
     * @param removeReferencedContent remove referenced content
     * @param removeChildren          if true all nodes that are inside the subtree of the given node are
     *                                deleted as well, if false the subtree is moved one level up (to the parent of the specified
     *                                node)
     * @throws FxApplicationException on errors
     * @deprecated use TreeEngine#remove(com.flexive.shared.tree.FxTreeNode, com.flexive.shared.tree.FxTreeRemoveOp, boolean)
     */
    @Deprecated
    void remove(FxTreeNode node, boolean removeReferencedContent, boolean removeChildren) throws FxApplicationException;

    /**
     * Remove a node and optionally its children.
     * Referenced contents will only be removed if removedReferencedContent is set to true and the referenced
     * content is not referenced elsewhere.
     * The only exception is if the referenced content is of type FOLDER, then the folder is removed if it is not referenced
     * from anywhere else.
     *
     * @param mode                    the tree mode (edit or live)
     * @param nodeId                  the node to removed
     * @param removeReferencedContent remove referenced content
     * @param removeChildren          if true all nodes that are inside the subtree of the given node are
     *                                deleted as well, if false the subtree is moved one level up (to the parent of the specified
     *                                node)
     * @throws FxApplicationException on errors
     * @deprecated use TreeEngine#remove(com.flexive.shared.tree.FxTreeMode, long, com.flexive.shared.tree.FxTreeRemoveOp, boolean)
     */
    @Deprecated
    void remove(FxTreeMode mode, long nodeId, boolean removeReferencedContent, boolean removeChildren) throws FxApplicationException;

    /**
     * Remove a node and optionally its children.
     * Referenced contents will be removed depending on chosen <code>removeOp</code>.
     * If the referenced content is of type FOLDER, then the folder is removed if it is not referenced
     * from anywhere else.
     *
     * @param node           the node to be removed
     * @param removeOp       remove operation to apply
     * @param removeChildren if true all nodes that are inside the subtree of the given node are
     *                       deleted as well, if false the subtree is moved one level up (to the parent of the specified
     *                       node)
     * @throws FxApplicationException on errors
     * @since 3.1
     */
    void remove(FxTreeNode node, FxTreeRemoveOp removeOp, boolean removeChildren) throws FxApplicationException;

    /**
     * Remove a node and optionally its children.
     * Referenced contents will be removed depending on chosen <code>removeOp</code>.
     * If the referenced content is of type FOLDER, then the folder is removed if it is not referenced
     * from anywhere else.
     *
     * @param mode           the tree mode (edit or live)
     * @param nodeId         the node to removed
     * @param removeOp       remove operation to apply
     * @param removeChildren if true all nodes that are inside the subtree of the given node are
     *                       deleted as well, if false the subtree is moved one level up (to the parent of the specified
     *                       node)
     * @throws FxApplicationException on errors
     * @since 3.1
     */
    void remove(FxTreeMode mode, long nodeId, FxTreeRemoveOp removeOp, boolean removeChildren) throws FxApplicationException;

    /**
     * Create tree folders of the given path relative to the parent node, creating all folders
     * stored in <code>path</code> if they dont exist (similar to {@link java.io.File#mkdirs()}).
     *
     * @param mode         operate on live or edit tree?
     * @param parentNodeId the parent node to create the path from
     * @param position     desired position (will be applied to all "folders" hence in most cases only min or max values
     *                     make sense)
     * @param path         the path to be created, e.g. "/my/virtual/folder"
     * @return the node id's of the created path
     * @throws FxApplicationException on errors
     */
    long[] createNodes(FxTreeMode mode, long parentNodeId, int position, String path) throws FxApplicationException;

    /**
     * Clears the requested tree and creates a new root node
     *
     * @param mode the tree to clear
     * @throws FxApplicationException on errors
     */
    void clear(FxTreeMode mode) throws FxApplicationException;

    /**
     * Moves a node to the specified parent and the specified position.
     *
     * @param mode          tree mode to use (Live or Edit tree)
     * @param nodeId        the node to move
     * @param destinationId the new parent
     * @param newPosition   the new position in the new parents children
     * @throws FxApplicationException on errors
     */
    void move(FxTreeMode mode, long nodeId, long destinationId, int newPosition) throws FxApplicationException;

    /**
     * Copies a node to the specified parent and the specified position.
     *
     * @param mode                tree mode to use (Live or Edit tree)
     * @param source              the parent id of the structure to copy
     * @param destination         the destination node
     * @param destinationPosition the position in the destination node's children @return the (root-)id the copy
     * @return the id of the new node (the "copy")
     * @throws FxApplicationException on errors
     * @deprecated
     */
    @Deprecated
    long copy(FxTreeMode mode, long source, long destination, int destinationPosition) throws FxApplicationException;

    /**
     * Copies a node to the specified parent and the specified position.
     *
     * @param mode                tree mode to use (Live or Edit tree)
     * @param source              the parent id of the structure to copy
     * @param destination         the destination node
     * @param destinationPosition the position in the destination node's children @return the (root-)id the copy
     * @param deepReferenceCopy   create a copy of all referenced contents or reference the original contents?
     * @return the id of the new node (the "copy")
     * @throws FxApplicationException on errors
     * @since 3.1
     */
    long copy(FxTreeMode mode, long source, long destination, int destinationPosition, boolean deepReferenceCopy) throws FxApplicationException;

    /**
     * Sets the data of the node.
     *
     * @param mode   tree mode to use (Live or Edit tree)
     * @param nodeId the node id
     * @param data   the data, or null for no data
     */
    void setData(FxTreeMode mode, long nodeId, String data);

    /**
     * Activates a node - copying it from the "Edit" to the "Live" tree
     *
     * @param mode            tree mode (currently only Edit supported)
     * @param nodeId          the node to activate
     * @param includeChildren if true all children of the node are activated as well
     * @throws FxApplicationException on errors
     * @see com.flexive.shared.interfaces.TreeEngine#activate(com.flexive.shared.tree.FxTreeMode, long, boolean, boolean)
     * @deprecated use com.flexive.shared.interfaces.TreeEngine#activate(com.flexive.shared.tree.FxTreeMode, long, boolean, boolean)
     */
    @Deprecated
    void activate(FxTreeMode mode, long nodeId, boolean includeChildren) throws FxApplicationException;

    /**
     * Activates a node - copying it from the "Edit" to the "Live" tree
     *
     * @param mode             tree mode (currently only Edit supported)
     * @param nodeId           the node to activate
     * @param includeChildren  if true all children of the node are activated as well
     * @param activateContents change the step of contents that have no live step to live in the max version?
     * @throws FxApplicationException on errors
     * @since 3.1
     */
    void activate(FxTreeMode mode, long nodeId, boolean includeChildren, boolean activateContents) throws FxApplicationException;

    //----------------------
    //- Read/Load
    //----------------------

    /**
     * Returns true if a node with the specified id exists.
     *
     * @param mode tree mode to use (Live or Edit tree)
     * @param id   the id to check for
     * @return true if the node exists
     * @throws FxApplicationException on errors
     */
    boolean exist(FxTreeMode mode, long id) throws FxApplicationException;

    /**
     * Returns the informations for a single node
     *
     * @param mode tree mode to use (Live or Edit tree)
     * @param id   the id of the node to get
     * @return the node information, or null if the node does not exist
     * @throws FxApplicationException on errors
     */
    FxTreeNode getNode(FxTreeMode mode, long id) throws FxApplicationException;

    /**
     * Retrieves a (sub)tree, starting from the given node.
     * Loading a tree with all data takes a lot of time hence the position of the nodes is not initialized and
     * the labels are only loaded in the language set as default for the calling user.
     * Incase the position or (detailed) label is needed the node can be reloaded using <code>getNode()</code>
     *
     * @param mode   tree mode to use (Live or Edit tree)
     * @param nodeId the nod to start from
     * @param depth  the maximum depth to read
     * @return the (sub)tree
     * @throws FxApplicationException on errors
     */
    FxTreeNode getTree(FxTreeMode mode, long nodeId, int depth) throws FxApplicationException;

    /**
     * Returns all the data to use for this node ordered by relevance.
     *
     * @param mode tree mode to use (Live or Edit tree)
     * @param id   the id to get the datas for
     * @return the data, or null if the node does not exist
     */
    String[] getDatas(FxTreeMode mode, long id);

    //----------------------
    //- Finders/Search
    //----------------------

    /**
     * Find a (direct) child with the given name under the node <code>nodeId</code>.
     *
     * @param mode   tree mode to use (Live or Edit tree)
     * @param nodeId the parent node ID
     * @param name   name of the requested node @return  the tree node
     * @return the selected node. If no node was found, a FxNotFoundException is thrown.
     * @throws FxApplicationException on errors
     */
    FxTreeNode findChild(FxTreeMode mode, long nodeId, String name) throws FxApplicationException;

    /**
     * Find a (direct) child with the given reference ID under the node <code>nodeId</code>.
     *
     * @param mode        tree mode to use (Live or Edit tree)
     * @param nodeId      the parent node ID
     * @param referenceId the reference ID
     * @return the selected node. If no node was found, a FxNotFoundException is thrown.
     * @throws FxApplicationException on errors
     */
    FxTreeNode findChild(FxTreeMode mode, long nodeId, long referenceId) throws FxApplicationException;

    /**
     * Find a (direct) child with the given PK under the node <code>nodeId</code>.
     *
     * @param mode   tree mode to use (Live or Edit tree)
     * @param nodeId the parent node ID
     * @param pk     the reference
     * @return the selected node. If no node was found, a FxNotFoundException is thrown.
     * @throws FxApplicationException on errors
     */
    FxTreeNode findChild(FxTreeMode mode, long nodeId, FxPK pk) throws FxApplicationException;

    /**
     * Find a (direct) child with the given reference under the node <code>nodeId</code>.
     *
     * @param mode      tree mode to use (Live or Edit tree)
     * @param nodeId    the parent node ID
     * @param reference the reference
     * @return the selected node. If no node was found, a FxNotFoundException is thrown.
     * @throws FxApplicationException on errors
     */
    FxTreeNode findChild(FxTreeMode mode, long nodeId, FxReference reference) throws FxApplicationException;

    /**
     * Returns all nodes that match the given reference.
     *
     * @param mode      tree mode to use (Live or Edit tree)
     * @param reference the reference
     * @return the matching nodes
     * @throws FxApplicationException on errors
     */
    List<FxTreeNode> getNodesWithReference(FxTreeMode mode, long reference) throws FxApplicationException;

    /**
     * Returns the node id specified by a path.
     *
     * @param mode tree mode to use (Live or Edit tree)
     * @param path the path - eg '/nodeA/nodeB', the virtual root '/Root' node must not be included,
     *             and the path has to start with a '/'.
     * @return the node id, or -1 if the path does not exist
     * @throws FxApplicationException on errors
     */
    long getIdByPath(FxTreeMode mode, String path) throws FxApplicationException;

    /**
     * Returns the node id specified by a FQN path thats starts from the given node.
     *
     * @param mode      tree mode to use (Live or Edit tree)
     * @param startNode the root node.
     * @param path      the path - eg '/nodeA/nodeB', the virtual root '/Root' node must not be included,
     *                  and the path has to start with a '/'.
     * @return the node id, or -1 if the path does not exist
     * @throws FxApplicationException on errors
     */
    long getIdByFQNPath(FxTreeMode mode, long startNode, String path) throws FxApplicationException;

    /**
     * Returns the node id specified by a label path thats starts from the given node.
     *
     * @param mode      tree mode to use (Live or Edit tree)
     * @param startNode the root node.
     * @param path      the path - eg '/nodeA/nodeB', the virtual root '/Root' node must not be included,
     *                  and the path has to start with a '/'.
     * @return the node id, or -1 if the path does not exist
     * @throws FxApplicationException on errors
     */
    long getIdByLabelPath(FxTreeMode mode, long startNode, String path) throws FxApplicationException;

    /**
     * Returns the path for a specified id.
     *
     * @param mode   tree mode to use (Live or Edit tree)
     * @param nodeId the node id to get the path for
     * @return the node id, or -1 if the path does not exist
     * @throws FxApplicationException on errors
     */
    String getPathById(FxTreeMode mode, long nodeId) throws FxApplicationException;

    /**
     * Returns all ids from the given node up to the root.
     *
     * @param mode   tree mode to use (Live or Edit tree)
     * @param nodeId the id to start with
     * @return the id chain, or null if the node does not exist
     * @throws FxApplicationException on errors
     */
    long[] getIdChain(FxTreeMode mode, long nodeId) throws FxApplicationException;

    /**
     * Returns all ids from the root down to the given node.
     *
     * @param mode tree mode to use (Live or Edit tree)
     * @param id   the id to start with
     * @return the id chain, or null if the node does not exist
     * @throws FxApplicationException on errors
     */
    long[] getReverseIdChain(FxTreeMode mode, long id) throws FxApplicationException;

    /**
     * Returns a list of paths made up of FQN's for the given id's
     * The root node will be excluded.
     * <p/>
     * Example: input id's = [12,4]<br>
     * Result: ["/Node1/Node12","/Node1/Node4"]
     *
     * @param mode tree mode to use (Live or Edit tree)
     * @param ids  the id's of the nodes to get the path to the root node for
     * @return a list with all paths made up of FQN's
     * @throws FxApplicationException on errors
     */
    List<String> getPaths(FxTreeMode mode, long... ids) throws FxApplicationException;

    /**
     * Returns a list of paths made up of Caption's for the given id's.
     * If there is no caption propery found in the instance, the FQN will be used.
     * The root node will be excluded. The language is the calling users preferred language.
     * <p/>
     * Example: input ids = [12,4]<br>
     * Result: ["/DescriptionForNode1/DescriptionForNode12","/DescriptionForNode1/DescriptionForNode4"]
     *
     * @param mode tree mode to use (Live or Edit tree)
     * @param ids  the id's of the nodes to get the path to the root node for
     * @return a list with all paths made up of Caption's
     * @throws FxApplicationException on errors
     */
    List<String> getLabels(FxTreeMode mode, long... ids) throws FxApplicationException;

    /**
     * Returns a list of paths made up of Caption's for the given id's.
     * If there is no caption propery found in the instance, the FQN will be used.
     * The root node will be excluded.
     * <p/>
     * Example: input ids = [12,4]<br>
     * Result: ["/DescriptionForNode1/DescriptionForNode12","/DescriptionForNode1/DescriptionForNode4"]
     *
     * @param mode tree mode to use (Live or Edit tree)
     * @param lang desired result language
     * @param ids  the id's of the nodes to get the path to the root node for
     * @return a list with all paths made up of Caption's
     * @throws FxApplicationException on errors
     */
    List<String> getLabels(FxTreeMode mode, FxLanguage lang, long... ids) throws FxApplicationException;

    //----------------------
    //- Internal/Lifecycle
    //----------------------

    /**
     * Populate the tree with test data with a default 3 child nodes (folder sub-levels)
     *
     * @param mode tree mode
     * @throws FxApplicationException on errors
     */
    void populate(FxTreeMode mode) throws FxApplicationException;

    /**
     * Populate the tree with test data with a given maximum number of node children (folder sub-levels)
     *
     * @param mode            tree mode
     * @param maxNodeChildren the number of nodes to be created as children of the root node (0 will not create any data, no value will create 3).
     * @throws FxApplicationException on errors
     */
    void populate(FxTreeMode mode, int maxNodeChildren) throws FxApplicationException;
}
