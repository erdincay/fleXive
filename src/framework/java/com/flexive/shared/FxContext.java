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
package com.flexive.shared;

import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.security.UserTicket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.net.URLDecoder;
import java.util.Locale;

/**
 * The [fleXive] context - user session specific data like UserTickets, etc.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxContext implements Serializable {
    private static final long serialVersionUID = -54895743895893486L;

    /**
     * Global division id
     */
    public final static int DIV_GLOBAL_CONFIGURATION = 0;
    /**
     * Undefined division id
     */
    public final static int DIV_UNDEFINED = -1;
    /**
     * Session key set if the user successfully logged into the admin area
     */
    public static final String ADMIN_AUTHENTICATED = "$flexive_admin_auth$";

    private static final transient Log LOG = LogFactory.getLog(FxContext.class);
    private static ThreadLocal<FxContext> info = new ThreadLocal<FxContext>() {
    };

    private final String sessionID;
    private final String requestURI;
    private final String remoteHost;
    private final boolean webDAV;
    private final String serverName;
    private final int serverPort;

    private boolean treeWasModified;
    private String contextPath;
    private String requestUriNoContext;
    private boolean globalAuthenticated;
    private int division;
    private int runAsSystem;
    private UserTicket ticket;
    private long nodeId = -1;

    protected static UserTicket getLastUserTicket(HttpSession session) {
        return (UserTicket) session.getAttribute("LAST_USERTICKET");
    }

    protected static void setLastUserTicket(HttpSession session, UserTicket lastUserTicket) {
        session.setAttribute("LAST_USERTICKET", lastUserTicket);
    }

    public UserTicket getTicket() {
        if (getRunAsSystem()) {
            return ticket.cloneAsGlobalSupervisor();
        }
        return ticket;
    }

    public void setTicket(UserTicket ticket) {
        this.ticket = ticket;
    }

    /**
     * Get the current users active tree node id  (CMS extension)
     *
     * @return the current users active tree node id
     */
    public long getNodeId() {
        return nodeId;
    }

    /**
     * Set the current users active tree node id (CMS extension)
     *
     * @param nodeId active tree node id
     */
    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * Returns true if the tree was modified within this thread by the
     * user belonging to this thread.
     *
     * @return true if the tree was modified
     */
    public boolean getTreeWasModified() {
        return treeWasModified;
    }

    /**
     * Flag the tree as modified
     */
    public void setTreeWasModified() {
        this.treeWasModified = true;
    }

    /**
     * Get the current users preferred locale (based on his preferred language)
     *
     * @return the current users preferred locale (based on his preferred language)
     */
    public Locale getLocale() {
        return ticket.getLanguage().getLocale();
    }

    /**
     * Get the current users preferred language
     *
     * @return the current users preferred language
     */
    public FxLanguage getLanguage() {
        return ticket.getLanguage();
    }

    /**
     * Tries to login a user.
     * <p/>
     * The next getUserTicket() call will return the new ticket.
     *
     * @param loginname the unique user name
     * @param password  the password
     * @param takeOver  the take over flag
     * @throws FxLoginFailedException  if the login failed
     * @throws FxAccountInUseException if take over was false and the account is in use
     */
    public void login(String loginname, String password, boolean takeOver) throws FxLoginFailedException,
            FxAccountInUseException {
        // Anything to do at all?
        if (ticket != null && ticket.getLoginName().equals(loginname)) {
            return;
        }
        // Try the login
        AccountEngine acc = EJBLookup.getAccountEngine();
        acc.login(loginname, password, takeOver);
        setTicket(acc.getUserTicket());
    }


    /**
     * Logout of the current user.
     *
     * @throws FxLogoutFailedException if the function fails
     */
    public void logout() throws FxLogoutFailedException {
        AccountEngine acc = EJBLookup.getAccountEngine();
        acc.logout();
        setTicket(acc.getUserTicket());
    }

    /**
     * Override the used ticket.
     * Please do not use this method! Its only purpose is to feed FxContext with a
     * USerTicket when no user is logged in - ie during system startup
     *
     * @param ticket ticket to override with
     */
    public void overrideTicket(UserTicket ticket) {
        if (!getRunAsSystem()) {
            setTicket(ticket);
        }
    }

    /**
     * Constructor
     *
     * @param request    the request
     * @param divisionId the division
     * @param isWebdav   true if this is an webdav request
     */
    private FxContext(HttpServletRequest request, int divisionId, boolean isWebdav) {
        this.sessionID = request.getSession().getId();
        this.requestURI = request.getRequestURI();
        this.contextPath = request.getContextPath();
        this.serverName = request.getServerName();
        this.serverPort = request.getServerPort();
        this.requestUriNoContext = request.getRequestURI().substring(request.getContextPath().length());
        this.webDAV = isWebdav;
        if (this.webDAV) {
            // Cut away servlet path, eg. "/webdav/"
            this.requestUriNoContext = this.requestUriNoContext.substring(request.getServletPath().length());
        }
        this.globalAuthenticated = request.getSession().getAttribute(ADMIN_AUTHENTICATED) != null;
        this.remoteHost = request.getRemoteHost();
        this.division = divisionId;
    }

    /**
     * Gets the user ticket from the ejb layer, and stores it in the session as 'last used user ticket'
     *
     * @param session the session
     * @return the user ticket
     */
    private static UserTicket getTicketFromEJB(final HttpSession session) {
        try {
            UserTicket ticket = EJBLookup.getAccountEngine().getUserTicket();
            setLastUserTicket(session, ticket);
            return ticket;
        } catch (Exception e) {
            System.err.println("Failed: " + e.getMessage() + "<br>");
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Constructor
     */
    private FxContext() {
        sessionID = "EJB_" + System.currentTimeMillis();
        requestURI = "";
        division = -1;
        remoteHost = "127.0.0.1 (SYSTEM)";
        webDAV = false;
        serverName = "localhost";
        serverPort = 80;
    }

    /**
     * Returns true if the division is the global configuration division.
     *
     * @return true if the division is the global configuration division
     */
    public boolean isGlobalConfigDivision() {
        return division == DivisionData.DIVISION_GLOBAL;
    }

    /**
     * Return true if the current context runs in the test division.
     *
     * @return true if the current context runs in the test division.
     */
    public boolean isTestDivision() {
        return division == DivisionData.DIVISION_TEST;
    }

    /**
     * Returns the id of the division.
     * <p/>
     *
     * @return the id of the division.
     */
    public int getDivisionId() {
        return this.division;
    }

    /**
     * Changes the divison ID. Use with care!
     * (Currently needed for embedded container testing.)
     *
     * @param division the division id
     */
    public void setDivisionId(int division) {
        this.division = division;
    }

    /**
     * Changes the context path (Currently needed for embedded container testing.)
     *
     * @param contextPath the context path
     */
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    /**
     * Runs all further calls as SYSTEM user with full permissions until stopRunAsSystem
     * gets called. Multiple calls to this function get stacked and the runAsSystem
     * flag is only removed when the stack is empty.
     */
    public void runAsSystem() {
        runAsSystem++;
    }

    /**
     * Removes one runeAsSystem flag from the stack.
     */
    public void stopRunAsSystem() {
        if (runAsSystem <= 0) {
            LOG.fatal("stopRunAsSystem called with no system flag on the stack");
        } else
            runAsSystem--;
    }

    /**
     * Returns true if all calls are done without permission checks for the time beeing.
     *
     * @return true if all calls are done without permission checks for the time beeing
     */
    public boolean getRunAsSystem() {
        return runAsSystem != 0;
    }


    /**
     * Returns the session id, which is unique at call time
     *
     * @return the session's id
     */
    public String getSessionId() {
        return sessionID;
    }

    /**
     * Returns the request URI.
     * <p/>
     * This URI contains the context path, use getRelativeRequestURI() to retrieve the path
     * without it.
     *
     * @return the request URI
     */
    public String getRequestURI() {
        return requestURI;
    }

    /**
     * Returns the decoded relative request URI.
     * <p/>
     * This function is the same as calling getRelativeRequestURI(true).
     *
     * @return the URI without its context path
     */
    public String getRelativeRequestURI() {
        return getRelativeRequestURI(true);
    }


    /**
     * Returns the relative request URI.
     *
     * @param decode if set to true the URI will be decoded (eg "%20" to a space), using UTF-8
     * @return the URI without its context path
     */
    @SuppressWarnings("deprecation")
    public String getRelativeRequestURI(boolean decode) {
        String result = requestURI.substring(contextPath.length());
        if (decode) {
            try {
                result = URLDecoder.decode(result, "UTF-8");
            } catch (Throwable t) {
                System.out.print("Failed to decode the URI using UTF-8, using fallback decoding. msg=" + t.getMessage());
                result = URLDecoder.decode(result);
            }
        }
        return result;
    }

    /**
     * Returns the name of the server handling this request, e.g. www.flexive.com
     *
     * @return the name of the server handling this request, e.g. www.flexive.com
     */
    public String getServerName() {
        return serverName;
    }

    /**
     * Returns the port of the server handling this request, e.g. 80
     *
     * @return the port of the server handling this request, e.g. 80
     */
    public int getServerPort() {
        return serverPort;
    }

    /**
     * Returns the full server URL including the port for this request, e.g. http://www.flexive.com:8080
     *
     * @return the full server URL including the port for this request, e.g. http://www.flexive.com:8080
     */
    public String getServer() {
        return "http://" + serverName + (serverPort != 80 ? ":" + serverPort : "");
    }

    /**
     * Returns the calling remote host.
     *
     * @return the remote host.
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    /**
     * Returns the id of the appication the request was made in.
     * <p/>
     * In webapps the application id equals the context path
     *
     * @return the id of the appication the request was made in.
     */
    public String getApplicationId() {
        return (contextPath != null && contextPath.length() > 0 && contextPath.charAt(0) == '/') ?
                contextPath.substring(1) : contextPath;
    }

    /**
     * Returns the absolute path for the given resource (i.e. the application name + the path).
     *
     * @param path the path of the resource (e.g. /pub/css/demo.css)
     * @return the absolute path for the given resource
     */
    public String getAbsolutePath(String path) {
        return "/" + getApplicationId() + path;
    }


    /**
     * Gets the session information for the running thread
     *
     * @return the session information for the running thread
     */
    public static FxContext get() {
        FxContext result = info.get();
        if (result == null) {
            result = new FxContext();
            info.set(result);
        }
        return result;
    }

    /**
     * Reload the UserTicket, needed i.e. when language settings change
     */
    public void _reloadUserTicket() {
        setTicket(EJBLookup.getAccountEngine().getUserTicket());
    }

    /**
     * Returns true if this request is triggered by a webdav operation.
     *
     * @return true if this request is triggered by a webdav operation
     */
    public boolean isWebDAV() {
        return webDAV;
    }

    /**
     * Return true if the user successfully authenticated for the
     * global configuration area
     *
     * @return true if the user successfully authenticated for the global configuration
     */
    public boolean isGlobalAuthenticated() {
        return globalAuthenticated;
    }


    /**
     * Authorize the user for the global configuration area
     *
     * @param globalAuthenticated true if the user should be authorized for the global configuration
     */
    public void setGlobalAuthenticated(boolean globalAuthenticated) {
        this.globalAuthenticated = globalAuthenticated;
    }

    /**
     * Returns the request URI without its context.
     *
     * @return the request URI without its context.
     */
    public String getRequestUriNoContext() {
        return requestUriNoContext;
    }

    /**
     * Return the context path of this request.
     *
     * @return the context path of this request.
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Stores the needed informations about the sessions.
     *
     * @param request        the users request
     * @param dynamicContent is the content dynamic?
     * @param divisionId     the division id
     * @param isWebdav       true if this is an webdav request
     * @return FxContext
     */
    public static FxContext storeInfos(HttpServletRequest request, boolean dynamicContent, int divisionId, boolean isWebdav) {
        FxContext si = new FxContext(request, divisionId, isWebdav);
        // Set basic informations needed for the user ticket retrieval
        info.set(si);
        // Do user ticket retrieval and store it in the threadlocal
        final HttpSession session = request.getSession();
        if (dynamicContent || isWebdav) {
            UserTicket last = getLastUserTicket(session);
            // Always determine the current user ticket for dynamic pages and webdav requests.
            // This takes about 1 x 5ms for every request on a development machine
            si.setTicket(getTicketFromEJB(session));
            if (si.ticket.isGuest()) {
                try {
                    if (last == null)
                        si.ticket.overrideLanguage(EJBLookup.getLanguageEngine().load(request.getLocale().getLanguage()));
                    else
                        si.ticket.overrideLanguage(last.getLanguage());
                } catch (FxApplicationException e) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Failed to use request locale from browser: " + e.getMessage(), e);
                    }
                }
            }
        } else {
            // For static content like images we use the last user ticket stored in the session
            // to speed up the request.
            si.setTicket(getLastUserTicket(session));
            if (si.ticket != null) {
                if (si.ticket.isGuest() && (si.ticket.getACLAssignments() == null || si.ticket.getACLAssignments().length == 0)) {
                    //reload from EJB layer if we have a guest ticket with no ACL assignments
                    //this can happen during initial loading
                    si.setTicket(getTicketFromEJB(session));
                }
            }
            if (si.ticket == null) {
                si.setTicket(getTicketFromEJB(session));
            }
        }
        info.set(si);
        return si;
    }

    /**
     * Performs a cleanup of the stored informations.
     */
    public static void cleanup() {
        if (info.get() != null) {
            info.remove();
        }
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    public String toString() {
        return this.getClass() + "[sessionId:" + sessionID + ";requestUri:" + requestURI + "]";
    }

}
