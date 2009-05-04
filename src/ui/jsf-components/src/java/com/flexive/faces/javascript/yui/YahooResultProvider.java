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
package com.flexive.faces.javascript.yui;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.converter.EnumConverter;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import static com.flexive.shared.EJBLookup.getResultPreferencesEngine;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.exceptions.FxUpdateException;
import com.flexive.shared.search.*;
import com.flexive.shared.security.PermissionSet;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.value.*;
import com.flexive.shared.value.renderer.FxValueRendererFactory;
import com.flexive.war.JsonWriter;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.ArrayUtils;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;

/**
 * Provides map interfaces for generating JSON row and column information
 * from a FxResultSet.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class YahooResultProvider implements Serializable {
    private static final long serialVersionUID = -4200104398592163875L;
    private static final Log LOG = LogFactory.getLog(YahooResultProvider.class);
    private static final String[] RESOLVED_SYSTEM_PROPS = new String[]{ "ACL", "TYPEDEF", "CREATED_BY", "MODIFIED_BY" };

    /**
     * Returns a complete JSON representation of the given search result.
     *
     * @param result      the search result to be rendered
     * @param firstColumn index of the first column to be rendered (1-based)
     * @return the search result in JSON notation
     * @throws java.io.IOException if the JSON output could not be written
     */
    public static String getSearchResultJSON(FxResultSet result, int firstColumn) throws IOException {
        final StringWriter out = new StringWriter();
        final JsonWriter writer = new JsonWriter(out);
        writer.startMap();

        writer.writeAttribute("columnCount", result.getColumnCount());
        writer.writeAttribute("totalRowCount", result.getTotalRowCount());
        writer.writeAttribute("totalTime", result.getTotalTime());
        writer.writeAttribute("rowCount", result.getRowCount());
        writer.writeAttribute("startIndex", result.getStartIndex());
        writer.writeAttribute("viewType", result.getViewType());
        writer.writeAttribute("resultLocation", result.getLocation());
        createResultColumns(result, writer, firstColumn);
        createResultRows(result, writer, firstColumn);
        createResponseSchema(result, writer, firstColumn);

        writer.closeMap();
        writer.finishResponse();

        return out.toString();
    }

    public String getResultRows(String query, int firstColumn) throws FxApplicationException, IOException {
        final StringWriter out = new StringWriter();
        final FxResultSet result = EJBLookup.getSearchEngine().search(query, 0, Integer.MAX_VALUE, null);
        final JsonWriter writer = new JsonWriter(out);
        writer.startMap();
        createResultRows(result, writer, firstColumn);
        writer.closeMap();
        writer.finishResponse();
        return out.toString();
    }

    /**
     * Moves the given column to the new index in the user's result preferences.
     *
     * @param encodedLocation the location, encoded using fx:encodeEnum
     * @param encodedViewType the view type, encoded using fx:encodeEnum
     * @param columnKey       the column key (i.e. the property/assignment name)
     * @param index           the new index (0-based)
     * @return nothing
     * @since 3.1
     */
    public String reorderResultColumn(long typeId, String encodedViewType, String encodedLocation, String columnKey, int index) throws FxApplicationException {
        final ResultViewType viewType = (ResultViewType) EnumConverter.getValue(encodedViewType);
        final ResultLocation location = (ResultLocation) EnumConverter.getValue(encodedLocation);
        final ResultPreferences preferences = getResultPreferencesEngine().load(typeId, viewType, location);
        final ArrayList<ResultColumnInfo> columns = Lists.newArrayList(preferences.getSelectedColumns());

        if (columnKey.indexOf('/') != -1) {
            // assignment --> prefix with #
            columnKey = "#" + columnKey;
        }
        // get column index for columnKey
        ResultColumnInfo column = null;
        for (ResultColumnInfo rci : columns) {
            if (rci.getPropertyName().equalsIgnoreCase(columnKey)) {
                column = rci;
                break;
            }
        }
        if (column == null) {
            throw new FxUpdateException("ex.jsf.searchResult.resultPreferences.columnNotFound", columnKey);
        }

        // move column to new position
        final int oldIndex = columns.indexOf(column);
        columns.remove(oldIndex);
        columns.add(
                Math.min(
                        columns.size() - 1,
                        index > oldIndex ? index - 1 : index
                ),
                column
        );

        // store new result preferences
        getResultPreferencesEngine().saveInSource(
                new ResultPreferences(columns, preferences.getOrderByColumns(), preferences.getRowsPerPage(), preferences.getThumbBoxSize()),
                typeId, viewType, location
        );
        return "[]";
    }

    private static void createResultColumns(FxResultSet result, JsonWriter writer, int firstColumn) throws IOException {
        writer.startAttribute("columns");
        writer.startArray();
        for (int i = firstColumn; i <= result.getColumnCount(); i++) {
            writer.startMap();
            writer.writeAttribute("key", getColumnKey(result, i));
            writer.writeAttribute("label", result.getColumnLabel(i));
            writer.writeAttribute("sortable", true);
            writer.writeAttribute("resizeable", true);
            writer.closeMap();
        }
        writer.closeArray();
    }

    private static void createResultRows(FxResultSet result, JsonWriter writer, int firstColumn) throws IOException {
        writer.startAttribute("rows");
        writer.startArray();
        final SimpleDateFormat sortableDateFormat = new SimpleDateFormat("yyyyMMdd");
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        for (FxResultRow row : result.getResultRows()) {
            writer.startMap();
            for (int i = firstColumn; i <= result.getColumnCount(); i++) {
                final Object value = row.getValue(i);
                final String output;

                if (value instanceof FxFloat || value instanceof FxDouble) {
                    // always format decimal numbers in English locale, because
                    // the YUI datatable currently does not work with "," as separator (instead of ".")
                    output = FxValueRendererFactory.getInstance(null).format((FxValue) value);
                } else if (value instanceof FxDate || value instanceof FxDateTime) {
                    final Date date = (Date) ((FxValue) value).getBestTranslation();
                    output = createSortableDate(sortableDateFormat, value, date);
                } else if (value instanceof FxDateRange || value instanceof FxDateTimeRange) {
                    final DateRange range = (DateRange) ((FxValue) value).getBestTranslation();
                    output = createSortableDate(sortableDateFormat, value, range != null ? range.getLower() : null);
                } else {
                    output = FxJsfUtils.formatResultValue(result.getColumnName(i), value, null, null, null);
                }
                writer.writeAttribute(getColumnKey(result, i), output);
            }
            if (result.getColumnIndex("@pk") != -1) {
                // add special PK column
                writer.writeAttribute("pk", row.getPk("@pk"));
            }
            if (result.getColumnIndex("@permissions") != -1) {
                // add permissions object
                final PermissionSet permissions = row.getPermissions(result.getColumnIndex("@permissions"));
                int perms = 0;
                perms |= (permissions.isMayRead() ? 1 : 0);         // read
                perms |= (permissions.isMayCreate() ? 1 : 0) << 1;  // create
                perms |= (permissions.isMayDelete() ? 1 : 0) << 2;  // delete
                perms |= (permissions.isMayEdit() ? 1 : 0) << 3;  // edit
                perms |= (permissions.isMayExport() ? 1 : 0) << 4;  // export
                perms |= (permissions.isMayRelate() ? 1 : 0) << 5;  // relate
                writer.writeAttribute("permissions", perms);
            }
            writer.closeMap();
        }
        writer.closeArray();
    }

    private static String createSortableDate(SimpleDateFormat sortableDateFormat, Object value, Date date) {
        // add sortable value to enable sorting independent of the string representation
        return (date != null ? "<!--" + sortableDateFormat.format(date) + "-->" : "")
                + FxJsfUtils.formatResultValue(value, null, null, null);
    }

    /**
     * Renders the response schema needed for the YUI datatable.
     *
     * @param result      the result to be written
     * @param writer      the JSON output writer
     * @param firstColumn the first column to be included (1-based)
     * @throws java.io.IOException on output errors
     */
    private static void createResponseSchema(FxResultSet result, JsonWriter writer, int firstColumn) throws IOException {
        writer.startAttribute("responseSchema");
        writer.startMap();
        writer.startAttribute("fields");
        writer.startArray();
        final FxEnvironment environment = CacheAdmin.getEnvironment();
        for (int i = firstColumn; i <= result.getColumnCount(); i++) {
            writer.startMap();
            writer.writeAttribute("key", getColumnKey(result, i));
            String parser = "string";    // the YUI data parser used for sorting
            boolean funref = false;     // is parser a direct function reference?
            final String columnName = result.getColumnName(i);
            try {
                if ("@pk".equals(columnName)) {
                    // PKs get rendered as <id>.<version>
                    parser = "number";
                } else {
                    // set parser according to property type
                    final FxProperty property = environment.getProperty(columnName);
                    final Class valueClass = property.getEmptyValue().getValueClass();
                    if (Number.class.isAssignableFrom(valueClass)
                            && ArrayUtils.indexOf(RESOLVED_SYSTEM_PROPS, columnName) == -1) {
                        parser = "number";
                    }
                }
            } catch (FxRuntimeException e) {
                // property not found, use default
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Property '" + columnName + " not found (ignored): " + e.getMessage(), e);
                }
            }
            writer.writeAttribute("parser", parser, !funref);
            writer.closeMap();
        }
        // include primary key in attribute pk, if available
        if (result.getColumnIndex("@pk") != -1) {
            writer.startMap().writeAttribute("key", "pk").closeMap();
        }
        if (result.getColumnIndex("@permissions") != -1) {
            writer.startMap().writeAttribute("key", "permissions").closeMap();
        }
        writer.closeArray();
        writer.closeMap();
    }

    private static String getColumnKey(FxResultSet result, int index) {
        return result.getColumnName(index);
    }

    /**
     * Writes the current type counts for each found content type.
     *
     * @param writer the target writer
     * @param result the search result
     * @throws java.io.IOException if the output could not be written
     */
    private void writeUpdatedTypeCounts(JsonWriter writer, FxResultSet result) throws IOException {
        writer.startAttribute("typeCounts");
        writer.startMap();
        for (FxFoundType type : result.getContentTypes()) {
            writer.writeAttribute(String.valueOf(type.getContentTypeId()), type.getFoundEntries());
        }
        writer.closeMap();
    }

}