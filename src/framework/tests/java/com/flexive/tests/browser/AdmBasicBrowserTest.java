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
     * All admin pages.
     *
     * To get the list of admin pages, execute
     * <pre>grep -o "\"\/.*jsf\"" src/ui/jsf/web/adm/main/navigation.xhtml</pre>
     */
    private static final String[] ADMIN_PAGES = {
            "/adm/main/account/overview.jsf",
            "/adm/main/account/create.jsf",
            "/adm/main/userGroup/overview.jsf",
            "/adm/main/userGroup/new.jsf",
            "/adm/main/mandator/overview.jsf",
            "/adm/main/mandator/create.jsf",
            "/adm/main/acl/aclOverview.jsf",
            "/adm/main/acl/aclCreate.jsf",
            "/adm/main/workflow/overview.jsf",
            "/adm/main/workflow/create.jsf",
            "/adm/main/workflow/stepDefinition/overview.jsf",
            "/adm/main/workflow/stepDefinition/edit.jsf",
            "/adm/main/template/contentTemplateOverview.jsf",
            "/adm/main/template/masterTemplateOverview.jsf",
            "/adm/main/template/tagOverview.jsf",
            "/adm/main/sqlsearch/search.jsf",
            "/adm/search/query.jsf",
            "/adm/content/contentEditor.jsf",
            "/adm/content/createTestData.jsf",
            "/adm/main/scripting/overview.jsf",
            "/adm/main/scripting/create.jsf",
            "/adm/main/scripting/scriptConsole.jsf",
            "/adm/main/system/systemInfo.jsf",
            "/adm/main/system/languages.jsf",
            "/adm/main/selectList/overview.jsf",
            "/adm/main/selectList/create.jsf"
    };

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

            // search/briefcase tab
            navigateTo(NavigationTab.Search);
            assertTrue(selenium.isTextPresent("Search queries"));
            assertTrue(selenium.isTextPresent("Create new Briefcase"));

            // admin tab
            navigateTo(NavigationTab.Administration);
            assertTrue(selenium.isTextPresent("Accounts"));
            assertTrue(selenium.isTextPresent("Groups"));

            // structure tab
            navigateTo(NavigationTab.Structure);
            assertTrue(selenium.isTextPresent("ROOT"));
            assertTrue(selenium.isTextPresent("ACL"));
            assertTrue(selenium.isTextPresent("Workflow Step"));

            // content tab
            navigateTo(NavigationTab.Content);
            assertTrue(selenium.isTextPresent("Root ["));
        } finally {
            logout();
        }
    }

    /**
     * Load all admin pages.
     */
    @Test(groups = "browser")
    public void loadAdminPages() {
        try {
            loginSupervisor();
            navigateTo(NavigationTab.Administration);
            for (String page: ADMIN_PAGES) {
                loadContentPage(page);
            }
        } finally {
            logout();
        }
    }
}
