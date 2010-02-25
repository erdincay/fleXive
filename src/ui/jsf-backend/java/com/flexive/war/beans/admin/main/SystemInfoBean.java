/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.war.beans.admin.main;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.media.FxMediaEngine;
import org.apache.commons.lang.time.DurationFormatUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.management.ManagementFactory;
import java.util.Date;
import java.util.Formatter;
import java.io.Serializable;

/**
 * JSF Bean exposing miscellaneous system/runtime parameters.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SystemInfoBean implements Serializable {
    private static final long serialVersionUID = 7421671438164771880L;
    private static final Log LOG = LogFactory.getLog(SystemInfoBean.class);

    /**
     * Get the current date/time
     *
     * @return current date/time
     */
    public Date getDateTime() {
        return new Date();
    }

    /**
     * Get the java version
     *
     * @return java version
     */
    public String getJavaVersion() {
        return System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")";
    }

    /**
     * Get the number of available processors
     *
     * @return number of available processors
     */
    public int getProcessors() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Get amount of free memory
     *
     * @return amount of free memory
     */
    public String getFreeMemoryMB() {
        return new Formatter().format("%.2f MB (of %.2f MB, max. %.2f MB)",
                (double) Runtime.getRuntime().freeMemory() / 1024. / 1024.,
                (double) Runtime.getRuntime().totalMemory() / 1024. / 1024.,
                (double) Runtime.getRuntime().maxMemory() / 1024. / 1024.).toString();
    }

    /**
     * Get the operating system name and architecture
     *
     * @return operating system name and architecture
     */
    public String getOperatingSystem() {
        return System.getProperty("os.name") + " " + System.getProperty("os.arch");
    }

    /**
     * Is ImageMagick available?
     *
     * @return ImageMagick available?
     */
    public boolean isIMAvailable() {
        return FxMediaEngine.hasImageMagickInstalled();
    }

    /**
     * Get the installed version of ImageMagick
     *
     * @return installed version of ImageMagick
     */
    public String getIMVersion() {
        return FxMediaEngine.getImageMagickVersion();
    }

    /**
     * Does [fleXive] use ImageMagick to identify images?
     *
     * @return use ImageMagick to identify images?
     */
    public boolean isUseIMIdentify() {
        return FxMediaEngine.isImageMagickIdentifySupported();
    }

    /**
     * Get the name of the application server [fleXive] is running on
     *
     * @return application server
     */
    public String getApplicationServerName() {
        return FxSharedUtils.getApplicationServerName();
    }

    /**
     * Get information about the database used for the current division
     *
     * @return information about the database used for the current division
     */
    public String getDatabaseInfo() {
        return EJBLookup.getDivisionConfigurationEngine().getDatabaseInfo();
    }

    /**
     * Get information about the database jdbc driver used for the current division
     *
     * @return information about the database jdbc driver used for the current division
     */
    public String getDatabaseDriverInfo() {
        return EJBLookup.getDivisionConfigurationEngine().getDatabaseDriverInfo();
    }

    /**
     * Get the used database schema version
     *
     * @return used database schema version
     */
    public Long getDatabaseSchemaVersion() {
        try {
            return EJBLookup.getDivisionConfigurationEngine().get(SystemParameters.DB_VERSION);
        } catch (FxApplicationException e) {
            LOG.error(e);
            return -1L;
        }
    }

    /**
     * Returns true if the global configuration plugin is installed.
     *
     * @return true if the global configuration plugin is installed.
     * @since 3.1
     */
    public boolean isGlobalConfigurationPluginInstalled() {
        try {
            Class.forName("com.flexive.faces.beans.GlobalConfigBean");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Get the timestamp of the last tree modification
     *
     * @return timestamp of the last tree modification
     */
    public Date getTreeModificationTimestamp() {
        return new Date(CacheAdmin.getTreeModificationTimestamp());
    }

    /**
     * @return  the uptime as a formatted string.
     * @since 3.1
     */
    public String getUptime() {
        return DurationFormatUtils.formatDurationWords(
                ManagementFactory.getRuntimeMXBean().getUptime(),
                true,
                true
        );
    }
}
