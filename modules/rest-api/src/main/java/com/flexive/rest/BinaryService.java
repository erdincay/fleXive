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
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.stream.FxStreamUtils;
import com.flexive.shared.value.BinaryDescriptor;
import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

/**
 * Service providing direct access to binaries. The response is streamed directly to the caller, the standard
 * response envelope will not be added.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Path("/binary/{id}")
@FxRestApi
public class BinaryService implements FxRestApiService {
    @Context
    private UriInfo uriInfo;
    @Context
    private HttpHeaders headers;

    @GET
    public Response downloadBinary(@PathParam("id") long id,
                                   @QueryParam("quality") String qualityParam) throws FxApplicationException {

        final BinaryDescriptor desc;

        // get the binary descriptor to determine the MIME type for the response (security will kick in
        // when actually streaming the binary, so we can use supervisor privileges here)
        FxContext.startRunningAsSystem();
        try {
            desc = EJBLookup.getContentEngine().getBinaryDescriptor(id);
        } finally {
            FxContext.stopRunningAsSystem();
        }

        final BinaryDescriptor.PreviewSizes quality = StringUtils.isBlank(qualityParam)
                ? BinaryDescriptor.PreviewSizes.ORIGINAL
                : BinaryDescriptor.PreviewSizes.valueOf(qualityParam.trim().toUpperCase(Locale.ENGLISH));

        final InputStream in = FxStreamUtils.getBinaryStream(desc, quality);

        return Response.ok(new StreamingOutput() {
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                try {
                    ByteStreams.copy(in, outputStream);
                } finally {
                    Closeables.close(in, false);
                }
            }
        }).type(desc.getMimeType()).build();
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
