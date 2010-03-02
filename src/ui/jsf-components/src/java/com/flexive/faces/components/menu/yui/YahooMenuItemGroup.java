/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.faces.components.menu.yui;

import com.flexive.faces.javascript.menu.MenuItemContainer;
import com.flexive.faces.javascript.yui.menu.YahooMenuItemData;
import com.flexive.faces.FxJsfUtils;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import java.util.List;
import java.util.ArrayList;
import java.io.IOException;

/**
 * A menu item group for YUI's menu widget. Menu item groups are grouped on the same level, separated
 * with horizontal separator lines.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class YahooMenuItemGroup extends UIOutput implements MenuItemContainer<YahooMenuItemData> {
    private final List<YahooMenuItemData> menuItems = new ArrayList<YahooMenuItemData>();

    public YahooMenuItemGroup() {
        setRendererType(null);
    }

    @Override
    public boolean getRendersChildren() {
        return false;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        final MenuItemContainer container = FxJsfUtils.findAncestor(this, MenuItemContainer.class);
        container.addMenuItem(new YahooMenuItemData(menuItems));
    }

    /**
     * {@inheritDoc}
     */
    public void addMenuItem(YahooMenuItemData menuItem) {
        menuItems.add(menuItem);
    }

    /**
     * {@inheritDoc}
     */
    public List<YahooMenuItemData> getMenuItems() {
        return menuItems;
    }
}
