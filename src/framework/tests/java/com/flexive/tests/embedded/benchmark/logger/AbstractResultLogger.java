/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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

/**
 * Base result logger implementation.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
*/
public abstract class AbstractResultLogger implements ResultLogger {

    /**
     * Log a benchmark result.
     *
     * @param name  unique name of the result
     * @param value the value to be logged
     * @param measurement   the human-readable measured action (e.g. "content creation")
     * @param unit  the human-readable result unit (e.g. "ms")
     */
    protected abstract void logResult(String name, double value, String measurement, String unit);

    /** {@inheritDoc} */
    @Override
    public synchronized void logTime(String name, long startTimeMillis, int factor, String measurement) {
        logResult(name, getLoggedResult(startTimeMillis, factor), measurement, "ms");
    }


    protected double getLoggedResult(long startTimeMillis, int factor) {
        return ((double) System.currentTimeMillis() - startTimeMillis) / factor;
    }

}
