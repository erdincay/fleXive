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
package com.flexive.shared.media;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.media.impl.FxMediaImageMagickEngine;
import com.flexive.shared.media.impl.FxMediaNativeEngine;
import com.flexive.shared.media.impl.FxUnknownMetadataImpl;
import com.flexive.shared.stream.BinaryDownloadCallback;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

/**
 * Media engine
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxMediaEngine {
    private static final Log LOG = LogFactory.getLog(FxMediaEngine.class);

    private static final String CLS_AUDIO_EXTRACTOR = "com.flexive.extractor.audio.AudioExtractor";
    private static final String CLS_VIDEO_EXTRACTOR = "com.flexive.extractor.video.VideoExtractor";


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
        // image file identification
        if (mimeType.startsWith("image")) {
            try {
                //try native first
                return FxMediaNativeEngine.identify(mimeType, file);
            } catch (FxApplicationException e) {
                if (FxMediaImageMagickEngine.IM_IDENTIFY_POSSIBLE) {
                    try {
                        return FxMediaImageMagickEngine.identify(mimeType, file);
                    } catch (FxApplicationException e1) {
                        LOG.error(e1);
                    }
                } else
                    LOG.error(e);
            }
        } else if (mimeType.startsWith("audio")) {
            // audio file identification (optional) - TODO: do the same for documents
            // or make this really extensible
            final FxMetadata meta = invokeIdentify(mimeType, file, CLS_AUDIO_EXTRACTOR);
            if (meta != null) {
                return meta;
            }
            // video file identification
        } else if (mimeType.startsWith("video")) {
            // video file identification (optional) - TODO: do the same for documents
            // or make this really extensible
            final FxMetadata meta = invokeIdentify(mimeType, file, CLS_VIDEO_EXTRACTOR);
            if (meta != null) {
                return meta;
            }
        }
        //last resort: unknown
        return new FxUnknownMetadataImpl(mimeType, file.getName());
    }

    /**
     * Invoke the "identify" method on the given extractor class dynamically.
     *
     * @param mimeType           the binary mime type
     * @param file               the binary file
     * @param extractorClassName the fully qualified extractor class name
     * @return the extracted meta data, or null if the extractor is not available
     *         or the invokation threw an exception
     */
    private static FxMetadata invokeIdentify(String mimeType, File file, String extractorClassName) {
        try {
            final Class<?> cls = Class.forName(extractorClassName);
            final Method idMethod = cls.getMethod("identify", String.class, File.class);
            try {
                return (FxMetadata) idMethod.invoke(null, mimeType, file);
            } catch (Exception e) {
                if (LOG.isErrorEnabled()) {
                    LOG.error(e.getMessage(), e);
                }
                // error in extractor, fall through to unknown metadata
                return null;
            }
        } catch (ClassNotFoundException e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug(extractorClassName + " not available");
            }
            return null;
        } catch (NoSuchMethodException e) {
            if (LOG.isErrorEnabled()) {
                LOG.error(e.getMessage(), e);
            }
            throw new IllegalArgumentException(e);
        }
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
     * Manipulate image raw data and stream them back
     *
     * @param data     raw image data
     * @param out      stream
     * @param callback optional callback to set mimetype and size
     * @param mimeType mimetype
     * @param selector operations to apply
     * @throws FxApplicationException on errors
     */
    public static void streamingManipulate(byte[] data, OutputStream out, BinaryDownloadCallback callback, String mimeType, FxMediaSelector selector) throws FxApplicationException {
        //for now we have only a native implementation
        FxMediaNativeEngine.streamingManipulate(data, out, callback, mimeType, selector);
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
        return FxMediaNativeEngine.detectMimeType(header, fileName);
    }
}
