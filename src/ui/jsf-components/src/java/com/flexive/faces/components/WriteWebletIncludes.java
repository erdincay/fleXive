/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.faces.components;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.exceptions.FxNotFoundException;

import javax.faces.application.ViewHandler;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import static javax.faces.context.FacesContext.getCurrentInstance;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders all weblet resources required by the current page.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class WriteWebletIncludes extends UIOutput {
    private static final String REQ_WEBLETS = WriteWebletIncludes.class.getName() + ".WEBLETS";

    private List<String> weblets = new ArrayList<String>();

    public WriteWebletIncludes() {
        // add flexive shared files
        weblets.add("com.flexive.faces.weblets/js/flexiveComponents.js");
        weblets.add("com.flexive.faces.weblets/css/components.css");
        if (FxJsfUtils.getRequest() != null) {
            FxJsfUtils.getRequest().setAttribute(REQ_WEBLETS, weblets);
        }
    }

    /**
     * Add the given weblet resource for the current page.
     *
     * @param weblet the weblet to be added, e.g. "com.flexive.faces.web/css/components.css"
     */
    @SuppressWarnings({"unchecked"})
    public static void addWebletInclude(String weblet) {
        final List<String> requestWeblets = (List<String>) FxJsfUtils.getRequest().getAttribute(REQ_WEBLETS);
        if (requestWeblets == null) {
            throw new FxNotFoundException("ex.jsf.webletIncludes.noTagFound").asRuntimeException();
        }
        if (!requestWeblets.contains(weblet)) {
            requestWeblets.add(weblet);
        }
    }

    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        super.encodeBegin(facesContext);
        final ViewHandler viewHandler = FacesContext.getCurrentInstance().getApplication().getViewHandler();
        final ResponseWriter out = facesContext.getResponseWriter();
        for (String weblet : weblets) {
            final String url = viewHandler.getResourceURL(getCurrentInstance(), "weblet://" + weblet);
            if (weblet.endsWith(".js")) {
                out.write("\n    <script type=\"text/javascript\" src=\"");
                out.write(url);
                out.write("\"> </script>");
            } else if (weblet.endsWith(".css")) {
                out.write("\n    <link rel=\"stylesheet\" type=\"text/css\" href=\"");
                out.write(url);
                out.write("\"/>");
            }
        }

    }
}
