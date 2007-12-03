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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.PropertyConfigurator;
import org.jboss.cache.TreeCache;
import org.jboss.cache.eviction.LRUConfiguration;
import org.jboss.cache.eviction.LRUPolicy;
import org.jboss.cache.eviction.RegionManager;

/**
 * JBossCache FxBackingCacheProvider
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxJBossEmbeddedCacheProvider implements FxBackingCacheProvider {
    
    private static transient Log LOG = LogFactory.getLog(FxJBossEmbeddedCacheProvider.class);

    private FxJBossTreeCacheWrapper cache;

    private static final String CONFIG_FILE = "embeddedJBossCacheConfig.xml";

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
            TreeCache tc = new TreeCache();
            PropertyConfigurator pc = new PropertyConfigurator();
            pc.configure(tc, CONFIG_FILE);
            tc.createService();
            tc.startService();
            cache = new FxJBossTreeCacheWrapper(tc);
        } catch (Exception e) {
            LOG.error("Failed to start TreeCache. Error: " + e.getMessage(), e);
            System.err.println("!!! Failed to start TreeCache !!!! Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() throws FxCacheException {
        cache.getCache().stopService();
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
