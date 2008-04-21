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
package com.flexive.shared.security;

import com.flexive.shared.FxArrayUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * A class for transporting and processing a list of groups.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class UserGroupList implements Serializable {
    private static final long serialVersionUID = -7462354889674331522L;

    private UserGroup[] list = null;

    /**
     * Constructor
     *
     * @param groups the groups in the list
     */
    public UserGroupList(UserGroup[] groups) {
        if (groups == null) groups = new UserGroup[0];
        this.list = FxArrayUtils.clone(groups);
    }

    /**
     * Constructor
     */
    public UserGroupList() {
        this.list = new UserGroup[0];
    }

    /**
     * Returns the amount of groups in the list.
     *
     * @return the amount of groups in the list
     */
    public int size() {
        return list.length;
    }

    /**
     * Returns all groups in the list.
     *
     * @return all groups in the list
     */
    public UserGroup[] getGroups() {
        return FxArrayUtils.clone(list);
    }

    /**
     * Returns the group at the given position.
     *
     * @param pos the position
     * @return the group at the given position
     */
    public UserGroup get(int pos) {
        return this.list[pos];
    }


    /**
     * Returns a array holding the id's of all groups in the list.
     *
     * @return a array holding the id's of all groups in the list
     */
    public long[] toLongArray() {
        long[] idArr = new long[list.length];
        for (int i = 0; i < list.length; i++) idArr[i] = list[i].getId();
        return idArr;
    }

    /**
     * Returns a array holding the names of all groups in the list.
     *
     * @return a array holding the names of all groups in the list
     */
    public String[] toNameArray() {
        if (list == null)
            return new String[0];
        String idArr[] = new String[list.length];
        for (int i = 0; i < list.length; i++)
            idArr[i] = list[i].getName();
        return idArr;
    }

    /**
     * Removes a group from the list.
     *
     * @param group the group to remove from the list
     */
    public void remove(UserGroup group) {
        remove(group.getId());
    }

    /**
     * Removes groups from the list.
     *
     * @param group the groups to remove from the list
     */
    public void remove(UserGroup[] group) {
        for (UserGroup aGroup : group)
            remove(aGroup.getId());
    }


    /**
     * Removes groups identified by the unique id from the list.
     *
     * @param groupId the groups to remove from the list
     */
    public void remove(long[] groupId) {
        for (long aGroupId : groupId)
            remove(aGroupId);
    }

    /**
     * Returns true if the group is contained in the group list.
     *
     * @param groupId the id of the group to look for
     * @return true if the group is contained in the group list.
     */
    public boolean contains(long groupId) {
        for (UserGroup aList : list)
            if (aList.getId() == groupId)
                return true;
        return false;
    }

    /**
     * Removes a group identified by its unique id from the list.
     *
     * @param groupId the group to remove from the list
     */
    public void remove(long groupId) {
        if (list != null) {
            int elementFound = 0;
            while (elementFound != -1) {
                // Look for the elemenet
                elementFound = -1;
                for (int i = 0; i < list.length; i++) {
                    if (list[i].getId() == groupId) {
                        elementFound = i;
                        break;
                    }
                }
                // Delete the element
                if (elementFound != -1) {
                    UserGroup tmp[] = new UserGroup[list.length - 1];
                    int pos = 0;
                    for (int i = 0; i < list.length; i++) {
                        if (i != elementFound) tmp[pos++] = list[i];
                    }
                    list = tmp;
                }
            }
        }
    }

    public List<UserGroup> getList() {
        ArrayList<UserGroup> result = new ArrayList<UserGroup>(list==null?0:list.length);
        if (list!=null) {
            result.addAll(Arrays.asList(list));
        }
        return result;
    }

    /**
     * Returns a string representation of the groups.
     *
     * @return a string representation of the groups
     */
    @Override
    public String toString() {
        return UserGroupList.class + "@[" + toNameString() + "]";
    }

    /**
     * Returns a comma seperated list of the group names.
     *
     * @return a comma seperated list of the group names.
     */
    public String toNameString() {
        String result = "";
        for (UserGroup aList : list) {
            if (result.length() > 0)
                result += ",";
            result += aList.getName();
        }
        return result;
    }

}

