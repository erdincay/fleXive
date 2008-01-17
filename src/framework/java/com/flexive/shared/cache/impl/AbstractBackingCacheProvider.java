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
package com.flexive.shared.cache.impl;

import com.flexive.shared.cache.FxBackingCacheProvider;
import com.flexive.shared.cache.FxBackingCache;
import com.flexive.shared.cache.FxCacheException;
import org.jboss.cache.RegionManager;
import org.jboss.cache.CacheSPI;
import org.jboss.cache.Fqn;
import org.jboss.cache.Region;
import org.jboss.cache.eviction.LRUConfiguration;

/**
 * Base backing cache provider implementation.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class AbstractBackingCacheProvider<T extends FxBackingCache> implements FxBackingCacheProvider {
    protected T cache;

    /**
     * {@inheritDoc}
     */
    public void setEvictionStrategy(String path, int maxContents, int timeToIdle, int timeToLive) throws FxCacheException {
        RegionManager rm = ((CacheSPI) cache.getCache()).getRegionManager();
        LRUConfiguration config = new LRUConfiguration();
        config.setMaxNodes(maxContents);
        config.setMaxAgeSeconds(timeToIdle);
        config.setTimeToLiveSeconds(timeToLive);
        rm.getRegion(Fqn.fromString(path), true).setEvictionPolicy(config);
    }
}
