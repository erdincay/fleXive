/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLAssignment;
import com.flexive.shared.value.FxString;

import javax.ejb.Remote;
import java.util.ArrayList;
import java.util.List;

/**
 * ACL engine interface
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface ACLEngine {

    /**
     * Creates a new ACL for a specific mandator.
     * <p/>
     * The caller needs to be in ACLManagement, and may only create ACLs for the
     * mandator he belongs to.<br>
     * GROUP_GLOBAL_SUPERVISOR may create ACLs for all mandators.
     *
     * @param name        the unique name for the new ACL
     * @param label       display label
     * @param mandatorId  the mandator the ACL belongs to
     * @param color       the color of the acl as 6 digit RGB value, for example FF0000 for pure red
     * @param description a description for the ACL
     * @param category    the category of the ACL
     * @return		      id of the newly created ACL
     * @throws FxApplicationException creation failed, acl with the given name exists, calling user lacks
     *      permissions, parameter (name,mandator,color,category) was invalid, mandator does not exist
     */
    long create(String name, FxString label, long mandatorId, String color, String description, ACL.Category category)
            throws FxApplicationException;
    /**
     * Remove an existing ACL identified by its unique id.
     * <p/>
     * A ACL may only be removed if it is not used by any object within the system.
     * The calling user needs to be in ACLManagement, and may only unassign ACLs belonging
     * to his mandator.<br>
     * GROUP_GLOBAL_SUPERVISOR may unassign ACLs of mandators.
     *
     * @param aclId  the id of the ACL to remove
     * @throws FxApplicationException when the function failed to unassign the ACL,
     *      when a ACL with the given id does not exist,
     *      when the function failed to unassign the ACL
     */
    void remove(long aclId) throws FxApplicationException;

    /**
     * Updates a existing ACL.
     * <p/>
     * The calling user needs to be in ACLManagement, and may only update ACLs belonging to his
     * mandator.<br>
     * GROUP_GLOBAL_SUPERVISOR may update ACLs of all mandators.
     *
     * @param aclId       The unique id of the acl that should be updated
     * @param name        The new unqiue name of the ACL, or null if the old name should be kept
     * @param label       display label
     * @param color       The new color of the ACL, or null if the old color should be kept
     * @param description The new description of the ACL, or null if the old description should be kept
     * @param assignments ACL assignments
     * @throws FxApplicationException update failed, acl does not exist, user lacks permissions, parameter is
     *      invalid, acl with the given name exists
     */
    void update(long aclId, String name, FxString label, String color, String description,
                List<ACLAssignment> assignments)
            throws FxApplicationException;

    /**
     * Loads a ACL definied by its unique id.
     * <p/>
     * The caller may only load ACLs belonging to his mandator, or ACLs that the caller is assigned to.<br>
     * GROUP_GLOBAL_SUPERVISOR may load all ACLs.
     *
     * @param id     the unique id of the ACL that should be loaded
     * @return the ACL
     * @throws FxApplicationException load failed, acl does no exist, calling user may not access the ACL
     */
    ACL load(long id) throws FxApplicationException;


    /**
     * Loads a ACL definied by its unique id.
     * <p/>
     * If ignoreSecurity is true the following permissison checks are performed:<br>
     * The caller may only load ACLs belonging to his mandator.<br>
     * GROUP_GLOBAL_SUPERVISOR may load all ACLs.
     *
     * @param id     the unique id of the ACL that should be loaded
     * @param ignoreSecurity security checks are skipped if set to true
     * @return the ACL
     * @throws FxApplicationException load failed, acl doesnt exist, calling user may not access the ACL
     */
    ACL load(long id, boolean ignoreSecurity) throws FxApplicationException;


    /**
     * Defines a ACL assignment between a group and a ACL.
     *
     * If all permissions are set to false no assignments is created, and any old assignment is removed.<br>
     * Any existing assignment between this group and the ACL is overwritten.<br>
     * The caller must be in ACLManagement and may only assign group and acls belonging zo his mandator.<br>
     * GROUP_EVERYONE and PRIVATE my be assigned regardless of their mandator.<br>
     * GLOBAL_SUPERVISOR may assign acls and groups of any mandator.
     *
     * @param aclId the acl
     * @param groupId the group that should be assigned to the acl
     * @param mayRead the read permission for the group/acl combination
     * @param mayEdit the edit permission for the group/acl combination
     * @param mayRelate the relate permission for the group/acl combination
     * @param mayRemove the unassign permission for the group/acl combination
     * @param mayExport the export permission for the group/acl combination
     * @param mayCreate the create permission for the group/acl combination
     * @throws FxApplicationException when the creation failed, when the calling user lacks the permission to create ACL
     *      assignments,when the group or ACL does not exist
     */
    void assign(long aclId, long groupId, boolean mayRead, boolean mayEdit,
                boolean mayRelate, boolean mayRemove, boolean mayExport, boolean mayCreate) throws
            FxApplicationException;

    /**
     * Defines an ACL assignment between a group and an ACL.
     * This is a shortcut for <code>assign(long, long, boolean, boolean, boolean, boolean, boolean, boolean)</code>
     *
     * @param aclId       the acl
     * @param groupId     the group that should be assigned to the acl
     * @param permissions list of permissions to set (NOT_.. permissions are ignored as default is <code>false</code>)
     * @throws FxApplicationException when the creation failed, when the calling user lacks the permission to create ACL
     *                                assignments,when the group or ACL does not exist
     * @see #assign(long,long,boolean,boolean,boolean,boolean,boolean,boolean)
     */
    void assign(long aclId, long groupId, ACL.Permission... permissions) throws FxApplicationException;

    /**
     * Loads all ACL assignments of a group.
     *
     * The caller may only load ACLAssingments belonging to a group of his mandator.<br>
     * GLOBAL_SUPERVISOR may load the ACLAssignments of all groups.
     *
     * @param groupId the group to load the ACL assignment for
     * @return the ACL assignments of the group
     * @throws FxApplicationException not found, load failed, caller may not access the given group
     */
    List<ACLAssignment> loadGroupAssignments(long groupId) throws FxApplicationException;

    /**
     * Loads all ACL assignments of a ACL.
     *
     * The caller may only load ACLAssingments belonging to a ACL of his mandator.<br>
     * GLOBAL_SUPERVISOR may load the ACLAssignments of all ACL.
     *
     * @param aclId the acl to load the assignment for
     * @return the ACL assignments of the group
     * @throws FxApplicationException not found, load failed, no access
     */
    List<ACLAssignment> loadAssignments(long aclId) throws FxApplicationException;

    /**
     * Removes an ACLAssignment defined by its groupId and aclId.
     *
     * Only callers in ACLManagement may unassign ACLAssignments of groups and acl belonging to his mandator.<br>
     * GROUP_EVERYONE and PRIVATE my be assigned regardless of their mandator.<br>
     * GLOBAL_SUPERVISOR may unassign every ACLAssignment.
     * @param aclId a acl id
     * @param groupId a group id
     * @throws FxApplicationException when the unassign failed,
     *      when a assignment with the groupId and aclId combination does not exist,
     *      when the calling user lacks the permission to manage ACLs
     */
    void unassign(long aclId, long groupId) throws FxApplicationException;

    /**
     * Loads all ACL assignments of a group or acl.
     *
     * The caller may only load ACL assingments belonging to a group or acl of his mandator.<br>
     * GLOBAL_SUPERVISOR may load the ACL assignments of all groups.
     *
     * @param aclId the acl to load the ACL assigments for, or null
     * @param groupId the group to load the ACL assignment for, or null
     * @return the ACL assignments of the group
     * @throws FxApplicationException when no data was found, if the user may not access the data, or when a
     *      unexpected error occured
     */
    List<ACLAssignment> loadAssignments(Long aclId,Long groupId) throws FxApplicationException;
}
