/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.shared.search;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxInvalidParameterException;

import java.util.ArrayList;
import java.util.List;

/**
 * Modifiable result preferences object.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ResultPreferencesEdit extends ResultPreferences {
    private static final long serialVersionUID = 5669756888973924325L;

    public ResultPreferencesEdit(List<ResultColumnInfo> selectedColumns, List<ResultOrderByInfo> orderByColumns, int rowsPerPage, int thumbBoxSize) {
        super(selectedColumns, orderByColumns, rowsPerPage, thumbBoxSize);
    }

    public <T extends ResultColumnInfo> void addSelectedColumn(T info) {
        FxSharedUtils.checkParameterEmpty(info, "SELECTEDCOLUMN");
        if (!selectedColumns.contains(info)) {
            selectedColumns.add(info);
        }
    }

    public <T extends ResultColumnInfo> void addSelectedColumn(int index, T info) {
        FxSharedUtils.checkParameterEmpty(info, "SELECTEDCOLUMN");
        if (!selectedColumns.contains(info)) {
            selectedColumns.add(Math.max(0, Math.min(selectedColumns.size(), index)), info);
        }
    }

    public <T extends ResultColumnInfo> ResultColumnInfo removeSelectedColumn(T info) {
        return removeSelectedColumn(selectedColumns.indexOf(info));
    }

    public ResultColumnInfo removeSelectedColumn(int index) {
        if (index >= 0 && selectedColumns.size() > index) {
            return selectedColumns.remove(index);
        } else {
            throw new FxInvalidParameterException("INDEX", "ex.ResultPreferences.selectedColumn.remove.index",
                    index, selectedColumns.size()).asRuntimeException();
        }
    }

    public <T extends ResultOrderByInfo> void addOrderByColumn(T info) {
        FxSharedUtils.checkParameterEmpty(info, "ORDERBYCOLUMN");
        if (!orderByColumns.contains(info)) {
            orderByColumns.add(info);
        }
    }

    public <T extends ResultOrderByInfo> void addOrderByColumn(int index, T info) {
        FxSharedUtils.checkParameterEmpty(info, "ORDERBYCOLUMN");
        if (!orderByColumns.contains(info)) {
            orderByColumns.add(Math.max(0, Math.min(orderByColumns.size(), index)), info);
        }
    }

    public <T extends ResultOrderByInfo> ResultOrderByInfo removeOrderByColumn(T info) {
        return removeOrderByColumn(orderByColumns.indexOf(info));
    }

    public ResultOrderByInfo removeOrderByColumn(int index) {
        if (index >= 0 && orderByColumns.size() > index) {
            return orderByColumns.remove(index);
        } else {
            throw new FxInvalidParameterException("INDEX", "ex.ResultPreferences.orderByColumn.remove.index",
                    index, orderByColumns.size()).asRuntimeException();
        }
    }

    public void setSelectedColumns(List<ResultColumnInfo> selectedColumns) {
        this.selectedColumns = new ArrayList<ResultColumnInfo>(selectedColumns);
    }

    public void setOrderByColumns(List<ResultOrderByInfo> orderByColumns) {
        this.orderByColumns = new ArrayList<ResultOrderByInfo>(orderByColumns);
    }

    public void setRowsPerPage(int rowsPerPage) {
        this.rowsPerPage = rowsPerPage;
    }

    public void setThumbBoxSize(int thumbBoxSize) {
        this.thumbBoxSize = thumbBoxSize;
    }

    public void setLastChecked(long lastChecked) {
        this.lastChecked = lastChecked;
    }
}
