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
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.*;
import org.apache.commons.lang.ArrayUtils;

import java.io.Serializable;
import java.util.*;

import static com.flexive.shared.EJBLookup.getAccountEngine;
import static com.flexive.shared.EJBLookup.getUserGroupEngine;
import static com.google.common.collect.Lists.newArrayList;

/**
 * Bean providing access the the userGroup functionality.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class UserGroupBean implements Serializable {
    private static final long serialVersionUID = -5545259367431927116L;
    private static final String ID_CACHE_KEY = UserGroupBean.class + "_id";

    private int overviewPageNumber;
    private int overviewRows;
    private String sortColumn;
    private String sortOrder;

    private UserGroupData currentData = new UserGroupData();

    /**
     * Holder class for a user gruop 
     */
    private static class UserGroupData {
        private String name = null;
        private String color = null;
        private long mandator = -1;
        private long id = -1;
        private Long[] roles = new Long[0];
        private Hashtable<Long, List<UserGroup>> groupLists;
        private long createdGroupId = -1;
        private List<Account> members;
        private List<ACLAssignment> aclAssignments;

        public String getName() {
            return name;
        }
    }

    /**
     * @return true if the edit tab should be opened
     * @since 3.1.4
     */
    public boolean isOpenTab() {
        return currentData != null && currentData.id >= 0;
    }

    /**
     * Opens the edit user in a tab
     * @return the name where to navigate
     * @since 3.1.4
     */
    public String openEditTab() {
        if (!isOpenTab()) return null;
        return edit(false);
    }

    /**
     * Opens the overview tab
     * @return the name where to navigate
     * @since 3.1.4
     */
    public String overview() {
        return "userGroupOverview";
    }

    public UserGroupData getCurrentData() {
        return currentData;
    }

    public void setCurrentData(UserGroupData currentData) {
        this.currentData = currentData;
    }

    public String getTmpName() {
        if (currentData != null) return currentData.name;
        return null;
    }

    public Long[] getRoles() {
        return this.currentData.roles;
    }

    public void setRoles(Long[] roles) {
        this.currentData.roles = roles;
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getName() {
        return currentData.name;
    }

    public void setName(String sName) {
        this.currentData.name = sName;
    }

    public String getColor() {
        return currentData.color;
    }

    public void setColor(String sColor) {
        this.currentData.color = sColor;
    }

    public long getMandator() {
        return currentData.mandator;
    }

    public void setMandator(long mandator) {
        this.currentData.mandator = mandator;
    }

    public long getId() {
        return currentData.id;
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

    public List<Account> getMembers() throws FxApplicationException {
        List<Account> members = currentData.members;
        long id = currentData.id;
        if ((members == null || members.isEmpty()) && id != -1) {
            members = newArrayList(getAccountEngine().getAssignedUsers(id, 0, -1));
            Collections.sort(members, new FxSharedUtils.SelectableObjectSorter());
        }
        if (members == null) {
            members = new ArrayList<Account>();
        }
        currentData.members = members;
        return members;
    }

    public List<ACLAssignment> getAclAssignments() throws FxApplicationException {
        List<ACLAssignment> aclAssignments = currentData.aclAssignments;
        long id = currentData.id;
        if ((aclAssignments == null || aclAssignments.isEmpty()) && id != -1) {
            aclAssignments = newArrayList(EJBLookup.getAclEngine().loadGroupAssignments(id));
        }
        if (aclAssignments == null) {
            aclAssignments = newArrayList();
        }
        currentData.aclAssignments = aclAssignments;
        return aclAssignments;
    }

    public void setId(long id) {
        this.currentData.id = id;
        FxJsfUtils.setSessionAttribute(ID_CACHE_KEY, id);
    }

    public long getCreatedGroupId() {
        return currentData.createdGroupId;
    }

    public UserGroupBean() {
        try {
            this.currentData.groupLists = new Hashtable<Long, List<UserGroup>>(5);
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
    }

    /**
     * Returns the list of all usergroups.
     *
     * @return all usergroups
     */
    public List<UserGroup> getList() {
        try {
            final UserTicket ticket = FxContext.getUserTicket();
            long mandatorId;
            if (ticket.isGlobalSupervisor()) {
                // Drop down list enabled -> handle it
                mandatorId = getMandator();
            } else {
                // Only show the groups of the mandator the user belongs to
                mandatorId = ticket.getMandatorId();
            }

            // Load an cache within the result
            List<UserGroup> result = currentData.groupLists.get(mandatorId);
            if (result == null) {
                result = getUserGroupEngine().loadAll(mandatorId);
                currentData.groupLists.put(mandatorId, result);
            }
            return result;


        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
            return new ArrayList<UserGroup>(0);
        }
    }

    /**
     * Get the number of users for a group, call using userGroupBean.userCount['groupId']
     *
     * @return user count
     */
    public Map<Long, Long> getUserCount() {
        return FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Long, Long>() {
            public Long get(Object key) {
                try {
                    return getAccountEngine().getAssignedUsersCount((Long) key, false);
                } catch (FxApplicationException e) {
                    return 0L;
                }
            }
        }, true);
    }

    /**
     * Creates a new user group.
     *
     * @return the next page to render
     */
    public String create() {
        try {
            final UserTicket ticket = FxContext.getUserTicket();
            long mandatorId = getMandator();
            if (!ticket.isGlobalSupervisor()) {
                mandatorId = ticket.getMandatorId();
            }
            this.setId(getUserGroupEngine().create(currentData.name, currentData.color, mandatorId));
            this.currentData.createdGroupId = this.currentData.id;
            new FxFacesMsgInfo("UserGroup.nfo.created", currentData.name).addToContext();

            // Assign the given roles to the group
            try {
                getUserGroupEngine().setRoles(this.currentData.id, ArrayUtils.toPrimitive(currentData.roles));
            } catch (Exception exc) {
                new FxFacesMsgErr(exc).addToContext();
                currentData.color = getUserGroupEngine().load(currentData.id).getColor();
                return "userGroupEdit";
            }

            // Deselect the group and return to the overview
            this.setId(-1);
            return "userGroupOverview";
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
            return "userGroupNew";
        }
    }

    /**
     * Deletes an existing user group.
     *
     * @return the next page to render.
     */
    public String delete() {
        try {
            ensureIdSet();
            getUserGroupEngine().remove(currentData.id);
            this.currentData.groupLists.clear();
            new FxFacesMsgInfo("UserGroup.nfo.deleted").addToContext();
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
        return "userGroupOverview";
    }



    /**
     * Edits a existing user group.
     *
     * @return the next page to render.
     */
    public String edit(){
        return edit(true);
    }

    /**
     * Edits a existing user group.
     *
     *
     * @param loadData should the current data be loaded ?
     * @return the next page to render.
     * @since 3.1.4
     */
    public String edit(boolean loadData) {
        try {
            ensureIdSet();
            if (loadData) {
                UserGroup grp = getUserGroupEngine().load(currentData.id);
                this.currentData.color = grp.getColor();
                this.currentData.name = grp.getName();
                this.currentData.mandator = grp.getMandatorId();
                this.currentData.roles = Role.toIdArray(getUserGroupEngine().getRoles(grp.getId()));
            }
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
        return "userGroupEdit";
    }


    private void ensureIdSet() {
        if (this.currentData.id <= 0) {
            this.currentData.id = (Long) FxJsfUtils.getSessionAttribute(ID_CACHE_KEY);
        }
    }

    /**
     * Updates a existing group.
     *
     * @return the next page to render
     */
    public String update() {
        try {
            // Update group data
            getUserGroupEngine().update(currentData.id, currentData.name, currentData.color);
            // Update the role assignments
            try {
                getUserGroupEngine().setRoles(this.currentData.id, ArrayUtils.toPrimitive(getRoles()));
                new FxFacesMsgInfo("UserGroup.nfo.updated", currentData.name).addToContext();
            } catch (Exception exc) {
                new FxFacesMsgErr(exc).addToContext();
            }
            // Reload data
            edit();
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
        return "userGroupEdit";
    }

}
