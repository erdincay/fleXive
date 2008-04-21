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
package com.flexive.faces;

import com.flexive.faces.beans.FxContentViewBean;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.application.NavigationHandler;
import javax.faces.context.FacesContext;
import java.io.IOException;
import java.util.Map;

/**
 * <p>The sole purpose of this navigation handler is to support external content URIs when
 * creating a new content instance with {@link FxContentViewBean}. When a new content was
 * created in the current request, the response is redirected to the external URI matching
 * the newly created content. Otherwise all subsequent calls relying on the external URI
 * (i.e. code that extracts the content ID from the URI) would fail.</p>
 * <p>
 * Note that you have to provide a constructor taking a single NavigationHandler argument for
 * your class, or JSF won't be able to add your handler to the chain.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class AbstractContentNavigationHandler extends NavigationHandler {
    private static final Log LOG = LogFactory.getLog(AbstractContentNavigationHandler.class);

    protected final NavigationHandler parent;
    protected final URIRouteCollection<ContentURIRoute> routes;

    protected AbstractContentNavigationHandler(NavigationHandler parent, URIRouteCollection<ContentURIRoute> routes) {
        this.parent = parent;
        this.routes = routes;
    }

    @Override
    public void handleNavigation(FacesContext facesContext, String fromAction, String outcome) {
        final Map requestMap = facesContext.getExternalContext().getRequestMap();
        final Boolean newFlag = (Boolean) requestMap.get(FxContentViewBean.REQUEST_ISNEW);
        if (newFlag != null && newFlag) {
            // got PK from content creation, redirect
            final FxContent content = (FxContent) requestMap.get(FxContentViewBean.REQUEST_CONTENT);
            final FxPK pk = (FxPK) requestMap.get(FxContentViewBean.REQUEST_PK);
            final ContentURIRoute route = ContentURIRoute.findForType(routes.getRoutes(),
                    CacheAdmin.getEnvironment().getType(content.getTypeId()).getName());
            if (route != null) {
                try {
                    facesContext.getExternalContext().redirect(facesContext.getExternalContext().getRequestContextPath()
                            + route.getMappedUri(pk));
                    return;
                } catch (IOException e) {
                    LOG.error("Failed to redirect to content URL: " + e.getMessage(), e);
                }
            }
        }
        parent.handleNavigation(facesContext, fromAction, outcome);
    }
}
