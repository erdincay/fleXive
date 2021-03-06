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
package com.flexive.shared.interfaces;

import com.flexive.shared.cmis.search.CmisResultSet;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxCmisQueryException;
import com.flexive.shared.exceptions.FxCmisSqlParseException;

import javax.ejb.Remote;

/**
 * <strong>Disclaimer: this API is part of the CMIS interface and is not yet considered stable.</strong><br/><br/>
 *
 * The CMIS search engine EJB interface.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
@Remote
public interface CmisSearchEngine {

    /**
     * @param query the CMIS-SQL query to be submitted
     * @return the query result
     * @throws FxCmisSqlParseException when the query could not be parsed
     * @throws FxCmisQueryException    when the query could not be executed
     */
    CmisResultSet search(String query) throws FxApplicationException;

    /**
     * @param query                 the CMIS-SQL query to be submitted
     * @param returnPrimitiveValues if true, the return values will not be boxed in
     *                              {@link com.flexive.shared.value.FxValue} objects
     * @param startRow              first row to return
     * @param maxRows               max. number of rows to return
     * @return the query result
     * @throws FxCmisSqlParseException when the query could not be parsed
     * @throws FxCmisQueryException    when the query could not be executed
     */
    CmisResultSet search(String query, boolean returnPrimitiveValues, int startRow, int maxRows) throws FxApplicationException;

}
