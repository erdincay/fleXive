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

import java.util.List;
import java.util.Map;

/**
 * FxSQL result set
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface FxResultSet {

    /**
     * Returns the time this resultset was created at.
     * 
     * @return the creation time of the resultset
     */
    public long getCreationTime();

    /**
     * Returns the start index specified with the search.
     *
     * @return the start index specified with the search
     */
    public int getStartIndex();


    /**
     * Returns the maximum rows parameter specified with the search.
     *
     * @return the maximum rows parameter specified with the search
     */
    public int getMaxFetchRows();


    /**
     * Returns the column names.
     *
     * @return the column names.
     */
    public String[] getColumnNames();

    /**
     * Returns the column label of the given index in the calling user's language.
     *
     * @param index the column index (first column is 1)
     * @return  the column label of the given index in the calling user's language.
     * @throws ArrayIndexOutOfBoundsException if the given position is not valid
     */
    public String getColumnLabel(int index) throws ArrayIndexOutOfBoundsException;

    /**
     * Returns the column labels in the calling user's language.
     *
     * @return  the column labels in the calling user's language.
     */
    public String[] getColumnLabels();

    /**
     * Returns all rows.
     *
     * @return the rows
     */
    public List<Object[]> getRows();

    /**
     * Get the designated column's name.
     *
     * @return the column name at the given position
     * @param pos the first column is 1, the second is 2, ...
     * @throws ArrayIndexOutOfBoundsException if the given position is not valid
     */
    public String getColumnName(int pos) throws ArrayIndexOutOfBoundsException;

    /**
     * Get the index for the given column. Returns -1 if the column name
     * does not exist in the result set.
     *
     * @param name  the column name
     * @return  the index for the given column name, 1-based
     */
    public int getColumnIndex(String name);

    /**
     * Returns a map returning the column index using the lowercase column name as a key.
     *
     * @return  a map returning the column index using the lowercase column name as a key.
     */
    public Map<String, Integer> getColumnIndexMap();

    /**
     * Returns the number of columns in this FxResultSet object.
     *
     * @return the number of columns
     */
    public int getColumnCount();

    /**
     * Returns the number of rows in this FxResultSet object.
     *
     * @return the number of rows
     */
    public int getRowCount();


    /**
     * Returns the total row count.
     * <p />
     * The total row count is the number of entries found by the search, even if they are not contained
     * in this resultset becaus of the specified start and end index.
     *
     * @return the total row count
     */
    public int getTotalRowCount() ;

    /**
     * Returns true if the found entry set was truncted.
     *
     * @return true if the result was truncated
     */
    public boolean isTruncated() ;

    /**
     * Retrieves the value of the designated column in the current row of this FxResultSet object as a
     * Object.
     *
     * @param rowIndex - the first row is 1, the second is 2, ...
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the value returned is null
     * @throws ArrayIndexOutOfBoundsException if the given row/column combination is out of range
     */
    public Object getObject(int rowIndex,int columnIndex) throws ArrayIndexOutOfBoundsException;

    /**
     * Retrieves the value of the designated column in the current row of this FxResultSet object as a
     * String.
     *
     * @param rowIndex - the first row is 1, the second is 2, ...
     * @param columnIndex - the first column is 1, the second is 2, ...
     * @return the column value; if the value is SQL NULL, the value returned is null
     * @throws ArrayIndexOutOfBoundsException if the given row/column combination is out of range
     */
    public String getString(int rowIndex,int columnIndex) throws ArrayIndexOutOfBoundsException;

    /**
     * Returns the time that was spent parsing the statement.
     *
     * @return the time that was spent parsing the statement
     */
    public int getParserExecutionTime();

    /**
     * Returns the time needed to find all matching records in the database.
     *
     * @return the time needed to find all matching records in the database
     */
    public int getDbSearchTime();

    /**
     * Returns the time needed to find fetch the matching records from the database.
     *
     * @return the time needed to find fetch the matching records from the database
     */
    public int getFetchTime();

    /**
     * Returns the total time spent for the search.
     * <p />
     * This time includes the parse time, search time, fetch time and additional
     * programm logic time.
     *
     * @return the total time spent for the search
     */
    public int getTotalTime();

    /**
     * Returns the location for which the query was executed.
     *
     * @return  the location for which the query was executed.
     */
    public ResultLocation getLocation();


    /**
     * Returns the view type for which the query was executed.
     *
     * @return  the view type for which the query was executed.
     */
    public ResultViewType getViewType();


    /**
     * Returns a list of all content types that are part of the resultset.
     * <p />
     * Items filtered by the parameter CTYPE are also taken into account.
     *
     * @return a list of all content types that are part of the resultset
     */
    public List<FxFoundType> getContentTypes();

    /**
     * Return a result row iterator for this result set.
     *
     * @return  a result row iterator for this result set.
     */
    public Iterable<FxResultRow> getResultRows();

    /**
     * Return a result row wrapper for the given row number.
     *
     * @param index the row number
     * @return  a result row wrapper for the given row number.
     */
    public FxResultRow getResultRow(int index);

    /**
     * If the query created a new briefcase, its ID can be retrieved with this method.
     *
     * @return  the ID of the created briefcase, or -1 if no briefcase was created
     */
    public long getCreatedBriefcaseId();

    /**
     * Projects a single column to a list.
     *
     * @param columnIndex   the 1-based column index
     * @return  all column values collected in a list
     */
    <T> List<T> collectColumn(int columnIndex);

    /**
     * Return the start column index of the properties selected by the user wildcard @*, or -1
     * if no such wildcard was present in the original statement.
     *
     * @return the start column index 1-based of the properties selected by the user wildcard @*, or -1
     * if no such wildcard was present in the original statement.
     */
    int getUserWildcardIndex();
}
