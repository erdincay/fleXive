/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
package com.flexive.shared.structure;

import com.flexive.shared.ObjectWithLabel;
import com.flexive.shared.SelectableObjectWithLabel;
import com.flexive.shared.SelectableObjectWithName;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.value.FxString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Select list
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxSelectList implements Serializable, ObjectWithLabel {
    private static final long serialVersionUID = 7886154346159624577L;

    /**
     * A select list containing all countries and their codes.
     */
    public static final String COUNTRIES = "COUNTRIES";

    protected long id;
    protected FxSelectList parentList;
    protected long parentListId;
    protected String name;
    protected FxString label;
    protected FxString description;
    protected boolean allowDynamicItemCreation;
    protected ACL createItemACL;
    protected ACL newItemACL;
    protected Map<Long, FxSelectListItem> items;
    protected FxSelectListItem defaultItem;
    protected long defaultItemId;

    /**
     * Internal(!) Constructor to be used while loading from storage
     *
     * @param id                       internal id
     * @param parentListId             parent list if >= 0
     * @param name                     name (unique)
     * @param label                    label
     * @param description              description
     * @param allowDynamicItemCreation is dynamic item creation allowed?
     * @param createItemACL            ACL whose create flag is checked against for creating new list items
     * @param newItemACL               ACL assigned to new items
     * @param defaultItemId            the id of the default selected item, optional, can be <code>null</code>
     */
    public FxSelectList(long id, long parentListId, String name, FxString label, FxString description,
                        boolean allowDynamicItemCreation, ACL createItemACL, ACL newItemACL,
                        long defaultItemId) {
        this.id = id;
        this.parentListId = parentListId;
        this.parentList = null;
        this.name = name;
        this.label = label;
        this.description = description;
        this.allowDynamicItemCreation = allowDynamicItemCreation;
        this.createItemACL = createItemACL;
        this.newItemACL = newItemACL;
        this.defaultItemId = defaultItemId;
        this.defaultItem = null;
        this.items = new HashMap<Long, FxSelectListItem>(10);
    }

    /**
     * Create a new in-memory select list. Not suitable for creating select lists
     * that will be stored in the database!
     *
     * @param name the select list name
     */
    public FxSelectList(String name) {
        this(-1, -1, name, new FxString(name), new FxString(name), true, null, null, 0);
    }

    /**
     * Internal method to synchronize/load parent lists and items
     *
     * @param env environment
     */
    public void _synchronize(FxEnvironment env) {
        if (this.defaultItemId > 0) {
            this.defaultItem = env.getSelectListItem(defaultItemId);
        }
        try {
            if (this.parentListId >= 0) {
                this.parentList = env.getSelectList(this.parentListId);
            } else
                return;
        } catch (Exception e) {
            this.parentListId = -1;
            return;
        }
        for (FxSelectListItem item : this.getItems())
            item._synchronize(env);
    }

    /**
     * Get the internal id
     *
     * @return internal id
     */
    public long getId() {
        return id;
    }


    /**
     * Get the parent list
     *
     * @return parent list
     */
    public FxSelectList getParentList() {
        return parentList;
    }

    /**
     * Is a parent list set for this list?
     *
     * @return if a parent list is set for this list
     */
    public boolean hasParentList() {
        return parentList != null;
    }

    /**
     * Get the (optional!) default item for this list
     *
     * @return the (optional!) default item for this list, can be <code>null</code>
     */
    public FxSelectListItem getDefaultItem() {
        return defaultItem;
    }

    /**
     * Is a default item set for this list?
     *
     * @return if a default item is set for this list
     */
    public boolean hasDefaultItem() {
        return defaultItem != null;
    }

    /**
     * Get the name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Get the label
     *
     * @return label
     */
    public FxString getLabel() {
        return label;
    }

    /**
     * Get the description
     *
     * @return description
     */
    public FxString getDescription() {
        return description;
    }

    /**
     * May items be created dynamically? (in UI's other than backends!)
     *
     * @return if items may be created dynamically
     */
    public boolean isAllowDynamicItemCreation() {
        return allowDynamicItemCreation;
    }

    /**
     * Get the ACL used to check if a user is allowed to create new items for this list dynamically
     *
     * @return ACL used to check if a user is allowed to create new items for this list dynamically
     */
    public ACL getCreateItemACL() {
        return createItemACL;
    }

    /**
     * Get the default ACL assigned to newly created items
     *
     * @return default ACL assigned to newly created items
     */
    public ACL getNewItemACL() {
        return newItemACL;
    }

    /**
     * Get the selectlist item with the given id
     *
     * @param id requested select list item id
     * @return select list item
     */
    public FxSelectListItem getItem(long id) {
        if (this.containsItem(id))
            return this.items.get(id);
        throw new FxNotFoundException("ex.structure.list.item.notFound", id).asRuntimeException();
    }

    /**
     * Get the selectlist item with the given id
     *
     * @param name requested select list item name
     * @return select list item
     */
    public FxSelectListItem getItem(String name) {
        for (FxSelectListItem item : items.values())
            if (item.getName().equals(name))
                return item;
        throw new FxNotFoundException("ex.structure.list.item.notFound.name", name).asRuntimeException();
    }

    /**
     * Get the (first) selectlist item with the given data
     *
     * @param data requested select list item data
     * @return select list item
     */
    public FxSelectListItem getItemByData(String data) {
        for (FxSelectListItem item : items.values())
            if (item.getData() != null && item.getData().equals(data))
                return item;
        throw new FxNotFoundException("ex.structure.list.item.notFound.data", data).asRuntimeException();
    }

    /**
     * Getter for the map containing items. Used by FxSelectListItem to "add" itself to a list during construction.
     *
     * @return map of all items of this list
     */
    protected final Map<Long, FxSelectListItem> getItemMap() {
        return items;
    }

    public final List<FxSelectListItem> getItems() {
        return new ArrayList<FxSelectListItem>(items.values());
    }

    /**
     * Get this FxSelectList as editable
     *
     * @return FxSelectListEdit
     */
    public FxSelectListEdit asEditable() {
        return new FxSelectListEdit(this);
    }

    /**
     * Check if this list contains an selectlist item with the requested id
     *
     * @param id requested item id
     * @return if the requested item is contained in this list
     */
    public boolean containsItem(long id) {
        return this.items.containsKey(id);
    }

    /**
     * Check if this list contains an selectlist item with the requested name
     *
     * @param name requested item name
     * @return if the requested item is contained in this list
     */
    public boolean containsItem(String name) {
        for (FxSelectListItem item : items.values())
            if (item.getName().equals(name))
                return true;
        return false;
    }

    /**
     * Factory method to create a select list based on a collection of
     * {@link SelectableObjectWithLabel} objects. This select list cannot be persisted
     * to the DB, but may used e.g. for UI input components.
     *
     * @param name  name of the select list to be created
     * @param items selectable objects with label
     * @return a new select list
     */
    public static FxSelectList createList(String name, List<? extends SelectableObjectWithLabel> items) {
        FxSelectListEdit list = new FxSelectList(name).asEditable();
        list.addAll(items);
        return list;
    }

    /**
     * Factory method to create a select list based on a collection of
     * {@link com.flexive.shared.SelectableObjectWithName} objects. This select list cannot be persisted
     * to the DB, but may used e.g. for UI input components.
     *
     * @param name  name of the select list to be created
     * @param items selectable objects
     * @return a new select list
     */
    public static FxSelectList createListWithName(String name, List<? extends SelectableObjectWithName> items) {
        FxSelectListEdit list = new FxSelectList(name).asEditable();
        list.addAllWithName(items);
        return list;
    }

    /**
     * Set a new default item, setting <code>null</code> will clear the default item
     * To be used internally from FxSelectListEdit only(!)
     *
     * @param defaultItem a new default item, setting <code>null</code> will clear the default item
     */
    protected void _setDefaultItem(FxSelectListItem defaultItem) {
        if (defaultItem == null) {
            this.defaultItem = null;
            this.defaultItemId = 0;
            return;
        }
        if (defaultItem.getList().getId() != this.getId())
            throw new FxInvalidParameterException("defaultItem", "ex.structure.list.item.invalidDefaultItem", this.getId(), this.defaultItem.getId()).asRuntimeException();
        this.defaultItem = defaultItem;
    }
}

