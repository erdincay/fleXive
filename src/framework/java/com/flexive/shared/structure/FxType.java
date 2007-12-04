/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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

import com.flexive.shared.AbstractSelectableObjectWithLabel;
import com.flexive.shared.FxArrayUtils;
import com.flexive.shared.SelectableObjectWithLabel;
import com.flexive.shared.XPathElement;
import com.flexive.shared.content.FxGroupData;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.FxCreateException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.scripting.FxScriptMapping;
import com.flexive.shared.scripting.FxScriptMappingEntry;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.LifeCycleInfo;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.Workflow;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Type definition
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxType extends AbstractSelectableObjectWithLabel implements Serializable, SelectableObjectWithLabel {
    private static final long serialVersionUID = 5598974905776911393L;

    /**
     * Virtual ROOT_ID type
     */
    public final static long ROOT_ID = 0;

    /**
     * Name of the account contact data type
     */
    public static final String CONTACTDATA = "CONTACTDATA";
    /**
     * Name of the folder data type.
     */
    public static final String FOLDER = "FOLDER";

    protected long id;
    protected ACL ACL;
    protected Workflow workflow;
    protected Mandator mandator;
    List<Mandator> allowedMandators;
    protected String name;
    protected FxString description;
    protected FxType parent;
    protected TypeStorageMode storageMode;
    protected TypeCategory category;
    protected TypeMode mode;
    protected boolean checkValidity;
    protected LanguageMode language;
    protected TypeState state;
    protected byte permissions;
    protected boolean trackHistory;
    protected long historyAge;
    protected long maxVersions;
    protected int maxRelSource;
    protected int maxRelDestination;
    protected LifeCycleInfo lifeCycleInfo;
    protected List<FxType> derivedTypes;
    protected List<FxTypeRelation> relations;
    protected List<FxPropertyAssignment> assignedProperties;
    protected List<FxProperty> uniqueProperties;
    protected List<FxGroupAssignment> assignedGroups;
    protected List<FxAssignment> scriptedAssignments;
    protected Map<FxScriptEvent, long[]> scriptMapping;

    public FxType(long id, ACL acl, Workflow workflow, Mandator mandator, List<Mandator> allowedMandators, String name, FxString description, FxType parent, TypeStorageMode storageMode,
                  TypeCategory category, TypeMode mode, boolean checkValidity, LanguageMode language, TypeState state, byte permissions,
                  boolean trackHistory, long historyAge, long maxVersions, int maxRelSource, int maxRelDestination,
                  LifeCycleInfo lifeCycleInfo, List<FxType> derivedTypes, List<FxTypeRelation> relations) {
        this.id = id;
        this.ACL = acl;
        this.workflow = workflow;
        this.mandator = mandator;
        this.allowedMandators = (allowedMandators != null ? allowedMandators : new ArrayList<Mandator>(0));
        this.name = name.toUpperCase();
        this.description = description;
        this.parent = parent;
        this.storageMode = storageMode;
        this.category = category;
        this.mode = mode;
        this.checkValidity = checkValidity;
        this.language = language;
        this.state = state;
        this.permissions = permissions;
        this.trackHistory = trackHistory;
        this.historyAge = historyAge;
        this.maxVersions = maxVersions;
        this.maxRelSource = maxRelSource;
        this.maxRelDestination = maxRelDestination;
        this.lifeCycleInfo = lifeCycleInfo;
        this.derivedTypes = derivedTypes;
        this.relations = relations;
        this.scriptMapping = new HashMap<FxScriptEvent, long[]>(10);

        //make sure the owner is included in allowedMandators
        boolean foundOwner = false;
        for (Mandator man : this.allowedMandators)
            if (man.getId() == mandator.getId()) {
                foundOwner = true;
                break;
            }
        if (!foundOwner)
            this.allowedMandators.add(0, mandator);
    }

    /**
     * Get the category of this FxType (System, User, ...)
     *
     * @return the category.
     */
    public TypeCategory getCategory() {
        return category;
    }

    /**
     * Perform validity checks on instances?
     *
     * @return is validity checks are performed on instances
     */
    public boolean isCheckValidity() {
        return checkValidity;
    }

    /**
     * Internal id of this FxType
     *
     * @return the internal id of this FxType
     */
    public long getId() {
        return id;
    }

    /**
     * Get the ACL of this type
     *
     * @return ACL of this type
     */
    public ACL getACL() {
        return ACL;
    }

    /**
     * Getter for the assigned Workflow
     *
     * @return Workflow
     */
    public Workflow getWorkflow() {
        return workflow;
    }

    /**
     * Reload this types workflow, internal method, called from the StructureLoader upon Workflow changes
     *
     * @param environment environment with updated workflows
     */
    public void reloadWorkflow(FxEnvironment environment) {
        this.workflow = environment.getWorkflow(this.getWorkflow().getId());
    }

    /**
     * Get the owning mandator of this type
     *
     * @return the owning mandator of this type
     */
    public Mandator getMandator() {
        return mandator;
    }

    /**
     * Get all mandators that have access to this type (including the owner)
     *
     * @return all mandators that have access to this type (including the owner)
     */
    public List<Mandator> getAllowedMandators() {
        return Collections.unmodifiableList(allowedMandators);
    }

    /**
     * How are languages handled? (None, Single, Multiple, ...)
     *
     * @return how languages are handled
     */
    public LanguageMode getLanguage() {
        return language;
    }

    /**
     * Get the state of this type
     *
     * @return TypeState
     */
    public TypeState getState() {
        return state;
    }

    /**
     * Is this FxType defining a content or relation?
     *
     * @return mode (Content or Relation)
     */
    public TypeMode getMode() {
        return mode;
    }

    /**
     * Get the name of this FxType
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the description of this FxType
     *
     * @return description
     */
    public FxString getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    public FxString getLabel() {
        return description != null && !description.isEmpty() ? description : new FxString(false, name);
    }

    /**
     * Returrn a localized, human-readable name for the type.
     *
     * @return a localized, human-readable name for the type.
     */
    public String getDisplayName() {
        if (description != null && !description.isEmpty()) {
            return description.getBestTranslation();
        } else {
            return name;
        }
    }

    /**
     * Is this type a relation?
     *
     * @return if this type is a relation
     */
    public boolean isRelation() {
        return getMode() == TypeMode.Relation;
    }

    /**
     * Is this FxType derived from another?
     *
     * @return if this FxType is derived from another
     * @see FxType#getParent()
     */
    public boolean isDerived() {
        return parent != null;
    }

    /**
     * If this FxType is derived from another FxType get the 'super' FxType
     *
     * @return FxType this one is derived from or <code>null</code>
     */
    public FxType getParent() {
        return parent;
    }

    /**
     * Get all FxTypes that are derived from this Type
     *
     * @return Iterator of all derived types
     */
    public List<FxType> getDerivedTypes() {
        return derivedTypes;
    }

    /**
     * Get how is data stored internally.
     *
     * @return how data is stored internally
     */
    public TypeStorageMode getStorageMode() {
        return storageMode;
    }

    /**
     * Use permissions at all?
     *
     * @return if permissions are used at all
     */
    public boolean usePermissions() {
        return permissions != 0;
    }

    /**
     * Use content instance permissions?
     *
     * @return if content instance permissions are used
     */
    public boolean useInstancePermissions() {
        return (permissions & FxPermissionUtils.PERM_MASK_INSTANCE) == FxPermissionUtils.PERM_MASK_INSTANCE;
    }

    /**
     * Use property permissions?
     *
     * @return if property permissions are used
     */
    public boolean usePropertyPermissions() {
        return (permissions & FxPermissionUtils.PERM_MASK_PROPERTY) == FxPermissionUtils.PERM_MASK_PROPERTY;
    }

    /**
     * Use step permissions?
     *
     * @return if step permissions are used
     */
    public boolean useStepPermissions() {
        return (permissions & FxPermissionUtils.PERM_MASK_STEP) == FxPermissionUtils.PERM_MASK_STEP;
    }

    /**
     * Use type permissions?
     *
     * @return if type permissions are used
     */
    public boolean useTypePermissions() {
        return (permissions & FxPermissionUtils.PERM_MASK_TYPE) == FxPermissionUtils.PERM_MASK_TYPE;
    }

    /**
     * Track history of changes?
     *
     * @return if history of changes is tracked
     */
    public boolean isTrackHistory() {
        return trackHistory;
    }

    /**
     * Get how many days history is tracked (0 = forever)
     *
     * @return how many days history is tracked (0 = forever)
     */
    public long getHistoryAge() {
        return historyAge;
    }

    /**
     * Get how many versions of instances are kept (-1 = infinite, 0 = none)
     *
     * @return how many versions of instances are kept (-1 = infinite, 0 = none)
     */
    public long getMaxVersions() {
        return maxVersions;
    }

    /**
     * How many source instances may be related to this instance in total? (infinte = <0)
     *
     * @return how many source instances may be related to this instance in total? (infinte = <0)
     */
    public int getMaxRelSource() {
        return maxRelSource;
    }

    /**
     * How many destination instances may be related to this instance in total? (infinte = <0)
     *
     * @return how many destination instances may be related to this instance in total? (infinte = <0)
     */
    public int getMaxRelDestination() {
        return maxRelDestination;
    }

    /**
     * Get information about changes
     *
     * @return information about changes
     */
    public LifeCycleInfo getLifeCycleInfo() {
        return lifeCycleInfo;
    }

    /**
     * Get all group assignments that are attached to the type's root
     *
     * @return all group assignments that are attached to the type's root
     */
    public List<FxGroupAssignment> getAssignedGroups() {
        return Collections.unmodifiableList(assignedGroups);
    }

    /**
     * Get all property assignments that are attached to the type's root
     *
     * @return all property assignments that are attached to the type's root
     */
    public List<FxPropertyAssignment> getAssignedProperties() {
        return Collections.unmodifiableList(assignedProperties);
    }

    /**
     * Do unique properties for this type exist?
     *
     * @return if unique properties for this type exist
     */
    public boolean hasUniqueProperties() {
        return uniqueProperties.size() > 0;
    }

    /**
     * Get all properties used in this type that have a unique constraint set
     *
     * @return all properties used in this type that have a unique constraint set
     */
    public List<FxProperty> getUniqueProperties() {
        return Collections.unmodifiableList(uniqueProperties);
    }

    /**
     * Get all possible relation combinations
     *
     * @return possible relation combinations
     */
    public List<FxTypeRelation> getRelations() {
        return Collections.unmodifiableList(relations);
    }

    /**
     * Does this type have mappings for the requested script event  type?
     *
     * @param event requested script event type
     * @return if mappings exist
     */
    public boolean hasScriptMapping(FxScriptEvent event) {
        return scriptMapping.get(event) != null;
    }

    /**
     * Do scripted assignments exists for this type?
     *
     * @return if scripted assignments exist for this type
     */
    public boolean hasScriptedAssignments() {
        return scriptedAssignments != null && scriptedAssignments.size() > 0;
    }

    /**
     * Get a list with all assignments that have scripts assigned for the given script type
     *
     * @param event script event
     * @return list with all assignments that have scripts assigned for the given script type
     */
    public synchronized List<FxAssignment> getScriptedAssignments(FxScriptEvent event) {
        List<FxAssignment> scripts = new ArrayList<FxAssignment>(5);
        if (!hasScriptedAssignments())
            return scripts;
        for (FxAssignment a : scriptedAssignments)
            if (a.getScriptMapping(event) != null && !scripts.contains(a))
                scripts.add(a);
        return scripts;
    }

    /**
     * Get the script id's that are mapped to this type for the requested script type
     *
     * @param event requested script event
     * @return mappings or <code>null</code> if mapping does not exist for this type
     */
    public long[] getScriptMapping(FxScriptEvent event) {
        return scriptMapping.get(event);
    }

    /**
     * Get the permissions set for this type bit coded
     *
     * @return bit coded permissions
     */
    public byte getBitCodedPermissions() {
        return permissions;
    }

    /**
     * Resolve references after initial loading
     *
     * @param fxStructure structure for references
     * @throws FxNotFoundException on errors
     */
    public void resolveReferences(FxEnvironment fxStructure) throws FxNotFoundException {
        //we only resolve if the parent is a preload type! => this can and will only happen once
        if (getParent() != null && getParent().getMode() == TypeMode.Preload) {
            //assign correct parents
            if (getParent().getId() == 0)
                this.parent = null;
            else {
                this.parent = fxStructure.getType(getParent().getId());
                if (!this.parent.derivedTypes.contains(this))
                    this.parent.derivedTypes.add(this);
            }
            //resolve derived types
            for (FxType derived : fxStructure.getTypes(true, true, true, true)) {
                if (derived.getParent() != null && derived.getParent().getId() == getId())
                    derivedTypes.add(derived);
            }
            if (relations.size() > 0) {
                for (FxTypeRelation relation : relations)
                    relation.resolveReferences(fxStructure);
            }
        }
        if (assignedProperties == null)
            assignedProperties = new ArrayList<FxPropertyAssignment>(10);
        else
            assignedProperties.clear();
        if (uniqueProperties == null)
            uniqueProperties = new ArrayList<FxProperty>(10);
        else
            uniqueProperties.clear();
        if (scriptedAssignments == null)
            scriptedAssignments = new ArrayList<FxAssignment>(10);
        else
            scriptedAssignments.clear();
        for (FxPropertyAssignment fxpa : fxStructure.getPropertyAssignments(true)) {
            if (fxpa.getAssignedType().getId() != this.getId())
                continue;
            if (fxpa.hasScriptMappings() && !scriptedAssignments.contains(fxpa))
                scriptedAssignments.add(fxpa);

            if (!fxpa.hasParentGroupAssignment())
                assignedProperties.add(fxpa);
            if (fxpa.getProperty().getUniqueMode() != UniqueMode.None && !uniqueProperties.contains(fxpa.getProperty()))
                uniqueProperties.add(fxpa.getProperty());

        }
        if (assignedGroups == null)
            assignedGroups = new ArrayList<FxGroupAssignment>(10);
        else
            assignedGroups.clear();
        for (FxGroupAssignment fxga : fxStructure.getGroupAssignments(true)) {
            if (fxga.getAssignedType().getId() != this.getId())
                continue;
            if (fxga.hasScriptMappings() && !scriptedAssignments.contains(fxga))
                scriptedAssignments.add(fxga);
            if (!fxga.hasParentGroupAssignment())
                assignedGroups.add(fxga);
        }

        //calculate used script mappings for this type
        this.scriptMapping.clear();
        for (FxScriptMapping sm : fxStructure.getScriptMappings()) {
            for (FxScriptMappingEntry sme : sm.getMappedTypes()) {
                if (!sme.isActive())
                    continue;
                if (sme.getId() == this.getId()) {
                    addScriptMapping(this.scriptMapping, sme.getScriptEvent(), sm.getScriptId());
                } else if (sme.isDerivedUsage()) {
                    for (long l : sme.getDerivedIds())
                        if (l == this.getId())
                            addScriptMapping(this.scriptMapping, sme.getScriptEvent(), sm.getScriptId());

                }
            }
        }
    }

    private synchronized void addScriptMapping(Map<FxScriptEvent, long[]> scriptMapping, FxScriptEvent scriptEvent, long scriptId) {
        if (scriptMapping.get(scriptEvent) == null)
            scriptMapping.put(scriptEvent, new long[]{scriptId});
        else {
            long[] scripts = scriptMapping.get(scriptEvent);
            if (FxArrayUtils.containsElement(scripts, scriptId))
                return;
            long[] new_scripts = new long[scripts.length + 1];
            System.arraycopy(scripts, 0, new_scripts, 0, scripts.length);
            new_scripts[new_scripts.length - 1] = scriptId;
            scriptMapping.put(scriptEvent, new_scripts);
        }
    }

    /**
     * Create an empty FxData hierarchy for a new FxContent starting with a
     * virtual root group.
     *
     * @param xpPrefix XPath prefix like "FxType name[@pk=..]"
     * @return empty FxData hierarchy
     * @throws FxCreateException on errors
     */
    public FxGroupData createEmptyData(String xpPrefix) throws FxCreateException {
        FxGroupData base;
        try {
            base = FxGroupData.createVirtualRootGroup(xpPrefix);
        } catch (FxInvalidParameterException e) {
            throw new FxCreateException(e);
        }
        for (FxPropertyAssignment fxpa : assignedProperties) {
            if (!fxpa.isEnabled())
                continue;
            /*if (fxpa.getMultiplicity().isOptional())
                base.addChild(fxpa.createEmptyData(base, 1));
            else*/
            for (int c = 0; c < fxpa.getDefaultMultiplicity(); c++)
                base.addChild(fxpa.createEmptyData(base, c + 1));
        }

        for (FxGroupAssignment fxga : assignedGroups) {
            if (!fxga.isEnabled())
                continue;
            /*if (fxga.getMultiplicity().isOptional())
                base.addChild(fxga.createEmptyData(base, 1));
            else*/
            for (int c = 0; c < fxga.getDefaultMultiplicity(); c++)
                base.addChild(fxga.createEmptyData(base, c + 1));
        }
        return base;
    }

    /**
     * Create a base group with random data
     *
     * @param pk              primary key of instance that uses this random data
     * @param env             environment
     * @param rnd             Random to use
     * @param maxMultiplicity the maximum multiplicity for groups
     * @return random data
     * @throws FxCreateException on errors
     */
    public FxGroupData createRandomData(FxPK pk, FxEnvironment env, Random rnd, int maxMultiplicity) throws FxCreateException {
        FxGroupData base;
        try {
            base = FxGroupData.createVirtualRootGroup(buildXPathPrefix(pk));
        } catch (FxInvalidParameterException e) {
            throw new FxCreateException(e);
        }
        int count;
        for (FxPropertyAssignment fxpa : assignedProperties) {
            if (!fxpa.isEnabled()
                    || fxpa.isSystemInternal()
                    || fxpa.getProperty().getDataType() == FxDataType.Binary
                    || fxpa.getProperty().getDataType() == FxDataType.Reference)
                continue;
            count = fxpa.getMultiplicity().getRandomRange(rnd, maxMultiplicity);
            for (int i = 0; i < count; i++)
                base.getChildren().add(fxpa.createRandomData(rnd, env, base, i + 1, maxMultiplicity));
        }

        for (FxGroupAssignment fxga : assignedGroups) {
            if (!fxga.isEnabled())
                continue;
            count = fxga.getMultiplicity().getRandomRange(rnd, maxMultiplicity);
            for (int i = 0; i < count; i++)
                base.getChildren().add(fxga.createRandomData(rnd, env, base, i + 1, maxMultiplicity));
        }
        return base;
    }

    /**
     * Get the assignment for the given XPath
     *
     * @param parentXPath desired XPath
     * @return FxAssignment
     * @throws FxInvalidParameterException if XPath is not valid
     * @throws FxNotFoundException         XPath not found
     */
    public FxAssignment getAssignment(String parentXPath) throws FxInvalidParameterException, FxNotFoundException {
        if (StringUtils.isEmpty(parentXPath) || "/".equals(parentXPath))
            return null; //connected to the root
        parentXPath = XPathElement.stripType(parentXPath);
        List<XPathElement> xpe = XPathElement.split(parentXPath.toUpperCase());
        if (xpe.size() == 0)
            return null; //play safe, but should not happen
        for (FxGroupAssignment rga : getAssignedGroups())
            if (rga.getAlias().equals(xpe.get(0).getAlias()))
                return rga.getAssignment(xpe, parentXPath);
        for (FxPropertyAssignment rpa : getAssignedProperties())
            if (rpa.getAlias().equals(xpe.get(0).getAlias()))
                return rpa;
        throw new FxNotFoundException("ex.structure.assignment.notFound.xpath", parentXPath);
    }

    /**
     * Get the FxPropertyAssignment for the given XPath.
     * This is a convenience method calling internally getAssignment and casting the result to FxPropertyAssignment if
     * appropriate, else throws an FxInvalidParameterException if the assignment is a group.
     *
     * @param parentXPath desired XPath
     * @return FxAssignment
     * @throws FxInvalidParameterException if XPath is not valid or a group
     * @throws FxNotFoundException         XPath not found
     */
    public FxPropertyAssignment getPropertyAssignment(String parentXPath) throws FxInvalidParameterException, FxNotFoundException {
        FxAssignment pa = getAssignment(parentXPath);
        if (pa instanceof FxPropertyAssignment)
            return (FxPropertyAssignment) pa;
        throw new FxInvalidParameterException("parentXPath", "ex.structure.assignment.noProperty", parentXPath);
    }

    /**
     * Get the FxGroupAssignment for the given XPath.
     * This is a convenience method calling internally getAssignment and casting the result to FxGroupAssignment if
     * appropriate, else throws an FxInvalidParameterException if the assignment is a property.
     *
     * @param parentXPath desired XPath
     * @return FxAssignment
     * @throws FxInvalidParameterException if XPath is not valid or a propery
     * @throws FxNotFoundException         XPath not found
     */
    public FxGroupAssignment getGroupAssignment(String parentXPath) throws FxInvalidParameterException, FxNotFoundException {
        FxAssignment pa = getAssignment(parentXPath);
        if (pa instanceof FxGroupAssignment)
            return (FxGroupAssignment) pa;
        throw new FxInvalidParameterException("parentXPath", "ex.structure.assignment.noGroup", parentXPath);
    }

    /**
     * Get a list of all FxPropertyAssignments connected to this type that are assigned to the requested property
     *
     * @param propertyId requested property id
     * @return list of all FxPropertyAssignments connected to this type that are assigned to the requested property
     */
    public List<FxPropertyAssignment> getAssignmentsForProperty(long propertyId) {
        List<FxPropertyAssignment> ret = new ArrayList<FxPropertyAssignment>(10);
        for (FxPropertyAssignment rpa : getAssignedProperties())
            if (rpa.getProperty().getId() == propertyId)
                ret.add(rpa);
        for (FxGroupAssignment rga : getAssignedGroups())
            for (FxAssignment a : rga.getAllChildAssignments())
                if (a instanceof FxPropertyAssignment && ((FxPropertyAssignment) a).getProperty().getId() == propertyId) {
                    ret.add((FxPropertyAssignment) a);
                }
        return ret;
    }

    /**
     * Get a list of all FxPropertyAssignments connected to this type that are of the given
     * {@link FxDataType}.
     *
     * @param dataType the data type
     * @return list of all FxPropertyAssignments connected to this type that are of the given data type
     */
    public List<FxPropertyAssignment> getAssignmentsForDataType(FxDataType dataType) {
        List<FxPropertyAssignment> ret = new ArrayList<FxPropertyAssignment>(10);
        for (FxPropertyAssignment rpa : getAssignedProperties())
            if (rpa.getProperty().getDataType().equals(dataType))
                ret.add(rpa);
        for (FxGroupAssignment rga : getAssignedGroups())
            for (FxAssignment a : rga.getAllChildAssignments())
                if (a instanceof FxPropertyAssignment && ((FxPropertyAssignment) a).getProperty().getDataType().equals(dataType)) {
                    ret.add((FxPropertyAssignment) a);
                }
        return ret;
    }

    /**
     * Get all assignments directly connected to the given XPath
     *
     * @param parentXPath desired XPath
     * @return ArrayList of FxAssignment
     * @throws FxInvalidParameterException if XPath is not valid
     * @throws FxNotFoundException         XPath not found
     */
    public List<FxAssignment> getConnectedAssignments(String parentXPath) throws FxInvalidParameterException, FxNotFoundException {
        List<FxAssignment> assignments = new ArrayList<FxAssignment>(10);
        if (StringUtils.isEmpty(parentXPath) || "/".equals(parentXPath)) {
            for (FxGroupAssignment rga : getAssignedGroups())
                if (rga.getParentGroupAssignment() == null)
                    assignments.add(rga);
            for (FxPropertyAssignment rpa : getAssignedProperties())
                if (rpa.getParentGroupAssignment() == null)
                    assignments.add(rpa);
            return Collections.unmodifiableList(FxAssignment.sort(assignments));
        }
        //check if the parentXPath is a group
        if (!(getAssignment(parentXPath) instanceof FxGroupAssignment))
            throw new FxInvalidParameterException("ex.structure.assignment.noGroup", parentXPath);
        for (FxGroupAssignment rga : getAssignedGroups())
            if (rga.getXPath().equals(parentXPath))
                return rga.getAssignments();
        throw new FxNotFoundException("ex.structure.assignments.notFound.xpath", parentXPath);
    }

    /**
     * Check if the givane mandatorId is valid for this type
     *
     * @param mandatorId requested mandator
     * @return if the mandatorId is valid for this type
     */
    public boolean isValidMandator(long mandatorId) {
        if (this.mandator.getId() == mandatorId)
            return true;
        for (Mandator m : getAllowedMandators())
            if (m.getId() == mandatorId)
                return true;
        return false;
    }

    /**
     * Check if the given XPath is valid for this content
     *
     * @param XPath         the XPath to check
     * @param checkProperty should the XPath point to a property?
     * @return if the XPath is valid or not
     */
    public boolean isXPathValid(String XPath, boolean checkProperty) {
        if (StringUtils.isEmpty(XPath))
            return false;
        if ("/".equals(XPath))
            return !checkProperty; //only valid for groups
        XPath = XPath.toUpperCase();
        if (!XPath.startsWith("/")) {
            if (!XPath.startsWith(getName() + "/"))
                return false;
            XPath = XPath.substring(XPath.indexOf('/'));
        }
        boolean valid = true;
        try {
            int depth = 0;
            FxAssignment last = null;
            for (XPathElement xpe : XPathElement.split(XPath)) {
                depth++;
                if (depth == 1) {
                    //check root assignments
                    boolean found = false;
                    for (FxPropertyAssignment pa : getAssignedProperties()) {
                        if (pa.getAlias().equals(xpe.getAlias())) {
                            last = pa;
                            valid = pa.isEnabled() && pa.getMultiplicity().isValid(xpe.getIndex());
                            found = true;
                            break;
                        }
                    }
                    if (found)
                        continue;
                    for (FxGroupAssignment ga : getAssignedGroups()) {
                        if (ga.getAlias().equals(xpe.getAlias())) {
                            last = ga;
                            valid = ga.isEnabled() && ga.getMultiplicity().isValid(xpe.getIndex());
                            break;
                        }
                    }
                    //end root assignments check
                } else {
                    if (last == null || (last instanceof FxPropertyAssignment)) {
                        valid = false;
                        break;
                    }
                    for (FxAssignment as : ((FxGroupAssignment) last).getAssignments()) {
                        if (last != null)
                            last = null; //reset to null for check
                        if (as.getAlias().equals(xpe.getAlias())) {
                            last = as;
                            valid = as.isEnabled() && as.getMultiplicity().isValid(xpe.getIndex());
                            break;
                        }
                    }
                }
                if (!valid)
                    break;
            }
            if (valid) {
                if (checkProperty)
                    valid = last instanceof FxPropertyAssignment;
                else
                    valid = last instanceof FxGroupAssignment;
            }
        } catch (FxInvalidParameterException e) {
            return false;
        }
        return valid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.getName() + "[id=" + this.getId() + ",mandator=" + this.getMandator().getId() + "]";
    }

    /**
     * Get this FxType as editable
     *
     * @return FxTypeEdit
     */
    public FxTypeEdit asEditable() {
        return new FxTypeEdit(this);
    }

    /**
     * Build an XPath prefix for addressing an instance in XPath's
     *
     * @param pk primary key of the instance
     * @return XPath prefix like "FxType name[@pk=..]"
     */
    public String buildXPathPrefix(FxPK pk) {
        return this.getName().toUpperCase() + "[@pk=" + pk + "]";
    }
}
