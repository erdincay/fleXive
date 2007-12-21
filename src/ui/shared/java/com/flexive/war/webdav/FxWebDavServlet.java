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

import org.apache.commons.codec.binary.Base64;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public abstract class FxWebDavServlet extends FxWebDavServletBase implements Servlet {

    private static final String RE_AUTH = "@AUTH_RE@";
    private static String PREFIX = null;
    private static FxDavContext dc = null;

    /**
     * Tries to LOG a user in for webdav access.
     * <p/>
     * This function has to be fast since some dav clients are calling the login with every request,
     * therefor caching is recommended.
     *
     * @param request  the request
     * @param username the username
     * @param password the password
     * @return true if the login was successfull
     */
    protected abstract boolean login(HttpServletRequest request, String username, String password);

    /**
     * Chcks if a webdav user is currently logged in.
     *
     * @param request the request
     * @return true if a user is currently logged in an may access webdav
     */
    protected abstract boolean isLoggedIn(HttpServletRequest request);

    /**
     * Returns the dav context that should be used.
     *
     * @return the dav context
     */
    protected static FxDavContext getDavContext() {
        return dc;
    }

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        try {
            String className = getInitParameter("FxDavContext");
            Class cdc = Class.forName(className);
            dc = ((FxDavContext) cdc.newInstance()).getSingleton();
        } catch (Exception exc) {
            String error = "Failed to initialize the webdav context: " + exc.getMessage();
            if (debug > 0) System.err.println(error);
            throw new ServletException(error);
        }
    }

    /**
     * The service methode.
     *
     * @param req      the request
     * @param response the response
     * @throws ServletException if a error occurs
     * @throws IOException      if a IO error occurs
     */
    public void service(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {

        if (debug > 0) System.out.println("WebDAVCall debug level: " + debug);

        // Initialize the href prefix upon the first call
        if (PREFIX == null) {
            PREFIX = ((req.getContextPath() == null) ? "" : req.getContextPath()) +
                    ((req.getServletPath() == null) ? "" : req.getServletPath());
        }
        // TODO: gnome nautilus looses ssl session every few seconds and reauthorizes itself,
        // this means we need some kind of userticket caching.

        // Security
        final String AUTH = req.getHeader("Authorization");
        if (AUTH != null && AUTH.length() > 0) {
            String enc = AUTH.substring(6);
            String dec = new String(Base64.decodeBase64(enc.getBytes("UTF-8")), "UTF-8");
            int idx = dec.indexOf(":");
            String uid = dec.substring(0, idx);
            String pwd = dec.substring(idx + 1);
            if (!login(req, uid, pwd)) {
                sendAuthRequest(req, response);
                return;
            }
            response.addCookie(new Cookie("flexive-webdav", "true"));
        } else if (!isLoggedIn(req) /*&& req.isSecure()*/) {
            sendAuthRequest(req, response);
            return;
        }

        // All okay, service the request
        super.service(req, response);
    }

    /**
     * Helper function to send the authenticate request.
     *
     * @param request  the request
     * @param response the repsonse
     */
    private void sendAuthRequest(HttpServletRequest request, HttpServletResponse response) {
        request.getSession().removeAttribute(RE_AUTH);
        response.addHeader("WWW-Authenticate", "BASIC realm=\"[fleXive] powered WebDav\"");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    /**
     * Return JAXP document builder instance.
     *
     * @return the JAXP document builder instance
     * @throws ServletException
     */
    protected static DocumentBuilder getDocumentbuilder() throws ServletException {
        DocumentBuilder documentBuilder;
        DocumentBuilderFactory documentBuilderFactory;
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            documentBuilder = documentBuilderFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new ServletException("Unable to get the document builder: " + e.getMessage());
        }
        return documentBuilder;
    }

    /**
     * Rewrites the url.
     *
     * @param path the path to process
     * @return the final url
     */
    protected static String rewriteUrl(String path) {
        path = PREFIX + (path.startsWith("/") ? "" : "/") + path;
        if (path.endsWith("/")) {
            path = path.substring(1, path.length() - 1);
        }
        if (path.charAt(0) != '/') {
            path = "/" + path;
        }
        return FxWebDavServletBase.rewriteUrl(path);
    }

    /**
     * Converts the input stream to a string.
     *
     * @param re the request
     * @return the input stream as string
     */
    protected static String inputStreamToString(HttpServletRequest re) {
        InputStream is = null;
        OutputStream os = null;
        try {
            byte[] buffer = new byte[4096];
            is = re.getInputStream();
            os = new ByteArrayOutputStream();
            while (true) {
                int read = is.read(buffer);
                if (read == -1) {
                    break;
                }
                os.write(buffer, 0, read);
            }
            return os.toString();
        } catch (Exception exc) {
            return "";
        } finally {
            if (is != null) try {
                is.close();
            } catch (Exception e) {/*ignore*/}
            if (os != null) try {
                os.close();
            } catch (Exception e) {/*ignore*/}
        }
    }
}
