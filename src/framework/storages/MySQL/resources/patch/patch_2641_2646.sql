-- Patch from v2641 -> v2646
-- Change: Explicit search value for phrases
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FX_PHRASE_VAL ADD COLUMN SVAL TEXT CHARACTER SET UTF8 NOT NULL;