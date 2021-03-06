/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.javascript.tree;

import com.flexive.shared.FxContext;
import com.flexive.shared.FxLockType;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxUpdateException;
import com.flexive.shared.interfaces.TreeEngine;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.tree.FxTreeNodeEdit;
import com.flexive.shared.tree.FxTreeRemoveOp;
import com.flexive.war.JsonWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;

import static com.flexive.shared.EJBLookup.getContentEngine;
import static com.flexive.shared.EJBLookup.getTreeEngine;
import static com.flexive.shared.tree.FxTreeMode.Edit;
import static com.flexive.shared.tree.FxTreeMode.Live;

/**
 * Content tree edit actions invoked via JSON/RPC.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ContentTreeEditor implements Serializable {
    private static final long serialVersionUID = -6518286246610051456L;
    private static final Log LOG = LogFactory.getLog(ContentTreeEditor.class);

    /**
     * Update the tree node's label.
     *
     * @param nodeId   the node id
     * @param label    the new label
     * @param liveTree true if the live tree should be rendered, false for the edit tree
     * @param pathMode true if node labels should be paths instead of content captions
     * @return nothing
     * @throws java.io.IOException if the response could not be created
     */
    public String saveLabel(long nodeId, String label, boolean liveTree, boolean pathMode) throws IOException {

        // Rename the node
        try {
            if (label.endsWith("<br>")) {
                label = label.substring(0, label.length() - 4);
            }
            final FxTreeNodeEdit node = getTreeEngine().getNode(liveTree ? Live : Edit, nodeId).asEditable();
            if (pathMode) {
                node.setName(label);
            } else {
                node.getLabel().setTranslation(FxContext.getUserTicket().getLanguage(), label);
            }
            getTreeEngine().save(node);
//            EJBLookup.getTreeInterface().renameNode(nodeId,false,label,null);
        } catch (Exception e) {
            LOG.error("Failed to save label: " + e.getMessage(), e);
        }

        String title = StringUtils.defaultString(label).trim();
        try {
            // add subnode count
            final FxTreeNode node = getTreeEngine().getNode(Edit, nodeId);
            int childCount = node.getDirectChildCount();
            if (childCount > 0) {
                title += " [" + childCount + "]";
            }

            // add dirty flag
            if (node.isDirty()) {
                title = "<span class=\"dirty\">" + title + "</span>";
            }
        } catch (Exception e) {
            LOG.error("Failed to save label: " + e.getMessage(), e);
        }

        // response: [{title: 'new title'}]
        StringWriter out = new StringWriter();
        new JsonWriter(out).startArray().startMap().writeAttribute("title", title)
                .closeMap().closeArray().finishResponse();
        return out.toString();
    }

    /**
     * Move the given node to the new parent at position <code>index</code>
     *
     * @param nodeId      the tree node to be moved
     * @param newParentId the the parent node
     * @param index       the new position
     * @param live        if the live tree should be used
     * @return nothing
     * @throws Exception if an error occured
     */
    public String move(long nodeId, long newParentId, int index, boolean live) throws Exception {
        return moveInto(nodeId, newParentId, live, index, new MoveOp());
    }

    /**
     * Move the given node above the target node ID.
     *
     * @param nodeId      the tree node to be moved
     * @param targetId    the target node ID
     * @param index       (unused)
     * @param live        if the live tree should be used
     * @return nothing
     * @throws Exception if an error occured
     */
    public String moveAbove(long nodeId, long targetId, int index, boolean live) throws Exception {
        return moveNear(nodeId, targetId, live, 0, new MoveOp());
    }

    /**
     * Move the given node below the target node ID.
     *
     * @param nodeId      the tree node to be moved
     * @param targetId    the target node ID
     * @param index       (unused)
     * @param live        if the live tree should be used
     * @return nothing
     * @throws Exception if an error occured
     */
    public String moveBelow(long nodeId, long targetId, int index, boolean live) throws Exception {
        return moveNear(nodeId, targetId, live, 1, new MoveOp());
    }

    /**
     * Remove a tree node and (depending on the removeContent parameter) the attached
     * content instance.
     *
     * @param nodeId         the tree node ID
     * @param removeContent  if true, the attached content instance will also be removed
     * @param live           if the tree is in live mode
     * @param deleteChildren if the node children should also be deleted
     * @return nothing
     * @throws Exception if an error occured
     */
    public String remove(long nodeId, boolean removeContent, boolean live, boolean deleteChildren) throws Exception {
        getTreeEngine().remove(
                live ? Live : Edit, nodeId,
                deleteChildren && removeContent
                        ? FxTreeRemoveOp.RemoveSingleFiled  // don't delete multi-filed children
                        : removeContent
                        ? FxTreeRemoveOp.Remove             // removeContent set, but not recursive - remove all instances
                        : FxTreeRemoveOp.Unfile,            // unfile content, remove tree node              
                deleteChildren
        );
        return "[]";
    }

    public String addReferences(long nodeId, long[] referenceIds) throws Exception {
        try {
            TreeEngine tree = getTreeEngine();
            for (long referenceId : referenceIds) {
                tree.save(FxTreeNodeEdit.createNew("").setParentNodeId(nodeId).setReference(new FxPK(referenceId)));
//                tree.createNode(nodeId, String.valueOf(referenceId), referenceId);
            }
        } catch (Exception e) {
            LOG.error("Failed to add nodes: " + e.getMessage(), e);
            throw e;
        }
        return "[]";
    }

    public String createFolder(long parentNodeId, String folderName) throws Exception {
        try {
            getTreeEngine().save(FxTreeNodeEdit.createNew(folderName).setParentNodeId(parentNodeId));
        } catch (Exception e) {
            LOG.error("Failed to create tree folder: " + e.getMessage(), e);
            throw e;
        }
        return "[]";
    }

    public String lockContent(long contentId, boolean live) throws Exception {
        getContentEngine().lock(FxLockType.Permanent, new FxPK(contentId, live ? FxPK.LIVE : FxPK.MAX));
        return "[]";
    }

    public String unlockContent(long contentId, boolean live) throws Exception {
        getContentEngine().unlock(new FxPK(contentId, live ? FxPK.LIVE : FxPK.MAX));
        return "[]";
    }

    public String lockSubtree(long nodeId, boolean live) throws Exception {
        performLockSubtree(nodeId, live, new PerformLock());
        return "[]";
    }

    public String unlockSubtree(long nodeId, boolean live) throws Exception {
        performLockSubtree(nodeId, live, new PerformUnlock());
        return "[]";
    }

    private int performLockSubtree(long nodeId, boolean live, LockOp lockOp) throws Exception {
        final FxTreeNode subtree = getTreeEngine().getTree(live ? Live : Edit, nodeId, 9999);
        int count = 0;
        for (FxTreeNode node : subtree) {
            if (lockOp.perform(node.getReference())) {
                count++;
            }
        }
        return count;
    }

    private static interface LockOp {
        boolean perform(FxPK pk);
    }

    private static class PerformLock implements LockOp {
        @Override
        public boolean perform(FxPK pk) {
            try {
                getContentEngine().lock(FxLockType.Permanent, pk);
                return true;
            } catch (FxApplicationException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to lock content " + pk + ": " + e.getMessage(), e);
                }
                return false;
            }
        }
    }
    
    private static class PerformUnlock implements LockOp {
        @Override
        public boolean perform(FxPK pk) {
            try {
                getContentEngine().unlock(pk);
                return true;
            } catch (FxApplicationException e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to unlock content " + pk + ": " + e.getMessage(), e);
                }
                return false;
            }
        }
    }

    public String activate(long nodeId, boolean includeChildren) throws Exception {
        try {
            getTreeEngine().activate(FxTreeMode.Edit, nodeId, includeChildren, true);
        } catch (Exception e) {
            LOG.error("Failed to activate node: " + e.getMessage(), e);
            throw e;
        }
        return "[]";
    }

    public String copy(long nodeId, long newParentId, int index, boolean live) throws Exception {
        return moveInto(nodeId, newParentId, live, index, new CopyOp());
    }

    public String copyAbove(long nodeId, long targetId, int index, boolean live) throws Exception {
        return moveNear(nodeId, targetId, live, 0, new CopyOp());
    }

    public String copyBelow(long nodeId, long targetId, int index, boolean live) throws Exception {
        return moveNear(nodeId, targetId, live, 1, new CopyOp());
    }

    public String copyReferences(long nodeId, long newParentId, int index, boolean live) throws Exception {
        return moveInto(nodeId, newParentId, live, index, new CopyReferencesOp());
    }

    public String copyReferencesAbove(long nodeId, long targetId, int index, boolean live) throws Exception {
        return moveNear(nodeId, targetId, live, 0, new CopyReferencesOp());
    }

    public String copyReferencesBelow(long nodeId, long targetId, int index, boolean live) throws Exception {
        return moveNear(nodeId, targetId, live, 1, new CopyReferencesOp());
    }

    private static interface TreeMoveOp {
        void perform(FxTreeMode treeMode, long nodeId, long targetId, int position) throws FxApplicationException;
    }

    /**
     * Tree move (cut/paste) operation.
     */
    private static class MoveOp implements TreeMoveOp {
        @Override
        public void perform(FxTreeMode treeMode, long nodeId, long targetId, int position) throws FxApplicationException {
            getTreeEngine().move(treeMode, nodeId, targetId, position);
        }
    }

    /**
     * Tree copy operation.
     */
    private static class CopyOp implements TreeMoveOp {
        @Override
        public void perform(FxTreeMode treeMode, long nodeId, long targetId, int position) throws FxApplicationException {
            getTreeEngine().copy(treeMode, nodeId, targetId, position, true);
        }
    }

    /**
     * Tree 'copy references' (shallow copy) operation.
     */
    private static class CopyReferencesOp implements TreeMoveOp {
        @Override
        public void perform(FxTreeMode treeMode, long nodeId, long targetId, int position) throws FxApplicationException {
            getTreeEngine().copy(treeMode, nodeId, targetId, position, false);
        }
    }

    private String moveInto(long nodeId, long targetId, boolean live, int position, TreeMoveOp op) throws Exception {
        final FxTreeMode treeMode = live ? Live : Edit;
        try {
            op.perform(treeMode, nodeId, targetId, position);
        } catch (Exception e) {
            LOG.error("failed to copy/move node: " + e, e);
            throw e;
        }
        return "[]";
    }

    private String moveNear(long nodeId, long targetId, boolean live, int deltaPos, TreeMoveOp op) throws Exception {
        final FxTreeMode treeMode = live ? Live : Edit;
        final FxTreeNode target = getTreeEngine().getNode(treeMode, targetId);
        if (target.getParentNodeId() <= 0) {
            throw new FxUpdateException("ex.jsf.contentTreeEditor.moveNear.noParent");
        }
        try {
            op.perform(treeMode, nodeId, target.getParentNodeId(), target.getPosition() + deltaPos);
        } catch (Exception e) {
            LOG.error("failed to move node: " + e, e);
            throw e;
        }
        return "[]";
    }


}