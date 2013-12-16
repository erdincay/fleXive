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
package com.flexive.shared.structure;

import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptMapping;
import com.flexive.shared.scripting.FxScriptSchedule;
import com.flexive.shared.security.*;
import com.flexive.shared.workflow.Route;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.Workflow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

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
    @Override
    public List<StepDefinition> getStepDefinitions() {
        return environment.getStepDefinitions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StepDefinition getStepDefinition(long id) {
        return environment.getStepDefinition(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StepDefinition getStepDefinition(String name) {
        return environment.getStepDefinition(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Step getStepByDefinition(long workflowId, long stepDefinitionId) {
        return environment.getStepByDefinition(workflowId, stepDefinitionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Step> getStepsByDefinition(long stepDefinitionId) {
        return environment.getStepsByDefinition(stepDefinitionId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Step> getStepsByWorkflow(long workflowId) {
        return environment.getStepsByWorkflow(workflowId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Step getStep(long stepId) {
        return environment.getStep(stepId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Step getStep(long workflowId, String stepName) {
        return environment.getStep(workflowId, stepName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Step> getSteps() {
        return environment.getSteps();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxDataType> getDataTypes() {
        return environment.getDataTypes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxDataType getDataType(long id) {
        return environment.getDataType(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ACL getACL(long id) {
        return environment.getACL(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ACL getACL(String name) {
        return environment.getACL(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean aclExists(String name) {
        return environment.aclExists(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ACL> getACLs() {
        return environment.getACLs();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ACL> getACLs(ACLCategory category) {
        return environment.getACLs(category);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ACL> getACLs(long mandatorId) {
        return getACLs(mandatorId, null, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<ACL> getACLs(long mandatorId, boolean includeForeignAccessible) {
        return getACLs(mandatorId, null, includeForeignAccessible);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public ACL getDefaultACL(ACLCategory category) {
        return environment.getDefaultACL(category);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Workflow getWorkflow(long id) {
        return environment.getWorkflow(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Workflow getWorkflow(String name) {
        return environment.getWorkflow(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Workflow> getWorkflows() {
        return environment.getWorkflows();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mandator getMandator(long id) {
        final Mandator mandator = environment.getMandator(id);
        if (!mandator.isActive())
            throw new FxNotFoundException("ex.structure.mandator.notFound.id", id).asRuntimeException();
        return mandator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Mandator getMandator(String name) {
        final Mandator mandator = environment.getMandator(name);
        if (!mandator.isActive())
            throw new FxNotFoundException("ex.structure.mandator.notFound.name", name).asRuntimeException();
        return mandator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Mandator> getMandators(boolean active, boolean inactive) {
        return environment.getMandators(active, inactive);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxGroup> getGroups(boolean returnReferenced, boolean returnUnreferenced, boolean returnRootGroups, boolean returnSubGroups) {
        return environment.getGroups(returnReferenced, returnUnreferenced, returnRootGroups, returnSubGroups);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxGroup getGroup(long id) {
        return environment.getGroup(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxGroup getGroup(String name) {
        return environment.getGroup(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxProperty> getProperties(boolean returnReferenced, boolean returnUnreferenced) {
        return environment.getProperties(returnReferenced, returnUnreferenced);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxProperty getProperty(long id) {
        return environment.getProperty(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxProperty getProperty(String name) {
        return environment.getProperty(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean propertyExists(String name) {
        return environment.propertyExists(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean propertyExistsInType(String typeName, String propertyName) {
        return environment.propertyExistsInType(typeName, propertyName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean groupExistsInType(String typeName, String groupName) {
        return environment.groupExistsInType(typeName, groupName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean assignmentExists(String xPath) {
        return environment.assignmentExists(xPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean groupExists(String name) {
        return environment.groupExists(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public List<FxPropertyAssignment> getSystemInternalRootPropertyAssignments() {
        return environment.getSystemInternalRootPropertyAssignments();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxPropertyAssignment> getPropertyAssignments(boolean includeDisabled) {
        return environment.getPropertyAssignments(includeDisabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxPropertyAssignment> getPropertyAssignments(long propertyId, boolean includeDisabled) {
        return environment.getPropertyAssignments(propertyId, includeDisabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxGroupAssignment> getGroupAssignments() {
        return environment.getGroupAssignments();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxGroupAssignment> getGroupAssignments(boolean includeDisabled) {
        return environment.getGroupAssignments(includeDisabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public List<FxType> getTypes() {
        return filterReadableTypes(getTypes(true, true, true, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxType> getTypes(boolean returnBaseTypes, boolean returnDerivedTypes, boolean returnTypes, boolean returnRelations) {
        return filterReadableTypes(environment.getTypes(returnBaseTypes, returnDerivedTypes, returnTypes, returnRelations));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxType> getReferencingRelationTypes(long typeId) {
        return filterReadableTypes(environment.getReferencingRelationTypes(typeId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxAssignment getAssignment(String xPath) {
        return environment.getAssignment(xPath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxAssignment getAssignment(long assignmentId) {
        return environment.getAssignment(assignmentId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxAssignment> getDerivedAssignments(long assignmentId) {
        return environment.getDerivedAssignments(assignmentId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxPropertyAssignment> getReferencingPropertyAssignments(long propertyId) {
        return environment.getReferencingPropertyAssignments(propertyId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxType getType(String name) {
        return environment.getType(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean typeExists(String name) {
        return environment.typeExists(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxType getType(long id) {
        return environment.getType(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxType> getTypesForProperty(long propertyId) {
        return environment.getTypesForProperty(propertyId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Route getRoute(long routeId) {
        return environment.getRoute(routeId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxScriptInfo> getScripts() {
        return environment.getScripts();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxScriptInfo getScript(long scriptId) {
        return environment.getScript(scriptId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxScriptInfo getScript(String name) {
        return environment.getScript(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean scriptExists(String name) {
        return environment.scriptExists(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxScriptMapping> getScriptMappings() {
        return environment.getScriptMappings();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxScriptMapping getScriptMapping(long scriptId) {
        return environment.getScriptMapping(scriptId);
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.1.2
     */
    @Override
    public List<FxScriptSchedule> getScriptSchedules() {
        return environment.getScriptSchedules();
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.1.2
     */
    @Override
    public FxScriptSchedule getScriptSchedule(long scriptScheduleId) {
        return environment.getScriptSchedule(scriptScheduleId);
    }

    /**
     * {@inheritDoc}
     *
     * @since 3.1.2
     */
    @Override
    public List<FxScriptSchedule> getScriptSchedulesForScript(long scriptId) {
        return environment.getScriptSchedulesForScript(scriptId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTimeStamp() {
        return environment.getTimeStamp();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxSelectList> getSelectLists() {
        return environment.getSelectLists();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxSelectList getSelectList(long id) {
        return environment.getSelectList(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxSelectList getSelectList(String name) {
        return environment.getSelectList(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean selectListExists(String name) {
        return environment.selectListExists(name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxSelectListItem getSelectListItem(long id) {
        return environment.getSelectListItem(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxSelectListItem getSelectListItem(FxSelectList list, String name) {
        return environment.getSelectListItem(list, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInactiveMandatorList() {
        return environment.getInactiveMandatorList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDeactivatedTypesList() {
        return environment.getDeactivatedTypesList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxPropertyAssignment getPropertyAssignment(String xpath) {
        return environment.getPropertyAssignment(xpath);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxPropertyAssignment getPropertyAssignment(long id) {
        return environment.getPropertyAssignment(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxFlatStorageMapping> getFlatStorageMappings(String storage, long typeId, int level) {
        return environment.getFlatStorageMappings(storage, typeId, level);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxLanguage getLanguage(long id) {
        return environment.getLanguage(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxLanguage getLanguage(String isoCode) {
        return environment.getLanguage(isoCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxLanguage> getLanguages() {
        return environment.getLanguages();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLanguageActive(long id) {
        return environment.isLanguageActive(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isLanguageActive(String isoCode) {
        return environment.isLanguageActive(isoCode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxType getMimeTypeMatch(String mimeType) {
        return environment.getMimeTypeMatch(mimeType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<UserGroup> getUserGroups() {
        return environment.getUserGroups();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserGroup getUserGroup(long id) {
        return environment.getUserGroup(id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserGroup getUserGroup(String name) {
        return environment.getUserGroup(name);
    }
}
