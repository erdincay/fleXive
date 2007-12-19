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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.ACLEngine;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.interfaces.LanguageEngine;
import com.flexive.shared.interfaces.MandatorEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Account;
import com.flexive.shared.security.Role;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Test user creation and management.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class TestUsers {
    /**
     * System property that contains the enabled test users (or "all" for all users)
     */
    public final static String ENABLE_USERS_PROPERTY = "tests.users";

    public final static TestUser SUPERVISOR = new TestUser("SUPERVISOR");
    public final static TestUser MANDATOR_SUPERVISOR = new TestUser("MANDATOR-SUPERVISOR");
    public final static TestUser GUEST = new TestUser("GUEST");

    public final static List<TestUser> ALL_USERS = Collections.unmodifiableList(Arrays.asList(
            new TestUser[]{SUPERVISOR, MANDATOR_SUPERVISOR, GUEST}));
    /**
     * Keeps track of user defined users
     */
    private static List<TestUser> USER_DEFINED_USERS = new ArrayList<TestUser>();


    private static final String MANDATOR_NAME = "UNIT_TEST_MANDATOR (automatically created)";

    private static boolean initialized = false;
    private static long mandatorId;
    private static long languageId;

    private static AccountEngine accounts;
    private static MandatorEngine mandators;
    private static LanguageEngine languages;
    private static ACLEngine acl;

    public static void initializeUsers() throws FxApplicationException {
        if (initialized) {
            return;
        }
        synchronized (TestUsers.class) {
            accounts = EJBLookup.getAccountEngine();
            mandators = EJBLookup.getMandatorEngine();
            languages = EJBLookup.getLanguageEngine();
            acl = EJBLookup.getACLEngine();

            initialized = true;

            try {
                FxContext.get().runAsSystem();
                languageId = languages.load("en").getId();
                mandatorId = getTestMandator();
                removeOrphanedAccounts();

                // setup supervisor
                SUPERVISOR.createUser(mandatorId, languageId);
                accounts.setRoleList(SUPERVISOR.getUserId(), Role.getList());

                // setup mandator supervisor
                MANDATOR_SUPERVISOR.createUser(mandatorId, languageId);
                accounts.setRoles(MANDATOR_SUPERVISOR.getUserId(), Role.MandatorSupervisor.getId());

                // setup guest user
                GUEST.createUser(mandatorId, languageId);

                // create user-defined users
                for (TestUser user : USER_DEFINED_USERS) {
                    user.createUser(mandatorId, languageId);
                }
                getTestMandator(); //create mandator
            } catch (Exception e) {
                deleteUsers();      // cleanup
                e.printStackTrace();
                throw new RuntimeException("Failed to initialize test users: " + e.getMessage());
            } finally {
                FxContext.get().stopRunAsSystem();
            }
        }
    }

    /**
     * Get id of english
     *
     * @return english id
     */
    public static long getEnglishLanguageId() {
        return languageId;
    }

    public static TestUser createUser(String informalName, Role... roles) throws FxApplicationException {
        TestUser user = new TestUser(informalName);
        if (roles != null && roles.length > 0) {
            user.setRoles(roles);
        }
        USER_DEFINED_USERS.add(user);
        return user;
    }

    /**
     * Assign the requested acl and permissions to the given user
     *
     * @param user        TestUser
     * @param aclId       ACL to assign
     * @param permissions permissions to assign for the acl
     * @throws FxApplicationException on errors
     */
    public static void assignACL(TestUser user, long aclId, ACL.Permission... permissions) throws FxApplicationException {
        acl.assign(aclId, user.getUserGroupId(), permissions);
    }

    public static long getTestMandator() throws FxApplicationException {
        try {
            return CacheAdmin.getEnvironment().getMandator(MANDATOR_NAME).getId();
        } catch (Exception e) {
            // ignore - try to create
            return mandators.create(MANDATOR_NAME, true);
        }
    }

    private static void removeOrphanedAccounts() throws FxApplicationException {
        // first remove all orphaned accounts created in previous runs
        for (Account account : accounts.loadAll(mandatorId)) {
            if (account.getEmail().startsWith("TESTEMAIL_")) {
                System.out.println("Removing orphaned test account: " + account.getLoginName());
                accounts.remove(account.getId());
            }
        }
    }

    public static synchronized void deleteUsers() throws FxApplicationException {
        try {
            FxContext.get().runAsSystem();
            List<TestUser> users = new ArrayList<TestUser>(ALL_USERS);
            users.addAll(USER_DEFINED_USERS);
            for (TestUser user : users) {
                try {
                    user.deleteUser();
                } catch (FxApplicationException e) {
                    System.out.println("Failed to remove test user from database: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            // remove test mandator
            mandators.remove(mandatorId);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
    }

    /**
     * Return the list of all test users as configured in the test environment.
     *
     * @return all test users as configured in the test environment.
     */
    public static List<TestUser> getConfiguredTestUsers() {
        String enableUsers = System.getProperty(ENABLE_USERS_PROPERTY);
        if (StringUtils.isBlank(enableUsers)) {
            return Collections.unmodifiableList(Arrays.asList(new TestUser[]{SUPERVISOR}));
        } else if ("all".equalsIgnoreCase(enableUsers)) {
            return ALL_USERS;
        } else {
            throw new RuntimeException("Invalid value for property " + ENABLE_USERS_PROPERTY + ": " + enableUsers);
        }
    }

    /**
     * Create a new ACL of category CONTENT
     *
     * @param name proposed name
     * @return id of the new ACL
     * @throws FxApplicationException on errors
     */
    public static long newContentACL(String name) throws FxApplicationException {
        return acl.create("TEST_" + name + RandomStringUtils.random(16, true, true), new FxString(name + " (content)"), TestUsers.getTestMandator(),
                "#112233", "", ACL.Category.INSTANCE);
    }

    /**
     * Create a new ACL of category STRUCTURE
     *
     * @param name proposed name
     * @return id of the new ACL
     * @throws FxApplicationException on errors
     */
    public static long newStructureACL(String name) throws FxApplicationException {
        return acl.create("TEST_" + name + RandomStringUtils.random(16, true, true), new FxString(name + " (structure)"), TestUsers.getTestMandator(),
                "#112233", "", ACL.Category.STRUCTURE);
    }

    /**
     * Create a new ACL of category WORKFLOW
     *
     * @param name proposed name
     * @return id of the new ACL
     * @throws FxApplicationException on errors
     */
    public static long newWorkflowACL(String name) throws FxApplicationException {
        return acl.create("TEST_" + name + RandomStringUtils.random(16, true, true), new FxString(name + " (workflow)"), TestUsers.getTestMandator(),
                "#112233", "", ACL.Category.WORKFLOW);
    }
}
