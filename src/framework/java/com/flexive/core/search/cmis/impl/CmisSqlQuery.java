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
package com.flexive.core.search.cmis.impl;

import com.flexive.core.search.cmis.model.*;
import static com.flexive.core.search.cmis.model.ValueFunction.Functions;
import com.flexive.core.storage.ContentStorage;
import com.flexive.shared.structure.FxEnvironment;

import java.util.*;

/**
 * Describes a CMIS {@link com.flexive.core.search.cmis.model.Statement} as an ANSI SQL statement for the
 * flexive database.
 *
 * <p>
 * This class performs the following tasks:
 * <ul>
 * <li>Store the Statement that holds the abstract model of the user query</li>
 * <li>Create a list of columns that represents the columns of the JDBC result set, including internal
 * columns like the row number</li>
 * <li>Keep query-specific parameters, such as the result language</li> 
 * </ul>
 * </p>
 *
 * <p>
 * The actual SQL query is generated by a {@link com.flexive.core.search.cmis.impl.sql.SqlDialect}, for example:
 * {@code SqlDialectFactory.getInstance(cmisSqlQuery).getSql()}
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class CmisSqlQuery {
    private final Statement statement;
    private final ContentStorage storage;
    private final long resultLanguageId;

    private final List<ResultColumn> resultColumns = new ArrayList<ResultColumn>();
    private final List<ResultColumn> userColumns = new ArrayList<ResultColumn>();
    private final Map<TableReference, ResultColumn> typeIdColumns = new HashMap<TableReference, ResultColumn>();
    private final int startRow;
    private final int maxRows;
    private final boolean returnPrimitiveValues;

    public CmisSqlQuery(FxEnvironment environment, ContentStorage storage, Statement statement, long resultLanguageId,
                        int startRow, int maxRows, boolean returnPrimitiveValues) {
        this.statement = statement;
        this.resultLanguageId = resultLanguageId;
        this.storage = storage;
        this.startRow = Math.max(0, startRow);
        this.maxRows = maxRows;
        this.returnPrimitiveValues = returnPrimitiveValues;
        
        // add internal result columns
        resultColumns.add(new ResultRowNumber("rownr"));

        addTypeColumns(environment, storage, statement);

        for (Selectable selectable : statement.getSelectedColumns()) {
            final ResultColumn column = getInstance(selectable);
            resultColumns.add(column);
            userColumns.add(column);
        }
    }

    private void addTypeColumns(final FxEnvironment environment, final ContentStorage storage, Statement statement) {
        // select type ID for tables with property permissions
        for (TableReference reference : statement.getTables()) {
            reference.accept(new TableReferenceVisitor() {
                public void visit(SingleTableReference singleTable) {
                    if (singleTable.isPropertySecurityEnabled() || !returnPrimitiveValues) {
                        // select the TYPEDEF property if property security is enable for this table
                        // or FxValue instances are returned
                        final ResultColumnReference resultColumn = new ResultColumnReference(
                                new ColumnReference(
                                        environment,
                                        storage,
                                        singleTable,
                                        "TYPEDEF",
                                        "TYPEDEF_" + singleTable.getAlias(),
                                        false)
                        );
                        resultColumns.add(resultColumn);
                        typeIdColumns.put(singleTable, resultColumn);
                    }
                }

                public void visit(JoinedTableReference joinedTable) {
                    // nop
                }
            });
        }
    }

    public ContentStorage getContentStorage() {
        return storage;
    }

    public Statement getStatement() {
        return statement;
    }

    public long getResultLanguageId() {
        return resultLanguageId;
    }

    public List<ResultColumn> getResultColumns() {
        return Collections.unmodifiableList(resultColumns);
    }

    public List<ResultColumn> getUserColumns() {
        return Collections.unmodifiableList(userColumns);
    }

    public ResultColumn getResultColumn(Selectable userColumn) {
        for (ResultColumn column : resultColumns) {
            if (column.getSelectedObject().equals(userColumn)) {
                return column;
            }
        }
        throw new IllegalArgumentException("Column not selected in this query: " + userColumn);
    }

    /**
     * Returns the result column that selected the type ID of the instance returned for the given table.
     * Note that type columns in general are only selected when they are required, i.e. when property
     * permissions are enabled for a table or when the query returns FxValue objects instead of primitives.
     *
     * @param table the table reference
     * @return  the result column that selected the type ID of the instance returned for the given table
     */
    public ResultColumn getTypeIdColumn(TableReference table) {
        return typeIdColumns.get(table);
    }

    /**
     * Return the SCORE() column indices of the query.
     *
     * @return  the SCORE() column indices of the query.
     */
    public List<Integer> getScoreColumnIndices() {
        final List<Integer> result = new ArrayList<Integer>();
        for (int i = 0; i < userColumns.size(); i++) {
            final ResultColumn column = userColumns.get(i);
            if (column.isScore()) {
                result.add(i + 1);
            }
        }
        return result;
    }

    public int getStartRow() {
        return startRow;
    }

    public int getMaxRows() {
        return maxRows;
    }

    /**
     * Returns the SelectedColumn implementation for the given {@link com.flexive.core.search.cmis.model.Selectable}
     * value (i.e. this method is a factory for any column that can be selected in a CMIS query).
     *
     * @param value the selected value from the CMIS query
     * @return      a SelectedColumn wrapper for the given value  
     */
    private ResultColumn getInstance(Selectable value) {
        if (value instanceof ColumnReference) {
            return new ResultColumnReference((ColumnReference) value);
        } else if (value instanceof ColumnValueFunction) {
            final ColumnValueFunction fun = (ColumnValueFunction) value;
            return new ResultColumnFunction(fun, new ResultColumnReference(fun.getColumnReference()));
        } else if (value instanceof NumericValueFunction && Functions.SCORE.equals(((NumericValueFunction) value).getFunction())) {
            return new ResultScore((NumericValueFunction) value);
        } else {
            throw new UnsupportedOperationException("SqlMapper for selectable instance not yet implemented: " + value.getClass());
        }
    }
}
