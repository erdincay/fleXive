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
package com.flexive.war.beans.admin.main;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.security.ACL;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListEdit;
import com.flexive.shared.structure.FxSelectListItemEdit;
import com.flexive.shared.value.FxString;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bean to display and edit FxSelectList objects and FxSelectListItem objects
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class SelectListBean {
    //used to filter out the select lists with id's 0 up to DELIMITER and prevent them from being edited
    private static final long SYSTEM_INTERNAL_LISTS_DELIMITER = 2;
    private FxSelectListEdit selectList = null;
    private long selectListId = -1;
    private String selectListName = "";
    private FxString selectListLabel = new FxString("");
    private FxString selectListDescription = new FxString("");
    private boolean selectListAllowDynamicCreation = true;
    private long selectListCreateItemACLId = ACL.Category.SELECTLIST.getDefaultId();
    private long selectListDefaultItemACLId = ACL.Category.SELECTLISTITEM.getDefaultId();

    private long listItemId = -1;
    private FxString itemLabel = new FxString("");
    private ACL itemACL = null;
    private String itemData = "";
    private String itemColor = "";

    /**
     * hack to generate unique id for the UI delete button, which can be used in java script
     *
     * @return unique id string for delete button
     */
    public Map<Long, String> getIdMap() {
        return FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Long, String>() {
            public String get(Object key) {
                long id = (Long) key;
                if (id >= 0)
                    return String.valueOf(id);
                else
                    return "_" + String.valueOf(id * -1);
            }
        });
    }

    public FxString getItemLabel() {
        return itemLabel;
    }

    public void setItemLabel(FxString itemLabel) {
        this.itemLabel = itemLabel;
    }

    public ACL getItemACL() {
        return itemACL;
    }

    public void setItemACL(ACL itemACL) {
        this.itemACL = itemACL;
    }

    public String getItemData() {
        return itemData;
    }

    public void setItemData(String itemData) {
        this.itemData = itemData;
    }

    public String getItemColor() {
        return itemColor;
    }

    public void setItemColor(String itemColor) {
        this.itemColor = itemColor;
    }

    public long getListItemId() {
        return listItemId;
    }

    public void setListItemId(long listItemId) {
        this.listItemId = listItemId;
    }

    public long getSelectListId() {
        return selectListId;
    }

    public void setSelectListId(long selectListId) {
        this.selectListId = selectListId;
    }

    public String getSelectListName() {
        return selectListName;
    }

    public void setSelectListName(String selectListName) {
        this.selectListName = selectListName;
    }

    public FxString getSelectListLabel() {
        return selectListLabel;
    }

    public void setSelectListLabel(FxString selectListLabel) {
        this.selectListLabel = selectListLabel;
    }

    public FxString getSelectListDescription() {
        return selectListDescription;
    }

    public void setSelectListDescription(FxString selectListDescription) {
        this.selectListDescription = selectListDescription;
    }

    public boolean isSelectListAllowDynamicCreation() {
        return selectListAllowDynamicCreation;
    }

    public void setSelectListAllowDynamicCreation(boolean selectListAllowDynamicCreation) {
        this.selectListAllowDynamicCreation = selectListAllowDynamicCreation;
    }

    public long getSelectListCreateItemACLId() {
        return selectListCreateItemACLId;
    }

    public void setSelectListCreateItemACLId(long selectListCreateItemACL) {
        this.selectListCreateItemACLId = selectListCreateItemACL;
    }

    public ACL getSelectListDefaultItemACL() {
        return CacheAdmin.getEnvironment().getACL(selectListDefaultItemACLId);
    }

    public void setSelectListDefaultItemACL(ACL selectListDefaultItemACL) {
        this.selectListDefaultItemACLId = selectListDefaultItemACL.getId();
        setItemACL(selectListDefaultItemACL);
    }

    public FxSelectListEdit getSelectList() {
        return selectList;
    }

    public List<FxSelectList> getSelectLists() {
        return doFilter(CacheAdmin.getEnvironment().getSelectLists());
    }

    private List<FxSelectList> doFilter(List<FxSelectList> selectLists) {
        List<FxSelectList> filtered = new ArrayList<FxSelectList>();
        for (FxSelectList s : selectLists) {
            if (s.getId() < 0 || s.getId() > SYSTEM_INTERNAL_LISTS_DELIMITER)
                filtered.add(s);
        }
        return filtered;
    }

    public String showSelectListOverview() {
        return "selectListOverview";
    }

    public String showCreateSelectList() {
        reset();
        return "createSelectList";
    }

    public String showEditSelectList() {
        return "editSelectList";
    }

    public void reset() {
        selectListName = "";
        selectListLabel = new FxString("");
        selectListDescription = new FxString("");
        selectListAllowDynamicCreation = true;
        selectListCreateItemACLId = ACL.Category.SELECTLIST.getDefaultId();
        selectListDefaultItemACLId = ACL.Category.SELECTLISTITEM.getDefaultId();
    }

    public String createSelectList() {
        try {
            selectListId = EJBLookup.getSelectListEngine().save(
                    FxSelectListEdit.createNew(selectListName, selectListLabel,
                            selectListDescription, selectListAllowDynamicCreation,
                            CacheAdmin.getEnvironment().getACL(selectListCreateItemACLId),
                            CacheAdmin.getEnvironment().getACL(selectListDefaultItemACLId))
            );
            reset();
            return initEditing();
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return null;
        }
    }

    public String initEditing() {
        selectList = CacheAdmin.getEnvironment().getSelectList(selectListId).asEditable();
        setSelectListAllowDynamicCreation(selectList.isAllowDynamicItemCreation());
        setSelectListDefaultItemACL(selectList.getNewItemACL());
        setSelectListCreateItemACLId(selectList.getCreateItemACL().getId());
        setSelectListDescription(selectList.getDescription());
        setSelectListLabel(selectList.getLabel());
        setSelectListName(selectList.getName());
        setItemACL(selectList.getNewItemACL());

        return showEditSelectList();
    }

    public void deleteSelectList() {
        try {
            EJBLookup.getSelectListEngine().remove(CacheAdmin.getEnvironment().getSelectList(selectListId));
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
        }
    }

    public void deleteListItem() {
        selectList.removeItem(listItemId);
    }

    public void addListItem() {
        new FxSelectListItemEdit(itemACL, selectList, itemLabel, itemData, itemColor);
        itemLabel = new FxString("");
        itemACL = selectList.getNewItemACL();
        itemData = "";
        itemColor = "";
    }

    public String saveSelectList() {
        try {
            EJBLookup.getSelectListEngine().save(selectList);
            reset();
            return showSelectListOverview();
        }
        catch (Throwable t) {
            new FxFacesMsgErr(t).addToContext();
            return null;
        }
    }


}
