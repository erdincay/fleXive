-- Patch from v2641 -> v2646
-- Change: Explicit search value for phrases
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FX_PHRASE_VAL ADD COLUMN SVAL TEXT NOT NULL;
CREATE INDEX FXI_PHRASE_SVAL ON FX_PHRASE_VAL(SVAL);