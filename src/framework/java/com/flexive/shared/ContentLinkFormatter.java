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
package com.flexive.shared;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.search.FxPaths;
import org.apache.commons.lang.StringUtils;

/**
 * Provides the default mapper for content URIs. The format method takes a FxPK identifying an
 * object and returns an URI for the given object. 
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ContentLinkFormatter {
    public static final String DEFAULT_ADMIN_CONTENT = "<a href=\"adm/content/contentEditor.jsf?action=editInstance&pk=%{id}.%{version}\">%{id}.%{version}</a>";
    public static final String DEFAULT_ADMIN_ITEM = "<a href=\"adm/content/contentEditor.jsf?action=editInstance&id=%{id}\">%{caption}</a>";

    private static final ContentLinkFormatter INSTANCE = new ContentLinkFormatter();
    private static final int MAX_CAPTION_LEN = 20;
    private static final String PATH_DELIM = " / ";

    /**
     * Protected c'tor to avoid instantiation
     */
    protected ContentLinkFormatter() {
    }

    public static ContentLinkFormatter getInstance() {
        return INSTANCE;
    }

    /**
     * <p>Uses the given format string to create a hyperlink for the given primary key.
     * Supported placeholders are:</p>
     * <table>
     * <tr>
     * <th>%{pk}</th>
     * <td>The primary key in "dot" notation. For example, new FxPK(42, 1)
     * results in "42.1" being substituted in the URI.</td>
     * </tr>
     * <tr>
     * <th>%{id}</th>
     * <td>The object ID.</td>
     * </tr>
     * <tr>
     * <th>%{version}</th>
     * <td>The object version.</td>
     * </tr>
     * </table>
     *
     * @param formatString  the input format string. For example, <code>"/content/%{id}.html"</code>
     * @param pk    the content PK to be formatted
     * @return  the resulting hyperlink
     */
    public String format(String formatString, FxPK pk) {
        return StringUtils.defaultString(formatString, DEFAULT_ADMIN_CONTENT).replace("%{id}", String.valueOf(pk.getId()))
                .replace("%{version}", String.valueOf(pk.getVersion()))
                .replace("%{pk}", pk.toString());
    }

    /**
     * <p>Uses the given format string to create an URI for the given tree path items.
     * If more than one path is contained in the given parameter, the paths are joined with
     * a ',' character.
     * Supported placeholders are:</p>
     * <table>
     * <tr>
     * <th>%{pk}</th>
     * <td>The primary key in "dot" notation. For example, new FxPK(42, 1)
     * results in "42.1" being substituted in the URI.</td>
     * </tr>
     * <tr>
     * <th>%{id}</th>
     * <td>The object ID.</td>
     * </tr>
     * <tr>
     * <th>%{version}</th>
     * <td>The object version.</td>
     * </tr>
     * <tr>
     * <th>%{nodeId}</th>
     * <td>The tree node ID.</td>
     * </tr>
     * <tr>
     * <th>%{caption}</th>
     * <td>The item caption (will be URL-escaped).</td>
     * </tr>
     * </table>
     *
     * @param formatString  the input format string. For example, <code>"/content/%{id}.html"</code>
     * @param paths the tree paths to be formatted
     * @return  the resulting URI string
     */
    public String format(String formatString, FxPaths paths) {
        StringBuilder out = new StringBuilder(255);
        for (FxPaths.Path path: paths.getPaths()) {
            out.append(out.length() > 0 ? ", " : "");
            for (FxPaths.Item item: path.getItems()) {
                out.append(format(formatString, item)).append(PATH_DELIM);
            }
            if (!path.getItems().isEmpty()) {
                out.delete(out.length() - PATH_DELIM.length(), out.length());
            }
        }
        return out.toString();
    }

    protected String format(String formatString, FxPaths.Item item) {
        final String result = format(StringUtils.defaultString(formatString, DEFAULT_ADMIN_ITEM),
                new FxPK(item.getReferenceId()))
                .replace("%{nodeId}", String.valueOf(item.getNodeId()));
        return result.replace("%{caption}", StringUtils.abbreviate(item.getCaption(), MAX_CAPTION_LEN));
    }
}
