/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
import com.flexive.shared.cache.FxCacheException;
import org.jboss.cache.Fqn;
import org.jboss.cache.CacheException;
import org.jboss.cache.Node;

import java.util.Set;
import java.util.HashSet;

/**
 * Base implementation of JBoss Cache wrappers.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class AbstractJBossTreeCacheWrapper implements FxBackingCache {
    
    /**
     * {@inheritDoc}
     */
    public Object get(String path, Object key) throws FxCacheException {
        try {
            return getCache().get(Fqn.fromString(path), key);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean exists(String path, Object key) throws FxCacheException {
        final Set<Object> keys = getCache().getKeys(path);
        return keys != null && keys.contains(key);
    }

    /**
     * {@inheritDoc}
     */
    public void put(String path, Object key, Object value) throws FxCacheException {
        try {
            getCache().put(Fqn.fromString(path), key, value);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(String path) throws FxCacheException {
        try {
            final Node<Object, Object> node = getNode(path);
            if (node != null) {
                for (Object name : node.getChildrenNames()) {
                    node.removeChild(name);
                }
                node.clearData();
            }
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void remove(String path, Object key) throws FxCacheException {
        try {
            getCache().remove(Fqn.fromString(path), key);
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getKeys(String path) throws FxCacheException {
        try {
            final Node<Object, Object> node = getNode(path);
            return node != null ? node.getKeys() : new HashSet();
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set getChildrenNames(String path) throws FxCacheException {
        try {
            final Node<Object,Object> node = getNode(path);
            return node != null ? node.getChildrenNames() : new HashSet();
        } catch (CacheException e) {
            throw new FxCacheException(e);
        }
    }

    protected Node<Object, Object> getNode(String path) throws FxCacheException {
        return getCache().getRoot().getChild(Fqn.fromString(path));
    }
}
