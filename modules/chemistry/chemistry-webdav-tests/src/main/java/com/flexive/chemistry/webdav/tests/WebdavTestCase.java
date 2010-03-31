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
package com.flexive.chemistry.webdav.tests;

import com.bradmcevoy.http.ReplaceableResource;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import junit.framework.TestCase;
import org.apache.chemistry.Repository;
import com.flexive.chemistry.webdav.DocumentResource;
import com.flexive.chemistry.webdav.FolderResource;
import com.flexive.chemistry.webdav.AuthenticationFilter;
import com.flexive.chemistry.webdav.ChemistryResourceFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Basic WebDAV for a Chemistry {@link Repository}.
 * <p>
 * To use it, subclass and implement {@link #getRepository()}
 * and {@link #getResourceFactory(org.apache.chemistry.Repository)}.
 * This test does not use the HTTP WebDAV protocol, but the Resources returned by a {@link ResourceFactory}
 * that is used by Milton for servicing HTTP WebDAV requests.
 * </p>
 *  
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class WebdavTestCase extends TestCase {
    /** Host passed to WebDAV */
    protected static final String HOST = "localhost";

    protected Repository repository;
    protected ChemistryResourceFactory resourceFactory;

    /**
     * Return the Chemistry repository to be used for this testcase.
     *
     * @return  the Chemistry repository to be used for this testcase.
     */
    protected abstract Repository getRepository();

    /**
     * Return a resource factory implementation for the given Chemistry repository.
     *
     * @param repository    the Chemistry repository
     * @return              a resource factory implementation for the given Chemistry repository.
     */
    protected abstract ChemistryResourceFactory getResourceFactory(Repository repository);

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.repository = getRepository();
        this.resourceFactory = getResourceFactory(repository);
        // set request connection
        AuthenticationFilter.setConnection(resourceFactory.createConnection(null, null));
    }

    @Override
    protected void tearDown() throws Exception {
        // remove and close connection
        AuthenticationFilter.setConnection(null);
    }

    public void testRootFolder() {
        final List<? extends Resource> children = getFolder("/").getChildren();
        assertFalse("Root folder has no children", children.isEmpty());
    }

    public void testCreateFolder() throws NotAuthorizedException, ConflictException {
        final FolderResource root = getFolder("/");
        final List<? extends Resource> childrenBefore = root.getChildren();
        root.createCollection("subfolder");
        assertEquals(
                "New folder not returned",
                childrenBefore.size() + 1,
                getFolder("/").getChildren().size()
        );

        // check if subfolder can be retrieved
        final FolderResource sub = getFolder("/subfolder");
        assertEquals(0, sub.getChildren().size());
    }

    public void testCreateContent() throws IOException, NotAuthorizedException {
        final int oldSize = getFolder("/").getChildren().size();
        final Resource doc = getFolder("/").createNew(
                "test document.txt",
                new ByteArrayInputStream("test data".getBytes("UTF-8")),
                null,
                "text/plain"
        );

        // check if document was created properly
        assertNotNull("New document should not be null", doc);
        assertEquals(
                "New document not stored in folder",
                oldSize + 1,
                getFolder("/").getChildren().size()
        );

        // retrieve document
        final DocumentResource saved = getDocument("/test document.txt");
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        saved.sendContent(os, null, null, null);
        assertEquals("test data", new String(os.toByteArray(), "UTF-8"));
    }

    public void testMoveFolder() throws NotAuthorizedException, ConflictException {
        final FolderResource m1 = getFolder("/").createCollection("move1");
        final FolderResource m2 = getFolder("/").createCollection("move2");
        m1.moveTo(m2, "new name");
        getFolder("/move2/new name");
        try {
            getFolder("/move1");
            fail("Moved folder still present");
        } catch (Exception e) {
            // pass
        }
    }

    public void testReplaceTextContent() throws IOException, NotAuthorizedException {
        final DocumentResource doc = getDocument("/folder 1/doc 1");
        assertTrue("Text document not Replaceable", doc instanceof ReplaceableResource);

        // fetch XML
        final String xml = getTextDocument("/folder 1/doc 1");
        assertTrue("XML header not found: " + xml, xml.startsWith("<?xml "));

        // replace title (set in test fixture in org.apache.chemistry.test.BasicHelper)
        ((ReplaceableResource) doc).replaceContent(
                new ByteArrayInputStream(
                        xml.replace("doc 1 title", "new doc 1 title").getBytes("UTF-8")
                ), null
        );

        // check if change was saved
        final String saved = getTextDocument("/folder 1/doc 1");
        assertTrue("New title not stored: " + saved, saved.contains("new doc 1 title"));
    }

    public void testCopyContent() {
        final DocumentResource doc = getDocument("/folder 1/doc 1");
        doc.copyTo(getFolder("/"), "copy of doc 1");
        getDocument("/copy of doc 1").delete();
    }

    public void testCopyFolder() {
        // test that test document exists
        getDocument("/folder 1/doc 1");

        final FolderResource folder = getFolder("/folder 1");
        folder.copyTo(getFolder("/"), "copy of folder 1");
        getFolder("/copy of folder 1").delete();

        // original document/folder should still exist
        getDocument("/folder 1/doc 1");
    }

    protected String getTextDocument(String path) throws IOException, NotAuthorizedException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        getDocument(path).sendContent(bos, null, null, null);
        return bos.toString("UTF-8");
    }

    protected FolderResource getFolder(String path) {
        return asResource(FolderResource.class, resourceFactory.getResource(HOST, path));
    }

    protected DocumentResource getDocument(String path) {
        return asResource(DocumentResource.class, resourceFactory.getResource(HOST, path));
    }

    protected <T extends Resource> T asResource(Class<T> typeToken, Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("resource must not be null");
        }
        if (!typeToken.isAssignableFrom(resource.getClass())) {
            fail("Expected WebDAV resource of type " + typeToken.getName() + ", got: " + resource.getClass().getName());
        }
        return typeToken.cast(resource);
    }
}
