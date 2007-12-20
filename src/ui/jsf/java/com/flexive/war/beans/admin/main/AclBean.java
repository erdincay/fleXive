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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.interfaces.ACLEngine;
import com.flexive.shared.interfaces.UserGroupEngine;
import com.flexive.shared.security.*;

import javax.faces.event.ActionEvent;
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
public class AclBean {

    private long id;
    private Mandator mandator;
    private ACL acl = null;
    private ACLEngine aclEngine;
    private UserGroupEngine groupEngine;
    private List<ACLAssignmentEdit> assignments;
    private long assignmentId;
    private static final String ID_CACHE_KEY = AclBean.class + "_id";
    private String selectedIds;


    public String getSelectedIds() {
        return selectedIds;
    }

    public void setSelectedIds(String selectedIds) {
        this.selectedIds = selectedIds;
    }

    public long[] getSelectedIdsAsLong() {
        if (selectedIds == null || selectedIds.trim().length() == 0) {
            return new long[0];
        } else {
            String ids[] = selectedIds.split(",");
            long result[] = new long[ids.length];
            int pos = 0;
            for (String id : ids) {
                result[pos++] = Long.valueOf(id);
            }
            return result;
        }
    }

    public long getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }

    public AclBean() {
        this.aclEngine = EJBLookup.getACLEngine();
        this.groupEngine = EJBLookup.getUserGroupEngine();
        this.acl = new ACL();
    }

    public ACL getAcl() {
        return acl;
    }

    public void setAcl(ACL acl) {
        this.acl = acl;
    }


    public Mandator getMandator() {
        return mandator;
    }

    public void setMandator(Mandator mandator) {
        this.mandator = mandator;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
        FxJsfUtils.setSessionAttribute(ID_CACHE_KEY, id);
    }

    public List<ACLAssignmentEdit> getAssignments() {
        if (assignments == null) {
            assignments = new ArrayList<ACLAssignmentEdit>(5);
        }
        return assignments;
    }

    public void setAssignments(ArrayList<ACLAssignmentEdit> assignments) {
        this.assignments = assignments;
    }

    public void addAssignment(ActionEvent event) {
        getAssignments().add(new ACLAssignmentEdit(acl));
    }

    public void removeAssignment(ActionEvent event) {
        for (ACLAssignmentEdit ass : assignments) {
            if (ass.getId() == assignmentId) {
                assignments.remove(ass);
                return;
            }
        }
    }

    /**
     * Returns a list of all ACLs.
     *
     * @return a list of all ACL.
     */
    public ArrayList<ACL> getList() {
        try {
            final UserTicket ticket = FxContext.get().getTicket();
            long mandatorId = (ticket.isGlobalSupervisor()) ?
                    (mandator == null ? -1 : mandator.getId()) : ticket.getMandatorId();
            return aclEngine.loadAll(mandatorId, true);
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
            aclEngine.remove(id);
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
                    aclEngine.remove(_id);
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
            final UserTicket ticket = FxContext.get().getTicket();
            long mandatorId = (ticket.isGlobalSupervisor()) ?
                    (mandator == null ? -1 : mandator.getId()) : ticket.getMandatorId();
            setId(aclEngine.create(acl.getName(), acl.getLabel(), mandatorId,
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
            acl = aclEngine.load(id);
            ArrayList<ACLAssignment> _assignments = aclEngine.loadAssignments(id);
            assignments = new ArrayList<ACLAssignmentEdit>(_assignments.size() + 5);
            for (ACLAssignment ass : _assignments)
                assignments.add(new ACLAssignmentEdit(ass));
            id = acl.getId();
            return "aclEdit";
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
            return "aclOverview";
        }
    }

    private void ensureIdSet() {
        if (this.id <= 0) {
            this.id = (Long) FxJsfUtils.getSessionAttribute(ID_CACHE_KEY);
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
                        ass.getMayCreate(), ass.getACLCategory()));
            }
            aclEngine.update(id, acl.getName(), acl.getLabel(), acl.getColor(), acl.getDescription(), list);
            this.acl = aclEngine.load(id);
            new FxFacesMsgInfo("ACL.nfo.saved", acl.getName()).addToContext();
        } catch (Exception exc) {
            new FxFacesMsgErr(exc).addToContext();
        }
        return "aclEdit";
    }

    /**
     * Getter for all available categories.
     *
     * @return all available categories
     */
    public ArrayList<SelectItem> getCategories() {
        ArrayList<SelectItem> result = new ArrayList<SelectItem>(ACL.Category.values().length);
        for (ACL.Category cat : ACL.Category.values()) {
            result.add(new SelectItem(String.valueOf(cat.getId()), String.valueOf(cat)));
        }
        return result;
    }

    /**
     * ACLAssignmentEdit extends ACLAssignment with the ability to set/get the Group as Object
     * for the jsf frontend.
     */
    public static class ACLAssignmentEdit extends ACLAssignment implements Serializable {
        private static long ID_GEN;
        private long id;
        private UserGroup group;

        public ACLAssignmentEdit(ACLAssignment ass) {
            super(ass.getAclId(), ass.getGroupId(), ass.getMayRead(), ass.getMayEdit(), ass.getMayRelate(), ass.getMayDelete(),
                    ass.getMayExport(), ass.getMayCreate(), ass.getACLCategory());
            id = generateId();
        }

        public ACLAssignmentEdit(ACL acl) {
            super(acl.getId(), -1, acl.getCategory());
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

        public UserGroup getGroup() {
            if (this.getGroupId() < 0) return null;
            if (group == null) {
                try {
                    group = getGroupEngine().load(this.getGroupId());
                } catch (Throwable t) {
                    new FxFacesMsgErr(t).addToContext();
                }
            }
            return group;
        }

        public void setGroup(UserGroup group) {
            this.group = group;
            this.setGroupId(group.getId());
        }

        /**
         * Helper funcion, return the group interface of the ACL beans itself.
         *
         * @return the group interface
         */
        private UserGroupEngine getGroupEngine() {
            return ((AclBean) FxJsfUtils.getManagedBean("aclBean")).groupEngine;
        }
    }
}
