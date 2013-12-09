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
package com.flexive.shared.content;

import com.flexive.shared.FxDiff;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.value.FxValue;
import org.apache.commons.lang.ArrayUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Compute Delta changes for FxContents
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxDelta implements Serializable {
    private static final long serialVersionUID = -6246822483703676822L;
    private List<FxPropertyData> flatData;

    /**
     * A single delta change
     */
    public static class FxDeltaChange implements Serializable {
        public static enum ChangeType {
            Add, Remove, Update, None
        }

        private static final long serialVersionUID = -9026076539541708345L;

        private String XPath;
        private FxData originalData;
        private FxData newData;
        private boolean positionChange;
        private boolean dataChange;
        private boolean property;
        private boolean languageSettingChanged;
        private int retryCount;
        private boolean multiColumn;
        private ChangeType changeType;
        private boolean flatStorageChange;

        /**
         * Ctor
         *
         * @param changeType   type of change
         * @param XPath        affected XPath
         * @param originalData original data
         * @param newData      new data
         */
        @SuppressWarnings({"ConstantConditions"})
        public FxDeltaChange(ChangeType changeType, String XPath, FxData originalData, FxData newData) {
            this.changeType = changeType;
            this.XPath = XPath;
            this.originalData = originalData;
            this.newData = newData;
            this.property = originalData instanceof FxPropertyData || newData instanceof FxPropertyData;
            if (newData == null || originalData == null) {
                this.positionChange = true;
                this.dataChange = true;
            } else {
                positionChange = newData.getPos() != originalData.getPos();
                if (property) {
                    final FxValue origValue = ((FxPropertyData) originalData).getValue();
                    final FxValue newValue = ((FxPropertyData) newData).getValue();
                    this.dataChange = !origValue.equals(newValue) && (!origValue.isEmpty() || !newValue.isEmpty());
                } else {
                    this.dataChange = false;
                }
            }
            this.languageSettingChanged = this.dataChange && this.property;
            if (this.languageSettingChanged && this.originalData != null && this.newData != null) {
                FxValue ov = ((FxPropertyData) originalData).getValue();
                FxValue nv = ((FxPropertyData) newData).getValue();
                this.languageSettingChanged = !ArrayUtils.isEquals(ov.getTranslatedLanguages(), nv.getTranslatedLanguages());
            }
            if (this.property && this.newData != null) {
                switch (((FxPropertyAssignment) newData.getAssignment()).getProperty().getDataType()) {
                    case SelectMany:
                        this.multiColumn = true;
                        break;
                    default:
                        this.multiColumn = false;
                }
            } else
                this.multiColumn = false;
            if (!isProperty())
                this.flatStorageChange = false;
            else {
                FxData data = (getNewData() != null ? getNewData() : getOriginalData());
                this.flatStorageChange = data != null && ((FxPropertyData) data).getPropertyAssignment().isFlatStorageEntry();
            }
        }

        /**
         * Get the type of change
         *
         * @return type of change
         */
        public ChangeType getChangeType() {
            return changeType;
        }

        /**
         * Getter for the XPath (based on the origina XPath)
         *
         * @return XPath
         */
        public String getXPath() {
            return XPath;
        }

        /**
         * Getter for the original data
         *
         * @return original data
         */
        public FxData getOriginalData() {
            return originalData;
        }

        /**
         * Getter for the new data
         *
         * @return new data
         */
        public FxData getNewData() {
            return newData;
        }

        /**
         * Is the change affecting a property?
         *
         * @return property affected?
         */
        public boolean isProperty() {
            return property;
        }

        /**
         * Is this a system internal property like version or step?
         *
         * @return is system internal
         */
        public boolean isInternal() {
            if (originalData != null)
                return originalData.isSystemInternal();
            return newData != null && newData.isSystemInternal();
        }

        /**
         * Is this a position change only?
         *
         * @return position change only?
         */
        public boolean isPositionChangeOnly() {
            return positionChange && !dataChange;
        }

        /**
         * Is the change affecting a group?
         *
         * @return group affected?
         */
        public boolean isGroup() {
            return !property;
        }

        /**
         * Did the position change?
         *
         * @return position change
         */
        public boolean isPositionChange() {
            return positionChange;
        }

        /**
         * Has data changed?
         *
         * @return data changed
         */
        public boolean isDataChange() {
            return dataChange;
        }

        /**
         * Have language settings changed? (New or removed translations,etc)
         *
         * @return language settings changed
         */
        public boolean isLanguageSettingChanged() {
            return languageSettingChanged;
        }

        /**
         * Does this change affect a flat storage entry?
         *
         * @return change affects a flat storage entry
         */
        public boolean isFlatStorageChange() {
            return flatStorageChange;
        }

        /**
         * <b>Internal use only!</b>
         * Is this delta updateable or do we need delete/insert?
         * A property is not updateable if language settings changed or it spans multiple columns like a select-many
         *
         * @return if this chance is updateable
         */
        public boolean _isUpdateable() {
            return !(languageSettingChanged || multiColumn);
        }

        /**
         * <b>Internal use only!</b>
         * Increase the retry count - used when saving and conflicts arise that
         * are fixed after some retries (ie positioning changes)
         */
        public synchronized void _increaseRetries() {
            retryCount++;
        }

        /**
         * <b>Internal use only!</b>
         * Getter for the retry count
         *
         * @return retry count
         */
        public synchronized int _getRetryCount() {
            return retryCount;
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {
            return XPath + ": " + changeType.name() + (isDataChange() ? " DATA" : "") + (isPositionChange() ? " POS" : "") + (languageSettingChanged ? " LANG" : "");
        }
    }

    private final static List<FxDeltaChange> EMPTY = Collections.unmodifiableList(new ArrayList<FxDeltaChange>(0));

    private List<FxDeltaChange> updates, adds, removes;
    private boolean internalPropertyChanged = false;
    private boolean onlyInternalPropertyChanges = true;

    /**
     * Ctor
     *
     * @param updates delta updates
     * @param adds    delta adds
     * @param removes delta removes
     */
    public FxDelta(List<FxDeltaChange> updates, List<FxDeltaChange> adds, List<FxDeltaChange> removes) {
        this.updates = (updates != null ? updates : EMPTY);
        this.adds = (adds != null ? adds : EMPTY);
        this.removes = (removes != null ? removes : EMPTY);
        checkInternal(this.updates);
        if (!internalPropertyChanged) checkInternal(this.adds);
        if (!internalPropertyChanged) checkInternal(this.removes);
        this.flatData = new ArrayList<FxPropertyData>(20);
        for (FxDeltaChange up : this.updates) {
            if (up.isFlatStorageChange())
                this.flatData.add((FxPropertyData) up.getNewData());
        }
        for (FxDeltaChange add : this.adds) {
            if (add.isFlatStorageChange())
                this.flatData.add((FxPropertyData) add.getNewData());
        }
    }

    private void checkInternal(List<FxDeltaChange> list) {
        for (FxDeltaChange c : list)
            if (c.isProperty() && (
                    (c.getOriginalData() != null && c.getOriginalData().isSystemInternal()) || (c.getNewData() != null && c.getNewData().isSystemInternal()))) {
                internalPropertyChanged = true;
                if (!onlyInternalPropertyChanges)
                    break; //only break if we know that "other" properties changed as well
            } else
                onlyInternalPropertyChanges = false;
    }

    /**
     * Get a list of all updated deltas
     *
     * @return list of all updated deltas
     */
    public List<FxDeltaChange> getUpdates() {
        return updates;
    }

    /**
     * Get a list of all added deltas
     *
     * @return list of all added deltas
     */
    public List<FxDeltaChange> getAdds() {
        return adds;
    }

    /**
     * Get a list of all removed deltas
     *
     * @return list of all removed deltas
     */
    public List<FxDeltaChange> getRemoves() {
        return removes;
    }

    /**
     * Get all adds and updates that affect a flat storage
     *
     * @return list of all adds and updates that affect a flat storage
     */
    public List<FxPropertyData> getFlatStorageAddsUpdates() {
        return flatData;
    }

    /**
     * Was there a change of system internal properties like ACL?
     *
     * @return change of system internal properies
     */
    public boolean isInternalPropertyChanged() {
        return internalPropertyChanged;
    }

    /**
     * Are there only internal property changes (like step, version) and no data changes?
     *
     * @return only internal property changes
     */
    public boolean isOnlyInternalPropertyChanges() {
        return onlyInternalPropertyChanges;
    }

    /**
     * Are there any changes?
     *
     * @return changes?
     */
    public boolean changes() {
        return !(updates.size() == 0 && removes.size() == 0 && adds.size() == 0);
    }

    /**
     * Are there any data (and not only position) changed?
     *
     * @return data changes?
     * @since 3.1.5
     */
    public boolean isDataChanged() {
        if( !changes())
            return false;
        if( adds.size() > 0 || removes.size() > 0)
            return true;
        for(FxDelta.FxDeltaChange change: updates)
            if( change.isDataChange())
                return true;
        return false;
    }

    /**
     * Are there any changes relating to groups or group positions?
     *
     * @return  group changes?
     * @since 3.2.0
     */
    public boolean isGroupDataChanged() {
        return containsGroupChanges(adds) || containsGroupChanges(removes) || containsGroupChanges(updates);
    }

    private boolean containsGroupChanges(List<FxDeltaChange> changes) {
        for (FxDeltaChange change : changes) {
            if (change.isGroup()) {
                return true;
            }
        }
        return false;
    }
    /**
     * Create a dump of all changes for debugging purposes
     *
     * @return dump of all changes for debugging purposes
     */
    public String dump() {
        if (!changes())
            return "===> No changes! <===";
        StringBuilder sb = new StringBuilder(1000);
        sb.append("<=== changes start ===>\n");
        if (updates.size() > 0) {
            sb.append("Updates:\n");
            for (FxDeltaChange c : updates) {
                if (c.isPositionChange() && !c.isDataChange()) {
                    sb.append(c.getXPath()).append(": Position changed from ").append(c.getOriginalData().getPos()).
                            append(" to ").append(c.getNewData().getPos()).append("\n");
                } else if (c.isDataChange()) {
                    sb.append(c.getXPath()).append(": ");
                    if (c.isPositionChange())
                        sb.append("Position changed from ").append(c.getOriginalData().getPos()).
                                append(" to ").append(c.getNewData().getPos());
                    if (c.isDataChange())
                        sb.append(" [data changes]");
                    if (c._isUpdateable())
                        sb.append(" [lang.settings changes]");
                    sb.append("\n");
                }
            }
        }
        if (adds.size() > 0) {
            sb.append("Adds:\n");
            for (FxDeltaChange c : adds) {
                sb.append(c.getXPath()).append(": added at position ").append(c.getNewData().getPos()).append("\n");
            }
        }
        if (removes.size() > 0) {
            sb.append("Removes:\n");
            for (FxDeltaChange c : removes) {
                sb.append(c.getXPath()).append(": removed at position ").append(c.getOriginalData().getPos()).append("\n");
            }
        }
        sb.append("<=== changes end ===>\n");

        return sb.toString();
    }

    /**
     * Get all changes from <i>original</i> to <i>compare</i> in correct order (for display in UI's)
     *
     * @param original original content
     * @param compare  content that original is compared against
     * @return differences
     */
    public List<FxDeltaChange> getDiff(final FxContent original, final FxContent compare) {
        //This is not performant but since it should be rarely used in timecritical environments we don't care for now...
        List<FxDeltaChange> ret = new ArrayList<FxDeltaChange>(10);
        Map<String, FxDeltaChange> cache = new HashMap<String, FxDeltaChange>(removes.size() + adds.size() + updates.size());
        for (FxDeltaChange a : adds)
            cache.put(a.getXPath(), a);
        for (FxDeltaChange r : removes)
            cache.put(r.getXPath(), r);
        for (FxDeltaChange u : updates)
            cache.put(u.getXPath(), u);


        List<String> org = original.getAllXPaths("/");
        List<String> comp = compare.getAllXPaths("/");
        FxDiff diff = new FxDiff(org, comp);
        List<FxDiff.Difference> ld = diff.diff();

        /*System.out.println("== ORG ==");
        int i = 0;
        for (String s : org)
            System.out.println((i++) + ": " + s);
        System.out.println("== COMP ==");
        i = 0;
        for (String s : comp)
            System.out.println((i++) + ": " + s);

        System.out.println("== DELTA ==");
        for (FxDeltaChange c : cache.values())
            System.out.println(c);
        System.out.println("== DIFF ==");*/
        int checked = 0; //index of last checked update
        for (FxDiff.Difference d : ld) {
//            System.out.println(d);
            if (d.isDelete()) {
                for (int pos = d.getDeletedStart(); pos <= d.getDeletedEnd(); pos++) {
                    String delXP = org.get(pos);
//                    System.out.println("Deleted: " + delXP);
                    if (cache.containsKey(delXP)) {
                        //check for any XPath "before" entry for a data change
                        checked = checkUpdated(d, ret, cache, org, checked);
                        ret.add(cache.remove(delXP));
                    }
                }
            }
            if (d.isAdd()) {
                for (int pos = d.getAddedStart(); pos <= d.getAddedEnd(); pos++) {
                    String addXP = comp.get(pos);
//                    System.out.println("Added: " + addXP);
                    if (cache.containsKey(addXP)) {
                        //check for any XPath "before" entry for a data change
                        checked = checkUpdated(d, ret, cache, org, checked);
                        ret.add(cache.remove(addXP));
                    }
                }
            }
        }
        checkUpdated(null, ret, cache, org, checked);
        ret.removeAll(Collections.singleton(null)); //remove null entries
        return ret;
    }

    private int checkUpdated(FxDiff.Difference d, List<FxDeltaChange> ret, Map<String, FxDeltaChange> cache, List<String> org, int checked) {
        if (checked >= org.size())
            return checked;
        int end = d == null ? org.size() : (d.isAdd() ? d.getAddedEnd() : d.getDeletedEnd() - 1);
        if (end > org.size())
            end = org.size();
        for (int i = 0; i < end; i++) {
            if (cache.containsKey(org.get(i))) {
                ret.add(cache.remove(org.get(i)));
//                System.out.println("Adding update: " + org.get(i));
            }
        }
        return checked;
    }

    /**
     * Compare <code>original</code> to <code>compare</code> FxContent.
     * Both contents should be compacted and empty entries removed for correct results.
     * Flatstorage entries will only be added to the remove list.
     *
     * @param original original content
     * @param compare  content that original is compared against
     * @return deltas
     * @throws FxInvalidParameterException on errors
     * @throws FxNotFoundException         on errors
     */
    public static FxDelta processDelta(final FxContent original, final FxContent compare) throws FxNotFoundException, FxInvalidParameterException {
        List<String> org = original.getAllXPaths("/");
        List<String> comp = compare.getAllXPaths("/");

        List<FxDeltaChange> updates = null, adds = null, removes = null;

        //remove all xpaths from comp that are not contained in org => updates
        List<String> update = new ArrayList<String>(comp);
        update.retainAll(org);
        for (String xp : update) {
            if (xp.endsWith("/")) {
                //group
                xp = xp.substring(0, xp.length() - 1);
                if (!compare.getGroupData(xp).equals(original.getGroupData(xp))) {
                    if (updates == null)
                        updates = new ArrayList<FxDeltaChange>(10);
                    updates.add(new FxDeltaChange(FxDeltaChange.ChangeType.Update, xp, original.getGroupData(xp), compare.getGroupData(xp)));
                }
            } else if (!compare.getData(xp).equals(original.getData(xp))) {
                //property
                if (updates == null)
                    updates = new ArrayList<FxDeltaChange>(10);
                final FxPropertyData orgData = original.getPropertyData(xp);
                updates.add(new FxDeltaChange(FxDeltaChange.ChangeType.Update, xp, orgData, compare.getPropertyData(xp)));
            }
        }

        List<String> add = new ArrayList<String>(comp);
        add.removeAll(org);
        for (String xp : add) {
            if (adds == null)
                adds = new ArrayList<FxDeltaChange>(10);
            if (xp.endsWith("/")) {
                //group
                xp = xp.substring(0, xp.length() - 1);
                adds.add(new FxDeltaChange(FxDeltaChange.ChangeType.Add, xp, null, compare.getGroupData(xp)));
            } else {
                //property
                final FxPropertyData pdata = compare.getPropertyData(xp);
                adds.add(new FxDeltaChange(FxDeltaChange.ChangeType.Add, xp, null, pdata));
            }
        }

        List<String> rem = new ArrayList<String>(org);
        rem.removeAll(comp);
        for (String xp : rem) {
            if (removes == null)
                removes = new ArrayList<FxDeltaChange>(10);
            if (xp.endsWith("/")) {
                xp = xp.substring(0, xp.length() - 1);
                removes.add(new FxDeltaChange(FxDeltaChange.ChangeType.Remove, xp, original.getGroupData(xp), null));
            } else
                removes.add(new FxDeltaChange(FxDeltaChange.ChangeType.Remove, xp, original.getPropertyData(xp), null));
        }
        return new FxDelta(updates, adds, removes);
    }
}
