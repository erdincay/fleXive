-- Patch from v1688 --> v1741
-- Change: FxType structures: Structure options for FxTypes
-- Author: Christopher Blasnik (cblasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

CREATE TABLE FXS_TYPE_OPT (
  OPTKEY VARCHAR(32),
  ID INTEGER UNSIGNED NOT NULL,
  MAYOVERRIDE BOOLEAN,
  PASSEDON BOOLEAN,
  OPTVALUE TEXT,
  FOREIGN KEY TYPE_PROPERTY_ID(ID) REFERENCES FXS_TYPEDEF(ID)
  	ON DELETE RESTRICT ON UPDATE RESTRICT
)
ENGINE = InnoDB
DEFAULT CHARSET = LATIN1
COMMENT = 'Options for types';