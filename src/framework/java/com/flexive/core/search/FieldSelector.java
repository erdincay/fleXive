/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
import com.flexive.sqlParser.Property;

/**
 * Helper to generate select statements on queries
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public interface FieldSelector {

    /**
     * This function applies the select logic on the statement.
     * <p/>
     * The statement parameter holds a single value (or subselect that will result in a single value).
     * This value is the reference that should be used for any further lookups.<br />
     * Example: <br/>
     * statement = "(select created_by from ... where ... limit 1)"<br/>
     * We can now apply another select logic on this value:<br/>
     * select username from FX_ACCOUNTS where id={statement}<br/>
     * The result is the updated statement StringBuffer.
     *
     * @param prop      the property that is beeing selected
     * @param entry     the property resolver entry
     * @param statement the statement to modify
     * @throws FxSqlSearchException if the function fails
     */
    void apply(Property prop, PropertyEntry entry, StringBuffer statement) throws FxSqlSearchException;

    /**
     * Returns all spported fields.
     * <p/>
     * For the FX_ACCOUNT table this fields would be: username, loginname, ...
     *
     * @return the fields.
     */
    String getAllowedFields();
}
