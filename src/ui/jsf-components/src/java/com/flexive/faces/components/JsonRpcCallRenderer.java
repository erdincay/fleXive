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
package com.flexive.faces.components;

import com.flexive.faces.FxJsfUtils;
import com.flexive.war.JsonWriter;
import com.metaparadigm.jsonrpc.JSONRPCBridge;
import com.metaparadigm.jsonrpc.JSONRPCResult;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;
import java.io.IOException;
import java.io.StringWriter;

/**
 * Renderer for the JsonRpcCall component.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class JsonRpcCallRenderer extends Renderer {
    private static final Log LOG = LogFactory.getLog(JsonRpcCallRenderer.class);
    public static final String SESSION_JSON_BRIDGE = "JSONRPCBridge";

    /**
     * {@inheritDoc}
     */
    @Override
    public void encodeBegin(FacesContext facesContext, UIComponent uiComponent) throws IOException {
        JsonRpcCall rpcCall = (JsonRpcCall) uiComponent;
        if (StringUtils.isBlank(rpcCall.getMethod())) {
            LOG.error("JsonRpcCall without method called");
            return;
        }
        String jsonRequest = getJsonRequest(rpcCall);
        try {
            JSONRPCBridge bridge = (JSONRPCBridge) FxJsfUtils.getSession().getAttribute(SESSION_JSON_BRIDGE);
            JSONRPCResult result = bridge.call(new Object[]{FxJsfUtils.getRequest()}, new JSONObject(jsonRequest));
            facesContext.getResponseWriter().write(result.getResult().toString());
        } catch (Throwable e) {
            LOG.error("Failed to executed JSON/RPC request: " + e.getMessage(), e);
        }
    }

    private String getJsonRequest(JsonRpcCall rpcCall) throws IOException {
        StringWriter result = new StringWriter();
        JsonWriter out = new JsonWriter(result);
        out.startMap();
        out.writeAttribute("id", 0);
        out.writeAttribute("method", rpcCall.getMethod());

        out.startAttribute("params");
        out.startArray();
        out.writeLiteral(rpcCall.getArgs(), false);
        out.closeArray();

        out.closeMap();
        out.finishResponse();
        return result.toString();
    }
}
