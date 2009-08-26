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
import static com.flexive.core.DatabaseConst.ML;
import static com.flexive.core.DatabaseConst.TBL_STEPDEFINITION;
import com.flexive.core.structure.StructureLoader;
import com.flexive.shared.CacheAdmin;
import static com.flexive.shared.CacheAdmin.getEnvironment;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.FxSystemSequencer;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.Workflow;
import com.flexive.ejb.beans.EJBUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * Class for the maintainance of Global Step Definitions.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "StepDefinitionEngine", mappedName="StepDefinitionEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class StepDefinitionEngineBean implements StepDefinitionEngine, StepDefinitionEngineLocal {

    private static final Log LOG = LogFactory.getLog(StepDefinitionEngineBean.class);

    @EJB
    private StepEngineLocal stepEngine;
    @EJB
    private SequencerEngineLocal seq;
    @Resource
    private SessionContext ctx;

    /**
     * Checks if a unique target id is valid to use.
     *
     * @param id             the step definition ID to be checked
     * @param uniqueTargetId the unique target to be checked
     * @throws FxInvalidParameterException if the unique target may not be used
     */
    private void checkValidUniqueTarget(long id, long uniqueTargetId) throws FxInvalidParameterException {
        // TODO check for cycles
        if (uniqueTargetId >= 0) {
            if (id == uniqueTargetId) {
                // step may not reference itself
                throw new FxInvalidParameterException("UNIQUETARGET", LOG,
                        "ex.stepdefinition.uniqueTarget.circular.self", id);
            }
            try {
                // check if the target exists and check for cycles
                HashSet<Long> visitedStepDefinitions = new HashSet<Long>();
                if (id != -1) {
                    // must not reach "id" from unique target
                    visitedStepDefinitions.add(id);
                }
                checkForCycles(visitedStepDefinitions, uniqueTargetId);
            } catch (FxRuntimeException exc) {
                if (exc.getCause() instanceof FxNotFoundException) {
                    // provide a clear message if the unique target does not exist
                    throw new FxInvalidParameterException("UNIQUETARGET", LOG,
                            "ex.stepdefinition.uniqueTarget.notFound");
                } else {
                    throw exc;
                }
            }
        }
    }

    /**
     * Follows the unique target of the current node and checks if there are cycles
     * in the step definition graph.
     *
     * @param visitedStepDefinitions a set storing already visited nodes
     * @param stepDefinitionId       the currently visited node ID
     * @throws com.flexive.shared.exceptions.FxInvalidParameterException
     *          if a cycle was detected
     */
    private void checkForCycles(Set<Long> visitedStepDefinitions, long stepDefinitionId) throws FxInvalidParameterException {
        if (!visitedStepDefinitions.add(stepDefinitionId)) {
            String id = "" + stepDefinitionId;
            try {
                id = CacheAdmin.getEnvironment().getStepDefinition(stepDefinitionId).getLabel() +
                        " (Id: " + stepDefinitionId + ")";
            } catch (Exception e) {
                //ignore
            }
            throw new FxInvalidParameterException("UNIQUETARGET", LOG,
                    "ex.stepdefinition.uniqueTarget.circular", id);
        }
        StepDefinition sd = getEnvironment().getStepDefinition(stepDefinitionId);
        if (sd.getUniqueTargetId() != -1) {
            checkForCycles(visitedStepDefinitions, sd.getUniqueTargetId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long create(StepDefinition stepDefinition)
            throws FxApplicationException {

        final UserTicket ticket = FxContext.getUserTicket();
        // Security checks
        FxPermissionUtils.checkRole(ticket, Role.WorkflowManagement);
        // Create the new step
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        FxString label = stepDefinition.getLabel();
        if (StringUtils.isEmpty(stepDefinition.getName()))
            throw new FxInvalidParameterException("NAME", "ex.stepdefinition.name.empty");
        if (label.isEmpty())
            throw new FxInvalidParameterException("LABEL", "ex.stepdefinition.label.empty");
        String name = stepDefinition.getName();
        long uniqueTargetId = stepDefinition.getUniqueTargetId();
        boolean success = false;
        long newId = -1;
        try {
            // Check the unique target
            checkValidUniqueTarget(-1, uniqueTargetId);

            if (StringUtils.isBlank(label.getDefaultTranslation())) {
                FxInvalidParameterException ip = new FxInvalidParameterException(
                        "NAME", "ex.stepdefinition.name.empty");
                if (LOG.isDebugEnabled()) LOG.debug(ip);
                throw ip;
            }

            // Obtain a database connection
            con = Database.getDbConnection();

            // Create the new workflow instance
            sql = "INSERT INTO " + TBL_STEPDEFINITION + " (ID,NAME,UNIQUE_TARGET) VALUES (?,?,?)";
            ps = con.prepareStatement(sql);
            newId = seq.getId(FxSystemSequencer.STEPDEFINITION);
            ps.setLong(1, newId);
            ps.setString(2, name);
            if (uniqueTargetId != -1) {
                ps.setLong(3, uniqueTargetId);
            } else {
                ps.setNull(3, Types.NUMERIC);
            }
            if (ps.executeUpdate() != 1)
                throw new FxCreateException(LOG, "ex.stepdefinition.create");
            Database.storeFxString(label, con, TBL_STEPDEFINITION, "name", "id", newId);
            success = true;
        } catch (FxInvalidParameterException exc) {
            throw exc;
        } catch (Exception exc) {
            if (StorageManager.isUniqueConstraintViolation(exc)) {
                FxEntryExistsException ee = new FxEntryExistsException("ex.stepdefinition.name.exists",
                        name);
                if (LOG.isDebugEnabled()) LOG.debug(ee);
                throw ee;
            } else {
                FxCreateException ce = new FxCreateException(LOG, "ex.stepdefinition.create", exc);
                LOG.error("Internal error: " + exc.getMessage(), ce);
                throw ce;
            }
        } finally {
            Database.closeObjects(StepDefinitionEngineBean.class, con, ps);
            if (!success) {
                EJBUtils.rollback(ctx);
            } else {
                StructureLoader.reloadWorkflows(FxContext.get().getDivisionId());
            }
        }
        return newId;
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void update(StepDefinition stepDefinition)
            throws FxApplicationException {

        final UserTicket ticket = FxContext.getUserTicket();
        final FxContext ri = FxContext.get();

        // Security checks
        FxPermissionUtils.checkRole(ticket, Role.WorkflowManagement);

        StepDefinition orgDefinition;
        try {
            // Lookup the stepDefinition, throws FxNotFoundException (+FxDbException)
            orgDefinition = getEnvironment().getStepDefinition(stepDefinition.getId());
        } catch (Exception exc) {
            throw new FxUpdateException(exc.getMessage(), exc);
        }

        // Unique target checks
        boolean uniqueTargetAdded = stepDefinition.getId() != -1 && stepDefinition.getUniqueTargetId() != -1
                && stepDefinition.getUniqueTargetId() != orgDefinition.getUniqueTargetId();

        // Check the unique target, throws FxInvalidParameterException if target is invalid (+FxDbException)
        if (stepDefinition.getUniqueTargetId() != -1) {
            checkValidUniqueTarget(stepDefinition.getId(), stepDefinition.getUniqueTargetId());
        }

        // Sanity checks
        if (stepDefinition.getLabel() == null || stepDefinition.getLabel().isEmpty()) {
            throw new FxInvalidParameterException("NAME", "ex.stepdefinition.name.empty");
        }

        Connection con = null;
        PreparedStatement stmt = null;
        String sql;
        boolean success = false;
        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // store label
            Database.storeFxString(stepDefinition.getLabel(), con, TBL_STEPDEFINITION,
                    "name", "id", stepDefinition.getId());

            sql = "UPDATE " + TBL_STEPDEFINITION + " SET NAME=?,UNIQUE_TARGET=? WHERE ID=?";
            stmt = con.prepareStatement(sql);
            stmt.setString(1, stepDefinition.getName());
            if (stepDefinition.getUniqueTargetId() != -1) {
                stmt.setLong(2, stepDefinition.getUniqueTargetId());
            } else {
                stmt.setNull(2, Types.NUMERIC);
            }
            stmt.setLong(3, stepDefinition.getId());


            int updateCount = stmt.executeUpdate();

            if (updateCount == 0) {
                FxNotFoundException nfe = new FxNotFoundException("ex.stepdefinition.load.notFound",
                        stepDefinition.getId());
                if (LOG.isInfoEnabled()) LOG.info(nfe);
                throw nfe;
            } else if (updateCount != 1) {
                FxUpdateException dbe = new FxUpdateException("ex.stepdefinition.update.rows");
                LOG.error(dbe);
                throw dbe;
            }

            // Unique target has to exist for every workflow
            long workflowId;
            if (uniqueTargetAdded) {
                try {
                    ri.runAsSystem();
                    List<StepDefinition> stepDefinitionList = new ArrayList<StepDefinition>();
                    stepDefinitionList.add(stepDefinition);
                    // Do this for all existing workflows ..
                    for (Workflow workflow : getEnvironment().getWorkflows()) {
                        workflowId = workflow.getId();
                        if (FxSharedUtils.getUsedStepDefinitions(workflow.getSteps(), stepDefinitionList).size() > 0) {
                            // create step IF the step definition is used by the workflow
                            stepEngine.createStep(new Step(-1, stepDefinition.getUniqueTargetId(),
                                    workflowId, getEnvironment().getStepByDefinition(workflowId,
                                    stepDefinition.getId()).getAclId()));
                        }
                    }
                } catch (Exception exc) {
                    throw new FxUpdateException(LOG, "ex.stepdefinition.uniqueTarget.create");
                } finally {
                    ri.stopRunAsSystem();
                }
            }
            success = true;
        } catch (SQLException exc) {
            if (StorageManager.isUniqueConstraintViolation(exc)) {
                FxEntryExistsException ee = new FxEntryExistsException("ex.stepdefinition.name.exists",
                        stepDefinition.getName());
                if (LOG.isDebugEnabled()) LOG.debug(ee);
                throw ee;
            }
            throw new FxUpdateException(LOG, exc, "ex.db.sqlError", exc.getMessage());
        } finally {
            Database.closeObjects(StepDefinitionEngineBean.class, con, stmt);
            if (!success) {
                EJBUtils.rollback(ctx);
            } else {
                StructureLoader.reloadWorkflows(FxContext.get().getDivisionId());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(long id)
            throws FxApplicationException {

        final UserTicket ticket = FxContext.getUserTicket();

        // Cannot delete basis step
        if (id == StepDefinition.LIVE_STEP_ID || id == StepDefinition.EDIT_STEP_ID) {
            throw new FxNoAccessException("ex.stepdefinition.delete.system");
        }

        // Security checks
        FxPermissionUtils.checkRole(ticket, Role.WorkflowManagement);

        // Check existance
        getEnvironment().getStepDefinition(id);

        // Now try to delete the stepDefinition
        Connection con = null;
        Statement stmt = null;
        String sql;
        boolean success = false;
        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // Read all stepDefinitions from the database
            stmt = con.createStatement();
            sql = "DELETE FROM " + TBL_STEPDEFINITION + ML + " WHERE ID=" + id;
            stmt.executeUpdate(sql);
            sql = "SELECT COUNT(*) FROM " + TBL_STEPDEFINITION + " WHERE UNIQUE_TARGET=" + id;
            ResultSet rs = stmt.executeQuery(sql);
            rs.next();
            if (rs.getInt(1) > 0) {
                FxRemoveException dbe = new FxRemoveException("ex.stepdefinition.delete.used");
                LOG.error(dbe);
                throw dbe;
            }
            sql = "DELETE FROM " + TBL_STEPDEFINITION + " WHERE ID=" + id;
            if (stmt.executeUpdate(sql) == 0) {
                FxEntryInUseException eiu = new FxEntryInUseException("ex.stepdefinition.load.notFound");
                if (LOG.isInfoEnabled()) LOG.info(eiu);
                throw eiu;
            }
            success = true;
        } catch (SQLException exc) {
            // TODO add Database.childRecordsExistViolation
            /*if (Database.childRecordsExistViolation(exc)) {
                   FxDeleteException dbe = new FxDeleteException("The StepDefinition is in use", exc);
                   LOG.error(dbe);
                   throw dbe;
               } else {*/
            throw new FxRemoveException(LOG, "ex.stepdefinition.delete", exc);
            //}
        } finally {
            Database.closeObjects(StepDefinitionEngineBean.class, con, stmt);
            if (!success) {
                EJBUtils.rollback(ctx);
            } else {
                StructureLoader.reloadWorkflows(FxContext.get().getDivisionId());
            }
        }
    }

}
