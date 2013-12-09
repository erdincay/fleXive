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
package com.flexive.tests.shared;

import com.flexive.shared.security.PermissionSet;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests for the {@link com.flexive.shared.security.PermissionSet} class.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class PermissionSetTest {

    @Test(groups = "shared")
    public void basicPermissionSet() {
        final PermissionSet set = new PermissionSet(true, false, false, true, true);
        assertTrue(set.isMayEdit());
        assertTrue(!set.isMayRelate());
        assertTrue(!set.isMayDelete());
        assertTrue(set.isMayExport());
        assertTrue(set.isMayCreate());
    }

    @Test(groups = "shared")
    public void genericPermissionSet() {
        for (int i = 0; i < 1 << 5; i++) {
            final boolean edit = (i & 1) > 0;
            final boolean relate = (i & 2) > 0;
            final boolean delete = (i & 4) > 0;
            final boolean export = (i & 8) > 0;
            final boolean create = (i & 16) > 0;
            final PermissionSet set = new PermissionSet(edit, relate, delete, export, create);
            assertTrue(edit == set.isMayEdit());
            assertTrue(relate == set.isMayRelate());
            assertTrue(delete == set.isMayDelete());
            assertTrue(export == set.isMayExport());
            assertTrue(create == set.isMayCreate());
        }
    }

    @Test(groups = "shared")
    public void permissionUnion() {
        final PermissionSet result =
                new PermissionSet(true, true, true, false, false)
                .union(new PermissionSet(false, false, false, false, true));
        assertEquals(result, new PermissionSet(true, true, true, false, true));
    }

    @Test(groups = "shared")
    public void permissionIntersect() {
        final PermissionSet result =
                new PermissionSet(true, true, false, true, false)
                .intersect(new PermissionSet(false, false, true, true, true));
        assertEquals(result, new PermissionSet(false, false, false, true, false));
    }
}
