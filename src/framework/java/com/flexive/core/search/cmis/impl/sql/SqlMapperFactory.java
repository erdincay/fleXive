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
package com.flexive.core.search.cmis.impl.sql;

import com.flexive.core.search.cmis.impl.ResultColumnFunction;
import com.flexive.core.search.cmis.impl.ResultColumnReference;
import com.flexive.core.search.cmis.impl.ResultRowNumber;
import com.flexive.core.search.cmis.impl.ResultScore;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionColumnMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionMapper;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;
import com.flexive.core.search.cmis.model.*;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public interface SqlMapperFactory {

    ResultColumnMapper<ResultRowNumber> selectRowNumber();

    ResultColumnMapper<ResultColumnReference> selectColumnReference();

    ResultColumnMapper<ResultColumnReference> selectPath();

    ResultColumnMapper<ResultColumnFunction> selectColumnFunction();

    ResultColumnMapper<ResultScore> selectScore();

    ConditionMapper<ComparisonCondition> conditionCompare();

    ConditionMapper<ContainsCondition> conditionContain();

    ConditionMapper<LikeCondition> conditionLike();

    ConditionMapper<InCondition> conditionIn();

    ConditionMapper<NullCondition> conditionNull();

    ConditionColumnMapper<ColumnReference> filterColumnReference();
    
    ConditionColumnMapper<Literal> filterLiteral();

    ConditionColumnMapper<ColumnValueFunction> filterColumnFunction();

    SqlDialect getSqlDialect();
}
