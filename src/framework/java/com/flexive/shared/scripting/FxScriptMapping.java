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
package com.flexive.shared.scripting;

import java.io.Serializable;
import java.util.List;

/**
 * Class describing the mapping of a script
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxScriptMapping implements Serializable {
    private static final long serialVersionUID = -3109571015814480974L;
    long scriptId;
    List<FxScriptMappingEntry> mappedTypes;
    List<FxScriptMappingEntry> mappedAssignments;

    /**
     * Constructor
     *
     * @param scriptId          id of the script
     * @param mappedTypes       List of FxMappingEntry for FxTypes
     * @param mappedAssignments List of FxMappingEntry for FxAssignments
     */
    public FxScriptMapping(long scriptId, List<FxScriptMappingEntry> mappedTypes, List<FxScriptMappingEntry> mappedAssignments) {
        this.scriptId = scriptId;
        this.mappedTypes = mappedTypes;
        this.mappedAssignments = mappedAssignments;
    }

    /**
     * Get the scripts id
     *
     * @return script id
     */
    public long getScriptId() {
        return scriptId;
    }

    /**
     * Get the mapped types
     *
     * @return mapped types
     */
    public List<FxScriptMappingEntry> getMappedTypes() {
        return mappedTypes;
    }

    /**
     * Get the mapped assignments
     *
     * @return mapped assignments
     */
    public List<FxScriptMappingEntry> getMappedAssignments() {
        return mappedAssignments;
    }
}
