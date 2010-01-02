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
package com.flexive.shared.scripting;

import java.io.Serializable;

/**
 * Class describing the mapping of a script to other objects (FxType, FxAssignment, ...)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see FxScriptMapping
 */
public class FxScriptMappingEntry implements Serializable {
    private static final long serialVersionUID = 3904586785341626084L;
    private long scriptId;
    private boolean active;
    private boolean derivedUsage;
    private long id;
    private long[] derivedIds;
    private FxScriptEvent scriptEvent;

    /**
     * Constructor
     *
     * @param scriptEvent  event of the script
     * @param scriptId     id of the script
     * @param active       is this mapping active?
     * @param derivedUsage should this mapping be used for derived objects as well?
     * @param id           object id of the mapping (type, assignment, ...)
     * @param derivedIds   object id's of derived objects
     */
    public FxScriptMappingEntry(FxScriptEvent scriptEvent, long scriptId, boolean active, boolean derivedUsage, long id, long[] derivedIds) {
        this.scriptEvent = scriptEvent;
        this.scriptId = scriptId;
        this.active = active;
        this.derivedUsage = derivedUsage;
        this.id = id;
        this.derivedIds = derivedIds.clone();
    }

    /**
     * Get the id of the script
     *
     * @return script id
     */
    public long getScriptId() {
        return scriptId;
    }

    /**
     * Is this mapping active?
     *
     * @return mapping active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Should this mapping be used in derived objects?
     *
     * @return usage in derived objects?
     */
    public boolean isDerivedUsage() {
        return derivedUsage;
    }

    /**
     * Get the mapped object id
     *
     * @return mapped object id
     */
    public long getId() {
        return id;
    }

    /**
     * Get the ids of all derived (and affected) objects
     *
     * @return ids of all derived (and affected) objects
     */
    public long[] getDerivedIds() {
        return derivedIds.clone();
    }

    /**
     * Get the event type of the script
     *
     * @return FxScriptEvent
     */
    public FxScriptEvent getScriptEvent() {
        return scriptEvent;
    }
}
