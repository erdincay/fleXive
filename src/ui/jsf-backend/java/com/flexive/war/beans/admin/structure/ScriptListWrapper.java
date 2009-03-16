/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.beans.admin.structure;

import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.scripting.FxScriptMappingEntry;
import com.flexive.shared.exceptions.FxEntryExistsException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxAssignment;

import java.util.*;
import java.io.Serializable;
import java.text.Collator;

/**
 * Conveniently wraps script mappings to simplify GUI Manipulaiton.
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ScriptListWrapper implements Serializable {
    private static final long serialVersionUID = 7620819571266609753L;
    public final static int ID_SCRIPT_REMOVED=-1;
    public final static int ID_SCRIPT_ADDED=-2;
    public final static int ID_SCRIPT_UPDATED=-3;
    public final static int SORT_STATUS_UNSORTED=0;
    public final static int SORT_STATUS_ASCENDING=1;
    public final static int SORT_STATUS_DESCENDING=2;
    private int sortStatusScriptInfo=-1;
    private int sortStatusEvent=-1;

    public static class ScriptListEntry {
        private int id;
        private FxScriptInfo scriptInfo=null;
        private FxScriptEvent scriptEvent=null;
        private boolean derived =false;
        private boolean derivedUsage = false;
        private long derivedFrom =-1;
        private boolean active = false;

        public ScriptListEntry(int id, FxScriptInfo scriptInfo, FxScriptEvent scriptEvent, boolean derived, long derivedFrom, boolean derivedUsage, boolean active) {
            this.id = id;
            this.scriptInfo = scriptInfo;
            this.scriptEvent = scriptEvent;
            this.derived = derived;
            this.derivedFrom = derivedFrom;
            this.derivedUsage = derivedUsage;
            this.active =active;
        }

        public int getId() {
            return id;
        }

        private void setId(int id) {
            this.id=id;
        }

        public FxScriptInfo getScriptInfo() {
            return scriptInfo;
        }

        public FxScriptEvent getScriptEvent() {
            return scriptEvent;
        }

        public boolean isDerived() {
            return derived;
        }

        public void setDerived(boolean derived) {
            this.derived = derived;
        }

        public boolean isDerivedUsage() {
            return derivedUsage;
        }

        public void setDerivedUsage(boolean derivedUsage) {
            this.derivedUsage = derivedUsage;
        }

        public long getDerivedFrom() {
            return derivedFrom;
        }

        public void setDerivedFrom(long derivedFrom) {
            this.derivedFrom = derivedFrom;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }
    }

    private List<ScriptListEntry> scriptList = null;
    private int ctr=0;
    private Comparator<ScriptListEntry> scriptComparator = new ScriptInfoSorter();
    private Comparator<ScriptListEntry> eventComparator = new ScriptEventSorter();
    private boolean isType;

    /**
     * Constructs a new script list wrapper
     *
     * @param id        the id of the type or assignment
     * @param isType    if the id is from a type or assignment
     */
    ScriptListWrapper(long id, boolean isType) {
        this.isType =isType;
        scriptList = buildScriptList(id);
        sortByScripts();
    }

    private List<ScriptListEntry> buildScriptList(long id) {
        //determine if type or assignment are derived
        boolean isDerived=false;
        if (isType) {
            FxType type= CacheAdmin.getEnvironment().getType(id);
            isDerived = type.isDerived();
        }
        else {
            FxAssignment ass = CacheAdmin.getEnvironment().getAssignment(id);
            isDerived = ass.isDerivedAssignment();
        }
        List<ScriptListEntry> list = new ArrayList<ScriptListEntry>();

        //set remaining properties
        for (FxScriptInfo s : CacheAdmin.getEnvironment().getScripts())
            for (FxScriptMappingEntry e : isType? CacheAdmin.getFilteredEnvironment().getScriptMapping(s.getId()).getMappedTypes() :
                    CacheAdmin.getFilteredEnvironment().getScriptMapping(s.getId()).getMappedAssignments()) {
                //determine if script is active and derived usage
                if (e.getId() ==id &&  s.getId() == e.getScriptId()) {
                    list.add(new ScriptListEntry(ctr++, s, e.getScriptEvent(), false, -1, e.isDerivedUsage(), e.isActive()));
                }
                //determine if script is derived and from which id
                else if(isDerived && e.isDerivedUsage() && s.getId() == e.getScriptId()) {
                    for (long derivedId : e.getDerivedIds()) {
                        if (id == derivedId) {
                            list.add(new ScriptListEntry(ctr++, s, e.getScriptEvent(), true, e.getId(), e.isDerivedUsage(), e.isActive()));
                            break;
                        }
                    }
                }
            }
        return list;
    }

   /**
     * builds the delta to the original scriptMapping. Returns a List of ScriptListEntry objects,
     * where the id is set to ScriptListWrapper.ID_SCRIPT_REMOVED for scripts that where removed from
     * the script list and where the id is set to ScriptListWrapper.ID_SCRIPT_ADDED for scripts that
     * where added to the script list. ID_SCRIPT_UPDATED for scripts that have been updated.
     *
     * @param id                        the id of the type or assignment for the script mapping
     * @return  a list of ScriptEntry objects which have changed in the script list relative to
     *          the original scriptMapping.
     */

    public List<ScriptListEntry> getDelta(long id) {
        List<ScriptListEntry> delta = new ArrayList<ScriptListEntry>();
        List<ScriptListEntry> original = buildScriptList(id);
        for (ScriptListEntry s : original) {
            if (!hasEntry(s)) {
                s.setId(ID_SCRIPT_REMOVED);
                delta.add(s);
            }
        }

        for (ScriptListEntry s : scriptList) {
            if (!hasEntry(original, s)) {
                delta.add(new ScriptListEntry(ID_SCRIPT_ADDED, s.getScriptInfo(), s.getScriptEvent(), s.isDerived(), s.getDerivedFrom(), s.derivedUsage, s.isActive()));
            }
        }

        for (ScriptListEntry s: scriptList)
            if (!hasEntry(delta, s )) {
                //TODO: check if value has really changed versus the original before marking UPDATED ->would increase computing time but decrease DB accesses
                delta.add(new ScriptListEntry(ID_SCRIPT_UPDATED, s.getScriptInfo(), s.getScriptEvent(), s.isDerived(), s.getDerivedFrom(), s.derivedUsage, s.isActive()));
            }

        return delta;
    }

    public List<ScriptListEntry> getScriptList() {
        return scriptList;
    }

    /**
     * checks the script list for a script list entry with matching script id, event and derived source.
     *
     * @param key   a script list entry
     * @return      true if another script list entry with matching script id and event was found
     */
    private boolean hasEntry(ScriptListEntry key) {
        for (ScriptListEntry le: scriptList) {
            if (le.getScriptEvent().getId() == key.getScriptEvent().getId()
                && le.getScriptInfo().getId() == key.getScriptInfo().getId()
                && le.getDerivedFrom()== key.getDerivedFrom())
                return true;
        }
        return false;
    }

    /**
     * checks a specific script list for a script list entry with matching
     * script id and event.
     *
     * @param scriptList    the script list to be searched
     * @param key           a script list entry
     * @return              true if another script list entry with matching script id and event was found
     */

    private boolean hasEntry(List<ScriptListEntry> scriptList, ScriptListEntry key) {
        for (ScriptListEntry le: scriptList) {
            if (le.getScriptEvent().getId() == key.getScriptEvent().getId()
                && le.getScriptInfo().getId() == key.getScriptInfo().getId()
                && le.getDerivedFrom() == key.getDerivedFrom())
                return true;
        }
        return false;
    }

    public void add(long scriptInfo, long scirptEvent, boolean derivedUsage, boolean active) throws FxEntryExistsException, FxNotFoundException {
        ScriptListEntry e = new ScriptListEntry(ctr++, CacheAdmin.getEnvironment().getScript(scriptInfo), FxScriptEvent.getById(scirptEvent), false, -1, derivedUsage, active);
        if (!hasEntry(e)) {
            scriptList.add(e);
            sortStatusScriptInfo=SORT_STATUS_UNSORTED;
            sortStatusEvent=SORT_STATUS_UNSORTED;
        }
        else
            throw new FxEntryExistsException("ex.scriptListWrapper.entryExists",e.getScriptInfo().getName(),e.getScriptEvent().getName());
    }

    public void remove(int entryId) {
        ScriptListEntry entry = null;
        for (ScriptListEntry e: scriptList) {
            if (e.getId() == entryId) {
                entry=e;
                break;
            }
        }
        scriptList.remove(entry);
    }

    public void sortByScripts() {
        Collections.sort(scriptList, scriptComparator);
        if (sortStatusScriptInfo == SORT_STATUS_UNSORTED)
            sortStatusScriptInfo=SORT_STATUS_ASCENDING;
        else if (sortStatusScriptInfo == SORT_STATUS_ASCENDING)
            sortStatusScriptInfo = SORT_STATUS_DESCENDING;
        else
            sortStatusScriptInfo = SORT_STATUS_ASCENDING;

        sortStatusEvent = SORT_STATUS_UNSORTED;
    }

    public void sortByEvents() {
        Collections.sort(scriptList, eventComparator);
        if (sortStatusEvent == SORT_STATUS_UNSORTED)
            sortStatusEvent=SORT_STATUS_ASCENDING;
        else if (sortStatusEvent == SORT_STATUS_ASCENDING)
            sortStatusEvent = SORT_STATUS_DESCENDING;
        else
            sortStatusEvent = SORT_STATUS_ASCENDING;

        sortStatusScriptInfo = SORT_STATUS_UNSORTED;
    }

    public int getSortStatusScriptInfo() {
        return sortStatusScriptInfo;
    }

    public void setSortStatusScriptInfo(int sortStatusScriptInfo) {
        this.sortStatusScriptInfo = sortStatusScriptInfo;
    }

    public int getSortStatusEvent() {
        return sortStatusEvent;
    }

    public void setSortStatusEvent(int sortStatusEvent) {
        this.sortStatusEvent = sortStatusEvent;
    }

    /**
     * compares script list entries, priorizes the script name and if equal the event name
     */
    private class ScriptInfoSorter implements Comparator<ScriptListEntry>, Serializable {
        private static final long serialVersionUID = -330634661618248486L;
        private final Collator collator = FxSharedUtils.getCollator();

        public int compare(ScriptListEntry o1, ScriptListEntry o2) {
            int multiplicator =1;
            if (sortStatusScriptInfo == SORT_STATUS_ASCENDING)
                multiplicator =-1;
            int c1 = this.collator.compare(o1.getScriptInfo().getName(), o2.getScriptInfo().getName());
            if (c1 !=0)
                return c1*multiplicator;
            else
                return multiplicator* this.collator.compare(o1.getScriptEvent().getName(), o2.getScriptEvent().getName());
        }
    }

    /**
     * compares script list entries, priorizes the script event name and if equal the script name
     */
    private class ScriptEventSorter implements Comparator<ScriptListEntry>, Serializable {
        private static final long serialVersionUID = -1285682507584328636L;
        private final Collator collator = FxSharedUtils.getCollator();

        public int compare(ScriptListEntry o1, ScriptListEntry o2) {
            int multiplicator =1;
            if (sortStatusEvent == SORT_STATUS_ASCENDING)
                multiplicator =-1;
            int c1 = this.collator.compare(o1.getScriptEvent().getName(), o2.getScriptEvent().getName());
            if (c1 !=0)
                return c1*multiplicator;
            else
                return multiplicator*this.collator.compare(o1.getScriptInfo().getName(), o2.getScriptInfo().getName());
        }
    }
}
