/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
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

package com.flexive.faces.components.content;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxHistory;
import static com.flexive.shared.configuration.SystemParameters.TREE_CAPTION_PROPERTY;
import com.flexive.shared.content.*;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.security.ACLPermission;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxReference;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.renderer.FxValueFormatter;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.*;

/**
 * Wrapper class for a content instance that provides convenient
 * getters and setters for enhanced GUI display for editing purposes.
 */
public class FxWrappedContent implements Serializable {
    private static final Log LOG = LogFactory.getLog(FxWrappedContent.class);

    // actual content instance
    private FxContent content;
    /**
     * Map providing the {@link FxValue} for given xpath
     */
    private ContentDataWrapper data;
    /**
     * Map returning a generated id for a specific {@link FxData} entry of the content
     */
    private FxCeIdGenerator idGenerator;
    /**
     * Map returning XPaths of assignments or groups that may be added to a specific {@link FxData} entry of the content
     */
    private AssignmentAddElementOptions addAbleAssignments;
    // the editor id of the associated contentEditor component.
    private String editorId;
    /**
     * Map returning a generated id of the parent of a specific {@link FxData} entry of the content
     */
    private FxCeIdGenerator parentIdGenerator;
     /**
     * HashMap used to access the hint of a group or property,
     * or if not set the hint of their assignment.
     */
    private PropertyHint propertyHint;
    /**
     * HashMap used to access the referenced type id of FxData.
     */
    private FxReferencedTypeId referencedTypeId;
     /**
     * HashMap returning if this FxData's assignment remesmbles the possibly reused caption property.
     */
    private IsCaptionProperty isCaptionProperty;

    private boolean reset = false;
    // GUI relevant settings
    private GuiSettings guiSettings;
    // boolean indicating if the content is referenced by another content that is currently being edited
    private boolean referenced = false;
    private transient FxContentVersionInfo versionInfo; // cached version info

    public FxWrappedContent(FxContent content, String editorId, GuiSettings guiSettings, boolean referenced) {
        this.content = content;
        this.guiSettings = guiSettings;
        this.data = new ContentDataWrapper(this);
        this.idGenerator = new FxCeIdGenerator(this);
        this.addAbleAssignments = new AssignmentAddElementOptions(this);
        this.editorId = editorId;
        this.parentIdGenerator = new FxCeParentIdGenerator(this);
        this.referencedTypeId = new FxReferencedTypeId();
        this.referenced = referenced;
        this.propertyHint = new PropertyHint();
        this.isCaptionProperty = new IsCaptionProperty();
    }

    /**
     * Returns if the content is referenced by another content that is currently being edited.
     *
     * @return if the content is referenced by another content that is currently being edited.
     */
    public boolean isReferenced() {
        return referenced;
    }

    /**
     * Returns GUI relevant settings.
     *
     * @return GUI relevant settings
     */
    public GuiSettings getGuiSettings() {
        return guiSettings;
    }

    /**
     * Sets GUI relevant settings.
     *
     * @param guiSettings gui relevant settings
     */
    public void setGuiSettings(GuiSettings guiSettings) {
        this.guiSettings = guiSettings;
    }

    /**
     * Returns if the reset flag is set. See {@link FxWrappedContent#setReset(boolean)}
     *
     * @return if the reset flag is set.
     */
    public boolean isReset() {
        return reset;
    }

    /**
     * Sets the reset flag. Upon the next request, the wrapped content will be removed from content storage and
     * reinitialized according to the contentEditor component attributes.
     *
     * @param reset true if the wrapped content should be reset upon the next request.
     */
    public void setReset(boolean reset) {
        this.reset = reset;
    }

