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

import org.apache.commons.logging.Log;

/**
 * Exception thrown if something can not be converted (ie FxValue's fromString() failed)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxConversionException extends FxApplicationException {
    private static final long serialVersionUID = -5433389482608308847L;


    /**
     * Localized exception constructor
     *
     * @param converted the message to convert
     */
    public FxConversionException(FxApplicationException converted) {
        super(converted);
    }

    /**
     * Localized exception constructor
     *
     * @param log       Log to use
     * @param converted exception to convert
     */
    public FxConversionException(Log log, FxApplicationException converted) {
        super(log, converted);
    }

    /**
     * Localized exception constructor
     *
     * @param key    exception messge resource key
     * @param values value parameters for resource message
     */
    public FxConversionException(String key, Object... values) {
        super(key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param log    Log to use
     * @param key    exception messge resource key
     * @param values value parameters for resource message
     */
    public FxConversionException(Log log, String key, Object... values) {
        super(log, key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param cause  causing exception
     * @param key    exception messge resource key
     * @param values value parameters for resource message
     */
    public FxConversionException(Throwable cause, String key, Object... values) {
        super(cause, key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param log    Log to use
     * @param cause  causing message
     * @param key    exception messge resource key
     * @param values value parameters for resource message
     */
    public FxConversionException(Log log, Throwable cause, String key, Object... values) {
        super(log, cause, key, values);
    }

    /**
     * Localized exception constructor
     *
     * @param key exception messge resource key
     */
    public FxConversionException(String key) {
        super(key);
    }

    /**
     * Localized exception constructor
     *
     * @param log Log to use
     * @param key exception messge resource key
     */
    public FxConversionException(Log log, String key) {
        super(log, key);
    }

    /**
     * Localized exception constructor
     *
     * @param message resource key
     * @param cause   causing exception
     */
    public FxConversionException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Localized exception constructor
     *
     * @param log     Log to use
     * @param message resource key
     * @param cause   exception cause
     */
    public FxConversionException(Log log, String message, Throwable cause) {
        super(log, message, cause);
    }

    /**
     * Localized exception constructor
     *
     * @param cause exception cause
     */
    public FxConversionException(Throwable cause) {
        super(cause);
    }

    /**
     * Localized exception constructor
     *
     * @param log   Log to use
     * @param cause causing exception
     */
    public FxConversionException(Log log, Throwable cause) {
        super(log, cause);    
    }
}
