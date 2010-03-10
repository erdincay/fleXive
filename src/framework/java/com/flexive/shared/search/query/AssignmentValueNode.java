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
package com.flexive.shared.search.query;

import static com.flexive.shared.CacheAdmin.getEnvironment;
import com.flexive.shared.exceptions.FxInvalidQueryNodeException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.FxVoid;
import com.flexive.shared.value.mapper.IdentityInputMapper;
import com.flexive.shared.value.mapper.InputMapper;

import java.util.Arrays;
import java.util.List;

/**
 * A query node representing a structure property.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class AssignmentValueNode extends QueryValueNode<FxValue, PropertyValueComparator> {
    private static final long serialVersionUID = 4285245221573681218L;
    private long assignmentId;

    /**
     * Create a new property query node.
     * 
     * @param id    Internal and unique ID of the node
     * @param assignmentId  the assignment to be set with this node
     */
    public AssignmentValueNode(int id, long assignmentId) {
        super(id);
        this.assignmentId = assignmentId;
        this.comparator = PropertyValueComparator.EQ;
        final FxAssignment assignment = getEnvironment().getAssignment(assignmentId);
        if (assignment instanceof FxPropertyAssignment) {
            // use the property's empty value to avoid using the assignment's default value
            value = getEmptyValue(((FxPropertyAssignment) assignment).getProperty());
            value.setXPath(assignment.getXPath());
        }
        if (this.value == null) {
        	setValue(new FxString(""));
        }
    }

    public FxAssignment getAssignment() {
        return getEnvironment().getAssignment(assignmentId);
    }

    public void setAssignmentId(long assignmentId) {
        this.assignmentId = assignmentId;
    }
    
 	/** {@inheritDoc} */
    @Override
    public boolean isValid() {
        try {
            final FxAssignment assignment = getAssignment();
            final FxDataType type = assignment instanceof FxPropertyAssignment
                    ? ((FxPropertyAssignment) assignment).getProperty().getDataType() : null;
            this.comparator.getSql(assignment, value);
            return value == null || value instanceof FxVoid
                    // date/datetime ranges are mapped to FxDate values, thus we cannot call assignment.isValid
                    || (FxDataType.DateRange == type || FxDataType.DateTimeRange == type)
                    // check additional assignment constraints
                    || assignment.isValid(value);
        } catch (RuntimeException e) {
            // if we can't generate a SQL expression, we aren't valid
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void buildSqlQuery(SqlQueryBuilder builder) {
        try {
            builder.condition(getAssignment(), comparator, getValueForSQL());
        } catch (FxRuntimeException e) {
            throw new FxInvalidQueryNodeException(getId(), e.getConverted()).asRuntimeException(); 
        }
    }

    /** {@inheritDoc} */
    @Override
    public FxString getLabel() {
        final String typeLabel = getAssignment().getAssignedType().getId() != FxType.ROOT_ID
                ? getAssignment().getAssignedType().getDisplayName() + "/" : "";
        return new FxString(typeLabel + getAssignment().getDisplayName(true));
    }

    /** {@inheritDoc} */
    @Override
    public List<PropertyValueComparator> getNodeComparators() {
        return getAssignment() instanceof FxPropertyAssignment
            ? PropertyValueComparator.getAvailable(((FxPropertyAssignment) getAssignment()).getProperty().getDataType())
            : Arrays.asList(PropertyValueComparator.values());
    }

    /** {@inheritDoc} */
    @Override
    public InputMapper getInputMapper() {
        return inputMapper != null ? inputMapper : getAssignment() instanceof FxPropertyAssignment
                ? getPropertyInputMapper(((FxPropertyAssignment) getAssignment()).getProperty())
                : IdentityInputMapper.getInstance();
    }

}
