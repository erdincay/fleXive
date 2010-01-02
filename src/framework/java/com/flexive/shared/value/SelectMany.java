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
package com.flexive.shared.value;

import com.flexive.shared.FxArrayUtils;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Container for manipulating FxSelectList items used in FxSelectMany values
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class SelectMany implements Serializable {
    private static final long serialVersionUID = -2077153739902083147L;
    private FxSelectList list;
    private List<FxSelectListItem> available, selected;

    /**
     * Constructor
     *
     * @param list the handled list
     */
    public SelectMany(FxSelectList list) {
        this.list = list;
        this.available = new ArrayList<FxSelectListItem>(list.getItems());
        this.selected = new ArrayList<FxSelectListItem>(available.size());
    }

    /**
     * Get all items available for selection (unmodifiable!)
     *
     * @return all items available for selection (unmodifiable!)
     */
    public List<FxSelectListItem> getAvailable() {
        return Collections.unmodifiableList(available);
    }

    /**
     * Get all items selected (unmodifiable!)
     *
     * @return all items selected (unmodifiable!)
     */
    public List<FxSelectListItem> getSelected() {
        return Collections.unmodifiableList(selected);
    }

    /**
     * Return the selected item ids.
     *
     * @return  the selected item ids.
     */
    public List<Long> getSelectedIds() {
        final List<Long> selectedIds = new ArrayList<Long>(selected.size());
        for (FxSelectListItem item: selected) {
            selectedIds.add(item.getId());
        }
        return selectedIds;
    }

    /**
     * Return the selected item labels.
     *
     * @return  the selected item labels.
     */
    public List<String> getSelectedLabels() {
        final List<String> selectedLabels = new ArrayList<String>(selected.size());
        for (FxSelectListItem item: selected) {
            selectedLabels.add(item.getLabel().getBestTranslation());
        }
        return selectedLabels;
    }

    /**
     * Select an item by its id, will throw an exception if item id does not belong to the managed list
     *
     * @param id item id to select
     * @return this
     */
    public SelectMany select(long id) {
        if( list != null )
            selectItem(list.getItem(id));
        return this;
    }

    /**
     * Select all items that are in the given comma separated list
     *
     * @param list items to select
     * @return this
     */
    public SelectMany selectFromList(String list) {
        try {
            for(long sel: FxArrayUtils.toLongArray(list,',') )
                select(sel);
        } catch (FxInvalidParameterException e) {
            //ignore
        }
        return this;
    }

    /**
     * Select an item
     *
     * @param item the item to select
     * @return this
     */
    public SelectMany selectItem(FxSelectListItem item) {
        if( list == null ||item == null )
            return this;
        if (!list.containsItem(item.getId())) {
            throw new FxNotFoundException("ex.content.value.selectMany.select", item.getId(), list.getId(),
                    list.getItems()).asRuntimeException();
        }
        //check if the item may be selected depending on list setting
        if(this.getSelectList().isOnlySameLevelSelect() && this.selected.size() > 0 ) {
            long firstParent = this.selected.get(0).getParentItem() == null ? 0 : this.selected.get(0).getParentItem().getId();
            long itemParent = item.getParentItem() == null ? 0 : item.getParentItem().getId();
            if(firstParent != itemParent)
                throw new FxInvalidParameterException(item.getLabelBreadcrumbPath(), "ex.selectlist.item.wrongParent", item.getLabelBreadcrumbPath()).asRuntimeException();
        }
        if( available.contains(item))
            available.remove(item);
        if( !selected.contains(item)) {
            selected.add(item);
            sortSelected();
        }
        return this;
    }

    /**
     * Deselect an item by its id, will throw an exception if item id does not belong to the managed list
     *
     * @param id item id to deselect
     * @return this
     */
    public SelectMany deselect(long id) {
        if( list != null )
            deselectItem(list.getItem(id));
        return this;
    }

    /**
     * Deselect an item
     *
     * @param item the item to deselect
     * @return this
     */
    public SelectMany deselectItem(FxSelectListItem item) {
        if( list == null || item == null || !list.containsItem(item.getId()) )
            return this;
        if( selected.contains(item))
            selected.remove(item);
        if( !available.contains(item))
            available.add(item);
        return this;
    }

    /**
     * Get this SelectMany's SelectList
     *
     * @return FxSelectList
     */
    public FxSelectList getSelectList() {
        return list;
    }

    /**
     * Updates the select list for this instance.
     *
     * @param list  the select list for this instance.
     */
    public void setSelectList(FxSelectList list) {
        this.list = list;
    }

    /**
     * Deselects all items
     */
    public void deselectAll() {
        selected.clear();
        available.clear();
        available.addAll(list.getItems());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        for(int i=0; i<selected.size(); i++ ) {
            if(i>0)
                sb.append(',');
            sb.append(selected.get(i).toString());
        }
        return sb.toString();
    }

    /**
     * Get an ordered comma separated list of selected id's
     *
     * @return ordered comma separated list of selected id's
     */
    public String getSelectedIdsList() {
        StringBuilder sb = new StringBuilder(100);
        for(int i=0; i<selected.size(); i++ ) {
            if(i>0)
                sb.append(',');
            sb.append(selected.get(i).getId());
        }
        return sb.toString();
    }

    private void sortSelected() {
        Collections.sort(selected, new FxSharedUtils.ItemPositionSorter());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SelectMany that = (SelectMany) o;

        if (!list.equals(that.list)) return false;
        if (!selected.equals(that.selected)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result;
        result = list.hashCode();
        result = 31 * result + selected.hashCode();
        return result;
    }
}
