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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.*;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepPermission;

import javax.ejb.Remote;
import java.util.List;

/**
 * The StepEngine class represents a step instance within a workflow
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface StepEngine {
	/**
	 * Creates a step in a workflow.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.<br>
	 * No Exception is thrown if the step already exists within the workflow.<br>
	 * If a unique target step is needed and not defined it will be created using the provided aclId.
	 *
	 * @param step	the step to be created
	 * @return the unique id of the new step
	 * @throws FxApplicationException TODO
	 * @throws FxCreateException   if the creation of the step failed
	 * @throws FxNoAccessException if the caller lacks the permissions to create the step
	 */
	long createStep(Step step) throws FxApplicationException;


	/**
	 * Returns all steps that a user has some kind of access to, combined with the
	 * the permissions for the user on the step.
	 * <p />
	 * The function does not check id the specified user exists, it will return an empty result
	 * for non existing entries.<br>
	 * Only GLOBAL_SUPERVISOR may call this function for all users, other users may only query themselfs.
	 *
	 * @param userId the user to get the steps for
	 * @return a array holding all Steps with its ACLs for the user
	 * @throws FxApplicationException TODO
	 * @throws FxLoadException if the load failed
	 * @throws FxNoAccessException  if the calling user may not load the requested steps 
	 */
	List<StepPermission> loadAllStepsForUser(long userId) throws FxApplicationException;

	/**
	 * Removes all steps within a workflow.
	 * <p/>
	 * The caller has to be within ROLE_WORKFLOWMANAGEMENT.
	 *
	 * @param workflowId the workflow id
	 * @throws FxApplicationException TODO
	 * @throws FxNoAccessException if the caller lacks the permissions to remove the steps
	 * @throws FxNotFoundException if the workflow does not exist
	 * @throws FxRemoveException   if the remove failed
	 */
	void removeSteps(long workflowId) throws FxApplicationException;

	/**
	 * Removes a step defined by its unique id.
	 * <p/>
	 * The caller has to be within ROLE_WORKFLOWMANAGEMENT.<br>
	 * The function does not throw a Exception if the step does not exist.
	 *
	 * @param stepId the unique step id
	 * @throws FxApplicationException TODO
	 * @throws FxNoAccessException   if the caller lacks the permissions to remove the steps
	 * @throws FxRemoveException     if the remove failed
	 * @throws FxEntryInUseException if the step is needed by another step as unique target can connot be removed
	 */
	void removeStep(long stepId) throws FxApplicationException;

	/**
	 * Updates a step specified by its unique id.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.
	 *
	 * @param stepId the step to update
	 * @param aclId  the ACL of the step
	 * @throws FxApplicationException TODO
	 * @throws FxUpdateException   if the update of the step failed
	 * @throws FxNoAccessException if the caller lacks the permissions to create the step
	 * @throws FxNotFoundException if the step does not exist
	 */
	void updateStep(long stepId, long aclId) throws FxApplicationException;

}
