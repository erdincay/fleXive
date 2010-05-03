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

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.media.FxMetadata;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * A Class to extract audio meta data information for various supported audio formats.
 * Currently the following audio formats are supported:
 * - .mp3, .ogg, .wav, etc
 * <p/>
 * Relies on the following libraries:
 * javax.sound
 * jlayer
 * jorbis
 * mp3spi
 *
 * @author Christopher Blasnik, cblasnik@flexive.com, unique computing solutions gmbh
 */
public class FxAudioExtractionEngine {

    private static final Log LOG = LogFactory.getLog(FxAudioExtractionEngine.class);

    /**
     * Identify a file, returning metadata
     *
     * @param mimeType if not null it will be used to call the correct identify routine
     * @param file     the file to identify
     * @return metadata
     * @throws FxApplicationException on errors
     */
    public static FxMetadata identify(String mimeType, File file) throws FxApplicationException {
        final AudioExtractor extractor = new AudioExtractor(mimeType, file);
        extractor.extractAudioData();

        return new FxAudioMetaDataImpl(mimeType, file.getName(), extractor.getMetaItems(), extractor.getLength());
    }

    /**
     * Inner class to deal with the actual audio data extraction
     */
    static class AudioExtractor {
        private long length = 0L;
        private FxMimeType mimeType;
        private File file;
        private List<FxMetadata.FxMetadataItem> metaItems;

        AudioExtractor(String mimeType, File file) {
            this.mimeType = FxMimeType.getMimeType(mimeType);
            this.file = file;
        }

        /**
         * Main extraction method
         * Extraction is based on the given mime type
         */
        public void extractAudioData() {
            final String subType = mimeType.getSubType();
            // ogg and mp3
            if (subType.contains("ogg") || subType.contains("vorbis")) {
                extractOggAudioData();
            } else if (subType.contains("mp3") || subType.contains("mpeg")) {
                extractMp3AudioData();
            } else { // the (current) rest
                extractNativeAudioData();
            }
        }

        /**
         * Extract data for files supported by Java's sound api, e.g. wave, aiff, snd, au
         */
        private void extractNativeAudioData() {
            metaItems = new ArrayList<FxMetadata.FxMetadataItem>(5);
            AudioInputStream audioInStream = null;
            Clip clip = null;
            try {
                audioInStream = AudioSystem.getAudioInputStream(file);
                clip = AudioSystem.getClip();
                clip.open(audioInStream);
                // length in microseconds
                length = clip.getMicrosecondLength();
                // audio format info
                final AudioFormat audioFormat = clip.getFormat();
                // encoding
                metaItems.add(new FxMetadata.FxMetadataItem("encoding", audioFormat.getEncoding().toString()));
                // channels (1 or 2, 1 = mono, int)
                metaItems.add(new FxMetadata.FxMetadataItem("channels", String.valueOf(audioFormat.getChannels())));
                // sample rate (float)
                metaItems.add(new FxMetadata.FxMetadataItem("samplerate", String.valueOf(audioFormat.getSampleRate())));

            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Failed to get audio stream from file or could not recognise audio file format", e);
                }
            } finally {
                if (clip != null) {
                    clip.close();
                }
                FxSharedUtils.close(audioInStream);
            }
        }

        /**
         * TODO: Extract mp3 audio data
         */
        private void extractMp3AudioData() {
            
            metaItems = new ArrayList<FxMetadata.FxMetadataItem>(1);
        }

        /**
         * TODO: Extract ogg vorbis audio data
         */
        private void extractOggAudioData() {
            metaItems = new ArrayList<FxMetadata.FxMetadataItem>(1);
        }

        public long getLength() {
            return length;
        }

        public List<FxMetadata.FxMetadataItem> getMetaItems() {
            return metaItems;
        }
    }
}