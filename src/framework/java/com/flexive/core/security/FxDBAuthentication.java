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
package com.flexive.core.security;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_ACCOUNTS;
import static com.flexive.core.DatabaseConst.TBL_ACCOUNT_DETAILS;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxAccountExpiredException;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.security.AuthenticationSource;
import com.flexive.shared.security.UserTicket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.security.auth.login.LoginException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Authentication against the divisions database
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
class FxDBAuthentication {

    private static transient Log LOG = LogFactory.getLog(FxDBAuthentication.class);

    public static UserTicket login(String username, String password, FxCallback callback) throws FxAccountInUseException, FxLoginFailedException, FxAccountExpiredException {
        final long SYS_UP = CacheAdmin.getInstance().getSystemStartTime();
        FxContext inf = FxContext.get();

        // Avoid null pointer exceptions
        if (password == null) password = "";
        if (username == null) username = "";

        String curSql;
        PreparedStatement ps = null;
        Connection con = null;
        try {
            // Obtain a database connection
            con = callback.getDataSource().getConnection();
            //               1-6 7      8           9              10                 11           12       13      14         15
            curSql = "SELECT d.*,a.ID,a.IS_ACTIVE,a.IS_VALIDATED,a.ALLOW_MULTILOGIN,a.VALID_FROM,a.VALID_TO,NOW(),a.PASSWORD,a.MANDATOR " +
                    "FROM " + TBL_ACCOUNTS + " a " +
                    "LEFT JOIN " +
                    " (SELECT ID,ISLOGGEDIN,LAST_LOGIN,LAST_LOGIN_FROM,FAILED_ATTEMPTS,AUTHSRC FROM " + TBL_ACCOUNT_DETAILS +
                    " WHERE APPLICATION=?) d ON a.ID=d.ID WHERE a.LOGIN_NAME=?";
            ps = con.prepareStatement(curSql);
            ps.setString(1, inf.getApplicationId());
            ps.setString(2, username);
            final ResultSet rs = ps.executeQuery();

            // Anything found?
            if (rs == null || !rs.next())
                throw new FxLoginFailedException("Login failed (invalid user or password)", FxLoginFailedException.TYPE_USER_OR_PASSWORD_NOT_DEFINED);

            // check if the hashed password matches the hash stored in the database
            final long id = rs.getLong(7);
            final boolean passwordMatches = FxSharedUtils.hashPassword(id, password).equals(rs.getString(14));
            if (!passwordMatches) {
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
                    (CacheAdmin.isEnvironmentLoaded() && !CacheAdmin.getEnvironment().getMandator(mandator).isActive()) ) {
                if (LOG.isDebugEnabled()) LOG.debug("Login for user [" + username +
                        "] failed, account is inactive. Active=" + active + ", Validated=" + validated +
                        ", Mandator active: " + CacheAdmin.getEnvironment().getMandator(mandator).isActive());
                increaseFailedLoginAttempts(con, id);
                throw new FxLoginFailedException("Login failed", FxLoginFailedException.TYPE_INACTIVE_ACCOUNT);
            }

            // Account date from-to valid?
            //Compute the day AFTER the dValidTo
            Calendar endDate = Calendar.getInstance();
            endDate.setTime(validTo);
            endDate.add(Calendar.DAY_OF_MONTH, 1);
            if (validFrom.getTime() > dbNow.getTime() || endDate.getTimeInMillis() < dbNow.getTime()) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
                if (LOG.isDebugEnabled())
                    LOG.debug("Login for user [" + username +
                            "] failed, from/to date not valid. from='" + sdf.format(validFrom) + "' to='" + validTo + "'");
                increaseFailedLoginAttempts(con, id);
                throw new FxAccountExpiredException(username, dbNow);
            }

            // Check 'Account in use and takeOver false'
            if (!allowMultiLogin && !callback.getTakeOverSession() && loggedIn && lastLogin != null) {
                // Only if the last login time was AFTER the system started
                if (lastLogin.getTime() >= SYS_UP) {
                    FxAccountInUseException aiu = new FxAccountInUseException(username, lastLoginFrom, lastLogin);
                    if (LOG.isInfoEnabled()) LOG.info(aiu);
                    increaseFailedLoginAttempts(con, id);
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
                final UserTicketImpl ticket = (UserTicketImpl) UserTicketStore.getUserTicket(username);
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
     * @param ticket the ticke of the user
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
            curSql = "UPDATE " + TBL_ACCOUNT_DETAILS + " SET ISLOGGEDIN=FALSE WHERE ID=? AND APPLICATION=?";
            ps = con.prepareStatement(curSql);
            ps.setLong(1, ticket.getUserId());
            ps.setString(2, inf.getApplicationId());

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
}
