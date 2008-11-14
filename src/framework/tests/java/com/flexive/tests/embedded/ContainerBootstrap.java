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
package com.flexive.tests.embedded;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.AfterSuite;
import com.flexive.shared.FxContext;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.core.security.UserTicketImpl;
import com.flexive.core.Database;

/**
 * OpenEJB embedded container bootstrap
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"bootstrap", "ejb", "configuration", "content", "structure", "jsf", "security",
        "workflow", "streaming", "scripting", "valuetest", "cache", "image", "tree", "relation",
        "search", "tutorial", "benchmark", "environment", "mandator", "importexport",
        "roles"})
public class ContainerBootstrap {
    @BeforeSuite
    public void startup() {
        try {
            // set current thread to test division
            FxContext.get().setDivisionId(DivisionData.DIVISION_TEST);
            FxContext.get().setContextPath("flexiveTests");
            FxContext.get().setTicket(UserTicketImpl.getGuestTicket());

            // force DS lookup now since the JNDI context does not always contain the resource entries
            // This is a known issue with OpenEJB up until 3.1, so this may not be necessary for future versions
//            Database.getGlobalDataSource();
//            Database.getDataSource("jdbc/flexiveTest");
//            Database.getDataSource();

            // force flexive initialization
            FxContext.get().runAsSystem();
            try {
                CacheAdmin.getEnvironment();
            } finally {
                FxContext.get().stopRunAsSystem();
            }
            // load guest ticket
            FxContext.get().setTicket(UserTicketImpl.getGuestTicket());
            TestUsers.initializeUsers();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @AfterSuite
    public void shutdown() throws FxApplicationException {
        try {
            ScriptingTest.allowTearDown = true;
            ScriptingTest.suiteShutDown();
            TestUsers.deleteUsers();
            System.out.println("=== shutting down EJB3 container ===");
            AccountEngine accountEngine = EJBLookup.getAccountEngine();
            for (UserTicket ticket : accountEngine.getActiveUserTickets())
                System.out.println("Still logged in: " + ticket);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

}
