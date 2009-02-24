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
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.ACLEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.value.FxString;
import org.testng.Assert;

/**
 * Helper base class to provide user login/logout and UserTicket information for tests
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxTestUtils {
    /**
     * Login the given user/password.
     *
     * @param username the user name
     * @param password the password
     * @throws FxLoginFailedException  if the login failed
     * @throws FxAccountInUseException if the user is already logged in (and not multilogin-capable)
     */
    public static void login(String username, String password) throws FxLoginFailedException, FxAccountInUseException {
        FxContext.get().login(username, password, true);
    }

    /**
     * Login a TestUser
     *
     * @param user TestUser
     * @throws FxLoginFailedException  if the login failed
     * @throws FxAccountInUseException if the user is already logged in (and not multilogin-capable)
     */
    public static void login(TestUser user) throws FxLoginFailedException, FxAccountInUseException {
        FxContext.get().login(user.getUserName(), user.getPassword(), true);
    }

    /**
     * The ticket of the user.
     * <p/>
     *
     * @return The ticket of the user
     */
    public static UserTicket getUserTicket() {
        return FxContext.getUserTicket();
    }

    /**
     * Change the current user for the test
     *
     * @param userName the test user name
     * @param password the test user password
     * @throws FxLoginFailedException
     * @throws FxAccountInUseException
     * @throws com.flexive.shared.exceptions.FxLogoutFailedException
     *
     */
    public static void changeUser(String userName, String password) throws FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        FxContext.get().logout();
        FxContext.get().login(userName, password, true);
    }

    /**
     * Perform logout of the current user.
     *
     * @throws FxLogoutFailedException if the logout failed
     */
    public static void logout() throws FxLogoutFailedException {
        FxContext.get().logout();
    }

    /**
     * Create a set of ACL's
     *
     * @param name     array with the names of the ACL's
     * @param category CATEGORY's
     * @param mandator the mandator
     * @return created ACL's
     * @throws FxApplicationException
     */
    public static ACL[] createACLs(String[] name, ACLCategory[] category, long mandator) throws FxApplicationException {
        Assert.assertTrue(name != null && category != null && name.length == category.length, "Invalid parameter(s) for createACL!");
        ACL[] acls = new ACL[name.length];
        ACLEngine acl = EJBLookup.getAclEngine();
        FxString label = new FxString("Unit Test ACL Label");
        for (int i = 0; i < name.length; i++) {
            long tmpId = acl.create(name[i], label, mandator, "#AABBCC", "UnitTest ACL", category[i]);
            acls[i] = CacheAdmin.getEnvironment().getACL(tmpId);
        }
        return acls;
    }

    /**
     * Create a new ACL.
     *
     * @param name
     * @param category
     * @param mandator
     * @return
     * @throws FxApplicationException
     */
    public static ACL createACL(String name, ACLCategory category, long mandator) throws FxApplicationException {
        return createACLs(new String[]{name}, new ACLCategory[]{category}, mandator)[0];
    }

    /**
     * Remove a set of ACL's
     *
     * @param acls the ACL's to remove
     * @throws FxApplicationException
     */
    public static void removeACL(ACL... acls) throws FxApplicationException {
        ACLEngine aclEngine = EJBLookup.getAclEngine();
        for (ACL acl : acls)
            aclEngine.remove(acl.getId());
    }
}
