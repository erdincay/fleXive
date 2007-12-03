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
package com.flexive.shared.security;

import com.flexive.shared.FxArrayUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Arrays;

/**
 * A class for transporting and processing a list of roles.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class RoleList implements Serializable {
    private static final long serialVersionUID = 9101501258624365441L;

    private Role[] list = null;


    /**
     * Constructor
     *
     * @param roles the roles in the list
     */
    public RoleList(Role[] roles) {
        this.list = FxArrayUtils.clone(roles);
    }

    /**
     * Constructor
     *
     * @param roles the roles in the list
     */
    public RoleList(byte[] roles) {
        if (roles == null) roles = new byte[0];
        list = new Role[roles.length];
        for (int i = 0; i < roles.length; i++) {
            list[i] = Role.getById(roles[i]);
        }
    }

    /**
     * Constructor for a empts role list
     */
    public RoleList() {
        list = new Role[0];
    }

    /**
     * Constructor
     *
     * @param roles the roles in the list, as Integer id objects in a array list
     */
    public RoleList(ArrayList roles) {
        if (roles == null) roles = new ArrayList(0);
        list = new Role[roles.size()];
        for (int i = 0; i < roles.size(); i++) {
            Integer id = (Integer) roles.get(i);
            list[i] = Role.getById(id);
        }
    }


    /**
     * Constructs a role list from a string with the comma separated role id's.
     *
     * @param roles a list of comma separated role id's.
     */
    public RoleList(String roles) {
        // Handle empty strings
        if (roles == null || roles.length() == 0) {
            list = new Role[0];
            return;
        }
        // Build the list
        List<Role> tmp1 = new java.util.ArrayList<Role>(20);
        StringTokenizer st = new java.util.StringTokenizer(roles, ",", false);
        while (st.hasMoreTokens()) {
            String value = st.nextToken();
            Role aRole = Role.getById(Integer.valueOf(value));
            tmp1.add(aRole);
        }
        list = tmp1.toArray(new Role[tmp1.size()]);
    }

    /**
     * Returns the amount of roles in the list.
     *
     * @return the amount of roles in the list
     */
    public int size() {
        return list.length;
    }

    /**
     * Returns all roles in the list.
     *
     * @return all roles in the list
     */
    public Role[] getRoles() {
        return FxArrayUtils.clone(list);
    }

    /**
     * Returns all roles as array list.
     *
     * @return the list
     */
    public ArrayList<Role> getList() {
        if (list==null || list.length==0) {
            return  new ArrayList<Role>(0);
        }
        ArrayList<Role> result = new ArrayList<Role>(list.length);
        result.addAll(Arrays.asList(list));
        return result;
    }

    /**
     * Returns the role at the given position.
     *
     * @param pos the position
     * @return the role at the given position
     */
    public Role get(int pos) {
        return this.list[pos];
    }

    /**
     * Adds a role to the end of the list, but only if its not already part of it
     *
     * @param roleId the role to add
     */
    public void add(byte roleId) {
        // Check if the role is already part of the list
        for (Role aList : list)
            if (aList.getId() == roleId)
                return;
        // Add the role
        Role[] tmp = new Role[list.length + 1];
        System.arraycopy(list, 0, tmp, 0, list.length);
        tmp[tmp.length - 1] = Role.getById(roleId);
        list = tmp;
    }


    /**
     * Returns a array holding the id's of all roles in the list.
     *
     * @return a array holding the id's of all roles in the list
     */
    public long[] toIdArray() {
        long idArr[] = new long[list.length];
        for (int i = 0; i < list.length; i++)
            idArr[i] = list[i].getId();
        return idArr;
    }

    /**
     * Returns a array holding the names of all roles in the list.
     *
     * @return a array holding the names of all roles in the list
     */
    public String[] toNameArray() {
        if (list == null) return new String[0];
        String idArr[] = new String[list.length];
        for (int i = 0; i < list.length; i++)
            idArr[i] = list[i].getName();
        return idArr;
    }

    /**
     * Removes a role from the list.
     *
     * @param role the roles to remove from the list
     */
    public void remove(Role role) {
        remove(role.getId());
    }

    /**
     * Removes groups from the list.
     *
     * @param role the groups to remove from the list
     */
    public void remove(Role role[]) {
        for (Role aRole : role)
            remove(aRole.getId());
    }


    /**
     * Removes role identified by the unique id from the list.
     *
     * @param roleId the role to remove from the list
     */
    public void remove(long roleId[]) {
        for (long aRoleId : roleId)
            remove(aRoleId);
    }


    /**
     * Removes a role identified by its unique id from the list.
     *
     * @param roleId the role to remove from the list
     */
    public void remove(long roleId) {
        if (list != null) {
            int elementFound = 0;
            while (elementFound != -1) {
                // Look for the elemenet
                elementFound = -1;
                for (int i = 0; i < list.length; i++) {
                    if (list[i].getId() == roleId) {
                        elementFound = i;
                        break;
                    }
                }
                // Delete the element
                if (elementFound != -1) {
                    Role tmp[] = new Role[list.length - 1];
                    int pos = 0;
                    for (int i = 0; i < list.length; i++) {
                        if (i != elementFound) tmp[pos++] = list[i];
                    }
                    list = tmp;
                }
            }
        }
    }

    /**
     * Returns a string representation of the roles.
     *
     * @return a string representation of the roles
     */
    @Override
    public String toString() {
        return RoleList.class + "@[" + toNameString() + "]";
    }

    /**
     * Returns a comma seperated list of the role names.
     *
     * @return a comma seperated list of the role names.
     */
    public String toNameString() {
        StringBuilder result = new StringBuilder(50);
        for (Role entry : list) {
            if (result.length() > 0) result.append(',');
            result.append(entry.getName());
        }
        return result.toString();
    }

}
