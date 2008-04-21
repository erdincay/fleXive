/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FxFacesMsgErr extends FxFacesMessage {

    private static transient Log LOG = LogFactory.getLog(FxFacesMsgErr.class);
    private static final long serialVersionUID = -3804224752720667187L;
    private static final Severity SEVERITY = SEVERITY_ERROR;

    public FxFacesMsgErr(String summaryKey, Object... summaryParams) {
        super(SEVERITY, summaryKey, summaryParams);
    }

    public FxFacesMsgErr(Throwable exc, String summaryKey, Object... summaryParams) {
        super(exc, SEVERITY, summaryKey, summaryParams);
        logMessage(exc);
    }

    public FxFacesMsgErr(Throwable exc) {
        super(exc, SEVERITY);
        logMessage(exc);
    }

    /**
     * Log the exception if it has not been already
     *
     * @param exc exception
     */
    private void logMessage(Throwable exc) {
        if (exc instanceof FxApplicationException)
            if (((FxApplicationException) exc).isMessageLogged())
                return;
        LOG.warn("Faces error:" + exc.getMessage(), exc);
    }

    /**
     * This function has no effect on the object, since the object always represents a ERROR message
     */
    public void setSeverity() {
        // nothing
    }
}
