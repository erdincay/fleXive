-- Patch from v1672 --> v1688
-- Change: FX-689: Deadlocks in tree engine. We drop the TOTAL_CHILDCOUNT column to reduce updates
-- during tree modifications and compute it on the fly only when required.
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_TREE DROP COLUMN TOTAL_CHILDCOUNT;
ALTER TABLE FXS_TREE_LIVE DROP COLUMN TOTAL_CHILDCOUNT;

