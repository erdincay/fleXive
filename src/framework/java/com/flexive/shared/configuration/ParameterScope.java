/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.shared.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Parameter scopes
 */
public enum ParameterScope {
    /**
     * Global parameter
     */
    GLOBAL,
    /**
     * Division parameter (with global configuration fallback)
     */
    DIVISION(ParameterScope.GLOBAL),
    /**
     * Division parameter (without global configuration fallback)
     */
    DIVISION_ONLY,
    /**
     * Node parameter (with division configuration fallback). The node name can be set with the
     * system property {@code flexive.nodename} and defaults to the system hostname as returned by
     * InetAddress.getLocalHost().getHostName().
     *
     * @since 3.1
     */
    NODE(ParameterScope.DIVISION_ONLY),
    /**
     * Node parameter (without fallback).
     */
    NODE_ONLY,
    /**
     * Application parameter scope (with division configuration fallback). The division fallback
     * does not include the global configuration, because this configuration is outside the scope
     * of a running fleXive application and cannot be updated by a flexive user without using
     * the authentication methods provided by the
     * {@link com.flexive.shared.interfaces.GlobalConfigurationEngine}.
     *
     * @since 3.1
     */
    APPLICATION(ParameterScope.DIVISION_ONLY),
    /**
     * Application parameter scope (without division configuration fallback).
     */
    APPLICATION_ONLY,
    /**
     * Mandator parameter scope (with division configuration fallback).
     *
     * @since 3.1.6
     */
    MANDATOR(ParameterScope.DIVISION_ONLY),
    /**
     * Mandator parameter scope (without fallback).
     *
     * @since 3.1.6
     */
    MANDATOR_ONLY,
    /**
     * User parameter (with application configuration fallback)
     */
    USER(ParameterScope.APPLICATION),
    /**
     * User parameter (without fallback values)
     */
    USER_ONLY;

    private List<ParameterScope> allFallbacks = new ArrayList<ParameterScope>();

    /**
     * Create a new scope.
     *
     * @param fallbacks Fallback scope(s) to be used. Fallback scopes
     *                  are applied recursively.
     */
    private ParameterScope(ParameterScope... fallbacks) {
        // get fallback scopes recursively and put them in allFallbacks
        for (ParameterScope scope : fallbacks) {
            allFallbacks.add(scope);
            final List<ParameterScope> fb2 = scope.getFallbacks();
            for (ParameterScope fallback : fb2) {
                if (!allFallbacks.contains(fallback)) {
                    allFallbacks.add(fallback);
                }
            }
        }
        this.allFallbacks = Collections.unmodifiableList(allFallbacks);
    }

    /**
     * Return the fallback scopes for this scope config.
     *
     * @return the fallback scopes for this scope config.
     */
    public List<ParameterScope> getFallbacks() {
        return this.allFallbacks;
    }

    /**
     * Returns true if the scope represents - directly or through
     * its fallbacks - the given scope.
     *
     * @param scope the scope to be checked
     * @return true if the scope represents - directly or through its fallbacks - the given scope.
     */
    public boolean hasScope(ParameterScope scope) {
        return this.allFallbacks.contains(scope);
    }
}
