/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.shared;

import com.flexive.shared.exceptions.FxLockException;
import com.flexive.shared.value.FxString;

/**
 * Type of a FxLock
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum FxLockType implements ObjectWithLabel {

    /**
     * No lock is set
     */
    None(0),

    /**
     * Loose lock, which can be taken over or unlocked by other users
     */
    Loose(1),

    /**
     * explicit lock, may only be taken over or unlocked only by global- and mandator-supervisors
     */
    Permanent(2);

    private static final long serialVersionUID = 6492585466399904047L;
    private int id;

    /**
     * Ctor
     *
     * @param id id in the storage
     */
    FxLockType(int id) {
        this.id = id;
    }

    /**
     * Get the id used in the storage
     *
     * @return id used in the storage
     */
    public int getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public FxString getLabel() {
        return FxSharedUtils.getEnumLabel(this);
    }

    public static FxLockType getById(int id) throws FxLockException {
        if( id == Loose.getId())
            return Loose;
        else if( id == Permanent.getId())
            return Permanent;
        else
            throw new FxLockException("ex.lock.invalidLockType.id", id);
    }
}
