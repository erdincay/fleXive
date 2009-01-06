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
package com.flexive.core.search.H2;

import com.flexive.core.search.SqlSearch;
import com.flexive.core.search.PropertyEntry;
import com.flexive.core.search.genericSQL.GenericSQLDataSelector;
import com.flexive.core.DatabaseConst;
import com.flexive.shared.structure.FxDataType;
import org.apache.commons.lang.StringUtils;

/**
 * H2 specific data selector
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class H2SQLDataSelector extends GenericSQLDataSelector {

    /**
     * Ctor
     *
     * @param search the search object
     */
    public H2SQLDataSelector(SqlSearch search) {
        super(search);
    }

    /*
     //mp: keeping this code as a reference, H2 supports this kind of subqueries since v1.105 
    protected String getContentDataSubselect(String column, PropertyEntry entry, boolean xpath) {
        //H2 needs the ISMLDEF column in select clause to perform the ORDER,
        //hence we have to select check for the selected language before we use the multilang default
        String _select = "SELECT " + SUBSEL_ALIAS + "." + column +
                " FROM " + DatabaseConst.TBL_CONTENT_DATA + " " +
                SUBSEL_ALIAS + " WHERE " +
                SUBSEL_ALIAS + ".ID=" +
                FILTER_ALIAS + ".ID AND " +
                SUBSEL_ALIAS + ".VER=" +
                FILTER_ALIAS + ".VER AND " +
                (entry.isAssignment()
                        ? "ASSIGN=" + entry.getAssignment().getId()
                        : "TPROP=" + entry.getProperty().getId()) + " AND ";
        String select = "(IFNULL(("+_select+" "+ SUBSEL_ALIAS + ".LANG=" + search.getLanguage().getId() + " LIMIT 1),("+
                _select+" "+SUBSEL_ALIAS + ".ISMLDEF=TRUE LIMIT 1)))";
        if (!xpath && entry.getProperty().getDataType() == FxDataType.Binary) {
            // select string-coded form of the BLOB properties
            // TODO link version/quality filtering to the main object version
            select = "(SELECT CONCAT_WS('" + BINARY_DELIM + "'," +
                    StringUtils.join(BINARY_COLUMNS, ',') + ") " +
                    "FROM " + DatabaseConst.TBL_CONTENT_BINARY + " " +
                    "WHERE ID=" + select + " " +
                    " AND VER=1 AND QUALITY=1)";
        }
        return select;
    }
    */

    /**
     * {@inheritDoc}
     */
    @Override
    protected String toDBTime(String expr) {
        return "TOTIMESTAMP("+expr+")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean supportsCounterAfterOrderBy() {
        return false;
    }
}
