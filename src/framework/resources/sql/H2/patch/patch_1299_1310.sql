-- Patch from v1299 -> v1310
-- Change: Allow nullable FBLOB columns in FXS_BINARY_TRANSIT
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FXS_BINARY_TRANSIT ALTER FBLOB SET NULL;