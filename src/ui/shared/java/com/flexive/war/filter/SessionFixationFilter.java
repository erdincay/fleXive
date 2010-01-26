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
package com.flexive.war.filter;

import com.google.common.collect.Maps;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

/**
 * <p>Filter to prevent <a href="http://en.wikipedia.org/wiki/Session_fixation">session fixation</a> attacks.</p>
 *
 * <p>When a {@code POST} to the mapped URL(s) is detected, the session is invalidated and the old session
 * attributes are moved to the new session (especially needed for JSF's server-side view state saving).
 * </p>
 *
 * <p>
 * This filter must be configured <strong>before</strong> {@link FxFilter}, otherwise the session wrapping
 * will collide with this filter.
 * </p>
 *
 * <h3>Configuration</h3>
 *
 * The filter is mapped on the actual login URL(s). No other configuration options are available.
 *
 * <h3>Notes</h3>
 *
 * On JBoss/Tomcat, you must set {@code emptySessionPath="false"} in the web container's server.xml, otherwise
 * the new session ID will be identical to the old one.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SessionFixationFilter implements Filter {
    private static final Log LOG = LogFactory.getLog(SessionFixationFilter.class);

    public void init(FilterConfig filterConfig) throws ServletException {
        // nop
    }

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            final HttpServletRequest request = (HttpServletRequest) servletRequest;

            if ("POST".equals(request.getMethod())) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("SessionFixationFilter: creating new session before login");
                }
                
                // remember old attributes (esp. JSF viewstate)
                final Map<String, Object> attributes = getAttributes(request.getSession());

                // create new session ID
                request.getSession().invalidate();
                request.getSession(true);

                // integrate old values
                setAttributes(request.getSession(false), attributes);
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private void setAttributes(HttpSession session, Map<String, Object> attributes) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            session.setAttribute(entry.getKey(), entry.getValue());
        }
    }

    private Map<String, Object> getAttributes(HttpSession session) {
        final Map<String, Object> attributes = Maps.newHashMap();
        final Enumeration attributeNames = session.getAttributeNames();
        while (attributeNames.hasMoreElements()) {
            final String name = (String) attributeNames.nextElement();
            attributes.put(name, session.getAttribute(name));
        }
        return attributes;
    }

    public void destroy() {
        // nop
    }
}
