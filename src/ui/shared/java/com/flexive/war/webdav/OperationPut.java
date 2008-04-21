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
package com.flexive.war.webdav;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Process a POST request for the specified resource.
 * <p/>
 * A PUT that would result in the creation of a resource without an appropriately scoped parent collection MUST
 * fail with a 409 (Conflict).<br><br>
 * Collections:<br>
 * As defined in the HTTP/1.1 specification [RFC2068], the "PUT method requests that the enclosed entity be
 * stored under the supplied Request-URI." Since submission of an entity representing a collection would implicitly
 * encode creation and deletion of resources, this specification intentionally does not define a transmission format
 * for creating a collection using PUT. Instead, the MKCOL method is defined to create collections.
 * <br>
 * When the PUT operation creates a new non-collection resource all ancestors MUST already exist. If all ancestors
 * do not exist, the method MUST fail with a 409 (Conflict) status code. For example, if resource /a/b/c/d.html
 * is to be created and /a/b/c/ does not exist, then the request must fail.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class OperationPut extends Operation {

    public OperationPut(HttpServletRequest req, HttpServletResponse resp, boolean readonly) {
        super(req, resp, readonly);
    }

    void writeResponse() throws IOException {
        int resultCode = FxWebDavServlet.getDavContext().createResource(request, path);
        if (resultCode == FxWebDavStatus.SC_CREATED) {
            response.setStatus(FxWebDavStatus.SC_CREATED);
        } else {
            response.sendError(resultCode, "Failed to create the resource");
        }
    }
}
