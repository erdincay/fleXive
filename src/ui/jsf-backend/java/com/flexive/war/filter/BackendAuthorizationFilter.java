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
package com.flexive.war.filter;

import com.flexive.shared.FxContext;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;

import javax.servlet.*;
import java.io.IOException;

/**
 * Handles security checks for the backend administration - checks if the calling user has the role
 * {@link Role#BackendAccess}. Must be called
 * <i>after</i> the main FxFilter in the filter chain.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
public class BackendAuthorizationFilter implements Filter {
    private FilterConfig config;

    public void init(FilterConfig filterConfig) throws ServletException {
        this.config = filterConfig;
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // get URI without application context path
        final UserTicket ticket = FxContext.getUserTicket();
        if (ticket.isGuest()) {
            // not logged in at all - forward to login page
            servletRequest.getRequestDispatcher("/pub/login.jsf").forward(servletRequest, servletResponse);
        } else if (!ticket.isInRole(Role.BackendAccess)) {
            // logged in, but lacks role for backend access - show error page
            servletRequest.getRequestDispatcher("/pub/backendRestricted.jsf").forward(servletRequest, servletResponse);
        } else {
            // proceed
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    public void destroy() {
    }
}
