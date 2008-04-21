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

import javax.ejb.Remote;


/**
 * Mandator Interface
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface MandatorEngine {

    /**
     * Create a new mandator
     *
     * @param name   name to assign
     * @param active active flag
     * @return id of the new mandator
     * @throws FxApplicationException TODO
     * @throws FxCreateException           on errors
     * @throws FxInvalidParameterException if the name is invalid
     * @throws FxEntryExistsException      if a mandator with this name already exists
     * @throws FxNoAccessException         if the user is not in the role MANDATOR_MANAGEMENT
     */
    int create(String name, boolean active) throws FxApplicationException;

    /**
     * Assign a content instance with metadata for this mandator
     *
     * @param mandatorId id of the mandator
     * @param contentId  id of the content containing the metadata
     * @throws FxApplicationException TODO
     * @throws FxNotFoundException if either the mandator or the metadata content dont exist
     * @throws FxNoAccessException if the user is not in the role MANDATOR_MANAGEMENT
     * @throws FxUpdateException   on update errors
     */
    void assignMetaData(int mandatorId, long contentId) throws FxApplicationException;

    /**
     * Remove metadata assigned to the given mandator
     *
     * @param mandatorId id of the mandator
     * @throws FxApplicationException TODO
     * @throws FxNotFoundException if the mandator does not exist
     * @throws FxNoAccessException if the user is not in the role MANDATOR_MANAGEMENT
     * @throws FxUpdateException   on update errors
     */
    void removeMetaData(int mandatorId) throws FxApplicationException;

    /**
     * Activate a mandator
     *
     * @param mandatorId id of the mandator
     * @throws FxApplicationException TODO
     * @throws FxNotFoundException if the mandator does not exist
     * @throws FxNoAccessException if the user is not in the role MANDATOR_MANAGEMENT
     * @throws FxUpdateException   on update errors
     */
    void activate(long mandatorId) throws FxApplicationException;

    /**
     * Deactivate a mandator
     *
     * @param mandatorId id of the mandator
     * @throws FxApplicationException TODO
     * @throws FxNotFoundException if the mandator does not exist
     * @throws FxNoAccessException if the user is not in the role MANDATOR_MANAGEMENT
     * @throws FxUpdateException   on update errors
     */
    void deactivate(long mandatorId) throws FxApplicationException;

    /**
     * Remove a mandator.
     * If the mandator is in use or is referenced this method will fail!
     *
     * @param mandatorId id of the mandator
     * @throws FxApplicationException TODO
     * @throws FxEntryInUseException if the mandator is referenced
     * @throws FxNotFoundException   if the mandator does not exist
     * @throws FxNoAccessException   if the user is not in the role MANDATOR_MANAGEMENT
     * @throws FxRemoveException     on remove errors
     */
    void remove(long mandatorId) throws FxApplicationException;

    /**
     * Updates the name of the specified mandator.
     *
     * @param mandatorId    the mandator to change the name for
     * @param name          the new name
     * @throws FxApplicationException TODO
     */
    void changeName(long mandatorId, String name) throws FxApplicationException;

}
