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

import com.flexive.shared.FxLanguage;
import com.flexive.shared.security.UserTicket;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * Runtime exception base class, wrapping a checked flexive exception.
 *  
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxRuntimeException extends RuntimeException implements FxLocalizedException {
    private static final long serialVersionUID = -5446816760325991049L;

    protected FxApplicationException converted;
    /**
     * was the message logged?
     */
    private boolean messageLogged = false;

    /**
     * Localized exception constructor. Package-protected - 
     * use {@link FxApplicationException#asRuntimeException()} for creating
     * run-time exceptions.
     *
     * @param converted the message to convert
     */
    FxRuntimeException(FxApplicationException converted) {
        super(converted.message.getKey());
        this.converted = converted;
    }

    /**
     * Has the message been logged?
     *
     * @return message logged
     */
    protected boolean messageLogged() {
        return this.messageLogged;
    }

    @Override
    public StackTraceElement[] getStackTrace() {
        // retain original stacktrace
        return converted.getStackTrace();
    }

    @Override
    public void printStackTrace(PrintStream s) {
        setStackTrace(getStackTrace());
        super.printStackTrace(s);
    }

    @Override
    public void printStackTrace(PrintWriter s) {
        setStackTrace(getStackTrace());
        super.printStackTrace(s);
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage() {
        return converted.getMessage();
    }


    /** {@inheritDoc} */
    public String getMessage(FxLanguage locale) {
        return converted.getMessage(locale);
    }

    /** {@inheritDoc} */
    public String getMessage(long localeId) {
        return converted.getMessage(localeId);
    }

    /** {@inheritDoc} */
    public String getMessage(UserTicket ticket) {
    	return converted.getMessage(ticket);
    }

    /** {@inheritDoc} */
    public FxExceptionMessage getExceptionMessage() {
        return converted.getExceptionMessage();
    }

    public FxApplicationException getConverted() {
        return this.converted;
    }
}
