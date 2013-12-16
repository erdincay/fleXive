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
import com.flexive.shared.security.ACL;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * XStream converter for FxPropertyAssignment
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxPropertyAssignmentConverter extends FxAssignmentConverter {

    /**
     * {@inheritDoc}
     */
    @Override
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        writer.startNode(ConversionEngine.KEY_PROPERTY_AS);
        FxPropertyAssignment prop = (FxPropertyAssignment) o;
        final FxProperty p = prop.getProperty();
        writer.addAttribute("property", p.getName());
        writer.addAttribute("acl", prop.getACL().getName());
        try {
            writer.addAttribute("defaultLanguage", ConversionEngine.getLang(CacheAdmin.getEnvironment(), prop.getDefaultLanguage()));
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        super.marshal(o, writer, ctx);
        if (prop.hasAssignmentDefaultValue()) {
            writer.startNode("defaultValue");
            ctx.convertAnother(prop.getDefaultValue());
            writer.endNode();
        }
        if (!prop.isDerivedAssignment()) {
            writer.startNode(ConversionEngine.KEY_PROPERTY);
            writer.addAttribute("name", p.getName());
            writer.addAttribute("multiplicity", p.getMultiplicity().toString());
            writer.addAttribute("dataType", p.getDataType().name());
            writer.addAttribute("acl", p.getACL().getName());
            writer.addAttribute("overrideACL", String.valueOf(p.mayOverrideACL()));
            writer.addAttribute("uniqueMode", p.getUniqueMode().name());
            if (p.hasReferencedType())
                writer.addAttribute("refType", p.getReferencedType().getName());
            if (p.hasReferencedList())
                writer.addAttribute("refList", p.getReferencedList().getName());

            writer.startNode("label");
            ctx.convertAnother(p.getLabel());
            writer.endNode();
            writer.startNode("hint");
            ctx.convertAnother(p.getHint());
            writer.endNode();

            if (p.isDefaultValueSet()) {
                writer.startNode("defaultValue");
                ctx.convertAnother(p.getDefaultValue());
                writer.endNode();
            }

            marshallOptions(writer, p.getOptions());
            writer.endNode();
        }
        writer.endNode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        FxType type = (FxType) ctx.get(ConversionEngine.KEY_TYPE);
        String property = reader.getAttribute(ConversionEngine.KEY_PROPERTY);
        FxEnvironment env = CacheAdmin.getEnvironment();
        ACL acl = env.getACL(reader.getAttribute("acl"));
        long defaultLanguage;
        try {
            defaultLanguage = ConversionEngine.getLang(CacheAdmin.getEnvironment(), reader.getAttribute("defaultLanguage"));
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        AssignmentData data = (AssignmentData) super.unmarshal(reader, ctx);
        String parentXPath;
        List<XPathElement> xpe = XPathElement.split(data.getXpath());
        if (xpe.size() > 0)
            xpe.remove(xpe.size() - 1);
        parentXPath = XPathElement.toXPathNoMult(xpe);
        FxValue defaultValue = null;//ConversionEngine.getFxValue("defaultValue", this, reader, ctx);

        while (reader.hasMoreChildren()) { //optional property and default value as subnodes
            reader.moveDown();
            if ("defaultValue".equals(reader.getNodeName())) {
                defaultValue = (FxValue) ctx.convertAnother(this, FxValue.class);
                reader.moveUp();
                continue;
            }
            //only allowed child is the property if it is not derived
            if (!ConversionEngine.KEY_PROPERTY.equals(reader.getNodeName()))
                throw new FxConversionException("ex.conversion.wrongNode", ConversionEngine.KEY_PROPERTY, reader.getNodeName()).asRuntimeException();
            //read property data and create if needed
            String propName = reader.getAttribute("name");
            FxMultiplicity propMult = FxMultiplicity.fromString(reader.getAttribute("multiplicity"));
            FxDataType propDataType = FxDataType.valueOf(reader.getAttribute("dataType"));
            ACL propACL = env.getACL(reader.getAttribute("acl"));
            boolean propOverrideACL = Boolean.valueOf(reader.getAttribute("overrideACL"));
            UniqueMode propUniqueMode = UniqueMode.valueOf(reader.getAttribute("uniqueMode"));
            FxType refType = null;
            if (!StringUtils.isEmpty(reader.getAttribute("refType")))
                refType = env.getType(reader.getAttribute("refType"));
            FxString propLabel = (FxString) ConversionEngine.getFxValue("label", this, reader, ctx);
            FxString propHint = (FxString) ConversionEngine.getFxValue("hint", this, reader, ctx);
            List<FxStructureOption> options = null;
            FxValue propDefaultValue = null;//ConversionEngine.getFxValue("defaultValue", this, reader, ctx);
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if ("defaultValue".equals(reader.getNodeName())) {
                    defaultValue = (FxValue) ctx.convertAnother(this, FxValue.class);
                } else if ("options".equals(reader.getNodeName())) {
                    options = super.unmarshallOptions(reader, ctx);
                } else
                    throw new FxConversionException("ex.conversion.unexcpectedNode", reader.getNodeName()).asRuntimeException();
                reader.moveUp();
            }
            if (env.propertyExists(propName)) {
                FxPropertyEdit prop = env.getProperty(propName).asEditable();
                prop.setACL(propACL);
                prop.setAssignmentDefaultMultiplicity(data.getDefaultMultiplicity());
                prop.setHint(propHint);
                prop.setLabel(propLabel);
                prop.setOverrideACL(propOverrideACL);
                prop.setUniqueMode(propUniqueMode);
                prop.setDefaultValue(propDefaultValue);
                prop.setOptions(options);
                if (refType != null)
                    prop.setReferencedType(refType);
                try {
                    EJBLookup.getAssignmentEngine().save(prop);
                    env = CacheAdmin.getEnvironment(); //refresh environment
                    ctx.put(ConversionEngine.KEY_TYPE, env.getType(type.getId()).asEditable());
                    type = (FxType) ctx.get(ConversionEngine.KEY_TYPE);
                } catch (FxApplicationException e) {
                    throw e.asRuntimeException();
                }
                if (env.assignmentExists(data.getParentAssignment()) && !type.isXPathValid(data.getXpath(), true)) {
                    //reuse it
                    try {
                        EJBLookup.getAssignmentEngine().save(
                                FxPropertyAssignmentEdit.reuse(data.getParentAssignment(), type.getName(), type.getName() + "/" + parentXPath,
                                        data.getAlias()),
                                false);
                        env = CacheAdmin.getEnvironment(); //refresh environment
                        ctx.put(ConversionEngine.KEY_TYPE, env.getType(type.getId()).asEditable());
                        type = (FxType) ctx.get(ConversionEngine.KEY_TYPE);
                    } catch (FxApplicationException e) {
                        throw e.asRuntimeException();
                    }
                }
            } else {
                //create new assignment and property
                FxPropertyEdit prop = FxPropertyEdit.createNew(propName, propLabel, propHint, propMult, propACL, propDataType);
                prop.setAssignmentDefaultMultiplicity(data.getDefaultMultiplicity());
                prop.setOverrideACL(propOverrideACL);
                prop.setUniqueMode(propUniqueMode);
                prop.setDefaultValue(propDefaultValue);
                prop.setOptions(options);
                if (refType != null)
                    prop.setReferencedType(refType);
                try {
                    EJBLookup.getAssignmentEngine().createProperty(type.getId(), prop, parentXPath, data.getAlias());
                    env = CacheAdmin.getEnvironment(); //refresh environment
                    ctx.put(ConversionEngine.KEY_TYPE, env.getType(type.getId()).asEditable());
                    type = (FxType) ctx.get(ConversionEngine.KEY_TYPE);
                } catch (FxApplicationException e) {
                    throw e.asRuntimeException();
                }
            }
            reader.moveUp();
        }
        //check if disabled
        try {
            if (type.hasAssignment(data.getXpath())) {
                FxPropertyAssignment check = type.getPropertyAssignment(data.getXpath());
                if (!check.isEnabled()) {
                    FxPropertyAssignmentEdit checkEnabled = check.asEditable().setEnabled(true);
                    System.out.println("Enabling: " + data.getXpath());
                    EJBLookup.getAssignmentEngine().save(checkEnabled, false);
                    type.resolveReferences(CacheAdmin.getEnvironment());
                }
            }
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        //check if the assignment exists and create if needed
        if (!type.isXPathValid(data.getXpath(), true)) {
            //property exists but not the xpath
            try {
                //create assignment from property
                FxPropertyAssignmentEdit paEdit;
                if (StringUtils.isEmpty(data.getParentAssignment()))
                    paEdit = FxPropertyAssignmentEdit.createNew(property, type, data.getAlias(),
                            type.getName() + "/" + parentXPath);
                else //reuse it
                    paEdit = FxPropertyAssignmentEdit.reuse(data.getParentAssignment(), type.getName(),
                            parentXPath, data.getAlias());
                if (!"/".equals(parentXPath))
                    paEdit.setParentGroupAssignment((FxGroupAssignment) type.getAssignment(parentXPath));
                EJBLookup.getAssignmentEngine().save(paEdit, false);

                env = CacheAdmin.getEnvironment(); //refresh environment
                ctx.put(ConversionEngine.KEY_TYPE, env.getType(type.getId()).asEditable());
                type = (FxType) ctx.get(ConversionEngine.KEY_TYPE);
            } catch (FxApplicationException e) {
                throw e.asRuntimeException();
            }
        }
        //assignment either exists now or has been created, apply all assignment specific data
        long paId;
        try {
            FxPropertyAssignmentEdit pa = ((FxPropertyAssignment) env.getAssignment(data.getXpath())).asEditable();
            pa.setACL(acl);
            pa.setDefaultLanguage(defaultLanguage);
            pa.setPosition(data.getPos());
            pa.setMultiplicity(data.getMultiplicity());
            pa.setDefaultMultiplicity(data.getDefaultMultiplicity());
            pa.setLabel(data.getLabel());
            pa.setHint(data.getHint());
            pa.setOptions(data.getOptions());
            pa.setDefaultValue(defaultValue);
            paId = EJBLookup.getAssignmentEngine().save(pa, false);
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        env = CacheAdmin.getEnvironment(); //refresh environment
        ctx.put(ConversionEngine.KEY_TYPE, env.getType(type.getId()).asEditable());
        return env.getAssignment(paId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConvert(Class type) {
        return FxPropertyAssignment.class.isAssignableFrom(type);
    }
}
