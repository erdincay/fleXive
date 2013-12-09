/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import java.util.regex.Matcher;

/**
 * An URI matcher that provides additional typesafe methods for querying
 * the parameters contained in a mapped content URI.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ContentURIMatcher extends URIMatcher {
    private static final Log LOG = LogFactory.getLog(ContentURIMatcher.class);
    public static final String REQUEST_PK = ContentURIMatcher.class.getName() + ".PK";
    public static final String REQUEST_ORIG_URI = ContentURIMatcher.class.getName() + ".ORIGINAL_URI";
    public static final String REQUEST_MAPPED_URI = ContentURIMatcher.class.getName() + ".MAPPED_URI";

    private FxPK pk;

    public ContentURIMatcher(ContentURIRoute mapper, String uri, Matcher matcher) {
        super(mapper, uri, matcher);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String apply(FacesContext context) {
        final HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        // store content pk found in URI
        request.setAttribute(REQUEST_PK, getPk());
        return super.apply(context);
    }

    public long getId() {
        return getPk().getId();
    }

    public FxPK getPk() {
        if (pk == null && isMatched()) {
            if (route.hasParameter("pk")) {
                pk = FxPK.fromString(getParameter("pk"));
            } else if (route.hasParameter("id")) {
                pk = new FxPK(Long.parseLong(getParameter("id")));
            }
        }
        if (pk == null) {
            throw new FxInvalidParameterException("uri", LOG, "ex.contentUriRoute.uri.pk", uri).asRuntimeException();
        }
        return pk;
    }
}
