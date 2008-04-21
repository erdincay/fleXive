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
package com.flexive.war.webdav.catalina;

import java.io.IOException;
import java.io.Writer;

/**
 * Catalina sources cloned for packaging issues to the flexive source tree.
 * Refactored to JDK 1.5 compatibility.
 * Licensed under the Apache License, Version 2.0
 * <p/>
 * XMLWriter helper class.
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class XMLWriter {

    // -------------------------------------------------------------- Constants


    /**
     * Opening tag.
     */
    public static final int OPENING = 0;


    /**
     * Closing tag.
     */
    public static final int CLOSING = 1;


    /**
     * Element with no content.
     */
    public static final int NO_CONTENT = 2;

    // ----------------------------------------------------- Instance Variables


    /**
     * Buffer.
     */
    protected StringBuilder buffer = new StringBuilder();


    /**
     * Writer.
     */
    protected Writer writer = null;

    // ----------------------------------------------------------- Constructors


    /**
     * Constructor.
     */
    public XMLWriter() {
    }


    /**
     * Constructor.
     */
    public XMLWriter(Writer writer) {
        this.writer = writer;
    }

    // --------------------------------------------------------- Public Methods


    /**
     * Retrieve generated XML.
     *
     * @return String containing the generated XML
     */
    public String toString() {
        return buffer.toString();
    }


    /**
     * Write property to the XML.
     *
     * @param namespace     Namespace
     * @param namespaceInfo Namespace info
     * @param name          Property name
     * @param value         Property value
     */
    public void writeProperty(String namespace, String namespaceInfo,
                              String name, String value) {
        writeElement(namespace, namespaceInfo, name, OPENING);
        buffer.append(value);
        writeElement(namespace, namespaceInfo, name, CLOSING);

    }


    /**
     * Write property to the XML.
     *
     * @param namespace Namespace
     * @param name      Property name
     * @param value     Property value
     */
    public void writeProperty(String namespace, String name, String value) {
        writeElement(namespace, name, OPENING);
        buffer.append(value);
        writeElement(namespace, name, CLOSING);
    }


    /**
     * Write property to the XML.
     *
     * @param namespace Namespace
     * @param name      Property name
     */
    public void writeProperty(String namespace, String name) {
        writeElement(namespace, name, NO_CONTENT);
    }


    /**
     * Write an element.
     *
     * @param name      Element name
     * @param namespace Namespace abbreviation
     * @param type      Element type
     */
    public void writeElement(String namespace, String name, int type) {
        writeElement(namespace, null, name, type);
    }


    /**
     * Write an element.
     *
     * @param namespace     Namespace abbreviation
     * @param namespaceInfo Namespace info
     * @param name          Element name
     * @param type          Element type
     */
    public void writeElement(String namespace, String namespaceInfo,
                             String name, int type) {
        if ((namespace != null) && (namespace.length() > 0)) {
            switch (type) {
                case OPENING:
                    if (namespaceInfo != null) {
                        buffer.append("<").append(namespace).append(":").append(name).append(" xmlns:").append(namespace).append("=\"").append(namespaceInfo).append("\">");
                    } else {
                        buffer.append("<").append(namespace).append(":").append(name).append(">");
                    }
                    break;
                case CLOSING:
                    buffer.append("</").append(namespace).append(":").append(name).append(">\n");
                    break;
                case NO_CONTENT:
                default:
                    if (namespaceInfo != null) {
                        buffer.append("<").append(namespace).append(":").append(name).append(" xmlns:").append(namespace).append("=\"").append(namespaceInfo).append("\"/>");
                    } else {
                        buffer.append("<").append(namespace).append(":").append(name).append("/>");
                    }
                    break;
            }
        } else {
            switch (type) {
                case OPENING:
                    buffer.append("<").append(name).append(">");
                    break;
                case CLOSING:
                    buffer.append("</").append(name).append(">\n");
                    break;
                case NO_CONTENT:
                default:
                    buffer.append("<").append(name).append("/>");
                    break;
            }
        }
    }


    /**
     * Write text.
     *
     * @param text Text to append
     */
    public void writeText(String text) {
        buffer.append(text);
    }


    /**
     * Write data.
     *
     * @param data Data to append
     */
    public void writeData(String data) {
        buffer.append("<![CDATA[").append(data).append("]]>");
    }


    /**
     * Write XML Header.
     */
    public void writeXMLHeader() {
        buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
    }


    /**
     * Send data and reinitializes buffer.
     */
    public void sendData() throws IOException {
        if (writer != null) {
            writer.write(buffer.toString());
            buffer = new StringBuilder();
        }
    }


}
