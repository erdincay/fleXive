-- Patch from v2494 -> 2511
-- Change: Increase maximum key length for resources to 250
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FX_RES CHANGE COLUMN RKEY RKEY VARCHAR(250) NOT NULL;