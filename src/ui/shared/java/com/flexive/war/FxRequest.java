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
package com.flexive.war;

import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.tree.FxTemplateInfo;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public interface FxRequest {

    // All recognized browsers
    enum Browser {
        KONQUEROR, IE, FIREFOX, MOZILLA, SAFARI, OPERA, UNKNOWN
    }

    // All recognized operating systems
    enum OperatingSystem {
        WINDOWS, MAC, LINUX, UNIX, UNKNOWN
    }

    /**
     * The underlying request.
     *
     * @return the underlying request
     */
    HttpServletRequest getRequest();

    /**
     * The time the request was started at.
     *
     * @return the time the request was started at.
     */
    long getRequestStartTime();

    /**
     * The ticke of the user.
     * <p/>
     *
     * @return The ticke of the user
     */
    UserTicket getUserTicket();

    /**
     * Returns the division the request was started in.
     *
     * @return the division the request was started in
     */
    int getDivisionId();

    public FxTemplateInfo getTemplateInfo();

    public long getNodeId();

    public long[] getIdChain();


    /**
     * Returns true if the given tree node id is part of the called url.
     *
     * @param nodeId the node to check for
     * @return true if the given tree node id is active
     */
    public boolean treeNodeIsActive(long nodeId);

    /**
     * Returns the operating system that generated the request.
     *
     * @return the operating system that generated the request
     */
    OperatingSystem getOperatingSystem();

    /**
     * Returns the browser that generated the request.
     *
     * @return the browser that generated the request
     */
    Browser getBrowser();

    /**
     * Returns the request URI without the context.
     *
     * @return the request URI without the context.
     */
    String getRequestURIWithoutContext();

    /**
     * Tries to login a user.
     * <p/>
     * The next getUserTicket() call will return the new ticket.
     *
     * @param username the unique user name
     * @param password the password
     * @param takeOver the take over flag
     * @throws com.flexive.shared.exceptions.FxLoginFailedException
     *          if the login failed
     * @throws com.flexive.shared.exceptions.FxAccountInUseException
     *          if take over was false and the account is in use
     */
    void login(String username, String password, boolean takeOver)
            throws FxLoginFailedException, FxAccountInUseException;


    /**
     * Returns true if this request references the content editor.
     *
     * @return true if this request references the content editor
     */
    public boolean isContentEditor();

    /**
     * Returns true if this request references the inline content editor.
     *
     * @return true if this request references the inline content editor.
     */
    public boolean isInlineContentEditor();

    public String getCharacterEncoding();

    public Map getParameterMap();


    /**
     * Logout of the current user.
     *
     * @throws com.flexive.shared.exceptions.FxLogoutFailedException
     *          -
     */
    void logout() throws FxLogoutFailedException;

    public String getRequestURI();

    public StringBuffer getRequestURL();

    boolean isWebDavMethod();

    boolean isDynamicContent();

    public void setAttribute(String string, Object object);

    public Object getAttribute(String string);

    public void removeAttribute(String string);

    String getServletPath();

    javax.servlet.http.HttpSession getSession(boolean b);

    javax.servlet.http.HttpSession getSession();

    String getContextPath();

    // --
    String getAuthType();

    String getMethod();

    String getPathInfo();

    String getQueryString();

    String getRemoteUser();  // TODO

    boolean isUserInRole(String string); // TODO

    java.security.Principal getUserPrincipal(); // TODO

    String getRequestedSessionId();

    boolean isRequestedSessionIdValid();

    boolean isRequestedSessionIdFromCookie();

    boolean isRequestedSessionIdFromURL();

    boolean isRequestedSessionIdFromUrl();

    String getRemoteHost();
}