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
package com.flexive.shared.structure;

import com.flexive.shared.exceptions.FxNotFoundException;

/**
 * Modes for uniqueness of properties
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum UniqueMode {
    /**
     * No uniqueness
     */
    None(0),

    /**
     * Globally unique across all usages of a property (check on property id)
     */
    Global(1),

    /**
     * Unique within a  type (check on property id for current type)
     */
    Type(2),

    /**
     * Unique for this type and all parent or derived types (check on property id for all affected types)
     */
    DerivedTypes(3),

    /**
     * Unique within an instance (check on property id within instance)
     */
    Instance(4);


    private int id;

    /**
     * Ctor
     *
     * @param id internal DB id
     */
    UniqueMode(int id) {
        this.id = id;
    }

    /**
     * Getter for the id
     *
     * @return id
     */
    public int getId() {
        return id;
    }

    /**
     * Get a UniqueMode by its id
     *
     * @param id internal id
     * @return UniqueMode
     * @throws FxNotFoundException on errors
     */
    public static UniqueMode getById(int id) throws FxNotFoundException {
        for (UniqueMode um : UniqueMode.values())
            if (um.id == id)
                return um;
        throw new FxNotFoundException("ex.structure.uniqueMode.notFound.id", id);
    }
}
