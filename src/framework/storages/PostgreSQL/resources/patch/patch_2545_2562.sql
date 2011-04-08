-- Patch from v2545 -> v2562
-- Change: FX-961: Add mandator-specific configuration table
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

CREATE TABLE FXS_MANDATORCONFIGURATION (
    MANDATOR_NAME VARCHAR(255) NOT NULL,
	CPATH VARCHAR(255) NOT NULL,
	CKEY VARCHAR(255) NOT NULL,
	CVALUE TEXT,
    CLASSNAME VARCHAR(255) NULL,
	PRIMARY KEY (MANDATOR_NAME, CPATH, CKEY),
    FOREIGN KEY(MANDATOR_NAME) REFERENCES FXS_MANDATOR(NAME)
        ON DELETE CASCADE ON UPDATE RESTRICT
);
COMMENT ON TABLE FXS_MANDATORCONFIGURATION IS 'Mandator configuration table';
