-- Patch from v2969 -> v2975
-- Change: history tracker: make account nullable
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FXS_HISTORY ALTER COLUMN ACCOUNT DROP NOT NULL;
CREATE INDEX FXI_HISTORY ON FXS_HISTORY(APPLICATION,LOGINNAME,TIMESTP,TYPENAME);