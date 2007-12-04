/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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
import com.flexive.shared.exceptions.FxInvalidParameterException
import com.flexive.shared.exceptions.FxRuntimeException
import com.flexive.shared.scripting.groovy.GroovyContentBuilder
import com.flexive.shared.scripting.groovy.GroovyTypeBuilder
import com.flexive.shared.structure.*
import com.flexive.shared.value.FxString
import com.flexive.tests.embedded.TestUsers
import static org.testng.Assert.assertEquals
import org.testng.annotations.AfterClass
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test

/**
* Tests for the {@link com.flexive.shared.scripting.groovy.FxTypeBuilder FxTypeBuilder} class.
*
* @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
* @version $Rev$
*/
class GroovyTypeBuilderTest {
    @BeforeClass (groups = ["ejb", "scripting", "structure"])
    def beforeClass() {
        com.flexive.tests.embedded.FxTestUtils.login(TestUsers.SUPERVISOR);
    }

    @AfterClass (groups = ["ejb", "scripting", "structure"])
    def afterClass() {
        com.flexive.tests.embedded.FxTestUtils.logout()
    }

    def getPropertyAssignment(FxType type, String xpath) {
        return (FxPropertyAssignment) type.getAssignment(xpath)
    }

    def getProperty(FxType type, String xpath) {
        return getPropertyAssignment(type, xpath).getProperty()
    }

    def environment() {
        return CacheAdmin.environment
    }

    @Test (groups = ["ejb", "scripting", "structure"])
    def simpleStructureBuilder() {
        try {
            // create the type "builderTest"
            new GroovyTypeBuilder().builderTest {
                // assign the caption property
                myCaption(assignment: "ROOT/CAPTION")
                // add some new properties
                stringPropertyDefault()
                numberProperty(FxDataType.Number)
                descriptionProperty(description: new FxString("string property description"))
                multilineProperty(multiline: true)
                multilangProperty(multilang: true)
                uniqueProperty(uniqueMode: UniqueMode.Global)
                referenceProperty(FxDataType.Reference, referencedType: CacheAdmin.environment.getType("DOCUMENT"))
                listProperty(FxDataType.SelectMany, referencedList: CacheAdmin.environment.getSelectLists().get(0))
                veryLongUniquePropertyName(alias: "myProperty")

                // create a new group
                MultiGroup(description: new FxString("my group"), multiplicity: FxMultiplicity.MULT_0_N) {
                    // assign a property inside the group
                    nestedCaption(assignment: "ROOT/CAPTION")
                    // create a new property
                    groupNumberProperty(FxDataType.Number)

                    // nest another group
                    NestedGroup(multiplicity: FxMultiplicity.MULT_1_N) {
                        nestedProperty()
                    }
                }

                // assign the standard contact data address group here
                TestAddress(assignment: "CONTACTDATA/ADDRESS")
            }
            def type = CacheAdmin.getEnvironment().getType("builderTest")

            assertEquals(getProperty(type, "/myCaption").getDataType(), FxDataType.String1024)
            assertEquals(getPropertyAssignment(type, "/myCaption").getBaseAssignmentId(), CacheAdmin.environment.getAssignment("ROOT/CAPTION").id)
            assertEquals(getProperty(type, "/MultiGroup/nestedCaption").getDataType(), FxDataType.String1024)
            assertEquals(getPropertyAssignment(type, "/MultiGroup/nestedCaption").getBaseAssignmentId(), CacheAdmin.environment.getAssignment("ROOT/CAPTION").id)
            assertEquals(getProperty(type, "/stringPropertyDefault").getDataType(), FxDataType.String1024)
            assertEquals(getProperty(type, "/stringPropertyDefault").isMultiLine(), false)
            assertEquals(getProperty(type, "/stringPropertyDefault").isMultiLang(), false)
            assertEquals(getProperty(type, "/numberProperty").getDataType(), FxDataType.Number)
            assertEquals(getProperty(type, "/numberProperty").isMultiLine(), false)
            assertEquals(getProperty(type, "/descriptionProperty").getLabel().getDefaultTranslation(), "string property description")
            assertEquals(getProperty(type, "/multilineProperty").isMultiLine(), true)
            assertEquals(getProperty(type, "/multilangProperty").isMultiLine(), false)
            assertEquals(getProperty(type, "/multilangProperty").isMultiLang(), true)
            assertEquals(getProperty(type, "/uniqueProperty").getUniqueMode(), UniqueMode.Global)
            assertEquals(getProperty(type, "/referenceProperty").getReferencedType(), CacheAdmin.environment.getType("DOCUMENT"))
            assertEquals(getProperty(type, "/listProperty").getReferencedList(), CacheAdmin.environment.getSelectLists().get(0))
            assertEquals(getProperty(type, "/multigroup/groupNumberProperty").getDataType(), FxDataType.Number)
            assertEquals(getPropertyAssignment(type, "/multigroup/groupNumberProperty").getParentGroupAssignment().getAlias(), "MULTIGROUP")
            assertEquals(type.getAssignment("/multigroup").getMultiplicity(), FxMultiplicity.MULT_0_N)
            assertEquals(type.getAssignment("/multigroup").getDisplayLabel().defaultTranslation, "my group")
            assertEquals(getProperty(type, "/multigroup/nestedGroup/nestedProperty").getDataType(), FxDataType.String1024)
            assertEquals(type.getAssignment("/multigroup/nestedGroup").getMultiplicity(), FxMultiplicity.MULT_1_N)
            assertEquals(type.getAssignment("/testAddress").getBaseAssignmentId(), CacheAdmin.environment.getAssignment("CONTACTDATA/ADDRESS").id)
            assertEquals(getProperty(type, "/myProperty").getDataType(), FxDataType.String1024); // check aliased assignment

            // extend type using builder
            def extBuilder = new GroovyTypeBuilder("builderTest")
            extBuilder {
                anotherStringProperty()
            }
            type = CacheAdmin.getEnvironment().getType("builderTest")
            assertEquals(getProperty(type, "/anotherStringProperty").getDataType(), FxDataType.String1024)

            // now use the content builder to set some properties
            final cbuilder = new GroovyContentBuilder("builderTest")
            cbuilder {
                myCaption("test content")
                myProperty("aliased property content")
            }
            final content = cbuilder.getContent()
            assertEquals(content.getValue("/myCaption").defaultTranslation, "test content")
            assertEquals(content.getValue("/myProperty").defaultTranslation, "aliased property content")
        } finally {
            removeTestType()
        }
    }

