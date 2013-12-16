/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
import com.flexive.core.search.DataFilter;
import com.flexive.core.search.DataSelector;
import com.flexive.core.search.SqlSearch;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.sql.MySQL.MySqlDialect;
import com.flexive.core.search.cmis.impl.sql.SqlDialect;
import com.flexive.core.search.genericSQL.GenericSQLDataFilter;
import com.flexive.core.storage.*;
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

import static com.flexive.core.DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM;

/**
 * Factory for the MySQL storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class MySQLStorageFactory extends GenericDBStorage  implements DBStorage {
    private static final Log LOG = LogFactory.getLog(MySQLStorageFactory.class);
    private final static String VENDOR = "MySQL";

    /**
     * {@inheritDoc}
     */
    @Override
    public String getStorageVendor() {
        return VENDOR;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public ContentStorage getContentStorage(TypeStorageMode mode) throws FxNotFoundException {
        if (mode == TypeStorageMode.Hierarchical)
            return MySQLHierarchicalStorage.getInstance();
        throw new FxNotFoundException("ex.structure.typeStorageMode.notImplemented", mode);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentLoader getEnvironmentLoader() {
        return MySQLEnvironmentLoader.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SequencerStorage getSequencerStorage() {
        return MySQLSequencerStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeStorage getTreeStorage() {
        return MySQLTreeStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataSelector getDataSelector(SqlSearch search) throws FxSqlSearchException {
        return new MySQLDataSelector(search);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DataFilter getDataFilter(Connection con, SqlSearch search) throws FxSqlSearchException {
        return new GenericSQLDataFilter(con, search);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SqlDialect getCmisSqlDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives) {
        return new MySqlDialect(environment, contentEngine, query, returnPrimitives);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getIfFunction(String condition, String exprtrue, String exprfalse) {
        return "IF(" + condition + "," + exprtrue + "," + exprfalse + ")";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRegExpLikeOperator(String column, String regexp) {
        return column + " REGEXP " + regexp;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getReferentialIntegrityChecksStatement(boolean enable) {
        return "SET FOREIGN_KEY_CHECKS=" + (enable ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDisableIntegrityTransactional() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSelectListItemReferenceFixStatement() {
        return "UPDATE " + TBL_STRUCT_SELECTLIST_ITEM + " i1, " + TBL_STRUCT_SELECTLIST_ITEM +
                " i2 SET i1.PARENTID=? WHERE i1.PARENTID=i2.ID AND i2.LISTID=?";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTimestampFunction() {
        return "UNIX_TIMESTAMP()*1000";
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
    public String getLimit(boolean hasWhereClause, long limit) {
        return " LIMIT " + limit;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLimitOffset(boolean hasWhereClause, long limit, long offset) {
        return " LIMIT " + limit + " OFFSET " + offset;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isForeignKeyViolation(Exception exc) {
        final int errorCode = Database.getSqlErrorCode(exc);
        //see http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html
        return errorCode == 1451 || errorCode == 1217;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isQueryTimeout(Exception e) {
        final int errorCode = Database.getSqlErrorCode(e);
        //see http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html
        return errorCode == 1317 || errorCode == 1028
                || e.getClass().getName().equals("com.mysql.jdbc.exceptions.MySQLTimeoutException");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isRollbackOnConstraintViolation() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUniqueConstraintViolation(Exception exc) {
        final int sqlErr = Database.getSqlErrorCode(exc);
        //see http://dev.mysql.com/doc/refman/5.1/en/error-messages-server.html
        // 1582 Example error: Duplicate entry 'ABSTRACT' for key 'UK_TYPEPROPS_NAME'
        return (sqlErr == 1062 || sqlErr == 1582);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDeadlock(Exception exc) {
        //see http://dev.mysql.com/doc/refman/5.0/en/error-messages-server.html
        final int errorCode = Database.getSqlErrorCode(exc);
        return errorCode == 1213 || errorCode == 1479 || errorCode == 1205 /* lock timeout exception */
                || errorCode == 1614 || errorCode == 1613;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDuplicateKeyViolation(SQLException exc) {
        return Database.getSqlErrorCode(exc) == 1062; 
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean requiresConfigSchema() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    protected GenericDivisionImporter getDivisionImporter() {
        return MySQLDivisionImporter.getInstance();
    }
}
