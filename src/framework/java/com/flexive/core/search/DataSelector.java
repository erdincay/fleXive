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
package com.flexive.core.search;

import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.value.BinaryDescriptor;

import java.sql.Connection;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Interface for DB specific DataSelectors
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class DataSelector {
    private static final transient Log LOG = LogFactory.getLog(DataSelector.class);

    /**
     * The delimiter for the encoded binary column returned by the selector.
     */
    public static final String BINARY_DELIM = "||";

    /**
     * The columns to be selected by the binary selector. The result will be returned in a single
     * string column, the entries delimited by {@link #BINARY_DELIM}.
     */
    protected static final List<String> BINARY_COLUMNS = Collections.unmodifiableList(
            Arrays.asList(
                    "ID", "NAME", "BLOBSIZE", "XMLMETA", "CREATED_AT", "MIMETYPE",
                    "ISIMAGE", "RESOLUTION", "WIDTH", "HEIGHT"
            )
    );

    /**
     * All result columns that are selected for search-internal queries and are not
     * returned to the user. Note that this is mostly an informal listing and used to determine
     * the number of internal columns, but that changing the order will probably not work as intended
     * since the generated SQL requires that these internal properties are actually selected.
     */
    protected static final List<String> INTERNAL_RESULTCOLS = Collections.unmodifiableList(
            Arrays.asList("rownr", "id", "ver", "created_by")
    );

    protected static final int COL_ROWNR = 1;
    protected static final int COL_ID = 2;
    protected static final int COL_VER = 3;
    protected static final int COL_CREATED_BY = 4;

    private static final Map<String, Integer> BINARY_COLUMN_INDICES;

    static {
        final Map<String, Integer> indices = new HashMap<String, Integer>(BINARY_COLUMNS.size());
        // create a lookup cache for binary columns
        for (int i = 0; i < BINARY_COLUMNS.size(); i++) {
            indices.put(BINARY_COLUMNS.get(i), i);
        }
        BINARY_COLUMN_INDICES = Collections.unmodifiableMap(indices);
    }

    public abstract String build(final Connection con) throws FxSqlSearchException;

    public abstract void cleanup(Connection con) throws FxSqlSearchException;

    public abstract Map<String, FieldSelector> getSelectors();

    /**
     * Returns the index of the given column in a encoded binary result value.
     *
     * @param columnName the column name (uppercase)
     * @return the index of the given column name
     */
    static int getBinaryIndex(String columnName) {
        final Integer value = BINARY_COLUMN_INDICES.get(columnName);
        return value != null ? value : -1;
    }

    static String getBinaryValue(String[] values, String columnName) {
        final int index = getBinaryIndex(columnName);
        if (index == -1) {
            throw new FxInvalidParameterException("COLUMNNAME", LOG,
                    "ex.sqlSearch.selector.binary.column", columnName).asRuntimeException();
        }
        return values[index];
    }
}
