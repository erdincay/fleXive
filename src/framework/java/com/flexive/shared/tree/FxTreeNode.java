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
package com.flexive.shared.tree;

import com.flexive.shared.*;
import com.flexive.shared.content.FxContentVersionInfo;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.LifeCycleInfo;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.ArrayUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * FxNode implementation for flexive
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxTreeNode implements Serializable, SelectableObjectWithLabel, SelectableObjectWithName, Iterable<FxTreeNode> {
    private static final long serialVersionUID = -1666004845250114348L;

    private String path = null;
    private boolean temporary = true;
    private boolean markForDelete = false;
    protected FxString label = null;
    protected FxPK reference;
    protected LifeCycleInfo referenceLifeCycleInfo;
    protected long referenceTypeId = 0;
    protected FxLock lock = FxLock.noLockPK();
    protected FxTreeMode mode;
    protected int position;
    protected List<FxTreeNode> children;
    private List<Long> childIds = null;
    private boolean dirty;
    protected long id;
    private long[] acls;
    protected long parentNodeId;
    protected String name;
    private long modifiedAt;
    protected String data;
    private int depth;
    private int totalChildCount;
    private int directChildCount;
    private boolean leaf;
    private boolean mayEdit, mayDelete, mayRelate, mayExport, mayCreate;
    protected boolean activate = false;

    //flag indicating that this tree node is only partial loaded
    private boolean partialLoaded = false;
    public static final int PARTIAL_LOADED_POS = -42;
    public static final String PATH_NOT_LOADED = "/not_loaded(partial!)";

    /**
     * Constant id of the root node
     */
    public final static long ROOT_NODE = 1;

    /**
     * Protected constructor to avoid construction
     */
    protected FxTreeNode() {
    }

    /**
     * Ctor
     *
     * @param mode                   FxTreeMode
     * @param lock                   the lock of the reference
     * @param id                     node id
     * @param parentNodeId           id of the parent node
     * @param reference              pk of the referenced content
     * @param referenceLifeCycleInfo if the node has a referenced content this is its LifeCycleInfo
     * @param referenceTypeId        type id of the referenced content
     * @param acls                   acls of the referenced content
     * @param name                   name (part of the path)
     * @param path                   complete path from the root node
     * @param label                  label
     * @param position               position
     * @param children               child nodes (only available if loaded with <code>#getTree</code>)
     * @param childIds               ids of the child nodes (only available if loaded with <code>#getTree</code>)
     * @param depth                  depth of this node relative to the root node
     * @param totalChildCount        total number of children
     * @param directChildCount       number of children attached to this node directly
     * @param leaf                   is this node a leaf?
     * @param dirty                  dirty flag
     * @param modifiedAt             timestamp of last modification
     * @param data                   optional data
     * @param mayEdit                edit permission for the calling user
     * @param mayCreate              create permission for the calling user
     * @param mayDelete              delete permission for the calling user
     * @param mayRelate              relate permission for the calling user
     * @param mayExport              export permission for the calling user
     */
    public FxTreeNode(FxTreeMode mode, FxLock lock, long id, long parentNodeId, FxPK reference, LifeCycleInfo referenceLifeCycleInfo,
                      long referenceTypeId, List<Long> acls, String name, String path,
                      FxString label, int position, List<FxTreeNode> children, List<Long> childIds, int depth,
                      int totalChildCount, int directChildCount, boolean leaf, boolean dirty, long modifiedAt,
                      String data, boolean mayEdit, boolean mayCreate, boolean mayDelete, boolean mayRelate, boolean mayExport) {
        this.path = FxFormatUtils.escapeTreePath(path);
        this.label = label;
        this.lock = lock;
        this.reference = reference;
        this.referenceLifeCycleInfo = referenceLifeCycleInfo;
        this.referenceTypeId = referenceTypeId;
        this.acls = acls != null ? FxArrayUtils.toPrimitiveLongArray(acls) : new long[0];
        this.mode = mode;
        this.position = position;
        this.children = children;
        this.childIds = childIds;
        this.dirty = dirty;
        this.id = id;
        this.parentNodeId = parentNodeId;
        this.name = FxFormatUtils.escapeTreePath(name);
        this.modifiedAt = modifiedAt;
        this.data = data;
        this.depth = depth;
        this.totalChildCount = totalChildCount;
        this.directChildCount = directChildCount;
        this.leaf = leaf;
        this.temporary = false;
        this.markForDelete = false;
        this.mayCreate = mayCreate;
        this.mayDelete = mayDelete;
        this.mayEdit = mayEdit;
        this.mayExport = mayExport;
        this.mayRelate = mayRelate;
        this.partialLoaded = position == PARTIAL_LOADED_POS;
    }

    /**
     * Get the position of this node
     *
     * @return position of this node
     */
    public int getPosition() {
        return position;
    }

    /**
     * Is this FxTreeNode only partially loaded?
     * Loading a (sub)tree is usually performed with partial loading enabled (only the calling
     * users current language filled in the label)
     *
     * @return if this FxTreeNode is only partially loaded
     */
    public boolean isPartialLoaded() {
        return this.partialLoaded;
    }

    /**
     * Is this node "live"?
     *
     * @return if the node is "live"
     */
    public boolean isLive() {
        return mode == FxTreeMode.Live;
    }

    /**
     * Is this node flagged as dirty?
     *
     * @return node flagged as dirty?
     */
    public boolean isDirty() {
        return dirty;
    }

    /**
     * {@inheritDoc}
     */
    public long getId() {
        return id;
    }

    /**
     * Get the id of the first ACL assigned to the referenced content
     *
     * @return id of the first ACL assigned to the referenced content
     * @deprecated use {@link #getACLIds()}
     */
    public long getACLId() {
        return acls != null && acls.length > 0 ? acls[0] : ACL.NULL_ACL_ID;
    }

    /**
     * Return the ACL id(s) of the referenced content.
     *
     * @return the ACL id(s) of the referenced content.
     */
    public List<Long> getACLIds() {
        return Arrays.asList(ArrayUtils.toObject(acls));
    }

    /**
     * Get the id of the parent node
     *
     * @return id of the parent node
     */
    public long getParentNodeId() {
        return parentNodeId;
    }

    /**
     * Get the name of this node
     *
     * @return name of this node
     */
    public String getName() {
        return name;
    }

    /**
     * Is a reference set for this node (Description)
     *
     * @return if a reference is set
     */
    public boolean hasReference() {
        return reference != null;
    }

    /**
     * Get the referenced content
     *
     * @return referenced content
     */
    public FxPK getReference() {
        return reference;
    }

    /**
     * Get the LifeCycleInfo of the referenced content
     *
     * @return LifeCycleInfo of the referenced content
     * @since 3.1
     */
    public LifeCycleInfo getReferenceLifeCycleInfo() {
        return referenceLifeCycleInfo;
    }

    /**
     * Get the type id of the referenced content
     *
     * @return type id of the referenced content
     */
    public long getReferenceTypeId() {
        return referenceTypeId;
    }

    /**
     * Get the lock of the reference, if set
     *
     * @return lock of the reference, if set
     * @since 3.1
     */
    public FxLock getLock() {
        return lock;
    }

    /**
     * Get the timestamp of the last modification
     *
     * @return timestamp of the last modification
     */
    public long getModifiedAt() {
        return modifiedAt;
    }

    /**
     * Is data assigned to this node?
     *
     * @return if data is assigned to this node
     */
    public boolean hasData() {
        return data != null;
    }

    /**
     * Get the template assigned to this node, can be <code>null</code>
     *
     * @return template assigned to this node, can be <code>null</code>
     */
    public String getData() {
        return data;
    }

    /**
     * Returns the depth of the node within the complete tree
     * The root node has depth 1
     *
     * @return the depth of the node
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get the number of child nodes attached to this node and all subchildren.
     * <p>
     * <strong>Warning:</strong>
     * From version 3.1 onwards, the total child count is no longer stored in the database
     * to improve performance for concurrent tree updates. Thus it is calculated on-the-fly
     * only when <strong>individual</strong> nodes are loaded. Most notably, it is not calculated
     * when fetching a (sub-)tree via
     * {@link com.flexive.shared.interfaces.TreeEngine#getTree(FxTreeMode, long, int)},
     * in this case, it is always -1.
     * </p>
     *
     * @return the number of child nodes attached to this node and all subchildren
     */
    public int getTotalChildCount() {
        return totalChildCount;
    }

    /**
     * Get the number of child nodes directly attached to this node
     *
     * @return the number of child nodes directly attached to this node
     */
    public int getDirectChildCount() {
        return directChildCount;
    }

    /**
     * Is this a leaf node?
     *
     * @return if this node is a leaf node
     */
    public boolean isLeaf() {
        return leaf;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized FxString getLabel() {
        return label;
    }

    /**
     * Get the path of this node
     *
     * @return path
     */
    public String getPath() {
        return path;
    }

    /**
     * Returns the children of this node, but only if they are a part of the resultset - use
     * isLeaf(), getDirectChildCount() and getTotalChildCount() to find out if the node has
     * children.
     * This function never returns null, but a empty List when no children are available
     *
     * @return the children of this node, but only if they are a part of the resultset
     */
    public List<FxTreeNode> getChildren() {
        return children;
    }

    /**
     * Returns the child Id's of this node, but only if they are a part of the resultset.
     *
     * @return the child Id's of this node, but only if they are a part of the resultset
     */
    public synchronized List<Long> getChildIds() {
        if (childIds == null) {
            childIds = new ArrayList<Long>(children.size());
            for (FxTreeNode child : getChildren())
                childIds.add(child.getId());
        }
        return childIds;
    }

    /**
     * ACL: Edit permission for the calling user
     *
     * @return ACL: Edit permission for the calling user
     */
    public boolean isMayEdit() {
        return mayEdit;
    }

    /**
     * ACL: Delete permission for the calling user
     *
     * @return ACL: Delete permission for the calling user
     */
    public boolean isMayDelete() {
        return mayDelete;
    }

    /**
     * ACL: Relate permission for the calling user
     *
     * @return ACL: Relate permission for the calling user
     */
    public boolean isMayRelate() {
        return mayRelate;
    }

    /**
     * ACL: Export permission for the calling user
     *
     * @return ACL: Export permission for the calling user
     */
    public boolean isMayExport() {
        return mayExport;
    }

    /**
     * ACL: Create permission for the calling user
     *
     * @return ACL: Create permission for the calling user
     */
    public boolean isMayCreate() {
        return mayCreate;
    }

    /**
     * Is this node marked to be deleted? (only used in GUI, not persisted!)
     *
     * @return marked for delete
     */
    public boolean isMarkForDelete() {
        return markForDelete;
    }

    /**
     * Mark to be deleted for UI (only used in GUI, not persisted!)
     *
     * @param markForDelete delete?
     * @return this
     */
    public FxTreeNode setMarkForDelete(boolean markForDelete) {
        this.markForDelete = markForDelete;
        return this;
    }

    /**
     * Set the active flag of this tree node (only used in GUI, not persisted!)
     *
     * @param activate activate flag
     */
    public void setActivate(boolean activate) {
        this.activate = activate;
    }

    /**
     * Is the node marked as active? (only used in GUI, not persisted!)
     *
     * @return active status
     */
    public boolean isActivate() {
        return activate;
    }


    /**
     * Is this node temporary only? (only used in GUI, not persisted!)
     *
     * @return if this node is temporary only
     */
    public boolean isTemporary() {
        return temporary;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (int) this.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null || (!(obj instanceof SelectableObject)))
            return false;

        SelectableObject comp = (SelectableObject) obj;
        return this.getId() == comp.getId();
    }

    /**
     * Create a temporary error node to be used in UI
     *
     * @param nodeId  desired node id
     * @param message error message
     * @return FxTreeNode
     */
    public static FxTreeNode createErrorNode(long nodeId, String message) {
        return new FxTreeNode(FxTreeMode.Edit, FxLock.noLockPK(), nodeId, 0, FxPK.createNewPK(),
                new FxContentVersionInfo.NewLifeCycleInfoImpl(), 0L,
                Arrays.asList(ACLCategory.INSTANCE.getDefaultId()), "Error", message, new FxString(false, "Error"),
                Integer.MAX_VALUE, new ArrayList<FxTreeNode>(0), new ArrayList<Long>(0), 0, 0, 0, true, true,
                System.currentTimeMillis(), "", true, true, true, true, true);
    }

    /**
     * Get the tree mode (live or edit)
     *
     * @return tree mode
     */
    public FxTreeMode getMode() {
        return mode;
    }

    /**
     * Make this node editable
     *
     * @return FxTreeNodeEdit
     */
    public FxTreeNodeEdit asEditable() {
        return new FxTreeNodeEdit(this);
    }

    /**
     * Flag this FxTreeNode as temporary
     *
     * @return this
     */
    private FxTreeNode flagTemporary() {
        this.temporary = true;
        return this;
    }

    /**
     * Create a temporary node below the given parent node
     *
     * @param parentNode the parent node
     * @return temporary node
     */
    public static FxTreeNode createNewTemporaryChildNode(FxTreeNode parentNode) {
        FxTreeNode n = new FxTreeNode(parentNode.getMode(), FxLock.noLockPK(), (System.currentTimeMillis() * -1), parentNode.getId(),
                FxPK.createNewPK(), new FxContentVersionInfo.NewLifeCycleInfoImpl(), 0L,
                Arrays.asList(ACLCategory.INSTANCE.getDefaultId()), "@@TMP", "",
                new FxString(parentNode.getLabel().isMultiLanguage(), ""), Integer.MAX_VALUE,
                new ArrayList<FxTreeNode>(0), new ArrayList<Long>(0), 0, 0, 0, true, true,
                System.currentTimeMillis(), "", true, true, true, true, true).flagTemporary();
        n.path = parentNode.getPath() + (parentNode.getPath().endsWith("/") ? "" : "/") + "*";
        return n;
    }

    /**
     * Internal method only used during loading phase of a tree
     *
     * @param node child node to add
     */
    public synchronized void _addChild(FxTreeNode node) {
        children.add(node);
        childIds.add(node.getId());
    }

    /**
     * Internal method to recursively apply a path from the root to all children
     *
     * @param path path to apply (includes name already!)
     */
    public void _applyPath(String path) {
        this.path = path;
        for (FxTreeNode node : this.getChildren())
            node._applyPath(path + (path.endsWith("/") ? node.getName() : "/" + node.getName()));
    }

    /**
     * Internal method to recursively apply positions
     *
     * @param position position to apply to this node
     */
    public void _applyPosition(int position) {
        this.position = position;
        int pos = 0;
        for (FxTreeNode node : this.getChildren())
            node._applyPosition(pos++);
    }

    /**
     * Returns an iterator over this node and its children.
     *
     * @return an iterator over this node and its children.
     */
    public Iterator<FxTreeNode> iterator() {
        return new NodeIterator();
    }

    private class NodeIterator implements Iterator<FxTreeNode>, Serializable {
        private static final long serialVersionUID = -3147200089772263291L;
        private int index = -1;
        private Iterator<FxTreeNode> childIterator;

        public boolean hasNext() {
            return index == -1 ||
                    (index >= 0 && !children.isEmpty() && (
                            (children.size() > index) ||
                                    (children.size() == index && childIterator != null && childIterator.hasNext())
                    )
                    );
        }

        public FxTreeNode next() {
            if (index == -1) {
                index++;
                return FxTreeNode.this;
            } else {
                if (childIterator == null || !childIterator.hasNext()) {
                    childIterator = children.get(index).iterator();
                    index++;
                }
                return childIterator.next();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
