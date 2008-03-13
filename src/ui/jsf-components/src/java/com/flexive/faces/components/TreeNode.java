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
package com.flexive.faces.components;

import static com.flexive.faces.FxJsfComponentUtils.getStringValue;
import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.MessageBean;
import com.flexive.faces.javascript.tree.TreeNodeWriter;
import com.flexive.faces.javascript.tree.TreeNodeWriter.Node;
import org.apache.commons.lang.StringUtils;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tree node component. Specifies a node in a Tree. Must be nested in
 * a fxTree component. May include nested nodes in its body.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class TreeNode extends UIOutput {
    private String title = null;
    private String icon = null;
    private String titleClass = null;
    private String titleKey = null;
    private String link = null;

    /**
     * Default constructor.
     */
    public TreeNode() {
        setRendererType(null);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRendersChildren() {
        return false;
    }

    /**
     * Write all tree nodes, including optional child nodes, to the
     * given tree writer.
     *
     * @param writer the tree node writer
     * @throws IOException if the tree could not be written to the output writer
     */
    public void writeTreeNodes(TreeNodeWriter writer) throws IOException {
        List<TreeNode> childNodes = getNodeChildren();
        final String linkUrl = StringUtils.isNotBlank(getLink())
                ? getLink().startsWith("javascript:")
                ? getLink() : FxJsfUtils.getRequest().getContextPath() + getLink() 
                : null;
        if (childNodes.size() == 0) {
            writer.writeNode(new Node(getId(), getTitle(), StringUtils.defaultString(getTitleClass(), Node.TITLE_CLASS_LEAF),
                    getIcon(), linkUrl));
        } else {
            writer.startNode(new Node(getId(), getTitle(), StringUtils.defaultString(titleClass, Node.TITLE_CLASS_NODE),
                    getIcon(), linkUrl));
            writer.startChildren();
            for (TreeNode child : childNodes) {
                child.writeTreeNodes(writer);
            }
            writer.closeChildren();
            writer.closeNode();
        }
    }

    private List<TreeNode> getNodeChildren() {
        List<TreeNode> result = new ArrayList<TreeNode>();
        for (Object child : getChildren()) {
            if (child instanceof TreeNode) {
                TreeNode node = (TreeNode) child;
                result.add(node);
            }
        }
        return result;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getTitle() {
        if (StringUtils.isNotBlank(getTitleKey())) {
            return MessageBean.getInstance().getMessage(getTitleKey());
        }
        if (title == null) {
            return getStringValue(this, "title");
        }
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleKey() {
        if (titleKey == null) {
            return getStringValue(this, "titleKey");
        }
        return titleKey;
    }

    public void setTitleKey(String titleKey) {
        this.titleKey = titleKey;
    }

    public void setTitleClass(String titleClass) {
        this.titleClass = titleClass;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getIcon() {
        if (icon == null) {
            return getStringValue(this, "icon");
        }
        return icon;
    }


    public String getLink() {
        if (link == null) {
            link = getStringValue(this, "link");
        }
        return link;
    }


    public String getTitleClass() {
        if (titleClass == null) {
            return getStringValue(this, "titleClass");
        }
        return titleClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[6];
        state[0] = super.saveState(facesContext);
        state[1] = icon;
        state[2] = link;
        state[3] = title;
        state[4] = titleClass;
        state[5] = titleKey;
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState(FacesContext facesContext, Object o) {
        Object[] state = (Object[]) o;
        super.restoreState(facesContext, state[0]);
        icon = (String) state[1];
        link = (String) state[2];
        title = (String) state[3];
        titleClass = (String) state[4];
        titleKey = (String) state[5];
    }
}
