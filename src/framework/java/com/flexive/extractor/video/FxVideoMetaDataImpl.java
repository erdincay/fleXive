/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.extractor.video;

import com.flexive.shared.media.FxMediaType;
import com.flexive.shared.media.FxMetadata;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.List;

/**
 * Metadata holder for video files
 *
 * @author Laszlo Hernadi lhernadi@ucs.at
 * @since 3.1.3
 */
public class FxVideoMetaDataImpl extends FxVideoMetadata {

    private String mimeType;

    private String fileName;
    private List<FxMetadata.FxMetadataItem> metadata;
    // the length in micro seconds
    private long length;

    /**
     * @param mimeType the mime type
     * @param fileName the file name
     * @param metadata the List of FxMetadataItems
     * @param length   the length of the audio file in microseconds
     */
    public FxVideoMetaDataImpl(String fileName, String mimeType, List<FxMetadata.FxMetadataItem> metadata, long length) {
        this.fileName = fileName;
        this.length = length;
        this.metadata = metadata;
        this.mimeType = mimeType;
    }

    @Override
    public String getLengthAsString() {
        return String.valueOf(length);
    }

    @Override
    public long getLength() {
        return length;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxMediaType getMediaType() {
        return FxMediaType.Video;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMimeType() {
        return mimeType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getFilename() {
        return fileName;
    }

    @Override
    public boolean isVideoMetadata() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FxMetadata.FxMetadataItem> getMetadata() {
        return metadata;
    }

    @Override
    protected void writeXMLTags(XMLStreamWriter writer) throws XMLStreamException {
    }
}
