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
package com.flexive.shared.search;

import com.flexive.shared.AbstractSelectableObjectWithName;
import com.flexive.shared.security.LifeCycleInfo;

import java.io.Serializable;

/**
 * The briefcase info object.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class Briefcase extends AbstractSelectableObjectWithName implements Serializable {
    private static final long serialVersionUID = -1594461846638190701L;

    protected long id;
    protected String name;
    protected String description;
    protected String sourceQuery;
    protected long acl;
    protected LifeCycleInfo lifeCycleInfo;
    protected long mandator;
    protected long iconId;
    protected int size;

    protected Briefcase() {
        this.id = -1;
        this.name = "";
        this.description = "";
        this.sourceQuery = null;
        this.acl = -1;
        this.lifeCycleInfo = null;
        this.mandator = -1;
        this.iconId = -1;
        this.size = 0;
    }

    public Briefcase(long id, String name, long mandator, String description,
                     String sourceQuery, long acl, LifeCycleInfo lifeCycleInfo, long iconId, int size) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.sourceQuery = sourceQuery;
        this.acl = acl;
        this.lifeCycleInfo = lifeCycleInfo;
        this.mandator = mandator;
        this.iconId = iconId;
        this.size = size;
    }


    public long getIconId() {
        return iconId;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }


    public String getDescription() {
        return description;
    }

    public String getSourceQuery() {
        return sourceQuery;
    }

    public long getAcl() {
        return acl;
    }

    public LifeCycleInfo getLifeCycleInfo() {
        return lifeCycleInfo;
    }

    public long getMandator() {
        return mandator;
    }

    public int getSize() {
        return size;
    }

    public BriefcaseEdit asEditable() {
        return new BriefcaseEdit(this);
    }
}
