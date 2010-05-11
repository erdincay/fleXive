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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.value.FxDateTime;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.StringUtils;

import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.io.Serializable;


/**
 * History viewer bean
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class HistoryBean implements Serializable {
    private static final long serialVersionUID = 8097761705884319936L;

    private Map<Object, String> dateMap;

    private int maxEntries = 100;
    private String category;
    private FxString accountMatch = new FxString(false, "");
    private FxDateTime startDate = new FxDateTime(FxLanguage.SYSTEM_ID, false).setEmpty();
    private FxDateTime endDate = new FxDateTime(FxLanguage.SYSTEM_ID, false).setEmpty();
    private List<FxHistory> results;
    private List<SelectItem> availableGroups = null;
    private String type;
    private String content;

    private FxHistory selectedEntry;

    /**
     * Get all available history groups to select from
     *
     * @return available history groups
     */
    public List<SelectItem> getAvailableGroups() {
        if (availableGroups == null) {
            availableGroups = new ArrayList<SelectItem>(10);
            availableGroups.add(new SelectItem("history.", "Any"));
            availableGroups.add(new SelectItem("history.account.", "Account"));
            availableGroups.add(new SelectItem("history.acl.", "ACL"));
            availableGroups.add(new SelectItem("history.parameter.", "Parameter"));
            availableGroups.add(new SelectItem("history.content.", "Instance"));
            availableGroups.add(new SelectItem("history.type.", "Type"));
            availableGroups.add(new SelectItem("history.group.", "Type group"));
            availableGroups.add(new SelectItem("history.assignment.", "Assignments"));
            availableGroups.add(new SelectItem("history.scriptSchedule.", "Script Schedule"));
        }
        return availableGroups;
    }

    public FxHistory getSelectedEntry() {
        return selectedEntry;
    }

    public void setSelectedEntry(FxHistory selectedEntry) {
        this.selectedEntry = selectedEntry;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public FxString getAccountMatch() {
        return accountMatch;
    }

    public void setAccountMatch(FxString accountMatch) {
        this.accountMatch = accountMatch;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public FxDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(FxDateTime startDate) {
        this.startDate = startDate;
    }

    public FxDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(FxDateTime endDate) {
        this.endDate = endDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * Mapped function to return the formatted date of a history entry
     *
     * @return formatted date of a history entry
     */
    public Map<Object, String> getDateFormat() {
        if (dateMap == null) {
            dateMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Object, String>() {
                public String get(Object key) {
                    try {
                        return FxFormatUtils.getUniversalDateTimeFormat().format(key);
                    } catch (Exception e) {
                        return "unknown";
                    }
                }
            });
        }
        return dateMap;
    }

    /**
     * Get the list of history entries based on user selections
     *
     * @return list of history entries based on user selections
     */
    public List<FxHistory> getList() {
        if (results == null) {
            performQuery();
        }
        return results;
    }

    /**
     * Setter for the list for ajax postbacks
     *
     * @param list the result list
     */
    public void setList(List<FxHistory> list) {
        this.results = list;
    }

    /**
     * Execute the query
     *
     * @return navigation result
     */
    public String performQuery() {
        Long account;
        if (accountMatch != null && !accountMatch.isEmpty()) {
            try {
                String match = accountMatch.getDefaultTranslation();
                if (StringUtils.isEmpty(match))
                    account = null;
                else
                    account = EJBLookup.getAccountEngine().load(accountMatch.getBestTranslation()).getId();
            } catch (FxApplicationException e) {
                new FxFacesMsgErr("History.error.account", accountMatch.getBestTranslation()).addToContext();
                results = new ArrayList<FxHistory>(0);
                return null;
            }
        } else
            account = null;
        Long typeId = !StringUtils.isEmpty(type) ? (Long.valueOf(type) >= 0 ? Long.valueOf(type) : null) : null;
        Long contentId = !StringUtils.isEmpty(content) ? Long.valueOf(content) : null;

        results = EJBLookup.getHistoryTrackerEngine().getEntries(category, account, typeId, contentId,
                startDate.isEmpty() ? null : startDate.getDefaultTranslation(),
                endDate.isEmpty() ? null : endDate.getDefaultTranslation(),
                maxEntries);
        return null;
    }
}
