/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.core.storage.binary;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Helper class to encapsulate information about a file in the binary transit space
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class FxBinaryUtils {

    private static final Log LOG = LogFactory.getLog(FxBinaryUtils.class);

    static final String TRANSIT_EXT = ".fxt";

    /**
     * Get the transit directory for the current node
     *
     * @return transit directory for the current node
     */
    public static String getTransitDirectory() {
        String path;
        try {
            path = EJBLookup.getNodeConfigurationEngine().get(SystemParameters.NODE_TRANSIT_PATH);
        } catch (FxApplicationException e) {
            LOG.error(e);
            path = "~" + File.separatorChar + "flexive" + File.separatorChar + "transit";
        }
        File dir = new File(expandPath(path));
        if (!dir.exists() && !dir.mkdirs()) {
            LOG.error("Could not create directory: " + dir.getAbsolutePath());
        }
        return dir.getAbsolutePath();
    }

    /**
     * Expand the path by replacing '~' with the user home directory and fix file separator chars
     *
     * @param path the path to expand
     * @return expanded path
     */
    private static String expandPath(String path) {
        //fix file separators
        if (File.separatorChar == '/')
            path = path.replace('\\', '/');
        else
            path = path.replace('/', '\\');
        //expand ~ to user home
        if (path.indexOf("~") >= 0)
            path = path.replaceAll("~", System.getProperty("user.home"));
        return path;
    }

    /**
     * Create a new binary transit file for the given division and handle
     *
     * @param divisionId division
     * @param handle     binary handle
     * @param ttl        time to live (will be part of the filename)
     * @return transit file
     * @throws IOException on errors
     */
    public static File createTransitFile(int divisionId, String handle, long ttl) throws IOException {
        File baseDir = new File(getTransitDirectory() + File.separatorChar + String.valueOf(divisionId));
        if (!baseDir.exists())
            if (!baseDir.mkdirs())
                throw new IOException("Failed to create directory " + baseDir.getAbsolutePath());
        File result = new File(baseDir.getAbsolutePath() + File.separatorChar + handle + "__" + String.valueOf(ttl) + TRANSIT_EXT);
        if (!result.createNewFile())
            throw new IOException("Failed to create file " + result.getAbsolutePath());
        return result;
    }

    /**
     * Get an existing transit file for the given division and handle
     *
     * @param divisionId division
     * @param handle     binary handle
     * @return the transit file or <code>null</code> if not found
     */
    public static File getTransitFile(int divisionId, final String handle) {
        File dir = new File(getTransitDirectory() + File.separatorChar + String.valueOf(divisionId) + File.separatorChar);
        File[] result = dir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.startsWith(handle + "__");
            }
        });
        if (result.length > 0)
            return result[0];
        return null;
    }

    /**
     * Remove all expired transit files for the given division
     *
     * @param divisionId division
     * @return number of expired and removed transit files
     */
    public static int removeExpiredTransitFiles(int divisionId) {
        File baseDir = new File(getTransitDirectory() + File.separatorChar + String.valueOf(divisionId));
        if (!baseDir.exists())
            return 0; //nothing to do
        final long now = System.currentTimeMillis();
        int count = 0;
        for (File expired : baseDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                try {
                    if (name.endsWith(TRANSIT_EXT) && name.indexOf("__") > 0) {
                        long ttl = Long.valueOf(name.split("__")[1].split("\\.")[0]);
                        if (new File(dir.getAbsolutePath() + File.separator + name).lastModified() + ttl < now)
                            return true;
                    }
                } catch (Exception e) {
                    LOG.error(e);
                    return false;
                }
                return false;
            }
        })) {
            count++;
            if (!expired.delete())
                expired.deleteOnExit();
        }
        return count;
    }
}
