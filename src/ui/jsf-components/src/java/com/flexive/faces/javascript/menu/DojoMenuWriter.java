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
package com.flexive.faces.javascript.menu;

import com.flexive.faces.javascript.RelativeUriMapper;
import com.flexive.shared.FxFormatUtils;
import com.flexive.war.JsonWriter;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.Writer;
import java.io.Serializable;
import java.util.Map;

/**
 * A writer for Dojo menu objects in JSON notation.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class DojoMenuWriter extends AbstractMenuWriter<DojoMenuItemData> implements Serializable {
    private static final long serialVersionUID = -4416660810793208460L;
    public static final String ICON_PATH = "adm/images/menu";

    public DojoMenuWriter(JsonWriter out, String widgetId, RelativeUriMapper uriMapper) throws IOException {
        super(out, widgetId, uriMapper);
    }

    public DojoMenuWriter(Writer writer, String widgetId, RelativeUriMapper uriMapper) throws IOException {
        super(writer, widgetId, uriMapper);
    }


    @Override
    public void startSubMenu() throws IOException {
        out.startAttribute("submenu").startArray();
    }

    @Override
    public void closeSubMenu() throws IOException {
        out.closeArray();
    }


    @Override
    public void writeEventSubscriptions(Writer out) throws IOException {
        for (Map.Entry<String, String> entry : engageSubscriptions.entrySet()) {
            out.write("dojo.event.topic.subscribe(dojo.widget.byId('" + entry.getKey() + "').eventNames.engage, "
                    + entry.getValue() + ");\n");
//            out.write("dojo.widget.byId('" + entry.getKey() + "').onClick = " + entry.getValue() + ";\n");
        }
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
                                 MenuItemContainer<DojoMenuItemData> container, RelativeUriMapper uriMapper, String contextMenuTarget, String showHandler) throws IOException {
        writer.write("var " + menuName + " = flexive.dojo.makeMenu("
                + (widgetId != null ? "'" + FxFormatUtils.escapeForJavaScript(widgetId) + "'" : "null")
                + ", '" + menuClass + "', '" + itemClass + "', ");
        // render menu-items in method call
        DojoMenuWriter menuWriter = new DojoMenuWriter(writer, widgetId, uriMapper);
        for (DojoMenuItemData item : container.getMenuItems()) {
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
}
