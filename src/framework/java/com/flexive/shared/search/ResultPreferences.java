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
package com.flexive.shared.search;

import com.flexive.shared.FxSharedUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result preferences object.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ResultPreferences implements Serializable {
    private static final long serialVersionUID = -3570651021661098088L;

    protected List<ResultColumnInfo> selectedColumns = new ArrayList<ResultColumnInfo>();
    protected List<ResultOrderByInfo> orderByColumns = new ArrayList<ResultOrderByInfo>();
    protected int rowsPerPage = -1;     // maximum rows per page
    protected int thumbBoxSize = 100;   // size of thumbnail box
    protected long lastChecked = -1;    // timestamp of last structural check

    public ResultPreferences() {
    }

    public ResultPreferences(List<ResultColumnInfo> selectedColumns, List<ResultOrderByInfo> orderByColumns, int rowsPerPage, int thumbBoxSize) {
        FxSharedUtils.checkParameterNull(selectedColumns, "SELECTEDCOLUMNS");
        FxSharedUtils.checkParameterNull(orderByColumns, "ORDERBYCOLUMNS");
        this.selectedColumns = new ArrayList<ResultColumnInfo>(selectedColumns);
        this.orderByColumns = new ArrayList<ResultOrderByInfo>(orderByColumns);
        this.rowsPerPage = rowsPerPage;
        this.thumbBoxSize = thumbBoxSize;
    }

    public List<ResultColumnInfo> getSelectedColumns() {
        return Collections.unmodifiableList(selectedColumns);
    }

    public List<ResultOrderByInfo> getOrderByColumns() {
        return Collections.unmodifiableList(orderByColumns);
    }

    public int getRowsPerPage() {
        return rowsPerPage;
    }

    public int getThumbBoxSize() {
        return thumbBoxSize;
    }

    public long getLastChecked() {
        return lastChecked;
    }

    public ResultPreferencesEdit getEditObject() {
        return new ResultPreferencesEdit(selectedColumns, orderByColumns, rowsPerPage, thumbBoxSize);
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof ResultPreferences)) return false;

        ResultPreferences that = (ResultPreferences) o;

        if (rowsPerPage != that.rowsPerPage) return false;
        if (thumbBoxSize != that.thumbBoxSize) return false;
        if (!orderByColumns.equals(that.orderByColumns)) return false;
        //noinspection RedundantIfStatement
        if (!selectedColumns.equals(that.selectedColumns)) return false;

        return true;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        int result;
        result = selectedColumns.hashCode();
        result = 31 * result + orderByColumns.hashCode();
        result = 31 * result + rowsPerPage;
        result = 31 * result + thumbBoxSize;
        return result;
    }


    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "ResultPreferences{selectedColumns: " + selectedColumns + ", orderByColumns:" + orderByColumns
                + ", rowsPerPage: " + rowsPerPage + ", thumbBoxSize: " + thumbBoxSize + "}";
    }
}
