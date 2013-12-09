/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
import com.flexive.shared.structure.FlattenOptions;
import com.flexive.shared.structure.FxType;
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

    /**
     * Export the type with the requested id as XML
     *
     * @param id requested type id
     * @return XML export
     * @throws FxApplicationException on errors
     */
    String export(long id) throws FxApplicationException;

    /**
     * Import a type fromt he given XML.
     * If the type exists, changes will be applied.
     *
     * @param typeXML the type as xml
     * @return the imported type
     * @throws FxApplicationException on errors
     */
    FxType importType(String typeXML) throws FxApplicationException;

    /**
     * Get the number of instances of a given type
     *
     * @param typeId requested type
     * @return number of instances
     * @since 3.1
     */
    public long getInstanceCount(long typeId);

    /**
     * Migrate all assignments of the given type that can be stored in a flat storage
     *
     * @param storage name of the storage
     * @param typeId  id of the type
     * @throws FxApplicationException on errors
     * @since 3.1
     */
    public void flatten(String storage, long typeId) throws FxApplicationException;

    /**
     * Migrate all assignments of the given type that can be stored in a flat storage
     *
     * @param storage name of the storage
     * @param typeId  id of the type
     * @param options options to filter the flatten operations
     * @throws FxApplicationException on errors
     * @since 3.2.0
     */
    public void flatten(String storage, long typeId, FlattenOptions options) throws FxApplicationException;

    /**
     * Migrate all assignments of the given type that can be stored in the default flat storage
     *
     * @param typeId  id of the type
     * @throws FxApplicationException on errors
     * @since 3.1
     */
    public void flatten(long typeId) throws FxApplicationException;

    /**
     * Migrate all assignments of the given type that can be stored in the default flat storage
     *
     * @param typeId  id of the type
     * @param options options to filter the flatten operations
     * @throws FxApplicationException on errors
     * @since 3.2.0
     */
    public void flatten(long typeId, FlattenOptions options) throws FxApplicationException;

    /**
     * Migrate all assignments of the given type that are stored in a flat storage to the hierarchical storage
     *
     * @param typeId  id of the type
     * @throws FxApplicationException on errors
     * @since 3.1
     */
    public void unflatten(long typeId) throws FxApplicationException;
}
