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
package com.flexive.tests.shared;

import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.ParameterDataEditBean;
import com.flexive.shared.configuration.SystemParameterPaths;
import com.flexive.shared.configuration.parameters.ParameterFactory;
import com.flexive.shared.content.FxPK;

/**
 * Configuration parameter definitions used for testing.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class TestParameters {
    /**
     * Cactus test parameter - may be modified by the test case
     */
    public static final Parameter<String> CACTUS_TEST = ParameterFactory.newInstance(String.class,
            new ParameterDataEditBean<String>(SystemParameterPaths.GLOBAL_CONFIG, "cactus.test.string", "cactus"));
    /**
     * Test parameter
     */
    public static final Parameter<Boolean> CACTUS_TEST_BOOL = ParameterFactory.newInstance(Boolean.class,
            new ParameterDataEditBean<Boolean>(SystemParameterPaths.GLOBAL_CONFIG, "cactus.test.boolean", true));
    /**
     * Test parameter
     */
    public static final Parameter<Long> CACTUS_TEST_LONG = ParameterFactory.newInstance(Long.class,
            new ParameterDataEditBean<Long>(SystemParameterPaths.GLOBAL_CONFIG, "cactus.test.long", 42L));
    /** Test parameter */
    public static final Parameter<Integer> CACTUS_TEST_INT = ParameterFactory.newInstance(Integer.class,
            new ParameterDataEditBean<Integer>(SystemParameterPaths.GLOBAL_CONFIG, "cactus.test.int", 42));
    public static final Parameter<FxPK> TEST_OBJ = ParameterFactory.newInstance(FxPK.class,
            new ParameterDataEditBean<FxPK>(SystemParameterPaths.GLOBAL_CONFIG, "cactus.test.obj", new FxPK(0, 0)));
}
