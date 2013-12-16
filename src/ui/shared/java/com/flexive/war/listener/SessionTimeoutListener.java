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
package com.flexive.war.listener;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.security.UserTicket;
import com.flexive.war.filter.FxSessionWrapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * Session timeout listener. Performs a logout when a session is destroyed, e.g. through a timeout event.
 * This works in clusters only when sessions are not moved between cluster nodes, otherwise the user will
 * be logged out when the session moves.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SessionTimeoutListener implements HttpSessionListener {
    private static final Log LOG = LogFactory.getLog(SessionTimeoutListener.class);

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Session created: " + event.getSession().getId());
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        final HttpSession session = event.getSession();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Session destroyed: " + session.getId());
        }
        // set parameters needed for logout
        final FxContext ctx = FxContext.get();
        
        try {
            ctx.setSessionID(session.getId());

            // retrieve context path
            final ServletContext servletContext = session.getServletContext();
            if (servletContext != null) {
                ctx.setContextPath(servletContext.getContextPath());
            }

            // get division ID from session
            final String divisionIdKey = FxSessionWrapper.encodeAttributeName(FxContext.SESSION_DIVISIONID);
            if (session.getAttribute(divisionIdKey) != null) {
                ctx.setDivisionId((Integer) session.getAttribute(divisionIdKey));
            }

            if (ctx.getDivisionId() == -1 || !CacheAdmin.isEnvironmentLoaded()) {
                // probably during undeploy
                return;
            }
            
            ctx.setTicket(FxContext.getTicketFromEJB(session));
            if (!ctx.getTicket().isGuest()) {
                // perform logout only when the user is logged in
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Performing logout for user of destroyed session (possible session timeout): "
                            + session.getId());
                }

                onLogout(ctx.getTicket());

                ctx.logout();
            }
        } catch (FxLogoutFailedException e) {
            LOG.error("Failed to logout user after session timeout: " + e.getMessage(), e);
        } finally {
            FxContext.cleanup();
        }
    }

    /**
     * Override to add custom behaviour when a user is logged out due to a session timeout.
     * Called before the user is actually logged out.
     *
     * @param ticket    the user whose session timed out
     * @since 3.2.0
     */
    protected void onLogout(UserTicket ticket) {
    }
}
