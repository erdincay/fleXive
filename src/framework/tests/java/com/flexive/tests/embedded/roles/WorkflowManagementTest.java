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
package com.flexive.tests.embedded.roles;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import static com.flexive.shared.EJBLookup.getWorkflowEngine;
import static com.flexive.shared.EJBLookup.getWorkflowStepEngine;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.interfaces.StepDefinitionEngine;
import com.flexive.shared.security.ACL;
import static com.flexive.shared.security.Role.WorkflowManagement;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.Route;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.Workflow;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Workflow role tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "security", "roles", "workflow"})
public class WorkflowManagementTest extends AbstractRoleTest {
    private long workflowId = -1;

    @AfterMethod(groups = {"ejb", "security", "roles", "workflow"})
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

    @Test
    public void editStepDefinitionTest() throws FxApplicationException {
        long id = -1;
        final StepDefinitionEngine sde = EJBLookup.getWorkflowStepDefinitionEngine();
        try {
            id = sde.create(new StepDefinition(-1, new FxString(true, "label"), "workflow roles test", -1));
            assertSuccess(WorkflowManagement, -1);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, WorkflowManagement, -1);
        } finally {
            if (id != -1) {
                sde.remove(id);
            }
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
