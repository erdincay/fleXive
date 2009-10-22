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
package com.flexive.chemistry.webdav;

import com.bradmcevoy.http.*;
import org.apache.chemistry.*;
import static com.flexive.chemistry.webdav.AuthenticationFilter.getConnection;

import java.io.IOException;

/**
 * Base class for returning resources from a Chemistry repository. It is also responsible for creating
 * new connections to the repository. The connection is provided (for the duration of the request)
 * by {@link AuthenticationFilter}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class ChemistryResourceFactory implements ResourceFactory {
    /**
     * Create a connection and authenticate the user with the request-based information (if available).
     * If auth is passed but invalid, the method should throw a {@link LoginException}.
     *
     * @param request   the WebDAV request
     * @return          the connection to be used for this request
     */
    public abstract Connection createConnection(Request request, Auth auth) throws LoginException;

    /**
     * If true, authentication will always be required (and cached in the session). This is useful to force
     * users working on the repository with their own account, rather than a shared ("guest") account.
     * Otherwise, authentication will only occur when the user requests a protected resource.
     *
     * @return  true if users must authenticate in order to work with this repository
     */
    protected abstract boolean requireAuthentication();

    /**
     * {@inheritDoc}
     */
    public Resource getResource(String host, String path) {
        final String objectPath = stripContextPath(path);
        final ObjectEntry object = getConnection().getSPI().getObjectByPath(objectPath, null, true, false);
        if (object == null) {
            return null;
        }
        return createResource(objectPath, getConnection().getObject(object));
    }

    protected String stripContextPath(String path) {
        final String stripped = path.replace(getContextPath(), "").trim();
        return stripped.length() == 0 ? "/" : stripped;
    }

    /**
     * {@inheritDoc}
     */
    public String getSupportedLevels() {
        return "1,2";
    }

    protected String getContextPath() {
        return ServletRequest.getRequest() != null
                ? ServletRequest.getRequest().getContextPath()
                : ""; // fallback for tests
    }

    protected Resource createResource(String path, CMISObject object) {
        if (object == null) {
            throw new IllegalArgumentException("object parameter not set");
        } else if (object instanceof Document) {
            try {
                final Document doc = (Document) object;
                return createDocument(path, doc);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else if (object instanceof Folder) {
            return createFolder(path, (Folder) object);
        } else {
            throw new IllegalArgumentException(
                    "Do not know how to create a WebDAV resource for an object with class " + object.getClass()
            );
        }
    }

    protected FolderResource createFolder(String path, Folder folder) {
        return new FolderResource(this, path, folder);
    }

    protected DocumentResource createDocument(String path, Document doc) throws IOException {
        if (doc.getContentStream() != null) {
            return createBinaryDocument(path, doc);
        } else {
            return createTextDocument(path, doc);
        }
    }

    protected TextDocumentResource createTextDocument(String path, Document doc) {
        return new TextDocumentResource(this, path, doc);
    }

    protected BinaryDocumentResource createBinaryDocument(String path, Document doc) {
        return new BinaryDocumentResource(this, path, doc);
    }
}
