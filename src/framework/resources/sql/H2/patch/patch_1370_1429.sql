-- Patch from v1370 --> v1429
-- Change: Add METADATA column to FXS_BRIEFCASE
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_BRIEFCASE_DATA ADD COLUMN METADATA VARCHAR(4096) NULL;