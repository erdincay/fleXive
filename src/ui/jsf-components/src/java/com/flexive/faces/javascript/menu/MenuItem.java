/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.faces.javascript.menu;

import com.flexive.faces.javascript.RelativeUriMapper;
import com.flexive.war.JsonWriter;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A menu item.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class MenuItem implements MenuItemContainer {
    private final static Map<String, Object> EMPTY_MAP = new HashMap<String, Object>();
    private final static List<MenuItem> EMPTY_LIST = new ArrayList<MenuItem>();

    private final String id;
    private final String title;
    private final String icon;
    private final String onClick;
    private final Map<String, Object> properties;
    private final List<MenuItem> menuItems;

    public MenuItem(String title) {
        this(null, title, null, null, EMPTY_MAP, EMPTY_LIST);
    }

    public MenuItem(String id, String title, String icon, String onClick, Map<String, Object> properties, List<MenuItem> menuItems) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.onClick = onClick;
        this.properties = properties != null ? properties : EMPTY_MAP;
        this.menuItems = menuItems;
    }

    public void renderItemAttributes(JsonWriter out, RelativeUriMapper uriMapper, Map<String, String> engageSubscriptions,
                                     String widgetId) throws IOException {
        final String caption = "<span id=\"" + widgetId + "\">" + title + "</span>";    // add an unique ID for browser tests
        out.writeAttribute("widgetId", widgetId);
        out.writeAttribute("caption", caption);
        if (StringUtils.isNotBlank(icon)) {
            out.writeAttribute("iconSrc", uriMapper.getAbsoluteUri(MenuWriter.ICON_PATH + "/" + icon + ".png"));
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            out.writeAttribute(entry.getKey(), entry.getValue());
        }
        if (StringUtils.isNotBlank(onClick)) {
            // event subscriptions will be written after the menu was rendered
            engageSubscriptions.put(widgetId, onClick);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addMenuItem(MenuItem menuItem) {
        menuItems.add(menuItem);
    }

    /**
     * {@inheritDoc}
     */
    public List<MenuItem> getMenuItems() {
        return menuItems;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getIcon() {
        return icon;
    }

    public String getOnClick() {
        return onClick;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }
}
