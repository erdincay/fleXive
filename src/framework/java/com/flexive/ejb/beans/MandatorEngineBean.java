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
import static com.flexive.core.DatabaseConst.TBL_GROUP;
import static com.flexive.core.DatabaseConst.TBL_MANDATORS;
import com.flexive.core.LifeCycleInfoImpl;
import com.flexive.core.structure.StructureLoader;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxEnvironment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Mandator Engine implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "MandatorEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
public class MandatorEngineBean implements MandatorEngine, MandatorEngineLocal {
    // Our logger
    private static final Log LOG = LogFactory.getLog(MandatorEngineBean.class);

    @Resource
    javax.ejb.SessionContext ctx;

    @EJB
    SequencerEngineLocal seq;
    @EJB
    UserGroupEngineLocal grp;

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int create(String name, boolean active) throws FxApplicationException {
        final UserTicket ticket = FxContext.get().getTicket();
        final FxEnvironment environment;
        // Security
        FxPermissionUtils.checkRole(ticket, Role.GlobalSupervisor);
        FxSharedUtils.checkParameterEmpty(name, "NAME");
        environment = CacheAdmin.getEnvironment();
        //exist check
        for (Mandator m : environment.getMandators(true, true))
            if (m.getName().equalsIgnoreCase(name))
                throw new FxEntryExistsException("ex.mandator.exists", name);

        Connection con = null;
        PreparedStatement ps = null;
        String sql;

        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // Obtain a new id
            int newId = (int) seq.getId(SequencerEngine.System.MANDATOR);

            sql = "INSERT INTO " + TBL_MANDATORS + "(" +
                    //1 2    3        4         5          6          7           8
                    "ID,NAME,METADATA,IS_ACTIVE,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT)" +
                    "VALUES (?,?,?,?,?,?,?,?)";
            final long NOW = System.currentTimeMillis();
            ps = con.prepareStatement(sql);
            ps.setInt(1, newId);
            ps.setString(2, name.trim());
            ps.setNull(3, java.sql.Types.INTEGER);
            ps.setBoolean(4, active);
            ps.setLong(5, ticket.getUserId());
            ps.setLong(6, NOW);
            ps.setLong(7, ticket.getUserId());
            ps.setLong(8, NOW);
            ps.executeUpdate();
            ps.close();
            sql = "INSERT INTO " + TBL_GROUP + " " +
                    "(ID,MANDATOR,AUTOMANDATOR,ISSYSTEM,NAME,COLOR,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT) VALUES (" +
                    "?,?," + newId + ",TRUE,?,?,?,?,?,?)";
            ps = con.prepareStatement(sql);
            long gid = seq.getId(SequencerEngine.System.GROUP);
            ps.setLong(1, gid);
            ps.setLong(2, newId);
            ps.setString(3, "Everyone (" + name.trim() + ")");
            ps.setString(4, "#00AA00");
            ps.setLong(5, 0);
            ps.setLong(6, NOW);
            ps.setLong(7, 0);
            ps.setLong(8, NOW);
            ps.executeUpdate();
            StructureLoader.addMandator(FxContext.get().getDivisionId(), new Mandator(newId, name.trim(), -1, active,
                    new LifeCycleInfoImpl(ticket.getUserId(), NOW, ticket.getUserId(), NOW)));
            return newId;
        } catch (SQLException exc) {
            final boolean uniqueConstraintViolation = Database.isUniqueConstraintViolation(exc);
            ctx.setRollbackOnly();
            if (uniqueConstraintViolation) {
                throw new FxEntryExistsException(LOG, "ex.mandator.exists", name);
            } else {
                throw new FxCreateException(LOG, exc, "ex.mandator.createFailed", name, exc.getMessage());
            }
        } finally {
            Database.closeObjects(MandatorEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void assignMetaData(int mandatorId, long contentId) throws FxApplicationException {
        //TODO: code me!
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeMetaData(int mandatorId) throws FxApplicationException {
        //TODO: code me!
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void activate(long mandatorId) throws FxApplicationException {
        final UserTicket ticket = FxContext.get().getTicket();
        final FxEnvironment environment;
        // Security
        FxPermissionUtils.checkRole(ticket, Role.GlobalSupervisor);
        environment = CacheAdmin.getEnvironment();
        //exist check
        Mandator mand = environment.getMandator(mandatorId);
        if (mand.isActive())
            return; //silently ignore
        Connection con = null;
        PreparedStatement ps = null;
        String sql;

        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            //                                                1              2              3          4
            sql = "UPDATE " + TBL_MANDATORS + " SET IS_ACTIVE=?, MODIFIED_BY=?, MODIFIED_AT=? WHERE ID=?";
            final long NOW = System.currentTimeMillis();
            ps = con.prepareStatement(sql);

            ps.setBoolean(1, true);
            ps.setLong(2, ticket.getUserId());
            ps.setLong(3, NOW);
            ps.setLong(4, mandatorId);
            ps.executeUpdate();
            StructureLoader.updateMandator(FxContext.get().getDivisionId(), new Mandator(mand.getId(), mand.getName(),
                    mand.getMetadataId(), true, new LifeCycleInfoImpl(mand.getLifeCycleInfo().getCreatorId(),
                    mand.getLifeCycleInfo().getCreationTime(), ticket.getUserId(), NOW)));
        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxUpdateException(LOG, exc, "ex.mandator.updateFailed", mand.getName(), exc.getMessage());
        } finally {
            Database.closeObjects(MandatorEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void deactivate(long mandatorId) throws FxApplicationException {
        final UserTicket ticket = FxContext.get().getTicket();
        final FxEnvironment environment;
        // Security
        FxPermissionUtils.checkRole(ticket, Role.GlobalSupervisor);
        environment = CacheAdmin.getEnvironment();
        //exist check
        Mandator mand = environment.getMandator(mandatorId);
        if (!mand.isActive())
            return; //silently ignore
        if( mand.getId() == ticket.getMandatorId() )
            throw new FxInvalidParameterException("mandatorId", "ex.mandator.deactivate.own", mand.getName(), mand.getId());
        Connection con = null;
        PreparedStatement ps = null;
        String sql;

        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            //                                                1              2              3          4
            sql = "UPDATE " + TBL_MANDATORS + " SET IS_ACTIVE=?, MODIFIED_BY=?, MODIFIED_AT=? WHERE ID=?";
            final long NOW = System.currentTimeMillis();
            ps = con.prepareStatement(sql);

            ps.setBoolean(1, false);
            ps.setLong(2, ticket.getUserId());
            ps.setLong(3, NOW);
            ps.setLong(4, mandatorId);
            ps.executeUpdate();
            StructureLoader.updateMandator(FxContext.get().getDivisionId(), new Mandator(mand.getId(), mand.getName(),
                    mand.getMetadataId(), false, new LifeCycleInfoImpl(mand.getLifeCycleInfo().getCreatorId(),
                    mand.getLifeCycleInfo().getCreationTime(), ticket.getUserId(), NOW)));
        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxUpdateException(LOG, exc, "ex.mandator.updateFailed", mand.getName(), exc.getMessage());
        } finally {
            Database.closeObjects(MandatorEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(long mandatorId) throws FxApplicationException {
        final UserTicket ticket = FxContext.get().getTicket();
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        // Security
        FxPermissionUtils.checkRole(ticket, Role.GlobalSupervisor);
        //exist check
        Mandator mand = environment.getMandator(mandatorId);
        Connection con = null;
        PreparedStatement ps = null;
        String sql;

        try {
            try {
                FxContext.get().runAsSystem();
                grp.remove(grp.loadMandatorGroup(mandatorId).getId());
            } finally {
                FxContext.get().stopRunAsSystem();
            }
            con = Database.getDbConnection();
            //                                                  1
            sql = "DELETE FROM " + TBL_GROUP + " WHERE MANDATOR=? AND AUTOMANDATOR=TRUE";
            ps = con.prepareStatement(sql);
            ps.setLong(1, mandatorId);
            ps.executeUpdate();
            ps.close();
            //                                                1
            sql = "DELETE FROM " + TBL_MANDATORS + " WHERE ID=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, mandatorId);
            ps.executeUpdate();
            StructureLoader.removeMandator(FxContext.get().getDivisionId(), mandatorId);
        } catch (SQLException exc) {
            final boolean keyViolation = Database.isForeignKeyViolation(exc);
            ctx.setRollbackOnly();
            if (keyViolation)
                throw new FxEntryInUseException(exc, "ex.mandator.removeFailed.inUse", mand.getName());
            throw new FxRemoveException(LOG, exc, "ex.mandator.removeFailed", mand.getName(), exc.getMessage());
        } finally {
            Database.closeObjects(MandatorEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void changeName(long mandatorId, String name) throws FxApplicationException {
        FxSharedUtils.checkParameterEmpty(name, "name");
        final UserTicket ticket = FxContext.get().getTicket();
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        // Security
        FxPermissionUtils.checkRole(ticket, Role.GlobalSupervisor);
        //exist check
        Mandator mand = environment.getMandator(mandatorId);
        Connection con = null;
        PreparedStatement ps = null;
        String sql;

        try {
            name = name.trim();
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                           1              2              3          4
            sql = "UPDATE " + TBL_MANDATORS + " SET NAME=?, MODIFIED_BY=?, MODIFIED_AT=? WHERE ID=?";
            final long NOW = System.currentTimeMillis();
            ps = con.prepareStatement(sql);
            ps.setString(1, name.trim());
            ps.setLong(2, ticket.getUserId());
            ps.setLong(3, NOW);
            ps.setLong(4, mandatorId);
            ps.executeUpdate();
            ps.close();
            sql = "UPDATE " + TBL_GROUP + " SET NAME=? WHERE AUTOMANDATOR=?";
            ps = con.prepareStatement(sql);
            ps.setString(1, "Everyone (" + name + ")");
            ps.setLong(2, mandatorId);
            ps.executeUpdate();
            StructureLoader.updateMandator(FxContext.get().getDivisionId(), new Mandator(mand.getId(), name,
                    mand.getMetadataId(), mand.isActive(), new LifeCycleInfoImpl(mand.getLifeCycleInfo().getCreatorId(),
                    mand.getLifeCycleInfo().getCreationTime(), ticket.getUserId(), NOW)));
        } catch (SQLException exc) {
            // check before rollback, because it might need an active transaciton
            final boolean uniqueConstraintViolation = Database.isUniqueConstraintViolation(exc);
            ctx.setRollbackOnly();
            if (uniqueConstraintViolation) {
                throw new FxUpdateException(LOG, "ex.mandator.update.name.unique", name);
            } else {
                throw new FxUpdateException(LOG, exc, "ex.mandator.updateFailed", mand.getName(), exc.getMessage());
            }
        } finally {
            Database.closeObjects(MandatorEngineBean.class, con, ps);
        }
    }

}
