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

import javax.ejb.SessionContext;
import javax.security.auth.callback.Callback;
import javax.sql.DataSource;

/**
 * Flexive callback class.
 * <p/>
 * Underlying security services instantiate and pass a
 * <code>FxCallback</code> to the <code>handle</code>
 * method of a <code>CallbackHandler</code> to retrieve name information.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @see javax.security.auth.callback.CallbackHandler
 */
public class FxCallback implements Callback, java.io.Serializable {
    private static final long serialVersionUID = -6475964749101506776L;

    private boolean takeOverSession = false;
    private transient SessionContext sessionContext = null;
    private transient DataSource dataSource = null;
    private boolean calledAsSupervisor = false;

    /**
     * If takeOver is disabled the login attempt fails if a other session is already
     * using the account. If enabled a other active session using the account is logged off,
     * and the login succeeds.
     *
     * @return true if take over is enabled.
     */
    public boolean getTakeOverSession() {
        return this.takeOverSession;
    }

    /**
     * Sets the takeOverSession option, see <code>getTakeOverSession()</code> for details.
     *
     * @param takeOver true or false
     */
    public void setTakeOverSession(boolean takeOver) {
        this.takeOverSession = takeOver;
    }

    public void setDataSource(DataSource ds) {
        this.dataSource = ds;
    }

    public void setSessionContext(SessionContext ctx) {
        this.sessionContext = ctx;
    }

    public DataSource getDataSource() {
        return this.dataSource;
    }

    public SessionContext getSessionContext() {
        return this.sessionContext;
    }

    /**
     * @return  true when the login method was called by a global supervisor
     * @since 3.1.6
     */
    public boolean isCalledAsGlobalSupervisor() {
        return calledAsSupervisor;
    }

    /**
     * Indicate that the login method was called by the global supervisor.
     *
     * @param calledAsSupervisor    true if the login method was called by a global supervisor
     * @since 3.1.6
     */
    public void setCalledAsSupervisor(boolean calledAsSupervisor) {
        this.calledAsSupervisor = calledAsSupervisor;
    }

}
