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

import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxData;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.value.FxValue;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream converter for FxPropertyData
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxPropertyDataConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxPropertyData pd = (FxPropertyData) o;
        pd.compact(); //make sure all gaps are closed
        writer.startNode(ConversionEngine.KEY_PROPERTY);
        writer.addAttribute("xpath", pd.getXPathFull());
        writer.addAttribute("pos", String.valueOf(pd.getPos()));
        ctx.convertAnother(pd.getValue());
        writer.endNode();
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        FxContent co = (FxContent) ctx.get(ConversionEngine.KEY_CONTENT);
        String xp = reader.getAttribute("xpath");
        int pos = Integer.valueOf(reader.getAttribute("pos"));
        FxValue val = (FxValue) ctx.convertAnother(this, FxValue.class);
        FxData data;
        try {
            co.setValue(xp, val);
            data = co.getPropertyData(xp);
            if (data.getPos() != pos)
                data.setPos(pos);
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        return data;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class aClass) {
        return FxPropertyData.class.isAssignableFrom(aClass);
    }
}
