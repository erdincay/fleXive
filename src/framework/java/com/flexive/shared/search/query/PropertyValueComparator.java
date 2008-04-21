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
package com.flexive.shared.search.query;

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.search.Table;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Supported value comparators for FxValues.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public enum PropertyValueComparator implements ValueComparator {
	EQ("="),
	NE("!="),
	LT("<"),
	LE("<="),
	GT(">"),
	GE(">="),
	EMPTY("NULL", false) {
        @Override
        public String getSql(String leftHandSide, String rightHandSide) {
            return leftHandSide + " IS NULL";
        }
    },
	NOT_EMPTY("NOTNULL", false) {
		@Override
        public String getSql(String leftHandSide, String rightHandSide) {
			return leftHandSide + " IS NOT NULL";
		}
		
	},
	LIKE("LIKE");

    private static final List<PropertyValueComparator> NUMERIC_OPERATORS = Collections.unmodifiableList(Arrays.asList(
            EQ, NE, LT, LE, GT, GE
    ));
    private static final List<PropertyValueComparator> STRING_OPERATORS = Collections.unmodifiableList(Arrays.asList(
            EQ, NE, EMPTY, NOT_EMPTY, LIKE
    ));
    private static final List<PropertyValueComparator> ORDINAL_OPERATORS = Collections.unmodifiableList(Arrays.asList(
            EQ, NE
    ));
    private static final List<PropertyValueComparator> DATE_OPERATORS = Collections.unmodifiableList(Arrays.asList(
            EQ, NE, LT, LE, GT, GE, EMPTY, NOT_EMPTY
    ));
    private static final List<PropertyValueComparator> DATERANGE_OPERATORS = Collections.unmodifiableList(Arrays.asList(
            LT, LE, GT, GE, EMPTY, NOT_EMPTY
    ));
    private static final List<PropertyValueComparator> EMPTY_OPERATORS = Collections.unmodifiableList(Arrays.asList(
            EMPTY, NOT_EMPTY
    ));

    private String id;
	private boolean needsInput;
	
	private PropertyValueComparator(String id) {
		this(id, true);
	}

	private PropertyValueComparator(String id, boolean needsInput) {
		this.id = id;
		this.needsInput = needsInput;
	}
	
	public final String getSql(FxAssignment assignment, FxValue value) {
        final String sqlValue;
        if (needsInput) {
            if (value.isEmpty()) {
                throw new FxInvalidStateException("ex.sqlQueryBuilder.value.empty", assignment.getLabel(), id).asRuntimeException();
            }
            try {
                sqlValue = value.getSqlValue();
            } catch (RuntimeException e) {
                throw new FxInvalidStateException("ex.sqlQueryBuilder.value.invalid",
                        assignment.getLabel(), id, value).asRuntimeException();
            }
        } else {
            sqlValue = null;
        }
        // select assignment by XPath
        return getSql(Table.CONTENT.getColumnName("#" + assignment.getXPath()), sqlValue);
	}

    public final String getSql(FxProperty property, FxValue value) {
        final String sqlValue;
        if (needsInput) {
            try {
                sqlValue = value.getSqlValue();
            } catch (RuntimeException e) {
                throw new FxInvalidStateException("ex.sqlQueryBuilder.value.invalid",
                        property.getName(), id, value).asRuntimeException();
            }
        } else {
            sqlValue = null;
        }
        return getSql(Table.CONTENT.getColumnName(property.getName()), sqlValue);
    }

    public final String getSql(String columnName, FxValue value) {
        return getSql(columnName, value != null ? value.getSqlValue() : null);
    }

    public final String getSql(String columnName, Object value) {
        return getSql(columnName, FxFormatUtils.escapeForSql(value));
    }

    /**
     * Returns the SQL expression generated by combining both parameters with
     * this comparator.
     *
     * @param leftHandSide  the left-hand-side of the comparison
     * @param rightHandSide the right-hand-side of the comparison
     * @return  the SQL query
     */
    protected String getSql(String leftHandSide, String rightHandSide) {
        return leftHandSide + " " + id + " " + rightHandSide;
    }

    /** {@inheritDoc} */
    public boolean isNeedsInput() {
		return needsInput;
	}

    /** {@inheritDoc} */
    public FxString getLabel() {
        return FxSharedUtils.getEnumLabel(this);
    }

    /**
     * Returns a list of value comparators applicable to the given data type.
     *
     * @param dataType  the data type
     * @return      a list of value comparators applicable to the given data type.
     */
    public static List<PropertyValueComparator> getAvailable(FxDataType dataType) {
        switch (dataType) {
            case Double:
            case Float:
            case LargeNumber:
            case Number:
                return NUMERIC_OPERATORS;
            case String1024:
            case Text:
            case HTML:
                return STRING_OPERATORS;
            case Binary:
                return EMPTY_OPERATORS;
            case Date:
            case DateTime:
                return DATE_OPERATORS;
            case DateRange:
            case DateTimeRange:
                return DATERANGE_OPERATORS;
            default:
                return ORDINAL_OPERATORS;
        }
    }
    
}