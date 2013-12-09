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

import org.testng.Assert;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

/**
 * Search result tests for the backend administration.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AdmSearchResultBrowserTest extends AbstractBackendBrowserTest {
    private static final boolean[] SKIP_TEST_S = calcSkips();

    /**
     * build the skip array, an array in which every test-method have an entry which
     * indicates if a method should be skiped
     * @return the skip-array
     */
    private static boolean [] calcSkips() {
        boolean [] skipList = new boolean [2];
        for (int i = 0; i < skipList.length; i++){
            skipList[i] = !AbstractBackendBrowserTest.isForceAll();
        }
//        skipList[2] = false;
        return skipList;
    }

    /**
     * only used if selenium browser must be setup for every class
     * @return <code>true</code> if all elements in the skip-array are true
     */
    protected boolean doSkip() {
        for (boolean cur : SKIP_TEST_S) {
            if (!cur) return false;
        }
        return true;
    }
    /**
     * Submit a quick fulltext search, switch to thumbnails, open first result row
     */
    @Test(groups = "browser")
    public void fulltextSearch() {
        if (SKIP_TEST_S[0]) {
            skipMe();
            return;
        }
//        System.out.println(new Throwable().getStackTrace()[0]);
//        if (true) return;
//        try {throw new Throwable();}catch (Throwable t) {t.printStackTrace();}
        try {
            loginSupervisor();
            Thread.sleep(2000);
            logout();
            loginSupervisor();

//            submitQuicksearch("test caption");
            demoSearch();
//            thumbnailView();

//            System.out.println(getSearchResultContextMenu().toString());
            // load first result row in edit mode through the context menu
//            selectFrame(Frame.Content);
            Thread.sleep(3000);
            getSearchResultContextMenu().openAt("yui-rec0");
//            selenium.click("more_link_9.1");
//            selenium.waitForPageToLoad("30000");
            selenium.click("link=Edit");
//            selenium.waitForPageToLoad("30000");

            // save content
            long start = System.currentTimeMillis();
            int trys = 100;
            while (trys-->0) {
                Thread.sleep(100);
                try {
                    selenium.click("link=Save");
                    break;
                } catch (Throwable t) {

                }
            }
            if (trys == 0) {
                selenium.click("link=Save");
            }
            start = System.currentTimeMillis() - start;
//            start *= -1;
            System.out.println(start + " ms");
//            Thread.sleep(3000);
//            selenium.waitForPageToLoad("30000");
        } catch (Throwable t) {
            t.printStackTrace();
//            try {
//                for (int i = 0; i < 10; i++){
//                    System.out.println(10 - i);
//                    Thread.sleep(1000);
//
//                }
//            } catch (InterruptedException e) {
//            }
            fail();
        } finally {
            logout();
        }
    }

    private void demoSearch() {
//        selenium.waitForPageToLoad("30000");
        navigateTo(NavigationTab.Search);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        navigateTo(NavigationTab.Search);
        final String result = selenium.getEval(WND + ".openQuery('Demo query')");
//        System.out.println(result);
        selectFrame(Frame.Content);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        selenium.click("link=Search");
        selenium.waitForPageToLoad("30000");
    }

    /**
     * Submit a quick search, then do basic row selection tasks in list and thumbnail mode.
     */
//    @Test(groups = "browser")
    private void searchResultRowSelection() {
        if (SKIP_TEST_S[1]) {
//            skipMe();
            return;
        }
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
        selenium.type("query", query);
        selenium.click("searchForm:submitQueryButton");

        // wait for search results
        selenium.waitForCondition(WND + ".getContentFrame().location.href.indexOf('searchResult.jsf') > 0", "30000");
        selenium.waitForCondition(WND + ".document.getElementById('thumbsButton_toolbarIcon') != null", "30000");
    }

}
