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

import com.flexive.shared.FxArrayUtils;

import java.io.Serializable;

/**
 * A list of accounts
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class AccountList implements Serializable {
    private static final long serialVersionUID = 4030589087083042264L;

    private Account[] list = null;

    /**
     * Constructor
     *
     * @param users the users in the list
     */
    public AccountList(Account[] users) {
        if (users == null) users = new Account[0];
        this.list = FxArrayUtils.clone(users);
    }

    /**
     * Returns the amount of users in the list.
     *
     * @return the amount of users in the list
     */
    public int size() {
        return list.length;
    }

    /**
     * Returns all users in the list.
     *
     * @return all users in the list
     */
    public Account[] getUsers() {
        return FxArrayUtils.clone(list);
    }

    /**
     * Returns the user at the given position.
     *
     * @param pos the position
     * @return the user at the given position
     */
    public Account get(int pos) {
        return this.list[pos];
    }

    /**
     * Returns a array holding the id's of all users the list.
     *
     * @return a array holding the id's of all users in the list
     */
    public long[] toIdArray() {
        long idArr[] = new long[list.length];
        for (int i = 0; i < list.length; i++) idArr[i] = list[i].getId();
        return idArr;
    }

    /**
     * Returns a array holding the names of all users in the list.
     *
     * @return a array holding the names of all users in the list
     */
    public String[] toNameArray() {
        if (list == null) return new String[0];
        String idArr[] = new String[list.length];
        for (int i = 0; i < list.length; i++) idArr[i] = list[i].getName();
        return idArr;
    }

    /**
     * Removes a user from the list.
     *
     * @param user the user to remove from the list
     */
    public void remove(Account user) {
        remove(user.getId());
    }

    /**
     * Removes users from the list.
     *
     * @param user the users to remove from the list
     */
    public void remove(Account user[]) {
        for (Account anUser : user)
            remove(anUser.getId());
    }


    /**
     * Removes groups identified by the unique id from the list.
     *
     * @param userId the users to remove from the list
     */
    public void remove(long userId[]) {
        for (long anUserId : userId)
            remove(anUserId);
    }


    /**
     * Removes a user identified by its unique id from the list.
     *
     * @param userId the user to remove from the list
     */
    public void remove(long userId) {
        if (list != null) {
            int elementFound = 0;
            while (elementFound != -1) {
                // Look for the elemenet
                elementFound = -1;
                for (int i = 0; i < list.length; i++) {
                    if (list[i].getId() == userId) {
                        elementFound = i;
                        break;
                    }
                }
                // Delete the element
                if (elementFound != -1) {
                    Account tmp[] = new Account[list.length - 1];
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
     * Returns a string representation of the users.
     *
     * @return a string representation of the users
     */
    @Override
    public String toString() {
        return AccountList.class + "@[" + toNameString() + "]";
    }

    /**
     * Returns a comma seperated list of the user names.
     *
     * @return a comma seperated list of the user names.
     */
    public String toNameString() {
        String result = "";
        for (Account entry : list) {
            if (result.length() > 0) result += ",";
            result += entry.getName();
        }
        return result;
    }

}

