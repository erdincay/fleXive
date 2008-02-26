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

import com.flexive.shared.content.FxData;
import com.flexive.shared.content.FxGroupData;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPropertyData;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream converter for FxGroupData
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxGroupDataConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxGroupData gd = (FxGroupData) o;
        gd.compact(); //make sure all gaps are closed
        writer.startNode("group");
        writer.addAttribute("xpath", gd.getXPathFull());
        if( gd.getPos() >= 0 ) //no position for root group
            writer.addAttribute("pos", String.valueOf(gd.getPos()));

        /* do {
          boolean removed;
          removed = false;
          for (FxData data : gd.getChildren())
              if (data.isEmpty() && data.isRemoveable()) {
                  try {
                      gd.removeChild(data);
                  } catch (FxApplicationException e) {
                      throw e.asRuntimeException();
                  }
                  removed = true;
                  break;
              }
      } while (removed);*/
        for (FxData data : gd.getChildren())
            if (!data.isSystemInternal())
                ctx.convertAnother(data);
        writer.endNode();
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        FxContent co = (FxContent)ctx.get(ConversionEngine.KEY_CONTENT);
        String xp = reader.getAttribute("xpath");
        int pos = -1;
        if( reader.getAttribute("pos") != null )
            pos = Integer.valueOf(reader.getAttribute("pos"));
        while( reader.hasMoreChildren() ) {
            reader.moveDown();
            if( "prop".equals(reader.getNodeName())) {
                ctx.convertAnother(this, FxPropertyData.class);
            } else if ("group".equals(reader.getNodeName())) {
                unmarshal(reader, ctx);
            }
            reader.moveUp();
        }
        try {
            if( pos >= 0 )
                co.getGroupData(xp).setPos(pos);
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class aClass) {
        return FxGroupData.class.isAssignableFrom(aClass);
    }
}
