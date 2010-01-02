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
package com.flexive.shared.exceptions;

import org.apache.commons.logging.Log;

/**
 * Exception that can occur whenever an invalid parameter was passed (localized).
 * The name of the parameter is (always) the first parameter of any constructor and
 * can be retrieved with <code>getParameter()</code>.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxInvalidParameterException extends FxApplicationException {

    private String parameter;
    private static final long serialVersionUID = -2833608098491155070L;

    /**
     * Get the name of the parameter flagged as invalid
     *
     * @return name of the parameter flagged as invalid
     */
    public String getParameter() {
        return parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param converted
     */
    public FxInvalidParameterException(String parameter, FxApplicationException converted) {
        super(converted);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param converted
     */
    public FxInvalidParameterException(String parameter, Log log, FxApplicationException converted) {
        super(log, converted);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param key
     * @param values
     */
    public FxInvalidParameterException(String parameter, String key, Object... values) {
        super(key, values);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param key
     * @param values
     */
    public FxInvalidParameterException(String parameter, Log log, String key, Object... values) {
        super(log, key, values);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param cause
     * @param key
     * @param values
     */
    public FxInvalidParameterException(String parameter, Throwable cause, String key, Object... values) {
        super(cause, key, values);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param cause
     * @param key
     * @param values
     */
    public FxInvalidParameterException(String parameter, Log log, Throwable cause, String key, Object... values) {
        super(log, cause, key, values);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param key
     */
    public FxInvalidParameterException(String parameter, String key) {
        super(key);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param key
     */
    public FxInvalidParameterException(String parameter, Log log, String key) {
        super(log, key);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param message
     * @param cause
     */
    public FxInvalidParameterException(String parameter, String message, Throwable cause) {
        super(message, cause);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param message
     * @param cause
     */
    public FxInvalidParameterException(String parameter, Log log, String message, Throwable cause) {
        super(log, message, cause);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param cause
     */
    public FxInvalidParameterException(String parameter, Throwable cause) {
        super(cause);
        this.parameter = parameter;
    }

    /**
     * Localized exception constructor
     *
     * @param log
     * @param cause
     */
    public FxInvalidParameterException(String parameter, Log log, Throwable cause) {
        super(log, cause);
        this.parameter = parameter;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FxInvalidParameterException setAffectedXPath(String affectedXPath, FxContentExceptionCause cause) {
        super.setAffectedXPath(affectedXPath, cause);
        return this;
    }
}
