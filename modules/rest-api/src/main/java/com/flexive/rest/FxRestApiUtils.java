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
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.value.FxValue;
import com.flexive.war.filter.FxBasicFilter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ws.rs.core.*;
import java.util.Locale;
import java.util.Map;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxRestApiUtils {
    private static final Log LOG = LogFactory.getLog(FxRestApiUtils.class);

    private static final String CTX_REQUEST = "REST-API.requestContext";

    public static enum ResponseFormat {
        XML(MediaType.APPLICATION_XML_TYPE),
        JSON(MediaType.APPLICATION_JSON_TYPE);

        private final MediaType mediaType;

        private ResponseFormat(MediaType mediaType) {
            this.mediaType = mediaType;
        }

        public MediaType getMediaType() {
            return mediaType;
        }
    }

    private FxRestApiUtils() {
    }

    /**
     * @return  the response format as selected by the format query parameter.
     */
    public static ResponseFormat getResponseFormat() {
        return getResponseFormat(getRequestContext().getUriInfo());
    }

    public static ResponseFormat getResponseFormat(UriInfo uriInfo) {
        final String format = uriInfo.getQueryParameters(true).getFirst("format");
        return StringUtils.isBlank(format)
                ? ResponseFormat.JSON
                : ResponseFormat.valueOf(format.trim().toUpperCase(Locale.ENGLISH));
    }

    public static Response buildResponse(FxRestApiResponse response) {
        final Response.ResponseBuilder builder = Response.ok(response);

        final RequestContext requestContext = getRequestContext();

        response.getStatus().setDurationMillis(response.getStatus().getTimestampMillis() - requestContext.getTimestampMillis());

        if (!response.getStatus().isSuccess()) {
            builder.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return builder.type(getResponseFormat(requestContext.getUriInfo()).getMediaType()).build();
    }

    /**
     * Apply common request parameters such as the requested language. Call this before <em>any</em> API call
     * as it processes security configuration and sets request context information that will be used later.
     *
     * <p>
     *     This method is called automatically on JAX-RS classes or methods annotated with {@link com.flexive.rest.interceptors.FxRestApi @FxRestApi}.
     * </p>
     */
    public static void applyRequestParameters(HttpHeaders headers, UriInfo uriInfo) throws FxApplicationException {
        final MultivaluedMap<String,String> queryParameters = uriInfo.getQueryParameters(true);

        if (FxContext.get().getAttribute(CTX_REQUEST) == null) {
            // set context for the first request only, subsequent JAX-RS handler invocations in this request
            // should reuse the existing context
            FxContext.get().setAttribute(CTX_REQUEST, new RequestContext(headers, uriInfo));
        }

        // get access token
        String token = queryParameters.getFirst(FxRestApiConst.HEADER_TOKEN);
        if (token == null && headers.getRequestHeader(FxRestApiConst.HEADER_TOKEN) != null) {
            token = Iterables.getFirst(headers.getRequestHeader(FxRestApiConst.HEADER_TOKEN), null);
        }
        if (StringUtils.isNotBlank(token)) {
            EJBLookup.getAccountEngine().loginByRestToken(token);
        }
        if (FxContext.getUserTicket().isGuest() && !FxBasicFilter.isGuestAccessAllowed()) {
            throw new GuestAccessDisabledException();
        }

        final String lang = queryParameters.getFirst("lang");
        if (StringUtils.isNotBlank(lang)) {
            // override request user's language
            final FxLanguage overrideLang;
            if (StringUtils.isNumeric(lang)) {
                overrideLang = CacheAdmin.getEnvironment().getLanguage(Long.parseLong(lang));
            } else {
                overrideLang = CacheAdmin.getEnvironment().getLanguage(lang);
            }
            FxContext.getUserTicket().setLanguage(overrideLang);
        }
    }

    public static ResponseMapBuilder responseMapBuilder() {
        return new ResponseMapBuilder();
    }

    private static RequestContext getRequestContext() {
        final RequestContext requestContext = (RequestContext) FxContext.get().getAttribute(CTX_REQUEST);

        if (requestContext == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("REST-API request context missing, annotate call with @FxRestApi or call FxRestApiUtils#applyRequestParameters in your handler");
            }
            throw new IllegalStateException("REST-API request context missing");
        }
        return requestContext;
    }

    private static class RequestContext {
        private final UriInfo uriInfo;
        private final HttpHeaders httpHeaders;
        private final long timestampMillis;

        private RequestContext(HttpHeaders httpHeaders, UriInfo uriInfo) {
            this.httpHeaders = httpHeaders;
            this.uriInfo = uriInfo;
            this.timestampMillis = System.currentTimeMillis();
        }

        private HttpHeaders getHttpHeaders() {
            return httpHeaders;
        }

        private UriInfo getUriInfo() {
            return uriInfo;
        }

        private long getTimestampMillis() {
            return timestampMillis;
        }
    }

    public static class ResponseMapBuilder extends ConditionalMapBuilder<String, Object, ResponseMapBuilder> {

        public ResponseMapBuilder put(String key, FxValue value) {
            if (value != null) {
                put(key, ContentService.serializeValue(value, value.getBestTranslation()));
            }
            return this;
        }

    }

    /**
     * Like Guava's ImmutableMapBuilder, but silently ignores null values (instead of throwing an exception), and
     * provides additional convenience methods.
     *
     * @param <K>    the key type
     * @param <V>    the value type
     */
    private static class ConditionalMapBuilder<K, V, T extends ConditionalMapBuilder<K, V, T>> extends ImmutableMap.Builder<K, V> {
        @Override
        public T put(Map.Entry<? extends K, ? extends V> entry) {
            return (T) super.put(entry);
        }

        @Override
        public T put(K key, V value) {
            if (value != null) {
                super.put(key, value);
            }
            return (T) this;
        }

        @Override
        public T putAll(Map<? extends K, ? extends V> map) {
            return (T) super.putAll(map);
        }
    }
}
