/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.Briefcase;

import javax.ejb.Remote;
import java.util.List;

/**
 * Bean handling Briefcases.
 * <p />
 * A briefcase is a object store which may be accessed with flexive SQL
 * or the API provided by this beans.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface BriefcaseEngine {

    /**
     * Creates a new empty briefcase.
     *
     * @param name the name of the briefcase.
     *
     * @param description the description (may be empty)
     * @param aclId the ACL to use if the briefcase should be shared, or null if the briefcase is
     * only visible for the owner.
     * @return the unique briefcase id
     * @throws FxApplicationException if the create failed
     */
    long create(String name, String description,Long aclId) throws FxApplicationException;

    /**
     * Gets a list of all briefcase for the calling user.
     *
     * @param includeShared if enabled shared briefcases will be included, if disabled only
     * the briefcases created by the calling user will be returned
     * @return the briefcases
     * @throws FxApplicationException if the function fails
     */
    List<Briefcase> getList(boolean includeShared) throws FxApplicationException;

    /**
     * Loads the briefcase with the given id.
     *
     * @param id the id
     * @return the briefcase
     * @throws FxApplicationException if a error occured
     * @throws com.flexive.shared.exceptions.FxNotFoundException if it could not be found or was not readable by the calling user.
     */
    Briefcase load(long id) throws FxApplicationException;


    /**
     * Removes the briefcase with the specified id.
     * <p />
     * The calling user may remove a briefcase if he is the owner, or if he has ben granted the remove
     * permission via the briefcases ACL.<br>
     * A global supervisor may remove and briefcase.<br>
     * A mandator supervisor may remove all briefcases that belong to his domain.<br>
     *
     * @param id the id
     * @throws FxApplicationException if the calling user lacks the remove permission and is not the owner,
     * or if a other error occured.
     */
    void remove(long id) throws FxApplicationException;


    /**
     * Modifies a briefcase.
     *
     * @param name the new name of the briefcase, or null to keep the old value
     * @param description the new description, or null to keep the old value
     * @param aclId the new ACL, or -1 to remove a old acl, or null to keep the old value
     * @param id the id of the briefcase to modify
     * @throws FxApplicationException if the modify operation failed
     */
    void modify(long id,String name, String description,Long aclId) throws FxApplicationException;

    /**
     * Removes all items of the given briefcase.
     *
     * @param id    the briefcase ID
     * @throws FxApplicationException   if the briefcase could not be cleared
     */
    void clear(long id) throws FxApplicationException;

    /**
     * Add the given items to the briefcase.
     *
     * @param id    the briefcase ID
     * @param objectIds the object IDs to be added
     * @throws FxApplicationException   if the items could not be added
     */
    void addItems(long id, long[] objectIds) throws FxApplicationException;

    /**
     * Replace the current briefcase content with the given objects (i.e. clear the
     * briefcase, then add the given objects).
     *
     * @param id    the briefcase ID
     * @param objectIds the new briefcase content
     * @throws FxApplicationException   if the content could not be replaced
     */
    void setItems(long id, long[] objectIds) throws FxApplicationException;

    /**
     * Adds/removes the given items for the briefcase.
     *
     * @param id    the briefcase ID
     * @param addObjectIds  items to be added
     * @param removeObjectIds   items to be removed
     * @throws FxApplicationException if the briefcase could not be updated
     */
    void updateItems(long id, long[] addObjectIds, long[] removeObjectIds) throws FxApplicationException;

    /**
     * Removes the given items from the briefcase.
     *
     * @param id    the briefcase ID
     * @param objectIds the objects to be removed
     * @throws FxApplicationException   if the items could not be removed
     */
    void removeItems(long id, long[] objectIds) throws FxApplicationException;

    /**
     * Load all item IDs stored in the given briefcase.
     *
     * @param id    the briefcase ID
     * @return  the item IDs
     * @throws FxApplicationException   if the items could not be loaded
     */
    long[] getItems(long id) throws FxApplicationException;
}
