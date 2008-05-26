-- Patch from v513 -> v515
-- Related to issue FX-254
-- default value is now stored as exported xml in the property and assignment table instead as string in the multilang tables
-- old default values are lost as a result!
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_TYPEPROPS_T DROP COLUMN DEFAULT_VALUE;
ALTER TABLE FXS_ASSIGNMENTS_T DROP COLUMN DEFAULT_VALUE;
ALTER TABLE FXS_TYPEPROPS ADD DEFAULT_VALUE TEXT CHARACTER SET UTF8;
ALTER TABLE FXS_ASSIGNMENTS ADD DEFAULT_VALUE TEXT CHARACTER SET UTF8;
