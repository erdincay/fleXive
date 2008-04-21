/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.javascript.search;

import com.flexive.faces.beans.ResultSessionData;
import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.search.*;
import static com.flexive.shared.search.ResultViewType.THUMBNAILS;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.war.JsonWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides map interfaces for generating JSON row and column information
 * from a FxResultSet.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SearchResultWriter implements Serializable {
    private static final long serialVersionUID = -4200104398592163875L;
    private static final Log LOG = LogFactory.getLog(SearchResultWriter.class);

    /**
     * Return a map for generating column JSON code.
     * e.g. #{searchResultJsonBean.columns[myResultSet]}
     *
     * @return a map for generating column JSON code.
     */
    public Map getColumns() {
        return new ColumnDataMap();
    }

    /**
     * Return a map for generating the rows contained in a result set.
     * e.g. #{searchResultJsonBean.rows[myResultSet]}
     *
     * @return a map for generating the rows contained in a result set.
     */
    public Map getRows() {
        return new RowDataMap();
    }

    /**
     * Return the rows of the user's current search result (SearchResultBean).
     *
     * @param session      the current user session (json/rpc auto parameter)
     * @param location     location of the search result ({@link com.flexive.shared.search.AdminResultLocations}).
     * @param startRow     first row to be returned
     * @param fetchRows    number of rows to be fetched (must be a valid enum value of {@link ResultViewType})
     * @param gridColumns  number of columns on the grid (for thumbnail views)
     * @param viewTypeName the view type
     * @param typeId       the type filter (-1 to show all types)
     * @param sortIndex    column index (of the rendered columns) used for sorting. If set to -1, the
     * sort order set in the result preferences will be used automatically.
     * @param ascending    sort direction (only used if sortIndex is set)
     * @return the rows of the user's current search result (SearchResultBean).
     * @throws Exception   if the search could not be submitted successfully
     */
    public String getCurrentResultRows(HttpSession session, String location, int startRow, int fetchRows,
                                       int gridColumns, String viewTypeName, long typeId, int sortIndex, boolean ascending) throws Exception {
        StringWriter out = new StringWriter();
        try {
            if (viewTypeName.indexOf("::") != -1) {
                // if the fully qualified enum id is supplied, extract the enum value
                viewTypeName = viewTypeName.substring(viewTypeName.indexOf("::") + 2);
            }
            final ResultViewType viewType = ResultViewType.valueOf(viewTypeName);
            // get session data from the HTTP session since the faces context is not available
            final ResultSessionData sessionData = ResultSessionData.getSessionData(session, AdminResultLocations.valueOf(location));
            // update startrow and fetchrows
            sessionData.setStartRow(startRow);
            sessionData.setFetchRows(fetchRows);
            sessionData.setTypeId(typeId);
            SqlQueryBuilder queryBuilder = sessionData.getQueryBuilder();
            queryBuilder.filterType(typeId);
            if (sortIndex != -1) {
                queryBuilder.orderBy(SqlQueryBuilder.COL_USERPROPS + sortIndex + 1,
                        ascending ? SortDirection.ASCENDING : SortDirection.DESCENDING);
            } else {
                queryBuilder.clearOrderBy();
            }
            //queryBuilder.orderBy(sortColumn + SqlQueryBuilder.COL_USERPROPS, sortDirection == 0); 
            // execute search
            FxResultSetDataModel resultModel = new FxResultSetDataModel(EJBLookup.getSearchEngine(),
                    queryBuilder, startRow, fetchRows);
            resultModel.setGridColumns(THUMBNAILS.equals(viewType) ? gridColumns : 1);
            final JsonWriter writer = new JsonWriter(out);
            writer.startMap();
            writer.writeAttribute("totalRows", resultModel.getRowCount());
            writer.writeAttribute("startRow", startRow);
            writer.writeAttribute("fetchRows", fetchRows);
            writer.startAttribute("columns");
            TableDataWriter.getInstance(writer, viewType).writeColumns(resultModel);
            writer.startAttribute("rows");
            TableDataWriter.getInstance(writer, viewType).writeRows(resultModel);
            writeUpdatedTypeCounts(writer, resultModel);
            writer.closeMap();
            sessionData.setContentTypes(resultModel.getResult().getContentTypes());
            return out.toString();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Writes the current type counts for each found content type.
     *
     * @param writer      the target writer
     * @param resultModel the result model
     * @throws IOException if the output could not be written
     */
    private void writeUpdatedTypeCounts(JsonWriter writer, FxResultSetDataModel resultModel) throws IOException {
        writer.startAttribute("typeCounts");
        writer.startMap();
        for (FxFoundType type : resultModel.getResult().getContentTypes()) {
            writer.writeAttribute(String.valueOf(type.getContentTypeId()), type.getFoundEntries());
        }
        writer.closeMap();
    }

    private static class ColumnDataMap extends AbstractMap<FxResultSet, String> {
        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Entry<FxResultSet, String>> entrySet() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get(Object key) {
            FxResultSetDataModel resultSetModel = getDataModel(key);
            StringWriter out = new StringWriter();
            try {
                TableDataWriter.getInstance(out, resultSetModel.getResult().getViewType()).writeColumns(resultSetModel);
            } catch (IOException e) {
                LOG.error(e, e);
            }
            return out.toString();
        }

    }

    private static final class RowDataMap extends AbstractMap<FxResultSet, String> {
        /**
         * {@inheritDoc}
         */
        @Override
        public Set<Entry<FxResultSet, String>> entrySet() {
            return null;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String get(Object key) {
            FxResultSetDataModel resultSetModel = getDataModel(key);
            StringWriter out = new StringWriter();
            try {
                TableDataWriter.getInstance(out, resultSetModel.getResult().getViewType()).writeRows(resultSetModel);
            } catch (IOException e) {
                LOG.error(e, e);
            }
            return out.toString();
        }

    }

    /**
     * Return a FxResultSetDataModel stored in key. Possibly create a new
     * model wrapper if a plain result set was passed.
     *
     * @param key the (map) key to be used for creating the data model
     * @return a result set data model
     */
    private static FxResultSetDataModel getDataModel(Object key) {
        if (key instanceof FxResultSet) {
            // create a new model
            return new FxResultSetDataModel((FxResultSet) key);
        } else {
            // use existing model
            return (FxResultSetDataModel) key;
        }
    }
}
