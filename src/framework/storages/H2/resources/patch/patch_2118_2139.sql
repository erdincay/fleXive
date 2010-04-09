-- Patch from v2118 -> v2139
-- Change: fix max length of MIMETYPE column in FX_BINARY table
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FX_BINARY ALTER COLUMN MIMETYPE VARCHAR(256);
