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
package com.flexive.shared.scripting;

import java.util.Date;

/**
 * Class for editing a script schedule
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com)
 * @since 3.1.2
 */
public class FxScriptScheduleEdit extends FxScriptSchedule {

    /**
     * Creates a repeating script schedule
     * that repeats between the specified start time and end time
     * with the given repeat interval.
     *
     * @param scriptId      scheduled script id
     * @param name script   schedule name
     * @param active        active flag
     * @param startTime     start time
     * @param endTime       end time
     * @param repeatInterval repeat interval in ms
     */
    public FxScriptScheduleEdit(long scriptId, String name, boolean active,
                                Date startTime, Date endTime, long repeatInterval) {
        super(-1,scriptId,name, active, startTime, endTime,
                repeatInterval,FxScriptSchedule.REPEAT_TIMES_UNBOUNDED,null);
    }

    /**
     * Creates a repeating script schedule
     * that repeats between the specified start time and end time
     * according to the specified cron String.
     *
     * @param scriptId      scheduled script id
     * @param name script   schedule name
     * @param active        active flag
     * @param startTime     start time
     * @param endTime       end time
     * @param cronString    cron String
     */
    public FxScriptScheduleEdit(long scriptId, String name, boolean active,
                                Date startTime, Date endTime, String cronString) {
        super(-1,scriptId, name, active, startTime, endTime,
                -1, FxScriptSchedule.REPEAT_TIMES_UNBOUNDED, cronString);
    }

    /**
     * Creates a repeating script schedule
     * that repeats N times using the specified
     * interval starting at the specified start time.
     *
     * @param scriptId      scheduled script id
     * @param name script   schedule name
     * @param active        active flag
     * @param startTime     start time
     * @param repeatInterval repeat interval in ms
     * @param repeatTimes   number of times to repeat (after the script was first started)
     */
    public FxScriptScheduleEdit(long scriptId, String name, boolean active,
                                Date startTime, long repeatInterval, int repeatTimes) {
        super(-1,scriptId, name, active, startTime, null,
                repeatInterval,repeatTimes,null);
    }

    /**
     * Creates a 'run-once' script schedule that
     * runs at the specified start time.
     *
     * @param scriptId      scheduled script id
     * @param name          schedule name
     * @param active        active flag
     * @param startTime     start time
     */
    public FxScriptScheduleEdit(long scriptId, String name, boolean active, Date startTime) {
        super(-1,scriptId, name, active, startTime, null,
                -1,0,null);
    }

    /**
     * Creates an editable script schedule
     * from an existing script schedule
     *
     * @param ss existing script schedule
     */
    public FxScriptScheduleEdit(FxScriptSchedule ss) {
        this.id = ss.id;
        this.scriptId = ss.scriptId;
        this.name = ss.name;
        this.active = ss.active;
        this.startTime = ss.startTime;
        this.endTime = ss.endTime;
        this.repeatTimes = ss.repeatTimes;
        this.repeatInterval = ss.repeatInterval;
        this.cronString = ss.cronString;
    }

    public void setScriptId(long scriptId) {
        this.scriptId = scriptId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime != null ? (Date) startTime.clone() : null;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime != null ? (Date) endTime.clone() : null;
    }

    public void setRepeatInterval(long repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public void setRepeatTimes(int repeatTimes) {
        this.repeatTimes = repeatTimes;
    }

    public void setCronString(String cronString) {
        this.cronString = cronString;
    }

    public void setId(long id) {
        this.id = id;
    }
    
}
