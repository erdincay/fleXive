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
package com.flexive.core.timer;

import com.flexive.core.security.UserTicketImpl;
import com.flexive.core.timer.jobs.MaintenanceJob;
import com.flexive.core.timer.jobs.ScriptExecutionJob;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.scripting.FxScriptSchedule;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;
import org.quartz.impl.SchedulerRepository;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.jdbcjobstore.JobStoreCMT;
import org.quartz.impl.jdbcjobstore.StdJDBCDelegate;
import org.quartz.impl.jdbcjobstore.PostgreSQLDelegate;
import org.quartz.simpl.SimpleThreadPool;

import java.text.ParseException;
import java.util.Properties;

/**
 * Quartz scheduler [fleXive] integration
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class FxQuartz {

    private static final Log LOG = LogFactory.getLog(FxQuartz.class);

    public final static String GROUP_INTERNAL = "FxInternal";
    public final static String TRIGGER_GROUP_FX_SCRIPT_SCHEDULE="FxScriptSchedule";
    public final static String JOB_GROUP_SCRIPT_EXECUTION = "FxScriptExecutionJob";
    public final static String JOB_MAINTENANCE = "FxMaintenanceJob";
    /** A system property to disable the Quartz-based timer service (e.g. for tests) */
    public static final String PROP_DISABLE = "flexive.quartz.disable";

    /**
     * Get the Scheduler for the current division
     *
     * @return Scheduler for the current division
     */
    public static Scheduler getScheduler() {
        return SchedulerRepository.getInstance().lookup("FxQuartzScheduler_Division_" + FxContext.get().getDivisionId());
    }

    /**
     * Start the Quartz scheduler
     *
     * @throws SchedulerException on errors
     */
    public static void startup() throws SchedulerException {

        FxContext currCtx = FxContext.get();
        if (currCtx.isTestDivision()) {
            //disable scheduler for embedded containers
            LOG.info("Quartz scheduler disabled for embedded (test) container.");
            return;
        }
        if (System.getProperty(PROP_DISABLE) != null) {
            LOG.info("Quartz scheduler disabled because system property " + PROP_DISABLE + " is set.");
            return;
        }

        // Grab the Scheduler instance from the Factory
        // see http://wiki.opensymphony.com/display/QRTZ1/ConfigJobStoreCMT for more options
        Properties props = new Properties();
        props.put(StdSchedulerFactory.PROP_DATASOURCE_PREFIX, "fxQuartzDS");
        props.put("org.quartz.jobStore.dataSource", "fxQuartzDS");
        props.put("org.quartz.dataSource.fxQuartzDS." + StdSchedulerFactory.PROP_CONNECTION_PROVIDER_CLASS,
                FxQuartzConnectionProviderNonTX.class.getCanonicalName());
        props.put("org.quartz.dataSource.fxQuartzDS.divisionId", String.valueOf(currCtx.getDivisionId()));
        props.put(StdSchedulerFactory.PROP_JOB_STORE_CLASS, JobStoreCMT.class.getCanonicalName());

        final String driverDelegateClass;
        if ("PostgreSQL".equalsIgnoreCase(FxContext.get().getDivisionData().getDbVendor())) {
            driverDelegateClass = PostgreSQLDelegate.class.getCanonicalName();
        } else {
            driverDelegateClass = StdJDBCDelegate.class.getCanonicalName(); 
        }
        props.put("org.quartz.jobStore.driverDelegateClass", driverDelegateClass);

        /*props.put("org.quartz.jobStore.dataSource", "fxQuartzDS");
        try {
            props.put("org.quartz.dataSource.fxQuartzDS.jndiURL", Database.getDivisionData().getDataSource());
        } catch (SQLException e) {
            LOG.error("Quartz scheduler disabled! Failed to lookup DataSource for org.quartz.dataSource.fxQuartzDS.jndiURL: " + e.getMessage());
            return;
        }

        System.out.println("==> QUARTZ DS: "+props.get("org.quartz.dataSource.fxQuartzDS.jndiURL"));
        */
        props.put(StdSchedulerFactory.PROP_JOB_STORE_PREFIX, "QRTZ_");
        props.put("org.quartz.scheduler.instanceId", StdSchedulerFactory.AUTO_GENERATE_INSTANCE_ID);
//        props.put("org.quartz.scheduler.xaTransacted", "false");
        props.put(StdSchedulerFactory.PROP_THREAD_POOL_CLASS, SimpleThreadPool.class.getCanonicalName());
        props.put("org.quartz.threadPool.threadCount", String.valueOf(1));
        props.put("org.quartz.jobStore.nonManagedTXDataSource", "fxQuartzNoTXDS");

        props.put(StdSchedulerFactory.PROP_SCHED_INSTANCE_NAME, "FxQuartzScheduler_Division_" + FxContext.get().getDivisionId());
        props.put("org.quartz.jobStore.isClustered", "true");
//        props.put("org.quartz.jobStore.txIsolationLevelSerializable", "false");
        props.put("org.quartz.jobStore.txIsolationLevelReadCommitted", "true");
        // lower db load
        props.put("org.quartz.jobStore.clusterCheckinInterval", "60000");



        props.put("org.quartz.dataSource.fxQuartzNoTXDS." + StdSchedulerFactory.PROP_CONNECTION_PROVIDER_CLASS, FxQuartzConnectionProviderNonTX.class.getCanonicalName());
        props.put("org.quartz.dataSource.fxQuartzNoTXDS.divisionId", String.valueOf(currCtx.getDivisionId()));

        /*props.put("org.quartz.dataSource.fxQuartzNoTXDS.driver","com.mysql.jdbc.Driver");
        props.put("org.quartz.dataSource.fxQuartzNoTXDS.URL", "jdbc:mysql://localhost:3306/flexive?useUnicode=true&amp;characterEncoding=utf8&amp;characterResultSets=utf8");
        props.put("org.quartz.dataSource.fxQuartzNoTXDS.user", "root");
        props.put("org.quartz.dataSource.fxQuartzNoTXDS.password", "a");*/

        Scheduler scheduler = new StdSchedulerFactory(props).getScheduler();
        FxContext ctx = FxContext._getEJBContext(currCtx);
        ctx.overrideTicket(UserTicketImpl.getGuestTicket().cloneAsGlobalSupervisor());
        scheduler.getContext().put("com.flexive.ctx", ctx);
        // and start it off
        scheduler.start();

        boolean found = false;
        for(String existingJob: scheduler.getJobNames(GROUP_INTERNAL))
                if( JOB_MAINTENANCE.equals(existingJob)) {
                    found = true;
                    break;
                }

        if( found ) {
//            System.out.println("Maintenance job already exists - done.");
            LOG.info("Quartz started. The scheduler can be disabled by with the command line option -D" + PROP_DISABLE);
            return;
        }

        JobDetail job = new JobDetail(JOB_MAINTENANCE, GROUP_INTERNAL, MaintenanceJob.class);
        job.setVolatility(false);
        //trigger every 30 minutes
        SimpleTrigger tr = new SimpleTrigger(JOB_MAINTENANCE, GROUP_INTERNAL);
        tr.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        tr.setRepeatInterval(1800000L); //30 minutes
//        tr.setRepeatInterval(10000L); //10 seconds for testing purposes
        tr.setVolatility(false);
        scheduler.scheduleJob(job, tr);
        LOG.info("Quartz started and maintenance job is scheduled.");
    }

    /**
     * Schedule a script
     *
     * @param scriptSchedule            script schedule
     * @throws FxApplicationException   on errors
     * @since 3.1.2
     */
    public static void scheduleScript(FxScriptSchedule scriptSchedule) throws FxApplicationException {
        if (!isInstalled())
            throw new FxApplicationException("ex.timer.not.installed");
        try {
            getScheduler().scheduleJob(createScriptExecutionJob(scriptSchedule),
                    getScriptScheduleTrigger(scriptSchedule));
            if (!scriptSchedule.isActive())
                getScheduler().pauseTrigger(String.valueOf(scriptSchedule.getId()),TRIGGER_GROUP_FX_SCRIPT_SCHEDULE);
        }
        catch (Exception e) {
            throw new FxApplicationException(LOG,e);
        }
    }

    /**
     * Update a scheduled script
     *
     * @param scriptSchedule            script schedule
     * @throws FxApplicationException   on errors
     * @since 3.1.2
     */
    public static void updateScriptSchedule(FxScriptSchedule scriptSchedule) throws FxApplicationException {
        if (!isInstalled())
            throw new FxApplicationException("ex.timer.not.installed");
        deleteScriptSchedule(scriptSchedule);
        scheduleScript(scriptSchedule);
    }

    /**
     * Delete a script schedule
     *
     * @param scriptSchedule            script schedule
     * @throws FxApplicationException   on errors
     * @since 3.1.2
     * @return true if script schedule was found and could be deleted
     */
    public static boolean deleteScriptSchedule(FxScriptSchedule scriptSchedule) throws FxApplicationException {
        if (!isInstalled())
            throw new FxApplicationException("ex.timer.not.installed");
        try {
            return getScheduler().deleteJob(String.valueOf(scriptSchedule.getId()),JOB_GROUP_SCRIPT_EXECUTION);
        }
        catch (Exception e) {
            throw new FxApplicationException(LOG,e);
        }
    }

    /**
     * Parses a Cron String and throws an exception
     * if it cannot be parsed
     *
     * @param cronString  Cron String
     * @since 3.1.2
     * @throws com.flexive.shared.exceptions.FxInvalidParameterException on errors
     */
    public static void parseCronString(String cronString) throws FxInvalidParameterException {
        try {
                new CronExpression(cronString);
            }
            catch (ParseException e) {
                throw new FxInvalidParameterException(
                       "cronString","ex.scripting.schedule.parameter.cronString",
                        cronString,e.getMessage());
            }
    }

     /**
     * Creates a trigger for a given script schedule.
     *
     * @param scriptSchedule script schedule
     * @return trigger
     * @throws java.text.ParseException        if the cron String cannot be parsed
     * @throws org.quartz.SchedulerException   if the trigger data is invalid
     */
    private static Trigger getScriptScheduleTrigger(FxScriptSchedule scriptSchedule) throws ParseException, SchedulerException {
         final Trigger t;
         final String triggerName = String.valueOf(scriptSchedule.getId());
         if (scriptSchedule.getCronString() != null) {
            t = new CronTrigger(triggerName, TRIGGER_GROUP_FX_SCRIPT_SCHEDULE,
                        scriptSchedule.getCronString());
         }
         else {
             t = new SimpleTrigger(triggerName, TRIGGER_GROUP_FX_SCRIPT_SCHEDULE,
                     scriptSchedule.getStartTime());
             if (scriptSchedule.isUnbounded())
                 ((SimpleTrigger)t).setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
             if (scriptSchedule.getRepeatInterval() > 0)
                ((SimpleTrigger)t).setRepeatInterval(scriptSchedule.getRepeatInterval());    
         }
         // set end time
         if (t.getEndTime() != null)
             t.setEndTime(scriptSchedule.getEndTime());
         // set volatility
         t.setVolatility(false);
         return t;
    }

    /**
     * Creates a script execution job detail for a given script schedule.
     *
     * @param scriptSchedule script schedule
     * @return script execution job detail
     */
    private static JobDetail createScriptExecutionJob(FxScriptSchedule scriptSchedule) {
        JobDetail scriptExecutionJob = new JobDetail(String.valueOf(scriptSchedule.getId()), JOB_GROUP_SCRIPT_EXECUTION, ScriptExecutionJob.class);
        scriptExecutionJob.getJobDataMap().put(ScriptExecutionJob.KEY_SCRIPT_ID,scriptSchedule.getScriptId());
        scriptExecutionJob.getJobDataMap().put(ScriptExecutionJob.KEY_SCHEDULE_NAME,scriptSchedule.getName());
        scriptExecutionJob.getJobDataMap().put(ScriptExecutionJob.KEY_SCHEDULE_ID,scriptSchedule.getId());
        scriptExecutionJob.setVolatility(false);
        return scriptExecutionJob;
    }

    /**
     * Shutdown the scheduler
     *
     * @throws SchedulerException on errors
     */
    public static void shutdown() throws SchedulerException {
        if (isInstalled())
            getScheduler().shutdown();
    }

    /**
     * Is the scheduler installed?
     *
     * @return scheduler installed
     */
    public static boolean isInstalled() {
        return getScheduler() != null;
    }
}
