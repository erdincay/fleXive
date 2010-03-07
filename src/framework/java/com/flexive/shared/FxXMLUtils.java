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
package com.flexive.shared;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;
import org.apache.commons.lang.StringEscapeUtils;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

/**
 * XML Utilities
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxXMLUtils {
    /**
     * XML header to use
     */
    public final static String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";

    /**
     * Get the character content of an element in xml data
     *
     * @param xml     the complete xml data
     * @param element requested element
     * @return data
     */
    public static String getElementData(String xml, String element) {
        if (xml == null)
            return null;
        int start = xml.indexOf("<" + element + ">");
        if (start <= 0)
            return null;
        int end = xml.indexOf("</", start);
        if (start <= 0)
            return null;
        String ret = xml.substring(start + element.length() + 2, end);
//        Strip CData if needed
//        if (ret.startsWith("<![CDATA["))
//            ret = ret.substring(9, ret.length() - 3);
        return StringEscapeUtils.unescapeXml(ret);
    }

    /**
     * Convenience method to create a more compact XML representation
     * of an object than {@link com.thoughtworks.xstream.XStream#toXML(Object)} creates (no pretty-printing).
     *
     * @param xStream the xstream instance
     * @param object  the object to be marshalled
     * @return the XML representation of the object
     */
    public static String toXML(XStream xStream, Object object) {
        Writer writer = new StringWriter();
        xStream.marshal(object, new CompactWriter(writer));
        return writer.toString();
    }

    /**
     * Write a "simple" tag
     *
     * @param writer  xml writer
     * @param tag     tag name
     * @param value   value
     * @param asCData use CDData?
     * @throws javax.xml.stream.XMLStreamException
     *          on errors
     */
    public static void writeSimpleTag(XMLStreamWriter writer, String tag, Object value, boolean asCData) throws XMLStreamException {
        writer.writeStartElement(tag);
        if (asCData)
            writer.writeCData(String.valueOf(value));
        else
            writer.writeCharacters(String.valueOf(value));
        writer.writeEndElement();
    }

}
