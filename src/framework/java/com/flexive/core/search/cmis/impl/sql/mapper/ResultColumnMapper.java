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
package com.flexive.core.search.cmis.impl.sql.mapper;

import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.ResultColumn;
import com.flexive.core.search.cmis.impl.sql.ColumnIndex;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.shared.structure.FxDataType;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Identifies a mapper for selection and decoding of a column in the result set (i.e. a column of the original CMIS SELECT clause).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public interface ResultColumnMapper<T extends ResultColumn> {

    /**
     * Return the (sub-)select for selecting the column in the result set.
     *
     * @param sqlMapperFactory  the SQL mapper
     * @param query             the query instance
     * @param column            the column to be selected
     * @param languageId        the language to be used for multilingual properties
     * @param xpath             if the XPath should be selected too
     * @param includeResultAlias  if the alias specified for the column should be added
     * @param index             column index position. Increment as you add columns to the result.
     * @return                  the (sub-)select for selecting the column in the result set.
     */
    String selectColumn(SqlMapperFactory sqlMapperFactory, CmisSqlQuery query, T column, long languageId, boolean xpath, boolean includeResultAlias, ColumnIndex index);

    /**
     * Decode the column(s) selected by
     * {@link #selectColumn(com.flexive.core.search.cmis.impl.sql.SqlMapperFactory, com.flexive.core.search.cmis.impl.CmisSqlQuery, com.flexive.core.search.cmis.impl.ResultColumn, long, boolean, boolean, com.flexive.core.search.cmis.impl.sql.ColumnIndex)}.
     * for the current row.
     *
     * @param factory       the SQL mapper
     * @param rs            the result set
     * @param column        the column instance
     * @param languageId    the language to be used for multilingual properties
     * @return              the decoded result value
     * @throws SQLException on SQL errors
     */
    Object decodeResultValue(SqlMapperFactory factory, ResultSet rs, T column, long languageId) throws SQLException;

    /**
     * Returns true if the SQL dialect can select multivalued properties for the given data type.
     * <p>
     * If false, only the ID and version will be selected, and the multivalued properties will be populated afterwards
     * (with a separate query or from the content engine).
     * </p>
     *
     * @param factory   the SQL mapper factory
     *@param column     the result column
     * @param dataType  the datatype to be checked   @return          true if the SQL dialect can select multivalued properties for the given data type
     */
    boolean isDirectSelectForMultivalued(SqlMapperFactory factory, T column, FxDataType dataType);
}
