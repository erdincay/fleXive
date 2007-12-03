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
package com.flexive.shared.search;

import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.UserTicket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Search parameters
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxSQLSearchParams implements Serializable {

    private static final long serialVersionUID = -3154811997979332790L;
    private final static int DEFAULT_QUERY_TIMEOUT = 15; /*seconds*/

    /**
     * The cache modes of the search.
     *
     * <li><b>OFF</b>: No caching will be used at all.</li>
     * <li><b>ON</b>: The search will try to find the query result from its cache, and writes the result to the cache
     *      for the next similar querys to find it</li>
     * <li><b>READ_ONLY</b>: The search will try to find the query result from its cache, but will
     *      not write the result to the cache</li>
     */
    static public enum CACHE_MODE {
        OFF(1),
        READ_ONLY(2),
        ON(3);
        private int id;

        CACHE_MODE(int id) {
            this.id = id;
        }

        /**
         * Getter for the internal id
         *
         * @return internal id
         */
        public int getId() {
            return id;
        }

        /**
         * Get a CACHE_MODE by its id
         *
         * @param id the id
         * @return CACHE_MODE the type
         * @throws com.flexive.shared.exceptions.FxNotFoundException if the mode does not exist
         *
         */
        public static CACHE_MODE getById(int id) throws FxNotFoundException {
            for (CACHE_MODE mode : CACHE_MODE.values()) {
                if (mode.id == id) return mode;
            }
            throw new FxNotFoundException("ex.sqlSearch.cacheMode.notFound.id", id);
        }
    }


    private BriefcaseCreationData bcd = null;
    private int queryTimeout = -1;
    private List<FxLanguage> resultLanguages;
    private CACHE_MODE cacheMode = CACHE_MODE.OFF;
    private static final CACHE_MODE CACHE_MODE_DEFAULT = CACHE_MODE.READ_ONLY;
    /**
     * Constructor.
     */
    public FxSQLSearchParams() {
        // empty
    }

    /**
     * Sets the caching mode for the search.
     *
     * @param mode the cache mode to use<br>
     *      CACHE_MODE.ON: read from the cache if possible and write result to the cache<br>
     *      CACHE_MODE.OFF: do not read or write the cache<br>
     *      CACHE_MODE.READ_ONLY: read from the cache, but do not write to it<br>
     *      if null is specified CACHE_MODE.READ_ONLY will be used.
     * @return this
     */
    public FxSQLSearchParams setCacheMode(CACHE_MODE mode) {
        this.cacheMode=mode==null?CACHE_MODE_DEFAULT:mode;
        return this;
    }


    /**
     * Returns the cache mode.
     *
     * @return the cache mode.
     */
    public CACHE_MODE getCacheMode() {
        return (cacheMode==null)?CACHE_MODE_DEFAULT:cacheMode;
    }

    /**
     * Sets the languages that the resultset should contain.
     *
     * @param languages the languages, if null or a emtpty array is specified the default language of the
     * calling user will be used.
     * @return this
     */
    public FxSQLSearchParams setResultLanguages(List<FxLanguage> languages) {
        this.resultLanguages = languages;
        return this;
    }

    /**
     * Gets the languages that the resultset will contain.
     *
     * @return the languages
     */
    public List<FxLanguage> getResultLanguages() {
        if (this.resultLanguages==null) {
            this.resultLanguages = new ArrayList<FxLanguage>(1);
            final UserTicket ticket = FxContext.get().getTicket();
            this.resultLanguages.add(ticket.getLanguage());
        }
        return this.resultLanguages;
    }

    /**
     * Saves the result of the query in a new briefcase.
     *
     * @param name the name of the briefcase to create
     * @param description the description of the briefcase
     * @param aclId null if the briefcase is not shared, or a ACL to grant permissions to other users
     * @return this
     */
    public FxSQLSearchParams saveResultInBriefcase(String name,String description,Long aclId) {
        this.bcd = new BriefcaseCreationData(name,description,aclId);
        return this;
    }

    /**
     * Saves the result of the query in a new briefcase.
     *
     * @param name the name of the briefcase to create
     * @param description the description of the briefcase
     * @param acl null if the briefcase is not shared, or a ACL to grant permissions to other users
     * @return this
     */
    public FxSQLSearchParams saveResultInBriefcase(String name,String description, ACL acl) {
        Long aclId = acl==null?null:acl.getId();
        this.bcd = new BriefcaseCreationData(name,description,aclId);
        return this;
    }

    /**
     * Retuns true if the query will create a briefcase with the found objects.
     *
     * @return true if the query will create a briefcase with the found objects.
     */
    public boolean getWillCreateBriefcase() {
        return (this.bcd!=null);
    }

    public BriefcaseCreationData getBriefcaseCreationData() {
        return bcd;
    }


    /**
     * Sets the query timeout on the database.
     *
     * @param value in seconds, zero means unlimited
     * @return this
     */
    public FxSQLSearchParams setQueryTimeout(int value) {
        this.queryTimeout = value<=0?-1:value;
        return this;
    }


    /**
     * Returns the query timeout in seconds.
     *
     * @return the query timeout
     */
    public int getQueryTimeout() {
        return (queryTimeout<0)?DEFAULT_QUERY_TIMEOUT:queryTimeout;
    }
}
