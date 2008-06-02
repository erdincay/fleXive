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
import com.flexive.shared.search.FxFoundType;
import com.flexive.shared.search.ResultLocation;
import com.flexive.shared.search.ResultViewType;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.search.query.VersionFilter;
import com.flexive.shared.value.BinaryDescriptor;

import javax.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

/**
 * Wrapper class for all session data needed by a search result page.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ResultSessionData implements Serializable {
    private static final long serialVersionUID = -3471062917140804393L;

    private ResultLocation location;
    private int clientSizeX;
    private int clientSizeY;
    private SqlQueryBuilder queryBuilder;
    private BinaryDescriptor.PreviewSizes previewSize;
    private long briefcaseId;
    private ResultViewType viewType = ResultViewType.LIST;
    private int startRow;
    private int fetchRows = 25;
    private long typeId = -1;
    private VersionFilter versionFilter = VersionFilter.MAX;
    private List<FxFoundType> contentTypes = new ArrayList<FxFoundType>(0);

    // Keep a reference on the current session if not retrieved from a JSF context
    private transient HttpSession session = null;

    /**
     * Session key for storing the current search session data. The first parameter
     * identifies the {@link com.flexive.shared.search.AdminResultLocations}
     * for the current search result. Thus each "location" is independent from the state
     * of others, but it is not possible to have two separate instances of the same location
     * (i.e. two browser windows).
     */
    private static String SESSION_DATA_STORE = "SearchResultBean/%s";

    /**
     * Creates an empty session data object.
     *
     * @param location the search result location
     */
    private ResultSessionData(ResultLocation location) {
        this(location, -1, -1, new SqlQueryBuilder(), BinaryDescriptor.PreviewSizes.PREVIEW2, -1);
    }

    /**
     * Copy constructor. Creates an independent copy of the given session data object.
     *
     * @param other the session data object to be copied
     */
    public ResultSessionData(ResultSessionData other) {
        this(other.location, other.clientSizeX, other.clientSizeY, new SqlQueryBuilder(other.queryBuilder), other.previewSize, other.briefcaseId);
    }

    private ResultSessionData(ResultLocation location, int clientSizeX, int clientSizeY, SqlQueryBuilder queryBuilder, BinaryDescriptor.PreviewSizes previewSize, long briefcaseId) {
        this.location = location;
        this.clientSizeX = clientSizeX;
        this.clientSizeY = clientSizeY;
        this.queryBuilder = queryBuilder;
        this.previewSize = previewSize;
        this.briefcaseId = briefcaseId;
    }

    public static ResultSessionData getSessionData(HttpSession session, ResultLocation location) {
        ResultSessionData data = (ResultSessionData) session.getAttribute(getSessionKey(location));
        if (data == null) {
            data = new ResultSessionData(location);
            data.session = session;
            data.saveInSession();
        }
        data.session = session;
        return data;
    }

    private static String getSessionKey(ResultLocation location) {
        return new Formatter().format(ResultSessionData.SESSION_DATA_STORE, location.getName()).toString();
    }

    private void saveInSession() {
        (session == null ? FxJsfUtils.getSession() : session).setAttribute(getSessionKey(location), this);
    }

    public ResultLocation getLocation() {
        return location;
    }

    public int getClientSizeX() {
        return clientSizeX;
    }

    public void setClientSizeX(int clientSizeX) {
        this.clientSizeX = clientSizeX;
        saveInSession();
    }

    public int getClientSizeY() {
        return clientSizeY;
    }

    public void setClientSizeY(int clientSizeY) {
        this.clientSizeY = clientSizeY;
        saveInSession();
    }

    public SqlQueryBuilder getQueryBuilder() {
        return queryBuilder;
    }

    public void setQueryBuilder(SqlQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
        saveInSession();
    }

    public BinaryDescriptor.PreviewSizes getPreviewSize() {
        return previewSize;
    }

    public void setPreviewSize(BinaryDescriptor.PreviewSizes previewSize) {
        this.previewSize = previewSize;
        saveInSession();
    }

    public long getBriefcaseId() {
        return briefcaseId;
    }

    public void setBriefcaseId(long briefcaseId) {
        this.briefcaseId = briefcaseId;
        saveInSession();
    }

    public ResultViewType getViewType() {
        return viewType;
    }

    public void setViewType(ResultViewType viewType) {
        this.viewType = viewType;
        saveInSession();
    }

    public int getStartRow() {
        return startRow;
    }

    public void setStartRow(int startRow) {
        this.startRow = startRow;
        saveInSession();
    }

    public int getFetchRows() {
        return fetchRows;
    }

    public void setFetchRows(int fetchRows) {
        this.fetchRows = fetchRows;
        saveInSession();
    }

    public long getTypeId() {
        return typeId;
    }

    public void setTypeId(long typeId) {
        this.typeId = typeId;
        saveInSession();
    }

    public List<FxFoundType> getContentTypes() {
        return contentTypes;
    }

    public void setContentTypes(List<FxFoundType> contentTypes) {
        this.contentTypes = contentTypes;
        saveInSession();
    }

    public VersionFilter getVersionFilter() {
        return versionFilter;
    }

    public void setVersionFilter(VersionFilter versionFilter) {
        this.versionFilter = versionFilter;
        saveInSession();
    }
}
