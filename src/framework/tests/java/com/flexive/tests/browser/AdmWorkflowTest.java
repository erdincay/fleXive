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
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

/**
 * Tests related to Workflows
 *
 * @author Laszlo Hernadi (laszlo.hernadi@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 462 $
 */
public class AdmWorkflowTest  extends AbstractBackendBrowserTest{
    private final static boolean [] SKIP_TEST_S = calcSkips();
    private static final Log LOG = LogFactory.getLog(AdmUserGroupTest.class);
    private final static String [] WORK_FLOWS = {"workflow01"};
    private final static String [] STEP_DEFINITIONS = {"sd01", "sd02", "sd10", "sd11", "sd12"};
    private final static String WORKFLOW_CREATE_PAGE = "/adm/main/workflow/create.jsf";
    private static final String WORKFLOW_OVERVIEW_PAGE = "/adm/main/workflow/overview.jsf";
    private final static String STEP_DEF_CREATE_PAGE = "/adm/main/workflow/stepDefinition/edit.jsf";        // edit.jsf == CREATE!!
    private final static String STEP_DEF_OVERVIEW_PAGE = "/adm/main/workflow/stepDefinition/overview.jsf";

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
     * try to delete the workflow and ignore if it fails
     * create a workflow
     * add a stetp to the created workflow
     * add a route from live to edit for group 1
     * try to add a route from live to live (fail)
     * add a rout from live to edit for group 2
     * try to create a workflow with the same name
     */
    @Test
    public void workflows_AW_1() {
        if (SKIP_TEST_S[0]){
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            try {
                deleteWorkflow(WORK_FLOWS[0], null, LOG);
            } catch (Throwable t){
                // ignore it...
            }
            Assert.assertTrue  (createWorkflow(WORK_FLOWS[0], "workflow01", true, LOG));
            Assert.assertTrue  (addStepToWorkflow(WORK_FLOWS[0], "Live", null, true, LOG));
            Assert.assertTrue  (addRouteToWorkflow(WORK_FLOWS[0], "Live", "Edit", "mand01: group01", true, LOG));
            Assert.assertTrue  (addRouteToWorkflow(WORK_FLOWS[0], "Live", "Live", "mand01: group02", false, LOG));
            Assert.assertTrue  (addRouteToWorkflow(WORK_FLOWS[0], "Live", "Edit", "mand01: group02", true, LOG));
            Assert.assertTrue  (createWorkflow(WORK_FLOWS[0], "workflow01", false, LOG));
        } catch (Throwable t){
            LOG.error(t.getMessage(),t);
            fail(t.getMessage(),t);
        } finally{
            logout();
        }
    }

    /**
     * clears the created stepdeffinitions
     * create a stepdefinition
     * rename the stepdefiniton
     * rename the stepdefintion back
     * create an other stepdefintion
     * add the stepdefinition to a workflow
     * remove the stepdefinition from the workflow
     * delete the stepdefinition
     */
    @Test(dependsOnMethods = {"workflows_AW_1"})
    public void workflows_AW_2() {
        if (SKIP_TEST_S[1]){
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            clearCreatedSDs();
            Assert.assertTrue (createStepdefinition(STEP_DEFINITIONS[0], "", true, LOG, STEP_DEFINITIONS[0]));
            Assert.assertTrue (editStepdefinition(STEP_DEFINITIONS[0], STEP_DEFINITIONS[1], null, true, LOG, STEP_DEFINITIONS[1]));
            Assert.assertTrue (editStepdefinition(STEP_DEFINITIONS[1], STEP_DEFINITIONS[0], null, true, LOG, STEP_DEFINITIONS[0]));
            Assert.assertTrue (createStepdefinition(STEP_DEFINITIONS[1], "", true, LOG, STEP_DEFINITIONS[1]));
            Assert.assertTrue (addStepToWorkflow(WORK_FLOWS[0], STEP_DEFINITIONS[1], null, true, LOG));
            Assert.assertTrue (deleteStepdefinition(STEP_DEFINITIONS[1], false, LOG));
            Assert.assertTrue (removeStepFromWorkflow(WORK_FLOWS[0], STEP_DEFINITIONS[1], true, LOG));
            Assert.assertTrue (deleteStepdefinition(STEP_DEFINITIONS[1], true, LOG));
        } catch (Throwable t){
            LOG.error(t.getMessage(),t);
            fail(t.getMessage(),t);
        } finally{
            logout();
        }
    }

