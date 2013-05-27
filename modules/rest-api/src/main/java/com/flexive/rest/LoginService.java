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

import com.flexive.core.Database;
import com.flexive.core.security.FxDBAuthentication;
import com.flexive.core.security.UserTicketStore;
import com.flexive.rest.interceptors.FxRestApi;
import com.flexive.rest.shared.FxRestApiConst;
import com.flexive.rest.shared.FxRestApiResponse;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.security.Account;
import com.google.common.collect.ImmutableMap;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.sql.SQLException;

/**
 * REST-API login service. Returns a REST-API token for a valid username/password combination that can be used
 * in subsequent requests as the "token" parameter for assuming the credentials of this user.
 * When a valid token already exists for the user, the token is returned (instead of returning a new one).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Path("/login")
@FxRestApi
public class LoginService implements FxRestApiService {
    @Context
    private UriInfo uriInfo;
    @Context
    private HttpHeaders headers;


    @POST
    public Object doLogin(@FormParam("username") String name, @FormParam("password") String password) throws FxApplicationException, SQLException, FxLoginFailedException {
        final AccountEngine accountEngine = EJBLookup.getAccountEngine();
        FxContext.startRunningAsSystem();
        try {
            if (FxDBAuthentication.checkLogin(name, password, FxContext.getUserTicket(), Database.getDataSource())) {
                FxContext.get().overrideTicket(
                        UserTicketStore.getUserTicket(name)
                );
                final Account account = accountEngine.load(name);
                final String token;
                if (account.isRestTokenExpired()) {
                    token = accountEngine.generateRestToken();
                } else {
                    token = account.getRestToken();
                }
                return FxRestApiResponse.ok(ImmutableMap.<String, Object>of("token", token));
            } else {
                return FxRestApiResponse.error(FxRestApiConst.STATUS_LOGIN_INVALID);
            }
        } finally {
            FxContext.stopRunningAsSystem();
        }
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
