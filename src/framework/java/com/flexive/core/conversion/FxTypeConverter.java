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
package com.flexive.core.conversion;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxTypeRelation;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream converter for FxType
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxTypeConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxType type = ((FxType) o);
        FxEnvironment env = CacheAdmin.getEnvironment();
        try {
            writer.addAttribute("name", type.getDisplayName());
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
                        writer.startNode("mapping");
                        writer.addAttribute("event", event.name());
                        writer.addAttribute("script", env.getScript(script).getName());
                        writer.endNode();
                    }
                }
                writer.endNode();
            }

            for( FxAssignment as: type.getConnectedAssignments("/") ) {
                if( as.isSystemInternal() )
                    continue;
                ctx.convertAnother(as);
            }

//        } catch (FxApplicationException e) {
        } catch (Exception e) {
//            throw e.asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        //TODO
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class type) {
        return FxType.class.isAssignableFrom(type);
    }
}
