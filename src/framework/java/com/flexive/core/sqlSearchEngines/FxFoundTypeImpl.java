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
package com.flexive.core.sqlSearchEngines;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.search.FxFoundType;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxString;

import java.io.Serializable;

/**
 * Helper for found types
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxFoundTypeImpl implements Serializable, FxFoundType {
    private static final long serialVersionUID = 4568969162913339997L;
    private long contentTypeId;
    private int foundEntries;
    private FxString description;
    private String displayName;

    /**
     * Constructor.
     *
     * @param contentTypeId the content type id
     * @param foundEntries  the amount of entries found by the query
     */
    public FxFoundTypeImpl(long contentTypeId, int foundEntries) {
        FxType type = CacheAdmin.getEnvironment().getType(contentTypeId);
        this.contentTypeId = contentTypeId;
        this.foundEntries = foundEntries;
        this.displayName = type.getDisplayName();
        this.description = type.getDescription();
    }

    /**
     * {@inheritDoc} *
     */
    public long getContentTypeId() {
        return contentTypeId;
    }

    /**
     * {@inheritDoc} *
     */
    public int getFoundEntries() {
        return foundEntries;
    }

    /**
     * {@inheritDoc} *
     */
    public FxString getDescription() {
        return description;
    }

    /**
     * {@inheritDoc} *
     */
    public String getDisplayName() {
        return displayName;
    }
}
