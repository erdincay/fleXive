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
package com.flexive.war.filter;

import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.tree.FxTemplateInfo;
import com.flexive.war.FxRequest;
import com.flexive.war.webdav.FxWebDavUtils;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * HttpRequest wrapper to privide flexive specific informations
 */
public class FxRequestWrapper extends HttpServletRequestWrapper implements FxRequest {
    public static final String PATH_ADMIN = "adm";
    public static final String PATH_PUBLIC = "pub";
    public static final String PATH_SETUP = "config";
    public static final String PATH_CMS = "cms";
    private static final String PATH_TESTS = "tests";

    private final static String[] SERVLETS = {"cefileupload", "cefiledownload", "thumbnail", "faces"};
    private HttpServletRequest request = null;
    private long timestamp = -1;
    private int division = -1;
    private String requestUriNoContext = null;
    private FxSessionWrapper sessionWrapper;
    private String pageType;
    private boolean isWebdavRequest = false;
    private String realRequestUriNoContext = null;
    private FxTemplateInfo templateInfo = null;
    private long nodeId = -1;
    private long[] idChain;


    public long getNodeId() {
        return nodeId;
    }

    public void setNodeId(long nodeId) {
        this.nodeId = nodeId;
    }


    public FxTemplateInfo getTemplateInfo() {
        return templateInfo;
    }

    protected void setTemplateInfo(FxTemplateInfo treeNodeInfo) {
        this.templateInfo = treeNodeInfo;
    }

    protected void setIdChain(long chain[]) {
        this.idChain = chain;
    }

    public long[] getIdChain() {
        return this.idChain;
    }

    /**
     * {@inheritDoc} *
     */
    public boolean treeNodeIsActive(long nodeId) {
        if (idChain == null) return false;
        for (long id : idChain) {
            if (id == nodeId) return true;
        }
        return false;
    }

    public boolean isWebDavMethod() {
        return FxWebDavUtils.isWebDavMethod(this.getMethod());
    }

    @Override
    public HttpSession getSession() {
        return this.getSession(true);
    }

    @Override
    public HttpSession getSession(boolean b) {
        if (sessionWrapper == null) {
            sessionWrapper = new FxSessionWrapper(request.getSession(b));
        }
        return sessionWrapper;
    }


    public String getRealRequestUriNoContext() {
        return (realRequestUriNoContext == null) ? requestUriNoContext : realRequestUriNoContext;
    }

    private void setRealRequestUriNoContext(String value) {
        this.realRequestUriNoContext = value;
    }

    public boolean browserMayCache() {
        return (!(isAdminURL() || isPublicURL() || isSetupURL() || isDynamicContent())) ||
                // Exceptions:
                requestUriNoContext.startsWith("/adm/layout/") ||
                requestUriNoContext.startsWith("/adm/css/") ||
                requestUriNoContext.startsWith("/adm/js/") ||
                requestUriNoContext.startsWith("/adm/images/") ||
                requestUriNoContext.equals("/adm/blank.gif") ||
                requestUriNoContext.startsWith("/pub/layout/") ||
                requestUriNoContext.startsWith("/pub/css/") ||
                requestUriNoContext.startsWith("/pub/images/") ||
                requestUriNoContext.startsWith("/pub/js/") ||
                requestUriNoContext.startsWith("/images/") ||
                requestUriNoContext.startsWith("/css/") ||
                requestUriNoContext.startsWith("/js/");
    }

    /**
     * Constructor.
     *
     * @param httpServletRequest the servlet request
     * @param divisionId         the division id
     * @param isWebdav           true if this is a webdav request
     */
    protected FxRequestWrapper(final HttpServletRequest httpServletRequest, final int divisionId, boolean isWebdav) {
        super(httpServletRequest);
        // Setup internal variables
        this.timestamp = System.currentTimeMillis();
        this.request = httpServletRequest;
        this.division = divisionId;
        this.isWebdavRequest = isWebdav;
        this.requestUriNoContext = request.getRequestURI().substring(request.getContextPath().length());
        if (isWebdav) {
            // Cut away servlet path, eg. "/webdav/"
            this.requestUriNoContext = this.requestUriNoContext.substring(request.getServletPath().length());
        }
        int lastDotIdx = requestUriNoContext.lastIndexOf('.');
        this.pageType = (lastDotIdx == -1) ? "" : this.requestUriNoContext.substring(lastDotIdx + 1).toLowerCase();
    }

    /**
     * Returns true if this request is a webdav request.
     *
     * @return true if this request is a webdav request
     */
    public boolean isWebdavRequest() {
        return isWebdavRequest;
    }


    /**
     * Returns the page type (eg 'jsp', 'fx', 'gif', ..) always as lowercase string.
     *
     * @return the page type (eg 'jsp', 'fx', 'gif', ..) always as lowercase string.
     */
    public String getPageType() {
        return this.pageType;
    }

    /**
     * Tries to login a user.
     * <p/>
     * The next getUserTicket() call will return the new ticket.
     *
     * @param username the unique user name
     * @param password the password
     * @param takeOver the take over flag
     * @throws FxLoginFailedException  if the login failed
     * @throws FxAccountInUseException if take over was false and the account is in use
     */
    public void login(String username, String password, boolean takeOver) throws FxLoginFailedException,
            FxAccountInUseException {
        FxContext.get().login(username, password, takeOver);
    }

    /**
     * Logout of the current user.
     *
     * @throws FxLogoutFailedException
     */
    public void logout() throws FxLogoutFailedException {
        FxContext.get().logout();
    }

    /**
     * The underlying request.
     *
     * @return the underlying request
     */
    @Override
    public HttpServletRequest getRequest() {
        return request;
    }


