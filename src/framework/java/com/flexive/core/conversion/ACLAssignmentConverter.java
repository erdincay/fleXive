/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
import com.flexive.shared.security.ACLAssignment;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.LifeCycleInfo;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream converter for ACLAssignment
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ACLAssignmentConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        writer.startNode(ConversionEngine.KEY_ACLASSIGNMENT);
        ACLAssignment aa = (ACLAssignment) o;
        writer.addAttribute("aclId", String.valueOf(aa.getAclId()));
        writer.addAttribute("acl", CacheAdmin.getEnvironment().getACL(aa.getAclId()).getName());
        writer.addAttribute("category", aa.getACLCategory().name());
        writer.addAttribute("groupId", String.valueOf(aa.getGroupId()));
        writer.addAttribute("group", CacheAdmin.getEnvironment().getUserGroup(aa.getGroupId()).getName());
        writer.startNode("permissions");
        writer.addAttribute("create", String.valueOf(aa.getMayCreate()));
        writer.addAttribute("delete", String.valueOf(aa.getMayDelete()));
        writer.addAttribute("edit", String.valueOf(aa.getMayEdit()));
        writer.addAttribute("export", String.valueOf(aa.getMayExport()));
        writer.addAttribute("read", String.valueOf(aa.getMayRead()));
        writer.addAttribute("relate", String.valueOf(aa.getMayRelate()));
        writer.endNode();
        ctx.convertAnother(aa.getLifeCycleInfo());
        writer.endNode();
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        reader.moveDown();
        long aclId = Long.valueOf(reader.getAttribute("aclId"));
        long groupId = Long.valueOf(reader.getAttribute("groupId"));
        ACLCategory category = ACLCategory.valueOf(reader.getAttribute("category"));
        reader.moveDown();
        boolean cr = Boolean.valueOf(reader.getAttribute("create"));
        boolean rm = Boolean.valueOf(reader.getAttribute("delete"));
        boolean ed = Boolean.valueOf(reader.getAttribute("edit"));
        boolean ex = Boolean.valueOf(reader.getAttribute("export"));
        boolean rd = Boolean.valueOf(reader.getAttribute("read"));
        boolean rl = Boolean.valueOf(reader.getAttribute("relate"));
        reader.moveUp();
        reader.moveDown();
        LifeCycleInfo lci = (LifeCycleInfo) ctx.convertAnother(this, LifeCycleInfo.class);
        reader.moveUp();
        reader.moveUp();
        return new ACLAssignment(aclId, groupId, rd, ed, rl, rm, ex, cr, category, lci);
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class aClass) {
        return ACLAssignment.class.isAssignableFrom(aClass);
    }
}
