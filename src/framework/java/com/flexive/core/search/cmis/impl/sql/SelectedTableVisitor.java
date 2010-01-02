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
package com.flexive.core.search.cmis.impl.sql;

import com.flexive.core.search.cmis.model.TableReference;
import com.flexive.core.search.cmis.model.TableReferenceVisitor;

import java.util.List;
import java.util.Map;

/**
 * Visitor to build the SELECT, FROM and WHERE clause to select all tables for a CMIS table or
 * CMIS join. The visitor accumulates the SQL fragments in internal lists and can be executed on several
 * tables.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public interface SelectedTableVisitor extends TableReferenceVisitor {
    /**
     * Returns the comma-separated list of columns that selects the object IDs and versions for all
     * tables of the query.
     *
     * @return  the comma-separated list of columns that selects the object IDs and versions for all
     * tables of the query.
     */
    String getSelect();

    /**
     * Returns the comma-separated list of columns that selects the object IDs and versions for a single
     * table, and selects only "null" values for all other tables.
     *
     * @param selectOnlyFrom    the table reference which will be used in the (sub-)query
     * @return  the comma-separated list of columns that selects the object IDs and versions for a single table
     */
    String getSelectForSingleTable(TableReference selectOnlyFrom);

    /**
     * Returns the core "FROM" tables to be used for filtering and combining conditions.
     *
     * @return  the core "FROM" tables to be used for filtering and combining conditions.
     */
    String getFrom();

    /**
     * Returns the conditions that were accumulated for the tables of the query, i.e. join conditions,
     * security and version filters.
     *
     * @return  the conditions that were accumulated for the tables of the query
     */
    List<String> getConditions();

    /**
     * Create an outer JOIN between the given table and all tables returned by {@link #getFrom()}.
     *
     * @param tableAlias    the target table alias
     * @param subtables     the subtable visitor of the target table (defines the ID and version filter columns
     * to be used for joining)
     *
     * @return              an outer JOIN between the given table and all tables returned by {@link #getFrom()}.
     */
    String outerJoin(String tableAlias, SelectedTableVisitor subtables);

    /**
     * Return the table alias prefix that was specified at visitor creation time (all table aliases
     * for subtables are prefixed with this value).
     *
     * @return  the table alias prefix that was specified at visitor creation time
     */
    String getTableAliasPrefix();

    /**
     * Return the alias for the given table.
     *
     * @param table the table (must have been visited by this visitor)
     * @return  the alias for the given table.
     */
    String getTableAlias(TableReference table);

    /**
     * Return all aliases mapped to their table references.
     *
     * @return the aliases mapped to their table references.
     */
    Map<TableReference, String> getTableAliases();

    /**
     * Returns true when the visited table structure contains at least one JOIN.
     *
     * @return  true when the visited table structure contains at least one JOIN.
     */
    boolean isUsesJoins();
}
