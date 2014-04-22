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
import com.flexive.faces.beans.ActionBean;
import com.flexive.faces.beans.SelectBean;
import com.flexive.faces.messages.FxFacesMessage;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.faces.messages.FxFacesMsgWarn;
import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.Role;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.flexive.war.javascript.tree.StructureTreeWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bean behind propertyAssignmentEditor.xhtml, propertyEditor.xhtml and propertyOptionEditor to
 * edit FxPropertyAssignment and FxProperty objects
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class PropertyEditorBean implements ActionBean, Serializable {
    private static final long serialVersionUID = 7853783961770884278L;
    private static final Log LOG = LogFactory.getLog(PropertyEditorBean.class);
    //private String gotoPropertyAssignment = null;
    private long propertyId = -1;
    //private FxLanguage defaultLanguage = null;
    private String assignmentOptionValue = null;
    private String assignmentOptionKey = null;
    private boolean assignmentOverridable = true;
    private boolean assignmentIsInherited = true;
    private String propertyOptionValue = null;
    private String propertyOptionKey = null;
    private boolean propertyOptionOverridable = true;
    private OptionWrapper.WrappedOption optionFiler = null;
    FxPropertyAssignmentEdit assignment = null;
    private String minMultiplicity = null;
    private String maxMultiplicity = null;
    private String propertyMinMultiplicity = null;
    private String propertyMaxMultiplicity = null;
    private OptionWrapper optionWrapper = null;
    //private boolean allowDefaultLanguage = false;
    private FxPropertyEdit property = null;
    private String parentXPath = null;
    private FxType parentType = null;
    //checker to restore system language
    private boolean originalLanguageSystemLanguage = false;
    //checker if current user may edit the property
    private boolean structureManagement = false;
    //checker for the editMode: if not in edit mode,
    // save and delete buttons are not rendered by the gui
    private boolean editMode = false;
    //assignment script editor tab
    private ScriptListWrapper scriptWrapper = null;
    private int scriptListFiler = -1;
    private FxScriptInfo selectedScriptInfo = null;
    private long selectedScriptEventId = -1;
    private boolean selectedDerivedUsage = true;
    private boolean selectedActive = true;
    private int defaultMultiplicity = -1;
    // show parent assignment options (CMIS / inheritance compliant derived assignments only)
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

    public long getAssignmentId() {
        return assignment != null ? assignment.getId() : -1;
    }

    //necessary only to prevent JSF errors because of value binding
    public void setAssignmentId(long assignmentId) {
    }

    public boolean isSystemInternal() {
        return property.isSystemInternal();
    }

    public boolean isPropertyMayOverrideACL() {
        return property.mayOverrideACL();
    }

    public void setPropertyMayOverrideACL(boolean b) throws FxInvalidParameterException {
        //only react to changes
        if (b != property.mayOverrideACL()) {
            if (!b && assignment != null) {
                assignment.setACL(null);
            }
            property.setOverrideACL(b);
        }
    }

    public boolean isOverrideACL() {
        return assignment.isOverridingPropertyACL();
    }

    public void setOverrideACL(boolean overrideACL) throws FxInvalidParameterException {
        //only react to changes
        if (isOverrideACL() != overrideACL) {
            if (overrideACL)
                assignment.setACL(CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()));
            else
                assignment.setACL(null);
        }
    }

    public boolean isPropertyMayOverrideBaseMultiplicity() {
        return property.mayOverrideBaseMultiplicity();
    }

    public void setPropertyMayOverrideBaseMultiplicity(boolean b) throws FxInvalidParameterException {
        //only react to changes
        if (b != property.mayOverrideBaseMultiplicity()) {
            if (!b && assignment != null) {
                assignment.setMultiplicity(null);
            }
            property.setOverrideMultiplicity(b);
        }
    }

    public boolean isOverrideMultiplicity() {
        return assignment.isOverridingPropertyMultiplicity();
    }

    public void setOverrideMultiplicity(boolean overrideMultiplicity) throws FxInvalidParameterException {
        //only react to changes
        if (isOverrideMultiplicity() != overrideMultiplicity) {
            if (overrideMultiplicity)
                assignment.setMultiplicity(new FxMultiplicity(property.getMultiplicity()));
            else
                assignment.setMultiplicity(null);
        }
    }

    public FxPropertyEdit getProperty() {
        return property;
    }

    public void setProperty(FxPropertyEdit property) {
        this.property = property;
    }

    public void setAcl(long aclid) {
        try {
            assignment.setACL(CacheAdmin.getEnvironment().getACL(aclid));
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public long getPropertyAcl() {
        return getProperty().getACL().getId();
    }

    public void setPropertyAcl(long aclid) {
        try {
            getProperty().setACL(CacheAdmin.getEnvironment().getACL(aclid));
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public long getAcl() {
        return assignment.getACL().getId();
    }

    public String getAlias() {
        return assignment.getAlias();
    }

    public void setAlias(String alias) {
        try {
            assignment.setAlias(alias);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public int getDefaultMultiplicity() {
        return defaultMultiplicity;
    }

    public void setDefaultMultiplicity(int defaultMultiplicity) {
        this.defaultMultiplicity = defaultMultiplicity;
    }

    public boolean isDefaultValueSet() {
        return assignment.hasAssignmentDefaultValue();
    }

    public void setDefaultValueSet(boolean setDefaultValue) {
        //only react to changes
        if (setDefaultValue != isDefaultValueSet())
            if (setDefaultValue)
                assignment.setDefaultValue(assignment.getProperty().getEmptyValue());
            else
                assignment.clearDefaultValue();
    }

    public FxValue getDefaultValue() {
        //check if multi language settings have changed and adjust the default value
        if (assignment.hasAssignmentDefaultValue() && assignment.isMultiLang() != assignment.getDefaultValue().isMultiLanguage()) {
            assignment.setDefaultValue(assignment.getEmptyValue());
            /*
            FxValue v = assignment.getEmptyValue();
            if (assignment.getDefaultValue().getBestTranslation() != null)
                v.setValue(assignment.getDefaultValue().getBestTranslation());
            setDefaultValue(v);
            */
        }
        return assignment.getDefaultValue();
    }

    public void setDefaultValue(FxValue val) {
        if (val != null && assignment.getDefaultValue() != null) {
            if (!assignment.getDefaultValue().getClass().equals(assignment.getProperty().getEmptyValue().getClass())) {
                /*
                if (!property.getEmptyValue().getClass().equals(property.getEmptyValue().getClass()))
                    property.setDefaultValue(property.getEmptyValue());
                */
                assignment.setDefaultValue(assignment.getProperty().getEmptyValue());
                return;
            }
        }
        try {
            assignment.setDefaultValue(val);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public boolean isPropertyDefaultValueSet() {
        return property.isDefaultValueSet();
    }

    public void setPropertyDefaultValueSet(boolean setDefaultValue) {
        //only react to changes
        if (setDefaultValue != isPropertyDefaultValueSet())
            if (setDefaultValue)
                property.setDefaultValue(property.getEmptyValue());
            else {
                property.clearDefaultValue();
            }
    }

    public FxValue getPropertyDefaultValue() {
        //check if multi language settings have changed and adjust the default value
        if (property.isDefaultValueSet() && property.isMultiLang() != property.getDefaultValue().isMultiLanguage()) {
            property.setDefaultValue(property.getEmptyValue());
            /*
            FxValue v = property.getEmptyValue();
            if (property.getDefaultValue().getBestTranslation() != null)
                v.setValue(property.getDefaultValue().getBestTranslation());
            setPropertyDefaultValue(v);
            */
        }
        return property.getDefaultValue();
    }

    /**
     * indicates if the set-default-checkbox should be disabled
     * @return <code>true</code> if nothing is selected from the 2. list
     */
    public boolean isDisableDefault() {
        boolean returnValue = false;
        if (isPropertyReference()) {
            returnValue = property.getReferencedType() == null;
        } else
        if (isPropertySelectList()) {
            returnValue = property.getReferencedList() == null;
        }
        return returnValue;
    }

    public void setPropertyDefaultValue(FxValue val) {
        if (val == null)
            return;
        if (!val.getClass().equals(property.getEmptyValue().getClass())) {
            property.setDefaultValue(property.getEmptyValue());
            return;
        }
        try {
            if (val.getClass().equals(property.getEmptyValue().getClass()))
                property.setDefaultValue(val);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public FxString getDefaultLabel() {
        return assignment.getDisplayLabel();
    }

    public void setDefaultLabel(FxString label) {
        try {
            assignment.setLabel(label);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }


    public FxString getLabel() {
        return assignment.getLabel();
    }

    public void setLabel(FxString label) {
        try {
            assignment.setLabel(label);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public FxString getPropertyLabel() {
        return getProperty().getLabel();
    }

    public void setPropertyLabel(FxString label) {
        try {
            getProperty().setLabel(label);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public boolean isEnabled() {
        return assignment.isEnabled();
    }

    public void setEnabled(boolean b) {
        assignment.setEnabled(b);
    }

    public FxString getHint() {
        return assignment.getHint();
    }

    public void setHint(FxString hint) {
        try {
            assignment.setHint(hint);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public FxString getPropertyHint() {
        return getProperty().getHint();
    }

    public void setPropertyHint(FxString hint) {
        try {
            getProperty().setHint(hint);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public String getPropertyName() {
        return getProperty().getName();
    }

    public void setPropertyName(String name) {
        try {
            getProperty().setName(name);
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public String getMinMultiplicity() {
        return minMultiplicity;
    }

    public void setMinMultiplicity(String minMultiplicity) {
        this.minMultiplicity = minMultiplicity;
    }

    public String getMaxMultiplicity() {
        return maxMultiplicity;
    }

    public void setMaxMultiplicity(String maxMultiplicity) {
        this.maxMultiplicity = maxMultiplicity;
    }

    public String getPropertyMinMultiplicity() {
        return propertyMinMultiplicity;
    }

    public void setPropertyMinMultiplicity(String minMultiplicity) {
        this.propertyMinMultiplicity = minMultiplicity;
    }

    public String getPropertyMaxMultiplicity() {
        return propertyMaxMultiplicity;
    }

    public void setPropertyMaxMultiplicity(String maxMultiplicity) {
        this.propertyMaxMultiplicity = maxMultiplicity;
    }

    public OptionWrapper.WrappedOption getOptionFiler() {
        return optionFiler;
    }

    public void setOptionFiler(OptionWrapper.WrappedOption optionFiler) {
        this.optionFiler = optionFiler;
    }

    public FxPropertyAssignmentEdit getAssignment() {
        return assignment;
    }

    public FxLanguage getDefaultLanguage() {
        //workaround for the system language, which is not loadable
        if (assignment.getDefaultLanguage() == FxLanguage.SYSTEM_ID) {
            return FxLanguage.DEFAULT;
        } else {
            try {
                return CacheAdmin.getEnvironment().getLanguage(assignment.getDefaultLanguage());
            } catch (FxRuntimeException e) {
                new FxFacesMsgErr(e).addToContext();
                return FxLanguage.DEFAULT;
            }
        }
    }

    public void setDefaultLanguage(FxLanguage defaultLanguage) {
        assignment.setDefaultLanguage(defaultLanguage.getId());
    }

    public void setAssignment(FxPropertyAssignmentEdit assignment) {
        this.assignment = assignment;
    }

    public FxDataType getPropertyDataType() {
        return getProperty().getDataType();
    }

    /**
     * Set the property's data type and update referenced type and referenced list accordingly
     *
     * @param dataType the data type
     */
    public void setPropertyDataType(FxDataType dataType) {
        boolean typeChanged = false;
        if (property.getDataType() != null && property.getDataType().getId() != dataType.getId())
            typeChanged = true;
        property.setDataType(dataType);
        if (typeChanged && assignment != null)
            assignment.setDefaultValue(property.getEmptyValue());
        property.setFulltextIndexed(dataType.isTextType());
        /*
        property.setDefaultValue(property.getDefaultValue());
        assignment.setDefaultValue(assignment.getDefaultValue());
        */
        if (dataType == FxDataType.HTML)
            optionWrapper.setOption(true, FxStructureOption.OPTION_HTML_EDITOR, true);
        if (!isPropertySelectList() && getPropertyReferencedList() != -1) {
            setPropertyReferencedList(-1);
        }
        if (!isPropertyReference() && getPropertyReferencedType() != -1) {
            setPropertyReferencedType(-1);
        }
    }

    public boolean isPropertyFulltextIndexed() {
        return getProperty().isFulltextIndexed();
    }

    public void setPropertyFulltextIndexed(boolean flag) {
        getProperty().setFulltextIndexed(flag);
    }

    /**
     * Is fulltext indexing allowed for the property (depending on the datatype)
     *
     * @return fulltext indexing allowed for the property (depending on the datatype)
     */
    public boolean isFulltextIndexingAllowed() {
        return getProperty().getDataType().isTextType();
    }

    public boolean isPropertyAutoUniquePropertyName() {
        return getProperty().isAutoUniquePropertyName();
    }

    public void setPropertyAutoUniquePropertyName(boolean b) {
        getProperty().setAutoUniquePropertyName(b);
    }

    public UniqueMode getPropertyUniqueMode() {
        return getProperty().getUniqueMode();
    }

    public boolean isPropertyUsedInInstance() {
        boolean result = true;
        try {
            result = !property.isNew() && (EJBLookup.getAssignmentEngine().getPropertyInstanceCount(propertyId) > 0);
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
        return result;
    }

    public void setPropertyUniqueMode(UniqueMode u) {
        getProperty().setUniqueMode(u);
    }

    public long getPropertyReferencedList() {
        if (property.getReferencedList() == null) {
            return -1;
        }
        return property.getReferencedList().getId();
    }

    public void setPropertyReferencedList(long id) {
        if (id == -1) {
            property.setReferencedList(null);
        } else {
            property.setReferencedList(CacheAdmin.getEnvironment().getSelectList(id));
        }
    }

    public boolean getPropertyHasReferencedType() {
        return property.hasReferencedType();
    }

    public long getPropertyReferencedType() {
        if (getProperty().getReferencedType() != null) {
            return getProperty().getReferencedType().getId();
        } else return -1;
    }

    public String getPropertyReferencedTypeName() {
        if (getPropertyReferencedType() != -1)
            return CacheAdmin.getEnvironment().getType(getPropertyReferencedType()).getName();
        else
            return "";
    }

    /**
     * Returns a all available Types as List&lt;SelectItem&gt; and adds an empty element for null.
     *
     * @return available Types including a dummy value for null.
     */
    public List<SelectItem> getTypes() {
        List<FxType> typesList = CacheAdmin.getFilteredEnvironment().getTypes(true/*baseTypes*/, true/*derivedTypes*/, true/*types*/, false/*relations*/);
        final List<SelectItem> result = new ArrayList<SelectItem>(typesList.size() + 1);
        result.add(new SelectItem((long) -1, ""));
        for (FxType item : typesList) {
            result.add(new SelectItem(item.getId(), item.getDisplayName()));
        }
        return result;
    }

    public void setPropertyReferencedType(long id) {
        if (id != -1) {
            getProperty().setReferencedType(CacheAdmin.getEnvironment().getType(id));
        } else {
            getProperty().setReferencedType(null);
        }
    }

    public long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(long propertyId) {
        this.propertyId = propertyId;
    }

    public String getAssignmentOptionValue() {
        return assignmentOptionValue;
    }

    public void setAssignmentOptionValue(String optionValue) {
        this.assignmentOptionValue = optionValue;
    }

    public String getAssignmentOptionKey() {
        return assignmentOptionKey;
    }

    public void setAssignmentOptionKey(String optionKey) {
        this.assignmentOptionKey = optionKey;
    }

    public boolean isPropertyOptionOverridable() {
        return propertyOptionOverridable;
    }

    public void setPropertyOptionOverridable(boolean propertyOptionOverridable) {
        this.propertyOptionOverridable = propertyOptionOverridable;
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

    public void addAssignmentOption() {
        try {
            optionWrapper.addOption(optionWrapper.getAssignmentOptions(),
                    assignmentOptionKey, assignmentOptionValue, assignmentOverridable,
                    assignmentIsInherited);
            assignmentOptionKey = null;
            assignmentOptionValue = null;
            // set the "default" values f. overridable and isInherited
            assignmentOverridable = true;
            assignmentIsInherited = true;

        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public String getPropertyOptionValue() {
        return propertyOptionValue;
    }

    public void setPropertyOptionValue(String propertyOptionValue) {
        this.propertyOptionValue = propertyOptionValue;
    }

    public String getPropertyOptionKey() {
        return propertyOptionKey;
    }

    public void setPropertyOptionKey(String propertyOptionKey) {
        this.propertyOptionKey = propertyOptionKey;
    }

    public void addPropertyOption() {
        try {
            optionWrapper.addOption(optionWrapper.getStructureOptions(),
                    propertyOptionKey, propertyOptionValue, propertyOptionOverridable,
                    true); // base option inheritance - always "true"
            // reset after add
            propertyOptionKey = null;
            propertyOptionValue = null;
            propertyOptionOverridable = true;
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public void deleteAssignmentOption() {
        optionWrapper.deleteOption(optionWrapper.getAssignmentOptions(), optionFiler);
    }

    public void deletePropertyOption() {
        optionWrapper.deleteOption(optionWrapper.getStructureOptions(), optionFiler);
    }

    public OptionWrapper getOptionWrapper() {
        return optionWrapper;
    }

    /**
     * Hack in order to use command buttons to submit the form values
     * and update the view of GUI elements
     */
    public void doNothing() {
    }

    /**
     * Returns all property assignments that are referencing this property which the
     * current user may see, excluding the system internal assignments.
     *
     * @return a list of property assignments that are referencing this property.
     */
    public List<FxPropertyAssignment> getReferencingPropertyAssignments() {
        return CacheAdmin.getFilteredEnvironment().getReferencingPropertyAssignments(property.getId());
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

    public boolean isPropertyMayOverrideMultiLang() {
        FxStructureOption ml = property.getOption(FxStructureOption.OPTION_MULTILANG);
        return ml.isSet() && ml.isOverridable();
    }

    public boolean isPropertyMayOverrideReferenceSelectOne() {
        FxStructureOption ml = property.getOption(FxStructureOption.OPTION_REFERENCE_SELECTONE);
        return ml.isSet() && ml.isOverridable();
    }

    public void setPropertyMayOverrideMultiLang(boolean b) {
        FxStructureOption ml = property.getOption(FxStructureOption.OPTION_MULTILANG);
        if (!ml.isSet() && b)
            property.setOption(FxStructureOption.OPTION_MULTILANG, true, false);
        if (ml.isSet())
            property.setOption(FxStructureOption.OPTION_MULTILANG, b, ml.isValueTrue());
        if (ml.isSet() && !b && assignment != null)
            assignment.clearOption(FxStructureOption.OPTION_MULTILANG);
    }

    public void setPropertyMayOverrideReferenceSelectOne(boolean b) {
        FxStructureOption ml = property.getOption(FxStructureOption.OPTION_REFERENCE_SELECTONE);
        if (!ml.isSet() && b)
            property.setOption(FxStructureOption.OPTION_REFERENCE_SELECTONE, true, false);
        if (ml.isSet())
            property.setOption(FxStructureOption.OPTION_REFERENCE_SELECTONE, b, ml.isValueTrue());
        if (ml.isSet() && !b && assignment != null)
            assignment.clearOption(FxStructureOption.OPTION_REFERENCE_SELECTONE);
    }

    public boolean isPropertyMultiLanguage() {
        return property.isMultiLang();
    }

    public void setPropertyMultiLanguage(boolean b) throws FxInvalidParameterException {
        FxStructureOption ml = property.getOption(FxStructureOption.OPTION_MULTILANG);
        if ((!ml.isSet() && b) || ml.isSet()) {
            try {
                property.setOption(FxStructureOption.OPTION_MULTILANG, ml.isOverridable(), b);
            }
            catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        }
    }

    public boolean isPropertyReferenceSelectOne() {
        return property.isReferenceSelectOne();
    }

    /**
     * Sets the property for the OptionReferenceSelectOne
     *
     * Always set it, even if value is false
     * If first created, set it to overwritable
     * @param b the value to set the property
     * @throws FxInvalidParameterException If something went wrong
     */
    public void setPropertyReferenceSelectOne(boolean b) throws FxInvalidParameterException {
        FxStructureOption ml = property.getOption(FxStructureOption.OPTION_REFERENCE_SELECTONE);
        try {
            optionWrapper.setOption(true, FxStructureOption.OPTION_REFERENCE_SELECTONE, b);
            if (!ml.isSet()) {
                optionWrapper.getOption(true, FxStructureOption.OPTION_REFERENCE_SELECTONE).setOverridable(true);
            }
        }
        catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    public boolean isAssignmentReferenceSelectOne() {
        return assignment.isReferenceSelectOne();
    }

    /**
     * Sets the assignment property for the OptionReferenceSelectOne
     *
     * Always set it, even if value is false
     * @param b the value to set the property
     * @throws FxInvalidParameterException If something went wrong
     */
    public void setAssignmentReferenceSelectOne(boolean b) throws FxInvalidParameterException {
        if (!isOverrideReferenceSelectOne()) return;
        try {
            assignment.setOption(FxStructureOption.OPTION_REFERENCE_SELECTONE, b);
            final OptionWrapper.WrappedOption wrappedOption = optionWrapper.getOption(true, FxStructureOption.OPTION_REFERENCE_SELECTONE);
            // If the current value is the same as the value in the property, delete the unused property from the optionWrapper
            if (b == wrappedOption.getBooleanValue()) {
                optionWrapper.deleteOption(optionWrapper.getAssignmentOptions(), wrappedOption);
            } else {
                optionWrapper.setOption(false, FxStructureOption.OPTION_REFERENCE_SELECTONE, b);
            }
        }
        catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }


    /**
     * Returns if the option FxStructureOption.OPTION_MULTILANG is set.
     * This option controls the multilingualism of a property.
     *
     * @return true if the generic option FxStructureOption.OPTION_MULTILANG is set.
     */

    public boolean isMultiLang() {
        return assignment.isMultiLang();
    }

    public boolean isAssignmentMultiLang() {
        FxStructureOption multilang = null;
        for (FxStructureOption o : assignment.getOptions()) {
            if (o.getKey().equals(FxStructureOption.OPTION_MULTILANG)) {
                multilang = o;
                break;
            }
        }
        return multilang != null && multilang.isValueTrue();
    }

    /**
     * Sets the FxStructureOption.OPTION_MULTILANG option defensively by considering
     * option overriding.
     *
     * @param b boolean to set the option
     */
    public void setAssignmentMultiLang(boolean b) {
        //only react to changing values
        if (assignment.isMultiLang() != b) {
            try {
                assignment.setOption(FxStructureOption.OPTION_MULTILANG, b);
            }
            catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        }
    }

    public boolean isOverrideMultiLang() {
        if (assignment == null)
            return false;
        //handle multi lang option seperately
        FxStructureOption multilang = null;
        for (FxStructureOption o : assignment.getOptions()) {
            if (o.getKey().equals(FxStructureOption.OPTION_MULTILANG)) {
                multilang = o;
                break;
            }
        }
        return multilang != null && isPropertyMayOverrideMultiLang() && multilang.isSet();
    }

    public void setOverrideMultiLang(boolean overrideMultiLang) {
        //only react to changing values
        if (isOverrideMultiLang() != overrideMultiLang) {
            try {
                if (overrideMultiLang)
                    assignment.setOption(FxStructureOption.OPTION_MULTILANG, !isPropertyMultiLanguage());
                else
                    assignment.clearOption(FxStructureOption.OPTION_MULTILANG);
            }
            catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        }
    }

    public boolean isOverrideReferenceSelectOne() {
        if (assignment == null)
            return false;
        for (FxStructureOption o : assignment.getOptions()) {
            if (o.getKey().equals(FxStructureOption.OPTION_REFERENCE_SELECTONE)) {
                return o.isSet();
            }
        }
        return false;
    }

    public void setOverrideReferenceSelectOne(boolean overrideReferenceSelectOne) {
        //only react to changing values
        if (isOverrideReferenceSelectOne() != overrideReferenceSelectOne) {
            try {
                if (overrideReferenceSelectOne)
                    assignment.setOption(FxStructureOption.OPTION_REFERENCE_SELECTONE, !isPropertyReferenceSelectOne());
                else {
                    assignment.clearOption(FxStructureOption.OPTION_REFERENCE_SELECTONE);
                }
            }
            catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        }
    }

    @Override
    public String getParseRequestParameters() {
        try {
            String action = FxJsfUtils.getParameter("action");
            if (StringUtils.isBlank(action)) {
                return null;
            } else if ("openInstance".equals(action)) {
                editMode = false;
                long assId = FxJsfUtils.getLongParameter("id", -1);
                FxPropertyAssignmentEdit assignment = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(assId)).asEditable();
                setAssignment(assignment);
                setProperty(assignment.getPropertyEdit());
                setPropertyId(assignment.getProperty().getId());
                initEditing();
            } else if ("editInstance".equals(action)) {
                editMode = true;
                long assId = FxJsfUtils.getLongParameter("id", -1);
                FxPropertyAssignmentEdit assignment = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(assId)).asEditable();
                setAssignment(assignment);
                setProperty(assignment.getPropertyEdit());
                setPropertyId(assignment.getProperty().getId());
                initEditing();
            } else if ("createProperty".equals(action)) {
                editMode = true;
                assignment = null;
                parentXPath = null;
                parentType = null;
                originalLanguageSystemLanguage = false;

                long id = FxJsfUtils.getLongParameter("id");
                String nodeType = FxJsfUtils.getParameter("nodeType");

                parentXPath = "/";

                if (StructureTreeWriter.NODE_TYPE_TYPE.equals(nodeType) ||
                        StructureTreeWriter.NODE_TYPE_TYPE_RELATION.equals(nodeType)) {
                    parentType = CacheAdmin.getEnvironment().getType(id);
                }

                if (StructureTreeWriter.NODE_TYPE_GROUP.equals(nodeType)) {
                    FxGroupAssignment ga = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(id);
                    parentType = ga.getAssignedType();
                    parentXPath = XPathElement.stripType(ga.getXPath());
                }

                property = FxPropertyEdit.createNew("NEWPROPERTY", new FxString("").setEmpty(), new FxString("").setEmpty(),
                        FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE),
                        FxDataType.Text);
                initNewPropertyEditing();
            }
        } catch (Throwable t) {
            LOG.error("Failed to parse request parameters: " + t.getMessage(), t);
            new FxFacesMsgErr(t).addToContext();
            return "structureContent";
        }

        return null;
    }

    /**
     * Is the assignment flattenable and not already flattened?
     *
     * @return flattenable
     */
    public boolean isFlattenable() {
        return !assignment.isFlatStorageEntry() && !assignment.isSystemInternal()
                && EJBLookup.getAssignmentEngine().isFlattenable(assignment);
    }

    public void flatten() throws FxApplicationException {
        EJBLookup.getAssignmentEngine().flattenAssignment(assignment);
        assignment = CacheAdmin.getEnvironment().getPropertyAssignment(assignment.getXPath()).asEditable();
        new FxFacesMsgInfo("PropertyEditor.message.info.converted", assignment.getLabel()).addToContext();
    }

    public void unflatten() throws FxApplicationException {
        EJBLookup.getAssignmentEngine().unflattenAssignment(assignment);
        assignment = CacheAdmin.getEnvironment().getPropertyAssignment(assignment.getXPath()).asEditable();
        new FxFacesMsgInfo("PropertyEditor.message.info.converted", assignment.getLabel()).addToContext();
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

    /**
     * Initializes variables and does workarounds so editing of an existing property and
     * property assignment is possible via the webinterface
     */
    private void initEditing() {
        // reset selected script
        this.selectedScriptInfo = null;
        structureManagement = FxJsfUtils.getRequest().getUserTicket().isInRole(Role.StructureManagement);
        if (!assignment.isNew())
            scriptWrapper = new ScriptListWrapper(assignment.getId(), false);

        this.defaultMultiplicity = assignment.getDefaultMultiplicity();
        setMinMultiplicity(FxMultiplicity.getIntToString(assignment.getMultiplicity().getMin()));
        setMaxMultiplicity(FxMultiplicity.getIntToString(assignment.getMultiplicity().getMax()));
        setPropertyMinMultiplicity(FxMultiplicity.getIntToString(property.getMultiplicity().getMin()));
        setPropertyMaxMultiplicity(FxMultiplicity.getIntToString(property.getMultiplicity().getMax()));

        optionWrapper = new OptionWrapper(property.getOptions(), assignment.getOptions(), true, isShowParentAssignmentOptions());
        if(isShowParentAssignmentOptions()) {
            optionWrapperParent = new OptionWrapper(null, CacheAdmin.getEnvironment().getAssignment(assignment.getBaseAssignmentId()).getOptions(), false);
        }

        try {
            //workaround for the system language, which is not loadable:
            //set default language as language during the editing process
            //if the property assignment didn't become multilang and antoher language was
            //assigned, ->restore the system language in the applyChanges method
            if (assignment.getDefaultLanguage() == FxLanguage.SYSTEM_ID) {
                originalLanguageSystemLanguage = true;
            } else {
                setDefaultLanguage(CacheAdmin.getEnvironment().getLanguage(assignment.getDefaultLanguage()));
            }
        } catch (Throwable t) {
            LOG.error("Failed to initialize the Editing process: " + t.getMessage(), t);
            new FxFacesMsgErr(t).addToContext();
        }
    }

    /**
     * Returns if the FxProperty's Data Type is reference or inlinereference
     * in order to enable or disable gui elements.
     *
     * @return true if the data type is reference
     */
    public boolean isPropertyReference() {
        if (property.getDataType() == null)
            return false;
        else
            return (property.getDataType().getId() == FxDataType.InlineReference.getId() ||
                    property.getDataType().getId() == FxDataType.Reference.getId());
    }


    /**
     * Initializes variables necessarry for creating a new property via the web interface.
     * during the creation process, new properties don't have assignments yet.
     */
    private void initNewPropertyEditing() {
        structureManagement = FxJsfUtils.getRequest().getUserTicket().isInRole(Role.StructureManagement);
        property.setAutoUniquePropertyName(true);
        setPropertyMinMultiplicity(FxMultiplicity.getIntToString(property.getMultiplicity().getMin()));
        setPropertyMaxMultiplicity(FxMultiplicity.getIntToString(property.getMultiplicity().getMax()));
        optionWrapper = new OptionWrapper(property.getOptions(), null, true);
        property.setDataType(FxDataType.String1024);
    }

    /**
     * Returns if the Fxproperty's Data Type is  SelectOne or SelectMany
     * in order to enable or disable gui elements.
     *
     * @return true if the data type is select list
     */
    public boolean isPropertySelectList() {
        if (property.getDataType() == null)
            return false;
        else
            return (property.getDataType().getId() == FxDataType.SelectMany.getId() ||
                    property.getDataType().getId() == FxDataType.SelectOne.getId());
    }

    /**
     * Stores a newly created property in DB
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void createProperty() {
        if (FxJsfUtils.getRequest().getUserTicket().isInRole(Role.StructureManagement)) {
            try {
                applyPropertyChanges();
                long assignmentId;
                AssignmentEngine assignmentEngine = EJBLookup.getAssignmentEngine();
                String oName = property.getName();
                if (parentType != null) {
                    assignmentId = assignmentEngine.createProperty(parentType.getId(), property, parentXPath);
                }
                else {
                    assignmentId = assignmentEngine.createProperty(property, parentXPath);
                }
                String newName = CacheAdmin.getEnvironment().getPropertyAssignment(assignmentId).getProperty().getName();
                if (!newName.equals(oName))
                    msgs.add(new FxFacesMsgWarn("ex.structure.property.notUnique", oName, newName));

                StructureTreeControllerBean s = (StructureTreeControllerBean) FxJsfUtils.getManagedBean("structureTreeControllerBean");
                s.addAction(StructureTreeControllerBean.ACTION_RELOAD_OPEN_ASSIGNMENT, assignmentId, "");
                hasMsg = true;
                msgs.add(new FxFacesMsgInfo("PropertyEditor.message.info.created"));
            } catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        } else
            new FxFacesMsgErr(new FxApplicationException("ex.role.notInRole", "StructureManagement")).addToContext();
    }

    /**
     * Forward property and property assignment changes to the DB
     */
    public void saveChanges() {
        try {
            if (!property.isNew())
                saveScriptChanges();
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }

        if (assignment.getProperty().getReferencedType() != null && assignment.getAssignedType().getId() == assignment.getProperty().getReferencedType().getId() && // selfreference
            !minMultiplicity.equalsIgnoreCase("0") ) {
            new FxFacesMsgErr("PropertyEditor.err.selfReference").addToContext();
            return;
        }

        if (FxJsfUtils.getRequest().getUserTicket().isInRole(Role.StructureManagement)) {
            try {
                applyPropertyChanges();
                EJBLookup.getAssignmentEngine().save(property);
                savePropertyAssignmentChanges();
                StructureTreeControllerBean s = (StructureTreeControllerBean) FxJsfUtils.getManagedBean("structureTreeControllerBean");
                s.addAction(StructureTreeControllerBean.ACTION_RENAME_ASSIGNMENT, assignment.getId(), assignment.getDisplayName());
                new FxFacesMsgInfo("PropertyEditor.message.info.savedChanges", assignment.getLabel()).addToContext();
                reInit();
            }
            catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        } else
            new FxFacesMsgInfo("StructureEditor.info.notInRole.structureManagement").addToContext();
    }

    /**
     * Re-initialise variables after saving
     */
    private void reInit() {
        property = CacheAdmin.getEnvironment().getProperty(property.getId()).asEditable();
        assignment = ((FxPropertyAssignment)CacheAdmin.getEnvironment().getAssignment(assignment.getId())).asEditable();
        // reload the optionWrapper
        optionWrapper = new OptionWrapper(property.getOptions(), assignment.getOptions(), true, isShowParentAssignmentOptions());
    }

    /**
     * Apply all changes to the property assignment which are still cached in
     * the view (property options, multiplicity, label, scripts) and forward them to DB
     *
     * @throws FxApplicationException if the label is invalid
     */
    private void savePropertyAssignmentChanges() throws FxApplicationException {
        if (assignment.getLabel().getIsEmpty()) {
            throw new FxApplicationException("ex.structureEditor.noLabel");
        }

        final List<FxStructureOption> removeOptions = new ArrayList<FxStructureOption>(1);
        final List<FxStructureOption> invalidOptions = new ArrayList<FxStructureOption>(1);

        int min = FxMultiplicity.getStringToInt(minMultiplicity, false);
        int max = FxMultiplicity.getStringToInt(maxMultiplicity, true);

        // retrieve edited options
        List<FxStructureOption> newOptions = optionWrapper.asFxStructureOptionList(optionWrapper.getAssignmentOptions());
        newOptions.addAll(assignment.getOptions());
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

        //in any case restore the system language for systeminternal properties
        if (isSystemInternal() && originalLanguageSystemLanguage) {
            assignment.setDefaultLanguage(FxLanguage.SYSTEM_ID);
        }

        if (!isSystemInternal()
                || FxJsfUtils.getRequest().getUserTicket().isInRole(Role.GlobalSupervisor)) {
            if (isOverrideMultiplicity() && getProperty().mayOverrideBaseMultiplicity()) {
                FxJsfUtils.checkMultiplicity(min, max);
                assignment.setMultiplicity(FxMultiplicity.of(min, max));
            }
            assignment.setDefaultMultiplicity(this.defaultMultiplicity);

            if (originalLanguageSystemLanguage && !isMultiLang()) {
                assignment.setDefaultLanguage(FxLanguage.SYSTEM_ID);
            }
            EJBLookup.getAssignmentEngine().save(assignment, false);
        }
    }

    /**
     * Apply all changes to the property which are still cached in
     * the view (property options, multiplicity, label)
     *
     * @throws FxApplicationException if the label is invalid
     */
    private void applyPropertyChanges() throws FxApplicationException {
        if (property.getLabel().getIsEmpty()) {
            throw new FxApplicationException("ex.structureEditor.noLabel");
        }

        final List<FxStructureOption> removeOptions = new ArrayList<FxStructureOption>(1);
        final List<FxStructureOption> invalidOptions = new ArrayList<FxStructureOption>(1);

        int min = FxMultiplicity.getStringToInt(propertyMinMultiplicity, false);
        int max = FxMultiplicity.getStringToInt(propertyMaxMultiplicity, true);

        FxJsfUtils.checkMultiplicity(min, max);

        // retrieve edited options
        List<FxStructureOption> newOptions = optionWrapper.asFxStructureOptionList(optionWrapper.getStructureOptions());
        newOptions.addAll(property.getOptions());
        // populate list of removed options
        for(FxStructureOption oldOpt : property.getOptions()) {
            if(!FxStructureOption.hasOption(oldOpt.getKey(), newOptions) || !oldOpt.isValid()) {
                if(!removeOptions.contains(oldOpt))
                    removeOptions.add(oldOpt);
            }
            // invalid options
            if(!oldOpt.isValid() && !invalidOptions.contains(oldOpt))
                invalidOptions.add(oldOpt);
        }
        //add edited options (checks if they are set)
        for (FxStructureOption o : newOptions) {
            if(o.isValid())
                property.setOption(o.getKey(), o.isOverridable(), o.getValue());
            else { // remove invalid options from the optionwrapper
                if(!removeOptions.contains(o))
                    removeOptions.add(o);
                if(!invalidOptions.contains(o))
                    invalidOptions.add(o);
            }
        }
        // remove operation
        if (removeOptions.size() > 0) {
            for (FxStructureOption o : removeOptions) {
                property.clearOption(o.getKey());
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
                new FxFacesMsgInfo("PropertyEditor.err.propertyKeysNotSaved", invalidOptMessage.toString()).addToContext();
        }

        if (!isSystemInternal() || FxJsfUtils.getRequest().getUserTicket().isInRole(Role.GlobalSupervisor)) {
            property.setMultiplicity(FxMultiplicity.of(min, max));
        }
    }


    /**
     * Show the PropertyAssignmentEditor
     *
     * @return the next page
     */
    public String showPropertyAssignmentEditor() {
        return "propertyAssignmentEditor";
    }

    /**
     * Show the PropertyEditor
     *
     * @return the next page
     */
    public String showPropertyEditor() {
        return "propertyEditor";
    }

    /**
     * Show the OptionEditor
     *
     * @return the next page
     */
    public String showPropertyOptionEditor() {
        return "propertyOptionEditor";
    }

    /**
     * ************** script editor tab begin ***********************
     */

    /**
     * Show the AssignmentScriptEditor
     *
     * @return the next page
     */
    public String showAssignmentScriptEditor() {
        return "assignmentScriptEditor";
    }

    /**
     * called from the script editor; to open an instance where the script is assigned to
     *
     * @return type editor page
     */
    public String gotoAssignmentScriptEditor() {
        editMode = false;
        long propId = FxJsfUtils.getLongParameter("oid", -1);
        setPropertyId(propId);
        FxPropertyAssignmentEdit assignment = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(propId)).asEditable();
        setAssignment(assignment);
        setProperty(assignment.getPropertyEdit());
        initEditing();
        return showAssignmentScriptEditor();
    }

    public ScriptListWrapper getScriptWrapper() {
        return scriptWrapper;
    }

    public int getScriptCount() {
        return scriptWrapper == null ? 0 : scriptWrapper.getScriptList().size();
    }

    public int getScriptListFiler() {
        return scriptListFiler;
    }

    public void setScriptListFiler(int scriptListFiler) {
        this.scriptListFiler = scriptListFiler;
    }

    public void removeScript() {
        scriptWrapper.remove(scriptListFiler);
    }

    public long getSelectedScriptInfoId() {
        if (getSelectedScriptInfo() == null)
            return -1;
        return getSelectedScriptInfo().getId();
    }

    public void setSelectedScriptInfoId(long selectedScriptInfoId) {
        if (selectedScriptInfoId != -1)
            setSelectedScriptInfo(CacheAdmin.getEnvironment().getScript(selectedScriptInfoId));
    }

    public FxScriptInfo getSelectedScriptInfo() {
        if (selectedScriptInfo == null) {
            SelectBean b = new SelectBean();
            if (b.getAssignmentScripts().size() > 0)
                selectedScriptInfo = CacheAdmin.getEnvironment().getScript((Long) b.getAssignmentScripts().get(0).getValue());
        }
        return selectedScriptInfo;
    }

    public void setSelectedScriptInfo(FxScriptInfo selectedScriptInfo) {
        this.selectedScriptInfo = selectedScriptInfo;
    }

    public long getSelectedScriptEventId() {
        if (selectedScriptInfo != null)
            return selectedScriptInfo.getEvent().getId();
        else return -1;
        //return selectedScriptEventId;
    }

    public void setSelectedScriptEventId(long selectedScriptEventId) {
        this.selectedScriptEventId = selectedScriptEventId;
    }

    public boolean isSelectedDerivedUsage() {
        return selectedDerivedUsage;
    }

    public void setSelectedDerivedUsage(boolean selectedDerivedUsage) {
        this.selectedDerivedUsage = selectedDerivedUsage;
    }

    public boolean isSelectedActive() {
        return selectedActive;
    }

    public void setSelectedActive(boolean selectedActive) {
        this.selectedActive = selectedActive;
    }

    public OptionWrapper getOptionWrapperParent() {
        return optionWrapperParent;
    }

    public void setOptionWrapperParent(OptionWrapper optionWrapperParent) {
        this.optionWrapperParent = optionWrapperParent;
    }

    public String getOpenParentOptions() {
        return openParentOptions;
    }

    public void setOpenParentOptions(String openParentOptions) {
        this.openParentOptions = openParentOptions;
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

    public void addScript() {
        try {
            scriptWrapper.add(selectedScriptInfo.getId(), selectedScriptEventId, selectedDerivedUsage, selectedActive);
            this.selectedScriptInfo = CacheAdmin.getFilteredEnvironment().getScripts().get(0);
            this.selectedScriptInfo.getEvent().getId();
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public Map<Long, String> getAssignmentNameForId() {
        return new HashMap<Long, String>() {
            private static final long serialVersionUID = -1106753357173316138L;

            @Override
            public String get(Object key) {
                return CacheAdmin.getFilteredEnvironment().getAssignment((Long) key).getXPath();
            }
        };
    }

    /**
     * Saves script assignment changes to DB.
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on errors
     */
    private void saveScriptChanges() throws FxApplicationException {
        boolean mayChange = FxJsfUtils.getRequest().getUserTicket().isInRole(Role.ScriptManagement);
        for (ScriptListWrapper.ScriptListEntry e : scriptWrapper.getDelta(assignment.getId())) {
            if (!mayChange) {
                new FxFacesMsgInfo("StructureEditor.info.notInRole.scriptManagement").addToContext();
                return;
            }
            if (e.getId() == ScriptListWrapper.ID_SCRIPT_ADDED)
                EJBLookup.getScriptingEngine().createAssignmentScriptMapping(e.getScriptEvent(),
                        e.getScriptInfo().getId(), e.isDerived() ? e.getDerivedFrom() : assignment.getId(),
                        e.isActive(), e.isDerivedUsage());
            else if (e.getId() == ScriptListWrapper.ID_SCRIPT_REMOVED)
                EJBLookup.getScriptingEngine().removeAssignmentScriptMappingForEvent(e.getScriptInfo().getId(),
                        e.isDerived() ? e.getDerivedFrom() : assignment.getId(), e.getScriptEvent());
            else if (e.getId() == ScriptListWrapper.ID_SCRIPT_UPDATED)
                EJBLookup.getScriptingEngine().updateAssignmentScriptMappingForEvent(e.getScriptInfo().getId(),
                        e.isDerived() ? e.getDerivedFrom() : assignment.getId(), e.getScriptEvent(), e.isActive(),
                        e.isDerivedUsage());
        }
    }

    /****script editor tab end*********/
}
