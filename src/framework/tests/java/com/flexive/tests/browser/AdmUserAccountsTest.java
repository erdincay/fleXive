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

import com.thoughtworks.selenium.SeleniumException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.testng.Assert.fail;

/**
 * Tests related to User Accounts
 *
 * @author Laszlo Hernadi (laszlo.hernadi@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 462 $
 */
public class AdmUserAccountsTest extends AbstractBackendBrowserTest {

    private final static String USER_ACCOUNT_OVERVIEW_PAGE = "/adm/main/account/overview.jsf";
    private final static String USER_ACCOUNT_CREATE_PAGE = "/adm/main/account/create.jsf";
    private static final Log LOG = LogFactory.getLog(AdmUserGroupTest.class);
    private final static String EDIT_TYPE = "adm/structure/typeEditor.jsf";
    private final static boolean[] SKIP_TEST_S = calcSkips();

    private final static String [] MANDATORS = {"mand01", "mand02"};
    private final static String [] USER_GROUPS = {"group01"};
    private final static String [] USERS = {"user01", "user02", "user03"};

    /**
     * build the skip array, an array in which every test-method have an entry which
     * indicates if a method should be skiped
     * @return the skip-array
     */
    private static boolean[] calcSkips() {
        boolean[] skipList = new boolean[6];
        for (int i = 0; i < skipList.length; i++) {
            skipList[i] = !AbstractBackendBrowserTest.isForceAll();
        }
//        skipList[4] = false;
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
     * create the needed groups
     */
    @BeforeClass
    @Override
    public void beforeClass () {
        super.beforeClass();
        createGroup();
    }


    private final static UserAcc[] TEST_USER = {new UserAcc(USERS[0], "aaaaaa", MANDATORS[0]),
            new UserAcc(USERS[1], "abcabc", MANDATORS[0]),
            new UserAcc(USERS[2], "bcaabc", MANDATORS[1])};

    /**
     * try to delete the User
     * try to create the user with blank password (fail)
     * try to create the user with not equal passwords (fail)
     * create the user with right data
     * try to create the user with a short password
     * try to create the user with an existing name to the same mandator
     * try to create the user with an existing name to an other mandator
     */
    @Test
    public void userAccounts_AUA_1() {
        if (SKIP_TEST_S[0]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            try {
                deleteUser(USERS[0], null, LOG);
            } catch (RuntimeException re) {/* don't care... */}
            Assert.assertTrue  (createUser(USERS[0], "", "", MANDATORS[0], "abc", false, false, false));
            Assert.assertTrue  (createUser(USERS[0], "aaaaaa", "bbbbbb", MANDATORS[0], "abc", false, false, false));
            Assert.assertTrue  (createUser(TEST_USER[0], "abc@abc.com", false, false, true));
            Assert.assertTrue  (createUser(USERS[1], "aaaaa", "aaaaa", MANDATORS[0], "abc@abc.com", false, false, false));
            Assert.assertTrue  (createUser(USERS[0], "aaaaaa", "aaaaaa", MANDATORS[0], "abc@abc.com", false, false, false));
            Assert.assertTrue  (createUser(USERS[0], "aaaaaa", "aaaaaa", MANDATORS[1], "abc@abc.com", false, false, false));
        } catch (RuntimeException re) {
            fail(re.getMessage(), re);
        } finally {
            logout();
        }
    }

    /**
     * try to edit the created user but shouldn't be visible
     * test the possible combinations of validiated + activated
     * delete the user
     */
    @Test(dependsOnMethods = {"userAccounts_AUA_1"})
    public void userAccounts_AUA_2() {
        if (SKIP_TEST_S[1]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            int curFlag = 0, nextFlag = 1;
            try {
                editUser(TEST_USER[0].name, true, true, true, true, null, null, null, null, false);
                fail("User should not be visible!!!");
            } catch (RuntimeException re) {
                // OK
            }
            while (curFlag < 4) {
                Assert.assertTrue  (editUser(TEST_USER[0].name, ((curFlag & 2) > 0), ((curFlag & 1) > 0), ((nextFlag & 2) > 0), ((nextFlag & 1) > 0), null, null, null, null, true));
                curFlag = nextFlag++;   // inc both
            }
            Assert.assertTrue  (deleteUser(TEST_USER[0].name, true, LOG));
        } catch (RuntimeException re) {
            LOG.error(re.getMessage(), re);
            fail(re.getMessage());
        } finally {
            logout();
        }
    }

    /**
     * try to delete the user and ignore it if not working
     * create 2 user
     */
    @Test(dependsOnMethods = {"userAccounts_AUA_2"})
    public void userAccounts_AUA_3() {
        if (SKIP_TEST_S[2]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            try {
             deleteUser(USERS[1], null, LOG);
            } catch (Throwable t) {/*ignore it...*/}
            try {
               deleteUser(USERS[2], null, LOG);
            } catch (Throwable t) {/*ignore it...*/}
            Assert.assertTrue  (createUser(TEST_USER[1], "abc@abc.com", true, true, true));
            Assert.assertTrue  (createUser(TEST_USER[2], "abc@abc.com", true, true, true));
        } finally {
            logout();
        }
    }

    /**
     * set grant an user backend-acces and test what he could see
     */
    @Test(dependsOnMethods = {"userAccounts_AUA_3"})
    public void userAccounts_AUA_4() {
        if (SKIP_TEST_S[3]) {
            skipMe();
            return;
        }
        ArrayList<String> errors = new ArrayList<String>();
        ArrayList<String> goods = new ArrayList<String>();
        boolean allOK = true;
        try {
            loginSupervisor();
            Assert.assertTrue  (editUser(USERS[1], true, true, true, "Backend Access"));
            logout();
            login(TEST_USER[1]);

//            errors.add("..::ADMIN::..");
            // admin tab
            navigateTo(NavigationTab.Administration);
            allOK &= !testIfNavigationTabTextPresent("Accounts", goods);
            allOK &= !testIfNavigationTabTextPresent("User groups", goods);
            allOK &= !testIfNavigationTabTextPresent("Mandators", goods);
            allOK &= !testIfNavigationTabTextPresent("Access control lists", goods);
            allOK &= !testIfNavigationTabTextPresent("Workflows", goods);
            allOK &= !testIfNavigationTabTextPresent("Scripts", goods);
            allOK &= testIfNavigationTabTextPresent("Selectlists", errors);
            allOK &= !testIfNavigationTabTextPresent("Import / Export", goods);
            allOK &= testIfNavigationTabTextPresent("System", errors);
//            testIfNavigationTabTextPresent("");

            // structure tab
//            errors.add("..::STRUCTURE::..");
            navigateTo(NavigationTab.Structure);
            allOK &= testIfNavigationTabTextPresent("ROOT", errors);
            allOK &= testIfNavigationTabTextPresent("ACL", errors);
            allOK &= testIfNavigationTabTextPresent("Workflow Step", errors);

            // content tab
//            errors.add("..::CONTENT::..");
            navigateTo(NavigationTab.Content);
            allOK &= testIfNavigationTabTextPresent("Root [", errors);

        } finally {
            logout();
            if (!allOK) {
                for (String s : errors) {
                    if (s.startsWith("-")) {
                        LOG.error(s.substring(1));
                    } else {
                        LOG.info(s.substring(1));
                    }
                }
                for (String s : goods) {
                    if (!s.startsWith("-")) {
                        LOG.error(s.substring(1));
                    } else {
                        LOG.info(s.substring(1));
                    }
                }
                fail();
            }
        }
    }

    /**
     * to login with an userAccount
     * @param userAcc the useraccount to login with
     */
    private void login(UserAcc userAcc) {
        login(userAcc.name, userAcc.pass);
    }

    /**
     * add some ACL permissions
     * edit the user and put him in the group01
     * grant the group backend-access
     * remove image permissions
     * login with the other user
     * shold test the upload of pictures, but can't really upload with javascript
     */
    @Test(dependsOnMethods = {"userAccounts_AUA_4"})
    public void userAccounts_AUA_5() {
        if (SKIP_TEST_S[4]) {
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            addACLPermission("Default Workflow ACL", generateGroupName(MANDATORS[0], USER_GROUPS[0]), true, true, false, false, false, false, true, LOG);
            addACLPermission("Default Structure ACL", generateGroupName(MANDATORS[0], USER_GROUPS[0]), true, true, false, false, false, false, true, LOG);
            editUser(TEST_USER[1].name, true, true, null, null, null, null, null, new String[]{USER_GROUPS[0]}, true, "");
            editUserGroup(USER_GROUPS[0], true, LOG, "Backend Access");
            removeImagePermissions();
            logout();
            login(TEST_USER[1]);

//            File f = new File("spcgray.gif");
//            LOG.info(f.exists());
//            uploadImage(f);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
            fail(t.getMessage());
        } finally {
            logout();
        }
    }
//
//    private void uploadImage(File img) {
//        loadContentPage("adm/content.jsf");
//        selenium.select("searchForm:type", "Image");
//        clickAndWait("searchForm:createContentButton");
//        String htmlSrc = selenium.getHtmlSource();
////
////        for (String name : selenium.getAllWindowNames()) {
////            LOG.info("Window names : " + name);
//////            selenium.getAllButtons()
////        }
//
//
//        // TODO : can't upload files with javaScript so selenium won't work, so we don't need to find out the name of the file upload box ;)
//
//        File f = new File("Content@" + System.currentTimeMillis() + ".html");
//        FileOutputStream fos = null;
//        try {
//            fos = new FileOutputStream(f);
//            fos.write(htmlSrc.getBytes());
//            fos.flush();
//            fos.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        } catch (IOException e) {
//            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
//        }
//        LOG.info(htmlSrc.length() + "\t" + f.getAbsolutePath());
//
//    }

    /**
     * remove image permissions
     */
    private void removeImagePermissions() {
        navigateTo(NavigationTab.Structure);
        String htmlSrc = selenium.getHtmlSource();
//        File f = new File("Navi@" + System.currentTimeMillis() + ".html");
//        FileOutputStream fos = new FileOutputStream(f);
//        fos.write(htmlSrc.getBytes());
//        fos.flush();
//        fos.close();
//        LOG.info(htmlSrc.length() + "\t" + f.getAbsolutePath());
        openEdit(htmlSrc, "{\"title\":'Image',\"nodeDocType\":'Type");
        // http://localhost:8080/flexive/adm/structure/typeEditor.jsf?action=editInstance&id=2
        htmlSrc = selenium.getHtmlSource();
        int index = htmlSrc.indexOf("<legend>Permissions</legend>");
        int ende = htmlSrc.indexOf("/fieldset>", index);
        htmlSrc = htmlSrc.substring(index, ende);
        String[] cbS = htmlSrc.split("<input ");
        String tmpID;
        for (int i = 1; i < cbS.length; i++) {
//            index = cbS[i].indexOf("id=\"") + 4;
//            ende = cbS[i].indexOf("\"", index);
//            tmpID = cbS[i].substring(index, ende);
            tmpID = getID(cbS[i]);
            setCheckboxState(tmpID, tmpID.equals("frm:useTypePermissions"));
        }
        clickAndWait("link=Save");

        checkText("Saved changes for type Image.", true, LOG);
    }

    /**
     * open the edit screen of a structure element
     * @param src the html source
     * @param prefix the prefix to search
     */
    private void openEdit(String src, String prefix) {
        Hashtable<String, String> params = buildHashtableFromMenu(src, prefix);
        if (params.get("nodeType").equals("Type")) {
            loadContentPage(EDIT_TYPE + "?action=editInstance&id=" + params.get("typeId"));
        }
    }

    /*
            //should be invoked from the nodes in the context menu
            function doContentActionFromContextMenu(node, defaultAction) {
                if ("Assignment" == node.nodeType || "AssignmentSystemInternal" == node.nodeType) {
                    invokeContentAction("adm/structure/propertyAssignmentEditor.jsf", defaultAction, {id: node["assignmentId"]});
                }
                else if (isType(node.nodeType)) {
                    invokeContentAction("adm/structure/typeEditor.jsf", defaultAction, {id: node["typeId"]});
                }
                else if ("Group" == node.nodeType) {
                    invokeContentAction("adm/structure/groupAssignmentEditor.jsf", defaultAction, {id: node["assignmentId"]});
                }
            }       

            function editNode(menuItem) {
                var menu = dojo.widget.byId("structureTreeMenu");
                doContentActionFromContextMenu(menu.getTreeNode(), "editInstance");
            }

            {"title":'Image',"nodeDocType":'Type_2',"alias":'IMAGE',"nodeType":'Type',"typeId":'2',"children":[

     */

    /**
     * various login tests using bad passwords and sql-injections
     */
    public void userAccounts_AUA_6() {
        if (SKIP_TEST_S[5]) {
            skipMe();
            return;
        }
        try {
            Assert.assertTrue  (!login(USERS[1], "xxxxxx"));
            Assert.assertTrue  (!login(USERS[1], "xxx"));
            Assert.assertTrue  (!login(USERS[1], "cbaabc"));
            Assert.assertTrue  (!login("supervisor", "'or 1=1 //"));
        } catch (Throwable t) {
            LOG.error(t.getMessage() + t);
            fail(t.getMessage());
//        } finally {
//            logout();
        }
    }


    private boolean editUser(String loginName, boolean listActive, boolean listValidated, Boolean expectedResult, String... roles) {
        return editUser(loginName, listActive, listValidated, null, null, null, null, null, null, expectedResult, roles);
    }

    /**
     * @param loginName      the Loginname of the user to search
     * @param listActive     search in actives only
     * @param listValidated  search in validated only
     * @param active         if not <code>null</code> overwrite the current active value
     * @param validated      if not <code>null<code> overwrite the current validated value
     * @param pass1          if not <code>null</code> the password to set (needs pass2 also)
     * @param pass2          if not <code>null</code> the password to set (should be same as pass1)
     * @param mailAddr       if not <code>null</code> the email Address to set
     * @param groups         if not <code>null</code> the groups to select if <code>""</code> then all groups will be removed (from user)
     * @param expectedResult the expected result could be <code>null</code> to don't care or true / false what ever expected (when expecting false, then it will return true when false (so the opposite))
     * @param roles          if not <code>null</code> the roles to select if <code>""</code> then all roles will be removed (from user)
     * @return true if the result is expected (see expectedResult)
     * @see com.flexive.tests.browser.AbstractBackendBrowserTest#markInSelectList(String, String[])
     */
    private boolean editUser(String loginName, boolean listActive, boolean listValidated, Boolean active, Boolean validated, String pass1, String pass2, String mailAddr, String[] groups, Boolean expectedResult, String... roles) {
        String table = getOverviewTable(listActive, listValidated);

        try {
            clickAndWait(getButtonFromTRContainingName_(table, loginName, ":editButton_"), 30000);
        } catch (NullPointerException npe) {
            throw new RuntimeException("User \"" + loginName + "\" not found!");
        }
        if (active != null) setCheckboxState("frm:active", active);
        if (validated != null) setCheckboxState("frm:validated", validated);
        if (pass1 != null) selenium.type("frm:password", pass1);
        if (pass2 != null) selenium.type("frm:passwordConfirm", pass2);
        if (mailAddr != null) selenium.type("frm:email", mailAddr);
        markInSelectList("frm:roles", roles);
        markInSelectList("frm:groups", groups);
        clickAndWait("link=Save");


        return checkText("The Account was saved successfully.", expectedResult, LOG);
    }

    /**
     * get the overview table of the user account
     * @param listActive shall list active only
     * @param listValidated shall list validiated only
     * @return the overview table
     */
    private String getOverviewTable(boolean listActive, boolean listValidated) {
        loadContentPage(USER_ACCOUNT_OVERVIEW_PAGE);
        selectFrame(Frame.Content);
        setCheckboxState("listForm:activeFilter", listActive);
        waitForPageAndIgnore(2000);
        setCheckboxState("listForm:validatedFilter", listValidated);
        waitForPageAndIgnore(2000);
//        selenium.select("yui-pg0-0-rpp", "100");
//        selenium.waitForPageToLoad("2000");

        return getResultTable(null);
    }

    /**
     * waits for a page to load, but ignore the SeleniumException
     * @param ms the ms to wait 
     */
    private void waitForPageAndIgnore(int ms) {
        try {
            selenium.waitForPageToLoad("" + ms);
        } catch (SeleniumException se) {
            // ignore it...
        }
    }

    private boolean createUser(UserAcc userAcc, String mailAddr, boolean active, boolean validated, Boolean expectedResult) {
        return createUser(userAcc.name, userAcc.pass, userAcc.pass, userAcc.mand, mailAddr, active, validated, expectedResult);
    }

    /**
     * create a user
     * @param name the name of the user
     * @param pass1 the pass of the user
     * @param pass2 the pass-again of the user
     * @param mandatorName the name of the mandator
     * @param mailAddr email address
     * @param active if the user should be active
     * @param validated if the user should be created validated
     * @param expectedResult the expected result
     * @return <code>expectedResult<code> if the account is created successfully
     */
    private boolean createUser(String name, String pass1, String pass2, String mandatorName, String mailAddr, boolean active, boolean validated, Boolean expectedResult) {
        loadContentPage(USER_ACCOUNT_CREATE_PAGE);
//        clickAndWait("link=create");
        selenium.waitForPageToLoad("8000");
        selenium.select("frm:mandator", mandatorName);
        selenium.type("frm:loginName", name);
        selenium.type("frm:name", name);
        selenium.type("frm:password", pass1);
        selenium.type("frm:passwordConfirm", pass2);
        selenium.type("frm:email", mailAddr);
        setCheckboxState("frm:active", active);
        setCheckboxState("frm:validated", validated);
        clickAndWait("link=Create");

        //

        return checkText("The Account was saved successfully", expectedResult, LOG);
    }

    /**
     * delete an account
     * @param loginName the login name
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>expectedResult</code> if the account was deleted successfully
     */
    private boolean deleteUser(String loginName, Boolean expectedResult, Log LOG) {
        String table = getOverviewTable(false, false);

        clickAndWait(getButtonFromTRContainingName_(table, loginName, ":deleteButton_"), 30000);
        return checkText("The Account was deleted successfully.", expectedResult, LOG);
    }

    /**
     * a holder class for an account
     */
    private static class UserAcc {
        private String name;
        private String pass;
        private String mand;

        private UserAcc(String name, String pass, String mand) {
            this.name = name;
            this.pass = pass;
            this.mand = mand;
        }
    }
}
