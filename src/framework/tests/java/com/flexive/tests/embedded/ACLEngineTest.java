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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.structure.FxTypeEdit;
import static com.flexive.shared.EJBLookup.getAclEngine;
import static com.flexive.shared.EJBLookup.getContentEngine;
import static com.flexive.shared.EJBLookup.getTypeEngine;
import static com.flexive.shared.FxContext.getUserTicket;
import com.flexive.shared.interfaces.ACLEngine;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.security.*;
import com.flexive.shared.security.ACLPermission;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.List;
import java.util.Arrays;

/**
 * Basic ACL engine tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "security"})
public class ACLEngineTest {

    @Test(groups = {"ejb", "security"})
    public void createAclTest() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        login(TestUsers.SUPERVISOR);
        final long aclId = getAclEngine().create("create-acl-test", new FxString("first label"), TestUsers.getTestMandator(),
                "#000000", "", ACLCategory.INSTANCE);
        try {
                getAclEngine().create("create-acl-test", new FxString("first label"), TestUsers.getTestMandator(),
                "#000000", "", ACLCategory.INSTANCE);
                Assert.fail("ACL's must have unique names");
            }
            catch (Exception e) {
                //ok
            }

        try {
            final ACL acl = CacheAdmin.getFilteredEnvironment().getACL(aclId);
            assertEquals(acl.getName(), "create-acl-test");
            assertEquals(acl.getDescription(), "");
            assertEquals(acl.getColor(), "#000000");
            assertEquals(acl.getLabel(), new FxString("first label"));

            Assert.assertTrue(getUserTicket().getGroups().length > 0);
            final long groupId = getUserTicket().getGroups()[0];

            getAclEngine().update(aclId, "new-acl-test", new FxString("test"), null, "new description",
                    Arrays.asList(new ACLAssignment(aclId, groupId, true, true, true, false, false, false,
                            ACLCategory.INSTANCE, null)));
            final ACL updatedAcl = CacheAdmin.getFilteredEnvironment().getACL(aclId);
            assertEquals(updatedAcl.getName(), "new-acl-test");
            assertEquals(updatedAcl.getDescription(), "new description");
            assertEquals(updatedAcl.getColor(), "#000000");
            assertEquals(updatedAcl.getLabel(), new FxString("test"));
        } finally {
            getAclEngine().remove(aclId);
            logout();
        }
    }

    @Test(groups = {"ejb", "security"})
    public void aclAssignmentsTest() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        login(TestUsers.SUPERVISOR);
        final ACLEngine aclEngine = getAclEngine();
        final long aclId = aclEngine.create("create-acl-test", new FxString("first label"), TestUsers.getTestMandator(),
                "#000000", "", ACLCategory.INSTANCE);
        try {
            final UserTicket ticket = getUserTicket();
            Assert.assertTrue(ticket.getGroups().length > 0);
            for (long group: ticket.getGroups()) {
                aclEngine.assign(aclId, group, ACLPermission.EDIT, ACLPermission.CREATE);
            }
            final List<ACLAssignment> assignments = aclEngine.loadAssignments(aclId);
            for (long group: ticket.getGroups()) {
                boolean found = false;
                for (ACLAssignment assignment : assignments) {
                    if (assignment.getGroupId() == group) {
                        Assert.assertTrue(assignment.getMayEdit(), "Expected edit permissions");
                        Assert.assertTrue(assignment.getMayCreate(), "Expected create permissions");
                        Assert.assertTrue(!assignment.getMayDelete());
                        Assert.assertTrue(!assignment.getMayRead());
                        Assert.assertTrue(!assignment.getMayExport());
                        Assert.assertTrue(!assignment.getMayRelate());
                        found = true;
                    }
                }
                Assert.assertTrue(found, "Group " + group + " not found in assignments: " + assignments);

                final List<ACLAssignment> groupAssignments = aclEngine.loadGroupAssignments(group);
                boolean foundOurAcl = false;
                for (ACLAssignment groupAssignment : groupAssignments) {
                    if (groupAssignment.getAclId() == aclId) {
                        foundOurAcl = true;
                    }
                }
                Assert.assertTrue(foundOurAcl, "Didn't find ACL " + aclId + " in group assignments for group " + group);
            }
        } finally {
            aclEngine.remove(aclId);
            logout();
        }
    }

    /**
     * Checks if guest user tickets reflect ACL changes
     */
    @Test(groups = {"ejb", "security"})
    public void guestAssignmentUpdateTest_FX349() throws FxApplicationException {
        Assert.assertTrue(getUserTicket().isGuest(), "Not logged in, expected guest ticket");
        final long aclId;
        final long typeId;
        try {
            FxContext.get().runAsSystem();
            aclId = getAclEngine().create("acl-fx349", new FxString("label"), TestUsers.getTestMandator(),
                    "#000000", "", ACLCategory.STRUCTURE);
            getAclEngine().assign(aclId, UserGroup.GROUP_EVERYONE, ACLPermission.READ);
            typeId = getTypeEngine().save(FxTypeEdit.createNew("fx349")
                    .setUseTypePermissions(true)
                    .setUseInstancePermissions(false)
                    .setUsePropertyPermissions(false)
                    .setUseStepPermissions(false)
                    .setACL(getAclEngine().load(aclId))
            );
        } finally {
            FxContext.get().stopRunAsSystem();
        }

        try {
            Assert.assertTrue(getUserTicket().isAssignedToACL(aclId), "Guest user ticket should be assigned to " + aclId);
            Assert.assertTrue(!getUserTicket().mayCreateACL(aclId, -1), "Guest user should not have create permissions on acl " + aclId);
            tryCreateInstanceAsGuest(typeId, false);

            // remove create perm
            try {
                FxContext.get().runAsSystem();
                getAclEngine().assign(aclId, UserGroup.GROUP_EVERYONE, ACLPermission.READ, ACLPermission.CREATE);
            } finally {
                FxContext.get().stopRunAsSystem();
            }
            Assert.assertTrue(getUserTicket().isAssignedToACL(aclId), "Guest user ticket should be assigned to " + aclId);
            Assert.assertTrue(getUserTicket().mayCreateACL(aclId, -1), "Guest user should now have create permissions on acl " + aclId);
            tryCreateInstanceAsGuest(typeId, true);
        } finally {
            try {
                FxContext.get().runAsSystem();
                getTypeEngine().remove(typeId);
                getAclEngine().remove(aclId);
            } finally {
                FxContext.get().stopRunAsSystem();
            }
        }
    }

    private void tryCreateInstanceAsGuest(long typeId, boolean hasPerms) throws FxApplicationException {
        try {
            // force user ticket update (as in a new request)
            FxContext.get().overrideTicket(EJBLookup.getAccountEngine().getUserTicket());
            
            final FxPK pk = getContentEngine().save(getContentEngine().initialize(typeId));
            try {
                FxContext.get().runAsSystem();
                getContentEngine().remove(pk);
            } finally {
                FxContext.get().stopRunAsSystem();
            }
            Assert.assertTrue(hasPerms, "User should not be able to create folder instance");
        } catch (FxApplicationException e) {
            if (!hasPerms && (e instanceof FxNoAccessException)) {
                // pass
            } else {
                throw e;
            }
        }
    }
}
