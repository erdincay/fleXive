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
package com.flexive.tests.browser;

import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

/**
 * <p>
 * Basic backend test cases.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AdmBasicBrowserTest extends AbstractBackendBrowserTest {
    /**
     * Logs in into the backend, then logs out again.
     */
    @Test(groups = "browser")
    public void loginLogout() {
        loginSupervisor();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            // ignore
        }
        logout();
    }

    /**
     * Clicks on all navigation tabs.
     */
    @Test(groups = "browser")
    public void basicNavigation() {
        try {
            loginSupervisor();
            // briefcase tab
            navigateTo(NavigationTab.Briefcases);
            // TODO check result
            // administration tab
            navigateTo(NavigationTab.Administration);
            assertTrue(selenium.isTextPresent("Accounts"));
            assertTrue(selenium.isTextPresent("Groups"));

            // structure tab
            navigateTo(NavigationTab.Structure);
            assertTrue(selenium.isTextPresent("ROOT"));
            assertTrue(selenium.isTextPresent("ACL"));
            assertTrue(selenium.isTextPresent("Workflow step"));

            // content tab
            navigateTo(NavigationTab.Content);
            assertTrue(selenium.isTextPresent("Root ["));
        } finally {
            logout();
        }
    }


}
