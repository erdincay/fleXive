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
import com.flexive.core.storage.genericSQL.GenericLockStorage;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.impex.FxDivisionExportInfo;
import com.flexive.shared.structure.TypeStorageMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.OutputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static com.flexive.core.DatabaseConst.TBL_STRUCT_SELECTLIST_ITEM;

/**
 * Database vendor specific storage, common implementations for all storages
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public abstract class GenericDBStorage implements DBStorage {

    private static final Log LOG = LogFactory.getLog(GenericDBStorage.class);

    final static String TRUE = "TRUE";
    final static String FALSE = "FALSE";

    /**
     * {@inheritDoc}
     */
    public String getBooleanExpression(boolean flag) {
        return flag ? TRUE : FALSE;
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
    public LockStorage getLockStorage() {
        return GenericLockStorage.getInstance();
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
    public String concat(String... text) {
        if (text.length == 0)
            return "";
        if (text.length == 1)
            return text[0];
        StringBuilder sb = new StringBuilder(500);
        for (int i = 0; i < text.length; i++) {
            if (i > 0 && i < text.length)
                sb.append("||");
            sb.append(text[i]);
        }
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public String concat_ws(String delimiter, String... text) {
        if (text.length == 0)
            return "";
        if (text.length == 1)
            return text[0];
        StringBuilder sb = new StringBuilder(500);
        for (int i = 0; i < text.length; i++) {
            if (i > 0 && i < text.length)
                sb.append("||'").append(delimiter).append("'||");
            sb.append(text[i]);
        }
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
    public String getLimitOffsetVar(String var, boolean hasWhereClause, long limit, long offset) {
        return getLimitOffset(hasWhereClause, limit, offset);
    }

    /**
     * {@inheritDoc}
     */
    public String getLastContentChangeStatement(boolean live) {
        String contentFilter = live ? " WHERE ISLIVE_VER=TRUE " : "";
        return "SELECT MAX(modified_at) FROM\n" +
                "(SELECT\n" +
                "(SELECT MAX(modified_at) FROM " + DatabaseConst.TBL_CONTENT + contentFilter + ") AS modified_at\n" +
                (live ? "\nUNION\n(SELECT MAX(modified_at) FROM " + DatabaseConst.TBL_TREE + "_LIVE)\n" : "") +
                "\nUNION\n(SELECT MAX(modified_at) FROM " + DatabaseConst.TBL_TREE + ")\n" +
                ") changes";
    }

    /**
     * {@inheritDoc}
     */
    public String formatDateCondition(java.util.Date date) {
        return "'" + FxFormatUtils.getDateTimeFormat().format(date) + "'";
    }

    /**
     * {@inheritDoc}
     */
    public String escapeFlatStorageColumn(String column) {
        return column;
    }

    /**
     * {@inheritDoc}
     */
    public String getSelectListItemReferenceFixStatement() {
        return "UPDATE " + TBL_STRUCT_SELECTLIST_ITEM + " SET PARENTID=? WHERE PARENTID IN (SELECT p.ID FROM " +
                TBL_STRUCT_SELECTLIST_ITEM + " p WHERE p.LISTID=?)";
    }

    /**
     * {@inheritDoc}
     */
    public void exportDivision(Connection con, OutputStream out) throws FxApplicationException {
        ZipOutputStream zip = new ZipOutputStream(out);
        GenericDivisionExporter exporter = GenericDivisionExporter.getInstance();
        StringBuilder sb = new StringBuilder(10000);
        try {
            try {
                exporter.exportLanguages(con, zip, sb);
                exporter.exportMandators(con, zip, sb);
                exporter.exportSecurity(con, zip, sb);
                exporter.exportWorkflows(con, zip, sb);
                exporter.exportConfigurations(con, zip, sb);
                exporter.exportStructures(con, zip, sb);
                exporter.exportFlatStorageMeta(con, zip, sb);
                exporter.exportTree(con, zip, sb);
                exporter.exportBriefcases(con, zip, sb);
                exporter.exportScripts(con, zip, sb);
                exporter.exportHistory(con, zip, sb);
                exporter.exportHierarchicalStorage(con, zip, sb);
                exporter.exportFlatStorages(con, zip, sb);
                exporter.exportSequencers(con, zip, sb);
                exporter.exportBuildInfos(con, zip, sb);
                exporter.exportBinaries(con, zip, sb);
            } finally {
                zip.finish();
            }
        } catch (Exception e) {
            throw new FxApplicationException(e, "ex.export.error", e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void importDivision(Connection _con, ZipFile zip) throws Exception {
        GenericDivisionImporter importer = getDivisionImporter();
        FxDivisionExportInfo exportInfo = importer.getDivisionExportInfo(zip);
        if (FxSharedUtils.getDBVersion() != exportInfo.getSchemaVersion()) {
            LOG.warn("DB Version mismatch! Current:" + FxSharedUtils.getDBVersion() +
                    ", exported schema:" + exportInfo.getSchemaVersion());
        }
        boolean isNonTX = importer.importRequiresNonTXConnection();
        Connection con = isNonTX ? Database.getNonTXDataSource().getConnection() : _con;
        
        boolean autoCommit = false;
        if (isNonTX) {
            autoCommit = con.getAutoCommit();
            con.setAutoCommit(false);
            con.commit(); //ensure a "clean" connection
        }
        Exception inner = null;
        try {
            importer.wipeDivisionData(con);
            if (isNonTX) con.commit();
            Statement stmt = con.createStatement();
            if (isNonTX) con.commit();
            try {
                importer.importLanguages(con, zip);
                if (isNonTX) con.commit();
                importer.importMandators(con, zip);
                if (isNonTX) con.commit();
                importer.importSecurity(con, zip);
                if (isNonTX) con.commit();
                importer.importWorkflows(con, zip);
                if (isNonTX) con.commit();
                importer.importConfigurations(con, zip);
                if (isNonTX) con.commit();
                importer.importBinaries(con, zip);
                if (isNonTX) con.commit();
                stmt.execute(getReferentialIntegrityChecksStatement(false));
                importer.importStructures(con, zip);
                if (isNonTX) con.commit();
                importer.importHierarchicalContents(con, zip);
                if (isNonTX) con.commit();
                importer.importScripts(con, zip);
                if (isNonTX) con.commit();
                importer.importTree(con, zip);
                if (isNonTX) con.commit();
                importer.importHistory(con, zip);
                if (isNonTX) con.commit();
                importer.importBriefcases(con, zip);
                if (isNonTX) con.commit();
                importer.importFlatStorages(con, zip, exportInfo);
                if (isNonTX) con.commit();
                importer.importSequencers(con, zip);
                if (isNonTX) con.commit();
            } catch (Exception e) {
                if (isNonTX) con.rollback();
                inner = e;
                throw e;
            } finally {
                if (isNonTX) con.commit();
                stmt.execute(getReferentialIntegrityChecksStatement(true));
            }
            if (isNonTX) con.commit();
            //rebuild fulltext index
            FulltextIndexer ft = StorageManager.getStorageImpl().getContentStorage(TypeStorageMode.Hierarchical).getFulltextIndexer(null, con);
            ft.rebuildIndex();
            if (isNonTX) con.commit();
        } catch (Exception e) {
            if (isNonTX) con.rollback();
            if (inner != null) {
                LOG.error(e);
                throw inner;
            }
            throw e;
        } finally {
            if (isNonTX) {
                con.commit();
                con.setAutoCommit(autoCommit);
                Database.closeObjects(GenericDBStorage.class, con, null);
            }
        }
    }

    /**
     * Get the DivisionImporter in use
     *
     * @return DivisionImporter
     */
    protected GenericDivisionImporter getDivisionImporter() {
        return GenericDivisionImporter.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    public FxDivisionExportInfo getDivisionExportInfo(ZipFile zip) throws FxApplicationException {
        return getDivisionImporter().getDivisionExportInfo(zip);
    }
}
