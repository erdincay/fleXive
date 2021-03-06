/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.war.beans.admin.structure;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMessage;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.Role;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.war.beans.admin.structure.OptionWrapper.WrappedOption;
import com.flexive.war.javascript.tree.StructureTreeWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bean behind groupAssignmentEditor.xhtml, groupEditor.xhtml and groupOptionEditor to
 * edit FxGroupAssignment and FxGroup objects
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class GroupEditorBean implements Serializable {
    private static final long serialVersionUID = 1712834106156641081L;
    private static final Log LOG = LogFactory.getLog(GroupEditorBean.class);
    
    private FxGroupEdit group = null;
    private FxGroupAssignmentEdit assignment = null;
    private String assignmentMinMul = null;
    private String assignmentMaxMul = null;
    private String groupMinMul = null;
    private String groupMaxMul = null;
    private String groupOptionValue = null;
    private String groupOptionKey = null;
    private boolean groupOptionOverridable = true;
    private String assignmentOptionValue = null;
    private String assignmentOptionKey = null;
    private boolean assignmentOverridable = true;
    private boolean assignmentIsInherited = true;
    private OptionWrapper optionWrapper = null;
    private WrappedOption optionFiler = null;
    private FxType parentType = null;
    private String parentXPath = null;
    private int defaultMultiplicity =-1;
    //checker for the editMode: if not in edit mode,
    // save and delete buttons are not rendered by the gui
    private boolean editMode = false;
    //checker if current user may edit the property
    private boolean structureManagement = false;
    private boolean showParentAssignmentOptions = false;
    private OptionWrapper optionWrapperParent = null;
    private String openParentOptions = "false"; // toggle panel f. parent assignment options

    // indicates if there are messages to print
    private boolean hasMsg = false;

    // stores messages from creating properties
    private List<FxFacesMessage> msgs = new ArrayList<FxFacesMessage>();

    /**
     * print the saved messages
     * @return
     */
    public boolean isHasMsg() {
        if (hasMsg) {
            for (FxFacesMessage curMsg : msgs) {
                curMsg.addToContext();
            }
        }
        msgs.clear();
        hasMsg = false;
        return hasMsg;
    }

    public String getParseRequestParameters() {
        try {
            String action = FxJsfUtils.getParameter("action");
            if (StringUtils.isBlank(action)) {
                return null;
            } else if ("openInstance".equals(action)) {
                editMode = false;
                long propId = FxJsfUtils.getLongParameter("id", -1);
                assignment = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(propId)).asEditable();
                group = assignment.getGroupEdit();
                initEditing();
            } else if ("editInstance".equals(action)) {
                editMode = true;
                long propId = FxJsfUtils.getLongParameter("id", -1);
                assignment = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(propId)).asEditable();
                group = assignment.getGroupEdit();
                initEditing();
            } else if ("createGroup".equals(action)) {
                editMode = true;
                assignment = null;
                parentXPath = null;
                parentType = null;

                long id = FxJsfUtils.getLongParameter("id");
                String nodeType = FxJsfUtils.getParameter("nodeType");

                parentXPath = "/";

                if (StructureTreeWriter.NODE_TYPE_TYPE.equals(nodeType) || StructureTreeWriter.NODE_TYPE_TYPE_RELATION.equals(nodeType)) {
                    parentType = CacheAdmin.getEnvironment().getType(id);
                }

                if (StructureTreeWriter.NODE_TYPE_GROUP.equals(nodeType)) {
                    FxGroupAssignment ga = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(id);
                    parentType = ga.getAssignedType();
                    parentXPath = ga.getXPath();
                }

                group = FxGroupEdit.createNew("NEWGROUP", new FxString("").setEmpty(), new FxString("").setEmpty(), false, FxMultiplicity.MULT_0_1);
                initNewGroupEditing();
            }
        } catch (Throwable t) {
            LOG.error("Failed to parse request parameters: " + t.getMessage(), t);
            new FxFacesMsgErr(t).addToContext();
            return "structureContent";
        }

        return null;
    }

    public long getAssignmentId() {
        return group != null ? group.getId() : -1;
    }

    //necessarry only to prevent JSF errors because of value binding
    public void setAssignmentId(long assignmentId) {
    }

    public void toggleEditMode() {
        editMode = !editMode;
    }

    public boolean getEditMode() {
        return editMode;
    }

    public boolean isStructureManagement() {
        return structureManagement;
    }

    public boolean isSystemInternal() {
        return assignment != null && assignment.isSystemInternal();
    }

    /**
     * initializes variables and does workarounds so editing
     * of an existing group and group assignment is possible via the webinterface
     */
    private void initEditing() {
        structureManagement = FxJsfUtils.getRequest().getUserTicket().isInRole(Role.StructureManagement);
        defaultMultiplicity = assignment.getDefaultMultiplicity();
        assignmentMinMul = FxMultiplicity.getIntToString(assignment.getMultiplicity().getMin());
        assignmentMaxMul = FxMultiplicity.getIntToString(assignment.getMultiplicity().getMax());
        groupMinMul = FxMultiplicity.getIntToString(group.getMultiplicity().getMin());
        groupMaxMul = FxMultiplicity.getIntToString(group.getMultiplicity().getMax());
        optionWrapper = new OptionWrapper(group.getOptions(), assignment.getOptions(), false, isShowParentAssignmentOptions());
        if (isShowParentAssignmentOptions()) {
            optionWrapperParent = new OptionWrapper(null, CacheAdmin.getEnvironment().getAssignment(assignment.getBaseAssignmentId()).getOptions(), false);
        }
    }

    /**
     * initializes variables necessarry for creating a new group via the web interface.
     * during the creation process, new groups don't have assignments yet.
     */
    private void initNewGroupEditing() {
        structureManagement = FxJsfUtils.getRequest().getUserTicket().isInRole(Role.StructureManagement);
        group.setOverrideMultiplicity(true);
        groupMinMul = FxMultiplicity.getIntToString(group.getMultiplicity().getMin());
        groupMaxMul = FxMultiplicity.getIntToString(group.getMultiplicity().getMax());
        optionWrapper = new OptionWrapper(group.getOptions(), null, false);
    }

    /**
     *  Return if the groupMode may be changed.
     *  This is the case for groups where no content exists yet.
     *
     * @return  if the groupMode may be changed
     */
    public boolean isMayChangeGroupMode() {
        try {
            return EJBLookup.getAssignmentEngine().getAssignmentInstanceCount(assignment.getId()) == 0;
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            //fallback
            return false;
        }
    }

    /**
     * If the assignment is derived, return the base assignments XPath
     *
     * @return the base assignments XPath if derived
     */
    public String getBaseAssignmentXPath() {
        if (assignment.isDerivedAssignment())
            return CacheAdmin.getEnvironment().getAssignment(assignment.getBaseAssignmentId()).getXPath();
        else
            return "";
    }

    public String getAssignmentAlias() {
        return assignment.getAlias();
    }

    public void setAssignmentAlias(String a) {
        try {
            //only react to changes, else xpath may change!
            if (!assignment.getAlias().equals(a))
                assignment.setAlias(a);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public int getAssignmentDefaultMultiplicity() {
        return this.defaultMultiplicity;
    }

    public void setAssignmentDefaultMultiplicity(int defaultMultiplicity) {
        this.defaultMultiplicity = defaultMultiplicity;
    }

    public boolean isAssignmentEnabled() {
        return assignment.isEnabled();
    }

    public void setAssignmentEnabled(boolean e) {
        assignment.setEnabled(e);
    }

    public FxString getAssignmentHint() {
        return assignment.getHint();
    }

    public void setAssignmentHint(FxString h) {
        assignment.setHint(h);
    }

    public FxString getAssignmentLabel() {
        return assignment.getLabel();
    }

    public void setAssignmentLabel(FxString l) {
        assignment.setLabel(l);
    }

    public GroupMode getAssignmentMode() {
        return assignment.getMode();
    }

    public void setAssignmentMode(GroupMode m) {
        assignment.setMode(m);
    }

    public FxGroupEdit getGroup() {
        return group;
    }

    public void setGroup(FxGroupEdit group) {
        this.group = group;
    }

    public FxGroupAssignmentEdit getAssignment() {
        return assignment;
    }

    public void setAssignment(FxGroupAssignmentEdit assignment) {
        this.assignment = assignment;
    }

    public String getAssignmentMinMul() {
        return assignmentMinMul;
    }

    public void setAssignmentMinMul(String assignmentMinMul) {
        this.assignmentMinMul = assignmentMinMul;
    }

    public String getAssignmentMaxMul() {
        return assignmentMaxMul;
    }

    public void setAssignmentMaxMul(String assignmentMaxMul) {
        this.assignmentMaxMul = assignmentMaxMul;
    }

    public String getGroupMaxMul() {
        return groupMaxMul;
    }

    public void setGroupMaxMul(String groupMaxMul) {
        this.groupMaxMul = groupMaxMul;
    }

    public String getGroupMinMul() {
        return groupMinMul;
    }

    public void setGroupMinMul(String groupMinMul) {
        this.groupMinMul = groupMinMul;
    }

    public GroupMode getGroupAssignmentGroupMode() {
        return group.getAssignmentGroupMode();
    }

    public void setGroupAssignmentGroupMode(GroupMode gm) {
        try {
            group.setAssignmentGroupMode(gm);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public FxString getGroupHint() {
        return group.getHint();
    }

    public void setGroupHint(FxString h) {
        group.setHint(h);
    }

    public FxString getGroupLabel() {
        return group.getLabel();
    }

    public void setGroupLabel(FxString l) {
        group.setLabel(l);
    }

    public String getGroupName() {
        return group.getName();
    }

    public void setGroupName(String n) {
        group.setName(n);
    }

    public boolean isGroupOverrideBaseMultiplicity() {
        return group.mayOverrideBaseMultiplicity();
    }

    public void setGroupOverrideBaseMultiplicity(boolean b) {
        if (group.mayOverrideBaseMultiplicity() && !b) {
            assignmentMinMul = groupMinMul;
            assignmentMaxMul = groupMaxMul;
        }
        group.setOverrideMultiplicity(b);
    }

    public String getOpenParentOptions() {
        return openParentOptions;
    }

    public void setOpenParentOptions(String openParentOptions) {
        this.openParentOptions = openParentOptions;
    }

    /**
     * Apply changes to the group and the assignment and forward them to DB
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void saveChanges() {
        if (FxJsfUtils.getRequest().getUserTicket().isInRole(Role.StructureManagement)) {
            try {
                applyGroupChanges();
                saveAssignmentChanges();
                EJBLookup.getAssignmentEngine().save(group);
                StructureTreeControllerBean s = (StructureTreeControllerBean) FxJsfUtils.getManagedBean("structureTreeControllerBean");
                s.addAction(StructureTreeControllerBean.ACTION_RENAME_ASSIGNMENT, assignment.getId(), assignment.getDisplayName());
                new FxFacesMsgInfo("GroupEditor.message.info.savedChanges", assignment.getLabel()).addToContext();
                reInit();
            }
            catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        }
        else
            new FxFacesMsgErr(new FxApplicationException("ex.role.notInRole", "StructureManagement")).addToContext();
    }

    /**
     * Re-initialise property/assignment/optionwrapper variables after a save
     */
    private void reInit() {
        group = CacheAdmin.getEnvironment().getGroup(group.getId()).asEditable();
        assignment = ((FxGroupAssignment)CacheAdmin.getEnvironment().getAssignment(assignment.getId())).asEditable();
        optionWrapper = new OptionWrapper(group.getOptions(), assignment.getOptions(), false, isShowParentAssignmentOptions());
    }

    /**
     * Save a newly created group to DB
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void createGroup() {
        if (FxJsfUtils.getRequest().getUserTicket().isInRole(Role.StructureManagement)) {
            try {
                applyGroupChanges();
                long assignmentId;
                if (parentType != null)
                    assignmentId = EJBLookup.getAssignmentEngine().createGroup(parentType.getId(), group, parentXPath);
                else
                    assignmentId = EJBLookup.getAssignmentEngine().createGroup(group, parentXPath);
                StructureTreeControllerBean s = (StructureTreeControllerBean) FxJsfUtils.getManagedBean("structureTreeControllerBean");
                s.addAction(StructureTreeControllerBean.ACTION_RELOAD_OPEN_ASSIGNMENT, assignmentId, "");
                hasMsg = true;
                msgs.add(new FxFacesMsgInfo("GroupEditor.message.info.created"));
            }
            catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        }
        else
            new FxFacesMsgErr(new FxApplicationException("ex.role.notInRole", "StructureManagement")).addToContext();
    }

    /**
     * Apply all changes to the group assignment which are still cached in
     * the view (options, multiplicity, label) and forward them to DB
     *
     * @throws FxApplicationException   if the label is invalid
     */
    private void saveAssignmentChanges() throws FxApplicationException {
        final List<FxStructureOption> removeOptions = new ArrayList<FxStructureOption>(1);
        final List<FxStructureOption> invalidOptions = new ArrayList<FxStructureOption>(1);

        // retrieve edited options
        List<FxStructureOption> newOptions = optionWrapper.asFxStructureOptionList(optionWrapper.getAssignmentOptions());
        // populate list of removed options
        for(FxStructureOption oldOpt : assignment.getOptions()) {
            if(!FxStructureOption.hasOption(oldOpt.getKey(), newOptions) || !oldOpt.isValid())
                removeOptions.add(oldOpt);
            // invalid options
            if(!oldOpt.isValid())
                invalidOptions.add(oldOpt);
        }

        // add edited options (checks if they are set)
        for (FxStructureOption o : newOptions) {
            if(o.isValid())
                assignment.setOption(o.getKey(), o.isOverridable(), o.getIsInherited(), o.getValue());
            else { // remove invalid options from the optionwrapper
                removeOptions.add(o);
                invalidOptions.add(o);
            }
        }
        // remove operation
        if (removeOptions.size() > 0) {
            for (FxStructureOption o : removeOptions) {
                assignment.clearOption(o.getKey());
            }
        }

        // check out diff missing options / removed options and build a message f. removed options
        if(invalidOptions.size() > 0) {
            final StringBuilder invalidOptMessage = new StringBuilder(200);
            for(FxStructureOption invalidOpt : invalidOptions) {
                invalidOptMessage.append(invalidOpt.toString());
            }
            invalidOptMessage.trimToSize();
            if(invalidOptMessage.length() > 0)
                new FxFacesMsgInfo("PropertyEditor.err.propertyAssKeysNotSaved", invalidOptMessage.toString()).addToContext();
        }

        if (assignment.getLabel().getIsEmpty()) {
            throw new FxApplicationException("ex.structureEditor.noLabel");
        }
        // min can be N as it could be before the patch so it is changeable to not-N
        int min = FxMultiplicity.getStringToInt(assignmentMinMul, true);
        int max = FxMultiplicity.getStringToInt(assignmentMaxMul, true);

        if (!isSystemInternal()
                || FxJsfUtils.getRequest().getUserTicket().isInRole(Role.GlobalSupervisor)) {
            if (group.mayOverrideBaseMultiplicity()) {
                FxJsfUtils.checkMultiplicity(min, max);
                assignment.setMultiplicity(FxMultiplicity.of(min, max));
            }
            assignment.setDefaultMultiplicity(this.defaultMultiplicity);
            EJBLookup.getAssignmentEngine().save(assignment, false);
        }
    }

    /**
     * Apply all changes to the group assignment which are still cached in
     * the view (options, multiplicity, label) and forward them to DB
     *
     * @throws FxApplicationException   if the label is invalid
     */
    private void applyGroupChanges() throws FxApplicationException {
        if (group.getLabel().getIsEmpty()) {
            throw new FxApplicationException("ex.structureEditor.noLabel");
        }
        FxMultiplicity grpMul = FxMultiplicity.of(FxMultiplicity.getStringToInt(groupMinMul, false), FxMultiplicity.getStringToInt(groupMaxMul, true));

        FxJsfUtils.checkMultiplicity(grpMul.getMin(),grpMul.getMax());

        final List<FxStructureOption> removeOptions = new ArrayList<FxStructureOption>(1);
        final List<FxStructureOption> invalidOptions = new ArrayList<FxStructureOption>(1);

        // retrieve edited options
        List<FxStructureOption> newOptions = optionWrapper.asFxStructureOptionList(optionWrapper.getStructureOptions());
        // populate list of removed options
        for(FxStructureOption oldOpt : group.getOptions()) {
            if(!FxStructureOption.hasOption(oldOpt.getKey(), newOptions) || !oldOpt.isValid())
                removeOptions.add(oldOpt);
            // invalid options
            if(!oldOpt.isValid())
                invalidOptions.add(oldOpt);
        }
        //add edited options (checks if they are set)
        for (FxStructureOption o : newOptions) {
            if(o.isValid())
                group.setOption(o.getKey(), o.isOverridable(), o.getValue());
            else { // remove invalid options from the optionwrapper
                removeOptions.add(o);
                invalidOptions.add(o);
            }
        }
        // remove operation
        if (removeOptions.size() > 0) {
            for (FxStructureOption o : removeOptions) {
                group.clearOption(o.getKey());
            }
        }

        // check out diff missing options / removed options and build a message f. removed options
        if(invalidOptions.size() > 0) {
            final StringBuilder invalidOptMessage = new StringBuilder(200);
            for(FxStructureOption invalidOpt : invalidOptions) {
                invalidOptMessage.append(invalidOpt.toString());
            }
            invalidOptMessage.trimToSize();
            if(invalidOptMessage.length() > 0)
                new FxFacesMsgInfo("GroupEditor.err.propertyKeysNotSaved", invalidOptMessage.toString()).addToContext();
        }

        if (!isSystemInternal() || FxJsfUtils.getRequest().getUserTicket().isInRole(Role.GlobalSupervisor))
            group.setMultiplicity(grpMul);
    }

    public String showGroupOptionEditor() {
        return "groupOptionEditor";
    }

    public String showGroupEditor() {
        return "groupEditor";
    }

    public String showGroupAssignmentEditor() {
        return "groupAssignmentEditor";
    }

    public OptionWrapper getOptionWrapper() {
        return optionWrapper;
    }

    public WrappedOption getOptionFiler() {
        return optionFiler;
    }

    public void setOptionFiler(WrappedOption o) {
        optionFiler = o;
    }

    public String getGroupOptionValue() {
        return groupOptionValue;
    }

    public void setGroupOptionValue(String groupOptionValue) {
        this.groupOptionValue = groupOptionValue;
    }

    public String getGroupOptionKey() {
        return groupOptionKey;
    }

    public void setGroupOptionKey(String groupOptionKey) {
        this.groupOptionKey = groupOptionKey;
    }

    public boolean isGroupOptionOverridable() {
        return groupOptionOverridable;
    }

    public void setGroupOptionOverridable(boolean groupOptionOverridable) {
        this.groupOptionOverridable = groupOptionOverridable;
    }

    public String getAssignmentOptionValue() {
        return assignmentOptionValue;
    }

    public void setAssignmentOptionValue(String assignmentOptionValue) {
        this.assignmentOptionValue = assignmentOptionValue;
    }

    public String getAssignmentOptionKey() {
        return assignmentOptionKey;
    }

    public void setAssignmentOptionKey(String assignmentOptionKey) {
        this.assignmentOptionKey = assignmentOptionKey;
    }

    public boolean isAssignmentOverridable() {
        return assignmentOverridable;
    }

    public void setAssignmentOverridable(boolean assignmentOverridable) {
        this.assignmentOverridable = assignmentOverridable;
    }

    public boolean isAssignmentIsInherited() {
        return assignmentIsInherited;
    }

    public void setAssignmentIsInherited(boolean assignmentIsInherited) {
        this.assignmentIsInherited = assignmentIsInherited;
    }

    /**
     * Toggle for showing the base assignment options
     *
     * @return true if inheritance conditions are met
     */
    public boolean isShowParentAssignmentOptions() {
        showParentAssignmentOptions = FxSharedUtils.checkAssignmentInherited(assignment);
        return showParentAssignmentOptions;
    }

    public void setShowParentAssignmentOptions(boolean showParentAssignmentOptions) {
        this.showParentAssignmentOptions = showParentAssignmentOptions;
    }

    public void addAssignmentOption() {
        try {
            optionWrapper.addOption(optionWrapper.getAssignmentOptions(), assignmentOptionKey, assignmentOptionValue, assignmentOverridable, assignmentIsInherited);
            // reset vars
            assignmentOptionKey = null;
            assignmentOptionValue = null;
            assignmentOverridable = true;
            assignmentIsInherited = true;
        }
        catch (Throwable t) {
             new FxFacesMsgErr(t).addToContext();
        }
    }

    public void addGroupOption() {
        try {
           optionWrapper.addOption(optionWrapper.getStructureOptions(),
                groupOptionKey, groupOptionValue, groupOptionOverridable);
            groupOptionKey = null;
            groupOptionValue = null;
            groupOptionOverridable = true;
        }
         catch (Throwable t) {
             new FxFacesMsgErr(t).addToContext();
        }
    }

    public void deleteAssignmentOption() {
        optionWrapper.deleteOption(optionWrapper.getAssignmentOptions(), optionFiler);
    }

    public void deleteGroupOption() {
        optionWrapper.deleteOption(optionWrapper.getStructureOptions(), optionFiler);
    }

    public OptionWrapper getOptionWrapperParent() {
        return optionWrapperParent;
    }

    public void setOptionWrapperParent(OptionWrapper optionWrapperParent) {
        this.optionWrapperParent = optionWrapperParent;
    }

    /**
     * Hack in order to use command buttons to submit the form values
     * and update the view of GUI elements
     */
    public void doNothing() {
    }

    /**
     * Returns all pgroup assignments that are referencing this property which the
     * current user may see, excluding the system internal assignments.
     *
     * @return  a list of group assignments that are referencing this group.
     */
    public List<FxGroupAssignment> getReferencingGroupAssignments() {
        List<FxGroupAssignment> assignments = CacheAdmin.getFilteredEnvironment().getGroupAssignments(true);
        List<FxGroupAssignment> result = new ArrayList<FxGroupAssignment>();
        for (FxGroupAssignment assignment : assignments) {
            if (assignment.getGroup().getId() == group.getId() && !assignment.isSystemInternal()) {
                result.add(assignment);
            }
        }
        return result;
    }
}
