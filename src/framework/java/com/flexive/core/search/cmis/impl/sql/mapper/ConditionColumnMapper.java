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

import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.model.ConditionalExpressionPart;

/**
 * Represents a part of a conditional expression that references a data field or a literal
 * (i.e. one part of a comparison).
 * <p>
 * The implementer maps one part of a conditional expression to a single column that can be used for a SQL condition
 * (e.g. a subselect that returns a single value, or a literal).
 * </p>
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 * @since 3.1
 */
public interface ConditionColumnMapper<T extends ConditionalExpressionPart> {

    /**
     * Return the filter column for the given expression, using a predefined table alias if data
     * from the content table needs to be selected.
     *
     * @param sqlMapperFactory the {@link com.flexive.core.search.cmis.impl.sql.SqlMapperFactory}
     * @param expression        the conditional expression part for which the column should be mapped
     * @param tableAlias        an optional table alias to select the table where the column data is stored
     * @return  the filter column for the given expression
     */
    String getConditionColumn(SqlMapperFactory sqlMapperFactory, T expression, String tableAlias);

}
