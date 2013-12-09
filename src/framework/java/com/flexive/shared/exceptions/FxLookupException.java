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

public class FxLookupException extends FxApplicationException {
    private static final long serialVersionUID = -8062345606391467444L;


    /**
     * Localized exception constructor
     *
     * @param converted
     */
    public FxLookupException(FxApplicationException converted) {
        super(converted);
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param converted
     */
    public FxLookupException(Log log, FxApplicationException converted) {
        super(log, converted);
    }

    /**
     * Localized exception constructor
     *
     * @param key
     * @param values
     */
    public FxLookupException(String key, Object... values) {
        super(key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param key
     * @param values
     */
    public FxLookupException(Log log, String key, Object... values) {
        super(log, key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param cause
     * @param key
     * @param values
     */
    public FxLookupException(Throwable cause, String key, Object... values) {
        super(cause, key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param cause
     * @param key
     * @param values
     */
    public FxLookupException(Log log, Throwable cause, String key, Object... values) {
        super(log, cause, key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param key
     */
    public FxLookupException(String key) {
        super(key);
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param key
     */
    public FxLookupException(Log log, String key) {
        super(log, key);
    }

    /**
     * Localized exception constructor
     *
     * @param message
     * @param cause
     */
    public FxLookupException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param message
     * @param cause
     */
    public FxLookupException(Log log, String message, Throwable cause) {
        super(log, message, cause);
    }

    /**
     * Localized exception constructor
     *
     * @param cause
     */
    public FxLookupException(Throwable cause) {
        super(cause);
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param cause
     */
    public FxLookupException(Log log, Throwable cause) {
        super(log, cause);
    }

}
