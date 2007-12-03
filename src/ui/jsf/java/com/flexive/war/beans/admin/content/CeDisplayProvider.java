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
package com.flexive.war.beans.admin.content;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.value.FxString;

import java.util.Hashtable;

/**
 * Helper class for the ContentEditor.
 * <p/>
 * It provides lookups for a property assignment's display name.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
class CeDisplayProvider extends Hashtable<Long, FxString> {

    private ContentEditorBean parent;

    /**
     * Constructor.
     *
     * @param parent the associated content editor
     */
    protected CeDisplayProvider(ContentEditorBean parent) {
        super(0);
        this.parent = parent;
    }

    /**
     * Fake put - does nothing expect returning the description for the given assignment id.
     *
     * @param assignmentId the assignment id
     * @param ignore       this parameter is discarded
     * @return the description of the assignment id, or null if the assignment id could not be resolved
     */
    public FxString put(Long assignmentId, FxString ignore) {
        // Do not put anything
        return get(assignmentId);
    }

    /**
     * Returns the description of the given assignment id.
     *
     * @param assignmentId the assignment id
     * @return the description, or null if the assignment id could not be resolved
     */
    public FxString get(Object assignmentId) {
        try {
            Long assId = (assignmentId instanceof Long) ? (Long) assignmentId :
                    Long.valueOf(String.valueOf(assignmentId));
            FxAssignment ass = parent.getEnvironment().getAssignment(assId);
            return ContentEditorBean.getDisplay(ass);
        } catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return null;
        }
    }


}
