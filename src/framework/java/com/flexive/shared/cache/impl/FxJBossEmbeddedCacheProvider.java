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
package com.flexive.shared.cache.impl;

import com.flexive.shared.cache.FxBackingCache;
import com.flexive.shared.cache.FxBackingCacheProvider;
import com.flexive.shared.cache.FxCacheException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;
import org.jboss.cache.*;
import org.jboss.cache.eviction.LRUConfiguration;
import org.jboss.cache.eviction.LRUPolicy;

/**
 * JBossCache FxBackingCacheProvider
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxJBossEmbeddedCacheProvider extends AbstractBackingCacheProvider<FxJBossTreeCacheWrapper> {
    /**
     * System property holding the XML configuration file for the embedded cache.
     * The path must be available either on the file system or in the classpath. 
     */
    public static final String SYSTEM_CACHE_CONFIG = "flexive.cache.config";
    private static final Log LOG = LogFactory.getLog(FxJBossEmbeddedCacheProvider.class);
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
            final Cache<Object, Object> tc = new DefaultCacheFactory<Object, Object>().createCache(
                    StringUtils.defaultString(System.getProperty(SYSTEM_CACHE_CONFIG), CONFIG_FILE)
            );
            tc.create();
            tc.start();
            cache = new FxJBossTreeCacheWrapper(tc);
        } catch (Exception e) {
            LOG.error("Failed to start TreeCache. Error: " + e.getMessage(), e);
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() throws FxCacheException {
        cache.getCache().stop();
    }

    /**
     * {@inheritDoc}
     */
    public FxBackingCache getInstance() throws FxCacheException {
        return cache;
    }
}
