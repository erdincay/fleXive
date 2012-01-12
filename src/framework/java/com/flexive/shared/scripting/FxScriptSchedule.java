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

import com.flexive.shared.SelectableObjectWithName;

import java.io.Serializable;
import java.util.Date;

/**
 * Class describing a script schedule
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com)
 * @since 3.1.2
 */
public class FxScriptSchedule implements Serializable, SelectableObjectWithName {
    private static final long serialVersionUID = -7568151422862806766L;
    
    /**
     * Constant designating unbounded repeat times<br/>
     * (useful for script schedules that should run up to the specified end time,
     * or indefinitely)
     */
    public final static int REPEAT_TIMES_UNBOUNDED = -1;
    protected int repeatTimes = REPEAT_TIMES_UNBOUNDED;
    protected long id = -1;
    protected long scriptId;
    protected String name;
    protected boolean active;
    protected Date startTime;
    protected Date endTime;
    protected long repeatInterval = -1;
    protected String cronString;

    protected FxScriptSchedule() {
        /* default constructor
        *  intentionally not public
        * (only used by subclass(es))
        */
    }

    /**
     * Constructor
     *
     * @param id                script schedule id
     * @param scriptId          script id
     * @param name              script schedule name
     * @param active            active flag
     * @param startTime         start time
     * @param endTime           end time
     * @param repeatInterval    repeat interval in ms
     * @param repeatTimes       number of repeat times
     * @param cronString        cron String
     */
    public FxScriptSchedule(long id, long scriptId, String name,
                            boolean active, Date startTime, Date endTime,
                            long repeatInterval, int repeatTimes, String cronString) {
        this.id = id;
        this.scriptId = scriptId;
        this.name = name;
        this.active = active;
        this.startTime = startTime != null ? (Date) startTime.clone() : null;
        this.endTime = endTime != null ? (Date) endTime.clone() : null;
        this.repeatTimes = repeatTimes;
        this.repeatInterval = repeatInterval;
        this.cronString = cronString;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public int getRepeatTimes() {
        return repeatTimes;
    }

    public long getRepeatInterval() {
        return repeatInterval;
    }

    public String getCronString() {
        return cronString;
    }

    public FxScriptScheduleEdit asEditable() {
        return new FxScriptScheduleEdit(this);
    }

    public long getScriptId() {
        return scriptId;
    }

    public boolean isUnbounded() {
        return repeatTimes == REPEAT_TIMES_UNBOUNDED;
    }
}
