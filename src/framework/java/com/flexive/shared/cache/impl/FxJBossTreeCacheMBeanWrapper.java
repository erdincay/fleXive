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
import com.flexive.shared.cache.FxCacheException;
import org.jboss.cache.CacheException;
import org.jboss.cache.TreeCacheMBean;

import java.util.Set;

/**
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxJBossTreeCacheMBeanWrapper implements FxBackingCache {

    private TreeCacheMBean cache;

    public FxJBossTreeCacheMBeanWrapper(TreeCacheMBean cache) {
        this.cache = cache;
    }
    /**
     * {@inheritDoc}
     */
    public Object get(String path, Object key) throws FxCacheException {
        try {
            return cache.get(path, key);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * Get the wrapped TreeCache
     *
     * @return wrapped TreeCache
     */
    protected TreeCacheMBean getCache() {
        return cache;
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(String path, Object key) throws FxCacheException {
        return cache.exists(path, key);
    }

    /**
     * {@inheritDoc}
     */
    public void put(String path, Object key, Object value) throws FxCacheException {
        try {
            cache.put(path, key, value);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(String path) throws FxCacheException {
        try {
            cache.remove(path);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(String path, Object key) throws FxCacheException {
        try {
            cache.remove(path, key);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getKeys(String path) throws FxCacheException {
        try {
            return cache.getKeys(path);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getChildrenNames(String path) throws FxCacheException {
        try {
            return cache.getChildrenNames(path);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

}
