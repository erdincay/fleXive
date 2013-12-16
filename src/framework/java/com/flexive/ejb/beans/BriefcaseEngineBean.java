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
import com.flexive.core.DatabaseConst;
import com.flexive.core.LifeCycleInfoImpl;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxReferenceMetaData;
import com.flexive.shared.FxSystemSequencer;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ACLEngineLocal;
import com.flexive.shared.interfaces.BriefcaseEngine;
import com.flexive.shared.interfaces.BriefcaseEngineLocal;
import com.flexive.shared.interfaces.SequencerEngineLocal;
import com.flexive.shared.search.Briefcase;
import com.flexive.shared.search.BriefcaseItemData;
import com.flexive.shared.security.*;
import com.google.common.collect.Lists;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.*;
import java.util.*;

import static com.flexive.core.Database.closeObjects;
import static com.flexive.core.DatabaseConst.TBL_BRIEFCASE_DATA;
import static com.flexive.core.DatabaseConst.TBL_BRIEFCASE_DATA_ITEM;

/**
 * Bean handling Briefcases.
 * <p/>
 * A briefcase is a object store which may be accessed with flexive SQL
 * or the API provided by this beans.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@SuppressWarnings("UnusedDeclaration")
@Stateless(name = "BriefcaseEngine", mappedName="BriefcaseEngine")
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class BriefcaseEngineBean implements BriefcaseEngine, BriefcaseEngineLocal {
    private static final Log LOG = LogFactory.getLog(BriefcaseEngineBean.class);
    private static final int MAX_METADATA_LENGTH = 4096;

    @Resource
    javax.ejb.SessionContext ctx;
    @EJB
    SequencerEngineLocal seq;
    @EJB
    ACLEngineLocal acl;

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long create(String name, String description, Long aclId) throws FxApplicationException {
        return create(name, description, aclId, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long create(String name, String description, Long aclId, LifeCycleInfo forcedLifeCycleInfo) throws FxApplicationException {
        final UserTicket ticket = FxContext.getUserTicket();

        if (description == null) {
            description = "";
        }
        if (name == null || name.trim().length() == 0) {
            throw new FxInvalidParameterException("ex.briefcase.nameMissing", "name");
        }
        if (aclId != null && aclId != -1) {
            ACL acl;
            try {
                acl = new ACLEngineBean().load(aclId);
            } catch (Throwable t) {
                throw new FxInvalidParameterException("ex.briefcase.invalidAcl", "acl");
            }
            if (!ticket.mayCreateACL(aclId, ticket.getUserId())) {
                throw new FxNoAccessException("ex.briefcase.noCreatePermission", acl.getLabel());
            }

        }
        if (forcedLifeCycleInfo != null && !ticket.isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.briefcase.lciPermission");
        }
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        String sourceQuery = "";

        try {
            // Obtain a database connection
            con = Database.getDbConnection();

            // Obtain a new id
            long newId = seq.getId(FxSystemSequencer.BRIEFCASE);

            sql = "INSERT INTO " + DatabaseConst.TBL_BRIEFCASE + "("
                    + //1,   2,  3        ,  4         , 5 ,    6        7         8              9      , 10     , 11
                    "ID,NAME,DESCRIPTION,SOURCE_QUERY,ACL,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT,MANDATOR,ICON_ID)"
                    + "VALUES (?,?,?,?,?,?,?,?,?,?,1)";
            final long NOW = System.currentTimeMillis();
            ps = con.prepareStatement(sql);
            ps.setLong(1, newId);
            ps.setString(2, name);
            ps.setString(3, description);
            ps.setString(4, sourceQuery);
            if (aclId != null && aclId != -1) {
                ps.setLong(5, aclId);
            } else {
                ps.setNull(5, java.sql.Types.NUMERIC);
            }
            if (forcedLifeCycleInfo != null) {
                ps.setLong(6, forcedLifeCycleInfo.getCreatorId());
                ps.setLong(7, forcedLifeCycleInfo.getCreationTime());
                ps.setLong(8, forcedLifeCycleInfo.getModificatorId());
                ps.setLong(9, forcedLifeCycleInfo.getModificationTime());
            } else {
                ps.setLong(6, ticket.getUserId());
                ps.setLong(7, NOW);
                ps.setLong(8, ticket.getUserId());
                ps.setLong(9, NOW);
            }
            ps.setLong(10, ticket.getMandatorId());
            ps.executeUpdate();
            return newId;
        } catch (SQLException exc) {
            final boolean uniqueConstraintViolation = StorageManager.isUniqueConstraintViolation(exc);
            if (ctx != null) {
                EJBUtils.rollback(ctx);
            } else {
                try {
                    if (con != null)
                        con.rollback();
                } catch (SQLException e) {
                    LOG.warn(e.getMessage(), e);
                }
            }
            if (uniqueConstraintViolation) {
                throw new FxEntryExistsException(LOG, "ex.briefcase.nameAlreadyExists", name);
            } else {
                throw new FxCreateException(LOG, exc, "ex.briefcase.createFailed");
            }
        } finally {
            closeObjects(BriefcaseEngineBean.class, con, ps);
        }
    }

    

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void modify(long id, String name, String description, Long aclId) throws FxApplicationException {

        // Anything to do?
        if (name != null && name.trim().length() == 0) {
            name = null;
        }
        if (name == null && description == null && aclId == null) {
            return;
        }

        // Lookup the briefcase
        Briefcase br = load(id);
        if (br == null) {
            throw new FxNotFoundException("ex.briefcase.notFound", ("#" + id));
        }
        // Permission checks
        checkEditBriefcase(br);
        // Delete operation
        Connection con = null;
        PreparedStatement ps = null;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            String sSql = "update " + DatabaseConst.TBL_BRIEFCASE + " set" +
                    ((name == null) ? "" : " name=?, ") +
                    ((aclId == null) ? "" : " acl=?, ") +
                    ((description == null) ? "" : " description=?, ") +
                    "mandator=mandator where id=" + id;
            ps = con.prepareStatement(sSql);
            int pos = 1;
            if (name != null) ps.setString(pos++, name);
            if (aclId != null) {
                if (aclId == -1) {
                    ps.setNull(pos++, java.sql.Types.NUMERIC);
                } else {
                    ps.setLong(pos++, aclId);
                }
            }
            if (description != null) ps.setString(pos, description);
            ps.executeUpdate();
        } catch (SQLException exc) {
            EJBUtils.rollback(ctx);
            throw new FxLoadException(LOG, exc, "ex.briefcase.modifyFailed", br.getName());
        } finally {
            closeObjects(BriefcaseEngineBean.class, con, ps);
        }
    }

    private void checkEditBriefcase(Briefcase br) throws FxNotFoundException {
        final UserTicket ticket = FxContext.getUserTicket();
        if (!ticket.isGlobalSupervisor() && br.getMandator() != ticket.getMandatorId()) {
            if (!ticket.mayEditACL((br.getAcl()), br.getLifeCycleInfo().getCreatorId())) {
                throw new FxNotFoundException("ex.briefcase.noEditPermission", br.getName());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(long id) throws FxApplicationException {
        // Lookup the briefcase
        Briefcase br = load(id);
        if (br == null) {
            throw new FxNotFoundException("ex.briefcase.notFound", ("#" + id));
        }
        // Permission checks
        final UserTicket ticket = FxContext.getUserTicket();
        if (!ticket.isGlobalSupervisor() && br.getMandator() != ticket.getMandatorId()) {
            if (!ticket.mayDeleteACL(br.getAcl(), br.getLifeCycleInfo().getCreatorId())) {
                throw new FxNotFoundException("ex.briefcase.noDeletePermission", br.getName());
            }
        }
        // Delete operation
        Connection con = null;
        Statement stmt = null;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            stmt = con.createStatement();
            stmt.addBatch("DELETE FROM " + TBL_BRIEFCASE_DATA_ITEM + " WHERE briefcase_id=" + id);
            stmt.addBatch("DELETE FROM " + TBL_BRIEFCASE_DATA + " WHERE briefcase_id=" + id);
            stmt.addBatch("DELETE FROM " + DatabaseConst.TBL_BRIEFCASE + " WHERE id=" + id);
            stmt.executeBatch();
        } catch (SQLException exc) {
            throw new FxRemoveException(LOG, exc, "ex.briefcase.deleteFailed", br.getName());
        } finally {
            closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<Briefcase> loadAll(boolean includeShared) throws FxApplicationException {
        return getList(null, includeShared);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Briefcase load(long id) throws FxApplicationException {
        List<Briefcase> l = getList(id, true);
        if (l != null && l.size() > 0) {
            return l.get(0);
        } else {
            throw new FxNotFoundException(LOG, "ex.briefcase.notFound.id", id);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void clear(long id) throws FxApplicationException {
        Connection con = null;
        PreparedStatement stmt = null;
        final Briefcase br = load(id);
        checkEditBriefcase(br);
        try {
            con = Database.getDbConnection();
            stmt = con.prepareStatement("DELETE FROM " + TBL_BRIEFCASE_DATA_ITEM + " WHERE briefcase_id=?");
            stmt.setLong(1, id);
            stmt.executeUpdate();
            stmt.close();
            stmt = con.prepareStatement("DELETE FROM " + TBL_BRIEFCASE_DATA + " WHERE briefcase_id=?");
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(LOG, e, "ex.briefcase.clear", br.getName(), e);
        } finally {
            closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addItems(long id, Collection<FxPK> contents) throws FxApplicationException {
        Connection con = null;
        PreparedStatement stmt = null;
        final Briefcase br = load(id);
        checkEditBriefcase(br);
        try {
            con = Database.getDbConnection();

            // keep lookup table of existing items to avoid adding an item twice
            final Set<Long> existingItems = new HashSet<Long>();
            final long[] items = getItems(id);
            for (long item : items) {
                existingItems.add(item);
            }

            stmt = con.prepareStatement("SELECT MAX(pos) FROM " + TBL_BRIEFCASE_DATA + " WHERE briefcase_id=?");
            stmt.setLong(1, id);
            final ResultSet rs = stmt.executeQuery();
            int pos = rs.next() ? rs.getInt(1) : 0;
            stmt.close();
            stmt = con.prepareStatement("INSERT INTO " + TBL_BRIEFCASE_DATA
                    + "(briefcase_id, id, pos, amount) VALUES (?, ?, ?, 1)");
            stmt.setLong(1, id);
            for (FxPK pk : contents) {
                if (!existingItems.contains(pk.getId())) {
                    stmt.setLong(2, pk.getId());
                    stmt.setLong(3, ++pos);
                    stmt.addBatch();
                    existingItems.add(pk.getId());
                }
            }
            stmt.executeBatch();
        } catch (Exception e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(LOG, e, "ex.briefcase.addItems", br.getName(), e);
        } finally {
            closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeItems(long id, Collection<FxPK> contents) throws FxApplicationException {
        if (contents == null || contents.isEmpty()) {
            return;
        }
        Connection con = null;
        PreparedStatement stmt = null;
        final Briefcase br = load(id);
        checkEditBriefcase(br);
        try {
            con = Database.getDbConnection();
            stmt = con.prepareStatement("DELETE FROM " + TBL_BRIEFCASE_DATA_ITEM + " WHERE briefcase_id=?" +
                    " AND id IN (" + StringUtils.join(FxPK.getIds(contents), ',') + ")");
            stmt.setLong(1, id);
            stmt.executeUpdate();
            stmt.close();
            stmt = con.prepareStatement("DELETE FROM " + TBL_BRIEFCASE_DATA + " WHERE briefcase_id=?" +
                    " AND id IN (" + StringUtils.join(FxPK.getIds(contents), ',') + ")");
            stmt.setLong(1, id);
            stmt.executeUpdate();
        } catch (Exception e) {
            EJBUtils.rollback(ctx);
            throw new FxRemoveException(LOG, e, "ex.briefcase.removeItems", br.getName(), e);
        } finally {
            closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateItems(long id, Collection<FxPK> addContents, Collection<FxPK> removeContents) throws FxApplicationException {
        removeItems(id, removeContents);
        addItems(id, addContents);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setItems(long id, Collection<FxPK> objectIds) throws FxApplicationException {
        clear(id);
        addItems(id, objectIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void moveItems(long fromId, long toId, Collection<FxPK> objectIds) throws FxApplicationException {
        removeItems(fromId, objectIds);
        addItems(toId, objectIds);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long[] getItems(long id) throws FxApplicationException {
        Connection con = null;
        PreparedStatement stmt = null;
        final Briefcase br = load(id);
        try {
            con = Database.getDbConnection();
            stmt = con.prepareStatement("SELECT id FROM " + TBL_BRIEFCASE_DATA + " WHERE briefcase_id=?");
            stmt.setLong(1, id);
            final ResultSet rs = stmt.executeQuery();
            final List<Long> result = new ArrayList<Long>();
            while (rs.next()) {
                result.add(rs.getLong(1));
            }
            return ArrayUtils.toPrimitive(result.toArray(new Long[result.size()]));
        } catch (Exception e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(LOG, e, "ex.briefcase.getItems", br.getName(), e);
        } finally {
            closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void setMetaData(long id, Collection<FxReferenceMetaData<FxPK>> metadata) throws FxApplicationException {
        checkEditBriefcase(load(id));
        Connection con = null;
        
        try {
            con = Database.getDbConnection();
            replaceMetaData(con, id, metadata);
        } catch (SQLException e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(LOG, e);
        } finally {
            closeObjects(BriefcaseEngineBean.class, con, null);
        }
    }

    private void replaceMetaData(Connection con, long id, Collection<FxReferenceMetaData<FxPK>> metadata) throws FxUpdateException {
        PreparedStatement stmt = null;
        boolean success = false;
        try {
            stmt = con.prepareStatement("UPDATE " + TBL_BRIEFCASE_DATA + " SET metadata=? WHERE briefcase_id=? AND id=?");
            stmt.setLong(2, id);

            for (FxReferenceMetaData<FxPK> metaData : metadata) {
                final String meta = metaData.getSerializedForm();
                if (meta.length() > MAX_METADATA_LENGTH) {
                    throw new FxUpdateException("ex.briefcase.metadata.size", MAX_METADATA_LENGTH);
                }
                stmt.setString(1, meta);
                stmt.setLong(3, metaData.getReference().getId());
                stmt.addBatch();
            }

            stmt.executeBatch();
            success = true;
        } catch (SQLException e) {
            throw new FxUpdateException(LOG, e);
        } finally {
            if (!success) {
                EJBUtils.rollback(ctx);
            }
            closeObjects(BriefcaseEngineBean.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void mergeMetaData(long id, Collection<FxReferenceMetaData<FxPK>> metaData) throws FxApplicationException {
        checkEditBriefcase(load(id));
        PreparedStatement stmt = null;
        Connection con = null;
        boolean success = false;
        try {
            con = Database.getDbConnection();

            // merge changes into existing metadata
            final List<FxReferenceMetaData<FxPK>> currentMeta = loadMetaData(con, id, -1, true);
            final List<FxReferenceMetaData<FxPK>> newMeta = Lists.newArrayListWithCapacity(currentMeta.size());
            for (FxReferenceMetaData<FxPK> update : metaData) {
                if (update.getReference() == null) {
                    throw new FxUpdateException(LOG, "ex.briefcase.metadata.update.reference");
                }
                // find existing metadata to apply changes to
                final FxReferenceMetaData<FxPK> oldMeta = FxReferenceMetaData.findByContent(currentMeta, update.getReference());
                if (oldMeta == null) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("No matching row found for briefcase item " + update.getReference() + ", ignoring.");
                    }
                } else {
                    // merge changes
                    oldMeta.merge(update);
                    newMeta.add(oldMeta);
                }
            }

            // write back changes
            replaceMetaData(con, id, newMeta);

            success = true;
        } catch (SQLException e) {
            throw new FxUpdateException(LOG, e);
        } finally {
            if (!success) {
                EJBUtils.rollback(ctx);
            }
            closeObjects(BriefcaseEngineBean.class, con, stmt);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public List<FxReferenceMetaData<FxPK>> getMetaData(long briefcaseId) throws FxApplicationException {
        load(briefcaseId);   // check read permissions
        Connection con = null;

        try {
            con = Database.getDbConnection();
            return loadMetaData(con, briefcaseId, -1, false);
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e);
        } finally {
            Database.closeObjects(BriefcaseEngineBean.class, con, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxReferenceMetaData<FxPK> getMetaData(long briefcaseId, FxPK pk) throws FxApplicationException {
        load(briefcaseId);   // check read permissions
        Connection con = null;

        try {
            con = Database.getDbConnection();
            final List<FxReferenceMetaData<FxPK>> meta = loadMetaData(con, briefcaseId, pk.getId(), false);
            return meta.isEmpty() ? null : meta.get(0);
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e);
        } finally {
            Database.closeObjects(BriefcaseEngineBean.class, con, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addItemData(long briefcaseId, BriefcaseItemData itemData) throws FxApplicationException {
        if(itemData == null)
            return;
        Briefcase br = load(briefcaseId);   // check read permissions
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = Database.getDbConnection();
            // check if the item actually exists
            stmt = con.prepareStatement("SELECT COUNT(*) FROM " + TBL_BRIEFCASE_DATA + " WHERE briefcase_id=? AND id=?");
            stmt.setLong(1, briefcaseId);
            stmt.setLong(2, itemData.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs == null || !rs.next() || rs.getLong(1) != 1)
                throw new FxNotFoundException(LOG, "ex.briefcase.notFound.item", itemData.getId(), br.getName());
            stmt.close();
            stmt = con.prepareStatement("SELECT MAX(pos) FROM " + TBL_BRIEFCASE_DATA_ITEM + " WHERE briefcase_id=? AND id=?");
            stmt.setLong(1, briefcaseId);
            stmt.setLong(2, itemData.getId());
            rs = stmt.executeQuery();
            int pos = rs.next() ? rs.getInt(1) : 0;
            stmt.close();
            stmt = con.prepareStatement("INSERT INTO " + TBL_BRIEFCASE_DATA_ITEM
                    + "(briefcase_id, id, pos, intflag1, intflag2, intflag3, longflag1, longflag2, metadata) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            stmt.setLong(1, briefcaseId);
            stmt.setLong(2, itemData.getId());
            stmt.setLong(3, ++pos);
            if(itemData.isIntFlagSet(1))
                stmt.setInt(4, itemData.getIntFlag1());
            else
                stmt.setNull(4, Types.INTEGER);
            if (itemData.isIntFlagSet(2))
                stmt.setInt(5, itemData.getIntFlag2());
            else
                stmt.setNull(5, Types.INTEGER);
            if (itemData.isIntFlagSet(3))
                stmt.setInt(6, itemData.getIntFlag3());
            else
                stmt.setNull(6, Types.INTEGER);
            if (itemData.isLongFlagSet(1))
                stmt.setLong(7, itemData.getLongFlag1());
            else
                stmt.setNull(7, Types.BIGINT);
            if (itemData.isLongFlagSet(2))
                stmt.setLong(8, itemData.getLongFlag2());
            else
                stmt.setNull(8, Types.BIGINT);
            stmt.setString(9, itemData.getMetaData());
            stmt.executeUpdate();
        } catch (Exception e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(LOG, e, "ex.briefcase.addItemData", br.getName(), itemData.getId(), e.getMessage());
        } finally {
            Database.closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void addItemData(long briefcaseId, List<BriefcaseItemData> itemDatas) throws FxApplicationException {
        if(itemDatas == null || itemDatas.size() == 0)
            return;
        Briefcase br = load(briefcaseId);   // check read permissions
        Connection con = null;
        PreparedStatement stmt = null;
        long lastId = -1;
        int pos = -1;
        try {
            con = Database.getDbConnection();

            for (BriefcaseItemData itemData : itemDatas) {
                if(lastId != itemData.getId()) {
                    lastId = itemData.getId();
                    if(stmt != null) {
                        stmt.executeBatch();
                        stmt.close();
                    }
                    //existance check and evaluate position
                    stmt = con.prepareStatement("SELECT COUNT(*) FROM " + TBL_BRIEFCASE_DATA + " WHERE briefcase_id=? AND id=?");
                    stmt.setLong(1, briefcaseId);
                    stmt.setLong(2, itemData.getId());
                    ResultSet rs = stmt.executeQuery();
                    if (rs == null || !rs.next() || rs.getLong(1) != 1)
                        throw new FxNotFoundException(LOG, "ex.briefcase.notFound.item", itemData.getId(), br.getName());
                    stmt.close();
                    stmt = con.prepareStatement("SELECT MAX(pos) FROM " + TBL_BRIEFCASE_DATA_ITEM + " WHERE briefcase_id=? AND id=?");
                    stmt.setLong(1, briefcaseId);
                    stmt.setLong(2, itemData.getId());
                    rs = stmt.executeQuery();
                    pos = rs.next() ? rs.getInt(1) : 0;
                    stmt.close();
                    stmt = con.prepareStatement("INSERT INTO " + TBL_BRIEFCASE_DATA_ITEM
                            + "(briefcase_id, id, pos, intflag1, intflag2, intflag3, longflag1, longflag2, metadata) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                    stmt.setLong(1, briefcaseId);
                    stmt.setLong(2, itemData.getId());
                }
                if(stmt == null ) {
                    LOG.fatal("PreparedStatement was null trying to add briefcase item data!");
                    continue;
                }
                stmt.setLong(3, ++pos);
                if (itemData.isIntFlagSet(1))
                    stmt.setInt(4, itemData.getIntFlag1());
                else
                    stmt.setNull(4, Types.INTEGER);
                if (itemData.isIntFlagSet(2))
                    stmt.setInt(5, itemData.getIntFlag2());
                else
                    stmt.setNull(5, Types.INTEGER);
                if (itemData.isIntFlagSet(3))
                    stmt.setInt(6, itemData.getIntFlag3());
                else
                    stmt.setNull(6, Types.INTEGER);
                if (itemData.isLongFlagSet(1))
                    stmt.setLong(7, itemData.getLongFlag1());
                else
                    stmt.setNull(7, Types.BIGINT);
                if (itemData.isLongFlagSet(2))
                    stmt.setLong(8, itemData.getLongFlag2());
                else
                    stmt.setNull(8, Types.BIGINT);
                stmt.setString(9, itemData.getMetaData());
                stmt.addBatch();
            }
            if (stmt != null)
                stmt.executeBatch();
        } catch (Exception e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(LOG, e, "ex.briefcase.addItemData", br.getName(), lastId, e.getMessage());
        } finally {
            Database.closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeItemData(long briefcaseId, Long itemId) throws FxApplicationException {
        Briefcase br = load(briefcaseId);   // check read permissions
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = Database.getDbConnection();
            // check if the item actually exists
            stmt = con.prepareStatement("DELETE FROM " + TBL_BRIEFCASE_DATA_ITEM + " WHERE briefcase_id=? " + (itemId != null ? " AND id=?" : ""));
            stmt.setLong(1, briefcaseId);
            if(itemId != null)
                stmt.setLong(2, itemId);
            stmt.executeUpdate();
        } catch (Exception e) {
            EJBUtils.rollback(ctx);
            if(itemId != null)
                throw new FxRemoveException(LOG, e, "ex.briefcase.removeItemData.id", br.getName(), itemId, e.getMessage());
            else
                throw new FxRemoveException(LOG, e, "ex.briefcase.removeItemData.all", br.getName(), e.getMessage());
        } finally {
            Database.closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<BriefcaseItemData> queryItemData(long briefcaseId, Long itemId, String metaData,
                                                 Integer intFlag1, Integer intFlag2, Integer intFlag3,
                                                 Long longFlag1, Long longFlag2,
                                                 BriefcaseItemData.SortField sortField, BriefcaseItemData.SortOrder sortOrder) throws FxApplicationException {
        Briefcase br = load(briefcaseId);   // check read permissions
        StringBuilder query = new StringBuilder(1000);
        //                   1    2         3         4         5          6          7         8
        query.append("SELECT pos, intflag1, intflag2, intflag3, longflag1, longflag2, metadata, id FROM " + TBL_BRIEFCASE_DATA_ITEM + " WHERE briefcase_id=").
                append(briefcaseId);
        if(itemId != null)
                query.append(" AND id=").append(itemId);
        if(metaData != null)
            query.append(" AND metaData=?");
        if(intFlag1 != null)
            query.append(" AND intFlag1=").append(intFlag1);
        if(intFlag2 != null)
            query.append(" AND intFlag2=").append(intFlag2);
        if (intFlag3 != null)
            query.append(" AND intFlag3=").append(intFlag3);
        if (longFlag1 != null)
            query.append(" AND longFlag1=").append(longFlag1);
        if (longFlag2 != null)
            query.append(" AND longFlag2=").append(longFlag2);
        query.append(" ORDER BY ").append(sortField.name()).append(' ').append(sortOrder.name());
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = Database.getDbConnection();
            stmt = con.prepareStatement(query.toString());
            if(metaData != null)
                stmt.setString(1, metaData);
            final ResultSet rs = stmt.executeQuery();
            List<BriefcaseItemData> result = Lists.newArrayListWithCapacity(1000);
            while(rs != null && rs.next()) {
                BriefcaseItemData data = BriefcaseItemData.createBriefCaseItemData(briefcaseId, rs.getLong(8), rs.getString(7));
                data.setPos(rs.getInt(1));
                Integer intFlag = rs.getInt(2);
                if (!rs.wasNull())
                    data.setIntFlag1(intFlag);
                intFlag = rs.getInt(3);
                if (!rs.wasNull())
                    data.setIntFlag2(intFlag);
                intFlag = rs.getInt(4);
                if (!rs.wasNull())
                    data.setIntFlag3(intFlag);
                Long longFlag = rs.getLong(5);
                if (!rs.wasNull())
                    data.setLongFlag1(longFlag);
                longFlag = rs.getLong(6);
                if (!rs.wasNull())
                    data.setLongFlag2(longFlag);
                result.add(data);
            }
            return result;
        } catch (Exception e) {
            throw new FxUpdateException(LOG, e, "ex.briefcase.queryItemData", br.getName(), e.getMessage());
        } finally {
            Database.closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int queryItemDataCount(long briefcaseId, Long itemId, String metaData,
                                  Integer intFlag1, Integer intFlag2, Integer intFlag3,
                                  Long longFlag1, Long longFlag2) throws FxApplicationException {
        Briefcase br = load(briefcaseId);   // check read permissions
        StringBuilder query = new StringBuilder(1000);
        query.append("SELECT COUNT(*) FROM " + TBL_BRIEFCASE_DATA_ITEM + " WHERE briefcase_id=").append(briefcaseId);
        if (itemId != null)
            query.append(" AND id=").append(itemId);
        if (metaData != null)
            query.append(" AND metaData=?");
        if (intFlag1 != null)
            query.append(" AND intFlag1=").append(intFlag1);
        if (intFlag2 != null)
            query.append(" AND intFlag2=").append(intFlag2);
        if (intFlag3 != null)
            query.append(" AND intFlag3=").append(intFlag3);
        if (longFlag1 != null)
            query.append(" AND longFlag1=").append(longFlag1);
        if (longFlag2 != null)
            query.append(" AND longFlag2=").append(longFlag2);
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = Database.getDbConnection();
            stmt = con.prepareStatement(query.toString());
            if (metaData != null)
                stmt.setString(1, metaData);
            final ResultSet rs = stmt.executeQuery();
            if (rs != null && rs.next())
                return rs.getInt(1);
            return 0;
        } catch (Exception e) {
            throw new FxUpdateException(LOG, e, "ex.briefcase.queryItemData", br.getName(), e.getMessage());
        } finally {
            Database.closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateItemData(long briefcaseId, BriefcaseItemData updateData) throws FxApplicationException {
        if (updateData == null)
            return;
        Briefcase br = load(briefcaseId);   // check read permissions
        Connection con = null;
        PreparedStatement stmt = null;
        try {
            con = Database.getDbConnection();
            // check if the item actually exists
            stmt = con.prepareStatement("SELECT COUNT(*) FROM " + TBL_BRIEFCASE_DATA_ITEM + " WHERE briefcase_id=? AND id=? AND pos=?");
            stmt.setLong(1, briefcaseId);
            stmt.setLong(2, updateData.getId());
            stmt.setInt(3, updateData.getPos());
            ResultSet rs = stmt.executeQuery();
            if (rs == null || !rs.next() || rs.getLong(1) != 1)
                throw new FxNotFoundException(LOG, "ex.briefcase.notFound.item", updateData.getId(), br.getName());
            stmt.close();
            stmt = con.prepareStatement("UPDATE " + TBL_BRIEFCASE_DATA_ITEM
                    //               1           2           3            4            5           6                    7        8         9
                    + " SET intflag1=?, intflag2=?, intflag3=?, longflag1=?, longflag2=?, metadata=? WHERE briefcase_id=? AND id=? AND pos=?");
            stmt.setLong(7, briefcaseId);
            stmt.setLong(8, updateData.getId());
            stmt.setLong(9, updateData.getPos());
            if (updateData.isIntFlagSet(1))
                stmt.setInt(1, updateData.getIntFlag1());
            else
                stmt.setNull(1, Types.INTEGER);
            if (updateData.isIntFlagSet(2))
                stmt.setInt(2, updateData.getIntFlag2());
            else
                stmt.setNull(2, Types.INTEGER);
            if (updateData.isIntFlagSet(3))
                stmt.setInt(3, updateData.getIntFlag3());
            else
                stmt.setNull(3, Types.INTEGER);
            if (updateData.isLongFlagSet(1))
                stmt.setLong(4, updateData.getLongFlag1());
            else
                stmt.setNull(4, Types.BIGINT);
            if (updateData.isLongFlagSet(2))
                stmt.setLong(5, updateData.getLongFlag2());
            else
                stmt.setNull(5, Types.BIGINT);
            stmt.setString(6, updateData.getMetaData());
            stmt.executeUpdate();
        } catch (Exception e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(LOG, e, "ex.briefcase.updateItemData", br.getName(), updateData.getId(), e.getMessage());
        } finally {
            Database.closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    private List<FxReferenceMetaData<FxPK>> loadMetaData(Connection con, long id, long itemId, boolean forUpdate) throws FxApplicationException {
        PreparedStatement stmt = null;
        try {
            final List<FxReferenceMetaData<FxPK>> result = Lists.newArrayList();
            stmt = con.prepareStatement("SELECT id, metadata FROM " + TBL_BRIEFCASE_DATA
                    + " WHERE briefcase_id=?"
                    + (itemId == -1 ? "" : " AND id=?")
                    + " ORDER BY pos"
                    + (forUpdate ? " FOR UPDATE" : ""));
            stmt.setLong(1, id);
            if (itemId != -1) {
                stmt.setLong(2, itemId);
            }
            final ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(
                        FxReferenceMetaData.fromSerializedForm(new FxPK(rs.getLong(1)), rs.getString(2))
                );
            }
            return result;
        } catch (SQLException e) {
            throw new FxApplicationException(LOG, e);
        } finally {
            Database.closeObjects(BriefcaseEngineBean.class, null, stmt);
        }
    }

    /**
     * Builds a sql filer that only selects briefcases that the calling user has permissions on.
     *
     * @param briefcaseTblAlias the alias of the briefcase table, or null
     * @param includeShared     true if shared briefcases should be included
     * @param perms             the permissions that are needed
     * @return a sql filter, eg '([briefcaseTblAlias.]CREATED_BY=12 OR ACL IS NOT NULL)'
     */
    private String getSqlAccessFilter(String briefcaseTblAlias, boolean includeShared, ACLPermission... perms) {
        final UserTicket ticket = FxContext.getUserTicket();
        StringBuilder filter = new StringBuilder(1024);
        if (briefcaseTblAlias == null) {
            briefcaseTblAlias = "";
        }
        if (briefcaseTblAlias.length() > 0) {
            briefcaseTblAlias = briefcaseTblAlias + ".";
        }
        final String colMANDATOR = briefcaseTblAlias + "MANDATOR";
        final String colACL = briefcaseTblAlias + "ACL";
        final String colCREATED_BY = briefcaseTblAlias + "CREATED_BY";
        filter.append("((").append(colCREATED_BY).append("=").append(ticket.getUserId())
                .append(" AND ").append(colACL).append(" IS NULL)");
        if (includeShared) {
            if (ticket.isGlobalSupervisor()) {
                // add all shared
                filter.append(" OR ").append(colACL).append(" IS NOT null");
            } else if (ticket.isMandatorSupervisor()) {
                // add all shared(match by ACL or mandator)
                String acls = ticket.getACLsCSV(0/*owner is irrelevant here*/, ACLCategory.BRIEFCASE, perms);
                filter.append((acls.length() > 0) ? (" OR " + colACL + " IN (" + acls + ") ") : "").
                        append(" OR (").append(colACL).append(" IS NOT null AND ").
                        append(colMANDATOR).append("=").append(ticket.getMandatorId()).append(")");
            } else {
                // add all shared(match by ACL)
                String acls = ticket.getACLsCSV(0/*owner is irrelevant here*/, ACLCategory.BRIEFCASE, perms);
                if (acls.length() > 0) {
                    filter.append(" OR ").append(colACL).append(" IN (").append(acls).append(") ");
                }
            }
        }
        filter.append(")");
        return filter.toString();
    }

    /**
     * Gets a list of all briefcase for the calling user.
     *
     * @param idFilter      if set only the pricelist with the given id will be loaded
     * @param includeShared if enabled shared briefcases will be included, if disabled only
     *                      the briefcases created by the calling user will be returned
     * @return the briefcases
     * @throws FxApplicationException if the function fails
     */
    private List<Briefcase> getList(Long idFilter, boolean includeShared) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        String sql;
        final ArrayList<Briefcase> result = new ArrayList<Briefcase>(500);
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            sql = "SELECT " +
                    //1,   2,  3        ,  4         , 5 ,    6        7         8              9      ,  10    , 11
                    "ID,NAME,DESCRIPTION,SOURCE_QUERY,ACL,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT,MANDATOR,ICON_ID, " +
                    // 12
                    "(SELECT COUNT(*) FROM " + TBL_BRIEFCASE_DATA + " bd WHERE bd.briefcase_id=b.id) AS \"size\"" +
                    "FROM " + DatabaseConst.TBL_BRIEFCASE + " b WHERE ";
            sql += getSqlAccessFilter(null, includeShared, ACLPermission.READ);
            if (idFilter != null) {
                sql += " and id=" + idFilter;
            }

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            while (rs != null && rs.next()) {
                final long id = rs.getLong(1);
                final String name = rs.getString(2);
                String desc = rs.getString(3);
                if( rs.wasNull() )
                    desc = "";
                String src = rs.getString(4);
                if( rs.wasNull() )
                    src = "";
                long acl = rs.getLong(5);
                if (rs.wasNull())
                    acl = -1;
                final LifeCycleInfo lc = LifeCycleInfoImpl.load(rs, 6, 7, 8, 9);
                final long mandator = rs.getLong(10);
                final long iconId = rs.getLong(11);
                final int size = rs.getInt(12);
                result.add(new Briefcase(id, name, mandator, desc, src, acl, lc, iconId, size));
            }
            result.trimToSize();
            return result;
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, exc, "ex.briefcase.failedToLoadList", exc.getMessage());
        } finally {
            closeObjects(BriefcaseEngineBean.class, con, stmt);
        }
    }

    private List<FxPK> toPKList(long[] objectIds) {
        final List<FxPK> result = new ArrayList<FxPK>(objectIds.length);
        for (long id : objectIds) {
            result.add(new FxPK(id));
        }
        return result;
    }
}
