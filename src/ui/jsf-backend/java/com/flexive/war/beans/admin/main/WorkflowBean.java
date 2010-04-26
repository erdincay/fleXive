/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.faces.messages.FxFacesMsgWarn;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.workflow.*;

import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Workflow service beans.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class WorkflowBean implements Serializable {
    private static final long serialVersionUID = 4771311329266251588L;

    private static final Log LOG = LogFactory.getLog(WorkflowBean.class);

    /**
     * Session key to store the last inserted step id (for validation purposes)
     */
    private static final String SESSION_LASTSTEPID = "__FXLASTNEWSTEPID__";

    private WorkflowEdit workflow = null;
    private List<StepEdit> steps = null;
    private List<RouteEdit> routes = null;
    private long stepDefinitionId = -1;
    private long stepACL = -1;
    private long workflowId = -1;
    private int stepIndex = -1;
    private int routeIndex = -1;
    private long fromStepId = -1;
    private long toStepId = -1;
    private long userGroup = -1;

    private int overviewPageNumber;
    private int overviewRows;
    private String sortColumn;
    private String sortOrder;

    /**
     * Default constructor
     */
    public WorkflowBean() {
    }

    /**
     * Create a new workflow.
     *
     * @return the next page
     */
    public String create() {
        try {
            workflow.setId(EJBLookup.getWorkflowEngine().create(workflow));
            workflowId = workflow.getId();
            new FxFacesMsgInfo("Workflow.nfo.created", workflow.getName()).addToContext();
            return edit();
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
            return "workflowCreate";
        }
        /*
           // test for cache transactions - keep until issue is resolved!
            *
          long workflowId = createTestWorkflow();
          try {
              ACL myWorkflowACL = EJBLookup.getAclInterface().load(EJBLookup.getAclInterface()
                  .create("CACTUS_TEST_WORKFLOW", new FxString("Test workflow"),
                  FxContext.get().getUserTicket().getMandatorId(),
                      "#000000", "cactus test ACL (ignore)", ACLCategory.WORKFLOW));
              EJBLookup.getAclInterface().assign(myWorkflowACL.getId(), Group.GROUP_EVERYONE,
                      true, true, true, true, true, true);
              WorkflowEdit editWorkflow = new WorkflowEdit(CacheAdmin.getEnvironment().getWorkflow(workflowId));
              // add two valid steps
              editWorkflow.getSteps().add(new StepEdit(new Step(-1, StepDefinition.EDIT_STEP_ID,
                  editWorkflow.getId(), myWorkflowACL.getId())));
              editWorkflow.getSteps().add(new StepEdit(new Step(-2, StepDefinition.LIVE_STEP_ID,
                  editWorkflow.getId(), myWorkflowACL.getId())));
              // ... and an invalid route (which will cause a rollback after the steps have been added)
              editWorkflow.getRoutes().add(new Route(-1, Group.GROUP_EVERYONE, -1, -10));

              List<Step> cachedSteps = CacheAdmin.getEnvironment().getSteps();
              try {
                  EJBLookup.getWorkflowInterface().update(editWorkflow);
                  assert false : "Should not be able to successfully create workflows with invalid routes.";
              } catch (Exception e) {
                  if (!cachedSteps.equals(CacheAdmin.getEnvironment().getSteps())) {
                      FxJsfUtils.addErrorMsg("Steps should have been rollbacked.\nSteps before update: "
                      + cachedSteps.toString()
                      + "\nEnvironment: " + CacheAdmin.getEnvironment().getSteps(), null);
                  }
              }
          } finally {
              EJBLookup.getWorkflowInterface().delete(workflowId);
          }
          return "workflowOverview";*/
    }

    /**
     * Deletes the workflow set in workflowId.
     *
     * @return the next page
     */
    public String delete() {
        try {
            if (workflowId == -1) {
                new FxFacesMsgErr("Workflow.err.notSelected").addToContext();
            } else {
                String oldName = CacheAdmin.getEnvironment().getWorkflow(workflowId).getName();
                EJBLookup.getWorkflowEngine().remove(workflowId);
                new FxFacesMsgInfo("Workflow.nfo.deleted", oldName).addToContext();
            }
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return "workflowOverview";
    }

    /**
     * Edit the workflow set in workflowId.
     *
     * @return the next page
     */
    public String edit() {
        if (workflowId == -1) {
            new FxFacesMsgErr("Workflow.err.notSelected").addToContext();
        } else {
            workflow = new WorkflowEdit(CacheAdmin.getEnvironment().getWorkflow(workflowId));
            getSteps().clear();
            for (Step step : workflow.getSteps()) {
                getSteps().add(new StepEdit(step));
            }
            getRoutes().clear();
            for (Route route : workflow.getRoutes()) {
                getRoutes().add(new RouteEdit(route));
            }
        }
        return "workflowEdit";
    }

    /**
     * Save the current workflow.
     *
     * @return the next page
     */
    public String save() {
        try {
            workflow.setSteps(steps);
            workflow.setRoutes(routes);
            EJBLookup.getWorkflowEngine().update(workflow);
            new FxFacesMsgInfo("Workflow.nfo.updated", workflow.getName()).addToContext();
            return null;
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
            return null;
        }
    }

    /**
     * Add a new step definition to the current workflow.
     */
    public synchronized void addStep() {
        StepEdit step = new StepEdit(new Step(getNewStepId(), stepDefinitionId, workflow.getId(), stepACL));
        getSteps().add(step);
        StepDefinition sd = CacheAdmin.getFilteredEnvironment().getStepDefinition(stepDefinitionId);
        if (sd.isUnique()) {
            //make sure the unique target is added as well
            boolean found = false;
            for (StepEdit se : getSteps()) {
                if (se.getStepDefinitionId() == sd.getUniqueTargetId()) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                //add step definiton
                getSteps().add(new StepEdit(new Step(getNewStepId(), sd.getUniqueTargetId(), workflow.getId(), stepACL)));
            }
        }
        workflow.setSteps(steps);
    }

    /**
     * Remove the step at position "stepIndex".
     *
     * @return the next page
     */
    public void removeStep() {
        if (stepIndex == -1) {
            new FxFacesMsgErr("Workflow.err.step.notSelected").addToContext();
        } else if (stepIndex < 0 || stepIndex > getSteps().size()) {
            new FxFacesMsgErr("Workflow.err.step.invalidIndex",
                    FxJsfUtils.getLocalizedMessage("Workflow.err.step.invalidIndex.detail", stepIndex)
            ).addToContext();
        } else if (getUsedSteps().contains(getSteps().get(stepIndex))) {
            // TODO add shortcut method
            String stepLabel = CacheAdmin.getEnvironment().getStepDefinition(
                    getSteps().get(stepIndex).getStepDefinitionId())
                    .getLabel().getBestTranslation();
            new FxFacesMsgErr("Workflow.err.step.inUse", stepLabel).addToContext();
        } else {
            getSteps().remove(stepIndex);
        }
        workflow.setSteps(getSteps());
    }

    public void moveStepUp() {
        moveStep(stepIndex - 1);
    }
    
    public void moveStepDown() {
        moveStep(stepIndex + 1);
    }

    private void moveStep(int targetPosition) {
        final StepEdit step = getSteps().remove(stepIndex);
        getSteps().add(
                Math.max(0, Math.min(getSteps().size(), targetPosition)),
                step
        );
    }
    /**
     * Check if a step can be removed as mapped function
     *
     * @return if step can be removed
     */
    public Map<Step, Boolean> getCanRemoveStep() {
        return FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Step, Boolean>() {
            public Boolean get(Object key) {
                if (key == null) {
                    return null;
                }
                Step s = (Step) key;
                for (Route r : getRoutes())
                    if (r.getFromStepId() == s.getId() || r.getToStepId() == s.getId())
                        return false;
                for (Step _s : getSteps())
                    if (CacheAdmin.getEnvironment().getStepDefinition(_s.getStepDefinitionId()).getUniqueTargetId() == s.getStepDefinitionId())
                        return false;
                return true;
            }
        }, true);
    }

    /**
     * Add a new route between steps[fromStepIndex] and steps[toStepIndex].
     */
    public void addRoute() {
        if (getStep(fromStepId) == null || getStep(toStepId) == null) {
            new FxFacesMsgErr("Workflow.err.route.create.steps.notFound").addToContext();
            return;
        }
        if (fromStepId == toStepId) {
            new FxFacesMsgErr("Workflow.err.route.create.steps.identical").addToContext();
            return;
        }
        RouteEdit route = new RouteEdit(new Route(-1, userGroup, fromStepId, toStepId));
        if (routes.contains(route)) {
            new FxFacesMsgWarn("Workflow.wng.route.exists").addToContext();
        } else {
            routes.add(route);
            final FxEnvironment env = CacheAdmin.getEnvironment();
            String fromStepName = env.getStepDefinition(
                    getStep(route.getFromStepId()).getStepDefinitionId()).getLabel().getBestTranslation();
            String toStepName = env.getStepDefinition(
                    getStep(route.getToStepId()).getStepDefinitionId()).getLabel().getBestTranslation();
            String groupName = String.valueOf(userGroup);
            try {
                groupName = EJBLookup.getUserGroupEngine().load(userGroup).getName();
            } catch (FxApplicationException e) {
                LOG.error(e);
            }
            new FxFacesMsgInfo("Workflow.nfo.route.added", fromStepName, toStepName, groupName).addToContext();
        }
    }

    /**
     * Remove an existing route (by index).
     */
    public void removeRoute() {
        if (routeIndex < 0 || routeIndex > getRoutes().size()) {
            new FxFacesMsgErr("Workflow.err.route.remove.invalid", routeIndex).addToContext();
            return;
        }
        Route route = routes.get(routeIndex);
        // TODO add convenience methods for route --> step --> stepdefinitionid --> stepdefinition
        final FxEnvironment env = CacheAdmin.getEnvironment();
        String fromStepName = env.getStepDefinition(
                getStep(route.getFromStepId()).getStepDefinitionId()).getLabel().getBestTranslation();
        String toStepName = env.getStepDefinition(
                getStep(route.getToStepId()).getStepDefinitionId()).getLabel().getBestTranslation();
        routes.remove(routeIndex);
        String grp = "" + route.getGroupId();
        try {
            grp = EJBLookup.getUserGroupEngine().load(route.getGroupId()).getName();
        } catch (FxApplicationException e) {
            //ignore
        }
        new FxFacesMsgInfo("Workflow.nfo.route.removed", fromStepName, toStepName, grp).addToContext();
    }

    /**
     * Return a list of all workflow step definitions that can be added to the current workflow
     * (i.e. all defined steps minus the steps already used in the workflow).
     *
     * @return a list of all workflow step definitions that can be added to the current workflow
     */
    @SuppressWarnings("unchecked")
    public List<SelectItem> getStepsForAdding() {
        final List<StepDefinition> stepDefinitions = new ArrayList<StepDefinition>(
                CacheAdmin.getFilteredEnvironment().getStepDefinitions()
        );
        stepDefinitions.removeAll(
                FxSharedUtils.getUsedStepDefinitions(steps != null ? steps : workflow.getSteps(), stepDefinitions)
        );
        return FxJsfUtils.asSelectListWithLabel(stepDefinitions);
    }

    /**
     * Return a list of all workflow steps that are available for routes.
     *
     * @return a list of all workflow steps that are available for routes.
     */
    public List<SelectItem> getStepsForRoutes() {
        List<SelectItem> result = new ArrayList<SelectItem>();
        List<? extends Step> listSteps = steps != null ? steps : CacheAdmin.getFilteredEnvironment().getSteps();
        if (steps == null) {
            // TODO ugly validation fix to allow temporary steps (negative id)
            Object lastNewStepIdValue = FxJsfUtils.getSessionAttribute(SESSION_LASTSTEPID);
            long lastNewStepId = lastNewStepIdValue != null ? (Long) lastNewStepIdValue : -1;
            for (long i = -1; i >= lastNewStepId; i--) {
                result.add(new SelectItem(i, ""));
            }
        }

        for (Step step : listSteps) {
            StepDefinition stepDefinition = CacheAdmin.getEnvironment().
                    getStepDefinition(step.getStepDefinitionId());
            result.add(new SelectItem(step.getId(),
                    stepDefinition.getLabel().getBestTranslation()));
        }
        return result;
    }

    /**
     * Returns a negative pseudo step ID to be used for identifying steps
     * in routes before they are persisted to the database.
     *
     * @return a new internal step ID to be used for identifying steps
     */
    private long getNewStepId() {
        long minStepId = -1;
        for (Step step : getSteps()) {
            if (step.getId() <= minStepId) {
                minStepId = step.getId() - 1;
            }
        }
        FxJsfUtils.setSessionAttribute(SESSION_LASTSTEPID, minStepId);
        return minStepId;
    }

    /**
     * Returns a list of all steps used by the current route definitions.
     *
     * @return a list of all steps used by the current route definitions.
     */
    private List<StepEdit> getUsedSteps() {
        List<StepEdit> routeSteps = new ArrayList<StepEdit>();
        for (Route route : getRoutes()) {
            routeSteps.add(getStep(route.getFromStepId()));
            routeSteps.add(getStep(route.getToStepId()));
        }
        return routeSteps;
    }


    /**
     * Return the step with the given ID (may be negative for temporary
     * steps).
     *
     * @param id step id
     * @return the step with the requested ID
     */
    private StepEdit getStep(long id) {
        for (StepEdit step : getSteps()) {
            if (step.getId() == id) {
                return step;
            }
        }
        return null;
    }

    public List<Workflow> getList() {
        return CacheAdmin.getFilteredEnvironment().getWorkflows();
    }

    /**
     * Return the current workflow.
     *
     * @return the current workflow.
     */
    public WorkflowEdit getWorkflow() {
        if (workflow != null) {
            return workflow;
        }
        workflow = new WorkflowEdit();
        return workflow;
    }

    public void setWorkflow(WorkflowEdit workflow) {
        this.workflow = workflow;
    }

    public long getStepDefinitionId() {
        return stepDefinitionId;
    }

    public void setStepDefinitionId(long stepDefinitionId) {
        this.stepDefinitionId = stepDefinitionId;
    }

    public long getStepACL() {
        return stepACL;
    }

    public void setStepACL(long stepACL) {
        this.stepACL = stepACL;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * Return the current step table.
     *
     * @return the current step table.
     */
    public List<StepEdit> getSteps() {
        if (steps != null) {
            return steps;
        }
        steps = new ArrayList<StepEdit>();
        return steps;
    }


    public void setSteps(List<StepEdit> steps) {
        this.steps = steps;
    }

    /**
     * Return a map with all steps indexed by ID.
     *
     * @return a map with all steps indexed by ID.
     */
    public Map<Long, StepEdit> getStepsById() {
        HashMap<Long, StepEdit> result = new HashMap<Long, StepEdit>(steps.size());
        for (StepEdit step : getSteps()) {
            result.put(step.getId(), step);
        }
        return result;
    }

    /**
     * Return the current route table.
     *
     * @return the current route table.
     */
    public List<RouteEdit> getRoutes() {
        if (routes != null) {
            return routes;
        }
        routes = new ArrayList<RouteEdit>();
        return routes;
    }

    public void setRoutes(List<RouteEdit> routes) {
        this.routes = routes;
    }

    public long getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(long workflowId) {
        this.workflowId = workflowId;
    }

    public int getStepIndex() {
        return stepIndex;
    }

    public void setStepIndex(int stepIndex) {
        this.stepIndex = stepIndex;
    }

    public int getRouteIndex() {
        return routeIndex;
    }

    public void setRouteIndex(int routeIndex) {
        this.routeIndex = routeIndex;
    }

    public long getFromStepId() {
        return fromStepId;
    }

    public void setFromStepId(long fromStepId) {
        this.fromStepId = fromStepId;
    }

    public long getToStepId() {
        return toStepId;
    }

    public void setToStepId(long toStepId) {
        this.toStepId = toStepId;
    }

    public long getUserGroup() {
        return userGroup;
    }

    public void setUserGroup(long userGroup) {
        this.userGroup = userGroup;
    }

    public int getOverviewPageNumber() {
        return overviewPageNumber;
    }

    public void setOverviewPageNumber(int overviewPageNumber) {
        this.overviewPageNumber = overviewPageNumber;
    }

    public int getOverviewRows() {
        return overviewRows;
    }

    public void setOverviewRows(int overviewRows) {
        this.overviewRows = overviewRows;
    }
}
