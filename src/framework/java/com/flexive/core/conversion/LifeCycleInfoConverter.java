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
import com.flexive.shared.security.LifeCycleInfo;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxApplicationException;

/**
 * XStream converter for LifeCycleInfo
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class LifeCycleInfoConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        LifeCycleInfo li = (LifeCycleInfo)o;
        writer.startNode("lci");
        try {
            writer.addAttribute("cr", EJBLookup.getAccountEngine().load(li.getCreatorId()).getLoginName());
            writer.addAttribute("crAt", FxFormatUtils.getUniversalDateTimeFormat().format(li.getCreationTime()));
            writer.addAttribute("mf", EJBLookup.getAccountEngine().load(li.getModificatorId()).getLoginName());
            writer.addAttribute("mfAt", FxFormatUtils.getUniversalDateTimeFormat().format(li.getModificationTime()));
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        writer.endNode();
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader writer, UnmarshallingContext ctx) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class aClass) {
        return LifeCycleInfo.class.isAssignableFrom(aClass);
    }
}
