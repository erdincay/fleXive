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
package com.flexive.ejb.beans;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.*;
import com.flexive.core.security.UserTicketImpl;
import com.flexive.core.structure.FxEnvironmentImpl;
import com.flexive.core.structure.StructureLoader;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.configuration.Parameter;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.interfaces.ScriptingEngineLocal;
import com.flexive.shared.interfaces.SequencerEngine;
import com.flexive.shared.interfaces.SequencerEngineLocal;
import com.flexive.shared.scripting.*;
import com.flexive.shared.security.Role;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxType;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.control.CompilationFailedException;

import javax.annotation.Resource;
import javax.ejb.*;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ScriptingEngine implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

//TODO: log all modifications done by CRUD operations

@Stateless(name = "ScriptingEngine")
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ScriptingEngineBean implements ScriptingEngine, ScriptingEngineLocal {

    // Our logger
    private static transient Log LOG = LogFactory.getLog(ScriptingEngineBean.class);

    @Resource
    javax.ejb.SessionContext ctx;

    @EJB
    SequencerEngineLocal seq;

    /**
     * Cache for compile groovy scripts
     */
    static ConcurrentMap<Long, Script> groovyScriptCache = new ConcurrentHashMap<Long, Script>(50);

    /**
     * Timestamp of the script cache
     */
    static volatile long scriptCacheTimestamp = -1;

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String loadScriptCode(long scriptId) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        String code;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                                    1
            sql = "SELECT SDATA FROM " + TBL_SCRIPTS + " WHERE ID=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptId);
            ResultSet rs = ps.executeQuery();
            if (rs == null || !rs.next())
                throw new FxNotFoundException("ex.scripting.notFound", scriptId);
            code = rs.getString(1);
        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxLoadException(LOG, exc, "ex.scripting.load.failed", scriptId, exc.getMessage());
        } finally {
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
        return code;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxScriptInfo getScriptInfo(long scriptId) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        FxScriptInfo si;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            //            1     2     3     4
            sql = "SELECT SNAME,SDESC,SDATA,STYPE FROM " + TBL_SCRIPTS + " WHERE ID=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptId);
            ResultSet rs = ps.executeQuery();
            if (rs == null || !rs.next())
                throw new FxNotFoundException("ex.scripting.notFound", scriptId);
            si = new FxScriptInfo(scriptId, FxScriptEvent.getById(rs.getLong(4)), rs.getString(1), rs.getString(2),
                    rs.getString(3));
        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxLoadException(LOG, exc, "ex.scripting.load.failed", scriptId, exc.getMessage());
        } finally {
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
        return si;
    }

    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxScriptInfo[] getScriptInfos() throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        ArrayList<FxScriptInfo> slist = new ArrayList<FxScriptInfo>();
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            //                      1     2     3     4     5
            sql = "SELECT ID, SNAME,SDESC,SDATA,STYPE FROM " + TBL_SCRIPTS + " ORDER BY ID";
            ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs != null && rs.next()) {
                slist.add(new FxScriptInfo(rs.getInt(1), FxScriptEvent.getById(rs.getLong(5)), rs.getString(2), rs.getString(3),
                        rs.getString(4)));
            }

        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxLoadException(LOG, exc, "ex.scripts.load.failed", exc.getMessage());
        } finally {
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
        return slist.toArray(new FxScriptInfo[slist.size()]);
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateScriptInfo(long scriptId, FxScriptEvent event, String name, String description, String code) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        try {
            if (code == null)
                code = "";
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                          1       2       3       4          5
            sql = "UPDATE " + TBL_SCRIPTS + " SET SNAME=?,SDESC=?,SDATA=?,STYPE=? WHERE ID=?";
            ps = con.prepareStatement(sql);
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, code);
            ps.setLong(4, event.getId());
            ps.setLong(5, scriptId);
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            throw new FxUpdateException(LOG, exc, "ex.scripting.update.failed", name, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void updateScriptCode(long scriptId, String code) throws FxApplicationException {
        FxScriptInfo si = getScriptInfo(scriptId);
        updateScriptInfo(si.getId(), si.getEvent(), si.getName(), si.getDescription(), code);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<Long> getByScriptType(FxScriptEvent scriptEvent) {
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        List<Long> ret = new ArrayList<Long>(10);
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                                              1
            sql = "SELECT DISTINCT ID FROM " + TBL_SCRIPTS + " WHERE STYPE=? ORDER BY ID";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptEvent.getId());
            ResultSet rs = ps.executeQuery();
            while (rs != null && rs.next())
                ret.add(rs.getLong(1));
        } catch (SQLException exc) {
            ctx.setRollbackOnly();
            throw new FxDbException(LOG, exc, "ex.db.sqlError", exc.getMessage()).asRuntimeException();
        } finally {
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
        return ret;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxScriptInfo createScript(FxScriptEvent event, String name, String description, String code) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        FxScriptInfo si;
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        try {
            si = new FxScriptInfo(seq.getId(SequencerEngine.System.SCRIPTS), event, name, description, code);
            if (code == null)
                code = "";
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                      1  2     3     4     5
            sql = "INSERT INTO " + TBL_SCRIPTS + " (ID,SNAME,SDESC,SDATA,STYPE) VALUES (?,?,?,?,?)";
            ps = con.prepareStatement(sql);
            ps.setLong(1, si.getId());
            ps.setString(2, si.getName());
            ps.setString(3, si.getDescription());
            ps.setString(4, code);
            ps.setLong(5, si.getEvent().getId());
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            if (Database.isUniqueConstraintViolation(exc))
                throw new FxEntryExistsException("ex.scripting.name.notUnique", name);
            throw new FxCreateException(LOG, exc, "ex.scripting.create.failed", name, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
        return si;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxScriptInfo createScriptFromLibrary(FxScriptEvent event, String libraryname, String name, String description) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String code = FxSharedUtils.loadFromInputStream(cl.getResourceAsStream("fxresources/scripts/library/" + libraryname), -1);
        if (code == null || code.length() == 0)
            throw new FxNotFoundException("ex.scripting.load.library.failed", libraryname);
        return createScript(event, name, description, code);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxScriptInfo createScriptFromDropLibrary(String dropName, FxScriptEvent event, String libraryname, String name, String description) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        String code = FxSharedUtils.loadFromInputStream(cl.getResourceAsStream(dropName + "Resources/scripts/library/" + libraryname), -1);
        if (code == null || code.length() == 0)
            throw new FxNotFoundException("ex.scripting.load.library.failed", libraryname);
        return createScript(event, name, description, code);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeScript(long scriptId) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            sql = "DELETE FROM " + TBL_SCRIPT_MAPPING_ASSIGN + " WHERE SCRIPT=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptId);
            ps.executeUpdate();
            ps.close();
            sql = "DELETE FROM " + TBL_SCRIPT_MAPPING_TYPES + " WHERE SCRIPT=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptId);
            ps.executeUpdate();
            ps.close();
            //                                                    1
            sql = "DELETE FROM " + TBL_SCRIPTS + " WHERE ID=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptId);
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            throw new FxRemoveException(LOG, exc, "ex.scripting.remove.failed", scriptId, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxScriptResult runScript(long scriptId, FxScriptBinding binding) throws FxApplicationException {
        FxScriptInfo si = CacheAdmin.getEnvironment().getScript(scriptId);

        if (!isGroovyScript(si.getName()))
            return internal_runScript(si.getName(), binding, si.getCode());

        if (si.getEvent() == FxScriptEvent.Manual)
            FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptExecution);

        long timeStamp = CacheAdmin.getEnvironment().getTimeStamp();
        if (timeStamp != scriptCacheTimestamp) {
            scriptCacheTimestamp = timeStamp;
            groovyScriptCache.clear();
        }
        Script script = groovyScriptCache.get(scriptId);
        if (script == null) {
            try {
                GroovyShell shell = new GroovyShell();
                script = shell.parse(CacheAdmin.getEnvironment().getScript(scriptId).getCode());
            } catch (CompilationFailedException e) {
                throw new FxInvalidParameterException(si.getName(), "ex.general.scripting.compileFailed", si.getName());
            } catch (Throwable t) {
                throw new FxInvalidParameterException(si.getName(), "ex.general.scripting.exception", si.getName(), t.getMessage());
            }
            groovyScriptCache.putIfAbsent(scriptId, script);
        }

        if (binding == null)
            binding = new FxScriptBinding();
        if (!binding.getProperties().containsKey("ticket"))
            binding.setVariable("ticket", FxContext.get().getTicket());
        if (!binding.getProperties().containsKey("environment"))
            binding.setVariable("environment", CacheAdmin.getEnvironment());
        binding.setVariable("scriptname", si.getName());

        try {
            Object result;
            synchronized (script) {
                script.setBinding(new Binding(binding.getProperties()));
                result = script.run();
            }
            return new FxScriptResult(binding, result);
        } catch (Throwable e) {
            if (e instanceof FxApplicationException)
                throw (FxApplicationException) e;
            LOG.error("Scripting error: " + e.getMessage(), e);
            throw new FxInvalidParameterException(si.getName(), "ex.general.scripting.exception", si.getName(), e.getMessage());
        }

        /*
//        long time = System.currentTimeMillis();
        GroovyShell shell = new GroovyShell();
//        System.out.println("shell creation took " + (System.currentTimeMillis() - time));
//        time = System.currentTimeMillis();
        Script script = shell.parse(CacheAdmin.getEnvironment().getScript(scriptId).getCode());
//        Script script = shell.parse(loadScriptCode(scriptId));
//        System.out.println("parsing took " + (System.currentTimeMillis() - time));
//        time = System.currentTimeMillis();
        if (!binding.getProperties().containsKey("ticket"))
            binding.setVariable("ticket", FxContext.get().getUserTicket());
        if (!binding.getProperties().containsKey("environment"))
            binding.setVariable("environment", CacheAdmin.getEnvironment());
        script.setBinding(new Binding(binding.getProperties()));
//        try {
        return script.run();
//        } finally {
//            System.out.println("execution took " + (System.currentTimeMillis() - time));
//        }*/
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxScriptResult runScript(long scriptId) throws FxApplicationException {
        return runScript(scriptId, null);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxScriptMappingEntry createAssignmentScriptMapping(FxScriptEvent scriptEvent, long scriptId, long assignmentId, boolean active, boolean derivedUsage) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        FxScriptMappingEntry sm;
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        //check existance
        FxScriptInfo si = getScriptInfo(scriptId);
        try {
            long[] derived;
            if (!derivedUsage)
                derived = new long[0];
            else {
                List<FxAssignment> ass = CacheAdmin.getEnvironment().getDerivedAssignments(assignmentId);
                derived = new long[ass.size()];
                for (int i = 0; i < ass.size(); i++)
                    derived[i] = ass.get(i).getId();
            }
            sm = new FxScriptMappingEntry(scriptEvent, scriptId, active, derivedUsage, assignmentId, derived);
            // Obtain a database connection
            con = Database.getDbConnection();
            sql = "INSERT INTO " + TBL_SCRIPT_MAPPING_ASSIGN + " (ASSIGNMENT,SCRIPT,DERIVED_USAGE,ACTIVE,STYPE) VALUES " +
                    //1,2,3,4,5
                    "(?,?,?,?,?)";
            ps = con.prepareStatement(sql);
            ps.setLong(1, sm.getId());
            ps.setLong(2, sm.getScriptId());
            ps.setBoolean(3, sm.isDerivedUsage());
            ps.setBoolean(4, sm.isActive());
            ps.setLong(5, sm.getScriptEvent().getId());
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            if (Database.isUniqueConstraintViolation(exc))
                throw new FxEntryExistsException("ex.scripting.mapping.assign.notUnique", scriptId, assignmentId);
            throw new FxCreateException(LOG, exc, "ex.scripting.mapping.assign.create.failed", scriptId, assignmentId, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
        return sm;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxScriptMappingEntry createAssignmentScriptMapping(long scriptId, long typeId, boolean active, boolean derivedUsage) throws FxApplicationException {
        FxScriptInfo si = getScriptInfo(scriptId);
        return createAssignmentScriptMapping(si.getEvent(), scriptId, typeId, active, derivedUsage);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxScriptMappingEntry createTypeScriptMapping(FxScriptEvent scriptEvent, long scriptId, long typeId, boolean active, boolean derivedUsage) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        FxScriptMappingEntry sm;
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        //check existance
        FxScriptInfo si = getScriptInfo(scriptId);
        try {
            long[] derived;
            if (!derivedUsage)
                derived = new long[0];
            else {
                List<FxType> types = CacheAdmin.getEnvironment().getType(typeId).getDerivedTypes();
                derived = new long[types.size()];
                for (int i = 0; i < types.size(); i++)
                    derived[i] = types.get(i).getId();
            }
            sm = new FxScriptMappingEntry(scriptEvent, scriptId, active, derivedUsage, typeId, derived);
            // Obtain a database connection
            con = Database.getDbConnection();
            sql = "INSERT INTO " + TBL_SCRIPT_MAPPING_TYPES + " (TYPEDEF,SCRIPT,DERIVED_USAGE,ACTIVE,STYPE) VALUES " +
                    //1,2,3,4,5
                    "(?,?,?,?,?)";
            ps = con.prepareStatement(sql);
            ps.setLong(1, sm.getId());
            ps.setLong(2, sm.getScriptId());
            ps.setBoolean(3, sm.isDerivedUsage());
            ps.setBoolean(4, sm.isActive());
            ps.setLong(5, sm.getScriptEvent().getId());
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            if (Database.isUniqueConstraintViolation(exc))
                throw new FxEntryExistsException("ex.scripting.mapping.types.notUnique", scriptId, typeId);
            throw new FxCreateException(LOG, exc, "ex.scripting.mapping.types.create.failed", scriptId, typeId, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
        return sm;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxScriptMappingEntry createTypeScriptMapping(long scriptId, long typeId, boolean active, boolean derivedUsage) throws FxApplicationException {
        FxScriptInfo si = getScriptInfo(scriptId);
        return createTypeScriptMapping(si.getEvent(), scriptId, typeId, active, derivedUsage);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeAssignmentScriptMapping(long scriptId, long assignmentId) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                                                1                2
            sql = "DELETE FROM " + TBL_SCRIPT_MAPPING_ASSIGN + " WHERE SCRIPT=? AND ASSIGNMENT=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptId);
            ps.setLong(2, assignmentId);
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            throw new FxRemoveException(LOG, exc, "ex.scripting.mapping.assign.remove.failed", scriptId, assignmentId, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
    }

     /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeAssignmentScriptMappingForEvent(long scriptId, long assignmentId, FxScriptEvent event) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                                                1                2
            sql = "DELETE FROM " + TBL_SCRIPT_MAPPING_ASSIGN + " WHERE SCRIPT=? AND ASSIGNMENT=? AND STYPE=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptId);
            ps.setLong(2, assignmentId);
            ps.setLong(3, event.getId());
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            throw new FxRemoveException(LOG, exc, "ex.scripting.mapping.assign.remove.failed", scriptId, assignmentId, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeTypeScriptMapping(long scriptId, long typeId) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                                               1                2
            sql = "DELETE FROM " + TBL_SCRIPT_MAPPING_TYPES + " WHERE SCRIPT=? AND TYPEDEF=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptId);
            ps.setLong(2, typeId);
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            throw new FxRemoveException(LOG, exc, "ex.scripting.mapping.type.remove.failed", scriptId, typeId, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeTypeScriptMappingForEvent(long scriptId, long typeId, FxScriptEvent event) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        try {
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                                               1                2
            sql = "DELETE FROM " + TBL_SCRIPT_MAPPING_TYPES + " WHERE SCRIPT=? AND TYPEDEF=? AND STYPE=?";
            ps = con.prepareStatement(sql);
            ps.setLong(1, scriptId);
            ps.setLong(2, typeId);
            ps.setLong(3, event.getId());
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            throw new FxRemoveException(LOG, exc, "ex.scripting.mapping.type.remove.failed", scriptId, typeId, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxScriptMappingEntry updateAssignmentScriptMappingForEvent(long scriptId, long assignmentId, FxScriptEvent event, boolean active, boolean derivedUsage) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        FxScriptMappingEntry sm;
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        //check existance
        FxScriptInfo si = getScriptInfo(scriptId);
        try {
            long[] derived;
            if (!derivedUsage)
                derived = new long[0];
            else {
                List<FxAssignment> ass = CacheAdmin.getEnvironment().getDerivedAssignments(assignmentId);
                derived = new long[ass.size()];
                for (int i = 0; i < ass.size(); i++)
                    derived[i] = ass.get(i).getId();
            }
            //TODO: dont overwrite type info, use xxEdit objects!!
            sm = new FxScriptMappingEntry(event, scriptId, active, derivedUsage, assignmentId, derived);
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                                                1        2                  3            4
            sql = "UPDATE " + TBL_SCRIPT_MAPPING_ASSIGN + " SET DERIVED_USAGE=?,ACTIVE=? WHERE ASSIGNMENT=? AND SCRIPT=? AND STYPE=?";
            ps = con.prepareStatement(sql);
            ps.setBoolean(1, sm.isDerivedUsage());
            ps.setBoolean(2, sm.isActive());
            ps.setLong(3, sm.getId());
            ps.setLong(4, sm.getScriptId());
            ps.setLong(5, sm.getScriptEvent().getId());
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            if (Database.isUniqueConstraintViolation(exc))
                throw new FxEntryExistsException("ex.scripting.mapping.assign.notUnique", scriptId, assignmentId);
            throw new FxUpdateException(LOG, exc, "ex.scripting.mapping.assign.update.failed", scriptId, assignmentId, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
        return sm;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxScriptMappingEntry updateTypeScriptMappingForEvent(long scriptId, long typeId, FxScriptEvent event, boolean active, boolean derivedUsage) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptManagement);
        FxScriptMappingEntry sm;
        Connection con = null;
        PreparedStatement ps = null;
        String sql;
        boolean success = false;
        //check existance
        FxScriptInfo si = getScriptInfo(scriptId);
        try {
            long[] derived;
            if (!derivedUsage)
                derived = new long[0];
            else {
                List<FxType> types = CacheAdmin.getEnvironment().getType(typeId).getDerivedTypes();
                derived = new long[types.size()];
                for (int i = 0; i < types.size(); i++)
                    derived[i] = types.get(i).getId();
            }
            //TODO: dont overwrite type info, use xxEdit objects!!
            sm = new FxScriptMappingEntry(event, scriptId, active, derivedUsage, typeId, derived);
            // Obtain a database connection
            con = Database.getDbConnection();
            //                                                               1        2             3          4          5
            sql = "UPDATE " + TBL_SCRIPT_MAPPING_TYPES + " SET DERIVED_USAGE=?,ACTIVE=? WHERE TYPEDEF=? AND SCRIPT=? AND STYPE=?";
            ps = con.prepareStatement(sql);
            ps.setBoolean(1, sm.isDerivedUsage());
            ps.setBoolean(2, sm.isActive());
            ps.setLong(3, sm.getId());
            ps.setLong(4, sm.getScriptId());
            ps.setLong(5, sm.getScriptEvent().getId());
            ps.executeUpdate();
            success = true;
        } catch (SQLException exc) {
            if (Database.isUniqueConstraintViolation(exc))
                throw new FxEntryExistsException("ex.scripting.mapping.types.notUnique", scriptId, typeId);
            throw new FxUpdateException(LOG, exc, "ex.scripting.mapping.types.update.failed", scriptId, typeId, exc.getMessage());
        } finally {
            if (!success)
                ctx.setRollbackOnly();
            else
                StructureLoader.reloadScripting(FxContext.get().getDivisionId());
            Database.closeObjects(ScriptingEngineBean.class, con, ps);
        }
        return sm;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void executeRunOnceScripts() {
        final long start = System.currentTimeMillis();
        runOnce(SystemParameters.DIVISION_RUNONCE, "fxresources", "flexive");
        if (LOG.isInfoEnabled()) {
            LOG.info("Executed flexive run-once scripts in " + (System.currentTimeMillis() - start) + "ms");
        }
    }

    /**
     * Execute all runOnce scripts in the resource denoted by prefix if param is "false"
     *
     * @param param           boolean parameter that will be flagged as "true" once the scripts are run
     * @param prefix          resource directory prefix
     * @param applicationName the corresponding application name (for debug messages)
     */
    private void runOnce(Parameter<Boolean> param, String prefix, String applicationName) {
        try {
            Boolean executed = EJBLookup.getDivisionConfigurationEngine().get(param);
            if (executed) {
//                System.out.println("=============> skip run-once <==============");
                return;
            }
        } catch (FxApplicationException e) {
            LOG.error(e);
            return;
        }
//        System.out.println("<=============> run run-once <==============>");
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream scriptIndex = cl.getResourceAsStream(prefix + "/scripts/runonce/scriptindex.flexive");
        if (scriptIndex == null) {
            if (LOG.isInfoEnabled()) {
                LOG.info("No run-once scripts defined for " + applicationName);
            }
            return;
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing run-once scripts for " + applicationName);
        }
        String[] files = FxSharedUtils.loadFromInputStream(scriptIndex, -1).
                replaceAll("\r", "").split("\n");
        FxContext.get().runAsSystem();
        final UserTicket originalTicket = FxContext.get().getTicket();
        try {
            FxScriptBinding binding = new FxScriptBinding();
            UserTicket ticket = ((UserTicketImpl) UserTicketImpl.getGuestTicket()).cloneAsGlobalSupervisor();
            binding.setVariable("ticket", ticket);
            FxContext.get().overrideTicket(ticket);
            for (String temp : files) {
                String[] file = temp.split("\\|");
                if (StringUtils.isBlank(file[0])) {
                    continue;
                }
                if (LOG.isInfoEnabled()) {
                    LOG.info("running run-once-script [" + file[0] + "] ...");
                }
                try {
                    internal_runScript(file[0], binding, FxSharedUtils.loadFromInputStream(cl.getResourceAsStream(prefix + "/scripts/runonce/" + file[0]), -1));
                } catch (Throwable e) {
                    LOG.error("Failed to run script " + file[0] + ": " + e.getMessage(), e);
                }
            }
        } finally {
            FxContext.get().stopRunAsSystem();
            FxContext.get().overrideTicket(originalTicket);
        }
        try {
            EJBLookup.getDivisionConfigurationEngine().put(param, true);
        } catch (FxApplicationException e) {
            LOG.error(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void executeDropRunOnceScripts(Parameter<Boolean> param, String dropName) throws FxApplicationException {
        if (!FxSharedUtils.getDrops().contains(dropName))
            throw new FxInvalidParameterException("dropName", "ex.scripting.drop.notFound", dropName);
        runOnce(param, dropName + "Resources", "drop " + dropName);
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void executeStartupScripts() {
        executeStartupScripts("fxresources", "flexive");
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void executeDropStartupScripts(String dropName) throws FxApplicationException {
        if (!FxSharedUtils.getDrops().contains(dropName))
            throw new FxInvalidParameterException("dropName", "ex.scripting.drop.notFound", dropName);
        executeStartupScripts(dropName + "Resources", "drop " + dropName);
    }

    /**
     * Eexecute startup scripts within the given subfolder identified by prefix
     *
     * @param prefix          subfolder for scripts
     * @param applicationName the corresponding application name (for debug messages)
     */
    private void executeStartupScripts(String prefix, String applicationName) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final InputStream scriptIndex = cl.getResourceAsStream(prefix + "/scripts/startup/scriptindex.flexive");
        if (scriptIndex == null) {
            if (LOG.isInfoEnabled()) {
                LOG.info("No startup scripts defined for " + applicationName);
            }
            return;
        }
        if (LOG.isInfoEnabled()) {
            LOG.info("Executing startup scripts for " + applicationName);
        }
        String[] files = FxSharedUtils.loadFromInputStream(scriptIndex, -1).
                replaceAll("\r", "").split("\n");
        FxContext.get().runAsSystem();
        final UserTicket originalTicket = FxContext.get().getTicket();
        try {
            FxScriptBinding binding = new FxScriptBinding();
            UserTicket ticket = ((UserTicketImpl) UserTicketImpl.getGuestTicket()).cloneAsGlobalSupervisor();
            binding.setVariable("ticket", ticket);
            FxContext.get().overrideTicket(ticket);
            for (String temp : files) {
                String[] file = temp.split("\\|");
                if (StringUtils.isBlank(file[0])) {
                    continue;
                }
                LOG.info("running startup-script [" + file[0] + "] ...");
                try {
                    internal_runScript(file[0], binding, FxSharedUtils.loadFromInputStream(cl.getResourceAsStream(prefix + "/scripts/startup/" + file[0]), -1));
                } catch (Throwable e) {
                    LOG.error("Failed to run script " + file[0] + ": " + e.getMessage(), e);
                }
            }
        } finally {
            FxContext.get().stopRunAsSystem();
            FxContext.get().overrideTicket(originalTicket);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxScriptResult runScript(String name, FxScriptBinding binding, String code) throws FxApplicationException {
        FxPermissionUtils.checkRole(FxContext.get().getTicket(), Role.ScriptExecution);
        return internal_runScript(name, binding, code);
    }

    /**
     * Execute a script.
     * This method does not check the calling user's role nor does it cache scripts.
     * It should only be used to execute code from the groovy console or code that is not to be expected to
     * be run more than once.
     *
     * @param name    name of the script, extension is needed to choose interpreter
     * @param binding bindings to apply
     * @param code    the script code
     * @return last script evaluation result
     * @throws FxApplicationException on errors
     */
    private FxScriptResult internal_runScript(String name, FxScriptBinding binding, String code) throws FxApplicationException {
        if (name == null)
            name = "unknown";
        if (binding != null) {
            if (!binding.getProperties().containsKey("ticket"))
                binding.setVariable("ticket", FxContext.get().getTicket());
            if (!binding.getProperties().containsKey("environment"))
                binding.setVariable("environment", CacheAdmin.getEnvironment());
            binding.setVariable("scriptname", name);
        }
        if (isGroovyScript(name)) {
            //we prefer the native groovy binding
            try {
                GroovyShell shell = new GroovyShell();
                Script script = shell.parse(code);
                if (binding != null)
                    script.setBinding(new Binding(binding.getProperties()));
                Object result = script.run();
                return new FxScriptResult(binding, result);
            } catch (Throwable e) {
                if (e instanceof FxApplicationException)
                    throw (FxApplicationException) e;
                LOG.error("Scripting error: " + e.getMessage(), e);
                throw new FxInvalidParameterException(name, "ex.general.scripting.exception", name, e.getMessage());
            }
        }
/*
        //if we run JDK 6, more engines might be available
        try {
            Class.forName("javax.script.ScriptEngineManager"); //provoke exception if no JDK >= 6 installed
            String ext = name.substring(name.lastIndexOf('.') + 1);
            javax.script.ScriptEngineManager manager = new javax.script.ScriptEngineManager();
            javax.script.ScriptEngine engine = manager.getEngineByName(ext);
            if (engine == null)
                throw new FxInvalidParameterException(name, "ex.general.scripting.noEngine", name).asRuntimeException();
            javax.script.Bindings b = engine.createBindings();
            if (binding != null)
                b.putAll(binding.getProperties());
            engine.setBindings(b, javax.script.ScriptContext.ENGINE_SCOPE);
            Object result = engine.eval(code);
            if (binding != null) {
                binding.getProperties().clear();
                Object o;
                for (String key : engine.getBindings(javax.script.ScriptContext.ENGINE_SCOPE).keySet()) {
                    o = engine.getBindings(javax.script.ScriptContext.ENGINE_SCOPE).get(key);
                    if (o instanceof Serializable)
                        binding.getProperties().put(key, (Serializable) o);
                }
            }
            return new FxScriptResult(binding, result);
        } catch (ClassNotFoundException cnfe) {
            System.err.println("No JDK 6 installed");
            throw new FxInvalidParameterException(name, "ex.general.scripting.needJDK6", name).asRuntimeException();
        } catch (javax.script.ScriptException e) {
            throw new FxInvalidParameterException(name, "ex.general.scripting.exception", name, e.getMessage()).asRuntimeException();
        }
*/
        return null;
    }

    /**
     * Is the script (most likely) a groovy script?
     *
     * @param name script name to check
     * @return if this script could be a groovy script
     */
    private static boolean isGroovyScript(String name) {
        return name.toLowerCase().endsWith(".gy") || name.toLowerCase().endsWith(".groovy");
    }

    public FxScriptMapping loadScriptMapping(Connection _con, long scriptId) throws FxLoadException, SQLException {
        FxScriptMapping mapping;
        PreparedStatement ps_a = null, ps_t = null;
        String sql;
        Connection con = _con;
        if (con == null)
            con = Database.getDbConnection();


        List<FxScriptMappingEntry> e_ass;
        List<FxScriptMappingEntry> e_types;
        FxEnvironmentImpl environment = ((FxEnvironmentImpl) CacheAdmin.getEnvironment()).deepClone();

        try {
            //            1          2             3      4
            sql = "SELECT ASSIGNMENT,DERIVED_USAGE,ACTIVE,STYPE FROM " + TBL_SCRIPT_MAPPING_ASSIGN + " WHERE SCRIPT=?";
            ps_a = con.prepareStatement(sql);
            sql = "SELECT TYPEDEF,DERIVED_USAGE,ACTIVE,STYPE FROM " + TBL_SCRIPT_MAPPING_TYPES + " WHERE SCRIPT=?";
            ps_t = con.prepareStatement(sql);
            ResultSet rs;

            ps_a.setLong(1, scriptId);
            ps_t.setLong(1, scriptId);
            rs = ps_a.executeQuery();
            e_ass = new ArrayList<FxScriptMappingEntry>(20);
            e_types = new ArrayList<FxScriptMappingEntry>(20);
            while (rs != null && rs.next()) {
                long[] derived;
                if (!rs.getBoolean(2))
                    derived = new long[0];
                else {
                    List<FxAssignment> ass = environment.getDerivedAssignments(rs.getLong(1));
                    derived = new long[ass.size()];
                    for (int i = 0; i < ass.size(); i++)
                        derived[i] = ass.get(i).getId();
                }
                e_ass.add(new FxScriptMappingEntry(FxScriptEvent.getById(rs.getLong(4)), scriptId, rs.getBoolean(3), rs.getBoolean(2), rs.getLong(1), derived));
            }
            rs = ps_t.executeQuery();
            while (rs != null && rs.next()) {
                long[] derived;
                if (!rs.getBoolean(2))
                    derived = new long[0];
                else {
                    List<FxType> types = environment.getType(rs.getLong(1)).getDerivedTypes();
                    derived = new long[types.size()];
                    for (int i = 0; i < types.size(); i++)
                        derived[i] = types.get(i).getId();
                }
                e_types.add(new FxScriptMappingEntry(FxScriptEvent.getById(rs.getLong(4)), scriptId, rs.getBoolean(3), rs.getBoolean(2), rs.getLong(1), derived));
            }
            mapping = new FxScriptMapping(scriptId, e_types, e_ass);


        } catch (SQLException exc) {
            throw new FxLoadException(LOG, exc, "ex.scripting.mapping.load.failed", exc.getMessage());
        } catch (FxNotFoundException e) {
            throw new FxLoadException(e);
        } finally {
            try {
                if (ps_t != null)
                    ps_t.close();
            } catch (SQLException e) {
                //ignore
            }
            if (_con == null) {
                Database.closeObjects(ScriptingEngineBean.class, con, ps_a);
            }
        }
        return mapping;

    }
}
