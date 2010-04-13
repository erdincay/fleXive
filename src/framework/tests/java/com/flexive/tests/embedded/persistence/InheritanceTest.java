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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.tests.embedded.FxTestUtils;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Tests for structure inheritance
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "structure", "inheritance"})
public class InheritanceTest extends StructureTestBase {

    private final static String TYPE_NAME = "INHERITANCETEST_" + RandomStringUtils.random(16, true, true);

    private ACL aclStructure, aclWorkflow, aclContent;
    private long typeId;

    private void createBaseProperty(String name, String XPath) throws FxApplicationException {
        ass.createProperty(
                typeId,
                FxPropertyEdit.createNew(name, new FxString("InheritanceTest UnitTest property " + name),
                        new FxString("hint..."), FxMultiplicity.MULT_0_1, aclStructure, FxDataType.String1024),
                XPath);
    }

    private void createDerivedProperty(long typeId, String name, String XPath) throws FxApplicationException {
        ass.createProperty(
                typeId,
                FxPropertyEdit.createNew(name, new FxString("InheritanceTest UnitTest property " + name),
                        new FxString("hint..."), FxMultiplicity.MULT_0_1, aclStructure, FxDataType.String1024),
                XPath);
    }

    private void createBaseGroup(String name, String XPath, GroupMode mode) throws FxApplicationException {
        ass.createGroup(
                typeId,
                FxGroupEdit.createNew(name, new FxString("InheritanceTest UnitTest group " + name),
                        new FxString("hint..."), true, FxMultiplicity.MULT_0_1).setAssignmentGroupMode(mode),
                XPath);
    }

    private void createDerivedGroup(long typeId, String name, String XPath, GroupMode mode) throws FxApplicationException {
        ass.createGroup(
                typeId,
                FxGroupEdit.createNew(name, new FxString("InheritanceTest UnitTest group " + name),
                        new FxString("hint..."), true, FxMultiplicity.MULT_0_1).setAssignmentGroupMode(mode),
                XPath);
    }

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        super.init();
        login(TestUsers.SUPERVISOR);
        //create the base type
        ACL[] tmp = FxTestUtils.createACLs(
                new String[]{
                        "STRUCTURE_" + RandomStringUtils.random(16, true, true),
                        "WORKFLOW_" + RandomStringUtils.random(16, true, true),
                        "CONTENT_" + RandomStringUtils.random(16, true, true)

                },
                new ACLCategory[]{
                        ACLCategory.STRUCTURE,
                        ACLCategory.WORKFLOW,
                        ACLCategory.INSTANCE
                },
                TestUsers.getTestMandator()
        );
        aclStructure = tmp[0];
        aclWorkflow = tmp[1];
        aclContent = tmp[2];
        typeId = type.save(FxTypeEdit.createNew(TYPE_NAME, new FxString("Test data"), aclStructure, null));

        /*
       type
       +- A_P1
       +- B
       |  +-B_P1
       |  +-B_P2
       +-C
         +-C_P1
         +-C_G1
           +- C_G1_P1
           +- C_G1_P2
        */

        createBaseProperty("A_P1", "/");

        createBaseGroup("B", "/", GroupMode.AnyOf);
        createBaseProperty("B_P1", "/B");
        createBaseProperty("B_P2", "/B");

