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
package com.flexive.war.beans.cms;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.interfaces.SearchEngine;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.FxSQLSearchParams;

import java.util.Hashtable;

public class SearchProviderBean extends Hashtable<String, FxResultSet> {

    // TODO:
    // When "group based" caching is used, the group key has to be created with the groups in sorted order,
    // so that the group array [1,2] equals [2,1].
    // Also the search must be run with a fake ticket, that does NOT take the users permissions into account, only
    // those of the groups.

    private SearchEngine search;

    /**
     * Default constructor
     */
    public SearchProviderBean() {
        super(20);
        search = EJBLookup.getSearchEngine();
    }

    private static String getCacheKey(String query) {
        String sGroups = "";
        for (long grp : FxContext.get().getTicket().getGroups()) {
            sGroups += "_" + grp;
        }
        return sGroups + query;
    }

    public FxResultSet getFromCache(String query) {
        try {
            String key = getCacheKey(query);
            String path = String.valueOf(SearchProviderBean.class);
            FxResultSet result = (FxResultSet) CacheAdmin.getInstance().get(path, key);
            if (result != null) {
                long tstp = search.getLastContentChange(false);
                if (tstp > result.getCreationTime()) {
                    CacheAdmin.getInstance().remove(path, key);
                    result = null;
                }
            }
            return result;
        } catch (Throwable t) {
            return null;
        }
    }

    public void putToCache(String query, FxResultSet result) {
        try {
            CacheAdmin.getInstance().put(String.valueOf(SearchProviderBean.class),
                    getCacheKey(query), result);
        } catch (Throwable t) {
            System.err.println("Failed to put the getResult result in to the cache: " + t.getMessage());
        }
    }

    /**
     * This will perform a search.
     * <p/>
     * Results are stored within the request, and if the same query is issued again the
     * result will be taken from this cache.
     *
     * @param query the search
     * @return the resultset
     */
    public FxResultSet get(Object query) {
        boolean fromCache = true;
        long time = System.currentTimeMillis();
        String sQuery = String.valueOf(query).trim();
        // Evaluate JSF/faceltets expressions within the query
        sQuery = FxJsfUtils.evalString(sQuery);
        FxResultSet result = super.get(sQuery);
        // Only issue the query if we didnt find it in the request cache
        if (result == null) {
            // Lookup the second cache
            result = getFromCache(sQuery);
            // Use the cache if possible, otherwise perform the query
            if (result == null) {
                try {
                    fromCache = false;
                    FxSQLSearchParams params = new FxSQLSearchParams();
                    params.setQueryTimeout(1000);
                    result = search.search(String.valueOf(sQuery), 0, 100, null);
                    super.put(sQuery, result);
                    putToCache(sQuery, result);
                } catch (Throwable t) {
                    System.err.println("Search failed: " + t.getMessage());
                    return null;
                }
            }
        }
        if (!fromCache) {
            System.out.print("SearchProviderBean: cache=" + fromCache + ", time=" + (System.currentTimeMillis() - time));
        }
        return result;
    }

    boolean initial = false;


}
