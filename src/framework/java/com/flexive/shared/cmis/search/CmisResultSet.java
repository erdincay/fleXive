/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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

import com.flexive.shared.TimestampRecorder;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.value.FxValue;
import com.google.common.collect.Lists;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * <strong>Disclaimer: this API is part of the CMIS interface and is not yet considered stable.</strong><br/><br/>
 *
 * The search result of a CMIS SQL query.
 * <p>
 * You can iterate over the rows by using the result object, or explicitly using the
 * {@link #getRows()} function:
 * </p>
 * <pre>
 * final CmisResultSet result = EJBLookup.getCmisSearchEngine().search("...");
 * for (CmisResultRow row : result) {
 *   for (CmisResultColumn column : row) {
 *      System.out.println(column.getValue());
 *   }
 * }</pre>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class CmisResultSet implements Serializable, Iterable<CmisResultRow> {
    // TODO: equals/hashCode

    private static final long serialVersionUID = -5423097964253577081L;

    private final List<CmisResultRow> rows;
    private final List<CmisResultColumnDefinition> columns;
    private final boolean frozen;
    private TimestampRecorder timestampRecorder;

    public CmisResultSet(List<CmisResultColumnDefinition> columns) {
        this.rows = new ArrayList<CmisResultRow>();
        this.columns = Collections.unmodifiableList(columns);
        this.frozen = false;
    }

    /**
     * Constructor used for creating empty result sets (used for testing only).
     *
     * @param columnCount   the number of columns
     */
    public CmisResultSet(int columnCount) {
        this.rows = new ArrayList<CmisResultRow>();
        final List<CmisResultColumnDefinition> columns = Lists.newArrayListWithCapacity(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            columns.add(new CmisResultColumnDefinition("column" + i, -1));
        }
        this.columns = Collections.unmodifiableList(columns);
        this.frozen = false;
    }

    private CmisResultSet(CmisResultSet other) {
        this.rows = Collections.unmodifiableList(other.rows);
        this.columns = other.columns;
        this.frozen = true;
    }

    /**
     * Return all rows of the result set.
     *
     * @return all rows of the result set.
     */
    public List<CmisResultRow> getRows() {
        return rows;
    }

    /**
     * Get the row with the given index (0-based).
     *
     * @param index the row index (0-based)
     * @return the row with the given index (0-based).
     */
    public CmisResultRow getRow(int index) {
        checkValidRowIndex(index);
        return rows.get(index);
    }

    /**
     * @return the column definitions.
     * @since  3.1.4
     */
    public List<CmisResultColumnDefinition> getColumns() {
        return columns;
    }

    /**
     * Return the column value at the given row and column.
     *
     * @param rowIndex    the row index (0-based)
     * @param columnIndex the column index (1-based)
     * @return the column value at the given row and column.
     */
    public CmisResultValue getColumn(int rowIndex, int columnIndex) {
        return getRow(rowIndex).getColumn(columnIndex);
    }

    /**
     * Return the column value at the given row and column.
     *
     * @param rowIndex    the row index (0-based)
     * @param columnAlias the column alias (case insensitive)
     * @return the column value at the given row and column.
     */
    public CmisResultValue getColumn(int rowIndex, String columnAlias) {
        return rows.get(rowIndex).getColumn(columnAlias);
    }

    /**
     * Create a new row for this result set. The row is initialized with the columns of this
     * result set, but is not yet added to it. To add the row, call {@link #addRow(CmisResultRow)}.
     *
     * @return a new row for this result set
     */
    public CmisResultRow newRow() {
        return new CmisResultRow(columns);
    }

    /**
     * Add the given row to this result set. The row must have been initialized with {@link #newRow()}.
     * Before adding, the row object is "frozen", i.e. it cannot be manipulated afterwards.
     *
     * @param row the row to be added
     * @return this
     */
    public CmisResultSet addRow(CmisResultRow row) {
        checkNotFrozen();
        assert row.getColumnDefinitions() == columns : "Row not created with #newRow() of this result set instance: " + this;
        rows.add(row.freeze());
        return this;
    }

    /**
     * Return the column aliases of this result set. The order corresponds to the columns of the result set.
     *
     * @return the column aliases of this result set
     */
    public List<String> getColumnAliases() {
        return Lists.transform(columns, CmisResultColumnDefinition.TRANFORM_ALIAS);
    }

    /**
     * Return the column aliases of this result set. The order corresponds to the columns of the result set.
     *
     * @return the column aliases of this result set
     */
    public List<Long> getColumnAssignmentIds() {
        return Lists.transform(columns, CmisResultColumnDefinition.TRANSFORM_ASSIGNMENT_ID);
    }

    /**
     * Return the number of columns in this result set.
     *
     * @return the number of columns in this result set.
     */
    public int getColumnCount() {
        return columns.size();
    }

    /**
     * Return the number of rows in this result set.
     *
     * @return the number of rows in this result set.
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * Filter all rows where a column's value matches the given object.
     *
     * @param columnIndex the 1-based column index
     * @param value       the value to be filtered. Note that column values of type {@link FxValue} are
     *                    unboxed automatically, so you can supply the "value type" instead of the full {@code FxValue} instance here
     *                    (i.e. {@code String} instead of {@code FxString}.
     * @return all rows where the column's value matches the given object.
     */
    public List<CmisResultRow> filterEqual(int columnIndex, Object value) {
        final List<CmisResultRow> result = new ArrayList<CmisResultRow>();
        for (CmisResultRow row : rows) {
            final CmisResultValue rowVal = row.getColumn(columnIndex);
            if ((value == null && rowVal.isEmpty())
                    || (value != null && rowVal.equals(value))) {
                result.add(row);
            }
        }
        return result;
    }

    /**
     * Projects the values of a single column to a list.
     *
     * @param columnIndex  the 1-based column index
     * @return all column values collected in a list
     */
    @SuppressWarnings({"unchecked"})
    public <T> List<T> collectColumnValues(int columnIndex) {
        return collectColumnValues(columnIndex, false);
    }

    /**
     * Projects the values of a single column to a list.
     *
     * @param columnAlias  the column alias
     * @return all column values collected in a list
     */
    @SuppressWarnings({"unchecked"})
    public <T> List<T> collectColumnValues(String columnAlias) {
        return collectColumnValues(columnAlias, false);
    }

    /**
     * Projects the values of a single column to a list.
     *
     * @param columnAlias  the column alias
     * @param unboxFxValue if FxValue-derived values should be unboxed
     * @return all column values collected in a list
     */
    @SuppressWarnings({"unchecked"})
    public <T> List<T> collectColumnValues(String columnAlias, boolean unboxFxValue) {
        if (rows.isEmpty()) {
            return new ArrayList<T>(0);
        }
        return collectColumnValues(rows.get(0).getColumnIndex(columnAlias), unboxFxValue);
    }

    /**
     * Projects the values of a single column to a list.
     *
     * @param columnIndex  the 1-based column index
     * @param unboxFxValue if FxValue-derived values should be unboxed
     * @return all column values collected in a list
     */
    @SuppressWarnings({"unchecked"})
    public <T> List<T> collectColumnValues(int columnIndex, boolean unboxFxValue) {
        final List<T> result = new ArrayList<T>(rows.size());
        for (CmisResultRow row : rows) {
            final CmisResultValue col = row.getColumn(columnIndex);
            final Object value = col.getValue();
            if (col.isAggregate()) {
                // TODO: clarify/fix handling of generic return type for multivalued properties
                if (unboxFxValue && col.getValue() instanceof FxValue) {
                    // unbox values
                    final List columnValues = new ArrayList(col.getValues().size());
                    for (Object rowValue : col.getValues()) {
                        columnValues.add(((FxValue) rowValue).getBestTranslation());
                    }
                    result.add((T) columnValues);
                } else {
                    // pass list of multivalued property values
                    result.add((T) col.getValues());
                }
            } else if (unboxFxValue && value instanceof FxValue) {
                result.add((T) ((FxValue) value).getBestTranslation());
            } else {
                result.add((T) value);
            }
        }
        return result;
    }

    private void checkNotFrozen() {
        if (frozen) {
            throw new FxInvalidStateException("ex.cmis.search.resultset.frozen").asRuntimeException();
        }
    }

    private void checkValidRowIndex(int row) {
        if (row < 0 || row >= rows.size()) {
            if (rows.isEmpty()) {
                throw new FxInvalidParameterException("row", "ex.cmis.search.resultset.row.rowIndex.empty", row)
                        .asRuntimeException();
            } else {
                throw new FxInvalidParameterException("row", "ex.cmis.search.resultset.row.rowIndex",
                        row, 0, rows.size() - 1).asRuntimeException();
            }
        }
    }

    /**
     * Freeze the result set and prevent further modifications (i.e. adding of more rows).
     *
     * @return the frozen result set
     */
    public CmisResultSet freeze() {
        if (frozen) {
            return this;
        }
        ((ArrayList) rows).trimToSize();
        return new CmisResultSet(this);
    }

    /**
     * Return the {@link com.flexive.shared.TimestampRecorder} instance used for recording the
     * execution times of this query.
     *
     * @return the {@link com.flexive.shared.TimestampRecorder} instance used for recording the
     *         execution times of this query.
     */
    public synchronized TimestampRecorder getTimestampRecorder() {
        return timestampRecorder;
    }

    /**
     * Set the {@link com.flexive.shared.TimestampRecorder} instance used for recording the
     * execution times of this query.
     *
     * @param timestampRecorder the timestamp recorder instance
     */
    public synchronized void setTimestampRecorder(TimestampRecorder timestampRecorder) {
        this.timestampRecorder = timestampRecorder;
    }

    /**
     * Return an {@link Iterator} over the rows of this resultset.
     *
     * @return an {@link Iterator} over the rows of this resultset.
     */
    public Iterator<CmisResultRow> iterator() {
        return new RowIterator();
    }

    private class RowIterator implements Iterator<CmisResultRow> {
        private int index = 0;

        public boolean hasNext() {
            return index < rows.size();
        }

        public CmisResultRow next() {
            return rows.get(index++);
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
