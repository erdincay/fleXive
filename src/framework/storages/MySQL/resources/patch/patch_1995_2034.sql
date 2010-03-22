-- Patch from v1995 -> v2034
-- Change: FXS_TYPE_OPT / type options: change col name from "PASSEDON" to "ISINHERITED"
-- Author: Christopher Blasnik (cblasnik@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_TYPE_OPT CHANGE COLUMN PASSEDON ISINHERITED BOOLEAN;