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

import com.flexive.core.search.PropertyResolver;
import com.flexive.shared.structure.FxPropertyAssignment;

import java.util.List;

/**
 * A function applied on a column value (e.g. UPPER(...)).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public abstract class ColumnValueFunction<T extends ColumnValueFunction> extends ValueFunction<T> {
    private final ColumnReference columnReference;

    protected ColumnValueFunction(Functions function, String alias, ColumnReference columnReference) {
        super(function, alias);
        this.columnReference = columnReference;
    }

    public ColumnReference getColumnReference() {
        return columnReference;
    }

    /**
     * {@inheritDoc}
     */
    public List<FxPropertyAssignment> getReferencedAssignments() {
        return columnReference.getReferencedAssignments();
    }

    /**
     * {@inheritDoc}
     */
    public FxPropertyAssignment getBaseAssignment() {
        return columnReference.getBaseAssignment();
    }

    /**
     * {@inheritDoc}
     */
    public TableReference getTableReference() {
        return columnReference.getTableReference();
    }

    /**
     * {@inheritDoc}
     */
    public PropertyResolver.Table getFilterTableType() {
        return columnReference.getPropertyEntry().getTableType();
    }

    /**
     * {@inheritDoc}
     */
    public String getFilterTableName() {
        return columnReference.getFilterTableName();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isMultivalued() {
        return columnReference.isMultivalued();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isMultilanguage() {
        return columnReference.isMultivalued();
    }
}
