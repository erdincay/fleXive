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
package com.flexive.shared.impex;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * Various information about an exported division
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxDivisionExportInfo implements Serializable {
    private int divisionId;
    private int schemaVersion;
    private int buildVersion;
    private String buildInfo;
    private String appServerInfo;
    private String databaseInfo;
    private String databaseDriverInfo;
    private String domainMatcher;
    private List<String> drops;
    private String exportUser;
    private java.util.Date exportDate;

    public FxDivisionExportInfo(int divisionId, int schemaVersion, int buildVersion, String buildInfo,
                                String appServerInfo, String databaseInfo, String databaseDriverInfo,
                                String domainMatcher, List<String> drops, String exportUser, Date exportDate) {
        this.divisionId = divisionId;
        this.schemaVersion = schemaVersion;
        this.buildVersion = buildVersion;
        this.buildInfo = buildInfo;
        this.appServerInfo = appServerInfo;
        this.databaseInfo = databaseInfo;
        this.databaseDriverInfo = databaseDriverInfo;
        this.domainMatcher = domainMatcher;
        this.drops = drops;
        this.exportUser = exportUser;
        this.exportDate = exportDate;
    }

    public int getDivisionId() {
        return divisionId;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public int getBuildVersion() {
        return buildVersion;
    }

    public String getBuildInfo() {
        return buildInfo;
    }

    public String getAppServerInfo() {
        return appServerInfo;
    }

    public String getDatabaseInfo() {
        return databaseInfo;
    }

    public String getDatabaseDriverInfo() {
        return databaseDriverInfo;
    }

    public String getDomainMatcher() {
        return domainMatcher;
    }

    public List<String> getDrops() {
        return drops;
    }

    public String getExportUser() {
        return exportUser;
    }

    public Date getExportDate() {
        return exportDate;
    }
}
