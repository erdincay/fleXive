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
package com.flexive.shared.structure;

import com.flexive.shared.exceptions.FxNotFoundException;

/**
 * Mode how groups act (one-of or any-of)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum GroupMode {
    /**
     * Only one of the groups children may be present, honoring their regular indices.
     * This mode only makes sense if all subgroups/properties are optional!
     */
    OneOf(0),

    /**
     * Any of the groups children may be present, honoring their regular indices
     */
    AnyOf(1);

    private int id;

    /**
     * Ctor
     *
     * @param id the internal db id
     */
    GroupMode(int id) {
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
     * Get a GroupMode by its id
     *
     * @param id internal id
     * @return GroupMode
     * @throws FxNotFoundException on errors
     */
    public static GroupMode getById(int id) throws FxNotFoundException {
        for (GroupMode groupMode : GroupMode.values())
            if (groupMode.id == id)
                return groupMode;
        throw new FxNotFoundException("ex.structure.groupMode.notFound.id", id);
    }
}
