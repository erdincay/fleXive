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

import org.apache.chemistry.*;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.flexive.cmis.Utils.getRepoConnection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FolderTest {
    @BeforeClass
    public static void init() {
        Utils.init();
    }

    @Test
    public void getAncestors() throws NameConstraintViolationException, UpdateConflictException {
        final Connection conn = getRepoConnection();

        assertEquals(0, conn.getRootFolder().getParents().size());
        
        final Folder folder = conn.newFolder("fold", conn.getRootFolder());
        folder.setName("getAncestors test folder");
        folder.save();
        try {
            assertEquals(1, folder.getParents().size());
            assertEquals(conn.getRootFolder().getId(), folder.getParents().iterator().next().getId());

        } finally {
            folder.deleteTree(Unfiling.DELETE);
        }
    }

    @Test
    public void moveFolder() throws UpdateConflictException, NameConstraintViolationException {
        final Connection conn = getRepoConnection();

        final Folder src = conn.newFolder("fold", conn.getRootFolder());
        final Folder child = conn.newFolder("fold", src);
        src.save();
        child.save();

        try {
            assertEquals(1, src.getChildren().size());
            assertTrue(src.getChildren().contains(child));

            child.move(conn.getRootFolder(), null);
            assertEquals(0, src.getChildren().size());
        } finally {
            src.deleteTree(Unfiling.DELETE);
            child.delete();
        }

    }
}
