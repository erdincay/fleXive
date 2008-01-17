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
package com.flexive.shared;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.CompactWriter;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import java.io.StringWriter;
import java.io.Writer;

/**
 * XML Utilities
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxXMLUtils {

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
        return fromCData(ret);
    }

    /**
     * Convert a content to CDATA and preserve all existing CDATA
     *
     * @param content content to convert
     * @return character data encoded content
     */
    public static String toCData(String content) {
        if (content.contains("]]>")) {
            //take care of nested CDATA sections
            return "<![CDATA[" + content.replaceAll("\\]\\]\\>", "]]]]><![CDATA[>") + "]]>";
        } else
            return "<![CDATA[" + content + "]]>";
    }

    /**
     * Convert a CData back to a String
     *
     * @param cdata CData to convert
     * @return String
     */
    public static String fromCData(String cdata) {
        StringBuilder sb = new StringBuilder(cdata);
        int idx;
        while ((idx = sb.indexOf("<![CDATA[")) > 0) {
            int close = sb.indexOf("]]>");
            if (close > 0) {
                sb.delete(idx, idx + 9);
                sb.delete(close - 9, close - 9 + 3);
            }
        }
        return sb.toString();
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
     * @param writer xml writer
     * @param tag tag name
     * @param value value
     * @param asCData use CDData?
     * @throws javax.xml.stream.XMLStreamException on errors
     */
    public static void writeSimpleTag(XMLStreamWriter writer, String tag, Object value, boolean asCData) throws XMLStreamException {
        writer.writeStartElement(tag);
        if( asCData )
            writer.writeCData(String.valueOf(value));
        else
            writer.writeCharacters(String.valueOf(value));
        writer.writeEndElement();
    }
}
