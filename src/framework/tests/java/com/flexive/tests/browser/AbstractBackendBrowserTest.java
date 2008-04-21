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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Base class for backend browser tests.
 * Context menu support needs a patched server as described at http://forums.openqa.org/thread.jspa?messageID=20582.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class AbstractBackendBrowserTest extends AbstractSeleniumTest {
    protected static final String WND = "selenium.browserbot.getCurrentWindow()";
    private static final String CONTEXTPATH = "/flexive";

    /**
     * All IFrames available in the backend administration.
     */
    protected enum Frame {
        Content("contentFrame"),
        Top("relative=top"),
        NavContent("treeNavFrame_0"),
        NavStructures("treeNavFrame_1"),
        NavSearch("treeNavFrame_2"),
        NavAdministration("treeNavFrame_3");

        private String name;

        private Frame(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * The available navigation tabs of the main navigation frame.
     */
    protected enum NavigationTab {
        // the order of the navigation tabs must match the actual layout
        // since the position determines the ID

        Content(Frame.NavContent),
        Structure(Frame.NavStructures),
        Search(Frame.NavSearch),
        Administration(Frame.NavAdministration);

        private Frame frame;

        NavigationTab(Frame frame) {
            this.frame = frame;
        }
    }

    public AbstractBackendBrowserTest() {
        super(null, "browser.properties");
    }

    @Override
    @BeforeClass
    public void beforeClass() {
        super.beforeClass();
    }

    @Override
    @AfterClass
    public void afterClass() {
        super.afterClass();
    }

    protected void login(String userName, String password) {
        selenium.open(CONTEXTPATH + "/adm/pub/login.jsf");
        selenium.type("loginForm:username", userName);
        selenium.type("loginForm:password", password);
        selenium.click("loginForm:takeOver");
        selenium.click("loginForm:loginImage");
        // wait for the loading screen to disappear
        selenium.waitForCondition(WND + ".document.getElementById('loading') != null " +
                "&& " + WND + ".document.getElementById('loading').style.display == 'none'", "120000");
    }

    protected void loginSupervisor() {
        login("supervisor", "supervisor");
    }

    protected void logout() {
        selectFrame(Frame.Top);
        selenium.click("searchForm:logoutImage");
        selectFrame(Frame.Content);
        selenium.waitForPageToLoad("30000");
        selenium.click("frm:logoutButton");
        selenium.waitForPageToLoad("30000");
    }

    protected void navigateTo(NavigationTab tab) {
        selectFrame(Frame.Top);
        final String result = selenium.getEval(WND + ".gotoNavMenu(" + tab.ordinal() + ")");
        selectFrame(tab.frame);
        if ("false".equals(result)) {
            // frame not loaded yet, wait for loading to complete
            selenium.waitForPageToLoad("30000");
        }
    }

    protected void selectFrame(Frame frame) {
        if (frame != Frame.Top) {
            selectFrame(Frame.Top); // select top frame first
        }
        selenium.selectFrame(frame.getName());
    }

    protected void loadContentPage(String url) {
        selectFrame(Frame.Content);
        selenium.open(CONTEXTPATH + (url.startsWith("/") ? url : "/" + url));
        selenium.waitForPageToLoad("30000");
    }
}
