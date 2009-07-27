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
package com.flexive.shared.value;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.ObjectWithLabel;
import com.flexive.shared.exceptions.FxStreamException;
import com.flexive.shared.stream.BinaryUploadPayload;
import com.flexive.shared.stream.FxStreamUtils;
import com.flexive.stream.ServerLocation;

import java.io.*;
import java.util.List;

/**
 * Descriptor for binaries (immutable).
 * Qualities and Versions are currently only planned features but not yet functional.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class BinaryDescriptor implements Serializable {
    private static final long serialVersionUID = -416186902840155773L;

    public final static int SYS_UNKNOWN = -1;
    public final static int SYS_NOACCESS = -2;
    public final static int SYS_AUDIO = -3;
    public final static int SYS_DOC = -4;
    public final static int SYS_ICAL = -5;
    public final static int SYS_INFO = -6;
    public final static int SYS_PDF = -7;
    public final static int SYS_TXT = -8;
    public final static int SYS_VIDEO = -9;
    public final static int SYS_XLS = -10;
    public final static int SYS_PPT = -6; //TODO: find a fitting thumbnail!
    public final static int SYS_HTML = -8; //TODO: find a fitting thumbnail!

    public final static int SYS_SELECTLIST_DEFAULT = -11;

    public final static String EMPTY = "[EMPTY]";
    /**
     * For images: box scaled size for preview 1
     */
    public final static int PREVIEW1_BOX = 42;
    /**
     * For images: box scaled size for preview 2
     */
    public final static int PREVIEW2_BOX = 85;
    /**
     * For images: box scaled size for preview 3
     */
    public final static int PREVIEW3_BOX = 232;

    /**
     * Enumeration of all available preview sizes.
     */
    public static enum PreviewSizes implements ObjectWithLabel {
        PREVIEW1(1, PREVIEW1_BOX),
        PREVIEW2(2, PREVIEW2_BOX),
        PREVIEW3(3, PREVIEW3_BOX),
        ORIGINAL(0, -1);

        private final int blobIndex;
        private final int size;

        /**
         * Ctor
         *
         * @param blobIndex blob index in the database
         * @param size      size constant
         */
        PreviewSizes(int blobIndex, int size) {
            this.blobIndex = blobIndex;
            this.size = size;
        }

        /**
         * Get the binary size constant
         *
         * @return binary size constant
         */
        public int getSize() {
            return size;
        }

        /**
         * Get the blob index in the database containing the binary
         *
         * @return blob index in the database containing the binary
         */
        public int getBlobIndex() {
            return blobIndex;
        }

        /**
         * {@inheritDoc}
         */
        public FxString getLabel() {
            return FxSharedUtils.getEnumLabel(this, size, size);
        }

        /**
         * Get a previewSize from a numeric String
         *
         * @param s String with numeric size
         * @return PreviewSizes
         */
        public static PreviewSizes fromString(String s) {
            int size = Integer.parseInt(s);
            for (PreviewSizes p : PreviewSizes.values())
                if (p.getBlobIndex() == size)
                    return p;
            return PreviewSizes.ORIGINAL;
        }

        /**
         * Get a previewSize from a numeric String
         *
         * @param size size constant
         * @return PreviewSizes
         */
        public static PreviewSizes fromSize(int size) {
            for (PreviewSizes p : PreviewSizes.values())
                if (p.getBlobIndex() == size)
                    return p;
            return PreviewSizes.ORIGINAL;
        }
    }

    private final String handle;
    private final boolean newBinary;
    private final List<ServerLocation> server;

    private long id = -1;
    private final int version;
    private final int quality;
    private final long creationTime;
    private final String name;
    private final long size;
    private final String metadata;
    private final String mimeType;
    private final boolean image;
    private final double resolution;
    private final int width;
    private final int height;

    /**
     * Constructor for a new empty binary
     */
    public BinaryDescriptor() {
        this(EMPTY);
    }

    /**
     * Constructor (for new Binaries)
     *
     * @param handle binary_transit handle
     */
    public BinaryDescriptor(String handle) {
        this(handle, EMPTY, -1, null, null);
    }

    /**
     * Constructor for new Binaries in prepareSave process ..
     *
     * @param handle   handle
     * @param name     name of the binary
     * @param size     size in bytes
     * @param mimeType MIME type
     * @param metadata xml meta data
     */
    public BinaryDescriptor(String handle, String name, long size, String mimeType, String metadata) {
        this.handle = handle;
        this.newBinary = true;
        this.name = name;
        this.size = size;
        this.metadata = metadata;
        this.mimeType = mimeType;
        this.server = null;
        this.id = -1;
        this.version = -1;
        this.quality = -1;
        this.creationTime = -1;
        this.image = false;
        this.resolution = 0;
        this.width = -1;
        this.height = -1;
    }

    /**
     * Constructor (for new Binaries)
     *
     * @param name         name of the binary
     * @param streamLength expected size of the binary
     * @param stream       an open input stream for the binary to upload
     * @throws FxStreamException on upload errors
     */
    public BinaryDescriptor(String name, long streamLength, InputStream stream) throws FxStreamException {
        this(FxStreamUtils.uploadBinary(streamLength, stream).getHandle(), name, streamLength,
                null, null);
    }

    /**
     * Constructor (for new Binaries)
     *
     * @param name         name of the binary
     * @param streamLength expected size of the binary
     * @param mimeType     the MIME type to be used, regardless of the stream content
     * @param stream       an open input stream for the binary to upload
     * @throws FxStreamException on upload errors
     * @since 3.1
     */
    public BinaryDescriptor(String name, long streamLength, String mimeType, InputStream stream) throws FxStreamException {
        this(FxStreamUtils.uploadBinary(streamLength, stream).getHandle(), name, streamLength,
                mimeType, null);
    }

    /**
     * Constructor (for new Binaries with unknown length - use with care since it will have to create a temp file to determine length!)
     *
     * @param name   name of the binary
     * @param stream an open input stream for the binary to upload
     * @throws FxStreamException on upload errors
     */
    public BinaryDescriptor(String name, InputStream stream) throws FxStreamException {
        this.name = name;

        File tmp = null;
        FileOutputStream fos = null;
        FileInputStream fin = null;
        final int BUF_SIZE = 4096;
        try {
            tmp = File.createTempFile("FxBinary", ".bin");
            fos = new FileOutputStream(tmp);
            byte[] buffer = new byte[BUF_SIZE];
            int read;
            while ((read = stream.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
            fos.flush();
            fos.close();
            fos = null;
            fin = new FileInputStream(tmp);
            this.size = tmp.length();
            BinaryUploadPayload payload = FxStreamUtils.uploadBinary(tmp.length(), fin);
            this.handle = payload.getHandle();
        } catch (IOException e) {
            throw new FxStreamException(e, "ex.stream", e.getMessage());
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                //ignore
            }
            try {
                if (fin != null)
                    fin.close();
            } catch (IOException e) {
                //ignore
            }
            if (tmp != null) {
                if (!tmp.delete())
                    tmp.deleteOnExit();
            }
        }
        this.newBinary = true;
        this.mimeType = null;
        this.metadata = null;
        this.server = null;
        this.id = -1;
        this.version = -1;
        this.quality = -1;
        this.creationTime = -1;
        this.image = false;
        this.resolution = 0;
        this.width = -1;
        this.height = -1;
    }


    /**
     * Constructor - used for loading from the content engine
     *
     * @param server       server location
     * @param id           binary id
     * @param version      binary version
     * @param quality      quality
     * @param creationTime timestamp when the binary data was created in the storage
     * @param name         name
     * @param size         size
     * @param metadata     xml metadata
     * @param mimeType     mime type
     * @param image        binary is an image?
     * @param resolution   resoltion (if image and detected)
     * @param width        width (if image and detected)
     * @param height       height (if image and detected)
     */
    public BinaryDescriptor(List<ServerLocation> server, long id, int version, int quality, long creationTime, String name, long size, String metadata, String mimeType, boolean image, double resolution, int width, int height) {
        this.newBinary = false;
        this.server = server;
        this.id = id;
        this.version = version;
        this.quality = quality;
        this.creationTime = creationTime;
        this.name = name;
        this.size = size;
        this.metadata = metadata;
        this.mimeType = mimeType;
        this.image = image;
        this.resolution = resolution;
        this.width = width;
        this.height = height;
        this.handle = null;
    }

    /**
     * Set the binary id, only allowed if not assigned previously
     *
     * @param binaryId binary id
     */
    public void setId(long binaryId) {
        if (this.id == -1)
            this.id = binaryId;
    }


    /**
     * Downloads the binary to the given stream.
     * The stream won't be flushed or closed!
     *
     * @param stream stream used for download
     * @throws FxStreamException on errors
     */
    public void download(OutputStream stream) throws FxStreamException {
        FxStreamUtils.downloadBinary(server, stream, this);
    }

    /**
     * Downloads the binary in the requested size (original or preview images if available) to the given stream.
     * The stream won't be flushed or closed!
     *
     * @param stream stream used for download
     * @param size   requested size (original or preview images if available)
     * @throws FxStreamException on errors
     */
    public void download(OutputStream stream, PreviewSizes size) throws FxStreamException {
        FxStreamUtils.downloadBinary(server, stream, this, size);
    }

    /**
     * Is this BinaryDescriptor empty?
     *
     * @return BinaryDescriptor is empty
     */
    public boolean isEmpty() {
        return EMPTY.equals(handle);
    }

    /**
     * Getter for the handle
     *
     * @return handle
     */
    public String getHandle() {
        return handle;
    }

    /**
     * Is this a new binary?
     *
     * @return new binary?
     */
    public boolean isNewBinary() {
        return newBinary;
    }

    /**
     * Getter for the binary id (not content instance id!)
     *
     * @return binary id
     */
    public long getId() {
        return id;
    }

    /**
     * Getter for the binary version
     *
     * @return binary version
     */
    public int getVersion() {
        return version;
    }

    /**
     * Getter for the quality
     *
     * @return quality
     */
    public int getQuality() {
        return quality;
    }

    /**
     * Getter for the creation timestamp
     *
     * @return creation timestamp
     */
    public long getCreationTime() {
        if (creationTime == -1)
            return System.currentTimeMillis();
        return creationTime;
    }

    /**
     * Name of the binary
     *
     * @return name of the binary
     */
    public String getName() {
        return name;
    }

    /**
     * Getter for the binary size in bytes
     *
     * @return binary size in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * Getter for optional metadata (usually XML)
     *
     * @return optional metadata (usually XML)
     */
    public String getMetadata() {
        return metadata;
    }

    /**
     * Getter for the mime type (if available)
     *
     * @return mime type (if available)
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Is this binary an image?
     *
     * @return binary is an image
     */
    public boolean isImage() {
        return image;
    }

    /**
     * If binary is an image, getter for the resolution in dpi
     *
     * @return resolution in dpi
     */
    public double getResolution() {
        return resolution;
    }

    /**
     * If binary is an image, getter for the width in pixel
     *
     * @return width in pixel
     */
    public int getWidth() {
        return width;
    }

    /**
     * If binary is an image, getter for the height in pixel
     *
     * @return height in pixel
     */
    public int getHeight() {
        return height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BinaryDescriptor)) return false;
        BinaryDescriptor b = (BinaryDescriptor) obj;
//        if( this.isNewBinary() && b.isNewBinary() )
//            return true;
        return !(b.getMetadata() != null && !b.getMetadata().equals(this.getMetadata())) &&
                !(this.getMetadata() != null && !this.getMetadata().equals(b.getMetadata())) &&
                !(this.getMetadata() == null && b.getMetadata() != null) &&
                !(b.getMetadata() == null && this.getMetadata() != null) &&
                !(b.getMimeType() != null && !b.getMimeType().equals(this.getMimeType())) &&
                !(this.getMimeType() != null && !this.getMimeType().equals(b.getMimeType())) &&
                !(this.getMimeType() == null && b.getMimeType() != null) &&
                !(b.getMimeType() == null && this.getMimeType() != null) &&
                !(this.handle != null && !this.handle.equals(b.handle)) &&
                !(this.id != -1 && this.id != b.id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return (int) this.id + (this.handle != null ? 31 * this.handle.hashCode() : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.getName() + " " + (this.mimeType == null ? "unknown/unknown" : this.mimeType);
    }
}
