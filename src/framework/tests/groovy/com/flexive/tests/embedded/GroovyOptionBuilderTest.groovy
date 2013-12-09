/** *************************************************************
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
 ************************************************************** */
package com.flexive.tests.embedded

import com.flexive.shared.scripting.groovy.GroovyOptionBuilder
import com.flexive.shared.structure.FxStructureOption
import org.testng.Assert
import org.testng.annotations.Test

/**
 * Tests for the     {@link com.flexive.shared.scripting.groovy.GroovyOptionBuilder GroovyOptionBuilder}     class.
 *
 * @author Christopher Blasnik (cblasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GroovyOptionBuilderTest {

    @Test(groups = ["ejb", "scripting", "structure"])
    void simpleOptionBuilder() {
        def optionList = new GroovyOptionBuilder().option1(value: "FOO") {
            option2(value: false)
            option3(value: "BAR", overridable: false, isInherited: false)
            option4(value: true, overridable: true, isInherited: true)
        }

        Assert.assertEquals(optionList.size(), 4);
        FxStructureOption option = optionList.get(0)
        Assert.assertEquals(option.getKey(), "OPTION1")
        Assert.assertEquals(option.getValue(), "FOO")
        // test defaults
        Assert.assertTrue(option.isOverridable())
        Assert.assertTrue(option.getIsInherited())

        option = optionList.get(1);
        Assert.assertFalse(option.isValueTrue());
        // test defaults
        Assert.assertTrue(option.isOverridable())
        Assert.assertTrue(option.getIsInherited())

        option = optionList.get(2);
        Assert.assertFalse(option.isOverridable())
        Assert.assertFalse(option.getIsInherited())

        option = optionList.get(3);
        Assert.assertTrue(option.isValueTrue())
        Assert.assertTrue(option.isOverridable())
        Assert.assertTrue(option.getIsInherited())
    }
}