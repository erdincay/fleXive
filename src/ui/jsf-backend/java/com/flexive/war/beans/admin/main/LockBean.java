/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.war.beans.admin.main;

import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLockException;
import com.flexive.shared.value.FxString;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.beans.MessageBean;
import com.flexive.faces.model.FxJSFSelectItem;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * Lock administration bean
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class LockBean implements Serializable {
    private static final long serialVersionUID = 6097768905804314932L;

    private List<FxLock> results;
    private List<SelectItem> availableLockTypes = null;
    private FxString accountMatch = new FxString(false, "");
    private String type;
    private int maxEntries = 100;
    private FxLockType lockType;
    private Map<Object, String> dateMap;
    private FxLock selectedLock;

    /**
     * Mapped function to return the formatted date of a lock entry
     *
     * @return formatted date of a lock entry
     */
    public Map<Object, String> getDateFormat() {
        if (dateMap == null) {
            dateMap = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Object, String>() {
                public String get(Object key) {
                    try {
                        return FxFormatUtils.getUniversalDateTimeFormat().format(key);
                    } catch (Exception e) {
                        return "unknown";
                    }
                }
            });
        }
        return dateMap;
    }

    public int getMaxEntries() {
        return maxEntries;
    }

    public void setMaxEntries(int maxEntries) {
        this.maxEntries = maxEntries;
    }

    public FxLock getSelectedLock() {
        return selectedLock;
    }

    public void setSelectedLock(FxLock selectedLock) {
        this.selectedLock = selectedLock;
    }

    /**
     * Get all available history groups to select from
     *
     * @return available history groups
     */
    public List<SelectItem> getAvailableLockTypes() {
        if (availableLockTypes == null) {
            availableLockTypes = new ArrayList<SelectItem>(10);
            availableLockTypes.add(new FxJSFSelectItem());
            availableLockTypes.add(new FxJSFSelectItem(FxLockType.Loose));
            availableLockTypes.add(new FxJSFSelectItem(FxLockType.Permanent));
        }
        return availableLockTypes;
    }

    public FxString getAccountMatch() {
        return accountMatch;
    }

    public void setAccountMatch(FxString accountMatch) {
        this.accountMatch = accountMatch;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public FxLockType getLockType() {
        return lockType;
    }

    public void setLockType(FxLockType lockType) {
        this.lockType = lockType;
    }

    /**
     * Get the list of lock entries based on user selections
     *
     * @return list of lock entries based on user selections
     */
    public List<FxLock> getList() {
        return results;
    }

    /**
     * Setter for the list for ajax postbacks
     *
     * @param list the result list
     */
    public void setList(List<FxLock> list) {
        this.results = list;
    }

    /**
     * Execute the query
     *
     * @return navigation result
     */
    public String performQuery() {
        Long account;
        if (accountMatch != null && !accountMatch.isEmpty()) {
            try {
                String match = accountMatch.getDefaultTranslation();
                if (StringUtils.isEmpty(match))
                    account = null;
                else
                    account = EJBLookup.getAccountEngine().load(accountMatch.getBestTranslation()).getId();
            } catch (FxApplicationException e) {
                new FxFacesMsgErr("Lock.error.account", accountMatch.getBestTranslation()).addToContext();
                results = new ArrayList<FxLock>(0);
                return null;
            }
        } else
            account = null;
        Long typeId = !StringUtils.isEmpty(type) ? (Long.valueOf(type) >= 0 ? Long.valueOf(type) : null) : null;

        try {
            results = EJBLookup.getContentEngine().getLocks(lockType, account == null ? -1L : account, typeId == null ? -1L : typeId, null);
        } catch (FxLockException e) {
            new FxFacesMsgErr(e).addToContext();
        }
        return null;
    }

    public void unlock() {
        if( selectedLock == null || !selectedLock.isContentLock())
            return;
        try {
            EJBLookup.getContentEngine().unlock(selectedLock.getLockedPK());
            performQuery();
        } catch (FxLockException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

}
