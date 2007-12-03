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
package com.flexive.shared.cache;

import com.flexive.shared.cache.impl.FxJBossEmbeddedCacheProvider;
import com.flexive.shared.cache.impl.FxJBossJNDICacheProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Factory class to create FxBackingCache providers
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxBackingCacheProviderFactory {

    private static transient Log LOG = LogFactory.getLog(FxBackingCacheProviderFactory.class);

    /**
     * Name of the environment property to set to force a provider
     */
    public final static String KEY = FxBackingCacheProvider.class.getCanonicalName();

    /**
     * Factory method to create a new FxBackingCacheProvider
     * <p/>
     * Strategy used in this order in case of failure:
     * <ol>
     * <li>if System property is set try to obtain a new instance of the given class</li>
     * <li>try to get a JNDI TreeCache instance with key "FxJBossTreeCache"</li>
     * <li>create a local TreeCache MBean</li>
     * </ol>
     *
     * @return FxBackingCacheProvider
     */
    public static FxBackingCacheProvider createNew() {
        String provider = System.getProperty(KEY);
        FxBackingCacheProvider instance = null;
        try {
            if (provider != null) {
                //try provided class
                try {
                    instance = (FxBackingCacheProvider) Class.forName(provider).newInstance();
                    return instance;
                } catch (InstantiationException e) {
                    LOG.error("Failed to instantiate " + provider + ": " + e.getMessage(), e);
                } catch (IllegalAccessException e) {
                    LOG.error(e.getMessage(), e);
                } catch (ClassNotFoundException e) {
                    LOG.error("Could not find class " + provider + ": " + e.getMessage(), e);
                }
            }
            try {
                instance = new FxJBossJNDICacheProvider();
                instance.init();
                return instance;
            } catch (FxCacheException e) {
                LOG.info("Failed to instantiate FxJBossJNDICacheProvider: " + e.getMessage());
            }
            instance = new FxJBossEmbeddedCacheProvider();
        } finally {
            if (instance != null)
                LOG.info("Using FxBackingCacheProvider instance " + instance.getClass().getCanonicalName());
            else
                LOG.fatal("Failed to create a FxBackingCacheProvider instance!");
        }
        return instance;
    }

}
