/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2014
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.javascript;

import com.flexive.shared.EJBLookup;
import com.flexive.war.JsonWriter;

import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.flexive.shared.search.AdminResultLocations.ADMIN;

/**
 * Implements JSON/RPC methods for the search query panel.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SearchQueryEditor implements Serializable {
    private static final long serialVersionUID = -1665068866212914870L;

    /**
     * Renders all stored search queries of the calling user.
     *
     * @return all search queries of the calling user (in JSON format).
     * @throws Exception on server-side errors
     */
    public String renderSearchQueries() throws Exception {
        final StringWriter out = new StringWriter();
        final JsonWriter writer = new JsonWriter(out);
        writer.startArray();
        final List<String> names = new ArrayList<String>(EJBLookup.getSearchEngine().loadNames(ADMIN));
        Collections.sort(names);
        for (String name: names) {
            writer.startMap();
            writer.writeAttribute("name", name);
            writer.closeMap();
        }
        writer.closeArray().finishResponse();
        return out.toString();
    }

    /**
     * Remove an admin search query definition.
     *
     * @param name  the query name
     * @return  nothing
     * @throws Exception    if the query was not found or could not be deleted
     */
    public String remove(String name) throws Exception {
        EJBLookup.getSearchEngine().remove(ADMIN, name);
        return "[]";
    }
}
