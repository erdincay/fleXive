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
package com.flexive.shared.cache;

import org.jboss.cache.Cache;

import java.util.Set;

/**
 * Backing Cache for storing environment and contents
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface FxBackingCache {

    /**
     * Cache key for the system up time
     */
    public static final String SYSTEM_UP_KEY = "SYSTEM_UP";

    /**
     * Reads a entry from the division cache that the current request is in.
     *
     * @param path path
     * @param key  key
     * @return entry
     * @throws FxCacheException on errors
     */
    Object get(String path, Object key) throws FxCacheException;

    /**
     * Checks if the given key exists under the given path.
     *
     * @param path path
     * @param key  key
     * @return entry
     * @throws FxCacheException on errors
     */
    boolean exists(String path, Object key) throws FxCacheException;

    /**
     * Puts a entry into the cache of the division the current request is in.
     *
     * @param path  path
     * @param key   key
     * @param value value
     * @throws FxCacheException on errors
     */
    void put(String path, Object key, Object value) throws FxCacheException;


    /**
     * Removes from the cache of the division the current request is in.
     *
     * @param path path
     * @throws FxCacheException on errors
     */
    void remove(String path) throws FxCacheException;

    /**
     * Removes a entry from the cache of the division the current request is in.
     *
     * @param path path
     * @param key  key
     * @throws FxCacheException on errors
     */
    void remove(String path, Object key) throws FxCacheException;

    /**
     * Get all keys for a path
     *
     * @param path path
     * @return set of keys
     * @throws FxCacheException on errors
     */
    Set getKeys(String path) throws FxCacheException;

    /**
     * Get all child nodes for a path
     *
     * @param path path
     * @return set of child names
     * @throws FxCacheException on errors
     */
    Set getChildrenNames(String path) throws FxCacheException;

    /**
     * Check if the given cache path (or any parent path) is locked by the current transaction.
     *
     * @param path    path
     * @return  true if the current transaction contains a lock for the given node
     * @since 3.2.1
     */
    boolean isPathLockedInTx(String path) throws FxCacheException;

    /**
     * Get the wrapped cache
     *
     * @return TreeCache
     * @throws FxCacheException on errors
     */
    Cache<Object, Object> getCache() throws FxCacheException;
}
