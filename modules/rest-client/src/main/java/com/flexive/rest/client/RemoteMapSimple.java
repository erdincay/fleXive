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

import org.apache.commons.lang.StringUtils;

import java.util.Locale;
import java.util.Map;

/**
 * Simple path-based or map-based lookup for a REST API response body.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class RemoteMapSimple {
    private final Map<String, Object> root;
    private final boolean forceUppercase;

    /**
     * Create a new remote map.
     *
     * @param root              the root map instance
     * @param forceUppercase    when true, all map keys are expected to be in upper case, and all queries will be
     *                          transformed into upper case as well (e.g. for FxContent-based maps).
     */
    public RemoteMapSimple(Map<String, Object> root, boolean forceUppercase) {
        this.root = root;
        this.forceUppercase = forceUppercase;
    }

    public Object getData(String path) {
        return get(path).getData();
    }

    public RemoteDataSimple get(String path) {
        Map<String, Object> current = root;
        final String[] parts = splitPath(path);

        RemoteDataSimple parent = new RemoteDataSimple(this, "/", root, null);

        for (int i = 0; i < parts.length; i++) {
            final String name = forceUppercase ? parts[i].toUpperCase(Locale.ENGLISH) : parts[i];
            final Object value = current.get(name);
            parent = new RemoteDataSimple(this, name, value, parent);
            if (i == parts.length - 1) {
                return parent;
            }
            if (value == null) {
                return null;
            }
            if (!(value instanceof Map)) {
                throw new IllegalArgumentException("Not a group attribute: " + name + " in " + path);
            }
            current = (Map<String, Object>) value;
        }
        return null;
    }

    /**
     * @return  untyped access to the remote response body.
     */
    public Map<String, Object> getRoot() {
        return root;
    }

    private String[] splitPath(String path) {
        return StringUtils.split(path, '/');
    }
}
