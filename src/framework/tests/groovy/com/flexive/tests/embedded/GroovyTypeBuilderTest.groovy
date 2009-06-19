/** *************************************************************
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
 ************************************************************** */
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
import org.testng.Assert
import com.flexive.shared.security.ACLCategory
import com.flexive.shared.FxLanguage

/**
 * Tests for the                                  {@link com.flexive.shared.scripting.groovy.GroovyTypeBuilder GroovyTypeBuilder}                                  class.
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

    def getGroupAssignment(FxType type, String xpath) {
        return (FxGroupAssignment) type.getAssignment(xpath)
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
            assertEquals(type.getAssignment("/TestAddress").getBaseAssignmentId(), CacheAdmin.environment.getAssignment("CONTACTDATA/ADDRESS").id)
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
            Assert.assertFalse(type.isUsePermissions())
            Assert.assertFalse(type.isUseInstancePermissions())
            Assert.assertFalse(type.isUsePropertyPermissions())
            Assert.assertFalse(type.isUseStepPermissions())
            Assert.assertFalse(type.isUseTypePermissions())
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useInstancePermissions: true)
            Assert.assertTrue(getTestType().isUseInstancePermissions())
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useInstancePermissions: false)
            Assert.assertFalse(getTestType().isUseInstancePermissions())
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(usePropertyPermissions: true)
            Assert.assertTrue(getTestType().isUsePropertyPermissions())
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(usePropertyPermissions: false)
            Assert.assertFalse(getTestType().isUsePropertyPermissions())
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useStepPermissions: true)
            Assert.assertTrue(getTestType().isUseStepPermissions())
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useStepPermissions: false)
            Assert.assertFalse(getTestType().isUseStepPermissions())
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useTypePermissions: true)
            Assert.assertTrue(getTestType().isUseTypePermissions())
        } finally {
            removeTestType()
        }
        try {
            new GroovyTypeBuilder().builderTest(useTypePermissions: false)
            Assert.assertFalse(getTestType().isUseTypePermissions())
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
            if (!(e.converted != null && e.converted instanceof FxInvalidParameterException                                   \
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
            if (!(e.converted != null && e.converted instanceof FxInvalidParameterException                                   \
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

    /**
     * Test creation of properties with all available typebuilder options
     * Change assignment of a newly created property by calling the builder for the same type / prop
     * Also tests changes to a prop assignment after setting an alias
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def propertyArgumentsTest() {
        // create custom ACL
        def aclEngine = EJBLookup.getAclEngine()
        def aclId = aclEngine.create("Testicus ACL", new FxString(true, "TESTICUS"), TestUsers.getTestMandator(), "#000000", "Testicus ACL Description", ACLCategory.STRUCTURE);
        def acl = environment().getACL(aclId);

        try {
            new GroovyTypeBuilder().builderTest(languageMode: LanguageMode.Single,
                    trackHistory: true,
                    typeMode: TypeMode.Content,
                    maxVersions: 10L) {
                testprop1unique(label: new FxString(true, "TestProp1"),
                        description: new FxString(true, "TestProp1Description"), // should be overwritten if a label is given
                        hint: new FxString(true, "TestProp1_Hint"),
                        alias: "TestProp1Alias", // will overwrite "testprop1unique" in the xPath
                        defaultMultiplicity: 3,
                        multiplicity: FxMultiplicity.MULT_0_N,
                        dataType: FxDataType.String1024,
                        defaultValue: new FxString(true, "A default value"),
                        multilang: true,
                        acl: acl.getName(),
                        multiline: false,
                        inOverview: false,
                        searchable: false,
                        useHtmlEditor: false,
                        maxLength: 20,
                        overrideACL: true,
                        overrideMultiplicity: true,
                        overrideInOverview: true,
                        overrideMaxLength: true,
                        overrideMultiline: true,
                        overrideSearchable: true,
                        overrideUseHtmlEditor: true,
                        overrideMultilang: true,
                        autoUniquePropertyName: true, // actually not the right place to test this
                        fullTextIndexed: true,
                        uniqueMode: UniqueMode.Type)

                testprop2derived(
                        assignment: "BUILDERTEST/TESTPROP1ALIAS",
                        label: new FxString(true, "TestProp2Derived"),
                        hint: new FxString(true, "TestProp2Derived Hint"),
                        alias: "TestProp2Derived",
                        defaultMultiplicity: 1,
                        dataType: FxDataType.Number, // should be ignored
                        acl: environment().getACL(ACLCategory.STRUCTURE.getDefaultId()), // should also work for an actual ACL object
                        defaultValue: new FxString("TestProp2 default value"),
                        multiplicity: FxMultiplicity.MULT_0_1,
                        defaultLanguage: FxLanguage.ENGLISH,
                        enabled: true,
                        multiline: true,
                        inOverview: true,
                        useHtmlEditor: true,
                        maxLength: 100,
                        multilang: true,
                        "A CUSTOM OPTION": 1000)
            }

            // asserts
            FxType t = CacheAdmin.getEnvironment().getType("BUILDERTEST")
            Assert.assertTrue(t.isTrackHistory())
            Assert.assertEquals(t.getMaxVersions(), 10L)
            Assert.assertEquals(t.getMode(), TypeMode.Content)

            FxProperty p = environment().getProperty("TESTPROP1UNIQUE")
            FxPropertyAssignment pa_orig = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/TESTPROP1ALIAS")
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/TESTPROP1UNIQUE"))
            FxPropertyAssignment pa_der = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/TESTPROP2DERIVED")
            FxStructureOption opt;

            Assert.assertEquals(p.getLabel().toString(), "TestProp1")
            Assert.assertEquals(p.getHint().toString(), "TestProp1_Hint")
            Assert.assertEquals(pa_orig.getAlias(), "TESTPROP1ALIAS")
            Assert.assertEquals(pa_orig.getDefaultMultiplicity(), 3)
            Assert.assertEquals(p.getDataType(), FxDataType.String1024)
            Assert.assertEquals(p.getDefaultValue().toString(), "A default value")
            Assert.assertTrue(p.isMultiLang())
            Assert.assertEquals(p.getACL().getName(), "Testicus ACL")
            Assert.assertFalse(p.isMultiLine())
            Assert.assertFalse(p.isInOverview())
            Assert.assertFalse(p.isSearchable())
            Assert.assertFalse(p.isUseHTMLEditor())
            Assert.assertEquals(p.getMaxLength(), 20)
            Assert.assertTrue(p.mayOverrideACL())
            Assert.assertTrue(p.mayOverrideBaseMultiplicity())
            Assert.assertTrue(p.isFulltextIndexed())
            Assert.assertEquals(p.getUniqueMode(), UniqueMode.Type)

            // structure options
            opt = p.getOption("SHOW.OVERVIEW");
            Assert.assertTrue(opt.isOverrideable())
            opt = p.getOption("MAXLENGTH")
            Assert.assertTrue(opt.isOverrideable())
            opt = p.getOption("MULTILINE")
            Assert.assertTrue(opt.isOverrideable())
            opt = p.getOption("SEARCHABLE")
            Assert.assertTrue(opt.isOverrideable())
            opt = p.getOption("HTML.EDITOR")
            Assert.assertTrue(opt.isOverrideable())
            opt = p.getOption("MULTILANG")
            Assert.assertTrue(opt.isOverrideable())

            // derived assignment
            Assert.assertEquals(pa_der.getProperty(), p)
            Assert.assertEquals(pa_der.getLabel().toString(), "TestProp2Derived")
            Assert.assertEquals(pa_der.getHint().toString(), "TestProp2Derived Hint")
            Assert.assertEquals(pa_der.getAlias(), "TESTPROP2DERIVED")
            Assert.assertEquals(pa_der.getDefaultMultiplicity(), 1)
            Assert.assertEquals(pa_der.getACL().getName(), "Default Structure ACL")
            Assert.assertEquals(pa_der.getDefaultValue().toString(), "TestProp2 default value")
            Assert.assertEquals(pa_der.getMultiplicity(), FxMultiplicity.MULT_0_1)
            Assert.assertEquals(pa_der.getDefaultLanguage(), FxLanguage.ENGLISH)
            Assert.assertTrue(pa_der.isEnabled())
            Assert.assertTrue(pa_der.isMultiLine())
            Assert.assertTrue(pa_der.isInOverview())
            Assert.assertTrue(pa_der.isUseHTMLEditor())
            Assert.assertTrue(pa_der.isMultiLang())
            Assert.assertEquals(pa_der.getMaxLength(), 100)
            opt = pa_der.getOption("A CUSTOM OPTION")
            Assert.assertEquals(opt.getValue(), "1000")

            // change the assignment of testprop1unique: label, hint, default value and ACL
            def builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                testprop1unique(label: new FxString(true, "TestProp1NewLabel"),
                        hint: new FxString(true, "TestProp1NewHint"),
                        defaultValue: new FxString(true, "A new default value"),
                        acl: "Default Structure ACL")
            }

            pa_orig = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/TESTPROP1ALIAS")
            Assert.assertEquals(pa_orig.getLabel().toString(), "TestProp1NewLabel")
            Assert.assertEquals(pa_orig.getHint().toString(), "TestProp1NewHint")
            Assert.assertEquals(pa_orig.getDefaultValue().toString(), "A new default value")
            Assert.assertEquals(pa_orig.getACL().getName(), "Default Structure ACL")
        } finally {
            removeTestType()
            aclEngine.remove(aclId);
        }
    }

    /**
     * Test group creation, group changes, equality of assignments for calls to the builder w/o changes,
     * mixture of nodes, mixing calls w/ and w/o changes
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def groupAssignmentTest() {
        def aclEngine = EJBLookup.getAclEngine()
        def aclId = aclEngine.create("Testicus ACL", new FxString(true, "TESTICUS"), TestUsers.getTestMandator(), "#000000", "Testicus ACL Description", ACLCategory.STRUCTURE);
        def acl = environment().getACL(aclId);
        try {
            new GroovyTypeBuilder().builderTest(
                    label: new FxString(true, "A buildertest"),
                    acl: "Testicus ACL") {
                prop1()
                Group1(label: new FxString(true, "Group 1"),
                        acl: "Testicus ACL", // should be ignored
                        defaultMultiplicity: 3,
                        multiplicity: FxMultiplicity.MULT_0_N) {
                    prop2()
                }
                Group2() {
                    prop1(assignment: "BUILDERTEST/PROP1",
                            label: new FxString(true, "Prop1 derived"))
                    Group3() {
                        Group1(assignment: "BUILDERTEST/GROUP1",
                                label: new FxString(true, "Group1 derived"))
                    }
                }
                prop3(overrideACL: true, acl: "Default Structure ACL")
                Group4() {
                    prop3(assignment: "BUILDERTEST/PROP3",
                            label: new FxString(true, "Prop3 derived"),
                            acl: acl)
                }
            }

            // asserts
            FxType t = environment().getType("BUILDERTEST")
            Assert.assertEquals(t.getLabel().toString(), "A buildertest")
            Assert.assertEquals(t.getACL().getName(), "Testicus ACL")
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/PROP1"))
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/PROP1").getLabel().toString(), "Prop1")

            FxGroup g = environment().getGroup("GROUP1");
            FxGroupAssignment ga = (FxGroupAssignment) environment().getAssignment("BUILDERTEST/GROUP1")
            Assert.assertEquals(g.getLabel().toString(), "Group 1")
            Assert.assertEquals(g.getMultiplicity(), FxMultiplicity.MULT_0_N)
            Assert.assertEquals(ga.getDefaultMultiplicity(), 3)
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/PROP2"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP2"))

            FxPropertyAssignment pa = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/GROUP2/PROP1")
            Assert.assertEquals(pa.getLabel().toString(), "Prop1 derived")
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP2/GROUP3"));

            ga = (FxGroupAssignment) environment().getAssignment("BUILDERTEST/GROUP2/GROUP3/GROUP1")
            Assert.assertEquals(ga.getLabel().toString(), "Group1 derived")

            FxProperty p = environment().getProperty("PROP3")
            Assert.assertTrue(p.mayOverrideACL())
            Assert.assertEquals(p.getACL().getName(), "Default Structure ACL")
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP4"))

            pa = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/GROUP4/PROP3")
            Assert.assertEquals(pa.getLabel().toString(), "Prop3 derived")
            Assert.assertEquals(pa.getACL().getName(), "Testicus ACL")

            // make changes to the various assignments
            // call the builder multiple times (w/ and w/o changes to various groups / props / all of them)
            // also mix the order of the groups
            def builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1(label: new FxString(true, "Prop1 new label"))
                Group1(label: new FxString(true, "Group1 new label"))
                Group2 {
                    Group3(label: new FxString(true, "Group3 new label")) {
                        Group1(label: new FxString(true, "Group1 derived new label"))
                    }
                }
            }

            t = environment().getType("BUILDERTEST")
            pa = getPropertyAssignment(t, "BUILDERTEST/PROP1")
            Assert.assertEquals(pa.getLabel().toString(), "Prop1 new label")

            ga = getGroupAssignment(t, "BUILDERTEST/GROUP1")
            Assert.assertEquals(ga.getLabel().toString(), "Group1 new label")

            ga = getGroupAssignment(t, "BUILDERTEST/GROUP2/GROUP3")
            Assert.assertEquals(ga.getLabel().toString(), "Group3 new label")

            ga = getGroupAssignment(t, "BUILDERTEST/GROUP2/GROUP3/GROUP1")
            Assert.assertEquals(ga.getLabel().toString(), "Group1 derived new label")

            // change assignments
            builder {
                Group4(label: new FxString(true, "Group4 new label")) {
                    prop3(label: new FxString(true, "Prop3 derived new label"),
                            acl: "Default Structure ACL")
                }
                Group1() {
                    prop2(label: new FxString(true, "Prop2 new label"))
                }
            }

            t = environment().getType("BUILDERTEST")
            ga = getGroupAssignment(t, "BUILDERTEST/GROUP4")
            Assert.assertEquals(ga.getLabel().toString(), "Group4 new label")

            pa = getPropertyAssignment(t, "BUILDERTEST/GROUP4/PROP3")
            Assert.assertEquals(pa.getLabel().toString(), "Prop3 derived new label")
            Assert.assertEquals(pa.getACL().getName(), "Default Structure ACL")

            pa = getPropertyAssignment(t, "BUILDERTEST/GROUP1/PROP2")
            Assert.assertEquals(pa.getLabel().toString(), "Prop2 new label")

            // change assignments
            builder {
                Group2(label: new FxString(true, "Group2")) {
                    Group3(label: new FxString(true, "Group3")) {
                        Group1(label: new FxString(true, "Group1 derived"))
                    }
                }
            }

            t = environment().getType("BUILDERTEST")
            def ga1 = getGroupAssignment(t, "BUILDERTEST/GROUP2")
            Assert.assertEquals(ga1.getLabel().toString(), "Group2")

            def ga2 = getGroupAssignment(t, "BUILDERTEST/GROUP2/GROUP3")
            Assert.assertEquals(ga2.getLabel().toString(), "Group3")

            def ga3 = getGroupAssignment(t, "BUILDERTEST/GROUP2/GROUP3/GROUP1")
            Assert.assertEquals(ga3.getLabel().toString(), "Group1 derived")


            /**
             * Test equality - groups (assignments) can be called w/o being changed
             */
            def builderNoChange = new GroovyTypeBuilder("BUILDERTEST")
            builderNoChange {
                Group1 {
                }
                Group2 {
                    Group3 {
                        Group1 {
                        }
                    }
                }
            }

            def t2 = environment().getType("BUILDERTEST")
            def ga1NoChange = getGroupAssignment(t2, "BUILDERTEST/GROUP2")
            def ga2NoChange = getGroupAssignment(t2, "BUILDERTEST/GROUP2/GROUP3")
            def ga3NoChange = getGroupAssignment(t2, "BUILDERTEST/GROUP2/GROUP3/GROUP1")

            Assert.assertTrue(groupAssignmentEquality(ga1, ga1NoChange))
            Assert.assertTrue(groupAssignmentEquality(ga2, ga2NoChange))
            Assert.assertTrue(groupAssignmentEquality(ga3, ga3NoChange))

        } finally {
            removeTestType()
            aclEngine.remove(aclId);
        }
    }



    /**
     * Compares FxAssignmentfields (i.e. their string concatenation), since the equals / toString method
     * only yield the repect. id or xpath
     *
     * @param a FxAssignment
     * @param b FxAssignment
     */
    def boolean groupAssignmentEquality(FxGroupAssignment a, FxGroupAssignment b) {
        def s1 = new StringBuilder(200)
        def s2 = new StringBuilder(200)
        s1.append(a.getLabel().toString()).append(a.getHint().toString()).append(a.getId()).append(a.getAlias()).append(a.getDefaultMultiplicity()).append(a.getDisplayName()).append(a.getDisplayLabel()).append(a.getMultiplicity()).append(a.getMode())

        s2.append(b.getLabel().toString()).append(b.getHint().toString()).append(b.getId()).append(b.getAlias()).append(b.getDefaultMultiplicity()).append(b.getDisplayName()).append(b.getDisplayLabel()).append(b.getMultiplicity()).append(a.getMode())

        return s1.toString().equalsIgnoreCase(s2.toString())
    }

    /**
     * Test creation of props / assignments within existing groups
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def groupChildAssignmentTest() {
        try {
            // let's create a type with several (sub) groups and one property in the root
            new GroovyTypeBuilder().builderTest {
                prop1()
                Group1 {
                    Group2 {
                        Group3 {
                        }
                    }
                }
                Group4 {
                    Group5 {
                        Group6 {
                        }
                    }
                }
            }

            // let's enhance the type
            def builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1 {
                    prop2()
                    Group2 {
                        Group3 {
                            prop1der(assignment: "BUILDERTEST/PROP1")
                        }
                    }
                }
                Group4 {
                    Group5 {
                        Group6 {
                            prop2der(assignment: "BUILDERTEST/GROUP1/PROP2")
                        }
                    }
                }
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/PROP2"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2/GROUP3/PROP1DER"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP4/GROUP5/GROUP6/PROP2DER"))

            // change a parent group's attribs and create a new subgroup
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group4(label: new FxString(true, "Group4 label")) {
                    NewSubGroup(label: new FxString(true, "SubGroup"))
                }
            }

            Assert.assertEquals(environment().getAssignment("BUILDERTEST/GROUP4").getLabel().toString(), "Group4 label")
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/GROUP4/NEWSUBGROUP").getLabel().toString(), "SubGroup")

            // rename again and add a new property and a new group
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group4 {
                    NewSubGroup(label: new FxString(true, "SubGroup new label")) {
                        prop3()
                        SubGroup2() {
                            prop4()
                        }
                    }
                }
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP4/NEWSUBGROUP/PROP3"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP4/NEWSUBGROUP/SUBGROUP2"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP4/NEWSUBGROUP/SUBGROUP2/PROP4"))

            // mixture of calling and creating / deriving props / groups
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1 {
                    prop5()
                    prop1der(assignment: "BUILDERTEST/PROP1")
                    Group2(label: new FxString(true, "Group2 new label"))
                    Group2a {
                        prop5der(assignment: "BUILDERTEST/GROUP1/PROP5")
                        Group4der(assignment: "BUILDERTEST/GROUP4") {
                            prop6()
                        }
                    }
                }
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/PROP5"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/PROP1DER"))
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/GROUP1/GROUP2").getLabel().toString(), "Group2 new label")
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2A"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2A/PROP5DER"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2A/GROUP4DER"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2A/GROUP4DER/PROP6"))

            // call derived groups again and create more structures
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1 {
                    Group2a {
                        Group4der {
                            Group7der(assignment: "BUILDERTEST/GROUP1/GROUP2A/GROUP4DER") {
                                Group8 {
                                    Group9der(assignment: "BUILDERTEST/GROUP1")
                                }
                            }
                        }
                    }
                    Group10(assignment: "BUILDERTEST/GROUP1/GROUP2A")
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            removeTestType()
        }
    }

    /**
     * Create a simple type and derive a new one
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def derivedTypeTest() {
        try {
            new GroovyTypeBuilder().builderBase {
                prop1()
                Group1()
            }
            new GroovyTypeBuilder().builderTest(parentTypeName: "BUILDERBASE") {
                prop2()
                Group2()
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERBASE/PROP1"));
            Assert.assertTrue(environment().assignmentExists("BUILDERBASE/GROUP1"));

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/PROP1"));
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1"));
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/PROP2"));
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP2"));

            // add a subgroup to the base
            def builder = new GroovyTypeBuilder("BUILDERBASE")
            builder {
                Group1 {
                    SubGroup1()
                }
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERBASE/GROUP1/SUBGROUP1"))
            // should be reflected in the derived type
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/SUBGROUP1"))

            // add a subgroup to the derived type
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1 {
                    SubGroup1 {
                        SubGroup2()
                    }
                }
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/SUBGROUP1/SUBGROUP2"))
            Assert.assertFalse(environment().assignmentExists("BUILDERBASE/GROUP1/SUBGROUP1/SUBGROUP2"))

        } finally {
            removeTestType()
            try {
                EJBLookup.getTypeEngine().remove(CacheAdmin.getEnvironment().getType("builderBase").id)
            } catch (FxRuntimeException e) {
                // ignore
            }
        }
    }



    /**
     * Tests the various options / attributes for a group (assignment)
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def groupAttributesTest() {
        try {
            new GroovyTypeBuilder().builderTest {
                Group1(defaultMultiplicity: 0,
                        overrideMultiplicity: true,  // this is the default
                        groupMode: "OneOf",
                        multiplicity: FxMultiplicity.MULT_0_N)
            }

            FxGroup g = environment().getGroup("Group1")
            Assert.assertEquals(g.getMultiplicity(), FxMultiplicity.MULT_0_N)
            Assert.assertTrue(g.mayOverrideBaseMultiplicity())

            FxGroupAssignment ga = (FxGroupAssignment) environment().getAssignment("BUILDERTEST/GROUP1")
            Assert.assertEquals(ga.getDefaultMultiplicity(), 0)
            Assert.assertEquals(ga.getMode(), GroupMode.OneOf)

            // change the assignment
            def builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1(hint: new FxString(true, "A hint"),
                        groupMode: "AnyOf",
                        defaultMultiplicity: 1,
                        multiplicity: FxMultiplicity.MULT_0_1)
            }

            ga = (FxGroupAssignment) environment().getAssignment("BUILDERTEST/GROUP1")
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1")) // assert the xpath was changed
            Assert.assertEquals(ga.getHint().toString(), "A hint")
            Assert.assertEquals(ga.getGroup().getId(), g.getId())
            Assert.assertEquals(ga.getDefaultMultiplicity(), 1)
            Assert.assertEquals(ga.getMultiplicity(), FxMultiplicity.MULT_0_1)
            Assert.assertEquals(ga.getMode(), GroupMode.AnyOf)

            // test setting the multiplicity as a String obj
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1(multiplicity: "0,5")
            }

            ga = (FxGroupAssignment) environment().getAssignment("BUILDERTEST/GROUP1")
            Assert.assertEquals(ga.getMultiplicity().getMin(), 0)
            Assert.assertEquals(ga.getMultiplicity().getMax(), 5)

            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder { // arbitrary spacing
                Group1(multiplicity: " 0  ,  7    ")
            }

            ga = (FxGroupAssignment) environment().getAssignment("BUILDERTEST/GROUP1")
            Assert.assertEquals(ga.getMultiplicity().getMin(), 0)
            Assert.assertEquals(ga.getMultiplicity().getMax(), 7)

        } finally {
            removeTestType()
        }
    }

    /**
     * Test settings type / list references f. properties
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def referenceTest() {
        try {
            // ty[es
            new GroovyTypeBuilder().builderTest {
                // as a String
                prop1(dataType: FxDataType.Reference,
                        referencedType: "ROOT")
            }
            def pa = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/PROP1")
            Assert.assertEquals(pa.getProperty().getDataType(), FxDataType.Reference)
            Assert.assertEquals(pa.getProperty().getReferencedType(), CacheAdmin.getEnvironment().getType("ROOT"))
            removeTestType()


            new GroovyTypeBuilder().builderTest {
                // as an actual FxType
                prop1(dataType: FxDataType.Reference,
                        referencedType: "ROOT")
            }
            pa = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/PROP1")
            Assert.assertEquals(pa.getProperty().getDataType(), FxDataType.Reference)
            Assert.assertEquals(pa.getProperty().getReferencedType(), CacheAdmin.getEnvironment().getType("ROOT"))
            removeTestType()

            // lists
            new GroovyTypeBuilder().builderTest {
                prop1(dataType: FxDataType.SelectOne,
                        referencedList: "COUNTRIES")
            }
            pa = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/PROP1")
            Assert.assertEquals(pa.getProperty().getDataType(), FxDataType.SelectOne)
            Assert.assertEquals(pa.getProperty().getReferencedList(), CacheAdmin.getEnvironment().getSelectList("COUNTRIES"))
            removeTestType()

            // lists
            new GroovyTypeBuilder().builderTest {
                prop1(dataType: FxDataType.SelectMany,
                        referencedList: CacheAdmin.getEnvironment().getSelectList("COUNTRIES"))
            }
            pa = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/PROP1")
            Assert.assertEquals(pa.getProperty().getDataType(), FxDataType.SelectMany)
            Assert.assertEquals(pa.getProperty().getReferencedList(), CacheAdmin.getEnvironment().getSelectList("COUNTRIES"))

        } finally {
            removeTestType()
        }
    }

    /**
     * Tests alias changes for assignments
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def aliasChangeTest() {
        try {
            new GroovyTypeBuilder().builderTest {
                prop1()
                Group1 {
                    prop2()
                    Group2 {
                        prop2(assignment: "BUILDERTEST/GROUP1/PROP2")
                    }
                    prop3()
                }
            }
            def builder = new GroovyTypeBuilder("BUILDERTEST")
            builder { // let's start out simple
                prop1(alias: "prop1alias")
            }
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/PROP1"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/PROP1ALIAS"))
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder { // let's start out simple
                Group1(alias: "group1alias")
            }
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS"))

            // test the walkthrough
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder { // let's start out simple
                Group1 {
                    Group2 {
                        prop2(alias: "PROP2ALIAS")
                    }
                }
            }

            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS/GROUP2/PROP2"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS/GROUP2/PROP2ALIAS"))

            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder { // let's start out simple
                Group1 {
                    Group2(alias: "group2alias")
                    prop2(alias: "PROP2ALIAS")
                }
            }

            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS/GROUP2"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS/GROUP2ALIAS"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS/PROP2"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS/PROP2ALIAS"))

            // walk through aliased groups/props
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1 {
                    Group2 {
                        Group3() // create a new group
                    }
                }
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS/GROUP2ALIAS/GROUP3"))

            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1 {
                    Group2 {
                        Group3(alias: "group3alias")
                    }
                }
                Group4(assignment: "BUILDERTEST/GROUP1ALIAS/GROUP2ALIAS") {
                    prop4(alias: "prop4alias") // immediate assignment
                }
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS/GROUP2ALIAS/GROUP3ALIAS"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS/GROUP2ALIAS/GROUP3"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP4"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP4/PROP4ALIAS"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP4/PROP4"))

            // reverse all aliases
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1(alias: "group1") // we start out simple
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1ALIAS"))

            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder { // we un-alias all elements and set a diff. alias for Group4 and the orig. prop2 and prop3
                Group1 {
                    prop2(alias: "prop2alias")
                    Group2(alias: "group2") {
                        prop2(alias: "prop2")
                        Group3(alias: "group3")
                    }
                    prop3(alias: "prop3alias")
                }
                Group4(alias: "group4alias") {
                    prop4(alias: "prop4")
                }
                prop1(alias: "prop1") // arbitrary order
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/PROP2ALIAS"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1/PROP2"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2/PROP2"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2/PROP2ALIAS"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2/GROUP3"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2/GROUP3ALIAS"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/PROP3ALIAS"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP1/PROP3"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP4ALIAS/PROP4"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUP4ALIAS/PROP4ALIAS"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/PROP1"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/PROP1ALIAS"))

            // add items to the structure and assign the aliases immediately
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                GroupX(alias: "groupXalias") {
                    propX(alias: "propXalias")
                    propZ(assignment: "BUILDERTEST/PROP1", alias: "propZalias")
                    GroupZ(alias: "groupZalias", assignment: "BUILDERTEST/GROUPXALIAS")
                }
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUPXALIAS"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUPX"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUPXALIAS/PROPXALIAS"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUPXALIAS/PROPX"))
            Assert.assertFalse(environment().assignmentExists("BUILDERTEST/GROUPXALIAS/PROPZ"))
            Assert.assertTrue(environment().getAssignment("BUILDERTEST/GROUPXALIAS/PROPZALIAS").isDerivedAssignment())
            Assert.assertTrue(environment().getAssignment("BUILDERTEST/GROUPXALIAS/GROUPZALIAS").isDerivedAssignment())

        } finally {
            removeTestType()
        }
    }

    /**
     * Tests creating properties groups which already exist within other structures
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def existingStructuretest() {
        try {
            new GroovyTypeBuilder().testicus {
                prop1()
                Group1 {
                    prop2()
                    Group2() {
                        prop3()
                    }
                }
            }

            // create a structure which derives all elements from "TESTICUS" and also has a few of its own
            // also test assignments by loading FxAssignments from the cache
            new GroovyTypeBuilder().builderTest {
                prop1(assignment: "TESTICUS/PROP1")
                propX()
                Group1(assignment: "TESTICUS/GROUP1") {
                    prop2(assignment: CacheAdmin.getEnvironment().getAssignment("TESTICUS/GROUP1/PROP2"))
                    propY()
                    Group2(assignment: "TESTICUS/GROUP1/GROUP2") {
                        prop3(assignment: CacheAdmin.getEnvironment().getAssignment("TESTICUS/GROUP1/GROUP2/PROP3"))
                        Group3 {
                            propZ()
                        }
                    }
                }
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/PROP1"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/PROPX"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/PROP2"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/PROPY"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2/PROP3"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/GROUP2/GROUP3/PROPZ"))

            def g2 = (FxGroupAssignment) environment().getAssignment("BUILDERTEST/GROUP1/GROUP2")
            def g2List = environment().getGroupAssignments(g2.getGroup().getId(), true)
            Assert.assertTrue(g2List.size() == 2)

            def builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1(label: new FxString(true, "Prop1 new label"))
                propX(label: new FxString(true, "PropX new label"))
                Group1(label: new FxString(true, "Group1 new label")) {
                    prop2(label: new FxString(true, "Prop2 new label"))
                    Group2(label: new FxString(true, "Group2 new label")) {
                        prop3(label: new FxString(true, "Prop3 new label"))
                        Group3(label: new FxString(true, "Group3 new label"))
                    }
                }
            }

            Assert.assertEquals(environment().getAssignment("BUILDERTEST/PROP1").getLabel().toString(), "Prop1 new label")
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/PROPX").getLabel().toString(), "PropX new label")
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/GROUP1").getLabel().toString(), "Group1 new label")
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/GROUP1/PROP2").getLabel().toString(), "Prop2 new label")
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/GROUP1/GROUP2").getLabel().toString(), "Group2 new label")
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/GROUP1/GROUP2/PROP3").getLabel().toString(), "Prop3 new label")
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/GROUP1/GROUP2/GROUP3").getLabel().toString(), "Group3 new label")

        } finally {
            removeTestType()
            try {
                EJBLookup.getTypeEngine().remove(CacheAdmin.getEnvironment().getType("testicus").id)
            } catch (FxRuntimeException e) {
                // not expected
            }
        }
    }

    /**
     * Tests a walk through a structure
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def structureWalkthroughTest() {
        try {
            new GroovyTypeBuilder().builderTest {
                propA()
                GroupA {
                    propB()
                    GroupB() {
                        propC()
                    }
                    propD()
                }
                GroupC {
                    propE()
                }
                propF()
            }

            def FxType before = environment().getType("BUILDERTEST")
            def FxType after;

            // load and do the walkthrough
            try {
                def builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    propF()
                    GroupC {
                        propE()
                    }
                    GroupA {
                        GroupB {
                            propC()
                        }
                        propB()
                        propD()
                    }
                    propA()
                }

                after = environment().getType("BUILDERTEST")
                def s1 = new StringBuilder(200);
                def s2 = new StringBuilder(200);
                s1.append(before.getLabel().toString()).append(before.getAssignedProperties().toString()).append(before.getAssignmentGroups().toString()).append(before.getUniqueProperties().toString())

                s2.append(after.getLabel().toString()).append(after.getAssignedProperties().toString()).append(after.getAssignmentGroups().toString()).append(after.getUniqueProperties().toString())

                Assert.assertEquals(s1.toString(), s2.toString());

            } catch (Exception e) {
                // shouldn't happen
            }
        } finally {
            removeTestType()
        }
    }



    /**
     * Tests the automatic setting of a translation to the default FxLanguage in case it is not present
     * (for "label" and "hint")
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def defaultTranslationTest() {
        new GroovyTypeBuilder().builderTest {
            prop1(label: new FxString(true, FxLanguage.GERMAN, "Ein Label"))
            prop2(label: new FxString(true, "A label"))
        }

        try {
            def p1 = environment().getProperty("PROP1")
            def p2 = environment().getProperty("PROP2")
            Assert.assertEquals(p1.getLabel().getTranslation(FxLanguage.GERMAN).toString(), "Ein Label")
            // Assert.assertEquals(p1.getLabel().getDefaultLanguage(), 2) // commented until fixed
            Assert.assertEquals(p1.getLabel().getTranslation(FxLanguage.DEFAULT_ID).toString(), "Ein Label")
            Assert.assertEquals(p1.getLabel().getTranslation(FxLanguage.ENGLISH).toString(), "Ein Label")
            Assert.assertEquals(p2.getLabel().getDefaultLanguage(), FxLanguage.DEFAULT_ID);
        } finally {
            removeTestType()
        }
    }

    /**
     * Test the creation of properties having the same name within the same type (on various
     * hierarchical levels)
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def createUniquePropsTest() {
        new GroovyTypeBuilder().builderTest {
            prop1()
            prop1(dataType: FxDataType.Number, alias: "prop1alias")
        }

        try {
            Assert.assertTrue(environment().propertyExists("PROP1"))
            Assert.assertTrue(environment().propertyExists("PROP1_1"))

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/PROP1"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/PROP1ALIAS"))

            // this will fail
            def builder = new GroovyTypeBuilder("BUILDERTEST")
            try {
                builder {
                    prop1(overrideMultilang: false)
                }
            } catch (e) {
                // expected
            }

            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1 {
                    prop1() // this should work w/o setting an alias
                }
            }

            Assert.assertTrue(environment().propertyExists("PROP1_2"))
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/GROUP1/PROP1"))

        } finally {
            removeTestType()
        }
    }



    /**
     * This test assures the default values provided upon structure creation as given by the
     * reference documentation's chapter "GroovyTypeBuilder: Property and Group Creation - Default Values"
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def defaultValueTest() {
        new GroovyTypeBuilder().defValueTest {
            defprop1()
            defGroup1()
        }

        try {
            def FxProperty p = environment().getProperty("DEFPROP1")
            def pa = (FxPropertyAssignment) environment().getAssignment("DEFVALUETEST/DEFPROP1")
            // overrides
            Assert.assertFalse(p.mayOverrideMultiLang())
            Assert.assertTrue(p.mayOverrideACL())
            Assert.assertFalse(p.mayOverrideMultiLine())
            Assert.assertFalse(p.mayOverrideInOverview())
            Assert.assertTrue(p.mayOverrideBaseMultiplicity())
            Assert.assertFalse(p.mayOverrideMaxLength())
            Assert.assertTrue(p.mayOverrideSearchable())
            Assert.assertFalse(p.mayOverrideUseHTMLEditor())

            // attributes (assignment & base prop)
            Assert.assertFalse(p.isMultiLang())
            Assert.assertEquals(p.getDataType(), FxDataType.String1024)
            Assert.assertFalse(pa.isMultiLine())
            Assert.assertTrue(p.isFulltextIndexed())
            Assert.assertFalse(p.isInOverview())
            Assert.assertEquals(p.getLabel().getBestTranslation(), "Defprop1")
            Assert.assertEquals(p.getMultiplicity(), FxMultiplicity.MULT_0_1)
            Assert.assertEquals(p.getName(), "DEFPROP1")
            Assert.assertEquals(p.getUniqueMode(), UniqueMode.None)
            Assert.assertTrue(p.isSearchable())
            Assert.assertEquals(pa.getDefaultMultiplicity(), 1)
            Assert.assertFalse(p.isUseHTMLEditor())

        } finally {
            EJBLookup.getTypeEngine().remove(environment().getType("DEFVALUETEST").getId())
        }
    }

    /**
     * Tests the default overrides in prop assignments
     * (i.e. only throw exceptions IFF the given overridable attribute != the base property's attribute
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def assignmentOverrideTest() {
        // default values (actual attrib values in () )
        /*
        Properties:

        # overrideACL: true (acl: "Default Structure ACL")
        # overrideMultiplicity: true (multiplicity: FxMultiplicity.MULT_0_1)
        # overrideMultilang: false (multilang: false)
        # overrideMultiline: false (multiline: false)
        # overrideInOverview: false (inOverview: false)
        # overrideMaxLength: false (maxLength / not set))
        # overrideSearchable: true  (searchable: true)
        # overrideUseHtmlEditor: false (useHtmlEditor: false)

        Groups:

        #overrideMultiplicity: false (multiplicity: FxMultiplicity.MULT_0_1)
        */

        new GroovyTypeBuilder().builderTest {
            prop1(overrideMultiplicity: false, overrideACL: false, overrideSearchable: false)
            prop2(dataType: FxDataType.Text, overrideMultiplicity: true, overrideACL: true,
                    overrideMultilang: true, overrideMultiline: true, overrideInOverview: true)
            Group1()
        }

        def aclId = EJBLookup.getAclEngine().create("Testicus ACL", new FxString(true, "TESTICUS"), TestUsers.getTestMandator(), "#000000", "Testicus ACL Description", ACLCategory.STRUCTURE);

        try {
            // multiplicity /////////////////////////////////////////
            def builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1der(assignment: "BUILDERTEST/PROP1", multiplicity: FxMultiplicity.MULT_0_1)

            }
            Assert.assertTrue(removeAssignment("BUILDERTEST/PROP1DER"))

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    prop1der(assignment: "BUILDERTEST/PROP1", multiplicity: FxMultiplicity.MULT_1_1)
                }
                Assert.fail("Assigning an unoverridable value where current != orig should have failed.")
            } catch (e) { }
            Assert.assertFalse(removeAssignment("BUILDERTEST/PROP1DER"))

            // ACL /////////////////////////////////////////
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1der(assignment: "BUILDERTEST/PROP1", acl: "Default Structure ACL")
            }
            Assert.assertTrue(removeAssignment("BUILDERTEST/PROP1DER"))

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    prop1der(assignment: "BUILDERTEST/PROP1", acl: "Testicus ACL")
                }
                Assert.fail("Assigning an unoverridable value where current != orig should have failed.")
            } catch (e) { }
            Assert.assertFalse(removeAssignment("BUILDERTEST/PROP1DER"))

            // multiline /////////////////////////////////////////
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1der(assignment: "BUILDERTEST/PROP1", multiline: false)
            }
            Assert.assertTrue(removeAssignment("BUILDERTEST/PROP1DER"))

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    prop1der(assignment: "BUILDERTEST/PROP1", multiline: true)
                }
                Assert.fail("Assigning an unoverridable value where current != orig should have failed.")
            } catch (Exception e) { }
            Assert.assertFalse(removeAssignment("BUILDERTEST/PROP1DER"))

            // inOverview /////////////////////////////////////////
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1der(assignment: "BUILDERTEST/PROP1", inOverview: false)
            }
            Assert.assertTrue(removeAssignment("BUILDERTEST/PROP1DER"))

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    prop1der(assignment: "BUILDERTEST/PROP1", inOverview: true)
                }
                Assert.fail("Assigning an unoverridable value where current != orig should have failed.")
            } catch (e) { }
            Assert.assertFalse(removeAssignment("BUILDERTEST/PROP1DER"))

            // multilang /////////////////////////////////////////
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1der(assignment: "BUILDERTEST/PROP1", multilang: false)
            }
            Assert.assertTrue(removeAssignment("BUILDERTEST/PROP1DER"))

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    prop1der(assignment: "BUILDERTEST/PROP1", multilang: true)
                }
                Assert.fail("Assigning an unoverridable value where current != orig should have failed.")
            } catch (e) { }
            Assert.assertFalse(removeAssignment("BUILDERTEST/PROP1DER"))

            // searchable /////////////////////////////////////////
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1der(assignment: "BUILDERTEST/PROP1", searchable: true)

            }
            Assert.assertTrue(removeAssignment("BUILDERTEST/PROP1DER"))

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    prop1der(assignment: "BUILDERTEST/PROP1", searchable: false)
                }
                Assert.fail("Assigning an unoverridable value where current != orig should have failed.")
            } catch (e) { }
            Assert.assertFalse(removeAssignment("BUILDERTEST/PROP1DER"))

            // maxLength /////////////////////////////////////////
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1der(assignment: "BUILDERTEST/PROP1", maxLength: 0)
                // 0 is the default setting upon property initialisation
            }
            Assert.assertTrue(removeAssignment("BUILDERTEST/PROP1DER"))

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    prop1der(assignment: "BUILDERTEST/PROP1", maxLength: 50)
                }
                Assert.fail("Assigning an unoverridable value where current != orig should have failed.")
            } catch (e) { }
            Assert.assertFalse(removeAssignment("BUILDERTEST/PROP1DER"))

            // useHtmlEditor /////////////////////////////////////////
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                prop1der(assignment: "BUILDERTEST/PROP1", useHtmlEditor: false)

            }
            Assert.assertTrue(removeAssignment("BUILDERTEST/PROP1DER"))

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    prop1der(assignment: "BUILDERTEST/PROP1", useHtmlEditor: true)
                }
                Assert.fail("Assigning an unoverridable value where current != orig should have failed.")
            } catch (e) { }
            Assert.assertFalse(removeAssignment("BUILDERTEST/PROP1DER"))

            // GROUP // multiplicity /////////////////////////////////
            builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                Group1der(assignment: "BUILDERTEST/GROUP1", multiplicity: FxMultiplicity.MULT_0_1)

            }
            Assert.assertTrue(removeAssignment("BUILDERTEST/GROUP1DER"))

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    Group1der(assignment: "BUILDERTEST/GROUP1", multiplicity: FxMultiplicity.MULT_0_N)
                }
                Assert.fail("Assigning an unoverridable value where current != orig should have failed.")
            } catch (e) { }
            Assert.assertFalse(removeAssignment("BUILDERTEST/GROUP1DER"))

            // quick override test
            def p = environment().getProperty("PROP2")
            Assert.assertTrue(p.mayOverrideMultiLang())
            Assert.assertTrue(p.mayOverrideBaseMultiplicity())
            Assert.assertTrue(p.mayOverrideACL())
            Assert.assertTrue(p.mayOverrideInOverview())
            Assert.assertTrue(p.mayOverrideMultiLine())

            try {
                builder = new GroovyTypeBuilder("BUILDERTEST")
                builder {
                    prop2der(assignment: "BUILDERTEST/PROP2", multilang: true,
                            multiplicity: "0,2",
                            acl: "Testicus ACL",
                            inOverview: true,
                            multiline: true)
                }

            } catch (ex) {
                Assert.fail("Overriding the (overridable) attributes should not have failed")
            }

            def pa = (FxPropertyAssignment) environment().getAssignment("BUILDERTEST/PROP2DER")

            Assert.assertTrue(pa.isMultiLang())
            Assert.assertTrue(pa.isInOverview())
            Assert.assertTrue(pa.isMultiLine())
            Assert.assertEquals(pa.getACL().getName(), "Testicus ACL")
            Assert.assertEquals(pa.getMultiplicity().getMin(), 0)
            Assert.assertEquals(pa.getMultiplicity().getMax(), 2)

        } finally {
            removeTestType()
            EJBLookup.getAclEngine().remove(aclId);
        }
    }

    /**
     * Helper method to remove an assignment for a given XPath
     *
     * @param xPath the assignment's xpath (to be removed)
     * @return returns true if the assignment was found and removed, false otherwise
     */
    def boolean removeAssignment(xPath) {
        if (environment().assignmentExists(xPath)) {
            def id = environment().getAssignment(xPath).getId()
            EJBLookup.getAssignmentEngine().removeAssignment(id)
            return true;
        }
        return false;
    }



    /**
     * Tests the fact that the GTB can be called for an existing type
     * as if creating a new one
     */
    @Test (groups = ["ejb", "scripting", "structure"])
    def constructorCallTest() {
        new GroovyTypeBuilder().builderTest {
            p1()
        }

        try {
            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/P1"))

            new GroovyTypeBuilder().builderTest {
                p1(label: new FxString(true, "P1 label"))
                p2()
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/P2"))
            Assert.assertEquals(environment().getAssignment("BUILDERTEST/P1").getLabel().toString(), "P1 label")

            // now call it the "ordinary way" when loading a type
            def builder = new GroovyTypeBuilder("BUILDERTEST")
            builder {
                p3()
            }

            Assert.assertTrue(environment().assignmentExists("BUILDERTEST/P3"))
        } finally {
            removeTestType()
        }
    }
}