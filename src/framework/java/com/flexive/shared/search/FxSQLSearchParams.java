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
package com.flexive.shared.search;

import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.interfaces.SearchEngine;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.UserTicket;
import com.google.common.collect.Lists;

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

    /**
     * The cache modes of the search.
     * <p/>
     * <li><b>OFF</b>: No caching will be used at all.</li>
     * <li><b>ON</b>: The search will try to find the query result from its cache, and writes the result to the cache
     * for the next similar querys to find it</li>
     * <li><b>READ_ONLY</b>: The search will try to find the query result from its cache, but will
     * not write the result to the cache</li>
     */
    public static enum CacheMode {
        OFF(1),
        READ_ONLY(2),
        ON(3);
        private int id;

        CacheMode(int id) {
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
         * Get a CacheMode by its id
         *
         * @param id the id
         * @return CacheMode the type
         * @throws com.flexive.shared.exceptions.FxNotFoundException
         *          if the mode does not exist
         */
        public static CacheMode getById(int id) throws FxNotFoundException {
            for (CacheMode mode : CacheMode.values()) {
                if (mode.id == id) return mode;
            }
            throw new FxNotFoundException("ex.sqlSearch.cacheMode.notFound.id", id);
        }
    }

    /**
     * Envelope to carry data needed for a briefcase creation.
     *
     * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
     */
    public static class BriefcaseCreationData implements Serializable {
        private static final long serialVersionUID = -1050668347506270241L;
        private final Long aclId;
        private final String description;
        private final String name;

        /**
         * Constructor.
         *
         * @param name        the name of the briefcase, in case of null or a empty String a name will be constructed
         * @param description the description
         * @param aclId       the acl the briefcase is using, or null if the briefcase is not shared
         */
        public BriefcaseCreationData(String name, String description, Long aclId) {
            FxSharedUtils.checkParameterEmpty(name, "name");
            this.description = description == null ? "" : description;
            this.name = name;
            this.aclId = aclId;
        }

        /**
         * Returns the acl that the briefcase is using, or null if the briefcase is not shared.
         *
         * @return the aclId, or null if the briefcase is not shared
         */
        public Long getAclId() {
            return aclId;
        }

        /**
         * The briefcase description (may be empty).
         *
         * @return the description
         */
        public String getDescription() {
            return description;
        }

        /**
         * The briefcase name.
         *
         * @return the name
         */
        public String getName() {
            return name;
        }
    }

    private static final CacheMode CACHE_MODE_DEFAULT = CacheMode.READ_ONLY;

    private BriefcaseCreationData bcd = null;
    private int queryTimeout = -1;
    private List<FxLanguage> resultLanguages;
    private CacheMode cacheMode = CacheMode.OFF;
    private List<Long> hintTypes = null;
    private boolean hintIgnoreXPath = false;
    private boolean hintNoResultInfo = false;

    /**
     * Constructor.
     */
    public FxSQLSearchParams() {
        // empty
    }

    /**
     * Copy constructor.
     *
     * @param other the instance to be copied
     * @since       3.1.6
     */
    protected FxSQLSearchParams(FxSQLSearchParams other) {
        this.bcd = other.bcd == null ? null : new BriefcaseCreationData(other.bcd.name, other.bcd.description, other.bcd.aclId);
        this.queryTimeout = other.queryTimeout;
        this.resultLanguages = other.resultLanguages == null ? null : Lists.newArrayList(other.resultLanguages);
        this.cacheMode = other.cacheMode;
        this.hintTypes = other.hintTypes == null ? null : Lists.newArrayList(other.hintTypes);
        this.hintNoResultInfo = other.hintNoResultInfo;
    }

    /**
     * @return  an independent copy of this parameter set
     */
    public FxSQLSearchParams copy() {
        return new FxSQLSearchParams(this);
    }

    /**
     * Sets the caching mode for the search.
     *
     * @param mode the cache mode to use<br>
     *             CacheMode.ON: read from the cache if possible and write result to the cache<br>
     *             CacheMode.OFF: do not read or write the cache<br>
     *             CacheMode.READ_ONLY: read from the cache, but do not write to it<br>
     *             if null is specified CacheMode.READ_ONLY will be used.
     * @return this
     */
    public FxSQLSearchParams setCacheMode(CacheMode mode) {
        this.cacheMode = mode == null ? CACHE_MODE_DEFAULT : mode;
        return this;
    }


    /**
     * Returns the cache mode.
     *
     * @return the cache mode.
     */
    public CacheMode getCacheMode() {
        return (cacheMode == null) ? CACHE_MODE_DEFAULT : cacheMode;
    }

    /**
     * Sets the languages that the resultset should contain.
     *
     * @param languages the languages, if null or a empty array is specified the default language of the
     *                  calling user will be used.
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
        if (this.resultLanguages == null) {
            this.resultLanguages = new ArrayList<FxLanguage>(1);
            final UserTicket ticket = FxContext.getUserTicket();
            this.resultLanguages.add(ticket.getLanguage());
        }
        return this.resultLanguages;
    }

    /**
     * Saves the result of the query in a new briefcase.
     *
     * @param name        the name of the briefcase to create
     * @param description the description of the briefcase
     * @param aclId       null if the briefcase is not shared, or a ACL to grant permissions to other users
     * @return this
     */
    public FxSQLSearchParams saveResultInBriefcase(String name, String description, Long aclId) {
        this.bcd = new BriefcaseCreationData(name, description, aclId);
        return this;
    }

    /**
     * Saves the result of the query in a new briefcase.
     *
     * @param name        the name of the briefcase to create
     * @param description the description of the briefcase
     * @param acl         null if the briefcase is not shared, or a ACL to grant permissions to other users
     * @return this
     */
    public FxSQLSearchParams saveResultInBriefcase(String name, String description, ACL acl) {
        Long aclId = acl == null ? null : acl.getId();
        this.bcd = new BriefcaseCreationData(name, description, aclId);
        return this;
    }

    /**
     * Retuns true if the query will create a briefcase with the found objects.
     *
     * @return true if the query will create a briefcase with the found objects.
     */
    public boolean getWillCreateBriefcase() {
        return (this.bcd != null);
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
        this.queryTimeout = value <= 0 ? -1 : value;
        return this;
    }


    /**
     * Returns the query timeout in seconds.
     *
     * @return the query timeout
     */
    public int getQueryTimeout() {
        return (queryTimeout < 0) ? SearchEngine.DEFAULT_QUERY_TIMEOUT : queryTimeout;
    }

    /**
     * @return  the list of types the caller expects this query to return.
     * @since   3.1.6
     */
    public List<Long> getHintTypes() {
        return hintTypes;
    }

    /**
     * Set the list of types the caller expects this query to return.
     * This allows optimizations for security filters, since FxSQL queries always operate
     * on the flat virtual table defined by all types.
     * <p>
     * <strong>Use with care!</strong>
     * When the result contains a type that is not covered by this list, security restrictions
     * may not be applied to the type.
     * </p>
     *
     * @param hintTypes the list of types the query may return
     * @since   3.1.6
     */
    public void setHintTypes(List<Long> hintTypes) {
        this.hintTypes = hintTypes != null ? Lists.newArrayList(hintTypes) : null;
    }

    /**
     * @return  whether the XPath for result values is needed by the caller
     * @since   3.1.6
     */
    public boolean isHintIgnoreXPath() {
        return hintIgnoreXPath;
    }

    /**
     * Indicate that the caller does not need the XPaths of the values returned by the search result.
     * XPaths may be selected if they are necessary for checking property permissions, regardless.
     *
     * @param hintIgnoreXPath   if the result values can ignore the XPath property
     * @since 3.1.6
     */
    public void setHintIgnoreXPath(boolean hintIgnoreXPath) {
        this.hintIgnoreXPath = hintIgnoreXPath;
    }

    /**
     * @return  when true, indicates that the caller does not need 'global' information about the result set
     * such as the total row count or the found content types
     * @since 3.1.7
     */
    public boolean isHintNoResultInfo() {
        return hintNoResultInfo;
    }

    /**
     * Disable the retrieval of result information such as the total row count or the found content types.
     * This allows the query engine to perform the entire query in one pass instead of populating a
     * temporary table first.
     *
     * @param hintNoResultInfo    when true, no result information will be available in the {@link FxResultSet}
     * @since 3.1.7
     */
    public void setHintNoResultInfo(boolean hintNoResultInfo) {
        this.hintNoResultInfo = hintNoResultInfo;
    }
}
