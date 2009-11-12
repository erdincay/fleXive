/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
import static org.testng.Assert.fail;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Tests related to SelectLists
 *
 * @author Laszlo Hernadi (laszlo.hernadi@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 462 $
 */
public class AdmSelectListTest extends AbstractBackendBrowserTest {
    private final static boolean[] SKIP_TEST_S = calcSkips();
    private final static Log LOG = LogFactory.getLog(AdmContentTest.class);
    private final static String OVERVIEW_LINK = "/adm/main/selectList/overview.jsf";
    private final static String CREATE_LINK = "/adm/main/selectList/create.jsf";

    private final static String[] SELECT_NAMES = {"valid@" + System.currentTimeMillis()};
    private final static String[] SELECT_ITEMS = {"aaa", "aab", "aac", "aad"};

    /**
     * build the skip array, an array in which every test-method have an entry which
     * indicates if a method should be skiped
     * @return the skip-array
     */
    private static boolean[] calcSkips() {
        boolean[] skipList = new boolean[3];
        for (int i = 0; i < skipList.length; i++) {
            skipList[i] = !AbstractBackendBrowserTest.isForceAll();
//            skipList[i] = false;
        }
        skipList[1] = false;
        skipList[skipList.length - 1] = false;
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
     * try to create a selectList with an empty name (should fail)
     * try to create a selectList with some blanks as name (should fail)
     * create a selectList
     * add an item with a blank as name (fail)
     * delete the selectList
     */
    public void selects_ASL_1() {
        if (SKIP_TEST_S[0]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            try { // could run after ASL_2 ... so what...
                deleteSelectList(SELECT_NAMES[0], null, LOG);
            } catch (Throwable t) {
                // ignore it, just be sure that the selectlist not exist
            }
            Assert.assertTrue  (createSelectList("", false, LOG));
            Assert.assertTrue  (createSelectList("  ", false, LOG));
            Assert.assertTrue  (createSelectList(SELECT_NAMES[0], true, LOG));
            Assert.assertTrue  (addItemToSelectList(SELECT_NAMES[0], " ", false, LOG));
            Assert.assertTrue  (deleteSelectList(SELECT_NAMES[0], true, LOG));
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            fail(t.getMessage(), t);
        } finally {
            logout();
        }
    }

    /**
     * try to delete a selectList (don't care about the result...)
     * create a selectList
     * add an item to the selectList
     * add the same item a second time to the selectList (fail)
     * add an other item to the selectList
     * click on the move-down button  (don't check the result)
     */
    @Test
    public void selects_ASL_2() {
        if (SKIP_TEST_S[1]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            try {
                deleteSelectList(SELECT_NAMES[0], null, LOG);
            } catch (Throwable t) {
                // ignore it, just be sure that the selectlist not exist
            }
            Assert.assertTrue  (createSelectList(SELECT_NAMES[0], true, LOG));
            Assert.assertTrue  (addItemToSelectList(SELECT_NAMES[0], SELECT_ITEMS[0], true, LOG));
            Assert.assertTrue  (addItemToSelectList(SELECT_NAMES[0], SELECT_ITEMS[0], false, LOG));
            Assert.assertTrue  (addItemToSelectList(SELECT_NAMES[0], SELECT_ITEMS[1], true, LOG));
            Assert.assertTrue ( moveSelectItemDown(SELECT_NAMES[0], SELECT_ITEMS[0], true, LOG));
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            fail(t.getMessage(), t);
        } finally {
            logout();
        }
    }

    /**
     * add more items to the previous created selectlist
     * assigns subelements to the elements so that each element is subelemnt of the next element
     * tests if the last element can't have any subelemnts (to prevent circular elements)
     * saves the selectList
     * deletes every element and saves it after each delete
     * save it again
     */
    @Test(dependsOnMethods = {"selects_ASL_2"})
    public void selects_ASL_3() {
        if (SKIP_TEST_S[2]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            Assert.assertTrue  (addItemsToSelectList(SELECT_NAMES[0], new String[]{SELECT_ITEMS[2],SELECT_ITEMS[3]}, true, LOG));
            for (int i = 0; i < 3; i++) {
                Assert.assertTrue (assignSubelement(SELECT_ITEMS[i], SELECT_ITEMS[i+1], true));
            }
            String [] tmp = getAssignableSubelements(SELECT_ITEMS[3]);
            Assert.assertTrue (tmp[0].length() == 0);
            selectFrame(Frame.Content);
            selenium.selectFrame("relative=up");
            selenium.click("link=Commit"); // the edit window of the element is still open..
            selenium.click("link=Save");
            checkText("Successfully saved selectlist", true, LOG);
            for (int i = 3; i >= 0; i--) {
                Assert.assertTrue (deleteAndCheckItem(SELECT_ITEMS[i]));
                Assert.assertTrue (saveAndCheck(true, LOG));
            }
            selenium.click("link=Save");
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            fail(t.getMessage(), t);
        } finally {
            logout();
        }
    }

    /**
     * click on save and check if it is successfully saveed
     * @param expectedResult the expected result of the save
     * @param LOG the Log where to log
     * @return <code>true</code> if the result is as expected
     */
    private boolean saveAndCheck(Boolean expectedResult, Log LOG) {
        clickAndWait("link=Save");

        sleep(200);

        return checkText("Successfully saved selectlist", expectedResult, LOG);
    }

    /**
     * Delete and check if it is deleted
     * The selectList must be open
     * @param selectItemName the name of the selectListItem
     * @return <code>true</code> if the selectItemName is gone afterwards
     */
    private boolean deleteAndCheckItem(String selectItemName) {
        selenium.click(getButtonFromTRContainingName_(getResultTable(null), selectItemName, ":deleteButton"));

        return getTRContainingName(getResultTable(null),selectItemName) == null;
    }

    /**
     * Moves a selectListItem down (don't check if it was moved, just click, and there should be no error)
     * @param selectListName the name of the selectList
     * @param selectItemName the name of the selectListItem
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>true</code> if the result is as expected (at least saved)
     */
    private boolean moveSelectItemDown(String selectListName, String selectItemName, boolean expectedResult, Log LOG) {
        editSelectList(selectListName);

        selenium.click(getButtonFromTRContainingName_(getResultTable(null), selectItemName, ":downButton"));

        return saveAndCheck(expectedResult, LOG);
    }

    /**
     * create a selectList
     * @param name the name of the selectList
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>true</code> if the result is as expected
     */
    private boolean createSelectList(String name, boolean expectedResult, Log LOG) {
        loadContentPage(CREATE_LINK);
        selectFrame(Frame.Content);

//        LOG.info("creating : " + name, new Throwable());

        selenium.type("frm:name", name);
        clickAndWait("link=Create");

        return checkText("Successfully created selectlist.", expectedResult, LOG);
    }

    /**
     * returns the assignable subelemnts of the given selectList item
     * the current selectList must be in edit-mode
     * @param selectItemName the name of the selectListItem
     * @return the available subelemnts
     */
    private String[] getAssignableSubelements(String selectItemName) {
//        editSelectList(selectListName);
        selectFrame(Frame.Content);
        selenium.click(getButtonFromTRContainingName_(getResultTable(null), selectItemName, ":editButton"));
        sleep(2000);
        selenium.selectFrame("relative=up");

        return selenium.getSelectOptions("panelForm:editChildren");

//        return null;
    }

    /**
     * assign a subelemnt
     * @param selectItemName the selectItemName to wich a subelement should be assigned
     * @param subelementName the subelemntName wich should be assigned
     * @param expectedResult the expectdResult
     * @return <code>expectedResult</code> if the subelement is available
     */
    private boolean assignSubelement (String selectItemName, String subelementName, boolean expectedResult) {
        for (String curName : getAssignableSubelements(selectItemName)) {
            if (curName.equals(subelementName)) {
                selenium.select("panelForm:editChildren", subelementName);
                selenium.click("link=Commit");
                return expectedResult;
            }
        }
        return !expectedResult;
    }

    /**
     * click on the edit of a selectList
     * @param name name of the selectList
     */
    private void editSelectList(String name) {
        loadContentPage(OVERVIEW_LINK);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null), name, ":editButton_"), 30000);
    }

    private boolean addItemToSelectList(String selectListName, String itemName, boolean expectedResult, Log LOG) {
        return addItemsToSelectList(selectListName, new String[]{itemName}, expectedResult, LOG);
    }

    /**
     * add items to a selectList
     * @param selectListName the name of the selectList
     * @param itemNames name of the items to add
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>expectedResult</code> if the the list could be saved
     */
    private boolean addItemsToSelectList(String selectListName, String[] itemNames, boolean expectedResult, Log LOG) {
        editSelectList(selectListName);

        for (String itemName : itemNames) {
            sleep(1000);
            selenium.click("link=Create new item");
            sleep(1500);
            selenium.selectFrame("relative=up");

            selenium.type("panelForm:editName", itemName);
            selenium.type("panelForm:editLabel_input_1", itemName);

            selenium.click("link=Commit");
        }
        sleep(1000);

        return saveAndCheck(expectedResult, LOG);
    }


    /**
     * delete a selectList
     * @param name the name of the selectList
     * @param expectedResult the expected result
     * @param LOG the Log where to log
     * @return <code>expectedResult</code> if the the list could be deleted
     */
    private boolean deleteSelectList(String name, Boolean expectedResult, Log LOG) {
        loadContentPage(OVERVIEW_LINK);
        selectFrame(Frame.Content);

        LOG.info("deleting : " + name);
        clickAndWait(getButtonFromTRContainingName_(getResultTable(null), name, ":deleteButton_"), 30000);

        return checkText("Successfully deleted selectlist.", expectedResult, LOG);
    }
}
