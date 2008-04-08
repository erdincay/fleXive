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
package com.flexive.shared;

import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.configuration.*;
import com.flexive.shared.content.FxCachedContent;
import com.flexive.shared.content.FxCachedContentContainer;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoadException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.mbeans.FxCacheMBean;
import com.flexive.shared.mbeans.FxCacheProxy;
import com.flexive.shared.mbeans.MBeanHelper;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxFilteredEnvironment;
import com.flexive.stream.ServerLocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import java.util.List;

/**
 * FxCache access
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class CacheAdmin {
    private static transient Log LOG = LogFactory.getLog(CacheAdmin.class);

    public final static String ROOT_USERTICKETSTORE = "/UserTicketStore";
    public final static String ROOT_WEBDAV_USERTICKETSTORE = "/WebdavUserTicketStore";
    public final static String CACHE_SERVICE_NAME = "flexive:service=FxCache";
    public final static String LANGUAGES_ID = "/FxLang/Id";
    public final static String LANGUAGES_ISO = "/FxLang/ISO";
    public final static String LANGUAGES_ALL = "/FxLang/ISO";
    public final static String ENVIRONMENT_BASE = "/FxEnvironment";
    public static final Object ENVIRONMENT_RUNTIME = "runtime";
    public final static String STREAMSERVER_BASE = "/FxStreamServers";
    public final static String STREAMSERVER_EJB_KEY = "ejb_servers";
    public final static String CONTENTCACHE_BASE = "/FxContent";

    private static final Parameter<Boolean> DROP_RUNONCE = ParameterFactory.newInstance(Boolean.class,
            "/cacheAdmin/dropInRunOnce", ParameterScope.DIVISION_ONLY, false);

    // "cached" cache beans
    private static FxCacheMBean cache = null;
    //    private static ThreadLocal<FxEnvironment> requestEnvironment = new ThreadLocal<FxEnvironment>();
    // environment loader lock
    private static final Object ENV_LOCK = new Object();


    /**
     * Returns the FxCache instance.
     *
     * @return the cache instance
     */
    public static synchronized FxCacheMBean getInstance() {
        if (cache != null) {
            return cache;
        }
//        System.out.println("Locating ....");

        try {
            MBeanServer server = MBeanHelper.locateServer();
            cache = new FxCacheProxy(server);
            if (!server.isRegistered(((FxCacheProxy) cache).getName())) {
                LOG.info("Registering FxCacheMBean .... ");
                EJBLookup.getGlobalConfigurationEngine().registerCacheMBean(((FxCacheProxy) cache).getName());
//                    server.registerMBean(new FxCache(), ((FxCacheProxy)cache).getName());
                if (LOG.isDebugEnabled())
                    LOG.debug("Registered: " + server.isRegistered(((FxCacheProxy) cache).getName()));
                cache.create();
            } else {
                //check if MBean Server is from current deployment
                String deployedId = cache.getDeploymentId();
                if (MBeanHelper.DEPLOYMENT_ID.equals(deployedId))
                    return cache;
                LOG.info("Redeployment! Have to undeploy " + deployedId + " (vs " + MBeanHelper.DEPLOYMENT_ID + ")");
                cache.destroy();
//                    System.out.println("Unregistering ...");
                server.unregisterMBean(((FxCacheProxy) cache).getName());
//                    System.out.println("Registering new ...");
                EJBLookup.getGlobalConfigurationEngine().registerCacheMBean(((FxCacheProxy) cache).getName());
//                    server.registerMBean(new FxCache(), ((FxCacheProxy)cache).getName());
                LOG.debug("Registered: " + server.isRegistered(((FxCacheProxy) cache).getName()));
                cache.create();
            }
            /* MBeanServer server = MBeanServerLocator.locate();
    cache = (FxCacheMBean) MBeanProxyExt.create(
            FxCacheMBean.class,
            CACHE_SERVICE_NAME,
            server);*/
            return cache;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Check if the cache MBean is installed in the application server
     *
     * @return cache MBean installed
     */
    public static boolean isCacheMBeanInstalled() {
        MBeanServer server = MBeanHelper.locateServer();
        try {
            return server.isRegistered((new FxCacheProxy(server)).getName());
        } catch (MalformedObjectNameException e) {
            LOG.error(e);
            return false;
        }
    }

    /**
     * Is this a new and uninitialized flexive installation?
     * Useful to display splashscreens etc.
     *
     * @return if this is a new flexive installation
     */
    public static boolean isNewInstallation() {
        try {
            return getInstance().get(ENVIRONMENT_BASE, ENVIRONMENT_RUNTIME) == null &&
                    !EJBLookup.getDivisionConfigurationEngine().get(SystemParameters.DIVISION_RUNONCE);
        } catch (FxCacheException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }

    /**
     * Has the environment been loaded (yet) - internal helper
     *
     * @return if the environment has been loaded yet
     */
    public static boolean isEnvironmentLoaded() {
        try {
            return getInstance().get(ENVIRONMENT_BASE, ENVIRONMENT_RUNTIME) != null;
        } catch (FxCacheException e) {
            return false;
        }
    }

    /**
     * Get the FxEnvironment runtime from the cache
     *
     * @return FxEnvironment
     */
    public static FxEnvironment getEnvironment() {
        synchronized (ENV_LOCK) {
            try {
//            if (requestEnvironment.get() != null) {
//                return requestEnvironment.get();
//            }
                FxEnvironment ret = (FxEnvironment) getInstance().get(ENVIRONMENT_BASE, ENVIRONMENT_RUNTIME);
                if (ret == null) {
                    FxContext ri = FxContext.get();
                    if (!DivisionData.isValidDivisionId(ri.getDivisionId())) {
                        throw new FxCacheException("Division Id missing in request information");
                    }
                    EJBLookup.getDivisionConfigurationEngine().patchDatabase();
                    getInstance().reloadEnvironment(ri.getDivisionId());
                    //execute run-once scripts
                    EJBLookup.getScriptingEngine().executeRunOnceScripts();
                    //execute startup scripts
                    EJBLookup.getScriptingEngine().executeStartupScripts();
                    EJBLookup.getTimerService().install(true);

                    for (String dropName : FxSharedUtils.getDrops()) {
                        // set the drop name as key on the DROP_RUNONCE parameter
                        final Parameter<Boolean> dropParameter = DROP_RUNONCE.copy().setData(
                                new ParameterDataEditBean<Boolean>(DROP_RUNONCE.getData()).setKey(dropName));
                        EJBLookup.getScriptingEngine().executeDropRunOnceScripts(dropParameter, dropName);

                        // also run startup-scripts now
                        EJBLookup.getScriptingEngine().executeDropStartupScripts(dropName);
                    }
                    //Create eviction strategy if supported
                    cache.setEvictionStrategy(ri.getDivisionId(), CONTENTCACHE_BASE, 5000, 0, 120);

                    // make sure we don't miss any updates in the environment
                    getInstance().reloadEnvironment(ri.getDivisionId());
                    ret = (FxEnvironment) getInstance().get(ENVIRONMENT_BASE, ENVIRONMENT_RUNTIME);
                    if (ret == null)
                        throw new FxLoadException("ex.structure.runtime.cache.notFound");
                }
                //            requestEnvironment.set(ret);
                return ret;
            } catch (FxCacheException e) {
                throw new FxLoadException(LOG, e, "ex.cache.access.error", e.getMessage()).asRuntimeException();
            } catch (FxLoadException f) {
                throw f.asRuntimeException(); //pass thru
            } catch (Exception e) {
                e.printStackTrace();
                throw new FxLoadException(LOG, e, "ex.cache.access.error", e.getClass().getName() + ": " + e.getMessage()).asRuntimeException();
            }
        }
    }

    /**
     * Return a {@link com.flexive.shared.structure.FxFilteredEnvironment} for the calling user.
     *
     * @return a {@link com.flexive.shared.structure.FxFilteredEnvironment} for the calling user.
     */
    public static FxEnvironment getFilteredEnvironment() {
        return new FxFilteredEnvironment(getEnvironment());
    }

    /**
     * Force reloading of the environment for the current division. May only
     * be called by the supervisor user.
     *
     * @throws Exception on errors
     */
    public static void reloadEnvironment() throws Exception {
        if (!FxContext.get().getTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.cache.reload.privileges").asRuntimeException();
        }
        getInstance().reloadEnvironment(FxContext.get().getDivisionId());
//        requestEnvironment.set(null);
    }

    /**
     * Put a content and its security info in the cache
     *
     * @param content the content to cache
     */
    public static void cacheContent(FxCachedContent content) {
        try {
            FxCachedContentContainer container =
                    (FxCachedContentContainer) getInstance().get(CONTENTCACHE_BASE, content.getContent().getId());
            if (container == null) {
                container = new FxCachedContentContainer(content);
            } else {
                container.add(content);
            }
            getInstance().put(CONTENTCACHE_BASE, container.getId(), container);
        } catch (FxCacheException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    /**
     * Try to obtain a cached copy of a content identified by its primary key, will return <code>null</code> if not cached
     *
     * @param pk requested primary key
     * @return the cached content or <code>null</code> if not found
     */
    public static FxCachedContent getCachedContent(FxPK pk) {
        try {
            FxCachedContentContainer container = (FxCachedContentContainer) getInstance().get(CONTENTCACHE_BASE, pk.getId());
            if (container != null)
                return container.get(pk);
            return null;
        } catch (FxCacheException e) {
            LOG.warn(e.getMessage(), e);
            return null;
        }
    }

    /**
     * Expire all cached versions of a content with the requested id
     *
     * @param id requested id
     */
    public static void expireCachedContent(long id) {
        if (id <= 0)
            return;
        try {
            getInstance().remove(CONTENTCACHE_BASE, id);
        } catch (FxCacheException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    /**
     * Expire all cached contents
     */
    public static void expireCachedContents() {
        try {
            getInstance().remove(CONTENTCACHE_BASE);
        } catch (FxCacheException e) {
            LOG.warn(e.getMessage(), e);
        }
    }

    /**
     * Called from external methods when the environment changed.
     */
    public static void environmentChanged() {
//        requestEnvironment.set(null);
    }

    /**
     * Get a list of all available StreamServers
     *
     * @return list of all available StreamServers
     */
    @SuppressWarnings("unchecked")
    public static List<ServerLocation> getStreamServers() {
        try {
            List<ServerLocation> ret = (List<ServerLocation>) getInstance().globalGet(STREAMSERVER_BASE, STREAMSERVER_EJB_KEY);
            if (ret == null)
                throw new FxLoadException("ex.cache.streamservers.notFound");
            return ret;
        } catch (FxCacheException e) {
            throw new FxLoadException(LOG, e, "ex.cache.access.error", e.getMessage()).asRuntimeException();
        } catch (FxLoadException f) {
            throw f.asRuntimeException(); //pass thru
        } catch (Exception e) {
            throw new FxLoadException(LOG, e, "ex.cache.access.error", e.getClass().getName(), e.getMessage()).asRuntimeException();
        }
    }


}
