package com.flexive.asubmission.war;

import com.flexive.shared.FxContext;
import com.flexive.shared.CacheAdmin;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Filter for authentication check
 *
 * @author Hans Bacher (hans.bacher@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SecurityFilter implements Filter {

    private FilterConfig filterConfig;

    /**
     * Init function
     *
     * @param config -
     * @throws javax.servlet.ServletException
     */
    public void init(FilterConfig config) throws ServletException {
        this.filterConfig = config;
    }

    /**
     * @param servletRequest -
     * @param servletResponse -
     * @param chain -
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws ServletException, IOException {

        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;
        final String contextPath = request.getContextPath();

        CacheAdmin.getEnvironment(); // This will execute the run once script of a fresh installation. It creates the users.

        // Check if the current user is a guest user
        if (FxContext.get().getTicket().isGuest() && request.getRequestURI().startsWith(contextPath + "/protected.area/")) {
            response.sendRedirect(contextPath + "/login.xhtml");
        } else {
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    /**
     * Destroy function
     */
    public void destroy() {
        this.filterConfig = null;
    }
}