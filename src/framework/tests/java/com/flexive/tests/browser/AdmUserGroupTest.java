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

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Tests related to User Groups
 *
 * @author Laszlo Hernadi (laszlo.hernadi@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 462 $
 */
public class AdmUserGroupTest extends AbstractBackendBrowserTest {

    private static final Log LOG = LogFactory.getLog(AdmUserGroupTest.class);
    private final static boolean [] SKIP_TEST_S = calcSkips();

    private final static String [] MANDATORS = {"mand01", "mand02"};
    private final static String [] GROUPS = {"group01", "group02", "group03"};

    /**
     * build the skip array, an array in which every test-method have an entry which
     * indicates if a method should be skiped
     * @return the skip-array
     */
    private static boolean [] calcSkips() {
        boolean [] skipList = new boolean [3];
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
     * create the needed mandators
     */
    @BeforeClass
    public void beforClass() {
        super.beforeClass();
        createMandators();
    }

    /**
     *  User groups	AUG_1
     *   Create user group "group01" and assign it to mandator "mand01".	Logged in as supervisor.	User group created
	 *	 Create user group "group02" and assign it to mandator "mand01".	Logged in as supervisor.	User group created
	 *	 Rename user group "group02" to "group01".	                        Logged in as supervisor.	Error
	 *	 Rename user group "group01" to "group03".	                        Logged in as supervisor.	User group renamed
	 *	 Rename user group "group03" to "group01".	                        Logged in as supervisor.	User group renamed
	 *	 Create user group named "group01" and assign it to "mand01".	    Logged in as supervisor.	Error
     */
    @Test
    public void userGroups_AUG_1() {
        if (SKIP_TEST_S[0]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            // To be sure that mand01 + mand02 exist...
            createMandator(MANDATORS[0], true);
            createMandator(MANDATORS[1], true);
//            deleteUserGroup(GROUPS[0]);
//            for (int i = 0; i < 3; i++) {
//                deleteUserGroup(GROUPS[i]);
//            }
//
//            Assert.assertTrue (createUserGroup(GROUPS[0], MANDATORS[0], true));
//            Assert.assertTrue (createUserGroup(GROUPS[1], MANDATORS[0], true));
            Assert.assertTrue (renameUserGroup(GROUPS[1], GROUPS[0], false));
            Assert.assertTrue (renameUserGroup(GROUPS[0], GROUPS[2], true));
            Assert.assertTrue (renameUserGroup(GROUPS[2], GROUPS[0], true));
            Assert.assertTrue (createUserGroup(GROUPS[0], MANDATORS[0], false));
//            Assert.assertTrue (createUserGroup(GROUPS[2], MANDATORS[1], true));

        } finally {
            logout();
        }
    }

    /**
     * create the needed groups
     */
    @Test(dependsOnMethods = {"userGroups_AUG_1"})
    public void userGroups_AUG_2() {
        if (SKIP_TEST_S[1]) {
            skipMe();
            return;
        }
        createGroup();
    }

    /**
     * create the needed groups
     * @see AbstractBackendBrowserTest#createGroup()
     * shouldn't be called direct
     */
    protected void createGroup_() {
        try {
            loginSupervisor();
            // NOT assert-ing because maybe AUG_1 don't run, so no groups to delete
            deleteUserGroup(GROUPS[0]);
            deleteUserGroup(GROUPS[1]);
            deleteUserGroup(GROUPS[2]);

            Assert.assertTrue (createUserGroup(GROUPS[0], MANDATORS[0], true));
            Assert.assertTrue (createUserGroup(GROUPS[1], MANDATORS[0], true));
            Assert.assertTrue (createUserGroup(GROUPS[2], MANDATORS[1], true));

        } finally {
            logout();
        }
    }

    /**
     * edit a usergroup and grant backend-access
     * edit the same usergroup and take away the backend-access right
     */
    public void userGroups_AUG_3(){
        if (SKIP_TEST_S[2]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            Assert.assertTrue  (editUserGroup(GROUPS[1], true, LOG, "Backend Access"));
            Assert.assertTrue  (editUserGroup(GROUPS[1], true, LOG,""));
        } finally {
            logout();
        }
    }

    /**
      * creates a givven userGroup
      * @param groupName the name to create
      * @param mandatorName the name of the mandator to create the user to
      * @param expectedResult if <code>null</code> then it is ignored, otherwise if the result not equals to the expectedResult, the error / warning is logged, and a RuntimeException is thrown
      * @throws RuntimeException when on the page is an error / warning message (and the expectedResult is true
      * @return depends on the expectedResult givven, if <code>true</code> or <code>null</code> then the result of the update, otherwise (<code>false</code>) the negated result of the update
      */
    private boolean createUserGroup(String groupName, String mandatorName, Boolean expectedResult) {
        loadContentPage(USER_OVERVIEW_PAGE);
//        final int MAX_TRYS = 10;
//        int trys = MAX_TRYS;
//        while (trys-->0) {
            clickAndWait("link=Create");
//            sleep(150);
//            try {
//            if (selenium.getHtmlSource().indexOf("Create new user group") > 0) {
//                break;
//            }
//            } catch (Throwable t) {
//                // try again...
//            }
//            sleep(400);
//        }
   //     selenium.waitForPageToLoad("30000");
        selenium.select("frm:mandator", mandatorName);
        selenium.type("frm:groupname", groupName);
        clickAndWait("link=Create");
//        selenium.waitForPageToLoad("30000");

        return checkText("Group " + groupName + " was successfully created.", expectedResult, LOG);
    }

    /**
     * Renames a usergroup to the givven name
     * @param fromName the Usergroup to search
     * @param toName the name to rename the found usergroup
     * @param expectedResult if <code>null</code> then it is ignored, otherwise if the result not equals to the expectedResult, the error / warning is logged, and a RuntimeException is thrown
     * @throws RuntimeException when on the page is an error / warning message (and the expectedResult is true
     * @return depends on the expectedResult givven, if <code>true</code> or <code>null</code> then the result of the update, otherwise (<code>false</code>) the negated result of the update
     */
    private boolean renameUserGroup(String fromName, String toName, Boolean expectedResult) {
        loadContentPage(USER_OVERVIEW_PAGE);
        
        selectFrame(Frame.Content);
        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),fromName,":editButton_"), 30000);
        selenium.type("frm:groupname", toName);
        sleep(200);
        clickAndWait("link=Save", 30000);
//        selenium.waitForPageToLoad("30000");

        return checkText("Group " + toName + " was successfully updated.", expectedResult, LOG);
    }

     /**
      * delete a usergroup to the givven name
      * @param name the Usergroup to search
      * @return <code>true</code> if the group was successfully deleted
      */
     private boolean deleteUserGroup(String name) {
         loadContentPage(USER_OVERVIEW_PAGE);

         selectFrame(Frame.Content);
         try {
             clickAndWait(getButtonFromTRContainingName_(getResultTable(USER_OVERVIEW_PAGE),name,":deleteButton_"), 30000);
//             selenium.waitForPageToLoad("30000");
         } catch (Throwable t) {
//             t.printStackTrace();
         }
//         selenium.type("frm:groupname", toName);
//         selenium.click("link=Save");
//         selenium.waitForPageToLoad("30000");

         //
         return selenium.isTextPresent("Group was successfully deleted.");
     }

}
