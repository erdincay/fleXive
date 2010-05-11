package com.flexive.core.timer.jobs;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptResult;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * @author Gerhard Glos (gerhard.glos@flexive.com)
 * @since 3.1.2
 */
public class ScriptExecutionJob implements Job {
    public static final String KEY_SCRIPT_ID ="com.flexive.scriptId";
    public static final String KEY_SCHEDULE_NAME ="com.flexive.scriptName";
    public static final String KEY_SCHEDULE_ID="com.flexive.scheduleId";

    private static final Log LOG = LogFactory.getLog(ScriptExecutionJob.class);

    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        FxContext ctx = null;
        FxScriptResult result;
        long scheduleId=-1;
        long scriptId=-1;
        String scheduleName="";
        String scriptName="";
        try {
            try {
                ctx = ((FxContext) jobExecutionContext.getScheduler().getContext().get("com.flexive.ctx")).copy();
                ctx.replace();
            }
            catch (Exception e) {
                //reset context so that ctx.stopRunAsSystem(); is not executed
                ctx=null;
                throw e;
            }
            ctx.runAsSystem();
            scheduleId = (Long) jobExecutionContext.getJobDetail().getJobDataMap().get(KEY_SCHEDULE_ID);
            scriptId = (Long) jobExecutionContext.getJobDetail().getJobDataMap().get(KEY_SCRIPT_ID);
            scheduleName = (String) jobExecutionContext.getJobDetail().getJobDataMap().get(KEY_SCHEDULE_NAME);

            ScriptingEngine se = EJBLookup.getScriptingEngine();
            // ************WORKAROUND START***************************
            //workaround because se.runScript(scriptId) does not work, as environment is not up to date
            // TODO: fix this
            FxScriptInfo si = null;
            for (FxScriptInfo s : se.getScriptInfos()) {
                if (s.getId() == scriptId) {
                    si = s;
                    scriptName=s.getName();
                    break;
                }
            }
            if (si != null) {
                result = se.runScript(si.getName(), null,se.loadScriptCode(scriptId));
            }
            else {
                throw new Exception("Script with id "+scheduleId+" not found!");
            }
            // ************WORKAROUND END***************************
            if (StringUtils.isNotBlank(scheduleName))
                EJBLookup.getHistoryTrackerEngine().track("history.scriptSchedule.execution.named.success",scriptId,
                        scheduleName,scriptName, String.valueOf(result.getResult()));
            else
                 EJBLookup.getHistoryTrackerEngine().track("history.scriptSchedule.execution.success",scriptId,
                        scriptName, String.valueOf(result.getResult()));
        } catch (Exception e) {
            LOG.error("Error during scheduled script execution: " + e.getMessage(), e);
            if (StringUtils.isNotBlank(scheduleName))
                EJBLookup.getHistoryTrackerEngine().track("history.scriptSchedule.execution.named.failure",
                        scriptId, scheduleName,scriptName,e);
            else
                EJBLookup.getHistoryTrackerEngine().track("history.scriptSchedule.execution.failure",
                        scriptId, scriptName,e);
        }
        finally {
            if (ctx != null)
                ctx.stopRunAsSystem();
        }
    }
}
