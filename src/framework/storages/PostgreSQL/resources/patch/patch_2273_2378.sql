-- Patch from v2273 -> v2378
-- Change: FX-908: FX_RES Resource table
-- Author: Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

CREATE TABLE FX_RES (
  RKEY VARCHAR(50) NOT NULL,
  LANG INTEGER NOT NULL,
  RVAL TEXT NOT NULL,
  PRIMARY KEY(RKEY, LANG),
  FOREIGN KEY (LANG) REFERENCES FXS_LANG(LANG_CODE)
  	ON DELETE RESTRICT ON UPDATE RESTRICT
);
COMMENT ON TABLE FX_RES IS 'Resources';
CREATE INDEX FXI_RES_KEY ON FX_RES(RKEY);