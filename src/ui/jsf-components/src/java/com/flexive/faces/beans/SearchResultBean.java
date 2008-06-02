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
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.faces.model.FxGridDataModel;
import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;
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
import org.apache.myfaces.component.html.ext.HtmlDataTable;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchResultBean implements ActionBean, Serializable {
    private static final long serialVersionUID = -3167186971609121457L;
    private static final Log LOG = LogFactory.getLog(SearchResultBean.class);

    private FxResultSetDataModel resultModel = null;
    private FxGridDataModel gridModel = null;
    private int gridRows = -1;
    private int rowCount = -1;
    private int gridColumns = -1;
    private ResultLocation location = AdminResultLocations.ADMIN;
    private HtmlDataTable resultDataTable = null;
    private ResultSessionData sessionData = null;
    private Briefcase briefcase = null; // cached briefcase object for briefcase queries
    private VersionFilter versionFilter = VersionFilter.MAX;

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
                setQueryBuilder(getSqlQueryBuilder().condition("*", PropertyValueComparator.EQ, query));
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
                setQueryBuilder(getSqlQueryBuilder().isChild(id));
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
                setQueryBuilder(getSqlQueryBuilder().filterBriefcase(briefcaseId));
                show();
            }
        } catch (Exception e) {
            // TODO possibly pass some error message to the HTML page
            LOG.error("Failed to parse request parameters: " + e.getMessage(), e);
        }
        return null;
    }

    private SqlQueryBuilder getSqlQueryBuilder() {
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

    public FxResultSet getResult() {
        return resultModel != null ? resultModel.getResult() : null;
    }

    public FxResultSetDataModel getResultModel() {
        if (resultModel == null) {
            refreshResults();
        }
        return resultModel;
    }

    /**
     * Like {@link #getResultModel()}, but returns all rows of the
     * current query at once.
     *
     * @return a result model with all rows
     */
    public FxResultSetDataModel getAllRowsResultModel() {
        if (getFetchRows() > 0) {
            // reset result model and fetch all results
            setFetchRows(-1);
            resultModel = createResultModel();
            resultModel.fetchResult(0);
        }
        return getResultModel();
    }

    public FxGridDataModel getGridModel() {
        if (gridModel == null) {
            // calculate grid based on posted client document size (if available)
            if (getPreviewSize().getSize() < 0) {
                // unlimited dimensions --> show 1 thumbnail per page (original size)
                gridRows = 1;
                setFetchRows(-1);
                gridColumns = 1;
            } else {
                calcThumbnailCount();
            }
            resultModel = null; // force result model refresh
            gridModel = new FxGridDataModel(getResultModel(), gridColumns);
        }
        return gridModel;
    }

    public List<String> getColumnNames() {
        if (getQueryBuilder() == null) {
            return new ArrayList<String>(0);
        }
        return getQueryBuilder().getColumnNames();
    }

    /**
     * Returns the index of the first data column that should be visible to the user.
     *
     * @return the index of the first data column that should be visible to the user.
     */
    public int getFirstColumnIndex() {
        final List<String> columnNames = getQueryBuilder().getColumnNames();
        for (int i = 0; i < columnNames.size(); i++) {
            String columnName = columnNames.get(i);
            if (columnName.equals("*")) {
                return i;
            }
        }
        return 0;
    }

    public SqlQueryBuilder getQueryBuilder() {
        return getSessionData().getQueryBuilder().viewType(getViewType());
    }

    public void setQueryBuilder(SqlQueryBuilder queryBuilder) {
        getSessionData().setQueryBuilder(queryBuilder);
    }

    public void setStartRow(int rowStart) {
        getSessionData().setStartRow(rowStart);
    }


    public int getStartRow() {
        if (resultDataTable != null) {
            getSessionData().setStartRow(getResultDataTable().getFirst());
        }
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
        if (resultModel != null) {
            getSessionData().setContentTypes(resultModel.getContentTypes());
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

    private void refreshResults() {
        try {
            resultModel = createResultModel();
            if (getRowCount() >= 0) {
                // use cached row count to avoid DB updates
                resultModel.setRowCount(getRowCount());
            } else {
                // search not submitted yet - we'll use row 0 anyway, so submit search right now and keep row count
                resultModel.setRowIndex(0);
                setRowCount(resultModel.getRowCount());
            }
        } catch (Exception e) {
            new FxFacesMsgErr("SearchResult.err.query", e.getMessage()).addToContext();
        }
    }

    private FxResultSetDataModel createResultModel() {
        if (ResultViewType.THUMBNAILS.equals(getViewType())) {
            // determine number of row based on viewport size
            calcThumbnailCount();
        }
        final FxResultSetDataModel model = new FxResultSetDataModel(EJBLookup.getSearchEngine(), getQueryBuilder(),
                getStartRow() * Math.max(1, gridColumns), getFetchRows() != -1 ? getFetchRows() : 999999);
        model.setGridColumns(gridColumns);
        if (Boolean.TRUE.equals(createBriefcase)) {
            if (StringUtils.isBlank(briefcaseName)) {
                new FxFacesMsgErr("Briefcase.err.name").addToContext();
            } else {
                model.setBriefcaseName(briefcaseName);
                model.setBriefcaseDescription(briefcaseDescription);
                model.setBriefcaseAcl(briefcaseAclId != -1 ? CacheAdmin.getEnvironment().getACL(briefcaseAclId) : null);
                new FxFacesMsgInfo("Briefcase.nfo.created", briefcaseName).addToContext();
            }
        }
        model.setCacheMode(cacheMode);
        return model;
    }

    private void calcThumbnailCount() {
        gridColumns = getClientSizeX() > 0 ? (getClientSizeX() - 50) / Math.max(1, getPreviewSize().getSize() + 10) : 5;
        setFetchRows(getGridRows() * gridColumns);
    }

    public BinaryDescriptor.PreviewSizes getPreviewSize() {
        return getSessionData().getPreviewSize();
    }

    public void setPreviewSize(BinaryDescriptor.PreviewSizes previewSize) {
        if (!getPreviewSize().equals(previewSize)) {
            resetGridModel();
        }
        getSessionData().setPreviewSize(previewSize);
    }


    public int getClientSizeX() {
        return getSessionData().getClientSizeX();
    }

    public void setClientSizeX(int clientSizeX) {
        if (clientSizeX != getClientSizeX()) {
            resetGridModel();
        }
        getSessionData().setClientSizeX(clientSizeX);
    }

    public int getClientSizeY() {
        return getSessionData().getClientSizeY();
    }

    public void setClientSizeY(int clientSizeY) {
        if (clientSizeY != getClientSizeY()) {
            resetGridModel();
        }
        getSessionData().setClientSizeY(clientSizeY);
    }

    public int getGridRows() {
        if (gridRows == -1) {
            gridRows = getClientSizeY() > 0 ? (getClientSizeY() - 150) / Math.max(1, getPreviewSize().getSize() + 25) : 5;
        }
        return gridRows;
    }

    public void setGridRows(int gridRows) {
        this.gridRows = gridRows;
    }

    private void resetGridModel() {
        gridModel = null; // refresh thumbnail results
        gridRows = -1;
        rowCount = -1;
        if (resultDataTable != null) {
            // reset first row
            resultDataTable.setFirst(0);
        }
    }

    public HtmlDataTable getResultDataTable() {
        if (resultDataTable == null) {
            resultDataTable = new HtmlDataTable();
        }
        return resultDataTable;
    }

    public void setResultDataTable(HtmlDataTable resultDataTable) {
        this.resultDataTable = resultDataTable;
    }

    public int getRowCount() {
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
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
}
