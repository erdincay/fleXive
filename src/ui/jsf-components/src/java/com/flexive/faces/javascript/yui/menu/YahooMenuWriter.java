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
package com.flexive.faces.javascript.yui.menu;

import com.flexive.war.JsonWriter;
import com.flexive.faces.javascript.RelativeUriMapper;
import com.flexive.faces.javascript.FxJavascriptUtils;
import com.flexive.faces.javascript.menu.AbstractMenuWriter;
import com.flexive.faces.javascript.menu.MenuItemContainer;

import java.io.IOException;
import java.io.Writer;
import java.io.Serializable;

/**
 * JSON renderer for Yahoo-style menus.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class YahooMenuWriter extends AbstractMenuWriter<YahooMenuItemData> implements Serializable {
    private static final long serialVersionUID = -6786448793772348178L;
    public static final String ICON_PATH = "adm/images/menu";
    private int submenuCounter = 1;

    public YahooMenuWriter() {
    }

    public YahooMenuWriter(JsonWriter out, String widgetId, RelativeUriMapper uriMapper) throws IOException {
        super(out, widgetId, uriMapper);
    }

    public YahooMenuWriter(Writer writer, String widgetId, RelativeUriMapper uriMapper) throws IOException {
        super(writer, widgetId, uriMapper);
    }

    @Override
    public void startSubMenu() throws IOException {
        out.startAttribute("submenu").startMap()
                .writeAttribute("id", menuId + "_submenu" + submenuCounter++)
                .startAttribute("itemdata").startArray();
    }

    @Override
    public void closeSubMenu() throws IOException {
        out.closeArray().closeMap();
    }

    @Override
    public void writeEventSubscriptions(Writer out) throws IOException {
        // TODO
    }

    public static void writeMenu(Writer writer, String widgetId, MenuItemContainer<YahooMenuItemData> container,
                                 RelativeUriMapper uriMapper, String trigger) throws IOException {
        // build JSON menu
        writer.write("var menuData = ");
        final YahooMenuWriter menuWriter = new YahooMenuWriter(writer, widgetId, uriMapper);
        for (YahooMenuItemData menuItem : container.getMenuItems()) {
            menuWriter.writeItem(menuItem);
        }
        menuWriter.finishResponse();
        writer.write(";\n");

        FxJavascriptUtils.writeYahooRequires(writer, "menu");
        writer.write("var " + widgetId + ";\n");
        final StringBuilder js = new StringBuilder();
        js.append("var __menu = new YAHOO.widget.ContextMenu(\"").append(widgetId)
                .append("\", { trigger: ").append(trigger).append(" });\n");
        js.append("__menu.addItems(menuData);\n");
        js.append(widgetId).append(" = __menu;\n");
        FxJavascriptUtils.onYahooLoaded(writer, "function() { " + js.toString() + "}");
    }
}
