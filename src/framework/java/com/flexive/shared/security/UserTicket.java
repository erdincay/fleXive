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

import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxPK;

import java.io.Serializable;

/**
 * The UserTicket caches informations about a user.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public interface UserTicket extends Serializable {

    /**
     * Getter for the (not unique) user name
     *
     * @return the (not unique) user name
     */
    String getUserName();

    /**
     * Returns true if the ticket is a guest user.
     *
     * @return true if the ticket is a guest user
     */
    boolean isGuest();

    /**
     * Returns the unique login name of the user.
     *
     * @return the unique login name of the user.
     */
    String getLoginName();

    /**
     * Getter for the user id
     *
     * @return the user id
     */
    long getUserId();

    /**
     * Get the primary key for the associated contact data
     *
     * @return primary key for the associated contact data
     */
    FxPK getContactData();


    /**
     * Returns true if more than one session with the tickets user can be logged in at the same time.
     *
     * @return true if more than one session with the tickets user can be logged in at the same time.
     */
    boolean isMultiLogin();

    /**
     * Return true if the user is a global supervisor.
     *
     * @return true if the user is a global supervisor
     */
    boolean isGlobalSupervisor();

    /**
     * Return true if the user is a supervisor within its domain
     *
     * @return true if the user is a supervisor within its domain
     */
    boolean isMandatorSupervisor();

    /**
     * Returns the id of the mandator the user belongs to.
     *
     * @return the id of the mandator the user belongs to
     */
    long getMandatorId();

    /**
     * Returns true if the user is a member of the given group.
     *
     * @param group the group to check for
     * @return true if the user is a member of the given group
     */
    boolean isInGroup(long group);

    /**
     * Returns true if the user is assigned to the given role.
     *
     * @param role the role to check for
     * @return true if the user is assigned to the given role
     */
    boolean isInRole(Role role);

    /**
     * Returns true if the user is a member of all the given group.
     * <p/>
     * Returns true if the groups parameter is null or empty
     *
     * @param groups the groups to check for
     * @return true if the user is a member of all the given group
     */
    boolean isInGroups(int groups[]);

    /**
     * Returns all groups the user is in.
     * <p/>
     * Every user is at least in the Group EVERYONE
     *
     * @return all groups the user is in.
     */
    long[] getGroups();

    /**
     * Returns true if the user is a member of at least one of the given groups.
     * <p/>
     * Returns true if the groups parameter is null or empty
     *
     * @param groups the groups to check for
     * @return true if the user is a member of all the given group
     */
    boolean isInAtLeastOneGroup(long[] groups);

    /**
     * Returns the application id the ticket belongs to.
     *
     * @return the application id the ticket belongs to
     */
    String getApplicationId();

    /**
     * Returns all ACLAssignments for the user.
     * <p/>
     * The user inherits all ACLAssignments from his groups.
     *
     * @return all ACLAssignments for the user, may be a empty list but is never null
     */
    ACLAssignment[] getACLAssignments();

    /**
     * Returns true if the user is assigned to a given ACL.
     *
     * @param aclId the acl to check for
     * @return true if the user is assigned to the ACL, or false.
     */
    boolean isAssignedToACL(long aclId);


    /**
     * Returns the time that this ticket was created at.
     *
     * @return the time that the ticket was created at
     */
    long getCreationTime();


    /**
     * Returns true if the user may read objects using the given ACL.
     *
     * @param aclId   the acl
     * @param ownerId id of the owner
     * @return true if the user may read objects using the given ACL
     */
    boolean mayReadACL(long aclId, long ownerId);

    /**
     * Returns true if the user may edit objects using the given ACL.
     *
     * @param aclId   the acl
     * @param ownerId id of the owner
     * @return true if the user may edit objects using the given ACL
     */
    boolean mayEditACL(long aclId, long ownerId);

    /**
     * Returns true if the user may export objects using the given ACL.
     *
     * @param aclId   the acl
     * @param ownerId id of the owner
     * @return true if the user may export objects using the given ACL
     */
    boolean mayExportACL(long aclId, long ownerId);

    /**
     * Returns true if the user may relate objects to a object using the given ACL.
     *
     * @param aclId   the acl
     * @param ownerId id of the owner
     * @return true if the user may relate objects using the given ACL
     */
    boolean mayRelateACL(long aclId, long ownerId);

    /**
     * Returns true if the user may create objects using the given ACL.
     *
     * @param aclId   the acl
     * @param ownerId id of the owner
     * @return true if the user may create objects using the given ACL
     */
    boolean mayCreateACL(long aclId, long ownerId);

    /**
     * Returns true if the user may edit objects using the given ACL.
     *
     * @param aclId   the acl
     * @param ownerId id of the owner
     * @return true if the user may edit objects using the given ACL
     */
    boolean mayDeleteACL(long aclId, long ownerId);

    /**
     * Returns all ACLAssignments for the user matching the filter parameters.
     * <p/>
     * A ACL may accure multiple times within the result, since the ACLAssignment object contains
     * the groupId that the ACL is assigned to. The user itself gets the ACLAssignments from the groups he
     * belongs to.
     *
     * @param category all assignments if null, or only those matching the given category
     * @param ownerId  id of the owner
     * @param perms    ACL.PERM
     * @return all ACLAssignments for the user matching the filter parameters, may be a empty array but is never null
     */
    ACLAssignment[] getACLAssignments(ACL.Category category, long ownerId, ACL.Permission... perms);

    /**
     * Returns the id of all ACLs for the user matching the filter parameters.
     * <p/>
     * The ACL ids are distinct within the result.<br>
     * The permissions the user gets from all groups he belongs to are taken into account.
     *
     * @param ownerId  id of the owner
     * @param category ACL.CATEGORY
     * @param perms    ACL.PERM
     * @return all ACL ids for the user matching the filter parameters, may be an empty array but is never null
     */
    Long[] getACLsId(long ownerId, ACL.Category category, ACL.Permission... perms);

    /**
     * Returns the id of all ACLs for the user matching the filter parameters as comma separated list.
     * <p/>
     * The ACL ids are distinct within the result.<br>
     * The permissions the user gets from all groups he belongs to are taken into account.
     *
     * @param ownerId  id of the owner
     * @param category all assignments if null, or only those matching the given category
     * @param perms    ACL.PERM
     * @return all ACL ids for the user matching the filter parameters  as comma separated list.
     */
    String getACLsCSV(long ownerId, ACL.Category category, ACL.Permission... perms);

    /**
     * Returns the id of all ACLs for the user matching the filter parameters.
     * <p/>
     * The ACL ids are distinct within the result.<br>
     * The permissions the user gets from all groups he belongs to are taken into account.
     *
     * @param owner    id of the owner
     * @param category ACL.CATEGORY
     * @param perms    ACL.PERM
     * @return all ACL ids for the user matching the filter parameters, may be an empty array but is never null
     */
    ACL[] getACLs(long owner, ACL.Category category, ACL.Permission... perms);

    /**
     * Get the default language of this user
     *
     * @return default language of this user
     */
    FxLanguage getLanguage();

    /**
     * Returns true if this is a ticket for the webdav part of the application.
     *
     * @return true if this is a ticket for the webdav part of the application.
     */
    boolean isWebDav();

    /**
     * Clones a ticket with global supervisor permissions.
     *
     * @return the cloned ticket with the given session id
     */
    UserTicket cloneAsGlobalSupervisor();

    /**
     * Get the number of failed login attempts until the user sucessfully logged in
     *
     * @return number of failed login attempts until the user sucessfully logged in
     */
    public long getFailedLoginAttempts();

    /**
     * Get the source used to authenticate the user
     *
     * @return source used to authenticate the user
     */
    public AuthenticationSource getAuthenticationSource();

    /**
     * Override the users language.
     * Please note that overriding a language will only work for the
     * current session. To change the language permantently edit the users account data!
     *
     * @param language language to override
     */
    public void setLanguage(FxLanguage language);
}


