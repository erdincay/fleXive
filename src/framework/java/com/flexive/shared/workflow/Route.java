/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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

import com.flexive.shared.SelectableObject;

import java.io.Serializable;

/**
 * The route class represents a route connecing two steps within a workflow.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Route implements SelectableObject, Serializable {
    private static final long serialVersionUID = -1555295747455481165L;

	protected long id = -1;
    protected long fromStepId = -1;
    protected long toStepId   = -1;
    protected long  groupId  = -1;

    /**
     * Creates a new route between two steps of a workflow.
     * 
     * @param id        route ID
     * @param groupId   group ID
     * @param fromStepId source step ID
     * @param toStepId  destination step ID
     */
    public Route(long id, long groupId, long fromStepId, long toStepId) {
        this.id = id;
        this.groupId = groupId;
        this.fromStepId = fromStepId;
        this.toStepId = toStepId;
    }
    
    /**
     * Copy constructor
     * @param route source route object
     */
    public Route(Route route) {
        this(route.getId(), route.getGroupId(), route.getFromStepId(), route.getToStepId());
    }
    
    /**
     * Protected default constructor
     */
    protected Route() {
    }
    
    /**
     * Checks if this route is equal to another. The route ID is ignored,
     * since equality checks are supposed to occur inside the same workflow.
     * 
     * @param o the route this object should be compared to
     * @return  true if the route is equal to o
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Route) {
            Route route = (Route) o;
            return route.getFromStepId() == fromStepId 
                && route.getToStepId() == toStepId && route.getGroupId() == groupId; 
        } else {
            return false;
        }
    }
    
    /** {@inheritDoc} */
	@Override
    public int hashCode() {
        return (int) (fromStepId + toStepId + groupId);
    }

    /**
	 * Returns the unique id of the route.
	 *
	 * @return the id of the route
	 */
	@Override
    public long getId() {
		return id;
	}
	
	/**
	 * Returns the source step of the route.
	 *
	 * @return the source step of the route
	 */
	public long getFromStepId() {
		return fromStepId;
	}
	
	/**
	 * Returns the destination step of the route.
	 *
	 * @return the destination step of the route
	 */
	public long getToStepId() {
		return toStepId;
	}
	
	/**
	 * Returns the group that the route belongs to.
	 *
	 * @return the group that the route belongs to.
	 */
	public long getGroupId() {
		return groupId;
	}


}
