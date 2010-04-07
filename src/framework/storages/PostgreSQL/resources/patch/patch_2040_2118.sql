-- Patch from v2040 -> v2118
-- Change: add mime type column to the binary transfer table
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_BINARY_TRANSIT ADD COLUMN MIMETYPE VARCHAR(256);