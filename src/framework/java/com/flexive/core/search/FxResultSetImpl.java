/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.core.search;

import com.flexive.shared.FxArrayUtils;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.value.FxString;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.search.*;
import com.flexive.sqlParser.FxStatement;
import com.flexive.sqlParser.SelectedValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * FxResultSet implementation
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxResultSetImpl implements Serializable, FxResultSet {
    private static final long serialVersionUID = -7038323618490882790L;

    private final List<Object[]> rows;
    private final String[] columnNames;
    private final int parserExecutionTime;
    private final int dbSearchTime;
    private final int startIndex;
    private final int maxFetchRows;
    private final ResultLocation location;
    private final ResultViewType viewType;
    private final List<FxFoundType> contentTypes;
    private final long creationTime;
    private final long createdBriefcaseId;
    private final long typeId;

    private int fetchTime = 0;
    private int totalTime = 0;
    private int totalRowCount;
    private boolean truncated;
    private Map<String, Integer> columnIndexMap;

    // cached properties
    private transient String[] columnLabels;

    private class RowIterator implements Iterator<FxResultRow> {
        int index = 0;

        public boolean hasNext() {
            return index < rows.size();
        }

        public FxResultRow next() {
            return new FxResultRow(FxResultSetImpl.this, index++);
        }

        public void remove() {
            throw new UnsupportedOperationException("Removing rows not supported");
        }
    }

    protected FxResultSetImpl(ResultLocation location, ResultViewType viewType) {
        this.rows = new ArrayList<Object[]>(0);
        this.columnNames = new String[0];
        this.contentTypes = new ArrayList<FxFoundType>(0);
        this.parserExecutionTime = 0;
        this.dbSearchTime = 0;
        this.totalRowCount = 0;
        this.truncated = false;
        this.startIndex = 0;
        this.maxFetchRows = 0;
        this.location = location;
        this.viewType = viewType;
        this.creationTime = System.currentTimeMillis();
        this.typeId = -1;
        this.createdBriefcaseId = -1;
    }

    protected FxResultSetImpl(final FxStatement fx_stmt, final int parserExecutionTime, int dbSearchTime,
                              int startIndex, int maxFetchRows, ResultLocation location, ResultViewType viewType,
                              List<FxFoundType> types, long typeId, long createdBriefcaseId) {
        this.parserExecutionTime = parserExecutionTime;
        this.startIndex = startIndex;
        this.maxFetchRows = maxFetchRows;
        this.dbSearchTime = dbSearchTime;
        this.location = location;
        this.viewType = viewType;
        this.contentTypes = types == null ? new ArrayList<FxFoundType>(0) : types;
        this.createdBriefcaseId = createdBriefcaseId;
        this.rows = new ArrayList<Object[]>(fx_stmt.getMaxResultRows());
        this.columnNames = new String[fx_stmt.getSelectedValues().size()];
        int pos = 0;
        for (SelectedValue v : fx_stmt.getSelectedValues()) {
            this.columnNames[pos++] = v.getAlias();
        }
        this.totalTime = parserExecutionTime;
        this.creationTime = System.currentTimeMillis();
        this.typeId = typeId;
    }

    /**
     * {@inheritDoc} *
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * {@inheritDoc} *
     */
    public int getStartIndex() {
        return startIndex;
    }


    /**
     * {@inheritDoc} *
     */
    public int getMaxFetchRows() {
        return maxFetchRows;
    }

    protected void setFetchTime(int fetchTime) {
        this.fetchTime = fetchTime;
    }

    protected void addRow(Object[] rowData) {
        rows.add(rowData);
    }

    protected void setTotalTime(int executeQueryTime) {
        this.totalTime = executeQueryTime + parserExecutionTime;
    }

    /**
     * {@inheritDoc} *
     */
    public String[] getColumnNames() {
        return FxArrayUtils.clone(this.columnNames);
    }

    /**
     * {@inheritDoc} *
     */
    public int getColumnIndex(String name) {
        return FxSharedUtils.getColumnIndex(columnNames, name);
    }

    /**
     * {@inheritDoc} *
     */
    public String getColumnLabel(int index) throws ArrayIndexOutOfBoundsException {
        return getColumnLabels()[index - 1];
    }

    /**
     * {@inheritDoc} *
     */
    public String[] getColumnLabels() {
        if (columnLabels == null) {
            columnLabels = new String[columnNames.length];
            for (int i = 0; i < columnNames.length; i++) {
                String name = columnNames[i];
                if (name.indexOf('(') > 0) {
                    // strip function calls
                    name = name.substring(name.lastIndexOf('('), name.indexOf(')'));
                }
                if (name.indexOf('.') > 0) {
                    // strip leading prefix
                    name = name.substring(name.indexOf('.') + 1);
                }
                if (name.indexOf('@') != -1) {
                    columnLabels[i] = name;     // don't translate virtual properties
                } else {
                    FxString label;
                    try {
                        label = CacheAdmin.getEnvironment().getAssignment(name).getLabel();
                    } catch (FxRuntimeException e) {
                        // assignment not found, try property
                        label = CacheAdmin.getEnvironment().getProperty(name).getLabel();
                    }
                    columnLabels[i] = label.getBestTranslation();
                }
            }
        }
        return columnLabels.clone();
    }

    /**
     * {@inheritDoc} *
     */
    public Map<String, Integer> getColumnIndexMap() {
        if (columnIndexMap == null) {
            columnIndexMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<String, Integer>() {
                private static final long serialVersionUID = 5832530872300639732L;

                public Integer get(Object key) {
                    return getColumnIndex(String.valueOf(key));
                }
            });
        }
        return columnIndexMap;
    }

    /**
     * {@inheritDoc} *
     */
    public List<Object[]> getRows() {
        return rows == null ? new ArrayList<Object[]>(0) : rows;
    }

    /**
     * {@inheritDoc} *
     */
    public String getColumnName(int pos) throws ArrayIndexOutOfBoundsException {
        try {
            return this.columnNames[pos - 1];
        } catch (Exception exc) {
            throw new ArrayIndexOutOfBoundsException("size: " + columnNames.length + ";pos:" + pos);
        }
    }

    /**
     * {@inheritDoc} *
     */
    public int getColumnCount() {
        return this.columnNames.length;
    }

    /**
     * {@inheritDoc} *
     */
    public int getRowCount() {
        return rows.size();
    }

    /**
     * {@inheritDoc} *
     */
    public int getTotalRowCount() {
        // no type filter specified, return global total row count
        if (typeId == -1) {
            return totalRowCount;
        }
        // type filter specified, find total rows for the current type
        for (FxFoundType contentType : contentTypes) {
            if (contentType.getContentTypeId() == typeId) {
                return contentType.getFoundEntries();
            }
        }
        // type filter specified, but no content found
        return 0;
    }

    /**
     * {@inheritDoc} *
     */
    public boolean isTruncated() {
        return truncated;
    }

    /**
     * {@inheritDoc} *
     */
    public Object getObject(int rowIndex, int columnIndex) throws ArrayIndexOutOfBoundsException {

        // Access row data
        Object rowData[];
        try {
            rowData = this.rows.get(rowIndex - 1);
        } catch (Exception exc) {
            throw new ArrayIndexOutOfBoundsException("size: " + columnNames.length + ";rowIndex:" + rowIndex);
        }

        // Access column data
        try {
            return rowData[columnIndex - 1];
        } catch (Exception exc) {
            throw new ArrayIndexOutOfBoundsException("size: " + columnNames.length + ";columnIndex:" + columnIndex);
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"unchecked"})
    public <T> List<T> collectColumn(int columnIndex) {
        final List<T> result = new ArrayList<T>(rows.size());
        if (rows.size() == 0) {
            return result;
        }
        checkColumnIndex(columnIndex);
        for (Object[] row : rows) {
            result.add((T) row[columnIndex - 1]);
        }
        return result;
    }

    /**
     * {@inheritDoc} *
     */
    public String getString(int rowIndex, int columnIndex) throws ArrayIndexOutOfBoundsException {
        Object value = getObject(rowIndex, columnIndex);
        if (value instanceof String) {
            return (String) value;
        } else {
            return value.toString();
        }
    }

    /**
     * {@inheritDoc} *
     */
    public int getParserExecutionTime() {
        return parserExecutionTime;
    }

    /**
     * Returns the time needed to find all matching records in the database.
     *
     * @return the time needed to find all matching records in the database
     */
    public int getDbSearchTime() {
        return dbSearchTime;
    }

    /**
     * Returns the time needed to find fetch the matching records from the database.
     *
     * @return the time needed to find fetch the matching records from the database
     */
    public int getFetchTime() {
        return fetchTime;
    }

    /**
     * Returns the total time spent for the search.
     * <p/>
     * This time includes the parse time, search time, fetch time and additional
     * programm logic time.
     *
     * @return the total time spent for the search
     */
    public int getTotalTime() {
        return totalTime;
    }

    /**
     * {@inheritDoc}
     */
    public ResultLocation getLocation() {
        return location;
    }

    /**
     * {@inheritDoc}
     */
    public ResultViewType getViewType() {
        return viewType;
    }

    /**
     * {@inheritDoc}
     */
    public List<FxFoundType> getContentTypes() {
        return contentTypes;
    }

    /**
     * {@inheritDoc}
     */
    public Iterable<FxResultRow> getResultRows() {
        return new Iterable<FxResultRow>() {
            public Iterator<FxResultRow> iterator() {
                return new RowIterator();
            }
        };
    }

    /**
     * {@inheritDoc}
     */
    public FxResultRow getResultRow(int index) {
        return new FxResultRow(this, index);
    }

    /**
     * {@inheritDoc}
     */
    public long getCreatedBriefcaseId() {
        return createdBriefcaseId;
    }

    protected void setTruncated(boolean truncated) {
        this.truncated = truncated;
    }

    protected void setTotalRowCount(int totalRowCount) {
        this.totalRowCount = totalRowCount;
    }

    /**
     * Checks the 1-based column index and throws a runtime exception when it is not valid in this result set.
     *
     * @param columnIndex   the 1-based column index
     */
    private void checkColumnIndex(int columnIndex) {
        if (rows.size() == 0) {
            return;
        }
        if (rows.get(0).length < columnIndex || columnIndex < 1) {
            throw new FxInvalidParameterException("columnIndex", "ex.sqlSearch.column.index", columnIndex, rows.get(0).length).asRuntimeException();
        }
    }

}
