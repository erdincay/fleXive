/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
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
import static com.flexive.shared.EJBLookup.getAclEngine;
import com.flexive.shared.FxContext;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLAssignment;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.UserTicket;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Management of ACLs.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AclBean implements Serializable {
    private static final long serialVersionUID = -3767461611278032535L;

    private AclHolder currentACL = new AclHolder();

    private long assignmentId;
    private static final String ID_CACHE_KEY = AclBean.class + "_id";

    private int overviewPageNumber;
    private int overviewRows;
    private String sortColumn;
    private String sortOrder;
    private static final String ACL_EDIT = "aclEdit";


    /**
     * Holder class for an ACL object + its fields
     *
     * @since 3.1.4
     */
    private static class AclHolder implements Serializable{
        private long id = -1;
        private long mandator;
        private ACL acl = null;
        private List<ACLAssignmentEdit> assignments;
        private String selectedIds;
    }

    /**
     * @return true if the edit tab should be opened
     * @since 3.1.4
     */
    public boolean isOpenTab() {
        return currentACL != null && currentACL.id >= 0;
    }

    /**
     * Opens the edit user in a tab
     * @return the name where to navigate
     * @since 3.1.4
     */
    public String openEditTab() {
        if (!isOpenTab()) return edit();
        return ACL_EDIT;
    }

    public AclHolder getCurrentACL() {
        return currentACL;
    }

    public void setCurrentACL(AclHolder currentACL) {
        this.currentACL = currentACL;
    }

    /**
     * Navigate back to the overview and remembers the changes of the acl
     *
     * @return overview page
     * @since 3.1.4
     */
    public String overview() {
//        tmpACL = currentACL;
        return "aclOverview";
    }

    public String getSelectedIds() {
        return currentACL.selectedIds;
    }

    public void setSelectedIds(String selectedIds) {
        this.currentACL.selectedIds = selectedIds;
    }

    public long[] getSelectedIdsAsLong() {
        if (currentACL.selectedIds == null || currentACL.selectedIds.trim().length() == 0) {
            return new long[0];
        } else {
            String ids[] = currentACL.selectedIds.split(",");
            long result[] = new long[ids.length];
            int pos = 0;
            for (String id : ids) {
                result[pos++] = Long.valueOf(id);
            }
            return result;
        }
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

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public AclBean() {
        this.currentACL.acl = new ACL();
    }

    public ACL getAcl() {
        return currentACL.acl;
    }

    public void setAcl(ACL acl) {
        this.currentACL.acl = acl;
    }


    public long getMandator() {
        return currentACL.mandator;
    }

    public void setMandator(long mandator) {
        this.currentACL.mandator = mandator;
    }

    public long getId() {
        return currentACL.id;
    }

    public void setId(long id) {
        this.currentACL.id = id;
        FxJsfUtils.setSessionAttribute(ID_CACHE_KEY, id);
    }

    public List<ACLAssignmentEdit> getAssignments() {
        if (currentACL.assignments == null) {
            currentACL.assignments = new ArrayList<ACLAssignmentEdit>(5);
        }
        return currentACL.assignments;
    }

    public void setAssignments(List<ACLAssignmentEdit> assignments) {
        this.currentACL.assignments = assignments;
    }

    public void addAssignment() {
        getAssignments().add(new ACLAssignmentEdit(currentACL.acl));
    }

    public void removeAssignment() {
        for (ACLAssignmentEdit ass : currentACL.assignments) {
            if (ass.getId() == assignmentId) {
                currentACL.assignments.remove(ass);
                return;
            }
        }
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

    /**
     * Returns a list of all ACLs.
     *
     * @return a list of all ACL.
     */
    public List<ACL> getList() {
        try {
            final UserTicket ticket = FxContext.getUserTicket();
            long mandatorId = (ticket.isGlobalSupervisor()) ? getMandator() : ticket.getMandatorId();
            return CacheAdmin.getFilteredEnvironment().getACLs(mandatorId, false);
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
            return new ArrayList<ACL>(0);
        }
    }

    /**
     * Deletes the acl specified by the id field.
     *
     * @return the next page to render
     */
    public String delete() {
        try {
            ensureIdSet();
            getAclEngine().remove(currentACL.id);
            new FxFacesMsgInfo("ACL.nfo.deleted").addToContext();
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
        return "aclOverview";
    }

    /**
     * Deletes all selected acls.
     *
     * @return the next page to render
     */
    public String deleteSelected() {
        int delCount = 0;
        long _ids[] = getSelectedIdsAsLong();
        if (_ids != null && _ids.length > 0) {
            for (long _id : _ids) {
                try {
                    getAclEngine().remove(_id);
                    delCount++;
                } catch (Exception exc) {
                    new FxFacesMsgErr(exc).addToContext();
                }
            }
            if (delCount > 0) {
                new FxFacesMsgInfo("ACL.nfo.deletedWithCount", delCount).addToContext();
            } else {
                new FxFacesMsgErr("ACL.err.noAclDeleted").addToContext();
            }
        } else {
            new FxFacesMsgInfo("ACL.nfo.noSelected").addToContext();
        }
        return "aclOverview";
    }

    /**
     * Creates a new acl.
     *
     * @return the next page to render
     */
    public String create() {
        try {
            // create the acl
            final UserTicket ticket = FxContext.getUserTicket();
            long mandatorId = (ticket.isGlobalSupervisor()) ? getMandator() : ticket.getMandatorId();
            ACL acl = currentACL.acl;
            setId(getAclEngine().create(acl.getName(), acl.getLabel(), mandatorId,
                    acl.getColor(), acl.getDescription(), acl.getCategory()));
            new FxFacesMsgInfo("ACL.nfo.created", acl.getName()).addToContext();
            // load and display the acl
            return edit();
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
        return "aclCreate";
    }

    /**
     * Edits a acl.
     *
     * @return the next page to render
     */
    public String edit() {
        try {
            ensureIdSet();
            currentACL.acl = getAclEngine().load(currentACL.id);
            final List<ACLAssignment> _assignments = getAclEngine().loadAssignments(currentACL.id);
            currentACL.assignments = new ArrayList<ACLAssignmentEdit>(_assignments.size() + 5);
            for (ACLAssignment ass : _assignments)
                currentACL.assignments.add(new ACLAssignmentEdit(ass));
            currentACL.id = currentACL.acl.getId();
            return ACL_EDIT;
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
            return "aclOverview";
        }
    }

    private void ensureIdSet() {
        if (this.currentACL.id <= 0) {
            this.currentACL.id = (Long) FxJsfUtils.getSessionAttribute(ID_CACHE_KEY);
        }
    }

    /**
     * Saves the changes to the ACL.
     *
     * @return the next page to render
     */
    public String save() {
        try {
            ArrayList<ACLAssignment> list = new ArrayList<ACLAssignment>(getAssignments().size());
            for (ACLAssignmentEdit ass : getAssignments()) {
                list.add(new ACLAssignment(ass.getAclId(), ass.getGroupId(), ass.getMayRead(), ass.getMayEdit(),
                        ass.getMayRelate(), ass.getMayDelete(), ass.getMayExport(),
                        ass.getMayCreate(), ass.getACLCategory(), ass.getLifeCycleInfo()));
            }
            ACL acl = currentACL.acl;
            getAclEngine().update(currentACL.id, acl.getName(), acl.getLabel(), acl.getColor(), acl.getDescription(), list);
            this.currentACL.acl = getAclEngine().load(currentACL.id);
            new FxFacesMsgInfo("ACL.nfo.saved", acl.getName()).addToContext();
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
        return ACL_EDIT;
    }

    /**
     * Getter for all available categories.
     *
     * @return all available categories
     */
    public List<SelectItem> getCategories() {
        List<SelectItem> result = new ArrayList<SelectItem>(ACLCategory.values().length);
        for (ACLCategory cat : ACLCategory.values()) {
            result.add(new SelectItem(String.valueOf(cat.getId()), String.valueOf(cat)));
        }
        return result;
    }

    /**
     * ACLAssignmentEdit extends ACLAssignment with the ability to set/get the Group as Object
     * for the jsf frontend.
     */
    public static class ACLAssignmentEdit extends ACLAssignment implements Serializable {
        private static final long serialVersionUID = -7468423774542760311L;

        private static long ID_GEN;
        private long id;
        private long group;

        public ACLAssignmentEdit(ACLAssignment ass) {
            super(ass.getAclId(), ass.getGroupId(), ass.getMayRead(), ass.getMayEdit(), ass.getMayRelate(), ass.getMayDelete(),
                    ass.getMayExport(), ass.getMayCreate(), ass.getACLCategory(), ass.getLifeCycleInfo());
            id = generateId();
            this.group = ass.getGroupId();
        }

        public ACLAssignmentEdit(ACL acl) {
            super(acl.getId(), -1, acl.getCategory(), acl.getLifeCycleInfo());
            id = generateId();
        }

        private synchronized long generateId() {
            if (ID_GEN == Long.MAX_VALUE) {
                ID_GEN = 0;
            }
            return ID_GEN++;
        }

        public long getId() {
            return id;
        }

        public long getGroup() {
            return group;
        }

        public void setGroup(long group) {
            this.group = group;
            this.setGroupId(group);
        }
    }
}
