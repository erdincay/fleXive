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
package com.flexive.shared.scripting;

import java.io.Serializable;

/**
 * Information about an executed script - currently only used to track runOnce scripts ...
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxScriptRunInfo implements Serializable {
    private static final long serialVersionUID = 2668881060310417915L;
    
    private long startTime, endTime;
    private String drop;
    private String name;
    private boolean successful;
    private String errorMessage = null;

    public FxScriptRunInfo(long startTime, String drop, String name) {
        this.startTime = startTime;
        this.drop = drop;
        this.name = name;
        this.successful = false;
        this.endTime = -1;
    }

    /**
     * End execution of the script
     *
     * @param successful script executed successful?
     */
    public void endExecution(boolean successful) {
        this.endTime = System.currentTimeMillis()-100;
        this.successful = errorMessage == null && successful;
    }

    /**
     * Is the script still running?
     *
     * @return script still running?
     */
    public boolean isRunning() {
        return endTime == -1;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public String getDrop() {
        return drop;
    }

    public String getName() {
        return name;
    }

    public boolean isSuccessful() {
        return successful;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
