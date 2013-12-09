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

import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxString;

import java.io.Serializable;

/**
 * A found FxType
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxFoundType implements Serializable {
    private static final long serialVersionUID = 4568969162913339997L;

    private final long contentTypeId;
    private final int foundEntries;
    private final FxString description;
    private final String displayName;

    /**
     * Constructor.
     *
     * @param type the content type
     * @param foundEntries  the amount of entries found by the query
     */
    public FxFoundType(FxType type, int foundEntries) {
        this.contentTypeId = type.getId();
        this.foundEntries = foundEntries;
        this.displayName = type.getDisplayName();
        this.description = type.getLabel();
    }

    /**
     * Returns the content type id.
     *
     * @return the content type name
     */
    public long getContentTypeId() {
        return contentTypeId;
    }

    /**
     * Returns the amount of items found by the query.
     *
     * @return the amount of items found by the quer
     */
    public int getFoundEntries() {
        return foundEntries;
    }

    /**
     * Returns the description of the content type.
     *
     * @return the description of the content type
     */
    public FxString getDescription() {
        return description;
    }

    /**
     * Returns the display name of the content type.
     *
     * @return the display name of the content type
     */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FxFoundType other = (FxFoundType) obj;
        if (this.contentTypeId != other.contentTypeId) {
            return false;
        }
        if (this.foundEntries != other.foundEntries) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41 * hash + (int) (this.contentTypeId ^ (this.contentTypeId >>> 32));
        hash = 41 * hash + this.foundEntries;
        return hash;
    }
}
