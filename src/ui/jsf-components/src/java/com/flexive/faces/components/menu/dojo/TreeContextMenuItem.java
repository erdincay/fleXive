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

import static com.flexive.faces.FxJsfComponentUtils.getStringValue;
import org.apache.commons.lang.StringUtils;

import javax.faces.context.FacesContext;
import java.util.Arrays;
import java.util.Map;

/**
 * Renders a tree context menu item. Must be embedded in a fx:tree component.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @see com.flexive.faces.components.tree.dojo.DojoTree
 */
public class TreeContextMenuItem extends DojoMenuItem {
    private String treeActions;

    /**
     * {@inheritDoc}
     */
    @Override
    protected Map<String, Object> getItemProperties() {
        Map<String, Object> properties = super.getItemProperties();
        if (StringUtils.isNotBlank(getTreeActions())) {
            properties.put("treeActions", Arrays.asList(getTreeActions()));
        }
        return properties;
    }

    public String getTreeActions() {
        if (treeActions == null) {
            treeActions = getStringValue(this, "treeActions");
        }
        return treeActions;
    }

    public void setTreeActions(String treeActions) {
        this.treeActions = treeActions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object saveState(FacesContext facesContext) {
        Object[] state = new Object[2];
        state[0] = super.saveState(facesContext);
        state[1] = treeActions;
        return state;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreState(FacesContext facesContext, Object o) {
        Object[] state = (Object[]) o;
        super.restoreState(facesContext, state[0]);
        treeActions = (String) state[1];
    }
}
