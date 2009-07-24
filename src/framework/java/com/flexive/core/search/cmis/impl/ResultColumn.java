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

import com.flexive.core.search.cmis.impl.sql.ColumnIndex;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Representation of a result column in the SQL query. This encompasses a superset of all columns from CMIS
 * columns (i.e. implementations of {@link com.flexive.core.search.cmis.model.Selectable}), because the
 * SQL mappers can define arbitrary columns selectors to retrieve additional information (e.g. row numbers).
 *
 * <p>
 * Implementations are responsible for emitting the SQL required for selecting the column in the SQL query
 * through the use of a {@link com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper}, which will be provided
 * by a {@link com.flexive.core.search.cmis.impl.sql.SqlMapperFactory} implementation.
 * </p>
 *
 * <p>
 * An implementation also has to implement a method to <em>decode</em> the selected values when they
 * are returned from the database.
 * </p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 *
 * @param <V> the type of the wrapped value (usually needed by the
 *            {@link com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper}
 *            implementation to retrieve the column's metadata (e.g. selected property)
 */
public interface ResultColumn<V> {

    V getSelectedObject();

    String getAlias();

    String getResultSetAlias();

    String selectSql(SqlMapperFactory factory, CmisSqlQuery query, long languageId, boolean xpath, boolean includeResultAlias, ColumnIndex index);

    Object decodeResultValue(SqlMapperFactory factory, ResultSet rs, long languageId) throws SQLException;

    int getColumnStart();

    int getColumnEnd();

    boolean isScore();
}
