/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2011
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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

/**
 * Listener that is called when values of a group have changed, changes are propagated to parent groups.
 * Group add/remove events are broadcast as well.
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public interface FxValueChangeListener {

    public enum ChangeType {
        Add,
        Remove,
        Update,

        /**
         * Change of a child's position.
         *
         * @see com.flexive.shared.content.FxData#getPos()
         */
        Move
    }

    /**
     * Value change event.
     *
     * @param xpath affected group or property xpath
     * @param changeType type of change
     */
    void onValueChanged(String xpath, ChangeType changeType);

    /**
     * An arbitrary change of an element's XPath (e.g. because the index was changed).
     *
     * @param xpathFrom    the previous XPath
     * @param xpathTo      the new XPath
     *
     * @since 3.2.1
     * @see com.flexive.shared.content.FxData#getXPathFull()
     */
    void onXPathChanged(String xpathFrom, String xpathTo);

    /**
     * Triggered when a call to {@link com.flexive.shared.content.FxContent#compact()} or
     * {@link com.flexive.shared.content.FxData#compact()} was started. This may be necessary
     * for syncing external data structures that are based on XPaths that may be changed by compact.
     * Since compacting groups is recursive, a single #compact call generally leads to multiple nested
     * start/complete events.
     *
     * @since 3.2.1
     */
    void onCompactStarted();

    /**
     * Triggered when a call to {@link com.flexive.shared.content.FxContent#compact()} or
     * {@link com.flexive.shared.content.FxData#compact()} was completed. This may be necessary
     * for syncing external data structures that are based on XPaths that may be changed by compact.
     *
     * @since 3.2.1
     */
    void onCompactCompleted();
}
