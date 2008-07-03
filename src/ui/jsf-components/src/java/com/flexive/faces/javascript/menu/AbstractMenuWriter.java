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

import com.flexive.war.JsonWriter;
import com.flexive.faces.javascript.RelativeUriMapper;

import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.Writer;

/**
  * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
  * @version $Rev$
 */
public abstract class AbstractMenuWriter<TMenuItem extends AbstractMenuItemData<TMenuItem>> {

    protected JsonWriter out;
    protected final RelativeUriMapper uriMapper;
    protected boolean standAlone = false;
    protected Map<String, String> engageSubscriptions = new HashMap<String, String>();
    protected int itemIdCounter = 0;
    protected final String menuId;

    /**
     * Create a new menu writer using the given JSON writer.
     *
     * @param out an existing JsonWriter
     * @throws java.io.IOException if an output error occured
     */
    protected AbstractMenuWriter(JsonWriter out, String widgetId, RelativeUriMapper uriMapper) throws IOException {
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
    protected AbstractMenuWriter(Writer writer, String widgetId, RelativeUriMapper uriMapper) throws IOException {
        this(new JsonWriter(writer), widgetId, uriMapper);
        this.standAlone = true;
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

    public void startItem(TMenuItem item) throws IOException {
        out.startMap();
        final String widgetId = item.getId();
        itemIdCounter++;
        item.renderItemAttributes(out, uriMapper, engageSubscriptions, widgetId);
        if (item.getMenuItems().size() > 0) {
            // add nested menu items
            startSubMenu();
            for (TMenuItem child : item.getMenuItems()) {
                writeItem(child);
            }
            closeSubMenu();
        }
    }

    public abstract void startSubMenu() throws IOException;

    public abstract void closeSubMenu() throws IOException;

    public void closeItem() throws IOException {
        out.closeMap();
    }

    public void writeItem(TMenuItem item) throws IOException {
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
    public abstract void writeEventSubscriptions(Writer out) throws IOException;

    public Map<String, String> getEngageSubscriptions() {
        return engageSubscriptions;
    }

    public void setEngageSubscriptions(Map<String, String> engageSubscriptions) {
        this.engageSubscriptions = engageSubscriptions;
    }

}
