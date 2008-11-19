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
import com.flexive.core.search.genericSQL.GenericSQLDataFilter;
import com.flexive.shared.exceptions.FxSqlSearchException;

import java.sql.Connection;

/**
 * H2 specific data filter
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 787 $
 */
public class H2SQLDataFilter extends GenericSQLDataFilter {

    /**
     * Ctor
     *
     * @param con    open and valid connection
     * @param search SqlSearch object
     * @throws FxSqlSearchException on errors
     */
    public H2SQLDataFilter(Connection con, SqlSearch search) throws FxSqlSearchException {
        super(con, search);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isQueryTimeoutSupported() {
        //H2 does not support timeouts (or they don't work at least...)
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String toDBTime(String expr) {
        return "TOTIMESTAMP(" + expr + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String fulltextMatch(String column, String expr) {
        return column + " LIKE '%" + expr.replaceAll("'", "" ) + "%'";
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected String getSubQueryLimit() {
        /* H2 currently chokes on unions of queries with row limits like this:

        SELECT * FROM (
            (SELECT  DISTINCT cd.id,cd.ver,cd.lang FROM FLEXIVETEST.FX_CONTENT_DATA cd WHERE UFCLOB like 'FOLDER COMMENT 1' AND TPROP=83 AND cd.ismax_ver=TRUE
             LIMIT 10000 )
            UNION
            (SELECT DISTINCT  cd.id,cd.ver,cd.lang FROM FLEXIVETEST.FX_CONTENT_DATA cd WHERE UFCLOB like 'FOLDER COMMENT 2' AND TPROP=83 AND cd.ismax_ver=TRUE
             LIMIT 10000 )
        )

         When the subquery limits are removed, the query returns correct results. With subquery limits,
         only one row is returned.
       */
        return "";
    }
}