    /**
     * clears the created stepdeffinitions
     * create 3 stepdefinitions
     * edit the stepdefinitions and trys to cycle dependences
     * delete the stepdefinitios
     */
    @Test(dependsOnMethods = {"workflows_AW_2"})
    public void workflows_AW_3() {
        if (SKIP_TEST_S[2]){
            skipMe();
            return;
        }
        try {
            loginSupervisor();
            clearCreatedSDs();
            for (int i = 2; i < 5; i++) {
                Assert.assertTrue (createStepdefinition(STEP_DEFINITIONS[i], "", true, LOG, STEP_DEFINITIONS[i]));
            }
            Assert.assertTrue (editStepdefinition(STEP_DEFINITIONS[2], null, STEP_DEFINITIONS[3], true, LOG));
            Assert.assertTrue (editStepdefinition(STEP_DEFINITIONS[3], null, STEP_DEFINITIONS[4], true, LOG));
            Assert.assertTrue (editStepdefinition(STEP_DEFINITIONS[4], null, STEP_DEFINITIONS[2], false, LOG));
            Assert.assertTrue (editStepdefinition(STEP_DEFINITIONS[4], null, STEP_DEFINITIONS[3], false, LOG));
            Assert.assertTrue (editStepdefinition(STEP_DEFINITIONS[4], null, STEP_DEFINITIONS[4], false, LOG));
            Assert.assertTrue (editStepdefinition(STEP_DEFINITIONS[4], null, "", true, LOG));
            for (int i = 2; i < 5; i++) {
                deleteStepdefinition(STEP_DEFINITIONS[i], null, LOG);
            }
        } catch (Throwable t){
            LOG.error(t.getMessage(),t);
            fail(t.getMessage(),t);
        } finally{
            logout();
        }
    }

    /**
     * clears the created stepdeffinitions
     */
    private void clearCreatedSDs() {
        for (String sd : STEP_DEFINITIONS) {
            try {
                deleteStepdefinition(sd, null, LOG);
            }   catch (RuntimeException re ){
                // if the step definition is not found, just ignore it
            }
        }
    }

