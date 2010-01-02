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
package com.flexive.shared.security;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.ObjectWithLabel;
import com.flexive.shared.value.FxString;

/**
 * ACL permissions
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.1
 */
public enum ACLPermission implements ObjectWithLabel {
    CREATE, READ, EDIT, DELETE, RELATE, EXPORT,
    NOT_CREATE, NOT_READ, NOT_EDIT, NOT_DELETE, NOT_RELATE, NOT_EXPORT;

    /**
     * Check if <code>check</code> is contained in perms
     *
     * @param check Perm to check
     * @param perms array of Perm's
     * @return if <code>check</code> is contained in perms
     */
    public static boolean contains(ACLPermission check, ACLPermission... perms) {
        for (ACLPermission perm : perms)
            if (perm == check)
                return true;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public FxString getLabel() {
        return FxSharedUtils.getEnumLabel(this);
    }
}
