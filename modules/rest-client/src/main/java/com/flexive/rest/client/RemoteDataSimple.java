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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Offers a convenient query-based API for the response body represented as a {@link RemoteMapSimple}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class RemoteDataSimple implements Iterable<RemoteDataSimple> {
    private static final Log LOG = LogFactory.getLog(RemoteDataSimple.class);

    private final RemoteMapSimple content;
    private final String path;
    private final RemoteDataSimple parent;
    private final Object data;

    RemoteDataSimple(RemoteMapSimple content, String path, Object data, RemoteDataSimple parent) {
        this.content = content;
        this.path = path;
        this.parent = parent;
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public List<RemoteDataSimple> getValues() {
        if (isCollection()) {
            final List values = (List) data;
            final List<RemoteDataSimple> result = Lists.newArrayListWithCapacity(values.size());
            for (Object value : values) {
                result.add(new RemoteDataSimple(content, path, value, this));
            }
            return result;
        } else {
            return ImmutableList.of(this);
        }
    }

    public RemoteDataSimple at(int index) {
        return getValues().get(index);
    }

    @Override
    public Iterator<RemoteDataSimple> iterator() {
        return getValues().iterator();
    }

    public RemoteDataSimple get(String name) {
        if (isEmpty() || isGroup()) {
            return new RemoteDataSimple(content, name, data == null ? null : ((Map) data).get(name.toUpperCase(Locale.ENGLISH)), this);
        } else {
            throw new IllegalArgumentException("Cannot perform a lookup on an element of type " + getType());
        }
    }

    public RemoteDataSimple getParent() {
        return parent;
    }

    public boolean isEmpty() {
        return data == null;
    }

    public boolean isCollection() {
        return data instanceof List;
    }

    public boolean isGroup() {
        return data instanceof Map;
    }

    public boolean isProperty() {
        return !isGroup() && !isCollection();
    }

    public String getType() {
        if (isEmpty()) {
            return "empty";
        } else if (isCollection()) {
            return "collection";
        } else if (isGroup()) {
            return "group";
        } else {
            return "property";
        }
    }

    public String getPath() {
        if (parent == null) {
            return path;
        } else {
            return parent.getPath() + path;
        }
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper(FxRestClient.JSON_FACTORY).writeValueAsString(data);
        } catch (JsonProcessingException e) {
            LOG.warn("Failed to convert a value to JSON", e);
            return super.toString();
        }
    }
}
