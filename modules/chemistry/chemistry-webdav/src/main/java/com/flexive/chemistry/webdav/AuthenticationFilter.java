/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.chemistry.webdav;

import com.bradmcevoy.http.*;
import org.apache.chemistry.Connection;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.IOException;

/**
 * Milton filter that authenticates the user before the resources are resolved and provides the
 * repository connection for the duration of the request.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AuthenticationFilter implements Filter {
    private static final Log LOG = LogFactory.getLog(AuthenticationFilter.class);

    private static final ThreadLocal<Connection> repositoryConnection = new ThreadLocal<Connection>();

    private static final String AUTH_SESSION_KEY = "AuthenticationFilter_Auth";

    /**
     * {@inheritDoc}
     */
    public void process(FilterChain chain, Request request, Response response) {

        final ChemistryResourceFactory factory = (ChemistryResourceFactory) chain.getHttpManager().getResourceFactory();

        try {
            initConnection(request, factory);
        } catch (LoginException e) {
            if (factory.requireAuthentication()) {
                // send unauthorized response status, force user authentication
                response.setStatus(Response.Status.SC_UNAUTHORIZED);
                response.setAuthenticateHeader(ObjectResource.SECURITY_REALM);

                // send some content, KDE's Dolphin thinks the resource doesn't exist otherwise
                // TODO: check if this problem persists with Milton's handling of unauthorized responses
                try {
                    final byte[] bytes = "Unauthorized\n\n".getBytes("UTF-8");
                    response.getOutputStream().write(bytes);
                    response.setContentLengthHeader((long) bytes.length);
                    response.getOutputStream().close();
                } catch (IOException e1) {
                    throw new RuntimeException(e);
                }

                // abort processing
                return;
            }
        }

        // either user is authenticated, or "guest" browsing is allowed
        try {
            chain.process(request, response);
        } finally {
            try {
                repositoryConnection.get().close();
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to close repository connection: " + e.getMessage(), e);
                }
            }
            repositoryConnection.set(null);
        }
        
    }

    /**
     * Initialize the connection using the authentication credentials from the current request
     * or from the session cache.
     *
     * @param request   the current request
     * @param factory   the resource factory
     */
    protected void initConnection(Request request, ChemistryResourceFactory factory) throws LoginException {
        // initialize connection, use authorization from HTTP headers or session cache
        final Auth auth = getRequestAuthorization(request);
        if (auth == null && factory.requireAuthentication()) {
            throw new LoginException("Login required, guest access disabled.");
        }
        repositoryConnection.set(
                factory.createConnection(request, auth)
        );
    }

    protected Auth getRequestAuthorization(Request request) {
        Auth auth = request.getAuthorization();
        if (auth == null) {
            // try authentification data stored in session
            if (request instanceof ServletRequest) {
                auth = (Auth) ((ServletRequest) request).getSession().getAttribute(AUTH_SESSION_KEY);
            }
        } else {
            // auth from request, keep in session
            if (request instanceof ServletRequest) {
                ((ServletRequest) request).getSession().setAttribute(AUTH_SESSION_KEY, auth);
            }
        }
        return auth;
    }

    /**
     * Returns the connection associated with the current WebDAV request.
     *
     * @return  the connection associated with the current WebDAV request.
     */
    public static Connection getConnection() {
        final Connection conn = repositoryConnection.get();
        if (conn == null) {
            throw new IllegalStateException("No connection associated with this request. Is the AuthenticationFilter enabled?");
        }
        return conn;
    }

    /**
     * Override the connection (useful for testcases).
     *
     * @param conn  the conn to be used for subsequent calls
     */
    public static void setConnection(Connection conn) {
        if (repositoryConnection.get() != null && repositoryConnection.get() != conn) {
            repositoryConnection.get().close();
        }
        repositoryConnection.set(conn);
    }
}
