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
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.impex.FxDivisionExportInfo;
import com.flexive.shared.impex.FxImportExportConstants;
import org.apache.commons.lang.StringEscapeUtils;
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

    private static final Log LOG = LogFactory.getLog(GenericDivisionImporter.class);

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
     * Get a file from the zip archive
     *
     * @param zip  zip archive containing the file
     * @param file name of the file
     * @return ZipEntry
     * @throws FxNotFoundException if the archive does not contain the file
     */
    private ZipEntry getZipEntry(ZipFile zip, String file) throws FxNotFoundException {
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
                    drops,
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

        FxFileUtils.removeDirectory(FxBinaryUtils.getBinaryDirectory() + File.separatorChar + String.valueOf(FxContext.get().getDivisionId()));
        FxFileUtils.removeDirectory(FxBinaryUtils.getTransitDirectory() + File.separatorChar + String.valueOf(FxContext.get().getDivisionId()));
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
    private void dropTable(Statement stmt, String table) throws SQLException {
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
    private void importTable(Statement stmt, final ZipFile zip, final ZipEntry ze, final String xpath, final String table) throws Exception {
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
     * @param updateColumns      columns that should be set to <code>null</code> in a first pass (insert) and updated to the provided values in a second pass (update)
     * @throws Exception on errors
     */
    private void importTable(Statement stmt, final ZipFile zip, final ZipEntry ze, final String xpath, final String table,
                             final boolean executeInsertPhase, final boolean executeUpdatePhase, final String... updateColumns) throws Exception {
        //analyze the table
        final ResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " WHERE 1=2");
        StringBuilder sbInsert = new StringBuilder(500);
        StringBuilder sbUpdate = updateColumns.length > 0 ? new StringBuilder(500) : null;
        if (rs == null)
            throw new IllegalArgumentException("Can not analyze table [" + table + "]!");
        sbInsert.append("INSERT INTO ").append(table).append(" (");
        if (sbUpdate != null) {
            sbUpdate.append("UPDATE ").append(table).append(" SET ");
            for (int i = 0; i < updateColumns.length; i++) {
                if (i > 0)
                    sbUpdate.append(',');
                sbUpdate.append(updateColumns[i]).append("=?");
            }
            sbUpdate.append(" WHERE ");
        }
        final ResultSetMetaData md = rs.getMetaData();
        final Map<String, ColumnInfo> insertColumns = new HashMap<String, ColumnInfo>(md.getColumnCount());
        final Map<String, ColumnInfo> updateClauseColumns = updateColumns.length > 0 ? new HashMap<String, ColumnInfo>(md.getColumnCount()) : null;
        final Map<String, ColumnInfo> updateSetColumns = updateColumns.length > 0 ? new HashMap<String, ColumnInfo>(md.getColumnCount()) : null;
        int insertIndex = 1;
        int updateSetIndex = 1;
        int updateClauseIndex = 1;
        boolean first = true;
        for (int i = 0; i < md.getColumnCount(); i++) {
            final String currCol = md.getColumnName(i + 1).toLowerCase();
            if (updateColumns.length > 0) {
                boolean abort = false;
                for (String col : updateColumns) {
                    if (currCol.equals(col)) {
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
            if (updateColumns.length > 0) {
                updateClauseColumns.put(currCol, new ColumnInfo(md.getColumnType(i + 1), updateClauseIndex++));
                sbUpdate.append(currCol).append("=? AND ");
            }
        }
        if (updateColumns.length > 0) {
            sbUpdate.delete(sbUpdate.length() - 5, sbUpdate.length()); //remove trailing " AND "
            //"shift" clause indices
            for (String col : updateClauseColumns.keySet()) {
                GenericDivisionImporter.ColumnInfo ci = updateClauseColumns.get(col);
                ci.index += (updateSetIndex - 1);
            }
        }
        sbInsert.append(")VALUES(");
        for (int i = 0; i < md.getColumnCount() - updateColumns.length; i++) {
            if (i > 0)
                sbInsert.append(',');
            sbInsert.append('?');
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
        final PreparedStatement psUpdate = updateColumns.length > 0 ? stmt.getConnection().prepareStatement(sbUpdate.toString()) : null;
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
                            data.put(attributes.getLocalName(i), attributes.getValue(i));
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

                        if (value == null)
                            ps.setNull(ci.index, ci.columnType);
                        else {
                            dataSet = true;
                            switch (ci.columnType) {
                                case Types.BIGINT:
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
                                case Types.NUMERIC:
                                    if (DBG) LOG.info("Long " + ci.index + "->" + Long.valueOf(value));
                                    ps.setLong(ci.index, Long.valueOf(value));
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
                                case Types.LONGNVARCHAR:
                                case Types.LONGVARCHAR:
                                case Types.NCHAR:
                                case Types.NCLOB:
                                case Types.NVARCHAR:
                                case Types.VARCHAR:
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
                    true, true, "unique_target");
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
            importTable(stmt, zip, ze, "structures/properties/property", DatabaseConst.TBL_STRUCT_PROPERTIES);
            importTable(stmt, zip, ze, "structures/properties/property_t", DatabaseConst.TBL_STRUCT_PROPERTIES + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/properties/poption", DatabaseConst.TBL_STRUCT_PROPERTY_OPTIONS);
            importTable(stmt, zip, ze, "structures/groups/group", DatabaseConst.TBL_STRUCT_GROUPS);
            importTable(stmt, zip, ze, "structures/groups/group_t", DatabaseConst.TBL_STRUCT_GROUPS + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/assignments/assignment", DatabaseConst.TBL_STRUCT_ASSIGNMENTS);
            importTable(stmt, zip, ze, "structures/assignments/assignment_t", DatabaseConst.TBL_STRUCT_ASSIGNMENTS + DatabaseConst.ML);
            importTable(stmt, zip, ze, "structures/selectlists/item", DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM);
            importTable(stmt, zip, ze, "structures/selectlists/item_t", DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM + DatabaseConst.ML);
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
            File binDir = new File(FxBinaryUtils.getBinaryDirectory() + File.separatorChar + String.valueOf(FxContext.get().getDivisionId()));
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
                EJBLookup.getNodeConfigurationEngine().put(SystemParameters.NODE_BINARY_PATH, binDir.getAbsolutePath());
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
            importTable(stmt, zip, ze, "hierarchical/ft", DatabaseConst.TBL_CONTENT_DATA_FT);
            importTable(stmt, zip, ze, "hierarchical/acl", DatabaseConst.TBL_CONTENT_ACLS);
            importTable(stmt, zip, ze_struct, "structures/types/tdef", DatabaseConst.TBL_STRUCT_TYPES, false, true, "icon_ref");
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
            importTable(stmt, zip, ze, "tree/edit/node", DatabaseConst.TBL_TREE, true, true, "parent");
            importTable(stmt, zip, ze, "tree/live/node", DatabaseConst.TBL_TREE + "_LIVE", true, true, "parent");
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
     * Import briefcases
     *
     * @param con an open and valid connection to store imported data
     * @param zip zip file containing the data
     * @throws Exception on errors
     */
    public void importFlatStorages(Connection con, ZipFile zip) throws Exception {
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
                    FxFlatStorageManager.getInstance().createFlatStorage(tableName, description, cstring, ctext, cbigInt, cdouble, cselect);
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
