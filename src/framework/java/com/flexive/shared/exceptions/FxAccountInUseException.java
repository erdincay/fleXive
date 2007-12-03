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
package com.flexive.shared.exceptions;

import javax.security.auth.login.LoginException;
import java.util.Date;

/**
 * Exception that is thrown when a account is already in use.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxAccountInUseException extends LoginException {

    private static final long serialVersionUID = 7479624129531481651L;
    private String loggedInFrom = null;
    private Date loggedInSince = null;

    /**
     * Constructor.
     *
     * @param username      user name
     * @param loggedInFrom  where is the user logged in from
     * @param loggedInSince login timestamp
     */
    public FxAccountInUseException(String username, String loggedInFrom, Date loggedInSince) {
        super("The account [" + username + "] is already active");
        this.loggedInFrom = loggedInFrom;
        this.loggedInSince = (Date) loggedInSince.clone();
    }

    /**
     * Returns the client address that the account is beeing used from.
     *
     * @return the client address that the account is beeing used from.
     */
    public String getLoggedInFrom() {
        return this.loggedInFrom;
    }

    public Date getLoggedInSince() {
        return (Date) this.loggedInSince.clone();
    }

}
