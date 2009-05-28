/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.war.beans.admin;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.war.filter.VersionUrlFilter;

import java.io.Serializable;

/**
 * Provides access to miscellaneous backend configuration settings.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AdminConfigurationBean implements Serializable {
    private static final long serialVersionUID = 5407293253771169788L;

    private Boolean treeLiveEnabled;
    private Boolean binaryTransitDB;
    private String binaryTransitPath;
    private String exportURLprefix;

    /**
     * Updates the system configuration. Since all getters should use the actual configuration
     * as fallback, only parameters that are actually set will be updated.
     */
    public void updateConfiguration() {
        try {
            EJBLookup.getConfigurationEngine().putInSource(SystemParameters.TREE_LIVE_ENABLED, isTreeLiveEnabled());
            EJBLookup.getConfigurationEngine().putInSource(SystemParameters.BINARY_TRANSIT_DB, isBinaryTransitDB());
            EJBLookup.getDivisionConfigurationEngine().put(SystemParameters.EXPORT_DOWNLOAD_URL, getExportURLprefix());
            EJBLookup.getNodeConfigurationEngine().put(SystemParameters.NODE_TRANSIT_PATH, getBinaryTransitPath());
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    /**
     * Returns true if the live tree is available.
     *
     * @return true if the live tree is available.
     * @throws FxApplicationException on system errors
     * @see SystemParameters#TREE_LIVE_ENABLED
     */
    public boolean isTreeLiveEnabled() throws FxApplicationException {
        if (treeLiveEnabled == null)
            treeLiveEnabled = EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_LIVE_ENABLED);
        return treeLiveEnabled;
    }

    public void setTreeLiveEnabled(boolean treeLiveEnabled) {
        this.treeLiveEnabled = treeLiveEnabled;
    }

    /**
     * Returns true if binary transits are stored in the database
     *
     * @return true if binary transits are stored in the database
     * @throws FxApplicationException on system errors
     * @see SystemParameters#BINARY_TRANSIT_DB
     */
    public boolean isBinaryTransitDB() throws FxApplicationException {
        if (binaryTransitDB == null)
            binaryTransitDB = EJBLookup.getConfigurationEngine().get(SystemParameters.BINARY_TRANSIT_DB);
        return binaryTransitDB;
    }

    /**
     * Returns the path on the local filesystem for binary transit files
     *
     * @return path on the local filesystem for binary transit files
     * @throws FxApplicationException on system errors
     * @see SystemParameters#NODE_TRANSIT_PATH
     */
    public String getBinaryTransitPath() throws FxApplicationException {
        if (binaryTransitPath == null)
            binaryTransitPath = EJBLookup.getNodeConfigurationEngine().get(SystemParameters.NODE_TRANSIT_PATH);
        return binaryTransitPath;
    }

    public void setBinaryTransitDB(boolean binaryTransitDB) {
        this.binaryTransitDB = binaryTransitDB;
    }

    public void setBinaryTransitPath(String binaryTransitPath) {
        this.binaryTransitPath = binaryTransitPath;
    }

    public String getExportURLprefix() throws FxApplicationException {
        if (exportURLprefix == null)
            exportURLprefix = EJBLookup.getDivisionConfigurationEngine().get(SystemParameters.EXPORT_DOWNLOAD_URL);
        return exportURLprefix;
    }

    public void setExportURLprefix(String exportURLprefix) {
        this.exportURLprefix = exportURLprefix;
    }

    /**
     * Inserts the current flexive version into the given URL. Available in EL via
     * {@code #{adm:versionedUrl(String)}.
     *
     * @param url the url to version
     * @return the URL with the current version
     */
    public static String getVersionedUrl(String url) {
        final int pos = Math.max(url.lastIndexOf('.'), url.lastIndexOf('/'));
        return pos != -1
                ? url.substring(0, pos) + VersionUrlFilter.URL_PATTERN + url.substring(pos)
                : VersionUrlFilter.URL_PATTERN + url;
    }
}
