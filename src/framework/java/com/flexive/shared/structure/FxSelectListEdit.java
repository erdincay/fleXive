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
package com.flexive.shared.structure;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.SelectableObjectWithLabel;
import com.flexive.shared.SelectableObjectWithName;
import com.flexive.shared.security.ACL;
import com.flexive.shared.value.FxString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Editable select list
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxSelectListEdit extends FxSelectList implements Serializable {

    private static final long serialVersionUID = 8842659204852529463L;

    /**
     * is this a new list?
     */
    private boolean isNew;
    private FxSelectList original;

    /**
     * Constructor
     *
     * @param list the list to make editable
     */
    public FxSelectListEdit(FxSelectList list) {
        super(list.id, list.parentListId, "" + list.name, list.label.copy(), list.description.copy(),
                list.allowDynamicItemCreation, list.createItemACL, list.newItemACL,
                list.hasDefaultItem() ? list.getDefaultItem().getId() : 0);
        this.isNew = false;
        this.original = list;
        this.parentList = list.parentList;
        for (FxSelectListItem i : list.getItems()) {
            new FxSelectListItemEdit(i, this);
        }
    }

    /**
     * Constructor for creating a new list
     *
     * @param parent                   parent list if not null
     * @param name                     name - has to be unique!
     * @param label                    label
     * @param description              description
     * @param allowDynamicItemCreation is dynamic item creation allowed?
     * @param createItemACL            ACL whose create flag is checked against for creating new list items
     * @param newItemACL               ACL assigned to new items
     * @param defaultItem              the default selected item, optional, can be <code>null</code>
     */
    public FxSelectListEdit(FxSelectList parent, String name, FxString label, FxString description,
                            boolean allowDynamicItemCreation, ACL createItemACL, ACL newItemACL,
                            FxSelectListItem defaultItem) {
        super(-1, parent != null ? parent.getId() : -1, name, label, description,
                allowDynamicItemCreation, createItemACL, newItemACL,
                defaultItem != null ? defaultItem.getId() : 0);
        if (parent != null || defaultItem != null)
            super._synchronize(CacheAdmin.getEnvironment());
        this.isNew = true;
    }

    /**
     * Is this a new list?
     *
     * @return is a new list?
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * Have any changes been made?
     *
     * @return changes made?
     */
    public boolean changes() {
        return isNew || original == null || !original.name.equals(this.name) ||
                original.parentListId != this.parentListId || !original.label.equals(this.label) ||
                original.createItemACL.getId() != this.createItemACL.getId() ||
                original.newItemACL.getId() != this.newItemACL.getId() ||
                original.allowDynamicItemCreation != this.allowDynamicItemCreation ||
                !original.description.equals(this.description);
    }

    /**
     * Set the name - has to be unique!
     *
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
        if (this.name == null)
            this.name = "";
    }

    /**
     * Set the label
     *
     * @param label label
     */
    public void setLabel(FxString label) {
        this.label = label;
        if (this.label == null)
            this.label = new FxString("");
    }

    /**
     * Set the description
     *
     * @param description description
     */
    public void setDescription(FxString description) {
        this.description = description;
        if (this.description == null)
            this.description = new FxString("");
    }

    /**
     * Set the create item ACL
     *
     * @param createItemACL create item ACL
     */
    public void setCreateItemACL(ACL createItemACL) {
        this.createItemACL = createItemACL;
    }

    /**
     * Set the new item ACL
     *
     * @param newItemACL new item ACL
     */
    public void setNewItemACL(ACL newItemACL) {
        this.newItemACL = newItemACL;
    }

    /**
     * Set a new default item, setting <code>null</code> will clear the default item
     *
     * @param defaultItem a new default item, setting <code>null</code> will clear the default item
     * @return this
     */
    public FxSelectListEdit setDefaultItem(FxSelectListItem defaultItem) {
        _setDefaultItem(defaultItem);
        return this;
    }

    /**
     * Set if items may be created dynamically (in UI's other than backends!)
     *
     * @param allowDynamicItemCreation items may be created dynamically
     */
    public void setAllowDynamicItemCreation(boolean allowDynamicItemCreation) {
        this.allowDynamicItemCreation = allowDynamicItemCreation;
    }

    /**
     * returns all list items as editable list items.
     *
     * @return a list of editable list items.
     */
    public List<FxSelectListItemEdit> getEditableItems() {
        List<FxSelectListItemEdit> editableItems = new ArrayList<FxSelectListItemEdit>();
        for (Long l : items.keySet()) {
            FxSelectListItem i = items.get(l);
            if (i instanceof FxSelectListItemEdit)
                editableItems.add((FxSelectListItemEdit) i);
            else
                editableItems.add(new FxSelectListItemEdit(i));
        }
        return editableItems;
    }

    /**
     * adds a new item to the select list.
     *
     * @param item the item to add to the select list
     */

    public void addItem(FxSelectListItem item) {
        new FxSelectListItemEdit(item.name, item.acl, this, item.label, item.data, item.color);
    }

    /**
     * Removes the item with Id from the items map.
     *
     * @param id id of the item to remove
     */

    public void removeItem(Long id) {
        this.items.remove(id);
    }

    /**
     * Adds all given objects to this select list.
     *
     * @param items the objects to be added
     */
    public void addAll(List<? extends SelectableObjectWithLabel> items) {
        for (SelectableObjectWithLabel item : items) {
            new FxSelectListItem(item.getId(), item.getLabel().getDefaultTranslation(), this, -1, item.getLabel());
        }
    }

    /**
     * Adds all given objects to this select list.
     *
     * @param items the objects to be added
     */
    public void addAllWithName(List<? extends SelectableObjectWithName> items) {
        for (SelectableObjectWithName item : items) {
            new FxSelectListItem(item.getId(), null, this, -1, new FxString(false, item.getName()));
        }
    }

    /**
     * Assign an id for a new created list to be synchronized with items
     *
     * @param newId new list id
     */
    public void _synchronizeId(long newId) {
        if (!isNew)
            return;
        this.id = newId;
        for (FxSelectListItem item : this.getItems())
            item.setList(this);
    }

    /**
     * Replace an item contained in this lists items with the passed item
     *
     * @param itemId item id to reset
     * @param item   the item to set
     */
    public void replaceItem(Long itemId, FxSelectListItem item) {
        if (!this.items.containsKey(itemId))
            return;
        this.items.put(itemId, item);
    }

    /**
     * Helper method to create a new FxSelectListEdit instance
     *
     * @param name                     name - has to be unique!
     * @param label                    label
     * @param description              description
     * @param allowDynamicItemCreation is dynamic item creation allowed?
     * @param createItemACL            ACL whose create flag is checked against for creating new list items
     * @param defaultItemACL           ACL assigned to new items
     * @return FxSelectListEdit
     */
    public static FxSelectListEdit createNew(String name, FxString label, FxString description,
                                             boolean allowDynamicItemCreation, ACL createItemACL, ACL defaultItemACL) {
        return new FxSelectListEdit(null, name, label, description, allowDynamicItemCreation, createItemACL,
                defaultItemACL, null);
    }

    /**
     * Helper method to create a new FxSelectListEdit instance
     *
     * @param parent                   parent list if not null
     * @param name                     name - has to be unique!
     * @param label                    label
     * @param description              description
     * @param allowDynamicItemCreation is dynamic item creation allowed?
     * @param createItemACL            ACL whose create flag is checked against for creating new list items
     * @param defaultItemACL           ACL assigned to new items
     * @return FxSelectListEdit
     */
    public static FxSelectListEdit createNew(FxSelectList parent, String name, FxString label, FxString description,
                                             boolean allowDynamicItemCreation, ACL createItemACL, ACL defaultItemACL) {
        return new FxSelectListEdit(parent, name, label, description, allowDynamicItemCreation, createItemACL,
                defaultItemACL, null);
    }
}
