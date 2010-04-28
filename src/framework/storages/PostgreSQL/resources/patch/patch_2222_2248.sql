-- Patch from v2222 -> v2248
-- Change: FX-873: Update Quartz to 1.8
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE QRTZ_JOB_DETAILS ALTER COLUMN JOB_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_JOB_DETAILS ALTER COLUMN JOB_GROUP TYPE VARCHAR(200);
ALTER TABLE QRTZ_JOB_DETAILS ALTER COLUMN DESCRIPTION TYPE VARCHAR(200);
ALTER TABLE QRTZ_JOB_DETAILS ALTER COLUMN JOB_CLASS_NAME TYPE VARCHAR(250);

ALTER TABLE QRTZ_JOB_LISTENERS ALTER COLUMN JOB_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_JOB_LISTENERS ALTER COLUMN JOB_GROUP TYPE VARCHAR(200);
ALTER TABLE QRTZ_JOB_LISTENERS ALTER COLUMN JOB_LISTENER TYPE VARCHAR(200);

ALTER TABLE QRTZ_TRIGGERS ALTER COLUMN TRIGGER_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_TRIGGERS ALTER COLUMN TRIGGER_GROUP TYPE VARCHAR(200);
ALTER TABLE QRTZ_TRIGGERS ALTER COLUMN JOB_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_TRIGGERS ALTER COLUMN JOB_GROUP TYPE VARCHAR(200);
ALTER TABLE QRTZ_TRIGGERS ALTER COLUMN DESCRIPTION TYPE VARCHAR(250);
ALTER TABLE QRTZ_TRIGGERS ALTER COLUMN CALENDAR_NAME TYPE VARCHAR(200);

ALTER TABLE QRTZ_SIMPLE_TRIGGERS ALTER COLUMN TRIGGER_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_SIMPLE_TRIGGERS ALTER COLUMN TRIGGER_GROUP TYPE VARCHAR(200);

ALTER TABLE QRTZ_CRON_TRIGGERS ALTER COLUMN TRIGGER_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_CRON_TRIGGERS ALTER COLUMN TRIGGER_GROUP TYPE VARCHAR(200);
ALTER TABLE QRTZ_CRON_TRIGGERS ALTER COLUMN CRON_EXPRESSION TYPE VARCHAR(120);

ALTER TABLE QRTZ_BLOB_TRIGGERS ALTER COLUMN TRIGGER_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_BLOB_TRIGGERS ALTER COLUMN TRIGGER_GROUP TYPE VARCHAR(200);

ALTER TABLE QRTZ_TRIGGER_LISTENERS ALTER COLUMN TRIGGER_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_TRIGGER_LISTENERS ALTER COLUMN TRIGGER_GROUP TYPE VARCHAR(200);
ALTER TABLE QRTZ_TRIGGER_LISTENERS ALTER COLUMN TRIGGER_LISTENER TYPE VARCHAR(200);

ALTER TABLE QRTZ_CALENDARS ALTER COLUMN CALENDAR_NAME TYPE VARCHAR(200);

ALTER TABLE QRTZ_PAUSED_TRIGGER_GRPS ALTER COLUMN TRIGGER_GROUP TYPE VARCHAR(200);

ALTER TABLE QRTZ_FIRED_TRIGGERS ALTER COLUMN TRIGGER_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_FIRED_TRIGGERS ALTER COLUMN TRIGGER_GROUP TYPE VARCHAR(200);
ALTER TABLE QRTZ_FIRED_TRIGGERS ALTER COLUMN INSTANCE_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_FIRED_TRIGGERS ALTER COLUMN JOB_NAME TYPE VARCHAR(200);
ALTER TABLE QRTZ_FIRED_TRIGGERS ALTER COLUMN JOB_GROUP TYPE VARCHAR(200);

ALTER TABLE QRTZ_SCHEDULER_STATE ALTER COLUMN INSTANCE_NAME TYPE VARCHAR(200);