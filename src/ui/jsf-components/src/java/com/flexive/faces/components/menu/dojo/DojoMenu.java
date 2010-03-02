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
package com.flexive.faces.components.menu.dojo;

import com.flexive.faces.FxJsfComponentUtils;
import com.flexive.faces.JsfRelativeUriMapper;
import com.flexive.faces.javascript.FxJavascriptUtils;
import com.flexive.faces.javascript.menu.DojoMenuItemData;
import com.flexive.faces.javascript.menu.*;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a Dojo menu component. Renders a stand-alone popup menu for a Dojo Menu2 widget
 * using javascript (i.e. no markup needs to be processed, and it does not work without javascript).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class DojoMenu extends UIOutput implements MenuItemContainer<DojoMenuItemData>, NamingContainer {
    public static final String COMPONENT_TYPE = "flexive.DojoMenu";

    private static final String DOJO_MENU = "PopupMenu2";
    private static final String DOJO_ITEM = "MenuItem2";

    private final List<DojoMenuItemData> menuItems = new ArrayList<DojoMenuItemData>();
    private String name;
    private String contextMenuTarget;
    private String showHandler;

    public DojoMenu() {
        setRendererType(null);
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public String getClientId(FacesContext facesContext) {
        if (getName() != null) {
            setId(getName());    // use the js component name as our id
        }
        return super.getClientId(facesContext);
    }

    @Override
    public boolean getRendersChildren() {
        return false;
    }

    /**
     * {@inheritDoc }
     */
    @Override
    public void encodeEnd(FacesContext facesContext) throws IOException {
        final ResponseWriter writer = facesContext.getResponseWriter();
        FxJavascriptUtils.beginJavascript(writer);
        FxJavascriptUtils.writeDojoRequires(writer, "dojo.widget.Menu2");
        writer.write("dojo.addOnLoad(function() {\n");
        // addOnLoad start --->
        DojoMenuWriter.writeMenu(writer, getName(), getName(), DOJO_MENU, DOJO_ITEM, this,
                new JsfRelativeUriMapper(), getContextMenuTarget(), getShowHandler());
        writer.write("document.getElementById('" + getName() + "').appendChild(" + getName() + ".domNode);\n");
        // <--- addOnLoad end
        writer.write("});\n");
        FxJavascriptUtils.endJavascript(writer);
        writer.write("<div id=\"" + getName() + "\"> </div>\n");
    }

    /**
     * {@inheritDoc}
     */
    public List<DojoMenuItemData> getMenuItems() {
        return menuItems;
    }

    public void addMenuItem(DojoMenuItemData menuItem) {
        menuItems.add(menuItem);
    }

    public String getName() {
        if (name == null) {
            name = FxJsfComponentUtils.getStringValue(this, "name");
        }
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContextMenuTarget() {
        if (contextMenuTarget == null) {
            contextMenuTarget = FxJsfComponentUtils.getStringValue(this, "contextMenuTarget");
        }
        return contextMenuTarget;
    }

    public void setContextMenuTarget(String contextMenuTarget) {
        this.contextMenuTarget = contextMenuTarget;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[4];
        state[0] = super.saveState(facesContext);
        state[1] = getName();
        state[2] = getContextMenuTarget();
        state[3] = getShowHandler();
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState(FacesContext facesContext, Object o) {
        Object[] state = (Object[]) o;
        super.restoreState(facesContext, state[0]);
        setName((String) state[1]);
        setContextMenuTarget((String) state[2]);
        setShowHandler((String) state[3]);
    }
}
