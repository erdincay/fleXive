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
import com.flexive.faces.beans.MessageBean;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import java.util.*;

/**
 * Bean behind typeEditor.xhtml to
 * edit FxType objects
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class TypeEditorBean {

    private static final Log LOG = LogFactory.getLog(TypeEditorBean.class);
    private FxTypeEdit type = null;
    private List<SelectItem> selectedMandators = null;
    private List<SelectItem> deselectedMandators = null;
    private SelectItem mandatorFiler = null;
    private List<WrappedRelation> wrappedRelations = null;
    private long defaultSelectListTypeId = CacheAdmin.getEnvironment().getTypes(true, true, true, false).get(0).getId();
    private long relSourceIdFiler = defaultSelectListTypeId;
    private long relDestIdFiler = defaultSelectListTypeId;
    private int relMaxSourceFiler = DEFAULT_REL_MAX;
    private int relMaxDestFiler = DEFAULT_REL_MAX;
    private boolean relSourceUnlimitedFiler = true;
    private boolean relDestUnlimitedFiler = true;
    private boolean editMode = false;


    private WrappedRelation wrappedRelationFiler = null;
    private boolean unlimitedVersions = true;
    private List<SelectItem> timeRanges = null;
    private int timeRange = -1;
    private long historyAge = -1;
    private boolean maxRelSourceUnlimited = false;
    private boolean maxRelDestUnlimited = false;
    private static final int DEFAULT_REL_MAX = 100;
    //checker if current user may edit the property
    private boolean mayEdit = false;
    //checker if current user may delete the property
    private boolean mayDelete = false;

    //type script editor tab
    private int scriptEventId = -1;
    private int selectedScriptId = -1;
    private FxScriptEvent scriptEvent;
    private long scriptId = -1;
    private FxScriptEvent selectedEvent;
    // the script/scripttype combinations for which the mappings for the currently specified type should be created
    private ArrayList<FxScriptInfo> selectedScripts = new ArrayList<FxScriptInfo>();


    public boolean isMayEdit() {
        return mayEdit;
    }

    public boolean isMayDelete() {
        return mayDelete;
    }

    public String getParseRequestParameters() {
        try {
            String action = FxJsfUtils.getParameter("action");
            if (StringUtils.isBlank(action)) {
                return null;
            } else if ("openInstance".equals(action)) {
                editMode = false;
                long propId = FxJsfUtils.getLongParameter("id", -1);
                mayEdit = FxJsfUtils.getBooleanParameter("mayEdit", false);
                mayDelete = FxJsfUtils.getBooleanParameter("mayDelete", false);

                this.type = CacheAdmin.getEnvironment().getType(propId).asEditable();

                initEditing();
            } else if ("editInstance".equals(action)) {
                editMode = true;
                long propId = FxJsfUtils.getLongParameter("id", -1);
                mayEdit = FxJsfUtils.getBooleanParameter("mayEdit", false);
                mayDelete = FxJsfUtils.getBooleanParameter("mayDelete", false);

                this.type = CacheAdmin.getEnvironment().getType(propId).asEditable();

                initEditing();
            } else if ("createType".equals(action)) {
                editMode = true;
                //long propId = FxJsfUtils.getLongParameter("id", -1);
                type = FxTypeEdit.createNew("NEWTYPE", new FxString(""), CacheAdmin.getEnvironment().getDefaultACL(ACL.Category.STRUCTURE));

                initEditing();
                setTypeMode(TypeMode.Content);
            } else if ("createTypeRelation".equals(action)) {
                editMode = true;
                //long propId = FxJsfUtils.getLongParameter("id", -1);
                type = FxTypeEdit.createNew("NEWTYPE", new FxString(""), CacheAdmin.getEnvironment().getDefaultACL(ACL.Category.STRUCTURE));

                initEditing();
                setTypeMode(TypeMode.Relation);
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

    private void initEditing() {
        selectedMandators = FxJsfUtils.asSelectList(type.getAllowedMandators(), false);
        deselectedMandators = FxJsfUtils.asSelectList(CacheAdmin.getFilteredEnvironment().getMandators(true, false), false);
        List<SelectItem> toremove = new ArrayList<SelectItem>();
        for (SelectItem i : selectedMandators) {
            for (SelectItem j : deselectedMandators) {
                if (((Mandator) i.getValue()).getId() == ((Mandator) j.getValue()).getId())
                    toremove.add(j);
            }
        }
        deselectedMandators.removeAll(toremove);

        wrappedRelations = new ArrayList<WrappedRelation>(type.getRelations().size());
        for (FxTypeRelation r : type.getRelations()) {
            wrappedRelations.add(new WrappedRelation(r));
        }
        unlimitedVersions = type.getMaxVersions() == -1;
        maxRelSourceUnlimited = type.getMaxRelSource() == 0;
        maxRelDestUnlimited = type.getMaxRelDestination() == 0;
        timeRange = initTimeRange();
    }

    public boolean isMaxRelSourceUnlimited() {
        return maxRelSourceUnlimited;
    }

    public void setMaxRelSourceUnlimited(boolean maxRelSourceUnlimited) {
        if (this.maxRelSourceUnlimited && !maxRelSourceUnlimited)
            setMaxRelSource(DEFAULT_REL_MAX);
        if (!this.maxRelSourceUnlimited && maxRelSourceUnlimited)
            setMaxRelSource(0);
        this.maxRelSourceUnlimited = maxRelSourceUnlimited;
    }

    public boolean isMaxRelDestUnlimited() {
        return maxRelDestUnlimited;
    }

    public void setMaxRelDestUnlimited(boolean maxRelDestUnlimited) {
        if (this.maxRelDestUnlimited && !maxRelDestUnlimited)
            setMaxRelDestination(DEFAULT_REL_MAX);
        if (!this.maxRelDestUnlimited && maxRelDestUnlimited)
            setMaxRelDestination(0);
        this.maxRelDestUnlimited = maxRelDestUnlimited;
    }

    private int initTimeRange() {
        //0=forever, 1=ms, 2=sec, 3=min, 4=day
        historyAge = type.getHistoryAge();
        if (historyAge <= 0)
            return 0;
        if ((historyAge / 1000) > 1 && historyAge % 1000 == 0) {
            historyAge = historyAge / 1000;
        } else return 1;
        if ((historyAge / 60) > 1 && historyAge % 60 == 0) {
            historyAge = historyAge / 60;
        } else return 2;
        if ((historyAge / (60 * 24)) > 1 && historyAge % (60 * 24) == 0) {
            historyAge = historyAge / (60 * 24);
            return 4;
        } else return 3;
    }

    private long historyAgeToMilis() {
        if (timeRange == 0)
            return 0;
        if (timeRange == 1)
            return historyAge;
        else if (timeRange == 2)
            return historyAge * 1000;
        else if (timeRange == 3)
            return historyAge * 1000 * 60;
        else if (timeRange == 4)
            return historyAge * 1000 * 60 * 60 * 24;

            //should never happen
        else return -1;
    }

    public void saveChanges() {
        try {
            if (unlimitedVersions) {
                type.setMaxVersions(-1);
            }
            List<Mandator> allowedMandators = new ArrayList<Mandator>(selectedMandators.size());
            for (SelectItem i : selectedMandators) {
                allowedMandators.add((Mandator) i.getValue());
            }
            this.type.setAllowedMandators(allowedMandators);
            updateRelations();

            type.setHistoryAge(historyAgeToMilis());
            long id = EJBLookup.getTypeEngine().save(type);
            StructureTreeControllerBean s = (StructureTreeControllerBean) FxJsfUtils.getManagedBean("structureTreeControllerBean");
            if (type.isNew()) {
                s.addAction(StructureTreeControllerBean.ACTION_RELOAD_SELECT_TYPE, id, "");
            } else
                s.addAction(StructureTreeControllerBean.ACTION_RENAME_SELECT_TYPE, id, type.getDisplayName());
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    /* remove all existing relations and add newly created relations,
     * created from wrappedRelations
     *
     */

    private void updateRelations() {
        while (!type.getRelations().isEmpty()) {
            type.removeRelation(type.getRelations().get(0));
        }

        /*
        List <FxTypeRelation> relationsToDelete = type.getRelations();
        for (FxTypeRelation rel: relationsToDelete) {
            type.removeRelation(rel);
        }
        */

        for (WrappedRelation r : wrappedRelations) {
            try {
                type.updateRelation(r.asRelation());
            }
            catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        }
    }

    public int getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(int timeRange) {
        this.timeRange = timeRange;
    }

    public List<SelectItem> getTimeRanges() {
        if (timeRanges == null) {
            timeRanges = new ArrayList<SelectItem>();
            MessageBean messageBean = (MessageBean) FxJsfUtils.getManagedBean("fxMessageBean");
            timeRanges.add(new SelectItem(0, messageBean.getMessage("TypeEditor.selectlist.timeRanges.label.forever")));
            timeRanges.add(new SelectItem(1, messageBean.getMessage("TypeEditor.selectlist.timeRanges.label.ms")));
            timeRanges.add(new SelectItem(2, messageBean.getMessage("TypeEditor.selectlist.timeRanges.label.sec")));
            timeRanges.add(new SelectItem(3, messageBean.getMessage("TypeEditor.selectlist.timeRanges.label.min")));
            timeRanges.add(new SelectItem(4, messageBean.getMessage("TypeEditor.selectlist.timeRanges.label.days")));
        }
        return timeRanges;
    }

    public boolean isUnlimitedVersions() {
        return unlimitedVersions;
    }

    public void setUnlimitedVersions(boolean unlimitedVersions) {
        if (!this.unlimitedVersions && unlimitedVersions)
            type.setMaxVersions(-1);
        if (this.unlimitedVersions && !unlimitedVersions)
            type.setMaxVersions(0);
        this.unlimitedVersions = unlimitedVersions;
    }

    public List<WrappedRelation> getWrappedRelations() {
        return wrappedRelations;
    }

    public long getRelSourceIdFiler() {
        return relSourceIdFiler;
    }

    public void setRelSourceIdFiler(long relSourceIdFiler) {
        this.relSourceIdFiler = relSourceIdFiler;
    }

    public long getRelDestIdFiler() {
        return relDestIdFiler;
    }

    public void setRelDestIdFiler(long relDestIdFiler) {
        this.relDestIdFiler = relDestIdFiler;
    }

    public int getRelMaxDestFiler() {
        return relMaxDestFiler;
    }

    public void setRelMaxDestFiler(int relMaxDestFiler) {
        this.relMaxDestFiler = relMaxDestFiler;
    }

    public int getRelMaxSourceFiler() {
        return relMaxSourceFiler;
    }

    public void setRelMaxSourceFiler(int relMaxSourceFiler) {
        this.relMaxSourceFiler = relMaxSourceFiler;
    }

    public boolean isRelSourceUnlimitedFiler() {
        return relSourceUnlimitedFiler;
    }

    public void setRelSourceUnlimitedFiler(boolean relSourceUnlimitedFiler) {
        if (this.relSourceUnlimitedFiler && !relSourceUnlimitedFiler)
            this.relMaxSourceFiler = DEFAULT_REL_MAX;
        if (!this.relSourceUnlimitedFiler && relSourceUnlimitedFiler)
            this.relMaxSourceFiler = 0;
        this.relSourceUnlimitedFiler = relSourceUnlimitedFiler;
    }

    public boolean isRelDestUnlimitedFiler() {
        return relDestUnlimitedFiler;
    }

    public void setRelDestUnlimitedFiler(boolean relDestUnlimitedFiler) {
        if (this.relDestUnlimitedFiler && !relDestUnlimitedFiler)
            this.relMaxDestFiler = DEFAULT_REL_MAX;
        if (!this.relDestUnlimitedFiler && relDestUnlimitedFiler)
            this.relMaxDestFiler = 0;
        this.relDestUnlimitedFiler = relDestUnlimitedFiler;
    }

    public void addRelation() {
        WrappedRelation relationToAdd = new WrappedRelation(getRelSourceIdFiler(), getRelDestIdFiler(),
                getRelMaxSourceFiler(), getRelMaxDestFiler());
        if (!wrappedRelations.contains(relationToAdd)) {
            wrappedRelations.add(relationToAdd);
            setRelSourceIdFiler(defaultSelectListTypeId);
            setRelDestIdFiler(defaultSelectListTypeId);
            setRelMaxSourceFiler(DEFAULT_REL_MAX);
            setRelMaxDestFiler(DEFAULT_REL_MAX);
        } else
            new FxFacesMsgErr("TypeEditor.message.error.relationAlreadyExists").addToContext();
    }

    public WrappedRelation getWrappedRelationFiler() {
        return wrappedRelationFiler;
    }

    public void setWrappedRelationFiler(WrappedRelation wrappedRelationFiler) {
        this.wrappedRelationFiler = wrappedRelationFiler;
    }

    public void removeRelation() {
        WrappedRelation wrappedRelToRemove = getWrappedRelationFiler();
        int removeIndex = -1;
        for (int i = 0; i < wrappedRelations.size(); i++) {
            if (wrappedRelations.get(i).equalsCompletely(wrappedRelToRemove)) {
                removeIndex = i;
                break;
            }
        }
        if (removeIndex != -1) {
            wrappedRelations.remove(removeIndex);
        }
    }

    public void removeAllowedMandator() {
        if (mandatorFiler != null)
            if (((Mandator) getMandatorFiler().getValue()).getId() != getMandator().getId()) {
                selectedMandators.remove(getMandatorFiler());
                deselectedMandators.add(getMandatorFiler());
            }
        mandatorFiler = null;
    }

    public void addAllowedMandator() {
        if (mandatorFiler != null) {
            selectedMandators.add(getMandatorFiler());
            deselectedMandators.remove(getMandatorFiler());
        }
        mandatorFiler = null;
    }

    public SelectItem getMandatorFiler() {
        return mandatorFiler;
    }

    public void setMandatorFiler(SelectItem mandatorFiler) {
        this.mandatorFiler = mandatorFiler;
    }

    public List<SelectItem> getSelectedMandators() {
        return selectedMandators;
    }

    public List<SelectItem> getDeselectedMandators() {
        return deselectedMandators;
    }

    public FxTypeEdit getType() {
        return type;
    }

    public void setType(FxTypeEdit type) {
        this.type = type;
    }

    public void setCategory(TypeCategory tg) {
        this.type.setCategory(tg);
    }

    public TypeCategory getCategory() {
        return this.type.getCategory();
    }

    public void setLanguageMode(LanguageMode lm) {
        this.type.setLanguage(lm);
    }

    public LanguageMode getLanguageMode() {
        return this.type.getLanguage();
    }

    public void setTypeMode(TypeMode tm) {
        this.type.setMode(tm);
    }

    public TypeMode getTypeMode() {
        return this.type.getMode();
    }

    public void setTypeState(TypeState ts) {
        this.type.setState(ts);
    }

    public TypeState getTypeState() {
        return this.type.getState();
    }

    public void setWorkflow(long id) {
        this.type.setWorkflow(CacheAdmin.getEnvironment().getWorkflow(id));
    }

    public long getWorkflow() {
        return this.type.getWorkflow().getId();
    }

    public void setMandator(Mandator m) {
        this.type.setMandator(m);
        SelectItem ms = null;
        boolean found = false;
        for (SelectItem i : selectedMandators) {
            if (((Mandator) i.getValue()).getId() == m.getId()) {
                found = true;
                ms = i;
                break;
            }
        }
        if (!found) {
            for (SelectItem i : deselectedMandators) {
                if (((Mandator) i.getValue()).getId() == m.getId()) {
                    ms = i;
                    break;
                }
            }
            selectedMandators.add(ms);
            deselectedMandators.remove(ms);
        }
    }

    public Mandator getMandator() {
        return this.type.getMandator();
    }

    public void setACL(long id) {
        this.type.setACL(CacheAdmin.getEnvironment().getACL(id));
    }

    public long getACL() {
        return this.type.getACL().getId();
    }

    public void setAllowedMandators(List<Mandator> mandators) {
        this.type.setAllowedMandators(mandators);
    }

    public List<Mandator> getAllowedMandators() {
        return this.type.getAllowedMandators();
    }

    public void setCheckValidity(boolean ck) {
        this.type.setCheckValidity(ck);
    }

    public boolean isCheckValidity() {
        return this.type.isCheckValidity();
    }

    public void setDescription(FxString desc) {
        this.type.setDescription(desc);
    }

    public FxString getDescription() {
        return this.type.getDescription();
    }

    public void setHistoryAge(long age) {
        this.historyAge = age;
    }

    public long getHistoryAge() {
        return this.historyAge;
    }

    public void setMaxRelDestination(int dest) {
        this.type.setMaxRelDestination(dest);
    }

    public int getMaxRelDestination() {
        return this.type.getMaxRelDestination();
    }

    public void setMaxRelSource(int source) {
        this.type.setMaxRelSource(source);
    }

    public int getMaxRelSource() {
        return this.type.getMaxRelSource();
    }

    public void setMaxVersions(long versions) {
        this.type.setMaxVersions(versions);
    }

    public long getMaxVersions() {
        return this.type.getMaxVersions();
    }

    public void setName(String name) {
        this.type.setName(name);
    }

    public String getName() {
        return this.type.getName();
    }

    public void setRemoveInstancesWithRelationTypes(boolean rel) {
        this.type.setRemoveInstancesWithRelationTypes(rel);
    }

    public boolean isRemoveInstancesWithRelationTypes() {
        return this.type.isRemoveInstancesWithRelationTypes();
    }

    public void setTrackHistory(boolean hist) {
        this.type.setTrackHistory(hist);
    }

    public boolean isTrackHistory() {
        return this.type.isTrackHistory();
    }

    public void setUseInstancePermissions(boolean perm) {
        this.type.setUseInstancePermissions(perm);
    }

    public boolean isUseInstancePermissions() {
        return this.type.useInstancePermissions();
    }

    public void setUsePropertyPermissions(boolean perm) {
        this.type.setUsePropertyPermissions(perm);
    }

    public boolean isUsePropertyPermissions() {
        return this.type.usePropertyPermissions();
    }

    public void setUseStepPermissions(boolean perm) {
        this.type.setUseStepPermissions(perm);
    }

    public boolean isUseStepPermissions() {
        return this.type.useStepPermissions();
    }

    public void setUseTypePermissions(boolean perm) {
        this.type.setUseTypePermissions(perm);
    }

    public boolean isUseTypePermissions() {
        return this.type.useTypePermissions();
    }

    /* Simple wrapper class for FxTypeRelation with convenient getters and setters
     * for GUI operations
     */
    public class WrappedRelation {
        private long sourceId;
        private long destId;
        protected int maxSource;
        protected int maxDest;
        private boolean maxSourceUnlimited = false;
        private boolean maxDestUnlimited = false;

        WrappedRelation(FxTypeRelation r) {
            this.sourceId = r.getSource().getId();
            this.destId = r.getDestination().getId();
            this.maxSource = r.getMaxSource();
            this.maxDest = r.getMaxDestination();
        }

        public WrappedRelation(long sourceId, long destId, int maxSource, int maxDest) {
            this.sourceId = sourceId;
            this.destId = destId;
            this.maxSource = maxSource;
            if (maxSource == 0)
                maxSourceUnlimited = true;
            this.maxDest = maxDest;
            if (maxDest == 0)
                maxDestUnlimited = true;
        }

        public FxTypeRelation asRelation() {
            return new FxTypeRelation(CacheAdmin.getEnvironment().getType(sourceId),
                    CacheAdmin.getEnvironment().getType(destId),
                    maxSource, maxDest);
        }

        public long getSourceId() {
            return sourceId;
        }

        public void setSourceId(long sourceId) {
            this.sourceId = sourceId;
        }

        public long getDestId() {
            return destId;
        }

        public void setDestId(long destinationId) {
            this.destId = destinationId;
        }

        public int getMaxSource() {
            return maxSource;
        }

        public void setMaxSource(int maxSource) {
            this.maxSource = maxSource;
        }

        public int getMaxDest() {
            return maxDest;
        }

        public void setMaxDest(int maxDestination) {
            this.maxDest = maxDestination;
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof FxTypeRelation)
                return this.asRelation().equals(o);
            else if (o instanceof WrappedRelation)
                return (((WrappedRelation) o).getSourceId() == this.getSourceId() &&
                        ((WrappedRelation) o).getDestId() == this.getDestId());
            else return false;
        }

        @Override
        public int hashCode() {
            int result;
            result = (int) (sourceId ^ (sourceId >>> 32));
            result = 31 * result + (int) (destId ^ (destId >>> 32));
            return result;
        }

        public boolean equalsCompletely(Object o) {
            if (o instanceof FxTypeRelation)
                return this.asRelation().equalsCompletely(o);
            else if (o instanceof WrappedRelation)
                return (((WrappedRelation) o).getSourceId() == this.getSourceId() &&
                        ((WrappedRelation) o).getDestId() == this.getDestId() &&
                        ((WrappedRelation) o).getMaxSource() == this.getMaxSource() &&
                        ((WrappedRelation) o).getMaxDest() == this.getMaxDest());
            else return false;
        }

        public boolean isMaxSourceUnlimited() {
            return maxSourceUnlimited;
        }

        public void setMaxSourceUnlimited(boolean maxSourceUnlimited) {
            if (this.maxSourceUnlimited && !maxSourceUnlimited)
                setMaxSource(DEFAULT_REL_MAX);
            if (!this.maxSourceUnlimited && maxSourceUnlimited)
                setMaxRelSource(0);
            this.maxSourceUnlimited = maxSourceUnlimited;
        }

        public boolean isMaxDestUnlimited() {
            return maxDestUnlimited;
        }

        public void setMaxDestUnlimited(boolean maxDestUnlimited) {
            if (this.maxDestUnlimited && !maxDestUnlimited)
                setMaxDest(DEFAULT_REL_MAX);
            if (!this.maxDestUnlimited && maxDestUnlimited)
                setMaxDest(0);
            this.maxDestUnlimited = maxDestUnlimited;
        }
    }

    public int getEventScriptCount() {
        Map scriptMapping = type.getScriptMapping();
        int count = 0;
        Collection<long[]> scriptIds = scriptMapping.values();
        for (long[] scripts : scriptIds) {
            if (scripts != null)
                count += scripts.length;
        }
        return count;
    }

    public String showTypeEditor() {
        return "typeEditor";
    }

    // called from the script editor; to open an instance where the script is assigned to
    public String showTypeEd() {
        editMode = false;
        long propId = FxJsfUtils.getLongParameter("oid", -1);
        mayEdit = false;
        mayDelete = false;
        this.type = CacheAdmin.getEnvironment().getType(propId).asEditable();
        initEditing();
        return "typeEditor";
    }

    public String showTypeScriptEditor() {
        return "typeScriptEditor";
    }

    /*
     * script editor tab
     */

    //get script types for this type
    public List<SelectItem> getScriptEvents() {
        ArrayList<SelectItem> scriptTypes = new ArrayList<SelectItem>(type.getScriptMapping().keySet().size() + 1);
        for (FxScriptEvent scriptEvent : (Set<FxScriptEvent>) type.getScriptMapping().keySet()) {
            scriptTypes.add(new SelectItem(scriptEvent.getId(), scriptEvent.getName()));
        }
        Collections.sort(scriptTypes, new FxJsfUtils.SelectItemSorter());
        scriptTypes.add(0, new SelectItem(-1,
                ((MessageBean) FxJsfUtils.getManagedBean("fxMessageBean")).getMessage("ScriptEditor.list.selectItem.all")));
        return scriptTypes;
    }

    public int getScriptEventId() {
        return scriptEventId;
    }

    public void setScriptEventId(int scriptEventId) {
        this.scriptEventId = scriptEventId;
    }

    public long getScriptId() {
        return scriptId;
    }

    public void setScriptId(long scriptId) {
        this.scriptId = scriptId;
    }

    public List<FxScriptInfo> getScripts() {
        long[] scriptIds = new long[0];
        ArrayList<FxScriptInfo> scripts = new ArrayList<FxScriptInfo>();
        //-1: show ALL scripts
        if (scriptEventId == -1) {
            HashSet idSet = new HashSet<Long>();
            for (long[] sids : (Collection<long[]>) type.getScriptMapping().values()) {
                for (long l : sids) {
                    idSet.add(l);
                }
            }
            scriptIds = new long[idSet.size()];
            Long[] helper = (Long[]) idSet.toArray(new Long[0]);
            for (int i = 0; i < scriptIds.length; i++) {
                scriptIds[i] = helper[i];
            }
        } else {
            try {
                scriptIds = type.getScriptMapping(FxScriptEvent.getById(scriptEventId));
            }
            catch (Throwable t) {
                new FxFacesMsgErr(t).addToContext();
            }
        }
        if (scriptIds != null) {
            for (long l : scriptIds)
                scripts.add(CacheAdmin.getEnvironment().getScript(l));
        }
        Collections.sort(scripts, new FxJsfUtils.ScriptInfoSorter());

        return scripts;
    }

    // get scripts as id-name pairs
    public List<SelectItem> getScriptsAsId() {
        ArrayList<SelectItem> scripts = new ArrayList<SelectItem>();
        // list starts with an empty entry (for the user interface)
        scripts.add(new SelectItem(-1, ""));
        if (this.scriptEvent != null) {
            List<Long> scriptIds = EJBLookup.getScriptingEngine().getByScriptType(this.scriptEvent);
            for (long l : scriptIds) {
                scripts.add(new SelectItem(l, CacheAdmin.getEnvironment().getScript(l).getName()));
            }
        }
        return scripts;
    }


    public int getSelectedScriptId() {
        return selectedScriptId;
    }

    public String getSelectedScriptName() {
        if (this.selectedScriptId != -1) {
            return CacheAdmin.getEnvironment().getScript(this.selectedScriptId).getName();
        } else {
            return "";
        }
    }

    public void setSelectedScriptId(int selectedScriptId) {
        this.selectedScriptId = selectedScriptId;
    }


    public FxScriptEvent getScriptEvent() {
        return scriptEvent;
    }

    public void setScriptEvent(FxScriptEvent scriptEvent) {
        this.scriptEvent = scriptEvent;
    }

    public FxScriptEvent getSelectedEvent() {
        return selectedEvent;
    }

    public void setSelectedEvent(FxScriptEvent selectedEvent) {
        this.selectedEvent = selectedEvent;
    }


    // adds a script/type-combination to the list of scripts for which mappings shall be created
    public String addScript() {

        if (this.selectedScriptId != -1 && this.selectedEvent != null) {
            try {
                this.selectedScripts.add(new FxScriptInfo(this.selectedScriptId, this.selectedEvent, CacheAdmin.getEnvironment().getScript(this.selectedScriptId).getName(), "", ""));
                // reset values...
                this.selectedScriptId = -1;
                this.selectedEvent = null;
            } catch (FxInvalidParameterException e) {
                throw e.asRuntimeException();
            }
        }

        return "typeScriptEditor";
    }

    // create new script type mappings
    public String assignScripts() {
        //TODO: what if mapping already exists?
        try {
            // 1: script type, 2: id of the script, 3: id of the type
            for (FxScriptInfo info : this.selectedScripts) {           //      1              2               3
                EJBLookup.getScriptingEngine().createTypeScriptMapping(info.getEvent(), info.getId(), this.type.getId(), true, false);
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }

        this.selectedScripts = new ArrayList<FxScriptInfo>();
        return "typeScriptEditor";
    }

    public ArrayList<FxScriptInfo> getSelectedScripts() {
        return selectedScripts;
    }

    // reset values ...
    public void clearSelectedScript(ActionEvent e) {
        this.selectedScriptId = -1;
        this.selectedEvent = null;
    }

    // clear table of type mapping assignments (the ones to add)
    public String clearTable() {
        this.selectedScripts = new ArrayList<FxScriptInfo>();
        return "typeScriptEditor";
    }

    /****script editor tab end*********/

}
