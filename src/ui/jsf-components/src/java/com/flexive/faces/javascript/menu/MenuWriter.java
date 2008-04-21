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
import com.flexive.shared.FxFormatUtils;
import com.flexive.war.JsonWriter;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * A writer for Dojo menu objects in JSON notation.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class MenuWriter {
    public static final String ICON_PATH = "adm/images/menu";

    private JsonWriter out;
    private final RelativeUriMapper uriMapper;
    private boolean standAlone = false;
    private Map<String, String> engageSubscriptions = new HashMap<String, String>();
    private int itemIdCounter = 0;
    private final String menuId;

    /**
     * Create a new menu writer using the given JSON writer.
     *
     * @param out an existing JsonWriter
     * @throws java.io.IOException if an output error occured
     */
    public MenuWriter(JsonWriter out, String widgetId, RelativeUriMapper uriMapper) throws IOException {
        this.out = out;
        this.menuId = widgetId;
        this.uriMapper = uriMapper;
        out.startArray();
    }

    /**
     * Create a menu writer that writes directly to the given
     * output writer.
     *
     * @param writer the output writer
     * @throws java.io.IOException if an output error occured
     */
    public MenuWriter(Writer writer, String widgetId, RelativeUriMapper uriMapper) throws IOException {
        this(new JsonWriter(writer), widgetId, uriMapper);
        this.standAlone = true;
    }

    /**
     * Render the JSON representation for the given menu.
     *
     * @param menuName          name of the JS variable where the menu widget is stored
     * @param menuClass         widget class of the menu widget, e.g. TreeContextMenuV3
     * @param itemClass         widget class of the menu items, e.g. TreeMenuItemV3
     * @param container         the menu container
     * @param contextMenuTarget target element if this menu should be attached as a context menu
     * @param showHandler       an optional javascript handler to be called when the menu is opened
     * @throws IOException if an output error occured @param writer    the output writer @param widgetId  the widget ID (set to null to use an auto-generated ID)
     */
    public static void writeMenu(Writer writer, String widgetId, String menuName, String menuClass, String itemClass,
                                 MenuItemContainer container, RelativeUriMapper uriMapper, String contextMenuTarget, String showHandler) throws IOException {
        writer.write("var " + menuName + " = makeMenu("
                + (widgetId != null ? "'" + FxFormatUtils.escapeForJavaScript(widgetId) + "'" : "null")
                + ", '" + menuClass + "', '" + itemClass + "', ");
        // render menu-items in method call
        MenuWriter menuWriter = new MenuWriter(writer, widgetId, uriMapper);
        for (MenuItem item : container.getMenuItems()) {
            menuWriter.writeItem(item);
        }
        menuWriter.finishResponse();
        writer.write(", false, " + (StringUtils.isNotBlank(contextMenuTarget) ? "'" + contextMenuTarget + "'" : "null")
                + ");\n");
        // add event subscriptions
        menuWriter.writeEventSubscriptions(writer);
        if (StringUtils.isNotBlank(showHandler)) {
            writer.write(menuName + ".show_ = " + menuName + ".show;\n");
            writer.write(menuName + ".show = " + showHandler + ";\n");
        }
    }

    /**
     * Ends the response.
     *
     * @throws java.io.IOException if an I/O error occured
     */
    public void finishResponse() throws IOException {
        out.closeArray();
        if (this.standAlone) {
            // finish JSON response
            out.finishResponse();
        }
    }

    public void startItem(MenuItem item) throws IOException {
        out.startMap();
        final String widgetId = item.getId();
        itemIdCounter++;
        item.renderItemAttributes(out, uriMapper, engageSubscriptions, widgetId);
        if (item.getMenuItems().size() > 0) {
            // add nested menu items
            out.startAttribute("submenu");
            out.startArray();
            for (MenuItem child : item.getMenuItems()) {
                writeItem(child);
            }
            out.closeArray();
        }
    }

    public void startSubMenu() throws IOException {
        out.startAttribute("submenu");
        out.startArray();
    }

    public void closeSubMenu() throws IOException {
        out.closeArray();
    }

    public void closeItem() throws IOException {
        out.closeMap();
    }

    public void writeItem(MenuItem item) throws IOException {
        startItem(item);
        closeItem();
    }

    /**
     * Render the event subscriptions accumulated while rendering the JSON code.
     *
     * @param out the output writer to be used. The JSON code must have been finished when
     *            this method is called!
     * @throws java.io.IOException if an I/O error occured
     */
    public void writeEventSubscriptions(Writer out) throws IOException {
        for (Map.Entry<String, String> entry : engageSubscriptions.entrySet()) {
            out.write("dojo.event.topic.subscribe(dojo.widget.byId('" + entry.getKey() + "').eventNames.engage, "
                    + entry.getValue() + ");\n");
//            out.write("dojo.widget.byId('" + entry.getKey() + "').onClick = " + entry.getValue() + ";\n");
        }
    }

    public Map<String, String> getEngageSubscriptions() {
        return engageSubscriptions;
    }

    public void setEngageSubscriptions(Map<String, String> engageSubscriptions) {
        this.engageSubscriptions = engageSubscriptions;
    }
}
