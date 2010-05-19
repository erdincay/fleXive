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
package com.flexive.faces;

import java.util.Map;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ResourceHandler;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import org.apache.commons.lang.StringUtils;

/**
 * JSF2-specific utility functions.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxJsf2Utils {

    /**
     * Add the given resource programmatically to the current view (like @ResourceDependency).
     *
     * @param library           the resource library
     * @param resourceName      the resource name (path)
     * @param target            the target (head, body, or null)
     * @throws FacesException   on errors
     */
    public static void addResource(String library, String resourceName, String target) throws FacesException {
        final FacesContext context = FacesContext.getCurrentInstance();
        final Application app = context.getApplication();
        final ResourceHandler rh = app.getResourceHandler();

        // create a UIOutput component to serve the resource (see javadoc of ResourceDependency)
        final UIComponent comp = app.createComponent("javax.faces.Output");
        comp.setRendererType(rh.getRendererTypeForResourceName(resourceName));

        // store resource attributes
        final Map<String, Object> attributes = comp.getAttributes();
        attributes.put("name", resourceName);
        if (StringUtils.isNotBlank(library)) {
            attributes.put("library", library);
        }
        if (StringUtils.isNotBlank(target)) {
            attributes.put("target", target);
        }

        // add component resource to view
        if (target != null) {
            context.getViewRoot().addComponentResource(context, comp, target);
        } else {
            context.getViewRoot().addComponentResource(context, comp);
        }
    }


}
