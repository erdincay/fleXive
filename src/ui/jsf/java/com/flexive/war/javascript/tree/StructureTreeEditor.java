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
package com.flexive.war.javascript.tree;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.structure.*;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Content tree edit actions invoked via JSON/RPC.
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */

public class StructureTreeEditor implements Serializable {
    private static final long serialVersionUID = -2853036616736591794L;
    private static Pattern aliasPattern = Pattern.compile("[a-zA-Z][a-zA-Z_0-9]*");

    public void deleteAssignment(long id) throws FxApplicationException {
        EJBLookup.getAssignmentEngine().removeAssignment(id, true, false);
    }

    public void deleteType(long id) throws FxApplicationException {
        EJBLookup.getTypeEngine().remove(id);
    }

    public long pasteAssignment(long assId, String childNodeType, long parentId, String parentNodeType, String newName) throws FxApplicationException {
        String parentXPath = "/";
        FxType parentType = null;
        long assignmentId = -1;

        if (StructureTreeWriter.DOC_TYPE_GROUP.equals(parentNodeType)) {
            FxGroupAssignment ga = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(parentId);
            parentType = ga.getAssignedType();
            parentXPath = ga.getXPath();
        } else if (StructureTreeWriter.DOC_TYPE_TYPE.equals(parentNodeType) ||
                StructureTreeWriter.DOC_TYPE_TYPE_RELATION.equals(parentNodeType)) {
            parentType = CacheAdmin.getEnvironment().getType(parentId);
        }

        if (StructureTreeWriter.DOC_TYPE_ASSIGNMENT.equals(childNodeType)) {
            FxPropertyAssignment assignment = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(assId);
            assignmentId = EJBLookup.getAssignmentEngine().save(FxPropertyAssignmentEdit.createNew(assignment, parentType, newName == null ? assignment.getAlias() : newName, parentXPath), false);
        } else if (StructureTreeWriter.DOC_TYPE_GROUP.equals(childNodeType)) {
            FxGroupAssignment assignment = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(assId);
            assignmentId = EJBLookup.getAssignmentEngine().save(FxGroupAssignmentEdit.createNew(assignment, parentType, newName == null ? assignment.getAlias() : newName, parentXPath), true);
        }
        return assignmentId;
    }

    public boolean validateAlias(String alias) {
        if (alias != null) {
            Matcher m = aliasPattern.matcher(alias);
            if (m.matches())
                return true; //all correct
        }
        return false;
    }
}
