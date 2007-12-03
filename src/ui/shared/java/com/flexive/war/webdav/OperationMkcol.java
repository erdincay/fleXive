/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.war.webdav;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Mkcol operation.
 * <p/>
 * MKCOL creates a new collection resource at the location specified by the Request-URI.
 * If the resource identified by the Request-URI is non-null then the MKCOL MUST fail.
 * During MKCOL processing, a server MUST make the Request-URI a member of its parent collection, unless the
 * Request-URI is "/". If no such ancestor exists, the method MUST fail.<br>
 * When the MKCOL operation creates a new collection resource, all ancestors MUST already exist, or the method MUST
 * fail with a 409 (Conflict) status code. For example, if a request to create collection /a/b/c/d/ is made, and
 * neither /a/b/ nor /a/b/c/ exists, the request must fail.<br>
 * When MKCOL is invoked without a request body, the newly created collection SHOULD have no members.
 * A MKCOL request message may contain a message body. The behavior of a MKCOL request when the body is present
 * is limited to creating collections, members of a collection, bodies of members and properties on the collections
 * or members. If the server receives a MKCOL request entity type it does not support or understand it
 * MUST respond with a 415 (Unsupported Media Type) status code. The exact behavior of MKCOL for various
 * request media types is undefined in this document, and will be specified in separate documents.
 * <br><br>
 * Possible return codes:
 * 201 (Created) - The collection or structured resource was created in its entirety.<br>
 * 403 (Forbidden) - This indicates at least one of two conditions: 1) the server does not allow the creation of
 * collections at the given location in its namespace, or 2) the parent collection of the Request-URI exists but
 * cannot accept members.<br>
 * 405 (Method Not Allowed) - MKCOL can only be executed on a deleted/non-existent resource.<br>
 * 409 (Conflict) - A collection cannot be made at the Request-URI until one or more intermediate collections have
 * been created.<br>
 * 415 (Unsupported Media Type)- The server does not support the request type of the body.<br>
 * 507 (Insufficient Storage) - The resource does not have sufficient space to record the state of the resource
 * after the execution of this method.<br>
 * 423 (Locked) - The resource is locked
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class OperationMkcol extends Operation {

    public OperationMkcol(HttpServletRequest req, HttpServletResponse resp, boolean readonly) {
        super(req, resp, readonly);
    }

    void writeResponse() throws IOException {
        int resultCode = FxWebDavServlet.getDavContext().createCollection(request, path);
        if (resultCode == FxWebDavStatus.SC_CREATED) {
            response.setStatus(FxWebDavStatus.SC_CREATED);
        } else {
            response.sendError(resultCode, "Failed to create the collection");
        }
    }
}
