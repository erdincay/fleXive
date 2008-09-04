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
package com.flexive.core.security;

import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.security.UserTicket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;


/**
 * Default login module.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxDefaultLogin implements LoginModule {
    private static final Log LOG = LogFactory.getLog(FxDefaultLogin.class);
    private CallbackHandler callbackHandler = null;
    private Subject subject = null;

    // temporary state, set in the commit function
    private Vector<FxPrincipal> tempPrincipals = null;
    private boolean success = false;


    /**
     * Returns the UserTicket stored within the subject.
     *
     * @param sub the suubject
     * @return the UserTicket stored within the subject
     * @throws FxNotFoundException if the subject doesnt hold a UserTicket.
     *                             This should never happen since the Login Module fills out
     *                             a UserTicket for every new subject.
     */
    public static UserTicket getUserTicket(Subject sub) throws FxNotFoundException {
        Iterator it = sub.getPrincipals(FxPrincipal.class).iterator();
        if (it.hasNext()) {
            FxPrincipal p = (FxPrincipal) it.next();
            return p.getUserTicket();
        } else {
            FxNotFoundException nfe = new FxNotFoundException("Subject without UserTicket encountered");
            LOG.fatal(nfe);
            throw nfe;
        }
    }

    /**
     * Sets the user ticket within a subject
     *
     * @param sub    the subject
     * @param ticket the new ticket for the subject
     * @return the user ticket
     */
    public static Subject updateUserTicket(Subject sub, UserTicket ticket) {
        // remove the old user ticket
        for (FxPrincipal p : sub.getPrincipals(FxPrincipal.class))
            sub.getPrincipals().remove(p);
        // Set the credentials and principals
        sub.getPrincipals().add(new FxPrincipal(ticket));
        return sub;
    }


    /**
     * Constructor
     */
    public FxDefaultLogin() {
        this.tempPrincipals = new Vector<FxPrincipal>(5);
        this.success = false;
    }

    /**
     * This method is called if the LoginContext's overall authentication failed.
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules did not succeed).
     * <p/>
     * If this LoginModule's own authentication attempt succeeded (checked by retrieving the private state saved by the
     * <code>login</code> and <code>commit</code> methods), then this method cleans up any state that was
     * originally saved.
     *
     * @return false if this LoginModule's own login and/or commit attempts failed, and true otherwise.
     * @throws LoginException if the abort fails.
     */
    public boolean abort() throws LoginException {
        clearTemporaryStates();
        // Login aborted
        success = false;
        return true;
    }


    /**
     * Abstract method to commit the authentication process (phase 2).
     * <p/>
     * This method is called if the LoginContext's overall authentication succeeded
     * (the relevant REQUIRED, REQUISITE, SUFFICIENT and OPTIONAL LoginModules succeeded).
     * <p/>
     * If this LoginModule's own authentication attempt succeeded (checked by retrieving the private state saved by the
     * <code>login</code> method), then this method associates a <code>RdbmsPrincipal</code>
     * with the <code>Subject</code> located in the
     * <code>LoginModule</code>.  If this LoginModule's own
     * authentication attempted failed, then this method removes
     * any state that was originally saved.
     *
     * @return true if this LoginModule's own login and commit attempts succeeded, or false otherwise.
     * @throws LoginException if the commit fails
     */

    public boolean commit() throws LoginException {
        if (success) {
            // Subject may not be read only
            if (subject.isReadOnly()) {
                LoginException le = new LoginException("Subject is Readonly");
                LOG.error(le);
                throw le;
            }
            // Set the principials and credentials
            subject.getPrincipals().addAll(tempPrincipals);
            //subject.getPublicCredentials().add(tempUserTicket);
        }
        // Clear all temp variables
        clearTemporaryStates();
        return true;
    }


    /**
     * Clears all temporary variables.
     */
    private void clearTemporaryStates() {
        tempPrincipals.clear();
        if (callbackHandler instanceof PassiveCallbackHandler)
            ((PassiveCallbackHandler) callbackHandler).clearPassword();
    }

    /**
     * Verify the name/password combination.
     *
     * @return true always, since this LoginModule should not be ignored.
     * @throws FailedLoginException if the authentication fails.
     * @throws LoginException       if this LoginModule is unable to perform the authentication.
     */
    public boolean login() throws LoginException {
        LoginException le = null;
        try {
            // Determine username and password using the callback handler
            final Callback[] callbacks = new Callback[]{
                    new NameCallback("user: "),
                    new PasswordCallback("password: ", true),
                    new FxCallback()
            };
            callbackHandler.handle(callbacks);

            FxCallback ac = ((FxCallback) callbacks[2]);
            final String username = ((NameCallback) callbacks[0]).getName();
            final PasswordCallback pc = (PasswordCallback) callbacks[1];
            final String password = new String((pc.getPassword()));
            pc.clearPassword();
            UserTicket ticket = FxAuthenticationHandler.login(username, password, ac);
            // Set the credentials and principals
            this.tempPrincipals.add(new FxPrincipal(ticket));
            // The login was successfull
            success = true;
            if (LOG.isInfoEnabled())
                LOG.info("User [" + ticket.getUserName() + "] successfully logged in, ticket=" + ticket);
        } catch (IOException exc) {
            le = new FxLoginFailedException("IOException: " + exc.getMessage(),
                    FxLoginFailedException.TYPE_UNKNOWN_ERROR);
            LOG.error(le);
        } catch (UnsupportedCallbackException exc) {
            le = new FxLoginFailedException("IOException: " + exc.getMessage(),
                    FxLoginFailedException.TYPE_UNKNOWN_ERROR);
            LOG.error(le);
        }
        // Log and throw exceptions
        if (le != null) {
            success = false;
            throw le;
        }
        return true;
    }


    /**
     * Logout a user.
     * <p/>
     * This method removes the Principals that were added by the commit method.
     *
     * @return true in all cases
     * @throws LoginException if the logout fails.
     */
    public boolean logout() throws LoginException {
        // Clear all temp variables
        clearTemporaryStates();

        // remove the principals the login module added
        for (FxPrincipal p : subject.getPrincipals(FxPrincipal.class))
            FxAuthenticationHandler.logout(p.getUserTicket());
        return true;
    }


    /**
     * Initialize this LoginModule.
     *
     * @param sub         the Subject to be authenticated.
     * @param callback    a CallbackHandler for communicating with the end user (prompting for usernames and
     *                    passwords, for example).
     * @param sharedState shared LoginModule state.
     * @param options     options specified in the login Configuration for this particular LoginModule.
     */
    public void initialize(Subject sub, CallbackHandler callback,
                           Map<String, ?> sharedState,
                           Map<String, ?> options) {
        this.callbackHandler = callback;
        this.subject = sub;
    }


}
