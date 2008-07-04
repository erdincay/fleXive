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
package com.flexive.shared.structure;

import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptMapping;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.workflow.Route;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.Workflow;

import java.io.Serializable;
import java.util.List;


/**
 * Runtime object for structure metadata held in the cache
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface FxEnvironment extends Serializable {

    /**
     * Get data types
     *
     * @return a list of all data types
     */
    List<FxDataType> getDataTypes();

    /**
     * Get a data type by its id
     *
     * @param id data type id
     * @return FxDataType
     */
    FxDataType getDataType(long id);

    /**
     * Get an ACL by its id
     *
     * @param id ACL id
     * @return ACL
     */
    ACL getACL(long id);

    /**
     * Get an ACL by its name
     *
     * @param name name of the ACL (case sensitive!)
     * @return ACL the found ACL
     */
    ACL getACL(String name);

    /**
     * Get all ACL's
     *
     * @return ACL's
     */
    List<ACL> getACLs();

    /**
     * Return all ACLs of the given category.
     *
     * @param category ACL category to be filtered
     * @return all ACLs of the given category.
     */
    List<ACL> getACLs(ACL.Category category);

    /**
     * Return all ACLs of a given mandatorId.
     *
     * @param mandatorId the function returns all ACLs from this mandatorId
     * @return all ACLs of the given mandator
     */
    List<ACL> getACLs(long mandatorId);

    /**
     * Return all ACLs of a given mandatorId and category.
     * <p/>
     *
     * @param mandatorId               the function loads all ACLs from this mandatorId.
     * @param includeForeignAccessible when true, also loads ACLs that the calling user has at least one permission on,
     *                                 even if they belong to another mandator.
     * @return the ACLs
     */
    List<ACL> getACLs(long mandatorId, boolean includeForeignAccessible);

    /**
     * Return all ACLs of a given mandatorId and category.
     * <p/>
     *
     * @param mandatorId               the function loads all ACLs from this mandatorId.
     * @param category                 a ACL.CATEGORY_... constant or <code>-1</code> if the category should be ignored
     * @param includeForeignAccessible when true, also loads ACLs that the calling user has at least one permission on,
     *                                 even if they belong to another mandator.
     * @return the ACLs
     */
    List<ACL> getACLs(long mandatorId, ACL.Category category, boolean includeForeignAccessible);

    /**
     * Get the default ACL for given categors
     *
     * @param category the category to get a default ACL for
     * @return the default ACL for given categors
     */
    ACL getDefaultACL(ACL.Category category);

    /**
     * Get a Workflow by its id
     *
     * @param id workflow id
     * @return Workflow
     */
    Workflow getWorkflow(long id);

    /**
     * Get a Workflow by its name
     *
     * @param name workflow name
     * @return Workflow
     */
    Workflow getWorkflow(String name);

    /**
     * Get all workflows
     *
     * @return all workflows
     */
    List<Workflow> getWorkflows();

    /**
     * Get a mandator by its id
     *
     * @param id the mandator id
     * @return the mandator with the given id
     */
    Mandator getMandator(long id);

    /**
     * Get a mandator by its name
     *
     * @param name the mandator name
     * @return the mandator of the given name
     */
    Mandator getMandator(String name);

    /**
     * Get a list of all mandators depending on selection criteria
     *
     * @param active   return active mandators?
     * @param inactive return inactive mandators?
     * @return list of all mandators depending on selection criteria
     */
    List<Mandator> getMandators(boolean active, boolean inactive);

    /**
     * Get groups depending on selection criteria
     *
     * @param returnReferenced   return groups that are referenced from a type
     * @param returnUnreferenced return groups that are not referenced from a type
     * @param returnRootGroups   return groups from the 'root' level (only checked if returning referenced)
     * @param returnSubGroups    return groups that are subgroups of other groups (only checked if returning referenced)
     * @return FxGroup ArrayList
     */
    List<FxGroup> getGroups(boolean returnReferenced, boolean returnUnreferenced,
                            boolean returnRootGroups, boolean returnSubGroups);

    /**
     * Get a group identified by its id
     *
     * @param id group id
     * @return FxGroup
     */
    FxGroup getGroup(long id);

    /**
     * Get a group identified by its name
     *
     * @param name group name
     * @return FxGroup
     */
    FxGroup getGroup(String name);

    /**
     * Get properties depending on selection criteria
     *
     * @param returnReferenced   return properties that are referenced from a type
     * @param returnUnreferenced return properties that are not referenced from a type
     * @return FxProperty iterator
     */
    List<FxProperty> getProperties(boolean returnReferenced, boolean returnUnreferenced);

    /**
     * Get a property identified by its id
     *
     * @param id property id
     * @return FxProperty
     */
    FxProperty getProperty(long id);

    /**
     * Get a property identified by its name
     *
     * @param name property name
     * @return FxProperty
     */
    FxProperty getProperty(String name);

    /**
     * Check if a property with the requested name exists
     *
     * @param name name of the property
     * @return exists
     */
    boolean propertyExists(String name);

    /**
     * Check if an assignment with the requested name exists
     *
     * @param name name of the assignment
     * @return exists
     */
    boolean assignmentExists(String name);

    /**
     * Check if a group with the requested name exists
     *
     * @param name name of the group
     * @return exists
     */
    boolean groupExists(String name);

    /**
     * Get all property assignments that are enabled
     *
     * @return enabled property assignments
     */
    List<FxPropertyAssignment> getPropertyAssignments();

    /**
     * Get all system internal property assignments that are connected
     * to the virtual root type
     *
     * @return system internal property assignments
     */
    List<FxPropertyAssignment> getSystemInternalRootPropertyAssignments();

    /**
     * Get all property assignments, optionally including disabled
     *
     * @param includeDisabled include disabled assignments?
     * @return property assignments, optionally including disabled
     */
    List<FxPropertyAssignment> getPropertyAssignments(boolean includeDisabled);

    /**
     * Get all group assignments that are enabled
     *
     * @return enabled group assignments
     */
    List<FxGroupAssignment> getGroupAssignments();

    /**
     * Get all group assignments, optionally including disabled
     *
     * @param includeDisabled include disabled assignments?
     * @return group assignments, optionally including disabled
     */
    List<FxGroupAssignment> getGroupAssignments(boolean includeDisabled);

    /**
     * Get a FxGroupAssignment or FxPropertyAssignment that matches for XPath (has to include the type/relation name as root)
     *
     * @param xPath requested XPath
     * @return FxGroupAssignment or FxPropertyAssignment that matches for XPath (has to include the type/relation name as root)
     */
    FxAssignment getAssignment(String xPath);

    /**
     * Get an assignment by its id
     *
     * @param assignmentId assignment id
     * @return the assignment
     */
    FxAssignment getAssignment(long assignmentId);

    /**
     * Get all assignments that are derived from the requested
     *
     * @param assignmentId the assignment whose derived children are sought after
     * @return derived assignments
     */
    List<FxAssignment> getDerivedAssignments(long assignmentId);

    /**
     * Get a type or relation identified by its name
     *
     * @param name type name
     * @return type or relation identified by its name
     */
    FxType getType(String name);

    /**
     * Check if a type with the given name exists
     *
     * @param name type name to check for
     * @return true if the type exists
     */
    boolean typeExists(String name);

    /**
     * Get types depending on selection criteria
     *
     * @param returnBaseTypes    return types that are not derived from another type
     * @param returnDerivedTypes return types that are derived from another type
     * @param returnTypes        return FxTypes
     * @param returnRelations    return FxTypes that are relations
     * @return FxType iterator
     */
    List<FxType> getTypes(boolean returnBaseTypes, boolean returnDerivedTypes,
                          boolean returnTypes, boolean returnRelations);

    /**
     * Gets relation types that contain the type with the specified id as source
     * or destination of their relations.
     *
     * @param typeId id which is referenced by relations
     * @return list of relation types that contain relations to the type with the specified id
     */
    List<FxType> getReferencingRelationTypes(long typeId);

    /**
     * Get a type or relation identified by its id
     *
     * @param id type id
     * @return type or relation identified by its id
     */
    FxType getType(long id);

    /**
     * Returns a step defined by its unique id.
     *
     * @param id the unique step definition id
     * @return the step definition object
     */
    StepDefinition getStepDefinition(long id);

    /**
     * Get all defined step definitions
     *
     * @return ArrayList<StepDefinition>
     */
    List<StepDefinition> getStepDefinitions();

    /**
     * Get all defined steps
     *
     * @return sll defined steps
     */
    List<Step> getSteps();

    /**
     * Return the step defined by its unique step definition id and workflow.
     *
     * @param stepDefinitionId the step definition id
     * @param workflowId       the workflow the step has to be in
     * @return the matching step
     */
    Step getStepByDefinition(long workflowId, long stepDefinitionId);

    /**
     * Return all steps using a given stepDefinition..
     *
     * @param stepDefinitionId the step definition id
     * @return the matching steps, is a empty result if the stepdefinitionId does not exist or if no steps were found
     */
    List<Step> getStepsByDefinition(long stepDefinitionId);

    /**
     * Return the steps assigned to a given workflow.
     *
     * @param workflowId workflow to return the step definitions for
     * @return the steps assigned to a given workflow.
     */
    List<Step> getStepsByWorkflow(long workflowId);

    /**
     * Get a Step by its id
     *
     * @param stepId step id
     * @return Step
     */
    Step getStep(long stepId);

    /**
     * Get a Step by its name
     *
     * @param workflowId id of the workflow
     * @param stepName   name of the step
     * @return Step
     */
    Step getStep(long workflowId, String stepName);

    /**
     * Load a given route.
     *
     * @param routeId the route to be loaded
     * @return the requested route
     */
    Route getRoute(long routeId);

    /**
     * Get all available select lists
     *
     * @return all available select lists
     */
    List<FxSelectList> getSelectLists();

    /**
     * Get the selectlist with the given id
     *
     * @param id requested select list id
     * @return select list
     */
    FxSelectList getSelectList(long id);

    /**
     * Get the selectlist with the given name
     *
     * @param name requested select list name
     * @return select list
     */
    FxSelectList getSelectList(String name);

    /**
     * Get the selectlist item with the given id
     *
     * @param id requested select list item id
     * @return select list item
     */
    FxSelectListItem getSelectListItem(long id);

    /**
     * Get the selectlist item with the given name in the given list
     *
     * @param list the list containing the item
     * @param name requested select list item name
     * @return select list item
     */
    FxSelectListItem getSelectListItem(FxSelectList list, String name);

    /**
     * Get all scripts
     *
     * @return all scripts
     */
    List<FxScriptInfo> getScripts();

    /**
     * Get all script mappings
     *
     * @return all script mappings
     */
    List<FxScriptMapping> getScriptMappings();

    /**
     * Get the script mapping for the requested script
     *
     * @param scriptId requested script id
     * @return script mapping
     */
    FxScriptMapping getScriptMapping(long scriptId);

    /**
     * Get a script by its id
     *
     * @param scriptId requested script id
     * @return the script info object
     */
    FxScriptInfo getScript(long scriptId);

    /**
     * Get a script by its name (which is unique)
     *
     * @param name requested script name
     * @return the script info object
     */
    FxScriptInfo getScript(String name);

    /**
     * Get the timestamp when this environment was loaded
     *
     * @return timestamp when this environment was loaded
     */
    long getTimeStamp();

    /**
     * Get a list of all types that have assignments of the requested property
     *
     * @param propertyId requested propery
     * @return list of all types that have assignments of the requested property
     */
    List<FxType> getTypesForProperty(long propertyId);

    /**
     * Get a comma separated list of inactive mandators
     *
     * @return comma separated list of inactive mandators
     */
    String getInactiveMandatorList();

    /**
     * Get a comma separated list of deactivated FxTypes
     *
     * @return comma separated list of deactivated FxTypes
     */
    String getDeactivatedTypesList();
}
