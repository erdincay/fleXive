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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.*;
import com.flexive.shared.workflow.Route;
import com.flexive.shared.workflow.Step;

import javax.ejb.Remote;

/**
 * Workflow route engine interface.
 * 
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface RouteEngine {
	
	/**
	 * Loads all possible to-steps.
	 *
	 * @param fromStep the from step
	 * @return all possible to-steps
	 * @throws FxApplicationException TODO
	 * @throws FxLoadException if the function failed to load the steps
	 * @throws FxNotFoundException if the from step does not exist
	 */
	Step[] getTargets(long fromStep) throws FxApplicationException;
	
	/**
	 * Returns all defined routes within a workflow.
	 *
	 * @param workflowId the unique workflow id
	 * @return all defined routes within a workflow
	 * @throws FxApplicationException TODO
	 * @throws FxLoadException if the function failed to read the routes
	 */
	Route[] loadRoutes(long workflowId) throws FxApplicationException;

    /**
	 * Creates a new route.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.
	 * @param fromStepId the from step
	 * @param toStepId the to step
	 * @param groupId the authorised group
	 * @return the unique id of the new route
	 * @throws FxApplicationException TODO
	 * @throws FxInvalidParameterException if invalid parameters were enountered
	 * @throws FxNotFoundException if the from/to step or group does not exist
	 * @throws FxCreateException if the creation failed (eg. from and to step are in a different workflow or ident)
	 * @throws FxNoAccessException if the caller may not acces the group
	 * @throws FxEntryExistsException if the route does already exist
	 */
	long create(long fromStepId, long toStepId, long groupId) throws FxApplicationException;
	
	/**
	 * Removes a route defined by its unique id.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.<br>
	 * @param routeId the unique route id
	 * @throws FxApplicationException TODO
	 * @throws FxRemoveException if the remove failed
	 */
	void remove(long routeId) throws FxApplicationException;
	
}
