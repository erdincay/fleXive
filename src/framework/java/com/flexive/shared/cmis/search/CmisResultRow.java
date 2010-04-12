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
package com.flexive.shared.cmis.search;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxInvalidStateException;
import com.google.common.collect.Lists;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <strong>Disclaimer: this API is part of the CMIS interface and is not yet considered stable.</strong><br/><br/>
 *
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
    private final List<CmisResultColumnDefinition> columnDefinitions;
    private boolean frozen;

    /**
     * Create a new result row. To add a new row to a result set, create the row with
     * {@link CmisResultSet#newRow()}.
     *
     * @param columnDefinitions the column definitions (fixes column count)
     */
    CmisResultRow(List<CmisResultColumnDefinition> columnDefinitions) {
        this.columnDefinitions = columnDefinitions;
        this.columns = new CmisResultValue[columnDefinitions.size()];
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

    public List<CmisResultColumnDefinition> getColumnDefinitions() {
        return columnDefinitions;
    }

    public int indexOf(String alias) {
        for (int i = 0; i < columnDefinitions.size(); i++) {
            if (columnDefinitions.get(i).getAlias().equalsIgnoreCase(alias)) {
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
                alias, getAliases()).asRuntimeException();
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

    private List<String> getAliases() {
        return Lists.transform(
                columnDefinitions, CmisResultColumnDefinition.TRANFORM_ALIAS
        );
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
