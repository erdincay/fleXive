/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.shared.configuration.parameters;

import com.flexive.shared.configuration.parameters.*;
import com.flexive.shared.configuration.*;
import com.flexive.shared.exceptions.FxCreateException;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;

/**
 * <p>
 * This factory beans provides methods for instantiating new parameter objects to be used with
 * flexive's configuration API. To create a typesafe parameter definition, you must supply
 * the parameter class (e.g. {@link Long}) and the attributes describing the parameter itself,
 * such as the (global) path it's stored under and its scope (e.g. user- or division-scoped).
 * </p>
 * <p>
 * :
 * <pre>
 * // define a boolean parameter "booleanParam" for path "/configuration" with a default value of false
 * Parameter&lt;Boolean> bp = ParameterFactory.newInstance(Boolean.class, "/configuration", ParameterScope.USER, "booleanParam", false);
 *
 * // define a long parameter with a default value of 25
 * Parameter&lt;Long> lp = ParameterFactory.newInstance(Long.class, "/configuration", ParameterScope.USER, "longParam", 25L);
 *
 * // define an object parameter for QueryRootNode.class with no default value that works by serializing the objects via XStream
 * Parameter&lt;QueryRootNode> DEFAULT_QUERY = ParameterFactory.newInstance(QueryRootNode.class, "/configuration", ParameterScope.USER, "defaultQuery", null);
 * </pre>
 * More examples can be found in {@link com.flexive.shared.configuration.SystemParameters}.
 * </p>
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public final class ParameterFactory {
    private static final Map<Class, Class<? extends Parameter>> INSTANCES;

    static {
        final Map<Class, Class<? extends Parameter>> instances = new HashMap<Class, Class<? extends Parameter>>();
        instances.put(Boolean.class, BooleanParameter.class);
        instances.put(Integer.class, IntegerParameter.class);
        instances.put(Long.class, LongParameter.class);
        instances.put(String.class, StringParameter.class);
        INSTANCES = Collections.unmodifiableMap(instances);
    }

    private ParameterFactory() {
    }

    /**
     * Creates a new parameter from a {@link ParameterData} instance.
     *
     * @param cls   the parameter type class
     * @param data  the bean holding all parameter properties
     * @return      a new parameter instance
     */
    public static <T> Parameter<T> newInstance(Class<T> cls, ParameterData<T> data) {
        return getImpl(cls).setData(data).freeze();
    }

    /**
     * Create a new parameter with key and default value, using path information from a {@link ParameterPath} object.
     *
     * @param cls   the parameter type class
     * @param path  the path description
     * @param key   the parameter key
     * @param defaultValue  the default value (if null, no default value will be used)
     * @return      a new parameter instance
     */
    public static <T> Parameter<T> newInstance(Class<T> cls, ParameterPath path, String key, T defaultValue) {
        return newInstance(cls, new ParameterDataBean<T>(path, key, defaultValue));
    }

    /**
     * Create a new parameter without a specific key and a default value. The key has to be specified
     * in configuration queries. This is useful for dynamic keys, e.g. for storing user preferences
     * for different pages (using the page path for the key).
     *
     * @param cls   the parameter type class
     * @param path  the path description
     * @param defaultValue  the default value (if null, no default value will be used)
     * @return      a new parameter instance
     */
    public static <T> Parameter<T> newInstance(Class<T> cls, ParameterPath path, T defaultValue) {
        return newInstance(cls, path, "", defaultValue);
    }

    /**
     * Create a new parameter for a path with key and default value.
     *
     * @param cls   the parameter type class
     * @param path  the path description
     * @param scope the parameter scope (user, division, or global)
     * @param key   the parameter key
     * @param defaultValue  the default value (if null, no default value will be used)
     * @return  a new parameter instance
     */
    public static <T> Parameter<T> newInstance(Class<T> cls, String path, ParameterScope scope, String key, T defaultValue) {
        return newInstance(cls, new ParameterPathBean(path, scope), key, defaultValue);
    }

    /**
     * Create a new parameter for a path with a default value, but without a fixed key. The key has to be specified
     * in configuration queries. This is useful for dynamic keys, e.g. for storing user preferences
     * for different pages (using the page path for the key).
     *
     * @param cls   the parameter type class
     * @param path  the path description
     * @param scope the parameter scope (user, division, or global)
     * @param defaultValue  the default value (if null, no default value will be used)
     * @return  a new parameter instance
     */
    public static <T> Parameter<T> newInstance(Class<T> cls, String path, ParameterScope scope, T defaultValue) {
        return newInstance(cls, new ParameterPathBean(path, scope), "", defaultValue);
    }

    private static <T> Parameter<T> getImpl(Class<T> cls) {
        if (INSTANCES.containsKey(cls)) {
            try {
                //noinspection unchecked
                return INSTANCES.get(cls).newInstance();
            } catch (Exception e) {
                throw new FxCreateException("ex.configuration.parameter.impl.instantiate", INSTANCES.get(cls), e).asRuntimeException();
            }
        } else {
            // handle all non-primitive objects with generical xml-based parameters
            return new ObjectParameter<T>(null, cls, false);
            //throw new FxNotFoundException("ex.configuration.parameter.impl.notFound", cls).asRuntimeException();
        }
    }
}