    /**
     * Returns possible workflow steps as select items.
     *
     * @return possible workflow steps as select items.
     */
    public List<SelectItem> getPossibleWorkflowSteps() {
        FxEnvironment environment = CacheAdmin.getFilteredEnvironment();
        UserTicket ticket = FxContext.getUserTicket();
        FxType fxType = environment.getType(content.getTypeId());

        List<Step> steps;
        boolean isNew = content.getPk().isNew();
        if (isNew) {
            steps = fxType.getWorkflow().getSteps();
        } else {
            steps = fxType.getWorkflow().getTargets(content.getStepId());
            if (steps.size() == 0 || !steps.contains(environment.getStep(content.getStepId())))
                steps.add(environment.getStep(content.getStepId()));
        }
        ArrayList<SelectItem> result = new ArrayList<SelectItem>(steps.size());
        for (Step step : steps) {
            if (!fxType.isUseStepPermissions() ||
                    (isNew ? ticket.mayCreateACL(step.getAclId(), content.getLifeCycleInfo().getCreatorId())
                            : ticket.mayEditACL(step.getAclId(), content.getLifeCycleInfo().getCreatorId()))) {
                StepDefinition def = environment.getStepDefinition(step.getStepDefinitionId());
                result.add(new SelectItem(String.valueOf(step.getId()), def.getLabel().getDefaultTranslation()));
            }
        }
        result.trimToSize();
        return result;
    }

    /**
     * Returns the current workflow step id as string.
     * (Used as a value-binding for a select box
     * in combination with {@link FxWrappedContent#getPossibleWorkflowSteps()}
     *
     * @return the current workflow step as string.
     */
    public String getStep() {
        return String.valueOf(content.getStepId());
    }

    /**
     * Sets the current workflow step id.
     * (Used as a value-binding for a select box
     * in combination with {@link FxWrappedContent#getPossibleWorkflowSteps()}
     *
     * @param value the workflow step id as string.
     */
    public void setStep(String value) {
        long stepId = Long.valueOf(value);
        content.setStepId(stepId);
    }

    /**
     * The editor id of the associated content editor component.
     *
     * @return the editor id of the content editor component
     *         in which the wrapped content currently edited.
     */
    public String getEditorId() {
        return editorId;
    }

    /**
     * Returns the associated {@link FxWrappedContent.AssignmentAddElementOptions} Map.
     *
     * @return the associated {@link FxWrappedContent.AssignmentAddElementOptions} Map.
     */
    public AssignmentAddElementOptions getAddAbleAssignments() {
        return addAbleAssignments;
    }

    /**
     * Returns if the content's pk is new or if it
     * already exists in the DB.
     *
     * @return if the content's pk is new or if it already exists in the DB.
     */
    public boolean isNew() {
        return content.getPk().isNew();
    }

    /**
     * Returns the unwrapped content instance.
     *
     * @return the unwrapped content instance.
     */
    public FxContent getContent() {
        return content;
    }

    /**
     * Returns if multiple ACLs for this content's type are allowed.
     *
     * @return if multiple ACLs for this content's type are allowed
     */
    public boolean isMultipleContentACLs() {
        try {
            return CacheAdmin.getFilteredEnvironment().getType(content.getTypeId()).isMultipleContentACLs();
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return false;
        }
    }

   /**
     * Return all ACLs assigned to this content.
     *
     * @return  all ACLs assigned to this content.
     */
    public Long[] getAclIds() {
       return content.getAclIds().toArray(new Long[content.getAclIds().size()]);
    }

    /**
     * Sets the new ACL ids for this content
     *
     * @param aclIds ACL ids
     */
    public void setAclIds(Long[] aclIds) {
        content.setAclIds(Arrays.asList(aclIds));
    }

    /**
     * (Used for contents with only one instance ACL !).
     * Returns the ACL id of the content instance.
     *
      * @return ACL id of the content instance.
     */
    public Long getAclId() {
        if (getAclIds().length >0)
            return getAclIds()[0];
        return null;
    }

    /**
     * Set the content ACL id. If more than one ACL was assigned, the additional ACLs are removed
     * before assigning the new ACL.
     *
     * @param aclId the ACL id
     */
    public void setAclId(Long aclId) {
       content.setAclId(aclId);
    }

    /**
     * Returns if this content instance supports security based on instance permissions.
     *
     * @return if this content instance supports security based on instance permissions.
     */
    public boolean isSupportSecurity() {
        return CacheAdmin.getFilteredEnvironment().getType(content.getTypeId()).isUseInstancePermissions();
    }

    /**
     * Returns true if more than one version exists and user has
     * {@link com.flexive.shared.security.ACLPermission#DELETE} permission, false otherwise.
     *
     * @return true if more than one version exists and user has
     *         {@link com.flexive.shared.security.ACLPermission#DELETE} permission, false otherwise.
     */
    public boolean isVersionDeleteAble() {
        return content.getPermissions().isMayDelete() && getVersionInfo().getVersionCount() > 1;
    }

