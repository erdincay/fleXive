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
package com.flexive.tests.embedded;

import com.flexive.shared.*;
import static com.flexive.shared.CacheAdmin.getEnvironment;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.*;
import static com.flexive.tests.embedded.FxTestUtils.*;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Workflow tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "workflow"})
public class WorkflowTest {

    private static final String WORKFLOW_NAME = "Cactus test workflow";
    private static final String WORKFLOW_DESCRIPTION = "Test description";
    private static final String STEPDEF_NAME = "Cactus test step definition";
    private static final String ACL_NAME = "Cactus test ACL";
    private static final String ACL_LABEL = "Cactus test label";

    private ACLEngine aclEngine = null;
    private WorkflowEngine workflowEngine = null;
    private StepEngine stepEngine = null;
    private StepDefinitionEngine stepDefinitionEngine = null;
    private RouteEngine routeEngine = null;

    private ACL myWorkflowACL = null;
    private static int ctr = 0;

    /**
     * Create test ACLs.
     *
     * @throws Exception if an error occurs
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        // EJB lookup
        aclEngine = EJBLookup.getAclEngine();
        workflowEngine = EJBLookup.getWorkflowEngine();
        stepEngine = EJBLookup.getWorkflowStepEngine();
        stepDefinitionEngine = EJBLookup.getWorkflowStepDefinitionEngine();
        routeEngine = EJBLookup.getWorkflowRouteEngine();
        // create test ACL
        login(TestUsers.SUPERVISOR);
        myWorkflowACL = aclEngine.load(aclEngine.create("CACTUS_TEST_WORKFLOW", new FxString("Test workflow"), getUserTicket().getMandatorId(),
                "#000000", "cactus test ACL (ignore)", ACL.Category.WORKFLOW));
        aclEngine.assign(myWorkflowACL.getId(), UserGroup.GROUP_EVERYONE, true, true, true, true, true, true);
    }


    /**
     * Remove test ACLs.
     *
     * @throws Exception if an error occurs
     */
    @AfterClass
    public void afterClass() throws Exception {
        // remove test ACL
        aclEngine.unassign(myWorkflowACL.getId(), UserGroup.GROUP_EVERYONE);
        aclEngine.remove(myWorkflowACL.getId());
        logout();
    }

