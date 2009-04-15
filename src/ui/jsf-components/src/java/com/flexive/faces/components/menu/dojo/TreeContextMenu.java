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
package com.flexive.faces.components.menu.dojo;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.components.tree.dojo.DojoTree;
import com.flexive.faces.javascript.menu.DojoMenuItemData;
import com.flexive.faces.javascript.menu.*;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tree context menu container.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class TreeContextMenu extends UIOutput implements MenuItemContainer<DojoMenuItemData> {
    public static final String COMPONENT_TYPE = "flexive.DojoTreeContextMenu";

    private final List<DojoMenuItemData> menuItems = new ArrayList<DojoMenuItemData>();
    private String showHandler;

    public TreeContextMenu() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getRendersChildren() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        // attach menu to enclosing tree
        FxJsfUtils.findAncestor(this, DojoTree.class).setContextMenu(this);
    }

    /**
     * {@inheritDoc}
     */
    public void addMenuItem(DojoMenuItemData menuItem) {
        menuItems.add(menuItem);
    }

    /**
     * {@inheritDoc}
     */
    public List<DojoMenuItemData> getMenuItems() {
        return menuItems;
    }

    public String getShowHandler() {
        if (showHandler == null) {
            showHandler = FxJsfComponentUtils.getStringValue(this, "showHandler");
        }
        return showHandler;
    }

    public void setShowHandler(String showHandler) {
        this.showHandler = showHandler;
    }
}