    /**
     * Returns the {@link com.flexive.shared.security.ACLPermission#EDIT} permission.
     *
     * @return the {@link com.flexive.shared.security.ACLPermission#EDIT} permission.
     */
    public boolean isMayImport() {
        try {
            return FxPermissionUtils.checkPermission(FxContext.getUserTicket(), content.getLifeCycleInfo().getCreatorId(),
                    ACLPermission.EDIT, CacheAdmin.getFilteredEnvironment().getType(content.getTypeId()),
                    CacheAdmin.getFilteredEnvironment().getStep(content.getStepId()).getAclId(), content.getAclIds(), false);
        } catch (FxNoAccessException e) {
            LOG.warn(e);
            return false;
        }
    }

    /**
     * Returns the preview icon of the content's associated type, or null if no icon is existing.
     *
     * @return the preview icon of the content's associated type, or null if no icon is existing.
     */
    public FxPK getTypeIcon() {
        final FxReference icon = CacheAdmin.getFilteredEnvironment().getType(content.getTypeId()).getIcon();
        return icon.isEmpty() ? null : icon.getDefaultTranslation();
    }

    /**
     * Returns the version information.
     *
     * @return the version information.
     */
    public FxContentVersionInfo getVersionInfo() {
        try {
            if (versionInfo == null || versionInfo.getId() != content.getId()) {
                versionInfo = isNew() ? FxContentVersionInfo.createEmpty() : EJBLookup.getContentEngine().getContentVersionInfo(content.getPk());
            }
            return versionInfo;
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return FxContentVersionInfo.createEmpty();
        }
    }

    /**
     * The history entries.
     *
     * @return the history entries.
     */
    public List<FxHistory> getHistoryEntries() {
        // TODO: cache the history entries
        return EJBLookup.getHistoryTrackerEngine().getContentEntries(content.getId());
    }

    /**
     * Returns the {@link FxWrappedContent.ContentDataWrapper} Map.
     *
     * @return the {@link FxWrappedContent.ContentDataWrapper} Map.
     */
    public ContentDataWrapper getData() {
        return data;
    }

    /**
     * Returns the {@link FxWrappedContent.FxCeIdGenerator} Map.
     *
     * @return the {@link FxWrappedContent.FxCeIdGenerator} Map.
     */
    public FxCeIdGenerator getIdGenerator() {
        return idGenerator;
    }

    /**
     * Returns the {@link FxWrappedContent.FxCeParentIdGenerator} Map.
     *
     * @return the {@link FxWrappedContent.FxCeParentIdGenerator} Map.
     */
    public FxCeIdGenerator getParentIdGenerator() {
        return parentIdGenerator;
    }

    /**
     * Returns the {@link com.flexive.faces.components.content.FxWrappedContent.FxReferencedTypeId} Map.
     *
     * @return the {@link FxWrappedContent.FxReferencedTypeId} Map.
     */
    public FxReferencedTypeId getReferencedTypeId() {
        return referencedTypeId;
    }

    /**
     * Returns the {@link com.flexive.faces.components.content.FxWrappedContent.PropertyHint} Map.
     *
     * @return the {@link FxWrappedContent.PropertyHint} Map.
     */
    public PropertyHint getPropertyHint() {
        return propertyHint;
    }

     /**
     * Returns the {@link com.flexive.faces.components.content.FxWrappedContent.IsCaptionProperty} Map.
     *
     * @return the {@link FxWrappedContent.IsCaptionProperty} Map.
     */
    public IsCaptionProperty getIsCaptionProperty() {
        return isCaptionProperty;     
    }

    /**
     * Map providing the {@link FxValue} for given xpath
     */
    private static class ContentDataWrapper extends Hashtable<String, FxValue> {
        private FxWrappedContent content;

        public ContentDataWrapper(FxWrappedContent content) {
            this.content = content;
        }

        public FxValue put(String xpath, FxValue value) {
            FxValue oldValue = get(xpath);
            try {
                content.getContent().setValue(xpath, value);
            } catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
            return oldValue;
        }

