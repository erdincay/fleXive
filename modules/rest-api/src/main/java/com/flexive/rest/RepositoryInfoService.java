/**
 * This file is part of the [fleXive](R) framework.
 *
 * Copyright (c) 1999-2013
 * UCS - unique computing solutions gmbh (http://www.ucs.at)
 * All rights reserved
 *
 * The [fleXive](R) project is free software; you can redistribute
 * it and/or modify it under the terms of the GNU Lesser General Public
 * License version 2.1 or higher as published by the Free Software Foundation.
 *
 * The GNU Lesser General Public License can be found at
 * http://www.gnu.org/licenses/lgpl.html.
 * A copy is found in the textfile LGPL.txt and important notices to the
 * license from the author are found in LICENSE.txt distributed with
 * these libraries.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * For further information about UCS - unique computing solutions gmbh,
 * please see the company website: http://www.ucs.at
 *
 * For further information about [fleXive](R), please see the
 * project website: http://www.flexive.org
 *
 *
 * This copyright notice MUST APPEAR in all copies of the file!
 */
package com.flexive.rest;

import com.flexive.rest.interceptors.FxRestApi;
import com.flexive.rest.shared.FxRestApiResponse;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.media.FxMediaEngine;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Repository info service.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Path("/info")
@FxRestApi
public class RepositoryInfoService implements FxRestApiService {
    @Context
    private UriInfo uriInfo;
    @Context
    private HttpHeaders headers;

    @GET
    public Object getData() throws FxApplicationException {
        return FxRestApiResponse.ok(FxRestApiUtils.responseMapBuilder()
                .put("flexive.version", FxSharedUtils.getFlexiveVersion())
                .put("flexive.edition", FxSharedUtils.getFlexiveEdition())
                .put("flexive.build", FxSharedUtils.getBuildNumber())
                .put("as.name", FxSharedUtils.getApplicationServerName())
                .put("db.name", EJBLookup.getDivisionConfigurationEngine().getDatabaseInfo())
                .put("db.driver", EJBLookup.getDivisionConfigurationEngine().getDatabaseDriverInfo())
                .put("tree.modified", new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Date(CacheAdmin.getTreeModificationTimestamp())))
                .put("media.imagemagick.available", FxMediaEngine.hasImageMagickInstalled())
                .put("media.imagemagic.version", FxMediaEngine.getImageMagickVersion())
                .put("media.imagemagick.identify", FxMediaEngine.isImageMagickIdentifySupported())
                .build());
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return headers;
    }

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }
}
