--  Main configuration table

CREATE TABLE FXS_CONFIGURATION (
  CPATH VARCHAR(255) NOT NULL,
  CKEY VARCHAR(255) NOT NULL,
  CVALUE TEXT,
  CONSTRAINT UK_CONFIGURATION PRIMARY KEY (CPATH, CKEY)
);
COMMENT ON TABLE FXS_CONFIGURATION IS 'Global [fleXive] configuration table';

INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig', 'root_login', 'administrator');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig', 'root_password', '976d934d4c84699020cb9941a38ad01690a23e17');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/datasources', '-2', 'jdbc/flexiveTest');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/datasources', '1', 'jdbc/flexiveDivision1');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/domains', '1', '.*');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/domains', '-2', '(test division)');