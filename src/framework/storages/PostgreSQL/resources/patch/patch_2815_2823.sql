-- Patch from v2815 -> v2823
-- Change: phrase engine: categories support (added missing column to table FX_PHRASE_TREE from previous patch)
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FX_PHRASE_TREE ADD COLUMN CAT INTEGER NOT NULL DEFAULT 0;
