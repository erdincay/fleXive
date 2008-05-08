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
import com.flexive.shared.FxContext;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.ACLEngine;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.UserGroupEngine;
import com.flexive.shared.security.ACL;
import static com.flexive.shared.security.ACL.Permission.*;
import com.flexive.shared.security.ACLAssignment;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserGroup;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxNoAccess;
import com.flexive.shared.value.FxString;
import com.flexive.shared.workflow.*;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUser;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;

/**
 * Testcase for content related security
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
@Test(groups = {"ejb", "content", "security"})
public class ContentSecurityTest {

    UserGroup[] groups;
    FxType type;
    Workflow workflow;
    ACL typeACL, instanceACL, property1ACL, property2ACL, editACL, liveACL;
    TestUser user;

    final static FxString PROP1_VALUE = new FxString(false, "Property 1 test value");
    final static FxString PROP2_VALUE = new FxString(false, "Property 2 test value");

    @BeforeClass
    public void setup() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException {
        user = TestUsers.createUser("SecurityTestUser", Role.BackendAccess);
        final UserGroupEngine ug = EJBLookup.getUserGroupEngine();
        final ACLEngine ae = EJBLookup.getAclEngine();
        long mandator = TestUsers.getTestMandator();
        FxContext.get().runAsSystem();
        try {
            user.createUser(TestUsers.getTestMandator(), TestUsers.getEnglishLanguageId());
            login(user);
            groups = new UserGroup[26];
            for (int i = 0; i < 26; i++)
                groups[i] = ug.load(ug.create("UG" + (i + 1), "#FFFFFF", mandator));
            typeACL = ae.load(ae.create("TYPE_" + RandomStringUtils.randomAlphanumeric(5), new FxString("Type ACL"),
                    mandator, "#FFFFFF", "Type ACL",
                    ACL.Category.STRUCTURE));
            instanceACL = ae.load(ae.create("INSTANCE_" + RandomStringUtils.randomAlphanumeric(5), new FxString("Instance ACL"),
                    mandator, "#FFFFFF", "Instance ACL",
                    ACL.Category.INSTANCE));
            property1ACL = ae.load(ae.create("PROPERTY1_" + RandomStringUtils.randomAlphanumeric(5), new FxString("Property1 ACL"),
                    mandator, "#FFFFFF", "Property1 ACL",
                    ACL.Category.STRUCTURE));
            property2ACL = ae.load(ae.create("PROPERTY2_" + RandomStringUtils.randomAlphanumeric(5), new FxString("Property2 ACL"),
                    mandator, "#FFFFFF", "Property2 ACL",
                    ACL.Category.STRUCTURE));
            editACL = ae.load(ae.create("EDIT_" + RandomStringUtils.randomAlphanumeric(5), new FxString("Edit ACL"),
                    mandator, "#FFFFFF", "Edit ACL",
                    ACL.Category.WORKFLOW));
            liveACL = ae.load(ae.create("LIVE_" + RandomStringUtils.randomAlphanumeric(5), new FxString("Live ACL"),
                    mandator, "#FFFFFF", "Live ACL",
                    ACL.Category.WORKFLOW));

            WorkflowEdit wfEdit = WorkflowEdit.createNew("Security_Workflow_" + RandomStringUtils.randomAlphanumeric(5));
            wfEdit.getSteps().add(StepEdit.createNew(StepDefinition.EDIT_STEP_ID));
            wfEdit.getSteps().add(StepEdit.createNew(StepDefinition.LIVE_STEP_ID));
            long wf = EJBLookup.getWorkflowEngine().create(wfEdit);
            workflow = CacheAdmin.getEnvironment().getWorkflow(wf);
            Step edit, live;
            Assert.assertEquals(workflow.getSteps().size(), 2);
            if (workflow.getSteps().get(0).isLiveStep()) {
                live = workflow.getSteps().get(0);
                edit = workflow.getSteps().get(1);
            } else {
                live = workflow.getSteps().get(1);
                edit = workflow.getSteps().get(0);
            }
            EJBLookup.getWorkflowStepEngine().updateStep(edit.getId(), editACL.getId());
            EJBLookup.getWorkflowStepEngine().updateStep(live.getId(), liveACL.getId());
            workflow = CacheAdmin.getEnvironment().getWorkflow(wf);
            //create routes for the route test
            wfEdit = workflow.asEditable();
            wfEdit.getRoutes().add(RouteEdit.createNew(getGroup(25), edit.getId(), live.getId()));
            wfEdit.getRoutes().add(RouteEdit.createNew(getGroup(26), live.getId(), edit.getId()));
            EJBLookup.getWorkflowEngine().update(wfEdit);
            workflow = CacheAdmin.getEnvironment().getWorkflow(wf);
            FxTypeEdit te = FxTypeEdit.createNew("SecurityTest");
            te.setWorkflow(workflow);
            te.setACL(typeACL);
            long typeId = EJBLookup.getTypeEngine().save(te);
            EJBLookup.getAssignmentEngine().createProperty(typeId,
                    FxPropertyEdit.createNew("P1", new FxString("Property 1"), new FxString("Hint"),
                            FxMultiplicity.MULT_1_1, property1ACL, FxDataType.String1024).setMultiLang(false),
                    "/");
            EJBLookup.getAssignmentEngine().createProperty(typeId,
                    FxPropertyEdit.createNew("P2", new FxString("Property 2"), new FxString("Hint"),
                            FxMultiplicity.MULT_0_1, property2ACL, FxDataType.String1024).setMultiLang(false),
                    "/");
            type = CacheAdmin.getEnvironment().getType(typeId);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
    }

    /**
     * Get a usergroup based on its number
     *
     * @param number group number
     * @return group id
     */
    private long getGroup(int number) {
        Assert.assertTrue(number > 0 && number < 27, "UserGroup has to be between 1 and 26, was: " + number);
        return groups[number - 1].getId();
    }

    /**
     * Remove all created artifacts
     *
     * @throws FxApplicationException  on errors
     * @throws FxLogoutFailedException on errors
     */
    @AfterClass
    public void teardown() throws FxApplicationException, FxLogoutFailedException {
        final UserGroupEngine ug = EJBLookup.getUserGroupEngine();
        FxContext.get().runAsSystem();
        try {
            if (type != null) {
                EJBLookup.getContentEngine().removeForType(type.getId());
                EJBLookup.getTypeEngine().remove(type.getId());
            }
            if (workflow != null)
                EJBLookup.getWorkflowEngine().remove(workflow.getId());
            for (int i = 0; i < 26; i++)
                ug.remove(groups[i].getId());
            final ACLEngine ae = EJBLookup.getAclEngine();
            ae.remove(typeACL.getId());
            ae.remove(instanceACL.getId());
            ae.remove(property1ACL.getId());
            ae.remove(property2ACL.getId());
            ae.remove(editACL.getId());
            ae.remove(liveACL.getId());
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        logout();
    }

    /**
     * Assign a test matrix of permissions to an ACL
     *
     * @param matrix the matrix
     * @param acl    the ACL
     * @throws FxApplicationException on errors
     */
    private void assignMatrix(int matrix, ACL acl) throws FxApplicationException {
        FxContext.get().runAsSystem();
        try {
            ArrayList<ACLAssignment> as = new ArrayList<ACLAssignment>(10);
            switch (matrix) {
                case 0: //Everything allowed
                    as.add(ACLAssignment.createNew(acl, UserGroup.GROUP_EVERYONE, READ, EDIT, RELATE, CREATE, DELETE, EXPORT));
                    break;
                case 1: //Type and Instance matrix
                    as.add(ACLAssignment.createNew(acl, groups[0].getId(), READ));
                    as.add(ACLAssignment.createNew(acl, groups[1].getId(), EDIT));
                    as.add(ACLAssignment.createNew(acl, groups[2].getId(), READ, EDIT));
                    as.add(ACLAssignment.createNew(acl, groups[3].getId(), CREATE, DELETE));
                    as.add(ACLAssignment.createNew(acl, groups[4].getId(), EDIT, CREATE, DELETE));
                    as.add(ACLAssignment.createNew(acl, groups[5].getId(), READ, EDIT, CREATE));
                    as.add(ACLAssignment.createNew(acl, groups[6].getId(), READ, EDIT, DELETE));
                    as.add(ACLAssignment.createNew(acl, groups[7].getId(), READ, EDIT, CREATE, DELETE));
                    as.add(ACLAssignment.createNew(acl, groups[8].getId(), READ, EDIT, RELATE, CREATE, DELETE));
                    as.add(ACLAssignment.createNew(acl, groups[9].getId(), READ, EDIT, RELATE, CREATE, DELETE, EXPORT));
                    break;
                case 2: //Property matrix
                    as.add(ACLAssignment.createNew(acl, groups[10].getId(), READ));
                    as.add(ACLAssignment.createNew(acl, groups[11].getId(), EDIT));
                    as.add(ACLAssignment.createNew(acl, groups[12].getId(), READ, EDIT));
                    as.add(ACLAssignment.createNew(acl, groups[13].getId(), READ, EDIT, CREATE));
                    as.add(ACLAssignment.createNew(acl, groups[14].getId(), READ, EDIT, DELETE));
                    as.add(ACLAssignment.createNew(acl, groups[15].getId(), CREATE, DELETE));
                    as.add(ACLAssignment.createNew(acl, groups[16].getId(), READ, EDIT, CREATE, DELETE));
                    break;
                case 3: //Workflow Step matrix
                    as.add(ACLAssignment.createNew(acl, groups[17].getId(), READ));
                    as.add(ACLAssignment.createNew(acl, groups[18].getId(), EDIT));
                    as.add(ACLAssignment.createNew(acl, groups[19].getId(), READ, EDIT));
                    as.add(ACLAssignment.createNew(acl, groups[20].getId(), READ, EDIT, CREATE));
                    as.add(ACLAssignment.createNew(acl, groups[21].getId(), READ, EDIT, DELETE));
                    as.add(ACLAssignment.createNew(acl, groups[22].getId(), CREATE, DELETE));
                    as.add(ACLAssignment.createNew(acl, groups[23].getId(), READ, EDIT, CREATE, DELETE));
                    break;
                case 4: //Workflow Route matrix - Edit
                    as.add(ACLAssignment.createNew(acl, groups[24].getId(), READ, EDIT, CREATE, DELETE));
                    break;
                case 5: //Workflow Route matrix - Live
                    as.add(ACLAssignment.createNew(acl, groups[25].getId(), READ, EDIT));
                    break;
                default:
                    Assert.assertFalse(true, "Invalid/unknown matrix: " + matrix);
            }
            EJBLookup.getAclEngine().update(acl.getId(), null, null, null, null, as);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
    }

    /**
     * Assign one of the predefined groups to the test user
     *
     * @param grp group to assign
     * @throws FxApplicationException on errors
     */
    private void assignGroup(long grp) throws FxApplicationException {
//        System.out.println("Before: "+ FxContext.get().getTicket());
        FxContext.get().runAsSystem();
        try {
            EJBLookup.getAccountEngine().setGroups(FxContext.get().getTicket().getUserId(), groups[((int) grp - 1)].getId());
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        FxContext.get()._reloadUserTicket();
//        System.out.println("After: "+ FxContext.get().getTicket());
//        for(long g: FxContext.get().getTicket().getGroups())
//            System.out.println("Group #"+g+": "+ EJBLookup.getUserGroupEngine().load(g).getName());
    }

    /**
     * Check if the given group is contained in the list
     *
     * @param grp    group to check for
     * @param groups list of groups
     * @return grp is contained in groups
     */
    private boolean containsGroup(int grp, int... groups) {
        for (int group : groups)
            if (group == grp)
                return true;
        return false;
    }

    /**
     * Create a reference content
     *
     * @return reference FxContent instance
     * @throws FxApplicationException on errors
     */
    private FxContent createReferenceContent() throws FxApplicationException {
        FxContent refContent;
        ContentEngine ce = EJBLookup.getContentEngine();
        try {
            FxContext.get().runAsSystem();
            refContent = ce.initialize(type.getId());
            refContent.setValue("/P1", PROP1_VALUE);
            refContent.setValue("/P2", PROP2_VALUE);
            refContent.setAclId(instanceACL.getId());
            return ce.load(ce.save(refContent));
        } finally {
            FxContext.get().stopRunAsSystem();
        }
    }

    /**
     * Run a series of instance and type related tests
     *
     * @throws FxApplicationException on errors
     */
    private void instanceTests() throws FxApplicationException {
        FxContent refContent = createReferenceContent();
        ContentEngine ce = EJBLookup.getContentEngine();
        FxContent compare;
        //1..10: Load ref, error expected for 2,4,5
        for (int grp = 1; grp <= 10; grp++) {
            assignGroup(grp);
            try {
                compare = ce.load(refContent.getPk());
                Assert.assertEquals(refContent, compare);
                Assert.assertFalse(containsGroup(grp, 2, 4, 5), "Test should have failed for group " + grp);
            } catch (FxApplicationException e) {
                switch (grp) {
                    case 2:
                    case 4:
                    case 5:
                        //expected
                        break;
                    default:
                        Assert.assertFalse(true, "Group " + grp + " should not have failed!");
                }
            }
        }
        //Save ref, error expected for 1
        for (int grp = 1; grp <= 10; grp++) {
            if (containsGroup(grp, 2, 4, 5))
                continue;
            assignGroup(grp);
            try {
                ce.save(refContent);
                Assert.assertFalse(containsGroup(grp, 1), "Test should have failed for group " + grp);
            } catch (FxApplicationException e) {
                switch (grp) {
                    case 1:
                        //expected
                        break;
                    default:
                        Assert.assertFalse(true, "Group " + grp + " should not have failed!");
                }
            }
        }
        //Create a clone of ref and remove it, error expected for 3, 6
        for (int grp = 1; grp <= 10; grp++) {
            if (containsGroup(grp, 1, 2, 4, 5))
                continue;
            FxContent clone;
            FxContext.get().runAsSystem();
            try {
                clone = ce.load(ce.save(refContent.copyAsNewInstance()));
            } finally {
                FxContext.get().stopRunAsSystem();
            }
            assignGroup(grp);
            try {
                ce.remove(clone.getPk());
                Assert.assertFalse(containsGroup(grp, 3, 6), "Test should have failed for group " + grp);
            } catch (FxApplicationException e) {
                switch (grp) {
                    case 3:
                    case 6:
                        //expected
                        FxContext.get().runAsSystem();
                        try {
                            ce.remove(clone.getPk());
                        } finally {
                            FxContext.get().stopRunAsSystem();
                        }
                        break;
                    default:
                        Assert.assertFalse(true, "Group " + grp + " should not have failed!");
                }
            }
        }
    }

    /**
     * Test 1 - Type permissions
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void test1_Type() throws FxApplicationException {
        //setup ACL test matrices
        assignMatrix(1, typeACL);
        assignMatrix(0, instanceACL);
        assignMatrix(0, property1ACL);
        assignMatrix(0, property2ACL);
        assignMatrix(0, editACL);
        assignMatrix(0, liveACL);

        FxTypeEdit te = type.asEditable();
        te.setPermissions(FxPermissionUtils.encodeTypePermissions(false, false, false, true));
        FxContext.get().runAsSystem();
        try {
            EJBLookup.getTypeEngine().save(te);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        type = CacheAdmin.getEnvironment().getType(type.getId());
        //run the test series
        instanceTests();
    }

    /**
     * Test 2 - Instance permissions
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void test2_Instance() throws FxApplicationException {
        //setup ACL test matrices
        assignMatrix(0, typeACL);
        assignMatrix(1, instanceACL);
        assignMatrix(0, property1ACL);
        assignMatrix(0, property2ACL);
        assignMatrix(0, editACL);
        assignMatrix(0, liveACL);

        FxTypeEdit te = type.asEditable();
        te.setPermissions(FxPermissionUtils.encodeTypePermissions(true, false, false, false));
        FxContext.get().runAsSystem();
        try {
            EJBLookup.getTypeEngine().save(te);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        type = CacheAdmin.getEnvironment().getType(type.getId());
        //run the test series
        instanceTests();
    }

    /**
     * Test 3 - Type and Instance permissions
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void test3_Type_Instance() throws FxApplicationException {
        //setup ACL test matrices
        assignMatrix(1, typeACL);
        assignMatrix(1, instanceACL);
        assignMatrix(0, property1ACL);
        assignMatrix(0, property2ACL);
        assignMatrix(0, editACL);
        assignMatrix(0, liveACL);

        FxTypeEdit te = type.asEditable();
        te.setPermissions(FxPermissionUtils.encodeTypePermissions(true, false, false, true));
        FxContext.get().runAsSystem();
        try {
            EJBLookup.getTypeEngine().save(te);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        type = CacheAdmin.getEnvironment().getType(type.getId());
        //run the test series
        instanceTests();
    }

    /**
     * Test 4 - Property permissions
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void test4_Properties() throws FxApplicationException {
        //setup ACL test matrices
        assignMatrix(0, typeACL);
        assignMatrix(0, instanceACL);
        assignMatrix(0, property1ACL);
        assignMatrix(2, property2ACL);
        assignMatrix(0, editACL);
        assignMatrix(0, liveACL);

        FxTypeEdit te = type.asEditable();
        te.setPermissions(FxPermissionUtils.encodeTypePermissions(false, true, false, false));
        FxContext.get().runAsSystem();
        try {
            EJBLookup.getTypeEngine().save(te);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        type = CacheAdmin.getEnvironment().getType(type.getId());
        //run the test series
        FxContent refContent = createReferenceContent();
        ContentEngine ce = EJBLookup.getContentEngine();
        FxContent compare;

        for (int grp = 11; grp <= 17; grp++) {
            assignGroup(grp);
            compare = ce.load(refContent.getPk());
            //11..17: Load ref, P2 should be FxNoAccess for 12,16
            if (containsGroup(grp, 12, 16)) {
                Assert.assertTrue(compare.getPropertyData("/P2").getValue() instanceof FxNoAccess, "/P2 expected to be FxNoAccess for group " + grp);
                Assert.assertNotSame(refContent, compare, "Group: " + grp);
            } else
                Assert.assertEquals(refContent, compare, "Group: " + grp);
            //11..17: change value, error expected for 11,16
            try {
                compare.setValue("/P2", PROP1_VALUE);
                Assert.assertFalse(containsGroup(grp, 11, 16), "Group " + grp + " should have thrown an exception trying to override a FxNoAccess value");
            } catch (FxApplicationException e) {
                if (!containsGroup(grp, 11, 16))
                    Assert.assertTrue(true, "Group " + grp + " should be allowed to modify /P2");
                else {
                    //force change for next test
                    FxContext.get().runAsSystem();
                    try {
                        compare.setValue("/P2", PROP1_VALUE);
                    } finally {
                        FxContext.get().stopRunAsSystem();
                    }
                }
            }
            //11,13-15,17: save changed value, error expected for 11
            if (containsGroup(grp, 11, 13, 14, 15, 17)) {
                try {
                    FxPK pk = null;
                    try {
                        pk = ce.save(compare.copyAsNewInstance());
                    } finally {
                        if (pk != null) {
                            FxContext.get().runAsSystem();
                            try {
                                ce.remove(pk);
                            } finally {
                                FxContext.get().stopRunAsSystem();
                            }
                        }
                    }
                    Assert.assertFalse(containsGroup(grp, 11), "Group " + grp + " should have thrown an exception trying to save an instance");
                } catch (FxApplicationException e) {
                    if (!containsGroup(grp, 11))
                        Assert.assertTrue(true, "Group " + grp + " should be allowed to save a modified /P2");
                }
            }
            //13-15,17: remove /P2 and save the instance, error expected for 13,14
            if (containsGroup(grp, 13, 14, 15, 17)) {
                try {
                    FxPK pk = null;
                    try {
                        FxContent co = compare.copyAsNewInstance();
                        FxContext.get().runAsSystem();
                        try {
                            pk = ce.save(co);
                            co = ce.load(pk);
                        } finally {
                            FxContext.get().stopRunAsSystem();
                        }
                        co.remove("/P2");
                        ce.save(co);
                        if (containsGroup(grp, 15, 17)) {
                            //try to re-add /P2, error expected for 15
                            try {
                                co.setValue("/P2", PROP2_VALUE);
                                ce.save(co);
                                if (grp == 15)
                                    Assert.assertFalse(true, "Group 15 should not be able to add /P2");
                            } catch (FxApplicationException e) {
                                if (grp != 15)
                                    Assert.assertFalse(true, "Only Group 15 and not " + grp + " should not be able to add /P2");
                            }
                        }
                    } finally {
                        if (pk != null) {
                            FxContext.get().runAsSystem();
                            try {
                                ce.remove(pk);
                            } finally {
                                FxContext.get().stopRunAsSystem();
                            }
                        }
                    }
                    Assert.assertFalse(containsGroup(grp, 13, 14), "Group " + grp + " should have thrown an exception trying to save an instance with a removed /P2");
                } catch (FxApplicationException e) {
                    if (!containsGroup(grp, 13, 14))
                        Assert.assertTrue(true, "Group " + grp + " should be allowed to save an instance with a removed /P2");
                }
            }
        }
    }

    /**
     * Test 5 - Workflow steps
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void test5_WFSteps() throws FxApplicationException {
        //setup ACL test matrices
        assignMatrix(0, typeACL);
        assignMatrix(0, instanceACL);
        assignMatrix(0, property1ACL);
        assignMatrix(0, property2ACL);
        assignMatrix(3, editACL);
        assignMatrix(0, liveACL);

        FxTypeEdit te = type.asEditable();
        te.setPermissions(FxPermissionUtils.encodeTypePermissions(false, false, true, false));
        FxContext.get().runAsSystem();
        try {
            EJBLookup.getTypeEngine().save(te);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        type = CacheAdmin.getEnvironment().getType(type.getId());
        //run the test series
        FxContent refContent = createReferenceContent();
        ContentEngine ce = EJBLookup.getContentEngine();
        FxContent compare = null;

        for (int grp = 18; grp <= 24; grp++) {
            assignGroup(grp);
            //18..24: Load ref, error expected for 19,23
            try {
                compare = ce.load(refContent.getPk());
                Assert.assertFalse(containsGroup(grp, 19, 23), "Group " + grp + " should have thrown an exception trying to load the reference instance");
            } catch (FxApplicationException e) {
                Assert.assertTrue(containsGroup(grp, 19, 23), "Group " + grp + " should not have thrown an exception trying to load the reference instance");
            }

            //18,20-22,24: save ref instance, error expected for 18
            if (!containsGroup(grp, 19, 23)) {
                try {
                    ce.save(compare);
                    Assert.assertFalse(containsGroup(grp, 18), "Group " + grp + " should have thrown an exception trying to save the reference instance");
                } catch (FxApplicationException e) {
                    Assert.assertTrue(containsGroup(grp, 18), "Group " + grp + " should not have thrown an exception trying to save the reference instance");
                }
            }

            //20-22,24: clone ref. instance and save as supervisor, remove instance with test user - error expected for 20,21
            if (!containsGroup(grp, 18, 19, 23)) {
                FxPK pk;
                try {
                    FxContext.get().runAsSystem();
                    compare = refContent.copyAsNewInstance();
                    pk = ce.save(compare);
                } finally {
                    FxContext.get().stopRunAsSystem();
                }
                boolean removed = false;
                try {
                    ce.remove(pk);
                    Assert.assertFalse(containsGroup(grp, 20, 21), "Group " + grp + " should have thrown an exception trying to remove the reference instance");
                    removed = true;
                } catch (FxApplicationException e) {
                    Assert.assertTrue(containsGroup(grp, 20, 21), "Group " + grp + " should not have thrown an exception trying to remove the reference instance");
                } finally {
                    if (!removed) {
                        try {
                            FxContext.get().runAsSystem();
                            ce.remove(pk);
                        } finally {
                            FxContext.get().stopRunAsSystem();
                        }
                    }
                }
            }

            //18-24: create a new instance in edit step, error expected for 18,19,20,22
            compare = ce.initialize(type.getId());
            compare.setValue("/P1", PROP1_VALUE);
            compare.setValue("/P2", PROP2_VALUE);
            compare.setAclId(instanceACL.getId());
            long stepId = -1;
            for (Step s : workflow.getSteps())
                if (s.isEditStep())
                    stepId = s.getId();
            Assert.assertFalse(stepId == -1, "Failed to find edit step!");
            compare.setStepId(stepId);
            boolean created = false;
            FxPK pk = null;
            try {
                pk = ce.save(compare);
                Assert.assertFalse(containsGroup(grp, 18, 19, 20, 22), "Group " + grp + " should have thrown an exception trying to create an edit instance");
            } catch (FxApplicationException e) {
                Assert.assertTrue(containsGroup(grp, 18, 19, 20, 22), "Group " + grp + " should not have thrown an exception trying to create an edit instance");
            } finally {
                if (!created && pk != null) {
                    try {
                        FxContext.get().runAsSystem();
                        ce.remove(pk);
                    } finally {
                        FxContext.get().stopRunAsSystem();
                    }
                }
            }
        }
    }

}
