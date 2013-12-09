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
package com.flexive.core.storage;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.flatstorage.FxFlatStorageInfo;
import com.flexive.core.flatstorage.FxFlatStorageManager;
import com.flexive.core.storage.binary.FxBinaryUtils;
import com.flexive.shared.*;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.impex.FxImportExportConstants;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.sql.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Division Exporter
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GenericDivisionExporter implements FxImportExportConstants {

/*
Groovy test script:

... standard imports ...
import com.flexive.core.*
import com.flexive.core.storage.*

File zip = new File("export.zip");
FileOutputStream fos = new FileOutputStream(zip)
def con = Database.dbConnection
StorageManager.storageImpl.exportDivision(con, fos)
fos.close()
con.close()
*/

    private static final Log LOG = LogFactory.getLog(GenericDivisionExporter.class);

    private static GenericDivisionExporter INSTANCE = new GenericDivisionExporter();

    /**
     * Getter for the exporter singleton
     *
     * @return exporter
     */
    public static GenericDivisionExporter getInstance() {
        return INSTANCE;
    }

    /**
     * Escape data for xml usage and append it to the string builder
     *
     * @param sb   the string builder to append data to
     * @param data string to escape and append
     */
    private void escape(StringBuilder sb, String data) {
        sb.append(StringEscapeUtils.escapeXml(data));
    }

    /**
     * Write the contents of a string builder in correct encoding to the given output stream
     *
     * @param out output stream to write to
     * @param sb  string builder containing data to be written
     * @throws IOException on errors
     */
    private void write(OutputStream out, StringBuilder sb) throws IOException {
        out.write(sb.toString().getBytes("UTF-8"));
        sb.setLength(0);
    }

    /**
     * Write an XML header to the given output stream
     *
     * @param out output stream
     * @throws IOException on errors
     */
    private void writeHeader(OutputStream out) throws IOException {
        out.write(FxXMLUtils.XML_HEADER.getBytes("UTF-8"));
    }

    /**
     * Dump all files (including subdirectories) to a zip archive
     *
     * @param zip     zip archive output stream to use
     * @param baseDir base directory
     * @throws IOException on errors
     */
    private void dumpFilesystem(ZipOutputStream zip, String baseDir) throws IOException {
        File base = new File(baseDir);
        if (!base.exists() || !base.isDirectory())
            return; //nothing to do
        dumpFile(zip, base, base.getAbsolutePath());
    }

    /**
     * Dump a single file to a zip output stream
     *
     * @param zip  zip output stream
     * @param file the file to dump
     * @param path absolute base directory path (will be stripped in the archive from file)
     * @throws IOException on errors
     */
    private void dumpFile(ZipOutputStream zip, File file, String path) throws IOException {
        if (file.isDirectory()) {
            for (File f : file.listFiles())
                dumpFile(zip, f, path);
            return;
        }
        ZipEntry ze = new ZipEntry(FOLDER_FS_BINARY + file.getAbsolutePath().substring(path.length()));
        zip.putNextEntry(ze);
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = fis.read(buffer)) != -1)
                zip.write(buffer, 0, read);
        } finally {
            if (fis != null)
                fis.close();
        }
        zip.closeEntry();
        zip.flush();
    }

    /**
     * Dump a generic table to XML
     *
     * @param tableName name of the table
     * @param stmt      an open statement
     * @param out       output stream
     * @param sb        an available and valid StringBuilder
     * @param xmlTag    name of the xml tag to write per row
     * @param idColumn  (optional) id column to sort results
     * @throws SQLException on errors
     * @throws IOException  on errors
     */
    private void dumpTable(String tableName, Statement stmt, OutputStream out, StringBuilder sb, String xmlTag, String idColumn) throws SQLException, IOException {
        dumpTable(tableName, stmt, out, sb, xmlTag, idColumn, false);
    }

    /**
     * Dump a generic table to XML
     *
     * @param tableName     name of the table
     * @param stmt          an open statement
     * @param out           output stream
     * @param sb            an available and valid StringBuilder
     * @param xmlTag        name of the xml tag to write per row
     * @param idColumn      (optional) id column to sort results
     * @param onlyBinaries  process binary fields (else these will be ignored)
     * @throws SQLException on errors
     * @throws IOException  on errors
     */
    private void dumpTable(String tableName, Statement stmt, OutputStream out, StringBuilder sb, String xmlTag, String idColumn, boolean onlyBinaries) throws SQLException, IOException {
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName +
                (StringUtils.isEmpty(idColumn) ? "" : " ORDER BY " + idColumn + " ASC"));
        final ResultSetMetaData md = rs.getMetaData();
        String value, att;
        boolean hasSubTags;
        while (rs.next()) {
            hasSubTags = false;
            if (!onlyBinaries) {
                sb.setLength(0);
                sb.append("  <").append(xmlTag);
            }
            for (int i = 1; i <= md.getColumnCount(); i++) {
                value = null;
                att = md.getColumnName(i).toLowerCase();
                switch (md.getColumnType(i)) {
                    case java.sql.Types.DECIMAL:
                    case java.sql.Types.NUMERIC:
                    case java.sql.Types.BIGINT:
                        if (!onlyBinaries) {
                            value = String.valueOf(rs.getBigDecimal(i));
                            if (rs.wasNull())
                                value = null;
                        }
                        break;
                    case java.sql.Types.INTEGER:
                    case java.sql.Types.SMALLINT:
                    case java.sql.Types.TINYINT:
                        if (!onlyBinaries) {
                            value = String.valueOf(rs.getLong(i));
                            if (rs.wasNull())
                                value = null;
                        }
                        break;
                    case java.sql.Types.DOUBLE:
                    case java.sql.Types.FLOAT:
                    case java.sql.Types.REAL:
                        if (!onlyBinaries) {
                            value = String.valueOf(rs.getDouble(i));
                            if (rs.wasNull())
                                value = null;
                        }
                        break;
                    case java.sql.Types.TIMESTAMP:
                    case java.sql.Types.DATE:
                        if (!onlyBinaries) {
                            final Timestamp ts = rs.getTimestamp(i);
                            if (rs.wasNull())
                                value = null;
                            else
                                value = FxFormatUtils.getDateTimeFormat().format(ts);
                        }
                        break;
                    case java.sql.Types.BIT:
                    case java.sql.Types.CHAR:
                    case java.sql.Types.BOOLEAN:
                        if (!onlyBinaries) {
                            value = rs.getBoolean(i) ? "1" : "0";
                            if (rs.wasNull())
                                value = null;
                        }
                        break;
                    case java.sql.Types.CLOB:
                    case java.sql.Types.BLOB:
                    case java.sql.Types.LONGVARBINARY:
                    case java.sql.Types.LONGVARCHAR:
                    case java.sql.Types.VARBINARY:
                    case java.sql.Types.VARCHAR:
                    case java.sql.Types.BINARY:
                    case SQL_LONGNVARCHAR:
                    case SQL_NCHAR:
                    case SQL_NCLOB:
                    case SQL_NVARCHAR:

                        hasSubTags = true;
                        break;
                    default:
                        LOG.warn("Unhandled type [" + md.getColumnType(i) + "] for [" + tableName + "." + att + "]");
                }
                if (value != null && !onlyBinaries)
                    sb.append(' ').append(att).append("=\"").append(value).append("\"");
            }
            if (hasSubTags) {
                if (!onlyBinaries)
                    sb.append(">\n");
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    switch (md.getColumnType(i)) {
                        case java.sql.Types.VARBINARY:
                        case java.sql.Types.LONGVARBINARY:
                        case java.sql.Types.BLOB:
                        case java.sql.Types.BINARY:
                            if (idColumn == null)
                                throw new IllegalArgumentException("Id column required to process binaries!");
                            String binFile = FOLDER_BINARY + "/BIN_" + String.valueOf(rs.getLong(idColumn)) + "_" + i + ".blob";
                            att = md.getColumnName(i).toLowerCase();
                            if (onlyBinaries) {
                                if (!(out instanceof ZipOutputStream))
                                    throw new IllegalArgumentException("out has to be a ZipOutputStream to store binaries!");
                                ZipOutputStream zip = (ZipOutputStream) out;
                                InputStream in = rs.getBinaryStream(i);
                                if (rs.wasNull())
                                    break;

                                ZipEntry ze = new ZipEntry(binFile);
                                zip.putNextEntry(ze);

                                byte[] buffer = new byte[4096];
                                int read;
                                while ((read = in.read(buffer)) != -1)
                                    zip.write(buffer, 0, read);
                                in.close();
                                zip.closeEntry();
                                zip.flush();
                            } else {
                                InputStream in = rs.getBinaryStream(i); //need to fetch to see if it is empty
                                if (rs.wasNull())
                                    break;
                                in.close();
                                sb.append("    <").append(att).append(">").append(binFile).append("</").append(att).append(">\n");
                            }
                            break;
                        case java.sql.Types.CLOB:
                        case SQL_LONGNVARCHAR:
                        case SQL_NCHAR:
                        case SQL_NCLOB:
                        case SQL_NVARCHAR:
                        case java.sql.Types.LONGVARCHAR:
                        case java.sql.Types.VARCHAR:
                            if (!onlyBinaries) {
                                value = rs.getString(i);
                                if (rs.wasNull())
                                    break;
                                att = md.getColumnName(i).toLowerCase();
                                sb.append("    <").append(att).append('>');
                                escape(sb, value);
                                sb.append("</").append(att).append(">\n");
                            }
                            break;
                    }
                }
                if (!onlyBinaries)
                    sb.append("  </").append(xmlTag).append(">\n");
            } else {
                if (!onlyBinaries)
                    sb.append("/>\n");
            }
            if (!onlyBinaries)
                write(out, sb);
        }
    }

    /**
     * Start a new zip entry/file
     *
     * @param out   zip output stream to use
     * @param entry name of the new entry
     * @throws IOException on errors
     */
    private void startEntry(ZipOutputStream out, String entry) throws IOException {
        ZipEntry ze = new ZipEntry(entry);
        out.putNextEntry(ze);
        writeHeader(out);
    }

    /**
     * Signal the end of a zip entry and flush the stream
     *
     * @param out zip output stream
     * @throws IOException on errors
     */
    private void endEntry(ZipOutputStream out) throws IOException {
        out.closeEntry();
        out.flush();
    }

    /**
     * Export all language data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportLanguages(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_LANGUAGES);
            sb.setLength(0);
            sb.append("<languages>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_LANG, stmt, out, sb, "lang", "LANG_CODE");
            dumpTable(DatabaseConst.TBL_LANG + DatabaseConst.ML, stmt, out, sb, "lang_t", "LANG_CODE");
            sb.append("</languages>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all mandator data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportMandators(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_MANDATORS);
            sb.setLength(0);
            sb.append("<mandators>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_MANDATORS, stmt, out, sb, "mandator", "ID");
            sb.append("</mandators>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all security data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportSecurity(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_SECURITY);
            sb.setLength(0);
            sb.append("<security>\n");
            sb.append("<accounts>\n");
            sb.append("<data>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_ACCOUNTS, stmt, out, sb, "account", "ID");
            sb.append("</data>\n");
            write(out, sb);
            sb.append("<details>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_ACCOUNT_DETAILS, stmt, out, sb, "detail", "ID");
            sb.append("</details>\n");
            sb.append("</accounts>\n");
            sb.append("<acls>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_ACLS, stmt, out, sb, "acl", "ID");
            dumpTable(DatabaseConst.TBL_ACLS + DatabaseConst.ML, stmt, out, sb, "acl_t", "ID");
            sb.append("</acls>\n");
            sb.append("<assignments>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_ACLS_ASSIGNMENT, stmt, out, sb, "assignment", null);
            sb.append("</assignments>\n");
            sb.append("<groups>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_USERGROUPS, stmt, out, sb, "group", "ID");
            sb.append("</groups>\n");
            sb.append("<groupAssignments>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_ASSIGN_GROUPS, stmt, out, sb, "assignment", null);
            sb.append("</groupAssignments>\n");
            sb.append("<roleAssignments>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_ROLE_MAPPING, stmt, out, sb, "assignment", null);
            sb.append("</roleAssignments>\n");
            sb.append("</security>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all workflow data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportWorkflows(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_WORKFLOWS);
            sb.setLength(0);
            sb.append("<workflow>\n");
            sb.append("<workflows>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_WORKFLOW, stmt, out, sb, "workflow", "ID");
            sb.append("</workflows>\n");
            sb.append("<stepDefinitions>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_WORKFLOW_STEPDEFINITION, stmt, out, sb, "stepdef", null);
            dumpTable(DatabaseConst.TBL_WORKFLOW_STEPDEFINITION + DatabaseConst.ML, stmt, out, sb, "stepdef_t", null);
            sb.append("</stepDefinitions>\n");
            sb.append("<steps>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_WORKFLOW_STEP, stmt, out, sb, "step", null);
            sb.append("</steps>\n");
            sb.append("<routes>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_WORKFLOW_ROUTES, stmt, out, sb, "route", null);
            sb.append("</routes>\n");
            sb.append("</workflow>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all configuration data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportConfigurations(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_CONFIGURATIONS);
            sb.setLength(0);
            sb.append("<configurations>\n");
            sb.append("<application>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_CONFIG_APPLICATION, stmt, out, sb, "entry", null);
            sb.append("</application>\n");
            sb.append("<division>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_CONFIG_DIVISION, stmt, out, sb, "entry", null);
            sb.append("</division>\n");
            sb.append("<node>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_CONFIG_NODE, stmt, out, sb, "entry", null);
            sb.append("</node>\n");
            sb.append("<user>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_CONFIG_USER, stmt, out, sb, "entry", null);
            sb.append("</user>\n");
            sb.append("</configurations>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all structure data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportStructures(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_STRUCTURES);
            sb.setLength(0);
            sb.append("<structures>\n");
            sb.append("<datatypes>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_DATATYPES, stmt, out, sb, "type", null);
            dumpTable(DatabaseConst.TBL_STRUCT_DATATYPES + DatabaseConst.ML, stmt, out, sb, "type_t", null);
            sb.append("</datatypes>\n");
            sb.append("<types>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_TYPES, stmt, out, sb, "tdef", null);
            dumpTable(DatabaseConst.TBL_STRUCT_TYPES + DatabaseConst.ML, stmt, out, sb, "tdef_t", null);
            dumpTable(DatabaseConst.TBL_STRUCT_TYPES_OPTIONS, stmt, out, sb, "topts", null);
            dumpTable(DatabaseConst.TBL_STRUCT_TYPERELATIONS, stmt, out, sb, "trel", null);
            sb.append("</types>\n");
            sb.append("<properties>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_PROPERTIES, stmt, out, sb, "property", null);
            dumpTable(DatabaseConst.TBL_STRUCT_PROPERTIES + DatabaseConst.ML, stmt, out, sb, "property_t", null);
            dumpTable(DatabaseConst.TBL_STRUCT_PROPERTY_OPTIONS, stmt, out, sb, "poption", null);
            sb.append("</properties>\n");
            sb.append("<groups>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_GROUPS, stmt, out, sb, "group", null);
            dumpTable(DatabaseConst.TBL_STRUCT_GROUPS + DatabaseConst.ML, stmt, out, sb, "group_t", null);
            dumpTable(DatabaseConst.TBL_STRUCT_GROUP_OPTIONS, stmt, out, sb, "goption", null);
            sb.append("</groups>\n");
            sb.append("<assignments>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_ASSIGNMENTS, stmt, out, sb, "assignment", null);
            dumpTable(DatabaseConst.TBL_STRUCT_ASSIGNMENTS + DatabaseConst.ML, stmt, out, sb, "assignment_t", null);
            sb.append("</assignments>\n");
            sb.append("<selectlists>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_SELECTLIST, stmt, out, sb, "list", null);
            dumpTable(DatabaseConst.TBL_STRUCT_SELECTLIST + DatabaseConst.ML, stmt, out, sb, "list_t", null);
            dumpTable(DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM, stmt, out, sb, "item", null);
            dumpTable(DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM + DatabaseConst.ML, stmt, out, sb, "item_t", null);
            sb.append("</selectlists>\n");
            sb.append("</structures>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all tree data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportTree(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_TREE);
            sb.setLength(0);
            sb.append("<tree>\n");
            sb.append("<edit>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_TREE, stmt, out, sb, "node", null);
            sb.append("</edit>\n");
            sb.append("<live>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_TREE + "_LIVE", stmt, out, sb, "node", null);
            sb.append("</live>\n");
            sb.append("</tree>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all briefcase data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportBriefcases(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_BRIEFCASES);
            sb.setLength(0);
            sb.append("<briefcases>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_BRIEFCASE, stmt, out, sb, "briefcase", null);
            dumpTable(DatabaseConst.TBL_BRIEFCASE_DATA, stmt, out, sb, "data", null);
            dumpTable(DatabaseConst.TBL_BRIEFCASE_DATA_ITEM, stmt, out, sb, "dataItem", null);
            sb.append("</briefcases>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all scripting data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportScripts(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_SCRIPTS);
            sb.setLength(0);
            sb.append("<scripts>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_SCRIPTS, stmt, out, sb, "script", null);
            dumpTable(DatabaseConst.TBL_SCRIPT_MAPPING_ASSIGN, stmt, out, sb, "assignmap", null);
            dumpTable(DatabaseConst.TBL_SCRIPT_MAPPING_TYPES, stmt, out, sb, "typemap", null);
            sb.append("</scripts>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all history data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportHistory(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_HISTORY);
            sb.setLength(0);
            sb.append("<history>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_HISTORY, stmt, out, sb, "entry", null);
            sb.append("</history>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all resource data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportResources(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_RESOURCES);
            sb.setLength(0);
            sb.append("<resources>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_RESOURCES, stmt, out, sb, "entry", null);
            dumpTable(DatabaseConst.TBL_PHRASE, stmt, out, sb, "phrase", null);
            dumpTable(DatabaseConst.TBL_PHRASE_VALUES, stmt, out, sb, "phraseVal", null);
            dumpTable(DatabaseConst.TBL_PHRASE_TREE, stmt, out, sb, "phraseTree", null);
            dumpTable(DatabaseConst.TBL_PHRASE_MAP, stmt, out, sb, "phraseMap", null);
            sb.append("</resources>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export flatstorage meta information
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportFlatStorageMeta(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_FLATSTORAGE_META);
            sb.setLength(0);
            sb.append("<flatstorageMeta>\n");
            for (FxFlatStorageInfo info : FxFlatStorageManager.getInstance().getFlatStorageInfos()) {
                sb.append("  <storageMeta bigInt=\"").append(info.getColumnsBigInt()).
                        append("\" double=\"").append(info.getColumnsDouble()).
                        append("\" select=\"").append(info.getColumnsSelect()).
                        append("\" string=\"").append(info.getColumnsString()).
                        append("\" text=\"").append(info.getColumnsText()).
                        append("\">\n");
                sb.append("    <name>");
                escape(sb, info.getName());
                sb.append("</name>\n");
                sb.append("    <description>");
                escape(sb, info.getDescription());
                sb.append("</description>\n");
                sb.append("  </storageMeta>\n");
            }
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_FLATSTORE_INFO, stmt, out, sb, "storage", null);
            dumpTable(DatabaseConst.TBL_STRUCT_FLATSTORE_MAPPING, stmt, out, sb, "mapping", null);
            sb.append("</flatstorageMeta>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all binaries
     *
     * @param con an open and valid connection to read the data from
     * @param zip destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportBinaries(Connection con, ZipOutputStream zip, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            //binary table, transit is ignored on purpose!
            startEntry(zip, FILE_BINARIES);
            sb.setLength(0);
            sb.append("<binaries>\n");
            write(zip, sb);
            //dump meta data
            dumpTable(DatabaseConst.TBL_CONTENT_BINARY, stmt, zip, sb, "binary", "id", false);
            sb.append("</binaries>\n");
            write(zip, sb);
            endEntry(zip);
            //dump the binaries
            dumpTable(DatabaseConst.TBL_CONTENT_BINARY, stmt, zip, sb, "binary", "id", true);
            //filesystem binaries
            final String baseDir = FxBinaryUtils.getBinaryDirectory() + File.separatorChar + String.valueOf(FxContext.get().getDivisionId());
            dumpFilesystem(zip, baseDir);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all hierarchical storage data
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportHierarchicalStorage(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_DATA_HIERARCHICAL);
            sb.setLength(0);
            sb.append("<hierarchical>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_CONTENT, stmt, out, sb, "content", "id");
            dumpTable(DatabaseConst.TBL_CONTENT_DATA, stmt, out, sb, "data", "id");
//            dumpTable(DatabaseConst.TBL_CONTENT_DATA_FT, stmt, out, sb, "ft", "id");
            dumpTable(DatabaseConst.TBL_CONTENT_ACLS, stmt, out, sb, "acl", "id");
            sb.append("</hierarchical>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all flat storages
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportFlatStorages(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_DATA_FLAT);
            sb.setLength(0);
            sb.append("<flatstorages>\n");
            write(out, sb);
            for (FxFlatStorageInfo info : FxFlatStorageManager.getInstance().getFlatStorageInfos()) {
                sb.append("<storage name=\"").append(info.getName()).append("\">\n");
                write(out, sb);
                dumpTable(info.getName(), stmt, out, sb, "data", "id");
                sb.append("</storage>\n");
            }
            sb.append("</flatstorages>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export all sequencer settings
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportSequencers(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_SEQUENCERS);
            sb.setLength(0);
            sb.append("<sequencers>\n");
            write(out, sb);
            final SequencerStorage ss = StorageManager.getSequencerStorage();
            for (FxSystemSequencer s : FxSystemSequencer.values()) {
                sb.append("  <syssequence>\n");
                sb.append("    <name>").append(StringEscapeUtils.escapeXml(s.name())).append("</name>\n");
                sb.append("    <value>").append(ss.getCurrentId(s)).append("</value>\n");
                sb.append("    <rollover>").append(s.isAllowRollover() ? 1 : 0).append("</rollover>\n");
                sb.append("  </syssequence>\n");
            }

            for (CustomSequencer s : ss.getCustomSequencers()) {
                sb.append("  <usrsequence>\n");
                sb.append("    <name>").append(StringEscapeUtils.escapeXml(s.getName())).append("</name>\n");
                sb.append("    <value>").append(s.getCurrentNumber()).append("</value>\n");
                sb.append("    <rollover>").append(s.isAllowRollover() ? 1 : 0).append("</rollover>\n");
                sb.append("  </usrsequence>\n");
            }
            sb.append("</sequencers>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    /**
     * Export information about the exported division
     *
     * @param con an open and valid connection to read the data from
     * @param out destination zip output stream
     * @param sb  string builder to reuse
     * @throws Exception on errors
     */
    public void exportBuildInfos(Connection con, ZipOutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            startEntry(out, FILE_BUILD_INFOS);
            sb.setLength(0);
            sb.append("<flexive>\n");
            sb.append("  <division>").append(FxContext.get().getDivisionId()).append("</division>\n");
            sb.append("  <schema>").append(FxSharedUtils.getDBVersion()).append("</schema>\n");
            sb.append("  <build>").append(FxSharedUtils.getBuildNumber()).append("</build>\n");
            sb.append("  <verbose>").append(StringEscapeUtils.escapeXml(
                    FxSharedUtils.getFlexiveEditionFull() + " " +
                            FxSharedUtils.getFlexiveVersion() + "/build #" + FxSharedUtils.getBuildNumber() + " - " +
                            FxSharedUtils.getBuildDate())).append("</verbose>\n");
            sb.append("  <appserver>").append(StringEscapeUtils.escapeXml(FxSharedUtils.getApplicationServerName())).
                    append("</appserver>\n");
            final DivisionData divisionData = FxContext.get().getDivisionData();
            sb.append("  <database>").
                    append(StringEscapeUtils.escapeXml(divisionData.getDbVendor() + " - " + divisionData.getDbVersion())).
                    append("</database>\n");
            sb.append("  <dbdriver>").append(StringEscapeUtils.escapeXml(divisionData.getDbDriverVersion())).append("</dbdriver>\n");
            sb.append("  <domain>").append(StringEscapeUtils.escapeXml(divisionData.getDomainRegEx())).append("</domain>\n");
            sb.append("  <drops>").append(StringEscapeUtils.escapeXml(ArrayUtils.toString(FxSharedUtils.getDrops()))).append("</drops>\n");
            sb.append("  <user>").append(FxContext.getUserTicket().getLoginName()).append("</user>\n");
            sb.append("  <date>").append(FxFormatUtils.getDateTimeFormat().format(new java.util.Date(System.currentTimeMillis()))).append("</date>\n");
            sb.append("</flexive>\n");
            write(out, sb);
            endEntry(out);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

}
