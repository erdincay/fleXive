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
package com.flexive.cmis;

import com.flexive.cmis.spi.FlexiveRepository;
import com.flexive.shared.tree.FxTreeNode;
import org.apache.chemistry.*;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class RepositoryInfoTest {
    private RepositoryInfo info;

    @BeforeClass
    public static void init() {
        Utils.init();
    }

    @Test
    public void getRootFolderId() {
        assertEquals(String.valueOf(FxTreeNode.ROOT_NODE), getInfo().getRootFolderId().getId());
    }

    @Test
    public void getCapabilities() {
        final RepositoryCapabilities capabilities = getInfo().getCapabilities();
        assertEquals(capabilities.hasMultifiling(), true);
        assertEquals(capabilities.getJoinCapability(), CapabilityJoin.INNER_ONLY);
        assertEquals(capabilities.hasVersionSpecificFiling(), false);
        assertEquals(capabilities.isAllVersionsSearchable(), true);
        assertEquals(capabilities.getQueryCapability(), CapabilityQuery.BOTH_COMBINED);
    }

    private synchronized RepositoryInfo getInfo() {
        if (info == null) {
            info = new FlexiveRepository(null).getInfo();
        }
        return info;
    }
}
