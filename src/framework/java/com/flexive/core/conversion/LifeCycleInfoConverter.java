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

import com.flexive.core.LifeCycleInfoImpl;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.structure.FxTypeEdit;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxConversionException;
import com.flexive.shared.interfaces.AccountEngine;
import com.flexive.shared.security.LifeCycleInfo;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import java.text.ParseException;
import java.util.Date;

/**
 * XStream converter for LifeCycleInfo
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class LifeCycleInfoConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        LifeCycleInfo li = (LifeCycleInfo) o;
        writer.startNode(ConversionEngine.KEY_LCI);
        try {
            final AccountEngine acc = EJBLookup.getAccountEngine();
            writer.addAttribute("cr", acc.load(li.getCreatorId()).getLoginName());
            writer.addAttribute("crAt", FxFormatUtils.getUniversalDateTimeFormat().format(li.getCreationTime()));
            writer.addAttribute("mf", acc.load(li.getModificatorId()).getLoginName());
            writer.addAttribute("mfAt", FxFormatUtils.getUniversalDateTimeFormat().format(li.getModificationTime()));
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        writer.endNode();
    }

    /**
     * {@inheritDoc}
     */
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        final AccountEngine acc = EJBLookup.getAccountEngine();
        String convDate = null;
        try {
            convDate = reader.getAttribute("crAt");
            long crAt = FxFormatUtils.getUniversalDateTimeFormat().parse(convDate).getTime();
            convDate = reader.getAttribute("mfAt");
            long mfAt = FxFormatUtils.getUniversalDateTimeFormat().parse(convDate).getTime();
            final long creatorId = acc.load(reader.getAttribute("cr")).getId();
            final long modificator = acc.load(reader.getAttribute("mf")).getId();
            if( ctx.get(ConversionEngine.KEY_CONTENT) instanceof FxContent) {
                LifeCycleInfoImpl lci = (LifeCycleInfoImpl)((FxContent)ctx.get(ConversionEngine.KEY_CONTENT)).getLifeCycleInfo();
                lci.setCreatorId(creatorId);
                lci.setCreationTime(crAt);
                lci.setModifcatorId(modificator);
                lci.setModificationTime(mfAt);
                return lci;
            } else if( ctx.get(ConversionEngine.KEY_TYPE) instanceof FxTypeEdit) {
                LifeCycleInfoImpl lci = (LifeCycleInfoImpl)((FxTypeEdit)ctx.get(ConversionEngine.KEY_TYPE)).getLifeCycleInfo();
                if( lci != null ) {
                    lci.setCreatorId(creatorId);
                    lci.setCreationTime(crAt);
                    lci.setModifcatorId(modificator);
                    lci.setModificationTime(mfAt);
                }
                return lci;
            }
            return new LifeCycleInfoImpl(creatorId, crAt, modificator, mfAt);
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        } catch (ParseException e) {
            throw new FxConversionException(e, "ex.conversion.value.error", Date.class.getCanonicalName(), convDate,
                    e.getMessage()).asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class aClass) {
        return LifeCycleInfo.class.isAssignableFrom(aClass);
    }
}
