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
package com.flexive.shared.workflow;

import java.io.Serializable;

/**
 * Editable route class.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see Route
 */
public class RouteEdit extends Route implements Serializable {
    private static final long serialVersionUID = -7869188253859784968L;

    /**
     * Constructor
     *
     * @param route source route
     */
    public RouteEdit(Route route) {
        super(route);
    }

    /**
     * Default constructor.
     */
    public RouteEdit() {
    }

    /**
     * Ctor to create a new route
     *
     * @param userGroupId user group
     * @param fromStepId  from step
     * @param toStepId    to step
     */
    public RouteEdit(long userGroupId, long fromStepId, long toStepId) {
        super(-1, userGroupId, fromStepId, toStepId);
    }


    /**
     * Sets the source step of the route.
     *
     * @param fromStepId the source step of the route.
     */
    public void setFromStepId(long fromStepId) {
        this.fromStepId = fromStepId;
    }

    /**
     * Sets the group ID of the route.
     *
     * @param groupId the group ID of the route.
     */
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    /**
     * Sets the unique ID of the route.
     *
     * @param id the unique ID of the route.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Sets the destination step ID of the route.
     *
     * @param toStepId the destination step ID of the route.
     */
    public void setToStepId(long toStepId) {
        this.toStepId = toStepId;
    }

    /**
     * Convenience method to create a new route
     *
     * @param userGroupId user group
     * @param fromStepId  from step
     * @param toStepId    to step
     * @return RouteEdit
     */
    public static RouteEdit createNew(long userGroupId, long fromStepId, long toStepId) {
        return new RouteEdit(userGroupId, fromStepId, toStepId);
    }
}
