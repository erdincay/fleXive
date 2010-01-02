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
package com.flexive.tests.embedded.benchmark;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.structure.FxEnvironment;
import static com.flexive.tests.embedded.benchmark.FxBenchmarkUtils.getResultLogger;
import org.testng.annotations.Test;

/**
 * Structure-related benchmarks (FxEnvironment and structure classes).
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = "benchmark", enabled = true)
public class StructureBenchmark {

    /**
     * A simple benchmark that reloads the entire structure environment
     * several times and returns the average load time.
     * @throws Exception on errors
     */
    public void benchStructureReload() throws Exception {
        FxContext.get().runAsSystem();
        try {
            CacheAdmin.reloadEnvironment(); // warm up
            CacheAdmin.reloadEnvironment();
            long start = System.currentTimeMillis();
            for (int i = 0; i < 200; i++) {
                CacheAdmin.reloadEnvironment();
            }
            getResultLogger().logTime("reloadEnvironment", start, 200, "reload");
        } finally {
            FxContext.get().stopRunAsSystem();
        }
    }

    public void benchGetAssignmentByXPath() {
        final String[] xpaths = {
                "article/id",
                "contactdata/surname",
                "image/step",
                "folder/acl"
        };
        // warm up
        final FxEnvironment env = CacheAdmin.getEnvironment();
        fetchAssignments(env, xpaths, 10000);

        // do benchmark
        final long start = System.currentTimeMillis();
        fetchAssignments(env, xpaths, 100000);
        getResultLogger().logTime("getAssignmentByXPath", start, 100000, "lookup");
    }

    private void fetchAssignments(FxEnvironment environment, String[] xpaths, int times) {
        for (int i = 0; i < times; i++) {
            for (String xpath: xpaths) {
                environment.getAssignment(xpath);
            }
        }
    }

    public void benchGetAssignmentById() {
        final FxEnvironment env = CacheAdmin.getEnvironment();
        final long[] assignmentIds = {
                env.getAssignment("article/id").getId(),
                env.getAssignment("contactdata/surname").getId(),
                env.getAssignment("image/step").getId(),
                env.getAssignment("folder/acl").getId()
        };
        // warm up
        fetchAssignmentsById(env, assignmentIds, 10000);

        // do benchmark
        final long start = System.currentTimeMillis();
        fetchAssignmentsById(env, assignmentIds, 100000);
        getResultLogger().logTime("getAssignmentById", start, 100000, "lookup");
    }

    private void fetchAssignmentsById(FxEnvironment environment, long[] ids, int times) {
        for (int i = 0; i < times; i++) {
            for (long id : ids) {
                environment.getAssignment(id);
            }
        }
    }
}
