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
package com.flexive.war.webdav;

import com.flexive.war.webdav.catalina.FastHttpDateFormat;
import com.flexive.war.webdav.catalina.URLEncoder;
import com.flexive.war.webdav.catalina.XMLWriter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;


public class FxDavEntry {

    private Date creationdate;
    private String displayname;
    private Date lastmodified;
    protected long contentlength;
    private boolean isCollection;


    protected static final String ALL_PROPS[] = {"creationdate", "displayname", "getlastmodified", "getcontentlength",
            "getcontenttype", "getetag", "resourcetype", "supportedlock"};


    protected static URLEncoder urlEncoder;


    protected FxDavEntry(boolean collection, Date creationdate, String displayname, Date lastmodified, long contentlength) {
        this.isCollection = collection;
        this.creationdate = creationdate;
        this.displayname = displayname;
        this.lastmodified = lastmodified;
        this.contentlength = contentlength;

    }

    private static SimpleDateFormat getDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    }

    public void writeProperties(XMLWriter generatedXML, String path, String[] properties) {

        generatedXML.writeElement(null, "response", XMLWriter.OPENING);

        // Generating href element
        writeElement(generatedXML, "href", FxWebDavServlet.rewriteUrl(path));

        // Write the properties
        String[] propertiesNotFound = _writeProperties(generatedXML, properties, path);

        // Write the property not found list
        if (propertiesNotFound.length > 0) {
            generatedXML.writeElement(null, "propstat", XMLWriter.OPENING);
            generatedXML.writeElement(null, "prop", XMLWriter.OPENING);
            for (String prop : propertiesNotFound) {
                generatedXML.writeElement(null, prop, XMLWriter.NO_CONTENT);
            }
            generatedXML.writeElement(null, "prop", XMLWriter.CLOSING);
            writeStatus(generatedXML, FxWebDavStatus.SC_NOT_FOUND);
            generatedXML.writeElement(null, "propstat", XMLWriter.CLOSING);
        }

        generatedXML.writeElement(null, "response", XMLWriter.CLOSING);
    }


    /**
     * @param generatedXML
     * @param properties
     * @return a vector with all properties that could not be resolved
     */
    private String[] _writeProperties(XMLWriter generatedXML, String[] properties, String path) {

        Vector<String> propertiesNotFound = new Vector<String>();
        generatedXML.writeElement(null, "propstat", XMLWriter.OPENING);
        generatedXML.writeElement(null, "prop", XMLWriter.OPENING);
        for (String property : properties) {
            if (property.equals("creationdate")) {
                generatedXML.writeProperty(null, "creationdate", getISOCreationDate(this.creationdate.getTime()));
            } else if (property.equals("displayname")) {
                writeElement(generatedXML, "displayname", this.displayname);
            } else if (property.equals("getcontentlanguage")) {
                if (this.isCollection) {
                    propertiesNotFound.addElement(property);
                } else {
                    generatedXML.writeElement(null, "getcontentlanguage", XMLWriter.NO_CONTENT);     // TODO
                }
            } else if (property.equals("getcontentlength")) {
                if (this.isCollection) {
                    propertiesNotFound.addElement(property);
                } else {
                    generatedXML.writeProperty(null, "getcontentlength", (String.valueOf(this.contentlength)));
                }
            } else if (property.equals("getcontenttype")) {
                if (this.isCollection) {
                    generatedXML.writeProperty(null, "getcontenttype", "httpd/unix-directory");
                } else {
                    generatedXML.writeProperty(null, "getcontenttype", FxWebDavServletBase.getMimeType(this.displayname));
                }
            } else if (property.equals("getetag")) {
                if (this.isCollection) {
                    propertiesNotFound.addElement(property);
                } else {
                    //generatedXML.writeProperty(null, "getetag", getETag(cacheEntry.attributes));
                    generatedXML.writeProperty(null, "getetag", "fx3");
                }
            } else if (property.equals("getlastmodified")) {
                if (this.isCollection) {
                    propertiesNotFound.addElement(property);
                } else {
                    generatedXML.writeProperty(null, "getlastmodified",
                            FastHttpDateFormat.formatDate(lastmodified.getTime(), null));
                }
            } else if (property.equals("resourcetype")) {
                if (this.isCollection) {
                    generatedXML.writeElement(null, "resourcetype", XMLWriter.OPENING);
                    generatedXML.writeElement(null, "collection", XMLWriter.NO_CONTENT);
                    generatedXML.writeElement(null, "resourcetype", XMLWriter.CLOSING);
                } else {
                    generatedXML.writeElement(null, "resourcetype", XMLWriter.NO_CONTENT);
                }
            } else if (property.equals("href")) {

                generatedXML.writeProperty(null, "href", FxWebDavServlet.rewriteUrl(path));
            } else if (property.equals("supportedlock")) {
                String supportedLocks =
                        "<lockentry><lockscope><exclusive/></lockscope><locktype><write/></locktype></lockentry>" +
                                "<lockentry><lockscope><shared/></lockscope><locktype><write/></locktype></lockentry>";
                writeElement(generatedXML, "supportedlock", supportedLocks);
            } else if (property.equals("lockdiscovery")) {
                // TODO!!
                //if (!generateLockDiscovery(path, generatedXML))
                //    propertiesNotFound.addElement(property);
            } else {
                // IE: name, parentname, ishidden, iscollection, isreadonly, contentclass, lastaccessed,
                // isstructureddocument, defaultdocument, isroot
                propertiesNotFound.addElement(property);
            }
        }

        // ALL_PROP only?
        //generateLockDiscovery(path, generatedXML);

        generatedXML.writeElement(null, "prop", XMLWriter.CLOSING);
        writeStatus(generatedXML, FxWebDavStatus.SC_OK);
        generatedXML.writeElement(null, "propstat", XMLWriter.CLOSING);

        return propertiesNotFound.toArray(new String[propertiesNotFound.size()]);
    }

    /**
     * @param generatedXML
     */
    protected void writePropertyNames(XMLWriter generatedXML, String path) {
        generatedXML.writeElement(null, "response", XMLWriter.OPENING);

        // Generating href element
        writeElement(generatedXML, "href", FxWebDavServlet.rewriteUrl(path));

        generatedXML.writeElement(null, "propstat", XMLWriter.OPENING);

        // Write all available properties
        generatedXML.writeElement(null, "prop", XMLWriter.OPENING);
        generatedXML.writeElement(null, "creationdate", XMLWriter.NO_CONTENT);
        generatedXML.writeElement(null, "displayname", XMLWriter.NO_CONTENT);
        generatedXML.writeElement(null, "resourcetype", XMLWriter.NO_CONTENT);
        generatedXML.writeElement(null, "source", XMLWriter.NO_CONTENT);
        generatedXML.writeElement(null, "lockdiscovery", XMLWriter.NO_CONTENT);
        if (!isCollection) {
            generatedXML.writeElement(null, "getcontentlanguage", XMLWriter.NO_CONTENT);
            generatedXML.writeElement(null, "getcontentlength", XMLWriter.NO_CONTENT);
            generatedXML.writeElement(null, "getcontenttype", XMLWriter.NO_CONTENT);
            generatedXML.writeElement(null, "getetag", XMLWriter.NO_CONTENT);
            generatedXML.writeElement(null, "getlastmodified", XMLWriter.NO_CONTENT);
        }
        generatedXML.writeElement(null, "prop", XMLWriter.CLOSING);

        // Write the status
        writeStatus(generatedXML, FxWebDavStatus.SC_OK);
        generatedXML.writeElement(null, "propstat", XMLWriter.CLOSING);

        generatedXML.writeElement(null, "response", XMLWriter.CLOSING);
    }

    /**
     * Get creation date in ISO format.
     */
    private String getISOCreationDate(long creationDate) {
        StringBuffer creationDateValue = new StringBuffer(getDateFormat().format(new Date(creationDate)));
        return creationDateValue.toString();
    }

    /**
     * @param generatedXML
     * @param status
     */
    private void writeStatus(XMLWriter generatedXML, int status) {
        String sStatus = "HTTP/1.1 " + status + " " + FxWebDavStatus.getStatusText(status);
        generatedXML.writeElement(null, "status", XMLWriter.OPENING);
        generatedXML.writeText(sStatus);
        generatedXML.writeElement(null, "status", XMLWriter.CLOSING);

    }

    /**
     * Writes a text element to the xml stream.
     *
     * @param generatedXML the xml stream
     * @param text         the text
     */
    private void writeElement(XMLWriter generatedXML, String name, String text) {
        generatedXML.writeElement(null, name, XMLWriter.OPENING);
        generatedXML.writeText(text);
        generatedXML.writeElement(null, name, XMLWriter.CLOSING);
    }

    public Date getCreationdate() {
        return creationdate;
    }

    public String getDisplayname() {
        return displayname;
    }

    public Date getLastmodified() {
        return lastmodified;
    }

    public boolean isCollection() {
        return isCollection;
    }

    public String toString() {
        return this.displayname + (isCollection ? ";Folder" : ";File;Size:" + contentlength);
    }
}
