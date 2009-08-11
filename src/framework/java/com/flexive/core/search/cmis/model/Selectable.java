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

import com.flexive.shared.structure.FxPropertyAssignment;

import java.util.List;

/**
 * A "column" that can be selected in a select statement.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public interface Selectable {
    /**
     * Returns a list of assignments referenced by this expression.
     *
     * @return  a list of assignments referenced by this expression.
     */
    List<FxPropertyAssignment> getReferencedAssignments();

    /**
     * Return the base assignment for this column. May be null if the column a virtual property
     * that is not mapped to a flexive property.
     *
     * @return  the base assignment for this column
     */
    FxPropertyAssignment getBaseAssignment();

    /**
     * Return the table reference of this column.
     *
     * @return  the table reference of this column.
     */
    TableReference getTableReference();

    /**
     * Return the alias of this column, as it is returned to the calling user.
     *
     * @return  the alias of this column, as it is returned to the calling user.
     */
    String getAlias();

    /**
     * Returns true if this selectable element should be treated as a CMIS multivalued property,
     * i.e. a property with a maximum multiplicity greater than 1.
     * <p>
     * Multivalued properties are returned as a list of result values, instead of a single result value
     * for singlevalued properties.
     * </p>
     *
     * @return  true if this selectable element should be treated as a CMIS multivalued property
     */
    boolean isMultivalued();
}
