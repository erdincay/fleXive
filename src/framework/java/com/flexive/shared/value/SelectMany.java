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
        Collections.sort(selected, new FxSharedUtils.SelectableObjectSorter());
    }
}
