-- Patch from 1429 -> 1484
-- Change: Support multiple ACLs for contents.
-- Author: Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)

ALTER TABLE FXS_TYPEDEF ADD COLUMN MULTIPLE_CONTENT_ACLS BOOLEAN NOT NULL DEFAULT TRUE COMMENT 'When true, multiple ACLs can be assigned to a content.';

-- Patch ROOT/ACL multiplicity
UPDATE FXS_TYPEPROPS SET DEFMAXMULT=2147483647, MAYOVERRIDEMULT=TRUE WHERE ID=4;
UPDATE FXS_ASSIGNMENTS SET MAXMULT=2147483647 WHERE APROPERTY=4;

-- Add default (null) ACL with ID 0
INSERT INTO FXS_ACL VALUES (0, 0, 'Null ACL', 'Placeholder ACL for empty ACL values', 1, '#000000', 2, UNIX_TIMESTAMP()*1000, 1, UNIX_TIMESTAMP()*1000);

-- -------------------------
-- Additional ACLs (2-) for contents.
-- -------------------------
CREATE TABLE FX_CONTENT_ACLS (
  ID INTEGER UNSIGNED NOT NULL,
  VER SMALLINT(6) NOT NULL,
  ACL INTEGER UNSIGNED NOT NULL,

  PRIMARY KEY (ID, VER, ACL),
  FOREIGN KEY FK_CONTENT_ACLS_ID_VER(ID,VER) REFERENCES FX_CONTENT(ID,VER)
    ON DELETE CASCADE ON UPDATE RESTRICT,
  FOREIGN KEY FK_CONTENT_ACLS_ACL(ACL) REFERENCES FXS_ACL(ID)
    ON DELETE RESTRICT ON UPDATE RESTRICT
)
ENGINE=InnoDB
COMMENT='Additional ACLs for contents';
