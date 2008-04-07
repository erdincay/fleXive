/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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

import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.interfaces.LanguageEngine;
import com.flexive.shared.security.Account;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import static com.flexive.tests.embedded.FxTestUtils.*;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Date;

/**
 * Account test cases.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb"})
public class AccountTest {

    private static final String USERNAME = "CACTUS_TEST_USER";
    private static final String LOGINNAME = StringUtils.reverse(USERNAME);
    private static final String PASSWORD = "123456";
    private static final String EMAIL = "test@cactus-user.com";

    private AccountEngine accountEngine = null;
    private LanguageEngine language = null;

    @BeforeClass
    public void beforeClass() throws Exception {
        accountEngine = EJBLookup.getAccountEngine();
        language = EJBLookup.getLanguageEngine();
        long userCount = accountEngine.getActiveUserTickets().size();
        if (userCount > 0) {
            for (UserTicket t : accountEngine.getActiveUserTickets())
                System.out.println("Logged in user: " + t);
        }
//        assert accountEngine.getActiveUserTickets().size() == 0 : "No user should be logged in!";
        login(TestUsers.SUPERVISOR);
//        assert accountEngine.getActiveUserTickets().size() == userCount+1 : "Test user should be logged in!";
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        logout();
    }

    /**
     * Test account creation.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void createAccount() throws Exception {
        long accountId = createAccount(0, 100, true, false);
        try {
            if (!getUserTicket().isInRole(Role.AccountManagement)) {
                assert false : "Test user is not in role user management.";
            }
            Account account = accountEngine.load(accountId);
            assert USERNAME.equals(account.getName().substring(1)) : "Username not stored correctly.";
            assert LOGINNAME.equals(account.getLoginName().substring(1)) : "Login not stored correctly.";
            assert account.isActive() : "Activation flag not set correctly.";
            assert !account.isValidated() : "Confirmation flag not set correctly.";
        } finally {
            // cleanup
            accountEngine.remove(accountId);
        }
    }

    /**
     * Try to create an account with an activation date set in the past.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void createPastAccount() throws Exception {
        long accountId = createAccount(-1, 2, true, true);
        try {
            Account account = accountEngine.load(accountId);
            assert new Date().after(account.getValidFrom()) : "Validation date not set correctly.";
            assert new Date().before(account.getValidTo()) : "Validation date not set correctly.";
        } finally {
            accountEngine.remove(accountId);
        }
    }

    /**
     * Try to create an account with an expiration date set in the past.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void createExpiredAccount() throws Exception {
        long accountId = -1;
        try {
            accountId = createAccount(-10, 2, true, true);
            Account account = accountEngine.load(accountId);
            assert false : "Able to create expired account (from = " + account.getValidFromString()
                    + ", to = " + account.getValidToString() + ")";
        } catch (Exception e) {
            // passed test
        } finally {
            if (accountId != -1) {
                accountEngine.remove(accountId);
            }
        }
    }

    /**
     * Try to create an account with an invalid date.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void createInvalidDateAccount() throws Exception {
        long accountId = -1;
        try {
            accountId = createAccount(1, -2, true, true);
            Account account = accountEngine.load(accountId);
            assert false : "Able to create account with invalid dates (from = "
                    + account.getValidFromString() + ", to = " + account.getValidToString() + ")";
        } catch (Exception e) {
            // passed test
        } finally {
            if (accountId != -1) {
                accountEngine.remove(accountId);
            }
        }
    }

    private static int COUNT = 0;

    /**
     * Create a test account
     *
     * @param deltaDays day difference from the current date
     * @param validDays validity for days
     * @param active    if the account should be active
     * @param confirmed if the account should be confirmed
     * @return a test account
     * @throws FxApplicationException on errors
     */
    private long createAccount(int deltaDays, int validDays, boolean active, boolean confirmed)
            throws FxApplicationException {
        Date begin = new Date(System.currentTimeMillis() + (long) deltaDays * 24 * 3600 * 1000);
        Date end = new Date(System.currentTimeMillis() + ((long) deltaDays + validDays) * 24 * 3600 * 1000);
        return accountEngine.create(COUNT + USERNAME, COUNT + LOGINNAME, COUNT + PASSWORD, (COUNT++) + EMAIL, language.load("en").getId(),
                getUserTicket().getMandatorId(), active, confirmed, begin, end, -1, "", false, true);
    }

}
