/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.tests.embedded.roles;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.Role;
import com.flexive.tests.embedded.TestUser;
import com.flexive.tests.embedded.TestUsers;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Creates users and other data needed by the role security tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"security", "roles"})
public class RoleTestUtils {
    private static List<TestUser> USERS = new CopyOnWriteArrayList<TestUser>();

    @Factory
    public Object[] createTestInstances() {
        final List<Object> result = new ArrayList<Object>();
        USERS = new CopyOnWriteArrayList<TestUser>(); // init before call f. jsf testrunner
        // create test cases
        result.addAll(createTestCases(WorkflowManagementTest.class));
        result.addAll(createTestCases(ACLManagementTest.class));
        result.addAll(createTestCases(AccountManagementTest.class));
        result.addAll(createTestCases(ScriptManagementTest.class));
        result.addAll(createTestCases(StructureManagementTest.class));

        return result.toArray(new Object[result.size()]);
    }

    private static List<Object> createTestCases(Class<? extends AbstractRoleTest> testClass) {
        final List<Object> result = new ArrayList<Object>();
        for (TestUser user : getTestUsers()) {
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
                for (Role role : Role.values()) {
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
     * @return the test users as required by a TestNG dataprovider.
     */
    private static List<TestUser> getTestUsers() {
        setupRoles();
        return USERS;
    }
}
