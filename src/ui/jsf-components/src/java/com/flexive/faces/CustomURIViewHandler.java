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
package com.flexive.faces;

import com.sun.facelets.FaceletViewHandler;

import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

/**
 * An extension to the facelet view handler that provides custom URIs using
 * {@link URIRoute URIRoutes}. You must provide a concrete implementation with
 * a public constructor that takes a single ViewHandler argument and passes
 * the URIRouteCollection to be used to this viewhandler.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class CustomURIViewHandler extends FaceletViewHandler {
    protected final URIRouteCollection<ContentURIRoute> routes;

    protected CustomURIViewHandler(ViewHandler parent, URIRouteCollection<ContentURIRoute> routes) {
        super(parent);
        this.routes = routes;
    }

    @Override
    protected String getRenderedViewId(FacesContext context, String actionId) {
        final URIMatcher uriMatcher = routes.getMatcher(actionId);
        return uriMatcher != null ? uriMatcher.apply(context) : super.getRenderedViewId(context, actionId);
    }

    @Override
    public UIViewRoot restoreView(FacesContext context, String viewId) {
        final URIMatcher uriMatcher = routes.getMatcher(viewId);
        if (uriMatcher != null) {
            // apply route before entering the JSF lifecycle (set request attributes, etc.)
            uriMatcher.apply(context);
        }
        return super.restoreView(context, viewId);
    }

    @Override
    public String getActionURL(FacesContext facesContext, String viewId) {
        final Object oRequest = facesContext.getExternalContext().getRequest();
        if (oRequest instanceof HttpServletRequest) {
            final HttpServletRequest request = (HttpServletRequest) oRequest;
            final String origUri = (String) request.getAttribute(ContentURIMatcher.REQUEST_ORIG_URI);
            final String mappedUri = (String) request.getAttribute(ContentURIMatcher.REQUEST_MAPPED_URI);
            if (origUri != null && mappedUri.equals(viewId)) {
                // use original url for postbacks
                return origUri;
            }
        }
        return super.getActionURL(facesContext, viewId);
    }

}
