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
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxResultRow;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.value.FxValue;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

/**
 * FxSQL query service. Accepts FxSQL queries via POST and the "q" parameter, and returns the result set in the requested language.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Path("/query/fxsql")
@FxRestApi
public class FxSqlService implements FxRestApiService {
    @Context
    private UriInfo uriInfo;
    @Context
    private HttpHeaders headers;

    @POST
    public Object query(@FormParam("q") String query) throws FxApplicationException {
        if (StringUtils.isBlank(query)) {
            throw new IllegalArgumentException("Empty query");
        }

        final FxResultSet result = EJBLookup.getSearchEngine().search(query, 0, Integer.MAX_VALUE, null);

        return FxRestApiResponse.ok(
                ImmutableMap.of(
                        "columns", result.getColumnNames(),
                        "columnLabels", result.getColumnLabels(),
                        "rows", Iterables.transform(result.getResultRows(), new ResultRowTransformer(result))
                )
        );
    }

    @Override
    public HttpHeaders getHttpHeaders() {
        return headers;
    }

    @Override
    public UriInfo getUriInfo() {
        return uriInfo;
    }

    private static class ResultRowTransformer implements Function<FxResultRow, Object> {
        private final int columnCount;

        private ResultRowTransformer(FxResultSet result) {
            this.columnCount = result.getColumnCount();
        }

        public Object apply(@Nullable FxResultRow row) {
            if (row == null) {
                return null;
            }
            final Object[] rowResult = new Object[columnCount];
            for (int i = 0; i < columnCount; i++) {
                final Object rowValue = row.getValue(i + 1);
                final Object serializedValue;
                if (rowValue instanceof FxValue) {
                    final FxValue fxValue = (FxValue) rowValue;
                    serializedValue = ContentService.serializeValue(fxValue, fxValue.getBestTranslation());
                } else if (rowValue instanceof FxPK) {
                    final FxPK pk = (FxPK) rowValue;
                    serializedValue = pk.getId() + "." + pk.getVersion();
                } else {
                    serializedValue = rowValue;
                }
                rowResult[i] = serializedValue;
            }
            return rowResult;
        }
    }
}
