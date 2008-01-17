/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
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
    private FxProperty property;

    /**
     * Create a new property query node.
     *
     * @param id    Internal and unique ID of the node
     * @param property the property to be set with this node
     */
    public PropertyValueNode(int id, FxProperty property) {
        super(id);
        this.property = property;
        this.comparator = PropertyValueComparator.EQ;
        if (property != null) {
            this.value = property.getEmptyValue();
        } else {
            setValue(new FxString(""));
        }
    }

    public FxProperty getProperty() {
        return property;
    }

    public void setProperty(FxProperty property) {
        this.property = property;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isValid() {
        try {
            this.comparator.getSql(property, value);
            // if we can generate a SQL epxression, check additional property constraints
            return true; // TODO property check //return property.isValid(value);
        } catch (RuntimeException e) {
            // if we can't generate a SQL expression, we aren't valid
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void buildSqlQuery(SqlQueryBuilder builder) {
        try {
            builder.condition(property, comparator, value);
        } catch (FxRuntimeException e) {
            throw new FxInvalidQueryNodeException(getId(), e.getConverted()).asRuntimeException();
        }
    }

    /** {@inheritDoc} */
    @Override
    public FxString getLabel() {
        return property.getLabel();
    }

    /** {@inheritDoc} */
    @Override
    public List<PropertyValueComparator> getNodeComparators() {
        return PropertyValueComparator.getAvailable(property.getDataType());
    }

    /** {@inheritDoc} */
    @Override
    public InputMapper getInputMapper() {
        return inputMapper != null ? inputMapper : getPropertyInputMapper(property);
    }
}
