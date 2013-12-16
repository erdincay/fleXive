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
package com.flexive.shared.cmis.search;

import com.google.common.base.Function;

import java.io.Serializable;

/**
 * <strong>Disclaimer: this API is part of the CMIS interface and is not yet considered stable.</strong><br/><br/>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class CmisResultColumnDefinition implements Serializable {
    private static final long serialVersionUID = -551715816082744944L;

    /**
     * Google transform function to extract the <strong>alias</strong> from a list of column definitions.
     */
    public static final Function<CmisResultColumnDefinition,Long> TRANSFORM_ASSIGNMENT_ID = new Function<CmisResultColumnDefinition, Long>() {
        @Override
        public Long apply(CmisResultColumnDefinition from) {
            return from.getAssignmentId();
        }
    };
    /**
     * Google transform function to extract the <strong>assignmentId</strong> from a list of column definitions.
     */
    public static final Function<CmisResultColumnDefinition,String> TRANFORM_ALIAS = new Function<CmisResultColumnDefinition, String>() {
        @Override
        public String apply(CmisResultColumnDefinition from) {
            return from.getAlias();
        }
    };

    private final String alias;
    private final long assignmentId;

    public CmisResultColumnDefinition(String alias, long assignmentId) {
        this.alias = alias;
        this.assignmentId = assignmentId;
    }

    public String getAlias() {
        return alias;
    }

    public long getAssignmentId() {
        return assignmentId;
    }
}
