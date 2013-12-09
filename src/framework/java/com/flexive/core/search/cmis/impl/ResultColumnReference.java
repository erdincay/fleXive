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
package com.flexive.core.search.cmis.impl;

import com.flexive.core.search.PropertyEntry;
import com.flexive.core.search.cmis.impl.sql.SqlMapperFactory;
import com.flexive.core.search.cmis.impl.sql.mapper.ResultColumnMapper;
import com.flexive.core.search.cmis.model.ColumnReference;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
*/
public class ResultColumnReference extends AbstractResultColumn<ColumnReference, ResultColumnReference> {
    private final ColumnReference reference;

    ResultColumnReference(ColumnReference reference) {
        this.reference = reference;
    }

    public String getAlias() {
        return reference.getAlias();
    }

    @Override
    protected ResultColumnReference getThis() {
        return this;
    }

    public ColumnReference getSelectedObject() {
        return reference;
    }

    public PropertyEntry getPropertyEntry() {
        return reference.getPropertyEntry();
    }

    @Override
    public ResultColumnMapper<ResultColumnReference> getSqlMapper(SqlMapperFactory factory) {
        if (reference.getCmisProperty() != null && !reference.getCmisProperty().isFxProperty()) {
            // CMIS property, not mapped to a FxSQL property
            switch (reference.getCmisProperty()) {
                case ParentId:
                    return factory.selectParentId();
                default:
                    throw new UnsupportedOperationException(
                            "Unsupported CMIS property selected: " + reference.getCmisProperty().getCmisPropertyName()
                    );
            }
        }
        // FxSQL property or normal property assignment reference
        final PropertyEntry entry = reference.getPropertyEntry();
        if (entry == null) {
            return factory.selectColumnReference(); // default mapper
        }
        switch (entry.getType()) {
            case METADATA:
                throw new UnsupportedOperationException("Briefcase metadata not supported yet.");
            case NODE_POSITION:
                throw new UnsupportedOperationException("Tree node positions not supported yet.");
            case PATH:
                return factory.selectPath();
            case PERMISSIONS:
                throw new UnsupportedOperationException("ACL/permissions not supported yet.");
            case PK:
            case PROPERTY_REF:
                return factory.selectColumnReference();
            default:
                throw new UnsupportedOperationException(
                        "Unsupported entry type: " + entry.getType()
                );
        }
    }
}
