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

import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.LifeCycleInfo;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * XStream converter for ACL
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ACLConverter implements Converter {

    private static final Log LOG = LogFactory.getLog(ACLConverter.class);

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        ACL acl = (ACL) o;
        writer.addAttribute("id", String.valueOf(acl.getId()));
        writer.addAttribute("name", acl.getName());
        writer.addAttribute("category", acl.getCategory().name());
        writer.addAttribute("mandatorId", String.valueOf(acl.getMandatorId()));
        writer.addAttribute("mandator", acl.getMandatorName());
        writer.startNode("color");
        writer.setValue(acl.getColor());
        writer.endNode();
        writer.startNode("description");
        writer.setValue(acl.getDescription());
        writer.endNode();
        writer.startNode("label");
        ctx.convertAnother(acl.getLabel());
        writer.endNode();
        ctx.convertAnother(acl.getLifeCycleInfo());
        /*writer.startNode("assignments");
        try {
            List<ACLAssignment> assignments = EJBLookup.getAclEngine().loadAssignments(acl.getId());
            for(ACLAssignment aa: assignments)
                ctx.convertAnother(aa);
        } catch (FxApplicationException e) {
            LOG.error(e);
        }
        writer.endNode();*/
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        long id = Long.valueOf(reader.getAttribute("id"));
        String name = reader.getAttribute("name");
        ACLCategory category = ACLCategory.valueOf(reader.getAttribute("category"));
        long mandatorId = Long.valueOf(reader.getAttribute("mandatorId"));
        String mandator = reader.getAttribute("mandator");
        reader.moveDown();
        String color = reader.getValue();
        reader.moveUp();
        reader.moveDown();
        String description = reader.getValue();
        reader.moveUp();
        reader.moveDown();
        FxString label = (FxString) ctx.convertAnother(this, FxValue.class);
        reader.moveUp();
        reader.moveDown();
        LifeCycleInfo lci = (LifeCycleInfo) ctx.convertAnother(this, LifeCycleInfo.class);
        reader.moveUp();
        return new ACL(id, name, label, mandatorId, mandator, description, color, category, lci);
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class aClass) {
        return ACL.class.isAssignableFrom(aClass);
    }
}
