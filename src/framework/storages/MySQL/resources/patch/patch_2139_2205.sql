-- Patch from v2139 -> v2205
-- Change: add boolean flag ISCACHED to FX_SCRIPTS table
--         set all groovy scripts (ending with .gy or .groovy to true, others to false)
-- Author: Gerhard Glos (gerhard.glos@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_SCRIPTS ADD COLUMN IS_CACHED BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE FXS_SCRIPTS SET IS_CACHED = FALSE
  WHERE (position('.gy' in lower(SNAME)) <= 1 or position('.gy' in lower(SNAME)) != char_length(SNAME) -2)
          and (position('.groovy' in lower(SNAME)) <= 1 or position('.groovy' in lower(SNAME)) != char_length(SNAME) -6);
