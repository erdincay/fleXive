/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2009
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
package com.flexive.core.storage.MySQL;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_SELECTLIST_ITEM;
import com.flexive.core.search.DataFilter;
import com.flexive.core.search.DataSelector;
import com.flexive.core.search.SqlSearch;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.sql.MySQL.MySqlDialect;
import com.flexive.core.search.cmis.impl.sql.SqlDialect;
import com.flexive.core.search.genericSQL.GenericSQLDataFilter;
import com.flexive.core.search.genericSQL.GenericSQLDataSelector;
import com.flexive.core.storage.*;
import com.flexive.core.storage.genericSQL.GenericLockStorage;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxSqlSearchException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.TypeStorageMode;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Factory for the MySQL storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class MySQLStorageFactory implements DBStorage {
    private static final Log LOG = LogFactory.getLog(MySQLStorageFactory.class);
    private final static String VENDOR = "MySQL";
    final static String TRUE = "TRUE";
    final static String FALSE = "FALSE";

    /**
     * {@inheritDoc}
     */
    public String getStorageVendor() {
        return VENDOR;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canHandle(DatabaseMetaData dbm) {
        try {
            return VENDOR.equals(dbm.getDatabaseProductName());
        } catch (SQLException e) {
            LOG.error(e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ContentStorage getContentStorage(TypeStorageMode mode) throws FxNotFoundException {
        if (mode == TypeStorageMode.Hierarchical)
            return MySQLHierarchicalStorage.getInstance();
        throw new FxNotFoundException("ex.structure.typeStorageMode.notImplemented", mode);
    }

    /**
     * {@inheritDoc}
     */
    public EnvironmentLoader getEnvironmentLoader() {
        return MySQLEnvironmentLoader.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public SequencerStorage getSequencerStorage() {
        return MySQLSequencerStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public TreeStorage getTreeStorage() {
        return MySQLTreeStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public LockStorage getLockStorage() {
        return GenericLockStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public DataSelector getDataSelector(SqlSearch search) throws FxSqlSearchException {
        return new GenericSQLDataSelector(search);
    }

    /**
     * {@inheritDoc}
     */
    public DataFilter getDataFilter(Connection con, SqlSearch search) throws FxSqlSearchException {
        return new GenericSQLDataFilter(con, search);
    }

    /**
     * {@inheritDoc}
     */
    public SqlDialect getCmisSqlDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives) {
        return new MySqlDialect(environment, contentEngine, query, returnPrimitives);
    }

    /**
     * {@inheritDoc}
     */
    public String getBooleanTrueExpression() {
        return TRUE;
    }

    /**
     * {@inheritDoc}
     */
    public String getBooleanFalseExpression() {
        return FALSE;
    }

    /**
     * {@inheritDoc}
     */
    public String escapeReservedWords(String query) {
        return query; //nothing to escape
    }

    /**
     * {@inheritDoc}
     */
    public String getIfFunction(String condition, String exprtrue, String exprfalse) {
        return "IF(" + condition + "," + exprtrue + "," + exprfalse + ")";
    }

    /**
     * {@inheritDoc}
     */
    public String getRegExpLikeOperator(String column, String regexp) {
        return column + " REGEXP " + regexp;
    }

    /**
     * {@inheritDoc}
     */
    public String getReferentialIntegrityChecksStatement(boolean enable) {
        return "SET FOREIGN_KEY_CHECKS=" + (enable ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    public String getSelectListItemReferenceFixStatement() {
        return "UPDATE " + TBL_SELECTLIST_ITEM + " i1, " + TBL_SELECTLIST_ITEM +
                " i2 SET i1.PARENTID=? WHERE i1.PARENTID=i2.ID AND i2.LISTID=?";
    }

    /**
     * {@inheritDoc}
     */
    public String getTimestampFunction() {
        return "UNIX_TIMESTAMP()*1000";
    }

    /**
     * {@inheritDoc}
     */
    public String concat(String... text) {
        if( text.length == 0)
            return "";
        if( text.length == 1)
            return text[0];
        StringBuilder sb = new StringBuilder(500);
        for (int i = 1; i < text.length; i++)
            sb.append("CONCAT(");
        for (int i = 0; i < text.length; i++) {
            if (i > 0)
                sb.append(',');
            sb.append(text[i]);
            if (i > 0)
                sb.append(')');
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String concat_ws(String delimiter, String... text) {
        if( text.length == 0)
            return "";
        if( text.length == 1)
            return text[0];
        StringBuilder sb = new StringBuilder(500);
        sb.append("CONCAT_WS('").append(delimiter).append("'");
        for (String s : text)
            sb.append(',').append(s);
        sb.append(')');
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String getFromDual() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public String getLimit(boolean hasWhereClause, long limit) {
        return " LIMIT " + limit;
    }

    /**
     * {@inheritDoc}
     */
    public String getLimitOffset(boolean hasWhereClause, long limit, long offset) {
        return " LIMIT " + limit + " OFFSET " + offset;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isForeignKeyViolation(Exception exc) {
        final int errorCode = Database.getSqlErrorCode(exc);
        //see http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html
        return errorCode == 1451 || errorCode == 1217;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isQueryTimeout(Exception e) {
        final int errorCode = Database.getSqlErrorCode(e);
        //see http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html
        return errorCode == 1317 || errorCode == 1028
                || e.getClass().getName().equals("com.mysql.jdbc.exceptions.MySQLTimeoutException");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRollbackOnConstraintViolation() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUniqueConstraintViolation(Exception exc) {
        final int sqlErr = Database.getSqlErrorCode(exc);
        //see http://dev.mysql.com/doc/refman/5.1/en/error-messages-server.html
        // 1582 Example error: Duplicate entry 'ABSTRACT' for key 'UK_TYPEPROPS_NAME'
        return (sqlErr == 1062 || sqlErr == 1582);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDeadlock(Exception exc) {
        //see http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html
        final int errorCode = Database.getSqlErrorCode(exc);
        return errorCode == 1213 || errorCode == 1479;
    }

    /**
     * {@inheritDoc}
     */
    public boolean requiresConfigSchema() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public Connection getConnection(String database, String schema, String jdbcURL, String jdbcURLParameters,
                                    String user, String password, boolean createDB, boolean createSchema,
                                    boolean dropIfExist) throws Exception {
        Connection con = null;
        Statement stmt = null;
        try {
            String db = schema;
            if (StringUtils.isBlank(db))
                db = database;
            if (StringUtils.isBlank(db))
                throw new IllegalArgumentException("No database or schema name specified!");
            System.out.println("Using schema [" + db + "] for " + VENDOR);
            String url = jdbcURL;
            if (StringUtils.isBlank(url))
                throw new IllegalArgumentException("No JDBC URL provided!");
            url = url.trim();
            if (!StringUtils.isBlank(jdbcURLParameters)) {
                String p = jdbcURLParameters.trim();
                if (!(url.endsWith("?") || p.startsWith("?")))
                    p = "?" + (p.charAt(0) == '&' ? p.substring(1) : p);
                url = url + p;
            }
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (ClassNotFoundException e) {
                System.err.println("MySQL JDBC Driver not found in classpath!");
                return null;
            }
            System.out.println("Connecting using JDBC URL " + url);
            con = DriverManager.getConnection(url, user, password);
            stmt = con.createStatement();
            int cnt = 0;
            if (dropIfExist)
                cnt += stmt.executeUpdate("DROP DATABASE IF EXISTS " + db);
            if (createDB)
                cnt += stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + db);
            if (cnt > 0)
                System.out.println("Executed " + cnt + " statements");
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        } finally {
            if (stmt != null)
                stmt.close();
        }
        return con;
    }

    /**
     * {@inheritDoc}
     */
    public boolean initConfiguration(Connection con, String schema, boolean dropIfExist) throws Exception {
        Map<String, String> scripts = FxSharedUtils.getStorageScriptResources(getStorageVendor());
        List<String> s = new ArrayList<String>();
        s.addAll(scripts.keySet());
        Collections.sort(s);
        Statement stmt;
        stmt = con.createStatement();
        int cnt = 0;
        if (StringUtils.isBlank(schema))
            throw new IllegalArgumentException("No schema name specified!");
        schema = schema.trim();
        try {
            if (dropIfExist) {
                cnt += stmt.executeUpdate("DROP DATABASE IF EXISTS " + schema);
                cnt += stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + schema);
            }
            cnt += stmt.executeUpdate("USE " + schema);

            for (String script : s) {
                if (script.startsWith("config/")) {
                    System.out.println("Executing " + script + " ...");
                    cnt += new FxSharedUtils.SQLExecutor(con, scripts.get(script), script.startsWith("SP") ? "|" : ";",
                            false, true, System.out).
                            execute();
                }
            }
        } finally {
            if (stmt != null)
                stmt.close();
        }
        if (cnt > 0)
            System.out.println("Executed " + cnt + " statements");
        return cnt > 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean initDivision(Connection con, String schema, boolean dropIfExist) throws Exception {
        Map<String, String> scripts = FxSharedUtils.getStorageScriptResources(getStorageVendor());
        List<String> s = new ArrayList<String>();
        s.addAll(scripts.keySet());
        Collections.sort(s);
        Statement stmt;
        stmt = con.createStatement();
        int cnt = 0;
        if (StringUtils.isBlank(schema))
            throw new IllegalArgumentException("No schema name specified!");
        schema = schema.trim();
        try {
            if (dropIfExist) {
                cnt += stmt.executeUpdate("DROP DATABASE IF EXISTS " + schema);
                cnt += stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + schema);
            }
            cnt += stmt.executeUpdate("USE " + schema);

            for (String script : s) {
                if (script.indexOf('/') == -1 || script.startsWith("tree/")) {
                    System.out.println("Executing " + script + " ...");
                    cnt += new FxSharedUtils.SQLExecutor(con, scripts.get(script),
                            script.startsWith("SP") || script.startsWith("tree/") ? "|" : ";",
                            false, true, System.out).
                            execute();
                }
            }
        } finally {
            if (stmt != null)
                stmt.close();
        }
        if (cnt > 0)
            System.out.println("Executed " + cnt + " statements");
        return cnt > 0;
    }

}
