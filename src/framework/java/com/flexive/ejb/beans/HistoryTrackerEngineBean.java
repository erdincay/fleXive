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
package com.flexive.ejb.beans;

import com.flexive.core.Database;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxHistory;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.interfaces.HistoryTrackerEngine;
import com.flexive.shared.interfaces.HistoryTrackerEngineLocal;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.flexive.core.DatabaseConst.TBL_HISTORY;

/**
 * History tracker service
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@SuppressWarnings("UnusedDeclaration")
@Stateless(name = "HistoryTrackerEngine", mappedName = "HistoryTrackerEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
public class HistoryTrackerEngineBean implements HistoryTrackerEngine, HistoryTrackerEngineLocal {

    private static final Log LOG = LogFactory.getLog(HistoryTrackerEngineBean.class);

    private static final String HISTORY_INSERT = "INSERT INTO " + TBL_HISTORY +
            //1       2         3       4          5           6          7       8           9
            "(ACCOUNT,LOGINNAME,TIMESTP,ACTION_KEY,ACTION_ARGS,EN_MESSAGE,SESSION,APPLICATION,REMOTEHOST," +
            //10    11       12   13    14
            "TYPEID,TYPENAME,PKID,PKVER,DATA)VALUES" +
            "(?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    private static final String HISTORY_INSERT_MANDATOR = "INSERT INTO " + TBL_HISTORY +
            //1       2         3       4          5           6          7       8           9
            "(ACCOUNT,LOGINNAME,TIMESTP,ACTION_KEY,ACTION_ARGS,EN_MESSAGE,SESSION,APPLICATION,REMOTEHOST," +
            //10    11       12   13    14   15
            "TYPEID,TYPENAME,PKID,PKVER,DATA,MANDATOR)VALUES" +
            "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    //                                                   1       2         3       4          5           6
    private static final String HISTORY_SELECT = "SELECT ACCOUNT,LOGINNAME,TIMESTP,ACTION_KEY,ACTION_ARGS,APPLICATION," +
            //7         8      9    10    11   12
            "REMOTEHOST,TYPEID,PKID,PKVER,DATA,SESSION FROM " + TBL_HISTORY;

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void track(String key, Object... args) {
        track(null, null, null, key, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void trackData(String data, String key, Object... args) {
        track(null, null, data, key, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void track(FxType type, String key, Object... args) {
        track(type, null, null, key, args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void track(Long mandator, String typeName, String loginname, String application, String session, String remoteHost,
                      String message, String data, String key, Object... args) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            final UserTicket ticket = FxContext.getUserTicket();
            con = Database.getDbConnection();
            boolean hasMandator = mandator != null;
            ps = con.prepareStatement(StorageManager.escapeReservedWords(hasMandator ? HISTORY_INSERT_MANDATOR : HISTORY_INSERT));
            ps.setLong(1, ticket.getUserId());
            ps.setString(2, StringUtils.isBlank(loginname) ? ticket.getLoginName() : loginname);

            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, key);
            StorageManager.setBigString(ps, 5, StringUtils.join(args, '|'));
            try {
                if (StringUtils.isNotBlank(message))
                    ps.setString(6, message);
                else
                    ps.setString(6, FxSharedUtils.getLocalizedMessage("History", FxLanguage.ENGLISH, "en", key, args));
            } catch (Exception e) {
                ps.setString(6, key);
            }
            FxContext si = FxContext.get();
            ps.setString(7, StringUtils.isNotBlank(session) ? session : (si.getSessionId() == null ? "<unknown>" : si.getSessionId()));
            ps.setString(8, StringUtils.isNotBlank(application) ? application : (si.getApplicationId() == null ? "<unknown>" : si.getApplicationId()));
            ps.setString(9, StringUtils.isNotBlank(remoteHost) ? remoteHost : (si.getRemoteHost() == null ? "<unknown>" : si.getRemoteHost()));
            if (StringUtils.isNotBlank(typeName)) {
                ps.setNull(10, java.sql.Types.NUMERIC);
                ps.setString(11, typeName);
            } else {
                ps.setNull(10, java.sql.Types.NUMERIC);
                ps.setNull(11, java.sql.Types.VARCHAR);
            }
            ps.setNull(12, java.sql.Types.NUMERIC);
            ps.setNull(13, java.sql.Types.NUMERIC);
            if (StringUtils.isNotBlank(data))
                StorageManager.setBigString(ps, 14, data);
            else
                ps.setNull(14, java.sql.Types.VARCHAR);
            if (hasMandator)
                ps.setLong(15, mandator);
            ps.executeUpdate();
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
        } finally {
            Database.closeObjects(HistoryTrackerEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public void track(FxType type, FxPK pk, String data, String key, Object... args) {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            final UserTicket ticket = FxContext.getUserTicket();
            con = Database.getDbConnection();
            ps = con.prepareStatement(StorageManager.escapeReservedWords(HISTORY_INSERT));
            ps.setLong(1, ticket.getUserId());
            ps.setString(2, ticket.getLoginName());

            ps.setLong(3, System.currentTimeMillis());
            ps.setString(4, key);
            StorageManager.setBigString(ps, 5, StringUtils.join(args, '|'));
            try {
                ps.setString(6, FxSharedUtils.getLocalizedMessage("History", FxLanguage.ENGLISH, "en", key, args));
            } catch (Exception e) {
                ps.setString(6, key);
            }
            FxContext si = FxContext.get();
            ps.setString(7, (si.getSessionId() == null ? "<unknown>" : si.getSessionId()));
            ps.setString(8, (si.getApplicationId() == null ? "<unknown>" : si.getApplicationId()));
            ps.setString(9, (si.getRemoteHost() == null ? "<unknown>" : si.getRemoteHost()));
            if (type != null) {
                ps.setLong(10, type.getId());
                ps.setString(11, type.getName());
            } else {
                ps.setNull(10, java.sql.Types.NUMERIC);
                ps.setNull(11, java.sql.Types.VARCHAR);
            }
            if (pk != null) {
                ps.setLong(12, pk.getId());
                ps.setInt(13, pk.getVersion());
            } else {
                ps.setNull(12, java.sql.Types.NUMERIC);
                ps.setNull(13, java.sql.Types.NUMERIC);
            }
            if (data != null)
                StorageManager.setBigString(ps, 14, data);
            else
                ps.setNull(14, java.sql.Types.VARCHAR);
            ps.executeUpdate();
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
        } finally {
            Database.closeObjects(HistoryTrackerEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxHistory> getContentEntries(long contentId) {
        List<FxHistory> ret = new ArrayList<FxHistory>(100);
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection();
            ps = con.prepareStatement(StorageManager.escapeReservedWords(HISTORY_SELECT) + " WHERE PKID=? ORDER BY TIMESTP");
            ps.setLong(1, contentId);
            ResultSet rs = ps.executeQuery();
            boolean loadData = FxContext.getUserTicket().isGlobalSupervisor();
            while (rs != null && rs.next())
                ret.add(new FxHistory(rs.getLong(3), rs.getLong(1), rs.getString(2), rs.getString(4), rs.getString(5).split("\\|"),
                        rs.getLong(8), rs.getLong(9), rs.getInt(10), rs.getString(6), rs.getString(7),
                        loadData ? rs.getString(11) : "No permission to load to data!", rs.getString(12)
                ));
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
        } finally {
            Database.closeObjects(HistoryTrackerEngineBean.class, con, ps);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxHistory> getEntries(String keyMatch, Long accountMatch, Long typeMatch, Long contentMatch, Date startDate, Date endDate, int maxEntries) {
        List<FxHistory> ret = new ArrayList<FxHistory>(100);
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection();
            String query = "";
            if (accountMatch != null) query += " AND ACCOUNT=" + accountMatch;
            if (typeMatch != null) query += " AND TYPEID=" + typeMatch;
            if (contentMatch != null) query += " AND PKID=" + contentMatch;
            ps = con.prepareStatement(StorageManager.escapeReservedWords(HISTORY_SELECT) + " WHERE TIMESTP>=? AND TIMESTP<=? AND ACTION_KEY LIKE ? " + query + " ORDER BY TIMESTP DESC");
            ps.setLong(1, startDate == null ? 0 : startDate.getTime());
            ps.setLong(2, endDate == null ? Long.MAX_VALUE - 1 : endDate.getTime());
            ps.setString(3, (StringUtils.isEmpty(keyMatch) ? "" : keyMatch) + "%");
            ResultSet rs = ps.executeQuery();
            boolean loadData = FxContext.getUserTicket().isGlobalSupervisor();
            int count = 0;
            while (rs != null && rs.next()) {
                if (count++ >= maxEntries)
                    break;
                long typeId = rs.getLong(8);
                if( rs.wasNull() )
                    typeId = -1;
                ret.add(new FxHistory(rs.getLong(3), rs.getLong(1), rs.getString(2), rs.getString(4), rs.getString(5).split("\\|"),
                        typeId, rs.getLong(9), rs.getInt(10), rs.getString(6), rs.getString(7),
                        loadData ? rs.getString(11) : "No permission to load to data!", rs.getString(12)
                ));
            }
        } catch (Exception ex) {
            LOG.error(ex.getMessage());
        } finally {
            Database.closeObjects(HistoryTrackerEngineBean.class, con, ps);
        }
        return ret;
    }
}
