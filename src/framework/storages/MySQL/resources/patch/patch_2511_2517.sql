-- Patch from v2511 -> v2517
-- Change: FX-953: Automatic versioning on data changes
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_TYPEDEF ADD COLUMN AUTO_VERSION BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'When true and contents have changed, a new version will be created when saving.';