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
package com.flexive.war.webdav;

import com.flexive.shared.FxSharedUtils;
import com.flexive.war.webdav.catalina.XMLWriter;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * PROPFIND - Used to retrieve properties, persisted as XML, from a resource.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class OperationPropFind extends Operation {


    // Find types
    private static enum FIND_BY {
        PROPERTY, PROPERTY_NAMES, ALL_PROP
    }

    // Default and infinity depth, set to 3 to limit tree brwosing
    static final int INFINITY = 3;

    private int depth;
    private String properties[];
    private FIND_BY type = null;


    /**
     * Constructor.
     *
     * @param req  the request
     * @param resp the response
     * @throws ServletException if a exception occurs
     */
    public OperationPropFind(HttpServletRequest req, HttpServletResponse resp, boolean readonly) throws ServletException {

        super(req, resp, readonly);

        // Propfind depth
        String depthStr = req.getHeader("Depth");
        depth = (depthStr == null || depthStr.equalsIgnoreCase("infinity")) ? INFINITY : Integer.parseInt(depthStr);
        Node propNode = determineType();
        properties = getProperties(propNode);

        if (debug) System.out.println(this.toString());
    }


    /**
     * Writes the propfind response to the client
     *
     * @throws IOException
     */
    public void writeResponse() throws IOException {
        if (debug) System.out.println("++ Requesting:" + path);
        if (path.endsWith("/index.jsp")) {
            path = path.substring(0, path.lastIndexOf("/index.jsp"));
            if (debug) System.out.println("Adjusted path to [" + path + "]");
        }
        FxDavEntry res = null;
        try {
            res = FxWebDavServlet.getDavContext().getResource(this.request, path);
        } catch (Exception exc) {
            if (debug) System.err.println("Failed to lookup '" + path + "': " + exc.getMessage());
        }
        if (res == null) { //check if application server 'magically' added a welcome file
            if ("index.jsp".equals(path)) {
                path = "";
            } else if (path != null && path.endsWith("/index.jsp")) {
                path = path.substring(0, path.lastIndexOf("/index.jsp"));
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, path);
                return;
            }
            //try again
            writeResponse();
            return;
        }
        // Start to write the response
        response.setStatus(FxWebDavStatus.SC_MULTI_STATUS);
        response.setContentType("text/xml; charset=UTF-8");
        // Create multistatus object
        XMLWriter generatedXML = new XMLWriter(response.getWriter());
        generatedXML.writeXMLHeader();
        generatedXML.writeElement(null, "multistatus" + generateNamespaceDeclarations(), XMLWriter.OPENING);
        handleElement(generatedXML, path, res, 1);
        generatedXML.writeElement(null, "multistatus", XMLWriter.CLOSING);
//        if (debug) System.out.println(generatedXML.toString());
        generatedXML.sendData();
    }

    /**
     * @param generatedXML
     * @param dr
     */
    private void handleElement(XMLWriter generatedXML, String path, FxDavEntry dr, final int curDepth) {
//        if( debug ) System.out.println("Processing [" + dr.toString() + "] depth: "+curDepth+"/"+depth);
        switch (type) {
            case ALL_PROP:
            case PROPERTY:
                dr.writeProperties(generatedXML, path, properties);
                break;
            case PROPERTY_NAMES:
                dr.writePropertyNames(generatedXML, path);
                break;
        }
        // Do recursion if needed
        if (dr.isCollection() && curDepth <= depth) {
            FxDavEntry[] children = FxWebDavServlet.getDavContext().getChildren(this.request, path);
            if (children != null) {
                for (FxDavEntry child : children) {
                    handleElement(generatedXML, path + "/" + child.getDisplayname(), child, curDepth + 1);
                }
            }
        }
    }


    /**
     * Generate the namespace declarations.
     */
    private String generateNamespaceDeclarations() {
        return " xmlns=\"" + DEFAULT_NAMESPACE + "\"";
    }


    /**
     * Get the requested properties.
     *
     * @param propNode
     * @return the requested properties
     */
    private String[] getProperties(final Node propNode) throws ServletException {
        if (type == FIND_BY.PROPERTY) {
            Vector<String> props = new Vector<String>();
            NodeList childList = propNode.getChildNodes();
            for (int i = 0; i < childList.getLength(); i++) {
                Node currentNode = childList.item(i);
                switch (currentNode.getNodeType()) {
                    case Node.TEXT_NODE:
                        break;
                    case Node.ELEMENT_NODE:
                        String nodeName = currentNode.getNodeName();
                        if (nodeName.indexOf(':') != -1) {
                            props.addElement(nodeName.substring(nodeName.indexOf(':') + 1));
                        } else {
                            props.addElement(nodeName);
                        }
                        break;
                    default:
                        throw new ServletException("Unknown element:" + currentNode.getNodeName());
                }
            }
            return props.toArray(new String[props.size()]);
        }
        return FxDavResource.ALL_PROPS;
    }


    /**
     * Determine the propfind type.
     *
     * @return the node if type is PROPERTY.
     */
    private Node determineType() {

        final String srequest = FxWebDavServlet.inputStreamToString(request);
        if (srequest.length() > 0) {
            ByteArrayInputStream bais = null;
            try {
                DocumentBuilder documentBuilder = FxWebDavServlet.getDocumentbuilder();
                bais = new ByteArrayInputStream(FxSharedUtils.getBytes(srequest));
                //Document document = documentBuilder.parse(new InputSource(request.getInputStream()));
                Document document = documentBuilder.parse(new InputSource(bais));
                Element rootElement = document.getDocumentElement();
                NodeList childList = rootElement.getChildNodes();
                for (int i = 0; i < childList.getLength(); i++) {
                    Node currentNode = childList.item(i);
                    switch (currentNode.getNodeType()) {
                        case Node.TEXT_NODE:
                            break;
                        case Node.ELEMENT_NODE:
                            if (currentNode.getNodeName().endsWith("prop")) {
                                type = FIND_BY.PROPERTY;
                                properties = null;
                                return currentNode;
                            } else if (currentNode.getNodeName().endsWith("propname")) {
                                type = FIND_BY.PROPERTY_NAMES;
                                properties = null;
                                return null;
                            } else if (currentNode.getNodeName().endsWith("allprop")) {
                                type = FIND_BY.ALL_PROP;
                                properties = FxDavResource.ALL_PROPS;
                                return null;
                            } else {
                                // nothing
                            }
                        default:
                            throw new ServletException("Unknown node: " + currentNode.getNodeName());
                    }
                }
                return null;
            } catch (Exception exc) {
                /* ignore and use fallback */
            } finally {
                if (bais != null) {
                    try {
                        bais.close();
                    } catch (Exception e) {/*ignore*/}
                }
            }
        }

        // Fallback
        type = FIND_BY.ALL_PROP;
        properties = FxDavResource.ALL_PROPS;
        return null;

    }

    /**
     * String representation of the object.
     *
     * @return a string representation of the object
     */
    public String toString() {
        return "PROPFIND: path:" + path + ";depth:" + depth + ";type:" + type;
    }

}
