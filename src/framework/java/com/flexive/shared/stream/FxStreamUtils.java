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
package com.flexive.shared.stream;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.exceptions.FxStreamException;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.stream.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.List;

/**
 * Utitlity class to access the StreamServer
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxStreamUtils {

    private static final transient Log LOG = LogFactory.getLog(FxStreamUtils.class);

    /**
     * 5 minutes default time to live
     */
    public final static long DEFAULT_TTL = 5 * 60 * 1000;

    /**
     * Upload a binary (using an OutputStream) to the StreamServer with a default time to live
     *
     * @param length expected length of the stream/binary
     * @param stream the Stream containing the binary
     * @return payload containing server side handle of the binary, mimeType and meta data
     * @throws FxStreamException on errors
     * @see FxStreamUtils#DEFAULT_TTL
     */
    public static BinaryUploadPayload uploadBinary(long length, InputStream stream) throws FxStreamException {
        return uploadBinary(length, stream, DEFAULT_TTL);
    }

    /**
     * Retrieve a StreamClient, local servers are preferred, remote only as fallback
     *
     * @return StreamClient
     * @throws FxStreamException on errors
     */
    public static StreamClient getClient() throws FxStreamException {
        List<ServerLocation> servers = CacheAdmin.getStreamServers();
        try {
            StreamClient client;
//            client = StreamClientFactory.getRemoteClient(servers.get(0).getAddress(), servers.get(0).getPort());
            client = StreamClientFactory.getClient(servers);
//            System.out.println("StreamClient is " + (client.isLocal() ? "LOCAL" : "REMOTE"));
            return client;
        } catch (StreamException e) {
            throw new FxStreamException(e);
        }
    }

    /**
     * Upload a binary (using an OutputStream) to the StreamServer with a given time to live.
     * Warning: if using a remote connection, this method will return a few miliseconds before
     * all binary data is stored in the DB! Local connected clients will return *after* all
     * data is stored. This is currently a 'feature' that might be fixed sometime.
     *
     * @param length     expected length of the stream/binary
     * @param stream     the Stream containing the binary
     * @param timeToLive time in miliseconds the binary is guaranteed to exist server side (will be removed once expired)
     * @return payload containing server side handle of the binary, mimeType and meta data
     * @throws FxStreamException on errors
     */
    public static BinaryUploadPayload uploadBinary(long length, InputStream stream, long timeToLive) throws FxStreamException {
        StreamClient client = null;
        try {
            client = getClient();
//            client = StreamClientFactory.getRemoteClient(servers.get(0).getAddress(), servers.get(0).getPort());
            DataPacket<BinaryUploadPayload> req = new DataPacket<BinaryUploadPayload>(new BinaryUploadPayload(length, timeToLive), true);
            DataPacket<BinaryUploadPayload> resp = client.connect(req);
            if (resp.getPayload().isServerError())
                throw new FxStreamException("ex.stream.serverError", resp.getPayload().getErrorMessage());
            if (resp.isExpectStream())
                client.sendStream(stream, length);
            client.close();
            client = null;
            return resp.getPayload();
        } catch (StreamException e) {
            throw new FxStreamException(e);
        } finally {
            try {
                if (client != null)
                    client.close();
            } catch (StreamException e) {
                //ignore
            }
        }
    }

    /**
     * Probe all network interfaces and return the most suited to run a StreamServer on.
     * Preferred are interfaces that are not site local.
     *
     * @return best suited host to run a StreamServer
     * @throws UnknownHostException on errors
     */
    public static InetAddress probeNetworkInterfaces() throws UnknownHostException {
        try {
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            NetworkInterface nif;
            InetAddress preferred = null, fallback = null;
            while (nifs.hasMoreElements()) {
                nif = nifs.nextElement();
                if (LOG.isDebugEnabled())
                    LOG.debug("Probing " + nif.getDisplayName() + " ...");
                if (nif.getDisplayName().startsWith("vmnet"))
                    continue;
                Enumeration<InetAddress> inas = nif.getInetAddresses();
                while (inas.hasMoreElements()) {
                    InetAddress na = inas.nextElement();
                    if (LOG.isDebugEnabled())
                        LOG.debug("Probing " + nif.getDisplayName() + na);
                    if (!(na instanceof Inet4Address))
                        continue;
                    if (!na.isLoopbackAddress() && na.isReachable(1000)) {
                        if (preferred == null || (preferred.isSiteLocalAddress() && !na.isSiteLocalAddress()))
                            preferred = na;
                    }
                    if (fallback == null && na.isLoopbackAddress())
                        fallback = na;
                }
            }

            if (LOG.isDebugEnabled())
                LOG.debug("preferred: " + preferred + " fallback: " + fallback);
            if (preferred != null)
                return preferred;
            if (fallback != null)
                return fallback;
            return InetAddress.getLocalHost();
        } catch (Exception e) {
            return InetAddress.getLocalHost();
        }
    }

    public static void downloadBinary(BinaryDownloadCallback callback, List<ServerLocation> server, OutputStream stream, long binaryId, int binarySize) throws FxStreamException {
        if (server == null || server.size() == 0 || stream == null || (binarySize < 0 || binarySize > 3))
            throw new FxStreamException("ex.stream.download.param.missing");
        StreamClient client = null;
        try {
            client = getClient();
//            client = StreamClientFactory.getRemoteClient(servers.get(0).getAddress(), servers.get(0).getPort());
            DataPacket<BinaryDownloadPayload> req = new DataPacket<BinaryDownloadPayload>(
                    new BinaryDownloadPayload(binaryId, 1, 1, binarySize), true, true);
            DataPacket<BinaryDownloadPayload> resp = client.connect(req);
            if (resp.getPayload().isServerError())
                throw new FxStreamException("ex.stream.serverError", resp.getPayload().getErrorMessage());
            if (callback != null) {
                callback.setMimeType(resp.getPayload().getMimeType());
                callback.setBinarySize(resp.getPayload().getDatasize());
            }
            client.receiveStream(stream);
            client.close();
            client = null;
        } catch (StreamException e) {
            throw new FxStreamException(e);
        } finally {
            try {
                if (client != null)
                    client.close();
            } catch (StreamException e) {
                //ignore
            }
        }
    }

    public static void downloadBinary(List<ServerLocation> server, OutputStream stream, BinaryDescriptor descriptor) throws FxStreamException {
        if (server == null || server.size() == 0 || descriptor.getSize() <= 0 || stream == null)
            throw new FxStreamException("ex.stream.download.param.missing");
        StreamClient client = null;
        try {
            client = getClient();
//            client = StreamClientFactory.getRemoteClient(servers.get(0).getAddress(), servers.get(0).getPort());
            DataPacket<BinaryDownloadPayload> req = new DataPacket<BinaryDownloadPayload>(
                    new BinaryDownloadPayload(descriptor.getId(), descriptor.getVersion(), descriptor.getQuality()), true, true);
            DataPacket<BinaryDownloadPayload> resp = client.connect(req);
            if (resp.getPayload().isServerError())
                throw new FxStreamException("ex.stream.serverError", resp.getPayload().getErrorMessage());
            client.receiveStream(stream, descriptor.getSize());
            client.close();
            client = null;
        } catch (StreamException e) {
            throw new FxStreamException(e);
        } finally {
            try {
                if (client != null)
                    client.close();
            } catch (StreamException e) {
                //ignore
            }
        }
    }
}
