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
package com.flexive.tests.embedded.benchmark.logger;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * <p>Logs to output files suitable for the Hudson Plot plugin.</p>
 *
 * <p>Each result will be stored in its own file, which can be used as dataseries file in the
 * plot plugin.</p>
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class HudsonPlotLogger extends PlainTextLogger {
    private static final Log LOG = LogFactory.getLog(HudsonPlotLogger.class);
    private static final String OUTPUT_DIRECTORY = "reports/benchmark/plot";

    @Override
    protected void logResult(String name, double time, String measurement, String unit) {
        super.logResult(name, time, measurement, unit);
        final String filename = OUTPUT_DIRECTORY
                + File.separator
                + StringUtils.replace(name, File.separator, "_")
                + ".properties";
        if (!new File(OUTPUT_DIRECTORY).exists() && !new File(OUTPUT_DIRECTORY).mkdirs()) {
            LOG.warn("Failed to create output directory " + OUTPUT_DIRECTORY);
            return;
        }
        FileWriter fw = null;
        try {
            fw = new FileWriter(new File(filename));
            fw.write("YVALUE=" + time + "\n");
        } catch (IOException e) {
            LOG.error("Failed to write result file: " + e.getMessage(), e);
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    LOG.error("Failed to close file: " + e.getMessage(), e);
                }
            }
        }
    }
}
