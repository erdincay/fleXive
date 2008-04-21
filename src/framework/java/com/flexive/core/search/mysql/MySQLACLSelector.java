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
package com.flexive.core.search.mysql;

import com.flexive.core.DatabaseConst;
import com.flexive.core.search.PropertyEntry;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.FxContext;
import com.flexive.sqlParser.Property;

/**
 * Selector for ACL's
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class MySQLACLSelector extends MySQLGenericSelector {
    public MySQLACLSelector() {
        super(DatabaseConst.TBL_ACLS, "id");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void apply(Property prop, PropertyEntry entry, StringBuffer statement) throws FxSqlSearchException {
        if ("LABEL".equals(prop.getField())) {
            statement.delete(0, statement.length());
            String _tbl = DatabaseConst.TBL_ACLS + DatabaseConst.ML;
            final long lang = FxContext.get().getTicket().getLanguage().getId();
            statement.append(("ifnull(\n" +
                    "(select acl.label from " + _tbl + " acl, " + DatabaseConst.TBL_CONTENT + " ct where ct.id=filter.id and " +
                    "ct.ver=filter.ver and ct.acl=acl.id and lang=" + lang + " limit 1) ,\n" +
                    "(select acl.label from " + _tbl + " acl, " + DatabaseConst.TBL_CONTENT + " ct where ct.id=filter.id and " +
                    "ct.ver=filter.ver and ct.acl=acl.id and deflang=true limit 1) \n" +
                    ")"));
            entry.overrideDataType(FxDataType.String1024);
            return;
        }
        super.apply(prop, entry, statement);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAllowedFields() {
        return super.getAllowedFields() + ",LABEL";
    }
}
