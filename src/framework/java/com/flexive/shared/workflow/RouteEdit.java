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
     * Sets the source step of the route.
     * @param fromStepId    the source step of the route.
     */
    public void setFromStepId(long fromStepId) {
        this.fromStepId = fromStepId;
    }

    /**
     * Sets the group ID of the route.
     * @param groupId   the group ID of the route.
     */
    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    /**
     * Sets the unique ID of the route.
     * @param id    the unique ID of the route.
     */
    public void setId(long id) {
        this.id = id;
    }

    /**
     * Sets the destination step ID of the route.
     * @param toStepId  the destination step ID of the route.
     */
    public void setToStepId(long toStepId) {
        this.toStepId = toStepId;
    }
}
