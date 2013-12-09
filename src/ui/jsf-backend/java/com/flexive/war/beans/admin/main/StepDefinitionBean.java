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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.StepDefinitionEdit;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.flexive.shared.EJBLookup.getWorkflowStepDefinitionEngine;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class StepDefinitionBean implements Serializable {
    private static final long serialVersionUID = -3654298727780108311L;

    private StepDefinitionHolder currentData = new StepDefinitionHolder();
    private static final String STEP_DEFINITION_OVERVIEW = "stepDefinitionOverview";
    private static final String STEP_DEFINITION_EDIT = "stepDefinitionEdit";

    private static class StepDefinitionHolder{
        private long stepDefinitionId = -1;
        private StepDefinitionEdit stepDefinition;
        private boolean uniqueTargetSelected;
    }

    private int overviewPageNumber;
    private int overviewRows;

    /**
     * @return true if the edit tab should be opened
     * @since 3.1.4
     */
    public boolean isOpenTab() {
        return currentData != null && currentData.stepDefinition != null && currentData.stepDefinition.getId() >= 0;
    }

    /**
     * Opens the edit mandator in a tab
     * @return the name where to navigate
     * @since 3.1.4
     */
    public String openEditTab() {
        if (!isOpenTab()) return edit();
        return STEP_DEFINITION_EDIT;
    }

    /**
     * Navigate back to the overview and remembers the changes of the mandator
     *
     * @return overview page
     */
    public String overview() {
        return STEP_DEFINITION_OVERVIEW;
    }

    public StepDefinitionHolder getCurrentData() {
        return currentData;
    }

    public void setCurrentData(StepDefinitionHolder currentData) {
        this.currentData = currentData;
    }

    /**
     * Initialize the edit form (load selected step definition)
     *
     * @return the edit form
     */
    public String edit() {
        currentData.stepDefinition =CacheAdmin.getEnvironment().getStepDefinition(currentData.stepDefinitionId).asEditable();
        initUniqueTargetSelected();
        return STEP_DEFINITION_EDIT;
    }

    /**
     * Delete the selected step definition.
     *
     * @return the overview page
     */
    public String delete() {
        try {
            long stepDefinitionId = currentData.stepDefinitionId;
            String name = CacheAdmin.getEnvironment().getStepDefinition(stepDefinitionId).getName();
            getWorkflowStepDefinitionEngine().remove(stepDefinitionId);
            new FxFacesMsgInfo("StepDefinition.nfo.deleted", name).addToContext();
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return overview();
    }

    /**
     * Create a new stepdefinition with the posted values.
     *
     * @return the overview page if creation was successful, or redisplay the edit form if it was not
     */
    public String create() {
        try {
            currentData.stepDefinitionId = getWorkflowStepDefinitionEngine().create(currentData.stepDefinition);
            new FxFacesMsgInfo("StepDefinition.nfo.created", currentData.stepDefinition.getName()).addToContext();
            return overview();
        } catch (Exception e) {
            new FxFacesMsgErr(e, "StepDefinition.err.create", e).addToContext();
            return STEP_DEFINITION_EDIT;
        }
    }

    /**
     * Save an existing step definition.
     *
     * @return overview page if the update was successful, otherwise redisplay the edit form
     */
    public String save() {
        try {
            StepDefinitionEdit stepDefinition = currentData.stepDefinition;
            if (!isUniqueTargetSelected())
                stepDefinition.setUniqueTargetId(-1);
            getWorkflowStepDefinitionEngine().update(stepDefinition);
            new FxFacesMsgInfo("StepDefinition.nfo.updated", stepDefinition.getName()).addToContext();
            return null;
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
            return null;
        }
    }

    public List<StepDefinition> getList() {
        return CacheAdmin.getFilteredEnvironment().getStepDefinitions();
    }

    public long getStepDefinitionId() {
        return currentData.stepDefinitionId;
    }

    public void setStepDefinitionId(long stepDefinitionId) {
        this.currentData.stepDefinitionId = stepDefinitionId;
    }

    public StepDefinitionEdit getStepDefinition() {
        if (currentData.stepDefinition != null) {
            return currentData.stepDefinition;
        }
        currentData.stepDefinition = new StepDefinitionEdit(new StepDefinition(-1, new FxString(""), null, -1));
        currentData.uniqueTargetSelected = currentData.stepDefinition.getUniqueTargetId() !=-1;
        return currentData.stepDefinition;
    }

    public void setStepDefinition(StepDefinitionEdit stepDefinition) {
        this.currentData.stepDefinition = stepDefinition;
        initUniqueTargetSelected();
    }

    public List<SelectItem> getStepDefinitions() {
        List<StepDefinition> sdList = CacheAdmin.getFilteredEnvironment().getStepDefinitions();
        List<StepDefinition> filteredList = new ArrayList<StepDefinition>(sdList.size());
        for (StepDefinition sd : sdList)
            if (sd.getId() != currentData.stepDefinition.getId()) {
                filteredList.add(sd);
            }
        //if list is empty, add empty element
        return filteredList.isEmpty() ? FxJsfUtils.asSelectListWithLabel(filteredList, true) : FxJsfUtils.asSelectListWithLabel(filteredList, false);
    }

    public boolean isUniqueTargetSelected() {
        return currentData.uniqueTargetSelected;
    }

    public void setUniqueTargetSelected(boolean b) {
        currentData.uniqueTargetSelected=b;
    }

    private void initUniqueTargetSelected() {
        currentData.uniqueTargetSelected = getStepDefinition().getUniqueTargetId() !=-1;
    }

    public int getOverviewPageNumber() {
        return overviewPageNumber;
    }

    public void setOverviewPageNumber(int overviewPageNumber) {
        this.overviewPageNumber = overviewPageNumber;
    }

    public int getOverviewRows() {
        return overviewRows;
    }

    public void setOverviewRows(int overviewRows) {
        this.overviewRows = overviewRows;
    }
}
