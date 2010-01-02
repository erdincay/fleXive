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
package com.flexive.faces;

import java.util.*;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class URIRouteCollection<T extends URIRoute> {
    // target --> route lookup table
    private final Map<String, T> routes = new HashMap<String, T>();

    public URIRouteCollection(List<T> routes) {
        for (T mapper : routes) {
            this.routes.put(mapper.getTarget(), mapper);
        }
    }

    /**
     * Returns a matcher for the given URI, or null if no matcher was found.
     *
     * @param uri the URI to be matched
     * @return a matcher for the given URI, or null if no matcher was found.
     */
    public URIMatcher getMatcher(String uri) {
        for (Map.Entry<String, T> entry : routes.entrySet()) {
            final URIMatcher matcher = entry.getValue().getMatcher(uri);
            if (matcher.isMatched()) {
                return matcher;
            }
        }
        return null;
    }

    /**
     * Returns a route for the given target URI.
     *
     * @param targetUri the target URI to be matched
     * @return a route for the given target URI, or null if none exists.
     */
    public T findForTarget(String targetUri) {
        return routes.get(targetUri);
    }

    /**
     * Returns a route for the given external URI.
     *
     * @param uri the external URI to be matched
     * @return a route for the given external URI, or null if none exists.
     */
    public T findForUri(String uri) {
        for (Map.Entry<String, T> entry : routes.entrySet()) {
            final URIMatcher matcher = entry.getValue().getMatcher(uri);
            if (matcher.isMatched()) {
                return entry.getValue();
            }
        }
        return null;
    }

    /**
     * Returns all available routes.
     *
     * @return all available routes.
     */
    public Collection<T> getRoutes() {
        return Collections.unmodifiableCollection(routes.values());
    }
}
