-- Patch from v2975 -> v2976
-- Change: history tracker: add mandator column
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FXS_HISTORY ADD COLUMN MANDATOR INTEGER UNSIGNED;
CREATE INDEX FXI_HISTORY2 ON FXS_HISTORY(MANDATOR,APPLICATION,LOGINNAME,TIMESTP,TYPENAME);