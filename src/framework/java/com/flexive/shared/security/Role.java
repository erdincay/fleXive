/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.shared.security;

import com.flexive.shared.ObjectWithColor;
import com.flexive.shared.SelectableObjectWithName;
import com.flexive.shared.SelectableObjectWithLabel;
import com.flexive.shared.value.FxString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User roles
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum Role implements SelectableObjectWithName, SelectableObjectWithLabel, ObjectWithColor, Serializable {


    /**
     * Global Supervisor - no restrictions at all
     */
    GlobalSupervisor((byte) 1, "Global Supervisor", "#0000FF"),

    /**
     * Mandator Supervisor - may do everything for "his" mandator
     */
    MandatorSupervisor((byte) 2, "Mandator Supervisor", "#0000AA"),

    /**
     * may CRUD ACLs for "his" mandator
     */
    ACLManagement((byte) 3, "ACL Management", "#00AA00"),

    /**
     * may CRUD users, groups and roles (may only add roles he is assigned himself,
     * may not alter assigned roles that he has not assigned himself) for "his" mandator
     */
    AccountManagement((byte) 4, "Account Management", "#00AA00"),

    /**
     * may see the user interface to edit selectlist items (role entitles to no CRUD rights!),
     * actual permissions are taken from the select lists createItemACL
     */
    SelectListEditor((byte) 5, "SelectList Editor", "#00AA00"),

    /**
     * may CRUD steps and workflows for "his" mandator
     */
    WorkflowManagement((byte) 6, "Workflow Management", "#00AA00"),

    /**
     * may CRUD types/relations/groups/properties/assignments/selectlists and assign scripts to structures for "his" mandator
     */
    StructureManagement((byte) 7, "Structure Management", "#00AA00"),

    /**
     * may CRUD scripts
     */
    ScriptManagement((byte) 8, "Script Management", "#00AA00"),

    /**
     * may execute scripts that can be run "standalone"
     */
    ScriptExecution((byte) 9, "Script Execution", "#00AA00"),

    /**
     * may login to the backend (does not imply any rights)
     */
    BackendAccess((byte) 10, "Backend Access", "#00AA00"),;

    private static final long serialVersionUID = -8561324370315868526L;

    private byte id = -1;
    private String desc = null;
    private String color = null;
    private FxString label = null;

    /**
     * Private constructor.
     *
     * @param id          the unique id
     * @param description the description
     * @param color       the color
     */
    private Role(byte id, String description, String color) {
        this.id = id;
        this.desc = description;
        this.color = color;
        this.label = new FxString(false, desc);
    }


    /**
     * Check if the given role id is undefined
     *
     * @param roleId role id to check
     * @return if undefined
     */
    public static boolean isUndefined(long roleId) {
        return roleId <= 0 || getById(roleId) == null;
    }

    /**
     * Get all roles as a List
     *
     * @return roles as List
     */
    public static List<Role> getList() {
        List<Role> result = new ArrayList<Role>(values().length);
        result.addAll(Arrays.asList(values()));
        return result;
    }

    /**
     * Returns a role identified by its unique id.
     *
     * @param roleId the id of the role to fetch
     * @return the role, or null if the id is not defined
     */
    public static Role getById(long roleId) {
        for (Role role : values()) {
            if (role.getId() == roleId) return role;
        }
        return null;
    }

    /**
     * Return the description of the role.
     *
     * @return the description of the role
     */
    public String getName() {
        return this.desc;
    }

    /**
     * Returns the unique id of the role.
     *
     * @return the unique id of the role
     */
    public long getId() {
        return this.id;
    }

    /**
     * Returns the color of the role.
     *
     * @return the color of the role.
     */
    public String getColor() {
        return this.color;
    }

    /**
     * Returns a string representation of the role.
     *
     * @return a string representation of the role
     */
    @Override
    public String toString() {
        return Role.class + "@[id=" + this.id + ",desc=" + this.getName() + "]";
    }

    /**
     * {@inheritDoc}
     */
    public FxString getLabel() {
        return this.label;
    }

    /**
     * Convert a Role array to an id array
     *
     * @param roles roles
     * @return id's
     */
    public static long[] toIdArray(Role[] roles) {
        if (roles == null || roles.length == 0)
            return new long[0];
        long[] res = new long[roles.length];
        for (int i = 0; i < roles.length; i++)
            res[i] = roles[i].getId();
        return res;
    }
}
