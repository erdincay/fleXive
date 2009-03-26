-- Patch from v743 -> v1149
-- Related to issue FX-502 (Cannot delete versions of a referenced content)
-- Change: foreign key FX_CONTENT_DATA.FK_FREF is dropped and checks are shifted to the framework logic
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

-- see also the following MySQL bug: http://bugs.mysql.com/bug.php?id=14347
-- using FX_CONTENT_DATA_ibfk_6 as workaround instead of FK_FREF
ALTER TABLE FX_CONTENT_DATA DROP FOREIGN KEY FX_CONTENT_DATA_ibfk_6;