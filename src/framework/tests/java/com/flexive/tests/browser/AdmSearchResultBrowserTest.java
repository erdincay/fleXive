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

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Search result tests for the backend administration.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AdmSearchResultBrowserTest extends AbstractBackendBrowserTest {
    /**
     * Submit a quick fulltext search, switch to thumbnails, open first result row
     */
    @Test(groups = "browser")
    public void fulltextSearch() {
        try {
            loginSupervisor();

            submitQuicksearch("test caption");
            thumbnailView();

            // load first result row in edit mode through the context menu
            getSearchResultContextMenu().openAt("row0").clickOption("edit");
            selenium.waitForPageToLoad("30000");

            // save content
            selenium.click("frm:saveButton_icon");
            selenium.waitForPageToLoad("30000");
        } finally {
            logout();
        }
    }

    /**
     * Submit a quick search, then do basic row selection tasks in list and thumbnail mode.
     */
    @Test(groups = "browser")
    public void searchResultRowSelection() {
        try {
            loginSupervisor();
            submitQuicksearch("test caption");

            listView();
            selectRowRange(0, 10);
            thumbnailView();
            selectRowRange(0, 10);

            selenium.click("row0");
        } finally {
            logout();
        }
    }

    private void selectRowRange(int firstRow, int lastRow) {
        // select a row range
        selenium.click("row" + firstRow);
        // one row should be selected
        Assert.assertEquals(Integer.parseInt(selenium.getEval("this.browserbot.getCurrentWindow().rowSelection.getSelected().length")), 1);
        selenium.shiftKeyDown();
        selenium.click("row" + lastRow);
        selenium.shiftKeyUp();
        // check selection range
        Assert.assertEquals(Integer.parseInt(selenium.getEval("this.browserbot.getCurrentWindow().rowSelection.getSelected().length")),
                (lastRow - firstRow + 1));
    }


    private FlexiveSelenium.ContextMenu getSearchResultContextMenu() {
        return selenium.getContextMenu("frm:searchResults_resultMenu");
    }

    private void thumbnailView() {
        // switch to thumbnail view
        selectFrame(Frame.Top);
        selenium.click("thumbsButton_toolbarIcon");
        selectFrame(Frame.Content);
        selenium.waitForPageToLoad("30000");
    }

    private void listView() {
        // switch to thumbnail view
        selenium.selectFrame("relative=top");
        selenium.click("listButton_toolbarIcon");
        selectFrame(Frame.Content);
        selenium.waitForPageToLoad("30000");
    }


    private void submitQuicksearch(String query) {
        selenium.type("getResult", query);
        selenium.click("searchForm:submitQueryButton");

        // wait for search results
        selenium.waitForCondition(WND + ".getContentFrame().location.href.indexOf('searchResult.jsf') > 0", "30000");
        selenium.waitForCondition(WND + ".document.getElementById('thumbsButton_toolbarIcon') != null", "30000");
    }

}
