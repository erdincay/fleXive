/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.war.filter;

import com.flexive.shared.FxContext;
import com.flexive.shared.security.UserTicket;
import com.flexive.war.FxRequest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;

/**
 * HttpRequest wrapper to provide flexive specific informations
 */
public class FxRequestWrapper extends HttpServletRequestWrapper implements FxRequest {
    private static final Log LOG = LogFactory.getLog(FxRequestWrapper.class);

    private HttpServletRequest request = null;
    private long timestamp = -1;
    private int division = -1;
    private FxSessionWrapper sessionWrapper;
    private String pageType;
    private BrowserDetect browserDetect;

    /**
     * {@inheritDoc} *
     */
    @Override
    public HttpSession getSession() {
        return this.getSession(true);
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public HttpSession getSession(boolean b) {
        if (sessionWrapper == null) {
            sessionWrapper = new FxSessionWrapper(request.getSession(b));
        }
        return sessionWrapper;
    }


    /**
     * @return  the request URI after forwarding
     *
     * @deprecated use {@link #getRequestURIWithoutContext} since the semantics lead to confusing errors.
     * As a rule of thumb, only the final URL after forwarding should be used to allow arbitrary rewriting
     * of external URLs, as is the case when calling {@link #getRequestURI()}
     * or {@link #getRequestURIWithoutContext()}.
     */
    public String getRealRequestUriNoContext() {
        return getRequestURIWithoutContext();
    }

    private void setRealRequestUriNoContext(String value) {
        // nop - remove when getRealRequestUriNoContext is removed
    }

    /**
     * Constructor.
     *
     * @param httpServletRequest the servlet request
     * @param divisionId         the division id
     */
    public FxRequestWrapper(final HttpServletRequest httpServletRequest, final int divisionId) {
        super(httpServletRequest);
        // Setup internal variables
        this.timestamp = System.currentTimeMillis();
        this.request = httpServletRequest;
        this.division = divisionId;
        final int lastDotIdx = httpServletRequest.getRequestURI().lastIndexOf('.');
        this.pageType = (lastDotIdx == -1) ? "" : httpServletRequest.getRequestURI().substring(lastDotIdx + 1).toLowerCase();
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
        return FxContext.getUserTicket();
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
        return request.getRequestURI().substring(request.getContextPath().length());
    }

    private BrowserDetect getBrowserDetect() {
        if (browserDetect == null) {
            browserDetect = new BrowserDetect(this.getRequest());
        }
        return browserDetect;
    }


    /**
     * Returns the operating system that gernerated the request.
     *
     * @return the operating system that generated the request
     */
    public OperatingSystem getOperatingSystem() {
        return getBrowserDetect().getOs();
    }

    /**
     * Returns the browser that generated the request.
     *
     * @return the browser that generated the request
     */
    public Browser getBrowser() {
        return getBrowserDetect().getBrowser();
    }

    /**
     * {@inheritDoc}
     */
    public double getBrowserVersion() {
        return getBrowserDetect().getBrowserVersion();
    }

    /**
     * {@inheritDoc} *
     */
    public boolean isDynamicContent() {
        // list only resources that are known to be static, otherwise we'll run into troubles
        // when flexive is integrated in other applications/frameworks (e.g. Seam)
        return !("jpg".equals(pageType) || "css".equals(pageType) || "gif".equals(pageType) || "png".equals(pageType)
            || "js".equals(pageType) || "ico".equals(pageType)
            // don't match JSF2 resource requests, even if they have a .html or .xhtml suffix
            || request.getRequestURI().indexOf("javax.faces.resource") != -1);
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
     * @deprecated  this does not work well with generic servlet applications that do not know about FxRequestWrapper
     */
    public boolean include(final HttpServletResponse response, final String path) {
        if (path == null || path.length() == 0) return false;
        String oldURI = getRequestURIWithoutContext();
        try {
            this.setRealRequestUriNoContext(path);
            RequestDispatcher rd = getRequestDispatcher(path);
            if (rd != null) rd.include(this, response);
            return true;
        } catch (Exception exc) {
            if (LOG.isErrorEnabled()) {
                LOG.error("[" + String.valueOf(this.getClass()) + "] include(" + path + ") failed:" + exc.getMessage());
            }
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
     * @deprecated  this does not work well with generic servlet applications that do not know about FxRequestWrapper
     */
    public boolean forward(final HttpServletResponse response, final String path) {
        if (path == null || path.length() == 0) return false;
        String oldURI = getRequestURI();
        try {
            this.setRealRequestUriNoContext(path);
            RequestDispatcher rd = getRequestDispatcher(path);
            if (rd != null) rd.forward(this, response);
            return true;
        } catch (Exception exc) {
            if (LOG.isErrorEnabled()) {
                LOG.error("[" + String.valueOf(this.getClass()) + "] forward(" + path + ") failed:" + exc.getMessage());
            }
            return false;
        } finally {
            this.setRealRequestUriNoContext(oldURI);
        }
    }

    /**
     * {@inheritDoc} *
     */
    @Override
    public String getCharacterEncoding() {
        return request.getCharacterEncoding();
    }


    /**
     * {@inheritDoc} *
     */
    @Override
    public Map getParameterMap() {
        return request.getParameterMap();
    }

}