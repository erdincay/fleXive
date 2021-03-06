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
package com.flexive.shared.structure;

import com.flexive.shared.*;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.LifeCycleInfo;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Items for select lists
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxSelectListItem implements Serializable, SelectableObjectWithLabel, SelectableObjectWithName, ObjectWithColor {
    private static final long serialVersionUID = -235396490388474264L;

    protected long id;
    protected ACL acl;
    protected FxSelectList list;
    protected FxSelectListItem parentItem;
    protected long parentItemId;
    protected String name;
    protected FxString label;
    protected String data;
    protected String color;
    protected long iconId;
    protected int iconVer;
    protected int iconQuality;
    protected int position;
    protected LifeCycleInfo lifeCycleInfo;

    public static final FxSelectListItem EMPTY = new FxSelectListItem(-1, "-", null, null, -1, new FxString("-"), "", "", -11, 1, 1, null, -1);

    /**
     * Internal(!) Constructor to be used while loading from storage
     *
     * @param id            id
     * @param name          name (unique within the list)
     * @param acl           the items acl
     * @param list          the list this item belongs to
     * @param parentItemId  parent item id if >= 0
     * @param label         this items label
     * @param data          optional data
     * @param color         color for display
     * @param iconId        id of icon (binary reference)
     * @param iconVer       version of icon (binary reference)
     * @param iconQuality   quality of icon (binary reference)
     * @param lifeCycleInfo life cycle
     * @param position      position within list
     */
    public FxSelectListItem(long id, String name, ACL acl, FxSelectList list, long parentItemId, FxString label, String data, String color, long iconId,
                            int iconVer, int iconQuality, LifeCycleInfo lifeCycleInfo, int position) {
        this.id = id;
        this.name = name;
        this.acl = acl;
        this.list = list;
        this.parentItemId = parentItemId;
        this.parentItem = null;
        this.label = label == null ? new FxString("") : label;
        this.data = data;
        if (this.data == null)
            this.data = "";
        this.color = color;
        this.iconId = iconId;
        this.iconVer = iconVer;
        this.iconQuality = iconQuality;
        this.lifeCycleInfo = lifeCycleInfo;
        this.position = position;
        if (list != null) {
            list.getItemMap().put(this.id, this);
            this.position = list.items.size();
        }
    }

    /**
     * Creates a new in-memory select list item. Not suitable for select lists that should
     * be stored in the database.
     *
     * @param id           the new select list item ID, possible some external ID
     * @param name         name (unique within the list)
     * @param list         the select list this item should be added to
     * @param parentItemId the parent item ID, if any
     * @param label        the select item label
     */
    public FxSelectListItem(long id, String name, FxSelectList list, long parentItemId, FxString label) {
        this(id, name, null, list, parentItemId, label, null, null, -1, -1, -1, null, -1);
    }

    /**
     * Internal method to synchronize/load parent items
     *
     * @param env environment
     */
    protected void _synchronize(FxEnvironment env) {
        try {
            if (this.parentItemId >= 0)
                this.parentItem = env.getSelectListItem(this.parentItemId);
        } catch (Exception e) {
            this.parentItemId = -1;
            this.parentItem = null;
        }
    }

    /**
     * Update this items list (internally used when creating new lists to have a valid id)
     *
     * @param list new list with a valid id
     */
    public void setList(FxSelectList list) {
        this.list = list;
    }

    /**
     * Get the internal id
     *
     * @return internal id
     */
    @Override
    public long getId() {
        return id;
    }

    /**
     * Get the name of the item.
     * The name is unique within the list and can be used as identifier
     *
     * @return name of the item
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the ACL for this item.
     * This ACL is treated in a special way:
     * The only relevant permissions are create and delete for adding and removing entries, all items are readable
     * and editable (to allow saving set items) if the create permission is set for the current user and are
     * deselectable if the user has the delete permission.
     *
     * @return ACL for this item
     */
    public ACL getAcl() {
        return acl;
    }

    /**
     * Is this item used as the "empty" item?
     *
     * @return if this is the "empty" item
     */
    public boolean isEmpty() {
        return list == null;
    }

    /**
     * Get the list this item belongs to
     *
     * @return list this item belongs to
     */
    public FxSelectList getList() {
        return list;
    }

    /**
     * Does a parent item exist for this item?
     *
     * @return parent item exists
     */
    public boolean hasParentItem() {
        return parentItem != null;
    }

    /**
     * Get the parent item of this item (check existance with hasParentItem() first!)
     *
     * @return parent item of this item
     */
    public FxSelectListItem getParentItem() {
        return parentItem;
    }

    /**
     * Are child items available for this list item?
     *
     * @return if child items exist
     */
    public boolean getHasChildren() {
        return list.getHasChildItems(this.getId());
    }

    /**
     * Return all child items for this list item
     *
     * @return all child items for this list item
     */
    public List<FxSelectListItem> getChildren() {
        return list.getChildItems(this.getId());
    }

    /**
     * Get the number of children of this item
     *
     * @return number of children of this item
     */
    public int getChildCount() {
        if (!getHasChildren())
            return 0;
        return getChildren().size();
    }

    /**
     * Get label
     *
     * @return label
     */
    @Override
    public FxString getLabel() {
        return label;
    }

    /**
     * Get optional data assigned to this item
     *
     * @return optional data assigned to this item
     */
    public String getData() {
        return data;
    }

    /**
     * Get the color to display for this item
     *
     * @return color to display for this item
     */
    @Override
    public String getColor() {
        return color;
    }

    /**
     * Id of icon used for user interfaces (reference to binaries, internal field!)
     *
     * @return id of icon used for user interfaces (reference to binaries, internal field!)
     */
    public long getIconId() {
        return iconId;
    }

    /**
     * Version of icon used for user interfaces (reference to binaries, internal field!)
     *
     * @return version of icon used for user interfaces (reference to binaries, internal field!)
     */
    public int getIconVer() {
        return iconVer;
    }

    /**
     * Quality of icon used for user interfaces (reference to binaries, internal field!)
     *
     * @return quality of icon used for user interfaces (reference to binaries, internal field!)
     */
    public int getIconQuality() {
        return iconQuality;
    }

    /**
     * Get this items lifecycle info
     *
     * @return lifecycle info
     */
    public LifeCycleInfo getLifeCycleInfo() {
        return lifeCycleInfo;
    }

    /**
     * Get the position within the list
     *
     * @return position within the list
     */
    public int getPosition() {
        return position;
    }

    /**
     * Set the position within the list
     * Only to be called from the list itself
     *
     * @param position new position
     */
    protected void setPosition(int position) {
        this.position = position;
    }

    /**
     * Get this FxSelectListItem as editable
     *
     * @return FxSelectListItemEdit
     */
    public FxSelectListItemEdit asEditable() {
        if (this instanceof FxSelectListItemEdit)
            return (FxSelectListItemEdit) this;
        return new FxSelectListItemEdit(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        if (getName() != null)
            return getName();
        else
            return super.toString();
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return (int) id;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        return obj instanceof FxSelectListItem && this.id == ((FxSelectListItem) obj).id;
    }

    /**
     * If this item is cascaded, get the label path up to the root element
     *
     * @return label path
     */
    public String getLabelBreadcrumbPath() {
        final UserTicket ticket = FxContext.getUserTicket();
        if (!hasParentItem())
            return getLabel().getBestTranslation(ticket);
        StringBuilder sb = new StringBuilder(100);
        FxSelectListItem curr = this;
        while (curr != null) {
            if (curr != this)
                sb.insert(0, getList().getBreadcrumbSeparator());
            sb.insert(0, curr.getLabel().getBestTranslation(ticket));
            curr = curr.getParentItem();
        }
        return sb.toString();
    }

    /**
     * If this item is cascaded, get the label path up to the root element
     *
     * @param outputLanguage desired output language
     * @return label path
     */
    public String getLabelBreadcrumbPath(FxLanguage outputLanguage) {
        if (!hasParentItem())
            return getLabel().getBestTranslation(outputLanguage);
        StringBuilder sb = new StringBuilder(100);
        FxSelectListItem curr = this;
        while (curr != null) {
            if (curr != this)
                sb.insert(0, getList().getBreadcrumbSeparator());
            sb.insert(0, curr.getLabel().getBestTranslation(outputLanguage));
            curr = curr.getParentItem();
        }
        return sb.toString();
    }

    /**
     * checks if the current listItem is valid
     * @return <code>true</code> if the name is valid
     */
    public boolean isValidName() {
        return (!StringUtils.isEmpty(name) && name.indexOf(',') < 0);
    }

    /**
     * checks if the current listItem is valid
     * @return <code>true</code> if the label is valid
     */
    public boolean isValidLabel() {
        return label != null && !label.getIsEmpty();
    }
}
