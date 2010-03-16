/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.javascript;

import com.flexive.shared.*;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.scripting.FxScriptRunInfo;

import java.io.Serializable;
import java.util.Date;

/**
 * SystemInformation for JSON calls
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SystemInformation implements Serializable {

    private static final long serialVersionUID = 8418186403485689042L;

    long firstCall = -1;

    /**
     * Get the current initialization status of flexive
     *
     * @return initialization status
     */
    public String getInitializationStatus() {
        if (firstCall == -1)
            firstCall = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder(1000);
        sb.append("<table class=\"initTable\">");
        try {
            sb.append("<tr><td class=\"initHeader2\">Status</td><td class=\"initHeader2\">Application</td><td class=\"initHeader2\">Script</td><td class=\"initHeader2\">Duration</td></tr>");
            for (FxScriptRunInfo ri : EJBLookup.getScriptingEngine().getRunOnceInformation()) {
                String duration = ri.isRunning()
                        ? "Started " + FxFormatUtils.getDateTimeFormat().format(new Date(ri.getStartTime())) + " ..."
                        : FxFormatUtils.formatTimeSpan(ri.getEndTime() - ri.getStartTime()) + "";
                sb.append("<tr class=\"initRow").append(ri.isRunning() ? "Running" : "").append("\">\n");
                if (ri.isRunning())
                    sb.append("  <td class=\"initStatusRunning\">Running");
                else if (ri.isSuccessful())
                    sb.append("<td class=\"initStatusOK\">OK");
                else if (!ri.isSuccessful())
                    sb.append("<td class=\"initStatusError\">ERROR");
                sb.append("</td><td class=\"initDrop\">");
                sb.append(getDropLabel(ri)).append("</td><td class=\"initName\">").append(ri.getName());
                sb.append("</td><td class=\"initDuration\">").append(duration).append("</td>\n");
                sb.append("</tr>\n");
            }
            sb.append("</table>\n");
        } catch (FxApplicationException e) {
            sb.append("Failed to retrieve information: ").append(e.getMessage(FxContext.getUserTicket()));
        }
        return sb.toString();
    }

    private String getDropLabel(FxScriptRunInfo ri) {
        try {
            return FxSharedUtils.getDropApplication(ri.getDrop()).getDisplayName();
        } catch (FxRuntimeException e) {
            return ri.getDrop();
        }
    }

    /**
     * Is [fleXive] still initializing?
     *
     * @return still initializing?
     */
    public boolean isInitializing() {
        return !CacheAdmin.isEnvironmentLoaded();
    }

    /**
     * Initialize environment by accessing it ...
     */
    public void initEnvironment() {
        CacheAdmin.getEnvironment();
    }

}
