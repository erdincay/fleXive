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

import com.flexive.core.DatabaseConst;
import com.flexive.core.flatstorage.FxFlatStorageInfo;
import com.flexive.core.flatstorage.FxFlatStorageManager;
import com.flexive.core.storage.binary.FxBinaryUtils;
import com.flexive.shared.FxContext;
import com.flexive.shared.FxFileUtils;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.impex.FxDivisionExportInfo;
import com.flexive.shared.impex.FxImportExportConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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

    /**
     * Getter for the importer singleton
     *
     * @return exporter
     */
    public static GenericDivisionImporter getInstance() {
        return INSTANCE;
    }

    /**
     * Get division export information from an exported archive
     *
     * @param zip zip file containing the export
     * @return FxDivisionExportInfo
     * @throws FxApplicationException on errors
     */
    public FxDivisionExportInfo getDivisionExportInfo(ZipFile zip) throws FxApplicationException {
        ZipEntry ze = zip.getEntry(FILE_BUILD_INFOS);
        if (ze == null)
            throw new FxNotFoundException("ex.import.missingFile", FILE_BUILD_INFOS, zip.getName());
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

    public void importMandators(Connection con, ZipFile zip) throws Exception {

    }
}
