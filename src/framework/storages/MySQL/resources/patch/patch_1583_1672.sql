-- Patch from v1583 -> v1672
-- Change: FX-687: Compute checksums for binary documents
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FX_BINARY ADD COLUMN MD5SUM VARCHAR(32) DEFAULT 'unknown';