package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.interfaces.ACLEngine;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.security.ACLAssignment;
import static com.flexive.shared.security.ACL.Permission;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Arrays;

/**
 * Basic ACL engine tests.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Test(groups = {"ejb", "security"})
public class ACLEngineTest {
    @BeforeClass
    public void beforeClass() throws Exception {
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        logout();
    }

    @Test(groups = {"ejb", "security"})
    public void createAclTest() throws FxApplicationException {
        final long aclId = EJBLookup.getACLEngine().create("create-acl-test", new FxString("first label"), TestUsers.getTestMandator(),
                "#000000", "", ACL.Category.INSTANCE);
        try {
            final ACL acl = CacheAdmin.getFilteredEnvironment().getACL(aclId);
            assertEquals(acl.getName(), "create-acl-test");
            assertEquals(acl.getDescription(), "");
            assertEquals(acl.getColor(), "#000000");
            assertEquals(acl.getLabel(), new FxString("first label"));

            assert FxContext.get().getTicket().getGroups().length > 0;
            final long groupId = FxContext.get().getTicket().getGroups()[0];

            EJBLookup.getACLEngine().update(aclId, "new-acl-test", new FxString("test"), null, "new description",
                    Arrays.asList(new ACLAssignment(aclId, groupId, true, true, true, false, false, false,
                            ACL.Category.INSTANCE, null)));
            final ACL updatedAcl = CacheAdmin.getFilteredEnvironment().getACL(aclId);
            assertEquals(updatedAcl.getName(), "new-acl-test");
            assertEquals(updatedAcl.getDescription(), "new description");
            assertEquals(updatedAcl.getColor(), "#000000");
            assertEquals(updatedAcl.getLabel(), new FxString("test"));
        } finally {
            EJBLookup.getACLEngine().remove(aclId);
        }
    }

    @Test(groups = {"ejb", "security"})
    public void aclAssignmentsTest() throws FxApplicationException {
        final ACLEngine aclEngine = EJBLookup.getACLEngine();
        final long aclId = aclEngine.create("create-acl-test", new FxString("first label"), TestUsers.getTestMandator(),
                "#000000", "", ACL.Category.INSTANCE);
        try {
            final UserTicket ticket = FxContext.get().getTicket();
            assert ticket.getGroups().length > 0;
            for (long group: ticket.getGroups()) {
                aclEngine.assign(aclId, group, Permission.EDIT, Permission.CREATE);
            }
            final List<ACLAssignment> assignments = aclEngine.loadAssignments(aclId);
            for (long group: ticket.getGroups()) {
                boolean found = false;
                for (ACLAssignment assignment : assignments) {
                    if (assignment.getGroupId() == group) {
                        assert assignment.getMayEdit() : "Expected edit permissions";
                        assert assignment.getMayCreate() : "Expected create permissions";
                        assert !assignment.getMayDelete();
                        assert !assignment.getMayRead();
                        assert !assignment.getMayExport();
                        assert !assignment.getMayRelate();
                        found = true;
                    }
                }
                assert found : "Group " + group + " not found in assignments: " + assignments;

                final List<ACLAssignment> groupAssignments = aclEngine.loadGroupAssignments(group);
                boolean foundOurAcl = false;
                for (ACLAssignment groupAssignment : groupAssignments) {
                    if (groupAssignment.getAclId() == aclId) {
                        foundOurAcl = true;
                    }
                }
                assert foundOurAcl : "Didn't find ACL " + aclId + " in group assignments for group " + group;
            }
        } finally {
            aclEngine.remove(aclId);
        }

    }
}
