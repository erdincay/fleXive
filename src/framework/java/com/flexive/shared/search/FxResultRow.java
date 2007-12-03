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

import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.value.FxValue;

import java.util.Arrays;

/**
 * Provides a thin wrapper for a result row of a SQL search
 * result set.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxResultRow {
    private final FxResultSet resultSet;
    private final int index;

    public FxResultRow(FxResultSet resultSet, int index) {
        if (index < 0 || index > resultSet.getRows().size()) {
            throw new FxInvalidParameterException("INDEX", "ex.sqlSearch.resultRow.index",
                    index, resultSet.getRows().size()).asRuntimeException();
        }
        this.resultSet = resultSet;
        this.index = index;
    }

    public Object[] getData() {
        return resultSet.getRows().get(index);
    }

    public String[] getColumnNames() {
        return resultSet.getColumnNames();
    }

    public int getColumnIndex(String columnName) {
        return resultSet.getColumnIndex(columnName);
    }

    public Object getValue(int column) {
        return getData()[column - 1];
    }

    public FxPK getPk(int column) {
        return (FxPK) getValue(column);
    }

    public FxValue getFxValue(int column) {
        return (FxValue) getValue(column);
    }

    public FxPaths getPaths(int column) {
        return (FxPaths) getValue(column);
    }

    public Object getValue(String columnName) {
        final int columnIndex = getColumnIndex(columnName);
        if (columnIndex == -1) {
            throw new FxNotFoundException("ex.sqlSearch.resultRow.column.notfound",
                    columnName, Arrays.asList(resultSet.getColumnNames())).asRuntimeException();
        }
        return getData()[columnIndex - 1];
    }

    public FxPK getPk(String columnName) {
        return (FxPK) getValue(columnName);
    }

    public FxValue getFxValue(String columnName) {
        return (FxValue) getValue(columnName);
    }

    public FxPaths getPaths(String columnName) {
        return (FxPaths) getValue(columnName);
    }

}
