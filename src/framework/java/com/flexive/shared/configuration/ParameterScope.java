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
package com.flexive.shared.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Parameter scopes */
public enum ParameterScope {
    /** Global parameter */
    GLOBAL,
    /** Division parameter (with global configuration fallback) */
    DIVISION(ParameterScope.GLOBAL),
    /** Division parameter (without global configuration fallback) */
    DIVISION_ONLY,
    /** User parameter (with division configuration fallback) */
    USER(ParameterScope.DIVISION_ONLY),
    /** User parameter (without fallback values) */
    USER_ONLY;

    private List<ParameterScope> allFallbacks = new ArrayList<ParameterScope>();

    /**
     * Create a new scope.
     * @param fallbacks Fallback scope(s) to be used. Fallback scopes
     * are applied recursively.
     */
    private ParameterScope(ParameterScope... fallbacks) {
        // get fallback scopes recursively and put them in allFallbacks
        for (ParameterScope scope: fallbacks) {
            allFallbacks.add(scope);
            List<ParameterScope> fb2 = scope.getFallbacks();
            for (ParameterScope fallback: fb2) {
                if (!allFallbacks.contains(fallback)) {
                    allFallbacks.add(fallback);
                }
            }
        }
        this.allFallbacks = Collections.unmodifiableList(allFallbacks);
    }

    /**
     * Return the fallback scopes for this scope config.
     * @return  the fallback scopes for this scope config.
     */
    public List<ParameterScope> getFallbacks() {
        return this.allFallbacks;
    }

    /**
     * Returns true if the scope represents - directly or through
     * its fallbacks - the given scope.
     * @param scope the scope to be checked
     * @return  true if the scope represents - directly or through its fallbacks - the given scope.
     */
    public boolean hasScope(ParameterScope scope) {
        return this.allFallbacks.indexOf(scope) != -1;
    }
}
