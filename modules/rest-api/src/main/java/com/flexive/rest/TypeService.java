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
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.*;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provide access to flexive type instances.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Path("/environment/type/{idOrName}")
@FxRestApi
public class TypeService implements FxRestApiService {

    @Context
    private UriInfo uriInfo;
    @Context
    private HttpHeaders headers;


    @GET
    public Object getType(@PathParam("idOrName") String idOrName) throws FxApplicationException {
        final FxEnvironment env = CacheAdmin.getEnvironment();
        final FxType type;
        if (StringUtils.isNumeric(idOrName)) {
            type = env.getType(Long.parseLong(idOrName));
        } else {
            type = env.getType(idOrName);
        }

        return FxRestApiResponse.ok(FxRestApiUtils.responseMapBuilder()
                .put("name", type.getName())
                .put("id", type.getId())
                .put("assignments", buildAssignments(env, type))
                .put("properties", buildProperties(env, type))
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

    private Object buildAssignments(FxEnvironment env, FxType type) {
        final List<FxAssignment> assignments = type.getAllAssignments();
        final List<Map<String, Object>> result = Lists.newArrayListWithCapacity(assignments.size());
        for (FxAssignment assignment : assignments) {
            result.add(FxRestApiUtils.responseMapBuilder()
                    .put("id", assignment.getId())
                    .put("xpath", assignment.getXPath())
                    .put("alias", assignment.getAlias())
                    .put("enabled", assignment.isEnabled())
                    .put("isProperty", assignment instanceof FxPropertyAssignment)
                    .put("isGroup", assignment instanceof FxGroupAssignment)
                    .put("parentId", assignment.getParentGroupAssignment() == null ? -1 : assignment.getParentGroupAssignment().getId())
                    .put("label", ContentService.serializeMultiLangSimple(env, assignment.getLabel()))
                    .put("hint", ContentService.serializeMultiLangSimple(env, assignment.getHint()))
                    .put("minOccurrences", assignment.getMultiplicity().getMin())
                    .put("maxOccurrences", assignment.getMultiplicity().getMax())
                    .putAll(getSpecificAssignmentOptions(assignment))
                    .build()
            );
        }
        return result;
    }

    private Map<String, Object> getSpecificAssignmentOptions(FxAssignment assignment) {
        if (assignment instanceof FxGroupAssignment) {
            final FxGroupAssignment ga = (FxGroupAssignment) assignment;
            return ImmutableMap.<String, Object>of(
                    "groupMode", ga.getMode().name()
            );
        } else {
            final FxPropertyAssignment pa = (FxPropertyAssignment) assignment;
            return ImmutableMap.<String, Object>of(
                    "aclId", pa.getACL() != null ? pa.getACL().getId() : -1,
                    "propertyId", pa.getProperty().getId(),
                    "multiLang", pa.isMultiLang(),
                    "defaultLanguage", pa.getDefaultLanguage()
            );
        }
    }

    private Object buildProperties(FxEnvironment env, FxType type) {
        final Set<FxProperty> properties = Sets.newHashSet(Lists.transform(type.getAllProperties(), new Function<FxPropertyAssignment, FxProperty>() {
            @Override
            public FxProperty apply(@Nullable FxPropertyAssignment input) {
                return input == null ? null : input.getProperty();
            }
        }));
        final List<Map<String, Object>> result = Lists.newArrayListWithCapacity(properties.size());
        final Map<String, Object> specificOptions = Maps.newHashMap();
        for (FxProperty prop : properties) {
            specificOptions.clear();
            if (prop.getReferencedList() != null) {
                specificOptions.put("referencedList", prop.getReferencedList().getName());
            }
            if (prop.getReferencedType() != null) {
                specificOptions.put("referencedType", prop.getReferencedType() == null ? null : prop.getReferencedType().getName());
            }

            result.add(FxRestApiUtils.responseMapBuilder()
                    .put("id", prop.getId())
                    .put("name", prop.getName())
                    .put("dataType", prop.getDataType().name())
                    .put("multiLang", prop.isMultiLang())
                    .put("emptyValue", ContentService.serializeMultiLangSimple(env, prop.getEmptyValue()))
                    .put("defaultValue", ContentService.serializeMultiLangSimple(env, prop.getDefaultValue()))
                    .put("uniqueMode", prop.getUniqueMode().name())
                    .putAll(specificOptions)
                    .build()
            );
        }
        return result;
    }
}
