/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.faces.model;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.search.FxEmptyResultSet;
import com.flexive.shared.search.FxFoundType;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.FxSQLSearchParams;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.security.ACL;
import org.apache.commons.lang.StringUtils;

import javax.faces.model.DataModel;
import java.io.Serializable;
import java.util.List;

import static com.flexive.shared.EJBLookup.getSearchEngine;

public class FxResultSetDataModel extends DataModel implements Serializable {
    private static final long serialVersionUID = -3476884537673965415L;

    protected FxResultSet result = null;
    private int rowIndex = -1;

    // fields for lazy-loading
    private SqlQueryBuilder queryBuilder = null;
    private int startRow = -1;
    private int fetchRows = -1;
    private int rowCount = -1;    // externally cached number of result rows to prevent unnecessary db access
    private int gridColumns = 1;    // result rows per output (datatable) row

    private FxSQLSearchParams.CacheMode cacheMode = FxSQLSearchParams.CacheMode.ON;

    // fields for briefcase creation
    private ACL briefcaseAcl;
    private String briefcaseDescription;
    private String briefcaseName;


    /**
     * Initialize a result set model.
     *
     * @param result the result set
     */
    public FxResultSetDataModel(FxResultSet result) {
        super();
        if (result == null) {
            throw new FxInvalidParameterException("result",
                    "ex.sqlSearch.dataModel.result.empty").asRuntimeException();
        }
        this.result = result;
        this.rowIndex = result.getStartIndex();
        this.startRow = result.getStartIndex();
    }

    /**
     * Initialize a result set model. A query builder is used to actually return the
     * requested rows.
     *
     * @param queryBuilder the query builder to be used for submitting queries
     * @param startRow     the first row to be fetched (if not accessed via UIData)
     * @param fetchRows    the default number of rows to be fetched when the first row is accessed
     *                     (should match the number of rows accessed, e.g. in the UI)
     */
    public FxResultSetDataModel(SqlQueryBuilder queryBuilder,
                                int startRow, int fetchRows) {
        this.queryBuilder = queryBuilder;
        this.startRow = startRow;
        this.fetchRows = fetchRows;
        this.rowIndex = startRow;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowCount() {
        if (rowCount >= 0) {
            // use cached value
            return rowCount;
        }
        if (result == null) {
            fetchResult(startRow);
        }
        rowCount = result.getTotalRowCount();
        return rowCount;
    }

    public void setRowCount(int rowCount) {
        this.rowCount = rowCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getRowData() {
        if (rowIndex == -1) {
            return null;
        }
        fetchResult(rowIndex);
        return result.getRows().get(rowIndex - result.getStartIndex());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getRowIndex() {
        return rowIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getWrappedData() {
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRowAvailable() {
//        fetchResult(rowIndex);
        return isAvailableIndex();
    }

    private boolean isAvailableIndex() {
        final boolean lazyAvailable = result == null && rowIndex > -1 && startRow >= 0 && rowIndex >= startRow && rowIndex < startRow + fetchRows;
        final boolean resultAvailable = result != null && rowIndex > -1 && rowIndex >= result.getStartIndex()
                && rowIndex < result.getStartIndex() + result.getRows().size();
        return lazyAvailable || resultAvailable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setWrappedData(Object data) {
        this.result = (FxResultSet) data;
        setRowIndex(this.result != null ? 0 : -1);
    }

    /**
     * Return the wrapped result set.
     *
     * @return the wrapped result set.
     */
    public FxResultSet getResult() {
        return this.result;
    }

    /**
     * Return the content types present in the result set (independent of the current view).
     *
     * @return the content types present in the result set (independent of the current view).
     */
    public List<FxFoundType> getContentTypes() {
        return getResult().getContentTypes();
    }

    public int getStartRow() {
        return startRow;
    }

    public int getFetchRows() {
        return fetchRows;
    }

    public ACL getBriefcaseAcl() {
        return briefcaseAcl;
    }

    public void setBriefcaseAcl(ACL briefcaseAcl) {
        this.briefcaseAcl = briefcaseAcl;
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

    public FxSQLSearchParams.CacheMode getCacheMode() {
        return cacheMode;
    }

    public void setCacheMode(FxSQLSearchParams.CacheMode cacheMode) {
        this.cacheMode = cacheMode;
    }

    public int getGridColumns() {
        return gridColumns;
    }

    public void setGridColumns(int gridColumns) {
        this.gridColumns = gridColumns;
    }

    /**
     * Check if the current result contains the row with the given index, or
     * submit a new search starting at index if it doesn't.
     *
     * @param index the row index to be accessed
     */
    public void fetchResult(int index) {
        if (queryBuilder == null || rowIndex == -1 || (result != null && isAvailableIndex())) {
            // use existing data for all operations
            return;
        }
        // fetch rows beginning at index
        /*if (queryBuilder == null) {
            throw new FxInvalidParameterException("searchEngine", "ex.sqlSearch.dataModel.queryBuilder.empty").asRuntimeException();
        } */
        try {
            FxSQLSearchParams params = new FxSQLSearchParams();
            if (StringUtils.isNotBlank(briefcaseName)) {
                params.saveResultInBriefcase(briefcaseName, briefcaseDescription, briefcaseAcl);
            }
            params.setCacheMode(cacheMode);
            result = getSearchEngine().search(queryBuilder.getQuery(), index, fetchRows, params, queryBuilder.getLocation(), queryBuilder.getViewType());
        } catch (FxApplicationException e) {
            result = new FxEmptyResultSet();
            // suppress exception to allow page to be rendered normally, but add a JSF error message
            new FxFacesMsgErr(e).addToContext();
        }
    }
}
