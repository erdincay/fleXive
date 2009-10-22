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
package com.flexive.cmis;

import static com.flexive.cmis.Utils.getRepoConnection;
import static com.flexive.shared.EJBLookup.getAccountEngine;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.security.AccountEdit;
import org.apache.chemistry.*;
import org.apache.chemistry.impl.simple.SimpleContentStream;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SPITest {
    private static long testAccountId;
    private static final String TEST_USER = "SPITest";

    @BeforeClass
    public static void init() throws FxApplicationException {
        Utils.init();
        FxContext.startRunningAsSystem();
        try {
            testAccountId = getAccountEngine().load(TEST_USER).getId();
        } catch (FxNotFoundException e) {
            testAccountId = getAccountEngine().create(
                    new AccountEdit().setName(TEST_USER).setEmail("root@localhost").setActive(true).setValidated(true),
                    TEST_USER
            );
        } finally {
            FxContext.stopRunningAsSystem();
        }
    }

    private void login() throws FxLoginFailedException, FxAccountInUseException {
        FxContext.get().login(TEST_USER, TEST_USER, true);
    }

    private void logout() throws FxLogoutFailedException {
        FxContext.get().logout();
    }
    
    @Test
    public void createDocument() {
        final Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put("Name", "test document");
        properties.put(Property.TYPE_ID, "doc");
        final ObjectId id = getSPI().createDocument(properties, Utils.getRepoConnection().getRootFolder(), null, null);
        getSPI().deleteObject(id, true);
    }

    @Test
    public void createFolder() {
        final Map<String, Serializable> properties = new HashMap<String, Serializable>();
        properties.put(Property.NAME, "test folder");
        properties.put(Property.TYPE_ID, "fold");
        final ObjectId id = getSPI().createFolder(properties, Utils.getRepoConnection().getRootFolder());
        getSPI().deleteObject(id, true);
    }

    @Test
    public void setContentStream() throws IOException {
        final Document doc = getRepoConnection().newDocument("doc", getRepoConnection().getRootFolder());
        doc.setValue("title", "setContentStream test title");
        try {
            doc.save();

            final String content = "setContentStream test doc";
            getSPI().setContentStream(doc, true, new SimpleContentStream(
                    content.getBytes("UTF-8"),
                    "text/plain",
                    "test.txt"
            ));

            final InputStream in = getSPI().getContentStream(doc, null).getStream();
            assertEquals(content, FxSharedUtils.loadFromInputStream(in));

            getSPI().deleteContentStream(doc);
            assertFalse("Content stream not deleted", getSPI().hasContentStream(doc));
        } finally {
            doc.deleteAllVersions();
        }
    }

    @Test
    public void moveObject() {
        final Connection conn = getRepoConnection();
        final Folder src = conn.newFolder("fold", conn.getRootFolder());
        src.setName("moveObject source folder");
        final Folder dest = conn.newFolder("fold", conn.getRootFolder());
        dest.setName("moveObject destination folder");
        try {
            src.save();
            dest.save();

            final Document doc = conn.newDocument("doc", null /* add via SPI */);
            doc.save();
            getSPI().addObjectToFolder(doc, src);

            assertEquals(1, conn.getFolder(src.getName()).getChildren().size());
            assertEquals(0, conn.getFolder(dest.getName()).getChildren().size());

            // object is single-filed, thus we don't need to specify the source folder
            getSPI().moveObject(doc, dest, null);

            assertEquals(0, conn.getFolder(src.getName()).getChildren().size());
            assertEquals(1, conn.getFolder(dest.getName()).getChildren().size());

            // move back to src - test with explicit source folder as well
            getSPI().moveObject(doc, src, dest);

            assertEquals(1, conn.getFolder(src.getName()).getChildren().size());
            assertEquals(0, conn.getFolder(dest.getName()).getChildren().size());
        } finally {
            dest.deleteTree(Unfiling.DELETE);
            getSPI().deleteTree(src, Unfiling.DELETE, false);
        }
    }

    @Test
    public void checkout() throws FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        final Document doc = getRepoConnection().newDocument("doc", getRepoConnection().getRootFolder());
        try {
            doc.setValue("title", "public title");
            doc.save();
            final ObjectId checkedOut = getSPI().checkOut(doc, new boolean[1]);

            try {
                getSPI().checkOut(doc, new boolean[1]);
                fail("A document cannot be checked out twice.");
            } catch (Exception e) {
                // pass
            }

            // check if we can edit the checked-out document
            login();
            try {
                doc.save();
                fail("Other users may not edit a checked-out instance.");
            } catch (FxRuntimeException e) {
                // success
            } finally {
                logout();
            }
            
            // make some changes
            final Document pwc = (Document) getRepoConnection().getObject(checkedOut);
            assertEquals("public title", pwc.getValue("title"));
            pwc.setValue("title", "my private title");
            pwc.save();

            // assert that the publicly visible document is still unchanged
            login();
            try {
                assertEquals(
                        "public title",
                        getRepoConnection().getObject(getSPI().newObjectId(doc.getId())).getValue("title")
                );
            } finally {
                logout();
            }

            final ObjectId newId = getSPI().checkIn(pwc, true, null, null, null);

            // assert that the changes were propagated (force reloading)
            assertEquals(
                    "my private title",
                    getRepoConnection().getObject(getSPI().newObjectId(newId.getId())).getValue("title")
            );

            try {
                getRepoConnection().getObject(getSPI().newObjectId(pwc.getId())).getType();
                fail("PWC should have been destroyed.");
            } catch (Exception e) {
                // pass
            }

        } finally {
            try {
                getSPI().cancelCheckOut(doc);
            } catch (Exception e) {
                // ignore
            }
            doc.deleteAllVersions();
        }
    }

    @Test
    public void objectPathFQN() {
        final ObjectEntry dog = getSPI().getObjectByPath("/folder 1/folder 2/dog.jpg", null, true, false);
        assertNotNull("Object not found by path", dog);
        assertEquals("dog.jpg", ((Document) dog).getName());
    }

    private SPI getSPI() {
        return Utils.getRepo().getSPI();
    }
}
