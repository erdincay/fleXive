/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxTypeEdit;

import javax.ejb.Remote;

/**
 * FxType management
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface TypeEngine {

    /**
     * Create a new or update an existing FxType
     *
     * @param type FxType to create or update
     * @return id of the type
     * @throws FxApplicationException on errors
     */
    long save(FxTypeEdit type) throws FxApplicationException;

    /**
     * Completely remove a FxType and its assignments (exception will be thrown if instances or derived types exist)
     *
     * @param id id of the type to remove
     * @throws FxApplicationException on errors
     */
    void remove(long id) throws FxApplicationException;
}
