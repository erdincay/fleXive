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
package com.flexive.shared.search;

import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.value.*;
import com.flexive.shared.security.PermissionSet;

import java.util.Arrays;
import java.util.List;
import java.util.Date;

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

    public List<FxPaths.Path> getPaths(int column) {
        return ((FxPaths) getValue(column)).getPaths();
    }

    public List<FxPaths.Path> getPaths(String columnName) {
        return getPaths(getColumnIndex(columnName));
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

    public long getLong(int column) {
        final Object value = getValue(column);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof FxNumber) {
            return ((FxNumber) value).getBestTranslation();
        } else if (value instanceof FxLargeNumber) {
            return ((FxLargeNumber) value).getBestTranslation();
        }
        throw new FxInvalidParameterException("column", "ex.sqlSearch.resultRow.invalidType",
                column, getColumnNames()[column - 1], "Long", value.getClass().getName()).asRuntimeException();
    }

    public long getLong(String columnName) {
        return getLong(getColumnIndex(columnName));
    }

    public int getInt(int column) {
        final Object value = getValue(column);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof FxNumber) {
            return ((FxNumber) value).getBestTranslation();
        }
        throw new FxInvalidParameterException("column", "ex.sqlSearch.resultRow.invalidType",
                column, getColumnNames()[column - 1], "Integer", value.getClass().getName()).asRuntimeException();
    }

    public int getInt(String columnName) {
        return getInt(getColumnIndex(columnName));
    }

    public String getString(int column) {
        final Object value = getValue(column);
        if (value instanceof FxString) {
            return ((FxString) value).getBestTranslation();
        } else if (value instanceof String) {
            return (String) value;
        } else {
            return value != null ? value.toString() : null;
        }
    }

    public String getString(String columnName) {
        return getString(getColumnIndex(columnName));
    }

    public Date getDate(int column) {
        final Object value = getValue(column);
        if (value instanceof FxDate) {
            return ((FxDate) value).getBestTranslation();
        } else if (value instanceof FxDateTime) {
            return ((FxDateTime) value).getBestTranslation();
        } else if (value instanceof Date) {
            return (Date) value;
        }
        throw new FxInvalidParameterException("column", "ex.sqlSearch.resultRow.invalidType",
                column, getColumnNames()[column - 1], "Date", value.getClass().getName()).asRuntimeException();
    }

    public Date getDate(String columnName) {
        return getDate(getColumnIndex(columnName));
    }

    public PermissionSet getPermissions(int column) {
        return (PermissionSet) getValue(column);
    }

    public PermissionSet getPermissions(String columnName) {
        return getPermissions(getColumnIndex(columnName));
    }
}
