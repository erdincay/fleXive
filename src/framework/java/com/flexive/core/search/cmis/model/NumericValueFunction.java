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
package com.flexive.core.search.cmis.model;

import com.flexive.core.search.PropertyResolver;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ConditionColumnMapper;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.structure.FxPropertyAssignment;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class NumericValueFunction extends ValueFunction<NumericValueFunction> {
    private static final Log LOG = LogFactory.getLog(NumericValueFunction.class);

    private final List<FxPropertyAssignment> assignments =
            Collections.unmodifiableList(new ArrayList<FxPropertyAssignment>(0));

    public NumericValueFunction(String function, String alias) {
        super(resolveFunction(function),
                StringUtils.defaultIfEmpty(
                        alias,
                        Functions.SCORE.equals(resolveFunction(function)) ? "SEARCH_SCORE" : function
                )
        );
    }

    private static Functions resolveFunction(String function) {
        if ("score".equalsIgnoreCase(function)) {
            return Functions.SCORE;
        } else {
            throw new FxInvalidParameterException("function", LOG, "ex.cmis.model.invalid.function",
                    function, Arrays.asList(Functions.values())).asRuntimeException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public List<FxPropertyAssignment> getReferencedAssignments() {
        return assignments;
    }

    /**
     * {@inheritDoc}
     */
    public FxPropertyAssignment getBaseAssignment() {
        return assignments.isEmpty() ? null : assignments.get(0);
    }

    /**
     * {@inheritDoc}
     */
    public TableReference getTableReference() {
        return null;
    }

    public ConditionColumnMapper<? super NumericValueFunction> getConditionColumnMapper(SqlMapperFactory factory) {
        throw new UnsupportedOperationException("Numeric function in conditions are not supported.");
    }

    /**
     * {@inheritDoc}
     */
    public PropertyResolver.Table getFilterTableType() {
        throw new UnsupportedOperationException("Numeric function in conditions are not supported.");
    }

    /**
     * {@inheritDoc}
     */
    public String getFilterTableName() {
        throw new UnsupportedOperationException("Numeric function in conditions are not supported.");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUseUpperCase() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultivalued() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultilanguage() {
        return false;
    }
}