    /**
     * Test creation and loading of workflows.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void createWorkflow() throws Exception {
        long workflowId = -1;
        try {
            workflowId = createTestWorkflow();
            try {
                createTestWorkflow();
                assert false:"Workflows must have unique names";
            }
            catch (Exception e) {
                //ok
            }
            assert getUserTicket().isInRole(Role.WorkflowManagement) : "User is not in role workflow management - call should have failed.";
            Workflow workflow = getEnvironment().getWorkflow(workflowId);
            assert workflowId == workflow.getId() : "Workflow ID not set/returned correctly.";
            assert WORKFLOW_NAME.equals(workflow.getName()) : "Workflow name was not stored correctly.";
            assert WORKFLOW_DESCRIPTION.equals(workflow.getDescription()) : "Workflow description was not stored correctly.";
        } finally {
            // cleanup
            if (workflowId != -1) {
                workflowEngine.remove(workflowId);
            }
        }
    }

    /**
     * Tests if a workflow is deleted properly.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void deleteWorkflow() throws Exception {
        long workflowId = createTestWorkflow();
        workflowEngine.remove(workflowId);
        assert getUserTicket().isInRole(Role.WorkflowManagement) : "User is not in role workflow management - call should have failed.";
        try {
            getEnvironment().getWorkflow(workflowId);
            assert false : "Able to retrieve deleted workflow.";
        } catch (Exception e) {
            // succeed
        }
    }

    /**
     * Tests if workflows are updated properly.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void updateWorkflow() throws Exception {
        long workflowId = createTestWorkflow();
        try {
            WorkflowEdit editWorkflow = new WorkflowEdit(getEnvironment().getWorkflow(workflowId));
            editWorkflow.setName(StringUtils.reverse(WORKFLOW_NAME));
            editWorkflow.setDescription(StringUtils.reverse(WORKFLOW_DESCRIPTION));
            editWorkflow.getSteps().add(new StepEdit(new Step(-1, StepDefinition.EDIT_STEP_ID, editWorkflow.getId(), myWorkflowACL.getId())));
            editWorkflow.getSteps().add(new StepEdit(new Step(-2, StepDefinition.LIVE_STEP_ID, editWorkflow.getId(), myWorkflowACL.getId())));
            editWorkflow.getRoutes().add(new Route(-1, UserGroup.GROUP_EVERYONE, -1, -2));
            workflowEngine.update(editWorkflow);
            Workflow workflow = getEnvironment().getWorkflow(workflowId);
            assert getUserTicket().isInRole(Role.WorkflowManagement) : "User is not in role workflow management - call should have failed.";
            assert StringUtils.reverse(WORKFLOW_NAME).equals(workflow.getName()) : "Workflow name not updated properly.";
            assert StringUtils.reverse(WORKFLOW_DESCRIPTION).equals(workflow.getDescription()) : "Workflow description not updated properly.";
            assert workflow.getSteps().size() == 2 : "Unexpected number of workflow steps: " + workflow.getSteps().size();
            assert workflow.getRoutes().size() == 1 : "Unexpected number of workflow routes: " + workflow.getRoutes();
        } finally {
            // cleanup
            workflowEngine.remove(workflowId);
        }
    }

    /**
     * Do some tests that caching works with transaction rollbacks
     *
     * @throws FxApplicationException if an error occured
     */
    @Test
    public void updateWorkflowCaching() throws FxApplicationException {
        long workflowId = createTestWorkflow();
        try {
            WorkflowEdit editWorkflow = new WorkflowEdit(getEnvironment().getWorkflow(workflowId));
            // add two valid steps
            editWorkflow.getSteps().add(new StepEdit(new Step(-1, StepDefinition.EDIT_STEP_ID, editWorkflow.getId(), myWorkflowACL.getId())));
            editWorkflow.getSteps().add(new StepEdit(new Step(-2, StepDefinition.LIVE_STEP_ID, editWorkflow.getId(), myWorkflowACL.getId())));
            // ... and an invalid route (which will cause a rollback after the steps have been added)
            editWorkflow.getRoutes().add(new Route(-1, UserGroup.GROUP_EVERYONE, -1, -10));

            List<Step> cachedSteps = getEnvironment().getSteps();
            try {
                workflowEngine.update(editWorkflow);
                assert false : "Should not be able to successfully create workflows with invalid routes.";
            } catch (Exception e) {
                assert cachedSteps.equals(getEnvironment().getSteps()) :
                        "Steps should have been rollbacked.\nSteps before update: " + cachedSteps.toString()
                                + "\nEnvironment: " + getEnvironment().getSteps();
            }
        } finally {
            workflowEngine.remove(workflowId);
        }
    }

