-- Patch from v1558 -> v1572
-- Change: FX-596: Store default language for multicolumn translation tables
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_ASSIGNMENTS_T ADD COLUMN DESCRIPTION_MLD BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'When true, this is the default language.';
ALTER TABLE FXS_ASSIGNMENTS_T ADD COLUMN HINT_MLD BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'When true, this is the default language.';
ALTER TABLE FXS_TYPEGROUPS_T ADD COLUMN DESCRIPTION_MLD BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'When true, this is the default language.';
ALTER TABLE FXS_TYPEGROUPS_T ADD COLUMN HINT_MLD BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'When true, this is the default language.';
ALTER TABLE FXS_TYPEPROPS_T ADD COLUMN DESCRIPTION_MLD BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'When true, this is the default language.';
ALTER TABLE FXS_TYPEPROPS_T ADD COLUMN HINT_MLD BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'When true, this is the default language.';
ALTER TABLE FXS_SELECTLIST_T ADD COLUMN LABEL_MLD BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'When true, this is the default language.';
ALTER TABLE FXS_SELECTLIST_T ADD COLUMN DESCRIPTION_MLD BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'When true, this is the default language.';