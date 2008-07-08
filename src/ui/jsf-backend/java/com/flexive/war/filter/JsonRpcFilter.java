/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.filter;

import com.flexive.faces.components.JsonRpcCallRenderer;
import com.flexive.faces.javascript.yui.YahooResultProvider;
import com.flexive.war.javascript.BriefcaseEditor;
import com.flexive.war.javascript.ContentEditor;
import com.flexive.war.javascript.SearchQueryEditor;
import com.flexive.war.javascript.SystemInformation;
import com.flexive.war.javascript.search.SearchResultWriter;
import com.flexive.war.javascript.tree.ContentTreeEditor;
import com.flexive.war.javascript.tree.ContentTreeWriter;
import com.flexive.war.javascript.tree.StructureTreeEditor;
import com.flexive.war.javascript.tree.StructureTreeWriter;
import com.metaparadigm.jsonrpc.JSONRPCBridge;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;

public class JsonRpcFilter implements Filter {

    /**
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
    }

    /**
     * {@inheritDoc}
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // set JSON bridge
        getJsonBridge(((HttpServletRequest) servletRequest).getSession());
        filterChain.doFilter(servletRequest, servletResponse);
    }

    /**
     * Initialize the JSON/RPC bridge, if not present.
     *
     * @param session the current user's session
     * @return the JSON/RPC bridge with all registered objects
     */
    public static JSONRPCBridge getJsonBridge(HttpSession session) {
        if (session.getAttribute(JsonRpcCallRenderer.SESSION_JSON_BRIDGE) != null) {
            return (JSONRPCBridge) session.getAttribute(JsonRpcCallRenderer.SESSION_JSON_BRIDGE);
        }
        JSONRPCBridge bridge = new JSONRPCBridge();
        bridge.registerObject("SearchResultWriter", new SearchResultWriter());
        bridge.registerObject("StructureTreeWriter", new StructureTreeWriter());
        bridge.registerObject("StructureTreeEditor", new StructureTreeEditor());
        bridge.registerObject("ContentTreeWriter", new ContentTreeWriter());
        bridge.registerObject("ContentTreeEditor", new ContentTreeEditor());
        bridge.registerObject("ContentEditor", new ContentEditor());
        bridge.registerObject("BriefcaseEditor", new BriefcaseEditor());
        bridge.registerObject("SearchQueryEditor", new SearchQueryEditor());
        bridge.registerObject("SystemInformation", new SystemInformation());
        // TODO move Yahoo-UI stuff to JSF-components
        bridge.registerObject("YahooResultProvider", new YahooResultProvider());
        session.setAttribute(JsonRpcCallRenderer.SESSION_JSON_BRIDGE, bridge);
        return bridge;
    }
}
