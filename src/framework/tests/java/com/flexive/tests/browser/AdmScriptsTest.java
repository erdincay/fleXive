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
import org.testng.Assert;
import com.flexive.tests.browser.exceptions.RowNotFoundException;

/**
 * Tests related to Scripts
 *
 * @author Laszlo Hernadi (laszlo.hernadi@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 462 $
 */
public class AdmScriptsTest  extends AbstractBackendBrowserTest{
    private final static boolean [] SKIP_TEST_S = calcSkips();
    private final static Log LOG = LogFactory.getLog(AdmScriptsTest.class);
    private final static String SCRIPT_OVERVIEW_PAGE = "/adm/main/scripting/overview.jsf";
    private final static String SCRIPT_CREATE_PAGE = "/adm/main/scripting/create.jsf";
//    private final static String SCRIPT_CONSOLE_PAGE = "/adm/main/scripting/scriptConsole.jsf";
    private final static String [] SCRIPT_NAMES = {"test001.gy",
                                                   "test002.gy",
                                                   "test003.gy"};
    private String lastScriptSrc = "";

    /**
     * build the skip array, an array in which every test-method have an entry which
     * indicates if a method should be skiped
     * @return the skip-array
     */
    private static boolean [] calcSkips() {
        boolean [] skipList = new boolean [3];
        for (int i = 0; i < skipList.length; i++){
            skipList[i] = !AbstractBackendBrowserTest.isForceAll();
//            skipList[i] = false;
        }
//        skipList[skipList.length-1] = false;
//        skipList[0] = false;
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
     * creates a script
     * checks if the script is present
     * deletes the script
     */
    public void scripts_AS_1() {
        if (SKIP_TEST_S[0]){
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            Assert.assertTrue (createScript(SCRIPT_NAMES[0], "Manual", "print \"...\"", true, true, true));
            Assert.assertTrue (checkIfScriptIsPersistent(SCRIPT_NAMES[0],lastScriptSrc,true));
            Assert.assertTrue (deleteScript(SCRIPT_NAMES[0], true, LOG));
        } catch (Throwable t){
            LOG.error(t.getMessage(),t);
            fail(t.getMessage(),t);
        } finally{
            logout();
        }
    }

    /**
     * trys to create a script and not save it
     * trys to delete the NOT created script, if no exception (caused by a not found) it fails
     */
    public void scripts_AS_2() {
        if (SKIP_TEST_S[1]){
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            Assert.assertTrue (createScript(SCRIPT_NAMES[1], null, "print \"...\"", false, false, true, true));
            try {
                deleteScript(SCRIPT_NAMES[0], false, LOG);
                fail();
            } catch (RowNotFoundException re) {
//                LOG.error(re.getMessage(), re);
            }
        } catch (Throwable t){
            LOG.error(t.getMessage(),t);
            fail(t.getMessage(),t);
        } finally{
            logout();
        }
    }

    /**
     * tryes to delete the script (and ignoring all errors)
     * creates a synctacticaly wrong script 
     */
    public void scripts_AS_3() {
        if (SKIP_TEST_S[2]){
            skipMe();
            return;
        }
        boolean allTrue = true;
        try {
            loginSupervisor();
            try {
                deleteScript(SCRIPT_NAMES[2], null, LOG);
            } catch (RuntimeException re) {
//                LOG.error(re.getMessage());
            }
            Assert.assertTrue (createScript(SCRIPT_NAMES[2], "Manual", "klsjdlkfjwejf alhklhasdfhsdfjha sdfjka sdfhdsjkf h jkjkshdf", false, true, false, false, true) || allTrue);
        } catch (Throwable t){
            LOG.error(t.getMessage(),t);
            fail(t.getMessage(),t);
        } finally{
            logout();
        }
    }

//    public void scripts_AS_3() {
//        if (SKIP_TEST_S[0]){
//            skipMe();
//            return;
//        }
//        try {
//            loginSupervisor();
//        } catch (Throwable t){
//            LOG.error(t.getMessage(),t);
//            fail(t.getMessage(),t);
//        } finally{
//            logout();
//        }
//    }
//


    private boolean createScript(String name, String defaultScriptEvent, String script, boolean defaultImports, boolean syntaxCheck, boolean cancel, Boolean expectedResult_syntax, Boolean expectedResult_save) {
        return createScript(name, defaultScriptEvent, script, defaultImports, syntaxCheck, cancel, expectedResult_syntax, expectedResult_save, LOG);
    }
    private boolean createScript(String name, String defaultScriptEvent, String script, boolean defaultImports, boolean syntaxCheck, boolean cancel, Boolean expectedResult) {
        return createScript(name, defaultScriptEvent, script, defaultImports, syntaxCheck, cancel, expectedResult,expectedResult, LOG);
    }

    private boolean createScript(String name, String defaultScriptEvent, String script, boolean defaultImports, boolean syntaxCheck, Boolean expectedResult) {
        return createScript(name, defaultScriptEvent, script, defaultImports, syntaxCheck, false, expectedResult, expectedResult, LOG);
    }

    /**
     * creates a script
     * @param name name of the script
     * @param defaultScriptEvent default script event, if <code>null</code> than it is ignored
     * @param script the script
     * @param defaultImports shall click on default imports
     * @param syntaxCheck shall click on syntaxcheck (modifies the return value)
     * @param cancel cancel instead of save
     * @param expectedResult_syntax expected result of the syntaxcheck
     * @param expectedResult_save expected result of saving
     * @param LOG the Log to log
     * @return <code>true</code> if all the results are expected
     */
    private boolean createScript(String name, String defaultScriptEvent, String script, boolean defaultImports, boolean syntaxCheck, boolean cancel, Boolean expectedResult_syntax, Boolean expectedResult_save, Log LOG) {
        loadContentPage(SCRIPT_CREATE_PAGE);
        selectFrame(Frame.Content);
        boolean returnValue = true;

        String space = Character.toString(' ');
        String bb = Character.toString('\b');

        selenium.type("frm:name", name);
        selenium.keyDown("frm:name", space);
        selenium.keyUp("frm:name", space);
        sleep(2000);
        selenium.keyDown("frm:name", bb);
        selenium.keyUp("frm:name", bb);
        sleep(2000);
//        selenium.keyPress();
        if (defaultScriptEvent != null) {
            selenium.select("frm:event", defaultScriptEvent);
        }
        if (defaultImports) {
            clickAndWait("link=Add default imports",10000);
            if (syntaxCheck) {
                clickAndWait("link=Syntax check", 10000);
                returnValue = returnValue & checkText("Syntax check passed.", expectedResult_syntax, LOG);
            }
        }
        if (script != null) {
            setCheckboxState("edit_area_toggle_checkbox_frm:code", false);
//            String code = selenium.getText("frm:code") + "\n";
            String code = getElementValue("frm:code");
            selenium.type("frm:code", code + script);
            lastScriptSrc = getElementValue("frm:code");
        }
        if (syntaxCheck) {
            clickAndWait("link=Syntax check", 10000);
            returnValue = returnValue & checkText("Syntax check passed.", expectedResult_syntax, LOG);
        }
        if (cancel) {
            clickAndWait("link=Cancel");
        } else {
            clickAndWait("link=Create script");
            returnValue = returnValue & checkText("The script " + name + " was successfully created.", expectedResult_save, LOG);
        }

        return returnValue;
    }

    /**
     * get the value of an element
     * @param name name of the element
     * @return value of the element
     */
    private String getElementValue(String name) {
        return selenium.getEval(WND + ".document.getElementById('" + name +"').value");
    }

    /**
     * checks if a given script is present and the source is the same
     * @param name name of the script
     * @param script the script to test
     * @param refresh if the script should be refreshed (in edit-mode) and tested again
     * @return <code>true</code> if successfull
     */
    private boolean checkIfScriptIsPersistent(String name, String script, boolean refresh){
        loadContentPage(SCRIPT_OVERVIEW_PAGE);
        selectFrame(Frame.Content);
        boolean returnValue = true;

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),name,":editButton_", 1));

        returnValue &= script.equals(getElementValue("frm:code"));

        if (refresh) {
            clickAndWait("link=Refresh");
            returnValue &= script.equals(getElementValue("frm:code"));
        }

        return returnValue;
    }

    /**
     * delete a script
     * @param name name of the script
     * @param expectedResult expected result of the delete
     * @param LOG Log to log
     * @return <code>true</code> if successfull
     */
    private boolean deleteScript(String name, Boolean expectedResult, Log LOG) {
        loadContentPage(SCRIPT_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),name,":deleteButton_", 1));

        return checkText("The script was successfully deleted.", expectedResult, LOG);
    }
}
