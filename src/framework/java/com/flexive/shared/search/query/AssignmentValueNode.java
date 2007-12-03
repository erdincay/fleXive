/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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

import com.flexive.shared.exceptions.FxInvalidQueryNodeException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
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
    private FxAssignment assignment;

    /**
     * Create a new property query node.
     * 
     * @param id    Internal and unique ID of the node
     * @param assignment  the property to be set with this node
     */
    public AssignmentValueNode(int id, FxAssignment assignment) {
        super(id);
        this.assignment = assignment;
        this.comparator = PropertyValueComparator.EQ;
        if (assignment instanceof FxPropertyAssignment) {
        	value = ((FxPropertyAssignment) assignment).getEmptyValue();
        }
        if (this.value == null) {
        	setValue(new FxString(""));
        }
    }

    public FxAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(FxAssignment property) {
        this.assignment = property;
    }
    
 	/** {@inheritDoc} */
    @Override
    public boolean isValid() {
        try {
            this.comparator.getSql(assignment, value);
            // if we can generate a SQL epxression, check additional assignment constraints
            return assignment.isValid(value);
        } catch (RuntimeException e) {
            // if we can't generate a SQL expression, we aren't valid
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void buildSqlQuery(SqlQueryBuilder builder) {
        try {
            builder.condition(assignment, comparator, value);
        } catch (FxRuntimeException e) {
            throw new FxInvalidQueryNodeException(getId(), e.getConverted()).asRuntimeException(); 
        }
    }

    /** {@inheritDoc} */
    @Override
    public FxString getLabel() {
        final String typeLabel = assignment.getAssignedType().getId() != FxType.ROOT_ID
                ? assignment.getAssignedType().getDisplayName() + "/" : "";
        return new FxString(typeLabel
                + (assignment.getParentGroupAssignment() != null
                    ? assignment.getParentGroupAssignment().getDisplayName() + "/" : "")
                + assignment.getDisplayName());
    }

    /** {@inheritDoc} */
    @Override
    public List<PropertyValueComparator> getNodeComparators() {
        return assignment instanceof FxPropertyAssignment
            ? PropertyValueComparator.getAvailable(((FxPropertyAssignment) assignment).getProperty().getDataType())
            : Arrays.asList(PropertyValueComparator.values());
    }

    /** {@inheritDoc} */
    @Override
    public InputMapper getInputMapper() {
        return inputMapper != null ? inputMapper : assignment instanceof FxPropertyAssignment 
                ? getPropertyInputMapper(((FxPropertyAssignment) assignment).getProperty())
                : IdentityInputMapper.getInstance();
    }

}
