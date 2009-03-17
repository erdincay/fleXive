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

import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.AccountEdit;
import org.apache.commons.lang.RandomStringUtils;

import java.util.Date;

/**
 * Interface for test user management.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class TestUser {

    private String userName;
    private String password;
    private String email;
    /**
     * Every user gets its own group to simplify security testing
     */
    private String userGroup;
    private Role[] roles = null;
    private long userId = -1;
    private long userGroupId = -1;

    public TestUser(String informalName) {
        this.userName = informalName + "_" + RandomStringUtils.random(16, true, true);
        this.userGroup = informalName + "_GROUP_" + RandomStringUtils.random(16, true, true);
        this.password = RandomStringUtils.random(32, true, true);
        this.email = "TESTEMAIL_" + RandomStringUtils.random(5, true, true) + "@" + RandomStringUtils.random(5, true, true) + ".com";
    }

    /**
     * Create the configured test user.
     *
     * @param mandatorId the test user mandator ID
     * @param languageId the test user language
     * @throws FxApplicationException if the account could not be created
     */
    public void createUser(long mandatorId, long languageId) throws FxApplicationException {
        AccountEngine accounts = EJBLookup.getAccountEngine();
        if (userId != -1) {
            throw new RuntimeException("Account already exists");
        }
        final AccountEdit account = new AccountEdit()
                .setName(userName)
                .setLoginName(userName)
                .setEmail(email)
                .setLanguage(EJBLookup.getLanguageEngine().load(languageId))
                .setMandatorId(mandatorId)
                .setValidTo(AccountEdit.VALID_FOREVER);

        userId = accounts.create(account, password);
        if (roles != null) {
            // set user-defined roles
            accounts.setRoles(userId, Role.toIdArray(roles));
        }
        this.userGroupId = EJBLookup.getUserGroupEngine().create(this.userGroup, "#112233", mandatorId);
        accounts.setGroups(this.userId, this.userGroupId);
    }

    /**
     * Remove the test user from the database.
     *
     * @throws FxApplicationException if the user could not be deleted
     */
    public void deleteUser() throws FxApplicationException {
        if (userId == -1) {
            return;
        }
        EJBLookup.getUserGroupEngine().remove(userGroupId);
        EJBLookup.getAccountEngine().remove(userId);
    }

    public String getUserName() {
        return this.userName;
    }

    public String getPassword() {
        return this.password;
    }

    public long getUserId() {
        return userId;
    }

    public long getUserGroupId() {
        return userGroupId;
    }

    public void setRoles(Role[] roles) {
        this.roles = roles;
    }


}