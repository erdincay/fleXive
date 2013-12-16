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
package com.flexive.faces.beans.admin;

import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.testRunner.FxTestRunner;
import com.flexive.testRunner.FxTestRunnerCallback;
import org.testng.ITestResult;


/**
 * JSF beans providing a simple runner for our TestNG tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class TestRunnerBean implements FxTestRunnerCallback {
    protected boolean running = false;
    protected int testSuccess = 0;
    protected int testFailure = 0;
    protected int testSkipped = 0;
    protected ITestResult currentTest = null;
    private String outputPath = null;
    protected boolean resultsAvailable = false;

    @Override
    public void started(ITestResult iTestResult) {
        this.currentTest = iTestResult;
    }

    @Override
    public void addFailure(ITestResult iTestResult) {
        testFailure++;
    }

    @Override
    public void addSuccess(ITestResult iTestResult) {
        testSuccess++;
    }

    @Override
    public void addSkipped(ITestResult iTestResult) {
        testSkipped++;
    }

    @Override
    public void setRunning(boolean state) {
        this.running = state;
    }

    public String getCurrentTest() {
        return currentTest == null ? "-none-" : currentTest.getTestClass().getName() + "." + currentTest.getMethod().getMethodName();
    }

    @Override
    public void resetTestInfo() {
        testSuccess = 0;
        testSkipped = 0;
        testFailure = 0;
        currentTest = null;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isResultsAvailable() {
        return resultsAvailable;
    }

    public int getTestSuccess() {
        return testSuccess;
    }

    public int getTestFailure() {
        return testFailure;
    }

    public int getTestSkipped() {
        return testSkipped;
    }

    public String getOutputPath() {
        return outputPath;
    }

    public void setOutputPath(String outputPath) {
        if(outputPath.endsWith("/")) {
            outputPath = outputPath.substring(0, outputPath.length() - 2);
        }
        this.outputPath = outputPath;
    }

    @Override
    public void setResultsAvailable(boolean resultsAvailable) {
        this.resultsAvailable = resultsAvailable;
    }

    public String runTests() {
        if (!isRunning()) {
            if (!FxTestRunner.checkTestConditions(getOutputPath(), true))
                return null;
            setResultsAvailable(false);
            FxTestRunner.runTests(this, getOutputPath());
            new FxFacesMsgInfo("TestRunner.nfo.running").addToContext();
        } else
            new FxFacesMsgInfo("TestRunner.nfo.progress").addToContext();
        return null;
    }
}
