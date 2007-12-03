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

        public String getXPath() {
            return XPath;
        }

        public FxData getOriginalData() {
            return originalData;
        }

        public FxData getNewData() {
            return newData;
        }

        public boolean isProperty() {
            return property;
        }

        public boolean isGroup() {
            return !property;
        }

        public boolean isPositionChange() {
            return positionChange;
        }

        public boolean isDataChange() {
            return dataChange;
        }

        /**
         * Is this delta updateable or do we need delete/insert?
         * A property is not updateable if language settings changed or it spans multiple columns like a select-many
         *
         * @return if this chance is updateable
         */
        public boolean isUpdateable() {
            return !(languageSettingChanged || multiColumn);
        }

        /**
         * Increase the retry count - used when saving and conflicts arise that
         * are fixed after some retries (ie positioning changes)
         */
        public synchronized void increaseRetries() {
            retryCount++;
        }

        public synchronized int getRetryCount() {
            return retryCount;
        }
    }

    private final static List<FxDeltaChange> EMPTY = Collections.unmodifiableList(new ArrayList<FxDeltaChange>(0));

    private List<FxDeltaChange> updates, adds, deletes;
    private boolean internalPropertyChanged = false;

    public FxDelta(List<FxDeltaChange> updates, List<FxDeltaChange> adds, List<FxDeltaChange> deletes) {
        this.updates = (updates != null ? updates : EMPTY);
        this.adds = (adds != null ? adds : EMPTY);
        this.deletes = (deletes != null ? deletes : EMPTY);
        checkInternal(this.updates);
        if (!internalPropertyChanged) checkInternal(this.adds);
        if (!internalPropertyChanged) checkInternal(this.deletes);
    }

    private void checkInternal(List<FxDeltaChange> list) {
        for (FxDeltaChange c : list)
            if (c.isProperty() && (
                    (c.getOriginalData() != null && c.getOriginalData().isSystemInternal()) || (c.getNewData() != null && c.getNewData().isSystemInternal()))) {
                internalPropertyChanged = true;
                break;
            }
    }

    public List<FxDeltaChange> getUpdates() {
        return updates;
    }

    public List<FxDeltaChange> getAdds() {
        return adds;
    }

    public List<FxDeltaChange> getDeletes() {
        return deletes;
    }

    public boolean isInternalPropertyChanged() {
        return internalPropertyChanged;
    }

    public boolean changes() {
        return !(updates.size() == 0 && deletes.size() == 0 && adds.size() == 0);
    }

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
                    if (c.isUpdateable())
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
        if (deletes.size() > 0) {
            sb.append("Deletes:\n");
            for (FxDeltaChange c : deletes) {
                sb.append(c.getXPath()).append(": deleted at position ").append(c.getOriginalData().getPos()).append("\n");
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
     * @throws com.flexive.shared.exceptions.FxInvalidParameterException
     *          on errors
     * @throws com.flexive.shared.exceptions.FxNotFoundException
     *          on errors
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
