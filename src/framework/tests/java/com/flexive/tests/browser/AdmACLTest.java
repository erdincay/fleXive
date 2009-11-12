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
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.Assert;

/**
 * Tests related to ACLs
 *
 * @author Laszlo Hernadi (laszlo.hernadi@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 462 $
 */
public class AdmACLTest  extends AbstractBackendBrowserTest {

    private final static String ACL_CREATE_PAGE = "/adm/main/acl/aclCreate.jsf";
    private static final Log LOG = LogFactory.getLog(AdmACLTest.class);
    private final static boolean [] SKIP_TEST_S = calcSkips();

    private final static String[] ACL_GROUPS = {"mand01: group01", "mand01: group02", "mand02: group03"};

    private final static String TEST_ACL_NAME = "ACL_en";

    /**
     * create the need groups
     */
    @BeforeClass
    public void beforeClass() {
        super.beforeClass();
        createGroup();
    }

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
     * trys to delete the ACL
     * creates the ACL, and checks if the label is correct
     * adds to the ACL a german description and set the default language to german
     * checks if the label is now german
     * sets the defaultlanguage back to english and test if the label is english
     * trys to delete the ACL again
     */
    @Test(dependsOnMethods = {"acl_AACL_3"})
    public void acl_AACL_1() {
        if (SKIP_TEST_S[0]){
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            try {deleteACL(TEST_ACL_NAME, null);}catch (RuntimeException re) {/* if the user don't exists...*/}
            Assert.assertTrue (createACL(TEST_ACL_NAME, "ACL_en", "ACL_en desc", true));
            Assert.assertTrue (getACL_Label(TEST_ACL_NAME).equals("ACL_en"));
            Assert.assertTrue (editACL(TEST_ACL_NAME, null, "ACL_de", null, null, null, 2, true));
            Assert.assertTrue (getACL_Label(TEST_ACL_NAME).equals("ACL_de"));
            Assert.assertTrue (editACL(TEST_ACL_NAME, null, null, null, null, null, 1, true));
            Assert.assertTrue (getACL_Label(TEST_ACL_NAME).equals("ACL_en"));
        } finally {
            try {deleteACL(TEST_ACL_NAME, null);}catch(RuntimeException re){/* if the user don't exists...*/}
            logout();
        }
    }

    /**
     * creates an ACL
     * adds the 3 ACL permissions
     * deletes the ACL afterwards
     */
    @Test(dependsOnMethods = {"acl_AACL_1"})
    public void acl_AACL_2() {
        if (SKIP_TEST_S[1]){
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            Assert.assertTrue (createACL(TEST_ACL_NAME, "ACL_en", "ACL_en desc", true));
            for (int i = 0; i < 3; i++) {
                Assert.assertTrue (addACLPermission(TEST_ACL_NAME, ACL_GROUPS[i], true, true, false, false, false, false, true, LOG));
            }
        } finally {
            try {deleteACL(TEST_ACL_NAME, null);}catch(RuntimeException re){/* if the user don't exists...*/}
            logout();
        }
    }

    /**
     * Delete-test:
     * creates an ACL and deletes it afterwards (must be succesfull)
     */
    @Test()
    public void acl_AACL_3() {
        if (SKIP_TEST_S[2]){
            skipMe();
            return;
        }
        try{
            loginSupervisor();
            Assert.assertTrue (createACL(TEST_ACL_NAME, "ACL_en", "ACL_en desc", true));
            Assert.assertTrue (deleteACL(TEST_ACL_NAME, true));
        } finally {
            logout();
        }
    }

    private boolean createACL(String name, String label_en, String desc, Boolean expectedResult) {
        return createACL(name, label_en, desc, expectedResult, LOG);
    }

    /**
     * creates an ACL
     *
     * @param name name of the ACL must not be <code>null</code>
     * @param label_en the label in english must not be <code>null</code>
     * @param desc the description can be <code>null</code> then it is ignored
     * @param expectedResult @see AbstractBackendBrowserTest#checkText
     * @param LOG the Log where to log
     * @return <code>true</code> if result is expected
     */
    private boolean createACL(String name, String label_en, String desc, Boolean expectedResult, Log LOG) {
        loadContentPage(ACL_CREATE_PAGE);
        selectFrame(Frame.Content);

        sleep(500);

        selenium.type("frm:name", name);
        selenium.type("frm:label_input_1", label_en);
        if (desc != null) selenium.type("frm:description", desc);

        clickAndWait("link=Create");

        sleep(400);

        return checkText("Access control list '" + name + "' was created successfully.", expectedResult, LOG);
    }

