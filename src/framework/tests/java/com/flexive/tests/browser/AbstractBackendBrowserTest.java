/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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

    /**
     * All IFrames available in the backend administration.
     */
    protected enum Frame {
        Content("contentFrame"),
        Top("relative=top"),
        Navigation("treeNavFrame");

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
        Content, Structure, Administration, Briefcases
    }

    ;

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
        selenium.open("/flexive/adm/pub/login.jsf");
        selenium.type("loginForm:username", userName);
        selenium.type("loginForm:password", password);
        selenium.click("loginForm:takeOver");
        selenium.click("loginForm:loginImage");
        // wait for the loading screen to disappear
        selenium.waitForCondition(WND + ".document.getElementById('loading') != null " +
                "&& " + WND + ".document.getElementById('loading').style.display == 'none'", "30000");
    }

    protected void loginSupervisor() {
        login("s", "s");
    }

    protected void logout() {
        selectFrame(Frame.Top);
        selenium.click("searchForm:logoutImage");
        selenium.waitForPageToLoad("30000");
    }

    protected void navigateTo(NavigationTab tab) {
        selectFrame(Frame.Navigation);
        final String locator;
        switch (tab) {
            case Content:
                locator = "mainNavForm:menu_0";
                break;
            case Structure:
                locator = "mainNavForm:menu_1";
                break;
            case Administration:
                locator = "mainNavForm:menu_2";
                break;
            case Briefcases:
                locator = "mainNavForm:menu_3";
                break;
            default:
                throw new IllegalArgumentException(tab.toString());
        }
        selenium.click(locator);
        selenium.waitForPageToLoad("30000");
    }

    protected void selectFrame(Frame frame) {
        if (frame != Frame.Top) {
            selectFrame(Frame.Top); // select top frame first
        }
        selenium.selectFrame(frame.getName());
    }
}
