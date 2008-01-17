/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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

import org.apache.commons.logging.Log;

/**
 * Exception thrown by the enchanced nested set tree implementation
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxTreeException extends FxApplicationException {
    private static final long serialVersionUID = -4189082196760239284L;

    /**
     * Localized exception constructor
     *
     * @param converted exception to convert to a FxTreeException
     */
    public FxTreeException(FxApplicationException converted) {
        super(converted);
    }

    /**
     * Localized exception constructor
     *
     * @param log       logger
     * @param converted exception to convert to a FxTreeException
     */
    public FxTreeException(Log log, FxApplicationException converted) {
        super(log, converted);
    }

    /**
     * Localized exception constructor
     *
     * @param key    message key
     * @param values message parameters
     */
    public FxTreeException(String key, Object... values) {
        super(key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param log    logger
     * @param key    message key
     * @param values message parameters
     */
    public FxTreeException(Log log, String key, Object... values) {
        super(log, key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param cause  original cause
     * @param key    message key
     * @param values message parameters
     */
    public FxTreeException(Throwable cause, String key, Object... values) {
        super(cause, key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param log    logger
     * @param cause  original cause
     * @param key    message key
     * @param values message parameters
     */
    public FxTreeException(Log log, Throwable cause, String key, Object... values) {
        super(log, cause, key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param key message key
     */
    public FxTreeException(String key) {
        super(key);
    }

    /**
     * Localized exception constructor
     *
     * @param log logger
     * @param key message key
     */
    public FxTreeException(Log log, String key) {
        super(log, key);
    }

    /**
     * Localized exception constructor
     *
     * @param message message
     * @param cause   original cause
     */
    public FxTreeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Localized exception constructor
     *
     * @param log     logger
     * @param message message
     * @param cause   original cause
     */
    public FxTreeException(Log log, String message, Throwable cause) {
        super(log, message, cause);
    }

    /**
     * Localized exception constructor
     *
     * @param cause original cause
     */
    public FxTreeException(Throwable cause) {
        super(cause);
    }

    /**
     * Localized exception constructor
     *
     * @param log   logger
     * @param cause original cause
     */
    public FxTreeException(Log log, Throwable cause) {
        super(log, cause);
    }
}
