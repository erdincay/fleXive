-- register stored procedures defined in com.flexive.H2.StoredProcedures
-- make sure the h2 server is started with flexive-storage-H2.jar in the classpath!
-- to build flexive-storage-H2.jar, call the ant target "jar.storages" in the source distribution of flexive

CREATE ALIAS IF NOT EXISTS TIMEMILLIS FOR "com.flexive.H2.StoredProcedures.getTimeMillis";
CREATE ALIAS IF NOT EXISTS TOTIMESTAMP FOR "com.flexive.H2.StoredProcedures.toTimestamp";
CREATE ALIAS IF NOT EXISTS PERMISSIONS FOR "com.flexive.H2.StoredProcedures.permissions";
CREATE ALIAS IF NOT EXISTS PERMISSIONS2 FOR "com.flexive.H2.StoredProcedures.permissions2";
CREATE ALIAS IF NOT EXISTS MAYREADINSTANCE FOR "com.flexive.H2.StoredProcedures.mayReadInstance";
CREATE ALIAS IF NOT EXISTS MAYREADINSTANCE2 FOR "com.flexive.H2.StoredProcedures.mayReadInstance2";
CREATE ALIAS IF NOT EXISTS TREE_ISLEAF FOR "com.flexive.H2.StoredProcedures.tree_isLeaf";
CREATE ALIAS IF NOT EXISTS TREE_IDTOPATH FOR "com.flexive.H2.StoredProcedures.tree_idToPath";
CREATE ALIAS IF NOT EXISTS TREE_PATHTOID FOR "com.flexive.H2.StoredProcedures.tree_pathToId";
CREATE ALIAS IF NOT EXISTS TREE_IDCHAIN FOR "com.flexive.H2.StoredProcedures.tree_idchain";
CREATE ALIAS IF NOT EXISTS TREE_FTEXT1024_CHAIN FOR "com.flexive.H2.StoredProcedures.tree_FTEXT1024_Chain";
CREATE ALIAS IF NOT EXISTS TREE_FTEXT1024_PATHS FOR "com.flexive.H2.StoredProcedures.tree_FTEXT1024_Paths";
CREATE ALIAS IF NOT EXISTS TREE_CAPTIONPATHTOID FOR "com.flexive.H2.StoredProcedures.tree_captionPathToID";
CREATE ALIAS IF NOT EXISTS TREE_NODEINDEX FOR "com.flexive.H2.StoredProcedures.tree_nodeIndex";
CREATE ALIAS IF NOT EXISTS TREE_GETPOSITION FOR "com.flexive.H2.StoredProcedures.tree_getPosition";
CREATE ALIAS IF NOT EXISTS CONCAT_WS FOR "com.flexive.H2.StoredProcedures.concat_ws";