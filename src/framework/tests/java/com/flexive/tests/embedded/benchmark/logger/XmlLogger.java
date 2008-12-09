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

import com.ociweb.xml.WAX;

import java.io.*;
import java.util.Formatter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Result logger with XML output.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class XmlLogger extends AbstractResultLogger {
    private static final Log LOG = LogFactory.getLog(XmlLogger.class);

    private static final String OUTPUT_DIRECTORY = "reports";
    private static final String OUTPUT_FILE = "benchmark-results.xml";

    private final Writer out = new StringWriter();
    private final WAX wax = new WAX(out);
    private boolean active = true;

    public XmlLogger() {
        wax.start("results");
    }

    /** {@inheritDoc} */
    @Override
    protected synchronized void logResult(String name, double value, String measurement, String unit) {
        assert active : "ResultLogger invoked after getOutput has been called";
        wax.start("result")
           .child("name", name)
           .child("value", new Formatter().format("%.5f", value).toString())
           .child("unit", unit)
           .child("measurement", measurement)
           .end();
    }

    /** {@inheritDoc} */
    public synchronized String getOutput() {
        if (active) {
            active = false;
            wax.close();

            // also write result to XML file
            if (!new File(OUTPUT_DIRECTORY).mkdirs()) {
                // ignore
            }
            final String targetName = OUTPUT_DIRECTORY + File.separator + OUTPUT_FILE;
            LOG.info("Writing XML benchmark results to " + targetName);
            final File f = new File(targetName);
            try {
                final FileWriter fw = new FileWriter(f);
                fw.write(out.toString());
                fw.close();
            } catch (IOException e) {
                throw new IllegalStateException(e);
            } 
        }
        return out.toString();
    }
}
