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
package com.flexive.rest.client;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flexive.rest.shared.FxRestApiResponse;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.annotation.ThreadSafe;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A thread-safe connector to the flexive REST-API.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@ThreadSafe
public class FxRestClient {
    static final JsonFactory JSON_FACTORY = new JsonFactory();

    public static enum CallMethod { GET, POST }

    private final String apiBaseUrl;
    private final HttpClient client;

    public FxRestClient(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
        this.client = new DefaultHttpClient();
    }

    public String login(String username, String password) throws IOException {
        final HttpPost post = createCall(new HttpPost(), "/login");

        setPostParams(post, ImmutableMap.of(
                "username", username,
                "password", password
        ));

        final FxRestApiResponse response = parseResponse(client.execute(post));

        final String token = (String) response.getBody().get("token");

        if (StringUtils.isBlank(token)) {
            // should not happen
            throw new RuntimeException("No token and no error code returned from REST-API call");
        }

        return token;
    }

    @SuppressWarnings("unchecked")
    public RemoteMapSimple loadContent(String token, long id, int version) throws IOException {
        final HttpGet get = createCall(new HttpGet(), "/content/" + id + (version > 0 ? "." + version : ""));
        setRequestParameters(get, token);

        final FxRestApiResponse response = parseResponse(client.execute(get));

        return new RemoteMapSimple((Map<String, Object>) response.getBody().get("data"), true);
    }

    public RemoteMapSimple remoteCall(String token, String path, CallMethod method, Map<String, String> parameters) throws IOException {
        final HttpRequestBase request;
        if (method == CallMethod.GET) {
            final HttpGet httpGet = new HttpGet();
            if (parameters != null) {
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    httpGet.getParams().setParameter(entry.getKey(), entry.getValue());
                }
            }
            request = httpGet;
        } else if (method == CallMethod.POST) {
            final HttpPost httpPost = new HttpPost();
            if (parameters != null) {
                setPostParams(httpPost, parameters);
            }
            request = httpPost;
        } else {
            throw new IllegalArgumentException("Method not supported: " + method);
        }
        setRequestParameters(request, token);

        final HttpRequestBase call = createCall(request, path);

        final FxRestApiResponse response = parseResponse(client.execute(call));

        return new RemoteMapSimple(response.getBody(), false);
    }

    @SuppressWarnings("unchecked")
    public RemoteFxSqlResult queryFxSql(String token, String query) throws IOException {
        final HttpPost post = createCall(new HttpPost(), "/query/fxsql");

        setRequestParameters(post, token);
        setPostParams(post, ImmutableMap.of(
                "q", query
        ));

        final FxRestApiResponse response = parseResponse(client.execute(post));

        final Map<String, Object> body = response.getBody();

        return new RemoteFxSqlResult((List<List<Object>>) body.get("rows"), (List<String>) body.get("columns"), (List<String>) body.get("columnLabels"));
    }

    private void setRequestParameters(HttpRequestBase request, String token) {
        if (StringUtils.isNotBlank(token)) {
            request.setHeader("token", token);
        }
    }

    private void setPostParams(HttpPost post, Map<String, String> params) {
        final List<NameValuePair> result = Lists.newArrayListWithCapacity(params.size());
        for (Map.Entry<String, String> entry : params.entrySet()) {
            result.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
        }
        try {
            post.setEntity(new UrlEncodedFormEntity(result));
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T extends HttpRequestBase> T createCall(T request, String path) {
        request.setURI(URI.create(getApiUrl(path)));
        return request;
    }

    private String getApiUrl(String path) {
        return apiBaseUrl + path;
    }

    private void checkSuccess(FxRestApiResponse response) {
        final FxRestApiResponse.ResponseStatus status = response.getStatus();
        if (!status.isSuccess()) {
            throw new RemoteCallException(status.getCode(), status.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private FxRestApiResponse parseResponse(HttpResponse httpResponse) throws IOException {
        try {
            final ObjectMapper mapper = new ObjectMapper(this.JSON_FACTORY);
            final FxRestApiResponse response = mapper.readValue(this.JSON_FACTORY.createJsonParser(httpResponse.getEntity().getContent()), FxRestApiResponse.class);
            if (response.getBody() == null) {
                response.setBody(Collections.<String, Object>emptyMap());
            }
            checkSuccess(response);
            return response;
        } finally {
            // release connection
            if (httpResponse.getEntity() != null) {
                httpResponse.getEntity().getContent().close();
            }
        }
    }
}
