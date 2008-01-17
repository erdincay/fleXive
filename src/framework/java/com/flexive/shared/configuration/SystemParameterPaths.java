/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Enumeration holding valid parameter paths. Each parameter path has an associated
 * scope (e.g. global, divison, user, with/without fallbacks).
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public enum SystemParameterPaths implements ParameterPath {
    /** Global configuration root path */
    GLOBAL_CONFIG("/globalconfig", ParameterScope.GLOBAL),
    /** Path for storing the datasources per division */
    GLOBAL_DIVISIONS_DS("/globalconfig/datasources", ParameterScope.GLOBAL),
    /** Path for storing the domain name matchers per division */
    GLOBAL_DIVISIONS_DOMAINS("/globalconfig/domains", ParameterScope.GLOBAL),
    /** Global tree config path */
    DIVISION_TREE("/division/tree", ParameterScope.DIVISION_ONLY),

    /** Path for storing division specific stuff */
    DIVISION_RUNONCE_CONFIG("/division/runonce", ParameterScope.DIVISION_ONLY),

    /** Path for storing user search queries */
    USER_QUERIES_CONTENT("/search/content", ParameterScope.USER),
    /** Search result preferences */
    USER_RESULT_PREFERENCES("/search/results", ParameterScope.USER),

    /** unit test entry */
    TEST_GLOBAL("/test/global", ParameterScope.GLOBAL),
    /** unit test entry */
    TEST_DIVISION("/test/division", ParameterScope.DIVISION),
    /** unit test entry */
    TEST_DIVISION_ONLY("/test/divisiononly", ParameterScope.DIVISION_ONLY),
    /** unit test entry */
    TEST_USER("/test/user", ParameterScope.USER),
    /** unit test entry */
    TEST_USER_ONLY("/test/useronly", ParameterScope.USER_ONLY);
    
    /** Paths used only for unit testing */
    private static final SystemParameterPaths[] TESTPATHS = {
        TEST_GLOBAL, TEST_DIVISION, TEST_DIVISION_ONLY, TEST_USER, TEST_USER_ONLY
    };

    private String value;
    private ParameterScope scope;

    /**
     * Constructor.
     * @param path  path value
     * @param parameterScope parameter scope
     */
    private SystemParameterPaths(String path, ParameterScope parameterScope) {
		this.value = path;
        this.scope = parameterScope;
	}

    /**
     * Returns the contained configuration path.
     * @return  configuration path
     */
    public String getValue() {
		return value;
	}

    /**
     * Returns the scope of the configuration path
     * @return  the scope of the configuration path
     */
    public ParameterScope getScope() {
        return scope;
    }
    
    /**
     * Return test path entries used for unit testing.
     * @return  test path entries used for unit testing.
     */
    public static List<SystemParameterPaths> getTestPaths() {
        return Collections.unmodifiableList(Arrays.asList(TESTPATHS));
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.value + "{" + this.scope + "}";
    }
}
