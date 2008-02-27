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
package com.flexive.shared.media.impl;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.media.FxMetadata;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.sanselan.ImageFormat;
import org.apache.sanselan.ImageInfo;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.IImageMetadata;
import org.apache.sanselan.common.ImageMetadata;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Java native Engine
 * This engine relies on java image io and apache sanselan
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxMediaNativeEngine {
    private static final transient Log LOG = LogFactory.getLog(FxMediaNativeEngine.class);

    /**
     * Do we run in headless mode?
     */
    private final static boolean headless;

    static {
        if ("true".equals(System.getProperty("java.awt.headless"))) {
            headless = true;
        } else {
            // check if graphics environment is available
            boolean caughtException = false;
            try {
                GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
            } catch (HeadlessException e) {
                caughtException = true;
            }
            headless = caughtException;
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
        if( headless && FxMediaImageMagickEngine.IM_AVAILABLE && ".GIF".equals(extension)) {
            //native headless engine can't handle gif transparency ... so if we have IM we use it, else
            //transparent pixels will be black
            return FxMediaImageMagickEngine.scale(original, scaled, extension, width, height);
        }
        BufferedImage bi;
        try {
            bi = ImageIO.read(original);
        } catch (Exception e) {
            LOG.info("Failed to read " + original.getName() + " using ImageIO, trying sanselan");
            try {
                bi = Sanselan.getBufferedImage(original);
            } catch (Exception e1) {
                throw new FxApplicationException(LOG, "ex.media.readFallback.error", original.getName(), extension, e.getMessage(), e1.getMessage());
            }
        }
        int scaleWidth = bi.getWidth(null);
        int scaleHeight = bi.getHeight(null);
        double scaleX = (double) width / scaleWidth;
        double scaleY = (double) height / scaleHeight;
        double scale = Math.min(scaleX, scaleY);
        scaleWidth = (int) ((double) scaleWidth * scale);
        scaleHeight = (int) ((double) scaleHeight * scale);
        Image scaledImage;
        BufferedImage bi2;
        if (headless) {
            // create a new buffered image, don't rely on a local graphics system (headless mode)
            bi2 = new BufferedImage(scaleWidth, scaleHeight, BufferedImage.TYPE_INT_ARGB);
        } else {
            GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
            bi2 = gc.createCompatibleImage(scaleWidth, scaleHeight, bi.getTransparency());
        }
        Graphics2D g = bi2.createGraphics();
        if (scale < 0.3) {
            scaledImage = bi.getScaledInstance(scaleWidth, scaleHeight, Image.SCALE_SMOOTH);
            new ImageIcon(scaledImage).getImage();
            g.drawImage(scaledImage, 0, 0, scaleWidth, scaleHeight, null);
        } else {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.drawImage(bi, 0, 0, scaleWidth, scaleHeight, null);
        }
        g.dispose();
        String eMsg;
        boolean fallback;
        try {
            fallback = !ImageIO.write(bi2, extension.substring(1), scaled);
            eMsg = "No ImageIO writer found.";
        } catch (Exception e) {
            eMsg = e.getMessage();
            fallback = true;
        }
        if (fallback) {
            try {
                ImageFormat iFormat;
                if (".BMP".equals(extension))
                    iFormat = ImageFormat.IMAGE_FORMAT_BMP;
                else if (".TIF".equals(extension))
                    iFormat = ImageFormat.IMAGE_FORMAT_TIFF;
                else if (".PNG".equals(extension))
                    iFormat = ImageFormat.IMAGE_FORMAT_PNG;
                else
                    iFormat = ImageFormat.IMAGE_FORMAT_GIF;
                Sanselan.writeImage(bi2, scaled, iFormat, new HashMap());
            } catch (Exception e1) {
                throw new FxApplicationException(LOG, "ex.media.write.error", scaled.getName(), extension,
                        eMsg + ", " + e1.getMessage());
            }
        }
        return new int[]{scaleWidth, scaleHeight};
    }

    public static BufferedImage convertRGBAToIndexed(BufferedImage src) {
        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_BYTE_INDEXED);
        Graphics g = dest.getGraphics();
        g.setColor(new Color(231, 20, 189));
        g.fillRect(0, 0, dest.getWidth(), dest.getHeight()); //fill with a hideous color and make it transparent
        dest = makeTransparent(dest, 0, 0);
        dest.createGraphics().drawImage(src, 0, 0, null);
        return dest;
    }

    public static BufferedImage makeTransparent(BufferedImage image, int x, int y) {
        ColorModel cm = image.getColorModel();
        if (!(cm instanceof IndexColorModel))
            return image; //sorry...
        IndexColorModel icm = (IndexColorModel) cm;
        WritableRaster raster = image.getRaster();
        int pixel = raster.getSample(x, y, 0); //pixel is offset in ICM's palette
        int size = icm.getMapSize();
        byte[] reds = new byte[size];
        byte[] greens = new byte[size];
        byte[] blues = new byte[size];
        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);
        IndexColorModel icm2 = new IndexColorModel(8, size, reds, greens, blues, pixel);
        return new BufferedImage(icm2, raster, image.isAlphaPremultiplied(), null);
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
            if (md == null || md.getItems() == null)
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
