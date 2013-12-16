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
package com.flexive.tests.browser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;

import static org.testng.Assert.fail;

/**
 * <p>
 * Basic backend test cases.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AdmBasicBrowserTest extends AbstractBackendBrowserTest {
    private ArrayList<String> notFounds = new ArrayList<String>();
    private boolean allOk = true;
    private static final Log LOG = LogFactory.getLog(AdmBasicBrowserTest.class);
    private final static boolean[] SKIP_TEST_S = calcSkips();

    /**
     * build the skip array, an array in which every test-method have an entry which
     * indicates if a method should be skiped
     *
     * @return the skip-array
     */
    private static boolean[] calcSkips() {
        boolean[] skipList = new boolean[3];
        for (int i = 0; i < skipList.length; i++) {
            skipList[i] = !AbstractBackendBrowserTest.isForceAll();
//            skipList[i] = false;
        }
//        skipList[0] = false;
        return skipList;
    }

    /**
     * only used if selenium browser must be setup for every class
     *
     * @return <code>true</code> if all elements in the skip-array are true
     */
    @Override
    protected boolean doSkip() {
        for (boolean cur : SKIP_TEST_S) {
            if (!cur) return false;
        }
        return true;
    }

    /**
     * All admin pages.
     * <p/>
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
    public void loginLogout() {
        if (SKIP_TEST_S[0]) {
            skipMe();
            return;
        }
        loginSupervisor();
        logout();
    }

    /**
     * Clicks on all navigation tabs.
     */
    public void basicNavigation() {
        if (SKIP_TEST_S[1]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            notFounds.clear();
            allOk = true;
//            System.out.println(new Throwable().getStackTrace()[0]);

            // search/briefcase tab
            navigateTo(NavigationTab.Search);
//            navigateTo(NavigationTab.Search);
            testIfNavigationTabTextPresent("Search queries");
            testIfNavigationTabTextPresent("Briefcases");

            // admin tab
            navigateTo(NavigationTab.Administration);
            testIfNavigationTabTextPresent("Accounts");
            testIfNavigationTabTextPresent("User groups");
            testIfNavigationTabTextPresent("Mandators");
            testIfNavigationTabTextPresent("Access control lists");
            testIfNavigationTabTextPresent("Workflows");
            testIfNavigationTabTextPresent("Scripts");
            testIfNavigationTabTextPresent("Selectlists");
            testIfNavigationTabTextPresent("Import / Export");
            testIfNavigationTabTextPresent("System");
//            testIfNavigationTabTextPresent("");

            // structure tab
            navigateTo(NavigationTab.Structure);
            testIfNavigationTabTextPresent("ROOT");
            testIfNavigationTabTextPresent("ACL");
            testIfNavigationTabTextPresent("Workflow Step");

            // content tab
            navigateTo(NavigationTab.Content);
            testIfNavigationTabTextPresent("Root [");

        } finally {
            logout();
            if (!allOk) {
//                System.out.println("ERROR : ");
                for (String s : notFounds) {
//                    System.out.println(s);
                    if (s.startsWith("-")) {
                        LOG.error(s.substring(1));
                    } else {
                        LOG.info(s.substring(1));
                    }
                }
                fail();
            }
        }
    }

    private void testIfNavigationTabTextPresent(String text) {
        allOk &= testIfNavigationTabTextPresent(text, notFounds);
    }

    /**
     * Load all admin pages.
     */
    public void loadAdminPages() {
        if (SKIP_TEST_S[2]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            notFounds.clear();
            allOk = true;
            navigateTo(NavigationTab.Administration);
            for (String page : ADMIN_PAGES) {
                try {
                    loadContentPage(page);
                    notFounds.add("+\tpage \"" + page + "\" found");
                } catch (Throwable t) {
                    notFounds.add("-\tpage \"" + page + "\" NOT found");
                    allOk = false;
                }
            }
        } finally {
            logout();
            if (!allOk) {
//                System.out.println("ERROR : ");
                for (String s : notFounds) {
                    if (s.startsWith("-")) {
                        LOG.error(s.substring(1));
                    } else {
                        LOG.info(s.substring(1));
                    }
                }
                fail();
            }
        }
    }
}
