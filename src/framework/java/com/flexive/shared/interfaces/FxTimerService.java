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
package com.flexive.shared.interfaces;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.scripting.FxScriptSchedule;

import javax.ejb.Remote;

/**
 * Timer- and scheduling service based on Quartz
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Remote
public interface FxTimerService {

    /**
     * Installs the timer service
     *
     * @param reinstall reinstall the timer if it is already installed?
     * @return if successful (should only fail in early versions of embedded containers!)
     */
    boolean install(boolean reinstall);

    /**
     * Uninstalls the timer service
     */
    void uninstall();

    /**
     * Check if the timer service is installed
     *
     * @return <code>true</code> if timer service is installed
     */
    boolean isInstalled();

    /**
     * perform maintenance
     */
    void maintenance();

    /**
     * Schedule a script
     *
     * @param scriptSchedule            script schedule
     * @throws FxApplicationException   on errors
     * @since 3.1.2
     */
    void scheduleScript(FxScriptSchedule scriptSchedule) throws FxApplicationException;

    /**
     * Update a scheduled script
     *
     * @param scriptSchedule            script schedule
     * @throws FxApplicationException   on errors
     * @since 3.1.2
     */
    void updateScriptSchedule(FxScriptSchedule scriptSchedule) throws FxApplicationException;

    /**
     * Delete a script schedule
     *
     * @param scriptSchedule            script schedule
     * @throws FxApplicationException   on errors
     * @since 3.1.2
     * @return  true if script schedule was found and could be deleted
     */
    boolean deleteScriptSchedule(FxScriptSchedule scriptSchedule) throws FxApplicationException;

    /**
     * Parses a Cron String and throws an exception
     * if it cannot be parsed
     *
     * @param cronString  Cron String
     * @since 3.1.2
     * @throws com.flexive.shared.exceptions.FxInvalidParameterException on errors
     */
    void parseCronString(String cronString) throws FxInvalidParameterException;
}
