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
package com.flexive.cmis.spi;

import java.util.Iterator;
import org.apache.chemistry.ListPage;
import org.apache.chemistry.Paging;
import org.apache.chemistry.impl.simple.SimpleListPage;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ListPageUtils {

    /**
     * A mapper function for {@link ListPageUtils#page}.
     * Use {@link ListPageUtils#identityFunction} to map a value to itself.
     *
     * @param <T>   the origin type
     * @param <U>   the target type
     */
    public static interface Function<T, U> {
        U apply(T value);
    }

    private static class Identity<T> implements Function<T, T> {
        public T apply(T value) {
            return value;
        }
    }

    private ListPageUtils() {
    }

    public static int getSkipCount(Paging paging) {
        return paging == null ? 0 : paging.skipCount;
    }

    public static int getMaxItems(Paging paging) {
        return paging == null || paging.maxItems == 0 ? Integer.MAX_VALUE - 1 : paging.maxItems;
    }

    public static <T> Iterator<T> skip(Iterator<T> iterator, Paging paging) {
        for (int i = 0; i < getSkipCount(paging) && iterator.hasNext(); i++) {
            iterator.next();
        }
        return iterator;
    }

    public static <T, U> ListPage<U> page(Iterable<T> iterable, Paging paging, Function<T, U> mapper) {
        final Iterator<T> iterator = iterable.iterator();
        skip(iterator, paging);
        final SimpleListPage<U> result = new SimpleListPage<U>();
        final int maxItems = getMaxItems(paging);
        int rows = getSkipCount(paging);
        while (iterator.hasNext()) {
            if (result.size() > maxItems) {
                result.setHasMoreItems(true);
                break;
            }
            result.add(mapper.apply(iterator.next()));
            rows++;
        }
        // count rest of rows - TODO: use Collection#size if applicable
        while (iterator.hasNext()) {
            rows++;
            iterator.next();
        }
        result.setNumItems(rows);
        return result;
    }

    public static <T> Function<T, T> identityFunction() {
        return new Identity<T>();
    }

}
