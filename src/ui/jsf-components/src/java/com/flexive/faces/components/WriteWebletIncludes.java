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
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;

import javax.faces.application.ViewHandler;
import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
import static javax.faces.context.FacesContext.getCurrentInstance;
import javax.faces.context.ResponseWriter;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang.StringUtils;
import net.java.dev.weblets.FacesWebletUtils;

/**
 * Renders all weblet resources required by the current page.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class WriteWebletIncludes extends UIOutput {
    private static final String REQ_WEBLETS = WriteWebletIncludes.class.getName() + ".WEBLETS";
    private static final String REQ_RENDERED = WriteWebletIncludes.class.getName() + ".WEBLETS_RENDERED";

    private Map<String, Boolean> weblets = new HashMap<String, Boolean>();

    public WriteWebletIncludes() {
        weblets.put("com.flexive.faces.weblets/js/flexiveComponents.js", false);
        weblets.put("com.flexive.faces.weblets/css/components.css", false);
        if (FxJsfUtils.getRequest() != null) {
            FxJsfUtils.getRequest().setAttribute(REQ_WEBLETS, weblets);
        }
    }

    /**
     * Add the given weblet resource for the current page.
     *
     * @param weblet the weblet to be added, e.g. "com.flexive.faces.web/css/components.css"
     * @return  true if the weblet has not been included in the current request
     */
    private static boolean addWebletInclude(String weblet) {
        final Map<String, Boolean> requestWeblets  = getRequestWebletMap();
        if (requestWeblets == null) {
            throw new FxNotFoundException("ex.jsf.webletIncludes.noTagFound").asRuntimeException();
        }
        if (!requestWeblets.containsKey(weblet)) {
            requestWeblets.put(weblet, false);
            return true;
        }
        return false;
    }

    @SuppressWarnings({"unchecked"})
    private static Map<String, Boolean> getRequestWebletMap() {
        return (Map<String, Boolean>) FxJsfUtils.getRequest().getAttribute(REQ_WEBLETS);
    }

    /**
     * Return the HTML representation of the given weblet. For example, if the weblet
     * identifies a Javascript file (.js extension), a &lt;script> tag is rendered.
     *
     * @param weblet    the weblet URI to be rendered (without weblet://)
     * @return  the HTML representation of the given weblet, or null if the weblet was already rendered.
     */
    public static String renderWeblet(String weblet) {
        if (!addWebletInclude(weblet) && getRequestWebletMap().get(weblet)) {
            return null;    // weblet already rendered
        }
        //final ViewHandler viewHandler = FacesContext.getCurrentInstance().getApplication().getViewHandler();
        final int idx = weblet.indexOf("/");
        final String url = FacesWebletUtils.getURL(FacesContext.getCurrentInstance(), weblet.substring(0,idx), weblet.substring(idx, weblet.length()));
        //final String url = viewHandler.getResourceURL(getCurrentInstance(), "weblet://" + weblet);
        getRequestWebletMap().put(weblet, true);    // set rendered flag for this weblet
        if (weblet.endsWith(".js")) {
            return "<script type=\"text/javascript\" src=\"" + url + "\"> </script>";
        } else if (weblet.endsWith(".css")) {
            return "<link rel=\"stylesheet\" type=\"text/css\" href=\"" + url + "\"/>";
        }
        throw new FxInvalidParameterException("ex.jsf.webletIncludes.unknownType", weblet).asRuntimeException();
    }

    @Override
    public void encodeBegin(FacesContext facesContext) throws IOException {
        super.encodeBegin(facesContext);
        final ResponseWriter out = facesContext.getResponseWriter();
        for (String weblet : weblets.keySet()) {
            out.write("\n    " + StringUtils.defaultString(renderWeblet(weblet)));
        }
        FxJsfUtils.getRequest().setAttribute(REQ_RENDERED, true);
    }
}
