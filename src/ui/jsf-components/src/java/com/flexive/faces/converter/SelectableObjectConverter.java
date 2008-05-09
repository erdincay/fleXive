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
package com.flexive.faces.converter;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.beans.SelectBean;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.SelectableObjectWithLabel;
import com.flexive.shared.SelectableObject;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.structure.FxType;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Basic Converter for all subclasses of SelectableObjectWithName.
 */
public class SelectableObjectConverter implements Converter {

    /**
     * Returns the selectable object.
     *
     * @param facesContext       the context
     * @param uiComponent        the component
     * @param selectableObjectId the class and id of the selectable object as string
     * @return the selectable object, or null if no match was found
     */
    public Object getAsObject(FacesContext facesContext, UIComponent uiComponent, String selectableObjectId) {

        if (selectableObjectId == null || selectableObjectId.equals("")) {
            return null;
        }

        // Extract value informations
        long id;
        Class theClass;
        try {
            String split[] = selectableObjectId.split(":");
            id = Long.valueOf(split[1]);
            theClass = Class.forName(split[0]);
        } catch (Exception e) {
            new FxFacesMsgErr("ex.converter.selectableObjectConverter.getAsObjectError",
                    selectableObjectId).addToContext();
            return null;
        }

        // Try to find the object
        List<SelectItem> items = getOptions(theClass);
        for (SelectItem item : items) {
            SelectableObject so = (SelectableObject) item.getValue();
            if (so.getId() == id) return so;
        }

        return null;
    }

    private List<SelectItem> getOptions(Class c) {
        SelectBean sb = (SelectBean) FxJsfUtils.getManagedBean("fxSelectBean");
        // Load the items depending on the object class
        try {
            if (c.equals(ACL.class)) {
                return sb.getACLs();
            } else if (c.equals(FxType.class)) {
                return sb.getTypes();
            } else if (c.equals(UserGroup.class)) {
                return sb.getGlobalUserGroups();
            } else if (c.equals(Mandator.class)) {
                return sb.getMandators();
            } else if (c.equals(Role.class)) {
                return sb.getRoles();
            } else if (c.equals(FxScriptInfo.class)) {
                return sb.getAllScripts();
            } else {
                new FxFacesMsgErr("ex.converter.selectableObjectConverter.classNotSupported",
                        c.getName() + "'").addToContext();
                return new ArrayList<SelectItem>(0);
            }
        } catch (Exception t) {
            new FxFacesMsgErr(t).addToContext();
            return new ArrayList<SelectItem>(0);
        }
    }

    /**
     * Returns the id of the selectabe object as string.
     *
     * @return the id of the selectabe object as string
     */
    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object object) {

        // Null check
        if (object == null) {
            return null;
        }

        if (object instanceof SelectableObject && ((SelectableObject) object).getId() == -1) {
            return null;
        }

        // Process object
        final List<SelectItem> items = getOptions(object.getClass());
        if (object instanceof SelectableObjectWithLabel) {
            final SelectableObjectWithLabel input = (SelectableObjectWithLabel) object;
            try {
                // Return the id of the SelectableObjectWithName
                if (items != null) {
                    for (SelectItem item : items) {
                        SelectableObjectWithLabel so = (SelectableObjectWithLabel) item.getValue();
                        if (so.equals(object)) {
                            return so.getClass().getName() + ":" + so.getId();
                        }
                    }
                }
                return "error";
            } catch (Exception t) {
                new FxFacesMsgErr("ex.converter.selectableObjectConverter.getAsStringError",
                        input.getClass(), input.getId()).addToContext();
                return "error";
            }
        } else if (object instanceof SelectableObject) {
            final SelectableObject input = (SelectableObject) object;
            try {
                // Return the id of the SelectableObjectWithName
                if (items != null) {
                    for (SelectItem item : items) {
                        SelectableObject so = (SelectableObject) item.getValue();
                        if (so.equals(object)) {
                            return so.getClass().getName() + ":" + so.getId();
                        }
                    }
                }
                return "error";
            } catch (Exception t) {
                new FxFacesMsgErr("ex.converter.selectableObjectConverter.getAsStringError",
                        input.getClass(), input.getId()).addToContext();
                return "error";
            }
        } else
            return "error";
    }

}
