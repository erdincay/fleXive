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
package com.flexive.ejb.mbeans;

import com.flexive.core.stream.BinaryDownloadProtocol;
import com.flexive.core.stream.BinaryUploadProtocol;
import com.flexive.core.structure.StructureLoader;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.cache.FxBackingCache;
import com.flexive.shared.cache.FxBackingCacheProvider;
import com.flexive.shared.cache.FxBackingCacheProviderFactory;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.mbeans.FxCacheMBean;
import com.flexive.shared.mbeans.FxCacheProxy;
import com.flexive.shared.mbeans.MBeanHelper;
import com.flexive.shared.stream.FxStreamUtils;
import com.flexive.stream.ServerLocation;
import com.flexive.stream.StreamServer;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Cache;

import javax.management.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * FxCache MBean
 * TODO: implement missing skeletons ...
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
//@Service(objectName = CacheAdmin.CACHE_SERVICE_NAME)
public class FxCache implements FxCacheMBean, DynamicMBean {
    private static final Log LOG = LogFactory.getLog(FxCache.class);


    private StreamServer server;
    private ServerLocation serverLocation;
    private long nodeStartupTime = -1;

    private FxBackingCacheProvider cacheProvider = null;


    /**
     * Get the backing cache
     *
     * @return FxBackingCache
     * @throws FxCacheException on errors
     */
    private FxBackingCache getBackingCache() throws FxCacheException {
        if (cacheProvider == null) {
            //start the cache
            cacheProvider = FxBackingCacheProviderFactory.createNew();
            LOG.info("Starting backing Cache {" + cacheProvider.getDescription() + "}");
            cacheProvider.init();
            if (cacheProvider.getInstance().get("/" + this.getClass().getName(), SYSTEM_UP_KEY) == null) {
                cacheProvider.getInstance().put("/" + this.getClass().getName(), SYSTEM_UP_KEY, System.currentTimeMillis());
            }
            nodeStartupTime = System.currentTimeMillis();
        }
        return cacheProvider.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public synchronized void create() throws Exception {
        if (server != null) return;

        //switch to UTF-8 encoding
        if (!"UTF-8".equals(System.getProperty("file.encoding"))) {
            // set default charset to UTF-8
            LOG.warn("Changing system character encoding from " + System.getProperty("file.encoding") + " to UTF-8.");
            System.setProperty("file.encoding", "UTF-8");
        }
        //start streamserver
        try {
            int port = DEFAULT_STREAMING_PORT;
            if( System.getProperty(STREAMING_PORT_PROPERTY) != null ) {
                String _port = System.getProperty(STREAMING_PORT_PROPERTY);
                try {
                    port = Integer.parseInt(_port);
                } catch (NumberFormatException e) {
                    //ignore port
                    LOG.error("Invalid streaming server port provided: ["+_port+"], using default port ["+port+"]");
                }
            }
            server = new StreamServer(FxStreamUtils.probeNetworkInterfaces(), port);
            server.addProtocol(new BinaryUploadProtocol());
            server.addProtocol(new BinaryDownloadProtocol());
            server.start();

            // update streaming server list in shared cache.
            // NOTE! Since we are operating directly on the backing cache, we have to do the actions
            // provided by FxCacheProxy (mapping paths, serializing values) manually
            
            final List<ServerLocation> servers = getCachedServerList();
            serverLocation = new ServerLocation(server.getAddress().getAddress(), server.getPort());
            if ((serverLocation.getAddress().isLinkLocalAddress() || serverLocation.getAddress().isAnyLocalAddress() || serverLocation.getAddress().isLoopbackAddress()))
                FxStreamUtils.addLocalServer(serverLocation);
            else if (!servers.contains(serverLocation)) //only add if not contained already and not bound to a local address
                servers.add(serverLocation);
            updateCachedServerList(servers);
            
            LOG.info("Added " + serverLocation + " to available StreamServers (" + servers.size() + " total) for cache " + getBackingCache().toString());
        } catch (Exception e) {
            LOG.error("Failed to start StreamServer. Error: " + e.getMessage(), e);
        }
    }


    private List<ServerLocation> getCachedServerList() throws FxCacheException {
        final String configPath = FxCacheProxy.globalDivisionEncodePath(CacheAdmin.STREAMSERVER_BASE);
        if (globalExists(configPath, CacheAdmin.STREAMSERVER_EJB_KEY)) {
            return Lists.newArrayList(
                    (List<ServerLocation>) FxCacheProxy.unmarshal(globalGet(configPath,
                    CacheAdmin.STREAMSERVER_EJB_KEY))
            );
        }
        return new ArrayList<ServerLocation>(5);
    }

    private void updateCachedServerList(List<ServerLocation> servers) throws FxCacheException {
        globalPut(FxCacheProxy.globalDivisionEncodePath(CacheAdmin.STREAMSERVER_BASE),
                CacheAdmin.STREAMSERVER_EJB_KEY, 
                FxCacheProxy.marshal((Serializable) servers)
        );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void destroy() throws Exception {
        //System.out.println("about to uninstall timer");
        //EJBLookup.getTimerServiceInterface().uninstall();
        //System.out.println("timers uninstalled");
        stopLocalStreamServers();
        try {
            if (cacheProvider != null) {
                cleanupAfterRequest();
                cacheProvider.shutdown();
                cacheProvider = null;
            }
        } catch (FxCacheException e) {
            LOG.error(e, e);
        }
    }

    private void stopLocalStreamServers() {
        if (server != null) {
            try {
                LOG.info("Shutting down StreamServer {" + server.getDescription() + "}");
                try {
                    server.stop();
                } catch (Exception e) {
                    LOG.error(e, e);
                }
                server = null;
                if (serverLocation != null) {
                    final List<ServerLocation> servers = getCachedServerList();
                    servers.remove(serverLocation);
                    updateCachedServerList(servers);
                    serverLocation = null;
                }
            } catch (FxCacheException ex) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Failed to stop local stream server: " + ex.getMessage(), ex);
                }
            }
        }
    }

