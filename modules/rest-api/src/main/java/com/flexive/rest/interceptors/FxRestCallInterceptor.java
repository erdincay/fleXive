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
package com.flexive.rest.interceptors;

import com.flexive.rest.FxRestApiService;
import com.flexive.rest.FxRestApiUtils;
import com.flexive.rest.shared.FxRestApiResponse;
import com.flexive.shared.FxContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@FxRestCall
@Interceptor
public class FxRestCallInterceptor {
    private static final Log LOG = LogFactory.getLog(FxRestCallInterceptor.class);

    @AroundInvoke
    public Object aroundInvoke(InvocationContext context) throws Exception {
        final Object target = context.getTarget();
        if (target instanceof FxRestApiService) {
            final FxRestApiService service = (FxRestApiService) target;

            FxRestApiUtils.applyRequestParameters(service.getHttpHeaders(), service.getUriInfo());

            if (LOG.isTraceEnabled()) {
                LOG.trace("REST-API call at " + service.getUriInfo().getBaseUri() + " with user " + FxContext.getUserTicket().getLoginName());
            }

            final Object result = context.proceed();

            if (result instanceof FxRestApiResponse) {
                // wrap response, set common parameters
                return FxRestApiUtils.buildResponse((FxRestApiResponse) result);
            }

            return result;
        } else {
            throw new IllegalStateException("@FxRestApi target does not implement FxRestApiService");
        }
    }

}
