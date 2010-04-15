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
package com.flexive.war.beans.admin.main;


import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.faces.messages.FxFacesMsgWarn;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.impex.FxDivisionExportInfo;

import java.io.File;
import java.io.Serializable;

/**
 * Division import/export
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
public class DivisionImportExportBean implements Serializable {
    private static final long serialVersionUID = 6097763705884318936L;

    private String source;
    private FxDivisionExportInfo exportInfo;
    private String status = "init";
    private boolean schemaMatch = false;
    private boolean flatStorageImportable = false;
    private boolean ignoreWarning = false;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public FxDivisionExportInfo getExportInfo() {
        return exportInfo;
    }

    public void setExportInfo(FxDivisionExportInfo exportInfo) {
        this.exportInfo = exportInfo;
    }

    public boolean isSchemaMatch() {
        return schemaMatch;
    }

    public void setSchemaMatch(boolean schemaMatch) {
        this.schemaMatch = schemaMatch;
    }

    public boolean isFlatStorageImportable() {
        return flatStorageImportable;
    }

    public void setFlatStorageImportable(boolean flatStorageImportable) {
        this.flatStorageImportable = flatStorageImportable;
    }

    public boolean isIgnoreWarning() {
        return ignoreWarning;
    }

    public void setIgnoreWarning(boolean ignoreWarning) {
        System.out.println("set boolean to "+ignoreWarning);
        this.ignoreWarning = ignoreWarning;
    }

    public void acceptWarning() {
        this.ignoreWarning = true;
    }

    /**
     * Examine the export file and get exportInfo
     */
    public void examineExport() {
        try {
            schemaMatch = false;
            flatStorageImportable = false;
            exportInfo = EJBLookup.getDivisionConfigurationEngine().getDivisionExportInfo(source);
            processWarnings();
            setStatus("examine");
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    /**
     * Process warnings for import and set faces messages if needed
     */
    private void processWarnings() {
        schemaMatch = exportInfo.getSchemaVersion() == FxSharedUtils.getDBVersion();
        boolean isMySQL = EJBLookup.getDivisionConfigurationEngine().getDatabaseInfo().toLowerCase().indexOf("mysql") >= 0;
        //MySQL can only import a flatstorage exported from a MySQL database ...
        flatStorageImportable = !(isMySQL && exportInfo.getDatabaseInfo().toLowerCase().indexOf("mysql") == -1);
        if(!schemaMatch)
            new FxFacesMsgWarn("DivisionImportExport.warning.schemaMismatch", exportInfo.getSchemaVersion(), FxSharedUtils.getDBVersion()).addToContext();
        if(!flatStorageImportable)
            new FxFacesMsgWarn("DivisionImportExport.warning.flatstorageNotImportable").addToContext();
    }


    /**
     * Import division
     */
    public void importDivision() {
        if( !ignoreWarning && (!schemaMatch || !flatStorageImportable)) {
            new FxFacesMsgErr("DivisionImportExport.warning.acceptWarnings").addToContext();
            processWarnings();
            setStatus("examine");
            return;
        }
        try {
            EJBLookup.getDivisionConfigurationEngine().importDivision(source);
            setStatus("import");
            new FxFacesMsgInfo("DivisionImportExport.import.success").addToContext();
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    /**
     * Export division
     */
    public void exportDivision() {
        try {
            EJBLookup.getDivisionConfigurationEngine().exportDivision(source);
            setStatus("export");
            new FxFacesMsgInfo("DivisionImportExport.export.success", new File(source).getAbsolutePath()).addToContext();
            examineExportedDivision();
        } catch (Exception e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    private void examineExportedDivision() {
        examineExport();
        setStatus("exportSuccess");
    }
}
