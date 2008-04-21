/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.shared.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;

/**
 * A concrete assignment of an ACL to a user group
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ACLAssignment implements Serializable, Cloneable {
    private static final long serialVersionUID = -8040584065610850035L;

    private long aclId;
    private long groupId;
    private ACL.Category category;
    private boolean mayRead;
    private boolean mayEdit;
    private boolean mayRelate;
    private boolean mayDelete;
    private boolean mayExport;
    private boolean mayCreate;
    private LifeCycleInfo lifeCycleInfo = null;
    private static transient Log LOG = LogFactory.getLog(ACLAssignment.class);

    /**
     * Creates a clone.
     *
     * @return the clone
     * @throws CloneNotSupportedException
     */
    @Override
    public ACLAssignment clone() throws CloneNotSupportedException {
        ACLAssignment clone = (ACLAssignment) super.clone();
        clone.aclId = this.aclId;
        clone.groupId = this.groupId;
        clone.category = this.category;
        clone.mayRead = this.mayRead;
        clone.mayEdit = this.mayEdit;
        clone.mayRelate = this.mayRelate;
        clone.mayDelete = this.mayDelete;
        clone.mayExport = this.mayExport;
        clone.mayCreate = this.mayCreate;
        clone.lifeCycleInfo = this.lifeCycleInfo;
        return clone;
    }


    /**
     * Clones the ACLAssignmentData array.
     *
     * @param data the array to clone
     * @return the clone
     */
    public static ACLAssignment[] clone(ACLAssignment data[]) {
        try {
            ACLAssignment aadClone[] = new ACLAssignment[data.length];
            int pos = 0;
            for (ACLAssignment item : data) {
                aadClone[pos++] = item.clone();
            }
            return aadClone;
        } catch (CloneNotSupportedException exc) {
            LOG.fatal("Unable to clone ACLAssignmentData[]: " + exc.getMessage(), exc);
            return null;
        }
    }

    /**
     * Constructor.
     *
     * @param aclId         the id
     * @param groupId       the assigned grou
     * @param read          the read permission
     * @param edit          the edit permission
     * @param relate        the relate permission
     * @param delete        the delete permission
     * @param export        the export permission
     * @param create        the create permission
     * @param category      the assignment category
     * @param lifeCycleInfo the lifecycle information
     */
    public ACLAssignment(long aclId, long groupId, boolean read, boolean edit, boolean relate, boolean delete,
                         boolean export, boolean create, ACL.Category category, LifeCycleInfo lifeCycleInfo) {
        this.aclId = aclId;
        this.groupId = groupId;
        this.mayRead = read;
        this.mayEdit = edit;
        this.mayRelate = relate;
        this.mayDelete = delete;
        this.mayExport = export;
        this.category = category;
        this.mayCreate = create;
        this.lifeCycleInfo = lifeCycleInfo;
    }

    /**
     * Constructor, all permissions flags are set to false
     *
     * @param aclId         the id
     * @param groupId       the assigned grou
     * @param category      the assignment category
     * @param lifeCycleInfo the lifecycle information
     */
    protected ACLAssignment(long aclId, long groupId, ACL.Category category, LifeCycleInfo lifeCycleInfo) {
        this.aclId = aclId;
        this.groupId = groupId;
        this.mayRead = false;
        this.mayEdit = false;
        this.mayRelate = false;
        this.mayDelete = false;
        this.mayExport = false;
        this.category = category;
        this.mayCreate = false;
        this.lifeCycleInfo = lifeCycleInfo;
    }


    /**
     * Returns the unqiue ACL id this assignment belongs to.
     *
     * @return the unqiue ACL id this assignment belongs to.
     */
    public long getAclId() {
        return this.aclId;
    }

    /**
     * Return true if the ACLAssignmentImpl grants read permission.
     *
     * @return true if the ACLAssignmentImpl grants read permission.
     */
    public boolean getMayRead() {
        return this.mayRead;
    }

    /**
     * Return true if the ACLAssignmentImpl grants edit permission.
     *
     * @return true if the ACLAssignmentImpl grants edit permission.
     */
    public boolean getMayEdit() {
        return this.mayEdit;
    }

    /**
     * Return true if the ACLAssignmentImpl grants relate permission.
     *
     * @return true if the ACLAssignmentImpl grants relate permission.
     */
    public boolean getMayRelate() {
        return this.mayRelate;
    }

    /**
     * Return true if the ACLAssignmentImpl grants unassign permission.
     *
     * @return true if the ACLAssignmentImpl grants unassign permission.
     */
    public boolean getMayDelete() {
        return this.mayDelete;
    }

    /**
     * Return true if the ACLAssignmentImpl grants export permission.
     *
     * @return true if the ACLAssignmentImpl grants export permission.
     */
    public boolean getMayExport() {
        return this.mayExport;
    }

    /**
     * Return true if the ACLAssignment grants create permission.
     *
     * @return true if the ACLAssignment grants create permission.
     */
    public boolean getMayCreate() {
        return !isOwnerGroupAssignment() && this.mayCreate;
    }

    /**
     * Returns the id of the group the acl is assigned to.
     *
     * @return the id of the group the acl is assigned to
     */
    public long getGroupId() {
        return this.groupId;
    }

    /**
     * Setter for the group id
     *
     * @param groupId group id
     */
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    /**
     * Returns the type of the ACL.
     *
     * @return the type of the ACL
     */
    public ACL.Category getACLCategory() {
        return this.category;
    }

    /**
     * Get lifecycle information
     *
     * @return lifecycle information
     */
    public LifeCycleInfo getLifeCycleInfo() {
        return lifeCycleInfo;
    }

    /**
     * Is this an assignment for the owner group?
     *
     * @return if this an assignment for the owner group?
     */
    public boolean isOwnerGroupAssignment() {
        return groupId == UserGroup.GROUP_OWNER;
    }

    /**
     * Returns a string representation.
     *
     * @return a string representation
     */
    @Override
    public String toString() {
        return this.getClass() + "@[acl=" + aclId + ",group=" + groupId + ",create=" + mayCreate + ",read=" + mayRead +
                ",edit=" + mayEdit + ",delete=" + mayDelete
                + ",relate=" + mayRelate + ",export=" + mayExport + "]";
    }

    public void setMayRead(boolean bMayRead) {
        this.mayRead = bMayRead;
    }

    public void setMayEdit(boolean bMayEdit) {
        this.mayEdit = bMayEdit;
    }

    public void setMayRelate(boolean bMayRelate) {
        this.mayRelate = bMayRelate;
    }

    public void setMayDelete(boolean bMayDelete) {
        this.mayDelete = bMayDelete;
    }

    public void setMayExport(boolean bMayExport) {
        this.mayExport = bMayExport;
    }

    public void setMayCreate(boolean bMayCreate) {
        this.mayCreate = bMayCreate;
    }

    /**
     * Check if the requested permission is granted
     *
     * @param permission the permission to check
     * @param ownerId    id of the owner
     * @param userId     id of the calling user
     * @return granted
     */
    public boolean getPermission(ACL.Permission permission, long ownerId, long userId) {
        if (isOwnerGroupAssignment() && (ownerId != userId || permission == ACL.Permission.CREATE))
            return false;
        switch (permission) {
            case CREATE:
                return mayCreate;
            case DELETE:
                return mayDelete;
            case EDIT:
                return mayEdit;
            case EXPORT:
                return mayExport;
            case RELATE:
                return mayRelate;
            case READ:
                return mayRead;
            default:
                return false;

        }
    }


}
