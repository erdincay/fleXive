DROP SCHEMA IF EXISTS flexiveConfiguration;
CREATE SCHEMA flexiveConfiguration;
USE flexiveConfiguration;

--  Main configuration table

CREATE TABLE FXS_CONFIGURATION (
  CPATH VARCHAR(255) NOT NULL,
  CKEY VARCHAR(255) CHARACTER SET UTF8 NOT NULL
  CVALUE LONGTEXT charset utf8,
  PRIMARY KEY UK_CONFIGURATION(CPATH, CKEY)
) 
ENGINE = InnoDB
charset = latin1
COMMENT = 'Global flexive3 configuration table';

INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig', 'root_login', 'administrator');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig', 'root_password', '976d934d4c84699020cb9941a38ad01690a23e17');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/datasources', '-2', 'jdbc/flexiveTest');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/datasources', '1', 'jdbc/flexiveDivision1');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/datasources', '2', 'jdbc/flexiveDivision2');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/domains', '1', '(\\d+\\.\\d+\\.\\d+\\.\\d+|localhost)');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/domains', '2', 'www\.flexive-develop2\.com|www2\.flexive-develop2\.com');
INSERT INTO FXS_CONFIGURATION VALUES('/globalconfig/domains', '-2', '(test division)');