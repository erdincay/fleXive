/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.Briefcase;
import com.flexive.shared.FxReferenceMetaData;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.search.BriefcaseItemData;
import com.flexive.shared.security.LifeCycleInfo;

import javax.ejb.Remote;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * Bean handling Briefcases.
 * <p />
 * A briefcase is a object store which may be accessed with FxSQL or the API provided by this bean.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
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
    long create(String name, String description, Long aclId) throws FxApplicationException;

    /**
     * Create a new empty briefcase, overriding the default lifecycle info (e.g. for imports).
     * The lifecycle info may only be specified when the calling user is a global supervisor.
     * 
     * @param description the description (may be empty)
     * @param aclId the ACL to use if the briefcase should be shared, or null if the briefcase is only visible for the
     * owner.
     * @param forcedLifeCycleInfo the lifecycle info to store for the briefcase, or null if the default values should be used
     * @return the unique briefcase id
     * @throws FxApplicationException if the create failed
     * @since 3.1.7
     */
    long create(String name, String description, Long aclId, LifeCycleInfo forcedLifeCycleInfo) throws FxApplicationException;
    
    /**
     * Gets a list of all briefcase for the calling user.
     *
     * @param includeShared if enabled shared briefcases will be included, if disabled only
     * the briefcases created by the calling user will be returned
     * @return the briefcases
     * @throws FxApplicationException if the function fails
     */
    List<Briefcase> loadAll(boolean includeShared) throws FxApplicationException;

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
     * @deprecated use {@link #addItems(long, java.util.Collection)}
     */
    @Deprecated
    void addItems(long id, long[] objectIds) throws FxApplicationException;

    /**
     * Add the given items to the briefcase.
     *
     * @param id    the briefcase ID
     * @param contents the content instances to be added
     * @throws FxApplicationException   if the items could not be added
     * @since 3.1
     */
    void addItems(long id, Collection<FxPK> contents) throws FxApplicationException;

    /**
     * Replace the current briefcase content with the given objects (i.e. clear the
     * briefcase, then add the given objects).
     *
     * @param id    the briefcase ID
     * @param objectIds the new briefcase content
     * @throws FxApplicationException   if the content could not be replaced
     * @deprecated use {@link #setItems(long, java.util.Collection)}
     */
    @Deprecated
    void setItems(long id, long[] objectIds) throws FxApplicationException;

    /**
     * Replace the current briefcase content with the given objects (i.e. clear the
     * briefcase, then add the given objects).
     *
     * @param id    the briefcase ID
     * @param contents the new briefcase content
     * @throws FxApplicationException   if the content could not be replaced
     * @since 3.1
     */
    void setItems(long id, Collection<FxPK> contents) throws FxApplicationException;

    /**
     * Adds/removes the given items for the briefcase.
     *
     * @param id    the briefcase ID
     * @param addObjectIds  items to be added
     * @param removeObjectIds   items to be removed
     * @throws FxApplicationException if the briefcase could not be updated
     * @deprecated use {@link #updateItems(long, java.util.Collection, java.util.Collection)}
     */
    @Deprecated
    void updateItems(long id, long[] addObjectIds, long[] removeObjectIds) throws FxApplicationException;

    /**
     * Adds/removes the given items for the briefcase.
     *
     * @param id    the briefcase ID
     * @param addContents  items to be added
     * @param removeContents   items to be removed
     * @throws FxApplicationException if the briefcase could not be updated
     * @since 3.1
     */
    void updateItems(long id, Collection<FxPK> addContents, Collection<FxPK> removeContents) throws FxApplicationException;

    /**
     * Replaces the current metadata for the given items.
     * <p>
     * Since metadata on a briefcase is typically shared by more than one application (or plugin),
     * this method should be used carefully. For everyday purposes, {@link #mergeMetaData(long, java.util.Collection)}
     * is considered to be a better alternative.
     * </p>
     *
     * @param id    the briefcase ID
     * @param metaData  the metadata updates (item IDs are stored in the metadata instances themselves)
     * @throws FxApplicationException   if the metadata could not be updated
     * @since 3.1
     */
    void setMetaData(long id, Collection<FxReferenceMetaData<FxPK>> metaData) throws FxApplicationException;

    /**
     * Merges the given metadata fields into the existing metadata stored in the items. Existing
     * values get overwritten. To remove a value, specify its key with a null or empty string.
     * <p>
     * This method should be used whenever it is not necessary to control the entire metadata associated to
     * a briefcase item, since it provides transactional safety for concurrent modifications of briefcase
     * items.
     * </p>
     *
     * @param id    the briefcase ID
     * @param metaData  the metadata updates (item IDs are stored in the metadata instances themselves)
     * @throws FxApplicationException   if the metadata could not be updated
     * @since 3.1
     */
    void mergeMetaData(long id, Collection<FxReferenceMetaData<FxPK>> metaData) throws FxApplicationException;

    /**
     * Loads the metadata instance(s) associated to the briefcase items. If an item has no metadata
     * stored, it is not returned by this method.
     *
     * @param briefcaseId    the briefcase ID
     * @return      the metadata instances
     * @throws FxApplicationException   if the metadata could not be retrieved
     * @since 3.1
     */
    List<FxReferenceMetaData<FxPK>> getMetaData(long briefcaseId) throws FxApplicationException;

    /**
     * Loads the metadata instance(s) associated to given briefcase item. If an item has no metadata
     * stored, or no item with the given ID was found, null is returned.
     *
     * @param briefcaseId    the briefcase ID
     * @param pk             the item PK
     * @return      the metadata instances
     * @throws FxApplicationException   if the metadata could not be retrieved
     * @since 3.1
     */
    FxReferenceMetaData<FxPK> getMetaData(long briefcaseId, FxPK pk) throws FxApplicationException;

    /**
     * Removes the given items from the briefcase.
     *
     * @param id    the briefcase ID
     * @param objectIds the objects to be removed
     * @throws FxApplicationException   if the items could not be removed
     * @deprecated use {@link #removeItems(long, java.util.Collection)}
     */
    @Deprecated
    void removeItems(long id, long[] objectIds) throws FxApplicationException;

    /**
     * Removes the given items from the briefcase.
     *
     * @param id    the briefcase ID
     * @param contents the objects to be removed
     * @throws FxApplicationException   if the items could not be removed
     * @since 3.1
     */
    void removeItems(long id, Collection<FxPK> contents) throws FxApplicationException;

    /**
     * Load all item IDs stored in the given briefcase.
     *
     * @param id    the briefcase ID
     * @return  the item IDs
     * @throws FxApplicationException   if the items could not be loaded
     */
    long[] getItems(long id) throws FxApplicationException;

    /**
     * Moves items from one briefcase to another. Item data will not be moved!
     *
     * @param fromId    the source briefcase ID
     * @param toId      the target briefcase ID
     * @param contents  the items to be moved
     * @since 3.1
     */
    void moveItems(long fromId, long toId, Collection<FxPK> contents) throws FxApplicationException;

    /**
     * Add a single item data for a briefcase item
     *
     * @param briefcaseId id of the briefcase
     * @param itemData    item data
     * @throws FxApplicationException on errors
     * @since 3.1.7
     */
    void addItemData(long briefcaseId, BriefcaseItemData itemData) throws FxApplicationException;

    /**
     * Add multiple item datas for a briefcase
     *
     * @param briefcaseId id of the briefcase
     * @param itemDatas   item datas
     * @throws FxApplicationException on errors
     * @since 3.1.7
     */
    void addItemData(long briefcaseId, List<BriefcaseItemData> itemDatas) throws FxApplicationException;

    /**
     * Remove item datas
     *
     * @param briefcaseId id of the briefcase
     * @param itemId      item id or if <code>null</code> for all items
     * @throws FxApplicationException on errors
     */
    void removeItemData(long briefcaseId, Long itemId) throws FxApplicationException;

    /**
     * Load matching item datas (non-null flags are queried)
     *
     * @param briefcaseId id of the briefcase
     * @param itemId      item id
     * @param metaData    meta data
     * @param intFlag1   integer flag 1
     * @param intFlag2   integer flag 2
     * @param intFlag3   integer flag 3
     * @param longFlag1  long flag 1
     * @param longFlag2  long flag 2
     * @param sortField   sort field
     * @param sortOrder   sort order
     * @return list of matching item datas
     * @throws FxApplicationException on errors
     * @since 3.1.7
     */
    List<BriefcaseItemData> queryItemData(long briefcaseId, Long itemId, String metaData,
                                          Integer intFlag1, Integer intFlag2, Integer intFlag3,
                                          Long longFlag1, Long longFlag2,
                                          BriefcaseItemData.SortField sortField, BriefcaseItemData.SortOrder sortOrder) throws FxApplicationException;

    /**
     * Evaluate the count of matching item datas (non-null flags are queried)
     *
     * @param briefcaseId id of the briefcase
     * @param itemId      item id
     * @param metaData    meta data
     * @param intFlag1   integer flag 1
     * @param intFlag2   integer flag 2
     * @param intFlag3   integer flag 3
     * @param longFlag1  long flag 1
     * @param longFlag2  long flag 2
     * @return number of matching item datas
     * @throws FxApplicationException on errors
     * @since 3.1.7
     */
    int queryItemDataCount(long briefcaseId, Long itemId, String metaData,
                           Integer intFlag1, Integer intFlag2, Integer intFlag3,
                           Long longFlag1, Long longFlag2) throws FxApplicationException;

    /**
     * Update a data item (except the position). The <code>updateItem</code> must contain a valid briefcase and object/item id
     *
     * @param briefcaseId id of the briefcase
     * @param updateData  data to update
     * @throws FxApplicationException on errors
     * @since 3.1.7
     */
    void updateItemData(long briefcaseId, BriefcaseItemData updateData) throws FxApplicationException;
}
