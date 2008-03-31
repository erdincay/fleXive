/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
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

import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.war.JsonWriter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
class ListDataWriter extends TableDataWriter {
    ListDataWriter(JsonWriter out) {
        super(out);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Column> getColumns(FxResultSet rs) {
        if (rs == null) {
            return new ArrayList<Column>(0);
        }
        List<Column> result = new ArrayList<Column>(rs.getColumnCount());
        for (int i = SqlQueryBuilder.COL_USERPROPS; i < rs.getColumnCount(); i++) {
            // TODO determine correct data type
            result.add(new Column(rs.getColumnLabel(i + 1), null, i));
        }
        return result;
    }

    private void writeRow(Object[] values) throws IOException {
        assert values[0] instanceof FxPK;

        out.startMap();
        out.writeAttribute("rowId", rowCounter);    // TODO: do we really need this?
        out.writeAttribute("id", "row" + rowCounter);   // write variables required for selection.js
        out.writeAttribute("rowNum", rowCounter);
        out.writeAttribute("positionId", values[0].toString());    // write PK
        out.writeAttribute("colorSet", rowCounter % 2);

        for (int i = SqlQueryBuilder.COL_USERPROPS; i < values.length; i++) {
            out.writeAttribute(getColumnName(i), FxSharedUtils.formatResultValue(values[i], null, null, null));
            if (values[i] instanceof FxPK) {
                // add oid/version columns
                FxPK pk = (FxPK) values[i];
                out.writeAttribute(COL_ID, pk.getId());
                out.writeAttribute(COL_VERSION, pk.getVersion());
            }
        }
        out.closeMap();
        rowCounter++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRows(FxResultSetDataModel dataModel) throws IOException {
        out.startArray();
        if (dataModel != null) {
            rowCounter = 0;
            for (int i = dataModel.getStartRow(); i < dataModel.getStartRow() + dataModel.getFetchRows(); i++) {
                dataModel.setRowIndex(i);
                if (!dataModel.isRowAvailable()) {
                    break;
                }
                writeRow((Object[]) dataModel.getRowData());
            }
        }
        out.closeArray();
    }

}

