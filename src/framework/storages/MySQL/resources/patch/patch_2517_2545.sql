-- Patch from v2517 -> v2545
-- Change: FX-960: Remove unnecessary XPaths from content data table
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FX_CONTENT_DATA DROP COLUMN XPATH;
ALTER TABLE FX_CONTENT_DATA DROP COLUMN XPATHMULT;
ALTER TABLE FX_CONTENT_DATA DROP COLUMN PARENTXPATH;