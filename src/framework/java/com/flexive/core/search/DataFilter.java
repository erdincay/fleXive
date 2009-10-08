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
import com.flexive.shared.search.FxFoundType;
import com.flexive.sqlParser.FxStatement;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Data filter for the FxSQL search engine
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public abstract class DataFilter {
    protected final SqlSearch search;
    private final Connection con;

    /**
     * Ctor
     *
     * @param con    an open and valid connection
     * @param search the SqlSearch object operated on
     */
    public DataFilter(Connection con, SqlSearch search) {
        this.search = search;
        this.con = con;
    }

    /**
     * Get the underlying FxStatement
     *
     * @return FxStatement
     */
    protected FxStatement getStatement() {
        return search.getFxStatement();
    }

    /**
     * Get the used property resolver
     *
     * @return property resolver
     */
    protected PropertyResolver getPropertyResolver() {
        return search.getPropertyResolver();
    }

    /**
     * Get the current database connection
     *
     * @return database connection
     */
    protected Connection getConnection() {
        return con;
    }

    /**
     * Build the filter
     *
     * @throws FxSqlSearchException on errors
     */
    public abstract void build() throws FxSqlSearchException, SQLException;


    /**
     * Perform cleanup code
     *
     * @throws FxSqlSearchException on errors
     */
    public abstract void cleanup() throws FxSqlSearchException;

    /**
     * Returns the number of found entries.
     *
     * @return the number of found entries
     */
    public abstract int getFoundEntries();

    /**
     * Returns true if the result was truncated by the specified max_result_rows parameter.
     *
     * @return true if the result was truncated
     */
    public abstract boolean isTruncated();

    /**
     * Returns a list of all content types that are part of the resultset.
     *
     * @return a list of all content types that are part of the resultset
     */
    public abstract List<FxFoundType> getContentTypes();

    /**
     * Does the database support query timeouts?
     *
     * @return database supports query timeouts
     */
    public abstract boolean isQueryTimeoutSupported();

    /**
     * Set a sql variable
     *
     * @param con      an open and valid connection
     * @param variable name of the variable
     * @param value    value to set
     * @throws SQLException on errors
     */
    public abstract void setVariable(Connection con, String variable, String value) throws SQLException;
}