    /**
     * Tests workflow step creation, editing and updating.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void createEditUpdateStep() throws Exception {
        long workflowId = -1;
        long stepDefinitionId = -1;
        long aclId = -1;
        long acl2Id = -1;
        Workflow workflow;
        try {
            stepDefinitionId = createStepDefinition(null);
            try {
                createStepDefinition(null);
                assert false:"Step definitions must have unique names for a specific language";
            }
            catch (Exception e) {
                //ok
            }
            aclId = createAcl();
            List<Step> steps = new ArrayList<Step>();
            steps.add(new Step(-1, stepDefinitionId, -1, aclId));
            workflow = new Workflow(-1, WORKFLOW_NAME, WORKFLOW_DESCRIPTION, steps, new ArrayList<Route>());
            workflowId = workflowEngine.create(workflow);
            workflow = getEnvironment().getWorkflow(workflowId);
            // try to remove previously created step
            assert 1 == workflow.getSteps().size() : "Step initialized with workflow not created automatically";

            // update step
            acl2Id = createAcl();
            long stepId = workflow.getSteps().get(0).getId();
            stepEngine.updateStep(stepId, acl2Id);
            assert getEnvironment().getStep(stepId).getAclId() == acl2Id;

            stepEngine.removeStep(stepId);
            workflow = getEnvironment().getWorkflow(workflowId);
            assert 0 == workflow.getSteps().size() : "Step not removed in CacheAdmin.getEnvironment().";
            // generate some steps...
            List<StepDefinition> stepDefinitions = getEnvironment().getStepDefinitions();
            for (StepDefinition stepDefinition : stepDefinitions) {
                stepEngine.createStep(new Step(-1, stepDefinition.getId(), workflow.getId(), aclId));
            }
            // check if structure was updated properly
            workflow = getEnvironment().getWorkflow(workflowId);
            assert stepDefinitions.size() == workflow.getSteps().size() : "Created steps not reflected in CacheAdmin.getEnvironment().";
            // create some routes...
            steps = workflow.getSteps();
            int routes = 0;
            for (int i = 0; i < steps.size() - 1; i++) {
                for (int j = 0; j < steps.size() - 1; j++) {
                    if (i != j) {
                        // add route from step i to step j
                        long routeId = routeEngine.create(steps.get(i).getId(), steps.get(j).getId(),
                                j % 2 == 0 ? UserGroup.GROUP_EVERYONE : UserGroup.GROUP_OWNER);
                        Route route = getEnvironment().getRoute(routeId);
                        List<Step> targets = routeEngine.getTargets(route.getFromStepId());
                        boolean found = false;
                        for (Step step : targets) {
                            if (step.getId() == route.getToStepId()) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            assert false : "Created route target not found with getTargets().";
                        }
                        routes++;
                    }
                }
            }
            workflow = getEnvironment().getWorkflow(workflowId);
            assert routes == workflow.getRoutes().size() : "Created routes not reflected in CacheAdmin.getEnvironment().";
            // delete some routes...
            routeEngine.remove(workflow.getRoutes().get(0).getId());
            workflow = getEnvironment().getWorkflow(workflowId);
            assert routes - 1 == workflow.getRoutes().size() : "Deleted routes not reflected in CacheAdmin.getEnvironment().";
            assert workflow.getRoutes().size() > 0;
            routeEngine.remove(workflow.getRoutes().get(0).getId());
            assert getEnvironment().getWorkflow(workflowId).getRoutes().size() == 0 : "Not all routes deleted.";
        } finally {
            if (workflowId != -1) {
                workflowEngine.remove(workflowId);
            }
            if (stepDefinitionId != -1) {
                stepDefinitionEngine.remove(stepDefinitionId);
            }
            if (aclId != -1) {
                aclEngine.remove(aclId);
            }
            if (acl2Id != -1) {
                aclEngine.remove(acl2Id);
            }
        }
    }

    /**
     * Tests the implicit creation of steps when adding a unique target
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          if an error occurs
     */
    @Test
    public void implicitStepCreation() throws FxApplicationException {
        long workflowId = -1;
        long aclId = -1;
        long sd1 = -1;
        long sd2 = -1;
        try {
            // create two independent steps
            aclId = createAcl();
            sd1 = stepDefinitionEngine.create(new StepDefinition(-1, new FxString("base"), "base", -1));
            sd2 = stepDefinitionEngine.create(new StepDefinition(-2, new FxString("derived"), "derived", -1));
            // create a workflow which uses only the first step definition
            workflowId = workflowEngine.create(new Workflow(-1, "test", "descr",
                    Arrays.asList(new Step(-1, sd1, -1, aclId)), new ArrayList<Route>()));
            // introduce a unique target to sd2
            StepDefinitionEdit edit = new StepDefinitionEdit(getEnvironment().getStepDefinition(sd1));
            edit.setUniqueTargetId(sd2);
            stepDefinitionEngine.update(edit);
            // check if the workflow contains a step for sd2
            Workflow workflow = getEnvironment().getWorkflow(workflowId);
            List<StepDefinition> usedStepDefinitions = FxSharedUtils.getUsedStepDefinitions(workflow.getSteps(), getEnvironment().getStepDefinitions());
            assert usedStepDefinitions.contains(getEnvironment().getStepDefinition(sd1)) : "No step exists for step definition " + sd1;
            assert usedStepDefinitions.contains(getEnvironment().getStepDefinition(sd2)) : "No step created for step definition " + sd2;
        } finally {
            if (workflowId != -1) {
                workflowEngine.remove(workflowId);
            }
            if (sd1 != -1) {
                stepDefinitionEngine.remove(sd1);
            }
            if (sd2 != -1) {
                stepDefinitionEngine.remove(sd2);
            }
            if (aclId != -1) {
                aclEngine.remove(aclId);
            }
        }
    }