    //TODO: finish me!
    private static MBeanInfo info = new MBeanInfo(
            FxCache.class.getCanonicalName(),
            "[fleXive] Cache MBean",
            new MBeanAttributeInfo[]{
                    new MBeanAttributeInfo("FxCache", FxCache.class.getCanonicalName(), "", true, false, false)
            },
            new MBeanConstructorInfo[]{
            },
            new MBeanOperationInfo[]{
                    new MBeanOperationInfo("get", "",
                            new MBeanParameterInfo[]{
                                    new MBeanParameterInfo("path", "java.lang.String", ""),
                                    new MBeanParameterInfo("key", "java.lang.Object", "")
                            }, "java.lang.Object", MBeanOperationInfo.INFO),
                    new MBeanOperationInfo("put", "",
                            new MBeanParameterInfo[]{
                                    new MBeanParameterInfo("path", "java.lang.String", ""),
                                    new MBeanParameterInfo("key", "java.lang.Object", ""),
                                    new MBeanParameterInfo("value", "java.lang.Object", "")
                            }, "void", MBeanOperationInfo.ACTION)
            },
            new MBeanNotificationInfo[]{
            }
    );


    /**
     * {@inheritDoc}
     */
    public String getDeploymentId() {
        return MBeanHelper.DEPLOYMENT_ID;
    }

    /**
     * {@inheritDoc}
     */
    public Cache<Object, Object> getCache() throws FxCacheException {
        return getBackingCache().getCache();
    }

    /**
     * {@inheritDoc}
     */
    public Object get(String path, Object key) throws FxCacheException {
        return getBackingCache().get(path, key);
    }

