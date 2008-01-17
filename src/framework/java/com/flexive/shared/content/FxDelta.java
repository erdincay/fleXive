/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.shared.content;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.value.FxValue;
import org.apache.commons.lang.ArrayUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Compute Delta changes for FxContents
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxDelta implements Serializable {
    private static final long serialVersionUID = -6246822483703676822L;

    /**
     * A single delta change
     */
    public static class FxDeltaChange implements Serializable {
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

        /**
         * Ctor
         *
         * @param XPath        affected XPath
         * @param originalData original data
         * @param newData      new data
         */
        @SuppressWarnings({"ConstantConditions"})
        public FxDeltaChange(String XPath, FxData originalData, FxData newData) {
            this.XPath = XPath;
            this.originalData = originalData;
            this.newData = newData;
            this.property = originalData instanceof FxPropertyData || newData instanceof FxPropertyData;
            if (newData == null || originalData == null) {
                this.positionChange = true;
                this.dataChange = true;
            } else {
                positionChange = newData.getPos() != originalData.getPos();
                this.dataChange = property && !((FxPropertyData) originalData).getValue().equals(((FxPropertyData) newData).getValue());
            }
            this.languageSettingChanged = this.dataChange && this.property;
            if (this.languageSettingChanged && this.originalData != null && this.newData != null) {
                FxValue ov = ((FxPropertyData) originalData).getValue();
                FxValue nv = ((FxPropertyData) newData).getValue();
                this.languageSettingChanged = !ArrayUtils.isEquals(ov.getTranslatedLanguages(), nv.getTranslatedLanguages());
            }
            if (this.property && this.newData != null) {
                switch (((FxPropertyAssignment) ((FxPropertyData) newData).getAssignment()).getProperty().getDataType()) {
                    case SelectMany:
                        this.multiColumn = true;
                        break;
                    default:
                        this.multiColumn = false;
                }
            } else
                this.multiColumn = false;
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
    }

    private final static List<FxDeltaChange> EMPTY = Collections.unmodifiableList(new ArrayList<FxDeltaChange>(0));

    private List<FxDeltaChange> updates, adds, removes;
    private boolean internalPropertyChanged = false;

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
    }

    private void checkInternal(List<FxDeltaChange> list) {
        for (FxDeltaChange c : list)
            if (c.isProperty() && (
                    (c.getOriginalData() != null && c.getOriginalData().isSystemInternal()) || (c.getNewData() != null && c.getNewData().isSystemInternal()))) {
                internalPropertyChanged = true;
                break;
            }
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
     * Was there a change of system internal properties like ACL?
     *
     * @return change of system internal properies
     */
    public boolean isInternalPropertyChanged() {
        return internalPropertyChanged;
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
     * Compare <code>original</code> to <code>compare</code> FxContent.
     * Both contents should be compacted and empty entries removed for correct results.
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

        List<FxDeltaChange> updates = null, adds = null, deletes = null;

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
                    updates.add(new FxDeltaChange(xp, original.getGroupData(xp), compare.getGroupData(xp)));
                }
            } else /*property*/ if (!compare.getData(xp).equals(original.getData(xp))) {
                if (updates == null)
                    updates = new ArrayList<FxDeltaChange>(10);
                updates.add(new FxDeltaChange(xp, original.getPropertyData(xp), compare.getPropertyData(xp)));
            }
        }

        List<String> add = new ArrayList<String>(comp);
        add.removeAll(org);
        for (String xp : add) {
            if (adds == null)
                adds = new ArrayList<FxDeltaChange>(10);
            if (xp.endsWith("/")) {
                xp = xp.substring(0, xp.length() - 1);
                adds.add(new FxDeltaChange(xp, null, compare.getGroupData(xp)));
            } else
                adds.add(new FxDeltaChange(xp, null, compare.getPropertyData(xp)));
        }

        List<String> rem = new ArrayList<String>(org);
        rem.removeAll(comp);
        for (String xp : rem) {
            if (deletes == null)
                deletes = new ArrayList<FxDeltaChange>(10);
            if (xp.endsWith("/")) {
                xp = xp.substring(0, xp.length() - 1);
                deletes.add(new FxDeltaChange(xp, original.getGroupData(xp), null));
            } else
                deletes.add(new FxDeltaChange(xp, original.getPropertyData(xp), null));
        }
        return new FxDelta(updates, adds, deletes);
    }
}