    /**
     * Tests step definition updates
     *
     * @throws FxApplicationException if an error occured
     */
    @Test
    public void updateStepDefinition() throws FxApplicationException {
        long stepDefinitionId = -1;
        try {
            stepDefinitionId = createStepDefinition(null);
            assert stepDefinitionId != -1 : "Failed to create step definition";
            StepDefinition stepDefinition = getEnvironment().getStepDefinition(stepDefinitionId);
            assert new FxString(STEPDEF_NAME).equals(stepDefinition.getLabel()) : "Invalid label: " + stepDefinition.getLabel();
            assert STEPDEF_NAME.equals(stepDefinition.getName()) : "Invalid name: " + stepDefinition.getName();
            StepDefinitionEdit definitionEdit = new StepDefinitionEdit(stepDefinition);
            definitionEdit.setName(StringUtils.reverse(stepDefinition.getName()));
            FxString label = new FxString(STEPDEF_NAME);
            label.setTranslation(label.getDefaultLanguage(), StringUtils.reverse(label.getDefaultTranslation()));
            definitionEdit.setLabel(label);
            stepDefinitionEngine.update(definitionEdit);
            stepDefinition = getEnvironment().getStepDefinition(stepDefinitionId);
            assert StringUtils.reverse(STEPDEF_NAME).equals(stepDefinition.getName()) : "Invalid name: " + stepDefinition.getName();
            assert label.equals(stepDefinition.getLabel()) : "Invalid label: " + stepDefinition.getLabel();
        } finally {
            if (stepDefinitionId != -1) {
                stepDefinitionEngine.remove(stepDefinitionId);
            }
        }
    }

    @Test
    public void stepDefinitionDefaultLanguage() throws FxApplicationException {
        long stepDefinitionId = -1;
        try {
            stepDefinitionId = createStepDefinition(null);
            StepDefinition stepDefinition = getEnvironment().getStepDefinition(stepDefinitionId);
            List<FxLanguage> languages = EJBLookup.getLanguageEngine().loadAvailable();
            for (FxLanguage language : languages) {
                stepDefinition.getLabel().setTranslation(language, "Translation " + language);
            }
            long defaultLanguage = languages.get(languages.size() - 1).getId();
            stepDefinition.getLabel().setDefaultLanguage(defaultLanguage);
            stepDefinitionEngine.update(stepDefinition);
            assert getEnvironment().getStepDefinition(stepDefinitionId).getLabel().getDefaultLanguage() == defaultLanguage
                    : "Default language not preserved correctly.";
        } finally {
            if (stepDefinitionId != -1) {
                stepDefinitionEngine.remove(stepDefinitionId);
            }
        }
    }

    @Test
    public void stepDefinitionUniqueTarget() throws FxApplicationException {
        long stepDefinitionId = -1;
        long targetStepDefinitionId = -1;
        try {
            stepDefinitionId = createStepDefinition("st1");
            targetStepDefinitionId = createStepDefinition("st2");
            StepDefinitionEdit stepDefinition = new StepDefinitionEdit(getEnvironment().getStepDefinition(stepDefinitionId));
            stepDefinition.setUniqueTargetId(targetStepDefinitionId);
            stepDefinitionEngine.update(stepDefinition);
            assert CacheAdmin.getEnvironment().getStepDefinition(stepDefinitionId).getUniqueTargetId() == targetStepDefinitionId
                    : "Target step definition ID not updated.";
        } finally {
            if (stepDefinitionId != -1) {
                stepDefinitionEngine.remove(stepDefinitionId);
            }
            if (targetStepDefinitionId != -1) {
                stepDefinitionEngine.remove(targetStepDefinitionId);
            }
        }
    }

    @Test
    public void loadAllSteps() throws FxApplicationException {
        long workflowId = -1;
        long stepDefinitionId = -1;
        long stepId = -1;
        try {
            workflowId = createTestWorkflow();
            stepDefinitionId = createStepDefinition(null);
            stepId = stepEngine.createStep(new Step(-1, stepDefinitionId, workflowId, myWorkflowACL.getId()));
            List<StepPermission> stepPermissions = stepEngine.loadAllStepsForUser(FxContext.get().getTicket().getUserId());
            assert stepPermissions.size() > 0 : "No steps/step permissions returned.";
            // TODO add more checks
        } finally {
            if (stepId != -1) {
                stepEngine.removeStep(stepId);
            }
            if (stepDefinitionId != -1) {
                stepDefinitionEngine.remove(stepDefinitionId);
            }
            if (workflowId != -1) {
                workflowEngine.remove(workflowId);
            }
        }
    }

