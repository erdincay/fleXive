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
import com.flexive.shared.workflow.StepDefinition;

import javax.ejb.Remote;

/**
 * Interface to the step definition engine.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface StepDefinitionEngine {

	/**
	 * Creates a new step definition.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.
	 *
	 * @param stepDefinition		the step definition to be created
	 * @return    The create step's ID 
	 * @throws FxApplicationException TODO
	 * @throws FxCreateException           if the creation of the step definition failed
	 * @throws FxNoAccessException         if the caller lacks the permission to manage workflows
	 * @throws FxInvalidParameterException if a parameter is invalid (empty name, invalid target ..)
	 * @throws FxEntryExistsException      If a step with the given name already exists
	 */
	long create(StepDefinition stepDefinition) throws FxApplicationException;

	/**
	 * Modifies a existing StepDefinition.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.<br>
	 * If the modification adds/modifies a unique target all workflows are checked if a step with this definition 
	 * exists, and if the new unqique target is defined. If the unqique target is missing in any workflow, 
     * it will be created using the ACL of the parent step.
	 *
	 * @param stepDefinition the stepdefinition to be stored
	 * @throws FxApplicationException TODO
	 * @throws FxNotFoundException         if the StepDefinition referenced by the parameter id does not exist
	 * @throws FxNoAccessException         if the caller lacks the permissions to modify the step
	 * @throws FxInvalidParameterException if the uniqueTarget id is invalid
	 * @throws FxUpdateException           if the function failed to modify the step definition
	 */
	void update(StepDefinition stepDefinition) throws FxApplicationException;

	/**
	 * Removes a step definition.
	 * <p/>
	 * The caller must be within ROLE_WORKFLOWMANAGEMENT.
	 *
	 * @param id     the unique id of the step definition to remove
	 * @throws FxApplicationException TODO
	 * @throws FxNotFoundException   if the step definition to remove does not exist
	 * @throws FxNoAccessException   if the caller lacks the permissions to remove the step definition
	 * @throws com.flexive.shared.exceptions.FxRemoveException     if the remove failed
	 * @throws FxEntryInUseException if the step definition is used (needed) and cannot be removed
	 */
	void remove(long id) throws FxApplicationException;
}
