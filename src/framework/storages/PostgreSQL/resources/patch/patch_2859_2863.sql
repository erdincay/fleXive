-- Patch from v2859 -> v2863
-- Change: Added group_pos to FX_CONTENT (requires custom migration in DivisionConfigurationEngine#patchDatabase)
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FX_CONTENT ADD COLUMN GROUP_POS TEXT NULL;
