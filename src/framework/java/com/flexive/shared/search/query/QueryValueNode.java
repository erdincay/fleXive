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

import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxProperty;
import com.flexive.shared.value.FxDate;
import com.flexive.shared.value.FxDateTime;
import com.flexive.shared.value.FxValue;
import com.flexive.shared.value.mapper.IdentityInputMapper;
import com.flexive.shared.value.mapper.InputMapper;
import com.flexive.shared.value.mapper.VoidInputMapper;
import com.flexive.shared.value.renderer.FxValueFormatter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Base class for value types.
 * 
 * @param <T> the value type
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public abstract class QueryValueNode<T extends FxValue, VC extends ValueComparator> extends QueryNode {
    private static final long serialVersionUID = 914234426030223950L;

    protected T value;
    protected VC comparator;
    protected InputMapper inputMapper;

    /**
     * Default constructor
     */
    protected QueryValueNode() {
    }
    
    /**
     * Protected constructor.
     * @param id    the node ID
     */
    protected QueryValueNode(int id) {
        super(id);
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    /** {@inheritDoc} */
	@Override
	public void visit(QueryNodeVisitor visitor) {
		visitor.visit(this);
	}

	/** {@inheritDoc} */
	@Override
	public boolean isValueNode() {
		return true;
	}

    /**
     * Return true if the input fields should be read only for this node.
     *
     * @return true if the input fields should be read only for this node.
     */
    public boolean isReadOnly() {
        return false;
    }

    /**
     * Override this method to provide your own formatter for read-only mode.
     *
     * @return  the {@link com.flexive.shared.value.renderer.FxValueFormatter} to be used for rendering read-only mode. 
     */
    public FxValueFormatter getValueFormatter() {
        return null;
    }

    public VC getComparator() {
        return comparator;
    }

    public void setComparator(VC comparator) {
        this.comparator = comparator;
    }

    /**
     * Returns the input mapper to be used for this query node. Input mappers allow
     * to use "fancier" inputs for common properties, e.g. a select list for an internal
     * ordinal value like the ACL.
     *
     * @return  the input mapper for this node
     * @see com.flexive.shared.value.mapper.InputMapper
     * @see com.flexive.shared.value.mapper.InputMapper#getInstance(com.flexive.shared.structure.FxProperty)
     */
    public InputMapper getInputMapper() {
        return inputMapper != null ? inputMapper : IdentityInputMapper.getInstance();
    }

    public void setInputMapper(InputMapper inputMapper) {
        this.inputMapper = inputMapper;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWideInput() {
        return value instanceof FxDate || value instanceof FxDateTime;
    }

    /**
     * Override this method to set the available value comparators of a node instance.
     *
     * @return  all available enum values for this query node.
     */
    protected abstract List<VC> getNodeComparators();

    /**
     * Returns all available enum values for this query node.
     * The returned comparator list is the intersection of the property value comparators
     * returned by {@link #getNodeComparators()} and those defined by the input mapper, if any.
     *
     * @return  all available enum values for this query node.
     */
    public List<VC> getAvailableComparators() {
        final List<VC> result = new ArrayList<VC>();
        result.addAll(getNodeComparators());
        final List inputMapperResult = getInputMapper().getAvailableValueComparators();
        if (!inputMapperResult.isEmpty()) {
            for (Iterator<VC> iterator = result.iterator(); iterator.hasNext(); ) {
                // remove all comparators that are not available both in the input mapper and the query node
                if (!inputMapperResult.contains(iterator.next())) {
                    iterator.remove();
                }
            }
        }
        return result;
    }

    /**
     * Return the input mapper for the given property.
     *
     * @param property  the property to be rendered
     * @return  the input mapper for the given property assignmen.t
     */
    protected InputMapper getPropertyInputMapper(FxProperty property) {
        if (FxDataType.Binary.equals(property.getDataType())) {
            return VoidInputMapper.getInstance();
        } else {
            return InputMapper.getInstance(property);
        }
    }
}
