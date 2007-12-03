/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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
package com.flexive.shared.tree;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.security.ACL;
import com.flexive.shared.value.FxString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Editable tree node.
 * Make a tree node editable by invoking its <code>#asEditable()</Code> method
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see FxTreeNode#asEditable()
 */
public class FxTreeNodeEdit extends FxTreeNode implements Serializable {
    private static final long serialVersionUID = -2399817258615361847L;

    private boolean isNew;
    private FxTreeMode originalMode;
    private String newName;
    private boolean activateWithChildren = false;

    /**
     * Ctor
     *
     * @param node the tree node to make editable
     */
    public FxTreeNodeEdit(FxTreeNode node) {
        super(node.getMode(), node.getId(), node.getParentNodeId(), node.getReference(), node.getACLId(), node.getName(), node.getPath(),
                node.getLabel(), node.getPosition(), node.getChildren(), node.getChildIds(), node.getDepth(),
                node.getTotalChildCount(), node.getDirectChildCount(), node.isLeaf(), node.isDirty(),
                node.getModifiedAt(), node.getTemplate(), true, true, true, true, true);
        this.isNew = false;
        this.newName = node.getName();
        this.originalMode = node.getMode();
        this.parentNodeId = node.getParentNodeId();
    }

    /**
     * Create a new editable node that will be the child of the passed node.
     * Some assumptions are made: the ACL of the new node is the one of the parent node and the calling user has full access to the referenced content
     *
     * @param parentNode parent node
     * @return editable node
     */
    public static FxTreeNodeEdit createNewChildNode(FxTreeNode parentNode) {
        FxTreeNodeEdit edit = new FxTreeNode(parentNode.getMode(), (System.currentTimeMillis() * -1), parentNode.getId(),
                FxPK.createNewPK(), parentNode.getACLId(), "", "", new FxString(parentNode.getLabel().isMultiLanguage(), ""), Integer.MAX_VALUE,
                new ArrayList<FxTreeNode>(0), new ArrayList<Long>(0), 0, 0, 0, true, true,
                System.currentTimeMillis(), "", true, true, true, true, true).asEditable();
        edit.isNew = true;
        return edit;
    }

    /**
     * Create a new node with the given name
     *
     * @param name name of the node
     * @return editable tree node
     */
    public static FxTreeNodeEdit createNew(String name) {
        FxTreeNodeEdit edit = new FxTreeNode(FxTreeMode.Edit, (System.currentTimeMillis() * -1), ROOT_NODE,
                FxPK.createNewPK(), ACL.Category.INSTANCE.getDefaultId(), name, "", new FxString(false, name), Integer.MAX_VALUE,
                new ArrayList<FxTreeNode>(0), new ArrayList<Long>(0), 0, 0, 0, true, true,
                System.currentTimeMillis(), "", true, true, true, true, true).asEditable();
        edit.isNew = true;
        return edit;
    }

    /**
     * Is this editable tree node for a new or an already existing node?
     *
     * @return if this editable tree node is for a new or an already existing node
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Set the referenced content
     *
     * @param reference referenced content
     * @return this
     */
    public FxTreeNodeEdit setReference(FxPK reference) {
        this.reference = reference;
        return this;
    }

    /**
     * Clear the reference, setting it to <code>null</code>
     *
     * @return this
     */
    public FxTreeNodeEdit clearReference() {
        this.reference = null;
        return this;
    }

    /**
     * If the mode was altered, this method will return the original mode
     *
     * @return original mode
     */
    public FxTreeMode getOriginalMode() {
        return originalMode;
    }

    /**
     * Prepare this node for activation
     *
     * @param includeChildren activate all children as well?
     * @return this
     */
    public FxTreeNodeEdit activate(boolean includeChildren) {
        this.activate = true;
        this.activateWithChildren = includeChildren;
        return this;
    }

    /**
     * Activate with children?
     *
     * @return activate with children?
     */
    public boolean isActivateWithChildren() {
        return activateWithChildren;
    }

    /**
     * Assign a list of children
     *
     * @param children list of children
     * @return this
     */
    public FxTreeNodeEdit setChildren(List<FxTreeNode> children) {
        this.children = children;
        return this;
    }

    /**
     * Set the label
     *
     * @param label label
     * @return this
     */
    public synchronized FxTreeNodeEdit setLabel(FxString label) {
        this.label = label;
        return this;
    }

    /**
     * Set the name
     *
     * @param name name
     * @return this
     */
    public FxTreeNodeEdit setName(String name) {
        this.newName = name;
        return this;
    }

    /**
     * Set the id of the parent node
     *
     * @param parentNodeId id of the parent node
     * @return this
     */
    public FxTreeNodeEdit setParentNodeId(long parentNodeId) {
        this.parentNodeId = parentNodeId;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return newName;
    }

    /**
     * Set the tree mode
     *
     * @param mode tree mode
     * @return this
     */
    public FxTreeNodeEdit setMode(FxTreeMode mode) {
        this.mode = mode;
        return this;
    }

    /**
     * Set this nodes position
     *
     * @param position new position
     * @return this
     */
    public FxTreeNodeEdit setPosition(int position) {
        this.position = position;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTemplate() {
        if (this.template == null)
            return super.getTemplate();
        return template;
    }

    /**
     * Set the template
     *
     * @param template template
     */
    public void setTemplate(String template) {
        this.template = template;
    }
}
