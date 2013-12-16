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
package com.flexive.core.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.SessionContext;
import javax.security.auth.callback.*;
import javax.sql.DataSource;
import java.io.IOException;


/**
 * PassiveCallbackHandler has constructor that takes
 * a username and password so its handle() method does
 * not have to prompt the user for input.
 * Useful for server-side applications.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class PassiveCallbackHandler implements CallbackHandler {

    private static final Log LOG = LogFactory.getLog(PassiveCallbackHandler.class);
    private String username = null;
    private char[] password = null;
    private boolean takeOverSession = false;
    private SessionContext ctx = null;
    private DataSource ds = null;

    /**
     * Creates a callback handler with the give username and password.
     *
     * @param user            the username
     * @param pass            the password
     * @param takeOverSession if an existing session should be "taken over"
     * @param ctx             the session context to be used
     * @param ds              the datasource to be used for authentication
     */
    public PassiveCallbackHandler(String user, String pass, boolean takeOverSession,
                                  SessionContext ctx, DataSource ds) {
        if (user == null) user = "";
        if (pass == null) pass = "";
        this.username = user;
        this.password = pass.toCharArray();
        this.takeOverSession = takeOverSession;
        this.ctx = ctx;
        this.ds = ds;
    }

    /**
     * Handles the specified set of Callbacks. Uses the username and password that were supplied to our
     * constructor to popluate the Callbacks.
     * <p/>
     * This class supports NameCallback and PasswordCallback.
     *
     * @param callbacks the callbacks to handle
     * @throws IOException                  if an input or output error occurs.
     * @throws UnsupportedCallbackException if the callback is not an instance of NameCallback or PasswordCallback
     */
    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

        for (Callback callback : callbacks) {
            if (callback == null) continue;
            if (callback instanceof NameCallback) {
                ((NameCallback) callback).setName(username);
            } else if (callback instanceof PasswordCallback) {
                ((PasswordCallback) callback).setPassword(password);
            } else if (callback instanceof FxCallback) {
                FxCallback ac = ((FxCallback) callback);
                ac.setTakeOverSession(takeOverSession);
                ac.setDataSource(ds);
                ac.setSessionContext(ctx);
            } else {
                UnsupportedCallbackException uce = new UnsupportedCallbackException(callback, "Callback class ["
                        + callback.getClass() + "] not supported");
                LOG.error(uce);
                throw uce;
            }
        }

    }

    /**
     * Clears out password state.
     */
    public void clearPassword() {
        if (password != null) {
            for (int i = 0; i < password.length; i++)
                password[i] = ' ';
            password = null;
        }
    }

}