    /**
     * Encapsules the Log parameter
     *
     * @see com.flexive.tests.browser.AdmACLTest#editACL(String, String, String, String, String, String, Integer, Boolean, org.apache.commons.logging.Log)
     * edit values of an ACL
     *
     * @param name name of ACL to edit
     * @param label_en the label in english can be <code>null</code> then it is ignored
     * @param label_de the label in german can be <code>null</code> then it is ignored
     * @param label_3 the label in the 3. lang can be <code>null</code> then it is ignored
     * @param label_4 the label in the 4. lang can be <code>null</code> then it is ignored
     * @param desc the description can be <code>null</code> then it is ignored
     * @param selectLang which language to choose can be <code>null</code> then it is ignored
     * @param expectedResult @see AbstractBackendBrowserTest#checkText
     * @return <code>true</code> if result is expected
     */
    private boolean editACL(String name, String label_en, String label_de, String label_3, String label_4, String desc, Integer selectLang, Boolean expectedResult) {
        return editACL(name, label_en, label_de, label_3, label_4, desc, selectLang, expectedResult, LOG);
    }


    private boolean deleteACL(String name, Boolean expectedResult) {
        return deleteACL(name,expectedResult, LOG);
    }

    /**
     * Deletes an ACL
     * @param name name of ACL to delete, must not be <code>null</code>
     * @param expectedResult @see AbstractBackendBrowserTest#checkText
     * @param LOG the Log where to log
     * @return <code>true</code> if result is expected
     */
    private boolean deleteACL(String name, Boolean expectedResult, Log LOG) {
        loadContentPage(ACL_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),name,":deleteButton_"));

        return checkText("Access control list successfully deleted.", expectedResult, LOG);
//        return true;
    }

    /**
     * edit values of an ACL
     *
     * @param name name of ACL to edit
     * @param label_en the label in english can be <code>null</code> then it is ignored
     * @param label_de the label in german can be <code>null</code> then it is ignored
     * @param label_3 the label in the 3. lang can be <code>null</code> then it is ignored
     * @param label_4 the label in the 4. lang can be <code>null</code> then it is ignored
     * @param desc the description can be <code>null</code> then it is ignored
     * @param selectLang which language to choose can be <code>null</code> then it is ignored
     * @param expectedResult @see AbstractBackendBrowserTest#checkText
     * @param LOG the Log where to log
     * @return <code>true</code> if result is expected
     */
    private boolean editACL(String name, String label_en, String label_de, String label_3, String label_4, String desc, Integer selectLang, Boolean expectedResult, Log LOG) {
        loadContentPage(ACL_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),name,":editButton_"));
//        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),name,":deleteButton_"));
        if (label_en != null) selenium.type("frm:label_input_1", label_en);
        if (label_de != null) selenium.type("frm:label_input_2", label_de);
        if (label_3 != null) selenium.type("frm:label_input_3", label_3);
        if (label_4 != null) selenium.type("frm:label_input_4", label_4);
        if (desc != null) selenium.type("frm:description", desc);

        ////input[@name='frm:label_defaultLanguage' and @value='2']

        selenium.select("frm:label_languageSelect", "All");

        if (selectLang != null) {
            
            for (int tmp = 1; tmp <= 4; tmp++){
                setRadioButton("name=frm:label_defaultLanguage value=" + tmp, tmp == selectLang);
            }
//            selenium.check("name=frm:label_defaultLanguage value=" + selectLang);

        }

        clickAndWait("link=Save");

        return checkText("Access control list '" + name + "' was saved successfully.", expectedResult, LOG);
    }

    /**
     * set a given radiobutton to a given value
     * @param name the name of the radiobutton
     * @param value the value to set
     */
    private void setRadioButton(String name, boolean value) {
        if (selenium.isChecked(name) ^ value) {
            selenium.click(name);
        }
    }

    /**
     * gets the label of an ACL
     * @param name name of ACL
     * @return the label of ACL 
     */
    private String getACL_Label(String name) {
        loadContentPage(ACL_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        String td;
        String tr;
        tr = getButtonFromTRContainingName_(getResultTable(null), name, null);
        try {
            td = "<td " + tr.split("<td")[4];
        } catch (Throwable t) {
            return null;
        }

//        LOG.info("td : \"" + td + "\"");
        td = td.replaceAll("<[/]*[\\w\\s=\\\"-]*>", "").trim();

//        LOG.info("td : \"" + td + "\"");
        // Sometimes we are "to fast" and get only the td content without the div but with tailing \n + spaces
        if (td.startsWith("<td ")) return td.substring(4).trim();

        return td;
    }
}
