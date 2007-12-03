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
package com.flexive.war.beans.admin.structure;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.war.beans.admin.structure.OptionWrapper.WrappedOption;
import com.flexive.war.javascript.tree.StructureTreeWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.List;

/**
 * Bean behind groupAssignmentEditor.xhtml, groupEditor.xhtml and groupOptionEditor to
 * edit FxGroupAssignment and FxGroup objects
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class GroupEditorBean {

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
    private OptionWrapper optionWrapper = null;
    private WrappedOption optionFiler = null;
    private FxType parentType = null;
    private String parentXPath = null;
    private boolean editMode = false;

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

                if (StructureTreeWriter.DOC_TYPE_TYPE.equals(nodeType) || StructureTreeWriter.DOC_TYPE_TYPE_RELATION.equals(nodeType)) {
                    parentType = CacheAdmin.getEnvironment().getType(id);
                }

                if (StructureTreeWriter.DOC_TYPE_GROUP.equals(nodeType)) {
                    FxGroupAssignment ga = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(id);
                    parentType = ga.getAssignedType();
                    parentXPath = ga.getXPath();
                }

                group = FxGroupEdit.createNew("NEWGROUP", new FxString(""), new FxString(""), false, FxMultiplicity.MULT_0_1);
                initNewGroupEditing();
            } else if ("assignGroup".equals(action)) {
                editMode = false;
                long id = FxJsfUtils.getLongParameter("id");
                String nodeType = FxJsfUtils.getParameter("nodeType");

                parentXPath = "/";

                if (StructureTreeWriter.DOC_TYPE_TYPE.equals(nodeType) || StructureTreeWriter.DOC_TYPE_TYPE_RELATION.equals(nodeType)) {
                    parentType = CacheAdmin.getEnvironment().getType(id);
                }

                if (StructureTreeWriter.DOC_TYPE_GROUP.equals(nodeType)) {
                    FxGroupAssignment ga = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(id);
                    parentType = ga.getAssignedType();
                    parentXPath = ga.getXPath();
                }
                long assignmentId = EJBLookup.getAssignmentEngine().save(FxGroupAssignmentEdit.createNew(assignment, parentType, assignment.getAlias(), parentXPath), true);
                StructureTreeControllerBean s = (StructureTreeControllerBean) FxJsfUtils.getManagedBean("structureTreeControllerBean");
                s.addAction(StructureTreeControllerBean.ACTION_RELOAD_SELECT_ASSIGNMENT, assignmentId, "");
            }

        } catch (Throwable t) {
            LOG.error("Failed to parse request parameters: " + t.getMessage(), t);
            new FxFacesMsgErr(t).addToContext();
        }

        return null;
    }

    public void toggleEditMode() {
        editMode = !editMode;
    }

    public boolean getEditMode() {
        return editMode;
    }

    public boolean isSystemInternal() {
        return assignment == null ? false : assignment.isSystemInternal();
    }

    private void initEditing() {
        assignmentMinMul = FxMultiplicity.getIntToString(assignment.getMultiplicity().getMin());
        assignmentMaxMul = FxMultiplicity.getIntToString(assignment.getMultiplicity().getMax());
        groupMinMul = FxMultiplicity.getIntToString(group.getMultiplicity().getMin());
        groupMaxMul = FxMultiplicity.getIntToString(group.getMultiplicity().getMax());
        optionWrapper = new OptionWrapper(group.getOptions(), assignment.getOptions(), false);
    }

    private void initNewGroupEditing() {
        group.setOverrideMultiplicity(true);
        groupMinMul = FxMultiplicity.getIntToString(group.getMultiplicity().getMin());
        groupMaxMul = FxMultiplicity.getIntToString(group.getMultiplicity().getMax());
        optionWrapper = new OptionWrapper(group.getOptions(), null, false);
    }

    //return if the groupMode may be changed.
    // this is the case for groups where no content yet exists
    public boolean isMayChangeGroupMode() {
        try {
            return EJBLookup.getAssignmentEngine().getAssignmentInstanceCount(assignment.getId()) == 0;
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            //safe mode
            return false;
        }
    }

    public String getAssignmentAlias() {
        return assignment.getAlias();
    }

    public void setAssignmentAlias(String a) {
        try {
            assignment.setAlias(a);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public int getAssignmentDefaultMultiplicity() {
        return assignment.getDefaultMultiplicity();
    }

    public void setAssignmentDefaultMultiplicity(int m) {
        assignment.setDefaultMultiplicity(m);
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

    public void saveChanges() {
        try {
            applyGroupChanges();
            applyAssignmentChanges();
            EJBLookup.getAssignmentEngine().save(group);
            EJBLookup.getAssignmentEngine().save(assignment, false);
            StructureTreeControllerBean s = (StructureTreeControllerBean) FxJsfUtils.getManagedBean("structureTreeControllerBean");
            s.addAction(StructureTreeControllerBean.ACTION_RENAME_SELECT_ASSIGNMENT, assignment.getId(), assignment.getDisplayName());
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public void createGroup() {
        try {
            applyGroupChanges();
            long assignmentId;
            if (parentType != null)
                assignmentId = EJBLookup.getAssignmentEngine().createGroup(parentType.getId(), group, parentXPath);
            else
                assignmentId = EJBLookup.getAssignmentEngine().createGroup(group, parentXPath);
            StructureTreeControllerBean s = (StructureTreeControllerBean) FxJsfUtils.getManagedBean("structureTreeControllerBean");
            s.addAction(StructureTreeControllerBean.ACTION_RELOAD_SELECT_ASSIGNMENT, assignmentId, "");

        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    private void applyAssignmentChanges() throws FxApplicationException {
        if (assignment.getLabel().getIsEmpty()) {
            throw new FxApplicationException("ex.structureEditor.noLabel");
        }
        FxMultiplicity assMul = new FxMultiplicity(FxMultiplicity.getStringToInt(assignmentMinMul),
                FxMultiplicity.getStringToInt(assignmentMaxMul));

        if (!assignment.isSystemInternal() && group.mayOverrideBaseMultiplicity())
            assignment.setMultiplicity(assMul);
        //delete current options
        while (!assignment.getOptions().isEmpty()) {
            String key = assignment.getOptions().get(0).getKey();
            assignment.clearOption(key);
        }
        List<FxStructureOption> newAssignmentOptions = optionWrapper.asFxStructureOptionList(optionWrapper.getAssignmentOptions());
        for (FxStructureOption o : newAssignmentOptions) {
            assignment.setOption(o.getKey(), o.getValue());
        }
    }

    /**
     * applies changes for groups
     *
     * @throws FxApplicationException localized exception on errors
     */
    private void applyGroupChanges() throws FxApplicationException {
        if (group.getLabel().getIsEmpty()) {
            throw new FxApplicationException("ex.structureEditor.noLabel");
        }
        FxMultiplicity grpMul = new FxMultiplicity(FxMultiplicity.getStringToInt(groupMinMul),
                FxMultiplicity.getStringToInt(groupMaxMul));

        if (!isSystemInternal())
            group.setMultiplicity(grpMul);

        while (!group.getOptions().isEmpty()) {
            String key = group.getOptions().get(0).getKey();
            group.clearOption(key);
        }
        List<FxStructureOption> newGroupOptions = optionWrapper.asFxStructureOptionList(optionWrapper.getStructureOptions());
        for (FxStructureOption o : newGroupOptions) {
            group.setOption(o.getKey(), o.isOverrideable(), o.getValue());
        }
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

    public void addAssignmentOption() {
        if (optionWrapper.addOption(optionWrapper.getAssignmentOptions(),
                assignmentOptionKey, assignmentOptionValue, false)) {
            assignmentOptionKey = null;
            assignmentOptionValue = null;
        }
    }

    public void addGroupOption() {
        if (optionWrapper.addOption(optionWrapper.getStructureOptions(),
                groupOptionKey, groupOptionValue, groupOptionOverridable)) {
            groupOptionKey = null;
            groupOptionValue = null;
            groupOptionOverridable = true;
        }
    }

    public void deleteAssignmentOption() {
        optionWrapper.deleteOption(optionWrapper.getAssignmentOptions(), optionFiler);
    }

    public void deleteGroupOption() {
        optionWrapper.deleteOption(optionWrapper.getStructureOptions(), optionFiler);
    }

    //hack in order to use command buttons to submit and update the view of GUI elements
    public void doNothing() {
    }

}
