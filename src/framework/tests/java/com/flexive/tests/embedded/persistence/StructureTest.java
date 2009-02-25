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
package com.flexive.tests.embedded.persistence;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ACLEngine;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.naming.Context;
import javax.transaction.UserTransaction;

/**
 * Structure tests
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "structure"})
public class StructureTest {

    private TypeEngine te;
    private AssignmentEngine ae;
    private ACLEngine acl;


    @BeforeClass
    public void beforeClass() throws FxLookupException, FxLoginFailedException, FxAccountInUseException {
        te = EJBLookup.getTypeEngine();
        ae = EJBLookup.getAssignmentEngine();
        acl = EJBLookup.getAclEngine();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws Exception {
        try {
            te.remove(CacheAdmin.getEnvironment().getType("ContactsTest").getId());
        } catch (FxRuntimeException e) {
//            ignore
        }
        try {
            ae.removeAssignment(CacheAdmin.getEnvironment().getAssignment("ROOT/CONTACT_DATA").getId(),
                    true, false);
        } catch (FxRuntimeException e) {
//            ignore
        }
        try {
            acl.remove(CacheAdmin.getEnvironment().getACL("ContactsTest").getId());
        } catch (FxRuntimeException e) {
//            ignore
        }
        logout();
    }

    @Test(invocationCount = 2)
    public void typeCreateDeleteUpdate() throws Exception {
        long testId = -1;
        try {
            testId = te.save(FxTypeEdit.createNew("TestCD", new FxString("description..."),
                    CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()), null));
//            testId = te.create(1, 1, 1, new ArrayList<Mandator>(2), "TestCD",
//                    new FxString("description..."), null, false,
//                    TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                    true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
            FxString desc = new FxString("Test data structure");
            desc.setTranslation(FxLanguage.GERMAN, "Testdaten Strukturen");
            FxString hint = new FxString("Hint text ...");
            FxGroupEdit ge = FxGroupEdit.createNew("TEST_DIRECT_DATA", desc, hint, true, new FxMultiplicity(0, 1));
            ae.createGroup(testId, ge, "/");
            ACL structACL = null;
            for (ACL a : CacheAdmin.getEnvironment().getACLs())
                if (a.getCategory() == ACLCategory.STRUCTURE) {
                    structACL = a;
                    break;
                }
            Assert.assertTrue(structACL != null, "No available ACL for structure found!");
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
        FxTypeEdit testEdit = new FxTypeEdit(CacheAdmin.getEnvironment().getType("TestCD"));
        long propCount = testEdit.getAssignedProperties().size();
        testEdit.setName("TestCDNewName");
        testEdit.setPermissions((byte) 0); //clear permissions
        testEdit.setUseInstancePermissions(true);
        testEdit.setLabel(new FxString("Changed label"));
        testEdit.setState(TypeState.Unavailable);
        te.save(testEdit);
        FxType testType = CacheAdmin.getEnvironment().getType("TestCDNewName");
        Assert.assertTrue(testType.getAssignedProperties().size() == propCount, "Property count mismatch");
        Assert.assertEquals(testType.getAssignment("/TestDirectProperty2").getXPath(), "TESTCDNEWNAME/TESTDIRECTPROPERTY2",
                "Expected [TESTCDNEWNAME/TESTDIRECTPROPERTY2] but got: [" + testType.getAssignment("/TestDirectProperty2").getXPath() + "]");
        Assert.assertTrue(testType.usePermissions());
        Assert.assertTrue(testType.useInstancePermissions());
        Assert.assertTrue(!testType.usePropertyPermissions());
        Assert.assertTrue(!testType.useStepPermissions());
        Assert.assertTrue(!testType.useTypePermissions());
        Assert.assertTrue(testType.getState() == TypeState.Unavailable);

        try {
            Assert.assertTrue(testId == testType.getId(), "Wrong id for type!");
            te.remove(testType.getId());
            try {
                CacheAdmin.getEnvironment().getType("TestCD");
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
    @Test
    public void derivedType() throws Exception {
        long testId = -1;
        try {
            testId = te.save(FxTypeEdit.createNew("TestCDP", new FxString("description..."), CacheAdmin.getEnvironment().getACLs(ACLCategory.STRUCTURE).get(0), null));
//            testId = te.create(1, 1, 1, new ArrayList<Mandator>(2), "TestCDP",
//                    new FxString("description..."), null, false,
//                    TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                    true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
            FxString desc = new FxString("Test data structure");
            desc.setTranslation(FxLanguage.GERMAN, "Testdaten Strukturen");
            FxString hint = new FxString("Hint text ...");
            ACL structACL = null;
            for (ACL a : CacheAdmin.getEnvironment().getACLs())
                if (a.getCategory() == ACLCategory.STRUCTURE) {
                    structACL = a;
                    break;
                }
            Assert.assertTrue(structACL != null, "No available ACL for structure found!");
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
        FxType testType = CacheAdmin.getEnvironment().getType("TestCDP");
        Assert.assertTrue(testId == testType.getId(), "Wrong id for type!");

        long testDerivedId;
        testDerivedId = te.save(FxTypeEdit.createNew("TestCDDerived", new FxString("description..."),
                CacheAdmin.getEnvironment().getACLs(ACLCategory.STRUCTURE).get(0), testType).setEnableParentAssignments(false));
//        testDerivedId = te.create(1, 1, 1, new ArrayList<Mandator>(2), "TestCDDerived",
//                new FxString("description..."), testType, false,
//                TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
        FxType testTypeDerived = CacheAdmin.getEnvironment().getType("TestCDDerived");
        Assert.assertTrue(testTypeDerived.getId() == testDerivedId, "Derived type id does not match!");
        Assert.assertTrue(testTypeDerived.getParent().getId() == testType.getId(), "Derived types parent does not match!");
        FxAssignment dp = null;
        try {
            dp = testTypeDerived.getAssignment("/TestDirectProperty1");
        } catch (Exception e) {
            Assert.fail("Failed to retrieved derived property: " + e.getMessage());
        }
        Assert.assertTrue(!dp.isEnabled(), "Property should be disabled!");
        FxPropertyAssignmentEdit dpe = new FxPropertyAssignmentEdit((FxPropertyAssignment) dp);
        dpe.setEnabled(true);
        ae.save(dpe, false);
        testTypeDerived = CacheAdmin.getEnvironment().getType("TestCDDerived");
        dp = testTypeDerived.getAssignment("/TestDirectProperty1");
        Assert.assertTrue(dp.isEnabled(), "Property should be enabled!");


        try {
            dp = testTypeDerived.getAssignment("/TestGroup/TestDirectProperty2");
        } catch (Exception e) {
            Assert.fail("Failed to retrieved derived property: " + e.getMessage());
        }
        Assert.assertTrue(!dp.isEnabled(), "Property should be disabled!");

        FxGroupAssignmentEdit gae = new FxGroupAssignmentEdit((FxGroupAssignment) testTypeDerived.getAssignment("/TestGroup"));
        gae.setEnabled(true);
        ae.save(gae, false);
        testTypeDerived = CacheAdmin.getEnvironment().getType("TestCDDerived");
        dp = testTypeDerived.getAssignment("/TestGroup/TestDirectProperty2");
        Assert.assertTrue(dp.isEnabled(), "Property should be enabled!");

        try {
            te.remove(testTypeDerived.getId());
            try {
                CacheAdmin.getEnvironment().getType("TestCDDerived");
                Assert.fail("TestCDDerived could be loaded after remove!");
            } catch (Exception e) {
                //ok
            }
            te.remove(testType.getId());
            try {
                CacheAdmin.getEnvironment().getType("TestCDP");
                Assert.fail("TestCD could be loaded after remove!");
            } catch (Exception e) {
                //ok
            }
        } catch (FxApplicationException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void contactData() throws Exception {
        try {
            if (CacheAdmin.getEnvironment().getType("ContactsTest") != null)
                return; //ok, exists already
        } catch (Exception e) {
            //ok, create the type
        }
        long aid = acl.create("ContactsTest", new FxString("Contact ACL"), Mandator.MANDATOR_FLEXIVE, "#AA0000", "ACL for ContactsTest", ACLCategory.STRUCTURE);
        ACL contactACL = CacheAdmin.getEnvironment().getACL(aid);
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

//        te.create(contactACL.getId(), 1, 1, new ArrayList<Mandator>(2), "ContactsTest",
//                new FxString("Contact data"), null, false,
//                TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
        FxGroupAssignment ga = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("ROOT/CONTACT_DATA");
        FxGroupAssignmentEdit gae = FxGroupAssignmentEdit.createNew(ga, CacheAdmin.getEnvironment().getType("ContactsTest"), null, "/");
        ae.save(gae, true);
    }

    @Test
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
                true, CacheAdmin.getEnvironment().getACL(1), FxDataType.Number, new FxString("123"),
                true, null, null, null);
        ae.createProperty(pe, "/GROUPTEST/SUBGROUP");
        FxGroupAssignment ga = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("ROOT/GROUPTEST");
        FxGroupAssignmentEdit gae = FxGroupAssignmentEdit.createNew(ga, CacheAdmin.getEnvironment().getType("ROOT"), "GTEST", "/");
        ae.save(gae, true);
        ut.rollback();
    }

    @Test
    public void assignmentRemove() throws Exception {
        long testId = -1;
        ACL structACL = null;
        FxString hint = new FxString("Hint text ...");
        FxString desc = new FxString("Test data structure");
        try {
            testId = te.save(FxTypeEdit.createNew("TestAssignmentRemove", new FxString("description..."), CacheAdmin.getEnvironment().getACLs(ACLCategory.STRUCTURE).get(0), null));
//                    te.create(1, 1, 1, new ArrayList<Mandator>(2), "TestAssignmentRemove",
//                    new FxString("description..."), null, false,
//                    TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                    true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
            desc.setTranslation(FxLanguage.GERMAN, "Testdaten Strukturen");
            structACL = null;
            for (ACL a : CacheAdmin.getEnvironment().getACLs())
                if (a.getCategory() == ACLCategory.STRUCTURE) {
                    structACL = a;
                    break;
                }
            Assert.assertTrue(structACL != null, "No available ACL for structure found!");
            FxPropertyEdit pe = FxPropertyEdit.createNew("TestAssignmentRemoveProperty1", desc, hint, true, new FxMultiplicity(0, 1),
                    true, structACL, FxDataType.String1024, new FxString(FxString.EMPTY),
                    true, null, null, null);
            ae.createProperty(testId, pe, "/");
        } catch (FxApplicationException e) {
            Assert.fail(e.getMessage());
        }
        FxType testType = CacheAdmin.getEnvironment().getType("TestAssignmentRemove");
        Assert.assertTrue(testId == testType.getId(), "Wrong id for type!");
        FxPropertyAssignment pa = null;
        long rmId = -1;
        try {
            pa = (FxPropertyAssignment) testType.getAssignment("/TestAssignmentRemoveProperty1");
            Assert.assertTrue(pa != null, "Created Property Assignment is null!");
            rmId = pa.getId();
        } catch (Exception e) {
            Assert.fail("Created Property Assignment does not exist!");
        }
        FxPropertyAssignmentEdit pae = FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty2", "/");
        try {
            long paId = ae.save(pae, true);
            pa = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(paId);
        } catch (Exception e) {
            Assert.fail("Failed to retrieve derived assignment(2): " + e.getMessage());
        }
        //create derived assignment over 3 hops
        ae.save(FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty3", "/"), true);
        //check base's
        testType = CacheAdmin.getEnvironment().getType(testId);
        Assert.assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty1").getBaseAssignmentId() == FxAssignment.NO_PARENT);
        Assert.assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty2").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty1").getId());
        Assert.assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty3").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty2").getId());
        ae.removeAssignment(rmId, true, true);
        Assert.assertTrue(CacheAdmin.getEnvironment().getSystemInternalRootPropertyAssignments().size() == CacheAdmin.getEnvironment().getType(testId).getAssignedProperties().size(), "Expected to have " + CacheAdmin.getEnvironment().getSystemInternalRootPropertyAssignments().size() + " assignments to the type!");

        //recreate
        FxPropertyEdit pe = FxPropertyEdit.createNew("TestAssignmentRemoveProperty1", desc, hint, true, new FxMultiplicity(0, 1),
                true, structACL, FxDataType.String1024, new FxString(FxString.EMPTY),
                true, null, null, null);
        long id1 = ae.createProperty(testId, pe, "/");
        pa = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(id1);
        long id2 = ae.save(FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty2", "/"), true);
        pa = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(id2);
//        long id3 =
        ae.save(FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty3", "/"), true);
        //recheck
        testType = CacheAdmin.getEnvironment().getType(testId);
        Assert.assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty1").getBaseAssignmentId() == FxAssignment.NO_PARENT);
        Assert.assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty2").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty1").getId());
        Assert.assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty3").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty2").getId());

        ae.removeAssignment(testType.getAssignment("/TestAssignmentRemoveProperty1").getId(), false, false);
        testType = CacheAdmin.getEnvironment().getType(testId);
        Assert.assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty2").getBaseAssignmentId() == FxAssignment.NO_PARENT);
        Assert.assertTrue(testType.getAssignment("/TestAssignmentRemoveProperty3").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty2").getId());

        // test if derived sub-assignments of a derived group are correctly
        // dereferenced from base assignments if parent group is removed

        //create base group with assignment
        FxGroupEdit ge = FxGroupEdit.createNew("TestRemoveGroupBase", desc, hint, true, new FxMultiplicity(0, 1));
        long baseGrpId = ae.createGroup(testId, ge, "/");
        FxGroupAssignment baseGrp = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(baseGrpId);
        FxPropertyEdit basePe = FxPropertyEdit.createNew("TestRemoveGroupBaseProperty", desc, hint, true, new FxMultiplicity(0, 1),
                true, structACL, FxDataType.String1024, new FxString(FxString.EMPTY),
                true, null, null, null);
        long baseAssId = ae.createProperty(testId, basePe, baseGrp.getXPath());

        //verify that property assignment is child of group
        FxPropertyAssignment baseAss= (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(baseAssId);
        Assert.assertTrue(baseAss.getParentGroupAssignment().getId() == baseGrp.getId());

        //create derived group assignment with derived sub assignments
        long derivedGrpId = ae.save(FxGroupAssignmentEdit.createNew(baseGrp, CacheAdmin.getEnvironment().getType(testId), "TestRemoveGroupDerived", "/"), true);
        FxGroupAssignment derivedGrp = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(derivedGrpId);

        //verify that group is derived
        Assert.assertTrue(derivedGrp.isDerivedAssignment() && derivedGrp.getBaseAssignmentId() == baseGrp.getId());

        //verify that derived group contains derived assignment from base group
        boolean found = false;
        long derivedAssId=-1;
        for (FxAssignment a : derivedGrp.getAssignments()) {
            if (a.isDerivedAssignment() && a.getBaseAssignmentId() == baseAss.getId()) {
                found = true;
                derivedAssId = a.getId();
                break;
            }
        }
        Assert.assertTrue(found);

        //remove base group (together with its sub assignments)
        ae.removeAssignment(baseGrp.getId());

        //verify that derived group exists and has been dereferenced
        Assert.assertTrue(!CacheAdmin.getEnvironment().getAssignment(derivedGrp.getId()).isDerivedAssignment() &&
                CacheAdmin.getEnvironment().getAssignment(derivedGrp.getId()).getBaseAssignmentId() == FxAssignment.NO_PARENT);

        //verify that derived assignment exists and has been dereferenced
        Assert.assertTrue(!CacheAdmin.getEnvironment().getAssignment(derivedAssId).isDerivedAssignment() &&
                CacheAdmin.getEnvironment().getAssignment(derivedAssId).getBaseAssignmentId() == FxAssignment.NO_PARENT);

        te.remove(testId);
    }

    @Test
    public void overrideCaptionMultiLangTest() throws FxNotFoundException, FxInvalidParameterException {
        final FxPropertyAssignmentEdit property = FxPropertyAssignmentEdit.createNew(
                (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment("ROOT/CAPTION"),

                CacheAdmin.getEnvironment().getType(FxType.CONTACTDATA), "test", "/");
        Assert.assertTrue(property.isMultiLang(), "Caption property should be multi-language by default");
        if (property.getProperty().mayOverrideMultiLang())
            Assert.assertTrue(false, "Expected Caption property to be not overrideable for this test case!");
        try {
            property.setMultiLang(false);
            Assert.assertTrue(false, "Non-overrideable option must not be overridden!");
        } catch (FxInvalidParameterException ipe) {
            //expected
        }
    }

    public void defaultValueLanguageTest() throws FxApplicationException {
        FxPropertyEdit prop = FxPropertyEdit.createNew("testDefLang",
                new FxString("Test Priority"),
                new FxString("Priority"),
                FxMultiplicity.MULT_1_1,
                CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.Number);

        FxString defML = new FxString(true, "test");
        FxString defSL = new FxString(false, "test");
        long asId = -1 ;

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
            if( asId != -1)
                ae.removeAssignment(asId);
        }
    }
}
