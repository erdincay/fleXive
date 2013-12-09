/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.tests.embedded

import com.flexive.shared.CacheAdmin
import com.flexive.shared.EJBLookup
import com.flexive.shared.content.FxPK
import com.flexive.shared.exceptions.FxRuntimeException
import com.flexive.shared.scripting.groovy.GroovyContentBuilder
import com.flexive.shared.scripting.groovy.GroovyTypeBuilder
import com.flexive.shared.structure.FxMultiplicity
import com.flexive.shared.value.FxString
import org.testng.Assert
import org.testng.annotations.Test

/**
* GroovyContentBuilder test cases.

* @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
* @version $Rev$
*/
class GroovyContentBuilderTest {
    @Test(groups = ["content", "ejb", "scripting"])
    void createContentTest() {
        def builder = new GroovyContentBuilder("ARTICLE")
        builder {
            title("Test article")
            Abstract("My abstract text")
            teaser {
                teaser_title("Teaser title")
                teaser_text("Teaser text")
            }
            box {
                box_title(new FxString(false, "Box title 1"))
            }
            box {
                box_title("Box title 2")
                box_text("Some box text")
            }
        }
        def content = builder.getContent()
        Assert.assertEquals(content.getValue("/title").defaultTranslation, "Test article")
        Assert.assertEquals(content.getValue("/abstract").defaultTranslation, "My abstract text")
        Assert.assertEquals(content.getValue("/teaser[1]/teaser_title").defaultTranslation, "Teaser title")
        Assert.assertEquals(content.getValue("/teaser[1]/teaser_text").defaultTranslation, "Teaser text")
        Assert.assertEquals(content.getValue("/box[1]/box_title").defaultTranslation, "Box title 1")
        Assert.assertEquals(content.getValue("/box[2]/box_title").defaultTranslation, "Box title 2")
        Assert.assertEquals(content.getValue("/box[2]/box_text").defaultTranslation, "Some box text")
    }

    @Test(groups = ["content", "ejb", "scripting"])
    void groupCardinalityTest_FX314() {
        try {
            FxTestUtils.login TestUsers.SUPERVISOR
            // create test group with default multiplicity of 1
            new GroovyTypeBuilder().testFX314 {
                Group(defaultMultiplicity: 1, multiplicity: FxMultiplicity.MULT_0_1) {
                    prop(defaultValue: new FxString(false, "value"))
                }
            }
            def builder = new GroovyContentBuilder("TESTFX314")
            builder {
                Group {
                    prop("value")
                }
            }
            FxPK pk = EJBLookup.contentEngine.save(builder.content)
            EJBLookup.contentEngine.remove(pk)
        } finally {
            try {
                EJBLookup.typeEngine.remove(CacheAdmin.environment.getType("TESTFX314").id)
            } catch (FxRuntimeException e) {
                // pass
            }
            FxTestUtils.logout()
        }
    }
}