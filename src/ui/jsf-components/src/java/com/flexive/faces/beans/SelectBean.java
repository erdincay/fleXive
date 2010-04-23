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
package com.flexive.faces.beans;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.model.FxJSFSelectItem;
import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.UserGroupEngine;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptScope;
import com.flexive.shared.search.AdminResultLocations;
import com.flexive.shared.search.FxSQLSearchParams;
import com.flexive.shared.search.ResultViewType;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.QueryOperatorNode;
import com.flexive.shared.security.*;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.BinaryDescriptor;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Wrapper bean that provides access to most internal flexive3 select lists
 * via JSF's SelectItem.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SelectBean implements Serializable {
    private static final long serialVersionUID = 9155928639067833112L;
    private FxEnvironment environment;

    /* Cache select lists during request */
    private List<SelectItem> workflows = null;
    private List<SelectItem> userGroups = null;
    private List<SelectItem> userGroupsNonSystem = null;
    private List<SelectItem> briefcaseACLs = null;
    private List<SelectItem> instanceACLs = null;
    private List<SelectItem> globalUserGroups = null;
    private List<SelectItem> globalUserGroupsNonSystem = null;
    private List<SelectItem> languages = null;
    // if the language is changed durring the request, we need to know that and rerender the language list
    private FxLanguage languageListLanguage = null;
    private List<SelectItem> languagesById = null;
    private List<SelectItem> mandators = null;
    private List<SelectItem> mandatorsForEdit = null;
    private List<SelectItem> mandatorsForEditNoEmpty = null;
    private List<SelectItem> queryNodeOperators = null;
    private List<SelectItem> queryNodeComparators = null;
    private List<SelectItem> roles = null;
    private List<SelectItem> stepDefinitions = null;
    private List<SelectItem> structureACLs = null;
    private List<SelectItem> workflowACLs = null;
    private List<SelectItem> acls = null;
    private List<SelectItem> resultViewTypes = null;
    private List<SelectItem> resultLocations = null;
    private List<SelectItem> resultDirections = null;
    private List<SelectItem> previewSizes = null;
    private List<SelectItem> cacheModes = null;
    private List<SelectItem> uniqueModes = null;
    private List<SelectItem> dataTypes = null;
    private List<SelectItem> languageModes = null;
    private List<SelectItem> typeModes = null;
    private List<SelectItem> typeStates = null;
    private List<SelectItem> typeCategories = null;
    private List<SelectItem> groupModes = null;
    private List<SelectItem> restrictedTypeModes = null;
    private List<SelectItem> restrictedTypeCategories = null;
    private List<SelectItem> typeScriptEvents = null;
    private List<SelectItem> assignmentScripts = null;
    private List<SelectItem> assignmentScriptEvents = null;
    private List<SelectItem> allScriptEvents = null;
    private List<SelectItem> allScriptEventsAsEnum = null;
    private List<SelectItem> scriptScopes = null;
    private List<SelectItem> allScripts = null;
    private List<SelectItem> typeScripts = null;
    private List<SelectItem> aclCategories = null;
    private List<SelectItem> selectListACLs = null;
    private List<SelectItem> scriptingEngines = null;

    /**
     * Constructs a new select bean.
     */
    public SelectBean() {
        environment = CacheAdmin.getFilteredEnvironment();
    }

    /**
     * Format an item to display the items mandator if it is not the users mandator
     *
     * @param userMandator user mandator id
     * @param itemMandator item mandator id
     * @param item         item name
     * @return formatted item
     */
    private String formatMandatorSelectItem(long userMandator, long itemMandator, String item) {
        if (userMandator == itemMandator)
            return item;
        return environment.getMandator(itemMandator).getName() + ": " + item;
    }

    /**
     * Returns all workflows currently defined in flexive3.
     *
     * @return all workflows currently defined in flexive3.
     */
    public List<SelectItem> getWorkflows() {
        if (workflows == null) {
            workflows = FxJsfUtils.asSelectListWithName(environment.getWorkflows());
        }
        return workflows;
    }

    /**
     * Returns all user groups defined for the current mandator.
     *
     * @return all user groups defined for the current mandator.
     * @throws FxApplicationException if the user groups could not be fetched successfully.
     */
    public List<SelectItem> getUserGroups() throws FxApplicationException {
        if (userGroups == null) {
            UserGroupEngine groupEngine = EJBLookup.getUserGroupEngine();
            long mandatorId = FxContext.getUserTicket().getMandatorId();
            List<UserGroup> groups = groupEngine.loadAll(mandatorId);
            userGroups = FxJsfUtils.asSelectListWithName(groups);
        }
        return userGroups;
    }

    /**
     * Returns all user groups defined for the current mandator that are not flagged as system.
     *
     * @return all user groups defined for the current mandator.
     * @throws FxApplicationException if the user groups could not be fetched successfully.
     */
    public List<SelectItem> getUserGroupsNonSystem() throws FxApplicationException {
        if (userGroupsNonSystem == null) {
            UserGroupEngine groupEngine = EJBLookup.getUserGroupEngine();
            long mandatorId = FxContext.getUserTicket().getMandatorId();
            List<UserGroup> groups = groupEngine.loadAll(mandatorId);
            userGroupsNonSystem = new ArrayList<SelectItem>(groups.size());

            for (UserGroup group : groups) {
                if (group.isSystem())
                    continue;
                userGroupsNonSystem.add(new FxJSFSelectItem(group));
            }
        }
        return userGroupsNonSystem;
    }

    /**
     * Returns all user groups within the system.
     *
     * @return all user groups in the system.
     * @throws FxApplicationException if the user groups could not be fetched successfully.
     */
    public List<SelectItem> getGlobalUserGroups() throws FxApplicationException {
        if (globalUserGroups == null) {
            UserGroupEngine groupEngine = EJBLookup.getUserGroupEngine();
            List<UserGroup> groups = groupEngine.loadAll(-1);
            globalUserGroups = new ArrayList<SelectItem>(groups.size());
            long mandator = FxContext.getUserTicket().getMandatorId();
            for (UserGroup group : groups) {
                globalUserGroups.add(new FxJSFSelectItem(
                        group,
                        group.isSystem()
                                ? group.getName()
                                : formatMandatorSelectItem(mandator, group.getMandatorId(), group.getName()))
                );
            }
        }
        return globalUserGroups;
    }

    /**
     * Returns all user groups within the system that are not flagged as system.
     *
     * @return all user groups in the system.
     * @throws FxApplicationException if the user groups could not be fetched successfully.
     */
    public List<SelectItem> getGlobalUserGroupsNonSystem() throws FxApplicationException {
        if (globalUserGroupsNonSystem == null) {
            UserGroupEngine groupEngine = EJBLookup.getUserGroupEngine();
            List<UserGroup> groups = groupEngine.loadAll(-1);
            globalUserGroupsNonSystem = new ArrayList<SelectItem>(groups.size());
            long mandator = FxContext.getUserTicket().getMandatorId();
            for (UserGroup group : groups) {
                if (group.isSystem())
                    continue;
                globalUserGroupsNonSystem.add(
                        new FxJSFSelectItem(group, formatMandatorSelectItem(mandator, group.getMandatorId(), group.getName())));
            }
        }
        return globalUserGroupsNonSystem;
    }

    /**
     * Returns all types within the system.
     *
     * @return all type in the system
     * @throws FxApplicationException if the types could not be fetched successfully.
     */
    public List<SelectItem> getTypes() throws FxApplicationException {
        List<FxType> typesList = CacheAdmin.getFilteredEnvironment().getTypes(true, true, true, false);
        return FxJsfUtils.asSelectListWithLabel(typesList);
    }

    /**
     * Returns all types within the system with an empty element.
     *
     * @return all type in the system with an empty element
     * @throws FxApplicationException if the types could not be fetched successfully.
     */
    public List<SelectItem> getTypesWithEmpty() throws FxApplicationException {
        List<FxType> typesList = CacheAdmin.getFilteredEnvironment().getTypes(true, true, true, false);
        return FxJsfUtils.asSelectListWithLabel(typesList, true);
    }

    /**
     * Returns all roles.
     *
     * @return all roles
     * @throws FxApplicationException if the function fails
     */
    public List<SelectItem> getRoles() throws FxApplicationException {
        if (roles == null) {
            roles = FxJsfUtils.asSelectListWithLabel(Role.getList());
        }
        return roles;
    }

    /**
     * Returns all mandators that may be selected in a create or edit screen, plus an empty element.
     *
     * @return all mandators that may be selected in a create or edit screen
     */
    public List<SelectItem> getMandatorsForEdit() {
        if (mandatorsForEdit == null) {
            mandatorsForEdit = FxJsfUtils.asSelectListWithName(_getMandatorsForEdit(), true);
        }
        return mandatorsForEdit;
    }

    /**
     * Returns all mandators that may be selected in a create or edit screen, with no empty element.
     *
     * @return all mandators that may be selected in a create or edit screen
     */
    public List<SelectItem> getMandatorsForEditNoEmpty() {
        if (mandatorsForEditNoEmpty == null) {
            mandatorsForEditNoEmpty = FxJsfUtils.asSelectListWithName(_getMandatorsForEdit());
        }
        return mandatorsForEditNoEmpty;
    }

    /**
     * Helper function for the getMandators__ functions.
     *
     * @return a list with all mandators
     */
    private List<Mandator> _getMandatorsForEdit() {
        final UserTicket ticket = FxJsfUtils.getRequest().getUserTicket();
        List<Mandator> list;

        if (ticket.isGlobalSupervisor()) {
            list = CacheAdmin.getFilteredEnvironment().getMandators(true, false);
        } else {
            list = new ArrayList<Mandator>(1);
            Mandator mand = CacheAdmin.getFilteredEnvironment().getMandator(ticket.getMandatorId());
            list.add(mand);
        }
        return list;
    }

    /**
     * Returns all active mandators.
     *
     * @return all active mandators
     */
    public List<SelectItem> getMandators() {
        if (mandators == null) {
            List<Mandator> list = CacheAdmin.getFilteredEnvironment().getMandators(true, false);
            mandators = FxJsfUtils.asSelectListWithName(list, false);
        }
        return mandators;
    }

    /**
     * Returns all select lists including an empty element.
     *
     * @return all available selectlists including an empty element.
     */

    public List<SelectItem> getSelectListsWithEmpty() {
        List<FxSelectList> selectLists = CacheAdmin.getFilteredEnvironment().getSelectLists();
        ArrayList<SelectItem> result = new ArrayList<SelectItem>(selectLists.size() + 1);
        result.add(new FxJSFSelectItem());
        for (FxSelectList list : selectLists) {
            result.add(new FxJSFSelectItem(list));
        }
        return result;
    }

    /**
     * Return all available languages.
     *
     * @return all available languages.
     */
    public List<SelectItem> getLanguages() {
        final UserTicket ticket = FxJsfUtils.getRequest().getUserTicket();
        // we need to rerender the languageList if it is empty or the language hase changed since the last rendering
        if (languages == null || !ticket.getLanguage().equals(languageListLanguage)) {
            try {
                List<FxLanguage> list = CacheAdmin.getEnvironment().getLanguages();
                List<SelectItem> result = new ArrayList<SelectItem>(list.size());
                for (FxLanguage item : list) {
                    String label = item.getLabel().getBestTranslation(ticket);
                    result.add(new SelectItem(item, label/*label*/, label/*description*/));
                }
                languages = result;
            } catch (Exception exc) {
                new FxFacesMsgErr(exc).addToContext();
                languages = new ArrayList<SelectItem>(0);
            } finally {
                languageListLanguage = ticket.getLanguage();
            }
        }
        return languages;
    }

    public List<SelectItem> getLanguagesById() {
        if (languagesById == null) {
            try {
                languagesById = FxJsfUtils.asSelectListWithLabel(CacheAdmin.getEnvironment().getLanguages());
            } catch (Exception e) {
                new FxFacesMsgErr(e).addToContext();
                languagesById = new ArrayList<SelectItem>(0);
            }
        }
        return languagesById;
    }

    /**
     * Return all available content ACLs.
     *
     * @return all available content ACLs.
     * @deprecated since 3.1.1 use getInstanceACLs instead
     */
    public List<SelectItem> getContentACLs() {
        return getInstanceACLs();
    }

    /**
     * Return all available content ACLs.
     *
     * @return all available content ACLs.
     * @since 3.1.1
     */
    public List<SelectItem> getInstanceACLs() {
        if (instanceACLs == null) {
            instanceACLs = FxJsfUtils.asSelectListWithLabel(environment.getACLs(ACLCategory.INSTANCE));
        }
        return instanceACLs;
    }

    /**
     * Returns all available  ACLs.
     *
     * @return all available ACLs.
     */
    public List<SelectItem> getACLs() {
        if (acls == null) {
            acls = FxJsfUtils.asSelectListWithLabel(environment.getACLs());
        }
        return acls;
    }


    /**
     * Return all available briefcase ACLs.
     *
     * @return all available briefcase ACLs.
     */
    public List<SelectItem> getBriefcaseACLs() {
        if (briefcaseACLs == null) {
            briefcaseACLs = FxJsfUtils.asSelectListWithLabel(environment.getACLs(ACLCategory.BRIEFCASE));
        }
        return briefcaseACLs;
    }

    /**
     * Return all available structure ACLs.
     *
     * @return all available structure ACLs asIdSelectList.
     */
    public List<SelectItem> getStructureACLs() {
        if (structureACLs == null) {
            structureACLs = FxJsfUtils.asSelectListWithLabel(environment.getACLs(ACLCategory.STRUCTURE));
        }
        return structureACLs;
    }

    /**
     * Return all available workflow ACLs.
     *
     * @return all available workflow ACLs.
     */
    public List<SelectItem> getWorkflowACLs() {
        if (workflowACLs == null) {
            workflowACLs = FxJsfUtils.asSelectListWithLabel(environment.getACLs(ACLCategory.WORKFLOW));
        }
        return workflowACLs;
    }

    /**
     * Return all available step defintions.
     *
     * @return all available step defintions.
     */
    public List<SelectItem> getStepDefinitions() {
        if (stepDefinitions == null) {
            stepDefinitions = FxJsfUtils.asSelectListWithLabel(environment.getStepDefinitions());
        }
        return stepDefinitions;
    }

    /**
     * Return all defined properties.
     *
     * @return all defined properties.
     */
    public List<SelectItem> getProperties() {
        return FxJsfUtils.asSelectListWithLabel(environment.getProperties(true, true));
    }

    /**
     * Return all defined language modes.
     *
     * @return all defined language modes.
     */
    public List<SelectItem> getLanguageModes() {
        if (languageModes == null) {
            languageModes = FxJsfUtils.enumsAsSelectList(LanguageMode.values());
        }
        return languageModes;
    }

    /**
     * Return all defined group modes.
     *
     * @return all defined group modes.
     */
    public List<SelectItem> getGroupModes() {
        if (groupModes == null) {
            groupModes = FxJsfUtils.enumsAsSelectList(GroupMode.values());
        }
        return groupModes;
    }

    /**
     * Return all defined type modes.
     *
     * @return all defined type modes.
     */
    public List<SelectItem> getTypeModes() {
        if (typeModes == null) {
            typeModes = FxJsfUtils.enumsAsSelectList(TypeMode.values());
        }
        return typeModes;
    }

    /**
     * Return type modes that are relevant for the structure editor ui
     *
     * @return structure editor relevant type modes.
     */
    public List<SelectItem> getRestrictedTypeModes() {
        if (restrictedTypeModes == null) {
            restrictedTypeModes = FxJsfUtils.enumsAsSelectList(TypeMode.values());
            SelectItem toRemove = null;
            for (SelectItem s : restrictedTypeModes) {
                TypeMode t = (TypeMode) s.getValue();
                if (t.getId() == TypeMode.Preload.getId()) {
                    toRemove = s;
                    break;
                }
            }
            restrictedTypeModes.remove(toRemove);
        }
        return restrictedTypeModes;
    }

    /**
     * Return all defined type states.
     *
     * @return all defined type states.
     */
    public List<SelectItem> getTypeStates() {
        if (typeStates == null) {
            typeStates = FxJsfUtils.enumsAsSelectList(TypeState.values());
        }
        return typeStates;
    }

    /**
     * Return all defined type categories.
     *
     * @return all defined type categories.
     */
    public List<SelectItem> getTypeCategories() {
        if (typeCategories == null) {
            typeCategories = FxJsfUtils.enumsAsSelectList(TypeCategory.values());
            //last element should be TypeCategory.System, so if it isn't switch
            int systemCatIndex = -1;
            for (int i = 0; i < typeCategories.size(); i++) {
                TypeCategory t = (TypeCategory) typeCategories.get(i).getValue();
                if (t.getId() == TypeCategory.System.getId())
                    systemCatIndex = i;
            }
            if (systemCatIndex >= 0 && systemCatIndex != typeCategories.size() - 1) {
                SelectItem s = typeCategories.get(systemCatIndex);
                typeCategories.remove(systemCatIndex);
                typeCategories.add(s);
            }
        }
        return typeCategories;
    }

    /**
     * Return user dependent type categories that are relevant for the structure editor GUI.
     *
     * @return GUI relevant type categories.
     */
    public List<SelectItem> getRestrictedTypeCategories() {
        if (FxContext.getUserTicket().isGlobalSupervisor())
            return getTypeCategories();
        else {
            if (restrictedTypeCategories == null) {
                restrictedTypeCategories = FxJsfUtils.enumsAsSelectList(TypeCategory.values());
                SelectItem toRemove = null;
                for (SelectItem s : restrictedTypeCategories) {
                    TypeCategory t = (TypeCategory) s.getValue();
                    if (t.getId() == TypeCategory.System.getId()) {
                        toRemove = s;
                        break;
                    }
                }
                restrictedTypeCategories.remove(toRemove);
            }
            return restrictedTypeCategories;
        }
    }

    /**
     * Return all defined query node operators.
     *
     * @return all defined query node operators.
     */
    public List<SelectItem> getQueryNodeOperators() {
        if (queryNodeOperators == null) {
            queryNodeOperators = FxJsfUtils.enumsAsSelectList(QueryOperatorNode.Operator.values());
        }
        return queryNodeOperators;
    }

    /**
     * Return all defined query value comparators (=, >, <, ...).
     *
     * @return all defined query value comparators (=, >, <, ...).
     */
    public List<SelectItem> getQueryValueComparators() {
        if (queryNodeComparators == null) {
            queryNodeComparators = FxJsfUtils.enumsAsSelectList(PropertyValueComparator.values());
        }
        return queryNodeComparators;
    }

    /**
     * Return all defined searchresult view types (LIST, THUMBNAILS).
     *
     * @return all defined searchresult view types (LIST, THUMBNAILS).
     */
    public List<SelectItem> getResultViewTypes() {
        if (resultViewTypes == null) {
            resultViewTypes = FxJsfUtils.enumsAsSelectList(ResultViewType.values());
        }
        return resultViewTypes;
    }

    /**
     * Return all defined search result locations.
     *
     * @return all defined search result locations.
     */
    public List<SelectItem> getResultLocations() {
        if (resultLocations == null) {
            resultLocations = FxJsfUtils.enumsAsSelectList(AdminResultLocations.values());
        }
        return resultLocations;
    }

    public List<SelectItem> getResultDirections() {
        if (resultDirections == null) {
            resultDirections = FxJsfUtils.enumsAsSelectList(new SortDirection[]
                    {SortDirection.ASCENDING, SortDirection.DESCENDING});
        }
        return resultDirections;
    }

    public List<SelectItem> getPreviewSizes() {
        if (previewSizes == null) {
            // filter preview sizes
            List<BinaryDescriptor.PreviewSizes> values = new ArrayList<BinaryDescriptor.PreviewSizes>();
            for (BinaryDescriptor.PreviewSizes value : BinaryDescriptor.PreviewSizes.values()) {
                if (value.getSize() > 0) {
                    values.add(value);
                }
            }
            previewSizes = FxJsfUtils.enumsAsSelectList(values.toArray(new Enum[values.size()]));
        }
        return previewSizes;
    }

    /**
     * Return the enum UniqueMode as SelectList.
     *
     * @return the enum UniqueMode as SelectList.
     */

    public List<SelectItem> getUniqueModes() {
        if (uniqueModes == null) {
            uniqueModes = FxJsfUtils.enumsAsSelectList(UniqueMode.values());
        }
        return uniqueModes;
    }

    /**
     * Return the enum FxDataType as SelectList.
     *
     * @return the enum FxDataType as SelectList.
     */

    public List<SelectItem> getDataTypes() {
        if (dataTypes == null) {
            dataTypes = FxJsfUtils.enumsAsSelectList(FxDataType.values());
            Collections.sort(this.dataTypes, new FxJsfUtils.SelectItemSorter());
        }
        return dataTypes;
    }


    public List<SelectItem> getCacheModes() {
        if (cacheModes == null) {
            cacheModes = FxJsfUtils.enumsAsSelectList(FxSQLSearchParams.CacheMode.values());
        }
        return cacheModes;
    }

    /**
     * Return the enum FxScriptEvent of ScriptScope "Type" as SelectList
     *
     * @return the enum of FxScriptEvent of scope "Type" as SelectList
     */
    public List<SelectItem> getTypeScriptEvents() {
        if (typeScriptEvents == null) {
            typeScriptEvents = new ArrayList<SelectItem>();
            for (FxScriptEvent e : FxScriptEvent.values()) {
                if (e.getScope().compareTo(FxScriptScope.Type) == 0)
                    typeScriptEvents.add(new SelectItem(e.getId(), e.getName()));
            }
        }
        return typeScriptEvents;
    }

    /**
     * Return all available FxScriptEvents as SelectList.
     *
     * @return all available FxScriptEvents as SelectList
     */
    public List<SelectItem> getAllScriptEvents() {
        if (allScriptEvents == null) {
            allScriptEvents = new ArrayList<SelectItem>();
            for (FxScriptEvent e : FxScriptEvent.values()) {
                allScriptEvents.add(new SelectItem(e.getId(), e.getName()));
            }
        }
        return allScriptEvents;
    }

    /**
     * Return scripts with the default event scope "Assignment "as SelectList.
     *
     * @return scripts with the default event scope "Assignment "as SelectList.
     */
    public List<SelectItem> getAssignmentScripts() {
        if (assignmentScripts == null) {
            List<FxScriptInfo> scriptList = new ArrayList<FxScriptInfo>();
            for (FxScriptInfo s : CacheAdmin.getFilteredEnvironment().getScripts()) {
                if (s.getEvent().getScope() == FxScriptScope.Assignment)
                    scriptList.add(s);
            }
            Collections.sort(scriptList, new FxJsfUtils.ScriptInfoSorter());
            assignmentScripts = FxJsfUtils.asSelectListWithName(scriptList);
        }
        return assignmentScripts;
    }

    /**
     * Return the enum FxScriptEvent of ScriptScope "Assignment" as SelectList
     *
     * @return the enum of FxScriptEvent of scope "Assignment" as SelectList
     */
    public List<SelectItem> getAssignmentScriptEvents() {
        if (assignmentScriptEvents == null) {
            assignmentScriptEvents = new ArrayList<SelectItem>();
            for (FxScriptEvent e : FxScriptEvent.values()) {
                if (e.getScope().compareTo(FxScriptScope.Assignment) == 0)
                    assignmentScriptEvents.add(new SelectItem(e.getId(), e.getName()));
            }
        }
        return assignmentScriptEvents;
    }

    /**
     * Return all available scripts as SelectList.
     *
     * @return all available scripts as SelectList
     */
    public List<SelectItem> getAllScripts() {
        if (allScripts == null) {
            allScripts = FxJsfUtils.asSelectListWithName(CacheAdmin.getFilteredEnvironment().getScripts());
        }
        return allScripts;
    }

    /**
     * Return scripts with the default event scope "Type "as SelectList.
     *
     * @return scripts with the default event scope "Type "as SelectList.
     */
    public List<SelectItem> getTypeScripts() {
        if (typeScripts == null) {
            List<FxScriptInfo> scriptList = new ArrayList<FxScriptInfo>();
            for (FxScriptInfo s : CacheAdmin.getFilteredEnvironment().getScripts()) {
                if (s.getEvent().getScope() == FxScriptScope.Type)
                    scriptList.add(s);
            }
            Collections.sort(scriptList, new FxJsfUtils.ScriptInfoSorter());
            typeScripts = FxJsfUtils.asSelectListWithName(scriptList);
        }
        return typeScripts;
    }

    /**
     * returns scriptScope enum as select list.
     *
     * @return scriptScope enum as select list.
     */
    public List<SelectItem> getScriptScopes() {
        if (scriptScopes == null) {
            scriptScopes = FxJsfUtils.enumsAsSelectList(FxScriptScope.values());
        }
        return scriptScopes;
    }

    /**
     * returns FxScriptEvent enum as select list.
     *
     * @return FxScriptEvent enum as select list.
     */
    public List<SelectItem> getAllScriptEventsAsEnum() {
        if (allScriptEventsAsEnum == null) {
            allScriptEventsAsEnum = FxJsfUtils.enumsAsSelectList(FxScriptEvent.values());
        }
        return allScriptEventsAsEnum;
    }

    /**
     * Returns a list of all available scripting engines
     *
     * @return list of all available scripting engines
     */
    public List<SelectItem> getScriptingEngines() {
        if (scriptingEngines == null) {
            try {
                scriptingEngines = FxJsfUtils.asSelectList(EJBLookup.getScriptingEngine().getAvailableScriptEngines());
            } catch (FxApplicationException e) {
                scriptingEngines = new ArrayList<SelectItem>(0);
            }
        }
        return scriptingEngines;
    }

    /**
     * Return the enum ACLCategory as SelectList
     *
     * @return the enum ACLCategory as SelectList
     */
    public List<SelectItem> getACLCategories() {
        if (aclCategories == null)
            aclCategories = FxJsfUtils.enumsAsSelectList(ACLCategory.values());
        return aclCategories;
    }

    /**
     * Return all available select list ACLs.
     *
     * @return all available select list ACLs
     */
    public List<SelectItem> getSelectListACLs() {
        if (selectListACLs == null) {
            selectListACLs = FxJsfUtils.asSelectListWithLabel(CacheAdmin.getEnvironment().getACLs(ACLCategory.SELECTLIST));
        }
        return selectListACLs;
    }

    /**
     * Provides programmatic access for the creation of select lists out of enum values.
     * This method takes a list of Enum values, and returns a corresponding list of select items.
     *
     * @return a map that maps enum lists to select item lists
     */
    public Map getEnumSelect() {
        return FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<List<Enum>, List<SelectItem>>() {
            private static final long serialVersionUID = 597032145345226494L;

            public List<SelectItem> get(Object key) {
                //noinspection unchecked
                final List<Enum> enums = (List<Enum>) key;
                return FxJsfUtils.enumsAsSelectList(enums.toArray(new Enum[enums.size()]));
            }
        });
    }

}
