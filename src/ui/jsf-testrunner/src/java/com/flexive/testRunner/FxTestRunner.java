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
package com.flexive.testRunner;

import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.configuration.DivisionData;
import com.flexive.shared.exceptions.FxApplicationException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * Test runner to execute tests in a separate thread.
 * Intended to be used from within an ear
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxTestRunner {
    private static final Log LOG = LogFactory.getLog(FxTestRunner.class);

    /**
     * Test classes to ignore
     */
    protected static String[] ignoreTests = {
            "DistPackageTest.class",
            "TutorialTest.class", //classloading issue??
            "ImageParserTest.class" //relies on external files which are not available here
    };

    /**
     * Test packages to ignore
     */
    protected static String[] ignorePackages = {
            "/browser/",   //requires selenium
            "/benchmark/", //no benchmarks in ear tests, only executed in embedded container!
            "/disttools/", //no distribution tests (not available in ear)
    };

    /**
     * Check if all conditions are met to run tests
     *
     * @param outputPath           output path for the test reports
     * @param addMessagesToContext add errors/warnings to the faces context as messages?
     * @return tests can be run
     */
    public static Boolean checkTestConditions(String outputPath, Boolean addMessagesToContext) {
        boolean hasTestDivision;
        boolean ok = true;
        try {
            DivisionData dd = EJBLookup.getGlobalConfigurationEngine().getDivisionData(DivisionData.DIVISION_TEST);
            hasTestDivision = dd.isAvailable();
        } catch (FxApplicationException e) {
            hasTestDivision = false;
        }
        if (!hasTestDivision) {
            if (addMessagesToContext)
                new FxFacesMsgErr("TestRunner.err.testDivision").addToContext();
            ok = false;
        }
        /* tests should run without assert enabled now using only testNG's Assert
        @SuppressWarnings({"UnusedAssignment"}) boolean assertsEnabled = false;
        assert assertsEnabled = true;
        if (!assertsEnabled) {
            if (addMessagesToContext)
                new FxFacesMsgErr("TestRunner.err.asserts").addToContext();
            ok = false;
        }*/
        if (StringUtils.isEmpty(outputPath)) {
            if (addMessagesToContext)
                new FxFacesMsgErr("TestRunner.err.path").addToContext();
            ok = false;
        }
        File path = new File(outputPath);
        if (!path.exists() && !path.mkdirs()) {
            if (addMessagesToContext)
                new FxFacesMsgErr("TestRunner.err.path").addToContext();
            ok = false;
        }
        return ok;
    }

    /**
     * Runs tests - this is a "fire and forget" function, callers have to make sure to not call this more than once!
     *
     * @param callback   the callback for results
     * @param outputPath test report output path
     */
    public static void runTests(FxTestRunnerCallback callback, String outputPath) {
        if (FxTestRunnerThread.isTestInProgress()) {
            LOG.error("A test is currently running. Tried to start another run!");
            return;
        }
        Thread t = new FxTestRunnerThread(callback, outputPath);
        t.setDaemon(true);
        if (callback != null)
            callback.setRunning(true);
        t.start();
    }

    /**
     * Commodity function to check if there is a running test in progress
     *
     * @return running test in progress
     */
    public static boolean isTestInProgress() {
        return FxTestRunnerThread.isTestInProgress();
    }

    /**
     * Runs tests - this is a "fire and forget" function, callers have to make sure to not call this more than once!
     * This method is intended to be called if no callback is needed/used
     *
     * @param outputPath test report output path
     */
    public static void runTests(String outputPath) {
        runTests(null, outputPath);
    }
}
