package com.flexive.tests.embedded.roles;

import static com.flexive.shared.EJBLookup.getWorkflowEngine;
import static com.flexive.shared.EJBLookup.getWorkflowStepEngine;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNoAccessException;
import static com.flexive.shared.security.Role.WorkflowManagement;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.workflow.Route;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.Workflow;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.CacheAdmin;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Workflow role tests.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Test(groups = {"security", "roles", "workflow"})
public class WorkflowManagementTest extends AbstractRoleTest {
    private long workflowId = -1;

    @AfterMethod(groups = {"security", "roles", "workflow"})
    private void cleanup() throws FxApplicationException {
        if (workflowId != -1) {
            getWorkflowEngine().remove(workflowId);
            workflowId = -1;
        }
    }

    @Test
    public void workflowCreateTest() throws FxApplicationException {
        try {
            createWorkflow();
        } catch (FxNoAccessException e) {
            assertNoAccess(e, WorkflowManagement, -1);
        }
    }

    @Test
    public void editWorkflowStepsTest() throws FxApplicationException {
        try {
            createWorkflow();
            final long id = getWorkflowStepEngine().createStep(new Step(-1, StepDefinition.EDIT_STEP_ID,
                    workflowId, getDefaultAclId()));
            assertSuccess(WorkflowManagement, -1);
            getWorkflowStepEngine().removeStep(id);
            assertSuccess(WorkflowManagement, -1);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, WorkflowManagement, -1);
        }
    }

    @Test
    public void editWorkflowRoutesTest() throws FxApplicationException {
        try {
            createWorkflow();
            final long from = getWorkflowStepEngine().createStep(new Step(-1, StepDefinition.EDIT_STEP_ID,
                    workflowId, getDefaultAclId()));
            final long to = getWorkflowStepEngine().createStep(new Step(-1, StepDefinition.LIVE_STEP_ID,
                    workflowId, getDefaultAclId()));
            assertSuccess(WorkflowManagement, -1);
            final long id = EJBLookup.getWorkflowRouteEngine().create(from, to, UserGroup.GROUP_EVERYONE);
            assertSuccess(WorkflowManagement, -1);
            EJBLookup.getWorkflowRouteEngine().remove(id);
            assertSuccess(WorkflowManagement, -1);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, WorkflowManagement, -1);
        }
    }

    private long getDefaultAclId() {
        return CacheAdmin.getEnvironment().getDefaultACL(ACL.Category.WORKFLOW).getId();
    }

    private void createWorkflow() throws FxApplicationException {
        final Workflow workflow = new Workflow(-1, "WF TEST", "", new ArrayList<Step>(0), new ArrayList<Route>(0));
        workflowId = getWorkflowEngine().create(workflow);
        assertSuccess(WorkflowManagement, -1);
    }

}
