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

import static com.flexive.shared.CacheAdmin.getEnvironment;
import com.flexive.shared.exceptions.FxInvalidQueryNodeException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.FxVoid;
import com.flexive.shared.value.mapper.InputMapper;

import java.util.List;

/**
 * A property query condition
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class PropertyValueNode extends QueryValueNode<FxValue, PropertyValueComparator> {
    private static final long serialVersionUID = -8094968229492570983L;
    private long propertyId;

    /**
     * Create a new property query node.
     *
     * @param id    Internal and unique ID of the node
     * @param propertyId the property to be set with this node
     */
    public PropertyValueNode(int id, long propertyId) {
        super(id);
        this.propertyId = propertyId;
        this.comparator = PropertyValueComparator.EQ;
        if (propertyId != -1) {
            final FxEnvironment environment = getEnvironment();
            this.value = getEmptyValue(environment.getProperty(propertyId));
            // use XPath of first assignment to enable property lookups via FxValue
            final List<FxPropertyAssignment> assignments = environment.getPropertyAssignments(propertyId, true);
            assert assignments.size() > 0 : "At least one assignment is expected to exist for property " + propertyId;
            this.value.setXPath(assignments.get(0).getXPath());
        } else {
            this.value = new FxString("");
        }
    }

    public FxProperty getProperty() {
        return propertyId != -1 ? getEnvironment().getProperty(propertyId) : null;
    }

    public void setPropertyId(long propertyId) {
        this.propertyId = propertyId;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid() {
        try {
            this.comparator.getSql(getProperty(), value);
            return true;    // we can generate a SQL expression, thus the node is valid
        } catch (RuntimeException e) {
            // if we can't generate a SQL expression, we aren't valid
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void buildSqlQuery(SqlQueryBuilder builder) {
        try {
            builder.condition(getProperty(), comparator, value);
        } catch (FxRuntimeException e) {
            throw new FxInvalidQueryNodeException(getId(), e.getConverted()).asRuntimeException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public FxString getLabel() {
        return getProperty() != null ? getProperty().getLabel() : new FxString("");
    }

    /** {@inheritDoc} */
    @Override
    public List<PropertyValueComparator> getNodeComparators() {
        return PropertyValueComparator.getAvailable(getProperty().getDataType());
    }

    /** {@inheritDoc} */
    @Override
    public InputMapper getInputMapper() {
        return inputMapper != null ? inputMapper : getPropertyInputMapper(getProperty());
    }
}
