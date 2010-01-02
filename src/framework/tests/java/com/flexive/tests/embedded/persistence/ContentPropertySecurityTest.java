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
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.interfaces.ACLEngine;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.ACLPermission;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxNoAccess;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUser;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Security test for the ContentEngine (for property permissions)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "content", "security"})
public class ContentPropertySecurityTest {

    private ContentEngine co;
    private ACLEngine acl;
    private TypeEngine type;
    private AssignmentEngine ass;

    private TestUser user;
    private long typeACL, prop1ACL, prop2ACL, contentACL, workflowACL;
    private long stepId;
    private final static String TYPE_NAME = "TESTSECURITY_" + RandomStringUtils.random(16, true, true);

    long typeId;
    private static final String PROP1_NAME = "PROP1_" + RandomStringUtils.random(16, true, true);
    private static final String PROP2_NAME = "PROP2_" + RandomStringUtils.random(16, true, true);

    private static final FxString PROP1_VALUE = new FxString(false, "PROP1_VALUE_" + RandomStringUtils.random(16, true, true));
    private static final FxString PROP1_NEW_VALUE = new FxString(false, "PROP1_NEW_VALUE_" + RandomStringUtils.random(16, true, true));
    private static final FxString PROP2_VALUE = new FxString(false, "PROP2_VALUE_" + RandomStringUtils.random(16, true, true));

    private FxPK refpk;

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void setup() throws Exception {
        co = EJBLookup.getContentEngine();
        acl = EJBLookup.getAclEngine();
        type = EJBLookup.getTypeEngine();
        ass = EJBLookup.getAssignmentEngine();
        login(TestUsers.SUPERVISOR);
        //acl for the 2 property assignments and the used type
        typeACL = TestUsers.newStructureACL("type");
        prop1ACL = TestUsers.newStructureACL("prop1");
        prop2ACL = TestUsers.newStructureACL("prop2");

        //acl for the content itself
        contentACL = TestUsers.newContentACL("content");
        //acl for the workflows of the type
        workflowACL = TestUsers.newWorkflowACL("workflow");

        //initialize the user with full access to all property assignments
        user = TestUsers.createUser("ContentPropertySecurity", Role.StructureManagement, Role.ACLManagement);
        user.createUser(TestUsers.getTestMandator(), TestUsers.getEnglishLanguageId());
//        TestUsers.assignACL(user, typeACL, ACL.PERM.CREATE);
        logout();
        login(user);
    }

    @BeforeClass(dependsOnMethods = {"setup"})
    public void setupStructures() throws Exception {
        typeId = type.save(FxTypeEdit.createNew(TYPE_NAME, new FxString("Test data"),
                CacheAdmin.getEnvironment().getACL(typeACL),
                null).setPermissions(FxPermissionUtils.encodeTypePermissions(true, true, false, false)));
//        typeId = type.create(typeACL, 1, TestUsers.getTestMandator(), new ArrayList<Mandator>(2), TYPE_NAME,
//                new FxString("Test data"), null, false,
//                TypeStorageMode.Hierarchical, TypeCategory.User, TypeMode.Content,
//                true, LanguageMode.Multiple, TypeState.Available,
//                FxPermissionUtils.encodeTypePermissions(true, true, false, false),
//                true, 0, 0, 0, 0);
        //create the 2 properties
        ass.createProperty(
                typeId,
                FxPropertyEdit.createNew(PROP1_NAME, new FxString("Security UnitTest"), new FxString("hint..."),
                        new FxMultiplicity(1, 2), CacheAdmin.getEnvironment().getACL(prop1ACL),
                        FxDataType.String1024), "/");
        ass.createProperty(
                typeId,
                FxPropertyEdit.createNew(PROP2_NAME, new FxString("Security UnitTest"), new FxString("hint..."),
                        new FxMultiplicity(1, 2), CacheAdmin.getEnvironment().getACL(prop2ACL),
                        FxDataType.String1024), "/");
        //get first possible step (steps aint subject of the tests here...)
        stepId = CacheAdmin.getEnvironment().getType(typeId).getWorkflow().getSteps().get(0).getId();
        FxContext.get().runAsSystem();
        FxContent test = co.initialize(typeId, TestUsers.getTestMandator(), contentACL, stepId, TestUsers.getEnglishLanguageId());
        test.setValue("/" + PROP1_NAME, PROP1_VALUE);
        test.setValue("/" + PROP2_NAME, new FxString(PROP2_VALUE));
        refpk = co.save(test);
        FxContext.get().stopRunAsSystem();
    }

    @Test
    public void checkTypePermissionSetup() throws FxApplicationException {
        FxType type = CacheAdmin.getEnvironment().getType(typeId);
        Assert.assertTrue(type.isUsePermissions());
        Assert.assertTrue(type.isUseInstancePermissions());
        Assert.assertTrue(type.isUsePropertyPermissions());
        Assert.assertTrue(!type.isUseStepPermissions());
        Assert.assertTrue(!type.isUseTypePermissions());
    }

    public void securityLoad() throws FxApplicationException {
    }

