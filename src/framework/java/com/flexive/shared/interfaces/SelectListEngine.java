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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListEdit;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.structure.FxSelectListItemEdit;

import javax.ejb.Remote;

/**
 * SelectList management
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface SelectListEngine {

    /**
     * Create or update a FxSelectList and all of its items
     *
     * @param list the FxSelectList to create or update
     * @throws FxApplicationException on errors
     * @return id of the list
     */
    long save(FxSelectListEdit list) throws FxApplicationException;

    /**
     * Create or update a FxSelectListItem
     *
     * @param item the FxSelectListItem to create or update
     * @throws FxApplicationException on errors
     * @return id of the item
     */
    long save(FxSelectListItemEdit item) throws FxApplicationException;

    /**
     * Remove a select list
     *
     * @param list the list ro remove
     * @throws FxApplicationException on errors
     */
    void remove(FxSelectList list) throws FxApplicationException;

    /**
     * Remove a select list item
     *
     * @param item the item to remove
     * @throws FxApplicationException on errors
     */
    void remove(FxSelectListItem item) throws FxApplicationException;


     /**
     * Get the number of entries using a given select list item,
     *
     * @param selectListItemId id of the requested select list item
     * @return number of entries using the select list item
     * @throws FxApplicationException on errors
     */
    long getSelectListItemInstanceCount(long selectListItemId) throws FxApplicationException;

}
