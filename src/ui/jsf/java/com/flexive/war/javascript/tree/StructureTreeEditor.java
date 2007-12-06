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
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.structure.*;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;

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

    /**
     * Creates a derived assignment from a given assignment and pastes it into the
     * the parent group or type. A new alias can also be specified.
     *
     * @param assId             the id from which the assignment will be derived
     * @param childNodeType     the nodeType from which the assignment will be derivedt (i.e. StructureTreeWriter.DOC_TYPE_GROUP, StructureTreeWriter.DOC_TYPE_ASSIGNMENT)
     * @param parentId          the id of the parent group or type.
     * @param parentNodeType    the node type of the parent (i.e. StructureTreeWriter.DOC_TYPE_GROUP, StructureTreeWriter.DOC_TYPE_TYPE).
     * @param newName           the new alias. if ==null the old will be taken.
     *
     * @return  the id of the newly created assignment
     * @throws com.flexive.shared.exceptions.FxApplicationException     on errors
     */
    public long pasteAssignmentInto(long assId, String childNodeType, long parentId, String parentNodeType, String newName) throws FxApplicationException {
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

     /**
     * Creates a derived assignment from a given assignment and pastes it at
     * a relative position above or below (indicated by steps) a destination assignment
     * at the same hierarchy level.
     * A new alias can also be specified.
     *
     * @param srcId             the id from which the assignment will be derived
     * @param srcNodeType       the nodeType from which the assignment will be derived (i.e. StructureTreeWriter.DOC_TYPE_GROUP, StructureTreeWriter.DOC_TYPE_ASSIGNMENT)
     * @param destId            the id of the destination assignment, where the assignment will be pasted at a relative position
     * @param destNodeType      the node type of the destination assignment
      * @param newName          the new alias. if ==null the old will be taken.
     * @param steps             the position relative to the destination assignment, where the derived assignment will be pasted.
     *
     * @return  the id of the newly created assignment
     * @throws com.flexive.shared.exceptions.FxApplicationException     on errors
     */
     public long pasteAssignmentRelative(long srcId, String srcNodeType, long destId, String destNodeType ,String newName, int steps) throws FxApplicationException {
        String destXPath = "/";
        long assignmentId = -1;
        FxAssignment destAssignment = CacheAdmin.getEnvironment().getAssignment(destId);
        FxType destType = destAssignment.getAssignedType();

        //get destination xpath
        if (StructureTreeWriter.DOC_TYPE_GROUP.equals(destNodeType)) {
            destXPath = destAssignment.getXPath();
        }
        else if (StructureTreeWriter.DOC_TYPE_ASSIGNMENT.equals(destNodeType)) {
            if (destAssignment.hasParentGroupAssignment())
                destXPath = destAssignment.getParentGroupAssignment().getXPath();
        }
        else {
            throw new FxInvalidParameterException("nodeType", "ex.structureTreeEditor.nodeType.invalid", destNodeType);
        }

        if (StructureTreeWriter.DOC_TYPE_GROUP.equals(srcNodeType)) {
            FxGroupAssignment srcAssignment = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(srcId);
            //create assignment
            FxGroupAssignmentEdit newAssignment = FxGroupAssignmentEdit.createNew(srcAssignment, destType, newName == null ? srcAssignment.getAlias() : newName, destXPath);
            //set position
            newAssignment.setPosition(destAssignment.getPosition()+steps);
            //save newly created assignment to db
            assignmentId=EJBLookup.getAssignmentEngine().save(newAssignment, true);
        }
        else if (StructureTreeWriter.DOC_TYPE_ASSIGNMENT.equals(srcNodeType)) {
            FxPropertyAssignment srcAssignment = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(srcId);
            //create assignment
            FxPropertyAssignmentEdit newAssignment = FxPropertyAssignmentEdit.createNew(srcAssignment, destType, newName == null ? srcAssignment.getAlias() : newName, destXPath);
            //set position
            newAssignment.setPosition(destAssignment.getPosition()+steps);
            //save newly created assignment to db
            assignmentId=EJBLookup.getAssignmentEngine().save(newAssignment, false);
        }
        else {
            throw new FxInvalidParameterException("nodeType", "ex.structureTreeEditor.nodeType.invalid", srcNodeType);
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

    /**
     * Compares if two assignments are positioned at the same hierarchy level.
     * @param id1       id of first assignment
     * @param id2       id of second assignment
     * @param nodeType  the nodeDocType  (i.e. StructureTreeWriter.DOC_TYPE_GROUP, StructureTreeWriter.DOC_TYPE_ASSIGNMENT)
     * @return          true if they have the same parent type, or if parent group assignments exist, true if they have the same parent group assignment
     * @throws FxInvalidParameterException      for invalid nodeDocTypes
     */

    public boolean isSameLevel(long id1, long id2, String nodeType) throws FxInvalidParameterException {
        if (StructureTreeWriter.DOC_TYPE_GROUP.equals(nodeType)) {
            FxGroupAssignment a1 = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(id1);
            FxGroupAssignment a2 = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(id2);
            if (a1.hasParentGroupAssignment() && a2.hasParentGroupAssignment() && a1.getParentGroupAssignment().getId()
                    == a2.getParentGroupAssignment().getId())
                return true;
            else if (!a1.hasParentGroupAssignment() && !a2.hasParentGroupAssignment() && a1.getAssignedType().getId()==
                    a2.getAssignedType().getId())
                return true;
       }
        else if (StructureTreeWriter.DOC_TYPE_ASSIGNMENT.equals(nodeType)) {
            FxPropertyAssignment a1 = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(id1);
            FxPropertyAssignment a2 = (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(id2);

            if (a1.hasParentGroupAssignment() && a2.hasParentGroupAssignment() && a1.getParentGroupAssignment().getId()
                    == a2.getParentGroupAssignment().getId())
                return true;
            else if (!a1.hasParentGroupAssignment() && !a2.hasParentGroupAssignment() && a1.getAssignedType().getId()==
                    a2.getAssignedType().getId())
                return true;
        }
        else
             throw new FxInvalidParameterException("nodeType", "ex.structureTreeEditor.nodeType.invalid", nodeType);
        return false;
    }

    /**
     * Checks if an assignment is the child of a given type or group.
     * @param assId       id of the assignment
     * @param parentId       id of type or group assignment
     * @param parentNodeType  the nodeDocType  (i.e. StructureTreeWriter.DOC_TYPE_GROUP, StructureTreeWriter.DOC_TYPE_TYPE) of the parent
     * @return          true if the assignment is a direct child of the type or group
     * @throws FxInvalidParameterException      for invalid nodeDocTypes
     * @throws com.flexive.shared.exceptions.FxNotFoundException    on errors
     */

    public boolean isChild(long assId, long parentId, String parentNodeType) throws FxInvalidParameterException, FxNotFoundException {
       if (StructureTreeWriter.DOC_TYPE_GROUP.equals(parentNodeType)) {
            FxGroupAssignment ga = (FxGroupAssignment)CacheAdmin.getEnvironment().getAssignment(parentId);
            for (FxAssignment a : ga.getAssignments()) {
                if (a.getId() == assId)
                    return true;
            }
       }
       else if (StructureTreeWriter.DOC_TYPE_TYPE.equals(parentNodeType)) {
           FxType type = CacheAdmin.getEnvironment().getType(parentId);
            for (FxAssignment a : type.getConnectedAssignments("/")) {
                 if (a.getId() == assId)
                    return true;
            }
       }
       else
             throw new FxInvalidParameterException("nodeType", "ex.structureTreeEditor.nodeType.invalid", parentNodeType);

       return false;
    }
}
