/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.faces.messages;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FxFacesMsgErr extends FxFacesMessage {

    private static final Log LOG = LogFactory.getLog(FxFacesMsgErr.class);
    private static final long serialVersionUID = -3804224752720667187L;
    private static final Severity SEVERITY = SEVERITY_ERROR;

    /**
     * Create a JSF error message. To render it to the client,
     * you have to call {@link #addToContext()} or {@link #addToContext(String)}.
     * The message will be logged by the logger defined for this class.
     *
     * @param summaryKey    the summary message key
     * @param summaryParams the values to be placed in the summary message
     */
    public FxFacesMsgErr(String summaryKey, Object... summaryParams) {
        super(SEVERITY, summaryKey, summaryParams);
    }

    /**
     * Create a JSF error message from an exception. To render it to the client,
     * you have to call {@link #addToContext()} or {@link #addToContext(String)}.
     * The message will be logged by the logger defined for this class.
     *
     * @param exc           the exception to be displayed
     * @param summaryKey    the summary message key
     * @param summaryParams the values to be placed in the summary message
     */
    public FxFacesMsgErr(Throwable exc, String summaryKey, Object... summaryParams) {
        super(exc, SEVERITY, summaryKey, summaryParams);
        logMessage(exc);
    }

    /**
     * Create a JSF error message from an exception. To render it to the client,
     * you have to call {@link #addToContext()} or {@link #addToContext(String)}.
     * The message will be logged by the logger defined for this class.
     *
     * @param exc           the exception to be displayed
     */
    public FxFacesMsgErr(Throwable exc) {
        super(exc, SEVERITY);
        logMessage(exc);
    }

    /**
     * Create a JSF error message from an exception. To render it to the client,
     * you have to call {@link #addToContext()} or {@link #addToContext(String)}.
     *
     * @param exc           the exception to be displayed
     * @param logMessage    if the message should be logged (unless it already has been logged)
     * @since 3.1
     */
    public FxFacesMsgErr(Throwable exc, boolean logMessage) {
        super(exc, SEVERITY);
        if (logMessage) {
            logMessage(exc);
        }
    }

    /**
     * Log the exception if it has not been already
     *
     * @param exc exception
     */
    private void logMessage(Throwable exc) {
        if (!(exc instanceof FxApplicationException && ((FxApplicationException) exc).isMessageLogged())
            && !(exc instanceof FxRuntimeException && ((FxRuntimeException) exc).getConverted().isMessageLogged())) {

            LOG.warn("Faces error:" + exc.getMessage(), exc);
        }
    }

    /**
     * This function has no effect on the object, since the object always represents a ERROR message
     */
    public void setSeverity() {
        // nothing
    }
}
