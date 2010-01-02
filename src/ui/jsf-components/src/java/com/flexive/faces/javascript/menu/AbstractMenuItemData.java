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
package com.flexive.faces.javascript.menu;

import com.flexive.faces.javascript.RelativeUriMapper;
import com.flexive.war.JsonWriter;

import java.io.IOException;
import java.util.*;

/**
 * A menu item.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class AbstractMenuItemData<T extends AbstractMenuItemData<T>> implements MenuItemContainer<T>, Iterable<T> {
    protected final String id;
    protected final String title;
    protected final String icon;
    protected final String onClick;
    protected final Map<String, Object> properties;
    protected final List<T> menuItems;
    protected final boolean itemGroup;

    protected AbstractMenuItemData(String title) {
        this(null, title, null, null, new HashMap<String, Object>(), new ArrayList<T>(), false);
    }

    /**
     * Creates a container menu item (for item groups).
     *
     * @param menuItems the nested menu items
     * @since 3.1
     */
    protected AbstractMenuItemData(List<T> menuItems) {
        this(null, null, null, null, new HashMap<String, Object>(), menuItems, true);
    }

    protected AbstractMenuItemData(String id, String title, String icon, String onClick, Map<String, Object> properties, List<T> menuItems, boolean itemGroup) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        this.onClick = onClick;
        this.properties = properties != null ? properties : new HashMap<String, Object>();
        this.menuItems = menuItems;
        this.itemGroup = itemGroup;
    }

    public abstract void renderItemAttributes(JsonWriter out, RelativeUriMapper uriMapper, Map<String, String> subscriptions,
                                     String widgetId) throws IOException;

    /**
     * {@inheritDoc}
     */
    public void addMenuItem(T menuItem) {
        menuItems.add(menuItem);
    }

    /**
     * {@inheritDoc}
     */
    public List<T> getMenuItems() {
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

    /**
     * Returns true if this item has no information on itself, but acts as a group container for
     * its menu items.
     *
     * @return  true for item group containers
     * @since 3.1
     */
    public boolean isItemGroup() {
        return itemGroup;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int index = 0;
            Iterator<T> current;

            public boolean hasNext() {
                return (current != null && current.hasNext()) || index < menuItems.size();
            }

            public T next() {
                if (current != null && current.hasNext()) {
                    return current.next();
                }
                // go to next menu item
                final T item = menuItems.get(index++);
                current = item.iterator();
                return item;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
