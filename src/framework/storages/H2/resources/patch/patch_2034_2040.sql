-- Patch from v2034 -> v2040
-- Change: FXS_PROP_OPT & FXS_GROUP_OPT: new field: "isinherited"
-- Author: Christopher Blasnik (cblasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_PROP_OPT ADD COLUMN ISINHERITED BOOLEAN;
ALTER TABLE FXS_GROUP_OPT ADD COLUMN ISINHERITED BOOLEAN;