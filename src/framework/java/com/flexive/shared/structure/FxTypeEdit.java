/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
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
package com.flexive.shared.structure;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.Workflow;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * FxType used for structure editing
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxTypeEdit extends FxType implements Serializable {
    private static final long serialVersionUID = 3089716188411029437L;

    private boolean isNew;
    protected boolean changed;
    private boolean enableParentAssignments;
    private boolean removeInstancesWithRelationTypes;
    private List<FxTypeRelation> originalRelations;

    /**
     * Constructor
     *
     * @param name                    name of the type
     * @param description             description
     * @param acl                     type ACL
     * @param workflow                workflow to use
     * @param parent                  parent type or <code>null</code> if not derived
     * @param enableParentAssignments if parent is not <code>null</code> enable all derived assignments from the parent?
     * @param storageMode             the storage mode
     * @param category                category mode (system, user)
     * @param mode                    type mode (content, relation)
     * @param language                language mode
     * @param state                   type state (active, locked, etc)
     * @param permissions             permissions bit coded
     * @param trackHistory            track history
     * @param historyAge              max. age of the history to track
     * @param maxVersions             max. number of versions to keep, if &lt; 0 unlimited
     * @param maxRelSource            max. number of instance related as source
     * @param maxRelDestination       max. number of instances related as destination
     */
    private FxTypeEdit(String name, FxString description, ACL acl, Workflow workflow, FxType parent,
                       boolean enableParentAssignments,
                       TypeStorageMode storageMode, TypeCategory category, TypeMode mode,
                       LanguageMode language, TypeState state, byte permissions,
                       boolean trackHistory, long historyAge, long maxVersions, int maxRelSource,
                       int maxRelDestination) {
        super(-1, acl, workflow, name, description, parent, storageMode,
                category, mode, language, state, permissions, trackHistory, historyAge,
                maxVersions, maxRelSource, maxRelDestination, null, null, new ArrayList<FxTypeRelation>(5));
        this.enableParentAssignments = enableParentAssignments;
        this.isNew = true;
        this.changed = true;
        this.removeInstancesWithRelationTypes = false;
        this.relations = new ArrayList<FxTypeRelation>(super.getRelations());
        this.originalRelations = new ArrayList<FxTypeRelation>(super.getRelations());
    }

    /**
     * Create a new FxTypeEdit instance for editing and updating an existing FxType
     *
     * @param type the FxType to edit
     */
    public FxTypeEdit(FxType type) {
        super(type.getId(), type.getACL(), type.getWorkflow(), 
                type.getName(), type.getDescription(), type.getParent(), type.getStorageMode(), type.getCategory(),
                type.getMode(), type.getLanguage(), type.getState(), type.permissions,
                type.isTrackHistory(), type.getHistoryAge(), type.getMaxVersions(), type.getMaxRelSource(),
                type.getMaxRelDestination(), type.getLifeCycleInfo(), type.getDerivedTypes(), type.getRelations());
        this.isNew = false;
        this.scriptMapping = type.scriptMapping;
        //make lists editable again
        this.assignedGroups = new ArrayList<FxGroupAssignment>(type.assignedGroups);
        this.assignedProperties = new ArrayList<FxPropertyAssignment>(type.assignedProperties);
        this.relations = new ArrayList<FxTypeRelation>(type.relations);
        this.changed = false;
        this.removeInstancesWithRelationTypes = false;
        this.originalRelations = new ArrayList<FxTypeRelation>(super.getRelations());
    }

    /**
     * Create a new FxTypeEdit instance for creating a new FxType
     *
     * @param name        name of the type
     * @return FxTypeEdit instance for creating a new FxType
     */
    public static FxTypeEdit createNew(String name) {
        return createNew(name, new FxString(FxLanguage.DEFAULT_ID, name),
                CacheAdmin.getEnvironment().getACL(com.flexive.shared.security.ACL.Category.STRUCTURE.getDefaultId()), null);
    }

    /**
     * Create a new FxTypeEdit instance for creating a new FxType
     *
     * @param name        name of the type
     * @param description description
     * @param acl         type ACL
     * @return FxTypeEdit instance for creating a new FxType
     */
    public static FxTypeEdit createNew(String name, FxString description, ACL acl) {
        return createNew(name, description, acl, null);
    }
    
    /**
     * Create a new FxTypeEdit instance for creating a new FxType
     *
     * @param name        name of the type
     * @param description description
     * @param acl         type ACL
     * @param parent      parent type or <code>null</code> if not derived
     * @return FxTypeEdit instance for creating a new FxType
     */
    public static FxTypeEdit createNew(String name, FxString description, ACL acl, FxType parent) {
        return createNew(name, description, acl, CacheAdmin.getEnvironment().getWorkflows().get(0),
                parent, parent != null, TypeStorageMode.Hierarchical,
                TypeCategory.User, TypeMode.Content, LanguageMode.Multiple, TypeState.Available,
                getDefaultTypePermissions(), false, 0, -1, 0, 0);
    }

    private static byte getDefaultTypePermissions() {
        return FxPermissionUtils.encodeTypePermissions(true, false, true, true);
    }

    /**
     * Create a new FxTypeEdit instance for creating a new FxType
     *
     * @param name                    name of the type
     * @param description             description
     * @param acl                     type ACL
     * @param workflow                workflow to use
     * @param parent                  parent type or <code>null</code> if not derived
     * @param enableParentAssignments if parent is not <code>null</code> enable all derived assignments from the parent?
     * @param storageMode             the storage mode
     * @param category                category mode (system, user)
     * @param mode                    type mode (content, relation)
     * @param language                language mode
     * @param state                   type state (active, locked, etc)
     * @param permissions             permissions bit coded
     * @param trackHistory            track history
     * @param historyAge              max. age of the history to track
     * @param maxVersions             max. number of versions to keep, if &lt; 0 unlimited
     * @param maxRelSource            max. number of instance related as source
     * @param maxRelDestination       max. number of instance related as destination
     * @return FxTypeEdit instance for creating a new FxType
     */
    public static FxTypeEdit createNew(String name, FxString description, ACL acl, Workflow workflow,
                                       FxType parent, boolean enableParentAssignments,
                                       TypeStorageMode storageMode, TypeCategory category, TypeMode mode,
                                       LanguageMode language, TypeState state, byte permissions,
                                       boolean trackHistory, long historyAge, long maxVersions, int maxRelSource,
                                       int maxRelDestination) {
        return new FxTypeEdit(name, description, acl, workflow, parent, enableParentAssignments,
                storageMode, category, mode, language, state, permissions, trackHistory, historyAge,
                maxVersions, maxRelSource, maxRelDestination);
    }

    /**
     * Is this a new type or updating an existing type?
     *
     * @return new or existing type?
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Returns the unmodifiable script mapping of this type.
     *
     * @return  the unmodifiable script mapping of this type
     */
    /*
    public Map<FxScriptEvent, long[]> getScriptMapping() {
        return Collections.unmodifiableMap(scriptMapping);
    }
    */

    /**
     * If FxTypeRelation entries are removed, remove all affected instances?
     *
     * @return remove all affected instances if FxTypeRelation entries are removed?
     */
    public boolean isRemoveInstancesWithRelationTypes() {
        return removeInstancesWithRelationTypes;
    }

    /**
     * Set if affected instances should be removed if FxTypeRelations are removed
     *
     * @param removeInstancesWithRelationTypes if affected instances should be removed if FxTypeRelations are removed
     */
    public void setRemoveInstancesWithRelationTypes(boolean removeInstancesWithRelationTypes) {
        this.removeInstancesWithRelationTypes = removeInstancesWithRelationTypes;
    }

    /**
     * Enable parent assignments if creating a derived type?
     * This method has no effect if editing an existing type!
     *
     * @return are parent assignments enabled?
     */
    public boolean isEnableParentAssignments() {
        return enableParentAssignments;
    }

    /**
     * Enable parent assignments if creating a derived type?
     * This method has no effect if editing an existing type!
     *
     * @param enableParentAssignments are parent assignments enabled?
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setEnableParentAssignments(boolean enableParentAssignments) {
        this.enableParentAssignments = enableParentAssignments;
        this.changed = true;
        return this;
    }

    /**
     * Assign a new ACL
     *
     * @param ACL ACL to assign for this type
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setACL(ACL ACL) {
        this.ACL = ACL;
        this.changed = true;
        return this;
    }

    /**
     * Set the workflow to use
     *
     * @param workflow the workflow to use
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setWorkflow(Workflow workflow) {
        this.workflow = workflow;
        this.changed = true;
        return this;
    }

    /**
     * Set the name of this type
     *
     * @param name the name of this type
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setName(String name) {
        if (StringUtils.isEmpty(name))
            return this;
        this.name = name.toUpperCase().trim();
        this.changed = true;
        return this;
    }

    /**
     * Set the types description
     *
     * @param description description
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setDescription(FxString description) {
        this.description = description;
        this.changed = true;
        return this;
    }

    /*public void setParent(FxType parent) {
        this.parent = parent;
    }

    public void setStorageMode(TypeStorageMode storageMode) {
        this.storageMode = storageMode;
    }*/

    /**
     * Set the types category (user, system)
     *
     * @param category the category to set
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setCategory(TypeCategory category) {
        this.category = category;
        this.changed = true;
        return this;
    }

    /**
     * Set this type's mode.
     * Setting does not check anything, if the requested mode is really available and possible is determined during saving
     *
     * @param mode requested mode
     * @return this
     */
    public FxTypeEdit setMode(TypeMode mode) {
        this.mode = mode;
        this.changed = true;
        return this;
    }

    /**
     * Set the language mode to use for this type
     *
     * @param language language mode to use
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setLanguage(LanguageMode language) {
        this.language = language;
        this.changed = true;
        return this;
    }

    /**
     * Set the state of this type
     *
     * @param state the state of this type
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setState(TypeState state) {
        this.state = state;
        this.changed = true;
        return this;
    }

    /**
     * Set the bit coded permissions of this type
     *
     * @param permissions bit coded permissions
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setPermissions(byte permissions) {
        this.permissions = permissions;
        this.changed = true;
        return this;
    }

    /**
     * Set usage of instance permissions
     *
     * @param use use instance permissions?
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setUseInstancePermissions(boolean use) {
        if (use == this.useInstancePermissions())
            return this;
        this.permissions ^= FxPermissionUtils.PERM_MASK_INSTANCE;
        this.changed = true;
        return this;
    }

    /**
     * Set usage of property permissions
     *
     * @param use use property permissions?
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setUsePropertyPermissions(boolean use) {
        if (use == this.usePropertyPermissions())
            return this;
        this.permissions ^= FxPermissionUtils.PERM_MASK_PROPERTY;
        this.changed = true;
        return this;
    }

    /**
     * Set usage of step permissions
     *
     * @param use use step permissions?
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setUseStepPermissions(boolean use) {
        if (use == this.useStepPermissions())
            return this;
        this.permissions ^= FxPermissionUtils.PERM_MASK_STEP;
        this.changed = true;
        return this;
    }

    /**
     * Set usage of type permissions
     *
     * @param use use type permissions?
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setUseTypePermissions(boolean use) {
        if (use == this.useTypePermissions())
            return this;
        this.permissions ^= FxPermissionUtils.PERM_MASK_TYPE;
        this.changed = true;
        return this;
    }

    /**
     * Track history for this type
     *
     * @param trackHistory track history?
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setTrackHistory(boolean trackHistory) {
        this.trackHistory = trackHistory;
        this.changed = true;
        return this;
    }

    /**
     * Set the max. age of history entries
     *
     * @param historyAge max. age of history entries
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setHistoryAge(long historyAge) {
        this.historyAge = (historyAge > 0 ? historyAge : 0);
        this.changed = true;
        return this;
    }

    /**
     * Set the max. number of versions to keep, if negative unlimited
     *
     * @param maxVersions max. number of versions to keep, if negative unlimited
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setMaxVersions(long maxVersions) {
        this.maxVersions = (maxVersions < -1 ? -1 : maxVersions);
        this.changed = true;
        return this;
    }

    /**
     * Set the max. number related source instances for this type, if negative unlimited
     *
     * @param maxRelSource max. number related source instances for this type, if negative unlimited
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setMaxRelSource(int maxRelSource) {
        this.maxRelSource = maxRelSource;
        this.changed = true;
        return this;
    }

    /**
     * Set the max. number related destination instances for this type, if negative unlimited
     *
     * @param maxRelDestination max. number related destination instances for this type, if negative unlimited
     * @return the type itself, useful for chained calls
     */
    public FxTypeEdit setMaxRelDestination(int maxRelDestination) {
        this.maxRelDestination = maxRelDestination;
        this.changed = true;
        return this;
    }

    /**
     * Find a relation in the given list that matches source and destination type
     *
     * @param rel  the relation to find
     * @param list the list to search
     * @return found relation or <code>null</code>
     */
    private FxTypeRelation findRelationInList(FxTypeRelation rel, List<FxTypeRelation> list) {
        for (FxTypeRelation check : list) {
            if (check.getSource().getId() == rel.getSource().getId() &&
                    check.getDestination().getId() == rel.getDestination().getId())
                return check;
        }
        return null;
    }

    /*
     * Validate the source and destination of a relation.
     * @param rel      the relation to validate.
     * @throws FxInvalidParameterException if the relation is invalid.
     */
    private void validateRelation(FxTypeRelation rel) throws FxInvalidParameterException {
        if (rel.getSource().isRelation()) {
            throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", this.getName(), rel.getSource().getName());
        }
        if (rel.getDestination().isRelation())
            throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", this.getName(), rel.getDestination().getName());
    }

    /*
     * Validate if the current relation sources and destination multiplicites together with
     * newRelationToAdd don't exceed the given maxima (maxRelSource or maxRelDestination).
     *
     * @param   newRelationToAdd    the relation to be added.
     * @throws  FxInvalidParameterException if (maxRelSource or maxRelDestination is exceeded.
     */
    private void validateRelationMultiplicity(FxTypeRelation newRelationToAdd) throws FxInvalidParameterException {
        long source = newRelationToAdd.getMaxSource();
        long dest = newRelationToAdd.getMaxDestination();

        if (source ==0 && getMaxRelSource() >=0)
            throw new FxInvalidParameterException("ex.structure.type.relation.maxRelSourceExceeded",
                this.getName(), newRelationToAdd.getSource().getName(),
                newRelationToAdd.getDestination().getName(), getMaxRelSource());

        else if (dest ==0 && getMaxRelDestination() >=0)
            throw new FxInvalidParameterException("ex.structure.type.relation.maxRelDestExceeded",
                this.getName(), newRelationToAdd.getSource().getName(),
                newRelationToAdd.getDestination().getName(), getMaxRelDestination());

        for (FxTypeRelation rel : relations) {
            if (getMaxRelSource() >=0) {
                if (rel.getMaxSource() == 0 || rel.getMaxSource()+source >getMaxRelSource()) {
                    throw new FxInvalidParameterException("ex.structure.type.relation.maxRelSourceExceeded",
                    this.getName(), rel.getSource().getName(),
                    rel.getDestination().getName(), getMaxRelSource());
                }
                else
                    source += rel.getMaxSource();
            }
            if(getMaxRelDestination() >=0) {
                if (rel.getMaxDestination() == 0 || rel.getMaxDestination()+dest >getMaxRelDestination() ) {
                    throw new FxInvalidParameterException("ex.structure.type.relation.maxRelDestExceeded",
                    this.getName(), rel.getSource().getName(),
                    rel.getDestination().getName(), getMaxRelDestination());
                }
                else
                    dest += rel.getMaxDestination();
            }
        }
    }


    /**
     * Return if the max source or destination settings for two FxTypeRelations differ
     *
     * @param o1 relation 1
     * @param o2 relation 2
     * @return differ
     */
    private boolean relationDiffers(FxTypeRelation o1, FxTypeRelation o2) {
        return o1.getMaxDestination() != o2.getMaxDestination() || o1.getMaxSource() != o2.getMaxSource();
    }

    /**
     * Get a list of all FxTypeRelations that have been removed
     *
     * @return list of all FxTypeRelations that have been removed
     */
    public List<FxTypeRelation> getRemovedRelations() {
        List<FxTypeRelation> list = new ArrayList<FxTypeRelation>(5);
        for (FxTypeRelation rel : originalRelations)
            if (findRelationInList(rel, relations) == null)
                list.add(rel);
        return list;
    }

    /**
     * Get a list of all FxTypeRelations that have been added
     *
     * @return list of all FxTypeRelations that have been added
     */
    public List<FxTypeRelation> getAddedRelations() {
        List<FxTypeRelation> list = new ArrayList<FxTypeRelation>(5);
        for (FxTypeRelation rel : relations)
            if (findRelationInList(rel, originalRelations) == null)
                list.add(rel);
        return list;
    }

    /**
     * Get a list of all FxTypeRelations that have been updated
     *
     * @return list of all FxTypeRelations that have been updated
     */
    public List<FxTypeRelation> getUpdatedRelations() {
        List<FxTypeRelation> list = new ArrayList<FxTypeRelation>(5);
        FxTypeRelation curr;
        for (FxTypeRelation rel : relations) {
            curr = findRelationInList(rel, originalRelations);
            if ((curr != null && relationDiffers(curr, rel)) ||
                    (rel instanceof FxTypeRelationEdit && ((FxTypeRelationEdit) rel).isChanged()))
                list.add(rel);
        }
        return list;
    }

    /**
     * Add or update a relation
     *
     * @param relation the relation to add or update
     * @return this
     * @throws FxInvalidParameterException if the relation is invalid.
     */
    public FxTypeEdit addRelation(FxTypeRelation relation) throws FxInvalidParameterException{
        validateRelation(relation);
        if (relations.contains(relation)) {
            relations.remove(relation);
            relations.add(relation);
        } else
            relations.add(relation);
        return this;
    }

    /**
     * Add or update a relation
     *
     * @param relation the relation to add or update
     * @throws FxInvalidParameterException if the relation is invalid.
     */
    public void updateRelation(FxTypeRelation relation) throws FxInvalidParameterException{
        addRelation(relation);
    }

    /**
     * Remove a relation
     *
     * @param relation the relation to remove
     */
    public void removeRelation(FxTypeRelation relation) {
        relations.remove(relation);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxGroupAssignment> getAssignedGroups() {
        return assignedGroups;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxPropertyAssignment> getAssignedProperties() {
        return assignedProperties;
    }

    /**
     * Have changes been made?
     *
     * @return changes made
     */
    public boolean isChanged() {
        return changed || getAddedRelations().size() > 0 || getRemovedRelations().size() > 0 || getUpdatedRelations().size() > 0;
    }
}
