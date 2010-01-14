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

import java.io.OutputStream;
import java.sql.Connection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.flexive.core.DatabaseConst.TBL_SELECTLIST_ITEM;

/**
 * Database vendor specific storage, common implementations for all storages
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public abstract class GenericDBStorage implements DBStorage {

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
        return "UPDATE " + TBL_SELECTLIST_ITEM + " SET PARENTID=? WHERE PARENTID IN (SELECT p.ID FROM " +
                TBL_SELECTLIST_ITEM + " p WHERE p.LISTID=?)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportDivision(Connection con, OutputStream out) throws Exception {
        ZipOutputStream zip = new ZipOutputStream(out);
        GenericDivisionExporter exporter = GenericDivisionExporter.getInstance();
        StringBuilder sb = new StringBuilder(10000);
        try {
            ZipEntry ze = new ZipEntry("languages.xml");
            zip.putNextEntry(ze);
            exporter.exportLanguages(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("mandators.xml");
            zip.putNextEntry(ze);
            exporter.exportMandators(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("security.xml");
            zip.putNextEntry(ze);
            exporter.exportSecurity(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("workflows.xml");
            zip.putNextEntry(ze);
            exporter.exportWorkflows(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("configurations.xml");
            zip.putNextEntry(ze);
            exporter.exportConfigurations(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("structures.xml");
            zip.putNextEntry(ze);
            exporter.exportStructures(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("flatstorage.xml");
            zip.putNextEntry(ze);
            exporter.exportFlatStorageMeta(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("tree.xml");
            zip.putNextEntry(ze);
            exporter.exportTree(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("briefcases.xml");
            zip.putNextEntry(ze);
            exporter.exportBriefcases(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("scripts.xml");
            zip.putNextEntry(ze);
            exporter.exportScripts(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("history.xml");
            zip.putNextEntry(ze);
            exporter.exportHistory(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("data_hierarchical.xml");
            zip.putNextEntry(ze);
            exporter.exportHierarchicalStorage(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("data_flat.xml");
            zip.putNextEntry(ze);
            exporter.exportFlatStorages(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("sequencers.xml");
            zip.putNextEntry(ze);
            exporter.exportSequencers(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            ze = new ZipEntry("flexive.xml");
            zip.putNextEntry(ze);
            exporter.exportBuildInfos(con, zip, sb);
            zip.closeEntry();
            zip.flush();
            exporter.exportBinaries("binaries.xml", con, zip, sb);
        } finally {
            zip.finish();
        }
    }
}
