-- Patch from v1355 -> v1370
-- Change: Add breadcrumb separator and flag to allow only selects from same level to selectlists
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FXS_SELECTLIST ADD COLUMN BCSEP VARCHAR(255);
ALTER TABLE FXS_SELECTLIST ADD COLUMN SAMELVLSELECT BOOLEAN;
ALTER TABLE FXS_SELECTLIST_ITEM ADD COLUMN POS INTEGER;