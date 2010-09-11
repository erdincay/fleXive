-- Patch from v2378 -> 2494
-- Change: Add SORTENTRIES to selectlists to enable sorting by language labels
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
ALTER TABLE FXS_SELECTLIST ADD COLUMN SORTENTRIES BOOLEAN DEFAULT FALSE;