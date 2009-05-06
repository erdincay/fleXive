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
package com.flexive.faces.components;

import com.flexive.faces.FxJsfUtils;
import static com.flexive.faces.FxJsfComponentUtils.getBooleanValue;
import com.flexive.faces.beans.SystemBean;
import static com.flexive.faces.javascript.FxJavascriptUtils.beginJavascript;
import static com.flexive.faces.javascript.FxJavascriptUtils.endJavascript;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.FxContext;

import javax.faces.component.UIOutput;
import javax.faces.context.FacesContext;
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
    public static final String COMPONENT_TYPE = "flexive.WriteWebletIncludes";

    private static final String REQ_WEBLETS = WriteWebletIncludes.class.getName() + ".WEBLETS";
    private static final String REQ_RENDERED = WriteWebletIncludes.class.getName() + ".WEBLETS_RENDERED";
    private static final String WEBLET_TINYMCE = "com.flexive.faces.weblets/js/tiny_mce/tiny_mce.js";
    private static final String WEBLET_YUI = "com.flexive.faces.weblets/js/yui/yuiloader/yuiloader-min.js";
    private static final String WEBLET_JSONRPC = "com.flexive.faces.weblets/js/jsonrpc.js";

    private static final String PADDING = "\n    ";

    private Map<String, Boolean> weblets = new HashMap<String, Boolean>();
    private boolean htmlEditor = false;
    private boolean yui = false;
    private boolean jsonRpc = false;
    private boolean all = false;

    public WriteWebletIncludes() {
        weblets.put("com.flexive.faces.weblets/js/flexiveComponents.js", false);
        weblets.put("com.flexive.faces.weblets/css/components.css", false);
        if(FxJsfUtils.getRequest() != null && String.valueOf(FxJsfUtils.getRequest().getBrowser()).equalsIgnoreCase("IE"))
            weblets.put("com.flexive.faces.weblets/css/componentsIE.css", false);
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
    public static String getWebletInclude(String weblet) {
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
        if (FxJsfUtils.getRequest().getAttribute(REQ_RENDERED) == null) {
            // render weblet includes only once
            for (String weblet : weblets.keySet()) {
                out.write(PADDING);
                out.write(StringUtils.defaultString(getWebletInclude(weblet)));
            }
            FxJsfUtils.getRequest().setAttribute(REQ_RENDERED, true);
        }
        // init flexiveComponents.js
        beginJavascript(out);
        out.write("flexive.baseUrl='" + FxJsfUtils.getManagedBean(SystemBean.class).getDocumentBase() + "';\n");
        out.write("flexive.componentsWebletUrl='" + FacesWebletUtils.getURL(facesContext, "com.flexive.faces.weblets", "") + "';\n");
        out.write("flexive.yuiBase=flexive.componentsWebletUrl + 'js/yui/';\n");
        final String languageIso = FxContext.getUserTicket().getLanguage().getIso2digit().toLowerCase();
        out.write("flexive.locale='" + languageIso + "';\n");
        out.write("flexive.guiTranslation='"
                + (FxSharedUtils.isTranslatedLocale(languageIso) ? languageIso : "en")
                + "';\n");
        endJavascript(out);
        if (isHtmlEditor()) {
            renderWebletInclude(out, WEBLET_TINYMCE);
            out.write(PADDING);
            beginJavascript(out);
            out.write("flexive.input.initHtmlEditor(false);");
            endJavascript(out);
        }
        if (isYui()) {
            renderWebletInclude(out, WEBLET_YUI);
        }
        if (isJsonRpc()) {
            renderWebletInclude(out, WEBLET_JSONRPC);
        }
    }

    private void renderWebletInclude(ResponseWriter out, String weblet) throws IOException {
        out.write(PADDING);
        out.write(getWebletInclude(weblet));
    }

    public boolean isHtmlEditor() {
        if (isAll()) {
            return true;
        }
        if (getBooleanValue(this, "htmlEditor") != null) {
            return getBooleanValue(this, "htmlEditor");
        }
        return htmlEditor;
    }

    public void setHtmlEditor(boolean htmlEditor) {
        this.htmlEditor = htmlEditor;
    }

    public boolean isYui() {
        if (isAll()) {
            return true;
        }
        if (getBooleanValue(this, "yui") != null) {
            return getBooleanValue(this, "yui");
        }
        return yui;
    }

    public void setYui(boolean yui) {
        this.yui = yui;
    }

    public boolean isJsonRpc() {
        if (isAll()) {
            return true;
        }
        if (getBooleanValue(this, "jsonRpc") != null) {
            return getBooleanValue(this, "jsonRpc");
        }
        return jsonRpc;
    }

    public void setJsonRpc(boolean jsonRpc) {
        this.jsonRpc = jsonRpc;
    }

    public boolean isAll() {
        if (getBooleanValue(this, "all") != null) {
            return getBooleanValue(this, "all");
        }
        return all;
    }

    public void setAll(boolean all) {
        this.all = all;
    }

    @Override
    public Object saveState(FacesContext context) {
        final Object[] state = new Object[5];
        state[0] = super.saveState(context);
        state[1] = this.htmlEditor;
        state[2] = this.yui;
        state[3] = this.all;
        state[4] = this.jsonRpc;
        return state;
    }

    @Override
    public void restoreState(FacesContext context, Object oState) {
        final Object[] state = (Object[]) oState;
        super.restoreState(context, state[0]);
        this.htmlEditor = (Boolean) state[1];
        this.yui = (Boolean) state[2];
        this.all = (Boolean) state[3];
        this.jsonRpc = (Boolean) state[4];
    }
}
