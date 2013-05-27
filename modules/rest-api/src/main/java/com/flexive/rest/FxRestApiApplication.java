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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import javax.ws.rs.core.Application;
import java.util.Set;

/**
 * Default REST-API application. Configure this directly via web.xml or extend for customization purposes.
 *
 * <p>
 *     A configuration example via {@code web.xml} (including the mandatory RestApiFilter) can be found in the
 *     {code web.xml} of flexive-rest-api-standalone.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxRestApiApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = Sets.newHashSet();
        classes.addAll(getApiClasses());
        classes.addAll(getErrorHandlingClasses());
        classes.addAll(getProviderClasses());
        classes.addAll(getCustomClasses());
        return classes;
    }


    /**
     * @return  all REST-API service classes
     */
    protected Set<Class<?>> getApiClasses() {
        return ImmutableSet.<Class<?>>of(
                // API services
                ContentService.class, RepositoryInfoService.class, FxSqlService.class, TreeLiveService.class,
                TreeEditService.class, BinaryService.class, LoginService.class, TypeService.class
        );
    }

    protected Set<Class<?>> getErrorHandlingClasses() {
        return ImmutableSet.<Class<?>>of(ExceptionHandler.class);
    }

    /**
     * @return  the JSON/XML providers (default: Jackson)
     */
    protected Set<Class<?>> getProviderClasses() {
        return ImmutableSet.<Class<?>>of(
                // Jackson providers
                JacksonJsonProvider.class, JacksonXMLProvider.class
        );
    }

    /**
     * @return  user-defined classes (override to provide additional services)
     */
    protected Set<Class<?>> getCustomClasses() {
        return ImmutableSet.of();
    }
}
