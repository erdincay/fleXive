/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.core.sqlSearchEngines.mysql;

import com.flexive.core.DatabaseConst;
import com.flexive.core.sqlSearchEngines.PropertyResolver;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.structure.FxDataType;
import com.flexive.sqlParser.Property;

/**
 * MySQL specific workflow step selector
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class MySQLStepSelector extends MySQLGenericSelector {

    final static String ML_SEL = "(select description from " + DatabaseConst.TBL_CONTENT + " ct," + DatabaseConst.TBL_STEP + " step, " +
            DatabaseConst.TBL_STEPDEFINITION + " def," + DatabaseConst.TBL_STEPDEFINITION + DatabaseConst.ML + " deft" +
            " where \n" +
            "ct.id=filter.id and ct.ver=filter.ver and step.id=ct.step and step.stepdef=def.id and" +
            " deft.id=def.id and ";

    public MySQLStepSelector() {
        super(DatabaseConst.TBL_STEP, null);
    }


    @Override
    public void apply(Property prop, PropertyResolver.Entry entry, StringBuffer statement) throws FxSqlSearchException {
        if (prop.getField().equals("LABEL")) {
            statement.delete(0, statement.length());
            int lang = 2;
            statement.append(("ifnull(\n" +
                    ML_SEL + "lang=" + lang + " limit 1) ,\n" +
                    ML_SEL + "deflang=true limit 1) \n" +
                    ")"));
            entry.overrideDataType(FxDataType.String1024);
            return;
        }
        super.apply(prop, entry, statement);
    }

    @Override
    public String getAllowedFiels() {
        return super.getAllowedFiels() + ",label";
    }
}
