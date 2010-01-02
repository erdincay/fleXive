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
package com.flexive.shared.security;

import com.flexive.shared.AbstractSelectableObjectWithName;
import com.flexive.shared.ObjectWithColor;

import java.io.Serializable;

/**
 * A group of users
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class UserGroup extends AbstractSelectableObjectWithName implements Serializable, ObjectWithColor {
    private static final long serialVersionUID = 8672395090967024861L;

    /**
     * Undefined group
     */
    public static final long GROUP_UNDEFINED = 0;

    /**
     * Group in which every user is in (including USER_GUEST).
     * This group may not be assigned to users.
     */
    public static final long GROUP_EVERYONE = 1;

    /**
     * Group for special ACL permissions.
     * This group may not be assigned to roles. A ACLAssignment using this
     * group defines that its permissions are granted to the CREATOR of an object,
     * not to the users within the GROUP_OWNER.
     */
    public static final long GROUP_OWNER = 2;

    /**
     * Dummy group (Id if a null value cannot be applied)
     */
    public static final long GROUP_NULL = 3;


    private long id = -1;
    private long mandator = -1;
    private long autoMandator = -1;
    private boolean system = false;
    private String name = null;
    private String color = null;

    /**
     * Ctor
     *
     * @param id       id
     * @param name     name
     * @param mandator mandator id
     * @param color    color
     */
    public UserGroup(long id, String name, long mandator, String color) {
        this.id = id;
        this.mandator = mandator;
        this.name = name;
        this.color = color;
    }

    /**
     * Ctor
     *
     * @param id           id
     * @param mandator     mandator id
     * @param autoMandator mandator id for auto generated system groups only, else <code>-1</code>
     * @param system       is this an auto generated system group?
     * @param name         name of the group
     * @param color        color
     */
    public UserGroup(long id, long mandator, long autoMandator, boolean system, String name, String color) {
        this.id = id;
        this.mandator = mandator;
        this.autoMandator = autoMandator;
        this.system = system;
        this.name = name;
        this.color = color;
    }

    /**
     * Getter for the group id
     *
     * @return group id
     */
    public long getId() {
        return id;
    }

    /**
     * Getter for the mandator id
     *
     * @return mandator id
     */
    public long getMandatorId() {
        return mandator;
    }

    /**
     * Get the id of the mandator this group is maintained for if it is a system group, else <code>-1</code>
     *
     * @return id of the mandator this group is maintained for if it is a system group, else <code>-1</code>
     */
    public long getAutoMandator() {
        return autoMandator;
    }

    /**
     * Is this an auto generated system group?
     *
     * @return auto generated system group?
     */
    public boolean isSystem() {
        return system;
    }

    /**
     * Getter for the name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the displayed color
     *
     * @return color
     */
    public String getColor() {
        return color;
    }

    /**
     * Setter for the name
     *
     * @param name group name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Setter for the color
     *
     * @param color color
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Returns true if the caller may see the group and its roles and assignments.
     *
     * @param ticket the caller
     * @return true if the caller may see the group its roles and assignments
     */
    public boolean mayAccessGroup(UserTicket ticket) {
        return ticket.isGlobalSupervisor() || id == GROUP_EVERYONE || id == GROUP_OWNER
                || mandator == ticket.getMandatorId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getName() + "(id:" + getId() + ", mandator: " + getMandatorId() + ")";
    }
}
