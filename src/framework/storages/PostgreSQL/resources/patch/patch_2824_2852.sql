-- Patch from v2824 -> v2852
-- Change: Dropped parentxmult column from FX_CONTENT_DATA
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FX_CONTENT_DATA DROP COLUMN PARENTXMULT;
