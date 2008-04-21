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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.*;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxGroupEdit;
import com.flexive.shared.structure.FxPropertyEdit;

import javax.ejb.Remote;

/**
 * Structure Assignment management
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface AssignmentEngine {

    /**
     * Create a new property and assign it at the given parentXPath to the virtual ROOT_ID type
     *
     * @param property    property to create
     * @param parentXPath the property's parent xpath
     * @return <b>assignment id</b> of the property
     * @throws FxApplicationException      on errors
     * @throws FxCreateException           for create errors
     * @throws FxInvalidParameterException if the given parentXPath is not valid
     * @throws FxEntryExistsException      if a property with this name already exists at the requested parentXPath
     * @throws FxNoAccessException         if the calling user is not permitted to call this method
     */
    long createProperty(FxPropertyEdit property, String parentXPath) throws FxApplicationException;

    /**
     * Create a new property and assign it at the given parentXPath to the given type
     *
     * @param typeId      id of the type to assign this property to
     * @param property    property to create
     * @param parentXPath the property's parent xpath
     * @return <b>assignment id</b> of the property
     * @throws FxApplicationException      on errors
     * @throws FxCreateException           for create errors
     * @throws FxInvalidParameterException if the given parentXPath is not valid
     * @throws FxEntryExistsException      if a property with this name already exists at the requested parentXPath
     * @throws FxNoAccessException         if the calling user is not permitted to call this method
     */
    long createProperty(long typeId, FxPropertyEdit property, String parentXPath) throws FxApplicationException;

    /**
     * Create a new property and assign it at the given parentXPath to the given type using assignmentAlias instead of
     * properties name.
     *
     * @param typeId          id of the type to assign this property to
     * @param property        property to create
     * @param parentXPath     the property's parent xpath
     * @param assignmentAlias alias to use for the assignment to the type
     * @return <b>assignment id</b> of the property
     * @throws FxApplicationException      on errors
     * @throws FxInvalidParameterException if the given parentXPath is not valid
     * @throws FxEntryExistsException      if a property with this name already exists at the requested parentXPath
     * @throws FxNoAccessException         if the calling user is not permitted to call this method
     */
    long createProperty(long typeId, FxPropertyEdit property, String parentXPath, String assignmentAlias) throws FxApplicationException;

    /**
     * Create a new group and assign it at the given parentXPath to the virtual ROOT_ID type
     *
     * @param group       the group to create
     * @param parentXPath optional parent xpath of the group
     * @return <b>assignment id</b> of the group
     * @throws FxApplicationException      on errors
     * @throws FxCreateException           for create errors
     * @throws FxInvalidParameterException if the given parentXPath is not valid
     * @throws FxEntryExistsException      if a group with this name already exists at the requested parentXPath
     * @throws FxNoAccessException         if the calling user is not permitted to call this method
     */
    long createGroup(FxGroupEdit group, String parentXPath) throws FxApplicationException;

    /**
     * Create a new group and assign it at the given parentXPath to the given type
     *
     * @param typeId      id of the type to assign this group to
     * @param group       the group to create
     * @param parentXPath optional parent xpath of the group
     * @return <b>assignment id</b> of the group
     * @throws FxApplicationException      on errors
     * @throws FxCreateException           for create errors
     * @throws FxInvalidParameterException if the given parentXPath is not valid
     * @throws FxEntryExistsException      if a group with this name already exists at the requested parentXPath
     * @throws FxNoAccessException         if the calling user is not permitted to call this method
     */
    long createGroup(long typeId, FxGroupEdit group, String parentXPath) throws FxApplicationException;

    /**
     * Save an existing or create a new assignment.
     *
     * @param assignment           instance of FxPropertyAssignmentEdit or FxGroupAssignmentEdit
     * @param createSubAssignments only used if creating a new group assignment
     * @return the id of the created or saved assignment
     * @throws FxApplicationException      on errors
     * @throws FxInvalidParameterException
     * @throws FxCreateException
     * @throws FxUpdateException
     * @throws FxNoAccessException         if the calling user is not permitted to call this method
     * @see com.flexive.shared.structure.FxPropertyAssignmentEdit
     * @see com.flexive.shared.structure.FxGroupAssignmentEdit
     */
    long save(FxAssignment assignment, boolean createSubAssignments) throws FxApplicationException;

    /**
     * Remove an assignment
     *
     * @param assignmentId             assignment to remove
     * @param removeSubAssignments     if assignment is a group, remove all attached properties and groups?
     * @param removeDerivedAssignments if derivates of this assignment in derived types exist, remove them as well?
     * @throws FxApplicationException on errors
     * @throws FxNotFoundException    if the assignmentId is invalid
     * @throws FxRemoveException      on remove errors
     * @throws FxNoAccessException    if the calling user is not permitted to call this method
     */
    void removeAssignment(long assignmentId, boolean removeSubAssignments, boolean removeDerivedAssignments)
            throws FxApplicationException;

    /**
     * Save an existing property.
     *
     * @param property instance of FxPropertyEdit
     * @return the id of the saved property
     * @throws FxApplicationException      on errors
     * @throws FxInvalidParameterException
     * @throws FxCreateException
     * @throws FxUpdateException
     * @throws FxNoAccessException         if the calling user is not permitted to call this method
     * @see com.flexive.shared.structure.FxPropertyEdit
     */
    long save(FxPropertyEdit property) throws FxApplicationException;

    /**
     * Save an existing group.
     *
     * @param group instance of FxGroupEdit
     * @return the id of the saved property
     * @throws FxApplicationException      on errors
     * @throws FxInvalidParameterException
     * @throws FxCreateException
     * @throws FxUpdateException
     * @throws FxNoAccessException         if the calling user is not permitted to call this method
     * @see com.flexive.shared.structure.FxGroupEdit
     */
    long save(FxGroupEdit group) throws FxApplicationException;

    /**
     * Get the number of content instances for a given assignment,
     * (works for group and property assignments)
     *
     * @param assignmentId id of the requested assignment
     * @return number of content instances using the assignment
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on errors
     */
    long getAssignmentInstanceCount(long assignmentId) throws FxApplicationException;

     /**
     * Get the number of existing content instances using a given property.
     *
     * @param propertyId id of the requested assignment
     * @return number of content instances using the assignment
     * @throws com.flexive.shared.exceptions.FxDbException on errors
     */
    long getPropertyInstanceCount(long propertyId) throws FxDbException;
}
