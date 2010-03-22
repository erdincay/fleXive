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
package com.flexive.ejb.beans.structure;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.*;
import com.flexive.core.LifeCycleInfoImpl;
import com.flexive.core.flatstorage.FxFlatStorage;
import com.flexive.core.storage.StorageManager;
import com.flexive.core.conversion.ConversionEngine;
import com.flexive.core.flatstorage.FxFlatStorageManager;
import com.flexive.core.structure.FxPreloadType;
import com.flexive.core.structure.StructureLoader;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSystemSequencer;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.*;
import com.flexive.ejb.beans.EJBUtils;
import com.thoughtworks.xstream.converters.ConversionException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.*;
import java.util.*;


/**
 * FxType management
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "TypeEngine", mappedName = "TypeEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class TypeEngineBean implements TypeEngine, TypeEngineLocal {

    private static final Log LOG = LogFactory.getLog(TypeEngineBean.class);
    @Resource
    javax.ejb.SessionContext ctx;

    @EJB
    SequencerEngineLocal seq;

    @EJB
    MandatorEngineLocal mandatorEngine;

    @EJB
    ACLEngineLocal aclEngine;

    @EJB
    AssignmentEngineLocal assignmentEngine;

    @EJB
    ContentEngineLocal contentEngine;

    @EJB
    HistoryTrackerEngineLocal htracker;

    public static final String TYPE_CREATE =
            //                                     1   2     3       4             5         6
            "INSERT INTO " + TBL_STRUCT_TYPES + " (ID, NAME, PARENT, STORAGE_MODE, CATEGORY, TYPE_MODE, " +
                    //7          8           9              10            11           12
                    "LANG_MODE,  TYPE_STATE, SECURITY_MODE, TRACKHISTORY, HISTORY_AGE, MAX_VERSIONS, " +
                    //13               14                15          16          17           18           19   20        21
                    "REL_TOTAL_MAXSRC, REL_TOTAL_MAXDST, CREATED_BY, CREATED_AT, MODIFIED_BY, MODIFIED_AT, ACL, WORKFLOW, ICON_REF," +
                    // 22                   23
                    "MULTIPLE_CONTENT_ACLS, INSUPERTYPEQUERY)" +
                    " VALUES " +
                    "(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long save(FxTypeEdit type) throws FxApplicationException {
        if (type.isNew())
            return this.create(type);
        return this.update(type);
    }


    /**
     * Create a new type
     *
     * @param type the type to create
     * @return id of the new type
     * @throws FxApplicationException on errors
     */
    private long create(FxTypeEdit type) throws FxApplicationException {
        final UserTicket ticket = FxContext.getUserTicket();
        FxPermissionUtils.checkRole(ticket, Role.StructureManagement);
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        if (StringUtils.isEmpty(type.getName()))
            throw new FxInvalidParameterException("NAME", "ex.structure.create.nameMissing");
        if (!type.getStorageMode().isSupported())
            throw new FxInvalidParameterException("STORAGEMODE", "ex.structure.typeStorageMode.notSupported", type.getStorageMode().getLabel().getBestTranslation(ticket));
        if (type.getACL().getCategory() != ACLCategory.STRUCTURE)
            throw new FxInvalidParameterException("aclId", "ex.acl.category.invalid", type.getACL().getCategory().name(), ACLCategory.STRUCTURE.name());

        Connection con = null;
        PreparedStatement ps = null;
        long newId = seq.getId(FxSystemSequencer.TYPEDEF);
        final long NOW = System.currentTimeMillis();
        try {
            con = Database.getDbConnection();
            ps = con.prepareStatement(TYPE_CREATE);
            ps.setLong(1, newId);
            ps.setString(2, type.getName());
            if (type.getParent() != null)
                ps.setLong(3, type.getParent().getId());
            else
                ps.setNull(3, java.sql.Types.INTEGER);
            ps.setInt(4, type.getStorageMode().getId());
            ps.setInt(5, type.getCategory().getId());
            ps.setInt(6, type.getMode().getId());
            ps.setInt(7, type.getLanguage().getId());
            ps.setInt(8, type.getState().getId());
            ps.setByte(9, type.getBitCodedPermissions());
            ps.setBoolean(10, type.isTrackHistory());
            ps.setLong(11, type.getHistoryAge());
            ps.setLong(12, type.getMaxVersions());
            ps.setInt(13, type.getMaxRelSource());
            ps.setInt(14, type.getMaxRelDestination());
            ps.setLong(15, ticket.getUserId());
            ps.setLong(16, NOW);
            ps.setLong(17, ticket.getUserId());
            ps.setLong(18, NOW);
            ps.setLong(19, type.getACL().getId());
            ps.setLong(20, type.getWorkflow().getId());
            if (type.getIcon().isEmpty())
                ps.setNull(21, java.sql.Types.INTEGER);
            else
                ps.setLong(21, type.getIcon().getDefaultTranslation().getId());
            ps.setBoolean(22, type.isUseInstancePermissions() && type.isMultipleContentACLs());
            ps.setBoolean(23, type.isIncludedInSupertypeQueries());
            ps.executeUpdate();
            Database.storeFxString(type.getLabel(), con, TBL_STRUCT_TYPES, "DESCRIPTION", "ID", newId);

            StructureLoader.reload(con);
            FxType thisType = CacheAdmin.getEnvironment().getType(newId);
            htracker.track(thisType, "history.type.create", type.getName(), newId);

            //store relations
            ps.close();
            if (type.getAddedRelations().size() > 0) {
                ps = con.prepareStatement("INSERT INTO " + TBL_STRUCT_TYPERELATIONS + " (TYPEDEF,TYPESRC,TYPEDST,MAXSRC,MAXDST)VALUES(?,?,?,?,?)");
                ps.setLong(1, thisType.getId());
                for (FxTypeRelation rel : type.getAddedRelations()) {
                    if (rel.getSource().isRelation())
                        throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", type.getName(), rel.getSource().getName());
                    if (rel.getDestination().isRelation())
                        throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", type.getName(), rel.getDestination().getName());
                    ps.setLong(2, rel.getSource().getId());
                    ps.setLong(3, rel.getDestination().getId());
                    ps.setLong(4, rel.getMaxSource());
                    ps.setLong(5, rel.getMaxDestination());
                    ps.addBatch();
                    htracker.track(thisType, "history.type.create.relation.add", type.getName(), rel.getSource().getName(),
                            rel.getMaxSource(), rel.getDestination().getName(), rel.getMaxDestination());
                }
                ps.executeBatch();
            }

            if (type.getParent() == null) {
                for (FxPropertyAssignment spa : environment.getSystemInternalRootPropertyAssignments()) {
                    final FxPropertyAssignmentEdit derived = FxPropertyAssignmentEdit.createNew(spa, thisType, spa.getAlias(), "/");
                    assignmentEngine.save(
                            updateAclAssignmentMultiplicity(type, derived).setEnabled(true)._setSystemInternal(),
                            false
                    );
                }
            } else {
                //create parent assignments
                List<FxAssignment> parentAssignments = type.getParent().getConnectedAssignments("/");
                for (FxAssignment as : parentAssignments) {
                    if (as instanceof FxPropertyAssignment) {
                        FxPropertyAssignmentEdit pae = FxPropertyAssignmentEdit.
                                createNew((FxPropertyAssignment) as, thisType, as.getAlias(), "/");
                        pae.setEnabled(type.isEnableParentAssignments());
                        assignmentEngine.save(updateAclAssignmentMultiplicity(type, pae), false);
                    } else if (as instanceof FxGroupAssignment) {
                        FxGroupAssignmentEdit pge = FxGroupAssignmentEdit.
                                createNew((FxGroupAssignment) as, thisType, as.getAlias(), "/");
                        pge.setEnabled(type.isEnableParentAssignments());
                        assignmentEngine.save(pge, true);
                    }
                }
            }

            // store structure options
            storeTypeOptions(con, TBL_STRUCT_TYPES_OPTIONS, "ID", newId, type.getOptions(), false);

            StructureLoader.reload(con);
        } catch (SQLException e) {
            if (StorageManager.isUniqueConstraintViolation(e)) {
                EJBUtils.rollback(ctx);
                throw new FxCreateException("ex.structure.type.exists", type.getName());
            }
            EJBUtils.rollback(ctx);
            throw new FxCreateException(LOG, e, "ex.db.sqlError", e.getMessage());
        } catch (FxCacheException e) {
            EJBUtils.rollback(ctx);
            throw new FxCreateException(LOG, e, "ex.cache", e.getMessage());
        } catch (FxLoadException e) {
            EJBUtils.rollback(ctx);
            throw new FxCreateException(e);
        } catch (FxNotFoundException e) {
            EJBUtils.rollback(ctx);
            throw new FxCreateException(e);
        } catch (FxEntryExistsException e) {
            EJBUtils.rollback(ctx);
            throw new FxCreateException(e);
        } catch (FxUpdateException e) {
            EJBUtils.rollback(ctx);
            throw new FxCreateException(e);
        } finally {
            Database.closeObjects(TypeEngineBean.class, con, ps);
        }
        return newId;
    }

    private FxPropertyAssignmentEdit updateAclAssignmentMultiplicity(FxTypeEdit type, FxPropertyAssignmentEdit derived) throws FxInvalidParameterException {
        if ("ACL".equals(derived.getProperty().getName())) {
            // set multiplicity based on the FxType#isMultipleContentACLs
            derived.setMultiplicity(
                    type.isMultipleContentACLs() ? FxMultiplicity.MULT_1_N : FxMultiplicity.MULT_1_1
            );
        }
        return derived;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long getInstanceCount(long typeId) {
        Connection con = null;
        long rc = -1;

        try {
            con = Database.getDbConnection();
            rc = getInstanceCount(con, typeId);
        } catch (SQLException e) {
            //noinspection ThrowableInstanceNeverThrown
            throw new FxApplicationException(LOG, e, "ex.db.sqlError", e.getMessage()).asRuntimeException();
        } finally {
            Database.closeObjects(TypeEngineBean.class, con, null);
        }
        return rc;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void flatten(String storage, long typeId) throws FxApplicationException {
        try {
            FxFlatStorageManager.getInstance().flattenType(storage, CacheAdmin.getEnvironment().getType(typeId));
        } catch (FxApplicationException e) {
            EJBUtils.rollback(ctx);
            throw e;
        }
        try {
            StructureLoader.reload(null);
        } catch (FxCacheException e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(e, "ex.cache", e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void flatten(long typeId) throws FxApplicationException {
        try {
            final FxFlatStorage flatStorage = FxFlatStorageManager.getInstance();
            flatStorage.flattenType(flatStorage.getDefaultStorage(), CacheAdmin.getEnvironment().getType(typeId));
        } catch (FxApplicationException e) {
            EJBUtils.rollback(ctx);
            throw e;
        }
        try {
            StructureLoader.reload(null);
        } catch (FxCacheException e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(e, "ex.cache", e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void unflatten(long typeId) throws FxApplicationException {
        try {
            final FxFlatStorage flatStorage = FxFlatStorageManager.getInstance();
            final FxType type = CacheAdmin.getEnvironment().getType(typeId);
            for (FxAssignment as : type.getAllAssignments()) {
                if (!(as instanceof FxPropertyAssignment))
                    continue;
                FxPropertyAssignment pa = (FxPropertyAssignment) as;
                if (pa.isFlatStorageEntry())
                    flatStorage.unflatten(pa);
            }
        } catch (FxApplicationException e) {
            EJBUtils.rollback(ctx);
            throw e;
        }
        try {
            StructureLoader.reload(null);
        } catch (FxCacheException e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(e, "ex.cache", e.getMessage());
        }
    }

    /**
     * Get the number of instances of a given type
     *
     * @param con    an open and valid Connection
     * @param typeId requested type
     * @return number of instances
     * @throws SQLException on errors
     */
    private long getInstanceCount(Connection con, long typeId) throws SQLException {
        PreparedStatement ps = null;
        long count = 0;
        try {
            ps = con.prepareStatement("SELECT COUNT(*) FROM " + TBL_CONTENT + " WHERE TDEF=?");
            ps.setLong(1, typeId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            count = rs.getLong(1);
        } finally {
            if (ps != null)
                ps.close();
        }
        return count;
    }


    /**
     * Get the instance count for relations
     *
     * @param con       an open and valid Connection
     * @param relTypeId type id of the relation type
     * @param srcTypeId type id of the source type
     * @param dstTypeId type id of the destination type
     * @return int array containing the number of relation instances, the number of source and the number of destination instances
     * @throws SQLException on errors
     */
    private int[] getRelationCount(Connection con, long relTypeId, long srcTypeId, long dstTypeId) throws SQLException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT COUNT(r.ID), COUNT(s.ID), COUNT(d.ID) FROM " + TBL_CONTENT + " r, " + TBL_CONTENT + " s, " + TBL_CONTENT + " d WHERE r.TDEF=? AND s.ID=r.RELSRC_ID AND d.ID=r.RELDST_ID AND s.TDEF=? AND d.TDEF=?");
            ps.setLong(1, relTypeId);
            ps.setLong(2, srcTypeId);
            ps.setLong(3, dstTypeId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return new int[]{rs.getInt(1), rs.getInt(2), rs.getInt(3)};
        } finally {
            if (ps != null)
                ps.close();
        }
    }

    /**
     * Remove all instances for the given relation,source,destination triple
     *
     * @param con       an open and valid Connection
     * @param relTypeId type id of the relation type
     * @param srcTypeId type id of the source type
     * @param dstTypeId type id of the destination type
     * @throws SQLException           on errors
     * @throws FxApplicationException on errors
     */
    private void removeRelationEntries(Connection con, long relTypeId, long srcTypeId, long dstTypeId) throws SQLException, FxApplicationException {
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT DISTINCT r.ID FROM " + TBL_CONTENT + " r, " + TBL_CONTENT + " s, " + TBL_CONTENT + " d WHERE r.TDEF=? AND s.ID=r.RELSRC_ID AND d.ID=r.RELDST_ID AND s.TDEF=? AND d.TDEF=?");
            ps.setLong(1, relTypeId);
            ps.setLong(2, srcTypeId);
            ps.setLong(3, dstTypeId);
            ResultSet rs = ps.executeQuery();
            while (rs != null && rs.next()) {
                contentEngine.remove(new FxPK(rs.getLong(1)));
            }
        } finally {
            if (ps != null)
                ps.close();
        }
    }

    /**
     * Update an existing type
     *
     * @param type the type to update
     * @return id of the type
     * @throws FxApplicationException on errors
     */
    private long update(FxTypeEdit type) throws FxApplicationException {
        if (!type.isChanged())
            return type.getId();

        final UserTicket ticket = FxContext.getUserTicket();
        FxPermissionUtils.checkRole(ticket, Role.StructureManagement);
        final FxEnvironment environment = CacheAdmin.getEnvironment();

        if (StringUtils.isEmpty(type.getName()))
            throw new FxInvalidParameterException("NAME", "ex.structure.update.nameMissing");

        //security checks start
        if (!ticket.mayEditACL(type.getACL().getId(), 0))
            throw new FxNoAccessException("ex.acl.noAccess.edit", type.getACL().getName());
        //security checks end

        boolean needReload = false; //full reload only needed if assignments have changed
        Connection con = null;
        PreparedStatement ps = null;
        FxType orgType = environment.getType(type.getId());

        StringBuilder sql = new StringBuilder(500);

        try {
            con = Database.getDbConnection();
            long instanceCount = -1; //number of instances
            //start name change
            if (!orgType.getName().equals(type.getName())) {
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET NAME=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setString(1, type.getName());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                //update all xpaths affected
                ps.close();
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_ASSIGNMENTS)
                        .append(" SET XPATH=REPLACE(XPATH, ?, ?) WHERE TYPEDEF=? AND ")
                        .append(StorageManager.getRegExpLikeOperator("XPATH", "?"));
                ps = con.prepareStatement(sql.toString());
                ps.setString(1, orgType.getName() + "/");
                ps.setString(2, type.getName() + "/");
                ps.setLong(3, type.getId());
                ps.setString(4, "^" + orgType.getName() + "/");
                int changed = ps.executeUpdate();
                if (changed > 0)
                    needReload = true;
                htracker.track(orgType, "history.type.update.name", orgType.getName(), type.getName(), type.getId(), changed);
            }
            //end name change

            //start description change
            if (!orgType.getLabel().equals(type.getLabel())) {
                Database.storeFxString(type.getLabel(), con, TBL_STRUCT_TYPES, "DESCRIPTION", "ID", type.getId());
                htracker.track(orgType, "history.type.update.description", orgType.getLabel(), type.getLabel());
            }
            //end description change

            //start type mode changes
            if (type.getMode().getId() != orgType.getMode().getId()) {
                if (instanceCount < 0)
                    instanceCount = getInstanceCount(con, type.getId());
                //allow relation => content (removing all relation specific entries) but content => relation requires 0 instances!
                if ((type.getMode() == TypeMode.Relation && orgType.getMode() == TypeMode.Content && instanceCount > 0) ||
                        orgType.getMode() == TypeMode.Preload || type.getMode() == TypeMode.Preload)
                    throw new FxUpdateException("ex.structure.type.typeMode.notUpgradeable", orgType.getMode(), type.getMode(), orgType.getName());
                if (type.getMode() == TypeMode.Content) {
                    if (type.getRelations().size() > 0) {
                        //TODO: remove relation mappings
                        throw new FxUpdateException("ex.notImplemented", "Remove all relation mappings for type");
                    }
                    if (instanceCount > 0) {
                        //TODO: remove all relation specific entries for existing contents
                        throw new FxUpdateException("ex.notImplemented", "Remove all relation specific entries for existing contents");
                    }
                }
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET TYPE_MODE=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setLong(1, type.getMode().getId());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.typeMode", orgType.getMode(), type.getMode());
            }
            //end type mode changes

            //start relation changes
            if (type.getAddedRelations().size() > 0) {
                sql.setLength(0);
                sql.append("INSERT INTO ").append(TBL_STRUCT_TYPERELATIONS).
                        append(" (TYPEDEF,TYPESRC,TYPEDST,MAXSRC,MAXDST)VALUES(?,?,?,?,?)");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setLong(1, type.getId());
                for (FxTypeRelation rel : type.getAddedRelations()) {
                    if (rel.getSource().isRelation())
                        throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", type.getName(), rel.getSource().getName());
                    if (rel.getDestination().isRelation())
                        throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", type.getName(), rel.getDestination().getName());
                    ps.setLong(2, rel.getSource().getId());
                    ps.setLong(3, rel.getDestination().getId());
                    ps.setLong(4, rel.getMaxSource());
                    ps.setLong(5, rel.getMaxDestination());
                    ps.addBatch();
                    htracker.track(type, "history.type.update.relation.add", type.getName(), rel.getSource().getName(),
                            rel.getMaxSource(), rel.getDestination().getName(), rel.getMaxDestination());
                }
                ps.executeBatch();
            }
            if (type.getUpdatedRelations().size() > 0) {
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPERELATIONS).
                        //                  1        2               3             4             5
                                append(" SET MAXSRC=?,MAXDST=? WHERE TYPEDEF=? AND TYPESRC=? AND TYPEDST=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setLong(3, type.getId());
                for (FxTypeRelation rel : type.getUpdatedRelations()) {
                    if (rel.getSource().isRelation())
                        throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", type.getName(), rel.getSource().getName());
                    if (rel.getDestination().isRelation())
                        throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", type.getName(), rel.getDestination().getName());
                    //TODO: check if maxSource/maxDestination is not violated if > 0
                    ps.setLong(4, rel.getSource().getId());
                    ps.setLong(5, rel.getDestination().getId());
                    ps.setLong(1, rel.getMaxSource());
                    ps.setLong(2, rel.getMaxDestination());
                    ps.addBatch();
                    htracker.track(type, "history.type.update.relation.update", type.getName(), rel.getSource().getName(),
                            rel.getMaxSource(), rel.getDestination().getName(), rel.getMaxDestination());
                }
                ps.executeBatch();
            }
            if (type.getRemovedRelations().size() > 0) {
                sql.setLength(0);
                sql.append("DELETE FROM ").append(TBL_STRUCT_TYPERELATIONS).
                        //                     1            2              3
                                append(" WHERE TYPEDEF=? AND TYPESRC=? AND TYPEDST=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setLong(1, type.getId());
                for (FxTypeRelation rel : type.getRemovedRelations()) {
                    if (rel.getSource().isRelation())
                        throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", type.getName(), rel.getSource().getName());
                    if (rel.getDestination().isRelation())
                        throw new FxInvalidParameterException("ex.structure.type.relation.wrongTarget", type.getName(), rel.getDestination().getName());
                    int[] rels = getRelationCount(con, type.getId(), rel.getSource().getId(), rel.getDestination().getId());
                    if (!type.isRemoveInstancesWithRelationTypes() && rels[0] > 0)
                        throw new FxRemoveException("ex.structure.type.relation.relationsExist", type.getName(), rel.getSource().getName(), rel.getDestination().getName(), rels[0]);
                    else if (type.isRemoveInstancesWithRelationTypes() && rels[0] > 0) {
                        removeRelationEntries(con, type.getId(), rel.getSource().getId(), rel.getDestination().getId());
                    }
                    ps.setLong(2, rel.getSource().getId());
                    ps.setLong(3, rel.getDestination().getId());
                    ps.addBatch();
                    htracker.track(type, "history.type.update.relation.remove", type.getName(), rel.getSource().getName(),
                            rel.getMaxSource(), rel.getDestination().getName(), rel.getMaxDestination());
                }
                ps.executeBatch();
            }
            //end relation changes

            //start ACL changes
            if (!type.getACL().equals(orgType.getACL())) {
                if (type.getACL().getCategory() != ACLCategory.STRUCTURE)
                    throw new FxInvalidParameterException("ACL", "ex.acl.category.invalid", type.getACL().getCategory(), ACLCategory.STRUCTURE);
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET ACL=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setLong(1, type.getACL().getId());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.acl", orgType.getACL(), type.getACL());
            }
            //end ACL changes

            //start Workflow changes
            if (!type.getWorkflow().equals(orgType.getWorkflow())) {

                if (instanceCount < 0)
                    instanceCount = getInstanceCount(con, type.getId());
                if (instanceCount > 0) {
                    //Workflow can not be changed with existing instances -> there is no way to reliably
                    //map steps of one workflow to steps of another (even not using stepdefinitions since they
                    //can be used multiple times). A possible solution would be to provide a mapping when changing
                    //workflows but this should be to seldom the case to bother implementing it
                    throw new FxUpdateException("ex.notImplemented", "Workflow changes with existing instance");
                }
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET WORKFLOW=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setLong(1, type.getWorkflow().getId());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.workflow", orgType.getWorkflow(), type.getWorkflow());
            }
            //end Workflow changes

            //start Category changes
            if (!type.getCategory().equals(orgType.getCategory())) {
                if (!ticket.isGlobalSupervisor())
                    throw new FxUpdateException("ex.structure.type.category.notSupervisor", orgType.getCategory(), type.getCategory(), orgType.getName());
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET CATEGORY=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setInt(1, type.getCategory().getId());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
            }
            //end Category changes

            //start language mode changes
            if (!type.getLanguage().equals(orgType.getLanguage())) {
                if (instanceCount < 0)
                    instanceCount = getInstanceCount(con, type.getId());
                if (instanceCount <= 0 || orgType.getLanguage().isUpgradeable(type.getLanguage())) {
                    sql.setLength(0);
                    sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET LANG_MODE=? WHERE ID=?");
                    if (ps != null) ps.close();
                    ps = con.prepareStatement(sql.toString());
                    ps.setInt(1, type.getLanguage().getId());
                    ps.setLong(2, type.getId());
                    ps.executeUpdate();
                    htracker.track(type, "history.type.update.languageMode", orgType.getLanguage().name(), type.getLanguage().name());
                } else
                    throw new FxUpdateException("ex.structure.type.languageMode.notUpgradeable", orgType.getLanguage(), type.getLanguage(), orgType.getName());
            }
            //end language mode changes

            //start state changes
            if (type.getState().getId() != orgType.getState().getId()) {
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET TYPE_STATE=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setInt(1, type.getState().getId());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.state", orgType.getState().name(), type.getState().name());
            }
            //end state changes

            //start permission changes
            if (type.getBitCodedPermissions() != orgType.getBitCodedPermissions()) {
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET SECURITY_MODE=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setInt(1, type.getBitCodedPermissions());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.perm", FxPermissionUtils.toString(orgType.getBitCodedPermissions()), FxPermissionUtils.toString(type.getBitCodedPermissions()));
            }
            //end permission changes

            //start multiple ACL setting changes
            if (type.isMultipleContentACLs() != orgType.isMultipleContentACLs()) {
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET MULTIPLE_CONTENT_ACLS=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setBoolean(1, type.isMultipleContentACLs());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.multipleContentACLs", orgType.isMultipleContentACLs(), type.isMultipleContentACLs());
            }
            //end multiple ACL setting changes

            //start isIncludedInSupertypeQueries setting changes
            if (type.isIncludedInSupertypeQueries() != orgType.isIncludedInSupertypeQueries()) {
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET INSUPERTYPEQUERY=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setBoolean(1, type.isIncludedInSupertypeQueries());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.inSupertypeQueries", orgType.isIncludedInSupertypeQueries(), type.isIncludedInSupertypeQueries());
            }
            //end isIncludedInSupertypeQueries setting changes

            //start history track/age changes
            if (type.isTrackHistory() != orgType.isTrackHistory() ||
                    type.getHistoryAge() != orgType.getHistoryAge()) {
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET TRACKHISTORY=?, HISTORY_AGE=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setBoolean(1, type.isTrackHistory());
                ps.setLong(2, type.getHistoryAge());
                ps.setLong(3, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.history", orgType.isTrackHistory(), type.isTrackHistory(), orgType.getHistoryAge(), type.getHistoryAge());
            }
            //end history track/age changes

            //start max.ver changes
            if (type.getMaxVersions() != orgType.getMaxVersions()) {
                //TODO: remove any versions that would exceed this count
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET MAX_VERSIONS=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setLong(1, type.getMaxVersions());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.maxVersions", orgType.getMaxVersions(), type.getMaxVersions());
            }
            //end max.ver changes

            //start max source relations changes
            if (type.isRelation() && type.getMaxRelSource() != orgType.getMaxRelSource()) {
                //TODO: check if the new condition is not violated by existing data
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET REL_TOTAL_MAXSRC=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setLong(1, type.getMaxRelSource());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.maxRelSource", orgType.getMaxRelSource(), type.getMaxRelSource());
            }
            //end max source relations changes

            //start max destination relations changes
            if (type.isRelation() && type.getMaxRelDestination() != orgType.getMaxRelDestination()) {
                //TODO: check if the new condition is not violated by existing data
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET REL_TOTAL_MAXDST=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                ps.setLong(1, type.getMaxRelDestination());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                htracker.track(type, "history.type.update.maxRelDest", orgType.getMaxRelDestination(), type.getMaxRelDestination());
            }
            //end max destination relations changes

            //start icon
            if (!type.getIcon().equals(orgType.getIcon())) {
                sql.setLength(0);
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(" SET ICON_REF=? WHERE ID=?");
                if (ps != null) ps.close();
                ps = con.prepareStatement(sql.toString());
                if (type.getIcon().isEmpty())
                    ps.setNull(1, java.sql.Types.INTEGER);
                else
                    ps.setLong(1, type.getIcon().getDefaultTranslation().getId());
                ps.setLong(2, type.getId());
                ps.executeUpdate();
                needReload = true;
                htracker.track(type, "history.type.update.icon", orgType.getIcon().getDefaultTranslation().getId(),
                        type.getIcon().getDefaultTranslation().getId());
            }
            //end icon

            // structure option changes
            boolean optionsChanged = updateTypeOptions(con, type, orgType);
            // check if any type options must be propagated to derived types
            if(type.getDerivedTypes().size() > 0) {
                final List<FxTypeOption> inherit = new ArrayList<FxTypeOption>(type.getOptions().size());
                for(FxTypeOption o : type.getOptions()) {
                    if(o.isInherited()) {
                        inherit.add(o);
                    }
                }
                if(inherit.size() > 0) {
                    for(FxType derived : type.getDerivedTypes()) {
                        updateDerivedTypeOptions(con, derived, inherit);
                    }
                }
            }

            //sync back to cache
            try {
                if (needReload)
                    StructureLoader.reload(con);
                else {
                    StructureLoader.updateType(FxContext.get().getDivisionId(), loadType(con, type.getId()));
                    // reload any derived types if type options have changed
                    if (optionsChanged && type.getDerivedTypes().size() > 0) {
                        for (FxType derivedType : type.getDerivedTypes()) {
                            StructureLoader.updateType(FxContext.get().getDivisionId(), loadType(con, derivedType.getId()));
                        }
                    }
                }
            } catch (FxLoadException e) {
                throw new FxUpdateException(e);
            } catch (FxCacheException e) {
                LOG.fatal(e.getMessage(), e);
            }
        } catch (SQLException e) {
            EJBUtils.rollback(ctx);
            throw new FxUpdateException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(TypeEngineBean.class, con, ps);
        }
        return type.getId();
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(long id) throws FxApplicationException {
        final UserTicket ticket = FxContext.getUserTicket();
        FxPermissionUtils.checkRole(ticket, Role.StructureManagement);

        FxType type = CacheAdmin.getEnvironment().getType(id);

        Connection con = null;
        PreparedStatement ps = null;
        StringBuilder sql = new StringBuilder(500);
        try {
            con = Database.getDbConnection();
            List<FxPropertyAssignment> allPropertyAssignments = new ArrayList<FxPropertyAssignment>(20);
            FxEnvironment env = CacheAdmin.getEnvironment();
            for (FxPropertyAssignment fxpa : env.getPropertyAssignments(true))
                if (fxpa.getAssignedType().getId() == id)
                    allPropertyAssignments.add(fxpa);
            List<Long> rmStackProp = new ArrayList<Long>(allPropertyAssignments.size());
            List<FxPropertyAssignment> rmProp = new ArrayList<FxPropertyAssignment>(allPropertyAssignments.size());

            for (FxPropertyAssignment a : allPropertyAssignments)
                if (a.getBaseAssignmentId() == FxAssignment.NO_PARENT)
                    rmStackProp.add(a.getId());
                else {
                    //check if base is from the same type
                    if (env.getAssignment(a.getBaseAssignmentId()).getAssignedType().getId() == id)
                        rmProp.add(a);
                    else
                        rmStackProp.add(a.getId());
                }
            boolean found;
            while (rmProp.size() > 0) {
                found = false;
                for (FxPropertyAssignment a : rmProp)
                    if (rmStackProp.contains(a.getBaseAssignmentId())) {
                        rmProp.remove(a);
                        rmStackProp.add(0, a.getId());
                        found = true;
                        break;
                    }
                assert found : "Internal error: no property assignment found to be removed!";
            }
            //remove group assignments in the 'correct' order (ie not violating parentgroup references)
            ArrayList<Long> rmStack = new ArrayList<Long>(10);
            buildGroupAssignmentRemoveStack(type.getConnectedAssignments("/"), rmStack);
            rmStack.addAll(0, rmStackProp);

            sql.setLength(0);
            sql.append("DELETE FROM ").append(TBL_STRUCT_ASSIGNMENTS).append(ML).append(" WHERE ID=?");
            ps = con.prepareStatement(sql.toString());
            for (Long rmid : rmStack) {
                ps.setLong(1, rmid);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();

            //prevent base-related constraint issues by setting the base to null prior to removal
            sql.setLength(0);
            sql.append("UPDATE ").append(TBL_STRUCT_ASSIGNMENTS).append(" SET BASE=NULL WHERE TYPEDEF=? AND ID=?");
            ps = con.prepareStatement(sql.toString());
            ps.setLong(1, type.getId());
            for (Long rmid : rmStack) {
                ps.setLong(2, rmid);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();

            //remove property and group assignment option entries
            sql.setLength(0);
            for (FxPropertyAssignment pa : allPropertyAssignments) {
                if ( //exclude the "ID" property whose Id is "0" which is "NO_PARENT"
                        !(pa.getProperty().getId() == FxAssignment.NO_PARENT)) {
                    if (sql.length() == 0) {
                        sql.append(" WHERE ASSID IN(").append(pa.getId());
                    } else
                        sql.append(',').append(pa.getId());
                }
            }
            if (sql.length() > 0) {
                sql.append(')');
                ps = con.prepareStatement("DELETE FROM " + TBL_STRUCT_PROPERTY_OPTIONS + sql.toString());
                ps.executeUpdate();
                ps.close();
            }
            sql.setLength(0);
            for (FxGroupAssignment ga : type.getAssignedGroups()) {
                if (ga.getBaseAssignmentId() == FxAssignment.NO_PARENT) {
                    if (sql.length() == 0) {
                        sql.append(" WHERE ASSID IN(").append(ga.getId());
                    } else
                        sql.append(',').append(ga.getId());
                }
            }
            if (sql.length() > 0) {
                sql.append(')');
                ps = con.prepareStatement("DELETE FROM " + TBL_STRUCT_GROUP_OPTIONS + sql.toString());
                ps.executeUpdate();
                ps.close();
            }

            // remove all type structure options
            storeTypeOptions(con, TBL_STRUCT_TYPES_OPTIONS, "ID", id, null, true);

            //remove all flat storage assignments for this type
            FxFlatStorageManager.getInstance().removeTypeMappings(con, type.getId());

            //remove the assignments
            sql.setLength(0);
            //clear parent key refs for removal to avoid referential integrity issues within the type itself
//            sql.append("UPDATE ").append(TBL_STRUCT_ASSIGNMENTS).append(" SET PARENTGROUP=ID WHERE TYPEDEF=?");
//            ps = con.prepareStatement(sql.toString());
//            ps.setLong(1, type.getId());
//            ps.executeUpdate();
//            ps.close();
//            sql.setLength(0);
//            ps = con.prepareStatement(sql.toString());
            sql.append("DELETE FROM ").append(TBL_STRUCT_ASSIGNMENTS).append(" WHERE TYPEDEF=? AND ID=?");
            ps = con.prepareStatement(sql.toString());
            ps.setLong(1, type.getId());
            for (Long rmid : rmStack) {
                ps.setLong(2, rmid);
                ps.addBatch();
            }
            ps.executeBatch();
            ps.close();

            sql.setLength(0);
            sql.append("DELETE FROM ").append(TBL_STRUCT_TYPERELATIONS).append(" WHERE TYPEDEF=? OR TYPESRC=? OR TYPEDST=?");
            ps = con.prepareStatement(sql.toString());
            ps.setLong(1, type.getId());
            ps.setLong(2, type.getId());
            ps.setLong(3, type.getId());
            ps.executeUpdate();
            ps.close();
            sql.setLength(0);
            sql.append("DELETE FROM ").append(TBL_STRUCT_TYPES).append(ML).append(" WHERE ID=?");
            ps = con.prepareStatement(sql.toString());
            ps.setLong(1, type.getId());
            ps.executeUpdate();
            ps.close();
             sql.setLength(0);
            sql.append("UPDATE ").append(TBL_STRUCT_PROPERTIES).append(" SET REFTYPE=NULL WHERE REFTYPE=?");
            ps = con.prepareStatement(sql.toString());
            ps.setLong(1, type.getId());
            ps.executeUpdate();
            ps.close();
            sql.setLength(0);
            sql.append("DELETE FROM ").append(TBL_STRUCT_TYPES).append(" WHERE ID=?");
            ps = con.prepareStatement(sql.toString());
            ps.setLong(1, type.getId());
            ps.executeUpdate();

            //remove eventually orphaned properties and groups
            FxStructureUtils.removeOrphanedProperties(con);
            FxStructureUtils.removeOrphanedGroups(con);

            StructureLoader.reload(con);
            htracker.track(type, "history.type.remove", type.getName(), type.getId());
        } catch (SQLException e) {
            if (StorageManager.isForeignKeyViolation(e)) {
                EJBUtils.rollback(ctx);
                throw new FxRemoveException(LOG, e, "ex.structure.type.inUse", type.getName());
            }
            EJBUtils.rollback(ctx);
            throw new FxRemoveException(LOG, e, "ex.db.sqlError", e.getMessage());
        } catch (FxCacheException e) {
            EJBUtils.rollback(ctx);
            throw new FxRemoveException(LOG, e, "ex.cache", e.getMessage());
        } catch (FxLoadException e) {
            EJBUtils.rollback(ctx);
            throw new FxRemoveException(e);
        } finally {
            Database.closeObjects(TypeEngineBean.class, con, ps);
        }
    }

    /**
     * Build recursively a stack of group assignment id's to enable removal without violating parentgroup dependencies
     * Base dependencies can not be resolved using this method! Set them to <code>null</code> before removing!
     *
     * @param connectedAssignments list of assignments to inspect
     * @param rmStack              List being built by this method containing id's in the correct removal order
     */
    private void buildGroupAssignmentRemoveStack(List connectedAssignments, ArrayList<Long> rmStack) {
        assert rmStack != null : "Called with no rmStack";
        for (Object a : connectedAssignments) {
            if (a instanceof FxGroupAssignment) {
                rmStack.add(0, ((FxGroupAssignment) a).getId());
                buildGroupAssignmentRemoveStack(((FxGroupAssignment) a).getAssignedGroups(), rmStack);
            }
        }
    }

    /**
     * Load a type from the given (and open!) connection
     *
     * @param con an open and valid connection
     * @param id  if of the type to load
     * @return FxType
     * @throws FxLoadException     on errors
     * @throws FxNotFoundException on errors
     */
    private FxType loadType(Connection con, long id) throws FxLoadException, FxNotFoundException {
        Statement stmt = null;
        PreparedStatement ps = null;
        String curSql;
        FxEnvironment environment = CacheAdmin.getEnvironment();
        try {
            // load structure options for a given type
            final List<FxTypeOption> options = loadTypeOptions(con, id, "ID", TBL_STRUCT_TYPES_OPTIONS);

            //                                 1         2       3        4
            ps = con.prepareStatement("SELECT TYPESRC, TYPEDST, MAXSRC, MAXDST FROM " + TBL_STRUCT_TYPERELATIONS + " WHERE TYPEDEF=?");
            //               1   2     3       4             5         6
            curSql = "SELECT ID, NAME, PARENT, STORAGE_MODE, CATEGORY, TYPE_MODE, " +
                    //7         8           9              10            11           12
                    "LANG_MODE, TYPE_STATE, SECURITY_MODE, TRACKHISTORY, HISTORY_AGE, MAX_VERSIONS," +
                    //13               14                15          16          17           18           19   20
                    "REL_TOTAL_MAXSRC, REL_TOTAL_MAXDST, CREATED_BY, CREATED_AT, MODIFIED_BY, MODIFIED_AT, ACL, WORKFLOW, " +
                    // 21                   22
                    "MULTIPLE_CONTENT_ACLS, INSUPERTYPEQUERY" +
                    " FROM " + TBL_STRUCT_TYPES + " WHERE ID=" + id;

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(curSql);
            ResultSet rsRelations;
            if (rs != null && rs.next()) {
                try {
                    ps.setLong(1, rs.getLong(1));
                    List<FxTypeRelation> alRelations = new ArrayList<FxTypeRelation>(10);
                    rsRelations = ps.executeQuery();

                    while (rsRelations != null && rsRelations.next())
                        alRelations.add(new FxTypeRelation(new FxPreloadType(rsRelations.getLong(1)), new FxPreloadType(rsRelations.getLong(2)),
                                rsRelations.getInt(3), rsRelations.getInt(4)));

                    return new FxType(rs.getLong(1), environment.getACL(rs.getInt(19)),
                            environment.getWorkflow(rs.getInt(20)), rs.getString(2),
                            Database.loadFxString(con, TBL_STRUCT_TYPES, "description", "id=" + rs.getLong(1)),
                            new FxPreloadType(rs.getLong(3)), TypeStorageMode.getById(rs.getInt(4)),
                            TypeCategory.getById(rs.getInt(5)), TypeMode.getById(rs.getInt(6)),
                            LanguageMode.getById(rs.getInt(7)), TypeState.getById(rs.getInt(8)), rs.getByte(9),
                            rs.getBoolean(21), rs.getBoolean(22), rs.getBoolean(10), rs.getLong(11), rs.getLong(12),
                            rs.getInt(13), rs.getInt(14), LifeCycleInfoImpl.load(rs, 15, 16, 17, 18),
                            new ArrayList<FxType>(5), alRelations, options);

                } catch (FxNotFoundException e) {
                    throw new FxLoadException(LOG, e);
                }
            }
            throw new FxNotFoundException(LOG, "FxType with id " + id + " does not exist!");
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "Failed to load FxType: " + exc.getMessage(), exc);
        } finally {
            try {
                if (ps != null)
                    ps.close();
            } catch (SQLException e) {
                //ignore
            }
            Database.closeObjects(TypeEngineBean.class, null, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public String export(long id) throws FxApplicationException {
        return ConversionEngine.getXStream().toXML(CacheAdmin.getEnvironment().getType(id));
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxType importType(String typeXML) throws FxApplicationException {
        try {
            return (FxType) ConversionEngine.getXStream().fromXML(typeXML);
        } catch (ConversionException e) {
            String key;
            Iterator i = e.keys();
            String path = "unknown";
            String line = "unknown";
            while (i.hasNext()) {
                key = (String) i.next();
                if ("path".equals(key))
                    path = e.get(key);
                else if ("line number".equals(key))
                    line = e.get(key);
            }
            throw new FxApplicationException(e.getCause(), "ex.structure.import.type.conversionError", path, line, e.getShortMessage());
        } catch (Exception e) {
            throw new FxApplicationException(e, "ex.structure.import.type.error", e.getMessage());
        }
    }

    /*
     * Helper to store options, (the information in brackets expalains how to use this method to store the options
     * for an FxPropertyAssignment)
     *
     * @param con               the DB connection
     * @param table             the table name to store the options (e.g. TBL_PROPERTY_OPTIONS)
     * @param primaryColumn     the column name of the primary key (where the property Id is stored, e.g. ID)
     * @param primaryId         the primary key itself (the property Id, e.g. FxPropertyAssignment.getProperty().getId())
     * @param options           the option list to store (e.g. FxPropertyAssignmentEdit.getOptions())
     * @param update set to true if an update should be performed
     */
    private void storeTypeOptions(Connection con, String table, String primaryColumn, long id, // Long assignmentId,
                              List<FxTypeOption> options, boolean update) throws SQLException, FxInvalidParameterException {
        PreparedStatement ps = null;
        try {
            if(update) {
                ps = con.prepareStatement("DELETE FROM " + table + " WHERE " + primaryColumn + "=?");
                ps.setLong(1, id);
                ps.executeUpdate();
                ps.close();
            }

            if (options == null || options.size() == 0)
                return;
            //                                                        1                 2      3           4        5
            ps = con.prepareStatement("INSERT INTO " + table + " (" + primaryColumn + ",OPTKEY,MAYOVERRIDE,ISINHERITED,OPTVALUE)VALUES(?,?,?,?,?)");
            for (FxTypeOption option : options) {
                ps.setLong(1, id);
                if (StringUtils.isEmpty(option.getKey()))
                    throw new FxInvalidParameterException("key", "ex.structure.option.key.empty", option.getValue());
                ps.setString(2, option.getKey());
                ps.setBoolean(3, option.isOverrideable());
                ps.setBoolean(4, option.isInherited());
                ps.setString(5, option.getValue());
                ps.addBatch();
            }
            ps.executeBatch();
        } finally {
            if (ps != null)
                ps.close();
        }
    }

    /**
     * Updates the options of an FxGroup
     * Before the options are updated, they are compared against the options that are
     * already stored in the DB. If there are changes, all present options are deleted
     * in the DB and newly created afterwards from the assignment's option list.
     *
     * @param con a valid and open connection
     * @param type the type whose structure options (might) have changed
     * @param orig the original type
     * @return true if changes were made
     * @throws SQLException on db errors
     * @throws FxInvalidParameterException on parameter errors
     */
    private boolean updateTypeOptions(Connection con, FxTypeEdit type, FxType orig) throws SQLException, FxInvalidParameterException {
        boolean changed = false;
        if (orig.getOptions().size() != type.getOptions().size()) {
            changed = true;
        } else {
            for (int i = 0; i < orig.getOptions().size(); i++) {
                FxTypeOption origOpt = orig.getOptions().get(i);
                FxTypeOption newOpt = type.getOption(origOpt.getKey());
                if (!origOpt.equals(newOpt)) {
                    changed = true;
                    break;
                }
            }
        }
        if (changed)
            storeTypeOptions(con, TBL_STRUCT_TYPES_OPTIONS, "ID", type.getId(), type.getOptions(), true);
        return changed;
    }

    /**
     * Update the options of any derived types if their supertypes acquire new options which must be passed on
     * ONLY do this iff the options is not already set
     * 
     * @param con an open and valid connection
     * @param derived the derived type t.b. updated
     * @param inheritedOpts the options of the source type t.b. inherited by the derived type
     * @return true if any changes had to be made
     * @throws SQLException on errors
     * @throws FxInvalidParameterException on errors
     */
    private boolean updateDerivedTypeOptions(Connection con, FxType derived, List<FxTypeOption> inheritedOpts)
        throws SQLException, FxInvalidParameterException {
        boolean changed = false;
        final List<FxTypeOption> current = derived.getOptions();
        final List<FxTypeOption> newOpts = new ArrayList<FxTypeOption>(inheritedOpts.size());
        for(FxTypeOption o : inheritedOpts) {
            if(!FxTypeOption.hasOption(o.getKey(), current))
                newOpts.add(o);
        }

        if(newOpts.size() > 0) {
            storeTypeOptions(con, TBL_STRUCT_TYPES_OPTIONS, "ID", derived.getId(), newOpts, false);
            changed = true;
        }

        return changed;
    }

    /**
     * Load the structure options for a given type
     *
     * @param con an open and valid connection
     * @param typeId the type's id
     * @param idColumn the name of the id column
     * @param table the table name
     * @return structure options
     * @throws SQLException on errors
     */
    private List<FxTypeOption> loadTypeOptions(Connection con, long typeId, String idColumn, String table) throws SQLException {
        PreparedStatement ps = null;
        List<FxTypeOption> result = new ArrayList<FxTypeOption>(4);
        try {
            //                                1      2           3        4
            ps = con.prepareStatement("SELECT OPTKEY,MAYOVERRIDE,ISINHERITED,OPTVALUE FROM " + table + " WHERE " + idColumn + "=?");
            ps.setLong(1, typeId);
            final ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                FxTypeOption.setOption(result, rs.getString(1), rs.getBoolean(2), rs.getBoolean(3), rs.getString(4));
            }
            ps.close();
            return result;
        } finally {
            if(ps != null)
                ps.close();
        }
    }
}
