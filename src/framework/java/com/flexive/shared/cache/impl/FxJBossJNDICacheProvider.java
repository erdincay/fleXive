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
package com.flexive.shared.cache.impl;

import com.flexive.shared.cache.FxBackingCache;
import com.flexive.shared.cache.FxBackingCacheProvider;
import com.flexive.shared.cache.FxCacheException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.TreeCacheMBean;
import org.jboss.cache.eviction.LRUConfiguration;
import org.jboss.cache.eviction.LRUPolicy;
import org.jboss.cache.eviction.RegionManager;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Set;

/**
 * FxBackingCache Provider for a JBossCache instance registered via JNDI
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxJBossJNDICacheProvider implements FxBackingCacheProvider {

    private static transient Log LOG = LogFactory.getLog(FxJBossJNDICacheProvider.class);

    private FxJBossTreeCacheMBeanWrapper cache = null;

    /**
     * {@inheritDoc}
     */
    public String getDescription() {
        return getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    public void init() throws FxCacheException {
        if (cache != null)
            return;
        try {
            Context jndiContext = new InitialContext();
            cache = new FxJBossTreeCacheMBeanWrapper(((TreeCacheMBean) jndiContext.lookup("FxJBossJNDICache")));
            evictChildren("");  // clean up possible leftovers from previous deployment
            LOG.trace(Fqn.class);
        } catch (NamingException e) {
            throw new FxCacheException(e);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() throws FxCacheException {
        //do nothing since we just "use" a cache and dont provide it
    }

    private void evictChildren(String fqn) throws CacheException {
        final TreeCacheMBean treeCache = cache.getCache();
        if (StringUtils.isNotBlank(fqn)) {
            // evict local cache entry
            if (LOG.isInfoEnabled()) {
                LOG.info("Evicting " + fqn);
            }
            treeCache.evict(treeCache.get(fqn).getFqn());
        }
        // also evict children
        final Set childrenNames = treeCache.getChildrenNames(fqn);
        if (childrenNames != null) {
            for (Object childFqn: childrenNames) {
                evictChildren(fqn + "/" + childFqn);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public FxBackingCache getInstance() throws FxCacheException {
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    public void setEvictionStrategy(String path, int maxContents, int timeToIdle, int timeToLive) {
        try {
            RegionManager rm = cache.getCache().getInstance().getEvictionRegionManager();
            if (!rm.hasRegion(path)) {
                LRUConfiguration config = new LRUConfiguration();
                config.setMaxNodes(maxContents);
                config.setMaxAgeSeconds(timeToIdle);
                config.setTimeToLiveSeconds(timeToLive);
                LRUPolicy policy = new LRUPolicy();
                policy.configure(cache.getCache().getInstance());
                rm.createRegion(path, policy, config);
            } else {
                LRUConfiguration config = new LRUConfiguration();
                config.setMaxNodes(maxContents);
                config.setMaxAgeSeconds(timeToIdle);
                config.setTimeToLiveSeconds(timeToLive);
                rm.getRegion(path).setEvictionConfiguration(config);
            }
        } catch (Throwable e) {
            LOG.error(e);
        }
    }
}
