-- Patch from v1149 -> v1166
-- Adds an application-scoped configuration table (FX-344)

CREATE TABLE FXS_APPLICATIONCONFIGURATION (
    APPLICATION_ID VARCHAR(255) NOT NULL,
	CPATH VARCHAR(255) NOT NULL,
	CKEY VARCHAR(255) CHARACTER SET UTF8 NOT NULL,
	CVALUE LONGTEXT CHARACTER SET UTF8,
	PRIMARY KEY (APPLICATION_ID, CPATH, CKEY)
)
ENGINE = InnoDB DEFAULT CHARSET = LATIN1 COMMENT = 'Application configuration table';

