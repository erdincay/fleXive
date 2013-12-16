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
package com.flexive.shared.security;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.ObjectWithLabel;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.value.FxString;

/**
 * ACL categories and their defaults
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum ACLCategory implements ObjectWithLabel {
    INSTANCE(1, 2),
    STRUCTURE(2, 7),
    WORKFLOW(3, 3),
    BRIEFCASE(4, 4),
    SELECTLIST(5, 5),
    SELECTLISTITEM(6, 6);

    private int id;
    private long defaultId;

    ACLCategory(int id, long defaultId) {
        this.id = id;
        this.defaultId = defaultId;
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
     * Get the Id of the default ACL for given category
     *
     * @return Id of the default ACL for given category
     */
    public long getDefaultId() {
        return defaultId;
    }

    /**
     * Get a TypeMode by its id
     *
     * @param id the id
     * @return TypeMode the type
     */
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public static ACLCategory getById(int id) {
        for (ACLCategory cat : ACLCategory.values())
            if (cat.id == id)
                return cat;
        throw new FxNotFoundException("ex.acl.category.notFound.id", id).asRuntimeException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxString getLabel() {
        return FxSharedUtils.getEnumLabel(this);
    }
}
