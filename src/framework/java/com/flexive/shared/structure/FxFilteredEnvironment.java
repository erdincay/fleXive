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
package com.flexive.shared.structure;

import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptMapping;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.workflow.Route;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.Workflow;

import java.util.*;

/**
 * <p>
 * An environment wrapper that returns only data visible for the calling user.
 * </p>
 * <p>
 * The default FxEnvironment implementation represents the global flexive environment and does
 * not filter contents based on the current user. This class performs additional checks and should
 * be used for retrieving environment data that is displayed to the user.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@SuppressWarnings({"ThrowableInstanceNeverThrown"})
public final class FxFilteredEnvironment implements FxEnvironment {
    private static final long serialVersionUID = 4401591013752540846L;

    private final FxEnvironment environment;

    public FxFilteredEnvironment(FxEnvironment environment) {
        this.environment = environment;
    }

    /**
     * {@inheritDoc}
     */
    public List<StepDefinition> getStepDefinitions() {
        return environment.getStepDefinitions();
    }

    /**
     * {@inheritDoc}
     */
    public StepDefinition getStepDefinition(long id) {
        return environment.getStepDefinition(id);
    }

    /**
     * {@inheritDoc}
     */
    public StepDefinition getStepDefinition(String name) {
        return environment.getStepDefinition(name);
    }

    /**
     * {@inheritDoc}
     */
    public Step getStepByDefinition(long workflowId, long stepDefinitionId) {
        return environment.getStepByDefinition(workflowId, stepDefinitionId);
    }

    /**
     * {@inheritDoc}
     */
    public List<Step> getStepsByDefinition(long stepDefinitionId) {
        return environment.getStepsByDefinition(stepDefinitionId);
    }

    /**
     * {@inheritDoc}
     */
    public List<Step> getStepsByWorkflow(long workflowId) {
        return environment.getStepsByWorkflow(workflowId);
    }

    /**
     * {@inheritDoc}
     */
    public Step getStep(long stepId) {
        return environment.getStep(stepId);
    }

    /**
     * {@inheritDoc}
     */
    public Step getStep(long workflowId, String stepName) {
        return environment.getStep(workflowId, stepName);
    }

    /**
     * {@inheritDoc}
     */
    public List<Step> getSteps() {
        return environment.getSteps();
    }

    /**
     * {@inheritDoc}
     */
    public List<FxDataType> getDataTypes() {
        return environment.getDataTypes();
    }

    /**
     * {@inheritDoc}
     */
    public FxDataType getDataType(long id) {
        return environment.getDataType(id);
    }

    /**
     * {@inheritDoc}
     */
    public ACL getACL(long id) {
        return environment.getACL(id);
    }

    /**
     * {@inheritDoc}
     */
    public ACL getACL(String name) {
        return environment.getACL(name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean aclExists(String name) {
        return environment.aclExists(name);
    }

    /**
     * {@inheritDoc}
     */
    public List<ACL> getACLs() {
        return environment.getACLs();
    }

    /**
     * {@inheritDoc}
     */
    public List<ACL> getACLs(ACLCategory category) {
        return environment.getACLs(category);
    }

    /**
     * {@inheritDoc}
     */
    public List<ACL> getACLs(long mandatorId) {
        return getACLs(mandatorId, null, true);
    }

    /**
     * {@inheritDoc}
     */
    public List<ACL> getACLs(long mandatorId, boolean includeForeignAccessible) {
        return getACLs(mandatorId, null, includeForeignAccessible);
    }

    /**
     * {@inheritDoc}
     */
    public List<ACL> getACLs(long mandatorId, ACLCategory category, boolean includeForeignAccessible) {
        final UserTicket ticket = FxContext.getUserTicket();
        if (!ticket.isGlobalSupervisor() && mandatorId != ticket.getMandatorId()) {
            throw new FxNoAccessException("ex.acl.loadAllFailed.foreignMandator").asRuntimeException();
        }
        return environment.getACLs(mandatorId, category, includeForeignAccessible);
    }

    /**
     * {@inheritDoc}
     */
    public ACL getDefaultACL(ACLCategory category) {
        return environment.getDefaultACL(category);
    }

    /**
     * {@inheritDoc}
     */
    public Workflow getWorkflow(long id) {
        return environment.getWorkflow(id);
    }

    /**
     * {@inheritDoc}
     */
    public Workflow getWorkflow(String name) {
        return environment.getWorkflow(name);
    }

    /**
     * {@inheritDoc}
     */
    public List<Workflow> getWorkflows() {
        return environment.getWorkflows();
    }

    /**
     * {@inheritDoc}
     */
    public Mandator getMandator(long id) {
        final Mandator mandator = environment.getMandator(id);
        if (!mandator.isActive())
            throw new FxNotFoundException("ex.structure.mandator.notFound.id", id).asRuntimeException();
        return mandator;
    }

    /**
     * {@inheritDoc}
     */
    public Mandator getMandator(String name) {
        final Mandator mandator = environment.getMandator(name);
        if (!mandator.isActive())
            throw new FxNotFoundException("ex.structure.mandator.notFound.name", name).asRuntimeException();
        return mandator;
    }

    /**
     * {@inheritDoc}
     */
    public List<Mandator> getMandators(boolean active, boolean inactive) {
        return environment.getMandators(active, inactive);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxGroup> getGroups(boolean returnReferenced, boolean returnUnreferenced, boolean returnRootGroups, boolean returnSubGroups) {
        return environment.getGroups(returnReferenced, returnUnreferenced, returnRootGroups, returnSubGroups);
    }

    /**
     * {@inheritDoc}
     */
    public FxGroup getGroup(long id) {
        return environment.getGroup(id);
    }

    /**
     * {@inheritDoc}
     */
    public FxGroup getGroup(String name) {
        return environment.getGroup(name);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxProperty> getProperties(boolean returnReferenced, boolean returnUnreferenced) {
        return environment.getProperties(returnReferenced, returnUnreferenced);
    }

    /**
     * {@inheritDoc}
     */
    public FxProperty getProperty(long id) {
        return environment.getProperty(id);
    }

    /**
     * {@inheritDoc}
     */
    public FxProperty getProperty(String name) {
        return environment.getProperty(name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean propertyExists(String name) {
        return environment.propertyExists(name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean propertyExistsInType(String typeName, String propertyName) {
        return environment.propertyExistsInType(typeName, propertyName);
    }

    /**
     * {@inheritDoc}
     */
    public boolean groupExistsInType(String typeName, String groupName) {
        return environment.groupExistsInType(typeName, groupName);
    }

    /**
     * {@inheritDoc}
     */
    public boolean assignmentExists(String xPath) {
        return environment.assignmentExists(xPath);
    }

    /**
     * {@inheritDoc}
     */
    public boolean groupExists(String name) {
        return environment.groupExists(name);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxPropertyAssignment> getPropertyAssignments() {
        final List<FxType> types = getTypes(true, true, true, false);
        final List<FxPropertyAssignment> assignments = new ArrayList<FxPropertyAssignment>(environment.getPropertyAssignments());
        // return only assignments for available types
        for (Iterator<FxPropertyAssignment> iterator = assignments.iterator(); iterator.hasNext();) {
            final FxPropertyAssignment assignment = iterator.next();
            if (!types.contains(assignment.getAssignedType())) {
                iterator.remove();
            }
        }
        return Collections.unmodifiableList(assignments);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxPropertyAssignment> getSystemInternalRootPropertyAssignments() {
        return environment.getSystemInternalRootPropertyAssignments();
    }

    /**
     * {@inheritDoc}
     */
    public List<FxPropertyAssignment> getPropertyAssignments(boolean includeDisabled) {
        return environment.getPropertyAssignments(includeDisabled);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxPropertyAssignment> getPropertyAssignments(long propertyId, boolean includeDisabled) {
        return environment.getPropertyAssignments(propertyId, includeDisabled);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxGroupAssignment> getGroupAssignments() {
        return environment.getGroupAssignments();
    }

    /**
     * {@inheritDoc}
     */
    public List<FxGroupAssignment> getGroupAssignments(boolean includeDisabled) {
        return environment.getGroupAssignments(includeDisabled);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxGroupAssignment> getGroupAssignments(long groupId, boolean includeDisabled) {
        return environment.getGroupAssignments(groupId, includeDisabled);
    }

    /**
     * Filter all types the user has no read permission for
     *
     * @param list list of types
     * @return filtered list containing only types the user has read permission for
     */
    private List<FxType> filterReadableTypes(List<FxType> list) {
        boolean needFilter = false;
        final UserTicket ticket = FxContext.getUserTicket();
        for (FxType t : list) {
            if (t.isUseTypePermissions() && !ticket.mayReadACL(t.getACL().getId(), -1)) {
                needFilter = true;
                break;
            }
        }
        if (!needFilter)
            return list;
        List<FxType> filtered = new ArrayList<FxType>(list.size() - 1);
        for (FxType t : list)
            if (!t.isUseTypePermissions() || ticket.mayReadACL(t.getACL().getId(), -1))
                filtered.add(t);
        return Collections.unmodifiableList(filtered);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxType> getTypes() {
        return filterReadableTypes(getTypes(true, true, true, true));
    }

    /**
     * {@inheritDoc}
     */
    public List<FxType> getTypes(boolean returnBaseTypes, boolean returnDerivedTypes, boolean returnTypes, boolean returnRelations) {
        return filterReadableTypes(environment.getTypes(returnBaseTypes, returnDerivedTypes, returnTypes, returnRelations));
    }

    /**
     * {@inheritDoc}
     */
    public List<FxType> getReferencingRelationTypes(long typeId) {
        return filterReadableTypes(environment.getReferencingRelationTypes(typeId));
    }

    /**
     * {@inheritDoc}
     */
    public FxAssignment getAssignment(String xPath) {
        return environment.getAssignment(xPath);
    }

    /**
     * {@inheritDoc}
     */
    public FxAssignment getAssignment(long assignmentId) {
        return environment.getAssignment(assignmentId);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxAssignment> getDerivedAssignments(long assignmentId) {
        return environment.getDerivedAssignments(assignmentId);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxPropertyAssignment> getReferencingPropertyAssignments(long propertyId) {
        return environment.getReferencingPropertyAssignments(propertyId);
    }

    /**
     * {@inheritDoc}
     */
    public FxType getType(String name) {
        return environment.getType(name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean typeExists(String name) {
        return environment.typeExists(name);
    }

    /**
     * {@inheritDoc}
     */
    public FxType getType(long id) {
        return environment.getType(id);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxType> getTypesForProperty(long propertyId) {
        return environment.getTypesForProperty(propertyId);
    }

    /**
     * {@inheritDoc}
     */
    public Route getRoute(long routeId) {
        return environment.getRoute(routeId);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxScriptInfo> getScripts() {
        return environment.getScripts();
    }

    /**
     * {@inheritDoc}
     */
    public FxScriptInfo getScript(long scriptId) {
        return environment.getScript(scriptId);
    }

    /**
     * {@inheritDoc}
     */
    public FxScriptInfo getScript(String name) {
        return environment.getScript(name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean scriptExists(String name) {
        return environment.scriptExists(name);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxScriptMapping> getScriptMappings() {
        return environment.getScriptMappings();
    }

    /**
     * {@inheritDoc}
     */
    public FxScriptMapping getScriptMapping(long scriptId) {
        return environment.getScriptMapping(scriptId);
    }

    /**
     * {@inheritDoc}
     */
    public long getTimeStamp() {
        return environment.getTimeStamp();
    }

    /**
     * {@inheritDoc}
     */
    public List<FxSelectList> getSelectLists() {
        return environment.getSelectLists();
    }

    /**
     * {@inheritDoc}
     */
    public FxSelectList getSelectList(long id) {
        return environment.getSelectList(id);
    }

    /**
     * {@inheritDoc}
     */
    public FxSelectList getSelectList(String name) {
        return environment.getSelectList(name);
    }

    /**
     * {@inheritDoc}
     */
    public boolean selectListExists(String name) {
        return environment.selectListExists(name);
    }

    /**
     * {@inheritDoc}
     */
    public FxSelectListItem getSelectListItem(long id) {
        return environment.getSelectListItem(id);
    }

    /**
     * {@inheritDoc}
     */
    public FxSelectListItem getSelectListItem(FxSelectList list, String name) {
        return environment.getSelectListItem(list, name);
    }

    /**
     * {@inheritDoc}
     */
    public String getInactiveMandatorList() {
        return environment.getInactiveMandatorList();
    }

    /**
     * {@inheritDoc}
     */
    public String getDeactivatedTypesList() {
        return environment.getDeactivatedTypesList();
    }

    /**
     * {@inheritDoc}
     */
    public FxPropertyAssignment getPropertyAssignment(String xpath) {
        return environment.getPropertyAssignment(xpath);
    }

    /**
     * {@inheritDoc}
     */
    public FxPropertyAssignment getPropertyAssignment(long id) {
        return environment.getPropertyAssignment(id);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxFlatStorageMapping> getFlatStorageMappings(String storage, long typeId, int level) {
        return environment.getFlatStorageMappings(storage, typeId, level);
    }

    /**
     * {@inheritDoc}
     */
    public FxLanguage getLanguage(long id) {
        return environment.getLanguage(id);
    }

    /**
     * {@inheritDoc}
     */
    public FxLanguage getLanguage(String isoCode) {
        return environment.getLanguage(isoCode);
    }

    /**
     * {@inheritDoc}
     */
    public List<FxLanguage> getLanguages() {
        return environment.getLanguages();
    }

}
