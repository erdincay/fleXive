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
package com.flexive.faces.javascript.yui.search;

import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.ResultViewType;
import com.flexive.war.JsonWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Renders columns and rows in JSON notation to be used
 * by Dojo's table widget.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
abstract class TableDataWriter {
    protected static final String COL_ID = "__id";
    protected static final String COL_VERSION = "__version";

    public static class Column {
        private final String label;
        private final String dataType;
        private final int index;

        public Column(String label, String dataType, int index) {
            this.label = label;
            this.dataType = dataType;
            this.index = index;
        }

        public String getLabel() {
            return label;
        }

        public String getDataType() {
            return dataType;
        }

        public int getIndex() {
            return index;
        }
    }

    protected JsonWriter out;
    protected int rowCounter = 0;

    protected TableDataWriter(JsonWriter out) {
        this.out = out;
    }

    public static TableDataWriter getInstance(JsonWriter out, ResultViewType viewType) {
        switch (viewType) {
            case LIST:
                return new ListDataWriter(out);
            case THUMBNAILS:
                return new ThumbnailDataWriter(out);
        }
        throw new IllegalArgumentException("Unknown viewType: " + viewType);
    }

    public static TableDataWriter getInstance(Writer out, ResultViewType viewType) {
        return getInstance(new JsonWriter(out), viewType);
    }

    public void writeColumns(List<Column> columns) throws IOException {
        out.startArray();
        for (Column column : columns) {
            out.startMap();
            out.writeAttribute("field", getColumnName(column.getIndex()));
            if (column.getDataType() != null) {
                out.writeAttribute("dataType", column.getDataType());
            }
            out.writeAttribute("label", column.getLabel());
            out.closeMap();
        }
        out.closeArray();
    }

    /**
     * Render the column headers for the given search result set.
     *
     * @param rs the search result set
     * @throws java.io.IOException if the output could not be written
     */
    public void writeColumns(FxResultSet rs) throws IOException {
        writeColumns(getColumns(rs));
    }

    /**
     * Generate columns for the given result set.
     *
     * @param rs the result set
     * @return generated columns
     */
    public abstract List<Column> getColumns(FxResultSet rs);

    /**
     * Renders all rows contained in the result set.
     *
     * @param result the result set to be written
     * @throws java.io.IOException if the output could not be written
     */
    public abstract void writeRows(FxResultSet result) throws IOException;

    protected String getColumnName(int index) {
		return "c" + (index + 1);
	}

}