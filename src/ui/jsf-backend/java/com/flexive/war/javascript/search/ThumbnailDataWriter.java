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

import com.flexive.faces.model.FxGridDataModel;
import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.value.BinaryDescriptor;
import com.flexive.war.JsonWriter;
import com.flexive.war.servlet.ThumbnailServlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class ThumbnailDataWriter extends TableDataWriter {
    private int gridColumns = 5;
    private final StringBuilder cellBuilder = new StringBuilder();

    ThumbnailDataWriter(JsonWriter out) {
        super(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Column> getColumns(FxResultSet rs) {
        final List<Column> result = new ArrayList<Column>();
        for (int i = 0; i < gridColumns; i++) {
            result.add(new Column(null, "html", i));
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeColumns(FxResultSetDataModel dataModel) throws IOException {
        gridColumns = dataModel.getGridColumns();
        super.writeColumns(dataModel);
    }

    private void writeCell(int columnIndex, Object[] values) throws IOException {
        final FxPK pk = (FxPK) values[0];
        final int boxSize = BinaryDescriptor.PreviewSizes.PREVIEW2.getSize();
        String label = "";
        if (values.length >= SqlQueryBuilder.COL_USERPROPS) {
            // render first user-defined property below image
            label = "<div class=\"label\">"
                    + FxSharedUtils.formatResultValue(values[SqlQueryBuilder.COL_USERPROPS], null, null, null)
                    + "</div>";
        }
        cellBuilder.setLength(0);
        cellBuilder
                .append("<div class=\"resultThumbnail").append(boxSize).append("\">")
                .append("<div ")    // add information flags for row selection
                .append("id=\"row").append(rowCounter).append("\" positionId=\"").append(values[0]).append("\"")
                .append(" rowNum=\"").append(rowCounter).append("\" colorSet=\"").append(columnIndex % 2).append("\"")
                .append(" onclick=\"rowSelection.onRowClicked(event)\" onmouseover=\"rowSelection.onMouseOver(event)\" onmouseout=\"rowSelection.onMouseOut(event)\"")
                .append(" class=\"inner1\">")
                        // render inner div
                .append("<div class=\"inner2\">")
                        // render thumbnail
                .append("<img src=\"")
                .append(ThumbnailServlet.getLink(pk, BinaryDescriptor.PreviewSizes.PREVIEW2, null,
                        /* TODO: select FxBinary timestamp */ System.currentTimeMillis()).substring(1))
                .append("\" alt=\"").append(pk).append("\" title=\"").append(values[0]).append("\"/>")
                .append("</div></div>")
                        // render bottom label
                .append(label)
                .append("</div>");
        out.writeAttribute(getColumnName(columnIndex), cellBuilder.toString());
        // add oid/version columns
        out.writeAttribute(COL_ID, pk.getId());
        out.writeAttribute(COL_VERSION, pk.getVersion());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRows(FxResultSetDataModel dataModel) throws IOException {
        gridColumns = dataModel.getGridColumns();
        out.startArray();
        final FxGridDataModel gridModel = new FxGridDataModel(dataModel, gridColumns);
        final int startRow = dataModel.getStartRow() / gridColumns;
        final int endRow = startRow + (dataModel.getRowCount() + gridColumns - 1) / gridColumns;
        rowCounter = 0;
        for (int i = startRow; i < endRow; i++) {
            gridModel.setRowIndex(i);
            if (!gridModel.isRowAvailable()) {
                break;
            }
            out.startMap();
            out.writeAttribute("rowId", i);
            final Object[] rowData = (Object[]) gridModel.getRowData();
            for (int j = 0; j < gridColumns; j++) {
                if (rowData[j] == null) {
                    break;
                }
                writeCell(j, (Object[]) rowData[j]);
                rowCounter++;
            }
            out.closeMap();
        }
        out.closeArray();
    }
}