    /**
     * The time the request was started at.
     *
     * @return the time the request was started at.
     */
    public long getRequestStartTime() {
        return this.timestamp;
    }

    /**
     * The ticke of the user.
     * <p/>
     *
     * @return The ticke of the user
     */
    public UserTicket getUserTicket() {
        return FxContext.get().getTicket();
    }

    /**
     * Returns true of the called URL is in the admin area.
     *
     * @return true of the called URL is in the admin area.
     */
    public boolean isAdminURL() {
        return requestUriNoContext.startsWith("/" + PATH_ADMIN + "/");
    }

    /**
     * Returns true if the called URL is the cactus servlets redirector.
     *
     * @return true if the called URL is the cactus servlets redirector.
     */
    public boolean isServletRedirectorURL() {
        return requestUriNoContext.endsWith("ServletRedirector");
    }

    public boolean isCmsUrl() {
        return requestUriNoContext.startsWith("/+") || requestUriNoContext.startsWith("/" + PATH_CMS + "/");
    }

    /**
     * Returns true of the called URL is in the public area.
     *
     * @return true of the called URL is in the public area.
     */
    public boolean isPublicURL() {
        // this must not use hard-coded public URLs.
        return !isAdminURL() && !isSetupURL();
        /*if (requestUriNoContext.startsWith("/" + PATH_PUBLIC + "/")) {
            return true;
        }
        for (String application: APPS) {
            if (request.getContextPath().equals("/" + application)) {
                return true;
            }
        }
        return false;*/
    }

    /**
     * Returns true of the called URL is in the public area.
     *
     * @return true of the called URL is in the public area.
     */
    public boolean isSetupURL() {
        return requestUriNoContext.startsWith("/" + PATH_SETUP + "/");
    }

    public boolean isTestURL() {
        return requestUriNoContext.startsWith("/" + PATH_TESTS + "/");
    }

    /**
     * Returns the division the request was started in.
     *
     * @return the division the request was started in
     */
    public int getDivisionId() {
        return division;
    }

    /**
     * Returns the request URI without the context.
     *
     * @return the request URI without the context.
     */
    public String getRequestURIWithoutContext() {
        return requestUriNoContext;
    }


    /**
     * Returns the operating system that gernerated the request.
     *
     * @return the operating system that generated the request
     */
    public OperatingSystem getOperatingSystem() {
        return new BrowserDetect(this.getRequest()).getOs();
    }

    /**
     * Returns the browser that gernerated the request.
     *
     * @return the browser that generated the request
     */
    public Browser getBrowser() {
        return new BrowserDetect(this.getRequest()).getBrowser();
    }

    /**
     * Returns true if the request is a dynamic content.
     *
     * @return true if the request is a dynamic content
     */
    public boolean isDynamicContent() {
        // list only resources that are known to be static, otherwise we'll run into troubles
        // when flexive is integrated in other applications/frameworks (e.g. Seam)
        return !("jpg".equals(pageType) || "css".equals(pageType) || "gif".equals(pageType) || "png".equals(pageType)
            || "js".equals(pageType));
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public void setAttribute(String string, Object object) {
        request.setAttribute(string, object);
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public Object getAttribute(String string) {
        return request.getAttribute(string);
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public void removeAttribute(String string) {
        request.removeAttribute(string);
    }

    /**
     * Includes a resource into the response.
     *
     * @param response the response
     * @param path     the path of the resource
     * @return true if the include was successfuly
     */
    public boolean include(final HttpServletResponse response, final String path) {
        if (path == null || path.length() == 0) return false;
        String oldURI = getRealRequestUriNoContext();
        try {
            this.setRealRequestUriNoContext(path);
            RequestDispatcher rd = getRequestDispatcher(path);
            if (rd != null) rd.include(this, response);
            return true;
        } catch (Exception exc) {
            System.out.println("[" + String.valueOf(this.getClass()) + "] include(" + path + ") failed:" + exc.getMessage());
            return false;
        } finally {
            this.setRealRequestUriNoContext(oldURI);
        }
    }


    /**
     * Forwards the request to a other resource on the server side (the browser is not notified of the forward)
     *
     * @param response the response
     * @param path     the path of the resource
     * @return true if the forward was successfuly
     */
    public boolean forward(final HttpServletResponse response, final String path) {
        if (path == null || path.length() == 0) return false;
        String oldURI = getRealRequestUriNoContext();
        try {
            this.setRealRequestUriNoContext(path);
            RequestDispatcher rd = getRequestDispatcher(path);
            if (rd != null) rd.forward(this, response);
            return true;
        } catch (Exception exc) {
            System.out.println("[" + String.valueOf(this.getClass()) + "] forward(" + path + ") failed:" + exc.getMessage());
            return false;
        } finally {
            this.setRealRequestUriNoContext(oldURI);
        }
    }

    @Override
    public String getCharacterEncoding() {
        return request.getCharacterEncoding();
    }


    @Override
    public Map getParameterMap() {
        return request.getParameterMap();
    }

    /**
     * Returns true if this request is handled by a servlet
     *
     * @return true if this request is handled by a servlet
     */
    public boolean isServlet() {
        if (isWebdavRequest) {
            return true;
        }
        for (String servlet : SERVLETS) {
            String comp = this.getRealRequestUriNoContext();
            if (comp.length() == servlet.length() + 1) {
                comp += "/";
            }
            if (comp.startsWith("/" + servlet + "/")) return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isContentEditor() {
        return getRealRequestUriNoContext().equals("/adm/content/contentEditor.jsf");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isInlineContentEditor() {
        return getRealRequestUriNoContext().equals("/adm/content/inlineContentEditor.jsf");
    }

}