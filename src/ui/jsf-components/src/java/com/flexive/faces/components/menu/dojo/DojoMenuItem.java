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
import com.flexive.faces.javascript.menu.MenuItemContainer;
import com.flexive.faces.javascript.menu.DojoMenuItemData;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adds a dojo menu item to the enclosing Dojo menu container. DojoMenuItems
 * can be nested to create submenus, i.e. a DojoMenuItem is an item container
 * like DojoMenu itself.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @see DojoMenu
 */
public class DojoMenuItem extends UIOutput implements MenuItemContainer<DojoMenuItemData> {
    public static final String COMPONENT_TYPE = "flexive.DojoMenuItem";

    private final List<DojoMenuItemData> menuItems = new ArrayList<DojoMenuItemData>();
    private String labelKey;
    private String label;
    private String icon;
    private String clickHandler;
    private String link;
    private Map<String, Object> itemProperties;

    public DojoMenuItem() {
        setRendererType(null);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        MenuItemContainer container = FxJsfUtils.findAncestor(this, MenuItemContainer.class);
        String itemLabel = getLabel() != null ? getLabel() : FxJsfUtils.getLocalizedMessage(getLabelKey());
        // create an item that also includes nested menu items (added in the body of this component)
        DojoMenuItemData item = new DojoMenuItemData(getClientId(facesContext), itemLabel, getIcon(), getClickHandler(), getItemProperties(), getMenuItems());
        container.addMenuItem(item);
    }

    /**
     * Return the new menu item's properties.
     *
     * @return the new menu item's properties
     */
    protected Map<String, Object> getItemProperties() {
        if (itemProperties == null) {
            itemProperties = new HashMap<String, Object>();
        }
        return itemProperties;
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

    public String getLabelKey() {
        if (labelKey == null) {
            labelKey = FxJsfComponentUtils.getStringValue(this, "labelKey");
        }
        return labelKey;
    }

    public void setLabelKey(String labelKey) {
        this.labelKey = labelKey;
    }

    public String getIcon() {
        if (icon == null) {
            icon = FxJsfComponentUtils.getStringValue(this, "icon");
        }
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getClickHandler() {
        if (clickHandler == null) {
            clickHandler = FxJsfComponentUtils.getStringValue(this, "clickHandler");
        }
        if (clickHandler == null && getLink() != null) {
            clickHandler = "function(menuItem) { loadContentPage('" + getLink() + "'); }";
        }
        return clickHandler;
    }

    public void setClickHandler(String clickHandler) {
        this.clickHandler = clickHandler;
    }

    public String getLabel() {
        if (label == null) {
            label = FxJsfComponentUtils.getStringValue(this, "label");
        }
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLink() {
        if (link == null) {
            link = FxJsfComponentUtils.getStringValue(this, "link");
        }
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[6];
        state[0] = super.saveState(facesContext);
        state[1] = clickHandler;
        state[2] = icon;
        state[3] = label;
        state[4] = labelKey;
        state[5] = link;
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
        icon = (String) state[2];
        label = (String) state[3];
        labelKey = (String) state[4];
        link = (String) state[5];
    }
}
