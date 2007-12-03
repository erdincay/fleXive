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
package com.flexive.war;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;


/**
 * This Hashtable finds its entries by comparing the hashtables keys with the given keys original value
 * or its string representation.
 * <p/>
 * In other words, .get(new Integer(1)) will deliver the same result as .get('1') on a
 * LookupHashtable&lt;Integer,?>.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class LookupHashtable<K, V> implements Map<K, V>, java.io.Serializable {

    private Hashtable<K, V> original;
    private Hashtable<String, V> cast;

    public LookupHashtable(int initialCapacity) {
        original = new Hashtable<K, V>(initialCapacity);
        cast = new Hashtable<String, V>(initialCapacity);
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return original == null ? 0 : original.size();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return original == null || original.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return original.containsKey(key) || cast.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object value) {
        return original != null && original.containsValue(value);
    }

    /**
     * {@inheritDoc}
     */
    public V get(Object key) {
        V result = original.get(key);
        if (result == null) {
            result = cast.get(String.valueOf(key));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public V put(K k, V v) {
        V oldValue = original.put(k, v);
        if (!(k instanceof String)) {
            cast.put(String.valueOf(k), v);
        }
        return oldValue;
    }

    /**
     * {@inheritDoc}
     */
    public V remove(Object key) {
        V value = original.remove(key);
        cast.remove(String.valueOf(key));
        return value;
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends K, ? extends V> map) {
        original.putAll(map);
        for (K key : map.keySet()) {
            if (key instanceof String) continue;
            cast.put(String.valueOf(key), map.get(key));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        original.clear();
        cast.clear();
    }

    /**
     * {@inheritDoc}
     */
    public Set<K> keySet() {
        return original.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<V> values() {
        return original.values();
    }

    /**
     * {@inheritDoc}
     */
    public Set<Entry<K, V>> entrySet() {
        return original.entrySet();
    }
}
