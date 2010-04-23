-- Patch from v2205 -> v2222
-- Change: FX-738: add default instance ACL column DEFACL to FXS_TYPEDEF table
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FXS_TYPEDEF ADD DEFACL INTEGER UNSIGNED;
ALTER TABLE FXS_TYPEDEF ADD FOREIGN KEY FK_TYPEDEF_DEFACL(DEFACL) REFERENCES FXS_ACL(ID) ON DELETE RESTRICT ON UPDATE RESTRICT;
