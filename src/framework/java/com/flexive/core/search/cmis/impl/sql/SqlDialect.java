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
package com.flexive.core.search.cmis.impl.sql;

import com.flexive.core.search.PropertyResolver;
import com.flexive.core.search.cmis.impl.ResultColumn;
import com.flexive.core.search.cmis.model.ColumnReference;
import com.flexive.core.search.cmis.model.TableReference;
import com.flexive.shared.cmis.search.CmisResultSet;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * A DBMS-specific implementation of the CMIS SQL --> ANSI SQL mapping for the flexive repository.
 *
 * <p>
 * The SqlDialect instance is instantiated with a {@link com.flexive.core.search.cmis.impl.CmisSqlQuery} that
 * contains the parsed CMIS-SQL query.
 * </p>
 * <p>
 * {@link #getSql()} returns the SQL representation of the CMIS SQL query for the corresponding database
 * vendor. The results from this query can then be processed with
 * {@link #processResultSet(java.sql.ResultSet)}.
 * </p>
 *
 * <p>
 * Additional methods provide information about the capabilities of this SQL dialect that depend on
 * the underlying database implementation (e.g. fulltext search features), as well as basic
 * SQL generators for common statements (e.g. filtering a table by content type).
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public interface SqlDialect {

    /**
     * Return the SQL representation of the query. The JDBC result set of this query should be processed with
     * {@link #processResultSet(java.sql.ResultSet)} of the same dialect instance.
     *
     * @return  the SQL representation of the query.
     */
    String getSql();

    /**
     * Add DBMS-specific initialization statements for the query, if necessary.
     *
     * @param con   the connection which will be used for executing the query returned by {@link #getSql()}
     * @throws java.sql.SQLException    on database errors
     */
    void prepareConnection(Connection con) throws SQLException;

    /**
     * Process the result set of the query generated by {@link #getSql()}. The JDBC result set is
     * consumed entirely, the returned {@link CmisResultSet} instance is completely disconnected
     * from the database result.
     *
     * @param rs    the JDBC result set
     * @return      the CMIS result set
     * @throws SQLException     if a DB error occured reading the result set
     */
    CmisResultSet processResultSet(ResultSet rs) throws SQLException;

    /**
     * Return the capabilities of the SQL dialect, which indicates the availability mostly of features that
     * are not covered by ANSI SQL, like full-text queries and scoring.
     *
     * @return  the capabilities of the SQL dialect.
     */
    Capabilities getCapabilities();

    /**
     * Return a select from the main content table with the given alias, for example:
     * {@code FX_CONTENT main}.
     *
     * @param alias the desired table alias
     * @return  a select from the content table with the given alias
     */
    String fromContentTable(String alias);

    /**
     * Filters the given column against the given types.
     *
     * @param column the type definition column
     * @param types the types to be filtered
     * @return SQL condition to match the column against all types used in the query
     */
    String getTypeFilter(String column, Collection<FxType> types);

    /**
     * Return the security filter for the calling user and the given table alias.
     *
     * @param table         the table reference that should be used for security checks
     * @param tableAlias    the table alias
     * @param contentTable  if {@code tableAlias} refers to the FX_CONTENT table. If true, the security check
     * can be performed more efficiently because the instance metadata (like owner or ACL) is already available
     * in the selected table.
     * @return  the security filter for the calling user and the given table alias
     */
    String getSecurityFilter(TableReference table, String tableAlias, boolean contentTable);

    /**
     * Filters the given column against the given list of assignments.
     *
     * @param tableAlias        the table alias
     * @param constrainResult   if the result <strong>must</strong> be constrained to the given types. For example,
     * when reading the value of a FX_CONTENT column, this flag does not have to be set (because the condition
     * filter already removed instances), but when creating a condition on a FX_CONTENT column, this
     * MUST be set - otherwise instances of foreign types are returned.
     * @param assignments       the assignments which should be filtered  @return                  a condition that constrains the given column to the assignments
     */
    String getAssignmentFilter(PropertyResolver.Table tableType, String tableAlias, boolean constrainResult, Collection<? extends FxPropertyAssignment> assignments);

    /**
     * Return the version filter for the given table alias.
     *
     * @param tableAlias    the table alias to be used (may be null).
     * @return  the version filter for the given table alias.
     */
    String getVersionFilter(String tableAlias);

    /**
     * Return the {@link com.flexive.core.search.cmis.impl.sql.SqlMapperFactory} associated with this dialect.
     *
     * @return  the {@link com.flexive.core.search.cmis.impl.sql.SqlMapperFactory} associated with this dialect.
     */
    SqlMapperFactory getSqlMapperFactory();

    boolean isReturnPrimitives();

    boolean isPropertyPermissionsEnabled(ResultColumn<? extends ColumnReference> column);

    boolean isAllowedByPropertyPermissions(ResultSet rs, ResultColumn<? extends ColumnReference> column, String xpath) throws SQLException;

    FxEnvironment getEnvironment();

    /**
     * Return an expression to limit the number of rows of a subquery (i.e. a condition), for example
     * "LIMIT 10000" (for MySQL).
     *
     * @return  an expression to limit the number of rows of a subquery (i.e. a condition).
     */
    String limitSubquery();

    /**
     * Returns the result column that selects the type ID for the given table. 
     *
     * @param tableReference    the query table
     * @return                  the result column that selects the type ID for the given table  
     */
    ResultColumn getTypeIdColumn(TableReference tableReference);

    /**
     * Return the SQL representation of an empty ID (e.g. "null").
     *
     * @return  the SQL representation of an empty ID (e.g. "null").
     */
    String getEmptyId();


    /**
     * Return the SQL representation of an empty version (e.g. "null").
     *
     * @return  the SQL representation of an empty version (e.g. "null").
     */
    String getEmptyVersion();
}
