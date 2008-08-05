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
package com.flexive.faces.beans;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.javascript.yui.YahooResultProvider;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.search.*;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.search.query.VersionFilter;
import com.flexive.shared.value.BinaryDescriptor;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.faces.model.SelectItem;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchResultBean implements ActionBean, Serializable {
    private static final long serialVersionUID = -3167186971609121457L;
    private static final Log LOG = LogFactory.getLog(SearchResultBean.class);

    private FxResultSet result;
    private ResultLocation location = AdminResultLocations.ADMIN;
    private ResultSessionData sessionData = null;
    private Briefcase briefcase = null; // cached briefcase object for briefcase queries
    private VersionFilter versionFilter = VersionFilter.MAX;
    private int rowCount = -1;

    // cache settings
    private FxSQLSearchParams.CacheMode cacheMode = FxSQLSearchParams.CacheMode.ON;

    // briefcase-related fields set in the query form
    private long briefcaseAclId;
    private String briefcaseDescription;
    private String briefcaseName;
    private Boolean createBriefcase;


    /**
     * {@inheritDoc}
     */
    public String getParseRequestParameters() {
        try {
            String action = FxJsfUtils.getParameter("action");
            if (StringUtils.isBlank(action)) {
                // no action requested
                return null;
            }
            // hack!
            FxJsfUtils.resetFaceletsComponent("frm");
            if ("fulltextSearch".equals(action)) {
                String query = FxJsfUtils.getParameter("query");
                if (StringUtils.isBlank(query)) {
                    new FxFacesMsgErr("SearchResult.err.query.fulltext.empty").addToContext();
                    return null;
                }
                resetFilters();
                setQueryBuilder(createSqlQueryBuilder().condition("*", PropertyValueComparator.EQ, query));
                show();
            } else if ("nodeSearch".equals(action)) {
                // search in subtree
                if (StringUtils.isBlank(FxJsfUtils.getParameter("nodeId"))) {
                    new FxFacesMsgErr("SearchResult.err.query.node.empty").addToContext();
                    return null;
                }
                resetFilters();
                final long id = FxJsfUtils.getLongParameter("nodeId");
                final boolean liveTree = FxJsfUtils.getBooleanParameter("liveMode", false);
                setVersionFilter(liveTree ? VersionFilter.LIVE : VersionFilter.MAX);
                setQueryBuilder(createSqlQueryBuilder().isChild(id));
                show();
            } else if ("openBriefcase".equals(action) || "openBriefcaseDetails".equals(action)) {
                if (StringUtils.isBlank(FxJsfUtils.getParameter("briefcaseId"))) {
                    new FxFacesMsgErr("SearchResult.err.query.briefcase.empty").addToContext();
                    return null;
                }
                // TODO: open briefcase in own location
                final long briefcaseId = FxJsfUtils.getLongParameter("briefcaseId");
                resetFilters();
                getSessionData().setBriefcaseId(briefcaseId);
                setQueryBuilder(createSqlQueryBuilder().filterBriefcase(briefcaseId));
                show();
            }
        } catch (Exception e) {
            // TODO possibly pass some error message to the HTML page
            LOG.error("Failed to parse request parameters: " + e.getMessage(), e);
        }
        return null;
    }

    private SqlQueryBuilder createSqlQueryBuilder() {
        final SqlQueryBuilder builder = new SqlQueryBuilder(location, getViewType());
        builder.maxRows(getFetchRows());
        builder.filterVersion(getVersionFilter());
        return builder;
    }

    public void resetFilters() {
        setStartRow(0);
        getSessionData().setBriefcaseId(-1);
        setTypeId(-1);
        setVersionFilter(VersionFilter.MAX);
    }

    /**
     * Show the current search result.
     *
     * @return the next page
     */
    public String show() {
        // backend briefcase creation - name check
        if (Boolean.TRUE.equals(createBriefcase) && StringUtils.isBlank(briefcaseName)) {
            new FxFacesMsgErr("Briefcase.err.name").addToContext();
            return FxJsfUtils.getManagedBean(QueryEditorBean.class).show();
        }
        try {
            getResult();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return "contentResult";
    }

    /**
     * Switch to list view.
     *
     * @return the next page
     */
    public String listView() {
        setViewType(ResultViewType.LIST);
        setFetchRows((Integer) getFetchRowItems().get(1).getValue());
        setStartRow(0);
        return show();
    }

    /**
     * Switch to thumbnail view.
     *
     * @return the next page
     */
    public String thumbView() {
        setViewType(ResultViewType.THUMBNAILS);
        setStartRow(0);
        return show();
    }

    public FxResultSet getResult() throws FxApplicationException {
        if (result == null) {
            if (Boolean.TRUE.equals(createBriefcase)) {
                getQueryBuilder().saveInBriefcase(briefcaseName, briefcaseDescription, briefcaseAclId);
                new FxFacesMsgInfo("Briefcase.nfo.created", briefcaseName).addToContext();
            }

            result = getQueryBuilder()
                    .select("@pk", "@permissions", "@*")
                    .startRow(0)
                    .maxRows(Integer.MAX_VALUE)
                    .getResult();
        }
        return result;
    }

    public SqlQueryBuilder getQueryBuilder() {
        final SqlQueryBuilder builder = getSessionData().getQueryBuilder();
        return builder.viewType(getViewType()).filterType(getTypeId());
    }

    public void setQueryBuilder(SqlQueryBuilder queryBuilder) {
        getSessionData().setQueryBuilder(queryBuilder);
    }

    public void setStartRow(int rowStart) {
        getSessionData().setStartRow(rowStart);
    }

    public int getStartRow() {
        return getSessionData().getStartRow();
    }

    public int getFetchRows() {
        return getSessionData().getFetchRows();
    }

    public void setFetchRows(int fetchRows) {
        getSessionData().setFetchRows(fetchRows);
    }

    /**
     * Returns selectitems for possible values of the fetchRows property.
     *
     * @return selectitems for possible values of the fetchRows property.
     */
    public List<SelectItem> getFetchRowItems() {
        final List<SelectItem> result = new ArrayList<SelectItem>();
        for (int count : new int[]{10, 25, 50, 100}) {
            result.add(new SelectItem(count, MessageBean.getInstance().getMessage("SearchResult.label.fetchRows", count)));
        }
        return result;
    }

    public ResultViewType getViewType() {
        return getSessionData().getViewType();
    }

    public void setViewType(ResultViewType viewType) {
        getSessionData().setViewType(viewType);
    }

    public boolean isListView() {
        return ResultViewType.LIST.equals(getViewType());
    }

    public boolean isThumbnailView() {
        return ResultViewType.THUMBNAILS.equals(getViewType());
    }

    public void setTypeId(long typeId) {
        getSessionData().setTypeId(typeId);
    }

    public long getTypeId() {
        return getSessionData().getTypeId();
    }

    public List<FxFoundType> getContentTypes() {
        if (result != null) {
            getSessionData().setContentTypes(result.getContentTypes());
        }
        return getSessionData().getContentTypes();
    }

    /**
     * Return JSF selectitems to be displayed in the UI for selecting a type of the result set.
     *
     * @return JSF selectitems to be displayed in the UI for selecting a type of the result set.
     */
    public List<SelectItem> getContentTypeItems() {
        final List<SelectItem> items = new ArrayList<SelectItem>(getContentTypes().size() + 1);
        // "all results" item
        items.add(new SelectItem(-1, MessageBean.getInstance().getMessage("SearchResult.label.type.all")));
        // add an entry for every found content type
        for (FxFoundType type : getContentTypes()) {
            final String label = type.getDisplayName() + " (" + type.getFoundEntries() + ")";
            items.add(new SelectItem(type.getContentTypeId(), label));
        }
        return items;
    }

    public BinaryDescriptor.PreviewSizes getPreviewSize() {
        return getSessionData().getPreviewSize();
    }

    public void setPreviewSize(BinaryDescriptor.PreviewSizes previewSize) {
        getSessionData().setPreviewSize(previewSize);
    }

    public ResultLocation getLocation() {
        return location;
    }

    public void setLocation(ResultLocation location) {
        this.location = location;
    }

    public ResultSessionData getSessionData() {
        if (sessionData == null) {
            sessionData = ResultSessionData.getSessionData(FxJsfUtils.getSession(), location);
        }
        return sessionData;
    }

    public long getBriefcaseAclId() {
        return briefcaseAclId;
    }

    public void setBriefcaseAclId(long briefcaseAclId) {
        this.briefcaseAclId = briefcaseAclId;
    }

    public String getBriefcaseDescription() {
        return briefcaseDescription;
    }

    public void setBriefcaseDescription(String briefcaseDescription) {
        this.briefcaseDescription = briefcaseDescription;
    }

    public String getBriefcaseName() {
        return briefcaseName;
    }

    public void setBriefcaseName(String briefcaseName) {
        this.briefcaseName = briefcaseName;
    }

    public Boolean getCreateBriefcase() {
        return createBriefcase;
    }

    public void setCreateBriefcase(Boolean createBriefcase) {
        this.createBriefcase = createBriefcase;
    }

    public FxSQLSearchParams.CacheMode getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(FxSQLSearchParams.CacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    public Briefcase getBriefcase() throws FxApplicationException {
        final long briefcaseId = getSessionData().getBriefcaseId();
        if (briefcaseId == -1) {
            return null;
        }
        if (briefcase == null || briefcase.getId() != briefcaseId) {
            try {
                briefcase = EJBLookup.getBriefcaseEngine().load(briefcaseId);
            } catch (FxNotFoundException e) {
                getSessionData().setBriefcaseId(-1);
            }
        }
        return briefcase;
    }

    public long getBriefcaseId() throws FxApplicationException {
        return getSessionData().getBriefcaseId();
    }

    public VersionFilter getVersionFilter() {
        return getSessionData().getVersionFilter();
    }

    public void setVersionFilter(VersionFilter versionFilter) {
        getSessionData().setVersionFilter(versionFilter);
    }

    public List<SelectItem> getVersionItems() {
        final List<SelectItem> result = new ArrayList<SelectItem>(2);
        final MessageBean mb = MessageBean.getInstance();
        result.add(new SelectItem(VersionFilter.MAX, mb.getResource("SearchResult.label.version.max")));
        result.add(new SelectItem(VersionFilter.LIVE, mb.getResource("SearchResult.label.version.live")));
        return result;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    /**
     * Returns the JSON representation of the given result set. Currently the column and row format is determined
     * by the Yahoo DataTable widget.
     *
     * @param key can be one of the following:
     * <ul>
     * <li>A {@link FxResultSet} instance</li>
     * <li>A FxSQL query, e.g. <code>SELECT id, caption</code></li>
     * <li>A {@link SqlQueryBuilder} instance</li>
     * </ul>
     * @param viewType "THUMBNAILS" or "LIST" (only relevant when a FxSQL query is passed)
     *
     * @return  the JSON representation of the search result
     * @throws java.io.IOException  if the JSON output could not be written
     */
    public static String getJsonResult(Object key, String viewType) throws IOException {
        if (key == null) {
            return "{}";
        }
        final ResultViewType resultViewType = StringUtils.isNotBlank(viewType)
                ? ResultViewType.valueOf(viewType.toUpperCase())
                : ResultViewType.LIST;
        final FxResultSet result = getResultSet(key, AdminResultLocations.DEFAULT, resultViewType);
        return YahooResultProvider.getSearchResultJSON(result,
                result.getUserWildcardIndex() != -1 ? result.getUserWildcardIndex() : 1);
    }

    /**
     * Return a FxResultSet stored in key. Possibly create a new
     * model wrapper if a plain result set was passed.
     *
     * @param key the (map) key to be used for creating the data model
     * @param location
     * @param viewType
     * @return a result set data model
     */
    private static FxResultSet getResultSet(Object key, ResultLocation location, ResultViewType viewType) {
        try {
            if (key instanceof FxResultSet) {
                return (FxResultSet) key;
            } else if (key instanceof SqlQueryBuilder) {
                return ((SqlQueryBuilder) key).getResult();
            } else if (key instanceof String) {
                return EJBLookup.getSearchEngine().search((String) key, 0, Integer.MAX_VALUE, null,
                        location, viewType);
            }
            throw new IllegalArgumentException("Unknown query type in YahooResultProvider: "
                    + (key != null ? key.getClass() : null));
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
    }
}
