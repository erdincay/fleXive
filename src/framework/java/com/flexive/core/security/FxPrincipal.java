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
package com.flexive.core.security;

import com.flexive.shared.security.UserTicket;

import java.security.Principal;

/**
 * Flexive security principal.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxPrincipal implements Principal, java.io.Serializable {

    private static final long serialVersionUID = 6997956811428518235L;
    private UserTicket ticket = null;

    /**
     * Constructs a new FlexivePrincipal.
     *
     * @param ticket the user ticket
     */
    public FxPrincipal(UserTicket ticket) {
        this.ticket = ticket;
    }


    /**
     * Returns the user name for this FlexivePrincipal.
     * The name can also be retrieved using getUserTicket.getName(), this function
     * serves as a shorcut.
     *
     * @return the user name for this FlexivePrincipal
     */
    public String getName() {
        return this.ticket.getUserName();
    }

    /**
     * Returns the unique user id for this FlexivePrincipal.
     * The id can also be retrieved using getUserTicket.getId(), this function
     * serves as a shorcut.
     *
     * @return the unique user id for this FlexivePrincipal
     */
    public long getId() {
        return this.ticket.getUserId();
    }

    /**
     * Returns the UserTicket for this FlexivePrincipal.
     *
     * @return the UserTicket for this FlexivePrincipal
     */
    public UserTicket getUserTicket() {
        return this.ticket;
    }


    /**
     * Returns a string representation of the FlexivePrincipal.
     *
     * @return a string representation of the FlexivePrincipal.
     */
    @Override
    public String toString() {
        if (this.ticket == null) {
            return FxPrincipal.class + "@[userId:null;ticket:null]";
        } else {
            return this.ticket.toString();
        }
    }

}