    /**
     * Attempts to create a circular dependency by setting a stepdef's unique target
     * on itself.
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          if an error occured
     */
    @Test
    public void stepDefinitionTargetCycle1() throws FxApplicationException {
        long id = -1;
        try {
            id = stepDefinitionEngine.create(new StepDefinition(-1, new FxString("base"), "base", -1));
            assert getEnvironment().getStepDefinition(id).getUniqueTargetId() == -1;
            // set unique target to itself
            StepDefinitionEdit edit = new StepDefinitionEdit(getEnvironment().getStepDefinition(id));
            try {
                // first try: sdedit should catch this
                edit.setUniqueTargetId(id);
                assert false : "It should not be possible to set the unique target ID to the ID of the "
                        + " step definition itself in StepDefinitionEdit";
            } catch (FxRuntimeException e) {
                // pass
            }
            try {
                // second try: the stepdef constructor should catch this too
                assert edit.getId() != -1;
                new StepDefinition(edit.getId(), new FxString("test"), "test", edit.getId());
                assert false : "The StepDefinition constructor should check if the unique target ID "
                        + " is equal to the step definition ID.";
            } catch (FxRuntimeException e) {
                // pass
            }
        } finally {
            if (id != -1) {
                stepDefinitionEngine.remove(id);
            }
        }
    }

    /**
     * Attempts to create a cycle between two workflow step definitions.
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          if an error occured
     */
    @Test
    public void stepDefinitionTargetCycle2() throws FxApplicationException {
        long sd1 = -1;
        long sd2 = -1;
        boolean createdCycle = false;
        try {
            sd1 = stepDefinitionEngine.create(new StepDefinition(-1, new FxString("base"), "base", -1));
            sd2 = stepDefinitionEngine.create(new StepDefinition(-1, new FxString("derived"), "derived", sd1));
            // until now, the step definitions are valid and no cycle is created
            assert getEnvironment().getStepDefinition(sd2).getUniqueTargetId() == sd1
                    : "Invalid unique target: " + getEnvironment().getStepDefinition(sd2).getUniqueTargetId();
            assert getEnvironment().getStepDefinition(sd1).getUniqueTargetId() == -1
                    : "Invalid unique target: " + getEnvironment().getStepDefinition(sd1).getUniqueTargetId();
            // create cycle by changing sd1's unique target to sd2
            StepDefinitionEdit sdEdit = new StepDefinitionEdit(getEnvironment().getStepDefinition(sd1));
            sdEdit.setUniqueTargetId(sd2);
            try {
                stepDefinitionEngine.update(sdEdit);
                createdCycle = true;
                assert false : "Possible to create cycles in unique target definitions";
            } catch (FxApplicationException e) {
                // pass
            }
        } finally {
            if (!createdCycle) {
                // cleanup - unless we successfully created a cycle, in that case
                // the stepdef table cannot be cleared up because of the circular dependency
                if (sd2 != -1) {
                    stepDefinitionEngine.remove(sd2);
                }
                if (sd1 != -1) {
                    stepDefinitionEngine.remove(sd1);
                }
            }
        }
    }

    /**
     * Attempts to create a longer cycle (with 10 nodes and the last one pointing
     * to the first).
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          if an error occured
     */
    @Test
    public void stepDefinitionTargetCycle10() throws FxApplicationException {
        List<Long> ids = new ArrayList<Long>();
        try {
            for (int i = 0; i < 10; i++) {
                // create a new step definition, with the unique target pointing to the last one (except for the first)
                ids.add(stepDefinitionEngine.create(new StepDefinition(-1, new FxString("test" + i), "test"+i,
                        i > 0 ? ids.get(i - 1) : -1)));

            }
            // check if the unique targets have been created properly
            long previousId = -1;
            for (Long id : ids) {
                StepDefinition sd = getEnvironment().getStepDefinition(id);
                assert sd.getId() != -1;
                if (previousId != -1) {
                    assert sd.getUniqueTargetId() == previousId : "Unique target for step " + sd.getId()
                            + " is " + sd.getUniqueTargetId() + ", expected: " + previousId;
                }
                previousId = id;
            }
            // try to create cycle
            StepDefinitionEdit sdEdit = new StepDefinitionEdit(getEnvironment().getStepDefinition(ids.get(0)));
            // point unique target to last element in list --> cycle created
            sdEdit.setUniqueTargetId(ids.get(ids.size() - 1));
            try {
                stepDefinitionEngine.update(sdEdit);
                assert false : "Cycle created, nodes = " + ids;
            } catch (FxApplicationException e) {
                // pass
            }
        } finally {
            Collections.reverse(ids);
            for (Long id : ids) {
                stepDefinitionEngine.remove(id);
            }
        }
    }

