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
package com.flexive.core.search.cmis.impl.sql.generic.mapper.condition;

import com.flexive.core.DatabaseConst;
import com.flexive.core.search.cmis.impl.sql.SelectedTableVisitor;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionMapper;
import com.flexive.core.search.cmis.model.FolderCondition;
import com.flexive.core.storage.genericSQL.GenericTreeStorage;
import com.flexive.shared.tree.FxTreeMode;

/**
 *
 * SQL mapper for IN_FOLDER conditions.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class GenericInFolderCondition implements ConditionMapper<FolderCondition> {
    private static final GenericInFolderCondition INSTANCE = new GenericInFolderCondition();

    public static GenericInFolderCondition getInstance() {
        return INSTANCE;
    }

    @Override
    public String getConditionSql(SqlMapperFactory sqlMapperFactory, FolderCondition condition, SelectedTableVisitor joinedTables) {
        final String alias = joinedTables.getTableAlias(condition.getTableReference());
        return "(SELECT DISTINCT " + joinedTables.getSelectForSingleTable(condition.getTableReference()) + " FROM "
                + DatabaseConst.TBL_CONTENT + " " + alias + " "
                + "WHERE " + alias + ".id IN ("
                + "SELECT ref FROM " + GenericTreeStorage.getTable(FxTreeMode.Edit) + " t "
                + "WHERE t.parent=" + condition.getFolderId()

                + sqlMapperFactory.getSqlDialect().limitSubquery()
                + ")"   // end tree subselect
                + " AND " + sqlMapperFactory.getSqlDialect().getTypeFilter(alias + ".tdef", condition.getTableReference().getReferencedTypes())
                + ")";
    }

}
