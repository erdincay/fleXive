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
package com.flexive.core.search.cmis.model;

import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionColumnMapper;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Arrays;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class StringValueFunction extends ColumnValueFunction<StringValueFunction> {
    private static final Log LOG = LogFactory.getLog(StringValueFunction.class);

    public StringValueFunction(ColumnReference columnReference, String function, String alias) {
        // TODO: handle optional alias for function
        super(resolveFunction(function), alias != null ? alias : function, columnReference);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConditionColumnMapper<? super StringValueFunction> getConditionColumnMapper(SqlMapperFactory factory) {
        return factory.filterColumnFunction();
    }

    private static Functions resolveFunction(String function) {
        if ("upper".equalsIgnoreCase(function)) {
            return Functions.UPPER;
        } else if ("lower".equalsIgnoreCase(function)) {
            return Functions.LOWER;
        } else {
            throw new FxInvalidParameterException("function", LOG, "ex.cmis.model.invalid.function",
                    function, Arrays.asList(Functions.values())).asRuntimeException();
        }
    }

}
