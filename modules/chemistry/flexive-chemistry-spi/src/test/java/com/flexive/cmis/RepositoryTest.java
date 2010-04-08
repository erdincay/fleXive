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

import com.flexive.cmis.spi.FlexiveType;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.tree.FxTreeNode;
import java.util.Collection;
import org.apache.chemistry.*;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.flexive.cmis.Utils.getRepo;
import static com.flexive.cmis.Utils.getRepoConnection;
import static org.junit.Assert.*;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class RepositoryTest {
    @BeforeClass
    public static void init() {
        Utils.init();
    }

    @Test
    public void loadRootFolder() {
        final Connection conn = getRepoConnection();
        final Folder rootFolder = conn.getRootFolder();
        assertEquals(String.valueOf(FxTreeNode.ROOT_NODE), rootFolder.getId());
    }

    @Test
    public void createFolder() throws NameConstraintViolationException, UpdateConflictException {
        final Connection conn = getRepoConnection();
        final Folder folder = conn.newFolder("folder", conn.getRootFolder());
        folder.setName("createFolder");
        folder.save();
        try {
            boolean found = false;
            for (CMISObject child : conn.getRootFolder().getChildren()) {
                if ("createFolder".equals(child.getName())) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                Assert.fail("Created folder not found, children: "
                        + conn.getRootFolder().getChildren()
                );
            }
        } finally {
            folder.delete();
        }
    }

    @Test
    public void getTypes() {
        final Repository repo = getRepo();
        assertEquals(FlexiveType.ROOT_TYPE_ID, repo.getType("ROOT").getId());
        final Collection<Type> typeDescendants = repo.getTypeDescendants(FlexiveType.ROOT_TYPE_ID);
        for (FxType fxType : CacheAdmin.getEnvironment().getTypes()) {
            if (!typeDescendants.contains(new FlexiveType(fxType))) {
                fail("All types should be a subtype of Root, could not find type: " + fxType.getName());
            }
        }
        assertEquals("Expected two folder types", 2, repo.getTypeDescendants(FxType.FOLDER).size());
        /*boolean[] hasMoreItems = new boolean[5];
        assertEquals("Type count limit not applied", 1, repo.getTypes(FxType.FOLDER, false, 1, 0, hasMoreItems).size());
        assertTrue("More items expected", hasMoreItems[0]);
        assertEquals("First entry not skipped", "fold", repo.getTypes(FxType.FOLDER, false, 1, 1, hasMoreItems).get(0).getId());*/
    }

    @Test
    public void getType() {
        assertEquals("fold", getRepo().getType("fold").getId());
        assertEquals(FlexiveType.ROOT_TYPE_ID, getRepo().getType(null).getId());
    }
}
