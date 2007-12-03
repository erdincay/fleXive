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

import com.flexive.shared.security.LifeCycleInfo;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

/**
 * Information about a content's existing versions
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxContentVersionInfo implements Serializable {

    private static final long serialVersionUID = -2928402301816290488L;
    private long id;
    private int minVersion;
    private int maxVersion;
    private int liveVersion;
    private int lastModifiedVersion;
    private Map<Integer, LifeCycleInfo> versions;

    /**
     * Ctor
     *
     * @param id                  instance id
     * @param minVersion          minimum existing version
     * @param maxVersion          maximum existing version
     * @param liveVersion         live version (if not exists: -1)
     * @param lastModifiedVersion version that has the latest modification date
     * @param versions            map of version,LifeCycleInfo entries
     */
    public FxContentVersionInfo(long id, int minVersion, int maxVersion, int liveVersion, int lastModifiedVersion, Map<Integer, LifeCycleInfo> versions) {
        this.id = id;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.liveVersion = liveVersion;
        this.lastModifiedVersion = lastModifiedVersion;
        this.versions = versions;
    }

    /**
     * Get the id of the instance
     *
     * @return id of the instance
     */
    public long getId() {
        return id;
    }

    /**
     * Get the minimum existing version
     *
     * @return minimum existing version
     */
    public int getMinVersion() {
        return minVersion;
    }

    /**
     * Get the maximum existing version
     *
     * @return maximum existing version
     */
    public int getMaxVersion() {
        return maxVersion;
    }

    /**
     * Get the live version. if no live version exists then <code>-1</code> is returned
     *
     * @return live version or <code>-1</code>
     */
    public int getLiveVersion() {
        return liveVersion;
    }

    /**
     * Does a live version exist for this content instance
     *
     * @return if a live version exists for this content instance
     */
    public boolean hasLiveVersion() {
        return liveVersion > 0;
    }

    /**
     * Get the version that was changed most recently
     *
     * @return version that was changed most recently
     */
    public int getLastModifiedVersion() {
        return lastModifiedVersion;
    }

    /**
     * Get an iterator for all available versions
     *
     * @return iterator for all available versions
     */
    public Iterator<Integer> getVersions() {
        return versions.keySet().iterator();
    }

    /**
     * Get the LifeCycleInfo for a requested version or <code>null</code> if the
     * requested version does not exist
     *
     * @param version the requested version
     * @return LifeCycleInfo for a requested version or <code>null</code> if the
     *         requested version does not exist
     */
    public LifeCycleInfo getLifeCycleInfo(int version) {
        return versions.get(version);
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(200);
        sb.append("ContentVersionInfo:\nGeneral information: {id=").append(this.id).append(",minVer=").append(this.minVersion).
                append(",maxVer=").append(this.maxVersion).append(",liveVer=").append(this.liveVersion).
                append(",lastModVer=").append(this.lastModifiedVersion).append("}\n");
        for (int v : versions.keySet())
            sb.append("Version ").append(v).append(":").append(versions.get(v).toString()).append("\n");

        return sb.toString();
    }

    /**
     * Check if the requested Pk's version exists
     *
     * @param pk
     * @return if the requested Pk's version exists
     */
    public boolean containsVersion(FxPK pk) {
        if (!pk.isDistinctVersion()) {
            return pk.getVersion() == FxPK.MAX || pk.getVersion() == FxPK.LIVE && liveVersion > 0;
        }
        return versions.containsKey(pk.getVersion());
    }

    /**
     * Get the correct maximum or live version if <code>version</code> is version constant from FxPK
     *
     * @param version the requested version
     * @return distinct version
     */
    public int getDistinctVersion(int version) {
        if( version == FxPK.MAX )
            return maxVersion;
        if( version == FxPK.LIVE )
            return liveVersion;
        return version;
    }
}
