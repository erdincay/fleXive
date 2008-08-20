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
package com.flexive.war.webdav.catalina;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Catalina sources cloned for packaging issues to the flexive source tree.
 * Refactored to JDK 1.5 compatibility.
 * Licensed under the Apache License, Version 2.0
 * <p/>
 * Attributes implementation.
 *
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ResourceAttributes implements Attributes {
    private static final long serialVersionUID = 8977002508350224301L;

    // -------------------------------------------------------------- Constants

    // Default attribute names

    /**
     * Creation date.
     */
    public static final String CREATION_DATE = "creationdate";


    /**
     * Creation date.
     */
    public static final String ALTERNATE_CREATION_DATE = "creation-date";


    /**
     * Last modification date.
     */
    public static final String LAST_MODIFIED = "getlastmodified";


    /**
     * Last modification date.
     */
    public static final String ALTERNATE_LAST_MODIFIED = "last-modified";


    /**
     * Name.
     */
    public static final String NAME = "displayname";


    /**
     * Type.
     */
    public static final String TYPE = "resourcetype";


    /**
     * Type.
     */
    public static final String ALTERNATE_TYPE = "content-type";


    /**
     * Source.
     */
    public static final String SOURCE = "source";


    /**
     * MIME type of the content.
     */
    public static final String CONTENT_TYPE = "getcontenttype";


    /**
     * Content language.
     */
    public static final String CONTENT_LANGUAGE = "getcontentlanguage";


    /**
     * Content length.
     */
    public static final String CONTENT_LENGTH = "getcontentlength";


    /**
     * Content length.
     */
    public static final String ALTERNATE_CONTENT_LENGTH = "content-length";


    /**
     * ETag.
     */
    public static final String ETAG = "getetag";


    /**
     * Collection type.
     */
    public static final String COLLECTION_TYPE = "<collection/>";


    /**
     * HTTP date format.
     */
    protected static final SimpleDateFormat format =
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);


    /**
     * Date formats using for Date parsing.
     */
    protected static final SimpleDateFormat formats[] = {
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };


    protected final static TimeZone gmtZone = TimeZone.getTimeZone("GMT");

    /**
     * GMT timezone - all HTTP dates are on GMT
     */
    static {

        format.setTimeZone(gmtZone);

        formats[0].setTimeZone(gmtZone);
        formats[1].setTimeZone(gmtZone);
        formats[2].setTimeZone(gmtZone);

    }

    // ----------------------------------------------------------- Constructors


    /**
     * Default constructor.
     */
    public ResourceAttributes() {
    }


    /**
     * Merges with another attribute set.
     */
    public ResourceAttributes(Attributes attributes) {
        this.attributes = attributes;
    }

    // ----------------------------------------------------- Instance Variables


    /**
     * Collection flag.
     */
    protected boolean collection = false;


    /**
     * Content length.
     */
    protected long contentLength = -1;


    /**
     * Creation time.
     */
    protected long creation = -1;


    /**
     * Creation date.
     */
    protected Date creationDate = null;


    /**
     * Last modified time.
     */
    protected long lastModified = -1;


    /**
     * Last modified date.
     */
    protected Date lastModifiedDate = null;


    /**
     * Last modified date in HTTP format.
     */
    protected String lastModifiedHttp = null;


    /**
     * MIME type.
     */
    protected String mimeType = null;


    /**
     * Name.
     */
    protected String name = null;


    /**
     * Weak ETag.
     */
    protected String weakETag = null;


    /**
     * Strong ETag.
     */
    protected String strongETag = null;


    /**
     * External attributes.
     */
    protected Attributes attributes = null;

    // ------------------------------------------------------------- Properties


    /**
     * Is collection.
     */
    public boolean isCollection() {
        if (attributes != null) {
            return (getResourceType().equals(COLLECTION_TYPE));
        } else {
            return (collection);
        }
    }


    /**
     * Set collection flag.
     *
     * @param collection New flag value
     */
    public void setCollection(boolean collection) {
        this.collection = collection;
        if (attributes != null) {
            String value = "";
            if (collection)
                value = COLLECTION_TYPE;
            attributes.put(TYPE, value);
        }
    }


    /**
     * Get content length.
     *
     * @return content length value
     */
    public long getContentLength() {
        if (contentLength != -1L)
            return contentLength;
        if (attributes != null) {
            Attribute attribute = attributes.get(CONTENT_LENGTH);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        contentLength = (Long) value;
                    } else {
                        try {
                            contentLength = Long.parseLong(value.toString());
                        } catch (NumberFormatException e) {
                            // Ignore
                        }
                    }
                } catch (NamingException e) {
                    // No value for the attribute
                }
            }
        }
        return contentLength;
    }


    /**
     * Set content length.
     *
     * @param contentLength New content length value
     */
    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
        if (attributes != null)
            attributes.put(CONTENT_LENGTH, contentLength);
    }


    /**
     * Get creation time.
     *
     * @return creation time value
     */
    public long getCreation() {
        if (creation != -1L)
            return creation;
        if (creationDate != null)
            return creationDate.getTime();
        if (attributes != null) {
            Attribute attribute = attributes.get(CREATION_DATE);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        creation = (Long) value;
                    } else if (value instanceof Date) {
                        creation = ((Date) value).getTime();
                        creationDate = (Date) value;
                    } else {
                        String creationDateValue = value.toString();
                        Date result = null;
                        // Parsing the HTTP Date
                        for (int i = 0; (result == null) &&
                                (i < formats.length); i++) {
                            try {
                                result = formats[i].parse(creationDateValue);
                            } catch (ParseException e) {
                                //ignore
                            }
                        }
                        if (result != null) {
                            creation = result.getTime();
                            creationDate = result;
                        }
                    }
                } catch (NamingException e) {
                    // No value for the attribute
                }
            }
        }
        return creation;
    }


    /**
     * Set creation.
     *
     * @param creation New creation value
     */
    public void setCreation(long creation) {
        this.creation = creation;
        this.creationDate = null;
        if (attributes != null)
            attributes.put(CREATION_DATE, new Date(creation));
    }


    /**
     * Get creation date.
     *
     * @return Creation date value
     */
    public Date getCreationDate() {
        if (creationDate != null)
            return creationDate;
        if (creation != -1L) {
            creationDate = new Date(creation);
            return creationDate;
        }
        if (attributes != null) {
            Attribute attribute = attributes.get(CREATION_DATE);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        creation = (Long) value;
                        creationDate = new Date(creation);
                    } else if (value instanceof Date) {
                        creation = ((Date) value).getTime();
                        creationDate = (Date) value;
                    } else {
                        String creationDateValue = value.toString();
                        Date result = null;
                        // Parsing the HTTP Date
                        for (int i = 0; (result == null) &&
                                (i < formats.length); i++) {
                            try {
                                result = formats[i].parse(creationDateValue);
                            } catch (ParseException e) {
                                //ignore
                            }
                        }
                        if (result != null) {
                            creation = result.getTime();
                            creationDate = result;
                        }
                    }
                } catch (NamingException e) {
                    // No value for the attribute
                }
            }
        }
        return creationDate;
    }


    /**
     * Creation date mutator.
     *
     * @param creationDate New creation date
     */
    public void setCreationDate(Date creationDate) {
        this.creation = creationDate.getTime();
        this.creationDate = creationDate;
        if (attributes != null)
            attributes.put(CREATION_DATE, creationDate);
    }


    /**
     * Get last modified time.
     *
     * @return lastModified time value
     */
    public long getLastModified() {
        if (lastModified != -1L)
            return lastModified;
        if (lastModifiedDate != null)
            return lastModifiedDate.getTime();
        if (attributes != null) {
            Attribute attribute = attributes.get(LAST_MODIFIED);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        lastModified = (Long) value;
                    } else if (value instanceof Date) {
                        lastModified = ((Date) value).getTime();
                        lastModifiedDate = (Date) value;
                    } else {
                        String lastModifiedDateValue = value.toString();
                        Date result = null;
                        // Parsing the HTTP Date
                        for (int i = 0; (result == null) &&
                                (i < formats.length); i++) {
                            try {
                                result =
                                        formats[i].parse(lastModifiedDateValue);
                            } catch (ParseException e) {
                                //ignore
                            }
                        }
                        if (result != null) {
                            lastModified = result.getTime();
                            lastModifiedDate = result;
                        }
                    }
                } catch (NamingException e) {
                    // No value for the attribute
                }
            }
        }
        return lastModified;
    }


    /**
     * Set last modified.
     *
     * @param lastModified New last modified value
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
        this.lastModifiedDate = null;
        if (attributes != null)
            attributes.put(LAST_MODIFIED, new Date(lastModified));
    }


    /**
     * Set last modified date.
     *
     * @param lastModified New last modified date value
     * @deprecated
     */
    public void setLastModified(Date lastModified) {
        setLastModifiedDate(lastModified);
    }


    /**
     * Get lastModified date.
     *
     * @return LastModified date value
     */
    public Date getLastModifiedDate() {
        if (lastModifiedDate != null)
            return lastModifiedDate;
        if (lastModified != -1L) {
            lastModifiedDate = new Date(lastModified);
            return lastModifiedDate;
        }
        if (attributes != null) {
            Attribute attribute = attributes.get(LAST_MODIFIED);
            if (attribute != null) {
                try {
                    Object value = attribute.get();
                    if (value instanceof Long) {
                        lastModified = (Long) value;
                        lastModifiedDate = new Date(lastModified);
                    } else if (value instanceof Date) {
                        lastModified = ((Date) value).getTime();
                        lastModifiedDate = (Date) value;
                    } else {
                        String lastModifiedDateValue = value.toString();
                        Date result = null;
                        // Parsing the HTTP Date
                        for (int i = 0; (result == null) &&
                                (i < formats.length); i++) {
                            try {
                                result =
                                        formats[i].parse(lastModifiedDateValue);
                            } catch (ParseException e) {
                                //ignore
                            }
                        }
                        if (result != null) {
                            lastModified = result.getTime();
                            lastModifiedDate = result;
                        }
                    }
                } catch (NamingException e) {
                    // No value for the attribute
                }
            }
        }
        return lastModifiedDate;
    }


    /**
     * Last modified date mutator.
     *
     * @param lastModifiedDate New last modified date
     */
    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModified = lastModifiedDate.getTime();
        this.lastModifiedDate = lastModifiedDate;
        if (attributes != null)
            attributes.put(LAST_MODIFIED, lastModifiedDate);
    }


    /**
     * @return Returns the lastModifiedHttp.
     */
    public String getLastModifiedHttp() {
        if (lastModifiedHttp != null)
            return lastModifiedHttp;
        Date modifiedDate = getLastModifiedDate();
        if (modifiedDate == null) {
            modifiedDate = getCreationDate();
        }
        if (modifiedDate == null) {
            modifiedDate = new Date();
        }
        synchronized (format) {
            lastModifiedHttp = format.format(modifiedDate);
        }
        return lastModifiedHttp;
    }


    /**
     * @param lastModifiedHttp The lastModifiedHttp to set.
     */
    public void setLastModifiedHttp(String lastModifiedHttp) {
        this.lastModifiedHttp = lastModifiedHttp;
    }


    /**
     * @return Returns the mimeType.
     */
    public String getMimeType() {
        return mimeType;
    }


    /**
     * @param mimeType The mimeType to set.
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }


    /**
     * Get name.
     *
     * @return Name value
     */
    public String getName() {
        if (name != null)
            return name;
        if (attributes != null) {
            Attribute attribute = attributes.get(NAME);
            if (attribute != null) {
                try {
                    name = attribute.get().toString();
                } catch (NamingException e) {
                    // No value for the attribute
                }
            }
        }
        return name;
    }


    /**
     * Set name.
     *
     * @param name New name value
     */
    public void setName(String name) {
        this.name = name;
        if (attributes != null)
            attributes.put(NAME, name);
    }


    /**
     * Get resource type.
     *
     * @return String resource type
     */
    public String getResourceType() {
        String result = null;
        if (attributes != null) {
            Attribute attribute = attributes.get(TYPE);
            if (attribute != null) {
                try {
                    result = attribute.get().toString();
                } catch (NamingException e) {
                    // No value for the attribute
                }
            }
        }
        if (result == null) {
            if (collection)
                result = COLLECTION_TYPE;
            else
                result = "";
        }
        return result;
    }


    /**
     * Type mutator.
     *
     * @param resourceType New resource type
     */
    public void setResourceType(String resourceType) {
        collection = resourceType.equals(COLLECTION_TYPE);
        if (attributes != null)
            attributes.put(TYPE, resourceType);
    }


    /**
     * Get ETag.
     *
     * @return Weak ETag
     */
    public String getETag() {
        return getETag(false);
    }


    /**
     * Get ETag.
     *
     * @param strong If true, the strong ETag will be returned
     * @return ETag
     */
    public String getETag(boolean strong) {
        String result;
        /*if (attributes != null) {
            Attribute attribute = attributes.get(ETAG);
            if (attribute != null) {
                try {
                    result = attribute.get().toString();
                } catch (NamingException e) {
                    // No value for the attribute
                }
            }
        }*/
        if (strong) {
            // The strong ETag must always be calculated by the resources
            result = strongETag;
        } else {
            // The weakETag is contentLenght + lastModified
            if (weakETag == null) {
                weakETag = "W/\"" + getContentLength() + "-"
                        + getLastModified() + "\"";
            }
            result = weakETag;
        }
        return result;
    }


    /**
     * Set strong ETag.
     */
    public void setETag(String eTag) {
        this.strongETag = eTag;
        if (attributes != null)
            attributes.put(ETAG, eTag);
    }


    /**
     * Return the canonical path of the resource, to possibly be used for
     * direct file serving. Implementations which support this should override
     * it to return the file path.
     *
     * @return The canonical path of the resource
     */
    public String getCanonicalPath() {
        return null;
    }

    // ----------------------------------------------------- Attributes Methods


    /**
     * Get attribute.
     */
    public Attribute get(String attrID) {
        if (attributes == null) {
            if (attrID.equals(CREATION_DATE)) {
                return new BasicAttribute(CREATION_DATE, getCreationDate());
            } else if (attrID.equals(ALTERNATE_CREATION_DATE)) {
                return new BasicAttribute(ALTERNATE_CREATION_DATE,
                        getCreationDate());
            } else if (attrID.equals(LAST_MODIFIED)) {
                return new BasicAttribute(LAST_MODIFIED,
                        getLastModifiedDate());
            } else if (attrID.equals(ALTERNATE_LAST_MODIFIED)) {
                return new BasicAttribute(ALTERNATE_LAST_MODIFIED,
                        getLastModifiedDate());
            } else if (attrID.equals(NAME)) {
                return new BasicAttribute(NAME, getName());
            } else if (attrID.equals(TYPE)) {
                return new BasicAttribute(TYPE, getResourceType());
            } else if (attrID.equals(ALTERNATE_TYPE)) {
                return new BasicAttribute(ALTERNATE_TYPE, getResourceType());
            } else if (attrID.equals(CONTENT_LENGTH)) {
                return new BasicAttribute(CONTENT_LENGTH, getContentLength());
            } else if (attrID.equals(ALTERNATE_CONTENT_LENGTH)) {
                return new BasicAttribute(ALTERNATE_CONTENT_LENGTH, getContentLength());
            }
        } else {
            return attributes.get(attrID);
        }
        return null;
    }


    /**
     * Put attribute.
     */
    public Attribute put(Attribute attribute) {
        if (attributes == null) {
            try {
                return put(attribute.getID(), attribute.get());
            } catch (NamingException e) {
                return null;
            }
        } else {
            return attributes.put(attribute);
        }
    }


    /**
     * Put attribute.
     */
    public Attribute put(String attrID, Object val) {
        if (attributes == null) {
            return null; // No reason to implement this
        } else {
            return attributes.put(attrID, val);
        }
    }


    /**
     * Remove attribute.
     */
    public Attribute remove(String attrID) {
        if (attributes == null) {
            return null; // No reason to implement this
        } else {
            return attributes.remove(attrID);
        }
    }


    /**
     * Get all attributes.
     */
    public NamingEnumeration getAll() {
        if (attributes == null) {
            Vector<BasicAttribute> attributes = new Vector<BasicAttribute>();
            attributes.addElement(new BasicAttribute
                    (CREATION_DATE, getCreationDate()));
            attributes.addElement(new BasicAttribute
                    (LAST_MODIFIED, getLastModifiedDate()));
            attributes.addElement(new BasicAttribute(NAME, getName()));
            attributes.addElement(new BasicAttribute(TYPE, getResourceType()));
            attributes.addElement(new BasicAttribute(CONTENT_LENGTH, getContentLength()));
            return new RecyclableNamingEnumeration(attributes);
        } else {
            return attributes.getAll();
        }
    }


    /**
     * Get all attribute IDs.
     */
    public NamingEnumeration getIDs() {
        if (attributes == null) {
            Vector<String> attributeIDs = new Vector<String>();
            attributeIDs.addElement(CREATION_DATE);
            attributeIDs.addElement(LAST_MODIFIED);
            attributeIDs.addElement(NAME);
            attributeIDs.addElement(TYPE);
            attributeIDs.addElement(CONTENT_LENGTH);
            return new RecyclableNamingEnumeration(attributeIDs);
        } else {
            return attributes.getIDs();
        }
    }


    /**
     * Retrieves the number of attributes in the attribute set.
     */
    public int size() {
        if (attributes == null) {
            return 5;
        } else {
            return attributes.size();
        }
    }


    /**
     * Clone the attributes object (WARNING: fake cloning).
     */
    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    public Object clone() {
        return this;
    }


    /**
     * Case sensitivity.
     */
    public boolean isCaseIgnored() {
        return false;
    }


}

