package com.flexive.tests.embedded.roles;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.Role;
import com.flexive.tests.embedded.TestUser;
import com.flexive.tests.embedded.TestUsers;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.testng.annotations.Factory;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Creates users and other data needed by the role security tests.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Test(groups={"security", "roles"})
public class RoleTestUtils {
    private static final List<TestUser> USERS = new CopyOnWriteArrayList<TestUser>();

    @Factory
    public Object[] createTestInstances() {
        final List<Object> result = new ArrayList<Object>();
        // create test cases
        result.addAll(createTestCases(WorkflowManagementTest.class));

        return result.toArray(new Object[result.size()]);
    }

    private static List<Object> createTestCases(Class<? extends AbstractRoleTest> testClass) {
        final List<Object> result = new ArrayList<Object>();
        for (TestUser user: getTestUsers()) {
            final AbstractRoleTest test;
            try {
                test = testClass.newInstance();
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
            test.init(user);
            result.add(test);
        }
        return result;
    }

    private static synchronized void setupRoles() {
        if (USERS.isEmpty()) {
            // create one test user per role, and one without roles
            try {
                USERS.add(TestUsers.createUser("RoleTest - no role"));
                for (Role role: Role.values()) {
                    USERS.add(TestUsers.createUser("RoleTest " + role.name(), role));
                }
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
            // create test users for all role permutations - probably overkill
            /*
             final int numRoles = Role.values().length;
             for (int i = 0; i < (int) Math.pow(2, numRoles); i++) {
                final List<Role> roles = new ArrayList<Role>();
                for (int j = 0; j < numRoles; j++) {
                    if ((i & (int) Math.pow(2, j)) > 0) {
                        roles.add(Role.values()[j]);
                    }
                }
                USERS.add(TestUsers.createUser("RoleTest" + i, (Role[]) roles.toArray(new Role[roles.size()])));
                System.out.println("User roles: " + FxSharedUtils.getEnumNames(roles));
            }*/
        }
    }

    /**
     * Returns the test users as required by a TestNG dataprovider.
     *
     * @return  the test users as required by a TestNG dataprovider.
     */
    private static List<TestUser> getTestUsers() {
        setupRoles();
        return USERS;
    }
}
