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
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static com.flexive.cmis.Utils.getRepoConnection;
import static org.junit.Assert.*;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ConnectionTest {
    @BeforeClass
    public static void init() {
        Utils.init();
    }

    @Test
    public void getRepository() {
        assertEquals(FlexiveRepository.class, getRepoConnection().getRepository().getClass());
    }

    @Test
    public void getRootFolder() {
        assertEquals(String.valueOf(FxTreeNode.ROOT_NODE), getRepoConnection().getRootFolder().getId());
    }

    @Test
    public void simpleQuery() {
        final Collection<CMISObject> result =
                getRepoConnection().query("SELECT doc.name, doc.title, doc.description, doc.date FROM doc AS doc", false);
        assertEquals(4, result.size());
    }

    @Test
    public void documentQuery() {
        final Collection<CMISObject> result = query("SELECT * FROM " + BaseType.DOCUMENT);
        final Set<String> foundTypes = collectResultTypes(result);
        assertTrue("Expected more than one type in the result set", foundTypes.size() > 1);
    }

    @Test
    public void folderQuery() {
        final Collection<CMISObject> result = query("SELECT * FROM " + BaseType.FOLDER);
        assertTrue(result.size() > 0);
        final Set<String> foundTypes = collectResultTypes(result);
        assertEquals(2, foundTypes.size());
        assertTrue("Folder type not contained: " + foundTypes, foundTypes.contains("folder"));
        assertTrue("Fold type not contained: " + foundTypes, foundTypes.contains("fold"));
        final String rootFolderId = getRepoConnection().getRootFolder().getId();
        for (CMISObject row : result) {
            if (!rootFolderId.equals(row.getId())) {
                assertNotNull("Non-root folder must have a parent", row.getParent());
                assertTrue(row.getParents().size() > 0);
            }
        }
    }

    @Test
    public void createDocument() throws UpdateConflictException, NameConstraintViolationException {
        final Document doc = getRepoConnection().newDocument("doc", getRepoConnection().getRootFolder());
        doc.setValue("title", "createDocument test title");
        try {
            doc.save();
        } finally {
            doc.deleteAllVersions();
        }
    }

    private Collection<CMISObject> query(String s) {
        final Collection<CMISObject> result = getRepoConnection().query(s, false);
        assertTrue(result.size() > 0);
        return result;
    }


    private Set<String> collectResultTypes(Collection<CMISObject> result) {
        final Set<String> foundTypes = new HashSet<String>();
        for (CMISObject row : result) {
            foundTypes.add(row.getTypeId());
        }
        return foundTypes;
    }

}
