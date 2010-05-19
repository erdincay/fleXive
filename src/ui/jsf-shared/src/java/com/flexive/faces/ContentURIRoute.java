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

import com.flexive.shared.content.FxPK;

import java.util.Collection;
import java.util.regex.Matcher;

/**
 * <p>
 * A content URI route.It allows to both map
 * external URIs containing instance PKs to internal JSF pages AND to generate
 * those external URIs based on an instance PK.
 * </p>
 * <p>
 * Supported placeholders:
 * <table>
 * <tr>
 * <th>${id}</td>
 * <td>The content ID</td>
 * </tr>
 * <tr>
 * <th>${pk}</td>
 * <td>The content PK</td>
 * </tr>
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ContentURIRoute extends URIRoute {
    //    private static final Log LOG = LogFactory.getLog(ContentURIMapper.class);
    static {
        PARAMETERS.put("id", "([0-9]+)");
        PARAMETERS.put("pk", "([0-9]+\\.(?:[0-9]+|LIVE|MAX))");
    }

    private final String typeName;

    /**
     * Create a new content URI mapper with the given format and for the given type name.
     *
     * @param target   the target URI
     * @param format   the format string
     * @param typeName the type for which this mapper should be applied.
     * @see com.flexive.shared.structure.FxType
     */
    public ContentURIRoute(String target, String format, String typeName) {
        super(target, format);
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ContentURIMatcher getMatcher(String uri) {
        final Matcher matcher = pattern.matcher(uri);
        matcher.find();
        return new ContentURIMatcher(this, uri, matcher);
    }

    public String getMappedUri(FxPK pk) {
        return replaceUriParameter(replaceUriParameter(format, "id", String.valueOf(pk.getId())), "pk", pk.toString());
    }

    /**
     * Returns the first route matching the given typename, or null when no route was found.
     *
     * @param routes   the routes to be searched
     * @param typeName the typename to be searched for
     * @return the first route matching the given typename, or null when no route was found.
     */
    public static ContentURIRoute findForType(Collection<? extends ContentURIRoute> routes, String typeName) {
        for (ContentURIRoute route : routes) {
            if (typeName.equalsIgnoreCase(route.getTypeName())) {
                return route;
            }
        }
        return null;
    }
}
