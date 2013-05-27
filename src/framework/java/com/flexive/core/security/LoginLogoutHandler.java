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
package com.flexive.core.security;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.security.UserTicket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.SessionContext;
import javax.security.auth.Subject;
import javax.sql.DataSource;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashSet;

/**
 * Provides convenience methods for login and logout of flexive users.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public final class LoginLogoutHandler {

    private static final Log LOG = LogFactory.getLog(LoginLogoutHandler.class);

    /**
     * Private construcutor
     */
    private LoginLogoutHandler() {
    }

    /**
     * Login function.
     *
     * @param username The username
     * @param password The users password
     * @param takeOver If a other session is already logged in with this unique username the other session is
     *                 invalidated (logged out), and this session is logged in.
     * @param ctx      the session context
     * @param ds       the datasource to be used
     * @return The new UserTicket if the login succeeded, or null if the login failed.
     * @throws FxLoginFailedException  if the login failed
     * @throws FxAccountInUseException if a other session is already logged in (with this unique username) AND take
     *                                 over is false
     */
    public static UserTicket doLogin(String username, String password, boolean takeOver,
                                     SessionContext ctx, DataSource ds)
            throws FxLoginFailedException, FxAccountInUseException {
        boolean success = false;
        try {
            // Actually logged in?
            UserTicket ticket = FxContext.getUserTicket();
            final boolean calledAsSupervisor = ticket.isGlobalSupervisor();
            if (!ticket.isGuest()) {
                doLogout();
            }

            // Try a login
            /*PassiveCallbackHandler pch = new PassiveCallbackHandler(username, password, takeOver, ctx, ds);
            LoginContext lc = new LoginContext(LOGIN_CTX, pch);
            lc.login();
            final Subject sub = lc.getSubject();
            ticket = FxDefaultLogin.getUserTicket(sub);*/
            final FxCallback callback = new FxCallback();
            callback.setTakeOverSession(takeOver);
            callback.setSessionContext(ctx);
            callback.setDataSource(ds);
            callback.setCalledAsSupervisor(calledAsSupervisor);
            ticket = FxAuthenticationHandler.login(username, password, callback);
            // Log out any other sessions of the user
            if (!ticket.isMultiLogin() && !ticket.isWebDav()) {
                // TODO: real logout?
                UserTicketStore.removeUserId(ticket.getUserId(), ticket.getApplicationId());
            }
            // Set session informations in cluster cache
            final Subject sub = new Subject(false, new HashSet<Principal>(Arrays.asList(new FxPrincipal(ticket))),
                    new HashSet(), new HashSet());
            UserTicketStore.storeSubject(sub);
            // flag success
            success = true;
            EJBLookup.getHistoryTrackerEngine().track("history.account.login", ticket.getLoginName());
            // Return the ticket
            return ticket;
        } catch (FxLoginFailedException exc) {
            EJBLookup.getHistoryTrackerEngine().track("history.account.login.error", username);
            throw exc;
        } catch (FxAccountInUseException exc) {
            throw exc;
        } catch (Exception exc) {
            FxLoginFailedException le = new FxLoginFailedException("Login failed (internal error): " + exc.getMessage(),
                    FxLoginFailedException.TYPE_UNKNOWN_ERROR);
            LOG.error(le);
            throw le;
        } finally {
            if (!success)
                try {
                    doLogout();
                } catch (Exception exc) {
                    // ignore, this is only a cleanup attempt that will most likely fail
                }
        }
    }


    /**
     * Logoff a user defined by its session.
     *
     * @throws com.flexive.shared.exceptions.FxLogoutFailedException
     *          if the system is unable to logoff the session
     */
    public static void doLogout() throws FxLogoutFailedException {
        Subject sub = UserTicketStore.getSubject(FxContext.get());
        if (sub == null) {
            // No information available, user is not logged in.
            return;
        }

        try {
            String loginname = FxContext.getUserTicket().getLoginName();
            FxAuthenticationHandler.logout(FxContext.getUserTicket());
            UserTicketStore.removeSubject();
            EJBLookup.getHistoryTrackerEngine().track("history.account.logout", loginname);
        } catch (Exception exc) {
            FxLogoutFailedException lfe = new FxLogoutFailedException("Logout failed", exc);
            if (LOG.isDebugEnabled()) LOG.debug(lfe);
            throw lfe;
        }
    }


}
