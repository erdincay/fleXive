/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License version 2.1 or higher as published by the Free Software Foundation.
 *
 *  The GNU Lesser General Public License can be found at
 *  http://www.gnu.org/licenses/lgpl.html.
 *  A copy is found in the textfile LGPL.txt and important notices to the
 *  license from the author are found in LICENSE.txt distributed with
 *  these libraries.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  For further information about UCS - unique computing solutions gmbh,
 *  please see the company website: http://www.ucs.at
 *
 *  For further information about [fleXive](R), please see the
 *  project website: http://www.flexive.org
 *
 *
 *  This copyright notice MUST APPEAR in all copies of the file!
 ***************************************************************/
package com.flexive.H2;

import java.sql.*;

/**
 * Stored Procedures for the H2 Database (www.h2database.com)
 * <p/>
 * Usage:
 * CREATE ALIAS IF NOT EXISTS TIMEMILLIS FOR "com.flexive.h2.StoredProcedures.getTimeMillis";
 * CREATE ALIAS IF NOT EXISTS TOTIMESTAMP FOR "com.flexive.h2.StoredProcedures.toTimestamp";
 * CREATE ALIAS IF NOT EXISTS PERMISSIONS FOR "com.flexive.h2.StoredProcedures.permissions";
 * CREATE ALIAS IF NOT EXISTS PERMISSIONS2 FOR "com.flexive.h2.StoredProcedures.permissions2";
 * CREATE ALIAS IF NOT EXISTS MAYREADINSTANCE FOR "com.flexive.h2.StoredProcedures.mayReadInstance";
 * CREATE ALIAS IF NOT EXISTS MAYREADINSTANCE2 FOR "com.flexive.h2.StoredProcedures.mayReadInstance2";
 * CREATE ALIAS IF NOT EXISTS TREE_ISLEAF FOR "com.flexive.h2.StoredProcedures.tree_isLeaf";
 * CREATE ALIAS IF NOT EXISTS TREE_IDTOPATH FOR "com.flexive.h2.StoredProcedures.tree_idToPath";
 * CREATE ALIAS IF NOT EXISTS TREE_PATHTOID FOR "com.flexive.h2.StoredProcedures.tree_pathToId";
 * CREATE ALIAS IF NOT EXISTS TREE_IDCHAIN FOR "com.flexive.h2.StoredProcedures.tree_idchain";
 * CREATE ALIAS IF NOT EXISTS TREE_FTEXT1024_CHAIN FOR "com.flexive.h2.StoredProcedures.tree_FTEXT1024_Chain";
 * CREATE ALIAS IF NOT EXISTS TREE_FTEXT1024_PATHS FOR "com.flexive.h2.StoredProcedures.tree_FTEXT1024_Paths";
 * CREATE ALIAS IF NOT EXISTS TREE_CAPTIONPATHTOID FOR "com.flexive.h2.StoredProcedures.tree_captionPathToID";
 * CREATE ALIAS IF NOT EXISTS TREE_NODEINDEX FOR "com.flexive.h2.StoredProcedures.tree_nodeIndex";
 * CREATE ALIAS IF NOT EXISTS TREE_GETPOSITION FOR "com.flexive.h2.StoredProcedures.tree_getPosition";
 * CREATE ALIAS IF NOT EXISTS CONCAT_WS FOR "com.flexive.h2.StoredProcedures.concat_ws";
 * <p/>
 * Examples:
 * SELECT TIMEMILLIS(NOW());
 * select permissions(1,1,1);
 * <p/>
 * Remarks:
 * not implemented since not needed: printTree(_lang char(2))
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class StoredProcedures {

    /**
     * Get a timestamp as BIGINT (long).
     * Source: http://groups.google.com/group/h2-database/browse_thread/thread/a0364dbaa4edb4a2
     *
     * @param ts timestamp
     * @return long (BIGINT) value
     */
    public static long getTimeMillis(Timestamp ts) {
        return ts.getTime();
    }

    /**
     * Convert a long value to a timestamp
     *
     * @param expr long value
     * @return timestamp
     */
    public static Timestamp toTimestamp(long expr) {
        return new Timestamp(expr);
    }


    //no permissions granted
    private final static String PERM_NOPERMISSIONS = "000000";
    //all permissions granted
    private final static String PERM_ALLPERMISSIONS = "111111";
    //special group: OWNER
    private final static long GRP_OWNER = 2;

    /**
     * This function retrieves the permissions of a given content instance for the given user
     *
     * @param con        connection provided by the database
     * @param contentId  the content id
     * @param contentVer the content version
     * @param userId     the user to retrieve permissions for
     * @return the permissions as [0|1] string array.
     *         Order: READ|EDIT|DELETE|CREATE|EXPORT|REL
     *         1=right granted, 0=right denied
     *         Example: '110000' for READ and EDIT granted, everything else denied
     * @throws SQLException on errors
     */
    public static String permissions(Connection con, Long contentId, Long contentVer, Long userId) throws SQLException {
        if (userId == 2)
            return PERM_ALLPERMISSIONS; //default supervisor account may do everything
        //get relevant role membership
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT ROLE FROM FXS_ROLEMAPPING WHERE ACCOUNT=" + userId + " OR USERGROUP IN(SELECT USERGROUP FROM FXS_USERGROUPMEMBERS WHERE ACCOUNT=" + userId + ")");

        boolean isSuperVisor = false;
        boolean isMandatorSupervisor = false;
        while (rs != null && rs.next()) {
            long role = rs.getLong(1);
            if (!rs.wasNull()) {
                if (role == 1)
                    isSuperVisor = true;
                else if (role == 2)
                    isMandatorSupervisor = true;
            }
        }
        stmt.close();

        if (isSuperVisor)
            return PERM_ALLPERMISSIONS;

        //get the users mandator
        stmt = con.createStatement();
        rs = stmt.executeQuery("SELECT MANDATOR FROM FXS_ACCOUNTS WHERE ID=" + userId);
        long userMandator = -1;
        if (rs != null && rs.next()) {
            userMandator = rs.getLong(1);
            if (rs.wasNull())
                userMandator = -1;
        }
        stmt.close();
        if (userMandator == -1)
            return PERM_NOPERMISSIONS;
        return permissionHelper(con, contentId, contentVer, userId, isMandatorSupervisor, userMandator);
    }

    /**
     * This function retrieves the permissions of a given content instance for the given user.
     * Same as permissions(), but faster since more data has to be specified when calling.
     *
     * @param con                  connection provided by the database
     * @param contentId            the content id
     * @param contentVer           the content version
     * @param userId               the user to retrieve permissions for
     * @param userMandator         mandator of the calling user
     * @param isMandatorSupervisor is the user mandator supervisor?
     * @param isSuperVisor         if the user global supervisor?
     * @return the permissions as [0|1] string array.
     *         Order: READ|EDIT|DELETE|CREATE|EXPORT|REL
     *         1=right granted, 0=right denied
     *         Example: '110000' for READ and EDIT granted, everything else denied
     * @throws SQLException on errors
     */
    public static String permissions2(Connection con, Long contentId, Long contentVer, Long userId, Long userMandator,
                                      Boolean isMandatorSupervisor, Boolean isSuperVisor) throws SQLException {
        if (isSuperVisor)
            return PERM_ALLPERMISSIONS;
        return permissionHelper(con, contentId, contentVer, userId, isMandatorSupervisor, userMandator);
    }

    /**
     * This function returns true if the user has read access on the given instance.
     *
     * @param con        connection provided by the database
     * @param contentId  the content id
     * @param contentVer the content version
     * @param userId     the user to retrieve permissions for
     * @return true if read permission is granted
     * @throws SQLException on errors
     */
    public static Boolean mayReadInstance(Connection con, Long contentId, Long contentVer, Long userId) throws SQLException {
        return permissions(con, contentId, contentVer, userId).charAt(0) == '1';
    }

    /**
     * This function returns true if the user has read access on the given instance.
     * Same as mayReadInstance(), but faster since more data has to be specified when calling.
     *
     * @param con                  connection provided by the database
     * @param contentId            the content id
     * @param contentVer           the content version
     * @param userId               the user to retrieve permissions for
     * @param userMandator         mandator of the calling user
     * @param isMandatorSupervisor is the user mandator supervisor?
     * @param isSuperVisor         if the user global supervisor?
     * @return true if read permission is granted
     * @throws SQLException on errors
     */
    public static Boolean mayReadInstance2(Connection con, Long contentId, Long contentVer, Long userId, Long userMandator,
                                           Boolean isMandatorSupervisor, Boolean isSuperVisor) throws SQLException {
        return permissions2(con, contentId, contentVer, userId, userMandator, isMandatorSupervisor, isSuperVisor).charAt(0) == '1';
    }

    /**
     * Helper to fetch and calculate permissions
     *
     * @param con                connection provided by the database
     * @param contentId          the content id
     * @param contentVer         the content version
     * @param userId             the user to retrieve permissions for
     * @param mandatorSupervisor is the user mandator supervisor?
     * @param userMandator       mandator of the user
     * @return the permissions as [0|1] string array.
     *         Order: READ|EDIT|DELETE|CREATE|EXPORT|REL
     *         1=right granted, 0=right denied
     *         Example: '110000' for READ and EDIT granted, everything else denied
     * @throws SQLException on errors
     */
    private static String permissionHelper(Connection con, Long contentId, Long contentVer, Long userId, boolean mandatorSupervisor, long userMandator) throws SQLException {
        ResultSet rs;//fetch the actual permissions
        PreparedStatement ps = con.prepareStatement("select " +
                //   1              2             3         4           5           6        7         8           9            10        11
                "dat.created_by,ass.usergroup,ass.PEDIT,ass.PREMOVE,ass.PEXPORT,ass.PREL,ass.PREAD,ass.PCREATE,acl.cat_type,dat.mandator,dat.securityMode" +
                " from" +
                "    (select con.mandator,con.step,con.created_by,con.id,con.ver,con.tdef,con.acl,stp.acl stepAcl,typ.acl typeAcl,typ.security_mode securityMode" +
                " from FX_CONTENT con,FXS_TYPEDEF typ, FXS_WF_STEPS stp where con.id=? and con.ver=? and " +
                " con.tdef=typ.id and stp.id=con.step) dat, FXS_ACLASSIGNMENTS ass, FXS_ACL acl " +
                "where" +
                " acl.id=ass.acl and" +
                " ass.usergroup in (select usergroup from FXS_USERGROUPMEMBERS where account=? union select " + GRP_OWNER +
                " from FXS_USERGROUPMEMBERS) and" +
                " (ass.acl=dat.acl or ass.acl=dat.typeAcl or ass.acl=dat.stepAcl)" +
                " UNION " +
                "select " +
                "dat.created_by,ass.usergroup,ass.PEDIT,ass.PREMOVE,ass.PEXPORT,ass.PREL,ass.PREAD,ass.PCREATE,acl.cat_type,dat.mandator,dat.securityMode" +
                " from" +
                "    (select con.mandator,con.step,con.created_by,con.id,con.ver,con.tdef,con.acl,stp.acl stepAcl,typ.acl typeAcl,typ.security_mode securityMode" +
                //                                                                   4            5
                " from FX_CONTENT con,FXS_TYPEDEF typ, FXS_WF_STEPS stp where con.id=? and con.ver=? and " +
                " con.tdef=typ.id and stp.id=con.step) dat, FXS_ACLASSIGNMENTS ass, FXS_ACL acl, " +
                //                                                   6      7
                " (select ca.acl from FX_CONTENT_ACLS ca where ca.id=? and ca.ver=?) contentACLs " +
                "where" +
                " acl.id=ass.acl and" +
                //                                                                      8
                " ass.usergroup in (select usergroup from FXS_USERGROUPMEMBERS where account=? union select " + GRP_OWNER +
                " from FXS_USERGROUPMEMBERS) and" +
                " ass.acl=contentACLs.acl"
        );
        ps.setLong(1, contentId);
        ps.setLong(2, contentVer);
        ps.setLong(3, userId);
        ps.setLong(4, contentId);
        ps.setLong(5, contentVer);
        ps.setLong(6, contentId);
        ps.setLong(7, contentVer);
        ps.setLong(8, userId);

        rs = ps.executeQuery();

//  -- Instance
        boolean IPREMOVE = false;
        boolean IPEDIT = false;
        boolean IPEXPORT = false;
        boolean IPREL = false;
        boolean IPREAD = false;
        boolean IPCREATE = false;
//  -- Step
        boolean SPREMOVE = false;
        boolean SPEDIT = false;
        boolean SPEXPORT = false;
        boolean SPREL = false;
        boolean SPREAD = false;
        boolean SPCREATE = false;
//  -- Type
        boolean TPREMOVE = false;
        boolean TPEDIT = false;
        boolean TPEXPORT = false;
        boolean TPREL = false;
        boolean TPREAD = false;
        boolean TPCREATE = false;

        long instanceMandator = -1;
        while (rs != null && rs.next()) {
            if (instanceMandator == -1)
                instanceMandator = rs.getLong(10);
            final int typeSecurityMode = rs.getInt(11);
            if ((typeSecurityMode & 0x01) == 0) {
                // content permissions are disabled
                IPREMOVE = IPEDIT = IPEXPORT = IPREL = IPREAD = IPCREATE = true;
            }
            if ((typeSecurityMode & 0x08) == 0) {
                // type permissions are disabled
                TPREMOVE = TPEDIT = TPEXPORT = TPREL = TPREAD = TPCREATE = true;
            }
            if ((typeSecurityMode & 0x04) == 0) {
                // workflow step permissions are disabled
                SPREMOVE = SPEDIT = SPEXPORT = SPREL = SPREAD = SPCREATE = true;
            }
            switch (rs.getInt(9)) { //cat_type
                case 1: //content
                    if (rs.getLong(2) != GRP_OWNER || (rs.getLong(2) == GRP_OWNER && rs.getLong(1) == userId)) {
                        if (rs.getBoolean(3)) IPEDIT = true;
                        if (rs.getBoolean(4)) IPREMOVE = true;
                        if (rs.getBoolean(5)) IPEXPORT = true;
                        if (rs.getBoolean(6)) IPREL = true;
                        if (rs.getBoolean(7)) IPREAD = true;
                        if (rs.getBoolean(8)) IPCREATE = true;
                    }
                    break;
                case 2: //type
                    if (rs.getBoolean(3)) TPEDIT = true;
                    if (rs.getBoolean(4)) TPREMOVE = true;
                    if (rs.getBoolean(5)) TPEXPORT = true;
                    if (rs.getBoolean(6)) TPREL = true;
                    if (rs.getBoolean(7)) TPREAD = true;
                    if (rs.getBoolean(8)) TPCREATE = true;
                    break;
                case 3: //Workflow step
                    if (rs.getBoolean(3)) SPEDIT = true;
                    if (rs.getBoolean(4)) SPREMOVE = true;
                    if (rs.getBoolean(5)) SPEXPORT = true;
                    if (rs.getBoolean(6)) SPREL = true;
                    if (rs.getBoolean(7)) SPREAD = true;
                    if (rs.getBoolean(8)) SPCREATE = true;
                    break;
            }
        }
        ps.close();
        if (mandatorSupervisor && userMandator == instanceMandator)
            return PERM_ALLPERMISSIONS;
        return ((IPREAD && TPREAD && SPREAD) ? "1" : "0") +
                ((IPEDIT && TPEDIT && SPEDIT) ? "1" : "0") +
                ((IPREMOVE && TPREMOVE && SPREMOVE) ? "1" : "0") +
                ((IPCREATE && TPCREATE && SPCREATE) ? "1" : "0") +
                ((IPEXPORT && TPEXPORT && SPEXPORT) ? "1" : "0") +
                ((IPREL && TPREL && SPREL) ? "1" : "0");
    }

    /**
     * Is the given node a leaf node?
     *
     * @param con    connection provided by the database
     * @param nodeId requested node id
     * @return node is a leaf
     * @throws SQLException on errors
     */
    public static Boolean tree_isLeaf(Connection con, Long nodeId) throws SQLException {
        Statement stmt = con.createStatement();
        try {
            ResultSet rs = stmt.executeQuery("select count(*) from FXS_TREE where parent=" + nodeId);
            return !(rs != null && rs.next()) || rs.getLong(1) == 0;
        } finally {
            stmt.close();
        }
    }

    /**
     * Get the path (excluding the root node) for a node
     *
     * @param con    connection provided by the database
     * @param nodeId requested node id
     * @param live   live or edit tree?
     * @return path excluding the root node
     * @throws SQLException on errors
     */
    public static String tree_idToPath(Connection con, Long nodeId, Boolean live) throws SQLException {
        if (nodeId == 1)
            return "/";
        Statement stmt = con.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT parent.name FROM FXS_TREE" + (live ? "_LIVE" : "") + " AS node, FXS_TREE" +
                (live ? "_LIVE" : "") + " AS parent\n" +
                "    WHERE node.lft>=parent.lft and node.lft<=parent.rgt AND node.id=" + nodeId +
                "    ORDER BY parent.lft;");
        StringBuilder sb = new StringBuilder(500);
        boolean root = true;
        while (rs != null && rs.next()) {
            if (root) {
                root = false; //skip root node
                continue;
            }
            sb.append('/');
            sb.append(rs.getString(1));
        }
        return sb.toString();
    }

    /**
     * Get the id of a tree path's leaf
     *
     * @param con       connection provided by the database
     * @param startNode the start node
     * @param _path     requested path
     * @param live      live or edit tree?
     * @return id of a tree path's leaf
     * @throws SQLException on errors
     */
    public static Long tree_pathToId(Connection con, Long startNode, String _path, Boolean live) throws SQLException {
        if ("/".equals(_path) || _path == null || _path.length() == 0)
            return 1L; //root node
        PreparedStatement ps = con.prepareStatement("SELECT ID FROM FXS_TREE" + (live ? "_LIVE" : "") + " WHERE NAME=? AND PARENT=?");
        String[] names = _path.substring(1).split("/");
        ResultSet rs;
        long currentParent = startNode;
        try {
            for (String name : names) {
                ps.setString(1, name);
                ps.setLong(2, currentParent);
                rs = ps.executeQuery();
                if (rs != null && rs.next()) {
                    currentParent = rs.getLong(1);
                } else {
                    return null;
                }
            }
            return currentParent;
        } finally {
            ps.close();
        }
    }

    /**
     * Get a chain of id's for the given node (from the root node) like /1/4/42 for node 42
     *
     * @param con    connection provided by the database
     * @param nodeId requested node
     * @param live   live or edit tree?
     * @return chain of id's for the given node (from the root node) like /1/4/42 for node 42
     * @throws SQLException on errors
     */
    public static String tree_idchain(Connection con, Long nodeId, Boolean live) throws SQLException {
        if (nodeId == 1L)
            return "/1"; //root node
        PreparedStatement ps = con.prepareStatement("SELECT PARENT FROM FXS_TREE" + (live ? "_LIVE" : "") + " WHERE ID=?");
        ResultSet rs;
        long currentNode = nodeId; //start at requested node
        StringBuilder sb = new StringBuilder(500);
        sb.insert(0, currentNode);
        sb.insert(0, '/');
        try {
            while (currentNode > 1) {
                ps.setLong(1, currentNode);
                rs = ps.executeQuery();
                if (rs != null && rs.next()) {
                    currentNode = rs.getLong(1);
                    sb.insert(0, currentNode);
                    sb.insert(0, '/');
                } else {
                    return null;
                }
            }
            return sb.toString();
        } finally {
            ps.close();
        }
    }

    /**
     * Get a chain of nodes with their caption from the root node plus additional information
     * Result format: /<node name>:<nodeId>:<refId>:<typeDefId>/...
     * <p/>
     * Example: select tree_FTEXT1024_Chain(2, 1, 20, false)
     *
     * @param con     connection provided by the database
     * @param _nodeId requested node
     * @param _lang   desired language for the name
     * @param _tprop  property id of the name field
     * @param _live   live or edit tree?
     * @return caption chain
     * @throws SQLException on errors
     */
    public static String tree_FTEXT1024_Chain(Connection con, Long _nodeId, Integer _lang, Long _tprop, Boolean _live) throws SQLException {
        if (_nodeId == 1L)
            return "/";
        String _result = "";
        final String TABLE = "FXS_TREE" + (_live ? "_LIVE" : "");
        PreparedStatement psNode = con.prepareStatement("SELECT PARENT,REF,NAME,IFNULL((SELECT TDEF FROM FX_CONTENT WHERE ID=REF AND ISMAX_VER=TRUE LIMIT 1),-1) TDEF" +
                " FROM " + TABLE + " WHERE ID=?;");
        PreparedStatement psDisplay = con.prepareStatement("SELECT IFNULL(IFNULL(" +
                //                                               1                                                           2          3
                "(SELECT FTEXT1024 FROM FX_CONTENT_DATA WHERE ID=? AND IS" + (_live ? "LIVE" : "MAX") + "_VER=TRUE AND TPROP=? AND LANG=? LIMIT 0,1)," +
                //                                               4                                                           5
                "(SELECT FTEXT1024 FROM FX_CONTENT_DATA WHERE ID=? AND IS" + (_live ? "LIVE" : "MAX") + "_VER=TRUE AND TPROP=? AND ISMLDEF LIMIT 0,1)" +
                // 6
                "),?);");
        try {
            psDisplay.setLong(2, _tprop);
            psDisplay.setInt(3, _lang);
            psDisplay.setLong(5, _tprop);

            long _id, _ref, _tdef;
            String _nodeName, _display;
            while (_nodeId != -1L) {
                psNode.setLong(1, _nodeId);
                ResultSet rsNode = psNode.executeQuery();
                if (rsNode != null && rsNode.next()) {
                    _id = rsNode.getLong(1);
                    if (rsNode.wasNull())
                        _id = -1;
                    _ref = rsNode.getLong(2);
                    _nodeName = rsNode.getString(3);
                    _tdef = rsNode.getLong(4);

                    psDisplay.setLong(1, _ref);
                    psDisplay.setLong(4, _ref);
                    psDisplay.setString(6, _nodeName);
                    ResultSet rsDisplay = psDisplay.executeQuery();
                    if (rsDisplay != null && rsDisplay.next()) {
                        _display = rsDisplay.getString(1);
                        if (rsDisplay.wasNull())
                            _display = "<null>";
                        // '/' and ':' are reserved characters, so we replace them with a space
                        _display = _display.replace('/', ' ');
                        _display = _display.replace(':', ' ');
                        if (_id != -1) {
                            _display += ":" + _nodeId + ":" + _ref + ":" + _tdef;
                            _result = "/" + _display + _result;
                        }
                        _nodeId = _id;
                    }
                } else
                    _nodeId = -1L;
            }
            return _result;
        } finally {
            psNode.close();
            psDisplay.close();
        }
    }

    /**
     * Obtains all paths for a given instance id.
     * The path elements are separated by a '/' character, and every element is encoded:
     * <displayName>:<treeNodeId>:<refId>
     *
     * @param con        connection provided by the database
     * @param _contentId the instance id
     * @param _lang      the language to retrieve
     * @param _tprop     the property to use for the path (must be of type FTEXT1024)
     * @param _live      true:  read from the live tree,
     *                   false: read from the edit tree,
     *                   null:  read from both trees (paths will be returned double if contained in both trees)
     * @return all paths for a given instance id
     * @throws SQLException on errors
     */
    public static String tree_FTEXT1024_Paths(Connection con, Long _contentId, Integer _lang, Long _tprop, Boolean _live) throws SQLException {
        StringBuilder sb = new StringBuilder(2000);
        if (_live == Boolean.TRUE || _live == null) {
            PreparedStatement ps = con.prepareStatement("SELECT tree_FTEXT1024_Chain(ID,?,?,TRUE) CHAIN FROM FXS_TREE_LIVE WHERE REF=? ORDER BY CHAIN");
            ps.setLong(1, _lang);
            ps.setLong(2, _tprop);
            ps.setLong(3, _contentId);
            try {
                ResultSet rs = ps.executeQuery();
                while (rs != null && rs.next()) {
                    if (sb.length() > 0)
                        sb.append('\n');
                    sb.append(rs.getString(1));
                }
            } finally {
                ps.close();
            }
        }
        if (_live == Boolean.FALSE || _live == null) {
            PreparedStatement ps = con.prepareStatement("SELECT tree_FTEXT1024_Chain(ID,?,?,FALSE) CHAIN FROM FXS_TREE WHERE REF=? ORDER BY CHAIN");
            ps.setLong(1, _lang);
            ps.setLong(2, _tprop);
            ps.setLong(3, _contentId);
            try {
                ResultSet rs = ps.executeQuery();
                while (rs != null && rs.next()) {
                    if (sb.length() > 0)
                        sb.append('\n');
                    sb.append(rs.getString(1));
                }
            } finally {
                ps.close();
            }
        }
        return sb.toString();
    }

    /**
     * Find the node-Id from a Caption-Path.
     * The path is being processed from left to right to match the name
     *
     * @param con       provided connection
     * @param startNode the start node to start looking at
     * @param path      requested caption path
     * @param tprop     property containing the caption
     * @param lang      desired language
     * @param live      live or edit tree?
     * @return node id or null if not found
     * @throws SQLException on errors
     */
    public static Long tree_captionPathToID(Connection con, Long startNode, String path, Long tprop, Integer lang, Boolean live) throws SQLException {
        if (startNode == null)
            startNode = 1L;
        if (path == null || path.length() == 0 || path.equals("/")) {
            if (startNode == 1L)
                return 1L; //root node
            return null; //invalid
        }
        //remove trailing '/'
        if (path.startsWith("/"))
            path = path.substring(1);
        String[] nodes = path.split("/");
        final String TABLE = "FXS_TREE" + (live ? "_LIVE" : "");
        String sql = "SELECT ID FROM " + TABLE + " node WHERE " +
                "IFNULL(IFNULL(" +
                "(SELECT f.FTEXT1024 FROM FX_CONTENT_DATA f WHERE f.TPROP=" + tprop + " AND LANG IN (" + lang + ",0) AND f.ISMAX_VER=TRUE AND f.ID=node.REF LIMIT 1)," +
                "(SELECT f.FTEXT1024 FROM FX_CONTENT_DATA f WHERE f.TPROP=" + tprop + " AND ISMLDEF=TRUE AND f.ISMAX_VER=TRUE AND f.ID=node.REF LIMIT 1)" +
                "),node.NAME)=? AND PARENT=?";
        long _result = startNode;
        PreparedStatement ps = con.prepareStatement(sql);
        ResultSet rs;
        try {
            for (String node : nodes) {
                ps.setString(1, node);
                ps.setLong(2, _result);
                rs = ps.executeQuery();
                if (rs != null && rs.next())
                    _result = rs.getLong(1);
                else
                    return null;
            }
        } finally {
            ps.close();
        }
        return _result;
    }

    /**
     * Obtains the position of a reference within a subtree.
     * <p/>
     * Example: select tree_nodeIndex(2, 10, false)
     *
     * @param con        connection provided by the database
     * @param rootNodeId id of the subtree's root node
     * @param refId      requested reference
     * @param live       live or edit tree?
     * @return position within subtree (0 if nodeId is first node or not found)
     * @throws SQLException on errors
     */
    public static Integer tree_nodeIndex(Connection con, Long rootNodeId, Long refId, Boolean live) throws SQLException {
        final String TABLE = "FXS_TREE" + (live ? "_LIVE" : "");
        PreparedStatement ps = con.prepareStatement("SELECT REF FROM " + TABLE + " WHERE " +
                "LFT>=(SELECT LFT FROM " + TABLE + " WHERE ID=?) AND RGT<=(SELECT RGT FROM " + TABLE + " WHERE ID=?) ORDER BY LFT");
        try {
            int pos = 0;
            ps.setLong(1, rootNodeId);
            ps.setLong(2, rootNodeId);
            ResultSet rs = ps.executeQuery();
            while (rs != null && rs.next()) {
                if (rs.getLong(1) == refId)
                    return pos;
                pos++;
            }
            return pos;
        } finally {
            ps.close();
        }
    }

    /**
     * Get the position of a node that is a child node of parentId relative to all children of parentId starting at 1
     *
     * @param con      connection provided by the database
     * @param live     live or edit tree?
     * @param nodeId   id of the node to get the position for
     * @param parentId id of the parent node (no checks are performed if nodeId is actually a child of parentId!)
     * @return position relative to all children of the parent node, starting at 1 or NULL if the node is no child of parent
     * @throws SQLException on errors
     */
    public static Integer tree_getPosition(Connection con, Boolean live, Long nodeId, Long parentId) throws SQLException {
        final String TABLE = "FXS_TREE" + (live ? "_LIVE" : "");
        PreparedStatement ps = con.prepareStatement("SELECT ID FROM " + TABLE + " WHERE PARENT=? ORDER BY LFT");
        try {
            int pos = 0;
            if (parentId == null)
                ps.setNull(1, java.sql.Types.NUMERIC);
            else
                ps.setLong(1, parentId);
            ResultSet rs = ps.executeQuery();
            while (rs != null && rs.next()) {
                if (rs.getLong(1) == nodeId)
                    return pos;
                pos++;
            }
            return null;
        } finally {
            ps.close();
        }
    }

    /**
     * Concatenate with separator (mimics MySQL CONCAT_WS function)
     *
     * @param separator separator to use
     * @param args      arguments to concatenate
     * @return concatenated arguments using the separator
     * @see http://dev.mysql.com/doc/refman/5.0/en/string-functions.html#function_concat-ws
     */
    public static String concat_ws(String separator, String... args) {
        StringBuilder ret = new StringBuilder(500);
        for (String arg : args) {
            if (ret.length() > 0)
                ret.append(separator);
            ret.append(arg);
        }
        return ret.toString();
    }
}
