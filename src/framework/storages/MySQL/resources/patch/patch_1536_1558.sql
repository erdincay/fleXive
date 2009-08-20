-- Patch from v1536 -> v1558
-- Change: FX-627 FxType attribute: includedInSupertypeQuery
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_TYPEDEF ADD COLUMN INSUPERTYPEQUERY BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'When true, this type will be included in supertype queries.';