    @Test
    public void securityFullAccess() throws FxApplicationException {
        TestUsers.assignACL(user, contentACL, ACLPermission.CREATE, ACLPermission.DELETE, ACLPermission.EDIT, ACLPermission.READ);
        TestUsers.assignACL(user, prop1ACL, ACLPermission.CREATE, ACLPermission.DELETE, ACLPermission.EDIT, ACLPermission.READ);
        TestUsers.assignACL(user, prop2ACL, ACLPermission.CREATE, ACLPermission.DELETE, ACLPermission.EDIT, ACLPermission.READ);
        FxContent test = co.initialize(typeId, TestUsers.getTestMandator(), contentACL, stepId, TestUsers.getEnglishLanguageId());
        test.setValue("/" + PROP1_NAME, PROP1_VALUE);
        test.setValue("/" + PROP2_NAME, new FxString(PROP2_VALUE));
        FxPK pk = co.save(test);
        FxContent comp = co.load(pk);

        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).getValue().equals(PROP1_VALUE));
        Assert.assertTrue(!comp.getPropertyData("/" + PROP1_NAME).getValue().isReadOnly());
        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).mayCreateMore());

        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).getValue().equals(PROP2_VALUE));
        Assert.assertTrue(!comp.getPropertyData("/" + PROP2_NAME).getValue().isReadOnly());
        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).mayCreateMore());
        co.remove(pk);
    }

    @Test
    public void securityReadOnlyAccess() throws FxApplicationException {
        TestUsers.assignACL(user, contentACL, ACLPermission.READ);
        TestUsers.assignACL(user, prop1ACL, ACLPermission.READ);
        TestUsers.assignACL(user, prop2ACL, ACLPermission.READ);
        FxContent comp = co.load(refpk);

        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).getValue().equals(PROP1_VALUE));
        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).getValue().isReadOnly());
        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).mayCreateMore());

        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).getValue().equals(PROP2_VALUE));
        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).getValue().isReadOnly());
        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).mayCreateMore());
    }

    @Test
    public void securityNoAccess() throws FxApplicationException {
        TestUsers.assignACL(user, contentACL, ACLPermission.READ, ACLPermission.EDIT);
        TestUsers.assignACL(user, prop1ACL, ACLPermission.READ, ACLPermission.EDIT);
        TestUsers.assignACL(user, prop2ACL);
        FxContent comp = co.load(refpk);
        //check if prop1 is full editable
        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).getValue().equals(PROP1_VALUE));
        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).mayCreateMore());
        Assert.assertTrue(!comp.getPropertyData("/" + PROP1_NAME).getValue().isReadOnly());
        Assert.assertTrue(!(comp.getPropertyData("/" + PROP1_NAME).getValue() instanceof FxNoAccess));
        //check if prop2 is not accessible
        Assert.assertTrue(!comp.getPropertyData("/" + PROP2_NAME).getValue().equals(PROP2_VALUE));
        Assert.assertTrue(!comp.getPropertyData("/" + PROP2_NAME).mayCreateMore());
        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).getValue().isReadOnly());
        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).getValue() instanceof FxNoAccess);
        //set prop1 to a new value and check if its set
        comp.getPropertyData("/" + PROP1_NAME).setValue(PROP1_NEW_VALUE);
        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).getValue().equals(PROP1_NEW_VALUE));
        //try to set prop2 to a new value and check if it remains unchanged and still not accessible
        try {
            comp.getPropertyData("/" + PROP2_NAME).setValue(PROP1_NEW_VALUE);
            Assert.fail("FxNoAccessException expected!");
        } catch (FxRuntimeException e) {
            if (!(e.getConverted() instanceof FxNoAccessException)) {
                throw e;
            }
            //ok this was expected
        }
        Assert.assertTrue(!comp.getPropertyData("/" + PROP2_NAME).getValue().equals(PROP2_VALUE));
        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).getValue().isReadOnly());
        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).getValue() instanceof FxNoAccess);
        co.save(comp);
        FxContext.get().runAsSystem();
        comp = co.load(refpk);
        //check for correct changes
        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).getValue().equals(PROP1_NEW_VALUE));
        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).getValue().equals(PROP2_VALUE));
        comp.getPropertyData("/" + PROP1_NAME).setValue(PROP1_VALUE);
        Assert.assertTrue(comp.getPropertyData("/" + PROP1_NAME).mayCreateMore());
        Assert.assertTrue(comp.getPropertyData("/" + PROP2_NAME).mayCreateMore());
        co.save(comp); //re-save with original prop1 value
        FxContext.get().stopRunAsSystem();
    }

    @Test
    public void searchWithPropertyPermissions_FX479() throws FxApplicationException {
        TestUsers.assignACL(user, contentACL, ACLPermission.READ, ACLPermission.EDIT);
        co.load(refpk); // assert that we can load the content
        final FxResultSet result = EJBLookup.getSearchEngine().search(
                "SELECT caption, " + PROP1_NAME + " WHERE typedef=" + typeId
        );
        Assert.assertTrue(result.getRowCount() > 0, "Expected at least one row");
    }

    @AfterClass
    public void cleanup() throws Exception {
        logout();
        login(TestUsers.SUPERVISOR);
        co.removeForType(typeId);
        type.remove(typeId);
        acl.remove(contentACL);
        acl.remove(workflowACL);
        acl.remove(prop1ACL);
        acl.remove(prop2ACL);
        acl.remove(typeACL);
        logout();
    }

}
