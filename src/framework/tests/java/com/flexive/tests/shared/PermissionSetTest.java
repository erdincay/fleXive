/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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

import org.testng.annotations.Test;
import org.testng.Assert;
import com.flexive.shared.security.PermissionSet;

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
        Assert.assertTrue(set.isMayEdit());
        Assert.assertTrue(!set.isMayRelate());
        Assert.assertTrue(!set.isMayDelete());
        Assert.assertTrue(set.isMayExport());
        Assert.assertTrue(set.isMayCreate());
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
            Assert.assertTrue(edit == set.isMayEdit());
            Assert.assertTrue(relate == set.isMayRelate());
            Assert.assertTrue(delete == set.isMayDelete());
            Assert.assertTrue(export == set.isMayExport());
            Assert.assertTrue(create == set.isMayCreate());
        }
    }

}
