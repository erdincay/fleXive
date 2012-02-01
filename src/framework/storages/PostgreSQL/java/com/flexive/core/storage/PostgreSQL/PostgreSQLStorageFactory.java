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
package com.flexive.core.storage.PostgreSQL;

import com.flexive.core.search.DataFilter;
import com.flexive.core.search.DataSelector;
import com.flexive.core.search.PostgreSQL.PostgreSQLDataFilter;
import com.flexive.core.search.PostgreSQL.PostgreSQLDataSelector;
import com.flexive.core.search.SqlSearch;
import com.flexive.core.search.cmis.impl.CmisSqlQuery;
import com.flexive.core.search.cmis.impl.sql.PostgreSQL.PostgreSQLDialect;
import com.flexive.core.search.cmis.impl.sql.SqlDialect;
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

/**
 * Factory for the PostgreSQL storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class PostgreSQLStorageFactory extends GenericDBStorage implements DBStorage {
    private static final Log LOG = LogFactory.getLog(PostgreSQLStorageFactory.class);
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

    /**
     * {@inheritDoc}
     */
    public ContentStorage getContentStorage(TypeStorageMode mode) throws FxNotFoundException {
        if (mode == TypeStorageMode.Hierarchical)
            return PostgreSQLHierarchicalStorage.getInstance();
        throw new FxNotFoundException("ex.structure.typeStorageMode.notImplemented", mode);
    }

    /**
     * {@inheritDoc}
     */
    public EnvironmentLoader getEnvironmentLoader() {
        return PostgreSQLEnvironmentLoader.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public SequencerStorage getSequencerStorage() {
        return PostgreSQLSequencerStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public TreeStorage getTreeStorage() {
        return PostgreSQLTreeStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public DataSelector getDataSelector(SqlSearch search) throws FxSqlSearchException {
        return new PostgreSQLDataSelector(search);
    }

    /**
     * {@inheritDoc}
     */
    public DataFilter getDataFilter(Connection con, SqlSearch search) throws FxSqlSearchException {
        return new PostgreSQLDataFilter(con, search);
    }

    /**
     * {@inheritDoc}
     */
    public SqlDialect getCmisSqlDialect(FxEnvironment environment, ContentEngine contentEngine, CmisSqlQuery query, boolean returnPrimitives) {
        return new PostgreSQLDialect(environment, contentEngine, query, returnPrimitives);
    }

    /**
     * {@inheritDoc}
     */
    public String getIfFunction(String condition, String exprtrue, String exprfalse) {
        return "(CASE WHEN(" + condition + ")THEN(" + exprtrue + ")ELSE (" + exprfalse + ")END)";
    }

    /**
     * {@inheritDoc}
     */
    public String getRegExpLikeOperator(String column, String regexp) {
        return column + " ~ " + regexp;
    }

    /**
     * {@inheritDoc}
     */
    public String getReferentialIntegrityChecksStatement(boolean enable) {
        return "SET CONSTRAINTS ALL " + (enable ? "IMMEDIATE" : "DEFERRED");
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDisableIntegrityTransactional() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public String getTimestampFunction() {
        return "TIMEMILLIS(NOW())";
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
        if (!(exc instanceof SQLException))
            return false;
        //see http://www.postgresql.org/docs/8.4/interactive/errcodes-appendix.html
        return "23503".equals(((SQLException) exc).getSQLState());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isQueryTimeout(Exception e) {
        //not supported out of the box. see: http://stackoverflow.com/questions/1175173/jdbc-postgres-query-with-a-timeout
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isRollbackOnConstraintViolation() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isUniqueConstraintViolation(Exception exc) {
        if (!(exc instanceof SQLException))
            return false;
        //see http://www.postgresql.org/docs/8.4/interactive/errcodes-appendix.html
        return "23505".equals(((SQLException) exc).getSQLState());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDeadlock(Exception exc) {
        if (!(exc instanceof SQLException))
            return false;
        //see http://www.postgresql.org/docs/8.4/interactive/errcodes-appendix.html
        return "40P01".equals(((SQLException) exc).getSQLState());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDuplicateKeyViolation(SQLException exc) {
        //see http://www.postgresql.org/docs/8.4/interactive/errcodes-appendix.html
        return "42710".equals(exc.getSQLState()) || "23505".equals(exc.getSQLState());
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
            if (StringUtils.isBlank(database))
                throw new IllegalArgumentException("No database name (path) specified!");
            System.out.println("using database [" + database + "], schema [" + schema + "] for " + VENDOR);
            String url = jdbcURL;
            if (StringUtils.isBlank(url))
                throw new IllegalArgumentException("No JDBC URL provided!");
            url = url.trim();
            if (!StringUtils.isBlank(jdbcURLParameters))
                url = url + jdbcURLParameters;

            try {
                Class.forName("org.postgresql.Driver").newInstance();
            } catch (ClassNotFoundException e) {
                System.err.println("PostgreSQL JDBC Driver not found in classpath!");
                return null;
            }
            System.out.println("Connecting using JDBC URL " + url);
            con = DriverManager.getConnection(url, user, password);
            stmt = con.createStatement();
            int cnt = 0;
            if (dropIfExist)
                cnt += stmt.executeUpdate("DROP DATABASE IF EXISTS \"" + database + "\"");
            if (createDB) {
                //first check if it already exists
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PG_CATALOG.PG_DATABASE WHERE DATNAME='" + database + "'");
                boolean exists = rs != null && rs.next() && rs.getInt(1) > 0;
                if (rs != null)
                    rs.close();
                if (!exists) {
                    cnt += stmt.executeUpdate("CREATE DATABASE \"" + database + "\"");
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
                cnt += stmt.executeUpdate("DROP SCHEMA IF EXISTS \"" + schema + "\" CASCADE");
                cnt += stmt.executeUpdate("CREATE SCHEMA \"" + schema + "\"");
            }
            cnt += stmt.executeUpdate("SET search_path TO \"" + schema + "\"");

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
