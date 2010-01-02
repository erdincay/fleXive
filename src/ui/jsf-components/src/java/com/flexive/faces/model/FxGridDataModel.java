/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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

import javax.faces.model.DataModel;
import java.io.Serializable;

/**
 * Organizes a linear DataModel in a 2D grid. Used for thumbnail gallerys.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxGridDataModel extends DataModel implements Serializable {
    private static final long serialVersionUID = -4884058521577844424L;

    private final int columns;
    private DataModel dataModel;
    private int rowIndex = -1;

    public FxGridDataModel(DataModel dataModel, int columns) {
        this.dataModel = dataModel;
        this.columns = columns;
    }

    @Override
    public boolean isRowAvailable() {
        return dataModel.isRowAvailable();
    }

    @Override
    public int getRowCount() {
        return dataModel.getRowCount() / columns + (dataModel.getRowCount() % columns > 0 ? 1 : 0);
    }

    @Override
    public Object getRowData() {
        Object[] rowData = new Object[columns];
        dataModel.setRowIndex(rowIndex * columns);
        try {
            int ctr = 0;
            while (dataModel.isRowAvailable() && ctr < columns) {
                rowData[ctr++] = dataModel.getRowData();
                dataModel.setRowIndex(dataModel.getRowIndex() + 1);
            }
            return rowData;
        } finally {
            // reset backing datamodel row index
            dataModel.setRowIndex(rowIndex * columns);
        }
    }

    @Override
    public int getRowIndex() {
        return rowIndex;
    }

    @Override
    public void setRowIndex(int rowIndex) {
        this.rowIndex = rowIndex;
        dataModel.setRowIndex(rowIndex >= 0 ? rowIndex * columns : -1);
    }

    @Override
    public Object getWrappedData() {
        return dataModel;
    }

    @Override
    public void setWrappedData(Object data) {
        this.dataModel = (DataModel) data;
    }

    public int getColumns() {
        return columns;
    }
}
