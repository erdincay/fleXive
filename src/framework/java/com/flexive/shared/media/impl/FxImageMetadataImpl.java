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

import com.flexive.shared.media.FxImageMetadata;
import com.flexive.shared.media.FxMediaType;
import com.flexive.shared.FxXMLUtils;
import static com.flexive.shared.FxXMLUtils.writeSimpleTag;

import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.XMLStreamException;
import java.awt.color.ICC_Profile;
import java.util.List;

/**
 * Image metadata
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxImageMetadataImpl extends FxImageMetadata {
    private String mimeType;
    private String filename;
    private List<FxMetadataItem> metadata;
    private int width;
    private int height;
    private String format;
    private String formatDescription;
    private String compressionAlgorithm;
    private double xResolution;
    private double yResolution;
    private String colorType;
    private boolean usePalette;
    private int bpp;
    private boolean progressive;
    private boolean transparent;
    private ICC_Profile icc;
    
    private static final double DEFAULT_RESOLUTION = 72.0d;

    public FxImageMetadataImpl(String mimeType, String filename, List<FxMetadataItem> metadata, int width, int height,
                               String format, String formatDescription, String compressionAlgorithm,
                               double xResolution, double yResolution, String colorType, boolean usePalette, int bpp,
                               boolean progressive, boolean transparent, ICC_Profile icc) {
        this.mimeType = mimeType;
        this.filename = filename;
        this.metadata = metadata;
        this.width = width;
        this.height = height;
        this.format = format;
        this.formatDescription = formatDescription;
        if( compressionAlgorithm != null ) {
            if( compressionAlgorithm.startsWith("RLE"))
                compressionAlgorithm = "RLE";
            else if( compressionAlgorithm.startsWith("PNG"))
                compressionAlgorithm = "PNG";
            else if( compressionAlgorithm.startsWith("CCITT Group 3"))
                compressionAlgorithm = "CCITT_GROUP_3 ";
            else if( compressionAlgorithm.startsWith("CCITT Group 4"))
                compressionAlgorithm = "CCITT_GROUP_3 ";
            else if( compressionAlgorithm.equals("CCITT 1D"))
                compressionAlgorithm = "CCITT_GROUP_1D ";
        }
        this.compressionAlgorithm = compressionAlgorithm;
        if( xResolution < 0 )
            this.xResolution = DEFAULT_RESOLUTION;
        else
            this.xResolution = xResolution;
        if( yResolution < 0 )
            this.yResolution = DEFAULT_RESOLUTION;
        else
            this.yResolution = yResolution;
        this.colorType = colorType;
        this.usePalette = usePalette;
        this.bpp = bpp;
        this.progressive = progressive;
        this.transparent = transparent;
        this.icc = icc;

    }

    /**
     * {@inheritDoc}
     */
    public FxMediaType getMediaType() {
        return FxMediaType.Image;
    }

    /**
     * {@inheritDoc}
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * {@inheritDoc}
     */
    public String getFilename() {
        return filename;
    }

    /**
     * {@inheritDoc}
     */
    public List<FxMetadataItem> getMetadata() {
        return metadata;
    }

    /**
     * {@inheritDoc}
     */
    public int getWidth() {
        return width;
    }

    /**
     * {@inheritDoc}
     */
    public int getHeight() {
        return height;
    }

    /**
     * {@inheritDoc}
     */
    public String getFormat() {
        return format;
    }

    /**
     * {@inheritDoc}
     */
    public String getFormatDescription() {
        return formatDescription;
    }

    /**
     * {@inheritDoc}
     */
    public String getCompressionAlgorithm() {
        return compressionAlgorithm;
    }

    /**
     * {@inheritDoc}
     */
    public double getXResolution() {
        return xResolution;
    }

    /**
     * {@inheritDoc}
     */
    public double getYResolution() {
        return yResolution;
    }

    /**
     * {@inheritDoc}
     */
    public String getColorType() {
        return colorType;
    }

    /**
     * {@inheritDoc}
     */
    public boolean usePalette() {
        return usePalette;
    }

    /**
     * {@inheritDoc}
     */
    public int getBitsPerPixel() {
        return bpp;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isProgressive() {
        return progressive;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isTransparent() {
        return transparent;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasICC_Profile() {
        return icc != null;
    }

    /**
     * {@inheritDoc}
     */
    public ICC_Profile getICC_Profile() {
        return icc;
    }

    /**
     * {@inheritDoc}
     */
    protected void writeXMLTags(XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("imageData");
        writeSimpleTag(writer, "width", getWidth(), false);
        writeSimpleTag(writer, "height", getHeight(), false);
        writeSimpleTag(writer, "bpp", getBitsPerPixel(), false);
        writeSimpleTag(writer, "colorType", getColorType(), true);
        writeSimpleTag(writer, "compressionAlgorithm", getCompressionAlgorithm(), true);
        writeSimpleTag(writer, "format", getFormat(), true);
        writeSimpleTag(writer, "formatDescription", getFormatDescription(), true);
        writeSimpleTag(writer, "xResolution", getXResolution(), false);
        writeSimpleTag(writer, "yResolution", getYResolution(), false);
        writer.writeEndElement();
    }
}
