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
package com.flexive.core.storage.Postgres;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_SELECTLIST_ITEM;
import com.flexive.core.search.DataFilter;
import com.flexive.core.search.DataSelector;
import com.flexive.core.search.SqlSearch;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.sql.Postgres.PostgresDialect;
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
 * Factory for the Postgres storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class PostgresStorageFactory implements DBStorage {
    private static final Log LOG = LogFactory.getLog(PostgresStorageFactory.class);
    private final static String VENDOR = "PostgreSQL";

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

    public boolean createSchema(String schema, boolean dropIfExists, String jdbcURL, String jdbcDriver, String user, String password) {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public ContentStorage getContentStorage(TypeStorageMode mode) throws FxNotFoundException {
        if (mode == TypeStorageMode.Hierarchical)
            return PostgresHierarchicalStorage.getInstance();
        throw new FxNotFoundException("ex.structure.typeStorageMode.notImplemented", mode);
    }

    /**
     * {@inheritDoc}
     */
    public EnvironmentLoader getEnvironmentLoader() {
        return PostgresEnvironmentLoader.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public SequencerStorage getSequencerStorage() {
        return PostgresSequencerStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public TreeStorage getTreeStorage() {
        return PostgresTreeStorage.getInstance();
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
        return new PostgresDialect(environment, contentEngine, query, returnPrimitives);
    }

    /**
     * {@inheritDoc}
     */
    public String getIfFunction() {
        return "IF";
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
        // TODO
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean escapeSchema() {
        return true;
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
            if (StringUtils.isBlank(database))
                throw new IllegalArgumentException("No database name (path) specified!");
            System.out.println("Using database [" + database + "] for " + VENDOR);
            String url = jdbcURL;
            if (StringUtils.isBlank(url))
                throw new IllegalArgumentException("No JDBC URL provided!");
            url = url.trim();
            if (!StringUtils.isBlank(jdbcURLParameters))
                url = url + jdbcURLParameters;

            try {
                Class.forName("org.postgresql.Driver").newInstance();
            } catch (ClassNotFoundException e) {
                System.err.println("Postgres JDBC Driver not found in classpath!");
                return null;
            }
            System.out.println("Connecting using JDBC URL " + url);
            con = DriverManager.getConnection(url, user, password);
            stmt = con.createStatement();
            int cnt = 0;
            if (dropIfExist)
                cnt += stmt.executeUpdate("DROP DATABASE IF EXISTS " + database);
            if (createDB) {
                //first check if it already exists
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PG_CATALOG.PG_DATABASE WHERE DATNAME='" + database + "'");
                boolean exists = rs != null && rs.next() && rs.getInt(1) > 0;
                if (rs != null)
                    rs.close();
                if (!exists) {
                    cnt += stmt.executeUpdate("CREATE DATABASE " + database);
                    System.out.println("Created database [" + database + "].");
                } else
                    System.out.println("Database [" + database + "] already exists. Skipping create.");
            }

            if (!jdbcURL.endsWith("/" + database)) {
                //(re)connect to that database as there seems to be no way to change the database from a sql statement
                stmt.close();
                stmt = null;
                con.close();
                url = url + (url.endsWith("/") ? database : "/" + database);
                con = DriverManager.getConnection(url, user, password);
            }

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
                System.out.println("(Re)creating schema " + schema);
                cnt += stmt.executeUpdate("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
                cnt += stmt.executeUpdate("CREATE SCHEMA \"" + schema + "\"");
            }
            cnt += stmt.executeUpdate("SET search_path TO \"" + schema + "\"");

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
                System.out.println("(Re)creating schema " + schema);
                cnt += stmt.executeUpdate("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
                cnt += stmt.executeUpdate("CREATE SCHEMA " + schema);
            }
            cnt += stmt.executeUpdate("SET search_path TO " + schema);

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
