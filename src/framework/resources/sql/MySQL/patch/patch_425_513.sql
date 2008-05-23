-- Patch from v425 -> v513
-- Related to issues FX-252, FX-253
-- "CREATED_BY" column had to be added to the search cache tables to be able to check property permissions correctly
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_SEARCHCACHE_PERM ADD CREATED_BY INTEGER UNSIGNED NOT NULL;
ALTER TABLE FXS_SEARCHCACHE_MEMORY ADD CREATED_BY INTEGER UNSIGNED NOT NULL;