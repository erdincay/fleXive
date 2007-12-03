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
package com.flexive.core.stream;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.shared.stream.BinaryUploadPayload;
import com.flexive.stream.DataPacket;
import com.flexive.stream.StreamException;
import com.flexive.stream.StreamProtocol;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * StreamServer protocol for receiving a binary stream
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class BinaryUploadProtocol extends StreamProtocol<BinaryUploadPayload> implements Runnable {

    protected static final transient Log LOG = LogFactory.getLog(BinaryUploadProtocol.class);

    private long timeToLive = 0;
    private String handle = null;
    private long count = 0;
    private int division = -1;
    private long rcvTime = 0;
    private long expectedLength = 0;
    private boolean protoInit = false;
    private boolean rcvStarted = false;
    private Thread rcvThread = null;

    PipedInputStream pin = null;
    PipedOutputStream pout = null;

    public BinaryUploadProtocol() {
        super(BinaryUploadPayload.class);
    }

    @Override
    public StreamProtocol<BinaryUploadPayload> getInstance() {
        return new BinaryUploadProtocol();
    }

    @Override
    public boolean canHandle(DataPacket<BinaryUploadPayload> dataPacket) throws StreamException {
        return true;
    }

    @Override
    public DataPacket<BinaryUploadPayload> processPacket(DataPacket<BinaryUploadPayload> dataPacket) throws StreamException {
        if (dataPacket.isExpectResponse() && !this.protoInit) {
            this.protoInit = true;
            this.timeToLive = dataPacket.getPayload().getTimeToLive();
            this.expectedLength = dataPacket.getPayload().getExpectedLength();
            if (LOG.isDebugEnabled()) LOG.debug("Receive started at " + new Date(System.currentTimeMillis()));
            this.handle = RandomStringUtils.randomAlphanumeric(32);
            this.division = dataPacket.getPayload().getDivision();
            return new DataPacket<BinaryUploadPayload>(new BinaryUploadPayload(handle), false, true);
        } else {
            if (LOG.isDebugEnabled()) LOG.debug("so finished ...");
            cleanup();
        }
        return null;
    }


    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p/>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public void run() {
        if (LOG.isDebugEnabled()) LOG.debug("thread started");
        try {
            createBlob();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (LOG.isDebugEnabled()) LOG.debug("thread finished");
    }

    /**
     * Create an entry in the transit table
     *
     * @throws SQLException on errors
     * @throws IOException  on errors
     */
    private void createBlob() throws SQLException, IOException {
        //see: http://bugs.mysql.com/bug.php?id=7745
        //default max allowed packet size is 1M
        //info about longblob: http://dev.mysql.com/doc/refman/5.0/en/blob.html

        long time;
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection(division);
            ps = con.prepareStatement("INSERT INTO " + DatabaseConst.TBL_BINARY_TRANSIT + " (BKEY,FBLOB,TFER_DONE,EXPIRE) VALUES(?,?,FALSE,?)");
            ps.setString(1, handle);
            ps.setBinaryStream(2, pin, (int) expectedLength);
            ps.setTimestamp(3, new Timestamp(System.currentTimeMillis() + timeToLive));
            time = System.currentTimeMillis();
            ps.executeUpdate();
            if (LOG.isDebugEnabled())
                LOG.debug("Stored " + count + " bytes in " + (System.currentTimeMillis() - time) + "[ms] in DB");
            try {
                pin.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } finally {
            Database.closeObjects(BinaryUploadProtocol.class, con, ps);
        }
    }

    /**
     * Mark the blob as received, set the size and generate meta data if applicable
     *
     * @param size size transferred
     * @throws SQLException on errors
     */
    private void markReceived(long size) throws SQLException {
        Connection con = null;
        PreparedStatement ps = null;
        try {
            con = Database.getDbConnection(division);

            ps = con.prepareStatement("UPDATE " + DatabaseConst.TBL_BINARY_TRANSIT + " SET TFER_DONE=?, BLOBSIZE=? WHERE BKEY=?");
            ps.setBoolean(1, true);
            ps.setLong(2, size);
            ps.setString(3, handle);
            ps.executeUpdate();
        } finally {
            Database.closeObjects(BinaryUploadProtocol.class, con, ps);
        }
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
            pin = new PipedInputStream();
            pout = new PipedOutputStream(pin);
            rcvThread = new Thread(this);
            rcvThread.setDaemon(true);
            rcvThread.start();
            rcvTime = System.currentTimeMillis();
        }
        if (LOG.isDebugEnabled() && count + buffer.remaining() > expectedLength) {
            LOG.debug("poss. overflow: pos=" + buffer.position() + " lim=" + buffer.limit() + " cap=" + buffer.capacity());
            LOG.debug("Curr count: " + count + " count+rem=" + (count + buffer.remaining() + " delta:" + ((count + buffer.remaining()) - expectedLength)));
        }
        count += buffer.remaining();
        pout.write(buffer.array(), buffer.position(), buffer.remaining());
//        System.out.println("Received: "+count+"/"+expectedLength+". expecting more");
        buffer.clear();
        if (expectedLength > 0 && count >= expectedLength) {
            if (LOG.isDebugEnabled()) LOG.debug("aborting");
            return false;
        }
        return true;
    }


    @Override
    public synchronized void cleanup() {
        if (pout == null) {
            if (LOG.isDebugEnabled()) LOG.debug("skipping cleanup");
            return;
        }
        try {
            pout.close();
            pout = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            rcvThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            markReceived(count);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Received " + count + " bytes in " + (System.currentTimeMillis() - rcvTime) + "[ms]");
            LOG.debug("===================================================");
            LOG.debug("=== Finished binary receive. Total length: " + count);
            LOG.debug("=== Handle was: " + handle);
            LOG.debug("===================================================");
        }
    }
}