        createBaseGroup("C", "/", GroupMode.AnyOf);
        createBaseGroup("C_G1", "/C", GroupMode.AnyOf);
        createBaseProperty("C_G1_P1", "/C/C_G1");
        createBaseProperty("C_G1_P2", "/C/C_G1");
        createBaseProperty("C_P1", "/C");
    }

    @AfterClass(dependsOnMethods = {"tearDownStructures"})
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        logout();
    }

    @AfterClass
    public void tearDownStructures() throws Exception {
        long typeId = CacheAdmin.getEnvironment().getType(TYPE_NAME).getId();
        co.removeForType(typeId);
        type.remove(typeId);
        FxTestUtils.removeACL(aclStructure, aclWorkflow, aclContent);
    }

    /**
     * Test directly derived type
     *
     * @throws Exception on errors
     */
    public void directDerivedType() throws Exception {
        String DERIVED_NAME = "DERIVED_" + RandomStringUtils.random(5, true, true);
        FxTypeEdit te = FxTypeEdit.createNew(DERIVED_NAME, new FxString("Description..."),
                aclStructure, CacheAdmin.getEnvironment().getType(TYPE_NAME));
        long derivedId = -1;
        try {
            derivedId = type.save(te);
            FxType derived = CacheAdmin.getEnvironment().getType(DERIVED_NAME);
            checkDerived(derived,
                    "/A_P1",
                    "/B", "/B/B_P1", "/B/B_P2",
                    "/C", "/C/C_P1", "/C/C_G1", "/C/C_G1/C_G1_P1", "/C/C_G1/C_G1_P2");
            //disable a derived assignment and test vs the parent type
            FxContent pc = co.initialize(typeId);
            FxContent dc = co.initialize(derivedId);
            FxString data = new FxString(false, "abc");
            final String TEST_XPATH = "/C/C_G1/C_G1_P1";
            pc.setValue(TEST_XPATH, data);
            dc.setValue(TEST_XPATH, data);
            FxPK pk_p = co.save(pc);
            FxPK pk_d = co.save(dc);
            FxPropertyAssignmentEdit fxa_d = new FxPropertyAssignmentEdit((FxPropertyAssignment) derived.getAssignment("/C/C_G1/C_G1_P1"));
            fxa_d.setEnabled(false);
            ass.save(fxa_d, false);
            derived = CacheAdmin.getEnvironment().getType(DERIVED_NAME);
            Assert.assertTrue(!derived.getAssignment(TEST_XPATH).isEnabled(), "Expected [" + TEST_XPATH + "] to be disabled!");
            FxContent dc_dis = co.load(pk_d);
            pc = co.load(pk_p);
            try {
                dc_dis.getPropertyData(TEST_XPATH);
                Assert.fail(TEST_XPATH + " should not exist after being disabled!");
            } catch (FxRuntimeException e) {
                if (!(e.getConverted() instanceof FxNotFoundException)) {
                    throw e;
                }
                //expected!
            }
            Assert.assertTrue(pc.getPropertyData(TEST_XPATH).getValue().equals(data), "Wrong data for parent instance!");
            //disable /B and subgroups
            FxGroupAssignmentEdit fxg_d = new FxGroupAssignmentEdit((FxGroupAssignment) derived.getAssignment("/B"));
            fxg_d.setEnabled(false);
            ass.save(fxg_d, false);
            derived = CacheAdmin.getEnvironment().getType(DERIVED_NAME);
            String[] tests = {"/B", "/B/B_P1", "/B/B_P2"};
            for (String xpath : tests)
                Assert.assertTrue(!derived.getAssignment(xpath).isEnabled(), "Expected [" + xpath + "] to be disabled!");
            //enable again
            fxg_d = new FxGroupAssignmentEdit((FxGroupAssignment) derived.getAssignment("/B"));
            fxg_d.setEnabled(true);
            ass.save(fxg_d, false);
            derived = CacheAdmin.getEnvironment().getType(DERIVED_NAME);
            for (String xpath : tests)
                Assert.assertTrue(derived.getAssignment(xpath).isEnabled(), "Expected [" + xpath + "] to be enabled!");
        } finally {
            if (derivedId != -1) {
                co.removeForType(derivedId);
                type.remove(derivedId);
            }
        }
    }

    /**
     * Test inheritance of assignments in inherited types of inherited types
     *
     * @throws Exception on errors
     */
    public void assignmentInheritance() throws Exception {
        /*
       type
       +- A_P1
       +- A_P2 <-- added in 1st inheritance, should exist in 2nd
       +- B
       |  +-B_P1
       |  +-B_P2
       |  +-B_P3 <-- added in 1st inheritance, should exist in 2nd
       +-C
       | +-C_P1
       | +-C_G1
       | | +- C_G1_P1
       | | +- C_G1_P2
       | +-C_G2 <-- added in 1st inheritance, should exist in 2nd
       |   +- C_G2_P1 <-- added in 1st inheritance, should exist in 2nd
       +-D <-- added in 1st inheritance, should exist in 2nd
         +-D_P1 <-- added in 1st inheritance, should exist in 2nd
        */
        final String DERIVED1_NAME = "DERIVED1_" + RandomStringUtils.random(5, true, true);
        final String DERIVED2_NAME = "DERIVED2_" + RandomStringUtils.random(5, true, true);
        long derived1Id = -1;
        long derived2Id = -1;
        try {
            derived1Id = type.save(FxTypeEdit.createNew(DERIVED1_NAME, new FxString("Description1..."),
                    aclStructure, CacheAdmin.getEnvironment().getType(TYPE_NAME)));
            derived2Id = type.save(FxTypeEdit.createNew(DERIVED2_NAME, new FxString("Description2..."),
                    aclStructure, CacheAdmin.getEnvironment().getType(DERIVED1_NAME)));
            derivedSubCheck("/A_P2", derived1Id, DERIVED1_NAME, DERIVED2_NAME);
            derivedSubCheck("/B/B_P3", derived1Id, DERIVED1_NAME, DERIVED2_NAME);
            createDerivedGroup(derived1Id, "C_G2", "/C", GroupMode.AnyOf);
            derivedSubCheck("/C/C_G2/C_G2_P1", derived1Id, DERIVED1_NAME, DERIVED2_NAME);
            createDerivedGroup(derived1Id, "D", "/", GroupMode.AnyOf);
            derivedSubCheck("/D/D_P1", derived1Id, DERIVED1_NAME, DERIVED2_NAME);
            //Add A_P3 to the base type
            createBaseProperty("A_P3", "/");
            checkDerived(CacheAdmin.getEnvironment().getType(DERIVED1_NAME), "/A_P3");
            checkDerived(CacheAdmin.getEnvironment().getType(DERIVED2_NAME), "/A_P3");
            FxType base = CacheAdmin.getEnvironment().getType(TYPE_NAME);
            long ap3_id = base.getAssignment("/A_P3").getId();
            //remove A_P3 from base type, 'breaking' inheritance and making derived1 the new parent of this property
            ass.removeAssignment(ap3_id, true, false);
            FxPropertyAssignment pa_d1 = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(DERIVED1_NAME + "/A_P3");
            FxPropertyAssignment pa_d2 = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(DERIVED2_NAME + "/A_P3");
            Assert.assertTrue(!pa_d1.isDerivedAssignment(), DERIVED1_NAME + "/A_P3 must not be a derived assignment!");
            Assert.assertTrue(pa_d2.isDerivedAssignment(), DERIVED2_NAME + "/A_P3 expected be a derived assignment!");
            Assert.assertTrue(pa_d2.getBaseAssignmentId() == pa_d1.getId(), "Expected " + DERIVED2_NAME + "/A_P3 to be derived from " + DERIVED1_NAME + "/A_P3!");
            removeAndCheckDerived(CacheAdmin.getEnvironment().getType(DERIVED2_NAME), "/A_P3");
        } finally {
            if (derived2Id != -1) {
                co.removeForType(derived2Id);
                type.remove(derived2Id);
            }
            if (derived1Id != -1) {
                co.removeForType(derived1Id);
                type.remove(derived1Id);
            }
        }
    }

    /**
     * Check if a derived type has really inherited the test property
     *
     * @param testProperty  XPath of the property to test
     * @param derived1Id    id of the type derived from the "base" type
     * @param DERIVED1_NAME name of the type derived from the "base" type
     * @param DERIVED2_NAME name of the type derived from derived1
     * @throws FxApplicationException on errors
     */
    private void derivedSubCheck(String testProperty, long derived1Id, String DERIVED1_NAME, String DERIVED2_NAME) throws FxApplicationException {
        createDerivedProperty(derived1Id,
                testProperty.substring(testProperty.lastIndexOf("/") + 1),
                testProperty.substring(0, (testProperty.lastIndexOf("/") > 0 ? testProperty.lastIndexOf("/") : 1)));
        FxType derived1 = CacheAdmin.getEnvironment().getType(DERIVED1_NAME);
        try {
            Assert.assertTrue(derived1.getAssignment(testProperty) instanceof FxPropertyAssignment, "Expected " + testProperty + " to be a property assignment!");
        } catch (Exception e) {
            Assert.fail("Failed to lookup " + testProperty + " in derived1 type! msg=" + e.getClass() + ":" + e.getMessage());
        }
        FxType derived2 = CacheAdmin.getEnvironment().getType(DERIVED2_NAME);
        checkDerived(derived2, testProperty); //should exist for derived2 as well!
    }

    /**
     * Check if the given xpath's are marked as derived and not removeable
     *
     * @param derived the type to check against
     * @param xpaths  xpath's that have to be derived
     * @throws FxApplicationException on errors
     */
    private void checkDerived(FxType derived, String... xpaths) throws FxApplicationException {
        for (String xpath : xpaths) {
            FxAssignment fxa = derived.getAssignment(xpath);
            Assert.assertTrue(fxa.isDerivedAssignment(), "Expected [" + xpath + "] to be a derived assignment!");
            try {
                ass.removeAssignment(fxa.getId(), true, false);
                Assert.fail("Removal of derived assignment [" + xpath + "] should not be possible!");
            } catch (FxApplicationException e) {
                if (!(e instanceof FxRemoveException))
                    throw e;
            }
        }
    }

    /**
     * Check if the given xpath's are marked as derived and not ("normal") removable, then remove it
     *
     * @param derived the type to check against
     * @param xpaths  xpath's that have to be derived
     * @throws FxApplicationException on errors
     */
    private void removeAndCheckDerived(FxType derived, String... xpaths) throws FxApplicationException {
        for (String xpath : xpaths) {
            FxAssignment fxa = derived.getAssignment(xpath);
            Assert.assertTrue(fxa.isDerivedAssignment(), "Expected [" + xpath + "] to be a derived assignment!");
            try {
                ass.removeAssignment(fxa.getId(), true, false);
                Assert.fail("Removal of derived assignment [" + xpath + "] should not be possible!");
            } catch (FxApplicationException e) {
                if (!(e instanceof FxRemoveException))
                    throw e;
            }
            ass.removeAssignment(fxa.getId());
            try {
                CacheAdmin.getEnvironment().getType(derived.getId()).getAssignment(xpath);
                Assert.fail("Expected that [" + xpath + "] has been removed!");
            } catch (FxRuntimeException e) {
                //expected
            }
        }
    }
}
