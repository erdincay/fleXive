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

import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxGroupAssignment;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

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
    public boolean canConvert(Class type) {
        return FxGroupAssignment.class.isAssignableFrom(type);
    }
}
