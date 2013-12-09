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

import org.apache.commons.logging.Log;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

public class FxEntryInUseException extends FxApplicationException {
    private static final long serialVersionUID = -7833477602386345832L;

    public FxEntryInUseException(FxApplicationException converted) {
        super(converted);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(Log log, FxApplicationException converted) {
        super(log, converted);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(Log log, String key, Object... values) {
        super(log, key, values);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(Log log, String message, Throwable cause) {
        super(log, message, cause);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(Log log, String key) {
        super(log, key);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(Log log, Throwable cause, String key, Object... values) {
        super(log, cause, key, values);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(Log log, Throwable cause) {
        super(log, cause);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(String key, Object... values) {
        super(key, values);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(String message, Throwable cause) {
        super(message, cause);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(String key) {
        super(key);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(Throwable cause, String key, Object... values) {
        super(cause, key, values);
        // TODO Auto-generated constructor stub
    }

    public FxEntryInUseException(Throwable cause) {
        super(cause);
        // TODO Auto-generated constructor stub
    }

}
