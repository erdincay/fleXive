-- Patch from v2646 -> v2738
-- Change: make base assignment constraint deferrable (division import fix)
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_ASSIGNMENTS DROP CONSTRAINT fxs_assignments_base_fkey;

ALTER TABLE FXS_ASSIGNMENTS  ADD CONSTRAINT fxs_assignments_base_fkey FOREIGN KEY (base)
      REFERENCES FXS_ASSIGNMENTS (id) MATCH SIMPLE
      ON UPDATE RESTRICT ON DELETE RESTRICT
      DEFERRABLE;
