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
package com.flexive.core.search.cmis.impl.sql.mapper;

import com.flexive.core.search.cmis.impl.sql.SelectedTableVisitor;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.model.Condition;

/**
 * Create the SQL for a single {@link Condition} (e.g. a comparison or IN condition).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 *
 * @param <T>  the concrete condition type the mapper can generate SQL for
 */
public interface ConditionMapper<T extends Condition> {

    /**
     * Return the SQL for the given condition.
     *
     * @param sqlMapperFactory  the SQL mapper factory
     * @param condition         the condition to be rendered
     * @param joinedTables      status object containing aliases for all joined tables.
     *      Its main purpose is to provide the column names of all selected ID/version
     *      columns, since they must be the same for all condition (sub-)selects.
     *      Use the method
     *      {@link SelectedTableVisitor#getSelectForSingleTable(com.flexive.core.search.cmis.model.TableReference)}
     *      for retrieving the "SELECT" part of your subquery,
     *      and {@link SelectedTableVisitor#getTableAlias(com.flexive.core.search.cmis.model.TableReference)}
     *      to obtain the desired table alias for a CMIS table.
     *
     * @return  the SQL for the given condition.
     */
    String getConditionSql(SqlMapperFactory sqlMapperFactory, T condition, SelectedTableVisitor joinedTables);

}
