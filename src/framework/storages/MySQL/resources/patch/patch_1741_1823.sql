-- Patch from v1741 -> v1823
-- Change: FX-764: Multiple locks for one pk possible
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FXS_LOCK ADD UNIQUE KEY UK_LOCK_PK(LOCK_ID,LOCK_VER)
ALTER TABLE FXS_LOCK ADD UNIQUE KEY UK_LOCK_RES(LOCK_RESOURCE(255))