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
import com.flexive.shared.content.*;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxNoAccess;
import com.flexive.shared.value.FxValue;
import com.google.common.collect.*;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Provides access to individual content instance.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Path("/content/{pk}")
@FxRestApi
public class ContentService implements FxRestApiService {
    /**
     * The result mode (controlled via ?mode=[simple|full])
     */
    public static enum Mode {
        /**
         * Simple mode, only return values in the calling user's language, no metadata.
         */
        SIMPLE,

        /**
         * Return a complete representation of the content, including all translations of multilingual
         * properties and associated metadata.
         */
        FULL
    }

    @Context
    private UriInfo uriInfo;
    @Context
    private HttpHeaders headers;

    @GET
    public Object getInstance(@PathParam("pk") String pkValue) throws FxApplicationException {
        final FxPK pk = FxPK.fromString(pkValue);
        final FxContent content = EJBLookup.getContentEngine().load(pk);

        // detect the response mode (full or simple)
        final String modeParam = uriInfo.getQueryParameters(true).getFirst("mode");
        final Mode mode = StringUtils.isBlank(modeParam)
                ? Mode.SIMPLE
                : Mode.valueOf(modeParam.trim().toUpperCase(Locale.ENGLISH));

        final Map<String, Object> data = Maps.newLinkedHashMap();

        addGroup(data, content.getRootGroup(), mode);

        return FxRestApiResponse.ok(FxRestApiUtils.responseMapBuilder()
                .put("pk", content.getPk().toString())
                .put("data", data)
                .put("mode", mode)
                .build()
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

    private void addGroup(Map<String, Object> result, FxGroupData group, Mode mode) {
        Multimap<String, FxData> multData = null;  // properties and groups with max. mult > 1 (rendered as lists)

        for (FxData data : group.getChildren()) {
            final String key = data.getAlias();
            if (data.getAssignmentMultiplicity().getMax() > 1) {
                // gather all instances of this property or group and render them afterwards
                if (multData == null) {
                    multData = ArrayListMultimap.create();
                }
                multData.put(data.getAlias(), data);
            } else {
                result.put(key, encode(data, mode));
            }
        }

        if (multData != null) {
            // render elements with max. mult. > 1 as lists
            for (String alias : multData.keys()) {
                final Collection<FxData> values = multData.get(alias);
                final List<Object> elements = Lists.newArrayListWithCapacity(values.size());
                for (FxData data : values) {
                    elements.add(encode(data, mode));
                }
                result.put(alias, elements);
            }
        }
    }

    private Object encode(FxData data, Mode mode) {
        if (data instanceof FxPropertyData) {
            return encodeProperty((FxPropertyData) data, mode);
        } else {
            return encodeGroup((FxGroupData) data, mode);
        }
    }

    private Object encodeGroup(FxGroupData data, Mode mode) {
        final Map<String, Object> childData = Maps.newHashMap();
        addGroup(childData, data, mode);
        return childData;
    }

    private Object encodeProperty(FxPropertyData prop, Mode mode) {
        final FxValue propValue = prop.getValue();
        final Object resultValue;
        if (mode == Mode.FULL) {
            // full representation of the FxValue data
            final Map<String, Object> propertyData = Maps.newHashMap();
            final List<Map<String, Object>> valueData = Lists.newArrayList();

            for (long lang : propValue.getTranslatedLanguages()) {
                final Object serializedVal = serializeValue(propValue, propValue.getTranslation(lang));
                if (serializedVal != null) {
                    valueData.add(ImmutableMap.<String, Object>of(
                            "lang", lang,
                            "value", serializedVal,
                            "isDefault", propValue.isDefaultLanguage(lang)
                    ));
                }
            }
            propertyData.put("values", valueData);
            if (propValue.getValueData() != null) {
                propertyData.put("valueData", propValue.getValueData());
            }
            resultValue = propertyData;
        } else {
            resultValue = serializeValue(propValue, propValue.getBestTranslation());
        }
        return resultValue;
    }

    static Object serializeValue(FxValue value, Object translation) {
        if (value instanceof FxNoAccess) {
            return ImmutableMap.of("access-denied", true);
        } else if (translation instanceof BinaryDescriptor) {
            final BinaryDescriptor descriptor = (BinaryDescriptor) translation;
            return FxRestApiUtils.responseMapBuilder()
                    .put("binaryId", descriptor.getId())
                    .put("binaryVersion", descriptor.getVersion())
                    .put("name", descriptor.getName())
                    .put("size", descriptor.getSize())
                    .put("mimeType", descriptor.getMimeType())
                    .put("width", descriptor.getWidth())
                    .put("height", descriptor.getHeight())
                    .put("dpi", descriptor.getResolution())
                    .build();
        } else {
            return value.getStringValue(translation);
        }
    }

    static Object serializeMultiLangSimple(FxEnvironment env, FxValue value) {
        if (value == null) {
            return ImmutableMap.of();
        }
        final long[] languages = value.getTranslatedLanguages();
        final Map<String, Object> result = Maps.newHashMapWithExpectedSize(languages.length);
        for (long lang : languages) {
            result.put(env.getLanguage(lang).getIso2digit(), serializeValue(value, value.getTranslation(lang)));
        }
        return result;
    }
}
