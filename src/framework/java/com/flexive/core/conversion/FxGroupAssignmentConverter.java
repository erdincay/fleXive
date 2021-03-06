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
import com.flexive.shared.XPathElement;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
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
    @Override
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
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        FxType type = (FxType) ctx.get(ConversionEngine.KEY_TYPE);
        GroupMode mode = GroupMode.valueOf(reader.getAttribute("mode"));
        AssignmentData data = (AssignmentData) super.unmarshal(reader, ctx);
        FxEnvironment env = CacheAdmin.getEnvironment();
        boolean isDerived = data.getParentAssignment() != null;
        String parentXPath;
        List<XPathElement> xpe = XPathElement.splitNew(data.getXpath());
        if (xpe.size() > 0)
            xpe.remove(xpe.size() - 1);
        parentXPath = XPathElement.toXPathNoMult(xpe);

        final AssignmentEngine assignmentEngine = EJBLookup.getAssignmentEngine();
        FxGroupAssignmentEdit grp = null;

        String grpName = null;
        FxMultiplicity grpMult = null;
        FxString grpLabel = null;
        FxString grpHint = null;

        if (!isDerived) {
            //read the group information
            reader.moveDown();
            //only allowed child is the property if it is not derived
            if (!ConversionEngine.KEY_GROUP.equals(reader.getNodeName())) {
                throw new FxConversionException("ex.conversion.wrongNode", ConversionEngine.KEY_GROUP, reader.getNodeName()).asRuntimeException();
            }
            //read property data and create if needed
            grpName = reader.getAttribute("name");
            grpMult = FxMultiplicity.fromString(reader.getAttribute("multiplicity"));
            grpLabel = (FxString) ConversionEngine.getFxValue("label", this, reader, ctx);
            grpHint = (FxString) ConversionEngine.getFxValue("hint", this, reader, ctx);
            //grpOptions = super.unmarshallOptions(reader, ctx);
            super.unmarshallOptions(reader, ctx);
            reader.moveUp();
        }


        if (env.assignmentExists(data.getXpath()))
            grp = ((FxGroupAssignment) env.getAssignment(data.getXpath())).asEditable();
        else {
            if (isDerived) {
                //if the group that this group is derived from exists, derive from it again
                if (env.assignmentExists(data.getParentAssignment())) {
                    FxGroupAssignment ga = ((FxGroupAssignment) env.getAssignment(data.getParentAssignment()));
                    try {
                        grp = FxGroupAssignmentEdit.createNew(ga, type, data.getAlias(), parentXPath);
                        long groupId = assignmentEngine.save(grp, false);
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
                long groupId;
                if (env.groupExists(data.getAlias())) {
                    groupId = env.getGroup(data.getAlias()).getId();
                    //reuse a group from another assignment!
                    List<FxGroupAssignment> assignments = CacheAdmin.getFilteredEnvironment().getGroupAssignments(true);
                    FxGroupAssignment reuse = null;
                    for (FxGroupAssignment assignment : assignments) {
                        if (assignment.getGroup().getId() == groupId && !assignment.isSystemInternal()) {
                            reuse = assignment;
                            break;
                        }
                    }
                    if (reuse != null) {
                        //reuse the group but not the group assignment
                        FxGroupAssignmentEdit gedit = FxGroupAssignmentEdit.createNew(reuse, type, data.getAlias(), parentXPath);
                        gedit.clearDerivedUse();
                        long aid = assignmentEngine.save(gedit, false);
                        env = CacheAdmin.getEnvironment();
                        grp = ((FxGroupAssignment) env.getAssignment(aid)).asEditable();
                    } else {
                        throw new IllegalStateException("Internal error - reused group not found: " + data.getAlias());
                    }
                } else {
                    groupId = assignmentEngine.createGroup(
                            isDerived
                                    ? FxGroupEdit.createNew(data.getAlias(), data.getLabel(), data.getHint(), true, data.getMultiplicity())
                                    : FxGroupEdit.createNew(grpName, grpLabel, grpHint, true, grpMult),
                            parentXPath);
                    env = CacheAdmin.getEnvironment();
                    grp = ((FxGroupAssignment) env.getAssignment(groupId)).asEditable();
                }
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
            assignmentEngine.save(grp, false);
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
    @Override
    public boolean canConvert(Class type) {
        return FxGroupAssignment.class.isAssignableFrom(type);
    }
}