    /**
     * Test the code we use in the workflow reference documentation
     * @throws com.flexive.shared.exceptions.FxApplicationException on errors
     */
    @Test
    public void workflowDocTest() throws FxApplicationException {
        final List<StepDefinition> stepDefinitions = new ArrayList<StepDefinition>();
        final List<Workflow> workflows = new ArrayList<Workflow>();
        try {
            // create step definition objects
            final StepDefinitionEdit definition1 =
                    new StepDefinition(new FxString("step 1"), "first step", -1).asEditable();
            final StepDefinitionEdit definition2 =
                    new StepDefinition(new FxString("step 2"), "second step", -1).asEditable();

            // store them in the database and store IDs
            definition1.setId(EJBLookup.getWorkflowStepDefinitionEngine().create(definition1));
            definition2.setId(EJBLookup.getWorkflowStepDefinitionEngine().create(definition2));

            stepDefinitions.addAll(Arrays.asList(definition1, definition2));

            // create a workflow and auto-create steps using intermediate IDs
            final Step step1 = new Step(-10, definition1.getId(), ACL.Category.WORKFLOW.getDefaultId());
            final Step step2 = new Step(-20, definition2.getId(), ACL.Category.WORKFLOW.getDefaultId());

            // create a route between step1 and step2
            final Route route = new Route(-1, UserGroup.GROUP_EVERYONE, -10, -20);

            // create workflow object and store it in the database
            final Workflow workflow = new Workflow(-1, "test wf", "my test workflow",
                    Arrays.asList(step1, step2), Arrays.asList(route));
            final long workflowId = EJBLookup.getWorkflowEngine().create(workflow);
            final Workflow dbWorkflow = CacheAdmin.getEnvironment().getWorkflow(workflowId);
            workflows.add(dbWorkflow);
            assert dbWorkflow.getRoutes().size() == 1;      // route available?
            assert dbWorkflow.getSteps().size() == 2;       // both steps available?
            
            // check from and to steps of our route
            assert dbWorkflow.getRoutes().get(0).getFromStepId() == dbWorkflow.getSteps().get(0).getId();
            assert dbWorkflow.getRoutes().get(0).getToStepId() == dbWorkflow.getSteps().get(1).getId();
        } finally {
            for (Workflow workflow: workflows) {
                EJBLookup.getWorkflowEngine().remove(workflow.getId());
            }
            for (StepDefinition def: stepDefinitions) {
                EJBLookup.getWorkflowStepDefinitionEngine().remove(def.getId());
            }
        }


    }

    /**
     * Creates a new step definition for testing.
     *
     * @return a new step definition for testing.
     * @throws FxApplicationException
     */
    private long createStepDefinition(String name) throws FxApplicationException {
        if (name == null)
            return stepDefinitionEngine.create(new StepDefinition(-1,
                    new FxString(STEPDEF_NAME), STEPDEF_NAME, -1));
        else
            return stepDefinitionEngine.create(new StepDefinition(-1,
                    new FxString(name), name, -1));
    }

    /**
     * Creates a new ACL for testing.
     *
     * @return an ACL for testing
     * @throws FxApplicationException
     */
    private long createAcl() throws FxApplicationException {
        return aclEngine.create(ACL_NAME + ctr++, new FxString(ACL_LABEL), getUserTicket().getMandatorId(),
                "#000000", "", ACL.Category.WORKFLOW);
    }

    /**
     * Creates a new workflow for testing.
     *
     * @return a new workflow for testing.
     * @throws FxApplicationException
     */
    private long createTestWorkflow() throws FxApplicationException {
        return workflowEngine.create(getTestWorkflow());
    }


    private Workflow getTestWorkflow() {
        return new Workflow(-1, WORKFLOW_NAME, WORKFLOW_DESCRIPTION,
                new ArrayList<Step>(), new ArrayList<Route>());
    }

}
