-- -------------------------
-- Sequenzer  Initialization, next value will be the given id+1
-- -------------------------
CREATE TABLE FXS_SEQUENCE (
    ID INTEGER UNSIGNED NOT NULL,
    NAME VARCHAR(255) CHARSET UTF8 NOT NULL,
    ROLLOVER BOOLEAN DEFAULT FALSE,
    PRIMARY KEY(NAME)
);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FX_CONTENT),"SYS_CONTENT",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_ACCOUNTS),"SYS_ACCOUNT",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_USERGROUPS),"SYS_GROUP",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_ACL),"SYS_ACL",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_MANDATOR),"SYS_MANDATOR",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_TYPEDEF), "SYS_TYPEDEF",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_TYPEGROUPS), "SYS_TYPEGROUP",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_TYPEPROPS), "SYS_TYPEPROP",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_ASSIGNMENTS), "SYS_ASSIGNMENT",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_WF_STEPDEFS), "SYS_STEPDEFINITION",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_WF_STEPS), "SYS_STEP",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_WF_ROUTES), "SYS_ROUTE",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_WORKFLOWS), "SYS_WORKFLOW",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FX_BINARY), "SYS_BINARY",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_SCRIPTS), "SYS_SCRIPTS",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_BRIEFCASE), "SYS_BRIEFCASE",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(SEARCH_ID),0) FROM FXS_SEARCHCACHE_MEMORY), "SYS_SEARCHCACHE_MEMORY",TRUE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(SEARCH_ID),0) FROM FXS_SEARCHCACHE_PERM), "SYS_SEARCHCACHE_PERM",TRUE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_SELECTLIST), "SYS_SELECTLIST",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_SELECTLIST_ITEM), "SYS_SELECTLIST_ITEM",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_TREE), "SYS_TREE_EDIT",FALSE);
INSERT INTO FXS_SEQUENCE(ID,NAME,ROLLOVER)VALUES ((SELECT IFNULL(MAX(ID),0) FROM FXS_TREE_LIVE), "SYS_TREE_LIVE",FALSE);
