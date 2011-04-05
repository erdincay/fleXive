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
package com.flexive.tests.embedded.roles;

import com.flexive.shared.EJBLookup;
import static com.flexive.shared.EJBLookup.getAccountEngine;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.security.AccountEdit;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.security.Role;
import static com.flexive.shared.security.Role.AccountManagement;
import com.flexive.tests.embedded.TestUser;
import com.flexive.tests.embedded.TestUsers;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;

/**
 * Account management role tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "security", "roles"})
public class AccountManagementTest extends AbstractRoleTest {
    long accountId = -1;
    boolean forced = false;

    @AfterMethod(groups = {"ejb", "security", "roles"})
    public void cleanup() throws FxApplicationException {
        if (accountId != -1) {
            if (forced) {
                FxContext.get().runAsSystem();
                getAccountEngine().remove(accountId);
                FxContext.get().stopRunAsSystem();
            } else {
                getAccountEngine().remove(accountId);
            }
            accountId = -1;
        }
    }

    @Test
    public void createAccountTest() throws FxApplicationException {
        try {
            createAccount(ticket.getMandatorId(), false);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, AccountManagement, ticket.getMandatorId());
        }
    }

    @Test
    public void createForeignAccountTest() throws FxApplicationException {
        try {
            createAccount(Mandator.MANDATOR_FLEXIVE, false);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, AccountManagement, Mandator.MANDATOR_FLEXIVE);
        }
    }

    @Test
    public void setRolesTest() throws FxApplicationException {
        setRoles(ticket.getMandatorId());
    }

    @Test
    public void setForeignRolesTest() throws FxApplicationException {
        setRoles(Mandator.MANDATOR_FLEXIVE);
    }

    private void setRoles(long mandatorId) throws FxApplicationException {
        createAccount(mandatorId, true);
        try {
            final List<Role> roles = new ArrayList<Role>();
            roles.add(Role.AccountManagement);
            if (ticket.isMandatorSupervisor()) {
                roles.add(Role.MandatorSupervisor);
            }
            if (ticket.isGlobalSupervisor()) {
                roles.add(Role.GlobalSupervisor);
            }
            getAccountEngine().setRoles(accountId, roles);
            assertSuccess(AccountManagement, mandatorId);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, AccountManagement, mandatorId);
        } 
    }

    @Test
    public void setGroupsTest() throws FxApplicationException {
        setGroups(ticket.getMandatorId());
    }

    @Test
    public void setForeignGroupsTest() throws FxApplicationException {
        setGroups(Mandator.MANDATOR_FLEXIVE);
    }

    private void setGroups(long mandatorId) throws FxApplicationException {
        createAccount(mandatorId, true);
        try {
            getAccountEngine().setGroups(accountId, user.getUserGroupId());
            assertSuccess(AccountManagement, mandatorId);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, AccountManagement, mandatorId);
        }
    }


    private void createAccount(long mandatorId, boolean force) throws FxApplicationException {
        try {
            if (force) {
                FxContext.get().runAsSystem();
            }
            accountId = getAccountEngine().create(
                    new AccountEdit().setLoginName("account_test").setMandatorId(mandatorId)
                    .setEmail("root@localhost"),
                    "abcdef");
            forced = force;
            if (!force) {
                assertSuccess(AccountManagement, mandatorId);
            }
        } catch (FxNoAccessException e) {
            assertNoAccess(e, AccountManagement, mandatorId);
            throw e;
        } finally {
            if (force) {
                FxContext.get().stopRunAsSystem();
            }
        }
    }
}
