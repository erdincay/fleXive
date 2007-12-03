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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.*;
import com.flexive.shared.workflow.Workflow;

import javax.ejb.Remote;

/**
 * The workflow class represets a workflow that defines steps that are connected
 * through routes.
 */
@Remote
public interface WorkflowEngine {

	/**
	 * Remove a workflow defined by its unique workflowId.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.
	 * @param workflowId the unique workflowId of the workflow to removed
	 * @throws FxApplicationException TODO
	 * @throws FxNotFoundException if the workflow defined by its workflowId does not exist
	 * @throws FxRemoveException   if the remove failed
	 * @throws FxNoAccessException if the caller lacks the permissions to remove a workflow
	 */
	void remove(long workflowId) throws FxApplicationException;

	/**
	 * Modifies a workflow.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.
	 * @param workflow TODO
	 * @throws FxApplicationException TODO
	 * @throws FxInvalidParameterException if a invalid parameter was encountered
	 * @throws FxNotFoundException         if the workflow does not exist
	 * @throws FxUpdateException           if the update failed
	 * @throws FxNoAccessException         if the caller lacks the permissions to update the workflow
	 * @throws FxEntryExistsException      if a workflow with the new name already exists
	 * @throws FxCreateException 		   if a step could not be created
	 * @throws FxRemoveException           if a removed workflow route could not be removed
	 * @throws FxEntryInUseException       if a removed workflow step is still in use
	 */
	void update(Workflow workflow) throws FxApplicationException;

	/**
	 * Creates a new workflow.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.
	 *
	 * @param workflow	the workflow to be created (the ID attribute is ignored)
	 * @return  the ID of the created workflow
	 * @throws FxApplicationException TODO
	 * @throws FxCreateException      if the creation failed
	 * @throws FxEntryExistsException if a workflow with the given name already exists
	 * @throws FxNoAccessException    if the caller lacks the permissions to remove the workflow
	 * @throws FxInvalidParameterException If a parameter is invalid
	 */
	long create(Workflow workflow) throws FxApplicationException;

}
