/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.MatchResult;

/**
 * A wrapper class that represents a (possible) match of an URI mapper on a
 * specific URI.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class URIMatcher {

    protected final URIRoute route;
    protected final String uri;
    protected final MatchResult matchResult;

    URIMatcher(URIRoute route, String uri, MatchResult matchResult) {
        this.route = route;
        this.uri = uri;
        this.matchResult = matchResult;
    }

    /**
     * Apply the route on this matched URI, returning the final page to be displayed
     * (usually the route target).
     *
     * @param context the current faces context
     * @return the final page to be displayed.
     */
    public String apply(FacesContext context) {
        final HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        request.setAttribute(ContentURIMatcher.REQUEST_ORIG_URI, request.getRequestURI());
        request.setAttribute(ContentURIMatcher.REQUEST_MAPPED_URI, route.getTarget());
        return route.getTarget();
    }

    public URIRoute getMapper() {
        return route;
    }

    public String getUri() {
        return uri;
    }

    public MatchResult getMatchResult() {
        return matchResult;
    }

    public String getParameter(String parameterName) {
        return matchResult.group(route.getPosition(parameterName));
    }

    public boolean isMatched() {
        try {
            matchResult.start();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
