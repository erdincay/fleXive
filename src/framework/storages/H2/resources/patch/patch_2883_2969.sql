-- Patch from v2883 -> v2969
-- Change: phrase engine: direct/indirect tree mapping
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FX_PHRASE_MAP ADD COLUMN DIRECT BOOLEAN NOT NULL DEFAULT TRUE;