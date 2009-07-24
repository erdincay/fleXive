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
package com.flexive.core.search.cmis.impl.sql.generic.mapper.condition;

import com.flexive.core.search.cmis.model.ColumnReference;
import com.flexive.core.search.cmis.model.NullCondition;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class GenericNullColumnReferenceCondition extends AbstractColumnReferenceCondition<NullCondition> {
    private static final GenericNullColumnReferenceCondition INSTANCE = new GenericNullColumnReferenceCondition();

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnReference getColumnReference(NullCondition condition) {
        return condition.getColumnReference();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getColumnCondition(NullCondition condition) {
        return "IS NOT NULL";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean negationQuery(NullCondition condition) {
        return !condition.isNegated();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean filterTypes(NullCondition condition) {
        return false;   //!condition.isNegated();   // type filter for negation queries is now included by parent
    }

    public static GenericNullColumnReferenceCondition getInstance() {
        return INSTANCE;
    }
}
