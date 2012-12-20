/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
import com.flexive.core.structure.StructureLoader;
import com.flexive.ejb.beans.EJBUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSystemSequencer;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.RouteEngine;
import com.flexive.shared.interfaces.RouteEngineLocal;
import com.flexive.shared.interfaces.SequencerEngineLocal;
import com.flexive.shared.interfaces.UserGroupEngineLocal;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.workflow.Step;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static com.flexive.core.DatabaseConst.TBL_WORKFLOW_ROUTES;
import static com.flexive.core.DatabaseConst.TBL_WORKFLOW_STEP;


/**
 * The RouteEngine class provides functions to create, modify and query routes between steps
 * within the workflows.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "RouteEngine", mappedName="RouteEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class RouteEngineBean implements RouteEngine, RouteEngineLocal {

    private static final Log LOG = LogFactory.getLog(StepEngineBean.class);

    @EJB private UserGroupEngineLocal groupEngine;
    @EJB private SequencerEngineLocal seq;
    @Resource private SessionContext ctx;

    /** {@inheritDoc} */
    public List<Step> getTargets(long fromStep) throws FxApplicationException {
        Connection con = null;
        String sql = null;
        Statement stmt = null;

        // Check fromStep
        Step step = CacheAdmin.getEnvironment().getStep(fromStep);

        try {
            final UserTicket ticket = FxContext.getUserTicket();
            con = Database.getDbConnection();
            stmt = con.createStatement();
            if (ticket.isMandatorSupervisor() || ticket.isGlobalSupervisor()) {
                sql = "SELECT ID FROM " + TBL_WORKFLOW_STEP + "  WHERE WORKFLOW="
                        + step.getWorkflowId() + " AND ID!=" + fromStep;
            } else {
                sql = "SELECT DISTINCT TO_STEP FROM " + TBL_WORKFLOW_ROUTES + " WHERE FROM_STEP=" + fromStep
                        + " AND USERGROUP IN (" + StringUtils.join(ArrayUtils.toObject(ticket.getGroups()), ',') + ")";
            }
            ResultSet rs = stmt.executeQuery(sql);
            ArrayList<Step> targets = new ArrayList<Step>(20);
            int stepId = -1;
            while (rs != null && rs.next()) {
                try {
                    stepId = rs.getInt(1);
                    targets.add(CacheAdmin.getEnvironment().getStep(stepId));
                } catch (Exception exc) {
                    LOG.error("Failed to load step " + stepId + " (skipping it), err=" + exc.getMessage(), exc);
                }
            }
            return targets;
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "ex.step.load.target", exc, fromStep, exc.getMessage());
        } finally {
            Database.closeObjects(RouteEngineBean.class, con, stmt);
        }
    }


    /** {@inheritDoc} */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long create(long fromStepId, long toStepId, long groupId) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.getUserTicket(), Role.WorkflowManagement);
        // Sanity checks.
        // StepImp.loadStep(..) throws a FxNotFoundException if the steps do not exist.
        Step fromStep;
        Step toStep;

        if (CacheAdmin.getEnvironment().getUserGroup(groupId) == null) {
            throw new FxNotFoundException("ex.usergroup.groupNotFound.id", groupId);
        }

        fromStep = CacheAdmin.getEnvironment().getStep(fromStepId);
        toStep = CacheAdmin.getEnvironment().getStep(toStepId);

        // from and to step must be in the same workflow or we may not connect them
        if (fromStep.getWorkflowId() != toStep.getWorkflowId()) {
            throw new FxInvalidParameterException("STEP_FROM", 
            		"ex.routes.create.differentWorkflows",
            		fromStepId, fromStep.getWorkflowId(), toStepId, toStep.getWorkflowId());
        }
        if (fromStepId == toStepId) {
            throw new FxInvalidParameterException("STEP_FROM", "ex.routes.create.loop");
        }

        // Create the route
        Connection con = null;
        Statement stmt = null;
        String sql;
        String routeString = "[from=" + fromStep.getId() + ",to=" + toStep.getId() + ",group=" + groupId + "]";
        boolean success = false;

        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // Create the new route
            stmt = con.createStatement();
            long routeId = seq.getId(FxSystemSequencer.ROUTE);
            sql = "INSERT INTO " + TBL_WORKFLOW_ROUTES + " (ID,FROM_STEP,TO_STEP,USERGROUP) VALUES ("
                    + routeId + "," + fromStep.getId() + "," + toStep.getId() + "," + groupId + ")";
            stmt.executeUpdate(sql);

            // Return the new id
            success = true;
            return routeId;
        } catch (SQLException exc) {
            if (StorageManager.isUniqueConstraintViolation(exc)) {
                throw new FxEntryExistsException(LOG, "ex.routes.create.exists");
            } else {
                throw new FxCreateException(LOG, "ex.routes.create", exc, routeString, exc.getMessage());
            }
        } finally {
            Database.closeObjects(RouteEngineBean.class, con, stmt);
        	if (!success) {
        		EJBUtils.rollback(ctx);
        	} else {
                StructureLoader.reloadWorkflows(FxContext.get().getDivisionId());
        	}
        }
    }


    /**
     * Deletes a route defined by its unique id.
     *
     * @param routeId   the route id
     * @throws FxApplicationException if an error occured
     */
    private void deleteRoute(long routeId) throws
            FxApplicationException {

        // Create the new step
        Connection con = null;
        PreparedStatement stmt = null;
        final String sql = "DELETE FROM " + TBL_WORKFLOW_ROUTES + " WHERE ID=?";
        boolean success = false;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();

            // Create the new workflow instance
            stmt = con.prepareStatement(sql);
            stmt.setLong(1, routeId);
            stmt.executeUpdate();
        	success = true;
        } catch (SQLException exc) {
            throw new FxRemoveException(LOG, "ex.routes.delete", exc, routeId, exc.getMessage());
        } finally {
            Database.closeObjects(RouteEngineBean.class, con, stmt);
        	if (!success) {
        		EJBUtils.rollback(ctx);
        	} else {
                StructureLoader.reloadWorkflows(FxContext.get().getDivisionId());
        	}
        }
    }

    /** {@inheritDoc} */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(long routeId) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.getUserTicket(), Role.WorkflowManagement);
        deleteRoute(routeId);
    }


}
