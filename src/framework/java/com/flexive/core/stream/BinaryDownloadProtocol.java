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
package com.flexive.core.stream;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_CONTENT_BINARY;
import com.flexive.shared.media.FxMediaEngine;
import com.flexive.shared.stream.BinaryDownloadPayload;
import com.flexive.stream.DataPacket;
import com.flexive.stream.StreamException;
import com.flexive.stream.StreamProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Stream protocol to download binaries
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class BinaryDownloadProtocol extends StreamProtocol<BinaryDownloadPayload> {

    private Connection con = null;
    private PreparedStatement ps = null;
    private InputStream bin = null;
    private byte[] _buffer;
    private int pre_read = 0;

    public BinaryDownloadProtocol() {
        super(BinaryDownloadPayload.class);
    }

    @Override
    public StreamProtocol<BinaryDownloadPayload> getInstance() {
        return new BinaryDownloadProtocol();
    }

    @Override
    public boolean canHandle(DataPacket<BinaryDownloadPayload> dataPacket) throws StreamException {
        return true;
    }

    @Override
    public DataPacket<BinaryDownloadPayload> processPacket(DataPacket<BinaryDownloadPayload> dataPacket) throws StreamException {
        if (dataPacket.isExpectResponse() && dataPacket.isExpectStream()) {
            String mimeType;
            int datasize;
            try {
                con = Database.getDbConnection(dataPacket.getPayload().getDivision());
                String column = "FBLOB";
                String sizeColumn = "BLOBSIZE";
                switch (dataPacket.getPayload().getSize()) {
                    case 1:
                        column = "PREV1";
                        sizeColumn = "PREV1SIZE";
                        break;
                    case 2:
                        column = "PREV2";
                        sizeColumn = "PREV2SIZE";
                        break;
                    case 3:
                        column = "PREV3";
                        sizeColumn = "PREV3SIZE";
                        break;
                }
                long previewId = 0;
                ResultSet rs;
                if (!"FBLOB".equals(column)) {
                    //unless the real content is requested, try to find the correct preview image
                    ps = con.prepareStatement("SELECT PREVIEW_REF FROM " + TBL_CONTENT_BINARY +
                            " WHERE ID=? AND VER=? AND QUALITY=?");
                    ps.setLong(1, dataPacket.getPayload().getId());
                    ps.setInt(2, dataPacket.getPayload().getVersion());
                    ps.setInt(3, dataPacket.getPayload().getQuality());
                    rs = ps.executeQuery();
                    if (rs != null && rs.next()) {
                        previewId = rs.getLong(1);
                        if (rs.wasNull())
                            previewId = 0;
                    }
                    ps.close();
                }
                ps = con.prepareStatement("SELECT " + column + ",MIMETYPE," + sizeColumn + " FROM " + TBL_CONTENT_BINARY +
                        " WHERE ID=? AND VER=? AND QUALITY=?");
                if (previewId != 0)
                    ps.setLong(1, previewId);
                else
                    ps.setLong(1, dataPacket.getPayload().getId());
                ps.setInt(2, dataPacket.getPayload().getVersion());
                ps.setInt(3, dataPacket.getPayload().getQuality());
                rs = ps.executeQuery();
                if (rs == null || !rs.next()) {
                    return new DataPacket<BinaryDownloadPayload>(new BinaryDownloadPayload(true, "ex.stream.notFound"), false);
                }
                bin = rs.getBinaryStream(1);
                mimeType = rs.getString(2);
                datasize = rs.getInt(3);
                _buffer = new byte[4096];
                if (!mimeType.startsWith("image")) {
                    try {
                        pre_read = bin.read(_buffer, 0, 100);
                    } catch (IOException e) {
                        //silently ignore
                    }
                    String tmp = FxMediaEngine.detectMimeType(_buffer);
                    if (tmp.startsWith("image"))
                        mimeType = tmp;
                } else
                    pre_read = 0;
            } catch (SQLException e) {
                return new DataPacket<BinaryDownloadPayload>(new BinaryDownloadPayload(true, "SQL Error: " + e.getMessage()), false);
            } finally {
                if (bin == null)
                    Database.closeObjects(BinaryUploadProtocol.class, con, ps);
            }
            return new DataPacket<BinaryDownloadPayload>(new BinaryDownloadPayload(mimeType, datasize), false);
        }
        return null;
    }


    @Override
    public void cleanup() {
        if (bin != null) {
            Database.closeObjects(BinaryUploadProtocol.class, con, ps);
        }
    }

    /**
     * Callback for sending streamed data
     *
     * @param buffer ByteBuffer to write the data into
     * @return true if more data needs to be sent and this method should be called again
     * @throws IOException
     */
    @Override
    public boolean sendStream(ByteBuffer buffer) throws IOException {
        if (buffer.remaining() <= 0 || bin == null)
            return true; //should not happen but possible ...
        if (pre_read > 0) {
            buffer.put(_buffer, 0, pre_read); //size should not matter since its only 100 bytes
            pre_read = 0;
        }
        int max = Math.min(buffer.remaining(), _buffer.length);
        int read = bin.read(_buffer, 0, max);
        if (read == -1) {
            return false;
        }
        buffer.put(_buffer, 0, read);
        return true;
    }
}
