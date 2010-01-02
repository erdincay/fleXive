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
package com.flexive.tests.embedded.benchmark.logger;

/**
 * Logger for benchmark results. The implementation can be selected with the
 * {@code flexive.benchmark.resultlogger} system property (specify the fully qualified classname).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public interface ResultLogger {
    /**
     * Log a benchmark result.
     *
     * @param name  unique name of the result
     * @param startTimeMillis   start time of the benchmark
     * @param factor    number of method invocations (used to scale down the resulting number)
     * @param measurement   the human-readable measured action (e.g. "content creation")
     */
    void logTime(String name, long startTimeMillis, int factor, String measurement);

    /**
     * Returns the accumulated output of the logged results. The logger should not be used
     * for logging after a call to this method.
     *
     * @return  the accumulated output of the logged results.
     */
    String getOutput();
}
