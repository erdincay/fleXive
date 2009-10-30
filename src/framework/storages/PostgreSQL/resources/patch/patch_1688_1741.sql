-- Patch from v1688 --> v1741
-- Change: FxType structures: Structure options for FxTypes
-- Author: Christopher Blasnik (cblasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

CREATE TABLE FXS_TYPE_OPT (
  OPTKEY VARCHAR(32),
  ID BIGINT NOT NULL,
  PASSEDON BOOLEAN,
  MAYOVERRIDE BOOLEAN,
  OPTVALUE TEXT,
  FOREIGN KEY (ID) REFERENCES FXS_TYPEDEF(ID)
  	ON DELETE RESTRICT ON UPDATE RESTRICT
);
COMMENT ON TABLE FXS_TYPE_OPT IS 'Options for types';