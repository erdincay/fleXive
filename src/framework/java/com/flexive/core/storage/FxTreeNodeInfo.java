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
package com.flexive.core.storage;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;

import java.io.Serializable;

/**
 * Information about a tree node that implementation specific and provide information about parameters
 * relevant to the nested set model.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public abstract class FxTreeNodeInfo implements Serializable {
    private static final long serialVersionUID = 6152424443687210402L;

    protected int totalChildCount;
    protected int directChildCount;
    protected int depth;
    protected long parentId;
    protected long id;
    protected String name;
    protected FxPK reference;
    protected long ACLId;
    protected FxTreeMode mode;
    protected int position;
    protected String template;
    protected long modifiedAt;
    protected boolean mayEdit, mayDelete, mayRelate, mayExport, mayCreate;

    /**
     * Ctor
     *
     * @param totalChildCount  total number of children
     * @param directChildCount number of direct children
     * @param depth            depth of this node
     * @param parentId         parent id
     * @param id               node id
     * @param name             name
     * @param reference        referenced content
     * @param ACLId            ACL of the referenced content
     * @param mode             tree mode
     * @param position         position
     * @param template         template
     * @param modifiedAt       last modified at
     * @param mayEdit          edit permission of the referenced ACL
     * @param mayCreate        create permission of the referenced ACL
     * @param mayDelete        delete permission of the referenced ACL
     * @param mayRelate        relate permission of the referenced ACL
     * @param mayExport        export permission of the referenced ACL
     */
    protected FxTreeNodeInfo(int totalChildCount, int directChildCount, int depth, long parentId, long id, String name,
                             FxPK reference, long ACLId, FxTreeMode mode, int position, String template, long modifiedAt,
                             boolean mayEdit, boolean mayDelete, boolean mayRelate, boolean mayExport, boolean mayCreate) {
        this.totalChildCount = totalChildCount;
        this.directChildCount = directChildCount;
        this.depth = depth;
        this.parentId = parentId;
        this.id = id;
        this.name = name;
        this.reference = reference;
        this.ACLId = ACLId;
        this.mode = mode;
        this.position = position;
        this.template = template;
        this.modifiedAt = modifiedAt;
        this.mayEdit = mayEdit;
        this.mayDelete = mayDelete;
        this.mayRelate = mayRelate;
        this.mayExport = mayExport;
        this.mayCreate = mayCreate;
    }

    /**
     * Default constructor
     */
    protected FxTreeNodeInfo() {
    }

    /**
     * Get the left slot
     *
     * @return left slot
     */
    public abstract Number getLeft();

    /**
     * Get the right slot
     *
     * @return right slot
     */
    public abstract Number getRight();

    /**
     * Get the parent left position
     *
     * @return parent left position
     */
    public abstract Number getParentLeft();

    /**
     * Get the parent right position
     *
     * @return parent right position
     */
    public abstract Number getParentRight();

    /**
     * Get the total number of children
     *
     * @return total number of children
     */
    public int getTotalChildCount() {
        return totalChildCount;
    }

    /**
     * Get the number of direct children
     *
     * @return number of direct children
     */
    public int getDirectChildCount() {
        return directChildCount;
    }

    /**
     * Get the depth of this node relative to the root node
     *
     * @return depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * Get the id of the parent node
     *
     * @return id of the parent node
     */
    public long getParentId() {
        return parentId;
    }

    /**
     * Get the id of the node
     *
     * @return id of the node
     */
    public long getId() {
        return id;
    }

    /**
     * Get the name of the node
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the primary key of the referenced content
     *
     * @return primary key of the referenced content
     */
    public FxPK getReference() {
        return reference;
    }

    /**
     * Get the id of the ACL assigned to the referenced content
     *
     * @return id of the ACL assigned to the referenced content
     */
    public long getACLId() {
        return ACLId;
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
     * Get the "tree" this node belongs to
     *
     * @return the "tree" this node belongs to
     */
    public FxTreeMode getMode() {
        return mode;
    }

    /**
     * Position in the hierarchy level below the parent node
     *
     * @return position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Get the assigned template
     *
     * @return assigned template
     */
    public String getTemplate() {
        return template;
    }

    /**
     * Comparator for the template
     *
     * @param compareTo template to compare to
     * @return has template
     */
    public boolean hasTemplate(String compareTo) {
        return !((template == null && compareTo != null) ||
                (template != null && compareTo == null)) &&
                (template == null || template.equals(compareTo));
    }

    /**
     * Get the last modification timestamp
     *
     * @return last modification timestamp
     */
    public long getModifiedAt() {
        return modifiedAt;
    }

    /**
     * Returns true if this is the root node.
     *
     * @return true if this is the root node
     */
    public boolean isRoot() {
        return this.id == FxTreeNode.ROOT_NODE;
    }

    /**
     * Are children attached to this node?
     *
     * @return if children are attached to this node
     */
    public boolean hasChildren() {
        return totalChildCount > 0;
    }

    /**
     * Returns true if the node is a child.
     *
     * @param node the node to check
     * @return true if the given node is a child
     */
    public abstract boolean isParentOf(FxTreeNodeInfo node);
}
