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
import org.testng.Assert;
import static org.testng.Assert.fail;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
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
    private ContentEngine ce;

    @BeforeClass
    public void beforeClass() throws FxLookupException, FxLoginFailedException, FxAccountInUseException {
        te = EJBLookup.getTypeEngine();
        ae = EJBLookup.getAssignmentEngine();
        acl = EJBLookup.getAclEngine();
        ce = EJBLookup.getContentEngine();
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
        FxPropertyAssignment baseAss = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(baseAssId);
        Assert.assertTrue(baseAss.getParentGroupAssignment().getId() == baseGrp.getId());

        //create derived group assignment with derived sub assignments
        long derivedGrpId = ae.save(FxGroupAssignmentEdit.createNew(baseGrp, CacheAdmin.getEnvironment().getType(testId), "TestRemoveGroupDerived", "/"), true);
        FxGroupAssignment derivedGrp = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(derivedGrpId);

        //verify that group is derived
        Assert.assertTrue(derivedGrp.isDerivedAssignment() && derivedGrp.getBaseAssignmentId() == baseGrp.getId());

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

    /**
     * Tests overriding the multilanguage setting for a property
     *
     * @throws FxNotFoundException         on errors
     * @throws FxInvalidParameterException on errors
     */
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

    /**
     * Tests setting the default language values f. properties
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void defaultValueLanguageTest() throws FxApplicationException {
        FxPropertyEdit prop = FxPropertyEdit.createNew("testDefLang",
                new FxString("Test Priority"),
                new FxString("Priority"),
                FxMultiplicity.MULT_1_1,
                CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
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
    @Test
    public void instanceMultiplicityTest() throws FxApplicationException {
        SelectListEngine se = EJBLookup.getSelectListEngine();
        long typeId = createTestTypeAndProp("STRUCTTESTTYPE", "Struct test type label", true);
        long typeIdRefTest = createTestTypeAndProp("STRUCTTESTTYPE_TWO", "Struct test type label 2", false);
        long selListId = -1;
        try { // test setting some of property attributes (for db update coverage)
            FxProperty p = CacheAdmin.getEnvironment().getProperty("TESTPROP");
            FxPropertyEdit pEdit = p.asEditable();
            ACL propACL = CacheAdmin.getEnvironment().getACL(ACLCategory.SELECTLIST.getDefaultId());
            ACL defacl = CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId());
            FxSelectListEdit testList = FxSelectListEdit.createNew("Test_selectlist111999", new FxString("test selectlist"), new FxString("test selectlist"), false, defacl, defacl);
            FxSelectListItemEdit.createNew("ITEM1", defacl, testList, new FxString("item_1"), "value_1", "#000000");
            FxSelectListItemEdit.createNew("ITEM2", defacl, testList, new FxString("item_2"), "value_2", "#000000");
            selListId = se.save(testList);
            testList = CacheAdmin.getEnvironment().getSelectList(selListId).asEditable();
            FxSelectListItem defItem = CacheAdmin.getEnvironment().getSelectListItem(testList, "ITEM1");

            pEdit.setDataType(FxDataType.SelectOne);
            pEdit.setReferencedType(CacheAdmin.getEnvironment().getType(typeIdRefTest));
            pEdit.setUniqueMode(UniqueMode.Type);
            pEdit.setACL(propACL);
            pEdit.setReferencedList(CacheAdmin.getEnvironment().getSelectList(selListId));
            pEdit.setOverrideMultiLang(true); // also needed for assignment tests
            ae.save(pEdit);

            p = CacheAdmin.getEnvironment().getProperty("TESTPROP");
            assertEquals(p.getDataType().getValueClass(), FxSelectOne.class);
            assertEquals(p.getReferencedType().getId(), typeIdRefTest);
            assertEquals(p.getUniqueMode(), UniqueMode.Type);
            assertEquals(p.getACL().getCategory(), ACLCategory.SELECTLIST);
            assertEquals(p.getReferencedList().getName(), "Test_selectlist111999");
            assertTrue(p.mayOverrideMultiLang());

            // test property assignments
            FxPropertyAssignment assProp = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment("STRUCTTESTTYPE/TESTPROP");
            FxPropertyAssignmentEdit assEdit = assProp.asEditable();
            assEdit.setACL(propACL);
            assEdit.setMultiLang(true);
            assEdit.setLabel(new FxString(true, "assignment_label_123456"));
            assEdit.setDefaultLanguage(FxLanguage.ENGLISH);
            assEdit.setDefaultValue(new FxSelectOne(true, defItem));
            ae.save(assEdit, false);

            assProp = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment("STRUCTTESTTYPE/TESTPROP");
            assertEquals(assProp.getACL(), propACL);
            assertTrue(assProp.isMultiLang());
            assertEquals(assProp.getLabel().toString(), "assignment_label_123456");
            assertEquals(assProp.getDefaultLanguage(), FxLanguage.ENGLISH);
            FxValue val = assProp.getDefaultValue();
            assertEquals(val.getValueClass(), FxSelectListItem.class);
            FxSelectOne selOne = (FxSelectOne)val;
            FxSelectListItem item = CacheAdmin.getEnvironment().getSelectListItem(testList, "ITEM1");
//            assertEquals(selOne.getStringValue(item), ""+item.getId());
            assertEquals(selOne.fromString(""+item.getId()).getData(), "value_1");
        } finally {
            // clean up
            te.remove(typeId);
            te.remove(typeIdRefTest);
            if(selListId != -1) {
                se.remove(CacheAdmin.getEnvironment().getSelectList(selListId));
            }
        }
        // remaining tests for multiplicity settings etc.
        typeId = createTestTypeAndProp("STRUCTTESTTYPE", "Struct test type label", true);
        FxPK contentPK1 = createTestContent(typeId, "/TESTPROP", "Testdata 1");
        FxPK contentPK2 = createTestContent(typeId, "/TESTPROP", "Testdata 2");
        try {
            FxPropertyAssignmentEdit assEdit = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment("STRUCTTESTTYPE/TESTPROP")).asEditable();
            FxProperty p = CacheAdmin.getEnvironment().getProperty("TESTPROP");
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

            p = CacheAdmin.getEnvironment().getProperty("TESTPROP");
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
                fail("Altering the minimum multiplicity of a property for which content exists should have thrown an exception");
            } catch (FxUpdateException e) {
                // expected
            }

            pEdit.setOverrideMultiplicity(true); // reset
            pEdit.setMultiplicity(new FxMultiplicity(0,2));
            ae.save(pEdit);
            assEdit = ((FxPropertyAssignment)CacheAdmin.getEnvironment().getAssignment("STRUCTTESTTYPE/TESTPROP")).asEditable();
            assEdit.setMultiplicity(new FxMultiplicity(0, 2));
            ae.save(assEdit, false);
            // create an additional content instance for the same assignment
            FxContent co = ce.load(contentPK2);
            co.setValue("/TESTPROP[2]", new FxString(false, "Testdata 2.2"));
            ce.save(co);

            pEdit = CacheAdmin.getEnvironment().getProperty("TESTPROP").asEditable();
            pEdit.setOverrideMultiplicity(false);
            pEdit.setMultiplicity(new FxMultiplicity(0,1));
            try {
                ae.save(pEdit);
                fail("Changing the maximum property multiplicity for assignment counts > new set maximum should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            pEdit.setMultiplicity(new FxMultiplicity(3,3));
            try {
                ae.save(pEdit);
                fail("Changing the minimum property multiplicity for assignment counts > new set minimum should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            // add another content instance
            pEdit.setOverrideMultiplicity(false);
            pEdit.setMultiplicity(new FxMultiplicity(0,3));
            ae.save(pEdit);
            co = ce.load(contentPK2);
            co.setValue("/TESTPROP[3]", new FxString(false, "Testdata 2.3"));
            ce.save(co);
            
            pEdit = CacheAdmin.getEnvironment().getProperty("TESTPROP").asEditable();
            pEdit.setOverrideMultiplicity(false);
            pEdit.setMultiplicity(new FxMultiplicity(0,2));
            try {
                ae.save(pEdit);
                fail("Changing the maximum property multiplicity for assignment counts > new set maximum should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            pEdit.setOverrideMultiplicity(false); // reset
            pEdit.setMultiplicity(new FxMultiplicity(0,3));
            ae.save(pEdit);

            // test multiplicity assignment via #save(FxAssignment)
            assEdit.setMultiplicity(new FxMultiplicity(0,5));
            try {
                ae.save(assEdit, false);
                fail("Overriding the base multiplicity should have failed due to setOverrideMultiplicity(false)");
            } catch (FxUpdateException e) {
                // expected
            }
            pEdit.setOverrideMultiplicity(true); // reset
            pEdit.setMultiplicity(new FxMultiplicity(0,3));
            ae.save(pEdit);

            assEdit.setMultiplicity(new FxMultiplicity(0,5));
            ae.save(assEdit, false);


            assEdit = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment("STRUCTTESTTYPE/TESTPROP")).asEditable();
            assertEquals(assEdit.getMultiplicity().getMin(), 0);
            assertEquals(assEdit.getMultiplicity().getMax(), 5);

            assEdit.setMultiplicity(new FxMultiplicity(4, 4));
            try {
                ae.save(assEdit, false);
                fail("Assignment of an invalid minimum FxMultiplicity of a property assignment for which content exists should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            // call the #save function again w/ changes to the property's attributes
            pEdit.setLabel(new FxString(true, FxLanguage.DEFAULT.getId(), "TESTPROP new label"));
            pEdit.setMultiplicity(new FxMultiplicity(0, 5));
            pEdit.setOverrideMultiplicity(true);
            pEdit.setHint(new FxString(true, FxLanguage.DEFAULT.getId(), "TESTPROP hint"));
            ae.save(pEdit);

            p = CacheAdmin.getEnvironment().getProperty(p.getId()); // reload from cache
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
    @Test
    public void groupAssignmentTests() throws FxApplicationException {
        long typeId = createTestTypeAndProp("STRUCTTESTTYPE", "Struct test type label", true);
        long anotherTypeId = createTestTypeAndProp("STRUCTTESTTYPE_TWO", "STRUCTTESTTYPE_TWO_LABEL", false);
        // assign the group to our given type and also create a property for the group
        FxGroupEdit ge = FxGroupEdit.createNew("STRUCTTESTGROUP", new FxString(true, "GROUPDESCR"), new FxString(true, "GROUPHINT"), false, new FxMultiplicity(0, 1));
        ge.setAssignmentGroupMode(GroupMode.OneOf);
        ae.createGroup(typeId, ge, "/");

        ae.createProperty(typeId, FxPropertyEdit.createNew(
                "TESTGROUPPROP", new FxString(true, FxLanguage.ENGLISH, "TESTGROUPPROP"), new FxString(true, FxLanguage.ENGLISH, "TESTPROP"),
                new FxMultiplicity(0, 1), CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024).setMultiLang(false), "/STRUCTTESTGROUP");
        // create two content instances for our group (within the same assignment)
        FxPK contentPK = createTestContent(typeId, "/STRUCTTESTGROUP/TESTGROUPPROP", "Testdata 111");
        try {
            // assert group properties
            ge = CacheAdmin.getEnvironment().getGroup("STRUCTTESTGROUP").asEditable();
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

            ge = CacheAdmin.getEnvironment().getGroup("STRUCTTESTGROUP_NEW").asEditable();
            assertEquals(ge.getId(), groupId);
            assertEquals(ge.getLabel().toString(), "GROUPDESCR_NEW");
            assertEquals(ge.getHint().toString(), "GROUPHINT_NEW");
            assertEquals(ge.getMultiplicity().getMin(), 0);
            assertEquals(ge.getMultiplicity().getMax(), 2);

            // increase multiplicity of assignment as well in order to be able to add more content
            FxGroupAssignmentEdit assEdit = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP")).asEditable();
            assEdit.getGroupEdit().setOverrideMultiplicity(true);
            assEdit.setMultiplicity(new FxMultiplicity(0, 2));
            ae.save(assEdit, false);

            assEdit = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP")).asEditable();
            assertEquals(assEdit.getMultiplicity().getMin(), 0);
            assertEquals(assEdit.getMultiplicity().getMax(), 2);
            assertEquals(assEdit.getMode(), GroupMode.OneOf);

            // create another group/content instance & change the multiplicity settings of both the group and the assignments
            FxContent co = ce.load(contentPK);
            co.setValue("/STRUCTTESTGROUP[2]/TESTGROUPPROP[1]", new FxString(false, "Testdata 222"));
            ce.save(co);
            ge = CacheAdmin.getEnvironment().getGroup("STRUCTTESTGROUP_NEW").asEditable();
            ge.setOverrideMultiplicity(false);
            ge.setMultiplicity(new FxMultiplicity(0, 1));
            try {
                ae.save(ge);
                fail("Setting the override mult. flag and the maximum group multiplicity to a value less than the number of given content instances should have failed");
            } catch (FxUpdateException e) {
                // expected
            }
            ge.setOverrideMultiplicity(false);
            ge.setMultiplicity(new FxMultiplicity(3, 3));
            try {
                ae.save(ge);
                fail("Setting the override mult. flag and the minimum group multiplicity to a value greater than the number of given content instances should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            // test multiplicity setting failure code blocks
            ge.setOverrideMultiplicity(false); // reset
            ge.setMultiplicity(new FxMultiplicity(0,3));
            ae.save(ge);
            co = ce.load(contentPK);
            co.setValue("/STRUCTTESTGROUP[3]/TESTGROUPPROP[1]", new FxString(false, "Testdata 333"));
            ce.save(co);

            ge = CacheAdmin.getEnvironment().getGroup("STRUCTTESTGROUP_NEW").asEditable();
            ge.setMultiplicity(new FxMultiplicity(4,4));
            try {
                ae.save(ge);
                fail("Setting the min. group multiplicity to a value > the current # of the assignment's content instances should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            ge.setMultiplicity(new FxMultiplicity(0,2));
            try {
                ae.save(ge);
                fail("Setting the max. group multiplicity to a value < the current # of the assignment's content instances should have failed");
            } catch (FxUpdateException e) {
                // expected
            }

            ge.setMultiplicity(new FxMultiplicity(0,3)); // reset
            ge.setOverrideMultiplicity(true);
            ae.save(ge);

            // test more group assignment code blocks
            assEdit = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("STRUCTTESTTYPE/STRUCTTESTGROUP")).asEditable();
            assEdit.setLabel(new FxString(true, "ASSIGNMENTLABEL_XX123456"));
            assEdit.setHint(new FxString(true, "ASSIGNMENTHINT_XX123456"));
            assEdit.setXPath("/STRUCTTESTGROUP_NEW_PATH");
            assEdit.setMode(GroupMode.AnyOf);

            ae.save(assEdit, false);

            assEdit = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("/STRUCTTESTGROUP_NEW_PATH")).asEditable();
            assertEquals(assEdit.getLabel().toString(), "ASSIGNMENTLABEL_XX123456");
            assertEquals(assEdit.getHint().toString(), "ASSIGNMENTHINT_XX123456");
            assertEquals(assEdit.getMode(), GroupMode.AnyOf);

            // set the group mode and the alias
            assEdit.setMode(GroupMode.OneOf);
            try {
                ae.save(assEdit, false);
                fail("Changing the a group's mode from AnyOf to OneOf for existing content instance should have failed");
            } catch (FxUpdateException e) {
                // expected
            }
            assEdit.setMode(GroupMode.AnyOf);
            assEdit.setAlias("STRUCTTESTGROUP_NEW_ALIAS");
            ae.save(assEdit, false);

            assEdit = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("/STRUCTTESTGROUP_NEW_ALIAS")).asEditable();
            assertEquals(assEdit.getAlias(), "STRUCTTESTGROUP_NEW_ALIAS");

            // disable the assignment
            assEdit.setEnabled(false);
            ae.save(assEdit, true);

            assEdit = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("/STRUCTTESTGROUP_NEW_ALIAS")).asEditable();
            assertTrue(!assEdit.isEnabled());
            assertEquals(assEdit.getDefaultMultiplicity(), 1);

            // test setting the default multiplicity to a higher value than the current
            assEdit.setDefaultMultiplicity(assEdit.getMultiplicity().getMax() + 1);
            ae.save(assEdit, false);

            assEdit = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("/STRUCTTESTGROUP_NEW_ALIAS")).asEditable();
            assertEquals(assEdit.getDefaultMultiplicity(), assEdit.getMultiplicity().getMax());

            // TODO: test set position, updategroupassignmentoptions, updategroupoptions, systeminternal flag (prop and group)
        } finally {
            // clean up
            ce.remove(contentPK);
            te.remove(typeId);
            te.remove(anotherTypeId);
        }
    }

    /**
     * Creates a test type and property
     *
     * @param typeName   the name for the type to be created
     * @param typeLabel  the label of the type
     * @param createProp if set to true, it will create a property called "TESTPROP" assigned to the type
     * @return the ids of the test type (long[0]) and its property (long[1])
     * @throws FxApplicationException on errors
     */
    private long createTestTypeAndProp(String typeName, String typeLabel, boolean createProp) throws FxApplicationException {
        long typeId = te.save(FxTypeEdit.createNew(typeName, new FxString(typeLabel),
                CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()), null));
        if (createProp) {
            ae.createProperty(typeId, FxPropertyEdit.createNew(
                    "TESTPROP", new FxString(true, FxLanguage.ENGLISH, "TESTPROP"), new FxString(true, FxLanguage.ENGLISH, "TESTPROP"),
                    new FxMultiplicity(0, 1), CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
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
    private FxPK createTestContent(long typeId, String propertyPath, String testData) throws FxApplicationException {
        FxString data = new FxString(false, testData);
        FxContent co = ce.initialize(typeId);
        FxPK contentPK = ce.save(co);
        co = ce.load(contentPK);
        co.setValue(propertyPath, data);
        contentPK = ce.save(co);

        return contentPK;
    }
}