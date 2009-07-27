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
package com.flexive.shared.search.cmis;

import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.exceptions.FxInvalidParameterException;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

/**
 * A row in a {@link CmisResultSet}. The column values are boxed in {@link CmisResultValue} objects.
 * <p>
 * You can iterate over the columns of this row using it as an iterator, or access the columns directly:
 * <pre>
 * final CmisResultRow row = ...;
 * for (CmisResultValue column : row) {
 *     System.out.println(column.getValue());
 * }
 * System.out.println(row.getColumn(1));        // prints the contents of the first column
 * System.out.println(row.getColumn("name"));   // access the column through its alias   
 * </pre>
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class CmisResultRow implements Serializable, Iterable<CmisResultValue> {
    private static final long serialVersionUID = 4584512684734863509L;
    private static final Log LOG = LogFactory.getLog(CmisResultRow.class);

    private final CmisResultValue[] columns;
    private final List<String> aliases;
    private boolean frozen;

    /**
     * Create a new result row. To add a new row to a result set, create the row with
     * {@link com.flexive.shared.search.cmis.CmisResultSet#newRow()}.
     *
     * @param aliases the column aliases (fixes column count)
     */
    CmisResultRow(List<String> aliases) {
        this.aliases = aliases;
        this.columns = new CmisResultValue[aliases.size()];
        this.frozen = false;
    }

    public CmisResultRow setValue(int column, Object value) {
        checkNotFrozen();
        checkValidColumnIndex(column);
        columns[column - 1] = CmisResultValue.createResultValue(value);
        return this;
    }

    public List<CmisResultValue> getColumns() {
        return Collections.unmodifiableList(Arrays.asList(columns));
    }

    public CmisResultValue getColumn(int column) {
        checkValidColumnIndex(column);
        final CmisResultValue value = columns[column - 1];
        return value != null ? value : CmisResultValue.createResultValue(null);
    }

    public CmisResultValue getColumn(String alias) {
        return getColumn(getColumnIndex(alias));
    }

    public Iterator<CmisResultValue> iterator() {
        return new ColumnIterator();
    }

    public List<String> getAliases() {
        return aliases;
    }

    public int indexOf(String alias) {
        for (int i = 0; i < aliases.size(); i++) {
            if (aliases.get(i).equalsIgnoreCase(alias)) {
                return i + 1;
            }
        }
        return -1;
    }

    int getColumnIndex(String alias) {
        final int index = indexOf(alias);
        if (index != -1) {
            return index;
        }
        throw new FxInvalidParameterException("column", LOG, "ex.cmis.search.resultset.row.alias",
                alias, aliases).asRuntimeException();
    }

    @Override
    public String toString() {
        return Arrays.asList(columns).toString();
    }

    private void checkNotFrozen() {
        if (frozen) {
            throw new FxInvalidStateException(LOG, "ex.cmis.search.resultset.row.frozen").asRuntimeException();
        }
    }

    private void checkValidColumnIndex(int column) {
        if (column < 1 || column > columns.length) {
            throw new FxInvalidParameterException("column", LOG, "ex.cmis.search.resultset.row.columnIndex",
                    column, 1, columns.length).asRuntimeException();
        }
    }

    /**
     * Freeze the result row and prevent further modifications (i.e. setting column values).
     *
     * @return this
     */
    CmisResultRow freeze() {
        frozen = true;
        return this;
    }

    /**
     * Iterator for the columns of this result row.
     */
    private class ColumnIterator implements Iterator<CmisResultValue> {
        private int columnIndex = 0;

        public boolean hasNext() {
            return columnIndex < columns.length;
        }

        public CmisResultValue next() {
            return columns[columnIndex++];
        }

        public void remove() {
            throw new UnsupportedOperationException("Removal of result column values is not supported");
        }
    }
}
