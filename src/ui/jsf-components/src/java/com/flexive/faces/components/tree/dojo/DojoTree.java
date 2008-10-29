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
package com.flexive.faces.components.tree.dojo;

import com.flexive.faces.FxJsfComponentUtils;
import static com.flexive.faces.FxJsfComponentUtils.getStringValue;
import com.flexive.faces.components.menu.dojo.TreeContextMenu;
import com.flexive.faces.javascript.tree.TreeNodeWriter;
import com.flexive.shared.exceptions.FxInvalidStateException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;

/**
 * Tree root component. Renders the tree DIVs and javascript to show
 * a dojo tree.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class DojoTree extends UIOutput {
    private static final Log LOG = LogFactory.getLog(DojoTree.class);

    private String name = null;
    private String targetId = null;
    private String clickHandler = null;
    private String menuHandler = null;
    private TreeNodeWriter nodeWriter = null;
    private boolean contentTree = false;
    private boolean selector = true;
    private boolean dragAndDrop = false;
    private boolean editor = false;
    private boolean docIcons = false;
    private boolean expandFirstNode = false;
    private String extensionPoint = null;
    private TreeContextMenu contextMenu = null;

    public DojoTree() {
        setRendererType("flexive.DojoTree");
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (name == null) {
            name = getStringValue(this, "name");
        }
        return name;
    }

    public String getTargetId() {
        if (targetId == null) {
            targetId = getStringValue(this, "targetId");
        }
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getClickHandler() {
        if (clickHandler == null) {
            clickHandler = getStringValue(this, "clickHandler");
        }
        return clickHandler;
    }

    public void setClickHandler(String clickHandler) {
        this.clickHandler = clickHandler;
    }


    public TreeNodeWriter getNodeWriter() {
        return nodeWriter;
    }

    public void setNodeWriter(TreeNodeWriter nodeWriter) {
        this.nodeWriter = nodeWriter;
    }

    /**
     * Returns true for content trees. Due to lazy loading, content trees
     * use a different tree controller.
     *
     * @return true for content trees
     */
    public boolean isContentTree() {
        return contentTree;
    }

    public void setContentTree(boolean contentTree) {
        this.contentTree = contentTree;
    }

    /**
     * Returns true if the Dojo selector controller should be used.
     *
     * @return true if the Dojo selector controller should be used.
     */
    public boolean isSelector() {
        return selector;
    }

    public void setSelector(boolean selector) {
        this.selector = selector;
    }

    /**
     * Returns true if the Dojo Drag&Drop controller should be used
     *
     * @return true if the Dojo Drag&Drop controller should be used
     */
    public boolean isDragAndDrop() {
        return dragAndDrop;
    }

    public void setDragAndDrop(boolean dragAndDrop) {
        this.dragAndDrop = dragAndDrop;
    }


    public TreeContextMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * Injected by an embedded TreeContextMenu tag.
     *
     * @param contextMenu the context menu
     */
    public void setContextMenu(TreeContextMenu contextMenu) {
        if (this.contextMenu != null) {
            throw new FxInvalidStateException(LOG, "ex.jsf.tree.contextMenu.alreadyDefined").asRuntimeException();
        }
        this.contextMenu = contextMenu;
    }

    public String getMenuHandler() {
        return menuHandler;
    }

    public void setMenuHandler(String menuHandler) {
        this.menuHandler = menuHandler;
    }

    public boolean isEditor() {
        return editor;
    }

    public void setEditor(boolean editor) {
        this.editor = editor;
    }

    public String getExtensionPoint() {
        if (extensionPoint == null) {
            return FxJsfComponentUtils.getStringValue(this, "extensionPoint");
        }
        return extensionPoint;
    }

    public void setExtensionPoint(String extensionPoint) {
        this.extensionPoint = extensionPoint;
    }

    /**
     * Return true if the document icons extension of the dojo tree should be used.
     * If this is enabled, the tree node icons are determined by the node type ("nodeDocType")
     * and are not rendered in the node itself.
     *
     * @return true if the document icons extension of the dojo tree should be used.
     */
    public boolean isDocIcons() {
        return docIcons;
    }

    public void setDocIcons(boolean docIcons) {
        this.docIcons = docIcons;
    }

    public boolean isExpandFirstNode() {
        return expandFirstNode;
    }

    public void setExpandFirstNode(boolean expandFirstNode) {
        this.expandFirstNode = expandFirstNode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[12];
        state[0] = super.saveState(facesContext);
        state[1] = clickHandler;
        state[2] = contentTree;
        state[3] = docIcons;
        state[4] = dragAndDrop;
        state[5] = editor;
        state[6] = expandFirstNode;
        state[7] = menuHandler;
        state[8] = name;
        state[9] = selector;
        state[10] = targetId;
        state[11] = extensionPoint;
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState(FacesContext facesContext, Object o) {
        Object[] state = (Object[]) o;
        super.restoreState(facesContext, state[0]);
        clickHandler = (String) state[1];
        contentTree = (Boolean) state[2];
        docIcons = (Boolean) state[3];
        dragAndDrop = (Boolean) state[4];
        editor = (Boolean) state[5];
        expandFirstNode = (Boolean) state[6];
        menuHandler = (String) state[7];
        name = (String) state[8];
        selector = (Boolean) state[9];
        targetId = (String) state[10];
        extensionPoint = (String) state[11];
    }
}
