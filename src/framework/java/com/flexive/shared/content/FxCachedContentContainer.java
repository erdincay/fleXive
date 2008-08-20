/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Content Container for the Cache to keep all versions for an id.
 * This class is used internally and does only contain versions that have been previously requested!
 * To load all available versions use <code>FxContentContainer</code>
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @see FxContentContainer
 */
public class FxCachedContentContainer implements Serializable {
    private static final long serialVersionUID = -6810666943342672605L;
    private final long id;
    private int maxVersion;
    private int liveVersion;
    private List<FxCachedContent> content;

    /**
     * Ctor
     *
     * @param content the content to cache (and its security info)
     */
    public FxCachedContentContainer(FxCachedContent content) {
        this.id = content.getContent().getId();
        this.maxVersion = content.getContent().getMaxVersion();
        this.liveVersion = content.getContent().getLiveVersion();
        this.content = new ArrayList<FxCachedContent>(maxVersion);
        this.content.add(content);
    }

    /**
     * Getter for the id
     *
     * @return id
     */
    public long getId() {
        return id;
    }

    /**
     * Getter for the max. version number
     *
     * @return max. version number
     */
    public synchronized int getMaxVersion() {
        return maxVersion;
    }

    /**
     * Getter for the live version number
     *
     * @return live version number
     */
    public synchronized int getLiveVersion() {
        return liveVersion;
    }

    /**
     * Try to get a content by its primary key, will return <code>null</code> if not found
     *
     * @param pk primary key
     * @return content
     */
    public synchronized FxCachedContent get(FxPK pk) {
        for (FxCachedContent found : content) {
            if (pk.isDistinctVersion()) {
                if (found.getContent().getVersion() == pk.getVersion())
                    return found;
            } else {
                if (pk.getVersion() == FxPK.MAX && found.getContent().isMaxVersion())
                    return found;
                else if (pk.getVersion() == FxPK.LIVE && found.getContent().isLiveVersion())
                    return found;
            }
        }
        return null;
    }

    /**
     * Add a content to the cache if - and only if - the id matches and its not contained already
     *
     * @param content the content to add
     */
    public synchronized void add(FxCachedContent content) {
        if (this.id == content.getContent().getId() && get(content.getContent().getPk()) == null) {
            this.content.add(content);
            this.maxVersion = content.getContent().getMaxVersion();
            this.liveVersion = content.getContent().getLiveVersion();
        }
    }
}
