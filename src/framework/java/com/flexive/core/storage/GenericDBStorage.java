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
import com.flexive.core.storage.genericSQL.GenericLockStorage;
import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.impex.FxDivisionExportInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.OutputStream;
import java.sql.Connection;
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
    @Override
    public String getBooleanExpression(boolean flag) {
        return flag ? TRUE : FALSE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBooleanTrueExpression() {
        return TRUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBooleanFalseExpression() {
        return FALSE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LockStorage getLockStorage() {
        return GenericLockStorage.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String escapeReservedWords(String query) {
        return query; //nothing to escape
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
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
    @Override
    public String getFromDual() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getLimitOffsetVar(String var, boolean hasWhereClause, long limit, long offset) {
        return getLimitOffset(hasWhereClause, limit, offset);
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public String formatDateCondition(java.util.Date date) {
        return "'" + FxFormatUtils.getDateTimeFormat().format(date) + "'";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String escapeFlatStorageColumn(String column) {
        return column;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getSelectListItemReferenceFixStatement() {
        return "UPDATE " + TBL_STRUCT_SELECTLIST_ITEM + " SET PARENTID=? WHERE PARENTID IN (SELECT p.ID FROM " +
                TBL_STRUCT_SELECTLIST_ITEM + " p WHERE p.LISTID=?)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
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
    @Override
    public void importDivision(Connection con, ZipFile zip) throws Exception {
        GenericDivisionImporter importer = GenericDivisionImporter.getInstance();
        FxDivisionExportInfo exportInfo = importer.getDivisionExportInfo(zip);
        if (FxSharedUtils.getDBVersion() != exportInfo.getSchemaVersion()) {
            LOG.warn("DB Version mismatch! Current:" + FxSharedUtils.getDBVersion() +
                    ", exported schema:" + exportInfo.getSchemaVersion());
        }
        importer.wipeDivisionData(con);
        importer.importLanguages(con, zip);
        importer.importMandators(con, zip);
        importer.importSecurity(con, zip);
        importer.importWorkflows(con, zip);
        importer.importConfigurations(con, zip);
        importer.importBinaries(con, zip);
        importer.importStructures(con, zip);
        importer.importHierarchicalContents(con, zip);
        importer.importScripts(con, zip);
        importer.importTree(con, zip);
        importer.importHistory(con, zip);
        importer.importBriefcases(con, zip);
        importer.importFlatStorages(con, zip);
        importer.importSequencers(con, zip);
    }
}
