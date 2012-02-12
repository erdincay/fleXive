/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2012
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
package com.flexive.shared;

import java.io.Serializable;

/**
 * Query for phrases
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @since 3.1.7
 */
@SuppressWarnings("UnusedDeclaration")
public class FxPhraseQuery implements Serializable {
    public static enum MatchMode {
        IGNORE,
        EXACT,
        STARTS_WITH,
        CONTAINS
    }

    public static enum SortMode {
        POS_ASC,
        POS_DESC,
        KEY_ASC,
        KEY_DESC,
        TAG_ASC,
        TAG_DESC,
        VALUE_ASC,
        VALUE_DESC,
    }

    //null = all languages
    private Long searchLanguage;
    private Long resultLanguage;
    private Long[] phraseMandators;
    private String tagQuery;
    private MatchMode tagMatchMode;
    private String keyQuery;
    private MatchMode keyMatchMode;
    private String valueQuery;
    private MatchMode valueMatchMode;
    private Long treeNode;
    private Long treeNodeMandator;
    private Long[] treeNodeMappingOwner;
    private boolean includeChildNodes;
    //fetch only single language results or "full" FxPhrase values?
    private boolean fetchFullPhraseInfo;
    private SortMode sortMode;
    //show own mandator at the top or bottom of search results if orderd by position
    private boolean ownMandatorTop;
    //find only phrases that are not assigned to the treeNodeMappingOwners
    private boolean onlyUnassignedPhrases;

    public FxPhraseQuery() {
        reset();
    }

    public FxPhraseQuery reset() {
        this.tagQuery = null;
        this.tagMatchMode = MatchMode.IGNORE;
        this.keyQuery = null;
        this.keyMatchMode = MatchMode.IGNORE;
        this.valueQuery = null;
        this.valueMatchMode = MatchMode.IGNORE;
        this.treeNode = null;
        this.treeNodeMandator = null;
        this.includeChildNodes = true;
        this.searchLanguage = FxContext.getUserTicket().getLanguage().getId();
        this.resultLanguage = this.searchLanguage;
        this.fetchFullPhraseInfo = false;
        this.sortMode = SortMode.VALUE_ASC;
        this.ownMandatorTop = false;
        this.phraseMandators = null;
        this.treeNodeMappingOwner = null;
        this.onlyUnassignedPhrases = false;
        return this;
    }

    public FxPhraseQuery setTagMatch(MatchMode matchMode, String query) {
        this.tagQuery = query;
        this.tagMatchMode = matchMode;
        return this;
    }

    public Long getSearchLanguage() {
        return this.searchLanguage;
    }

    public void setSearchLanguage(Long searchLanguage) {
        this.searchLanguage = searchLanguage;
    }

    public boolean isSearchLanguageRestricted() {
        return this.searchLanguage != null;
    }

    public Long getResultLanguage() {
        return resultLanguage;
    }

    public void setResultLanguage(Long resultLanguage) {
        this.resultLanguage = resultLanguage;
    }

    public boolean isResultLanguageRestricted() {
        return this.resultLanguage != null;
    }

    public String getTagQuery() {
        return this.tagQuery;
    }

    public void setTagQuery(String tagQuery) {
        this.tagQuery = tagQuery;
    }

    public MatchMode getTagMatchMode() {
        return this.tagMatchMode;
    }

    public void setTagMatchMode(MatchMode tagMatchMode) {
        this.tagMatchMode = tagMatchMode;
    }

    public boolean isTagMatchRestricted() {
        return this.tagQuery != null && this.tagMatchMode != MatchMode.IGNORE;
    }

    public String getKeyQuery() {
        return this.keyQuery;
    }

    public void setKeyQuery(String keyQuery) {
        this.keyQuery = keyQuery;
    }

    public MatchMode getKeyMatchMode() {
        return this.keyMatchMode;
    }

    public void setKeyMatchMode(MatchMode keyMatchMode) {
        this.keyMatchMode = keyMatchMode;
    }

    public boolean isKeyMatchRestricted() {
        return this.keyQuery != null && this.keyMatchMode != MatchMode.IGNORE;
    }

    public String getValueQuery() {
        return this.valueQuery;
    }

    public void setValueQuery(String valueQuery) {
        this.valueQuery = valueQuery;
    }

    public MatchMode getValueMatchMode() {
        return this.valueMatchMode;
    }

    public void setValueMatchMode(MatchMode valueMatchMode) {
        this.valueMatchMode = valueMatchMode;
    }

    public boolean isValueMatchRestricted() {
        return this.valueQuery != null && this.valueMatchMode != MatchMode.IGNORE;
    }

    public Long getTreeNode() {
        return this.treeNode;
    }

    public void setTreeNode(Long treeNode) {
        this.treeNode = treeNode;
    }

    public boolean isTreeNodeRestricted() {
        return this.treeNode != null && this.treeNodeMandator != null;
    }

    public boolean isIncludeChildNodes() {
        return this.includeChildNodes;
    }

    public void setIncludeChildNodes(boolean includeChildNodes) {
        this.includeChildNodes = includeChildNodes;
    }

    public Long getTreeNodeMandator() {
        return treeNodeMandator;
    }

    public void setTreeNodeMandator(Long treeNodeMandator) {
        this.treeNodeMandator = treeNodeMandator;
    }

    public Long[] getPhraseMandators() {
        return phraseMandators;
    }

    public void setPhraseMandators(Long... phraseMandators) {
        this.phraseMandators = phraseMandators;
    }

    public boolean isPhraseMandatorRestricted() {
        return this.phraseMandators != null && this.phraseMandators.length > 0;
    }

    public Long[] getTreeNodeMappingOwner() {
        return treeNodeMappingOwner;
    }

    public void setTreeNodeMappingOwner(Long... treeNodeMappingOwner) {
        this.treeNodeMappingOwner = treeNodeMappingOwner;
    }

    public boolean isTreeNodeMappingOwnerRestricted() {
        return this.treeNodeMappingOwner != null && this.treeNodeMappingOwner.length > 0;
    }

    public boolean isFetchFullPhraseInfo() {
        return fetchFullPhraseInfo;
    }

    public void setFetchFullPhraseInfo(boolean fetchFullPhraseInfo) {
        this.fetchFullPhraseInfo = fetchFullPhraseInfo;
    }

    public SortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(SortMode sortMode) {
        this.sortMode = sortMode;
    }

    public boolean isOwnMandatorTop() {
        return ownMandatorTop;
    }

    public void setOwnMandatorTop(boolean ownMandatorTop) {
        this.ownMandatorTop = ownMandatorTop;
    }

    public boolean isOnlyUnassignedPhrases() {
        return onlyUnassignedPhrases;
    }

    public void setOnlyUnassignedPhrases(boolean onlyUnassignedPhrases) {
        this.onlyUnassignedPhrases = onlyUnassignedPhrases;
    }

    /**
     * Execute this query returning the result set
     *
     * @param page     page of the result set to load
     * @param pageSize number of results per page
     * @return FxPhraseQueryResult
     */
    public FxPhraseQueryResult execute(int page, int pageSize) {
        return EJBLookup.getPhraseEngine().search(this, page, pageSize);
    }
}
