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
package com.flexive.shared.media.impl;

import com.google.common.collect.Maps;
import eu.medsea.mimeutil.MimeType;
import eu.medsea.mimeutil.MimeUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.*;

/**
 * A general mime type / subtype implementation
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.1
 */
public class FxMimeType implements Serializable {

    /**
     * Replacement table with overrides for common extensions with mime-utils.
     */
    private static final Map<String, String> EXT_REPLACEMENTS;
    private static final List<String> MIME_DETECTORS = Collections.unmodifiableList(Arrays.asList(
            "eu.medsea.mimeutil.detector.MagicMimeMimeDetector",
            // For Linux/OpenDesktop machines - disabled because it is not thread-safe:
            // http://sourceforge.net/tracker/?func=detail&aid=3007610&group_id=205064&atid=992132
            // "eu.medsea.mimeutil.detector.OpendesktopMimeDetector",
            // For Windows machines
            "eu.medsea.mimeutil.detector.WindowsRegistryMimeDetector",
            // Last resort - use file extension
            "eu.medsea.mimeutil.detector.ExtensionMimeDetector"
    ));

    private static volatile boolean DETECTORS_INITIALIZED = false;
    private static final Object INIT_LOCK = new Object();

    private static Log LOG = LogFactory.getLog(FxMimeType.class);

    static {
        // populate EXT_REPLACEMENTS
        final Map <String, String> replacements = Maps.newHashMap();
        replacements.put("wmv", "video/x-ms/wmv");
        EXT_REPLACEMENTS = Collections.unmodifiableMap(replacements);
    }

    /**
     * "Main/default" mime types
     */
    public static final String APPLICATION = "application";
    public static final String AUDIO = "audio";
    public static final String EXAMPLE = "example";
    public static final String IMAGE = "image";
    public static final String MESSAGE = "message";
    public static final String MODEL = "model";
    public static final String MULTIPART = "multipart";
    public static final String TEXT = "text";
    public static final String VIDEO = "video";
    public static final String UNKNOWN = "unknown";
    public static final String DEFAULT = UNKNOWN + "/" + UNKNOWN;

    private String type;
    private String subType;
    private static final long serialVersionUID = -5416305800516366421L;

    /**
     * Default constructor, sets a default of "application/octet-stream"
     */
    public FxMimeType() {
        this.type = "application";
        this.subType = "octet-stream";
    }

    /**
     * Constr. providing a main type only, no subtype ("unknown")
     *
     * @param type the main mime type
     */
    public FxMimeType(String type) {
        this.type = type;
        this.subType = UNKNOWN;
    }

    /**
     * Construct a mimeType from a given main- and sub type
     *
     * @param type    the type (e.g. "application")
     * @param subType the sub type (e.g. "msword")
     */
    public FxMimeType(String type, String subType) {
        this.type = type;
        this.subType = subType;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        this.subType = subType;
    }

    @Override
    public String toString() {
        if (!StringUtils.isBlank(subType))
            return type + "/" + subType;
        else return type + "/" + UNKNOWN;
    }

    @Override
    public int hashCode() {
        int hashCode;
        hashCode = (type != null ? type.hashCode() : 0);
        hashCode = 31 * hashCode + (subType != null ? subType.hashCode() : 0);
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof FxMimeType))
            return false;
        if (o == this)
            return true;
        final FxMimeType comp = (FxMimeType) o;
        return !(!this.type.equals(comp.type) || !this.subType.equals(comp.subType));
    }

    /**
     * Construct a MimeType from a given String. If the parameterized mime type cannot be identified,
     * a DEFAULT mime type of "application/octet-stream" will be constructed
     * A null input will return "unknown/unknown"
     *
     * @param mimeType the mimetype as a String, e.g. "application/pdf"
     * @return returns a MimeType
     */
    public static FxMimeType getMimeType(String mimeType) {
        if (mimeType == null)
            mimeType = "";
        final String[] s = mimeType.split("/");
        final String type = StringUtils.trim(s[0].toLowerCase());
        final String subType;

        if (s.length > 1 && !StringUtils.isBlank(type))
            subType = StringUtils.trim(s[1].toLowerCase());
        else if (StringUtils.isBlank(type))
            return new FxMimeType(FxMimeType.UNKNOWN, FxMimeType.UNKNOWN);
        else
            subType = UNKNOWN;

        return new FxMimeType(type, subType);
    }

    /**
     * This is a helper method for mime type detection based on medsea's mimeutils
     * Mime type detection based on header bytes and filenames, the latter is used as a
     * fallback if header detection fails
     *
     * @param header the file header as a byte array
     * @param fileName the file name as a String
     * @return the FxMimeType constructed from the input
     * @since 3.1.2
     */
    @SuppressWarnings({"unchecked"})
    public static FxMimeType detectMimeType(byte[] header, String fileName) {
        MimeType mt;

        initialize();

        // try and find the mimetype from the given file header
        Collection<MimeType> detected = MimeUtil.getMimeTypes(header);
        if (!detected.isEmpty()) {
            mt = MimeUtil.getMostSpecificMimeType(detected);
            if (!"application/octet-stream".equals(mt.toString())) {
                return new FxMimeType(mt.getMediaType(), mt.getSubType());
            }

        } 
        
        // extension based detection
        if (LOG.isDebugEnabled()) {
            LOG.debug("Failed to detect file's mime type from header, trying a file extension match: " + fileName);
        }
        // check manual extension replacement table
        if (fileName != null && fileName.indexOf('.') != -1) {
            final String extension = fileName.substring(fileName.lastIndexOf('.') + 1);
            if (EXT_REPLACEMENTS.containsKey(extension.toLowerCase())) {
                return new FxMimeType(EXT_REPLACEMENTS.get(extension));
            }
        }
        
        // use extension detector from mime-utils
        if (StringUtils.isNotBlank(fileName)) {
            detected = MimeUtil.getMimeTypes(fileName);
        }
        
        if(detected.isEmpty()) {
            return new FxMimeType(UNKNOWN);
        }
        // the first mime-type from mime-util's mapping table is usually fine
        mt = detected.iterator().next();
        return new FxMimeType(mt.getMediaType(), mt.getSubType());
    }

    private static void initialize() {
        if (DETECTORS_INITIALIZED) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (DETECTORS_INITIALIZED) {
                // someone else beat us to it
                return;
            }
            for (String detector : MIME_DETECTORS) {
                try {
                    MimeUtil.registerMimeDetector(detector);
                } catch (Exception e) {
                    if (LOG.isInfoEnabled()) {
                        LOG.info("Failed to initialize MIME detector " + detector + " (ignored): "
                                + e.getMessage(), e);
                    }
                }
            }
            DETECTORS_INITIALIZED = true;
        }
    }

    /**
     * Shutdown the MIME detectors. Needed since some (OpendesktopMimeDetector!) open resources or spawn
     * threads that must be explicitly terminated by the application.
     *
     * @since 3.1.4
     */
    public static void shutdownDetectors() {
        if (!DETECTORS_INITIALIZED) {
            return;     // nothing to do
        }
        for (String detector : MIME_DETECTORS) {
            try {
                MimeUtil.unregisterMimeDetector(detector);
            } catch (Exception e) {
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Failed to shutdown detector " + detector + ": " + e.getMessage(), e);
                }
            }
        }
    }
}
