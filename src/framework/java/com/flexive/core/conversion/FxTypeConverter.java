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
package com.flexive.core.conversion;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_STRUCT_TYPES;
import com.flexive.core.LifeCycleInfoImpl;
import com.flexive.core.structure.FxPreloadType;
import com.flexive.core.structure.StructureLoader;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.content.FxPermissionUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.scripting.FxScriptMappingEntry;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.LifeCycleInfo;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.workflow.Workflow;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.lang.ArrayUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

/**
 * XStream converter for FxType
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxTypeConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxType type = ((FxType) o);
        FxEnvironment env = CacheAdmin.getEnvironment();
        try {
            writer.addAttribute("name", type.getName());
            writer.addAttribute("derived", String.valueOf(type.isDerived()));
            if (type.isDerived())
                writer.addAttribute("parent", type.getParent().getName());

            writer.addAttribute("acl", type.getACL().getName());
            writer.addAttribute("category", type.getCategory().name());
            writer.addAttribute("languageMode", type.getLanguage().name());
            writer.addAttribute("maxVersions", String.valueOf(type.getMaxVersions()));
            writer.addAttribute("mode", type.getMode().name());
            writer.addAttribute("state", type.getState().name());
            writer.addAttribute("storageMode", type.getStorageMode().name());
            writer.addAttribute("workflow", type.getWorkflow().getName());
            writer.addAttribute("permType", String.valueOf(type.useTypePermissions()));
            writer.addAttribute("permProp", String.valueOf(type.usePropertyPermissions()));
            writer.addAttribute("permInst", String.valueOf(type.useInstancePermissions()));
            writer.addAttribute("permStep", String.valueOf(type.useStepPermissions()));

            writer.addAttribute("trackHistory", String.valueOf(type.isTrackHistory()));
            if (type.isTrackHistory())
                writer.addAttribute("historyAge", String.valueOf(type.getHistoryAge()));

            if (type.isRelation()) {
                writer.startNode("relations");
                writer.addAttribute("maxSrc", String.valueOf(type.getMaxRelSource()));
                writer.addAttribute("maxDst", String.valueOf(type.getMaxRelDestination()));
                for (FxTypeRelation rel : type.getRelations()) {
                    writer.startNode("relation");
                    writer.addAttribute("src", rel.getSource().getName());
                    writer.addAttribute("maxSrc", String.valueOf(rel.getMaxSource()));
                    writer.addAttribute("dst", rel.getDestination().getName());
                    writer.addAttribute("maxDst", String.valueOf(rel.getMaxDestination()));
                    writer.endNode();
                }
                writer.endNode();
            }

            writer.startNode("label");
            ctx.convertAnother(type.getLabel());
            writer.endNode();

            ctx.convertAnother(type.getLifeCycleInfo());

            if (!type.getScriptEvents().isEmpty()) {
                writer.startNode("scriptEvents");
                for (FxScriptEvent event : type.getScriptEvents()) {
                    for (long script : type.getScriptMapping(event)) {
                        writer.startNode("scriptMapping");
                        writer.addAttribute("event", event.name());
                        writer.addAttribute("script", env.getScript(script).getName());
                        for (FxScriptMappingEntry sme : env.getScriptMapping(script).getMappedTypes()) {
                            if (sme.getId() == type.getId() || ArrayUtils.contains(sme.getDerivedIds(), type.getId())) {
                                writer.addAttribute("active", String.valueOf(sme.isActive()));
                                writer.addAttribute("derivedUsage", String.valueOf(sme.isDerivedUsage()));
                                writer.addAttribute("derived", String.valueOf(sme.getId() != type.getId()));
                                if (sme.getId() != type.getId())
                                    writer.addAttribute("baseType", env.getType(sme.getId()).getName());
                                break;
                            }
                        }
                        writer.endNode();
                    }
                }
                writer.endNode();
            }

            writer.startNode("assignments");
            for (FxAssignment as : type.getConnectedAssignments("/")) {
                if (as.isSystemInternal())
                    continue;
                ctx.convertAnother(as);
            }
            writer.endNode();

        } catch (Exception e) {
            throw new FxConversionException(e, "ex.conversion.type.error", type.getName(), e.getMessage()).asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        final FxEnvironment env = CacheAdmin.getEnvironment();
        String name = reader.getAttribute("name");
        boolean existing = env.typeExists(name);
        FxTypeEdit typeEdit = existing ? env.getType(name).asEditable() : null;
        final TypeEngine typeEngine = EJBLookup.getTypeEngine();

        FxType parent = null;
        if (Boolean.valueOf(reader.getAttribute("derived"))) {
            //this value can not be changed for existing types
            parent = env.getType(reader.getAttribute("parent"));
        }
        ACL acl = env.getACL(reader.getAttribute("acl"));
        TypeCategory cat = TypeCategory.valueOf(reader.getAttribute("category"));
        LanguageMode langMode = LanguageMode.valueOf(reader.getAttribute("languageMode"));
        long maxVersions = Long.valueOf(reader.getAttribute("maxVersions"));
        TypeMode mode = TypeMode.valueOf(reader.getAttribute("mode"));
        TypeState state = TypeState.valueOf(reader.getAttribute("state"));
        TypeStorageMode storageMode = TypeStorageMode.valueOf(reader.getAttribute("storageMode"));
        Workflow workflow = env.getWorkflow(reader.getAttribute("workflow"));
        byte permissions = FxPermissionUtils.encodeTypePermissions(
                Boolean.valueOf(reader.getAttribute("permInst")),
                Boolean.valueOf(reader.getAttribute("permProp")),
                Boolean.valueOf(reader.getAttribute("permStep")),
                Boolean.valueOf(reader.getAttribute("permType")));
        boolean trackHistory = Boolean.valueOf(reader.getAttribute("trackHistory"));
        long historyAge = 0;
        if (trackHistory)
            historyAge = Long.valueOf(reader.getAttribute("historyAge"));
        int maxRelSource = -1;
        int maxRelDest = -1;
        FxString label = null;
        List<FxTypeScriptImportMapping> scriptMapping = null;
        if (!existing) {
            typeEdit = FxTypeEdit.createNew(name, label, acl, workflow, parent, false/*irrelevant*/,
                    storageMode, cat, mode, langMode, state, permissions, trackHistory, historyAge, maxVersions,
                    maxRelSource, maxRelDest);
            typeEdit.setLifeCycleInfo(LifeCycleInfoImpl.createNew(FxContext.getUserTicket()));
        }
        if (existing) {
            typeEdit.setACL(acl);
            typeEdit.setCategory(cat);
            typeEdit.setLanguage(langMode);
            typeEdit.setMaxVersions(maxVersions);
            typeEdit.setMode(mode);
            typeEdit.setState(state);
            //storage mode can not be changed for existing types (yet)
            typeEdit.setWorkflow(workflow);
            typeEdit.setPermissions(permissions);
            typeEdit.setTrackHistory(trackHistory);
            typeEdit.setHistoryAge(historyAge);
        }
        ctx.put(ConversionEngine.KEY_TYPE, typeEdit);
        boolean processAssignments = false;

        while (reader.hasMoreChildren()) {
            reader.moveDown();
            String node = reader.getNodeName();
            if ("relations".equals(node)) {
                maxRelSource = Integer.valueOf(reader.getAttribute("maxSrc"));
                maxRelDest = Integer.valueOf(reader.getAttribute("maxDst"));
                typeEdit.setMaxRelSource(maxRelSource);
                typeEdit.setMaxRelDestination(maxRelDest);
            } else if ("relation".equals(node)) {
                try {
                    typeEdit.addRelation(new FxTypeRelation(
                            new FxPreloadType(reader.getAttribute("src")),
                            new FxPreloadType(reader.getAttribute("dst")),
                            Long.valueOf(reader.getAttribute("maxSrc")),
                            Long.valueOf(reader.getAttribute("maxDst"))
                    ));
                } catch (FxInvalidParameterException e) {
                    throw e.asRuntimeException();
                }
            } else if ("label".equals(node)) {
                typeEdit.setLabel((FxString) ctx.convertAnother(this, FxValue.class));
            } else if (ConversionEngine.KEY_LCI.equals(node)) {
                ctx.convertAnother(this, LifeCycleInfo.class);
            } else if ("scriptEvents".equals(node)) {
                scriptMapping = new ArrayList<FxTypeScriptImportMapping>(5);
                while (reader.hasMoreChildren()) {
                    reader.moveDown();
                    if( !"scriptMapping".equals(reader.getNodeName()))
                        throw new FxConversionException("ex.conversion.wrongNode", "scriptMapping", reader.getNodeName()).asRuntimeException();
                    final Boolean derived = Boolean.valueOf(reader.getAttribute("derived"));
                    scriptMapping.add(new FxTypeScriptImportMapping(FxScriptEvent.valueOf(reader.getAttribute("event")),
                            reader.getAttribute("script"),
                            Boolean.valueOf(reader.getAttribute("active")),
                            Boolean.valueOf(reader.getAttribute("derivedUsage")),
                            derived,
                            derived ? reader.getAttribute("baseType") : ""));
                    reader.moveUp();
                }
            } else if ("assignments".equals(node)) {
                processAssignments = true;
                break;
            }
            if (!processAssignments)
                reader.moveUp();
        }

        try {
            long typeId = typeEngine.save(typeEdit);
            //appy LifeCycleInfos
            Connection con = null;
            PreparedStatement ps = null;
            StringBuilder sql = new StringBuilder(500);
            try {
                con = Database.getDbConnection();
                sql.append("UPDATE ").append(TBL_STRUCT_TYPES).append(
                        " SET CREATED_BY=?, CREATED_AT=?, MODIFIED_BY=?, MODIFIED_AT=? WHERE ID=?");
                ps = con.prepareStatement(sql.toString());
                ps.setLong(1, typeEdit.getLifeCycleInfo().getCreatorId());
                ps.setLong(2, typeEdit.getLifeCycleInfo().getCreationTime());
                ps.setLong(3, typeEdit.getLifeCycleInfo().getModificatorId());
                ps.setLong(4, typeEdit.getLifeCycleInfo().getModificationTime());
                ps.setLong(5, typeId);
                ps.executeUpdate();
                StructureLoader.reload(con);
            } catch (Exception e) {
                throw new FxApplicationException(e, "ex.conversion.type.error", typeEdit.getName(), e.getMessage()).asRuntimeException();
            } finally {
                Database.closeObjects(FxTypeConverter.class, con, ps);
            }
            typeEdit = CacheAdmin.getEnvironment().getType(typeId).asEditable();
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        ctx.put(ConversionEngine.KEY_TYPE, typeEdit);

        try {
            //save and reload
            if (scriptMapping != null) {
                typeEdit = CacheAdmin.getEnvironment().getType(typeEngine.save(typeEdit)).asEditable();
                final ScriptingEngine scriptingEngine = EJBLookup.getScriptingEngine();
                //remove all script assignments
                for (FxScriptEvent ev : typeEdit.getScriptEvents())
                    for (long scriptId : typeEdit.getScriptMapping(ev)) {
                        scriptingEngine.removeTypeScriptMapping(scriptId, typeEdit.getId());
                    }
                //and re-add them

                for (FxTypeScriptImportMapping sm : scriptMapping) {
                    scriptingEngine.createTypeScriptMapping(sm.getEvent(),
                            env.getScript(sm.getScriptName()).getId(), typeEdit.getId(),
                            sm.isActive(), sm.isDerivedUsage());
                }
                try {
                    StructureLoader.reload(null);
                } catch (FxCacheException e) {
                    throw new FxApplicationException(e, "ex.conversion.type.error", typeEdit.getName(), e.getMessage()).asRuntimeException();
                }
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }

        if (processAssignments) {
            while (reader.hasMoreChildren() || (processAssignments && reader.hasMoreChildren())) {
                reader.moveDown();
                String node = reader.getNodeName();
                if (ConversionEngine.KEY_PROPERTY_AS.equals(node)) {
                    ctx.convertAnother(this, FxPropertyAssignment.class);
                } else if (ConversionEngine.KEY_GROUP_AS.equals(node)) {
                    ctx.convertAnother(this, FxGroupAssignment.class);
                } else
                    throw new FxConversionException("ex.conversion.unexcpectedNode", reader.getNodeName()).asRuntimeException();
                reader.moveUp();
            }
            reader.moveUp(); //move up the final assignments closing node
        }
        return typeEdit;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class type) {
        return FxType.class.isAssignableFrom(type);
    }
}