    @Test (groups = ["ejb", "scripting", "structure"])
    def typeConstructorArguments() {
        // test miscellaneous arguments to the type constructor
        try {
            new GroovyTypeBuilder().builderTest(usePermissions: false)
            final FxType type = getTestType()
            assert !type.usePermissions()
            assert !type.useInstancePermissions()
            assert !type.usePropertyPermissions()
            assert !type.useStepPermissions()
            assert !type.useTypePermissions()
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useInstancePermissions: true)
            assert getTestType().useInstancePermissions()
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useInstancePermissions: false)
            assert !getTestType().useInstancePermissions()
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(usePropertyPermissions: true)
            assert getTestType().usePropertyPermissions()
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(usePropertyPermissions: false)
            assert !getTestType().usePropertyPermissions()
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useStepPermissions: true)
            assert getTestType().useStepPermissions()
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useStepPermissions: false)
            assert !getTestType().useStepPermissions()
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useTypePermissions: true)
            assert getTestType().useTypePermissions()
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useTypePermissions: false)
            assert !getTestType().useTypePermissions()
        } finally {
            removeTestType()
        }

    }

    @Test (groups = ["ejb", "scripting", "structure"])
    def invalidPropertyAssignmentBuilder() {
        // try to create a property assignment using a group assignment
        try {
            new GroovyTypeBuilder().builderTest {
                invalidCaption(assignment: "CONTACTDATA/ADDRESS")
            }
            assert false: "Successfully created a property assignment referencing to a group"
        } catch (FxRuntimeException e) {
            if (!(e.converted != null && e.converted instanceof FxInvalidParameterException  \
                 && ("assignment".equalsIgnoreCase(((FxInvalidParameterException) e.converted).parameter)))) {
                throw e;
            }
            // else: success
        } finally {
            removeTestType()
        }
    }

    @Test (groups = ["ejb", "scripting", "structure"])
    def invalidGroupAssignmentBuilder() {
        // try to create a group assignment using a property assignment
        try {
            new GroovyTypeBuilder().builderTest {
                InvalidAddress(assignment: "ROOT/CAPTION")
            }
            assert false: "Successfully created a group assignment referencing to a property"
        } catch (FxRuntimeException e) {
            if (!(e.converted != null && e.converted instanceof FxInvalidParameterException  \
                 && ("assignment".equalsIgnoreCase(((FxInvalidParameterException) e.converted).parameter)))) {
                throw e;
            }
            // else: success
        } finally {
            removeTestType()
        }
    }

    def private removeTestType() {
        try {
            EJBLookup.getTypeEngine().remove(CacheAdmin.getEnvironment().getType("builderTest").id)
        } catch (FxRuntimeException e) {
            // ignore
        }
    }

    def private FxType getTestType() {
        return environment().getType("builderTest")
    }
}