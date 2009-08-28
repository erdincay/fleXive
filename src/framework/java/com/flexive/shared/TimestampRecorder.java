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
package com.flexive.shared;

import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.io.Serializable;

/**
 * An utility class to record timestamps during execution of a method and print (debugging) benchmark
 * information.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class TimestampRecorder implements Serializable {
    private static final long serialVersionUID = 8965004786783826514L;

    private long startNanos;
    private long totalNanos;
    private final List<Pair<String, Long>> timestamps = new ArrayList<Pair<String, Long>>();

    /**
     * Resets the timer to begin recording.
     */
    public void begin() {
        timestamps.clear();
        startNanos = System.nanoTime();
        totalNanos = 0;
    }

    /**
     * Add a timestamp with the given name. The execution time of atimestamp
     * is the time elapsed since the last call to timestamp or begin, if no timestamp was
     * recorded yet.
     *
     * @param name  Timestamp name which will be included in the string representation
     * @return      the time taken for the last task, in nanoseconds
     */
    public long timestamp(String name) {
        final long nanos = System.nanoTime() - totalNanos - startNanos;
        timestamps.add(new Pair<String, Long>(name, nanos));
        totalNanos += nanos;
        return nanos;
    }

    /**
     * Returns the list of timestamps in nanosecond precision.
     *
     * @return  the list of timestamps in nanosecond precision.
     */
    public List<Pair<String, Long>> getTimestamps() {
        return Collections.unmodifiableList(timestamps);
    }

    @Override
    public String toString() {
        final List<String> detailTimes = new ArrayList<String>(timestamps.size());
        for (Pair<String, Long> timestamp : timestamps) {
            detailTimes.add(String.format("%s: %.2fms", timestamp.getFirst(), timestamp.getSecond() / 1000000.0));
        }
        return String.format("Total execution time: %.2fms. %s", totalNanos / 1000000.0, StringUtils.join(detailTimes, ", "));
    }
}
