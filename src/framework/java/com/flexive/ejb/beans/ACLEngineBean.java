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
import com.flexive.core.LifeCycleInfoImpl;
import static com.flexive.core.DatabaseConst.*;
import com.flexive.core.security.UserTicketStore;
import com.flexive.core.structure.StructureLoader;
import com.flexive.shared.*;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.security.*;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;


/**
 * Bean handling ACLs.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "ACLEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ACLEngineBean implements ACLEngine, ACLEngineLocal {

    private static transient Log LOG = LogFactory.getLog(ACLEngineBean.class);
    @Resource
    javax.ejb.SessionContext ctx;
    @EJB
    AccountEngineLocal account;
    @EJB
    UserGroupEngineLocal group;
    @EJB
    MandatorEngineLocal mandator;
    @EJB
    SequencerEngineLocal seq;


    public ACLEngineBean() {
    }

    /**
     * Helper function.
     *
     * @param description the description to check
     * @return the description
     * @throws FxInvalidParameterException if the description id invalid
     */
    private String checkDescription(String description) throws FxInvalidParameterException {
        if (description == null) description = "";
        if (description.length() > 255)
            throw new FxInvalidParameterException("DESCRIPTION", "ex.acl.propertyDescriptionTooLong");
        return description;
    }

    /**
     * Helper function.
     *
     * @param name the name to check
     * @return the name
     * @throws FxInvalidParameterException if the name id invalid
     */
    private String checkName(String name) throws FxInvalidParameterException {
        if (StringUtils.isEmpty(name))
            throw new FxInvalidParameterException("NAME", "ex.acl.propertyNameMissing");
        if (name.length() > 250)
            throw new FxInvalidParameterException("NAME", "ex.acl.propertyNameTooLong");
        return name;
    }

    /**
     * Helper function.
     *
     * @param label the label to check
     * @return the label
     * @throws FxInvalidParameterException if the label id invalid
     */
    private FxString checkLabel(FxString label) throws FxInvalidParameterException {
        if (label == null || label.isEmpty())
            throw new FxInvalidParameterException("LABEL", "ex.acl.propertyLabelMissing");
        return label;
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public long create(String name, FxString label, long mandatorId, String color, String description, ACL.Category category)
            throws FxApplicationException {

        final UserTicket ticket = FxContext.get().getTicket();
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        // Security
        if (!ticket.isInRole(Role.MandatorSupervisor))
            FxPermissionUtils.checkRole(ticket, Role.ACLManagement);
        if (!ticket.isGlobalSupervisor()) {
            if (ticket.getMandatorId() != mandatorId) {
                String mandatorName = environment.getMandator(mandatorId).getName();
                FxNoAccessException na = new FxNoAccessException("ex.acl.mayNotCreateForForeignMandator", mandatorName);
                if (LOG.isInfoEnabled()) LOG.info(na, na);
                throw na;
            }
        }

        // Parameter checks
        color = FxFormatUtils.processColorString("COLOR", color);
        description = checkDescription(description);
        name = checkName(name);
        label = checkLabel(label);
        if (category == null)
            throw new FxInvalidParameterException("CATEGORY", "ex.acl.categoryMissing");

        Connection con = null;
        PreparedStatement ps = null;
        String sql;

        try {

            // Obtain a database connection
            con = Database.getDbConnection();

            // Obtain a new id
            int newId = (int) seq.getId(SequencerEngine.System.ACL);
            sql = "INSERT INTO " + TBL_ACLS + "(" +
                    //       1      2     3          4              5       6        7         8              9
                    "ID,CAT_TYPE,COLOR,DESCRIPTION,MANDATOR,NAME,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT)" +
                    "VALUES (?,?,?,?,?,?,?,?,?,?)";
            final long NOW = System.currentTimeMillis();
            ps = con.prepareStatement(sql);
            ps.setInt(1, newId);
            ps.setInt(2, category.getId());
            ps.setString(3, color);
            ps.setString(4, description);
            ps.setLong(5, mandatorId);
            ps.setString(6, name);
            ps.setLong(7, ticket.getUserId());
            ps.setLong(8, NOW);
            ps.setLong(9, ticket.getUserId());
            ps.setLong(10, NOW);
            ps.executeUpdate();

            Database.storeFxString(label, con, TBL_ACLS, "LABEL", "ID", newId);
            try {
                StructureLoader.updateACL(FxContext.get().getDivisionId(), this.load(newId));
            } catch (FxLoadException e) {
                LOG.error(e, e);
            } catch (FxNotFoundException e) {
                LOG.error(e, e);
            }
            return newId;
        } catch (SQLException exc) {
            final boolean uniqueConstraintViolation = Database.isUniqueConstraintViolation(exc);
            ctx.setRollbackOnly();
            if (uniqueConstraintViolation) {
                throw new FxEntryExistsException("ex.acl.aclAlreadyExists", name);
            } else {
                throw new FxCreateException(LOG, exc, "ex.acl.createFailed", exc.getMessage());
            }
        } finally {
            Database.closeObjects(ACLEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(long id)
            throws FxApplicationException {
        UserTicket ticket = FxContext.get().getTicket();
        ACL theACL = load(id);

        // Protected interal ACLs
        if (theACL.getId() <= ACL.MAX_INTERNAL_ID)
            throw new FxNoAccessException(LOG, "ex.acl.deleteFailed.internalACL", theACL.getName());

        // Security
        if (!ticket.isGlobalSupervisor()) {
            // Security
            if (!ticket.isInRole(Role.MandatorSupervisor) && !ticket.isInRole(Role.ACLManagement))
                throw new FxNoAccessException(LOG, "ex.acl.deleteFailed.noPermission", theACL.getName());
            if (ticket.getMandatorId() != theACL.getMandatorId())
                throw new FxNoAccessException(LOG, "ex.acl.deleteFailed.foreignMandator", theACL.getName());
        }

        Connection con = null;
        PreparedStatement stmt = null;
        String curSql;

        try {
            // Obtain a database connection
            con = Database.getDbConnection();

            // Delete all ACLAssignments
            curSql = "DELETE FROM " + TBL_ASSIGN_ACLS + " WHERE ACL=" + id;
            stmt = con.prepareStatement(curSql);
            stmt.executeUpdate();
            stmt.close();

            // Delete the label
            curSql = "DELETE FROM " + TBL_ACLS + ML + " WHERE ID=" + id;
            stmt = con.prepareStatement(curSql);
            stmt.executeUpdate();
            stmt.close();

            // Delete the ACL itself
            curSql = "DELETE FROM " + TBL_ACLS + " WHERE ID=" + id;
            stmt = con.prepareStatement(curSql);
            stmt.executeUpdate();

            // Refresh all UserTicket that are affected
            UserTicketStore.flagDirtyHavingACL(id);
            StructureLoader.removeACL(FxContext.get().getDivisionId(), id);
        } catch (SQLException exc) {
            final boolean uniqueConstraintViolation = Database.isUniqueConstraintViolation(exc);
            final boolean keyViolation = Database.isForeignKeyViolation(exc);
            ctx.setRollbackOnly();
            if (uniqueConstraintViolation)
                throw new FxRemoveException(LOG, exc, "ex.acl.aclAlreadyExists", theACL.getName());
            else if (keyViolation)
                throw new FxRemoveException(LOG, exc, "ex.acl.deleteFailed.aclIsInUse", theACL.getName());
            else
                throw new FxRemoveException(LOG, exc, "ex.acl.deleteFailed", theACL.getName(), exc.getMessage());
        } finally {
            Database.closeObjects(ACLEngineBean.class, con, stmt);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void update(long id, String name, FxString label, String color, String description,
                       List<ACLAssignment> assignments)
            throws FxApplicationException {
        UserTicket ticket = FxContext.get().getTicket();
        // Load the current version of the ACL
        ACL theACL = load(id);

        // Check the assignments array: Every group may only be contained once
        if (assignments == null) {
            assignments = new ArrayList<ACLAssignment>(0);
        } else if (assignments.size() > 0) {
            Hashtable<Long, Boolean> uniqueCheck = new Hashtable<Long, Boolean>(assignments.size());
            for (ACLAssignment ass : assignments) {
                // Group defined more than once in the list?
                if (uniqueCheck.put(ass.getGroupId(), Boolean.TRUE) != null) {
                    String groupName = "unknown";
                    try {
                        groupName = group.load(ass.getGroupId()).getName();
                    } catch (Throwable t) {
                        //ignore
                    }
                    throw new FxUpdateException("ex.aclAssignment.update.groupDefinedMoreThanOnce",
                            theACL.getName(), groupName);
                }
            }
        }

        // Security
        if (!ticket.isGlobalSupervisor()) {
            // Security
            if (!ticket.isInRole(Role.MandatorSupervisor) && !ticket.isInRole(Role.ACLManagement))
                throw new FxNoAccessException(LOG, "ex.acl.updateFailed.noPermission", theACL.getName());
            if (ticket.getMandatorId() != theACL.getMandatorId())
                throw new FxNoAccessException(LOG, "ex.acl.updateFailed.foreignMandator", theACL.getName());
        }

        // Parameter checks
        if (color != null) color = FxFormatUtils.processColorString("COLOR", color);
        if (name != null) name = checkName(name);
        if (label != null) label = checkLabel(label);
        if (description != null) description = checkDescription(description);

        // Set values
        if (color != null) theACL.setColor(color);
        if (name != null) theACL.setName(name);
        if (label != null) theACL.setLabel(label);
        if (description != null) theACL.setDescription(description);

        Connection con = null;
        PreparedStatement stmt = null;
        String curSql;

        try {
            // Obtain a database connection
            con = Database.getDbConnection();

            curSql = "UPDATE " + TBL_ACLS + " SET " +
                    //       1         2        3            4           5         6
                    "cat_type=?,color=?,description=?,name=?,modified_by=?,modified_at=? WHERE id=" + id;
            final long NOW = System.currentTimeMillis();
            stmt = con.prepareStatement(curSql);
            stmt.setInt(1, theACL.getCategory().getId());
            stmt.setString(2, theACL.getColor());
            stmt.setString(3, theACL.getDescription());
            stmt.setString(4, theACL.getName());
            stmt.setLong(5, ticket.getUserId());
            stmt.setLong(6, NOW);
            final int uCount = stmt.executeUpdate();
            stmt.close();
            if (uCount != 1) {
                throw new SQLException(uCount + " rows affected instead of 1");
            }
            Database.storeFxString(theACL.getLabel(), con, TBL_ACLS, "LABEL", "ID", theACL.getId());
            //remove assignments
            curSql = "DELETE FROM " + TBL_ASSIGN_ACLS + " WHERE ACL=?";
            stmt = con.prepareStatement(curSql);
            stmt.setLong(1, theACL.getId());
            stmt.executeUpdate();
            if (assignments.size() > 0) {
                stmt.close();
                //                                             1          2    3      4        5        6     7      8
                curSql = "INSERT INTO " + TBL_ASSIGN_ACLS + " (USERGROUP, ACL, PEDIT, PREMOVE, PEXPORT, PREL, PREAD, PCREATE, " +
                        //9          10          11           12
                        "CREATED_BY, CREATED_AT, MODIFIED_BY, MODIFIED_AT) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
                stmt = con.prepareStatement(curSql);
                stmt.setLong(2, theACL.getId());
                stmt.setLong(9, ticket.getUserId());
                stmt.setLong(10, NOW);
                stmt.setLong(11, ticket.getUserId());
                stmt.setLong(12, NOW);
                for (ACLAssignment assignment : assignments) {
                    stmt.setLong(1, assignment.getGroupId());
                    stmt.setBoolean(3, assignment.getMayEdit());
                    stmt.setBoolean(4, assignment.getMayDelete());
                    stmt.setBoolean(5, assignment.getMayExport());
                    stmt.setBoolean(6, assignment.getMayRelate());
                    stmt.setBoolean(7, assignment.getMayRead());
                    stmt.setBoolean(8, assignment.getMayCreate());
                    if (assignment.getMayCreate() || assignment.getMayDelete() || assignment.getMayEdit() ||
                            assignment.getMayExport() || assignment.getMayRelate() || assignment.getMayRead())
                        stmt.executeUpdate();
                }
            }
            try {
                StructureLoader.updateACL(FxContext.get().getDivisionId(), this.load(id));
            } catch (FxLoadException e) {
                LOG.error(e, e);
            } catch (FxNotFoundException e) {
                LOG.error(e, e);
            }
        } catch (SQLException exc) {
            final boolean uniqueConstraintViolation = Database.isUniqueConstraintViolation(exc);
            ctx.setRollbackOnly();
            if (uniqueConstraintViolation)
                throw new FxEntryExistsException(LOG, "ex.acl.updateFailed.nameTaken",
                        theACL.getName(), name);
            else
                throw new FxUpdateException(LOG, "ex.acl.updateFailed", theACL.getName(), exc.getMessage());
        } finally {
            Database.closeObjects(ACLEngineBean.class, con, stmt);
        }

    }

    /**
     * {@inheritDoc}
     */
    public ACL load(long id) throws FxApplicationException {
        return load(id, false);
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public ACL load(long id, boolean ignoreSecurity) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        String curSql;
        try {
            final FxEnvironment environment = CacheAdmin.getEnvironment();

            // Obtain a database connection
            con = Database.getDbConnection();
            //               1        2    3        4           5     6          7          8           9
            curSql = "SELECT MANDATOR,NAME,CAT_TYPE,DESCRIPTION,COLOR,CREATED_BY,CREATED_AT,MODIFIED_BY,MODIFIED_AT FROM " +
                    TBL_ACLS + " WHERE ID=" + id;
            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(curSql);

            if (rs == null || !rs.next())
                throw new FxNotFoundException("ex.acl.load.notFound", id);

            // Read the ACL
            long mandatorId = rs.getLong(1);
            String name = rs.getString(2);
            int cat = rs.getInt(3);
            String desc = rs.getString(4);
            String color = rs.getString(5);
            FxString label = Database.loadFxString(con, TBL_ACLS, "LABEL", "ID=" + id);
            String sMandator = environment.getMandator(mandatorId).getName();
            ACL theACL = new ACL(id, name, label, mandatorId, sMandator, desc, color, ACL.Category.getById(cat),
                    LifeCycleInfoImpl.load(rs, 6, 7, 8, 9));
            if (ignoreSecurity && mandatorId != FxContext.get().getTicket().getMandatorId())
                throw new FxNoAccessException(LOG, "ex.acl.loadFailed.foreignMandator", theACL.getName());
            // Return the ACL
            return theACL;
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, "ex.acl.load.sqlError", id, exc.getMessage());
        } finally {
            Database.closeObjects(ACLEngineBean.class, con, stmt);
        }
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void assign(long aclId, long groupId, boolean mayRead, boolean mayEdit,
                       boolean mayRelate, boolean mayDelete, boolean mayExport, boolean mayCreate)
            throws FxApplicationException {
        UserTicket ticket = FxContext.get().getTicket();
        // Delete rather than create?
        if (!mayRead && !mayRelate && !mayExport && !mayEdit && !mayDelete) {
            try {
                unassign(aclId, groupId);
                return;
            } catch (FxNotFoundException e) {
                //can happen if an ACL only has create set! -> ignore it then
            } catch (FxRemoveException exc) {
                throw new FxCreateException(exc.getMessage(), exc);
            }
        }

        // Permission & exists check
        checkPermissions(ticket, groupId, aclId, true);

        Connection con = null;
        PreparedStatement stmt = null;
        String curSql;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();

            // Delete any old assignments
            curSql = "DELETE FROM " + TBL_ASSIGN_ACLS + " WHERE USERGROUP=" + groupId + " AND ACL=" + aclId;
            stmt = con.prepareStatement(curSql);
            int ucount = stmt.executeUpdate();
            stmt.close();
            if (LOG.isDebugEnabled())
                LOG.debug("Deleted assignments group[" + groupId + "]-acl[" + aclId + "]:" + ucount);

            curSql = "INSERT INTO " + TBL_ASSIGN_ACLS +
                    //1         2   3     4     5       6       7       8     9          10         11          12
                    "(USERGROUP,ACL,PREAD,PEDIT,PREMOVE,PCREATE,PEXPORT,PREL,CREATED_AT,CREATED_BY,MODIFIED_AT,MODIFIED_BY)" +
                    " VALUES " +
                    "(?,?,?,?,?,?,?,?,?,?,?,?)";

            final long NOW = System.currentTimeMillis();
            stmt = con.prepareStatement(curSql);
            stmt.setLong(1, groupId);
            stmt.setLong(2, aclId);
            stmt.setBoolean(3, mayRead);
            stmt.setBoolean(4, mayEdit);
            stmt.setBoolean(5, mayDelete);
            stmt.setBoolean(6, mayCreate);
            stmt.setBoolean(7, mayExport);
            stmt.setBoolean(8, mayRelate);
            stmt.setLong(9, NOW);
            stmt.setLong(10, ticket.getUserId());
            stmt.setLong(11, NOW);
            stmt.setLong(12, ticket.getUserId());

            stmt.executeUpdate();

            // Update active UserTickets
            UserTicketStore.flagDirtyHavingGroupId(groupId);
        } catch (Exception exc) {
            ctx.setRollbackOnly();
            FxCreateException dbe = new FxCreateException(exc, "ex.aclAssignment.createFailed");
            LOG.error(dbe, exc);
            throw dbe;
        } finally {
            Database.closeObjects(ACLEngineBean.class, con, stmt);
        }

    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void assign(long aclId, long groupId, ACL.Permission... permissions) throws FxApplicationException {
        boolean mayRead = false;
        boolean mayEdit = false;
        boolean mayRelate = false;
        boolean mayDelete = false;
        boolean mayExport = false;
        boolean mayCreate = false;
        for (ACL.Permission perm : permissions) {
            switch (perm) {
                case CREATE:
                    mayCreate = true;
                    break;
                case DELETE:
                    mayDelete = true;
                    break;
                case EDIT:
                    mayEdit = true;
                    break;
                case EXPORT:
                    mayExport = true;
                    break;
                case RELATE:
                    mayRelate = true;
                    break;
                case READ:
                    mayRead = true;
                    break;
            }
        }
        assign(aclId, groupId, mayRead, mayEdit, mayRelate, mayDelete, mayExport, mayCreate);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ACLAssignment> loadGroupAssignments(long groupId) throws FxApplicationException {
        return loadAssignments(null, groupId);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ACLAssignment> loadAssignments(long aclId) throws FxApplicationException {
        return loadAssignments(aclId, null);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void unassign(long aclId, long groupId) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        String curSql;
        UserTicket ticket = FxContext.get().getTicket();
        checkPermissions(ticket, groupId, aclId, true);

        try {
            // Obtain a database connection
            con = Database.getDbConnection();

            // Delete any old assignments
            curSql = "DELETE FROM " + TBL_ASSIGN_ACLS + " WHERE USERGROUP=" + groupId + " AND ACL=" + aclId;
            stmt = con.createStatement();
            if (stmt.executeUpdate(curSql) == 0) {
                FxNotFoundException nfe = new FxNotFoundException("ex.aclAssignment.notFound", aclId, groupId);
                if (LOG.isInfoEnabled()) LOG.info(nfe, nfe);
                throw nfe;
            }

            // Update active UserTickets
            UserTicketStore.flagDirtyHavingGroupId(groupId);
        } catch (FxNotFoundException e) {
            throw e;
        } catch (Exception exc) {
            ctx.setRollbackOnly();
            FxRemoveException dbe = new FxRemoveException(exc, "ex.aclAssignment.unassigneFailed", aclId, groupId);
            LOG.error(dbe, exc);
            throw dbe;
        } finally {
            Database.closeObjects(ACLEngineBean.class, con, stmt);
        }

    }

    /**
     * Permission check helper function.
     *
     * @param ticket    the ticket to use
     * @param groupId   the group id
     * @param aclId     the acl id
     * @param checkRole the role check option
     * @throws FxApplicationException if a error occured
     */
    private void checkPermissions(UserTicket ticket, Long groupId, Long aclId, boolean checkRole)
            throws FxApplicationException {

        // Security check (1)
        if (checkRole && !ticket.isInRole(Role.MandatorSupervisor))
            FxPermissionUtils.checkRole(ticket, Role.ACLManagement);

        if (groupId != null) {
            UserGroup grp = group.load(groupId);
            if (!grp.mayAccessGroup(ticket))
                throw new FxNoAccessException(LOG, "ex.acl.noAccess.foreignMandator");
        }

        if (aclId != null) {
            ACL acl = load(aclId);
            if (!ticket.isGlobalSupervisor() && acl.getMandatorId() != ticket.getMandatorId()) {
                // ACL belongs a foreign mandator
                throw new FxNoAccessException(LOG, "ex.acl.noAccess.read", aclId);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<ACLAssignment> loadAssignments(Long aclId, Long groupId) throws FxApplicationException {
        Connection con = null;
        Statement stmt = null;
        String curSql;
        UserTicket ticket = FxContext.get().getTicket();

        // Permission & Exists check
        checkPermissions(ticket, groupId, aclId, false);
        try {
            // Obtain a database connection
            con = Database.getDbConnection();

            // load assignments
            //                   1             2       3         4         5           6           7
            curSql = "SELECT ass.USERGROUP,ass.ACL,ass.PREAD,ass.PEDIT,ass.PREMOVE,ass.PEXPORT,ass.PREL," +
                    //   8        ,      9        10             11             12              13
                    "ass.PCREATE,acl.CAT_TYPE,ass.CREATED_BY,ass.CREATED_AT,ass.MODIFIED_BY,ass.MODIFIED_AT " +
                    "FROM " + TBL_ASSIGN_ACLS + " ass, " + TBL_ACLS + " acl WHERE " +
                    "ass.ACL=acl.ID AND " +
                    ((groupId != null) ? "USERGROUP=" + groupId : "") +
                    ((groupId != null && aclId != null) ? " AND " : "") +
                    ((aclId != null) ? "ACL=" + aclId : "");

            stmt = con.createStatement();
            ResultSet rs = stmt.executeQuery(curSql);

            // Read the data
            ArrayList<ACLAssignment> result = new ArrayList<ACLAssignment>(20);
            while (rs != null && rs.next())
                result.add(new ACLAssignment(rs.getLong(2), rs.getLong(1),
                        rs.getBoolean(3), rs.getBoolean(4), rs.getBoolean(7), rs.getBoolean(5), rs.getBoolean(6),
                        rs.getBoolean(8), ACL.Category.getById(rs.getByte(9)),
                        LifeCycleInfoImpl.load(rs, 10, 11, 12, 13)));

            // Return the found entries
            result.trimToSize();
            return result;
        } catch (SQLException exc) {
            throw new FxLoadException(LOG, exc, "ex.aclAssignment.loadFailed");
        } finally {
            Database.closeObjects(ACLEngineBean.class, con, stmt);
        }
    }

}