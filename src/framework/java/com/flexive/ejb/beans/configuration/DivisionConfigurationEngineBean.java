/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.ejb.beans.configuration;

import com.flexive.core.Database;
import static com.flexive.core.DatabaseConst.TBL_DIVISION_CONFIG;
import com.flexive.core.storage.ContentStorage;
import com.flexive.core.storage.StorageManager;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.configuration.DBVendor;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.configuration.ParameterScope;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxDbException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.interfaces.DivisionConfigurationEngine;
import com.flexive.shared.interfaces.DivisionConfigurationEngineLocal;
import com.flexive.shared.structure.TypeStorageMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.ejb.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Division configuration implementation.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */

@TransactionManagement(TransactionManagementType.CONTAINER)
@TransactionAttribute(TransactionAttributeType.REQUIRED)
@Stateless(name = "DivisionConfigurationEngine", mappedName="DivisionConfigurationEngine")
public class DivisionConfigurationEngineBean extends GenericConfigurationImpl implements DivisionConfigurationEngine, DivisionConfigurationEngineLocal {
    private static final Log LOG = LogFactory.getLog(DivisionConfigurationEngineBean.class);
    /**
     * Division config cache root.
     */
    private static final String CACHE_ROOT = "/divisionConfig";

    @Override
    protected ParameterScope getDefaultScope() {
        return ParameterScope.DIVISION;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Connection getConnection() throws SQLException {
        return Database.getDbConnection();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getInsertStatement(Connection conn, String path, String key, String value, String className)
            throws SQLException, FxNoAccessException {
        if (!FxContext.getUserTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.configuration.update.perm.division");
        }
        String sql = "INSERT INTO " + TBL_DIVISION_CONFIG + " (cpath, ckey, cvalue, className) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, path);
        stmt.setString(2, key);
        stmt.setString(3, value);
        stmt.setString(4, className);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getSelectStatement(Connection conn, String path, String key) throws SQLException {
        final String sql = "SELECT cvalue FROM " + TBL_DIVISION_CONFIG + " WHERE cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, path);
        stmt.setString(2, key);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getSelectStatement(Connection conn, String path) throws SQLException {
        final String sql = "SELECT ckey, cvalue FROM " + TBL_DIVISION_CONFIG + " WHERE cpath=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, path);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getSelectStatement(Connection conn) throws SQLException {
        final String sql = "SELECT cpath, ckey, cvalue, className FROM " + TBL_DIVISION_CONFIG;
        return conn.prepareStatement(sql);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getUpdateStatement(Connection conn, String path, String key, String value, String className)
            throws SQLException, FxNoAccessException {
        if (!FxContext.getUserTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.configuration.update.perm.division");
        }
        final String sql = "UPDATE " + TBL_DIVISION_CONFIG + " SET cvalue=?, className=? WHERE cpath=? AND ckey=?";
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, value);
        stmt.setString(2, className);
        stmt.setString(3, path);
        stmt.setString(4, key);
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PreparedStatement getDeleteStatement(Connection conn, String path, String key)
            throws SQLException, FxNoAccessException {
        if (!FxContext.getUserTicket().isGlobalSupervisor()) {
            throw new FxNoAccessException("ex.configuration.delete.perm.division");
        }
        final String sql = "DELETE FROM " + TBL_DIVISION_CONFIG + " WHERE cpath=? "
                + (key != null ? " AND ckey=?" : "");
        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setString(1, path);
        if (key != null) {
            stmt.setString(2, key);
        }
        return stmt;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getCachePath(String path) {
        return CACHE_ROOT + path;
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void installBinary(long binaryId, String resourceName) throws FxApplicationException {
        Connection con = null;
        try {
            ContentStorage storage = StorageManager.getContentStorage(TypeStorageMode.Hierarchical);
            String binaryName = resourceName;
            String subdir = "";
            if (binaryName.indexOf('/') > 0) {
                binaryName = binaryName.substring(binaryName.lastIndexOf('/') + 1);
                subdir = resourceName.substring(0, resourceName.lastIndexOf('/') + 1);
            }
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            con = getConnection();
            long length = 0;
            String[] files = FxSharedUtils.loadFromInputStream(cl.getResourceAsStream("fxresources/binaries/" + subdir + "resourceindex.flexive"), -1).
                    replaceAll("\r", "").split("\n");
            for (String file : files) {
                if (file.startsWith(binaryName + "|")) {
                    length = Long.parseLong(file.split("\\|")[1]);
                    break;
                }
            }
            //TODO: check if length is still 0 or exception
            storage.storeBinary(con, binaryId, 1, 1, binaryName, length, cl.getResourceAsStream("fxresources/binaries/" + resourceName));
        } catch (SQLException e) {
            throw new FxDbException(LOG, e, "ex.db.sqlError", e.getMessage());
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    private static class SQLPatchScript {
        long from;
        long to;
        String script;

        SQLPatchScript(long from, long to, String script) {
            this.from = from;
            this.to = to;
            this.script = script;
        }
    }

    /**
     * {@inheritDoc}
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void patchDatabase() throws FxApplicationException {
        final long oldVersion = get(SystemParameters.DB_VERSION);
        final long patchedVersion = performPatching();
        if (patchedVersion != oldVersion) {
            modifyDatabaseVersion(patchedVersion);
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private long performPatching() throws FxApplicationException {
        FxContext.get().runAsSystem();
        try {
            long dbVersion = get(SystemParameters.DB_VERSION);
            long currentVersion = dbVersion;
            if (dbVersion == -1) {
                put(SystemParameters.DB_VERSION, FxSharedUtils.getDBVersion());
                return dbVersion; //no need to patch
            } else if (dbVersion == FxSharedUtils.getDBVersion()) {
                //nothing to do
                return dbVersion;
            } else if (dbVersion > FxSharedUtils.getDBVersion()) {
                //the database is more current than the EAR!
                LOG.warn("This [fleXive] build is intended for database schema version #" + FxSharedUtils.getDBVersion() + " but the database reports a higher schema version! (database schema version: " + dbVersion + ")");
                return dbVersion;
            }
            //lets see if we have a patch we can apply
            try {
                final DBVendor dbVendor = FxContext.get().getDivisionData().getDbVendor();
                final String dir = "fxresources/sql/" + dbVendor + "/";
                String idxFile = dir + "resourceindex.flexive";
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                final InputStream scriptIndex = cl.getResourceAsStream(idxFile);
                if (scriptIndex == null) {
                    LOG.info("No patches available for " + dbVendor);
                    return dbVersion;
                }
                String[] files = FxSharedUtils.loadFromInputStream(scriptIndex, -1).replaceAll("\r", "").split("\n");
                Connection con = null;
                Statement stmt = null;
                try {
                    con = Database.getNonTXDataSource().getConnection();
                    stmt = con.createStatement();
                    List<SQLPatchScript> scripts = new ArrayList<SQLPatchScript>(50);
                    for (String file : files) {
                        String[] f = file.split("\\|");
                        int size = Integer.valueOf(f[1]);
                        String[] data = f[0].split("_");
                        if (data.length != 3) {
                            LOG.warn("Expected " + f[0] + " to have format xxx_yyy_zzz.sql");
                            continue;
                        }
                        if (!"patch".equals(data[0])) {
                            LOG.info("Expected a patch file, but got: " + data[0]);
                            continue;
                        }
                        if (!data[2].endsWith(".sql")) {
                            LOG.info("Expected an sql file, but got: " + data[2]);
                            continue;
                        }
                        long fromVersion = Long.parseLong(data[1]);
                        long toVersion = Long.parseLong(data[2].substring(0, data[2].indexOf('.')));
                        String code = FxSharedUtils.loadFromInputStream(cl.getResourceAsStream(dir + f[0]), size);
                        scripts.add(new SQLPatchScript(fromVersion, toVersion, code));
                    }
//                LOG.info("Patch available from version " + fromVersion + " to " + toVersion);
//                    stmt.executeUpdate(code);
                    boolean patching = true;
                    long maxVersion = currentVersion;
                    while (patching) {
                        patching = false;
                        for (SQLPatchScript ps : scripts) {
                            if (ps.from == currentVersion) {
                                LOG.info("Patching database schema from version [" + ps.from + "] to [" + ps.to + "] ... ");
                                new SQLScriptExecutor(ps.script, stmt).execute();
                                currentVersion = ps.to;
                                patching = true;
                                if (ps.to > maxVersion)
                                    ps.to = maxVersion;
                                break;
                            }
                        }
                    }
                    if (currentVersion < maxVersion) {
                        LOG.warn("Failed to patch to maximum available database schema version (" + maxVersion + "). Current database schema version: " + currentVersion);
                    }
                    return currentVersion;
                } finally {
                    Database.closeObjects(DivisionConfigurationEngineBean.class, con, stmt);
                }
            } catch (IOException e) {
                LOG.fatal(e);
                return currentVersion;
            } catch (SQLException e) {
                LOG.fatal(e);
                return currentVersion;
            }
        } finally {
            FxContext.get().stopRunAsSystem();
        }
    }

    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    private void modifyDatabaseVersion(long currentVersion) throws FxApplicationException {
        FxContext.get().runAsSystem();
        try {
            put(SystemParameters.DB_VERSION, currentVersion);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
    }

    /**
     * Helper class to execute an SQL script which contains of multiple statements and comments
     */
    static class SQLScriptExecutor {

        /**
         * Statement delimiter
         */
        public final static char DELIMITER = ';';

        private Statement stmt;
        private String script;

        /**
         * Ctor
         *
         * @param script the script to parse and execute
         * @param stat   an open and valid statements
         * @throws SQLException on errors
         * @throws IOException  on errors
         */
        public SQLScriptExecutor(String script, Statement stat) throws SQLException, IOException {
            this.stmt = stat;
            this.script = script;
            parse();
        }

        /**
         * Parse the script
         *
         * @throws IOException  on errors
         * @throws SQLException on errors
         */
        protected void parse() throws IOException, SQLException {
            BufferedReader reader = new BufferedReader(new StringReader(script));
            String currLine;
            StringBuilder sb = new StringBuilder(500);
            boolean eos;

            while ((currLine = reader.readLine()) != null) {
                if (isComment(currLine))
                    continue;
                eos = currLine.indexOf(DELIMITER) != -1;
                sb.append(currLine);
                if (eos) {
                    stmt.addBatch(sb.toString());
                    sb.setLength(0);
                }
            }
        }

        /**
         * Is the passed line a comment?
         * Only single line comments starting with "#" or "--" are supported!
         *
         * @param line line to examine
         * @return is comment
         */
        private boolean isComment(String line) {
            return (line != null) && (line.length() > 0) && (line.trim().charAt(0) == '#' || line.trim().startsWith("--"));
        }

        /**
         * Execute the script
         *
         * @throws SQLException on errors
         */
        public void execute() throws SQLException {
            stmt.executeBatch();
        }

    }

    /**
     * {@inheritDoc}
     */
    public String getDatabaseInfo() {
        final DivisionData divisionData = FxContext.get().getDivisionData();
        return "Division #" + divisionData.getId() + " - " + divisionData.getDbVendor().name() + " " + divisionData.getDbVersion();
    }
}
