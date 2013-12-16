/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
package com.flexive.shared.search;

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.value.FxString;

/**
 * Administration area result locations
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public enum AdminResultLocations implements ResultLocation {
    /**
     * Main admin search results
     */
    ADMIN(false),
    /**
     * Default location to be used when no location is specified
     */
    DEFAULT(false),
    /**
     * Browse references popup (FxReference input helper)
     */
    BROWSE_REFERENCES(false);

    private final boolean cacheInSession;

    AdminResultLocations(boolean cacheInSession) {
        this.cacheInSession = cacheInSession;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxString getLabel() {
        return FxSharedUtils.getEnumLabel(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return getClass().getName() + "." + name();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isCacheInSession() {
        return cacheInSession;
    }
}
