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
package com.flexive.ejb.beans.workflow;


import com.flexive.core.Database;
import com.flexive.core.storage.StorageManager;
import static com.flexive.core.DatabaseConst.TBL_WORKFLOW;
import com.flexive.core.structure.StructureLoader;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSystemSequencer;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.workflow.Route;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.Workflow;
import com.flexive.ejb.beans.EJBUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Class to handle the basic workflow setup.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "WorkflowEngine", mappedName="WorkflowEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class WorkflowEngineBean implements WorkflowEngine, WorkflowEngineLocal {

    private static final Log LOG = LogFactory.getLog(WorkflowEngineBean.class);

    @Resource
    private SessionContext ctx;
    @EJB
    private StepEngineLocal stepEngine;
    @EJB
    private RouteEngineLocal routeEngine;
    @EJB
    private SequencerEngineLocal seq;

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(long workflowId) throws FxApplicationException {
        UserTicket ticket = FxContext.getUserTicket();
        // Permission checks
        FxPermissionUtils.checkRole(ticket, Role.WorkflowManagement);

        // Do work...
        Connection con = null;
        Statement stmt = null;
        String sql = null;
        boolean success = false;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            // First delete all steps within the workflow ..
            stepEngine.removeSteps(workflowId);
            // .. then delete the workflow itself
            stmt = con.createStatement();
            sql = "DELETE FROM " + TBL_WORKFLOW + " WHERE ID=" + workflowId;
            int count = stmt.executeUpdate(sql);
            // Check if the delete succeeded
            if (count == 0) {
                throw new FxNotFoundException(LOG, "ex.workflow.notFound", workflowId);
            }
            success = true;
        } catch (SQLException exc) {
            if (StorageManager.isForeignKeyViolation(exc))
                throw new FxRemoveException("ex.workflow.delete.inUse",
                        CacheAdmin.getEnvironment().getWorkflow(workflowId).getName(), workflowId);
            throw new FxRemoveException(LOG, "ex.workflow.delete", exc, workflowId, exc.getMessage());
        } finally {
            Database.closeObjects(WorkflowEngineBean.class, con, stmt);
            if (!success) {
                EJBUtils.rollback(ctx);
            } else {
                StructureLoader.reloadWorkflows(FxContext.get().getDivisionId());
            }
        }
    }

    /**
     * Checks if the given workflow is valid
     * <p/>
     * Throws a FxInvalidParameterException if the name or description is not valid.
     *
     * @param workflow Workflow to be checked
     * @throws FxInvalidParameterException if the name is not valid
     */
    private void checkIfValid(Workflow workflow) throws FxInvalidParameterException {
        String name = workflow.getName();
        String description = workflow.getDescription();

        // Name checks
        if (StringUtils.isBlank(name)) {
            throw new FxInvalidParameterException("NAME", "ex.workflow.name.empty");
        } else if (name.length() > 60) {
            throw new FxInvalidParameterException("NAME", "ex.workflow.name.length");
        } else if (name.indexOf('\'') > -1 || name.indexOf('"') > -1) {
            throw new FxInvalidParameterException("NAME", "ex.workflow.name.char");
        }

        // Description checks
        if (description != null && description.length() > 1024) {
            throw new FxInvalidParameterException("DESCRIPTION", "ex.workflow.description.length");
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void update(Workflow workflow)
            throws FxApplicationException {

        UserTicket ticket = FxContext.getUserTicket();

        // Permission checks
        FxPermissionUtils.checkRole(ticket, Role.WorkflowManagement);

        Workflow org = CacheAdmin.getEnvironment().getWorkflow(workflow.getId()).asEditable();

        List<Step> dups = new ArrayList<Step>(2); //duplicates (removed and re-added)
        for (Step check : workflow.getSteps()) {
            for (Step stp : org.getSteps())
                if (check.getId() < 0 && stp.getStepDefinitionId() == check.getStepDefinitionId())
                    dups.add(check);
        }
        if (dups.size() > 0) {
            //sync steps
            workflow.getSteps().removeAll(dups);
            for (Step stp : org.getSteps())
                for (Step dp : dups)
                    if (dp.getStepDefinitionId() == stp.getStepDefinitionId()) {
                        workflow.getSteps().add(stp);
                        break;
                    }
            //sync routes
            boolean changes = true;
            while (changes) {
                changes = false;
                for (Route r : workflow.getRoutes()) {
                    for (Step s : dups) {
                        if (r.getFromStepId() == s.getId()) {
                            long _from = r.getFromStepId();
                            for (Step stp : org.getSteps())
                                if (stp.getStepDefinitionId() == s.getStepDefinitionId()) {
                                    _from = stp.getId();
                                    break;
                                }
                            Route nr = new Route(r.getId(), r.getGroupId(), _from, r.getToStepId());
                            if (!workflow.getRoutes().contains(nr)) {
                                workflow.getRoutes().remove(r);
                                workflow.getRoutes().add(nr);
                                changes = true;
                            }
                            break;
                        } else if (r.getToStepId() == s.getId()) {
                            long _to = r.getToStepId();
                            for (Step stp : org.getSteps())
                                if (stp.getStepDefinitionId() == s.getStepDefinitionId()) {
                                    _to = stp.getId();
                                    break;
                                }
                            Route nr = new Route(r.getId(), r.getGroupId(), r.getFromStepId(), _to);
                            if (!workflow.getRoutes().contains(nr)) {
                                workflow.getRoutes().remove(r);
                                workflow.getRoutes().add(nr);
                                changes = true;
                            }
                            break;
                        }
                        if (changes) break;
                    }
                    if (changes) break;
                }
            }
        }
        Connection con = null;
        PreparedStatement stmt = null;
        String sql = "UPDATE " + TBL_WORKFLOW + " SET NAME=?, DESCRIPTION=? WHERE ID=?";

        boolean success = false;
        try {
            // Sanity checks
            checkIfValid(workflow);

            // Obtain a database connection
            con = Database.getDbConnection();

            // Update the workflow instance
            stmt = con.prepareStatement(sql);
            stmt.setString(1, workflow.getName());
            stmt.setString(2, StringUtils.defaultString(workflow.getDescription()));
            stmt.setLong(3, workflow.getId());

            stmt.executeUpdate();

            FxEnvironment fxEnvironment = CacheAdmin.getEnvironment();
            // Remove steps?
            List<Step> remove = new ArrayList<Step>(2);
            for (Step step : fxEnvironment.getStepsByWorkflow(workflow.getId())) {
                if (!workflow.getSteps().contains(step)) {
                    // remove step
                    remove.add(step);
                }
            }

            if (remove.size() > 0) {
                int tries = remove.size() * 2;
                List<Step> tmpRemove = new ArrayList<Step>(remove.size());
                while (remove.size() > 0 && --tries > 0) {
                    for (Step step : remove) {
                        try {
                            //remove affected routes as well
                            for (Route route : org.getRoutes())
                                if (route.getFromStepId() == step.getId() || route.getToStepId() == step.getId())
                                    routeEngine.remove(route.getId());

                            stepEngine.removeStep(step.getId());
                            tmpRemove.add(step);
                        } catch (FxApplicationException e) {
                            //ignore since rmeove order matters
                        }
                    }
                    remove.removeAll(tmpRemove);
                }
            }

            // Add/update steps, if necessary
            Map<Long, Step> createdSteps = new HashMap<Long, Step>();
            for (Step step : workflow.getSteps()) {
                if (step.getId() < 0) {
                    long newStepId = stepEngine.createStep(step);
                    // map created steps using the old ID - if routes reference them
                    createdSteps.put(step.getId(), new Step(newStepId, step));
                } else {
                    //ACL changed?
                    if (fxEnvironment.getStep(step.getId()).getAclId() != step.getAclId())
                        stepEngine.updateStep(step.getId(), step.getAclId());
                }
            }

            // Remove routes?
            boolean found;
            for (Route route : org.getRoutes()) {
                found = false;
                for( Route check: workflow.getRoutes() ) {
                    if( check.getGroupId() == route.getGroupId() &&
                            check.getFromStepId() == route.getFromStepId() &&
                            check.getToStepId() == route.getToStepId() ) {
                        workflow.getRoutes().remove(check); //dont add this one again
                        found = true;
                        break;
                    }
                }
                // remove route if not found
                if (!found)
                    routeEngine.remove(route.getId());
            }

            // add routes
            for (Route route : workflow.getRoutes()) {
                if (route.getId() < 0) {
                    long fromStepId = resolveTemporaryStep(createdSteps, route.getFromStepId());
                    long toStepId = resolveTemporaryStep(createdSteps, route.getToStepId());
                    routeEngine.create(fromStepId, toStepId, route.getGroupId());
                }
            }

            success = true;
        } catch (SQLException exc) {
            if (StorageManager.isUniqueConstraintViolation(exc)) {
                throw new FxEntryExistsException("ex.workflow.exists");
            } else {
                throw new FxUpdateException(LOG, exc, "ex.workflow.update", workflow.getName(), exc.getMessage());
            }
        } catch (Exception exc) {
            throw new FxUpdateException(LOG, exc, "ex.workflow.update", workflow.getName(), exc.getMessage());
        } finally {
            Database.closeObjects(WorkflowEngineBean.class, con, stmt);
            if (!success) {
                EJBUtils.rollback(ctx);
            } else {
                StructureLoader.reloadWorkflows(FxContext.get().getDivisionId());
            }
        }
    }

    /**
     * Resolve route references to steps that have not been created yet.
     * If a step is added, a temporary negative index is used for identifying
     * it in new routes. The createdSteps lookup table is used for mapping
     * these internal negative IDs to the database-generated, persisted step IDs.
     *
     * @param createdSteps mapping between internal step IDs and the created steps
     * @param stepId       the step ID to be mapped (may be negative)
     * @return the (positive) step ID that actually exists in the database
     * @throws FxInvalidParameterException if the step could not be mapped
     */
    private long resolveTemporaryStep(Map<Long, Step> createdSteps, long stepId)
            throws FxInvalidParameterException {
        if (stepId < 0) {
            if (!createdSteps.containsKey(stepId)) {
                throw new FxInvalidParameterException("ROUTES", "ex.workflow.route.referencedStep", stepId);
            }
            return createdSteps.get(stepId).getId();
        } else {
            return stepId;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long create(Workflow workflow) throws FxApplicationException {
        UserTicket ticket = FxContext.getUserTicket();
        // Permission checks
        FxPermissionUtils.checkRole(ticket, Role.WorkflowManagement);

        // Do work ..
        Connection con = null;
        PreparedStatement stmt = null;
        String sCurSql = null;
        boolean success = false;
        long id = -1;
        try {
            // Sanity checks
            checkIfValid(workflow);

            // Obtain a database connection
            con = Database.getDbConnection();

            // Create the new workflow instance
            sCurSql = "INSERT INTO " + TBL_WORKFLOW + " (ID,NAME,DESCRIPTION) VALUES (?,?,?)";
            stmt = con.prepareStatement(sCurSql);
            id = seq.getId(FxSystemSequencer.WORKFLOW);
            stmt.setLong(1, id);
            stmt.setString(2, workflow.getName());
            stmt.setString(3, StringUtils.defaultString(workflow.getDescription()));
            stmt.executeUpdate();

            // create step(s)
            final Map<Long, Step> createdSteps = new HashMap<Long, Step>();
            for (Step step : workflow.getSteps()) {
                final Step wfstep = new Step(-1, step.getStepDefinitionId(), id, step.getAclId());
                final long newStepId = stepEngine.createStep(wfstep);
                createdSteps.put(step.getId(), new Step(newStepId, wfstep));
            }

            // create route(s)
            for (Route route : workflow.getRoutes()) {
                routeEngine.create(
                        resolveTemporaryStep(createdSteps, route.getFromStepId()),
                        resolveTemporaryStep(createdSteps, route.getToStepId()),
                        route.getGroupId()
                );
            }
            success = true;
        } catch (Exception exc) {
            if (StorageManager.isUniqueConstraintViolation(exc)) {
                throw new FxEntryExistsException("ex.workflow.exists", workflow.getName());
            } else {
                throw new FxCreateException(LOG, "ex.workflow.create", exc, exc.getMessage());
            }
        } finally {
            Database.closeObjects(WorkflowEngineBean.class, con, stmt);
            if (!success) {
                EJBUtils.rollback(ctx);
            } else {
                StructureLoader.reloadWorkflows(FxContext.get().getDivisionId());
            }
        }
        return id;
    }
}
