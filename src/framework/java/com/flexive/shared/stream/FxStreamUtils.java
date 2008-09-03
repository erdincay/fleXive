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
package com.flexive.shared.stream;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.media.FxMediaEngine;
import com.flexive.shared.media.FxMediaSelector;
import com.flexive.shared.exceptions.FxStreamException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.stream.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Utitlity class to access the StreamServer
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxStreamUtils {

    private static final Log LOG = LogFactory.getLog(FxStreamUtils.class);

    /**
     * 5 minutes default time to live
     */
    public final static long DEFAULT_TTL = 5 * 60 * 1000;

    /**
     * List of local servers
     */
    private volatile static List<ServerLocation> localServers = new ArrayList<ServerLocation>(5);

    /**
     * Add a local server
     *
     * @param serverLocation local server location
     */
    public static void addLocalServer(ServerLocation serverLocation) {
        if (!localServers.contains(serverLocation))
            localServers.add(serverLocation);
    }

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
     * Retrieve a StreamClient, local servers are preferred, remote only as fallback.
     * Remote servers will be fetched from the cache
     *
     * @return StreamClient
     * @throws FxStreamException on errors
     */
    public static StreamClient getClient() throws FxStreamException {
        return getClient(null);
    }

    /**
     * Retrieve a StreamClient, local servers are preferred, remote only as fallback
     *
     * @param servers (optional) known remote servers
     * @return StreamClient
     * @throws FxStreamException on errors
     */
    public static StreamClient getClient(List<ServerLocation> servers) throws FxStreamException {
        if (servers == null)
            servers = CacheAdmin.getStreamServers();
        try {
            StreamClient client;
            List<ServerLocation> allServers = new ArrayList<ServerLocation>(servers.size() + localServers.size());
            allServers.addAll(localServers);
            allServers.addAll(servers);
            if (allServers.size() == 0)
                throw new FxStreamException("ex.stream.download.param.server");
            client = StreamClientFactory.getClient(allServers);
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

    /**
     * Download a binary
     *
     * @param callback   callback handler to set mimetype and size for downloads
     * @param server     (optional) list of remote stream servers
     * @param stream     the output stream to send the binary to
     * @param binaryId   id of the binary
     * @param selector   selector of binary to use and for optional manipulations to perform
     * @throws FxStreamException on errors
     */
    @SuppressWarnings({"UnusedAssignment"})
    public static void downloadBinary(BinaryDownloadCallback callback, List<ServerLocation> server, OutputStream stream, long binaryId, FxMediaSelector selector) throws FxStreamException {
        FxSharedUtils.checkParameterEmpty(stream, "stream");
        StreamClient client = null;
        try {
            client = getClient(server);
//            client = StreamClientFactory.getRemoteClient(servers.get(0).getAddress(), servers.get(0).getPort());
            DataPacket<BinaryDownloadPayload> req = new DataPacket<BinaryDownloadPayload>(
                    new BinaryDownloadPayload(binaryId, 1, 1, selector.getSize().getBlobIndex()), true, true);
            DataPacket<BinaryDownloadPayload> resp = client.connect(req);
            if (resp.getPayload().isServerError())
                throw new FxStreamException("ex.stream.serverError", resp.getPayload().getErrorMessage());
            if (!selector.isApplyManipulations()) {
                if (callback != null) {
                    callback.setMimeType(resp.getPayload().getMimeType());
                    callback.setBinarySize(resp.getPayload().getDatasize());
                }
                client.receiveStream(stream);
                client.close();
            } else {
                //perform on-the-fly modifications
                ByteArrayOutputStream _out = new ByteArrayOutputStream(resp.getPayload().getDatasize());
                client.receiveStream(_out);
                client.close();
                byte[] _data = _out.toByteArray();
                _out = null;
                try {
                    FxMediaEngine.streamingManipulate(_data, stream, callback, resp.getPayload().getMimeType(), selector);
                } catch (FxApplicationException e) {
                    throw new FxStreamException(e);
                }
            }
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

    /**
     * Download a binary
     *
     * @param server     (optional) list of remote stream servers
     * @param stream     the output stream to send the binary to
     * @param descriptor binary descriptor
     * @throws FxStreamException on errors
     */
    public static void downloadBinary(List<ServerLocation> server, OutputStream stream, BinaryDescriptor descriptor) throws FxStreamException {
        if (descriptor.getSize() <= 0 || stream == null)
            throw new FxStreamException("ex.stream.download.param.missing");
        StreamClient client = null;
        try {
            client = getClient(server);
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
