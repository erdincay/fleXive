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
package com.flexive.shared.cache;

/**
 * A FxBackingCache Provider
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface FxBackingCacheProvider {

    /**
     * Get a description of the provider
     *
     * @return description of the provider
     */
    public String getDescription();

    /**
     * Initialize the provider
     *
     * @throws FxCacheException on errors
     */
    public void init() throws FxCacheException;

    /**
     * Shutdown the provider
     *
     * @throws FxCacheException on errors
     */
    public void shutdown() throws FxCacheException;

    /**
     * Get the cache instance
     *
     * @return cache instance
     * @throws FxCacheException on errors
     */
    public FxBackingCache getInstance() throws FxCacheException;

    /**
     * Set the eviction strategy for a path (if the backing cache supports this)
     *
     * @param path        path
     * @param maxContents max. number of entries to allow (0=unlimited)
     * @param timeToIdle  time a value has to be idle to be evicted (0=forever)
     * @param timeToLive  time to live (0=forever)
     * @throws FxCacheException on cache errors
     */
    public void setEvictionStrategy(String path, int maxContents, int timeToIdle, int timeToLive) throws FxCacheException;

    /**
     * Set the eviction strategy for a path (if the backing cache supports this)
     *
     * @param path        path
     * @param maxContents max. number of entries to allow (0=unlimited)
     * @param timeToIdle  time a value has to be idle to be evicted (0=forever)
     * @param timeToLive  time to live (0=forever)
     * @param overwrite   if an existing policy should be overwritten
     * @throws FxCacheException on cache errors
     * @since 3.0.2
     */
    public void setEvictionStrategy(String path, int maxContents, int timeToIdle, int timeToLive, boolean overwrite) throws FxCacheException;
}
