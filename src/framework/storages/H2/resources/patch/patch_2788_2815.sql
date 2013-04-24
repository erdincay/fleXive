-- Patch from v2788 -> v2815
-- Change: phrase engine: categories support
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FX_PHRASE ADD COLUMN CAT INTEGER NOT NULL DEFAULT 0;
CREATE INDEX FXI_PHRASE_CAT ON FX_PHRASE(CAT);