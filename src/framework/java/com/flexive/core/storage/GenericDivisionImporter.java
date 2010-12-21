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
import com.flexive.core.flatstorage.FxFlatStorage;
import com.flexive.core.flatstorage.FxFlatStorageInfo;
import com.flexive.core.flatstorage.FxFlatStorageManager;
import com.flexive.core.storage.binary.FxBinaryUtils;
import com.flexive.shared.*;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxDbException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.impex.FxDivisionExportInfo;
import com.flexive.shared.impex.FxImportExportConstants;
import com.flexive.shared.structure.FxDataType;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.*;
import java.text.ParseException;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Division Importer
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class GenericDivisionImporter implements FxImportExportConstants {
/*

//test script for flatstorage as hierarchical:

// Default [fleXive] imports for Groovy
import com.flexive.shared.*
import com.flexive.shared.interfaces.*
import com.flexive.shared.value.*
import com.flexive.shared.content.*
import com.flexive.shared.search.*
import com.flexive.shared.search.query.*
import com.flexive.shared.tree.*
import com.flexive.shared.workflow.*
import com.flexive.shared.media.*
import com.flexive.shared.scripting.groovy.*
import com.flexive.shared.structure.*
import com.flexive.shared.security.*
import com.flexive.shared.impex.*
import com.flexive.core.*
import com.flexive.core.storage.*
import java.util.zip.*

File data = new File("/home/mplesser/impex/export_pg.zip")
ZipFile zip = new ZipFile(data)
java.sql.Connection con = Database.getDbConnection()
try {
  FxDivisionExportInfo ei = GenericDivisionImporter.getInstance().getDivisionExportInfo(zip)
  GenericDivisionImporter.getInstance().importFlatStoragesHierarchical(con, zip, ei)
} finally {
  con.close()
}
 */
    private static final Log LOG = LogFactory.getLog(GenericDivisionImporter.class);

    @SuppressWarnings({"FieldCanBeLocal"})
    private static GenericDivisionImporter INSTANCE = new GenericDivisionImporter();

    private static boolean DBG = false;

    /**
     * Getter for the importer singleton
     *
     * @return exporter
     */
    public static GenericDivisionImporter getInstance() {
        return INSTANCE;
    }

    /**
     * Does importing a division require a non-transactional connection?
     *
     * @return need non-TX Connection or use "regular"?
     */
    public boolean importRequiresNonTXConnection() {
        return false;
    }

    /**
     * Get a file from the zip archive
     *
     * @param zip  zip archive containing the file
     * @param file name of the file
     * @return ZipEntry
     * @throws FxNotFoundException if the archive does not contain the file
     */
    protected ZipEntry getZipEntry(ZipFile zip, String file) throws FxNotFoundException {
        ZipEntry ze = zip.getEntry(file);
        if (ze == null)
            throw new FxNotFoundException("ex.import.missingFile", file, zip.getName());
        return ze;
    }

    /**
     * Get division export information from an exported archive
     *
     * @param zip zip file containing the export
     * @return FxDivisionExportInfo
     * @throws FxApplicationException on errors
     */
    public FxDivisionExportInfo getDivisionExportInfo(ZipFile zip) throws FxApplicationException {
        ZipEntry ze = getZipEntry(zip, FILE_BUILD_INFOS);
        FxDivisionExportInfo exportInfo;
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(zip.getInputStream(ze));

            XPath xPath = XPathFactory.newInstance().newXPath();

            String[] drops;
            String dropsRaw = xPath.evaluate("/flexive/drops", document);
            if (dropsRaw == null || !dropsRaw.startsWith("["))
                drops = new String[0];
            else {
                dropsRaw = dropsRaw.substring(1, dropsRaw.length() - 1);
                drops = dropsRaw.split(", ");
            }
            exportInfo = new FxDivisionExportInfo(
                    Integer.parseInt(xPath.evaluate("/flexive/division", document)),
                    Integer.parseInt(xPath.evaluate("/flexive/schema", document)),
                    Integer.parseInt(xPath.evaluate("/flexive/build", document)),
                    xPath.evaluate("/flexive/verbose", document),
                    xPath.evaluate("/flexive/appserver", document),
                    xPath.evaluate("/flexive/database", document),
                    xPath.evaluate("/flexive/dbdriver", document),
                    xPath.evaluate("/flexive/domain", document),
                    Arrays.asList(drops),
                    xPath.evaluate("/flexive/user", document),
                    FxFormatUtils.getDateTimeFormat().parse(xPath.evaluate("/flexive/date", document))
            );
        } catch (Exception e) {
            throw new FxApplicationException(e, "ex.import.parseInfoFailed", e.getMessage());
        }
        return exportInfo;
    }

    /**
     * Wipe all tables of a division
     *
     * @param con an open and valid connection for the division to wipe
     * @throws Exception on errors
     */
    public void wipeDivisionData(Connection con) throws Exception {
        Statement stmt = con.createStatement();
        dropTableData(stmt, DatabaseConst.TBL_CONTENT_DATA_FT);
        dropTableData(stmt, DatabaseConst.TBL_CONTENT_DATA);
        dropTableData(stmt, DatabaseConst.TBL_CONTENT_ACLS);
        dropTableData(stmt, DatabaseConst.TBL_BINARY_TRANSIT);
        dropTableData(stmt, DatabaseConst.TBL_SEARCHCACHE_PERM);
        dropTableData(stmt, DatabaseConst.TBL_SEARCHCACHE_MEMORY);
        dropTableData(stmt, DatabaseConst.TBL_BRIEFCASE_DATA);
        dropTableData(stmt, DatabaseConst.TBL_BRIEFCASE);
        setFieldNull(stmt, DatabaseConst.TBL_TREE + "_LIVE", "PARENT");
        dropTableData(stmt, DatabaseConst.TBL_TREE + "_LIVE");
        setFieldNull(stmt, DatabaseConst.TBL_TREE, "PARENT");
        dropTableData(stmt, DatabaseConst.TBL_TREE);
        boolean hasFlatStorages = false;
        for (FxFlatStorageInfo flat : FxFlatStorageManager.getInstance().getFlatStorageInfos()) {
            hasFlatStorages = true;
            dropTableData(stmt, flat.getName());
            dropTable(stmt, flat.getName());
        }
        if (hasFlatStorages) {
            dropTableData(stmt, DatabaseConst.TBL_STRUCT_FLATSTORE_INFO);
            dropTableData(stmt, DatabaseConst.TBL_STRUCT_FLATSTORE_MAPPING);
        }
        dropTableData(stmt, DatabaseConst.TBL_RESOURCES);
        dropTableData(stmt, DatabaseConst.TBL_HISTORY);
        dropTableData(stmt, DatabaseConst.TBL_ACCOUNT_DETAILS);
        dropTableData(stmt, DatabaseConst.TBL_LOCKS);
        setFieldNull(stmt, DatabaseConst.TBL_STRUCT_TYPES, "ICON_REF");
        dropTableData(stmt, DatabaseConst.TBL_CONTENT);

        dropTableData(stmt, DatabaseConst.TBL_ACLS + DatabaseConst.ML);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_ASSIGNMENTS + DatabaseConst.ML);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM + DatabaseConst.ML);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_SELECTLIST + DatabaseConst.ML);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_TYPES + DatabaseConst.ML);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_GROUPS + DatabaseConst.ML);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_PROPERTIES + DatabaseConst.ML);
        dropTableData(stmt, DatabaseConst.TBL_WORKFLOW_STEPDEFINITION + DatabaseConst.ML);

        dropTableData(stmt, DatabaseConst.TBL_SCRIPT_MAPPING_TYPES);
        dropTableData(stmt, DatabaseConst.TBL_SCRIPT_MAPPING_ASSIGN);
        dropTableData(stmt, DatabaseConst.TBL_ROLE_MAPPING);

        dropTableData(stmt, DatabaseConst.TBL_STRUCT_GROUP_OPTIONS);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_PROPERTY_OPTIONS);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_TYPES_OPTIONS);

        stmt.execute(StorageManager.getStorageImpl().getReferentialIntegrityChecksStatement(false));
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_ASSIGNMENTS);
        stmt.execute(StorageManager.getStorageImpl().getReferentialIntegrityChecksStatement(true));
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_PROPERTIES);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_GROUPS);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_TYPERELATIONS);
        setFieldNull(stmt, DatabaseConst.TBL_STRUCT_TYPES, "PARENT");
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_TYPES);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_DATATYPES + DatabaseConst.ML);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_DATATYPES);

        dropTableData(stmt, DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM);
        dropTableData(stmt, DatabaseConst.TBL_STRUCT_SELECTLIST);
        dropTableData(stmt, DatabaseConst.TBL_SCRIPTS);

        setFieldNull(stmt, DatabaseConst.TBL_CONTENT_BINARY, "PREVIEW_REF");
        dropTableData(stmt, DatabaseConst.TBL_CONTENT_BINARY);

        dropTableData(stmt, DatabaseConst.TBL_WORKFLOW_ROUTES);
        dropTableData(stmt, DatabaseConst.TBL_WORKFLOW_STEP);
        dropTableData(stmt, DatabaseConst.TBL_WORKFLOW_STEPDEFINITION);
        dropTableData(stmt, DatabaseConst.TBL_WORKFLOW);

        dropTableData(stmt, DatabaseConst.TBL_CONFIG_APPLICATION);
        dropTableData(stmt, DatabaseConst.TBL_CONFIG_DIVISION);
        dropTableData(stmt, DatabaseConst.TBL_CONFIG_NODE);
        dropTableData(stmt, DatabaseConst.TBL_CONFIG_USER);

        dropTableData(stmt, DatabaseConst.TBL_ASSIGN_GROUPS);
        dropTableData(stmt, DatabaseConst.TBL_ACLS_ASSIGNMENT);
        dropTableData(stmt, DatabaseConst.TBL_USERGROUPS);
        dropTableData(stmt, DatabaseConst.TBL_ACLS);
        dropTableData(stmt, DatabaseConst.TBL_ACCOUNTS);
        dropTableData(stmt, DatabaseConst.TBL_MANDATORS);

        dropTableData(stmt, DatabaseConst.TBL_LANG + DatabaseConst.ML);
        dropTableData(stmt, DatabaseConst.TBL_LANG);

        final String divisionPath = File.separatorChar + String.valueOf(FxContext.get().getDivisionId());
        FxFileUtils.removeDirectory(FxBinaryUtils.getBinaryDirectory() + divisionPath);
        FxFileUtils.removeDirectory(FxBinaryUtils.getTransitDirectory() + divisionPath);
    }

    /**
     * Set a column of a table to <code>null</code>
     *
     * @param stmt   statement to operate on
     * @param table  name of the table
     * @param column name of the column
     * @throws SQLException on errors
     */
    private void setFieldNull(Statement stmt, String table, String column) throws SQLException {
        int count = stmt.executeUpdate("UPDATE " + table + " SET " + column + "=NULL");
        LOG.info("Set [" + count + "] entries in [" + table + "." + column + "] to NULL");
    }

    /**
     * Drop a table
     *
     * @param stmt  statement to operate on
     * @param table name of the table to drop
     * @throws SQLException on errors
     */
    protected void dropTable(Statement stmt, String table) throws SQLException {
        stmt.executeUpdate("DROP TABLE " + table);
        LOG.info("Dropped table [" + table + "]");
    }

    /**
     * Drop all data of a table
     *
     * @param stmt  statement to operate on
     * @param table name of the table
     * @throws SQLException on errors
     */
    private void dropTableData(Statement stmt, String table) throws SQLException {
        int count = stmt.executeUpdate("DELETE FROM " + table);
        LOG.info("Removed [" + count + "] entries from table [" + table + "]");
    }

    /**
     * Helper class to keep information about columns in a prepared statement
     */
    class ColumnInfo {
        ColumnInfo(int columnType, int index) {
            this.columnType = columnType;
            this.index = index;
        }

        int columnType;
        int index;
    }


    /**
     * Import data from a zip archive to a database table
     *
     * @param stmt  statement to use
     * @param zip   zip archive containing the zip entry
     * @param ze    zip entry within the archive
     * @param xpath xpath containing the entries to import
     * @param table name of the table
     * @throws Exception on errors
     */
    protected void importTable(Statement stmt, final ZipFile zip, final ZipEntry ze, final String xpath, final String table) throws Exception {
        importTable(stmt, zip, ze, xpath, table, true, false);
    }

    /**
     * Import data from a zip archive to a database table
     *
     * @param stmt               statement to use
     * @param zip                zip archive containing the zip entry
     * @param ze                 zip entry within the archive
     * @param xpath              xpath containing the entries to import
     * @param table              name of the table
     * @param executeInsertPhase execute the insert phase?
     * @param executeUpdatePhase execute the update phase?
     * @param updateColumns      columns that should be set to <code>null</code> in a first pass (insert)
     *                           and updated to the provided values in a second pass (update),
     *                           columns that should be used in the where clause have to be prefixed
     *                           with "KEY:", to assign a default value use the expression "columnname:default value",
     *                           if the default value is "@", it will be a negative counter starting at 0, decreasing.
     *                           If the default value starts with "%", it will be set to the column following the "%"
     *                           character in the first pass
     * @throws Exception on errors
     */
    protected void importTable(Statement stmt, final ZipFile zip, final ZipEntry ze, final String xpath, final String table,
                               final boolean executeInsertPhase, final boolean executeUpdatePhase, final String... updateColumns) throws Exception {
        //analyze the table
        final ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " WHERE 1=2");
        StringBuilder sbInsert = new StringBuilder(500);
        StringBuilder sbUpdate = updateColumns.length > 0 ? new StringBuilder(500) : null;
        if (rs == null)
            throw new IllegalArgumentException("Can not analyze table [" + table + "]!");
        sbInsert.append("INSERT INTO ").append(table).append(" (");
        final ResultSetMetaData md = rs.getMetaData();
        final Map<String, ColumnInfo> updateClauseColumns = updateColumns.length > 0 ? new HashMap<String, ColumnInfo>(md.getColumnCount()) : null;
        final Map<String, ColumnInfo> updateSetColumns = updateColumns.length > 0 ? new LinkedHashMap<String, ColumnInfo>(md.getColumnCount()) : null;
        final Map<String, String> presetColumns = updateColumns.length > 0 ? new HashMap<String, String>(10) : null;
        //preset to a referenced column (%column syntax)
        final Map<String, String> presetRefColumns = updateColumns.length > 0 ? new HashMap<String, String>(10) : null;
        final Map<String, Integer> counters = updateColumns.length > 0 ? new HashMap<String, Integer>(10) : null;
        final Map<String, ColumnInfo> insertColumns = new HashMap<String, ColumnInfo>(md.getColumnCount() + (counters != null ? counters.size() : 0));
        int insertIndex = 1;
        int updateSetIndex = 1;
        int updateClauseIndex = 1;
        boolean first = true;
        for (int i = 0; i < md.getColumnCount(); i++) {
            final String currCol = md.getColumnName(i + 1).toLowerCase();
            if (updateColumns.length > 0) {
                boolean abort = false;
                for (String col : updateColumns) {
                    if (col.indexOf(':') > 0 && !col.startsWith("KEY:")) {
                        String value = col.substring(col.indexOf(':') + 1);
                        col = col.substring(0, col.indexOf(':'));
                        if ("@".equals(value)) {
                            if (currCol.equalsIgnoreCase(col)) {
                                counters.put(col, 0);
                                insertColumns.put(col, new ColumnInfo(md.getColumnType(i + 1), insertIndex++));
                                sbInsert.append(',').append(currCol);
                            }
                        } else if( value.startsWith("%")) {
                            if (currCol.equalsIgnoreCase(col)) {
                                presetRefColumns.put(col, value.substring(1));
                                insertColumns.put(col, new ColumnInfo(md.getColumnType(i + 1), insertIndex++));
                                sbInsert.append(',').append(currCol);
//                                System.out.println("==> adding presetRefColumn "+col+" with value of "+value.substring(1));
                            }
                        } else if (!presetColumns.containsKey(col))
                            presetColumns.put(col, value);
                    }
                    if (currCol.equalsIgnoreCase(col)) {
                        abort = true;
                        updateSetColumns.put(currCol, new ColumnInfo(md.getColumnType(i + 1), updateSetIndex++));
                        break;
                    }
                }
                if (abort)
                    continue;
            }
            if (first) {
                first = false;
            } else
                sbInsert.append(',');
            sbInsert.append(currCol);
            insertColumns.put(currCol, new ColumnInfo(md.getColumnType(i + 1), insertIndex++));
        }
        if (updateColumns.length > 0 && executeUpdatePhase) {
            sbUpdate.append("UPDATE ").append(table).append(" SET ");
            int counter = 0;
            for (String updateColumn : updateSetColumns.keySet()) {
                if (counter++ > 0)
                    sbUpdate.append(',');
                sbUpdate.append(updateColumn).append("=?");
            }
            sbUpdate.append(" WHERE ");
            boolean hasKeyColumn = false;
            for (String col : updateColumns) {
                if (!col.startsWith("KEY:"))
                    continue;
                hasKeyColumn = true;
                String keyCol = col.substring(4);
                for (int i = 0; i < md.getColumnCount(); i++) {
                    if (!md.getColumnName(i + 1).equalsIgnoreCase(keyCol))
                        continue;
                    updateClauseColumns.put(keyCol, new ColumnInfo(md.getColumnType(i + 1), updateClauseIndex++));
                    sbUpdate.append(keyCol).append("=? AND ");
                    break;
                }

            }
            if (!hasKeyColumn)
                throw new IllegalArgumentException("Update columns require a KEY!");
            sbUpdate.delete(sbUpdate.length() - 5, sbUpdate.length()); //remove trailing " AND "
            //"shift" clause indices
            for (String col : updateClauseColumns.keySet()) {
                GenericDivisionImporter.ColumnInfo ci = updateClauseColumns.get(col);
                ci.index += (updateSetIndex - 1);
            }
        }
        if (presetColumns != null) {
            for (String key : presetColumns.keySet())
                sbInsert.append(',').append(key);
        }
        sbInsert.append(")VALUES(");
        for (int i = 0; i < insertColumns.size(); i++) {
            if (i > 0)
                sbInsert.append(',');
            sbInsert.append('?');
        }
        if (presetColumns != null) {
            for (String key : presetColumns.keySet())
                sbInsert.append(',').append(presetColumns.get(key));
        }
        sbInsert.append(')');
        if (DBG) {
            LOG.info("Insert statement:\n" + sbInsert.toString());
            if (updateColumns.length > 0)
                LOG.info("Update statement:\n" + sbUpdate.toString());
        }
        //build a map containing all nodes that require attributes
        //this allows for matching simple xpath queries like "flatstorages/storage[@name='FX_FLAT_STORAGE']/data"
        final Map<String, List<String>> queryAttributes = new HashMap<String, List<String>>(5);
        for (String pElem : xpath.split("/")) {
            if (!(pElem.indexOf('@') > 0 && pElem.indexOf('[') > 0))
                continue;
            List<String> att = new ArrayList<String>(5);
            for (String pAtt : pElem.split("@")) {
                if (!(pAtt.indexOf('=') > 0))
                    continue;
                att.add(pAtt.substring(0, pAtt.indexOf('=')));
            }
            queryAttributes.put(pElem.substring(0, pElem.indexOf('[')), att);
        }
        final PreparedStatement psInsert = stmt.getConnection().prepareStatement(sbInsert.toString());
        final PreparedStatement psUpdate = updateColumns.length > 0 && executeUpdatePhase ? stmt.getConnection().prepareStatement(sbUpdate.toString()) : null;
        try {
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            final DefaultHandler handler = new DefaultHandler() {
                private String currentElement = null;
                private Map<String, String> data = new HashMap<String, String>(10);
                private StringBuilder sbData = new StringBuilder(10000);
                boolean inTag = false;
                boolean inElement = false;
                int counter;
                List<String> path = new ArrayList<String>(10);
                StringBuilder currPath = new StringBuilder(100);
                boolean insertMode = true;


                /**
                 * {@inheritDoc}
                 */
                @Override
                public void startDocument() throws SAXException {
                    counter = 0;
                    inTag = false;
                    inElement = false;
                    path.clear();
                    currPath.setLength(0);
                    sbData.setLength(0);
                    data.clear();
                    currentElement = null;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void processingInstruction(String target, String data) throws SAXException {
                    if (target != null && target.startsWith("fx_")) {
                        if (target.equals("fx_mode"))
                            insertMode = "insert".equals(data);
                    } else
                        super.processingInstruction(target, data);
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void endDocument() throws SAXException {
                    if (insertMode)
                        LOG.info("Imported [" + counter + "] entries into [" + table + "] for xpath [" + xpath + "]");
                    else
                        LOG.info("Updated [" + counter + "] entries in [" + table + "] for xpath [" + xpath + "]");
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    pushPath(qName, attributes);
                    if (currPath.toString().equals(xpath)) {
                        inTag = true;
                        data.clear();
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String name = attributes.getLocalName(i);
                            if (StringUtils.isEmpty(name))
                                name = attributes.getQName(i);
                            data.put(name, attributes.getValue(i));
                        }
                    } else {
                        currentElement = qName;
                    }
                    inElement = true;
                    sbData.setLength(0);
                }

                /**
                 * Push a path element from the stack
                 *
                 * @param qName element name to push
                 * @param att attributes
                 */
                private void pushPath(String qName, Attributes att) {
                    if (att.getLength() > 0 && queryAttributes.containsKey(qName)) {
                        String curr = qName + "[";
                        boolean first = true;
                        final List<String> attList = queryAttributes.get(qName);
                        for (int i = 0; i < att.getLength(); i++) {
                            if (!attList.contains(att.getQName(i)))
                                continue;
                            if (first)
                                first = false;
                            else
                                curr += ',';
                            curr += "@" + att.getQName(i) + "='" + att.getValue(i) + "'";
                        }
                        curr += ']';
                        path.add(curr);
                    } else
                        path.add(qName);
                    buildPath();
                }

                /**
                 * Pop the top path element from the stack
                 */
                private void popPath() {
                    path.remove(path.size() - 1);
                    buildPath();
                }

                /**
                 * Rebuild the current path
                 */
                private synchronized void buildPath() {
                    currPath.setLength(0);
                    for (String s : path)
                        currPath.append(s).append('/');
                    if (currPath.length() > 1)
                        currPath.delete(currPath.length() - 1, currPath.length());
//                    System.out.println("currPath: " + currPath);
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (currPath.toString().equals(xpath)) {
                        if (DBG) LOG.info("Insert [" + xpath + "]: [" + data + "]");
                        inTag = false;
                        try {
                            if (insertMode) {
                                if (executeInsertPhase) {
                                    processColumnSet(insertColumns, psInsert);
                                    counter += psInsert.executeUpdate();
                                }
                            } else {
                                if (executeUpdatePhase) {
                                    if (processColumnSet(updateSetColumns, psUpdate)) {
                                        processColumnSet(updateClauseColumns, psUpdate);
                                        counter += psUpdate.executeUpdate();
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            throw new SAXException(e);
                        } catch (ParseException e) {
                            throw new SAXException(e);
                        }
                    } else {
                        if (inTag) {
                            data.put(currentElement, sbData.toString());
                        }
                        currentElement = null;
                    }
                    popPath();
                    inElement = false;
                    sbData.setLength(0);
                }

                /**
                 * Process a column set
                 *
                 * @param columns the columns to process
                 * @param ps prepared statement to use
                 * @return if data other than <code>null</code> has been set
                 * @throws SQLException on errors
                 * @throws ParseException on date/time conversion errors
                 */
                private boolean processColumnSet(Map<String, ColumnInfo> columns, PreparedStatement ps) throws SQLException, ParseException {
                    boolean dataSet = false;
                    for (String col : columns.keySet()) {
                        ColumnInfo ci = columns.get(col);
                        String value = StringEscapeUtils.unescapeXml(data.get(col));
                        if (insertMode && counters != null && counters.get(col) != null) {
                            final int newVal = counters.get(col) - 1;
                            value = String.valueOf(newVal);
                            counters.put(col, newVal);
//                            System.out.println("new value for " + col + ": " + newVal);
                        }
                        if (insertMode && presetRefColumns != null && presetRefColumns.get(col) != null) {
                            value = StringEscapeUtils.unescapeXml(data.get(presetRefColumns.get(col)));
//                            System.out.println("Set presetRefColumn for "+col+" to ["+value+"] from column ["+presetRefColumns.get(col)+"]");
                        }

                        if (value == null)
                            ps.setNull(ci.index, ci.columnType);
                        else {
                            dataSet = true;
                            switch (ci.columnType) {
                                case Types.BIGINT:
                                case Types.NUMERIC:
                                    if (DBG) LOG.info("BigInt " + ci.index + "->" + new BigDecimal(value));
                                    ps.setBigDecimal(ci.index, new BigDecimal(value));
                                    break;
                                case java.sql.Types.DOUBLE:
                                    if (DBG) LOG.info("Double " + ci.index + "->" + Double.parseDouble(value));
                                    ps.setDouble(ci.index, Double.parseDouble(value));
                                    break;
                                case java.sql.Types.FLOAT:
                                    if (DBG) LOG.info("Float " + ci.index + "->" + Float.parseFloat(value));
                                    ps.setFloat(ci.index, Float.parseFloat(value));
                                    break;
                                case java.sql.Types.TIMESTAMP:
                                case java.sql.Types.DATE:
                                    if (DBG)
                                        LOG.info("Timestamp/Date " + ci.index + "->" + FxFormatUtils.getDateTimeFormat().parse(value));
                                    ps.setTimestamp(ci.index, new Timestamp(FxFormatUtils.getDateTimeFormat().parse(value).getTime()));
                                    break;
                                case Types.TINYINT:
                                case Types.SMALLINT:
                                    if (DBG) LOG.info("Integer " + ci.index + "->" + Integer.valueOf(value));
                                    ps.setInt(ci.index, Integer.valueOf(value));
                                    break;
                                case Types.INTEGER:
                                case Types.DECIMAL:
                                    try {
                                        if (DBG) LOG.info("Long " + ci.index + "->" + Long.valueOf(value));
                                        ps.setLong(ci.index, Long.valueOf(value));
                                    } catch (NumberFormatException e) {
                                        //Fallback (temporary) for H2 if the reported long is a big decimal (tree...)
                                        ps.setBigDecimal(ci.index, new BigDecimal(value));
                                    }
                                    break;
                                case Types.BIT:
                                case Types.CHAR:
                                case Types.BOOLEAN:
                                    if (DBG) LOG.info("Boolean " + ci.index + "->" + value);
                                    if ("1".equals(value) || "true".equals(value))
                                        ps.setBoolean(ci.index, true);
                                    else
                                        ps.setBoolean(ci.index, false);
                                    break;
                                case Types.LONGVARBINARY:
                                case Types.VARBINARY:
                                case Types.BLOB:
                                case Types.BINARY:
                                    ZipEntry bin = zip.getEntry(value);
                                    if (bin == null) {
                                        LOG.error("Failed to lookup binary [" + value + "]!");
                                        ps.setNull(ci.index, ci.columnType);
                                        break;
                                    }
                                    try {
                                        ps.setBinaryStream(ci.index, zip.getInputStream(bin), (int) bin.getSize());
                                    } catch (IOException e) {
                                        LOG.error("IOException importing binary [" + value + "]: " + e.getMessage(), e);
                                    }
                                    break;
                                case Types.CLOB:
                                case Types.LONGVARCHAR:
                                case Types.VARCHAR:
                                case SQL_LONGNVARCHAR:
                                case SQL_NCHAR:
                                case SQL_NCLOB:
                                case SQL_NVARCHAR:
                                    if (DBG) LOG.info("String " + ci.index + "->" + value);
                                    ps.setString(ci.index, value);
                                    break;
                                default:
                                    LOG.warn("Unhandled type [" + ci.columnType + "] for column [" + col + "]");
                            }
                        }
                    }
                    return dataSet;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (inElement)
                        sbData.append(ch, start, length);
                }


            };
            handler.processingInstruction("fx_mode", "insert");
            parser.parse(zip.getInputStream(ze), handler);
            if (updateColumns.length > 0 && executeUpdatePhase) {
                handler.processingInstruction("fx_mode", "update");
                parser.parse(zip.getInputStream(ze), handler);
            }
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, psInsert, psUpdate);
        }
    }

    /**
     * Import language settings
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importLanguages(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_LANGUAGES);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "languages/lang", DatabaseConst.TBL_LANG);
            importTable(stmt, zip, ze, "languages/lang_t", DatabaseConst.TBL_LANG + DatabaseConst.ML);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import mandators
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importMandators(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_MANDATORS);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "mandators/mandator", DatabaseConst.TBL_MANDATORS);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import security data
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importSecurity(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_SECURITY);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "security/accounts/data/account", DatabaseConst.TBL_ACCOUNTS);
            importTable(stmt, zip, ze, "security/accounts/details/detail", DatabaseConst.TBL_ACCOUNT_DETAILS);
            importTable(stmt, zip, ze, "security/acls/acl", DatabaseConst.TBL_ACLS);
            importTable(stmt, zip, ze, "security/acls/acl_t", DatabaseConst.TBL_ACLS + DatabaseConst.ML);
            importTable(stmt, zip, ze, "security/groups/group", DatabaseConst.TBL_USERGROUPS);
            importTable(stmt, zip, ze, "security/assignments/assignment", DatabaseConst.TBL_ACLS_ASSIGNMENT);
            importTable(stmt, zip, ze, "security/groupAssignments/assignment", DatabaseConst.TBL_ASSIGN_GROUPS);
            importTable(stmt, zip, ze, "security/roleAssignments/assignment", DatabaseConst.TBL_ROLE_MAPPING);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import workflow data
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importWorkflows(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_WORKFLOWS);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "workflow/workflows/workflow", DatabaseConst.TBL_WORKFLOW);
            importTable(stmt, zip, ze, "workflow/stepDefinitions/stepdef", DatabaseConst.TBL_WORKFLOW_STEPDEFINITION,
                    true, true, "unique_target", "KEY:id");
            importTable(stmt, zip, ze, "workflow/stepDefinitions/stepdef_t", DatabaseConst.TBL_WORKFLOW_STEPDEFINITION + DatabaseConst.ML);
            importTable(stmt, zip, ze, "workflow/steps/step", DatabaseConst.TBL_WORKFLOW_STEP);
            importTable(stmt, zip, ze, "workflow/routes/route", DatabaseConst.TBL_WORKFLOW_ROUTES);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import configurations
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importConfigurations(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_CONFIGURATIONS);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "configurations/application/entry", DatabaseConst.TBL_CONFIG_APPLICATION);
            importTable(stmt, zip, ze, "configurations/division/entry", DatabaseConst.TBL_CONFIG_DIVISION);
            importTable(stmt, zip, ze, "configurations/node/entry", DatabaseConst.TBL_CONFIG_NODE);
            importTable(stmt, zip, ze, "configurations/user/entry", DatabaseConst.TBL_CONFIG_USER);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import structural data
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importStructures(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_STRUCTURES);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "structures/selectlists/list", DatabaseConst.TBL_STRUCT_SELECTLIST);
            importTable(stmt, zip, ze, "structures/selectlists/list_t", DatabaseConst.TBL_STRUCT_SELECTLIST + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/datatypes/type", DatabaseConst.TBL_STRUCT_DATATYPES);
            importTable(stmt, zip, ze, "structures/datatypes/type_t", DatabaseConst.TBL_STRUCT_DATATYPES + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/types/tdef", DatabaseConst.TBL_STRUCT_TYPES, true, false, "icon_ref");
            importTable(stmt, zip, ze, "structures/types/tdef_t", DatabaseConst.TBL_STRUCT_TYPES + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/types/trel", DatabaseConst.TBL_STRUCT_TYPERELATIONS);
            importTable(stmt, zip, ze, "structures/types/topts", DatabaseConst.TBL_STRUCT_TYPES_OPTIONS);
            importTable(stmt, zip, ze, "structures/properties/property", DatabaseConst.TBL_STRUCT_PROPERTIES);
            importTable(stmt, zip, ze, "structures/properties/property_t", DatabaseConst.TBL_STRUCT_PROPERTIES + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/groups/group", DatabaseConst.TBL_STRUCT_GROUPS);
            importTable(stmt, zip, ze, "structures/groups/group_t", DatabaseConst.TBL_STRUCT_GROUPS + DatabaseConst.ML);
            DBStorage storage = StorageManager.getStorageImpl();
            try {
                stmt.execute(storage.getReferentialIntegrityChecksStatement(false));
                importTable(stmt, zip, ze, "structures/assignments/assignment", DatabaseConst.TBL_STRUCT_ASSIGNMENTS, true, true, "base:0", "parentgroup:%id", "pos:@", "KEY:id");
            } finally {
                stmt.execute(storage.getReferentialIntegrityChecksStatement(true));
            }
            importTable(stmt, zip, ze, "structures/assignments/assignment_t", DatabaseConst.TBL_STRUCT_ASSIGNMENTS + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/selectlists/item", DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM);
            importTable(stmt, zip, ze, "structures/selectlists/item_t", DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/groups/goption", DatabaseConst.TBL_STRUCT_GROUP_OPTIONS);
            importTable(stmt, zip, ze, "structures/properties/poption", DatabaseConst.TBL_STRUCT_PROPERTY_OPTIONS);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import database and filesystem binaries
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importBinaries(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_BINARIES);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "binaries/binary", DatabaseConst.TBL_CONTENT_BINARY);
            ZipEntry curr;
            final String nodeBinDir = FxBinaryUtils.getBinaryDirectory();
            File binDir = new File(nodeBinDir + File.separatorChar + String.valueOf(FxContext.get().getDivisionId()));
            if (!binDir.exists())
                //noinspection ResultOfMethodCallIgnored
                binDir.mkdirs();
            if (!binDir.exists() && binDir.isDirectory()) {
                LOG.error("Failed to create binary directory [" + binDir.getAbsolutePath() + "]!");
                return;
            }
            int count = 0;
            for (Enumeration e = zip.entries(); e.hasMoreElements();) {
                curr = (ZipEntry) e.nextElement();
                if (curr.getName().startsWith(FOLDER_FS_BINARY) && !curr.isDirectory()) {
                    File out = new File(binDir.getAbsolutePath() + File.separatorChar + curr.getName().substring(FOLDER_FS_BINARY.length() + 1));
                    String path = out.getAbsolutePath();
                    path = path.replace('\\', '/'); //normalize separator chars
                    path = path.replace('/', File.separatorChar);
                    path = path.substring(0, out.getAbsolutePath().lastIndexOf(File.separatorChar));
                    File fPath = new File(path);
                    if (!fPath.exists()) {
                        if (!fPath.mkdirs()) {
                            LOG.error("Failed to create path [" + path + "!]");
                            continue;
                        }
                    }
                    if (!out.createNewFile()) {
                        LOG.error("Failed to create file [" + out.getAbsolutePath() + "]!");
                        continue;
                    }
                    if (FxFileUtils.copyStream2File(curr.getSize(), zip.getInputStream(curr), out))
                        count++;
                    else
                        LOG.error("Failed to write zip stream to file [" + out.getAbsolutePath() + "]!");
                }
            }
            FxContext.get().runAsSystem();
            try {
                EJBLookup.getNodeConfigurationEngine().put(SystemParameters.NODE_BINARY_PATH, nodeBinDir);
            } finally {
                FxContext.get().stopRunAsSystem();
            }
            LOG.info("Imported [" + count + "] files to filesystem binary storage located at [" + binDir.getAbsolutePath() + "]");
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import hierarchical contents
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importHierarchicalContents(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_DATA_HIERARCHICAL);
        ZipEntry ze_struct = getZipEntry(zip, FILE_STRUCTURES);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "hierarchical/content", DatabaseConst.TBL_CONTENT);
            importTable(stmt, zip, ze, "hierarchical/data", DatabaseConst.TBL_CONTENT_DATA);
            importTable(stmt, zip, ze, "hierarchical/acl", DatabaseConst.TBL_CONTENT_ACLS);
            importTable(stmt, zip, ze_struct, "structures/types/tdef", DatabaseConst.TBL_STRUCT_TYPES, false, true, "icon_ref", "KEY:id");
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import scripting data
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importScripts(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_SCRIPTS);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "scripts/script", DatabaseConst.TBL_SCRIPTS);
            importTable(stmt, zip, ze, "scripts/assignmap", DatabaseConst.TBL_SCRIPT_MAPPING_ASSIGN);
            importTable(stmt, zip, ze, "scripts/typemap", DatabaseConst.TBL_SCRIPT_MAPPING_TYPES);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import tree data
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importTree(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_TREE);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "tree/edit/node", DatabaseConst.TBL_TREE, true, true, "parent", "KEY:id");
            importTable(stmt, zip, ze, "tree/live/node", DatabaseConst.TBL_TREE + "_LIVE", true, true, "parent", "KEY:id");
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import history data
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importHistory(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_HISTORY);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "history/entry", DatabaseConst.TBL_HISTORY);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import resource data
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importResources(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_RESOURCES);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "resources/entry", DatabaseConst.TBL_RESOURCES);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import briefcases
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importBriefcases(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_BRIEFCASES);
        Statement stmt = con.createStatement();
        try {
            importTable(stmt, zip, ze, "briefcases/briefcase", DatabaseConst.TBL_BRIEFCASE);
            importTable(stmt, zip, ze, "briefcases/data", DatabaseConst.TBL_BRIEFCASE_DATA);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Import flat storages to the hierarchical storage
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    protected void importFlatStoragesHierarchical(Connection con, ZipFile zip) throws Exception {
        //mapping: storage->level->columnname->assignment id
        final Map<String, Map<Integer, Map<String, Long>>> flatAssignmentMapping = new HashMap<String, Map<Integer, Map<String, Long>>>(5);
        //mapping: assignment id->position index
        final Map<Long, Integer> assignmentPositions = new HashMap<Long, Integer>(100);
        //mapping: flatstorage->column sizes [string,bigint,double,select,text]
        final Map<String, Integer[]> flatstoragesColumns = new HashMap<String, Integer[]>(5);
        ZipEntry zeMeta = getZipEntry(zip, FILE_FLATSTORAGE_META);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = builder.parse(zip.getInputStream(zeMeta));
        XPath xPath = XPathFactory.newInstance().newXPath();

        //calculate column sizes
        NodeList nodes = (NodeList) xPath.evaluate("/flatstorageMeta/storageMeta", document, XPathConstants.NODESET);
        Node currNode;
        for (int i = 0; i < nodes.getLength(); i++) {
            currNode = nodes.item(i);
            int cbigInt = Integer.parseInt(currNode.getAttributes().getNamedItem("bigInt").getNodeValue());
            int cdouble = Integer.parseInt(currNode.getAttributes().getNamedItem("double").getNodeValue());
            int cselect = Integer.parseInt(currNode.getAttributes().getNamedItem("select").getNodeValue());
            int cstring = Integer.parseInt(currNode.getAttributes().getNamedItem("string").getNodeValue());
            int ctext = Integer.parseInt(currNode.getAttributes().getNamedItem("text").getNodeValue());
            String tableName = null;
            if (currNode.hasChildNodes()) {
                for (int j = 0; j < currNode.getChildNodes().getLength(); j++)
                    if (currNode.getChildNodes().item(j).getNodeName().equals("name")) {
                        tableName = currNode.getChildNodes().item(j).getTextContent();
                    }
            }
            if (tableName != null) {
                flatstoragesColumns.put(tableName, new Integer[]{cstring, cbigInt, cdouble, cselect, ctext});
            }
        }

        //parse mappings
        nodes = (NodeList) xPath.evaluate("/flatstorageMeta/mapping", document, XPathConstants.NODESET);
        for (int i = 0; i < nodes.getLength(); i++) {
            currNode = nodes.item(i);
            long assignment = Long.valueOf(currNode.getAttributes().getNamedItem("assid").getNodeValue());
            int level = Integer.valueOf(currNode.getAttributes().getNamedItem("lvl").getNodeValue());
            String storage = null;
            String columnname = null;
            final NodeList childNodes = currNode.getChildNodes();
            for (int c = 0; c < childNodes.getLength(); c++) {
                Node child = childNodes.item(c);
                if ("tblname".equals(child.getNodeName()))
                    storage = child.getTextContent();
                else if ("colname".equals(child.getNodeName()))
                    columnname = child.getTextContent();
            }
            if (storage == null || columnname == null)
                throw new Exception("Invalid flatstorage export: could not read storage or column name!");
            if (!flatAssignmentMapping.containsKey(storage))
                flatAssignmentMapping.put(storage, new HashMap<Integer, Map<String, Long>>(20));
            Map<Integer, Map<String, Long>> levelMap = flatAssignmentMapping.get(storage);
            if (!levelMap.containsKey(level))
                levelMap.put(level, new HashMap<String, Long>(30));
            Map<String, Long> columnMap = levelMap.get(level);
            if (!columnMap.containsKey(columnname))
                columnMap.put(columnname, assignment);
            //calculate position
            assignmentPositions.put(assignment, getAssignmentPosition(flatstoragesColumns.get(storage), columnname));
        }
        if (flatAssignmentMapping.size() == 0) {
            LOG.warn("No flatstorage assignments found to process!");
            return;
        }
        ZipEntry zeData = getZipEntry(zip, FILE_DATA_FLAT);

        final String xpathStorage = "flatstorages/storage";
        final String xpathData = "flatstorages/storage/data";

        final PreparedStatement psGetAssInfo = con.prepareStatement("SELECT DISTINCT a.APROPERTY,a.XALIAS,p.DATATYPE FROM " + DatabaseConst.TBL_STRUCT_ASSIGNMENTS + " a, " + DatabaseConst.TBL_STRUCT_PROPERTIES + " p WHERE a.ID=? AND p.ID=a.APROPERTY");
        final Map<Long, Object[]> assignmentPropAlias = new HashMap<Long, Object[]>(assignmentPositions.size());
        final String insert1 = "INSERT INTO " + DatabaseConst.TBL_CONTENT_DATA +
                //1  2   3   4    5     6             7     8                                              9         10                 11
                "(ID,VER,POS,LANG,TPROP,ASSIGN,XDEPTH,XPATH,XPATHMULT,XMULT,XINDEX,PARENTXMULT,PARENTXPATH,ISMAX_VER,ISLIVE_VER,ISGROUP,ISMLDEF,";
        final String insert2 = "(?,?,?,?,?,?,1,?,?,1,1,1,'/',?,?," + StorageManager.getBooleanFalseExpression() + ",?,";
        final PreparedStatement psString = con.prepareStatement(insert1 + "FTEXT1024,UFTEXT1024,FSELECT,FINT)VALUES" +
                insert2 + "?,?,0,?)");
        final PreparedStatement psText = con.prepareStatement(insert1 + "FCLOB,UFCLOB,FSELECT,FINT)VALUES" +
                insert2 + "?,?,0,?)");
        final PreparedStatement psDouble = con.prepareStatement(insert1 + "FDOUBLE,FSELECT,FINT)VALUES" +
                insert2 + "?,0,?)");
        final PreparedStatement psNumber = con.prepareStatement(insert1 + "FINT,FSELECT,FBIGINT)VALUES" +
                insert2 + "?,0,?)");
        final PreparedStatement psLargeNumber = con.prepareStatement(insert1 + "FBIGINT,FSELECT,FINT)VALUES" +
                insert2 + "?,0,?)");
        final PreparedStatement psFloat = con.prepareStatement(insert1 + "FFLOAT,FSELECT,FINT)VALUES" +
                insert2 + "?,0,?)");
        final PreparedStatement psBoolean = con.prepareStatement(insert1 + "FBOOL,FSELECT,FINT)VALUES" +
                insert2 + "?,0,?)");
        final PreparedStatement psReference = con.prepareStatement(insert1 + "FREF,FSELECT,FINT)VALUES" +
                insert2 + "?,0,?)");
        final PreparedStatement psSelectOne = con.prepareStatement(insert1 + "FSELECT,FINT)VALUES" +
                insert2 + "?,?)");
        try {
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            final DefaultHandler handler = new DefaultHandler() {
                private String currentElement = null;
                private String currentStorage = null;
                private Map<String, String> data = new HashMap<String, String>(10);
                private StringBuilder sbData = new StringBuilder(10000);
                boolean inTag = false;
                boolean inElement = false;
                List<String> path = new ArrayList<String>(10);
                StringBuilder currPath = new StringBuilder(100);
                int insertCount = 0;


                /**
                 * {@inheritDoc}
                 */
                @Override
                public void startDocument() throws SAXException {
                    inTag = false;
                    inElement = false;
                    path.clear();
                    currPath.setLength(0);
                    sbData.setLength(0);
                    data.clear();
                    currentElement = null;
                    currentStorage = null;
                    insertCount = 0;
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void endDocument() throws SAXException {
                    LOG.info("Imported [" + insertCount + "] flatstorage entries into the hierarchical storage");
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
                    pushPath(qName, attributes);
                    if (currPath.toString().equals(xpathData)) {
                        inTag = true;
                        data.clear();
                        for (int i = 0; i < attributes.getLength(); i++) {
                            String name = attributes.getLocalName(i);
                            if (StringUtils.isEmpty(name))
                                name = attributes.getQName(i);
                            data.put(name, attributes.getValue(i));
                        }
                    } else if (currPath.toString().equals(xpathStorage)) {
                        currentStorage = attributes.getValue("name");
                        LOG.info("Processing storage: " + currentStorage);
                    } else {
                        currentElement = qName;
                    }
                    inElement = true;
                    sbData.setLength(0);
                }

                /**
                 * Push a path element from the stack
                 *
                 * @param qName element name to push
                 * @param att attributes
                 */
                @SuppressWarnings({"UnusedDeclaration"})
                private void pushPath(String qName, Attributes att) {
                    path.add(qName);
                    buildPath();
                }

                /**
                 * Pop the top path element from the stack
                 */
                private void popPath() {
                    path.remove(path.size() - 1);
                    buildPath();
                }

                /**
                 * Rebuild the current path
                 */
                private synchronized void buildPath() {
                    currPath.setLength(0);
                    for (String s : path)
                        currPath.append(s).append('/');
                    if (currPath.length() > 1)
                        currPath.delete(currPath.length() - 1, currPath.length());
//                    System.out.println("currPath: " + currPath);
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (currPath.toString().equals(xpathData)) {
//                        LOG.info("Insert [" + xpathData + "]: [" + data + "]");
                        inTag = false;
                        processData();
                        /*try {
                            if (insertMode) {
                                if (executeInsertPhase) {
                                    processColumnSet(insertColumns, psInsert);
                                    counter += psInsert.executeUpdate();
                                }
                            } else {
                                if (executeUpdatePhase) {
                                    if (processColumnSet(updateSetColumns, psUpdate)) {
                                        processColumnSet(updateClauseColumns, psUpdate);
                                        counter += psUpdate.executeUpdate();
                                    }
                                }
                            }
                        } catch (SQLException e) {
                            throw new SAXException(e);
                        } catch (ParseException e) {
                            throw new SAXException(e);
                        }*/
                    } else {
                        if (inTag) {
                            data.put(currentElement, sbData.toString());
                        }
                        currentElement = null;
                    }
                    popPath();
                    inElement = false;
                    sbData.setLength(0);
                }

                void processData() {
//                    System.out.println("processing " + currentStorage + " -> " + data);
                    final String[] cols = {"string", "bigint", "double", "select", "text"};
                    for (String column : data.keySet()) {
                        if (column.endsWith("_mld"))
                            continue;
                        for (String check : cols) {
                            if (column.startsWith(check)) {
                                if ("select".equals(check) && "0".equals(data.get(column)))
                                    continue; //dont insert 0-referencing selects
                                try {
                                    insertData(column);
                                } catch (SQLException e) {
                                    //noinspection ThrowableInstanceNeverThrown
                                    throw new FxDbException(e, "ex.db.sqlError", e.getMessage()).asRuntimeException();
                                }
                            }
                        }
                    }
                }

                private void insertData(String column) throws SQLException {
                    final int level = Integer.parseInt(data.get("lvl"));
                    long assignment = flatAssignmentMapping.get(currentStorage).get(level).get(column.toUpperCase());
                    int pos = FxArrayUtils.getIntElementAt(data.get("positions"), ',', assignmentPositions.get(assignment));
                    String _valueData = data.get("valuedata");
                    Integer valueData = _valueData == null ? null : FxArrayUtils.getHexIntElementAt(data.get("valuedata"), ',', assignmentPositions.get(assignment));
                    Object[] propXP = getPropertyXPathDataType(assignment);
                    long prop = (Long) propXP[0];
                    String xpath = (String) propXP[1];
                    FxDataType dataType;
                    try {
                        dataType = FxDataType.getById((Long) propXP[2]);
                    } catch (FxNotFoundException e) {
                        throw e.asRuntimeException();
                    }
                    long id = Long.parseLong(data.get("id"));
                    int ver = Integer.parseInt(data.get("ver"));
                    long lang = Integer.parseInt(data.get("lang"));
                    boolean isMaxVer = "1".equals(data.get("ismax_ver"));
                    boolean isLiveVer = "1".equals(data.get("islive_ver"));
                    boolean mlDef = "1".equals(data.get(column + "_mld"));
                    PreparedStatement ps;
                    int vdPos;
                    switch (dataType) {
                        case String1024:
                            ps = psString;
                            ps.setString(12, data.get(column));
                            ps.setString(13, data.get(column).toUpperCase());
                            vdPos = 14;
                            break;
                        case Text:
                        case HTML:
                            ps = psText;
                            ps.setString(12, data.get(column));
                            ps.setString(13, data.get(column).toUpperCase());
                            vdPos = 14;
                            break;
                        case Number:
                            ps = psNumber;
                            ps.setLong(12, Long.valueOf(data.get(column)));
                            vdPos = 13;
                            break;
                        case LargeNumber:
                            ps = psLargeNumber;
                            ps.setLong(12, Long.valueOf(data.get(column)));
                            vdPos = 13;
                            break;
                        case Reference:
                            ps = psReference;
                            ps.setLong(12, Long.valueOf(data.get(column)));
                            vdPos = 13;
                            break;
                        case Float:
                            ps = psFloat;
                            ps.setFloat(12, Float.valueOf(data.get(column)));
                            vdPos = 13;
                            break;
                        case Double:
                            ps = psDouble;
                            ps.setDouble(12, Double.valueOf(data.get(column)));
                            vdPos = 13;
                            break;
                        case Boolean:
                            ps = psBoolean;
                            ps.setBoolean(12, "1".equals(data.get(column)));
                            vdPos = 13;
                            break;
                        case SelectOne:
                            ps = psSelectOne;
                            ps.setLong(12, Long.valueOf(data.get(column)));
                            vdPos = 13;
                            break;
                        default:
                            //noinspection ThrowableInstanceNeverThrown
                            throw new FxInvalidParameterException("assignment", "ex.structure.flatstorage.datatype.unsupported", dataType.name()).asRuntimeException();
                    }
                    ps.setLong(1, id);
                    ps.setInt(2, ver);
                    ps.setInt(3, pos);
                    ps.setLong(4, lang);
                    ps.setLong(5, prop);
                    ps.setLong(6, assignment);
                    ps.setString(7, "/" + xpath);
                    ps.setString(8, "/" + xpath + "[1]");
                    ps.setBoolean(9, isMaxVer);
                    ps.setBoolean(10, isLiveVer);
                    ps.setBoolean(11, mlDef);
                    if (valueData == null)
                        ps.setNull(vdPos, java.sql.Types.NUMERIC);
                    else
                        ps.setInt(vdPos, valueData);
                    ps.executeUpdate();
                    insertCount++;
                }

                /**
                 * Get property id, xpath and data type for an assignment
                 *
                 * @param assignment assignment id
                 * @return Object[] {propertyId, xpath, datatype}
                 */
                private Object[] getPropertyXPathDataType(long assignment) {
                    if (assignmentPropAlias.get(assignment) != null)
                        return assignmentPropAlias.get(assignment);
                    try {
                        psGetAssInfo.setLong(1, assignment);
                        ResultSet rs = psGetAssInfo.executeQuery();
                        if (rs != null && rs.next()) {
                            Object[] data = new Object[]{rs.getLong(1), rs.getString(2), rs.getLong(3)};
                            assignmentPropAlias.put(assignment, data);
                            return data;
                        }
                    } catch (SQLException e) {
                        throw new IllegalArgumentException("Could not load data for assignment " + assignment + ": " + e.getMessage());
                    }
                    throw new IllegalArgumentException("Could not load data for assignment " + assignment + "!");
                }

                /**
                 * {@inheritDoc}
                 */
                @Override
                public void characters(char[] ch, int start, int length) throws SAXException {
                    if (inElement)
                        sbData.append(ch, start, length);
                }


            };
            parser.parse(zip.getInputStream(zeData), handler);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, psGetAssInfo, psString, psBoolean, psDouble, psFloat,
                    psLargeNumber, psNumber, psReference, psSelectOne, psText);
        }
    }

    /**
     * Get the assignment position based on the column name
     *
     * @param columnSizes number of columns o feach type
     * @param columnname  name of the column to evaluate
     * @return position
     */
    private int getAssignmentPosition(Integer[] columnSizes, String columnname) {
        if (columnSizes == null)
            throw new IllegalArgumentException("No columnSizes available!");
        //[string,bigint,double,select,text]
        int start;
        if (columnname.startsWith("STRING"))
            start = 0;
        else if (columnname.startsWith("BIGINT"))
            start = columnSizes[0];
        else if (columnname.startsWith("DOUBLE"))
            start = columnSizes[0] + columnSizes[1];
        else if (columnname.startsWith("SELECT"))
            start = columnSizes[0] + columnSizes[1] + columnSizes[2];
        else if (columnname.startsWith("TEXT"))
            start = columnSizes[0] + columnSizes[1] + columnSizes[2] + columnSizes[3];
        else
            throw new IllegalArgumentException("Unknown column: " + columnname);
        return start + Integer.parseInt(columnname.substring(columnname.length() - 3)); //format COLUMNxxx, xxx=number
    }

    /**
     * Import briefcases
     *
     * @param con        an open and valid connection to store imported data
     * @param zip        zip file containing the data
     * @param exportInfo information about the exported data
     * @throws Exception on errors
     */
    public void importFlatStorages(Connection con, ZipFile zip, FxDivisionExportInfo exportInfo) throws Exception {
        if (!canImportFlatStorages(exportInfo)) {
            importFlatStoragesHierarchical(con, zip);
            return;
        }
        ZipEntry ze = getZipEntry(zip, FILE_FLATSTORAGE_META);
        Statement stmt = con.createStatement();
        try {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(zip.getInputStream(ze));
            XPath xPath = XPathFactory.newInstance().newXPath();

            NodeList nodes = (NodeList) xPath.evaluate("/flatstorageMeta/storageMeta", document, XPathConstants.NODESET);
            Node currNode;
            List<String> storages = new ArrayList<String>(5);
            for (int i = 0; i < nodes.getLength(); i++) {
                currNode = nodes.item(i);
                int cbigInt = Integer.parseInt(currNode.getAttributes().getNamedItem("bigInt").getNodeValue());
                int cdouble = Integer.parseInt(currNode.getAttributes().getNamedItem("double").getNodeValue());
                int cselect = Integer.parseInt(currNode.getAttributes().getNamedItem("select").getNodeValue());
                int cstring = Integer.parseInt(currNode.getAttributes().getNamedItem("string").getNodeValue());
                int ctext = Integer.parseInt(currNode.getAttributes().getNamedItem("text").getNodeValue());
                String tableName = null;
                String description = null;
                if (currNode.hasChildNodes()) {
                    for (int j = 0; j < currNode.getChildNodes().getLength(); j++)
                        if (currNode.getChildNodes().item(j).getNodeName().equals("name")) {
                            tableName = currNode.getChildNodes().item(j).getTextContent();
                        } else if (currNode.getChildNodes().item(j).getNodeName().equals("description")) {
                            description = currNode.getChildNodes().item(j).getTextContent();
                        }
                }
                if (tableName != null) {
                    if (description == null)
                        description = "FlatStorage " + tableName;
                    final FxFlatStorage flatStorage = FxFlatStorageManager.getInstance();
                    for (FxFlatStorageInfo fi : flatStorage.getFlatStorageInfos()) {
                        if (fi.getName().equals(tableName)) {
                            flatStorage.removeFlatStorage(tableName);
                            break;
                        }
                    } 
                    flatStorage.createFlatStorage(con, tableName, description, cstring, ctext, cbigInt, cdouble, cselect);
                    storages.add(tableName);
                }
            }

            importTable(stmt, zip, ze, "flatstorageMeta/mapping", DatabaseConst.TBL_STRUCT_FLATSTORE_MAPPING);

            ZipEntry zeData = getZipEntry(zip, FILE_DATA_FLAT);
            for (String storage : storages)
                importTable(stmt, zip, zeData, "flatstorages/storage[@name='" + storage + "']/data", storage);
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }

    /**
     * Can this importer import flatstorages from the given export?
     *
     * @param exportInfo info about the export
     * @return flatstorages can be imported
     */
    protected boolean canImportFlatStorages(FxDivisionExportInfo exportInfo) {
        return true;
    }

    /**
     * Import sequencer settings
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importSequencers(Connection con, ZipFile zip) throws Exception {
        ZipEntry ze = getZipEntry(zip, FILE_SEQUENCERS);
        Statement stmt = con.createStatement();
        try {
            SequencerStorage seq = StorageManager.getSequencerStorage();
            for (CustomSequencer cust : seq.getCustomSequencers())
                seq.removeSequencer(cust.getName());
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = builder.parse(zip.getInputStream(ze));
            XPath xPath = XPathFactory.newInstance().newXPath();

            NodeList nodes = (NodeList) xPath.evaluate("/sequencers/syssequence", document, XPathConstants.NODESET);
            Node currNode;
            String seqName;
            long value;
            for (int i = 0; i < nodes.getLength(); i++) {
                currNode = nodes.item(i);
                if (currNode.hasChildNodes()) {
                    seqName = null;
                    value = -1L;
                    for (int j = 0; j < currNode.getChildNodes().getLength(); j++)
                        if (currNode.getChildNodes().item(j).getNodeName().equals("name")) {
                            seqName = currNode.getChildNodes().item(j).getTextContent();
                        } else if (currNode.getChildNodes().item(j).getNodeName().equals("value")) {
                            value = Long.parseLong(currNode.getChildNodes().item(j).getTextContent());
                        }
                    if (value != -1L && seqName != null) {
                        try {
                            if (value <= 0)
                                value = 1; //make sure we have a valid value
                            FxSystemSequencer sseq = FxSystemSequencer.valueOf(seqName); //check if this is really a system sequencer
                            seq.setSequencerId(sseq.getSequencerName(), value);
                            LOG.info("Set sequencer [" + seqName + "] to [" + value + "]");
                        } catch (IllegalArgumentException e) {
                            LOG.error("Could not find system sequencer named [" + seqName + "]!");
                        }

                    }
                }
            }

            nodes = (NodeList) xPath.evaluate("/sequencers/usrsequence", document, XPathConstants.NODESET);
            boolean rollOver = false;
            for (int i = 0; i < nodes.getLength(); i++) {
                currNode = nodes.item(i);
                if (currNode.hasChildNodes()) {
                    seqName = null;
                    value = -1L;
                    for (int j = 0; j < currNode.getChildNodes().getLength(); j++)
                        if (currNode.getChildNodes().item(j).getNodeName().equals("name")) {
                            seqName = currNode.getChildNodes().item(j).getTextContent();
                        } else if (currNode.getChildNodes().item(j).getNodeName().equals("value")) {
                            value = Long.parseLong(currNode.getChildNodes().item(j).getTextContent());
                        } else if (currNode.getChildNodes().item(j).getNodeName().equals("rollover")) {
                            rollOver = "1".equals(currNode.getChildNodes().item(j).getTextContent());
                        }
                    if (value != -1L && seqName != null) {
                        seq.createSequencer(seqName, rollOver, value);
                        LOG.info("Created sequencer [" + seqName + "] with start value [" + value + "], rollover: " + rollOver);
                    }
                }
            }
        } finally {
            Database.closeObjects(GenericDivisionImporter.class, stmt);
        }
    }
}
