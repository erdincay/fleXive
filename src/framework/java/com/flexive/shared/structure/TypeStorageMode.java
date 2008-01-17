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
package com.flexive.shared.structure;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.ObjectWithLabel;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.value.FxString;

import java.io.Serializable;

/**
 * Mode how tables are stored/accessed
 * 
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum TypeStorageMode implements Serializable, ObjectWithLabel {

    /**
     * One table per type
     */
    Flat(0, false, "One table per type"),

    /**
     * Master/Detail tables to support hierarchies
     */
    Hierarchical(1, true, "Master/Detail tables to support hierarchies"),

    /**
     * External (not by fleXive managed) table
     */
    External(2, false, "External (not by fleXive managed) table"),

    /**
     * In-memory storage
     */
    Memory(3, false, "In-memory storage");

    private int id;
    private boolean supported;
    private String description;

    /**
     * Ctor
     *
     * @param id id
     * @param supported mode is supported
     * @param description description
     */
    private TypeStorageMode(int id, boolean supported, String description) {
        this.id = id;
        this.supported = supported;
        this.description = description;
    }

    /**
     * Getter for the internal id
     *
     * @return internal id
     */
    public int getId() {
        return id;
    }

    /**
     * Getter for the description
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Is this TypeStorageMode supported (yet)?
     *
     * @return if this TypeStorageMode is supported
     */
    public boolean isSupported() {
        return supported;
    }


    /**
     * Get a TypeStorageMode by its id
     *
     * @param id id
     * @return TypeStorageMode
     * @throws FxNotFoundException on errors
     */
    public static TypeStorageMode getById(int id) throws FxNotFoundException {
        for( TypeStorageMode mode: TypeStorageMode.values())
            if( mode.id == id)
                return mode;
        throw new FxNotFoundException("ex.structure.typeStorageMode.notFound.id", id);
    }

    /**
     * {@inheritDoc}
     */
    public FxString getLabel() {
        return FxSharedUtils.getEnumLabel(this);
    }
}
