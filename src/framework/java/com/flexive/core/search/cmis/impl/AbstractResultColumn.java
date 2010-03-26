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
package com.flexive.core.search.cmis.impl;

import com.flexive.core.search.cmis.impl.sql.ColumnIndex;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Base class for result column wrappers. The type is self-referential to allow a typesafe generic
 * implementation of the {@link com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper} delegate methods.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public abstract class AbstractResultColumn<V, T extends AbstractResultColumn<V, T>> implements ResultColumn<V> {
    private int columnStart = -1;
    private int columnEnd = -1;

    protected abstract T getThis();

    protected abstract ResultColumnMapper<T> getSqlMapper(SqlMapperFactory factory);

    /** {@inheritDoc} */
    public String selectSql(SqlMapperFactory factory, CmisSqlQuery query, long languageId, boolean xpath, boolean includeResultAlias, ColumnIndex index) {
        // remember positions in JDBC result set
        columnStart = index.getNextIndex();

        // call SQL mapper to generate the SQL and advance the result column index
        final String sql = getSqlMapper(factory).selectColumn(factory, query, getThis(), languageId, xpath, includeResultAlias, index);

        // remember last column for this statement (mapper can select more than one column)
        columnEnd = index.getNextIndex() - 1;

        return sql;
    }

    /** {@inheritDoc} */
    public Object decodeResultValue(SqlMapperFactory factory, ResultSet rs, long languageId) throws SQLException {
        return getSqlMapper(factory).decodeResultValue(factory, rs, getThis(), languageId);
    }

    /** {@inheritDoc} */
    public int getColumnStart() {
        return columnStart;
    }

    /** {@inheritDoc} */
    public int getColumnEnd() {
        return columnEnd;
    }

    /** {@inheritDoc} */
    public String getResultSetAlias() {
        assert columnStart != -1 : "Start column not set, result set alias not available.";
        return "c" + columnStart + "_" + getEscapedAlias();
    }

    /**
     * @return  the escaped alias for SQL queries, or null if no alias was defined.
     */
    protected String getEscapedAlias() {
        // for a list of escaped chars see IDENT in CmisSql.g
        return getAlias() == null ? null : getAlias().replace(':', '_');
    }


    /** {@inheritDoc} */
    public boolean isScore() {
        return false;
    }
}
