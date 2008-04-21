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
package com.flexive.ejb.beans;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_SEQUENCE;
import com.flexive.shared.CustomSequencer;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxCreateException;
import com.flexive.shared.exceptions.FxDbException;
import com.flexive.shared.interfaces.SequencerEngine;
import com.flexive.shared.interfaces.SequencerEngineLocal;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Sequencer beans.
 * Generates Id's for data-tables or user defined sequencers.
 * This beans should be used instead of database specific autoincrements or sequences.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "SequencerEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
public class SequencerEngineBean implements SequencerEngine, SequencerEngineLocal {
    private static transient Log LOG = LogFactory.getLog(SequencerEngineBean.class);
    @Resource
    javax.ejb.SessionContext ctx;

    private final static String SQL_CREATE = "INSERT INTO " + TBL_SEQUENCE + "(ID,NAME,ROLLOVER)VALUES(?,?,?)";
    private final static String SQL_DELETE = "DELETE FROM " + TBL_SEQUENCE + " WHERE NAME=?";
    private final static String SQL_EXIST = "SELECT COUNT(*) FROM " + TBL_SEQUENCE + " WHERE NAME=?";
    private final static String SQL_GET_USER = "SELECT NAME,ROLLOVER,ID FROM " + TBL_SEQUENCE + " WHERE NAME NOT LIKE 'SYS_%' ORDER BY NAME ASC";

    private final static String SQL_NEXT = "UPDATE " + TBL_SEQUENCE + " SET ID=LAST_INSERT_ID(ID+1) WHERE NAME=?";
    private final static String SQL_RESET = "UPDATE " + TBL_SEQUENCE + " SET ID=0 WHERE NAME=?";
    private final static String SQL_GETID = "SELECT LAST_INSERT_ID()";
    private final static String SQL_GET_ROLLOVER = "SELECT ROLLOVER FROM " + TBL_SEQUENCE + " WHERE NAME=?";

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getId(final System type) throws FxApplicationException {
        return fetchId(type.getSequencerName(), type.isAllowRollover());
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void createSequencer(String name, boolean allowRollover, long startNumber) throws FxApplicationException {
        if (StringUtils.isEmpty(name) || name.toUpperCase().trim().startsWith("SYS_"))
            throw new FxCreateException(LOG, "ex.sequencer.create.invalid.name", name);
        name = name.toUpperCase().trim();
        if (sequencerExists(name))
            throw new FxCreateException(LOG, "ex.sequencer.create.invalid.name", name);

        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection();

            ps = con.prepareStatement(SQL_CREATE);
            ps.setLong(1, startNumber);
            ps.setString(2, name);
            ps.setBoolean(3, allowRollover);
            ps.executeUpdate();
            if (ps.getUpdateCount() == 0)
                throw new FxCreateException(LOG, "ex.sequencer.create.failed", name);
        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxCreateException(LOG, exc, "ex.sequencer.create.failed", name);
        } finally {
            Database.closeObjects(SequencerEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeSequencer(String name) throws FxApplicationException {
        if (!sequencerExists(name))
            throw new FxCreateException(LOG, "ex.sequencer.remove.notFound", name);
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection();
            ps = con.prepareStatement(SQL_DELETE);
            ps.setString(1, name);
            ps.executeUpdate();
            if (ps.getUpdateCount() == 0)
                throw new FxCreateException(LOG, "ex.sequencer.remove.failed", name);
        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxCreateException(LOG, exc, "ex.sequencer.remove.failed", name);
        } finally {
            Database.closeObjects(SequencerEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getId(String name) throws FxApplicationException {
        if (StringUtils.isEmpty(name) || name.toUpperCase().trim().startsWith("SYS_"))
            throw new FxCreateException(LOG, "ex.sequencer.fetch.invalid", name);
        return fetchId(name, false);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public boolean sequencerExists(String name) throws FxApplicationException {
        if (StringUtils.isEmpty(name) || name.toUpperCase().trim().startsWith("SYS_"))
            return false;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection();
            ps = con.prepareStatement(SQL_EXIST);
            ps.setString(1, name.trim().toUpperCase());
            ResultSet rs = ps.executeQuery();
            return !(rs == null || !rs.next()) && rs.getInt(1) != 0;
        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxDbException(LOG, exc, "ex.db.sqlError", exc.getMessage());
        } finally {
            Database.closeObjects(SequencerEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<CustomSequencer> getCustomSequencers() throws FxApplicationException {
        List<CustomSequencer> res = new ArrayList<CustomSequencer>(20);
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection();
            ps = con.prepareStatement(SQL_GET_USER);
            ResultSet rs = ps.executeQuery();
            while (rs != null && rs.next())
                res.add(new CustomSequencer(rs.getString(1), rs.getBoolean(2), rs.getLong(3)));
        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxDbException(LOG, exc, "ex.db.sqlError", exc.getMessage());
        } finally {
            Database.closeObjects(SequencerEngineBean.class, con, ps);
        }
        return res;
    }

    /**
     * Generates a new unique id for the given type.
     * Issues a ctx.setRollbackOnly if the DB failes to deliver a new ID
     *
     * @param name     the name
     * @param allowRollover if MAX_ID is reached throw exception or reset to zero?
     * @return the new id
     * @throws com.flexive.shared.exceptions.FxCreateException
     *          if the function fails
     */
    private long fetchId(final String name, boolean allowRollover) throws FxCreateException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();

            // Prepare the new id
            ps = con.prepareStatement(SQL_NEXT);
            ps.setString(1, name);
            ps.executeUpdate();
            if (ps.getUpdateCount() == 0)
                throw new FxCreateException(LOG, "ex.sequencer.typeUnknown", name);
            ps.close();

            // Get the new id
            ps = con.prepareStatement(SQL_GETID);
            ResultSet rs = ps.executeQuery();
            long newId;
            if (rs == null || !rs.next())
                throw new FxCreateException(LOG, "ex.sequencer.fetch.failed", name);
            newId = rs.getLong(1);
            if (rs.wasNull())
                throw new FxCreateException(LOG, "ex.sequencer.fetch.failed", name);
            if (newId >= MAX_ID) {
                if (!name.startsWith("SYS_")) {
                    //get allowRollover setting
                    ps.close();
                    ps = con.prepareStatement(SQL_GET_ROLLOVER);
                    ps.setString(1, name);
                    ResultSet rso = ps.executeQuery();
                    if (rso == null || !rso.next())
                        throw new FxCreateException(LOG, "ex.sequencer.fetch.failed", name);
                    allowRollover = rso.getBoolean(1);
                }
                if (!allowRollover)
                    throw new FxCreateException(LOG, "ex.sequencer.exhausted", name);
                ps.close();
                ps = con.prepareStatement(SQL_RESET);
                ps.setString(1, name);
                ps.executeUpdate();
                if (ps.getUpdateCount() == 0)
                    throw new FxCreateException(LOG, "ex.sequencer.typeUnknown", name);
                newId = 0;
            }
            // Return new id
            return newId;
        } catch (SQLException exc) {
            if (ctx != null) {
                ctx.setRollbackOnly();
            }
            throw new FxCreateException(LOG, exc, "ex.sequencer.fetch.failedMsg", name, exc.getMessage());
        } finally {
            Database.closeObjects(SequencerEngineBean.class, con, ps);
        }
    }

}
