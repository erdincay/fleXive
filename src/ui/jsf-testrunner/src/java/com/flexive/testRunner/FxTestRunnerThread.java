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
package com.flexive.testRunner;

import com.flexive.faces.messages.FxFacesMsgErr;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * Thread executing the tests
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxTestRunnerThread extends Thread {
    private static final Log LOG = LogFactory.getLog(FxTestRunnerThread.class);

    private final static Object lock = new Object();

    private static boolean testInProgress = false;

    private FxTestRunnerCallback callback;
    private String outputPath;

    /**
     * Is a test currently in progress?
     *
     * @return if test currently in progress
     */
    public static boolean isTestInProgress() {
        synchronized (lock) {
            return testInProgress;
        }
    }

    /**
     * Ctor
     *
     * @param callback   callback for notifications
     * @param outputPath path for the test reports
     */
    public FxTestRunnerThread(FxTestRunnerCallback callback, String outputPath) {
        this.callback = callback;
        this.outputPath = outputPath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run() {
        synchronized (lock) {
            if (testInProgress)
                return;
            testInProgress = true;
        }
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            URL jar = cl.getResource("lib/flexive-tests.jar");

            //build a list of all test classes
            List<Class> testClasses = new ArrayList<Class>(100);
            try {
                JarInputStream jin = new JarInputStream(jar.openStream());
                while (jin.available() != 0) {
                    JarEntry je = jin.getNextJarEntry();
                    if (je == null)
                        continue;

                    final String name = je.getName();
                    //only classes, no inner classes, abstract or mock classes
                    if (name.endsWith(".class") && !(name.indexOf('$') > 0) &&
                            !(name.indexOf("Abstract") > 0) &&
                            !(name.indexOf("Mock") > 0)) {
                        boolean ignore = false;
                        //check ignore package
                        for (String pkg : FxTestRunner.ignorePackages)
                            if (name.indexOf(pkg) > 0) {
                                ignore = true;
                                break;
                            }
                        if (ignore) continue;
                        final String className = name.substring(name.lastIndexOf('/') + 1);
                        //check ignore classes
                        for (String cls : FxTestRunner.ignoreTests)
                            if (className.indexOf(cls) > 0) {
                                ignore = true;
                                break;
                            }
                        if (ignore) continue;
                        final String fqn = name.replaceAll("\\/", ".").substring(0, name.lastIndexOf('.'));
                        try {
                            testClasses.add(Class.forName(fqn));
                        } catch (ClassNotFoundException e) {
                            LOG.error("Could not find test class: " + fqn);
                        }
                    }
                }
            } catch (IOException e) {
                LOG.error(e);
            }
            TestNG testng = new TestNG();
            testng.setTestClasses(testClasses.toArray(new Class[testClasses.size()]));
            // skip.ear groups have to be excluded, else tests that include these will be skipped as well (like ContainerBootstrap which is needed)
            testng.setExcludedGroups("skip.ear");
            System.setProperty("flexive.tests.ear", "1");
            TestListenerAdapter tla = new TestListenerAdapter();
            testng.addListener(tla);

            if (callback != null) {
                FxTestRunnerListener trl = new FxTestRunnerListener(callback);
                testng.addListener(trl);
            }

            testng.setThreadCount(4);
            testng.setVerbose(0);
            testng.setDefaultSuiteName("EARTestSuite");
            testng.setOutputDirectory(outputPath);
            if (callback != null)
                callback.resetTestInfo();
            try {
                testng.run();
            } catch (Exception e) {
                LOG.error(e);
                new FxFacesMsgErr("TestRunner.err.testNG", e.getMessage()).addToContext();
            }
            if (callback != null) {
                callback.setRunning(false);
                callback.setResultsAvailable(true);
            }
        } finally {
            synchronized (lock) {
                testInProgress = false;
            }
        }
    }
}
