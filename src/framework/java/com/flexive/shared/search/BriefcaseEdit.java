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

import com.flexive.shared.security.LifeCycleInfo;

/**
 * An editable briefcase
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class BriefcaseEdit extends Briefcase {
    private static final long serialVersionUID = -2321222840214504878L;

    /**
     * Create a new editable briefcase object.
     *
     * @param bc    the template object, e.g. a briefcase retrieved from the database
     */
    public BriefcaseEdit(Briefcase bc) {
        super(bc.getId(), bc.getName(), bc.getMandator(), bc.getDescription(), bc.getSourceQuery(), bc.getAcl(),
                bc.getLifeCycleInfo(), bc.getIconId(), bc.getSize());
    }

    /**
     * Creates a blank editable briefcase object.
     */
    public BriefcaseEdit() {
        super();
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setSourceQuery(String sourceQuery) {
        this.sourceQuery = sourceQuery;
    }

    public void setAcl(long acl) {
        this.acl = acl;
    }

    public void setLifeCycleInfo(LifeCycleInfo lifeCycleInfo) {
        this.lifeCycleInfo = lifeCycleInfo;
    }

    public void setMandator(long mandator) {
        this.mandator = mandator;
    }

    public void setIconId(long iconId) {
        this.iconId = iconId;
    }

    public void setSize(int size) {
        this.size = size;
    }
    
}
