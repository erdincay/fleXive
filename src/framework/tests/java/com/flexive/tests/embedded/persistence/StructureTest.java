/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.tests.embedded.persistence;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLanguage;

import static com.flexive.shared.CacheAdmin.getEnvironment;
import static com.flexive.shared.EJBLookup.getTypeEngine;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxSelectOne;
import com.flexive.shared.value.FxValue;

import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;

import com.flexive.tests.embedded.TestUsers;
import com.google.common.collect.Lists;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.Assert;
import static org.testng.Assert.*;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.naming.Context;
import javax.transaction.UserTransaction;
import java.util.List;
import java.util.Collections;

/**
 * Structure tests
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test
public class StructureTest {

    private TypeEngine te;
    private AssignmentEngine ae;
    private ACLEngine acl;
    private ContentEngine ce;

    @BeforeClass(groups = {"ejb", "structure"})
    public void beforeClass() throws FxLookupException, FxLoginFailedException, FxAccountInUseException {
        te = getTypeEngine();
        ae = EJBLookup.getAssignmentEngine();
        acl = EJBLookup.getAclEngine();
        ce = EJBLookup.getContentEngine();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass(groups = {"ejb", "structure"})
    public void afterClass() throws Exception {
        try {
            te.remove(env().getType("ContactsTest").getId());
        } catch (FxRuntimeException e) {
            // ignore
        }
        try {
            ae.removeAssignment(env().getAssignment("ROOT/CONTACT_DATA").getId(),
                    true, false);
        } catch (FxRuntimeException e) {
            // ignore
        }
        try {
            acl.remove(env().getACL("ContactsTest").getId());
        } catch (FxRuntimeException e) {
            // ignore
        }
        logout();
    }

    @Test(groups = {"ejb", "structure"}, invocationCount = 2)
    public void typeCreateDeleteUpdate() throws Exception {
        long testId = -1;
        try {
            testId = te.save(FxTypeEdit.createNew("TestCD", new FxString("description..."),
                    env().getACL(ACLCategory.STRUCTURE.getDefaultId()), null));
            FxString desc = new FxString("Test data structure");
            desc.setTranslation(FxLanguage.GERMAN, "Testdaten Strukturen");
            FxString hint = new FxString("Hint text ...");
            FxGroupEdit ge = FxGroupEdit.createNew("TEST_DIRECT_DATA", desc, hint, true, new FxMultiplicity(0, 1));
            ae.createGroup(testId, ge, "/");
            ACL structACL = null;
            for (ACL a : env().getACLs())
                if (a.getCategory() == ACLCategory.STRUCTURE) {
                    structACL = a;
                    break;
                }
            assertTrue(structACL != null, "No available ACL for structure found!");
            FxPropertyEdit pe = FxPropertyEdit.createNew("TestDirectProperty1", desc, hint, true, new FxMultiplicity(0, 1),
                    true, structACL, FxDataType.String1024, new FxString(FxString.EMPTY),
                    true, null, null, null);
            ae.createProperty(testId, pe, "/");
            pe.setName("TestDirectProperty2");
            pe.setDataType(FxDataType.String1024);
            pe.setMultiplicity(new FxMultiplicity(1, 1));
            ae.createProperty(testId, pe, "/");
            pe.setName("TestDirectProperty3");
            pe.setDataType(FxDataType.String1024);
            pe.setMultiplicity(new FxMultiplicity(0, 5));
            ae.createProperty(testId, pe, "/");
        } catch (FxApplicationException e) {
            Assert.fail(e.getMessage());
        }
        FxTypeEdit testEdit = new FxTypeEdit(env().getType("TestCD"));
        long propCount = testEdit.getAssignedProperties().size();
        testEdit.setName("TestCDNewName");
        testEdit.setPermissions((byte) 0); //clear permissions
        testEdit.setUseInstancePermissions(true);
        testEdit.setLabel(new FxString("Changed label"));
        testEdit.setState(TypeState.Unavailable);
        te.save(testEdit);
        FxType testType = env().getType("TestCDNewName");
        assertTrue(testType.getAssignedProperties().size() == propCount, "Property count mismatch");
        assertEquals(testType.getAssignment("/TestDirectProperty2").getXPath(), "TESTCDNEWNAME/TESTDIRECTPROPERTY2",
                "Expected [TESTCDNEWNAME/TESTDIRECTPROPERTY2] but got: [" + testType.getAssignment("/TestDirectProperty2").getXPath() + "]");
        assertTrue(testType.isUsePermissions());
        assertTrue(testType.isUseInstancePermissions());
        assertTrue(!testType.isUsePropertyPermissions());
        assertTrue(!testType.isUseStepPermissions());
        assertTrue(!testType.isUseTypePermissions());
        assertTrue(testType.getState() == TypeState.Unavailable);

        try {
            assertTrue(testId == testType.getId(), "Wrong id for type!");
            te.remove(testType.getId());
            try {
                env().getType("TestCD");
                Assert.fail("TestCD could be loaded after remove!");
            } catch (Exception e) {
                //ok
            }
        } catch (FxApplicationException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Test derived types
     *
     * @throws Exception on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void derivedType() throws Exception {
        long testId = -1;
        try {
            testId = te.save(FxTypeEdit.createNew("TestCDP", new FxString("description..."), env().getACLs(ACLCategory.STRUCTURE).get(0), null));
//            testId = te.create(1, 1, 1, new ArrayList<Mandator>(2), "TestCDP",
//                    new FxString("description..."), null, false,
//                    TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                    true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
            FxString desc = new FxString("Test data structure");
            desc.setTranslation(FxLanguage.GERMAN, "Testdaten Strukturen");
            FxString hint = new FxString("Hint text ...");
            ACL structACL = null;
            for (ACL a : env().getACLs())
                if (a.getCategory() == ACLCategory.STRUCTURE) {
                    structACL = a;
                    break;
                }
            assertTrue(structACL != null, "No available ACL for structure found!");
            FxPropertyEdit pe = FxPropertyEdit.createNew("TestDirectProperty1", desc, hint, true, new FxMultiplicity(0, 1),
                    true, structACL, FxDataType.String1024, new FxString(FxString.EMPTY),
                    true, null, null, null);
            ae.createProperty(testId, pe, "/");
            FxGroupEdit ge = FxGroupEdit.createNew("TestGroup", desc, hint, true, new FxMultiplicity(0, 1));
            ae.createGroup(testId, ge, "/");
            pe.setName("TestDirectProperty2");
            pe.setMultiplicity(new FxMultiplicity(0, 1));
            ae.createProperty(testId, pe, "/TestGroup");
        } catch (FxApplicationException e) {
            Assert.fail(e.getMessage());
        }
        FxType testType = env().getType("TestCDP");
        assertTrue(testId == testType.getId(), "Wrong id for type!");

        long testDerivedId;
        testDerivedId = te.save(FxTypeEdit.createNew("TestCDDerived", new FxString("description..."),
                env().getACLs(ACLCategory.STRUCTURE).get(0), testType).setEnableParentAssignments(false));
//        testDerivedId = te.create(1, 1, 1, new ArrayList<Mandator>(2), "TestCDDerived",
//                new FxString("description..."), testType, false,
//                TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
        FxType testTypeDerived = env().getType("TestCDDerived");
        assertTrue(testTypeDerived.getId() == testDerivedId, "Derived type id does not match!");
        assertTrue(testTypeDerived.getParent().getId() == testType.getId(), "Derived types parent does not match!");
        FxAssignment dp = null;
        try {
            dp = testTypeDerived.getAssignment("/TestDirectProperty1");
        } catch (Exception e) {
            Assert.fail("Failed to retrieved derived property: " + e.getMessage());
        }
        assertTrue(!dp.isEnabled(), "Property should be disabled!");
        FxPropertyAssignmentEdit dpe = new FxPropertyAssignmentEdit((FxPropertyAssignment) dp);
        dpe.setEnabled(true);
        ae.save(dpe, false);
        testTypeDerived = env().getType("TestCDDerived");
        dp = testTypeDerived.getAssignment("/TestDirectProperty1");
        assertTrue(dp.isEnabled(), "Property should be enabled!");


        try {
            dp = testTypeDerived.getAssignment("/TestGroup/TestDirectProperty2");
        } catch (Exception e) {
            Assert.fail("Failed to retrieved derived property: " + e.getMessage());
        }
        assertTrue(!dp.isEnabled(), "Property should be disabled!");

        FxGroupAssignmentEdit gae = new FxGroupAssignmentEdit((FxGroupAssignment) testTypeDerived.getAssignment("/TestGroup"));
        gae.setEnabled(true);
        ae.save(gae, false);
        testTypeDerived = env().getType("TestCDDerived");
        dp = testTypeDerived.getAssignment("/TestGroup/TestDirectProperty2");
        assertTrue(dp.isEnabled(), "Property should be enabled!");

        try {
            te.remove(testTypeDerived.getId());
            try {
                env().getType("TestCDDerived");
                Assert.fail("TestCDDerived could be loaded after remove!");
            } catch (Exception e) {
                //ok
            }
            te.remove(testType.getId());
            try {
                env().getType("TestCDP");
                Assert.fail("TestCD could be loaded after remove!");
            } catch (Exception e) {
                //ok
            }
        } catch (FxApplicationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test(groups = {"ejb", "structure"})
    public void contactData() throws Exception {
        try {
            if (env().getType("ContactsTest") != null)
                return; //ok, exists already
        } catch (Exception e) {
            //ok, create the type
        }
        long aid = acl.create("ContactsTest", new FxString("Contact ACL"), Mandator.MANDATOR_FLEXIVE, "#AA0000", "ACL for ContactsTest", ACLCategory.STRUCTURE);
        ACL contactACL = env().getACL(aid);
        FxString desc = new FxString("Template for contact data");
        desc.setTranslation(FxLanguage.GERMAN, "Kontaktdaten Vorlage");
        FxString hint = new FxString("");
        FxGroupEdit ge = FxGroupEdit.createNew("CONTACT_DATA", desc, hint, true, new FxMultiplicity(0, FxMultiplicity.N));
        ae.createGroup(ge, "/");
        ge.setName("ADDRESS");
        desc.setTranslation(FxLanguage.ENGLISH, "Address");
        desc.setTranslation(FxLanguage.GERMAN, "Adresse");
        ge.setLabel(desc);
        ae.createGroup(ge, "/CONTACT_DATA");
        desc.setTranslation(FxLanguage.ENGLISH, "Surname");
        desc.setTranslation(FxLanguage.GERMAN, "Nachname");
        hint.setTranslation(FxLanguage.ENGLISH, "Surname of the person");
        hint.setTranslation(FxLanguage.GERMAN, "Nachname der Person");
        FxPropertyEdit pe = FxPropertyEdit.createNew("SURNAME", desc, hint, new FxMultiplicity(1, 1),
                contactACL, FxDataType.String1024).setAutoUniquePropertyName(true);
        ae.createProperty(pe, "/CONTACT_DATA");
        desc.setTranslation(FxLanguage.ENGLISH, "Name");
        desc.setTranslation(FxLanguage.GERMAN, "Vorname");
        hint.setTranslation(FxLanguage.ENGLISH, "Name of the person");
        hint.setTranslation(FxLanguage.GERMAN, "Vorname der Person");
        pe.setLabel(desc);
        pe.setHint(hint);
        pe.setName("NAME");
        ae.createProperty(pe, "/CONTACT_DATA");
        desc.setTranslation(FxLanguage.ENGLISH, "Street");
        desc.setTranslation(FxLanguage.GERMAN, "Strasse");
        pe.setLabel(desc);
        pe.setHint(desc);
        pe.setDataType(FxDataType.String1024);
        pe.setName("STREET");
        ae.createProperty(pe, "/CONTACT_DATA/ADDRESS");
        desc.setTranslation(FxLanguage.ENGLISH, "ZIP code");
        desc.setTranslation(FxLanguage.GERMAN, "Postleitzahl");
        pe.setDataType(FxDataType.String1024);
        pe.setLabel(desc);
        pe.setHint(desc);
        pe.setName("ZIP");
        ae.createProperty(pe, "/CONTACT_DATA/ADDRESS");
        desc.setTranslation(FxLanguage.ENGLISH, "City");
        desc.setTranslation(FxLanguage.GERMAN, "Stadt");
        pe.setLabel(desc);
        pe.setHint(desc);
        pe.setName("CITY");
        ae.createProperty(pe, "/CONTACT_DATA/ADDRESS");
        desc.setTranslation(FxLanguage.ENGLISH, "Country");
        desc.setTranslation(FxLanguage.GERMAN, "Land");
        pe.setLabel(desc);
        pe.setHint(desc);
        pe.setName("COUNTRY");
        ae.createProperty(pe, "/CONTACT_DATA/ADDRESS");
        te.save(FxTypeEdit.createNew("ContactsTest", new FxString("Contact data"), contactACL, null));

        FxGroupAssignment ga = (FxGroupAssignment) env().getAssignment("ROOT/CONTACT_DATA");
        FxGroupAssignmentEdit gae = FxGroupAssignmentEdit.createNew(ga, env().getType("ContactsTest"), null, "/");
        ae.save(gae, true);
    }

    @Test(groups = {"ejb", "structure"})
    public void assignmentGroupProperty() throws Exception {
        Context c = EJBLookup.getInitialContext();
        UserTransaction ut = (UserTransaction) c.lookup("java:comp/UserTransaction");
        ut.begin();
        FxString desc = new FxString("group description...");
        desc.setTranslation(2, "gruppen beschreibung");
        FxGroupEdit ge = FxGroupEdit.createNew("grouptest", desc, new FxString("hint..."), true, new FxMultiplicity(0, FxMultiplicity.N));
        ae.createGroup(ge, "/");
        ge.setName("subgroup");
        ae.createGroup(ge, "/GROUPTEST");
        ge.setName("subgroup2");
        ae.createGroup(ge, "/GROUPTEST/SUBGROUP");
        desc.setTranslation(1, "property description...");
        desc.setTranslation(2, "attribut beschreibung...");
        FxPropertyEdit pe = FxPropertyEdit.createNew("testproperty", desc, new FxString("property hint"), true, new FxMultiplicity(1, 1),
                true, env().getACL(1), FxDataType.Number, new FxString("123"),
                true, null, null, null);
        ae.createProperty(pe, "/GROUPTEST/SUBGROUP");
        FxGroupAssignment ga = (FxGroupAssignment) env().getAssignment("ROOT/GROUPTEST");
        FxGroupAssignmentEdit gae = FxGroupAssignmentEdit.createNew(ga, env().getType("ROOT"), "GTEST", "/");
        ae.save(gae, true);
        ut.rollback();
    }

    @Test(groups = {"ejb", "structure"})
    public void assignmentRemove() throws Exception {
        long testId = -1;
        ACL structACL = null;
        FxString hint = new FxString("Hint text ...");
        FxString desc = new FxString("Test data structure");
        try {
            testId = te.save(FxTypeEdit.createNew("TestAssignmentRemove", new FxString("description..."), env().getACLs(ACLCategory.STRUCTURE).get(0), null));
            desc.setTranslation(FxLanguage.GERMAN, "Testdaten Strukturen");
            structACL = null;
            for (ACL a : env().getACLs())
                if (a.getCategory() == ACLCategory.STRUCTURE) {
                    structACL = a;
                    break;
                }
            assertTrue(structACL != null, "No available ACL for structure found!");
            FxPropertyEdit pe = FxPropertyEdit.createNew("TestAssignmentRemoveProperty1", desc, hint, true, new FxMultiplicity(0, 1),
                    true, structACL, FxDataType.String1024, new FxString(FxString.EMPTY),
                    true, null, null, null);
            ae.createProperty(testId, pe, "/");
        } catch (FxApplicationException e) {
            Assert.fail(e.getMessage());
        }
        FxType testType = env().getType("TestAssignmentRemove");
        assertTrue(testId == testType.getId(), "Wrong id for type!");
        FxPropertyAssignment pa = null;
        long rmId = -1;
        try {
            pa = (FxPropertyAssignment) testType.getAssignment("/TestAssignmentRemoveProperty1");
            assertTrue(pa != null, "Created Property Assignment is null!");
            rmId = pa.getId();
        } catch (Exception e) {
            Assert.fail("Created Property Assignment does not exist!");
        }
        FxPropertyAssignmentEdit pae = FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty2", "/");
        try {
            long paId = ae.save(pae, true);
            pa = (FxPropertyAssignment) env().getAssignment(paId);
        } catch (Exception e) {
            Assert.fail("Failed to retrieve derived assignment(2): " + e.getMessage());
        }
        //create derived assignment over 3 hops
        ae.save(FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty3", "/"), true);
        //check base's
        testType = env().getType(testId);
        assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty1").getBaseAssignmentId() == FxAssignment.NO_PARENT);
        assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty2").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty1").getId());
        assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty3").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty2").getId());
        ae.removeAssignment(rmId, true, true);
        assertTrue(env().getSystemInternalRootPropertyAssignments().size() == env().getType(testId).getAssignedProperties().size(), "Expected to have " + env().getSystemInternalRootPropertyAssignments().size() + " assignments to the type!");

        //recreate
        FxPropertyEdit pe = FxPropertyEdit.createNew("TestAssignmentRemoveProperty1", desc, hint, true, new FxMultiplicity(0, 1),
                true, structACL, FxDataType.String1024, new FxString(FxString.EMPTY),
                true, null, null, null);
        long id1 = ae.createProperty(testId, pe, "/");
        pa = (FxPropertyAssignment) env().getAssignment(id1);
        long id2 = ae.save(FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty2", "/"), true);
        pa = (FxPropertyAssignment) env().getAssignment(id2);
        ae.save(FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty3", "/"), true);
        //recheck
        testType = env().getType(testId);
        assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty1").getBaseAssignmentId() == FxAssignment.NO_PARENT);
        assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty2").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty1").getId());
        assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty3").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty2").getId());

        ae.removeAssignment(testType.getAssignment("/TestAssignmentRemoveProperty1").getId(), false, false);
        testType = env().getType(testId);
        assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty2").getBaseAssignmentId() == FxAssignment.NO_PARENT);
        assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty3").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty2").getId());

        // test if derived sub-assignments of a derived group are correctly
        // dereferenced from base assignments if parent group is removed

        //create base group with assignment
        FxGroupEdit ge = FxGroupEdit.createNew("TestRemoveGroupBase", desc, hint, true, new FxMultiplicity(0, 1));
        long baseGrpId = ae.createGroup(testId, ge, "/");
        FxGroupAssignment baseGrp = (FxGroupAssignment) env().getAssignment(baseGrpId);
        FxPropertyEdit basePe = FxPropertyEdit.createNew("TestRemoveGroupBaseProperty", desc, hint, true, new FxMultiplicity(0, 1),
                true, structACL, FxDataType.String1024, new FxString(FxString.EMPTY),
                true, null, null, null);
        long baseAssId = ae.createProperty(testId, basePe, baseGrp.getXPath());

        //verify that property assignment is child of group
        FxPropertyAssignment baseAss = (FxPropertyAssignment) env().getAssignment(baseAssId);
        assertTrue(baseAss.getParentGroupAssignment().getId() == baseGrp.getId());

        //create derived group assignment with derived sub assignments
        long derivedGrpId = ae.save(FxGroupAssignmentEdit.createNew(baseGrp, env().getType(testId), "TestRemoveGroupDerived", "/"), true);
        FxGroupAssignment derivedGrp = (FxGroupAssignment) env().getAssignment(derivedGrpId);

        //verify that group is derived
        assertTrue(derivedGrp.isDerivedAssignment() && derivedGrp.getBaseAssignmentId() == baseGrp.getId());

        //verify that derived group contains derived assignment from base group
        boolean found = false;
        long derivedAssId = -1;
        for (FxAssignment a : derivedGrp.getAssignments()) {
            if (a.isDerivedAssignment() && a.getBaseAssignmentId() == baseAss.getId()) {
                found = true;
                derivedAssId = a.getId();
                break;
            }
        }
        assertTrue(found);

        //remove base group (together with its sub assignments)
        ae.removeAssignment(baseGrp.getId());

        //verify that derived group exists and has been dereferenced
        assertTrue(!env().getAssignment(derivedGrp.getId()).isDerivedAssignment() &&
                env().getAssignment(derivedGrp.getId()).getBaseAssignmentId() == FxAssignment.NO_PARENT);

        //verify that derived assignment exists and has been dereferenced
        assertTrue(!env().getAssignment(derivedAssId).isDerivedAssignment() &&
                env().getAssignment(derivedAssId).getBaseAssignmentId() == FxAssignment.NO_PARENT);

        te.remove(testId);
    }

    /**
     * Tests overriding the multilanguage setting for a property
     *
     * @throws FxNotFoundException         on errors
     * @throws FxInvalidParameterException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void overrideCaptionMultiLangTest() throws FxNotFoundException, FxInvalidParameterException {
        final FxPropertyAssignmentEdit property = FxPropertyAssignmentEdit.createNew(
                (FxPropertyAssignment) env().getAssignment("ROOT/CAPTION"),

                env().getType(FxType.CONTACTDATA), "test", "/");
        assertTrue(property.isMultiLang(), "Caption property should be multi-language by default");
        if (property.getProperty().mayOverrideMultiLang())
            assertTrue(false, "Expected Caption property to be not overrideable for this test case!");
        try {
            property.setMultiLang(false);
            assertTrue(false, "Non-overrideable option must not be overridden!");
        } catch (FxInvalidParameterException ipe) {
            //expected
        }
    }

    /**
     * Tests setting the default language values f. properties
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void defaultValueLanguageTest() throws FxApplicationException {
        FxPropertyEdit prop = FxPropertyEdit.createNew("testDefLang",
                new FxString("Test Priority"),
                new FxString("Priority"),
                FxMultiplicity.MULT_1_1,
                env().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.Number);

        FxString defML = new FxString(true, "test");
        FxString defSL = new FxString(false, "test");
        long asId = -1;

        try {
            prop.setMultiLang(false);
            try {
                prop.setDefaultValue(defML);
                asId = ae.createProperty(FxType.ROOT_ID, prop, "/");
                Assert.fail("Expected create property to fail with a multilang default value for a singlelang assignment");
            } catch (FxRuntimeException e) {
                //expected
            }
            prop.setMultiLang(true);
            try {
                prop.setDefaultValue(defSL);
                asId = ae.createProperty(FxType.ROOT_ID, prop, "/");
                Assert.fail("Expected create property to fail with a singlelang default value for a singlelang assignment");
            } catch (FxRuntimeException e) {
                //expected
            }
            prop.setMultiLang(true);
            prop.setDefaultValue(defML);
            asId = ae.createProperty(FxType.ROOT_ID, prop, "/");
            //this should work
        } finally {
            if (asId != -1)
                ae.removeAssignment(asId);
        }
    }

    /**
     * AssignmentEngineBean: #getInstanceMultiplicity (implicitly), #getAssignmentInstanceCount,
     * #getPropertyInstanceCount, #save(FxPropertyEdit), #updateProperty (implicitly), #updatePropertyOptions (implicitly)
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void instanceMultiplicityTest() throws FxApplicationException {
        SelectListEngine se = EJBLookup.getSelectListEngine();
        final String TYPE_NAME = "STRUCTTESTTYPE_" + RandomStringUtils.random(16, true, true);
        long typeId = createTestTypeAndProp(TYPE_NAME, "TESTPROP", true);
        long typeIdRefTest = createTestTypeAndProp(TYPE_NAME + "_TWO", "", false);
        long selListId = -1;
        try { // test setting some of property attributes (for db update coverage)
            FxProperty p = env().getProperty("TESTPROP");
            FxPropertyEdit pEdit = p.asEditable();
            ACL propACL = env().getACL(ACLCategory.SELECTLIST.getDefaultId());
            ACL defacl = env().getACL(ACLCategory.STRUCTURE.getDefaultId());
            FxSelectListEdit testList = FxSelectListEdit.createNew("Test_selectlist111999", new FxString("test selectlist"), new FxString("test selectlist"), false, defacl, defacl);
            FxSelectListItemEdit.createNew("ITEM1", defacl, testList, new FxString("item_1"), "value_1", "#000000");
            FxSelectListItemEdit.createNew("ITEM2", defacl, testList, new FxString("item_2"), "value_2", "#000000");
            selListId = se.save(testList);
            testList = env().getSelectList(selListId).asEditable();
            FxSelectListItem defItem = env().getSelectListItem(testList, "ITEM1");

            pEdit.setDataType(FxDataType.SelectOne);
            pEdit.setReferencedType(env().getType(typeIdRefTest));
            pEdit.setUniqueMode(UniqueMode.Type);
            pEdit.setACL(propACL);
            pEdit.setReferencedList(env().getSelectList(selListId));
            pEdit.setOverrideMultiLang(true); // also needed for assignment tests
            ae.save(pEdit);

            p = env().getProperty("TESTPROP");
            assertEquals(p.getDataType().getValueClass(), FxSelectOne.class);
            assertEquals(p.getReferencedType().getId(), typeIdRefTest);
            assertEquals(p.getUniqueMode(), UniqueMode.Type);
            assertEquals(p.getACL().getCategory(), ACLCategory.SELECTLIST);
            assertEquals(p.getReferencedList().getName(), "Test_selectlist111999");
            assertTrue(p.mayOverrideMultiLang());

            // test property assignments
            FxPropertyAssignment assProp = (FxPropertyAssignment) env().getAssignment(TYPE_NAME + "/TESTPROP");
            FxPropertyAssignmentEdit assEdit = assProp.asEditable();
            assEdit.setACL(propACL);
            assEdit.setMultiLang(true);
            assEdit.setLabel(new FxString(true, "assignment_label_123456"));
            assEdit.setDefaultLanguage(FxLanguage.ENGLISH);
            assEdit.setDefaultValue(new FxSelectOne(true, defItem));
            ae.save(assEdit, false);

            assProp = (FxPropertyAssignment) env().getAssignment(TYPE_NAME + "/TESTPROP");
            assertEquals(assProp.getACL(), propACL);
            assertTrue(assProp.isMultiLang());
            assertEquals(assProp.getLabel().toString(), "assignment_label_123456");
            assertEquals(assProp.getDefaultLanguage(), FxLanguage.ENGLISH);
            FxValue val = assProp.getDefaultValue();
            assertEquals(val.getValueClass(), FxSelectListItem.class);
            FxSelectOne selOne = (FxSelectOne) val;
            FxSelectListItem item = env().getSelectListItem(testList, "ITEM1");
            assertEquals(selOne.fromString("" + item.getId()).getData(), "value_1");
        } finally {
            // clean up
            te.remove(typeId);
            te.remove(typeIdRefTest);
            if (selListId != -1) {
                se.remove(env().getSelectList(selListId));
            }
        }
        // remaining tests for multiplicity settings etc.
        typeId = createTestTypeAndProp(TYPE_NAME, "TESTPROP", true);
        FxPK contentPK1 = createTestContent(typeId, "/TESTPROP", "Testdata 1");
        FxPK contentPK2 = createTestContent(typeId, "/TESTPROP", "Testdata 2");
        try {
            FxPropertyAssignmentEdit assEdit = ((FxPropertyAssignment) env().getAssignment(TYPE_NAME + "/TESTPROP")).asEditable();
            FxProperty p = env().getProperty("TESTPROP");
            // we should have two assignments for our testprop
            assertEquals(ae.getAssignmentInstanceCount(assEdit.getId()), 2);
            // and two instances of testprop were created
            assertEquals(ae.getPropertyInstanceCount(p.getId()), 2);
            // alter the multiplicity to 0..2 and set some other properties
            FxPropertyEdit pEdit = p.asEditable();
            pEdit.setOverrideMultiplicity(false);
            pEdit.setMultiplicity(new FxMultiplicity(0, 2));
            boolean isFulltextIndexed = pEdit.isFulltextIndexed();
            if (isFulltextIndexed) {
                pEdit.setFulltextIndexed(false);
            } else {
                pEdit.setFulltextIndexed(true);
            }
            boolean mayOverrideACL = pEdit.mayOverrideACL();
            if (mayOverrideACL) {
                pEdit.setOverrideACL(false);
            } else {
                pEdit.setOverrideACL(true);
            }
            pEdit.setDefaultValue(new FxString(false, "DefaultValue_for_TESTPROP"));
            ae.save(pEdit);

            p = env().getProperty("TESTPROP");
            assertEquals(p.getMultiplicity().getMin(), 0);
            assertEquals(p.getMultiplicity().getMax(), 2);
            assertEquals(p.isFulltextIndexed(), !isFulltextIndexed);
            assertEquals(p.mayOverrideACL(), !mayOverrideACL);
            assertEquals(p.getDefaultValue().toString(), "DefaultValue_for_TESTPROP");

            // modify the property and implicitly fire #getInstanceMultiplicity via #save
            pEdit = p.asEditable();
            pEdit.setMultiplicity(new FxMultiplicity(2, 2));
            pEdit.setOverrideMultiplicity(false);
            try {
                ae.save(pEdit);
                Assert.fail("Altering the minimum multiplicity of a property for which content exists should have thrown an exception");
            } catch (FxUpdateException e) {
                // expected
            }

            pEdit.setOverrideMultiplicity(true); // reset
            pEdit.setMultiplicity(new FxMultiplicity(0, 2));
            ae.save(pEdit);
            assEdit = ((FxPropertyAssignment) env().getAssignment(TYPE_NAME + "/TESTPROP")).asEditable();
            assEdit.setMultiplicity(new FxMultiplicity(0, 2));
            ae.save(assEdit, false);
            // create an additional content instance for the same assignment
            FxContent co = ce.load(contentPK2);
            co.setValue("/TESTPROP[2]", new FxString(false, "Testdata 2.2"));
            ce.save(co);

            pEdit = env().getProperty("TESTPROP").asEditable();
            pEdit.setOverrideMultiplicity(false);
            pEdit.setMultiplicity(new FxMultiplicity(0, 1));
            try {
                ae.save(pEdit);
                Assert.fail("Changing the maximum property multiplicity for assignment counts > new set maximum should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            pEdit.setMultiplicity(new FxMultiplicity(3, 3));
            try {
                ae.save(pEdit);
                Assert.fail("Changing the minimum property multiplicity for assignment counts > new set minimum should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            // add another content instance
            pEdit.setOverrideMultiplicity(false);
            pEdit.setMultiplicity(new FxMultiplicity(0, 3));
            ae.save(pEdit);
            co = ce.load(contentPK2);
            co.setValue("/TESTPROP[3]", new FxString(false, "Testdata 2.3"));
            ce.save(co);

            pEdit = env().getProperty("TESTPROP").asEditable();
            pEdit.setOverrideMultiplicity(false);
            pEdit.setMultiplicity(new FxMultiplicity(0, 2));
            try {
                ae.save(pEdit);
                Assert.fail("Changing the maximum property multiplicity for assignment counts > new set maximum should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            pEdit.setOverrideMultiplicity(false); // reset
            pEdit.setMultiplicity(new FxMultiplicity(0, 3));
            ae.save(pEdit);

            // test multiplicity assignment via #save(FxAssignment)
            assEdit.setMultiplicity(new FxMultiplicity(0, 5));
            try {
                ae.save(assEdit, false);
                Assert.fail("Overriding the base multiplicity should have failed due to setOverrideMultiplicity(false)");
            } catch (FxUpdateException e) {
                // expected
            }
            pEdit.setOverrideMultiplicity(true); // reset
            pEdit.setMultiplicity(new FxMultiplicity(0, 3));
            ae.save(pEdit);

            assEdit.setMultiplicity(new FxMultiplicity(0, 5));
            ae.save(assEdit, false);


            assEdit = ((FxPropertyAssignment) env().getAssignment(TYPE_NAME + "/TESTPROP")).asEditable();
            assertEquals(assEdit.getMultiplicity().getMin(), 0);
            assertEquals(assEdit.getMultiplicity().getMax(), 5);

            assEdit.setMultiplicity(new FxMultiplicity(4, 4));
            try {
                ae.save(assEdit, false);
                Assert.fail("Assignment of an invalid minimum FxMultiplicity of a property assignment for which content exists should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            // call the #save function again w/ changes to the property's attributes
            pEdit.setLabel(new FxString(true, FxLanguage.DEFAULT.getId(), "TESTPROP new label"));
            pEdit.setMultiplicity(new FxMultiplicity(0, 5));
            pEdit.setOverrideMultiplicity(true);
            pEdit.setHint(new FxString(true, FxLanguage.DEFAULT.getId(), "TESTPROP hint"));
            ae.save(pEdit);

            p = env().getProperty(p.getId()); // reload from cache
            assertEquals(p.getLabel().toString(), "TESTPROP new label");
            FxMultiplicity pMult = p.getMultiplicity();
            assertEquals(pMult.getMin(), 0);
            assertEquals(pMult.getMax(), 5);
            assertEquals(p.getHint().toString(), "TESTPROP hint");
            assertTrue(p.mayOverrideBaseMultiplicity());
        } finally {
            // clean up
            ce.remove(contentPK1);
            ce.remove(contentPK2);
            te.remove(typeId);
        }
    }

    /**
     * AssignmentEngineBean: this method tests #save(FxGroupEdit), #updateGroup(..) [implicitly]
     * #updateGroupOptions(..) [implicitly]
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void groupAssignmentTests() throws FxApplicationException {
        long typeId = createTestTypeAndProp("STRUCTTESTTYPE", "TESTPROP", true);
        // assign the group to our given type and also create a property for the group
        FxGroupEdit ge = FxGroupEdit.createNew("STRUCTTESTGROUP", new FxString(true, "GROUPDESCR"), new FxString(true, "GROUPHINT"), false, new FxMultiplicity(0, 1));
        ge.setAssignmentGroupMode(GroupMode.OneOf);
        ae.createGroup(typeId, ge, "/");

        ae.createProperty(typeId, FxPropertyEdit.createNew(
                "TESTGROUPPROP", new FxString(true, FxLanguage.ENGLISH, "TESTGROUPPROP"), new FxString(true, FxLanguage.ENGLISH, "TESTPROP"),
                new FxMultiplicity(0, 1), env().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024).setMultiLang(false), "/STRUCTTESTGROUP");
        // create two content instances for our group (within the same assignment)
        FxPK contentPK = createTestContent(typeId, "/STRUCTTESTGROUP/TESTGROUPPROP", "Testdata 111");
        try {
            // assert group properties
            ge = env().getGroup("STRUCTTESTGROUP").asEditable();
            assertEquals(ge.getName(), "STRUCTTESTGROUP");
            assertEquals(ge.getLabel().toString(), "GROUPDESCR");
            assertEquals(ge.getHint().toString(), "GROUPHINT");

            // make changes to the group and assert them
            ge.setOverrideMultiplicity(true);
            ge.setMultiplicity(new FxMultiplicity(0, 2));
            ge.setHint(new FxString(true, "GROUPHINT_NEW"));
            ge.setLabel(new FxString(true, "GROUPDESCR_NEW"));
            ge.setName("STRUCTTESTGROUP_NEW");
            long groupId = ae.save(ge);

            ge = env().getGroup("STRUCTTESTGROUP_NEW").asEditable();
            assertEquals(ge.getId(), groupId);
            assertEquals(ge.getLabel().toString(), "GROUPDESCR_NEW");
            assertEquals(ge.getHint().toString(), "GROUPHINT_NEW");
            assertEquals(ge.getMultiplicity().getMin(), 0);
            assertEquals(ge.getMultiplicity().getMax(), 2);

            // increase multiplicity of assignment as well in order to be able to add more content
            FxGroupAssignmentEdit assEdit = ((FxGroupAssignment) env().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP")).asEditable();
            assEdit.getGroupEdit().setOverrideMultiplicity(true);
            assEdit.setMultiplicity(new FxMultiplicity(0, 2));
            ae.save(assEdit, false);

            assEdit = ((FxGroupAssignment) env().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP")).asEditable();
            assertEquals(assEdit.getMultiplicity().getMin(), 0);
            assertEquals(assEdit.getMultiplicity().getMax(), 2);
            assertEquals(assEdit.getMode(), GroupMode.OneOf);

            // create another group/content instance & change the multiplicity settings of both the group and the assignments
            FxContent co = ce.load(contentPK);
            co.setValue("/STRUCTTESTGROUP[2]/TESTGROUPPROP[1]", new FxString(false, "Testdata 222"));
            ce.save(co);
            ge = env().getGroup("STRUCTTESTGROUP_NEW").asEditable();
            ge.setOverrideMultiplicity(false);
            ge.setMultiplicity(new FxMultiplicity(0, 1));
            try {
                ae.save(ge);
                Assert.fail("Setting the override mult. flag and the maximum group multiplicity to a value less than the number of given content instances should have failed");
            } catch (FxUpdateException e) {
                // expected
            }
            ge.setOverrideMultiplicity(false);
            ge.setMultiplicity(new FxMultiplicity(3, 3));
            try {
                ae.save(ge);
                Assert.fail("Setting the override mult. flag and the minimum group multiplicity to a value greater than the number of given content instances should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            // test multiplicity setting failure code blocks
            ge.setOverrideMultiplicity(false); // reset
            ge.setMultiplicity(new FxMultiplicity(0, 3));
            ae.save(ge);
            co = ce.load(contentPK);
            co.setValue("/STRUCTTESTGROUP[3]/TESTGROUPPROP[1]", new FxString(false, "Testdata 333"));
            ce.save(co);

            ge = env().getGroup("STRUCTTESTGROUP_NEW").asEditable();
            ge.setMultiplicity(new FxMultiplicity(4, 4));
            try {
                ae.save(ge);
                Assert.fail("Setting the min. group multiplicity to a value > the current # of the assignment's content instances should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            ge.setMultiplicity(new FxMultiplicity(0, 2));
            try {
                ae.save(ge);
                Assert.fail("Setting the max. group multiplicity to a value < the current # of the assignment's content instances should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            ge.setMultiplicity(new FxMultiplicity(0, 3)); // reset
            ge.setOverrideMultiplicity(true);
            ae.save(ge);

            // test more group assignment code blocks
            assEdit = ((FxGroupAssignment) env().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP")).asEditable();
            assEdit.setLabel(new FxString(true, "ASSIGNMENTLABEL_XX123456"));
            assEdit.setHint(new FxString(true, "ASSIGNMENTHINT_XX123456"));
            assEdit.setXPath("/STRUCTTESTGROUP_NEW_PATH");
            assEdit.setMode(GroupMode.AnyOf);

            ae.save(assEdit, false);

            assEdit = ((FxGroupAssignment) env().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP_NEW_PATH")).asEditable();
            assertEquals(assEdit.getLabel().toString(), "ASSIGNMENTLABEL_XX123456");
            assertEquals(assEdit.getHint().toString(), "ASSIGNMENTHINT_XX123456");
            assertEquals(assEdit.getMode(), GroupMode.AnyOf);

            // set the group mode and the alias
            assEdit.setMode(GroupMode.OneOf);
            try {
                ae.save(assEdit, false);
                Assert.fail("Changing the a group's mode from AnyOf to OneOf for existing content instance should have failed");
            } catch (FxUpdateException e) {
                // expected
            }
            assEdit.setMode(GroupMode.AnyOf);
            assEdit.setAlias("STRUCTTESTGROUP_NEW_ALIAS");
            ae.save(assEdit, false);

            assEdit = ((FxGroupAssignment) env().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP_NEW_ALIAS")).asEditable();
            assertEquals(assEdit.getAlias(), "STRUCTTESTGROUP_NEW_ALIAS");

            // disable the assignment
            assEdit.setEnabled(false);
            ae.save(assEdit, true);

            assEdit = ((FxGroupAssignment) env().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP_NEW_ALIAS")).asEditable();
            assertTrue(!assEdit.isEnabled());
            assertEquals(assEdit.getDefaultMultiplicity(), 1);

            // test setting the default multiplicity to a higher value than the current
            assEdit.setDefaultMultiplicity(assEdit.getMultiplicity().getMax() + 1);
            ae.save(assEdit, false);

            assEdit = ((FxGroupAssignment) env().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP_NEW_ALIAS")).asEditable();
            assertEquals(assEdit.getDefaultMultiplicity(), assEdit.getMultiplicity().getMax());

            // TODO: (remaining code blocks) test set position, updategroupassignmentoptions, updategroupoptions, systeminternal flag (prop and group)
        } finally {
            // clean up
            ce.remove(contentPK);
            te.remove(typeId);
        }
    }

    /**
     * AssignmentEngineBean: tests #removeProperty(long propertyId)
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void removePropertyTests() throws FxApplicationException {
        long typeId1 = createTestTypeAndProp("REMOVALTEST1", "", false);
        long typeId2 = createTestTypeAndProp("REMOVALTEST2", "", false);
        // create properties
        ae.createProperty(typeId1, FxPropertyEdit.createNew( // for type1
                "REMOVETESTPROP1", new FxString(true, FxLanguage.ENGLISH, "REMOVETESTPROP1"), new FxString(true, FxLanguage.ENGLISH, "REMOVETESTPROP1"),
                new FxMultiplicity(0, 2), env().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024).setMultiLang(false), "/");
        ae.createProperty(typeId1, FxPropertyEdit.createNew( // for type1
                "REMOVETESTPROP2", new FxString(true, FxLanguage.ENGLISH, "REMOVETESTPROP2"), new FxString(true, FxLanguage.ENGLISH, "REMOVETESTPROP2"),
                new FxMultiplicity(0, 2), env().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024).setMultiLang(false), "/");
        // create derived assignments
        ae.save(FxPropertyAssignmentEdit.reuse("REMOVALTEST1/REMOVETESTPROP1", "REMOVALTEST2", "/", "REMOVETESTPROP1_ALIAS"), false); // type2
        ae.save(FxPropertyAssignmentEdit.reuse("REMOVALTEST1/REMOVETESTPROP2", "REMOVALTEST2", "/", "REMOVETESTPROP2_ALIAS"), false); // type2

        try {
            // tests go here
            List<FxPropertyAssignment> assignments;
            FxProperty prop1, prop2;
            prop1 = env().getProperty("REMOVETESTPROP1");
            assignments = env().getPropertyAssignments(prop1.getId(), true);
            assertEquals(assignments.size(), 2);
            assertTrue(findDerivedPropInList(assignments, "REMOVETESTPROP1_ALIAS"));

            prop2 = env().getProperty("REMOVETESTPROP2");
            assignments = env().getPropertyAssignments(prop2.getId(), true);
            assertEquals(assignments.size(), 2);
            assertTrue(findDerivedPropInList(assignments, "REMOVETESTPROP2_ALIAS"));

            // try to remove a property which doesn't exist
            try {
                ae.removeProperty(999999);
                Assert.fail("Removing a non-existing property should throw an exception!");
            } catch (FxNotFoundException e) {
                // expected
            }

            // remove prop1 and assert that both the property and the alias were removed
            ae.removeProperty(prop1.getId());
            assertTrue(!findInPropAssignments("REMOVETESTPROP1"));
            assertTrue(!findInPropAssignments("REMOVETESTPROP1_ALIAS"));

            // create content for the remaining assignments and the original properties
            FxContent co = ce.initialize(typeId2);
            co.setValue("/REMOVETESTPROP2_ALIAS", new FxString(false, "content for prop2_alias"));
            FxPK contentPK1 = ce.save(co);

            co = ce.initialize(typeId1);
            co.setValue("/REMOVETESTPROP2", new FxString(false, "content for prop2"));
            FxPK contentPK2 = ce.save(co);

            // remove the property and assert that the corresponding content was removed as well
            ae.removeProperty(prop2.getId());
            assertTrue(!findInPropAssignments("REMOVETESTPROP2"));
            assertTrue(!findInPropAssignments("REMOVETESTPROP2_ALIAS"));
            co = ce.load(contentPK1);
            try {
                co.getValue("/REMOVETESTPROP2_ALIAS");
                Assert.fail("Loading a content value for a non-existing Xpath should have failed");
            } catch (Exception e) {
                // expected
            }
            co = ce.load(contentPK2);
            try {
                co.getValue("/REMOVETESTPROP2");
                Assert.fail("Loading a content value for a non-existing Xpath should have failed");
            } catch (Exception e) {
                // expected
            }

        } finally {
            // clean up
            ce.removeForType(typeId1);
            ce.removeForType(typeId2);
            te.remove(typeId1);
            te.remove(typeId2);
        }
    }

    /**
     * AssignmentEngineBean: tests #removeGroup(long groupId)
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void removeGroupTests() throws FxApplicationException {
        createTestTypeAndProp("REMOVALTEST1", "", false);
        createTestTypeAndProp("REMOVALTEST2", "", false);

        FxType t1, t2;
        FxGroup g1, g2;
        FxProperty p1, p2;
        FxPK contentPK1 = null;
        FxPK contentPK2 = null;
        t1 = env().getType("REMOVALTEST1");
        t2 = env().getType("REMOVALTEST2");

        // create groups
        ae.createGroup(t1.getId(), FxGroupEdit.createNew("REMOVETESTGROUP1", new FxString(true, FxLanguage.ENGLISH, "REMOVETESTGROUP1"),
                new FxString(true, FxLanguage.ENGLISH, "HINT1"), true, new FxMultiplicity(0, 2)), "/");
        ae.createGroup(t1.getId(), FxGroupEdit.createNew("REMOVETESTGROUP2", new FxString(true, FxLanguage.ENGLISH, "REMOVETESTGROUP2"),
                new FxString(true, FxLanguage.ENGLISH, "HINT2"), true, new FxMultiplicity(0, 2)), "/");

        g1 = env().getGroup("REMOVETESTGROUP1");
        g2 = env().getGroup("REMOVETESTGROUP2");

        // create properties
        ae.createProperty(t1.getId(), FxPropertyEdit.createNew( // for type1
                "REMOVETESTPROP1", new FxString(true, FxLanguage.ENGLISH, "REMOVETESTPROP1"), new FxString(true, FxLanguage.ENGLISH, "REMOVETESTPROP1"),
                new FxMultiplicity(0, 2), env().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024).setMultiLang(false), "/REMOVETESTGROUP1");
        ae.createProperty(t1.getId(), FxPropertyEdit.createNew( // for type1
                "REMOVETESTPROP2", new FxString(true, FxLanguage.ENGLISH, "REMOVETESTPROP2"), new FxString(true, FxLanguage.ENGLISH, "REMOVETESTPROP2"),
                new FxMultiplicity(0, 2), env().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024).setMultiLang(false), "/REMOVETESTGROUP2");

        p1 = env().getProperty("REMOVETESTPROP1");
        p2 = env().getProperty("REMOVETESTPROP2");

        // create derived assignments
        List<FxGroupAssignment> gAssigns = env().getGroupAssignments(g1.getId(), true);

        assertTrue(gAssigns.size() == 1);

        ae.save(FxGroupAssignmentEdit.createNew(gAssigns.get(0), t2, "REMOVETESTGROUP1_ALIAS", "/"), false);
        ae.save(FxPropertyAssignmentEdit.reuse("REMOVALTEST1/REMOVETESTGROUP1/REMOVETESTPROP1", "REMOVALTEST2", "/REMOVETESTGROUP1_ALIAS", "REMOVETESTPROP1_ALIAS"), false); // type2
        gAssigns = env().getGroupAssignments(g1.getId(), true);

        assertTrue(gAssigns.size() == 2);

        gAssigns = env().getGroupAssignments(g2.getId(), true);

        assertTrue(gAssigns.size() == 1);

        ae.save(FxGroupAssignmentEdit.createNew(gAssigns.get(0), t2, "REMOVETESTGROUP2_ALIAS", "/"), false);
        ae.save(FxPropertyAssignmentEdit.reuse("REMOVALTEST1/REMOVETESTGROUP2/REMOVETESTPROP2", "REMOVALTEST2", "/REMOVETESTGROUP2_ALIAS", "REMOVETESTPROP2_ALIAS"), false); // type2
        gAssigns = env().getGroupAssignments(g2.getId(), true);

        assertTrue(gAssigns.size() == 2);

        try { // tests go here
            List<FxPropertyAssignment> pAssigns;
            gAssigns = env().getGroupAssignments(g1.getId(), true);
            pAssigns = env().getPropertyAssignments(p1.getId(), true);
            assertTrue(gAssigns.size() == 2);
            assertTrue(findDerivedGroupInList(gAssigns, "REMOVETESTGROUP1_ALIAS"));
            assertTrue(findDerivedPropInList(pAssigns, "REMOVETESTPROP1_ALIAS"));

            gAssigns = env().getGroupAssignments(g2.getId(), true);
            pAssigns = env().getPropertyAssignments(p2.getId(), true);
            assertTrue(gAssigns.size() == 2);
            assertTrue(findDerivedGroupInList(gAssigns, "REMOVETESTGROUP2_ALIAS"));
            assertTrue(findDerivedPropInList(pAssigns, "REMOVETESTPROP2_ALIAS"));

            // remove a group which doesn't exist
            try {
                ae.removeGroup(999999);
                Assert.fail("Removing a non-existant group should have failed");
            } catch (FxNotFoundException e) {
                // expected
            }

            // remove one of the above created groups
            ae.removeGroup(g2.getId());
            gAssigns = env().getGroupAssignments(g2.getId(), true);
            assertTrue(gAssigns.size() == 0);
            // prop 2 should also be a goner
            pAssigns = env().getPropertyAssignments(p2.getId(), true);
            assertTrue(pAssigns.size() == 0);

            // create content for the remaining group / properties
            FxContent co = ce.initialize(t1.getId());
            co.setValue("/REMOVETESTGROUP1/REMOVETESTPROP1", new FxString(false, "group 1/ prop 1 content"));
            contentPK1 = ce.save(co);

            co = ce.initialize(t2.getId());
            co.setValue("/REMOVETESTGROUP1_ALIAS/REMOVETESTPROP1_ALIAS", new FxString(false, "group 1 alias / prop 1 alias content"));
            contentPK2 = ce.save(co);

            ae.removeGroup(g1.getId());
            try {
                co = ce.load(contentPK1);
                try {
                    co.getValue("/REMOVETESTGROUP1/REMOVETESTPROP1");
                    Assert.fail("Loading content values for removed props / groups should have thrown an exception");
                } catch (Exception e) {
                    // expected
                }
            } catch (Exception e) {
                // might fail here already
            }

            try {
                co = ce.load(contentPK2);
                try {
                    co.getValue("/REMOVETESTGROUP1_ALIAS/REMOVETESTPROP1_ALIAS");
                    Assert.fail("Loading content values for removed props / groups should have thrown an exception");
                } catch (Exception e) {
                    // expected
                }
            } catch (Exception e) {
                // might fail here already
            }

            gAssigns = env().getGroupAssignments(g1.getId(), true);
            assertTrue(gAssigns.size() == 0);
            // prop 2 should also be a goner
            pAssigns = env().getPropertyAssignments(p1.getId(), true);
            assertTrue(pAssigns.size() == 0);
        } finally {
            if (contentPK1 != null)
                ce.remove(contentPK1);
            if (contentPK2 != null)
                ce.remove(contentPK2);

            te.remove(t2.getId());
            te.remove(t1.getId());
        }
    }

    /**
     * AssignmentEngineBean: Test changing a property's alias
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void propertyAliasChangeTest() throws FxApplicationException {
        long typeId = createTestTypeAndProp("ALIASTEST", "", false);
        // add properties
        ae.createProperty(typeId, FxPropertyEdit.createNew("PROP1", new FxString(true, "PROP1"), new FxString(true, ""), new FxMultiplicity(0, 5),
                env().getACL("Default Structure ACL"), FxDataType.String1024), "/");
        ae.createProperty(typeId, FxPropertyEdit.createNew("PROP2", new FxString(true, "PROP2"), new FxString(true, ""), new FxMultiplicity(0, 5),
                env().getACL("Default Structure ACL"), FxDataType.String1024), "/");
        ae.createProperty(typeId, FxPropertyEdit.createNew("PROP3", new FxString(true, "PROP3"), new FxString(true, ""), new FxMultiplicity(0, 5),
                env().getACL("Default Structure ACL"), FxDataType.String1024), "/");

        // create content
        FxPK contentPK1 = createTestContent(typeId, "/PROP1", "Testdata1_1", "Testdata1_2");
        FxPK contentPK2 = createTestContent(typeId, "/PROP2", "Testdata2_1", "Testdata2_2");
        FxPK contentPK3 = createTestContent(typeId, "/PROP3", "Testdata3_1", "Testdata3_2");

        // change the property assignments' aliases
        try {
            FxPropertyAssignmentEdit ed;
            ed = getPropAssEd("ALIASTEST/PROP1").setAlias("PROP1ALIAS");
            ae.save(ed, true);

            ed = getPropAssEd("ALIASTEST/PROP2").setAlias("PROP2ALIAS");
            ae.save(ed, true);

            ed = getPropAssEd("ALIASTEST/PROP3").setAlias("PROP3ALIAS");
            ae.save(ed, true);

            // assert that both the aliases have changed, the original XPath assignments do not exist anymore and also that
            // the relevant content instances' paths have changed
            assertFalse(env().assignmentExists("ALIASTEST/PROP1"));
            assertFalse(env().assignmentExists("ALIASTEST/PROP2"));
            assertFalse(env().assignmentExists("ALIASTEST/PROP3"));
            assertTrue(env().assignmentExists("ALIASTEST/PROP1ALIAS"));
            assertTrue(env().assignmentExists("ALIASTEST/PROP2ALIAS"));
            assertTrue(env().assignmentExists("ALIASTEST/PROP3ALIAS"));

            FxContent co;
            co = ce.load(contentPK1);
            try {
                co.getValue("ALIASTEST/PROP1");
            } catch (Exception e) {
                // expected
            }
            try {
                co.getValue("ALIASTEST/PROP1[2]");
            } catch (Exception e) {
                // expected
            }
            assertEquals(co.getValue("ALIASTEST/PROP1ALIAS[1]").toString(), "Testdata1_1");
            assertEquals(co.getValue("ALIASTEST/PROP1ALIAS[2]").toString(), "Testdata1_2");
            co = ce.load(contentPK2);
            try {
                co.getValue("ALIASTEST/PROP2");
            } catch (Exception e) {
                // expected
            }
            try {
                co.getValue("ALIASTEST/PROP2[2]");
            } catch (Exception e) {
                // expected
            }
            assertEquals(co.getValue("ALIASTEST/PROP2ALIAS[1]").toString(), "Testdata2_1");
            assertEquals(co.getValue("ALIASTEST/PROP2ALIAS[2]").toString(), "Testdata2_2");
            co = ce.load(contentPK3);
            try {
                co.getValue("ALIASTEST/PROP3");
            } catch (Exception e) {
                // expected
            }
            try {
                co.getValue("ALIASTEST/PROP3[2]");
            } catch (Exception e) {
                // expected
            }
            assertEquals(co.getValue("ALIASTEST/PROP3ALIAS").toString(), "Testdata3_1");
            assertEquals(co.getValue("ALIASTEST/PROP3ALIAS[2]").toString(), "Testdata3_2");
        } finally {
            for (FxPK pk : ce.getPKsForType(typeId, true)) {
                ce.remove(pk);
            }
            te.remove(typeId);
        }
    }

    /**
     * AssignmentEngineBean: Test changing a group's alias
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void groupAliasChangeTest() throws FxApplicationException {
        /**
         * Structure:
         * ALIASTEST
         *       --PROP1
         *       --GROUP1
         *           --PROP2
         *           --GROUP2
         *               --PROP3
         */

        long typeId = createTestTypeAndProp("ALIASTEST", "", false);
        // add more properties
        ae.createProperty(typeId, FxPropertyEdit.createNew("PROP1", new FxString(true, "PROP1"), new FxString(true, ""), new FxMultiplicity(0, 5),
                env().getACL("Default Structure ACL"), FxDataType.String1024), "/");
        ae.createGroup(typeId, FxGroupEdit.createNew("GROUP1", new FxString(true, "GROUP1"), new FxString(true, ""), true, new FxMultiplicity(0, 2)), "/");
        ae.createProperty(typeId, FxPropertyEdit.createNew("PROP2", new FxString(true, "PROP2"), new FxString(true, ""), new FxMultiplicity(0, 2),
                env().getACL("Default Structure ACL"), FxDataType.String1024), "/GROUP1");
        ae.createGroup(typeId, FxGroupEdit.createNew("GROUP2", new FxString(true, "GROUP2"), new FxString(true, ""), true, new FxMultiplicity(0, 2)), "/GROUP1");
        ae.createProperty(typeId, FxPropertyEdit.createNew("PROP3", new FxString(true, "PROP3"), new FxString(true, ""), new FxMultiplicity(0, 2),
                env().getACL("Default Structure ACL"), FxDataType.String1024), "/GROUP1/GROUP2");

        // create content
        FxPK contentPK1 = createTestContent(typeId, "/PROP1", "Testdata1_1", "Testdata1_2");
        FxPK contentPK2 = createTestContent(typeId, "/GROUP1/PROP2", "Testdata2_1", "Testdata2_2");
        FxPK contentPK3 = createTestContent(typeId, "/GROUP1/GROUP2/PROP3", "Testdata3_1", "Testdata3_2");

        try {

            // change group aliases
            FxGroupAssignmentEdit ga;
            ga = getGroupAssEd("ALIASTEST/GROUP1").setAlias("GROUP1ALIAS");
            ae.save(ga, true);

            ga = getGroupAssEd("ALIASTEST/GROUP1ALIAS/GROUP2").setAlias("group2alias"); // diff. case
            ae.save(ga, true);

            assertFalse(env().assignmentExists("ALIASTEST/GROUP1"));
            assertFalse(env().assignmentExists("ALIASTEST/GROUP1/GROUP2"));
            assertTrue(env().assignmentExists("ALIASTEST/GROUP1ALIAS/GROUP2ALIAS"));

            // try to load the respective content 2 - proves functionality for all other contents
            FxContent co;
            co = ce.load(contentPK2);
            try {
                co.getValue("ALIASTEST/GROUP1/PROP2");
            } catch (Exception e) {
                // expected
            }
            try {
                co.getValue("ALIASTEST/GROUP1/PROP2[2]");
            } catch (Exception e) {
                // expected
            }
            assertEquals(co.getValue("ALIASTEST/GROUP1ALIAS/PROP2").toString(), "Testdata2_1");
            assertEquals(co.getValue("ALIASTEST/GROUP1ALIAS/PROP2[2]").toString(), "Testdata2_2");

            // make changes to the property aliases
            FxPropertyAssignmentEdit pa;
            pa = getPropAssEd("ALIASTEST/PROP1").setAlias("PROP1ALIAS");
            ae.save(pa, true);

            pa = getPropAssEd("ALIASTEST/GROUP1ALIAS/PROP2").setAlias("PROP2ALIAS");
            ae.save(pa, true);

            pa = getPropAssEd("ALIASTEST/GROUP1ALIAS/GROUP2ALIAS/PROP3").setAlias("PROP3ALIAS");
            ae.save(pa, true);

            assertFalse(env().assignmentExists("ALIASTEST/PROP1"));
            assertFalse(env().assignmentExists("ALIASTEST/GROUP1ALIAS/PROP2"));
            assertFalse(env().assignmentExists("ALIASTEST/GROUP1ALIAS/GROUP2ALIAS/PROP3"));
            assertTrue(env().assignmentExists("ALIASTEST/PROP1ALIAS"));
            assertTrue(env().assignmentExists("ALIASTEST/GROUP1ALIAS/PROP2ALIAS"));
            assertTrue(env().assignmentExists("ALIASTEST/GROUP1ALIAS/GROUP2ALIAS/PROP3ALIAS"));

            // test w/ content 1 & 3
            co = ce.load(contentPK1);
            try {
                co.getValue("ALIASTEST/PROP1");
            } catch (Exception e) {
                // expected
            }
            try {
                co.getValue("ALIASTEST/PROP1[2]");
            } catch (Exception e) {
                // expected
            }
            assertEquals(co.getValue("ALIASTEST/PROP1ALIAS").toString(), "Testdata1_1");
            assertEquals(co.getValue("ALIASTEST/PROP1ALIAS[2]").toString(), "Testdata1_2");

            co = ce.load(contentPK3);
            try {
                co.getValue("ALIASTEST/GROUP1ALIAS/GROUP2ALIAS/PROP3");
            } catch (Exception e) {
                // expected
            }
            try {
                co.getValue("ALIASTEST/GROUP1ALIAS/GROUP2ALIAS/PROP3[2]");
            } catch (Exception e) {
                // expected
            }
            assertEquals(co.getValue("ALIASTEST/GROUP1ALIAS/GROUP2ALIAS/PROP3ALIAS").toString(), "Testdata3_1");
            assertEquals(co.getValue("ALIASTEST/GROUP1ALIAS/GROUP2ALIAS/PROP3ALIAS[2]").toString(), "Testdata3_2");

        } finally {
            for (FxPK pk : ce.getPKsForType(typeId, true)) {
                ce.remove(pk);
            }
            te.remove(typeId);
        }
    }

    /**
     * AssignmentEngineBean: test creating props / groups with immedately assigned aliases
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void createAliasedElementsTest() throws FxApplicationException {
        long typeId = createTestTypeAndProp("ALIASASSIGN", "", false);
        // add a prop and groups
        ae.createProperty(typeId, FxPropertyEdit.createNew("PROP1", new FxString(true, "PROP1"), new FxString(true, ""), new FxMultiplicity(0, 5),
                env().getACL("Default Structure ACL"), FxDataType.String1024), "/", "prop1alias");
        ae.createGroup(typeId, FxGroupEdit.createNew("GROUP1", new FxString(true, "GROUP1"), new FxString(true, ""), true, new FxMultiplicity(0, 2)),
                "/", "group1alias");

        try {
            assertTrue(env().assignmentExists("ALIASASSIGN/PROP1ALIAS"));
            assertFalse(env().assignmentExists("ALIASASSIGN/PROP1"));
            assertTrue(env().assignmentExists("ALIASASSIGN/GROUP1ALIAS"));
            assertFalse(env().assignmentExists("ALIASASSIGN/GROUP1"));

            // create aliased sub-group
            ae.createGroup(typeId, FxGroupEdit.createNew("GROUP2", new FxString(true, "group2"), new FxString(true, ""), true, new FxMultiplicity(0, 2)),
                    "/group1alias", "group2alias");

            assertTrue(env().assignmentExists("ALIASASSIGN/GROUP1ALIAS/GROUP2ALIAS"));
            assertFalse(env().assignmentExists("ALIASASSIGN/GROUP1ALIAS/GROUP2"));

        } finally {
            te.remove(typeId);
        }
    }

    /**
     * Tests setting generic structure options for groups / properties
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void testGenericStructureOptions() throws FxApplicationException {
        long typeId = createTestTypeAndProp("OPTTEST", "", false);
        FxPropertyEdit pEd = FxPropertyEdit.createNew("PROP1OPTTEST", new FxString(true, "PROP1"), new FxString(true, ""), new FxMultiplicity(0, 5),
                env().getACL("Default Structure ACL"), FxDataType.String1024);
        pEd.setOption("OPT.1P", false, "OPT 1 value");
        pEd.setOption("OPT.2P", true, false);
        ae.createProperty(typeId, pEd, "/");

        FxGroupEdit gEd = FxGroupEdit.createNew("GROUP1OPTTEST", new FxString(true, "GROUP1"), new FxString(true, ""), true, new FxMultiplicity(0, 2));
        gEd.setOption("OPT.1G", false, "OPT 1 value");
        gEd.setOption("OPT.2G", true, false);
        ae.createGroup(typeId, gEd, "/");

        try {
            FxProperty p = env().getProperty("PROP1OPTTEST");
            FxGroup g = env().getGroup("GROUP1OPTTEST");

            assertEquals(p.getOption("OPT.1P").getValue(), "OPT 1 value");
            assertEquals(p.getOption("OPT.2P").getIntValue(), 0);
            assertFalse(p.getOption("OPT.1P").isOverrideable());
            assertTrue(p.getOption("OPT.2P").isOverrideable());

            assertEquals(g.getOption("OPT.1G").getValue(), "OPT 1 value");
            assertEquals(g.getOption("OPT.2G").getIntValue(), 0);
            assertFalse(g.getOption("OPT.1G").isOverrideable());
            assertTrue(g.getOption("OPT.2G").isOverrideable());

            // create an assignment and check whether the group options are "passed along"
            FxGroupAssignmentEdit ga = FxGroupAssignmentEdit.createNew((FxGroupAssignment) getEnvironment().getAssignment("OPTTEST/GROUP1OPTTEST"),
                    getEnvironment().getType("OPTTEST"), "GROUP1OPTTESTDER", "/");

            long aId = ae.save(ga, false);

            FxGroupAssignment gass = (FxGroupAssignment) getEnvironment().getAssignment("OPTTEST/GROUP1OPTTESTDER");

            assertEquals(gass.getOption("OPT.1G").getValue(), "OPT 1 value");
            assertEquals(gass.getOption("OPT.2G").getIntValue(), 0);
            assertFalse(gass.getOption("OPT.1G").isOverrideable());
            assertTrue(gass.getOption("OPT.2G").isOverrideable());

            ae.removeAssignment(aId);
        } finally {
            te.remove(typeId);
        }

    }

    @Test(groups = {"ejb", "structure"})
    public void typeAclAssignmentMultiplicity() throws FxApplicationException {
        final String base = "typeAclAssignmentMultiplicity";
        final List<Long> typeIds = Lists.newArrayList();

        try {
            typeIds.add(getTypeEngine().save(FxTypeEdit.createNew(base + "_1").setMultipleContentACLs(false)));
            assertEquals(getEnvironment().getAssignment(base + "_1/ACL").getMultiplicity(), FxMultiplicity.MULT_1_1);

            // derive without changing the multiple ACLs setting
            typeIds.add(getTypeEngine().save(FxTypeEdit.createNew(base + "_1_2", base + "_1")));
            assertEquals(getEnvironment().getAssignment(base + "_1_2/ACL").getMultiplicity(), FxMultiplicity.MULT_1_1);

            typeIds.add(getTypeEngine().save(FxTypeEdit.createNew(base + "_1_2_3", base + "_1_2").setMultipleContentACLs(true)));
            assertEquals(getEnvironment().getAssignment(base + "_1_2_3/ACL").getMultiplicity(), FxMultiplicity.MULT_1_N);

            typeIds.add(getTypeEngine().save(FxTypeEdit.createNew(base + "_2").setMultipleContentACLs(true)));
            assertEquals(getEnvironment().getAssignment(base + "_2/ACL").getMultiplicity(), FxMultiplicity.MULT_1_N);
        } finally {
            Collections.reverse(typeIds);
            for (Long typeId : typeIds) {
                getTypeEngine().remove(typeId);
            }
        }

    }

    @Test(groups = {"ejb", "structure"})
    public void typeFastCreateTest() throws FxApplicationException {
        // creates a type through FxTypeEdit's add... methods
        final FxTypeEdit type = FxTypeEdit.createNew("typeFastCreateTest").save();
        FxPK pk = null;
        try {
            type.addProperty("string", FxDataType.String1024).setMultiplicity(FxMultiplicity.MULT_1_1).save();
            type.addGroup("group1").setLabel(new FxString(true, "group label")).save();
            type.addProperty("group1/groupstring", FxDataType.String1024);
            type.addGroup("group1/group2");
            type.addProperty("group1/group2/group2string", FxDataType.String1024);
            type.addProperty("number", FxDataType.Number);
            FxPropertyAssignmentEdit fxpae = type.addProperty("test", FxDataType.HTML);
            fxpae.setMultiplicity(FxMultiplicity.MULT_0_1).save();

            // try to create some content...
            final FxContent content = EJBLookup.getContentEngine().initialize(type.getId());
            content.setValue("/string", "a value");
            content.setValue("/group1/groupstring", "group value");
            content.setValue("/group1/group2/group2string", "group 2 value");
            content.setValue("/number", 123);
            pk = content.save().getPk();

            type.save();

            try{
                fxpae.setMultiplicity(FxMultiplicity.MULT_1_1).save();
                fail("the modification of minimumMultiplicity should not be possible");
            } catch (FxUpdateException e) {
                // pass
            }
            try {
                type.addProperty("test2", FxDataType.HTML).setMultiplicity(FxMultiplicity.MULT_1_1).save();
                fail("the modification of minimumMultiplicity should not be possible");
            } catch (FxUpdateException e) {
                // pass
            }
        } finally {
            if (pk != null) {
                EJBLookup.getContentEngine().remove(pk);
            }
            te.remove(type.getId());
        }
    }

    /**
     * Test setting / loading structural options for types and derived types
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void typeStructureOptionsTest() throws FxApplicationException {
        FxTypeEdit type = null;
        FxTypeEdit typeDer = null;
        FxType t1 = null;
        FxType t1der = null;
        try {
            type = FxTypeEdit.createNew("STRUCTOPTTEST");
            type.setOption("OPTION_A", "a value"); // overrideable, not inherited
            type.setOption("OPTION_B", "another value", false, true); // non-overrideable, inherited
            type.setOption("OPTION_BOOL_A", true); // overrideable, not inherited
            type.setOption("OPTION_BOOL_B", false, false, true); // non-overrideable, inherited
            type.setOption("OPTION_C", "C", true, true); // overrideable, inherited
            type = type.save();

            // create a derived type
            FxTypeEdit.createNew("STRUCTOPTTESTDERIVED", type.getId()).save();

            // safety reload
            t1 = getEnvironment().getType("STRUCTOPTTEST");
            t1der = getEnvironment().getType("STRUCTOPTTESTDERIVED");

            Assert.assertEquals(t1.getOption("OPTION_A").getValue(), "a value");
            Assert.assertTrue(t1.getOption("OPTION_A").isOverrideable());
            Assert.assertFalse(t1.getOption("OPTION_A").getIsInherited());
            Assert.assertEquals(t1.getOption("OPTION_B").getValue(), "another value");
            Assert.assertFalse(t1.getOption("OPTION_B").isOverrideable());
            Assert.assertTrue(t1.getOption("OPTION_B").getIsInherited());
            Assert.assertEquals(t1.getOption("OPTION_BOOL_A").isValueTrue(), true);
            Assert.assertTrue(t1.getOption("OPTION_BOOL_A").isOverrideable());
            Assert.assertFalse(t1.getOption("OPTION_BOOL_A").getIsInherited());
            Assert.assertEquals(t1.getOption("OPTION_BOOL_B").isValueTrue(), false);
            Assert.assertFalse(t1.getOption("OPTION_BOOL_B").isOverrideable());
            Assert.assertTrue(t1.getOption("OPTION_BOOL_B").getIsInherited());
            Assert.assertEquals(t1.getOption("OPTION_C").getValue(), "C");
            Assert.assertTrue(t1.getOption("OPTION_C").isOverrideable());
            Assert.assertTrue(t1.getOption("OPTION_C").getIsInherited());

            Assert.assertEquals(t1der.getOption("OPTION_B").getValue(), "another value");
            Assert.assertFalse(t1der.getOption("OPTION_BOOL_B").isValueTrue());
            Assert.assertEquals(t1der.getOption("OPTION_C").getValue(), "C");

            // try to set the overrideable and the non-overrideable options
            typeDer = getEnvironment().getType("STRUCTOPTTESTDERIVED").asEditable();
            typeDer.setOption("OPTION_C", "C NEW");

            try {
                typeDer.setOption("OPTION_B", "new value");
                Assert.fail("It should not be possible to set values for un-overrideable, inherited options");
            } catch (FxApplicationException e) {
                // expected
            }

            typeDer.save();
            t1der = getEnvironment().getType("STRUCTOPTTESTDERIVED");
            Assert.assertEquals(t1der.getOption("OPTION_C").getValue(), "C NEW");

        } finally {
            if (t1der != null)
                te.remove(t1der.getId());
            if (t1 != null)
                te.remove(t1.getId());
        }
    }

    /**
     * Test setting mime types for DOCUMENTFILE and its derived types
     *
     * @throws FxApplicationException on errors
     */
    @Test(groups = {"ejb", "structure"})
    public void mimeTypeTest() throws FxApplicationException {
        // FxTypeEdit document;
        // TODO
    }

    /**
     * Helper method to find the alias in a list of property assignments
     *
     * @param list the list of FxPropertyAssignments
     * @param name the name of the property (alias)
     * @return returns true if the alias was found
     */
    private boolean findDerivedPropInList(List<FxPropertyAssignment> list, String name) {
        for (FxPropertyAssignment a : list) {
            if (name.equalsIgnoreCase(a.getAlias()) || name.equalsIgnoreCase(a.getDisplayName()))
                return true;
        }
        return false;
    }

    /**
     * Helper method to find the alias in a list of group assignments
     *
     * @param list the list of FxGroupAssignments
     * @param name the name of the group (alias)
     * @return returns true if the alias was found
     */
    private boolean findDerivedGroupInList(List<FxGroupAssignment> list, String name) {
        for (FxGroupAssignment a : list) {
            if (name.equalsIgnoreCase(a.getAlias()) || name.equalsIgnoreCase(a.getDisplayName()))
                return true;
        }
        return false;
    }

    /**
     * Helper method to assert existance of a property assignment.
     *
     * @param alias the alias or name of the property
     * @return returns true if the name / alias was found in the list of property assignments
     */
    private boolean findInPropAssignments(String alias) {
        for (FxPropertyAssignment a : env().getPropertyAssignments(true)) {
            if (alias.equals(a.getAlias()) || alias.equals(a.getDisplayName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a test type and property
     *
     * @param typeName     the name/label for the type to be created
     * @param propertyName the name/label of the property which should be created
     * @param createProp   if set to true, it will create a property called "TESTPROP" assigned to the type
     * @return the ids of the test type (long[0]) and its property (long[1])
     * @throws FxApplicationException on errors
     */
    private long createTestTypeAndProp(String typeName, String propertyName, boolean createProp) throws FxApplicationException {
        long typeId = te.save(FxTypeEdit.createNew(typeName, new FxString(typeName),
                env().getACL(ACLCategory.STRUCTURE.getDefaultId())));
        if (createProp) {
            ae.createProperty(typeId, FxPropertyEdit.createNew(
                    propertyName, new FxString(true, FxLanguage.ENGLISH, propertyName), new FxString(true, FxLanguage.ENGLISH, "TESTPROP"),
                    new FxMultiplicity(0, 1), env().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                    FxDataType.String1024).setMultiLang(false), "/");
        }
        return typeId;
    }

    /**
     * Creates a test content instance for the "STRUCTTESTTYPE" and its assigned "TESTPROP"
     *
     * @param typeId       the id of the type for which the content instance will be created
     * @param propertyPath the property's XPath
     * @param testData     a String value for test data
     * @return FxPK the content instance PK
     * @throws FxApplicationException on errors
     */
    private FxPK createTestContent(long typeId, String propertyPath, String... testData) throws FxApplicationException {
        final FxContent co = ce.initialize(typeId);
        int coCount = 1;
        for (String tData : testData) {
            final FxString data = new FxString(false, tData);
            co.setValue(propertyPath + "[" + coCount++ + "]", data);
        }
        return ce.save(co);
    }

    /**
     * @param xPath the assignment's XPath
     * @return returns an FxPropertyAssignmentEdit instance
     */
    private FxPropertyAssignmentEdit getPropAssEd(String xPath) {
        return ((FxPropertyAssignment) env().getAssignment(xPath)).asEditable();
    }

    /**
     * @param xPath the assignment's XPath
     * @return returns an FxGroupAssignmentEdit instance
     */
    private FxGroupAssignmentEdit getGroupAssEd(String xPath) {
        return ((FxGroupAssignment) env().getAssignment(xPath)).asEditable();
    }

    /**
     * @return FxEnvironment
     */
    private FxEnvironment env() {
        return getEnvironment();
    }
}
