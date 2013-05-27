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

import com.flexive.rest.exceptions.GuestAccessDisabledException;
import com.flexive.rest.shared.FxRestApiConst;
import com.flexive.rest.shared.FxRestApiResponse;
import com.flexive.shared.exceptions.FxRestApiTokenExpiredException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Wrap any exception in a {@link com.flexive.rest.shared.FxRestApiResponse} envelope.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Provider
public class ExceptionHandler implements ExceptionMapper<Throwable> {
    private static final Log LOG = LogFactory.getLog(ExceptionHandler.class);

    public Response toResponse(Throwable throwable) {
        final FxRestApiResponse response;

        if (throwable instanceof FxRestApiTokenExpiredException) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid or expired REST-API token: " + ((FxRestApiTokenExpiredException) throwable).getToken());
            }
            response = FxRestApiResponse.error(FxRestApiConst.STATUS_TOKEN_INVALID, "Invalid REST-API token or token expired");
        } else if (throwable instanceof GuestAccessDisabledException) {
            response = FxRestApiResponse.error(FxRestApiConst.STATUS_ERROR_GENERIC, "No security token provided and guest access is disabled");
        } else {
            LOG.error("Error during REST-API request", throwable);
            response = FxRestApiResponse.error(throwable.getMessage());
        }

        return FxRestApiUtils.buildResponse(response);
    }
}