        public FxValue get(Object xpath) {
            try {
                return content.getContent().getPropertyData(String.valueOf(xpath)).getValue();
            } catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
                return new FxString("Error for " + xpath + ":" + t.getMessage());
            }
        }
    }

    /**
     * Encapuslates GUI relevant settings which are used by the contentEditor component.
     */
    public static class GuiSettings implements Serializable {
        private boolean editMode;
        // disable ACL selection
        private boolean disableAcl;
        // disable WF-Step selection
        private boolean disableWorkflow;
        // disable Buttons
        private boolean disableEdit;
        private boolean disableDelete;
        private boolean disableVersion;
        private boolean disableCompact;
        private boolean disableSave;
        private boolean disableCancel;
        private boolean disableButtons;
        // disable assignment actions
        private boolean disableAddAssignment;
        private boolean disableRemoveAssignment;
        private boolean disablePositionAssignment;
        // disable rendering of "h:messages" inside the template
        private boolean disableMessages;
        // id of jsf-component to re-render after ajax-requests
        private String reRender;
        // custom value formatter
        private FxValueFormatter valueFormatter;
        // form prefix
        private String formPrefix;
        private String openedReferenceId;

        public GuiSettings(boolean editMode, boolean disableAcl, boolean disableWorkflow,
                           boolean disableEdit, boolean disableDelete, boolean disableVersion,
                           boolean disableCompact, boolean disableSave, boolean disableCancel,
                           boolean disableButtons, boolean disableAddAssignment, boolean disableRemoveAssignment,
                           boolean disablePositionAssignment, boolean disableMessages, String formPrefix,
                           String reRender, FxValueFormatter valueFormatter) {
            this.editMode = editMode;
            this.disableAcl = disableAcl;
            this.disableWorkflow = disableWorkflow;
            this.disableEdit = disableEdit;
            this.disableDelete = disableDelete;
            this.disableVersion = disableVersion;
            this.disableCompact = disableCompact;
            this.disableSave = disableSave;
            this.disableCancel = disableCancel;
            this.disableButtons = disableButtons;
            this.disableAddAssignment = disableAddAssignment;
            this.disableRemoveAssignment = disableRemoveAssignment;
            this.disablePositionAssignment = disablePositionAssignment;
            this.disableMessages = disableMessages;
            this.formPrefix = formPrefix;
            this.reRender = reRender;
            this.valueFormatter = valueFormatter;
        }

        public static GuiSettings createGuiSettingsForReference(GuiSettings guiSettings, boolean editMode) {
            return new GuiSettings(editMode, guiSettings.isDisableAcl(), guiSettings.isDisableWorkflow(),
                    guiSettings.isDisableEdit(), true, guiSettings.isDisableVersion(),
                    guiSettings.isDisableCompact(), guiSettings.isDisableSave(), guiSettings.isDisableCancel(),
                    guiSettings.isDisableButtons(), guiSettings.isDisableAddAssignment(),
                    guiSettings.isDisableRemoveAssignment(), guiSettings.isDisablePositionAssignment(),
                    guiSettings.isDisableMessages(), guiSettings.formPrefix, guiSettings.reRender, guiSettings.valueFormatter);
        }


        public String getOpenedReferenceId() {
            return openedReferenceId;
        }

        public void setOpenedReferenceId(String openedReferenceId) {
            this.openedReferenceId = openedReferenceId;
        }

        public String getFormPrefix() {
            return formPrefix;
        }

        public void setFormPrefix(String formPrefix) {
            this.formPrefix = formPrefix;
        }

        public boolean isEditMode() {
            return editMode;
        }

        public void setEditMode(boolean editMode) {
            this.editMode = editMode;
        }

        public boolean isDisableAcl() {
            return disableAcl;
        }

        public void setDisableAcl(boolean disableAcl) {
            this.disableAcl = disableAcl;
        }

        public boolean isDisableWorkflow() {
            return disableWorkflow;
        }

        public void setDisableWorkflow(boolean disableWorkflow) {
            this.disableWorkflow = disableWorkflow;
        }

        public boolean isDisableEdit() {
            return disableEdit;
        }

        public void setDisableEdit(boolean disableEdit) {
            this.disableEdit = disableEdit;
        }

        public boolean isDisableDelete() {
            return disableDelete;
        }

        public void setDisableDelete(boolean disableDelete) {
            this.disableDelete = disableDelete;
        }

        public boolean isDisableVersion() {
            return disableVersion;
        }

        public void setDisableVersion(boolean disableVersion) {
            this.disableVersion = disableVersion;
        }

        public boolean isDisableCompact() {
            return disableCompact;
        }

        public void setDisableCompact(boolean disableCompact) {
            this.disableCompact = disableCompact;
        }

        public boolean isDisableSave() {
            return disableSave;
        }

        public void setDisableSave(boolean disableSave) {
            this.disableSave = disableSave;
        }

        public boolean isDisableCancel() {
            return disableCancel;
        }

        public void setDisableCancel(boolean disableCancel) {
            this.disableCancel = disableCancel;
        }

        public boolean isDisableButtons() {
            return disableButtons;
        }

        public void setDisableButtons(boolean disableButtons) {
            this.disableButtons = disableButtons;
        }

        public boolean isDisableRemoveAssignment() {
            return disableRemoveAssignment;
        }

        public void setDisableRemoveAssignment(boolean disableRemoveAssignment) {
            this.disableRemoveAssignment = disableRemoveAssignment;
        }

        public boolean isDisableAddAssignment() {
            return disableAddAssignment;
        }

        public void setDisableAddAssignment(boolean disableAddAssignment) {
            this.disableAddAssignment = disableAddAssignment;
        }

        public boolean isDisableMessages() {
            return disableMessages;
        }

        public void setDisableMessages(boolean disableMessages) {
            this.disableMessages = disableMessages;
        }

        public boolean isDisablePositionAssignment() {
            return disablePositionAssignment;
        }

        public void setDisablePositionAssignment(boolean disablePositionAssignment) {
            this.disablePositionAssignment = disablePositionAssignment;
        }

        public String getReRender() {
            return reRender;
        }

        public void setReRender(String reRender) {
            this.reRender = reRender;
        }

        public FxValueFormatter getValueFormatter() {
            return valueFormatter;
        }

        public void setValueFormatter(FxValueFormatter valueFormatter) {
            this.valueFormatter = valueFormatter;
        }
    }

    /**
     * Map returning XPaths of assignments or groups that may be added to a specific {@link FxData} entry of the content
     */
    private static class AssignmentAddElementOptions extends HashMap<FxData, List<SelectItem>> {
        private FxWrappedContent content;

        public AssignmentAddElementOptions(FxWrappedContent content) {
            this.content = content;
        }

        public List<SelectItem> get(Object element) {

            // Avoid null pointer
            if (element == null) {
                return new ArrayList<SelectItem>(0);
            }

            try {
                ArrayList<SelectItem> result = new ArrayList<SelectItem>(10);

                // Get the group data to work on
                FxGroupData gd;
                if (element instanceof FxPropertyData) {
                    gd = ((FxPropertyData) element).getParent();
                } else {
                    gd = (FxGroupData) element;
                }

                // Determine the available options
                if (gd != null) {
                    List<String> createable = gd.getCreateableChildren(true);
                    final FxEnvironment environment = CacheAdmin.getFilteredEnvironment();
                    for (String createable_xpath : createable) {
                        String display = createable_xpath;
                        try {
                            FxAssignment ass = environment.getType(content.getContent().getTypeId()).getAssignment(createable_xpath);
                            display = ass.getDisplayName();
                        } catch (Throwable t) { /*ignore*/ }
                        result.add(new SelectItem(createable_xpath, display));
                    }
                    result.trimToSize();
                }

                return result;
            }
            catch (Throwable t) {
                System.err.println(t.getMessage());
                t.printStackTrace();
                return new ArrayList<SelectItem>(0);
            }
        }
    }

    /**
     * HashMap used to access the JSF-id of a FxData element.
     */
    public static class FxCeIdGenerator extends HashMap<String, Object> {
        protected FxWrappedContent content;

        /**
         * Constructor.
         *
         * @param content wrapped content
         */
        public FxCeIdGenerator(FxWrappedContent content) {
            super(0);
            this.content = content;
        }

        /**
         * Returns the id of the object, which is generated from its XPath.
         *
         * @param object FxData
         * @return the description, or null if the assignment id could not be resolved
         */
        public String get(Object object) {
            // Id String may only contain [a-z]|[A-Z]|'-'|'_' to be accepted by jsf components
            try {
                if (object instanceof FxData) {
                    String result = ((FxData) object).getXPathFull();
                    return content.getEditorId() + "_" + FxJsfUtils.encodeJSFIdentifier(result);
                }
                return null;
            } catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
                return null;
            }
        }

        // decode generated id to Xpath again.
        public String decode(Object object) {
             try {
                return FxJsfUtils.decodeJSFIdentifier(object.toString().substring(content.getEditorId().length()+1));
            } catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
                return null;
            }
        }

        /**
         * Decodes the id of an object.
         *
         * @param id the encoded id
         * @return the decoded id
         */
        public static String decodeToXPath(String id) {
            return FxJsfUtils.decodeJSFIdentifier(id);
        }

    }

    /**
     * HashMap used to access the JSF-id of the parent FxData element.
     */
    private static class FxCeParentIdGenerator extends FxCeIdGenerator {

        /**
         * Constructor.
         *
         * @param content wrapped content
         */
        public FxCeParentIdGenerator(FxWrappedContent content) {
            super(content);
        }

        /**
         * Returns the xpath of the parent ecnoded as JSF id.
         * If no parent exists (data is virtual root group), the id of the data itself is retuned
         *
         * @param object FxData
         * @return the xpath of the parent ecnoded as JSF id or null if FxData could not be resolved.
         */
        public String get(Object object) {
            // Id String may only contain [a-z]|[A-Z]|'-'|'_' to be accepted by jsf components
            try {
                if (object instanceof FxData) {
                    FxData data = (FxData) object;
                    String result = data.getParent() == null ? data.getXPathFull() : data.getParent().getXPathFull();
                    return content.getEditorId() + "_" + FxJsfUtils.encodeJSFIdentifier(result);
                }
                return null;
            } catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
                return null;
            }
        }
    }

    /**
     * HashMap used to access the referenced type id of FxPropertyData.
     */
    private static class FxReferencedTypeId extends HashMap<FxPropertyData, Long> {

        /**
         * HashMap used to access the referenced type id of FxPropertyData
         *
         * @param object FxPropertyData
         * @return the type id of the referenced type of the FxPropertyData instance, null if the referenced type could not be resolved.
         */
        public Long get(Object object) {
            try {
                if (object instanceof FxPropertyData) {
                    return CacheAdmin.getFilteredEnvironment().getProperty(((FxPropertyData)object).getPropertyId()).getReferencedType().getId();
                }
                return null;
            } catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
                return null;
            }
        }
    }

     /**
     * HashMap used to access the hint of a group or property,
     * or if not set the hint of their assignment.
     */
    private static class PropertyHint extends HashMap<FxPropertyData, String> {
        /**
         * HashMap used to access the hint of a group or property,
         * or if not set the hint of their assignment.
         *
         * @param object FxData
         * @return  hint of a group or property, or if not set the hint of their assignment.
         */
        public String get(Object object) {
            try {
                FxString hint=null;
                if (object instanceof FxPropertyData) {
                    hint = CacheAdmin.getFilteredEnvironment().getProperty(((FxPropertyData)object).getPropertyId()).getHint();
                    if (hint == null || hint.isEmpty()) {
                        hint = ((FxPropertyData)object).getAssignment().getHint();
                    }
                }
                else if (object instanceof FxGroupData) {
                    FxGroupAssignment ga = (FxGroupAssignment)((FxGroupData)object).getAssignment();
                    hint = ga.getGroup().getHint();
                    if (hint == null || hint.isEmpty()) {
                        hint = ga.getHint();
                    }
                }
                if (hint != null) {
                    return hint.getBestTranslation();
                }
                return null;
            } catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
                return null;
            }
        }
    }

     /**
     * HashMap returning if this FxData's assignment remesmbles the possibly reused caption property.
     */
    private static class IsCaptionProperty extends HashMap<FxPropertyData, Boolean> {
         private final static long captionId;
         static {
             long cId=-1;
             try {
                cId = EJBLookup.getConfigurationEngine().get(TREE_CAPTION_PROPERTY);
             }
             catch (Throwable t) {
                 LOG.error("failed to look up caption assignment",t);
             }
             captionId=cId;
         }

        /**
         * HashMap returning if this FxData's assignment remesmbles the possibly reused caption property.
         *
         * @param object FxData
         * @return true if this FxData's assignment remesmbles the possibly reused caption property.
         */
        public Boolean get(Object object) {
            try {
                if (object instanceof FxPropertyData) {
                    if(((FxPropertyAssignment)((FxPropertyData)object).getAssignment()).getProperty().getId() ==
                           captionId)
                        return true;
                }
                return false;
            } catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
                return false;
            }
        }
    }
}
