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
package com.flexive.core;

import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxUpdateException;
import com.flexive.shared.security.LifeCycleInfo;
import com.flexive.shared.security.UserTicket;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * LifeCycleInfo implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class LifeCycleInfoImpl implements LifeCycleInfo, Serializable {
    private static final long serialVersionUID = -7886706421888644107L;

    /**
     * our logger
     */
    private static final transient Log LOG = LogFactory.getLog(LifeCycleInfoImpl.class);

    /**
     * Value to set if a column is not set (=null)
     */
    private static final int NOT_DEFINED = -1;

    private long iCreatorId;
    private long lCreationTime;
    private long iModifcatorId;
    private long lModificationTime;

    /**
     * Empty LifeCycleInfo for tables where undefined
     */
    public static final LifeCycleInfo EMPTY = new LifeCycleInfoImpl(0, 0, 0, 0);

    /**
     * Get the Id of the User that created this entry
     *
     * @return Id of the User that created this entry
     */
    public long getCreatorId() {
        return iCreatorId;
    }

    /**
     * Get the timestamp when this object was created
     *
     * @return the timestamp when this object was created
     */
    public long getCreationTime() {
        return lCreationTime;
    }

    /**
     * Get the Id of the most recent User that modified this entry
     *
     * @return Id of the most recent User that modified this entry
     */
    public long getModificatorId() {
        return iModifcatorId;
    }

    /**
     * Get the timestamp of the most recent modification
     *
     * @return timestamp of the most recent modification
     */
    public long getModificationTime() {
        return lModificationTime;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        return "LCI:{C:" + this.getCreatorId() + "/" + sdf.format(new Date(this.getCreationTime())) + "|"
                + "M:" + this.getModificatorId() + "/" + sdf.format(new Date(this.getModificationTime()));
    }

    /**
     * Ctor
     *
     * @param creatorId        user id of the creator
     * @param creationTime     timestamp of creation
     * @param modificator      user id of the user that made the last modification
     * @param modificationTime modification timestamp
     */
    public LifeCycleInfoImpl(long creatorId, long creationTime, long modificator, long modificationTime) {
        this.iCreatorId = creatorId;
        this.lCreationTime = creationTime;
        this.iModifcatorId = modificator;
        this.lModificationTime = modificationTime;
    }

    /**
     * Helper function for a less error prone and faster loading from the database
     *
     * @param rs                     ResultSet containing all the required info
     * @param creatorColumn          column index of the create user reference
     * @param creationTimeColumn     column index of the create timestamp
     * @param modificatorColumns     column index of the modified by user reference
     * @param modificationTimeColumn column index of the modified by timestamp
     * @return LifeCycleInfo with the relevant data gathered from the ResultSet
     * @throws java.sql.SQLException if a column could not be read
     */
    public static LifeCycleInfo load(ResultSet rs, int creatorColumn, int creationTimeColumn, int modificatorColumns,
                                     int modificationTimeColumn) throws SQLException {
        if (rs == null) {
            throw new IllegalArgumentException("Can not read from a null ResultSet!");
        }
        int cid;
        long ct;
        int mid;
        long mt;
        java.sql.Timestamp dTmp;
        cid = rs.getInt(creatorColumn);
        if (rs.wasNull()) {
            cid = NOT_DEFINED;
        }
        dTmp = rs.getTimestamp(creationTimeColumn);
        ct = (rs.wasNull() ? NOT_DEFINED : dTmp.getTime());
        if (modificatorColumns < 0) {
            mid = NOT_DEFINED;
        } else {
            mid = rs.getInt(modificatorColumns);
            if (rs.wasNull())
                mid = NOT_DEFINED;
        }
        if (modificationTimeColumn < 0) {
            mt = NOT_DEFINED;
        } else {
            dTmp = rs.getTimestamp(modificationTimeColumn);
            mt = (rs.wasNull() ? NOT_DEFINED : dTmp.getTime());
        }
        return new LifeCycleInfoImpl(cid, ct, mid, mt);
    }

    /**
     * Store a complete lifecycle info
     *
     * @param ps                     prepared statement
     * @param creatorColumn          column index of the create user reference
     * @param creationTimeColumn     column index of the create timestamp
     * @param modificatorColumn      column index of the modified by user reference
     * @param modificationTimeColumn column index of the modified by timestamp
     * @throws java.sql.SQLException if a column could not be set
     */
    public static void store(PreparedStatement ps, int creatorColumn, int creationTimeColumn, int modificatorColumn,
                             int modificationTimeColumn) throws SQLException {
        final UserTicket ticket = FxContext.get().getTicket();
        final java.sql.Timestamp ts = new Timestamp(System.currentTimeMillis());
        ps.setLong(creatorColumn, ticket.getUserId());
        ps.setTimestamp(creationTimeColumn, ts);
        ps.setLong(modificatorColumn, ticket.getUserId());
        ps.setTimestamp(modificationTimeColumn, ts);
    }

    /**
     * Update a lifyecycle info
     *
     * @param ps                     prepared statement
     * @param modificatorColumn      column index of the modified by user reference
     * @param modificationTimeColumn column index of the modified by timestamp
     * @throws java.sql.SQLException if a column could not be set
     */
    public static void updateLifeCycleInfo(PreparedStatement ps, int modificatorColumn, int modificationTimeColumn)
            throws SQLException {
        final UserTicket ticket = FxContext.get().getTicket();
        final java.sql.Timestamp ts = new Timestamp(System.currentTimeMillis());
        ps.setLong(modificatorColumn, ticket.getUserId());
        ps.setTimestamp(modificationTimeColumn, ts);
    }


    /**
     * Update a tables LifeCycleInfo
     *
     * @param table   table that contains the lifecycle
     * @param idField field containing the id
     * @param id      the id to update
     * @throws FxUpdateException if the lifecycle info could not be updated
     */
    public static void updateLifeCycleInfo(String table, String idField, long id)
            throws FxUpdateException {
        updateLifeCycleInfo(table, idField, null, id, -1, false, true);
    }

    /**
     * Update a tables LifeCycleInfo
     *
     * @param table         table that contains the lifecycle
     * @param idField       field containing the id
     * @param id            the id to update
     * @param verField      field containing the id (optional)
     * @param ver           the version to update (optional)
     * @param updateCreated update created by/at as well?
     * @param throwOnNone   throw an exception if no rows were updated?
     * @throws FxUpdateException if a database field could not be updated
     */
    public static void updateLifeCycleInfo(String table, String idField, String verField,
                                           long id, int ver, boolean updateCreated, boolean throwOnNone) throws FxUpdateException {
        final UserTicket ticket = FxContext.get().getTicket();

        Connection con = null;
        PreparedStatement stmt = null;

        try {
            con = Database.getDbConnection();
            stmt = con.prepareStatement("UPDATE " + table + " SET MODIFIED_BY=?, MODIFIED_AT=?"
                    + (updateCreated ? ", CREATED_BY=?, CREATED_AT=?" : "") + " WHERE " + idField + "=?"
                    + (verField != null && ver > 0 ? " AND " + verField + "=?" : ""));
            Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            stmt.setInt(1, (int) ticket.getUserId());
            stmt.setTimestamp(2, now);
            if (updateCreated) {
                stmt.setInt(3, (int) ticket.getUserId());
                stmt.setTimestamp(4, now);
                stmt.setLong(5, id);
            } else
                stmt.setLong(3, id);
            if (verField != null && ver > 0)
                stmt.setInt((updateCreated ? 6 : 4), ver);
            int iCnt = stmt.executeUpdate();
            if (iCnt != 1 && throwOnNone)
                throw new FxUpdateException("Updating LifeCycleInfo failed. " + iCnt + " rows were updated!");
        } catch (SQLException se) {
            throw new FxUpdateException(LOG, se.getMessage(), se);
        } finally {
            Database.closeObjects(LifeCycleInfoImpl.class, con, stmt);
        }
    }

    /**
     * Initialize a new empty lci
     *
     * @param ticket the calling user's ticket
     * @return a new lifecycle info for the calling user
     */
    public static LifeCycleInfo createNew(UserTicket ticket) {
        return new LifeCycleInfoImpl(ticket.getUserId(), System.currentTimeMillis(),
                ticket.getUserId(), System.currentTimeMillis());
    }
}
