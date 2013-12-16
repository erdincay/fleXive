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
package com.flexive.core.conversion;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxGroupData;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.security.LifeCycleInfo;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.workflow.Step;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

import static com.flexive.shared.FxSharedUtils.*;

/**
 * XStream converter for FxContent
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxContentConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxContent co = ((FxContent) o).copy(); //remove/compacts will be performed -> so work on a copy
        FxEnvironment env = CacheAdmin.getEnvironment();
        ctx.put("pk", co.getPk());
        writer.addAttribute("pk", co.getPk().toString());
        writer.addAttribute("type", env.getType(co.getTypeId()).getName());
        writer.addAttribute("mandator", env.getMandator(co.getMandatorId()).getName());
        writer.addAttribute("acls", StringUtils.join(
                getSelectableObjectNameList(
                        filterSelectableObjectsById(env.getACLs(), co.getAclIds())
                ), ','
        ));
        final Step step = env.getStep(co.getStepId());
        final String stepName = env.getStepDefinition(step.getStepDefinitionId()).getName();
        final String wfName = env.getWorkflow(step.getWorkflowId()).getName();
        writer.addAttribute("workflow", wfName);
        writer.addAttribute("step", stepName);
        writer.addAttribute("mainLanguage", CacheAdmin.getEnvironment().getLanguage(co.getMainLanguage()).getIso2digit());
        writer.addAttribute("relation", String.valueOf(co.isRelation()));
        if (co.isRelation()) {
            writer.startNode("relation");
            writer.startNode("source");
            writer.addAttribute("pk", co.getRelatedSource().toString());
            writer.addAttribute("pos", String.valueOf(co.getRelatedSourcePosition()));
            writer.endNode();
            writer.startNode("destination");
            writer.addAttribute("pk", co.getRelatedDestination().toString());
            writer.addAttribute("pos", String.valueOf(co.getRelatedDestinationPosition()));
            writer.endNode();
            writer.endNode();
        }
        ctx.convertAnother(co.getLifeCycleInfo());
        ctx.convertAnother(co.getRootGroup());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        FxEnvironment env = CacheAdmin.getEnvironment();
        FxContent co;
        try {
            co = EJBLookup.getContentEngine().initialize(env.getType(reader.getAttribute("type")).getId(),
                    env.getMandator(reader.getAttribute("mandator")).getId(),
                    -1,
                    env.getStep(env.getWorkflow(reader.getAttribute("workflow")).getId(), reader.getAttribute("step")).getId(),
                    CacheAdmin.getEnvironment().getLanguage(reader.getAttribute("mainLanguage")).getId());
            co.setAclIds(
                    getSelectableObjectIdList(
                        filterSelectableObjectsByName(
                                env.getACLs(),
                                Arrays.asList(StringUtils.split(reader.getAttribute("acls"), ','))
                        )
                    )
            );
            boolean relation = Boolean.valueOf(reader.getAttribute("relation"));
            if (relation) {
                //TODO: resolve relation pk's after import
                reader.moveDown(); //relation
                reader.moveDown(); //source
                co.setRelatedSource(FxPK.fromString(reader.getAttribute("pk")));
                co.setRelatedSourcePosition(Integer.valueOf(reader.getAttribute("pos")));
                reader.moveUp(); //source
                reader.moveDown(); //destination
                co.setRelatedDestination(FxPK.fromString(reader.getAttribute("pk")));
                co.setRelatedDestinationPosition(Integer.valueOf(reader.getAttribute("pos")));
                reader.moveUp(); //destination
                reader.moveUp(); //relation
            }
            ctx.put(ConversionEngine.KEY_CONTENT, co);
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if (ConversionEngine.KEY_LCI.equals(reader.getNodeName())) {
                    ctx.convertAnother(this, LifeCycleInfo.class);
                } else if (ConversionEngine.KEY_GROUP.equals(reader.getNodeName())) {
                    ctx.convertAnother(this, FxGroupData.class);
                }
                reader.moveUp();
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        return co;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvert(Class aClass) {
        return FxContent.class.isAssignableFrom(aClass);
    }


}
