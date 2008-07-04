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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.XPathElement;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.structure.*;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.List;

/**
 * XStream converter for FxGroupAssignment
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxGroupAssignmentConverter extends FxAssignmentConverter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        writer.startNode(ConversionEngine.KEY_GROUP_AS);
        FxGroupAssignment grp = (FxGroupAssignment) o;
        writer.addAttribute("mode", grp.getMode().name());
        super.marshal(o, writer, ctx);
        if (!grp.isDerivedAssignment()) {
            writer.startNode(ConversionEngine.KEY_GROUP);
            writer.addAttribute("name", grp.getGroup().getName());
            writer.addAttribute("multiplicity", grp.getGroup().getMultiplicity().toString());

            writer.startNode("label");
            ctx.convertAnother(grp.getGroup().getLabel());
            writer.endNode();
            writer.startNode("hint");
            ctx.convertAnother(grp.getGroup().getHint());
            writer.endNode();

            marshallOptions(writer, grp.getGroup().getOptions());
            writer.endNode();
        }
        for (FxAssignment as : grp.getAssignments())
            ctx.convertAnother(as);
        writer.endNode();
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        FxType type = (FxType) ctx.get(ConversionEngine.KEY_TYPE);
        GroupMode mode = GroupMode.valueOf(reader.getAttribute("mode"));
        AssignmentData data = (AssignmentData) super.unmarshal(reader, ctx);
        FxEnvironment env = CacheAdmin.getEnvironment();
        boolean isDerived = data.getParentAssignment() != null;
        List<FxStructureOption> options = unmarshallOptions(reader, ctx);
        String parentXPath;
        try {
            List<XPathElement> xpe = XPathElement.split(data.getXpath());
            if (xpe.size() > 0)
                xpe.remove(xpe.size() - 1);
            parentXPath = XPathElement.toXPathNoMult(xpe);
        } catch (FxInvalidParameterException e) {
            throw e.asRuntimeException();
        }

        FxGroupAssignmentEdit grp = null;

        if (env.assignmentExists(data.getXpath()))
            grp = ((FxGroupAssignment) env.getAssignment(data.getXpath())).asEditable();
        else {
            if (isDerived) {
                //if the group that this group is derived from exists, derive from it again
                if (env.assignmentExists(data.getParentAssignment())) {
                    FxGroupAssignment ga = ((FxGroupAssignment) env.getAssignment(data.getParentAssignment()));
                    try {
                        grp = FxGroupAssignmentEdit.createNew(ga, type, data.getAlias(), parentXPath);
                        long groupId = EJBLookup.getAssignmentEngine().save(grp, false);
                        env = CacheAdmin.getEnvironment();
                        grp = ((FxGroupAssignment) env.getAssignment(groupId)).asEditable();
                    } catch (FxApplicationException e) {
                        throw e.asRuntimeException();
                    }
                }
            }
        }

        if (grp == null) {
            //create a new one
            try {
                long groupId = EJBLookup.getAssignmentEngine().createGroup(
                        FxGroupEdit.createNew(data.getAlias(), data.getLabel(), data.getHint(), true, data.getMultiplicity()),
                        parentXPath);
                env = CacheAdmin.getEnvironment();
                grp = ((FxGroupAssignment) env.getAssignment(groupId)).asEditable();
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }

        //now that we have the assignment, apply all options
        try {
            grp.setOptions(data.getOptions());
            grp.setEnabled(data.isEnabled());
            grp.setLabel(data.getLabel());
            grp.setHint(data.getHint());
            grp.setMode(mode);
            grp.setMultiplicity(data.getMultiplicity());
            grp.setPosition(data.getPos());
            EJBLookup.getAssignmentEngine().save(grp, false);
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        env = CacheAdmin.getEnvironment(); //refresh environment
        ctx.put(ConversionEngine.KEY_TYPE, env.getType(type.getId()).asEditable());

        while (reader.hasMoreChildren()) {
            reader.moveDown();
            if (ConversionEngine.KEY_GROUP_AS.equals(reader.getNodeName()))
                ctx.convertAnother(this, FxGroupAssignment.class);
            else if (ConversionEngine.KEY_PROPERTY_AS.equals(reader.getNodeName()))
                ctx.convertAnother(this, FxPropertyAssignment.class);
            else
                throw new FxConversionException("ex.conversion.unexcpectedNode", reader.getNodeName()).asRuntimeException();
            reader.moveUp();
        }
        return CacheAdmin.getEnvironment().getAssignment(grp.getId());
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class type) {
        return FxGroupAssignment.class.isAssignableFrom(type);
    }
}