    /**
     * {@inheritDoc}
     */
    public Object globalGet(String path, Object key) throws FxCacheException {
        return getBackingCache().get(path, key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean globalExists(String path, Object key) throws FxCacheException {
        return getBackingCache().exists(path, key);
    }

    /**
     * {@inheritDoc}
     */
    public void put(String path, Object key, Object value) throws FxCacheException {
        getBackingCache().put(path, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(String path, Object key) throws FxCacheException {
        return getBackingCache().exists(path, key);
    }


    /**
     * {@inheritDoc}
     */
    public void globalPut(String path, Object key, Object value) throws FxCacheException {
        getBackingCache().put(path, key, value);
    }

    /**
     * {@inheritDoc}
     */
    public void remove(String path) throws FxCacheException {
        getBackingCache().remove(path);
    }

    /**
     * {@inheritDoc}
     */
    public void globalRemove(String path) throws FxCacheException {
        getBackingCache().remove(path);
    }

    /**
     * {@inheritDoc}
     */
    public Set getKeys(String path) throws FxCacheException {
        return getBackingCache().getKeys(path);
    }

    /**
     * {@inheritDoc}
     */
    public Set globalGetKeys(String path) throws FxCacheException {
        return getBackingCache().getKeys(path);
    }


    /**
     * {@inheritDoc}
     */
    public Set getChildrenNames(String path) throws FxCacheException {
        return getBackingCache().getChildrenNames(path);
    }

    /**
     * {@inheritDoc}
     */
    public void remove(String path, Object key) throws FxCacheException {
        getBackingCache().remove(path, key);
    }

    /**
     * {@inheritDoc}
     */
    public void globalRemove(String path, Object key) throws FxCacheException {
        getBackingCache().remove(path, key);
    }

    /**
     * {@inheritDoc}
     */
    public void reloadEnvironment(Integer divisionId) throws Exception {
        StructureLoader.load(divisionId, true, null);
    }

    /**
     * {@inheritDoc}
     */
    public void setEvictionStrategy(Integer divisionId, String path, Integer maxContents, Integer timeToIdle,
                                    Integer timeToLive) throws FxCacheException {
        setEvictionStrategy(divisionId, path, maxContents, timeToIdle, timeToLive, true);
    }

    /**
     * {@inheritDoc}
     */
    public void setEvictionStrategy(Integer divisionId, String path, Integer maxContents, Integer timeToIdle, Integer timeToLive, Boolean overwrite) throws FxCacheException {
        cacheProvider.setEvictionStrategy("/Division" + divisionId + (path.charAt(0) == '/' ? path : '/' + path),
                maxContents, timeToIdle, timeToLive, overwrite);
    }

    /**
     * {@inheritDoc}
     */
    public long getSystemStartTime() {
        try {
            return (Long) getBackingCache().get("/" + this.getClass().getName(), SYSTEM_UP_KEY);
        } catch (Exception exc) {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getNodeStartTime() {
        return nodeStartupTime;
    }

    /**
     * {@inheritDoc}
     */
    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        /*if ("FxCache".equals(attribute))
            return getCache();
        else*/
        if ("DeploymentId".equals(attribute))
            return getDeploymentId();
        else if ("SystemStartTime".equals(attribute))
            return getSystemStartTime();
        else if ("NodeStartTime".equals(attribute))
            return getNodeStartTime();
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * {@inheritDoc}
     */
    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        //TODO: code me!
    }

    /**
     * {@inheritDoc}
     */
    public AttributeList getAttributes(String[] attributes) {
        //TODO: code me!
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public AttributeList setAttributes(AttributeList attributes) {
        //TODO: code me!
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void cleanupAfterRequest() throws FxCacheException {
        getBackingCache().getCache().setInvocationContext(null);
    }

    /**
     * {@inheritDoc}
     */
    public Object invoke(String actionName, Object params[], String signature[]) throws MBeanException, ReflectionException {
        try {
            if ("get".equals(actionName)) {
                return get((String) params[0], params[1]);
            } else if ("put".equals(actionName)) {
                put((String) params[0], params[1], params[2]);
            } else if ("remove".equals(actionName) && params.length == 1) {
                remove((String) params[0]);
            } else if ("remove".equals(actionName) && params.length == 2) {
                remove((String) params[0], params[1]);
            } else if ("exists".equals(actionName) && params.length == 2) {
                return exists((String) params[0], params[1]);
            } else if ("getKeys".equals(actionName)) {
                return getKeys((String) params[0]);
            } else if ("globalGet".equals(actionName)) {
                return globalGet((String) params[0], params[1]);
            } else if ("globalPut".equals(actionName)) {
                globalPut((String) params[0], params[1], params[2]);
            } else if ("globalRemove".equals(actionName) && params.length == 1) {
                globalRemove((String) params[0]);
            } else if ("globalRemove".equals(actionName) && params.length == 2) {
                globalRemove((String) params[0], params[1]);
            } else if ("globalExists".equals(actionName) && params.length == 2) {
                return globalExists((String) params[0], params[1]);
            } else if ("globalGetKeys".equals(actionName) && params.length == 1) {
                return globalGetKeys((String) params[0]);
            } else if ("getChildrenNames".equals(actionName)) {
                return getChildrenNames((String) params[0]);
            } else if ("reloadEnvironment".equals(actionName)) {
                reloadEnvironment((Integer) params[0]);
            } else if ("create".equals(actionName)) {
                create();
            } else if ("destroy".equals(actionName)) {
                destroy();
            } else if ("setEvictionStrategy".equals(actionName) && params.length == 5) {
                setEvictionStrategy((Integer) params[0], (String) params[1],
                        (Integer) params[2], (Integer) params[3], (Integer) params[4]);
            } else if ("setEvictionStrategy".equals(actionName) && params.length == 6) {
                setEvictionStrategy((Integer) params[0], (String) params[1],
                        (Integer) params[2], (Integer) params[3], (Integer) params[4],
                        (Boolean) params[5]);
            } else if ("cleanupAfterRequest".equals(actionName)) {
                cleanupAfterRequest();
            } else {
                LOG.warn("Tried to call [" + actionName + "] which is not implemented!");
            }
        } catch (Exception e) {
            LOG.error("Failed to invoke MBean op: " + e.getMessage());
            throw new MBeanException(e);
        } 
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public MBeanInfo getMBeanInfo() {
        return info;
    }
}
