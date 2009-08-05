-- Patch from v515 -> v654
-- Related to issue FX-289
-- Added a content reference column to FXS_TYPEDEF for preview icons 
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_TYPEDEF ADD ICON_REF INTEGER UNSIGNED;
ALTER TABLE FXS_TYPEDEF ADD FOREIGN KEY FK_ICON_REF (ICON_REF) REFERENCES FX_CONTENT (ID)
 ON DELETE RESTRICT ON UPDATE RESTRICT;