    /**
     * remove Stepdefinitions from a workflow
     * @param workflowName the name of the workflow
     * @param stepName the name of the stepdefinition
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>expectedResult</code> when the workflow could be updated
     */
    private boolean removeStepFromWorkflow(String workflowName, String stepName, Boolean expectedResult, Log LOG) {
        loadContentPage(WORKFLOW_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),workflowName,":editButton_", 2));
        String htmlSRC = selenium.getHTMLSource("<legend>", "</legend>", "<table ", "</table>");

        int index = htmlSRC.indexOf("<legend>Steps</legend>");
        if (index < 0) {
            return false;       // TODO : throw exception...
        }
        index = Math.max(htmlSRC.indexOf("<table width=\"100%\">", index), htmlSRC.indexOf("<table width=100%>", index));
        int ende = htmlSRC.indexOf("</table>", index);
        clickAndWait(getButtonFromTRContainingName_(htmlSRC.substring(index, ende),stepName,":removeStep", 3));

        clickAndWait("link=Save workflow");
        sleep(300);
        return checkText("Updated workflow " + workflowName + ".", expectedResult, LOG);
    }

    /**
     * delete a stepdefinition
     * @param name name of the stepdefinition
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>expectedResult</code> if the stepdefintion is deleted
     */
    private boolean deleteStepdefinition(String name, Boolean expectedResult, Log LOG) {
        loadContentPage(STEP_DEF_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),name,":deleteButton_", 1));

        return checkText("Deleted step definition \"" + name + "\".", expectedResult, LOG);
    }

    /**
     * create a stepdefinition
     * @param name name of the stepdefinition
     * @param uniqueTarget the unique target of the stepdefinition
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @param desc the description array
     * @return <code>expectedResult</code> if the stepdefinition was created successfully
     */
    private boolean createStepdefinition(String name, String uniqueTarget, Boolean expectedResult, Log LOG, String ... desc){
        loadContentPage(STEP_DEF_CREATE_PAGE);
        selectFrame(Frame.Content);

        if (name == null) throw new RuntimeException("Name must not be null!!");
        fillInStepDef(name,uniqueTarget, desc);

        clickAndWait("link=Create step definition"); 

        return checkText("Created step definition \"" + name + "\".", expectedResult, LOG);
    }

    /**
     * edit a stepdefinition
     * @param name the name of the stepdefinition
     * @param newName the new name of the stepdefinition
     * @param uniqueTarget the new uniqueTarget of the stepdefinition
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @param desc the description array
     * @return <code>expectedResult</code> if the update was successfully
     */
    private boolean editStepdefinition(String name, String newName, String uniqueTarget, Boolean expectedResult, Log LOG, String ... desc){
        loadContentPage(STEP_DEF_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),name,":editButton_", 1));
        if (!fillInStepDef(newName, uniqueTarget, desc)) {
            try {
                return !expectedResult;
            } catch (NullPointerException npe) {
                return true;
            }
        }

        clickAndWait("link=Save step definition");

        return checkText("Updated step definition ", expectedResult, LOG);
    }

    /**
     * fill in the stepdefinitions
     * @param name if not <code>null</code> set the name of the open stepdefintion-edit window
     * @param uniqueTarget if not <code>null</code> set the unique target of the open stepdefintion-edit window
     * @param desc if not <code>null</code> set the descriptions of the open stepdefintion-edit window
     * @return <code>false</code>uniqueTarget not found <code>true</code>otherwise
     */
    private boolean fillInStepDef(String name, String uniqueTarget, String ... desc) {
        if (name != null) selenium.type("frm:stepDescription", name);
        if (desc != null) {
            for (int i = 0; i < desc.length; i++) {
                try {
                    selenium.type("frm:stepName_input_" + (i +1), desc[i]);
                } catch (NullPointerException npe){
                    // if the desc[i] is null we don't need to set it ;)
                }
            }
        }
        if (uniqueTarget != null) {
            if (uniqueTarget.length() == 0) {
                setCheckboxState("frm:uniqueTargetSelect", false);
            }
            else {
                setCheckboxState("frm:uniqueTargetSelect", true);
                sleep(200);
                try {
                selenium.select("frm:uniqueTarget", uniqueTarget);
                } catch (SeleniumException se) {
//                    if (se.getMessage().indexOf("Element frm:uniqueTarget not found") <0)
//                        LOG.error(se.getMessage(), se);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * delete a workflow
     * @param workflowName the name of the workflow to delete
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>expectedResult</code> if deleted successfully
     */
    private boolean deleteWorkflow(String workflowName, Boolean expectedResult, Log LOG){
        loadContentPage(WORKFLOW_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),workflowName,":deleteButton_", 2));

        return checkText("Deleted workflow " + workflowName + ".", expectedResult, LOG);
    }

    /**
     * add a route to a workflow
     * @param workflowName the workflow name
     * @param fromName the name to select at the from field
     * @param toName the name to select at the to field
     * @param userGroup the name to select at the group field
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>expectedResult</code> if the route is added
     */
    private boolean addRouteToWorkflow(String workflowName, String fromName, String toName,  String userGroup, Boolean expectedResult, Log LOG) {
        loadContentPage(WORKFLOW_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),workflowName,":editButton_", 2));

        if (fromName != null) selenium.select("frm:fromStepId", fromName);
        if (toName != null) selenium.select("frm:toStepId", toName);
        if (userGroup != null) selenium.select("frm:userGroup", userGroup);

        selenium.click("frm:addRoute");
        sleep(300);

        boolean result = checkText("Added route " + fromName + " - " + toName + " for user group", expectedResult, LOG);

        clickAndWait("link=Save workflow");
        sleep(300);
        return result;
    }

    /**
     * add a step to a workflwo
     * @param workflowName name of the workflow
     * @param label lable of the workflowstep
     * @param stepACL ACL-step to select
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>expectedResult</code> if updated successfully
     */
    private boolean addStepToWorkflow(String workflowName, String label, String stepACL, Boolean expectedResult, Log LOG) {
        loadContentPage(WORKFLOW_OVERVIEW_PAGE);
        selectFrame(Frame.Content);

        clickAndWait(getButtonFromTRContainingName_(getResultTable(null),workflowName,":editButton_", 2));

        if (label != null) selenium.select("frm:workflowStep", label);
        if (stepACL != null) selenium.select("frm:stepAcl", stepACL);

        selenium.click("frm:addStep");
        sleep(300);

        clickAndWait("link=Save workflow");
        return checkText("Updated workflow " + workflowName + ".", expectedResult, LOG);
    }

    /**
     * create a workflow
     * @param name the name of the workflow
     * @param desc the description of the workflow
     * @param expectedResult the expected result
     * @param LOG the Log to log
     * @return <code>expectedResult</code> if updated successfully
     */
    private boolean createWorkflow(String name, String desc, Boolean expectedResult, Log LOG) {
        loadContentPage(WORKFLOW_CREATE_PAGE);
        selectFrame(Frame.Content);
        selenium.type("frm:workflowName", name);
        selenium.type("frm:workflowDescription", desc);

        selenium.click("link=Create workflow");

        boolean returnCode =  checkText("Created workflow " + name + ".", expectedResult, LOG);
        clickAndWait("link=Cancel");


        return returnCode;
    }
}

