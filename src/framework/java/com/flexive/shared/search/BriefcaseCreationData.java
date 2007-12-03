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
package com.flexive.shared.search;

import com.flexive.shared.FxContext;
import com.flexive.shared.security.UserTicket;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Envelope to carry data needed for a briefcase creation.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class BriefcaseCreationData implements Serializable {

    private static final long serialVersionUID = -1050668347506270241L;
    private Long aclId;
    private String description;
    private String name;

    /**
     * Constructor.
     *
     * @param name the name of the briefcase, in case of null or a empty String a name will be constructed
     * @param description the description
     * @param aclId the acl the briefcase is using, or null if the briefcase is not shared
     */
    public BriefcaseCreationData(String name,String description,Long aclId) {
        this.description = description==null?"":description;
        this.name = name;
        if (name==null) {
            final UserTicket ticket = FxContext.get().getTicket();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
            this.name = ticket.getUserName()+"_"+sdf.format(new Date());
        }
        this.aclId = aclId;
    }

    /**
     * Returns the acl that the briefcase is using, or null if the briefcase is not shared.
     *
     * @return the aclId, or null if the briefcase is not shared
     */
    public Long getAclId() {
        return aclId;
    }

    /**
     * The briefcase description (may be empty).
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * The briefcase name.
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }
}
