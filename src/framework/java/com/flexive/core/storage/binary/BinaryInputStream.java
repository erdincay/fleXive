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
package com.flexive.core.storage.binary;

import java.io.IOException;
import java.io.InputStream;

/**
 * Specialized InputStream for binaries that additionally returns the mimeType and size
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class BinaryInputStream extends InputStream {
    private InputStream stream;
    private String mimeType;
    private int size;
    private boolean binaryFound;

    /**
     * Ctor
     *
     * @param binaryFound was the binary found?
     * @param stream      wrapped stream
     * @param mimeType    mimeType
     * @param size        size
     */
    public BinaryInputStream(boolean binaryFound, InputStream stream, String mimeType, int size) {
        this.binaryFound = binaryFound;
        this.stream = stream;
        this.mimeType = mimeType;
        this.size = size;
    }

    /**
     * Was the binary found?
     *
     * @return binary found
     */
    public boolean isBinaryFound() {
        return binaryFound;
    }

    /**
     * Get the mimeType
     *
     * @return mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Get the size
     *
     * @return size
     */
    public int getSize() {
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
        return stream.read();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b) throws IOException {
        return stream.read(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return stream.read(b, off, len);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long skip(long n) throws IOException {
        return stream.skip(n);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int available() throws IOException {
        return stream.available();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
        if (stream != null)
            stream.close();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void mark(int readlimit) {
        stream.mark(readlimit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reset() throws IOException {
        stream.reset();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean markSupported() {
        return stream.markSupported();
    }
}
