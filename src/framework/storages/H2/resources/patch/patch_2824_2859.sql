-- Patch from v2824 -> v2859
-- Change: phrase engine: categories support
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FX_PHRASE_MAP ADD COLUMN CAT INTEGER NOT NULL DEFAULT 0;