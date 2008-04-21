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
package com.flexive.war.webdav;

import com.flexive.war.webdav.catalina.ResourceAttributes;

import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet which adds support for WebDAV level 2.
 * <p/>
 * All the basic HTTP requests are handled by the DefaultServlet.<br>
 * WebDAV added the following methods to HTTP:<br>
 * PROPFIND - Used to retrieve properties, persisted as XML, from a resource. It is also overloaded to allow one to
 * retrieve the collection structure (a.k.a. directory hierarchy) of a remote system.<br>
 * PROPPATCH - Used to change and delete multiple properties on a resource in a single atomic act.<br>
 * MKCOL - Used to create collections (a.k.a. directory)<br>
 * COPY - Used to copy a resource from one URI to another<br>
 * MOVE - Used to move a resource from one URI to another<br>
 * LOCK - Used to put a lock on a resource, WebDAV supports both shared and exclusive locks<br>
 * UNLOCK - To remove a lock from a resource<br>
 * <br>
 * This is a work in progress class, the javadoc text of the webDav operations may not be
 * implemented correctly at this time, but describes what the operations should do in the
 * final implementation.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxWebDavServletBase extends DefaultServlet {

    private static final String METHOD_PROPFIND = "PROPFIND";
    private static final String METHOD_PROPPATCH = "PROPPATCH";
    private static final String METHOD_MKCOL = "MKCOL";
    private static final String METHOD_COPY = "COPY";
    private static final String METHOD_MOVE = "MOVE";
    private static final String METHOD_LOCK = "LOCK";
    private static final String METHOD_UNLOCK = "UNLOCK";
    private static final String METHOD_PUT = "PUT";
    private static final String METHOD_DELETE = "DELETE";
    private static ServletContext servletContext;

    /**
     * Initialize this servlets.
     */
    public void init() throws ServletException {
        if (debug > 0) {
            log("WebDavServletBase is initializing");
        }
        super.init();
    }

    /**
     * Method handling all webDav request
     *
     * @param request the reuqest
     * @param resp    the response
     * @throws ServletException if the service method fails
     * @throws IOException      if a IO Exception occures
     */
    protected void service(HttpServletRequest request, HttpServletResponse resp)
            throws ServletException, IOException {
        final String method = request.getMethod();
        try {
            // Write debug
            if (debug > 0) {
                log("[" + method + "] " + request.getRequestURI());
            }

            // Toggle by mothod
            if (method.equals("GET")) {
                FxWebDavServlet.getDavContext().serviceResource(request, resp, FxWebDavUtils.getDavPath(request));
            } else if (method.equals(METHOD_PROPFIND)) {
                new OperationPropFind(request, resp, readOnly).writeResponse();
            } else if (method.equals(METHOD_PROPPATCH)) {
                new OperationProppatch(request, resp, readOnly);
            } else if (method.equals(METHOD_MKCOL)) {
                new OperationMkcol(request, resp, readOnly).writeResponse();
            } else if (method.equals(METHOD_COPY)) {
                new OperationCopy(request, resp, readOnly).writeResponse();
            } else if (method.equals(METHOD_MOVE)) {
                new OperationMove(request, resp, readOnly).writeResponse();
            } else if (method.equals(METHOD_LOCK)) {
                new OperationLock(request, resp, readOnly).writeResponse();
            } else if (method.equals(METHOD_UNLOCK)) {
                new OperationUnlock(request, resp, readOnly).writeResponse();
            } else if (method.equals(METHOD_PUT)) {
                new OperationPut(request, resp, readOnly).writeResponse();
            } else if (method.equals(METHOD_DELETE)) {
                new OperationDelete(request, resp, readOnly).writeResponse();
            } else {
                super.service(request, resp); // DefaultServlet processing
            }
        } catch (Exception exc) {
            resp.setStatus(FxWebDavStatus.SC_INTERNAL_SERVER_ERROR);
            String message = this.getClass().getName() + ": [" + method + "] failed, msg=" + exc.getMessage();
            log(message);
            System.err.println(message);
        }
    }


    /**
     * Check if the conditions specified in the optional If headers are satisfied.
     *
     * @param request            The servlets request we are processing
     * @param response           The servlets response we are creating
     * @param resourceAttributes The resource information
     * @return boolean true if the resource meets all the specified conditions,
     *         and false if any of the conditions is not satisfied, in which case
     *         request processing is stopped
     */
    protected boolean checkIfHeaders(HttpServletRequest request, HttpServletResponse response,
                                     ResourceAttributes resourceAttributes) throws IOException {
        return super.checkIfHeaders(request, response, resourceAttributes);
    }


    /**
     * OPTIONS Method.
     *
     * @param req  The request
     * @param resp The response
     * @throws ServletException If an error occurs
     * @throws IOException      If an IO error occurs
     */
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.addHeader("DAV", "1,2");
        resp.addHeader("DAV", "<http://apache.org/dav/propset/fs/1>");
        resp.addHeader("MS-Author-Via", "DAV");
        resp.addHeader("Allow", determineMethodsAllowed(resources, req).toString());
        resp.addHeader("Content-Length", "0");
        resp.addHeader("content-type", "httpd/unix-directory");
    }


    /**
     * Determines the methods normally allowed for the resource.
     *
     * @param resources the resources
     * @param req       the request
     * @return the allowed methods
     */
    private StringBuffer determineMethodsAllowed(DirContext resources, HttpServletRequest req) {

        StringBuffer methodsAllowed = new StringBuffer();
        boolean exists = true;
        Object object = null;
        try {
            String path = getRelativePath(req);
            if (resources != null)
                object = resources.lookup(path);
            else
                exists = false;
        } catch (NamingException e) {
            exists = false;
        }

        if (!exists) {
            methodsAllowed.append("OPTIONS, MKCOL, PUT, LOCK");
            return methodsAllowed;
        }

        methodsAllowed.append("OPTIONS, GET, HEAD, POST, DELETE, TRACE");
        methodsAllowed.append(", PROPPATCH, COPY, MOVE, LOCK, UNLOCK");

        if (listings) {
            methodsAllowed.append(", PROPFIND");
        }

        if (!(object instanceof DirContext)) {
            methodsAllowed.append(", PUT");
        }

        return methodsAllowed;
    }


    public static String getMimeType(String filename) {
        return servletContext.getMimeType(filename);
    }

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        servletContext = getServletContext();
    }


}