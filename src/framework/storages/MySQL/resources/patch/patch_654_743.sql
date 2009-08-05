-- Patch from v654 -> v743
-- Related to issue FX-317 (default values for binaries)
-- Change default_values from TEXT to LONGTEXT to allow values > 64k characters
-- Change SelectListItem Data column as well to LONGTEXT
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_ASSIGNMENTS MODIFY DEFAULT_VALUE LONGTEXT CHARSET UTF8 COMMENT 'Default value serialized to XML';
ALTER TABLE FXS_SELECTLIST_ITEM MODIFY DATA LONGTEXT CHARACTER SET UTF8;
ALTER TABLE FXS_TYPEPROPS MODIFY DEFAULT_VALUE LONGTEXT CHARACTER SET UTF8 COMMENT 'Default value serialized to XML';