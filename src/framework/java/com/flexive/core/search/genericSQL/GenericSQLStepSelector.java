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
package com.flexive.core.search.genericSQL;

import com.flexive.core.DatabaseConst;

/**
 * Generic SQL workflow step selector
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class GenericSQLStepSelector extends GenericSQLForeignTableSelector {
    private static final String ML_SEL = "(SELECT deft.name FROM " + DatabaseConst.TBL_CONTENT + " ct," + DatabaseConst.TBL_STEP + " step, " +
            DatabaseConst.TBL_STEPDEFINITION + " def," + DatabaseConst.TBL_STEPDEFINITION + DatabaseConst.ML + " deft" +
            " WHERE \n" +
            "ct.id=filter.id AND ct.ver=filter.ver AND step.id=ct.step AND step.stepdef=def.id AND" +
            " deft.id=def.id AND ";

    public GenericSQLStepSelector() {
        super("step", DatabaseConst.TBL_STEP, "id", true, "label");
    }

    @Override
    protected String getLabelSelect() {
        return ML_SEL;
    }
}
