/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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
package com.flexive.shared.media.impl;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.media.FxMetadata;
import org.apache.commons.lang.StringUtils;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.common.ImageMetadata;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Java native Engine
 * This engine relies on java image io and apache sanselan
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxMediaNativeEngine {


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
        throw new FxApplicationException("ex.general.notImplemented", "scaling using " + FxMediaNativeEngine.class.getName());
    }

    /**
     * Identify a file, returning metadata
     *
     * @param mimeType if not null it will be used to call the correct identify routine
     * @param file     the file to identify
     * @return metadata
     * @throws FxApplicationException on errors
     */
    public static FxMetadata identify(String mimeType, File file) throws FxApplicationException {
        try {
            ImageInfo sii = Sanselan.getImageInfo(file);
            IImageMetadata md = Sanselan.getMetadata(file);
            List<FxMetadata.FxMetadataItem> metaItems;
            if( md == null || md.getItems() == null )
                metaItems = new ArrayList<FxMetadata.FxMetadataItem>(0);
            else {
                metaItems = new ArrayList<FxMetadata.FxMetadataItem>(md.getItems().size());
                for (Object o : md.getItems()) {
                    if (o instanceof ImageMetadata.Item) {
                        ImageMetadata.Item mdi = (ImageMetadata.Item) o;
                        if (!"Unknown".equals(mdi.getKeyword()))
                            metaItems.add(new FxMetadata.FxMetadataItem(mdi.getKeyword(), parseText(mdi.getText())));
                    }
                }
            }
            return new FxImageMetadataImpl(mimeType, file.getName(), metaItems, sii.getWidth(), sii.getHeight(),
                    sii.getFormat().name, sii.getFormatName(), sii.getCompressionAlgorithm(), sii.getPhysicalWidthDpi(),
                    sii.getPhysicalHeightDpi(), sii.getColorTypeDescription(), sii.getUsesPalette(), sii.getBitsPerPixel(),
                    sii.getIsProgressive(), sii.getIsTransparent(), Sanselan.getICCProfile(file));
        } catch (Exception e) {
            throw new FxApplicationException("ex.media.identify.error", (file == null ? "unknown" : file.getName()),
                    mimeType, e.getMessage());
        }
    }

    /**
     * Filter out '' from strings and avoid null-Strings
     *
     * @param text text to parse
     * @return filtered text
     */
    private static String parseText(String text) {
        if (StringUtils.isEmpty(text))
            return "";
        if (text.startsWith("'") && text.endsWith("'"))
            return text.substring(1, text.length() - 1);
        return text;
    }
}
