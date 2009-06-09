-- Patch from v1353 -> v1355
-- Change: Allow nullable FBLOB columns in FX_BINARY
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FX_BINARY ALTER FBLOB SET NULL;

