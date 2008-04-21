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
package com.flexive.shared.media;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.media.impl.FxMediaImageMagickEngine;
import com.flexive.shared.media.impl.FxMediaNativeEngine;
import com.flexive.shared.media.impl.FxUnknownMetadataImpl;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.Sanselan;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Media engine
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxMediaEngine {
    private static final transient Log LOG = LogFactory.getLog(FxMediaEngine.class);


    /**
     * Is ImageMagick installed?
     *
     * @return if ImageMagick is installed
     */
    public static boolean hasImageMagickInstalled() {
        return FxMediaImageMagickEngine.IM_AVAILABLE;
    }

    /**
     * Get the ImageMagick version as String
     *
     * @return ImageMagick version as String
     */
    public static String getImageMagickVersion() {
        return FxMediaImageMagickEngine.IM_VERSION;
    }

    /**
     * We need at least version 6.3.x of ImageMagick to be able to parse identify output
     *
     * @return can ImageMagick's identify be used?
     */
    public static boolean isImageMagickIdentifySupported() {
        return FxMediaImageMagickEngine.IM_IDENTIFY_POSSIBLE;
    }

    /**
     * Identify a file, returning metadata
     *
     * @param mimeType if not null it will be used to call the correct identify routine
     * @param file     the file to identify
     * @return metadata
     */
    public static FxMetadata identify(String mimeType, File file) {
        if (mimeType == null) {
            byte[] header = new byte[5];
            //read the first 5 bytes
            if (file.length() > 5) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    if (fis.read(header, 0, 5) != 5)
                        header = null;
                } catch (IOException e) {
                    LOG.error(e);
                } finally {
                    try {
                        if (fis != null)
                            fis.close();
                    } catch (IOException e) {
                        LOG.error(e);
                    }
                }
            }
            mimeType = detectMimeType(header, file.getName());
        }
        try {
            if (mimeType.startsWith("image")) {
                if (FxMediaImageMagickEngine.IM_IDENTIFY_POSSIBLE) {

                }
                //native
                return FxMediaNativeEngine.identify(mimeType, file);
            }
        } catch (FxApplicationException e) {
            LOG.error(e);
        }
        //last resort: unknown
        return new FxUnknownMetadataImpl(mimeType, file.getName());
    }

    /**
     * Scale an image and return the dimensions (width and height) as int array
     *
     * @param original  original file
     * @param scaled    scaled file
     * @param extension extension
     * @param width     desired width
     * @param height    desired height
     * @return actual width ([0]) and height ([1]) of scaled image
     * @throws FxApplicationException on errors
     */
    public static int[] scale(File original, File scaled, String extension, int width, int height) throws FxApplicationException {
        if (FxMediaImageMagickEngine.IM_AVAILABLE)
            return FxMediaImageMagickEngine.scale(original, scaled, extension, width, height);
        return FxMediaNativeEngine.scale(original, scaled, extension, width, height);
    }

    /**
     * Detect the mimetype of a file based on the first n bytes and the filename
     *
     * @param header first n bytes of the file to examine
     * @return detected mimetype
     */
    public static String detectMimeType(byte[] header) {
        return detectMimeType(header, null);
    }

    /**
     * Detect the mimetype of a file based on the first n bytes and the filename
     *
     * @param header   first n bytes of the file to examine
     * @param fileName filename
     * @return detected mimetype
     */
    public static String detectMimeType(byte[] header, String fileName) {
        if (header != null && header.length > 5) {
            try {
                ImageFormat iformat = Sanselan.guessFormat(header);
                if (iformat.actual) {
                    return "image/" + iformat.extension.toLowerCase();
                }
            } catch (Exception e) {
                LOG.error(e);
            }
        }
        if (!StringUtils.isEmpty(fileName) && fileName.indexOf('.') > 0) {
            //extension based detection
            fileName = fileName.trim().toUpperCase();
            if (fileName.endsWith(".JPG"))
                return "image/jpeg";
            if (fileName.endsWith(".GIF"))
                return "image/gif";
            if (fileName.endsWith(".PNG"))
                return "image/png";
            if (fileName.endsWith(".BMP"))
                return "image/bmp";
            if (fileName.endsWith(".DOC") || fileName.endsWith(".DOCX"))
                return "application/msword";
            if (fileName.endsWith(".XLS") || fileName.endsWith(".XLSX"))
                return "application/msexcel";
            if (fileName.endsWith(".PPT") || fileName.endsWith(".PPTX"))
                return "application/mspowerpoint";
            if (fileName.endsWith(".PDF"))
                return "application/pdf";
            if (fileName.endsWith(".HTM"))
                return "text/html";
            if (fileName.endsWith(".HTML"))
                return "text/html";
            if (fileName.endsWith(".TXT"))
                return "text/plain";
            if (fileName.endsWith(".ICO"))
                return "image/vnd.microsoft.icon";
        }
        //byte signature based detection
        if (header != null && header.length > 5 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) { //PNG
            return "image/png";
        }
        return "application/unknown";
    }
}
