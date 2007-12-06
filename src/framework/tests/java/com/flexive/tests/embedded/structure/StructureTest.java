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
package com.flexive.tests.embedded.structure;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ACLEngine;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
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
import javax.naming.InitialContext;
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
        System.out.println("setting up structure test");
        te = EJBLookup.getTypeEngine();
        ae = EJBLookup.getAssignmentEngine();
        acl = EJBLookup.getACLEngine();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws Exception {
        try {
            if (CacheAdmin.getEnvironment().getType("ContactsTest") != null)
                te.remove(CacheAdmin.getEnvironment().getType("ContactsTest").getId());
            ae.removeAssignment(CacheAdmin.getEnvironment().getAssignment("ROOT/CONTACT_DATA").getId(),
                    true, false);
        } catch (FxNotFoundException e) {
//            ignore
        }
        logout();
    }

//    @AfterSuite
//    public void afterSuite() throws Exception {
//        login(TestUsers.SUPERVISOR);
//
//        logout();
//    }

    @Test(invocationCount = 2)
    public void typeCreateDeleteUpdate() throws Exception {
        long testId = -1;
        try {
            testId = te.save(FxTypeEdit.createNew("TestCD", new FxString("description..."),
                    CacheAdmin.getEnvironment().getACL(ACL.Category.STRUCTURE.getDefaultId()), null)
                    .setCheckValidity(false));
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
                if (a.getCategory() == ACL.Category.STRUCTURE) {
                    structACL = a;
                    break;
                }
            assert structACL != null : "No available ACL for structure found!";
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
            assert false : e.getMessage();
        }
        FxTypeEdit testEdit = new FxTypeEdit(CacheAdmin.getEnvironment().getType("TestCD"));
        long propCount = testEdit.getAssignedProperties().size();
        testEdit.setName("TestCDNewName");
        testEdit.setPermissions((byte) 0); //clear permissions
        testEdit.setUseInstancePermissions(true);
        testEdit.setDescription(new FxString("Changed description"));
        testEdit.setCheckValidity(true);
        testEdit.setState(TypeState.Unavailable);
        te.save(testEdit);
        FxType testType = CacheAdmin.getEnvironment().getType("TestCDNewName");
        assert testType.getAssignedProperties().size() == propCount : "Property count mismatch";
        assert testType.getAssignment("/TestDirectProperty2").getXPath().equals("TESTCDNEWNAME/TESTDIRECTPROPERTY2") :
                "Expected [TESTCDNEWNAME/TESTDIRECTPROPERTY2] but got: [" + testType.getAssignment("/TestDirectProperty2").getXPath() + "]";
        assert testType.usePermissions();
        assert testType.useInstancePermissions();
        assert !testType.usePropertyPermissions();
        assert !testType.useStepPermissions();
        assert !testType.useTypePermissions();
        assert testType.isCheckValidity();
        assert testType.getState() == TypeState.Unavailable;

        try {
            assert testId == testType.getId() : "Wrong id for type!";
            te.remove(testType.getId());
            try {
                CacheAdmin.getEnvironment().getType("TestCD");
                assert false : "TestCD could be loaded after remove!";
            } catch (Exception e) {
                //ok
            }
        } catch (FxApplicationException e) {
            assert false : e.getMessage();
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
            testId = te.save(FxTypeEdit.createNew("TestCDP", new FxString("description..."), CacheAdmin.getEnvironment().getACLsByCategory(ACL.Category.STRUCTURE).get(0), null));
//            testId = te.create(1, 1, 1, new ArrayList<Mandator>(2), "TestCDP",
//                    new FxString("description..."), null, false,
//                    TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                    true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
            FxString desc = new FxString("Test data structure");
            desc.setTranslation(FxLanguage.GERMAN, "Testdaten Strukturen");
            FxString hint = new FxString("Hint text ...");
            ACL structACL = null;
            for (ACL a : CacheAdmin.getEnvironment().getACLs())
                if (a.getCategory() == ACL.Category.STRUCTURE) {
                    structACL = a;
                    break;
                }
            assert structACL != null : "No available ACL for structure found!";
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
            assert false : e.getMessage();
        }
        FxType testType = CacheAdmin.getEnvironment().getType("TestCDP");
        assert testId == testType.getId() : "Wrong id for type!";

        long testDerivedId;
        testDerivedId = te.save(FxTypeEdit.createNew("TestCDDerived", new FxString("description..."),
                CacheAdmin.getEnvironment().getACLsByCategory(ACL.Category.STRUCTURE).get(0), testType).setEnableParentAssignments(false));
//        testDerivedId = te.create(1, 1, 1, new ArrayList<Mandator>(2), "TestCDDerived",
//                new FxString("description..."), testType, false,
//                TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
        FxType testTypeDerived = CacheAdmin.getEnvironment().getType("TestCDDerived");
        assert testTypeDerived.getId() == testDerivedId : "Derived type id does not match!";
        assert testTypeDerived.getParent().getId() == testType.getId() : "Derived types parent does not match!";
        FxAssignment dp = null;
        try {
            dp = testTypeDerived.getAssignment("/TestDirectProperty1");
        } catch (Exception e) {
            assert false : "Failed to retrieved derived property: " + e.getMessage();
        }
        assert !dp.isEnabled() : "Property should be disabled!";
        FxPropertyAssignmentEdit dpe = new FxPropertyAssignmentEdit((FxPropertyAssignment) dp);
        dpe.setEnabled(true);
        ae.save(dpe, false);
        testTypeDerived = CacheAdmin.getEnvironment().getType("TestCDDerived");
        dp = testTypeDerived.getAssignment("/TestDirectProperty1");
        assert dp.isEnabled() : "Property should be enabled!";


        try {
            dp = testTypeDerived.getAssignment("/TestGroup/TestDirectProperty2");
        } catch (Exception e) {
            assert false : "Failed to retrieved derived property: " + e.getMessage();
        }
        assert !dp.isEnabled() : "Property should be disabled!";

        FxGroupAssignmentEdit gae = new FxGroupAssignmentEdit((FxGroupAssignment) testTypeDerived.getAssignment("/TestGroup"));
        gae.setEnabled(true);
        ae.save(gae, false);
        testTypeDerived = CacheAdmin.getEnvironment().getType("TestCDDerived");
        dp = testTypeDerived.getAssignment("/TestGroup/TestDirectProperty2");
        assert dp.isEnabled() : "Property should be enabled!";

        try {
            te.remove(testTypeDerived.getId());
            try {
                CacheAdmin.getEnvironment().getType("TestCDDerived");
                assert false : "TestCDDerived could be loaded after remove!";
            } catch (Exception e) {
                //ok
            }
            te.remove(testType.getId());
            try {
                CacheAdmin.getEnvironment().getType("TestCDP");
                assert false : "TestCD could be loaded after remove!";
            } catch (Exception e) {
                //ok
            }
        } catch (FxApplicationException e) {
            assert false : e.getMessage();
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
        long aid = acl.create("ContactsTest", new FxString("Contact ACL"), Mandator.MANDATOR_FLEXIVE, "#AA0000", "ACL for ContactsTest", ACL.Category.STRUCTURE);
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
        Context c = new InitialContext();
        UserTransaction ut = (UserTransaction) c.lookup("UserTransaction");
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
            testId = te.save(FxTypeEdit.createNew("TestAssignmentRemove", new FxString("description..."), CacheAdmin.getEnvironment().getACLsByCategory(ACL.Category.STRUCTURE).get(0), null));
//                    te.create(1, 1, 1, new ArrayList<Mandator>(2), "TestAssignmentRemove",
//                    new FxString("description..."), null, false,
//                    TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                    true, LanguageMode.Multiple, TypeState.Available, (byte) 0, true, 0, 0, 0, 0);
            desc.setTranslation(FxLanguage.GERMAN, "Testdaten Strukturen");
            structACL = null;
            for (ACL a : CacheAdmin.getEnvironment().getACLs())
                if (a.getCategory() == ACL.Category.STRUCTURE) {
                    structACL = a;
                    break;
                }
            assert structACL != null : "No available ACL for structure found!";
            FxPropertyEdit pe = FxPropertyEdit.createNew("TestAssignmentRemoveProperty1", desc, hint, true, new FxMultiplicity(0, 1),
                    true, structACL, FxDataType.String1024, new FxString(FxString.EMPTY),
                    true, null, null, null);
            ae.createProperty(testId, pe, "/");
        } catch (FxApplicationException e) {
            assert false : e.getMessage();
        }
        FxType testType = CacheAdmin.getEnvironment().getType("TestAssignmentRemove");
        assert testId == testType.getId() : "Wrong id for type!";
        FxPropertyAssignment pa = null;
        long rmId = -1;
        try {
            pa = (FxPropertyAssignment) testType.getAssignment("/TestAssignmentRemoveProperty1");
            assert pa != null : "Created Property Assignment is null!";
            rmId = pa.getId();
        } catch (Exception e) {
            assert false : "Created Property Assignment does not exist!";
        }
        FxPropertyAssignmentEdit pae = FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty2", "/");
        try {
            long paId = ae.save(pae, true);
            pa = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(paId);
        } catch (Exception e) {
            assert false : "Failed to retrieve derived assignment(2): " + e.getMessage();
        }
        //create derived assignment over 3 hops
        ae.save(FxPropertyAssignmentEdit.createNew(pa, testType, "TestAssignmentRemoveProperty3", "/"), true);
        //check base's
        testType = CacheAdmin.getEnvironment().getType(testId);
        assert testType.getAssignment("/TestAssignmentRemoveProperty1").getBaseAssignmentId() == FxAssignment.NO_PARENT;
        assert testType.getAssignment("/TestAssignmentRemoveProperty2").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty1").getId();
        assert testType.getAssignment("/TestAssignmentRemoveProperty3").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty2").getId();
        ae.removeAssignment(rmId, true, true);
        assert CacheAdmin.getEnvironment().getSystemInternalRootPropertyAssignments().size() == CacheAdmin.getEnvironment().getType(testId).getAssignedProperties().size() : "Expected to have " + CacheAdmin.getEnvironment().getSystemInternalRootPropertyAssignments().size() + " assignments to the type!";

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
        assert testType.getAssignment("/TestAssignmentRemoveProperty1").getBaseAssignmentId() == FxAssignment.NO_PARENT;
        assert testType.getAssignment("/TestAssignmentRemoveProperty2").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty1").getId();
        assert testType.getAssignment("/TestAssignmentRemoveProperty3").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty2").getId();

        ae.removeAssignment(testType.getAssignment("/TestAssignmentRemoveProperty1").getId(), false, false);
        testType = CacheAdmin.getEnvironment().getType(testId);
        assert testType.getAssignment("/TestAssignmentRemoveProperty2").getBaseAssignmentId() == FxAssignment.NO_PARENT;
        assert testType.getAssignment("/TestAssignmentRemoveProperty3").getBaseAssignmentId() == testType.getAssignment("/TestAssignmentRemoveProperty2").getId();

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
}
