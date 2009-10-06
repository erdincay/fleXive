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

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.model.FxJSFSelectItem;
import com.flexive.shared.*;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLockException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.StringUtils;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private Map<Object, String> readableDateFormat;
    private FxLock selectedLock;
    private ConcurrentMap<FxPK, Boolean> markedLocks = new ConcurrentHashMap<FxPK, Boolean>();
    private boolean allMarked = false;

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

    public ConcurrentMap<FxPK, Boolean> getMarkedLocks() {
        return markedLocks;
    }

    public void setMarkedLocks(ConcurrentMap<FxPK, Boolean> markedLocks) {
        this.markedLocks = markedLocks;
    }

    public boolean isAllMarked() {
        return allMarked;
    }

    public void setAllMarked(boolean allMarked) {
        this.allMarked = allMarked;
    }

    /**
     * Get the list of lock entries based on user selections
     *
     * @return list of lock entries based on user selections
     */
    public List<FxLock> getList() {
        if (results == null) {
            performQuery();
        }
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
        // synchronise markedLocks map
        synchMarkedMap();
        return null;
    }

    public void unlock() {
        if (selectedLock == null || !selectedLock.isContentLock())
            return;

        unlockRef(selectedLock);
        performQuery();
    }

    /**
     * Action method: Extend the duration of a loose lock by 10 minutes, extend a permanent lock by 1 hour
     */
    public void extendLock() {
        if (selectedLock == null || !selectedLock.isContentLock())
            return;

        extendLockRef(selectedLock);
        performQuery();
    }

    /**
     * Action method: extend all selected locks
     */
    public void extendSelected() {
        for (FxPK pk : markedLocks.keySet()) {
            if (markedLocks.get(pk)) {
                final FxLock l = getLockFromPK(pk);
                if (l != null)
                    extendLockRef(l);
            }
        }
        performQuery();
    }


    /**
     * Action method: unlock selected content instances
     */
    public void unlockSelected() {
        for (FxPK pk : markedLocks.keySet()) {
            if (markedLocks.get(pk)) {
                final FxLock l = getLockFromPK(pk);
                if(l != null)
                    unlockRef(l);
            }
        }
        performQuery();
    }

    /**
     * Refactorisation of the unlocking action
     *
     * @param lock the lock t.b. unlocked
     */
    private void unlockRef(FxLock lock) {
        try {
            EJBLookup.getContentEngine().unlock(lock.getLockedPK());
        } catch (FxLockException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    /**
     * Retrieve a lock for a given PK
     *
     * @param inputPK the input PK
     * @return returns the lock if found, null otherwise
     */
    private FxLock getLockFromPK(FxPK inputPK) {
        for (FxLock l : results) {
            if (l.getLockedPK().equals(inputPK))
                return l;
        }
        return null;
    }

    /**
     * Refactorisation of the lock duration extension alg.
     *
     * @param lock the FxLock to extend
     */
    private void extendLockRef(FxLock lock) {
        final ContentEngine ce = EJBLookup.getContentEngine();
        final FxLockType t = lock.getLockType();

        try {
            if (t == FxLockType.Loose)
                ce.extendLock(lock.getLockedPK(), 10 * 60 * 1000);
            else if (t == FxLockType.Permanent)
                ce.extendLock(lock.getLockedPK(), 60 * 60 * 1000);

        } catch (FxLockException e) {
            new FxFacesMsgErr(e.getCause()).addToContext();
        }
    }

    /**
     * Action method: marks / unmarks all locks in the list
     */
    public void markAll() {
        if (allMarked) {
            for (FxPK pk : markedLocks.keySet()) {
                markedLocks.replace(pk, true);
            }
        } else {
            for (FxPK pk : markedLocks.keySet()) {
                markedLocks.replace(pk, false);
            }
        }
    }

    /**
     * Create a human readable tooltip for the expires / created dates
     *
     * @return the readable date format
     */
    public Map<Object, String> getReadableDateFormat() {
        if (readableDateFormat == null) {
            readableDateFormat = FxSharedUtils.getMappedFunction(new FxSharedUtils.ParameterMapper<Object, String>() {
                public String get(Object key) {
                    try {
                        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(key);
                    } catch (Exception e) {
                        return "unknown";
                    }
                }
            });
        }
        return readableDateFormat;
    }

    /**
     * Synchronizes the list of results with the marked Ids (GUI)
     */
    private void synchMarkedMap() {
        if (results == null) {
            markedLocks = new ConcurrentHashMap<FxPK, Boolean>(0);
            return;
        }

        ConcurrentMap<FxPK, Boolean> tmp = new ConcurrentHashMap<FxPK, Boolean>(results.size());
        for (FxLock l : results) {
            final FxPK resultPK = l.getLockedPK();
            if (markedLocks.containsKey(resultPK))
                tmp.put(resultPK, markedLocks.get(resultPK));
            else
                tmp.put(resultPK, false);
        }
        markedLocks = new ConcurrentHashMap<FxPK, Boolean>(tmp.size());
        markedLocks.putAll(tmp);
    }
}