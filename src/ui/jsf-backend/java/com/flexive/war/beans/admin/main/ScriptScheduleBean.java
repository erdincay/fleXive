package com.flexive.war.beans.admin.main;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptSchedule;
import com.flexive.shared.scripting.FxScriptScheduleEdit;
import org.apache.commons.lang.StringUtils;

import javax.faces.model.SelectItem;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Bean for scheduling scripts
 *
 * @author Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class ScriptScheduleBean implements Serializable {
    private int overviewPageNumber;
    private int overviewRows;
    private String sortColumn;
    private String sortOrder;

    public static final String PAGE_ID_OVERVIEW="scriptScheduleOverview";

    private static final int SCHEDULE_TYPE_RUN_ONCE=0;
    private static final int SCHEDULE_TYPE_RECURRING=1;
    private static final int SCHEDULE_TYPE_CRON_SCRIPT=2;

    private static final int END_TIME_TYPE_FOREVER=0;
    private static final int END_TIME_TYPE_DATE=1;
    private static final int END_TIME_TYPE_REPEAT=2;

    private static final int UNIT_SECOND=0;
    private static final int UNIT_MINUTE=1;
    private static final int UNIT_HOUR=2;
    private static final int UNIT_DAY=3;
    private static final int UNIT_WEEK=4;

    private static final String DATE_TIME_PATTERN="yyyy-MM-dd HH:mm";

    private long id=-1;
    private FxScriptScheduleEdit schedule;
    private int scheduleType=0;
    private int endTimeType=0;
    private long repeatInterval=1;
    private int repeatIntervalUnit=3;
    private List<SelectItem> manualScripts = null;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public List<FxScriptSchedule> getScriptSchedules() {
        return CacheAdmin.getEnvironment().getScriptSchedules();
    }

    public static String getScriptNameForId(long id) {
        try {
            return CacheAdmin.getEnvironment().getScript(id).getName();
        }
        catch (Exception e) {
            //ignore
        }
        return "";
    }

    public String getSortColumn() {
        return sortColumn;
    }

    public void setSortColumn(String sortColumn) {
        this.sortColumn = sortColumn;
    }

    public String getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public int getOverviewRows() {
        return overviewRows;
    }

    public void setOverviewRows(int overviewRows) {
        this.overviewRows = overviewRows;
    }

    public int getOverviewPageNumber() {
        return overviewPageNumber;
    }

    public void setOverviewPageNumber(int overviewPageNumber) {
        this.overviewPageNumber = overviewPageNumber;
    }

    public String getDateTimePattern() {
        return DATE_TIME_PATTERN;
    }

    public String edit() {
        try {
            schedule=CacheAdmin.getEnvironment().getScriptSchedule(id).asEditable();
            // determine end time type
            if (schedule.getEndTime() == null && schedule.getRepeatTimes() == FxScriptSchedule.REPEAT_TIMES_UNBOUNDED)
                endTimeType = END_TIME_TYPE_FOREVER;
            else if (schedule.getEndTime() != null)
                endTimeType = END_TIME_TYPE_DATE;
            else
                endTimeType = END_TIME_TYPE_REPEAT;

            // determine schedule type
            if (schedule.getRepeatTimes() == 0 && schedule.getRepeatInterval() <0)
                scheduleType = SCHEDULE_TYPE_RUN_ONCE;
            else if (StringUtils.isBlank(schedule.getCronString()))
                scheduleType = SCHEDULE_TYPE_RECURRING;
            else
                scheduleType = SCHEDULE_TYPE_CRON_SCRIPT;

            //determine time interval unit
            if (schedule.getRepeatInterval() >0) {
                long sec = schedule.getRepeatInterval() /1000;
                repeatIntervalUnit = UNIT_SECOND;
                repeatInterval = sec;
                if (sec % 60 == 0) {
                    long min = sec / 60;
                    repeatIntervalUnit = UNIT_MINUTE;
                    repeatInterval = min;
                    if (min % 60 == 0) {
                        long h = min /60;
                        repeatIntervalUnit = UNIT_HOUR;
                        repeatInterval = h;
                        if (h % 24 == 0) {
                            long d = h/24;
                            repeatIntervalUnit = UNIT_DAY;
                            repeatInterval = d;
                            if (d % 7 ==0) {
                                long w = d/7;
                                repeatIntervalUnit = UNIT_WEEK;
                                repeatInterval = w;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
            return null;
        }
        return null;
    }

    private void setBeanDefaultValues() {
        scheduleType=0;
        endTimeType=0;
        repeatInterval=1;
        repeatIntervalUnit=3;
    }

    public void create() {
        // create new edit object and set default values
        schedule = new FxScriptScheduleEdit(-1,null,true,new Date(),new Date(),-1);
        schedule.setRepeatTimes(5);
        schedule.setCronString("0 0 0 ? * *");
        setBeanDefaultValues();
    }

    public void delete() {
        try {
            EJBLookup.getScriptingEngine().removeScriptSchedule(id);
        }
        catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    public void cancel() {
        schedule=null;
        id=-1;
        setBeanDefaultValues();
    }

    public List<SelectItem> getManualScripts() {
        // lazy init and request cache
        if (manualScripts == null) {
            List<FxScriptInfo> manualScripts = new ArrayList<FxScriptInfo>(0);
            for (FxScriptInfo i : CacheAdmin.getEnvironment().getScripts())
                if (i.getEvent() == FxScriptEvent.Manual)
                    manualScripts.add(i);
            Collections.sort(manualScripts, new FxJsfUtils.ScriptInfoSorter());
            this.manualScripts = FxJsfUtils.asSelectListWithName(manualScripts,false);
        }
        return manualScripts;
    }

    public int getScheduleType() {
        return scheduleType;
    }

    public void setScheduleType(int scheduleType) {
        this.scheduleType = scheduleType;
    }

    public int getEndTimeType() {
        return endTimeType;
    }

    public void setEndTimeType(int endTimeType) {
        this.endTimeType = endTimeType;
    }

    public void createSchedule() throws Exception {
        if (schedule == null) {
           throw new Exception("Unexpected Exception: schedule is null");
        }
        boolean createSchedule = schedule.getId() == -1;
        long editId = schedule.getId();
        try {
            // ui error checks
            if (endTimeType == END_TIME_TYPE_DATE && schedule.getEndTime() == null) {
                new FxFacesMsgErr("ScriptSchedule.error.endTime.empty").addToContext("panelMessages");
            }
            if (scheduleType == SCHEDULE_TYPE_CRON_SCRIPT && StringUtils.isBlank(schedule.getCronString())) {
                new FxFacesMsgErr("ScriptSchedule.error.cronString.empty").addToContext("panelMessages");
                return;
            }

            //determine appropriate constructor
            switch (scheduleType) {
                case SCHEDULE_TYPE_RECURRING:
                    if (endTimeType ==END_TIME_TYPE_REPEAT) {
                        schedule = new FxScriptScheduleEdit(
                                schedule.getScriptId(),schedule.getName(),schedule.isActive(),
                                schedule.getStartTime(),repeatIntervalToMs(),schedule.getRepeatTimes());
                    }
                    else {
                        schedule = new FxScriptScheduleEdit(
                                schedule.getScriptId(),schedule.getName(),schedule.isActive(),
                                schedule.getStartTime(),endTimeType == END_TIME_TYPE_DATE ?
                                        schedule.getEndTime() : null, repeatIntervalToMs());
                    }
                    break;
                case SCHEDULE_TYPE_CRON_SCRIPT:
                    schedule = new FxScriptScheduleEdit(schedule.getScriptId(),schedule.getName(),
                            schedule.isActive(), schedule.getStartTime(),
                            endTimeType == END_TIME_TYPE_DATE ?
                            schedule.getEndTime() : null,schedule.getCronString());
                    break;
                default:                  
                    schedule = new FxScriptScheduleEdit(schedule.getScriptId(),schedule.getName(),
                            schedule.isActive(),schedule.getStartTime());
                    break;
            }
            if (createSchedule) {
                EJBLookup.getScriptingEngine().createScriptSchedule(schedule);
                new FxFacesMsgInfo("ScriptSchedule.info.create.success").addToContext();
            }
            else {
                schedule.setId(editId);
                EJBLookup.getScriptingEngine().updateScriptSchedule(schedule);
                new FxFacesMsgInfo("ScriptSchedule.info.update.success").addToContext();    
            }
            cancel();
        }
        catch (Exception e) {
            new FxFacesMsgErr(e).addToContext("panelMessages");
        }
    }

    public long getRepeatInterval() {
        return repeatInterval;
    }

    public void setRepeatInterval(long repeatInterval) {
        this.repeatInterval = repeatInterval;
    }

    public int getRepeatIntervalUnit() {
        return repeatIntervalUnit;
    }

    public void setRepeatIntervalUnit(int repeatIntervalUnit) {
        this.repeatIntervalUnit = repeatIntervalUnit;
    }

    private long repeatIntervalToMs() {
        switch (repeatIntervalUnit) {
            case UNIT_SECOND:
                return repeatInterval*1000;
            case UNIT_MINUTE:
                return repeatInterval*1000*60;
            case UNIT_HOUR:
                return repeatInterval*1000*60*60;
            case UNIT_DAY:
                return repeatInterval*1000*60*60*24;
            case UNIT_WEEK:
                return repeatInterval*1000*60*60*24*7;
        }
        return -1;
    }

    public String showOverview() {
        return PAGE_ID_OVERVIEW;
    }

    public FxScriptScheduleEdit getSchedule() {
        return schedule;
    }

    public void setSchedule(FxScriptScheduleEdit schedule) {
        this.schedule = schedule;
    }
}
