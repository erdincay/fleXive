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


abstract class Operation {

    protected static final String DEFAULT_NAMESPACE = "DAV:";
    protected static boolean debug = false;

    protected HttpServletRequest request;
    protected HttpServletResponse response;
    protected String path;
    protected boolean readonly;
    private String dest = null;


    /**
     * Constructor.
     *
     * @param req  the request
     * @param resp the response
     */
    public Operation(HttpServletRequest req, HttpServletResponse resp, boolean readonly) {
        this.request = req;
        this.response = resp;
        this.readonly = readonly;

        // Build the path we want to handle
        this.path = FxWebDavUtils.getDavPath(req);
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        dest = request.getHeader("Destination");
    }

    public String getDestination() {
        return dest;
    }

    /**
     * Writes the response to the webdav client.
     *
     * @throws IOException if the operation fails
     */
    abstract void writeResponse() throws IOException;

}
