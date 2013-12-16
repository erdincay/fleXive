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

import com.flexive.shared.Pair;
import com.flexive.shared.cache.FxBackingCache;
import com.flexive.shared.cache.FxCacheException;
import com.flexive.shared.mbeans.MBeanHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.cache.CacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.jmx.CacheJmxWrapperMBean;

import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;

/**
 * FxBackingCache Provider for a JBossCache instance registered via an external -service.xml deployment
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * 
 */
public class FxJBossExternalCacheProvider extends AbstractBackingCacheProvider<FxJBossTreeCacheMBeanWrapper> {
    private static final Log LOG = LogFactory.getLog(FxJBossExternalCacheProvider.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return getClass().getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() throws FxCacheException {
        if (cache != null)
            return;
        try {
            // first check if the cache MBean exists
            final ObjectName objectName = new ObjectName("jboss.cache:service=JNDITreeCache");
            final Pair<MBeanServer,MBeanInfo> beanInfo = MBeanHelper.getMBeanInfo(objectName);
            // create wrapper MBean (cast necessary for java 1.5)
            final CacheJmxWrapperMBean wrapper = (CacheJmxWrapperMBean) MBeanServerInvocationHandler.newProxyInstance(
                    beanInfo.getFirst(), objectName, CacheJmxWrapperMBean.class, false);
            cache = new FxJBossTreeCacheMBeanWrapper(wrapper);
            evictChildren("");  // clean up possible leftovers from previous deployment
            LOG.trace(Fqn.class);
        } catch (Exception e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() throws FxCacheException {
        //do nothing since we just "use" a cache and dont provide it
    }

    private void evictChildren(String fqn) throws CacheException {
        final CacheJmxWrapperMBean<Object, Object> treeCache = cache.getCacheWrapper();
        if (StringUtils.isNotBlank(fqn)) {
            // evict local cache entry
            if (LOG.isInfoEnabled()) {
                LOG.info("Evicting " + fqn);
            }
            treeCache.getCache().evict(Fqn.fromString(fqn), true);
        }
        // also evict children
//        final Set childrenNames = treeCache.getChildrenNames(fqn);
//        if (childrenNames != null) {
//            for (Object childFqn: childrenNames) {
//                evictChildren(fqn + "/" + childFqn);
//            }
//        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxBackingCache getInstance() throws FxCacheException {
        return cache;
    }
}
