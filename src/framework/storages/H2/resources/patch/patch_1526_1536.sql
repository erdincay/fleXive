-- Update package name for stored procedures

DROP ALIAS TIMEMILLIS;
DROP ALIAS TOTIMESTAMP;
DROP ALIAS PERMISSIONS;
DROP ALIAS PERMISSIONS2;
DROP ALIAS MAYREADINSTANCE;
DROP ALIAS MAYREADINSTANCE2;
DROP ALIAS TREE_ISLEAF;
DROP ALIAS TREE_IDTOPATH;
DROP ALIAS TREE_PATHTOID;
DROP ALIAS TREE_IDCHAIN;
DROP ALIAS TREE_FTEXT1024_CHAIN;
DROP ALIAS TREE_FTEXT1024_PATHS;
DROP ALIAS TREE_CAPTIONPATHTOID;
DROP ALIAS TREE_NODEINDEX;
DROP ALIAS TREE_GETPOSITION;
DROP ALIAS CONCAT_WS;

CREATE ALIAS TIMEMILLIS FOR "com.flexive.H2.StoredProcedures.getTimeMillis";
CREATE ALIAS TOTIMESTAMP FOR "com.flexive.H2.StoredProcedures.toTimestamp";
CREATE ALIAS PERMISSIONS FOR "com.flexive.H2.StoredProcedures.permissions";
CREATE ALIAS PERMISSIONS2 FOR "com.flexive.H2.StoredProcedures.permissions2";
CREATE ALIAS MAYREADINSTANCE FOR "com.flexive.H2.StoredProcedures.mayReadInstance";
CREATE ALIAS MAYREADINSTANCE2 FOR "com.flexive.H2.StoredProcedures.mayReadInstance2";
CREATE ALIAS TREE_ISLEAF FOR "com.flexive.H2.StoredProcedures.tree_isLeaf";
CREATE ALIAS TREE_IDTOPATH FOR "com.flexive.H2.StoredProcedures.tree_idToPath";
CREATE ALIAS TREE_PATHTOID FOR "com.flexive.H2.StoredProcedures.tree_pathToId";
CREATE ALIAS TREE_IDCHAIN FOR "com.flexive.H2.StoredProcedures.tree_idchain";
CREATE ALIAS TREE_FTEXT1024_CHAIN FOR "com.flexive.H2.StoredProcedures.tree_FTEXT1024_Chain";
CREATE ALIAS TREE_FTEXT1024_PATHS FOR "com.flexive.H2.StoredProcedures.tree_FTEXT1024_Paths";
CREATE ALIAS TREE_CAPTIONPATHTOID FOR "com.flexive.H2.StoredProcedures.tree_captionPathToID";
CREATE ALIAS TREE_NODEINDEX FOR "com.flexive.H2.StoredProcedures.tree_nodeIndex";
CREATE ALIAS TREE_GETPOSITION FOR "com.flexive.H2.StoredProcedures.tree_getPosition";
CREATE ALIAS CONCAT_WS FOR "com.flexive.H2.StoredProcedures.concat_ws";