-- Patch 1823 --> 1995: add POS column to FXS_WF_STEPS
-- @author Daniel Lichtenberger, UCS
--
-- The position column is mandatory. Set the position for all existing
-- steps to 1, the environment loader uses the ID as secondary
-- sort column to preserve the original behaviour for upgraded tables.

ALTER TABLE FXS_WF_STEPS ADD COLUMN POS INTEGER NOT NULL DEFAULT 1;