/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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

import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

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
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        writer.startNode(ConversionEngine.KEY_PROPERTY_AS);
        FxPropertyAssignment prop = (FxPropertyAssignment) o;
        final FxProperty p = prop.getProperty();
        writer.addAttribute("property", p.getName());
        writer.addAttribute("acl", prop.getACL().getName());
        try {
            writer.addAttribute("defaultLanguage", ConversionEngine.getLang(EJBLookup.getLanguageEngine(), prop.getDefaultLanguage()));
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        super.marshal(o, writer, ctx);
        if (!prop.isDerivedAssignment()) {
            writer.startNode(ConversionEngine.KEY_PROPERTY);
            writer.addAttribute("name", p.getName());
            writer.addAttribute("multiplicity", p.getMultiplicity().toString());
            writer.addAttribute("dataType", p.getDataType().name());
            writer.addAttribute("acl", p.getACL().getName());
            writer.addAttribute("overrideACL", String.valueOf(p.mayOverrideACL()));
            writer.addAttribute("uniqueMode", p.getUniqueMode().name());
            if( p.hasReferencedType() )
                writer.addAttribute("refType", p.getReferencedType().getName());
            if( p.hasReferencedList())
                writer.addAttribute("refList", p.getReferencedList().getName());

            writer.startNode("label");
            ctx.convertAnother(p.getLabel());
            writer.endNode();
            writer.startNode("hint");
            ctx.convertAnother(p.getHint());
            writer.endNode();

            writer.startNode("defaultValue");
            ctx.convertAnother(p.getDefaultValue());
            writer.endNode();

            marshallOptions(writer, p.getOptions());
            writer.endNode();
        }
        writer.startNode("defaultValue");
        ctx.convertAnother(prop.getDefaultValue());
        writer.endNode();
        writer.endNode();
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class type) {
        return FxPropertyAssignment.class.isAssignableFrom(type);
    }
}
