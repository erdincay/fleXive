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

    public String getLengthAsString() {
        return String.valueOf(length);
    }

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

    protected void writeXMLTags(XMLStreamWriter writer) throws XMLStreamException {
    }
}
