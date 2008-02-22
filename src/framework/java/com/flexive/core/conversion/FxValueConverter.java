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

import com.flexive.shared.value.FxValue;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream converter for FxValues
 * <p/>
 * XML format for e.g. a FxString:
 * <p/>
 * <code><val t="FxString" ml="1" dl="1">
 * <d l="1">text lang 1</d>
 * <d l="2">text lang 2</d>
 * </val></code>
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxValueConverter implements Converter {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void marshal(Object o, HierarchicalStreamWriter writer, MarshallingContext ctx) {
        FxValue value = (FxValue) o;
        writer.addAttribute("t", value.getClass().getSimpleName());
        writer.addAttribute("ml", value.isMultiLanguage() ? "1" : "0");
        writer.addAttribute("dl", "" + value.getDefaultLanguage());
        for (long lang : value.getTranslatedLanguages()) {
            writer.startNode("d");
            writer.addAttribute("l", "" + lang);
            writer.setValue(value.getStringValue(value.getTranslation(lang)));
            writer.endNode();
        }
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext ctx) {
        FxValue v = null;
        try {
            boolean multiLanguage = "1".equals(reader.getAttribute("ml"));
            long defaultLanguage = Long.valueOf(reader.getAttribute("dl"));
            v = (FxValue) Class.forName("com.flexive.shared.value." + reader.getAttribute("t")).
                    getConstructor(long.class, boolean.class).newInstance(defaultLanguage, multiLanguage);
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                if ("d".equals(reader.getNodeName())) {
                    long lang = Long.valueOf(reader.getAttribute("l"));
                    v.setTranslation(lang, v.fromString(reader.getValue()));
                }
                reader.moveUp();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return v;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canConvert(Class aClass) {
        return FxValue.class.isAssignableFrom(aClass);
    }
}
