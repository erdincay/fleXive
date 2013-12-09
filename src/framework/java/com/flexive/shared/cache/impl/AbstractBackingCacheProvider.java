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
package com.flexive.shared.cache.impl;

import com.flexive.shared.cache.FxBackingCache;
import com.flexive.shared.cache.FxBackingCacheProvider;
import com.flexive.shared.cache.FxCacheException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.Fqn;
import org.jboss.cache.Region;
import org.jboss.cache.config.EvictionRegionConfig;
import org.jboss.cache.eviction.LRUAlgorithmConfig;

import java.util.concurrent.TimeUnit;

/**
 * Base backing cache provider implementation.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class AbstractBackingCacheProvider<T extends FxBackingCache> implements FxBackingCacheProvider {
    private static final Log LOG = LogFactory.getLog(AbstractBackingCacheProvider.class);

    protected T cache;

    /**
     * {@inheritDoc}
     */
    public void setEvictionStrategy(String path, int maxContents, int timeToIdle, int timeToLive, boolean overwrite) throws FxCacheException {
        final Fqn<String> fqn = Fqn.fromString(path);
        final Region region = cache.getCache().getRegion(fqn, true);
        if (overwrite || !region.isActive() || region.getEvictionRegionConfig() == null) {
            LOG.info("Setting eviction strategy for region [" + path + "] to [" + maxContents + "] contents, TTI=" +
                    timeToIdle + ", TTL=" + timeToLive + ", overwrite=" + overwrite + ", region active:" + region.isActive());
            LRUAlgorithmConfig lruc = new LRUAlgorithmConfig();
            lruc.setMaxNodes(maxContents);
            lruc.setMaxAge(timeToIdle, TimeUnit.SECONDS);
            lruc.setTimeToLive(timeToLive, TimeUnit.SECONDS);
            EvictionRegionConfig erc = new EvictionRegionConfig(fqn, lruc);
            region.setEvictionRegionConfig(erc);
            if (!region.isActive())
                region.setActive(true);
        } else {
            EvictionRegionConfig erc = region.getEvictionRegionConfig();
            if (erc.getEvictionAlgorithmConfig() instanceof LRUAlgorithmConfig) {
                LRUAlgorithmConfig lruc = (LRUAlgorithmConfig) erc.getEvictionAlgorithmConfig();
                LOG.info("Ignoring setEvictionStrategy request. Current settings for region [" + path + "]: [" +
                        lruc.getMaxNodes() + "] contents, TTI=" + lruc.getMaxAge() + ", TTL=" + lruc.getTimeToLive() +
                        ", region active:" + region.isActive()+". Requested settings: ["+ maxContents +
                        "] contents, TTI=" + timeToIdle + ", TTL=" + timeToLive);
            } else
                LOG.info("Ignoring setEvictionStrategy request. Current settings for region [" + path + "] with a config of type :" + erc.getEvictionAlgorithmConfig().getEvictionAlgorithmClassName());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setEvictionStrategy(String path, int maxContents, int timeToIdle, int timeToLive) throws FxCacheException {
        setEvictionStrategy(path, maxContents, timeToIdle, timeToLive, true);
    }
}
