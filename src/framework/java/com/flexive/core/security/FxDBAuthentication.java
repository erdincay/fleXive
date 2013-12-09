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
package com.flexive.core.security;

import com.flexive.core.Database;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.security.AuthenticationSource;
import com.flexive.shared.security.UserTicket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.security.auth.login.LoginException;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static com.flexive.core.DatabaseConst.TBL_ACCOUNTS;
import static com.flexive.core.DatabaseConst.TBL_ACCOUNT_DETAILS;

/**
 * Authentication against the divisions database
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public final class FxDBAuthentication {

    private static final Log LOG = LogFactory.getLog(FxDBAuthentication.class);

    /**
     * Login a user using flexive's database
     *
     * @param loginname name of the user
     * @param password plaintext password
     * @param callback callback providing datasource, ejb context and "take over"
     * @return Authenticated UserTicket
     * @throws FxAccountInUseException   on errors
     * @throws FxLoginFailedException    on errors
     * @throws FxAccountExpiredException on errors
     */
    public static UserTicket login(String loginname, String password, FxCallback callback) throws FxAccountInUseException, FxLoginFailedException, FxAccountExpiredException {
        final long SYS_UP = CacheAdmin.getInstance().getSystemStartTime();
        FxContext inf = FxContext.get();

        // Avoid null pointer exceptions
        if (password == null) password = "";
        if (loginname == null) loginname = "";

        String curSql;
        PreparedStatement ps = null;
        Connection con = null;
        try {
            // Obtain a database connection
            con = callback.getDataSource().getConnection();
            //               1-6 7      8           9              10                 11           12       13      14         15       16
            curSql = "SELECT d.*,a.ID,a.IS_ACTIVE,a.IS_VALIDATED,a.ALLOW_MULTILOGIN,a.VALID_FROM,a.VALID_TO,NOW(),a.PASSWORD,a.MANDATOR,a.LOGIN_NAME " +
                    "FROM " + TBL_ACCOUNTS + " a " +
                    "LEFT JOIN " +
                    " (SELECT ID,ISLOGGEDIN,LAST_LOGIN,LAST_LOGIN_FROM,FAILED_ATTEMPTS,AUTHSRC FROM " + TBL_ACCOUNT_DETAILS +
                    " WHERE APPLICATION=? ORDER BY LAST_LOGIN DESC) d ON a.ID=d.ID WHERE UPPER(a.LOGIN_NAME)=UPPER(?)";
            ps = con.prepareStatement(curSql);
            ps.setString(1, inf.getApplicationId());
            ps.setString(2, loginname);
            final ResultSet rs = ps.executeQuery();

            // Anything found?
            if (rs == null || !rs.next())
                throw new FxLoginFailedException("Login failed (invalid user or password)", FxLoginFailedException.TYPE_USER_OR_PASSWORD_NOT_DEFINED);

            // check if the hashed password matches the hash stored in the database
            final long id = rs.getLong(7);
            final String dbLoginName = rs.getString(16);    // use DB login name for non-lowercase login names
            final String dbPassword = rs.getString(14);
            boolean passwordMatches = FxSharedUtils.hashPassword(id, dbLoginName, password).equals(dbPassword);
            if (!passwordMatches && "supervisor".equalsIgnoreCase(loginname)) {
                // before 3.2.0 the default supervisor password was incorrectly hashed against the lower-cased login name
                passwordMatches = FxSharedUtils.hashPassword(id, "supervisor", password).equals(dbPassword);
            }
            if (!passwordMatches && !callback.isCalledAsGlobalSupervisor()) {
                increaseFailedLoginAttempts(con, id);
                throw new FxLoginFailedException("Login failed (invalid user or password)", FxLoginFailedException.TYPE_USER_OR_PASSWORD_NOT_DEFINED);
            }

            // Read data
            final boolean loggedIn = rs.getBoolean(2);
            final Date lastLogin = new Date(rs.getLong(3));
            final String lastLoginFrom = rs.getString(4);
            final long failedAttempts = rs.getLong(5);
            final boolean active = rs.getBoolean(8);
            final boolean validated = rs.getBoolean(9);
            final boolean allowMultiLogin = rs.getBoolean(10);
            final Date validFrom = new Date(rs.getLong(11));
            final Date validTo = new Date(rs.getLong(12));
            final Date dbNow = rs.getTimestamp(13);
            final long mandator = rs.getLong(15);

            // Account active?
            if (!active || !validated ||
                    (CacheAdmin.isEnvironmentLoaded() && !CacheAdmin.getEnvironment().getMandator(mandator).isActive())) {
                if (LOG.isDebugEnabled()) LOG.debug("Login for user [" + loginname +
                        "] failed, account is inactive. Active=" + active + ", Validated=" + validated +
                        ", Mandator active: " + CacheAdmin.getEnvironment().getMandator(mandator).isActive());
                increaseFailedLoginAttempts(con, id);
                throw new FxLoginFailedException("Login failed, account is inactive.", FxLoginFailedException.TYPE_INACTIVE_ACCOUNT);
            }

            // Account date from-to valid?
            //Compute the day AFTER the dValidTo
            Calendar endDate = Calendar.getInstance();
            endDate.setTime(validTo);
            endDate.add(Calendar.DAY_OF_MONTH, 1);
            if (validFrom.getTime() > dbNow.getTime() || endDate.getTimeInMillis() < dbNow.getTime()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                if (LOG.isDebugEnabled())
                    LOG.debug("Login for user [" + loginname +
                            "] failed, from/to date not valid. from='" + sdf.format(validFrom) + "' to='" + validTo + "'");
                increaseFailedLoginAttempts(con, id);
                throw new FxAccountExpiredException(loginname, dbNow);
            }

            // Check 'Account in use and takeOver false'
            if (!allowMultiLogin && !callback.getTakeOverSession() && loggedIn && lastLogin != null) {
                // Only if the last login time was AFTER the system started
                if (lastLogin.getTime() >= SYS_UP) {
                    FxAccountInUseException aiu = new FxAccountInUseException(loginname, lastLoginFrom, lastLogin);
                    if (LOG.isInfoEnabled()) LOG.info(aiu);
                    // don't log this as an invalid login attempt - this happens routinely when a session times
                    // out and the cached session data has not been evicted by the maintenance task yet

                    //increaseFailedLoginAttempts(con, id);
                    throw aiu;
                }
            }

            // Clear any old data
            curSql = "DELETE FROM " + TBL_ACCOUNT_DETAILS + " WHERE ID=? AND APPLICATION=?";
            ps.close();
            ps = con.prepareStatement(curSql);
            ps.setLong(1, id);
            ps.setString(2, inf.getApplicationId());
            ps.executeUpdate();

            // Mark user as active in the database
            // This can lead to duplicate rows for a user/application for concurrent logins (e.g. WebDAV clients),
            // but we prefer this to actually locking the complete table before updates. (FX-868)
            curSql = "INSERT INTO " + TBL_ACCOUNT_DETAILS + " (ID,APPLICATION,ISLOGGEDIN,LAST_LOGIN,LAST_LOGIN_FROM,FAILED_ATTEMPTS,AUTHSRC) " +
                    "VALUES (?,?,?,?,?,?,?)";
            ps.close();
            ps = con.prepareStatement(curSql);
            ps.setLong(1, id);
            ps.setString(2, inf.getApplicationId());
            ps.setBoolean(3, true);
            ps.setLong(4, System.currentTimeMillis());
            ps.setString(5, inf.getRemoteHost());
            ps.setLong(6, 0); //reset failed attempts
            ps.setString(7, AuthenticationSource.Database.name());
            ps.executeUpdate();

            // Load the user and construct a user ticket
            try {
                final UserTicketImpl ticket = (UserTicketImpl) UserTicketStore.getUserTicket(loginname);
                ticket.setFailedLoginAttempts(failedAttempts);
                ticket.setAuthenticationSource(AuthenticationSource.Database);
                return ticket;
            } catch (FxApplicationException e) {
                if (callback.getSessionContext() != null)
                    callback.getSessionContext().setRollbackOnly();
                throw new FxLoginFailedException(e.getExceptionMessage().getLocalizedMessage(FxLanguage.DEFAULT_ID),
                        FxLoginFailedException.TYPE_UNKNOWN_ERROR);
            }
        } catch (SQLException exc) {
            if (callback.getSessionContext() != null)
                callback.getSessionContext().setRollbackOnly();
            throw new FxLoginFailedException("Database error: " + exc.getMessage(), FxLoginFailedException.TYPE_SQL_ERROR);
        } finally {
            Database.closeObjects(FxDBAuthentication.class, con, ps);
        }
    }

    /**
     * Mark a user as no longer active in the database.
     *
     * @param ticket the ticket of the user
     * @throws javax.security.auth.login.LoginException
     *          if the function failed
     */
    public static void logout(UserTicket ticket) throws LoginException {
        PreparedStatement ps = null;
        String curSql;
        Connection con = null;
        FxContext inf = FxContext.get();
        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // EJBLookup user in the database, combined with a update statement to make sure
            // nothing changes between the lookup/set ISLOGGEDIN flag.
            curSql = "UPDATE " + TBL_ACCOUNT_DETAILS + " SET ISLOGGEDIN=? WHERE ID=? AND APPLICATION=?";
            ps = con.prepareStatement(curSql);
            ps.setBoolean(1, false);
            ps.setLong(2, ticket.getUserId());
            ps.setString(3, inf.getApplicationId());

            // Not more than one row should be affected, or the logout failed
            final int rowCount = ps.executeUpdate();
            if (rowCount > 1) {
                // Logout failed.
                LoginException le = new LoginException("Logout for user [" + ticket.getUserId() + "] failed");
                LOG.error(le);
                throw le;
            }

        } catch (SQLException exc) {
            LoginException le = new LoginException("Database error: " + exc.getMessage());
            LOG.error(le);
            throw le;
        } finally {
            Database.closeObjects(FxDBAuthentication.class, con, ps);
        }
    }

    /**
     * Increase the number of failed login attempts for the given user
     *
     * @param con    an open and valid connection
     * @param userId user id
     * @throws SQLException on errors
     */
    private static void increaseFailedLoginAttempts(Connection con, long userId) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE " + TBL_ACCOUNT_DETAILS + " SET FAILED_ATTEMPTS=FAILED_ATTEMPTS+1 WHERE ID=?");
            ps.setLong(1, userId);
            if (ps.executeUpdate() == 0) {
                ps.close();
                ps = con.prepareStatement("INSERT INTO " + TBL_ACCOUNT_DETAILS + " (ID,APPLICATION,ISLOGGEDIN,LAST_LOGIN,LAST_LOGIN_FROM,FAILED_ATTEMPTS,AUTHSRC) " +
                        "VALUES (?,?,?,?,?,?,?)");
                ps.setLong(1, userId);
                ps.setString(2, FxContext.get().getApplicationId());
                ps.setBoolean(3, false);
                ps.setLong(4, System.currentTimeMillis());
                ps.setString(5, FxContext.get().getRemoteHost());
                ps.setLong(6, 1); //one failed attempt
                ps.setString(7, AuthenticationSource.Database.name());
                ps.executeUpdate();
            }
        } finally {
            if (ps != null)
                ps.close();
        }
    }

    /**
     * @param username the username
     * @param password the password
     * @param currentTicket the UserTicket requesting the password match
     * @param ds thedatasource
     * @return returns true if the login and password match
     * @throws FxDbException on db errors
     * @throws FxLoginFailedException on authentication errors
     */
    public static boolean checkLogin(String username, String password, UserTicket currentTicket, DataSource ds) throws FxDbException, FxLoginFailedException {
        FxContext inf = FxContext.get();

        // Avoid null pointer exceptions
        if (password == null) password = "";
        if (username == null) username = "";

        String curSql;
        PreparedStatement ps = null;
        Connection con = null;
        try {
            // Obtain a database connection
            con = ds.getConnection();
            //               1      2           3
            curSql = "SELECT a.ID,a.USERNAME,a.PASSWORD " +
                    "FROM " + TBL_ACCOUNTS + " a " +
                    "LEFT JOIN " +
                    " (SELECT ID,ISLOGGEDIN,LAST_LOGIN,LAST_LOGIN_FROM,FAILED_ATTEMPTS,AUTHSRC FROM " + TBL_ACCOUNT_DETAILS +
                    " WHERE APPLICATION=?) d ON a.ID=d.ID WHERE UPPER(a.LOGIN_NAME)=UPPER(?)";
            ps = con.prepareStatement(curSql);
            ps.setString(1, inf.getApplicationId());
            ps.setString(2, username);
            final ResultSet rs = ps.executeQuery();

            // Anything found
            if (rs == null || !rs.next())
                throw new FxLoginFailedException("Invalid user or password", FxLoginFailedException.TYPE_USER_OR_PASSWORD_NOT_DEFINED);

            // check if the hashed password matches the hash stored in the database
            final long id = rs.getLong(1);
            final String dbUserName = rs.getString(2);
            final String hashedPass = rs.getString(3);

            // current user authorised to perform the check (ticket user id matches db user id?)
            if (id != currentTicket.getUserId() && !currentTicket.isGlobalSupervisor())
                throw new FxLoginFailedException("User not authorized to perform login check", FxLoginFailedException.TYPE_USER_OR_PASSWORD_NOT_DEFINED);

            return FxSharedUtils.hashPassword(id, dbUserName, password).equals(hashedPass)
                    // before 3.2.0 the default supervisor password was incorrectly hashed against the lower-cased login name
                    || ("SUPERVISOR".equals(username) && FxSharedUtils.hashPassword(id, "supervisor", password).equals(hashedPass));

        } catch (SQLException exc) {
            throw new FxDbException("Database error: " + exc.getMessage(), FxLoginFailedException.TYPE_SQL_ERROR);
        } finally {
            Database.closeObjects(FxDBAuthentication.class, con, ps);
        }
    }
}
