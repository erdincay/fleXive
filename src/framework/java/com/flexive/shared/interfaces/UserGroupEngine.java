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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.*;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserGroup;

import javax.ejb.Remote;
import java.util.List;

/**
 * Interface to the user group engine.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface UserGroupEngine {

    /**
     * Loads a group defined by its unique id.
     * <p/>
     * No permission checks are performed<br>
     *
     * @param groupId the unique id of the group to load
     * @return the requested user group
     * @throws FxApplicationException TODO
     * @throws com.flexive.shared.exceptions.FxNoAccessException
     *                                if the user may not access the group
     * @throws com.flexive.shared.exceptions.FxNotFoundException
     *                                if the group does not exist
     * @throws com.flexive.shared.exceptions.FxLoadException
     *                                if the load failed
     */
    UserGroup load(long groupId) throws FxApplicationException;

    /**
     * Load the user group that contains all users of a given mandator
     *
     * @param mandatorId mandator
     * @return UserGroup containing all users of the mandator
     * @throws FxApplicationException on errors
     */
    UserGroup loadMandatorGroup(long mandatorId) throws FxApplicationException;

    /**
     * Loads all groups belonging to a specific mandator, plus GROUP_EVERYONE and GROUP_OWNER.
     * <p/>
     * Specify -1 t load all groups within the system (all mandators).
     *
     * @param mandatorId the mandator id to load the groups for, or -1 to load all groups
     * @return all groups belonging to the mandator
     * @throws FxApplicationException TODO
     * @throws FxNoAccessException    If the calling user may not access the groups of the given mandator
     * @throws FxLoadException        if the load failed
     */
    List<UserGroup> loadAll(long mandatorId) throws FxApplicationException;

    /**
     * Creates a new group for a specific mandator.
     * <p/>
     * A user may only create groups for the mandator he belongs to, and only if he is in the role
     * ROLE_GROUP_MANAGEMENT.
     * Global supervisors are a exception and may create groups for all mandators.
     *
     * @param name       the unique name for the group
     * @param color      the color of the group as 7 digit string holding the RGB value, eg '#FF0000' for pure red,
     *                   or the name of a CSS class
     * @param mandatorId the mandator the group belongs to
     * @return the created group's ID
     * @throws FxApplicationException TODO
     * @throws FxNoAccessException    if the calling user lacks the permissions to create the group
     * @throws com.flexive.shared.exceptions.FxEntryExistsException
     *                                if a group with the given name exists
     * @throws com.flexive.shared.exceptions.FxInvalidParameterException
     *                                if a parameter was invalid (name,color,mandator)
     * @throws com.flexive.shared.exceptions.FxCreateException
     *                                if the create failed
     */
    long create(String name, String color, long mandatorId) throws FxApplicationException;

    /**
     * Updates a group defined by its unique id.
     * <p/>
     * Users may only update groups of the mandator they belong to if they are in role
     * ROLE_GROUP_MANAGEMENT.<br>
     * Global supervisors may update the groups of all mandators.
     *
     * @param groupId The group that should be updated
     * @param color   the color of the group as 6 digit RGB value (eg 'FF0000' for pure red), or null to keep the old value
     * @param name    the new name of the group, or null to keep the old value
     * @throws FxApplicationException      TODO
     * @throws FxNotFoundException         if the group to update does not exist
     * @throws FxNoAccessException         if the user lacks the permissions to update the group
     * @throws FxUpdateException           if the update failed
     * @throws FxEntryExistsException      if a group with the desired new name does already exist
     * @throws FxInvalidParameterException if a parameter was invalid (eg. the color format)
     */
    void update(long groupId, String name, String color) throws FxApplicationException;

    /**
     * Removes a group defined by its unique id.
     * <p/>
     * Users may only remove groups belonging to their mandator, and only if they are in
     * ROLE_GROUP_MANAGEMENT.<br>
     * Global supervisors may remove groups of all mandators.
     * The groups GROUP_EVERYONE and GROUP_OWNER may not be removed in any case.
     *
     * @param groupId the unqiue id of the group to remove
     * @throws FxApplicationException TODO
     * @throws FxNoAccessException    if the user lacks the permissions to remove the group
     * @throws FxNotFoundException    if the group does not exist
     * @throws FxRemoveException      if the remove failed
     */
    void remove(long groupId) throws FxApplicationException;


    /**
     * Sets the roles a group is in.
     * <p/>
     * To set roles the caller must be in role ROLE_ROLE_MANAGEMENT, and may only update groups belonging
     * to his mandator.<br>
     * GROUP_GLOBAL_SUPERVISOR may set the roles for groups of all users.<br>
     *
     * @param groupId the group to set the roles for
     * @param roles   the roles to set
     * @throws FxApplicationException TODO
     * @throws FxNoAccessException    if the calling user lacks the permissions to set the roles for the given group
     * @throws FxNotFoundException    if the group does not exist
     * @throws FxUpdateException      if setting the roles failed
     */
    void setRoles(long groupId, long[] roles) throws FxApplicationException;

    /**
     * Sets the roles a group is in.
     * <p/>
     * To set roles the caller must be in role ROLE_ROLE_MANAGEMENT, and may only update groups belonging
     * to his mandator.<br>
     * GROUP_GLOBAL_SUPERVISOR may set the roles for groups of all users.<br>
     *
     * @param groupId the group to set the roles for
     * @param roles   the roles to set
     * @throws FxApplicationException TODO
     * @throws FxNoAccessException    if the calling user lacks the permissions to set the roles for the given group
     * @throws FxNotFoundException    if the group does not exist
     * @throws FxUpdateException      if setting the roles failed
     */
    void setRoles(long groupId, List<Role> roles) throws FxApplicationException;

    /**
     * Gets all roles that are assigned to a group.
     * <p/>
     * The caller may see the role assignments for all groups belonging to his mandator.<br>
     * GLOBAL_SUPERVISOR may see the role assignments of all groups.
     *
     * @param groupId the group to get the assigned roles for
     * @return a list of the roles that ate assigned to the group
     * @throws FxApplicationException TODO
     * @throws FxNoAccessException    if the caller lacks the permissions to get the roles
     * @throws FxNotFoundException    if the group does not exist
     * @throws FxLoadException        if the function failed to load the data
     */
    List<Role> getRoles(long groupId) throws FxApplicationException;

    /**
     * Rebuild the mandator groups
     *
     * @throws FxApplicationException on errors
     */
    public void rebuildMandatorGroups() throws FxApplicationException;

}
