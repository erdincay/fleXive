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
import com.flexive.core.LifeCycleInfoImpl;
import com.flexive.core.storage.ContentStorage;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.*;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.content.*;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.*;
import com.flexive.shared.scripting.FxScriptBinding;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.TypeStorageMode;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.shared.value.FxBinary;
import com.flexive.shared.workflow.Step;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Resource;
import javax.ejb.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Content Engine implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Stateless(name = "ContentEngine")
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@TransactionManagement(TransactionManagementType.CONTAINER)
public class ContentEngineBean implements ContentEngine, ContentEngineLocal {

    private static transient Log LOG = LogFactory.getLog(ContentEngineBean.class);
    @Resource
    javax.ejb.SessionContext ctx;

    @EJB
    LanguageEngine language;

    @EJB
    SequencerEngineLocal seq;

    @EJB
    ScriptingEngineLocal scripting;

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxContent initialize(long typeId, long mandatorId, long prefACL, long prefStep, long prefLang)
            throws FxApplicationException {
        UserTicket ticket = FxContext.get().getTicket();
        FxEnvironment environment;
        environment = CacheAdmin.getEnvironment();
        FxType type = environment.getType(typeId);
        //security check start
        if (type.useTypePermissions() && !ticket.mayCreateACL(type.getACL().getId(), ticket.getUserId()))
            throw new FxNoAccessException("ex.acl.noAccess.create", type.getACL().getName());
        //security check end
        long acl = prefACL;
        try {
            environment.getACL(acl);
        } catch (Exception e) {
            if (ticket.isGlobalSupervisor()) {
                acl = ACL.Category.INSTANCE.getDefaultId();
            } else {
                //get best fit if possible
                Long[] acls = ticket.getACLsId(ticket.getUserId(), ACL.Category.INSTANCE, ACL.Permission.CREATE, ACL.Permission.EDIT, ACL.Permission.READ);
                if (acls.length > 0)
                    acl = acls[0];
                else {
                    if (type.useInstancePermissions())
                        throw new FxNoAccessException("ex.content.noSuitableACL", type.getName());
                    else
                        acl = ACL.Category.INSTANCE.getDefaultId();
                }
            }
        }
        long step = prefStep;
        if (!type.getWorkflow().isStepValid(step))
            try {
                step = type.getWorkflow().getSteps().get(0).getId();
            } catch (Exception e) {
                throw new FxInvalidParameterException("STEP", "ex.workflow.noStepDefined", type.getWorkflow().getName());
            }
        long lang = prefLang;
        try {
            language.load(lang);
        } catch (FxInvalidLanguageException e) {
            lang = ticket.getLanguage().getId();
        }
        FxPK sourcePK = null, destinationPK = null;
        int sourcePos = 0, destinationPos = 0;
        if (type.isRelation()) {
            sourcePK = FxPK.createNewPK();
            destinationPK = FxPK.createNewPK();
            sourcePos = destinationPos = 0;
        }
        FxContent content = new FxContent(FxPK.createNewPK(), type.getId(), type.isRelation(), mandatorId, acl, step, 1,
                environment.getStep(step).isLiveStep() ? 1 : 0, true, lang,
                sourcePK, destinationPK, sourcePos, destinationPos,
                LifeCycleInfoImpl.createNew(ticket), type.createEmptyData(type.buildXPathPrefix(FxPK.createNewPK())), BinaryDescriptor.SYS_UNKNOWN, 1).initSystemProperties();
        //scripting after start
        FxScriptBinding binding = null;
        long[] scripts = type.getScriptMapping(FxScriptEvent.AfterContentInitialize);
        if (scripts != null)
            for (long script : scripts) {
                if (binding == null)
                    binding = new FxScriptBinding();
                binding.setVariable("content", content);
                scripting.runScript(script, binding);
            }
        //scripting after end
        return content;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxContent initialize(long typeId) throws FxApplicationException {
        FxType type = CacheAdmin.getEnvironment().getType(typeId);
        UserTicket ticket = FxContext.get().getTicket();
        return initialize(type.getId(), ticket.getMandatorId(),
                -1 /*invalid ACL will cause a lookup for best-fit ACL*/,
                type.getWorkflow().getSteps().get(0).getId(), ticket.getLanguage().getId());
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxContent initialize(String typeName) throws FxApplicationException {
        return initialize(CacheAdmin.getEnvironment().getType(typeName).getId());
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public FxContent load(FxPK pk) throws FxApplicationException {
//        long time = System.currentTimeMillis();
//        System.out.println("=> Cache check for " + pk);
        FxCachedContent cachedContent = CacheAdmin.getCachedContent(pk);
        FxEnvironment env = CacheAdmin.getEnvironment();
        Connection con = null;
        PreparedStatement ps = null;
        try {
            if (cachedContent == null) {
//                System.out.println("=> Cache miss for " + pk);
                ContentStorage storage = StorageManager.getContentStorage(pk.getStorageMode());
                StringBuilder sql = new StringBuilder(2000);
                con = Database.getDbConnection();
                FxContent rawContent = storage.contentLoad(con, pk, env, sql).copy();
                cachedContent = new FxCachedContent(rawContent, storage.getContentSecurityInfo(con, pk));
                CacheAdmin.cacheContent(cachedContent);
//                System.out.println("=> Cached " + pk);
            } else {
//                System.out.println("=> Cache hit for " + pk);
            }
            FxContent content = cachedContent.getContent().copy();

            //security check start
            UserTicket ticket = FxContext.get().getTicket();
            FxType type = env.getType(cachedContent.getContent().getTypeId());
            FxPermissionUtils.checkPermission(ticket, content.getLifeCycleInfo().getCreatorId(), ACL.Permission.READ, type,
                    cachedContent.getSecurityInfo().getStepACL(),
                    cachedContent.getSecurityInfo().getContentACL(), true);

            if (type.usePropertyPermissions() && !ticket.isGlobalSupervisor()) {
                //wrap with FxNoAccess or set to readonly when appropriate
                FxPermissionUtils.wrapNoAccessValues(ticket, cachedContent.getSecurityInfo(),
                        content, type, env);
            }
            //security check end
            //scripting after start
            FxScriptBinding binding = null;
            long[] scripts = env.getType(content.getTypeId()).getScriptMapping(FxScriptEvent.AfterContentLoad);
            if (scripts != null)
                for (long script : scripts) {
                    if (binding == null)
                        binding = new FxScriptBinding();
                    binding.setVariable("content", content);
                    scripting.runScript(script, binding);
                }
            //scripting after end
            content.getRootGroup().removeEmptyEntries();
            content.getRootGroup().compactPositions(true);
            return content;
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, ps);
//            System.out.println("=> Load time: " + (System.currentTimeMillis() - time));
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxPK save(FxContent content) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        FxPK pk;
        FxScriptEvent beforeAssignmentScript, afterAssignmentScript;

        try {
            FxEnvironment env = CacheAdmin.getEnvironment();
            ContentStorage storage = StorageManager.getContentStorage(content.getPk().getStorageMode());
            con = Database.getDbConnection();

            //security check start
            FxType type = env.getType(content.getTypeId());
            Step step = env.getStep(content.getStepId());
            UserTicket ticket = FxContext.get().getTicket();
            if (content.getPk().isNew()) {
                FxPermissionUtils.checkPermission(ticket, ticket.getUserId(), ACL.Permission.CREATE, type, step.getAclId(), content.getAclId(), true);
                beforeAssignmentScript = FxScriptEvent.BeforeAssignmentDataCreate;
                afterAssignmentScript = FxScriptEvent.AfterAssignmentDataCreate;
            } else {
                FxPermissionUtils.checkPermission(ticket, content.getLifeCycleInfo().getCreatorId(), ACL.Permission.EDIT, type, step.getAclId(), content.getAclId(), true);
                beforeAssignmentScript = FxScriptEvent.BeforeAssignmentDataSave;
                afterAssignmentScript = FxScriptEvent.AfterAssignmentDataSave;
            }
            if (type.usePropertyPermissions() && !ticket.isGlobalSupervisor()) 
                FxPermissionUtils.checkPropertyPermissions(content, content.getPk().isNew() ? ACL.Permission.CREATE : ACL.Permission.EDIT);
            //security check end

            storage.prepareSave(con, content);

            FxScriptBinding binding = null;
            long[] typeScripts;
            //scripting before start
            //type scripting
            typeScripts = type.getScriptMapping(
                    content.getPk().isNew()
                            ? FxScriptEvent.BeforeContentCreate
                            : FxScriptEvent.BeforeContentSave);
            if (typeScripts != null)
                for (long script : typeScripts) {
                    if (binding == null)
                        binding = new FxScriptBinding();
                    binding.setVariable("content", content);
                    Object result = scripting.runScript(script, binding).getResult();
                    if (result != null && result instanceof FxContent) {
                        content = (FxContent) result;
                    }
                }
            //assignment scripting
            if (type.hasScriptedAssignments()) {
                binding = null;
                for (FxAssignment as : type.getScriptedAssignments(beforeAssignmentScript)) {
                    if (binding == null)
                        binding = new FxScriptBinding();
                    binding.setVariable("assignment", as);
                    for (long script : as.getScriptMapping(beforeAssignmentScript)) {
                        binding.setVariable("content", content);
                        Object result = scripting.runScript(script, binding).getResult();
                        if (result != null && result instanceof FxContent) {
                            content = (FxContent) result;
                        }
                    }
                }
            }
            //scripting before end
            //unwrap all no access values so they can be saved
            if (type.usePropertyPermissions() && !ticket.isGlobalSupervisor()) {
                FxContext.get().runAsSystem();
                FxPermissionUtils.unwrapNoAccessValues(content);
                FxContext.get().stopRunAsSystem();
            }
            if (content.getPk().isNew()) {
                pk = storage.contentCreate(con, env, null, seq.getId(SequencerEngine.System.CONTENT), content);
                if (LOG.isDebugEnabled())
                    LOG.debug("creating new content for type " + CacheAdmin.getEnvironment().getType(content.getTypeId()).getName());
            } else {
                pk = storage.contentSave(con, env, null, content, EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_FQN_PROPERTY));
            }
            //scripting after start
            //assignment scripting
            if (type.hasScriptedAssignments()) {
                binding = new FxScriptBinding();
                binding.setVariable("pk", pk);
                for (FxAssignment as : type.getScriptedAssignments(afterAssignmentScript)) {
                    binding.setVariable("assignment", as);
                    for (long script : as.getScriptMapping(afterAssignmentScript)) {
                        scripting.runScript(script, binding);
                    }
                }
            }
            //type scripting
            typeScripts = env.getType(content.getTypeId()).getScriptMapping(
                    content.getPk().isNew()
                            ? FxScriptEvent.AfterContentCreate
                            : FxScriptEvent.AfterContentSave);
            if (typeScripts != null)
                for (long script : typeScripts) {
                    if (binding == null)
                        binding = new FxScriptBinding();
                    binding.setVariable("pk", pk);
                    scripting.runScript(script, binding);
                }
            //scripting after end
            return pk;
        } catch (FxNotFoundException e) {
            ctx.setRollbackOnly();
            throw new FxCreateException(e);
        } catch (SQLException e) {
            ctx.setRollbackOnly();
//            if (Database.isUniqueConstraintViolation(e))
//                throw new FxEntryExistsException("ex.structure.property.exists", property.getName(), (parentXPath.length() == 0 ? "/" : parentXPath));
            throw new FxCreateException(LOG, e, "ex.db.sqlError", e.getMessage());
        } catch (FxInvalidParameterException e) {
            ctx.setRollbackOnly();
            throw new FxCreateException(e);
        } catch (Throwable t) {
            ctx.setRollbackOnly();
            throw new FxCreateException(LOG, t);
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, ps);
            if (!ctx.getRollbackOnly())
                CacheAdmin.expireCachedContent(content.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxPK createNewVersion(FxContent content) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            //security check start
            FxEnvironment env = CacheAdmin.getEnvironment();
            FxType type = env.getType(content.getTypeId());
            Step step = env.getStep(content.getStepId());
            UserTicket ticket = FxContext.get().getTicket();
            FxPermissionUtils.checkPermission(ticket, ticket.getUserId(), ACL.Permission.CREATE, type, step.getAclId(), content.getAclId(), true);
            //security check end
            ContentStorage storage = StorageManager.getContentStorage(content.getPk().getStorageMode());
            con = Database.getDbConnection();
            return storage.contentCreateVersion(con, CacheAdmin.getEnvironment(), null, content);
        } catch (SQLException e) {
            ctx.setRollbackOnly();
            throw new FxCreateException(LOG, e, "ex.db.sqlError", e.getMessage());
        } catch (FxApplicationException e) {
            ctx.setRollbackOnly();
            throw new FxCreateException(e);
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, ps);
            if (!ctx.getRollbackOnly())
                CacheAdmin.expireCachedContent(content.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxContent prepareSave(FxContent content) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            ContentStorage storage = StorageManager.getContentStorage(content.getPk().getStorageMode());
            con = Database.getDbConnection();
            storage.prepareSave(con, content);
            FxScriptBinding binding = null;
            long[] scripts;
            //scripting before start
            scripts = CacheAdmin.getEnvironment().getType(content.getTypeId()).getScriptMapping(
                    content.getPk().isNew()
                            ? FxScriptEvent.PrepareContentCreate
                            : FxScriptEvent.PrepareContentSave);
            if (scripts != null)
                for (long script : scripts) {
                    if (binding == null)
                        binding = new FxScriptBinding();
                    binding.setVariable("content", content);
                    Object result = scripting.runScript(script, binding).getResult();
                    if (result != null && result instanceof FxContent) {
//                        System.out.println("setting result");
                        content = (FxContent) result;
                    }
                }
            //scripting before end
            return content;
        } catch (FxNotFoundException e) {
            ctx.setRollbackOnly();
            throw new FxCreateException(e);
        } catch (SQLException e) {
            ctx.setRollbackOnly();
            throw new FxCreateException(LOG, e, "ex.db.sqlError", e.getMessage());
        } catch (FxInvalidParameterException e) {
            ctx.setRollbackOnly();
            throw new FxCreateException(e);
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, ps);
        }
    }


    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void remove(FxPK pk) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            ContentStorage storage = StorageManager.getContentStorage(pk.getStorageMode());
            con = Database.getDbConnection();
            //security check start
            if (!(FxContext.get().getRunAsSystem() || FxContext.get().getTicket().isGlobalSupervisor())) {
                FxContentVersionInfo cvi = storage.getContentVersionInfo(con, pk.getId());
                FxPK currPK;
                for (int currVer: cvi.getVersions()) {
                    currPK = new FxPK(pk.getId(), currVer);
                    FxContentSecurityInfo si = storage.getContentSecurityInfo(con, currPK);
                    FxPermissionUtils.checkPermission(FxContext.get().getTicket(), ACL.Permission.DELETE, si, true);
                }
            }
            //security check end
            FxScriptBinding binding = null;
            long[] scripts;
            FxContentSecurityInfo securityInfo = storage.getContentSecurityInfo(con, pk);
            long typeId = securityInfo.getTypeId();
            FxType type = CacheAdmin.getEnvironment().getType(typeId);

            //scripting before start
            //type scripting
            scripts = CacheAdmin.getEnvironment().getType(typeId).getScriptMapping(FxScriptEvent.BeforeContentRemove);
            if (scripts != null)
                for (long script : scripts) {
                    if (binding == null)
                        binding = new FxScriptBinding();
                    binding.setVariable("pk", pk);
                    if (!binding.getProperties().containsKey("securityInfo"))
                        binding.setVariable("securityInfo", securityInfo);
                    scripting.runScript(script, binding);
                }
            //assignment scripting
            if (type.hasScriptedAssignments()) {
                binding = new FxScriptBinding();
                binding.setVariable("pk", pk);
                for (FxAssignment as : type.getScriptedAssignments(FxScriptEvent.BeforeAssignmentDataDelete)) {
                    binding.setVariable("assignment", as);
                    for (long script : as.getScriptMapping(FxScriptEvent.BeforeAssignmentDataDelete)) {
                        scripting.runScript(script, binding);
                    }
                }
            }
            //scripting before end
            storage.contentRemove(con, pk);
            //scripting after start
            //assignment scripting
            if (type.hasScriptedAssignments()) {
                binding = new FxScriptBinding();
                binding.setVariable("pk", pk);
                for (FxAssignment as : type.getScriptedAssignments(FxScriptEvent.AfterAssignmentDataDelete)) {
                    binding.setVariable("assignment", as);
                    for (long script : as.getScriptMapping(FxScriptEvent.AfterAssignmentDataDelete)) {
                        scripting.runScript(script, binding);
                    }
                }
            }
            //type scripting
            scripts = CacheAdmin.getEnvironment().getType(typeId).getScriptMapping(FxScriptEvent.AfterContentRemove);
            if (scripts != null)
                for (long script : scripts) {
                    if (binding == null)
                        binding = new FxScriptBinding();
                    binding.setVariable("pk", pk);
                    if (!binding.getProperties().containsKey("securityInfo"))
                        binding.setVariable("securityInfo", securityInfo);
                    scripting.runScript(script, binding);
                }
            //scripting after end
        } catch (FxRemoveException e) {
            ctx.setRollbackOnly();
            throw e;
        } catch (FxNotFoundException e) {
            //not found is ok for removed instances
        } catch (SQLException e) {
            ctx.setRollbackOnly();
            throw new FxRemoveException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, ps);
            CacheAdmin.expireCachedContent(pk.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void removeVersion(FxPK pk) throws FxApplicationException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            ContentStorage storage = StorageManager.getContentStorage(pk.getStorageMode());
            con = Database.getDbConnection();
            //security check start
            FxContentSecurityInfo si = StorageManager.getContentStorage(pk.getStorageMode()).getContentSecurityInfo(con, pk);
            FxPermissionUtils.checkPermission(FxContext.get().getTicket(), ACL.Permission.DELETE, si, true);
            //security check end

            storage.contentRemoveVersion(con, pk);
        } catch (FxNotFoundException e) {
            ctx.setRollbackOnly();
            throw new FxRemoveException(e);
        } catch (SQLException e) {
            ctx.setRollbackOnly();
            throw new FxRemoveException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, ps);
            CacheAdmin.expireCachedContent(pk.getId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int removeForType(long typeId) throws FxApplicationException {
        Connection con = null;
        try {
            FxType type = CacheAdmin.getEnvironment().getType(typeId);
            ContentStorage storage = StorageManager.getContentStorage(type.getStorageMode());
            con = Database.getDbConnection();
            long[] scriptsBefore = null;
            long[] scriptsAfter = null;

            FxScriptBinding binding = null;
            UserTicket ticket = FxContext.get().getTicket();
            if (CacheAdmin.getEnvironment().getType(typeId).getScriptMapping(FxScriptEvent.BeforeContentRemove) != null)
                scriptsBefore = CacheAdmin.getEnvironment().getType(typeId).getScriptMapping(FxScriptEvent.BeforeContentRemove);
            if (CacheAdmin.getEnvironment().getType(typeId).getScriptMapping(FxScriptEvent.AfterContentRemove) != null)
                scriptsAfter = CacheAdmin.getEnvironment().getType(typeId).getScriptMapping(FxScriptEvent.AfterContentRemove);
            List<FxPK> pks = null;
            if (scriptsBefore != null || scriptsAfter != null || !ticket.isGlobalSupervisor()) {
                binding = new FxScriptBinding();
                if (!ticket.isGlobalSupervisor() || scriptsBefore != null) {
                    pks = storage.getPKsForType(con, type, false);
                    for (FxPK pk : pks) {
                        //security check start
                        FxContentSecurityInfo si = storage.getContentSecurityInfo(con, pk);
                        if (!ticket.isGlobalSupervisor())
                            FxPermissionUtils.checkPermission(FxContext.get().getTicket(), ACL.Permission.DELETE, si, true);
                        //security check end
                        //scripting before start
                        //type scripting
                        if (scriptsBefore != null) {
                            binding.setVariable("pk", pk);
                            binding.setVariable("securityInfo", si);
                            for (long script : scriptsBefore)
                                scripting.runScript(script, binding);
                        }
                        //assignment scripting
                        if (type.hasScriptedAssignments()) {
                            binding = new FxScriptBinding();
                            binding.setVariable("pk", pk);
                            for (FxAssignment as : type.getScriptedAssignments(FxScriptEvent.BeforeAssignmentDataDelete)) {
                                binding.setVariable("assignment", as);
                                for (long script : as.getScriptMapping(FxScriptEvent.BeforeAssignmentDataDelete)) {
                                    scripting.runScript(script, binding);
                                }
                            }
                        }
                        //scripting before end
                    }
                }
            }
            int removed = storage.contentRemoveForType(con, type);
            if (scriptsAfter != null) {
                if (pks == null)
                    pks = storage.getPKsForType(con, type, false);
                for (FxPK pk : pks) {
                    binding.getProperties().remove("securityInfo");
                    //scripting after start
                    //assignment scripting
                    if (type.hasScriptedAssignments()) {
                        binding = new FxScriptBinding();
                        binding.setVariable("pk", pk);
                        for (FxAssignment as : type.getScriptedAssignments(FxScriptEvent.AfterAssignmentDataDelete)) {
                            binding.setVariable("assignment", as);
                            for (long script : as.getScriptMapping(FxScriptEvent.AfterAssignmentDataDelete)) {
                                scripting.runScript(script, binding);
                            }
                        }
                    }
                    //type scripting
                    if (scriptsBefore != null) {
                        binding.setVariable("pk", pk);
                        for (long script : scriptsAfter)
                            scripting.runScript(script, binding);
                    }
                    //scripting after end
                }
            }
            return removed;
        } catch (FxNotFoundException e) {
            ctx.setRollbackOnly();
            throw new FxRemoveException(e);
        } catch (SQLException e) {
            ctx.setRollbackOnly();
            throw new FxRemoveException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, null);
            CacheAdmin.expireCachedContents();
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxContentSecurityInfo getContentSecurityInfo(FxPK pk) throws FxApplicationException {
        FxSharedUtils.checkParameterNull(pk, "pk");
        Connection con = null;
        try {
            ContentStorage storage = StorageManager.getContentStorage(pk.getStorageMode());
            con = Database.getDbConnection();
            return storage.getContentSecurityInfo(con, pk);
        } catch (FxNotFoundException e) {
            throw new FxLoadException(e);
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public FxContentVersionInfo getContentVersionInfo(FxPK pk) throws FxApplicationException {
        FxSharedUtils.checkParameterNull(pk, "pk");
        Connection con = null;
        try {
            ContentStorage storage = StorageManager.getContentStorage(pk.getStorageMode());
            con = Database.getDbConnection();
            return storage.getContentVersionInfo(con, pk.getId());
        } catch (FxNotFoundException e) {
            throw new FxLoadException(e);
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public int getReferencedContentCount(FxPK pk) throws FxApplicationException {
        FxSharedUtils.checkParameterNull(pk, "pk");
        Connection con = null;
        try {
            ContentStorage storage = StorageManager.getContentStorage(pk.getStorageMode());
            con = Database.getDbConnection();
            return storage.getReferencedContentCount(con, pk.getId());
        } catch (FxNotFoundException e) {
            throw new FxLoadException(e);
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public List<FxPK> getPKsForType(long typeId, boolean onePkPerInstance) throws FxApplicationException {
        if (!FxContext.get().getTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.content.type.getAll.noPermission");
        }
        Connection con = null;
        try {
            con = Database.getDbConnection();
            return StorageManager.getContentStorage(TypeStorageMode.Hierarchical).getPKsForType(con,
                    CacheAdmin.getEnvironment().getType(typeId), onePkPerInstance);
        } catch (Exception e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, null);
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public long getBinaryId(FxPK pk, String xpath, FxLanguage language) throws FxApplicationException {
        FxSharedUtils.checkParameterNull(pk, "pk");
        FxSharedUtils.checkParameterNull(xpath, "xpath");
        Connection con = null;
        try {
            ContentStorage storage = StorageManager.getContentStorage(pk.getStorageMode());
            con = Database.getDbConnection();
            FxContent co = load(pk);
            UserTicket ticket = FxContext.get().getTicket();
            if (xpath.equals("/")) {
                if (!ticket.isGlobalSupervisor()) {
                    FxType type = CacheAdmin.getEnvironment().getType(co.getTypeId());
                    if (type.usePermissions()) {
                        FxContentSecurityInfo si = storage.getContentSecurityInfo(con, pk);
                        FxPermissionUtils.checkPermission(ticket, ACL.Permission.READ, si, true);
                    }
                }
                return co.getBinaryPreviewId();
            }
            FxPropertyData pd = co.getPropertyData(xpath);
            if (!pd.getValue().isEmpty() && pd.getValue() instanceof FxBinary)
                return ((FxBinary) pd.getValue()).getBestTranslation(language != null ? language : ticket.getLanguage()).getId();
            throw new FxInvalidParameterException("XPATH", "ex.content.binary.xpath.invalid", xpath);
        } catch (FxNotFoundException e) {
            throw new FxLoadException(e);
        } catch (SQLException e) {
            throw new FxLoadException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            Database.closeObjects(ContentEngineBean.class, con, null);
        }
    }
}
