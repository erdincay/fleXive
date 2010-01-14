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
package com.flexive.core.storage;

import com.flexive.core.Database;
import com.flexive.core.DatabaseConst;
import com.flexive.core.flatstorage.FxFlatStorageInfo;
import com.flexive.core.flatstorage.FxFlatStorageManager;
import com.flexive.core.storage.binary.FxBinaryUtils;
import com.flexive.shared.*;
import com.flexive.shared.configuration.DivisionData;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.sql.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Division Exporter
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GenericDivisionExporter {

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

    public final static String FS_BINARY_FOLDER = "fsbinary";
    public final static String BINARY_FOLDER = "binary";
    public final static String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";


    private static GenericDivisionExporter INSTANCE = new GenericDivisionExporter();

    public static GenericDivisionExporter getInstance() {
        return INSTANCE;
    }

    private void escape(StringBuilder sb, String data) {
        sb.append(StringEscapeUtils.escapeXml(data));
    }

    private void write(OutputStream out, StringBuilder sb) throws IOException {
        out.write(sb.toString().getBytes("UTF-8"));
        sb.setLength(0);
    }

    private void writeHeader(OutputStream out) throws IOException {
        out.write(XML_HEADER.getBytes("UTF-8"));
    }

    private void dumpFilesystem(ZipOutputStream zip, String baseDir) throws IOException {
        File base = new File(baseDir);
        if (!base.exists() || !base.isDirectory())
            return; //nothing to do
        dumpFile(zip, base, base.getAbsolutePath());
    }

    private void dumpFile(ZipOutputStream zip, File file, String path) throws IOException {
        if (file.isDirectory()) {
            for (File f : file.listFiles())
                dumpFile(zip, f, path);
            return;
        }
        ZipEntry ze = new ZipEntry(FS_BINARY_FOLDER + file.getAbsolutePath().substring(path.length()));
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
        dumpTable(tableName, stmt, out, sb, xmlTag, idColumn, true);
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
     * @param writeToStream write StringBuilder content to the stream
     * @throws SQLException on errors
     * @throws IOException  on errors
     */
    private void dumpTable(String tableName, Statement stmt, OutputStream out, StringBuilder sb, String xmlTag, String idColumn, boolean writeToStream) throws SQLException, IOException {
        ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName +
                (StringUtils.isEmpty(idColumn) ? "" : " ORDER BY " + idColumn + " ASC"));
        final ResultSetMetaData md = rs.getMetaData();
        String value, att;
        boolean hasSubTags;
        while (rs.next()) {
            hasSubTags = false;
            if (writeToStream)
                sb.setLength(0);
            sb.append("  <").append(xmlTag);
            for (int i = 1; i <= md.getColumnCount(); i++) {
                value = null;
                att = md.getColumnName(i).toLowerCase();
                switch (md.getColumnType(i)) {
                    case java.sql.Types.BIGINT:
                        value = String.valueOf(rs.getBigDecimal(i));
                        if (rs.wasNull())
                            value = null;
                        break;
                    case java.sql.Types.INTEGER:
                    case java.sql.Types.DECIMAL:
                    case java.sql.Types.SMALLINT:
                    case java.sql.Types.NUMERIC:
                        value = String.valueOf(rs.getLong(i));
                        if (rs.wasNull())
                            value = null;
                        break;
                    case java.sql.Types.BIT:
                    case java.sql.Types.CHAR:
                    case java.sql.Types.BOOLEAN:
                        value = rs.getBoolean(i) ? "1" : "0";
                        if (rs.wasNull())
                            value = null;
                        break;
                    case java.sql.Types.CLOB:
                    case java.sql.Types.BLOB:
                    case java.sql.Types.LONGNVARCHAR:
                    case java.sql.Types.LONGVARBINARY:
                    case java.sql.Types.LONGVARCHAR:
                    case java.sql.Types.NCHAR:
                    case java.sql.Types.NCLOB:
                    case java.sql.Types.NVARCHAR:
                    case java.sql.Types.VARBINARY:
                    case java.sql.Types.VARCHAR:
                        hasSubTags = true;
                        break;
                }
                if (value != null)
                    sb.append(' ').append(att).append("=\"").append(value).append("\"");
            }
            if (hasSubTags) {
                sb.append(">\n");
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    switch (md.getColumnType(i)) {
                        case java.sql.Types.VARBINARY:
                        case java.sql.Types.LONGVARBINARY:
                        case java.sql.Types.BLOB:
//                            System.err.println("Can not handle blobs yet! (" + tableName + "." + md.getColumnName(i) + ")");
                            if (!(out instanceof ZipOutputStream))
                                throw new IllegalArgumentException("out has to be a ZipOutputStream to store binaries!");
                            ZipOutputStream zip = (ZipOutputStream) out;
                            InputStream in = rs.getBinaryStream(i);
                            if (rs.wasNull())
                                break;

                            att = md.getColumnName(i).toLowerCase();
                            String binFile = BINARY_FOLDER + "/BIN_" +
                                    (idColumn == null ? RandomStringUtils.randomAlphanumeric(8) : String.valueOf(rs.getLong(idColumn))) +
                                    "_" + RandomStringUtils.randomAlphanumeric(8) + ".blob";

                            ZipEntry ze = new ZipEntry(binFile);
                            zip.putNextEntry(ze);

                            byte[] buffer = new byte[4096];
                            int read;
                            while ((read = in.read(buffer)) != -1)
                                zip.write(buffer, 0, read);
                            in.close();
                            zip.closeEntry();
                            zip.flush();
                            sb.append("    <").append(att).append(">").append(binFile).append("</").append(att).append(">\n");
                            break;
                        case java.sql.Types.CLOB:
                        case java.sql.Types.LONGNVARCHAR:
                        case java.sql.Types.LONGVARCHAR:
                        case java.sql.Types.NCHAR:
                        case java.sql.Types.NCLOB:
                        case java.sql.Types.NVARCHAR:
                        case java.sql.Types.VARCHAR:
                            value = rs.getString(i);
                            if (rs.wasNull())
                                break;
                            att = md.getColumnName(i).toLowerCase();
                            sb.append("    <").append(att).append('>');
                            escape(sb, value);
                            sb.append("</").append(att).append(">\n");
                            break;
                    }
                }
                sb.append("  </").append(xmlTag).append(">\n");
            } else {
                sb.append("/>\n");
            }
            if (writeToStream)
                write(out, sb);
        }
    }

    public void exportLanguages(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            //                                       1          2         3      4
            ResultSet rs = stmt.executeQuery("SELECT LANG_CODE, ISO_CODE, INUSE, DISPPOS FROM " + DatabaseConst.TBL_LANG + " ORDER BY LANG_CODE ASC");
            int pos;
            writeHeader(out);
            sb.setLength(0);
            sb.append("<languages>\n");
            write(out, sb);
            while (rs != null && rs.next()) {
                sb.append("  <lang code=\"").append(rs.getLong(1)).append("\" iso=\"").append(rs.getString(2)).append("\"");
                if (rs.getBoolean(3))
                    sb.append(" inuse=\"1\"");
                pos = rs.getInt(4);
                if (!rs.wasNull())
                    sb.append(" pos=\"").append(pos).append("\"");
                sb.append("/>\n");
                write(out, sb);
            }
            //                             1          2     3
            rs = stmt.executeQuery("SELECT LANG_CODE, LANG, DESCRIPTION FROM " + DatabaseConst.TBL_LANG + DatabaseConst.ML + " ORDER BY LANG_CODE ASC");
            while (rs != null && rs.next()) {
                sb.append("  <lang_t code=\"").append(rs.getLong(1)).append("\" lang=\"").append(rs.getLong(2)).append("\">");
                escape(sb, rs.getString(3));
                sb.append("</lang_t>\n");
                write(out, sb);
            }
            sb.append("</languages>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportMandators(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            //                                       1   2     3         4          5           6           7            8
            ResultSet rs = stmt.executeQuery("SELECT ID, NAME, METADATA, IS_ACTIVE, CREATED_BY, CREATED_AT, MODIFIED_BY, MODIFIED_AT FROM " + DatabaseConst.TBL_MANDATORS + " ORDER BY ID ASC");
            long meta;
            writeHeader(out);
            sb.setLength(0);
            sb.append("<mandators>\n");
            write(out, sb);
            while (rs != null && rs.next()) {
                sb.append("  <mandator id=\"").append(rs.getLong(1)).append("\"");
                meta = rs.getLong(3);
                if (!rs.wasNull())
                    sb.append(" metadata=\"").append(meta).append("\"");
                sb.append(" active=\"").append(rs.getBoolean(4) ? "1" : "0").append("\"");
                sb.append(" created_by=\"").append(rs.getLong(5)).append("\"");
                sb.append(" created_at=\"").append(rs.getLong(6)).append("\"");
                sb.append(" modified_by=\"").append(rs.getLong(7)).append("\"");
                sb.append(" modified_at=\"").append(rs.getLong(8)).append("\"");
                sb.append(">");
                escape(sb, rs.getString(2));
                sb.append("</mandator>\n");
                write(out, sb);
            }
            sb.append("</mandators>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportSecurity(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
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
            dumpTable(DatabaseConst.TBL_ASSIGN_ACLS, stmt, out, sb, "assignment", null);
            sb.append("</assignments>\n");
            sb.append("<groups>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_GROUP, stmt, out, sb, "group", "ID");
            sb.append("</groups>\n");
            sb.append("<groupAssignments>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_ASSIGN_GROUPS, stmt, out, sb, "assignment", null);
            sb.append("</groupAssignments>\n");
            sb.append("<roleAssignments>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_ASSIGN_ROLES, stmt, out, sb, "assignment", null);
            sb.append("</roleAssignments>\n");
            sb.append("</security>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportWorkflows(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
            sb.setLength(0);
            sb.append("<workflow>\n");
            sb.append("<workflows>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_WORKFLOW, stmt, out, sb, "workflow", "ID");
            sb.append("</workflows>\n");
            sb.append("<stepDefinitions>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STEPDEFINITION, stmt, out, sb, "stepdef", null);
            dumpTable(DatabaseConst.TBL_STEPDEFINITION + DatabaseConst.ML, stmt, out, sb, "stepdef_t", null);
            sb.append("</stepDefinitions>\n");
            sb.append("<steps>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STEP, stmt, out, sb, "step", null);
            sb.append("</steps>\n");
            sb.append("<routes>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_ROUTES, stmt, out, sb, "route", null);
            sb.append("</routes>\n");
            sb.append("</workflow>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportConfigurations(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
            sb.setLength(0);
            sb.append("<configurations>\n");
            sb.append("<application>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_APPLICATION_CONFIG, stmt, out, sb, "entry", null);
            sb.append("</application>\n");
            sb.append("<division>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_DIVISION_CONFIG, stmt, out, sb, "entry", null);
            sb.append("</division>\n");
            sb.append("<node>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_NODE_CONFIG, stmt, out, sb, "entry", null);
            sb.append("</node>\n");
            sb.append("<user>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_USER_CONFIG, stmt, out, sb, "entry", null);
            sb.append("</user>\n");
            sb.append("</configurations>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportStructures(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
            sb.setLength(0);
            sb.append("<structures>\n");
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
            dumpTable(DatabaseConst.TBL_PROPERTY_OPTIONS, stmt, out, sb, "poption", null);
            sb.append("</properties>\n");
            sb.append("<groups>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_GROUPS, stmt, out, sb, "group", null);
            dumpTable(DatabaseConst.TBL_STRUCT_GROUPS + DatabaseConst.ML, stmt, out, sb, "group_t", null);
            dumpTable(DatabaseConst.TBL_GROUP_OPTIONS, stmt, out, sb, "goption", null);
            sb.append("</groups>\n");
            sb.append("<assignments>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_ASSIGNMENTS, stmt, out, sb, "assignment", null);
            dumpTable(DatabaseConst.TBL_STRUCT_ASSIGNMENTS + DatabaseConst.ML, stmt, out, sb, "assignment_t", null);
            sb.append("</assignments>\n");
            sb.append("<selectlists>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_SELECTLIST, stmt, out, sb, "list", null);
            dumpTable(DatabaseConst.TBL_SELECTLIST + DatabaseConst.ML, stmt, out, sb, "list_t", null);
            dumpTable(DatabaseConst.TBL_SELECTLIST_ITEM, stmt, out, sb, "item", null);
            dumpTable(DatabaseConst.TBL_SELECTLIST_ITEM + DatabaseConst.ML, stmt, out, sb, "item_t", null);
            sb.append("</selectlists>\n");
            sb.append("</structures>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportTree(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
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
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportBriefcases(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
            sb.setLength(0);
            sb.append("<briefcases>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_BRIEFCASE, stmt, out, sb, "briefcase", null);
            dumpTable(DatabaseConst.TBL_BRIEFCASE_DATA, stmt, out, sb, "data", null);
            sb.append("</briefcases>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportScripts(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
            sb.setLength(0);
            sb.append("<scripts>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_SCRIPTS, stmt, out, sb, "script", null);
            dumpTable(DatabaseConst.TBL_SCRIPT_MAPPING_ASSIGN, stmt, out, sb, "assignmap", null);
            dumpTable(DatabaseConst.TBL_SCRIPT_MAPPING_TYPES, stmt, out, sb, "typemap", null);
            sb.append("</scripts>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportHistory(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
            sb.setLength(0);
            sb.append("<history>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_HISTORY, stmt, out, sb, "entry", null);
            sb.append("</history>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportFlatStorageMeta(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
            sb.setLength(0);
            sb.append("<flatstorageMeta>\n");
            for (FxFlatStorageInfo info : FxFlatStorageManager.getInstance().getFlatStorageInfos()) {
                sb.append("  <storage bigInt=\"").append(info.getColumnsBigInt()).
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
                sb.append("</desription>\n");
                sb.append("  </storage>\n");
            }
            write(out, sb);
            dumpTable(DatabaseConst.TBL_STRUCT_FLATSTORE_INFO, stmt, out, sb, "storage", null);
            dumpTable(DatabaseConst.TBL_STRUCT_FLATSTORE_MAPPING, stmt, out, sb, "mapping", null);
            sb.append("</flatstorageMeta>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportBinaries(String xmlFile, Connection con, ZipOutputStream zip, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            //binary table, transit is ignored on purpose!
            sb.setLength(0);
            sb.append("<binaries>\n");
            dumpTable(DatabaseConst.TBL_CONTENT_BINARY, stmt, zip, sb, "binary", "id", false);
            ZipEntry ze = new ZipEntry(xmlFile);
            zip.putNextEntry(ze);
            sb.append("</binaries>\n");
            writeHeader(zip);
            write(zip, sb);
            zip.closeEntry();
            zip.flush();
            //filesystem binaries
            final String baseDir = FxBinaryUtils.getBinaryDirectory() + File.separatorChar + String.valueOf(FxContext.get().getDivisionId());
            dumpFilesystem(zip, baseDir);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportHierarchicalStorage(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
            sb.setLength(0);
            sb.append("<hierarchical>\n");
            write(out, sb);
            dumpTable(DatabaseConst.TBL_CONTENT, stmt, out, sb, "content", "id");
            dumpTable(DatabaseConst.TBL_CONTENT_DATA, stmt, out, sb, "data", "id");
            dumpTable(DatabaseConst.TBL_CONTENT_DATA_FT, stmt, out, sb, "ft", "id");
            dumpTable(DatabaseConst.TBL_CONTENT_ACLS, stmt, out, sb, "acl", "id");
            sb.append("</hierarchical>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportFlatStorages(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
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
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportSequencers(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
            sb.setLength(0);
            sb.append("<sequencers>\n");
            write(out, sb);
            final SequencerStorage ss = StorageManager.getSequencerStorage();
            for (FxSystemSequencer s : FxSystemSequencer.values()) {
                sb.append("  <syssequence>\n");
                sb.append("    <name>").append(StringEscapeUtils.escapeXml(s.name())).append("</name>\n");
                sb.append("    <value>").append(ss.getCurrentId(s)).append("</name>\n");
                sb.append("    <rollover>").append(s.isAllowRollover() ? 1 : 0).append("</rollover>\n");
                sb.append("  </syssequence>\n");
            }

            for (CustomSequencer s : ss.getCustomSequencers()) {
                sb.append("  <usrsequence>\n");
                sb.append("    <name>").append(StringEscapeUtils.escapeXml(s.getName())).append("</name>\n");
                sb.append("    <value>").append(s.getCurrentNumber()).append("</name>\n");
                sb.append("    <rollover>").append(s.isAllowRollover() ? 1 : 0).append("</rollover>\n");
                sb.append("  </usrsequence>\n");
            }
            sb.append("</sequencers>\n");
            write(out, sb);
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

    public void exportBuildInfos(Connection con, OutputStream out, StringBuilder sb) throws Exception {
        Statement stmt = null;
        try {
            stmt = con.createStatement();
            writeHeader(out);
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
        } finally {
            Database.closeObjects(GenericDivisionExporter.class, stmt);
        }
    }

}
