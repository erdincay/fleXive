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
package com.flexive.tests.embedded;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.security.UserTicket;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

/**
 * OpenEJB embedded container bootstrap
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"bootstrap", "ejb", "configuration", "content", "structure", "jsf", "security",
        "workflow", "streaming", "scripting", "valuetest", "cache", "image", "tree", "relation",
        "search", "tutorial", "benchmark", "environment", "mandator", "importexport", "uniquemode",
        "roles", "reference", "binary", "flatstorage", "briefcase", "cmis", "lock", "sequencer", "resultset",
        "contentconversion", "stresstest", "inheritance", "tree-stress", "resource"})
public class ContainerBootstrap {

    /**
     * Check if tests are run from within an application server (ear)
     *
     * @return tests are run from within an application server (ear)
     */
    @Test(enabled = false) //prevent to be run as testcase
    public static boolean runInEAR() {
        return System.getProperty("flexive.tests.ear") != null;
    }

    @BeforeSuite
    public void startup() {
        try {
            FxContext.initializeSystem(DivisionData.DIVISION_TEST, "flexiveTests");
            TestUsers.initializeUsers();
        } catch (Exception ex) {
            if (ex.getMessage().indexOf("Parameter /globalconfig/datasources{GLOBAL}/-2 not found.") >= 0) {
                System.err.println("*******************************************************************");
                System.err.println("** you should run \"ant db.update.config\" to resolve this problem **");
                System.err.println("*******************************************************************");
            }
            throw new RuntimeException(ex);
        }
    }

    @AfterSuite
    public void shutdown() throws FxApplicationException {
        try {
            ScriptingTest.allowTearDown = true;
            ScriptingTest.suiteShutDown();
            TestUsers.deleteUsers();
            System.out.println("=== shutting down tests ===");
            AccountEngine accountEngine = EJBLookup.getAccountEngine();
            for (UserTicket ticket : accountEngine.getActiveUserTickets())
                System.out.println("Still logged in: " + ticket);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

}
