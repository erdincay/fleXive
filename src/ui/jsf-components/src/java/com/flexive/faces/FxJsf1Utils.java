/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.faces;

import com.sun.facelets.tag.jsf.ComponentSupport;
import net.java.dev.weblets.FacesWebletUtils;
import org.apache.commons.lang.StringUtils;

import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;

/**
 * JSF1-specific utility methods
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.2
 */
public class FxJsf1Utils {

    private FxJsf1Utils() {
    }

    /**
     * Workaround for facelets component tree update problems - deletes
     * the given component and its children so they can be recreated
     * when the component tree is rendered again
     *
     * @param componentId the complete component ID, e.g. frm:queryEditor
     */
    public static void resetFaceletsComponent(String componentId) {
        if (StringUtils.isBlank(componentId)) {
            return;
        }
        UIViewRoot viewRoot = FacesContext.getCurrentInstance().getViewRoot();
        if (viewRoot == null) {
            return;
        }
        try {
            UIComponent component = viewRoot.findComponent(componentId);
            if (component != null) {
                ComponentSupport.markForDeletion(component);
                ComponentSupport.finalizeForDeletion(component);
            }
        } catch (IllegalArgumentException e) {
            // RI throws IAE when the component isn't found - do nothing
        }
    }

    /**
     * Workaround for facelets component tree update problems - deletes
     * the given components and its children so they can be recreated
     * when the component tree is rendered again
     *
     * @param componentIds comma separated component ids, e.g. frm:queryEditor, frm:contentEditor
     */
    public static void resetFaceletsComponents(String componentIds) {
        if (StringUtils.isBlank(componentIds)) {
            return;
        }
        String[] ids = StringUtils.split(componentIds,",");
        for (String id : ids) {
            resetFaceletsComponent(id.trim());
        }
    }

    /**
     * Workaround for facelets component tree update problems - deletes
     * the given components and its children so they can be recreated
     * when the component tree is rendered again.
     * Prefixes component ids with the given form prefix.
     * (e.g prefix "frm" and componentId "queryEditor" result in "frm:queryEditor")
     *
     * @param formPrefix the form prefix, e.g. frm
     * @param componentIds comma separated component ids, e.g. queryEditor, contentEditor
     */
    public static void resetFaceletsComponents(String formPrefix, String componentIds) {
        if (StringUtils.isBlank(componentIds)) {
            return;
        }
        if (StringUtils.isEmpty(formPrefix)) {
            resetFaceletsComponents(componentIds);
        }
        else {
            String[] ids = StringUtils.split(componentIds,",");
            for (String id : ids) {
                resetFaceletsComponent(formPrefix.trim() + NamingContainer.SEPARATOR_CHAR + id.trim());
            }
        }
    }

    /**
     * Replacement for fx:webletUrl that uses the current faces context (fx:webletUrl). The returned URL
     * is absolute and contains the context path.
     *
     * @param weblet    the weblet ID
     * @param pathInfo  the weblet path
     * @return          an URL for the given weblet
     */
    public static String getWebletUrl(String weblet, String pathInfo) {
        return FacesWebletUtils.getURL(
                FacesContext.getCurrentInstance(),
                weblet,
                pathInfo
        );
    }

}
