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
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxStructureOption;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.util.List;

/**
 * XStream converter for assignments
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public abstract class FxAssignmentConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxAssignment as = (FxAssignment) o;
        writer.addAttribute("alias", as.getAlias());
        writer.addAttribute("xpath", as.getXPath());
        writer.addAttribute("pos", String.valueOf(as.getPosition()));
        writer.addAttribute("enabled", String.valueOf(as.isEnabled()));
        writer.addAttribute("multiplicity", as.getMultiplicity().toString());
        writer.addAttribute("defaultMultiplicity", String.valueOf(as.getDefaultMultiplicity()));

        if (as.isDerivedAssignment())
            writer.addAttribute("parent", CacheAdmin.getEnvironment().getAssignment(as.getBaseAssignmentId()).getXPath());

        writer.startNode("label");
        ctx.convertAnother(as.getLabel());
        writer.endNode();
        writer.startNode("hint");
        ctx.convertAnother(as.getHint());
        writer.endNode();

        marshallOptions(writer, as.getOptions());
    }

    /**
     * Marshall FxStructureOption's
     *
     * @param writer  HierarchicalStreamWriter
     * @param options List<FxStructureOption>
     */
    protected void marshallOptions(HierarchicalStreamWriter writer, List<FxStructureOption> options) {
        if (options.size() > 0) {
            writer.startNode("options");
            for (FxStructureOption opt : options) {
                writer.startNode("option");
                writer.addAttribute("key", opt.getKey());
                writer.addAttribute("value", opt.getValue());
                writer.addAttribute("overrideable", String.valueOf(opt.isOverrideable()));
                writer.addAttribute("set", String.valueOf(opt.isSet()));
                writer.endNode();
            }
            writer.endNode();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader hierarchicalStreamReader, UnmarshallingContext unmarshallingContext) {
        //TODO
        return null;
    }
}
