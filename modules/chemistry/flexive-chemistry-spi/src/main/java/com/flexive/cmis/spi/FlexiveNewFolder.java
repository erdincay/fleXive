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
package com.flexive.cmis.spi;

import com.flexive.shared.EJBLookup;
import static com.flexive.shared.EJBLookup.getTreeEngine;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNodeEdit;
import com.flexive.shared.tree.FxTreeNode;
import org.apache.chemistry.Folder;
import org.apache.chemistry.Type;

/**
 * A new folder instance that will be inserted into the tree when calling {@code save()}..
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FlexiveNewFolder extends FlexiveFolder {
    private FxContent content;
    private FxTreeNodeEdit node;
    private final FlexiveFolder parent;

    /**
     * Create a new folder instance.
     *
     * @param context   the connection context
     * @param typeId    the folder type ID
     * @param parent    the parent folder
     */
    FlexiveNewFolder(FlexiveConnection.Context context, String typeId, FlexiveFolder parent) {
        super(context, parent == null
                ? null
                : FxTreeNodeEdit.createNew("").setParentNodeId(parent.getNode().getId())
        );
        this.parent = parent;
        try {
            content = EJBLookup.getContentEngine().initialize(SPIUtils.getFxTypeName(typeId));
            if (parent != null) {
                content.setAclIds(parent.getNode().getACLIds());
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        node = super.getNode();
    }

    /**
     * Create a new folder instance.
     *
     * @param context   the connection context
     * @param node      the tree node
     * @param content   the folder content
     * @param parent    the parent folder
     */
    FlexiveNewFolder(FlexiveConnection.Context context, FxTreeNode node, FxContent content, FlexiveFolder parent) {
        super(context, node);
        if (node.getId() >= 0) {
            throw new IllegalArgumentException("Node has already been saved (ID=" + node.getId() + ")");
        }
        if (!content.getPk().isNew()) {
            throw new IllegalArgumentException("Content is already saved (PK=" + content.getPk().toString() + ")");
        }
        this.parent = parent;
        this.content = content;
        this.node = super.getNode();
    }

    @Override
    public String getId() {
        return isNew() ? null : super.getId();
    }

    @Override
    protected FxContent getContent() {
        return content;
    }

    @Override
    public FxTreeNodeEdit getNode() {
        return node;
    }

    @Override
    protected long getNodeId() {
        return node.getId();
    }

    @Override
    protected FxTreeMode getTreeMode() {
        return node.getMode();
    }

    @Override
    public Type getType() {
        return isNew() ? new FlexiveType(content.getTypeId()) : super.getType();
    }

    @Override
    public void save() {
        if (content.getPk().isNew()) {
            // check if a folder item with our (label) path already exists
            if (parent != null) {
                checkUniqueName(
                        getNodeWithChildren(parent.getNode()),
                        getName()
                );
            }
            
            // save content and folder
            try {
                content = getContent().save();
                if (getNode() != null) {
                    getNode().setReference(content.getPk());
                    if (parent != null) {
                        // update parent node ID, because it might have changed in case that the parent folder
                        // is a "new" folder too
                        if (parent.getNodeId() < 0) {
                            throw new IllegalArgumentException("Please save the parent folder [" + parent + "] before saving a child node.");
                        }
                        node.setParentNodeId(parent.getNodeId());
                    }
                    try {
                        node = getTreeEngine().getNode(FxTreeMode.Edit, getTreeEngine().save(node)).asEditable();
                    } catch (FxApplicationException e) {
                        throw e.asRuntimeException();
                    }
                    super.save();   // process folders added to this node (TODO: calls TreeEngine#save twice)
                }
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        } else {
            super.save();
        }
        if (parent != null) {
            parent.refreshNode();
        }
    }

    @Override
    public void move(Folder targetFolder, Folder sourceFolder) {
        super.move(targetFolder, sourceFolder);
        // apply change on our node
        node.setParentNodeId(SPIUtils.getNodeId(targetFolder.getId()));
    }

    private boolean isNew() {
        return getNodeId() < 0;
    }
}
