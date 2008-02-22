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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.exceptions.FxApplicationException;

/**
 * XStream converter for FxContent
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxContentConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxContent co = (FxContent)o;
        FxEnvironment env = CacheAdmin.getEnvironment();
        try {
            writer.addAttribute("pk", co.getPk().toString());
            writer.addAttribute("type", env.getType(co.getTypeId()).getName());
            writer.addAttribute("mandator", env.getMandator(co.getMandatorId()).getName());
            writer.addAttribute("acl", env.getACL(co.getAclId()).getName());
            writer.addAttribute("step", env.getStepDefinition(env.getStep(co.getStepId()).getStepDefinitionId()).getName());
            writer.addAttribute("mainLanguage", EJBLookup.getLanguageEngine().load(co.getMainLanguage()).getIso2digit());
            writer.addAttribute("relation", String.valueOf(co.isRelation()));
            if( co.isRelation() ) {
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
//            ctx.convertAnother(co.getRootGroup());
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class aClass) {
        return FxContent.class.isAssignableFrom(aClass);
    }



}
