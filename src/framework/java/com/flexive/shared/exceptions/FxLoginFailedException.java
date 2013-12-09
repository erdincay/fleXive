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
package com.flexive.shared.exceptions;

import javax.security.auth.login.LoginException;

/**
 * Exception that is thrown when a login fails.
 * This is the only exception that does not extend FxException since it is used in the JAAS login module.
 * Localization is performed in the struts action classes using the <code>type</code>
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxLoginFailedException extends LoginException {

    private static final long serialVersionUID = 6665899900333137586L;
    final public static byte TYPE_USER_OR_PASSWORD_NOT_DEFINED = 0;
    final public static byte TYPE_INACTIVE_ACCOUNT = 1;
    final public static byte TYPE_UNKNOWN_ERROR = 2;

    final public static byte TYPE_SQL_ERROR = 3;
    private byte type = -1;

    /**
     * Constructor.
     *
     * @param msg  the message for the exception
     * @param type the type, may be TYPE_INACTIVE_ACCOUNT or TYPE_USER_OR_PASSWORD_NOT_DEFINED
     */
    public FxLoginFailedException(String msg, byte type) {
        super(msg);
        this.type = type;
    }

    /**
     * Returns the type, may be TYPE_INACTIVE_ACCOUNT or TYPE_USER_OR_PASSWORD_NOT_DEFINED.
     *
     * @return the type, may be TYPE_INACTIVE_ACCOUNT or TYPE_USER_OR_PASSWORD_NOT_DEFINED
     */
    public byte getType() {
        return this.type;
  }



}
