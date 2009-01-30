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
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxCreateException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
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

/**
 * Test for the group mode (one-of and any-of) of group assignments
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "structure"})
public class GroupModeTest extends StructureTestBase {

    private final static String TYPE_NAME = "GROUPMODETEST_" + RandomStringUtils.random(16, true, true);

    private ACL aclStructure, aclWorkflow, aclContent;
    private long typeId;

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

        createGroup("A", "/", GroupMode.AnyOf);
        createProperty("A_P1", "/A");
        createProperty("A_P2", "/A");

        createGroup("B", "/", GroupMode.OneOf);
        createProperty("B_P1", "/B");
        createProperty("B_P2", "/B");

        createGroup("C", "/", GroupMode.OneOf);
        createGroup("C_G1", "/C", GroupMode.OneOf);
        createProperty("C_G1_P1", "/C/C_G1");
        createProperty("C_G1_P2", "/C/C_G1");
        createProperty("C_P1", "/C");
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

    private void createProperty(String name, String XPath) throws FxApplicationException {
        ass.createProperty(
                typeId,
                FxPropertyEdit.createNew(name, new FxString("GroupMode UnitTest property " + name),
                        new FxString("hint..."), FxMultiplicity.MULT_0_1, aclStructure, FxDataType.String1024),
                XPath);
    }

    private void createGroup(String name, String XPath, GroupMode mode) throws FxApplicationException {
        ass.createGroup(
                typeId,
                FxGroupEdit.createNew(name, new FxString("GroupMode UnitTest group " + name),
                        new FxString("hint..."), true, FxMultiplicity.MULT_0_1).setAssignmentGroupMode(mode),
                XPath);
    }

    @Test
    public void groupModeTest() throws Exception {
        FxContent test = co.initialize(typeId);
        assert test.getGroupData("/B").getCreateableChildren(false).size() == 2 : "2 creatable children expected!";
        FxString testValue = new FxString(false, "Test...");
        test.setValue("/A/A_P1", testValue);
        test.setValue("/A/A_P2", testValue);
        test.setValue("/B/B_P1", testValue);
        test.setValue("/C/C_P1", testValue);
        try {
            test.setValue("/B/B_P2", testValue);
            assert false : "One-Of groups used twice!";
        } catch (FxCreateException e) {
            //ok
        }
        assert test.getGroupData("/B").getCreateableChildren(false).size() == 0 : "No creatable children should be returned!";
        FxPK pk = co.save(test);
        FxContent loaded = co.load(pk);
        assert loaded.getValue("/A/A_P1").equals(testValue);
        assert loaded.getValue("/A/A_P2").equals(testValue);
        assert loaded.getValue("/B/B_P1").equals(testValue);
        try {
            loaded.setValue("/B/B_P2", testValue);
            assert false : "One-Of groups used twice!";
        } catch (FxCreateException e) {
            //ok
        }
        loaded.remove("/B/B_P1");
        loaded.setValue("/B/B_P2", testValue);
        try {
            loaded.setValue("/C/C_G1/C_G1_P1", testValue);
            assert false : "One-Of groups used twice!";
        } catch (FxCreateException e) {
            //ok
        }
        loaded.remove("/C/C_P1");
        loaded.setValue("/C/C_G1/C_G1_P1", testValue);
        try {
            loaded.setValue("/C/C_G1/C_G1_P2", testValue);
            assert false : "One-Of groups used twice!";
        } catch (FxCreateException e) {
            //ok
        }
        co.save(loaded);
        loaded = co.load(pk);
        assert loaded.getValue("/B/B_P2").equals(testValue);
        FxContent random = co.initialize(typeId).randomize();
        pk = co.save(random);
        co.load(pk);
    }

}
