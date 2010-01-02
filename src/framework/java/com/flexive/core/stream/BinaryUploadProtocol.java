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
package com.flexive.core.stream;

import com.flexive.core.storage.StorageManager;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.stream.BinaryUploadPayload;
import com.flexive.shared.structure.TypeStorageMode;
import com.flexive.stream.DataPacket;
import com.flexive.stream.StreamException;
import com.flexive.stream.StreamProtocol;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.Date;

/**
 * StreamServer protocol for receiving a binary stream
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class BinaryUploadProtocol extends StreamProtocol<BinaryUploadPayload> {

    protected static final Log LOG = LogFactory.getLog(BinaryUploadProtocol.class);

    private long timeToLive = 0;
    private String handle = null;
    private long count = 0;
    private int division = -1;
    private long expectedLength = 0;
    private boolean protoInit = false;
    private boolean rcvStarted = false;

    OutputStream pout = null;

    /**
     * Ctor
     */
    public BinaryUploadProtocol() {
        super(BinaryUploadPayload.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StreamProtocol<BinaryUploadPayload> getInstance() {
        return new BinaryUploadProtocol();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canHandle(DataPacket<BinaryUploadPayload> dataPacket) throws StreamException {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataPacket<BinaryUploadPayload> processPacket(DataPacket<BinaryUploadPayload> dataPacket) throws StreamException {
        if (dataPacket.isExpectResponse() && !this.protoInit) {
            this.protoInit = true;
            this.timeToLive = dataPacket.getPayload().getTimeToLive();
            this.expectedLength = dataPacket.getPayload().getExpectedLength();
            if (LOG.isDebugEnabled()) LOG.debug("Receive started at " + new Date(System.currentTimeMillis()));
            this.handle = RandomStringUtils.randomAlphanumeric(32);
            this.division = dataPacket.getPayload().getDivision();
            if( this.expectedLength == 0 ) {
                //create an empty transit entry
                try {
                    pout = StorageManager.getContentStorage(TypeStorageMode.Hierarchical).receiveTransitBinary(division, handle, expectedLength, timeToLive);
                } catch (Exception e) {
                    LOG.error(e);
                }
            }
            return new DataPacket<BinaryUploadPayload>(new BinaryUploadPayload(handle), false, this.expectedLength > 0);
        } else {
            cleanup();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean receiveStream(ByteBuffer buffer) throws IOException {
        if (!buffer.hasRemaining()) {
            //this can only happen on remote clients
            if (LOG.isDebugEnabled()) LOG.debug("aborting (empty)");
            return false;
        }
        if (!rcvStarted) {
            rcvStarted = true;
            if (LOG.isDebugEnabled()) LOG.debug("(internal serverside) receive start");
            try {
                pout = StorageManager.getContentStorage(TypeStorageMode.Hierarchical).receiveTransitBinary(division, handle, expectedLength, timeToLive);
            } catch (SQLException e) {
                LOG.error("SQL Error trying to receive binary stream: " + e.getMessage(), e);
            } catch (FxNotFoundException e) {
                LOG.error("Failed to lookup content storage for division #" + division + ": " + e.getLocalizedMessage());
            }
        }
        if (LOG.isDebugEnabled() && count + buffer.remaining() > expectedLength) {
            LOG.debug("poss. overflow: pos=" + buffer.position() + " lim=" + buffer.limit() + " cap=" + buffer.capacity());
            LOG.debug("Curr count: " + count + " count+rem=" + (count + buffer.remaining() + " delta:" + ((count + buffer.remaining()) - expectedLength)));
        }
        count += buffer.remaining();
        pout.write(buffer.array(), buffer.position(), buffer.remaining());
        buffer.clear();
        if (expectedLength > 0 && count >= expectedLength) {
            if (LOG.isDebugEnabled()) LOG.debug("aborting");
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void cleanup() {
        if (pout == null) {
            if (LOG.isDebugEnabled()) LOG.debug("skipping cleanup");
            return;
        }
        try {
            pout.flush();
            pout.close();
            pout = null;
        } catch (IOException e) {
            LOG.error("Failed to close binary output stream!", e);
        }
    }
}
