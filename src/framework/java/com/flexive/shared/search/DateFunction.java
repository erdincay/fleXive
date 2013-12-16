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
package com.flexive.shared.search;

import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.structure.FxDataType;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

/**
 * FxSQL functions available for date/datetime queries and selects.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public enum DateFunction implements FxSQLFunction {
    YEAR("YEAR"), MONTH("MONTH"), DAY("DAY"), HOUR("HOUR"), MINUTE("MINUTE"), SECOND("SECOND");

    private final String sqlName;

    DateFunction(String sqlName) {
        this.sqlName = sqlName;
    }

    @Override
    public String getSqlName() {
        return sqlName;
    }

    @Override
    public FxDataType getOverrideDataType() {
        return FxDataType.Number;   // all currently supported date functions return integers
    }


    /**
     * Return the date function for the given FxSQL name (e.g. "year").
     *
     * @param sqlName   the FxSQL function name
     * @return  the date function
     * @throws com.flexive.shared.exceptions.FxRuntimeException if the given SQL name is not a date function
     */
    public static DateFunction getBySqlName(String sqlName) {
        for (DateFunction function: DateFunction.values()) {
            if (function.getSqlName().equalsIgnoreCase(sqlName)) {
                return function;
            }
        }
        throw new FxNotFoundException("ex.sqlSearch.function.date", sqlName,
                StringUtils.join(FxSQLFunctions.getSqlNames(Arrays.asList(DateFunction.values())), ", ")).asRuntimeException();
    }
}
