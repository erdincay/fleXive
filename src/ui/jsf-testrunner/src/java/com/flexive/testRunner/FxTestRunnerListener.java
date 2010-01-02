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

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestNG listener to notify the callback
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class FxTestRunnerListener implements ITestListener {
    private FxTestRunnerCallback callback;

    /**
     * Ctor
     *
     * @param callback the callback to notify
     */
    public FxTestRunnerListener(FxTestRunnerCallback callback) {
        this.callback = callback;
    }

    /**
     * {@inheritDoc}
     */
    public void onTestStart(ITestResult iTestResult) {
//        System.out.println("==> Started " + iTestResult.getName() + " [" + iTestResult.getMethod().getMethodName() + "]");
        if (callback != null)
            callback.started(iTestResult);
    }

    /**
     * {@inheritDoc}
     */
    public void onTestSuccess(ITestResult iTestResult) {
//        System.out.println("==> Success " + iTestResult.getName() + " [" + iTestResult.getMethod().getMethodName() + "]");
        if (callback != null)
            callback.addSuccess(iTestResult);
    }

    /**
     * {@inheritDoc}
     */
    public void onTestFailure(ITestResult iTestResult) {
//        System.out.println("==> Failure " + iTestResult.getName() + " [" + iTestResult.getMethod().getMethodName() + "]");
        if (callback != null)
            callback.addFailure(iTestResult);
    }

    /**
     * {@inheritDoc}
     */
    public void onTestSkipped(ITestResult iTestResult) {
//        System.out.println("==> Skipped " + iTestResult.getName() + " [" + iTestResult.getMethod().getMethodName() + "]");
        if (callback != null)
            callback.addSkipped(iTestResult);
    }

    /**
     * {@inheritDoc}
     */
    public void onTestFailedButWithinSuccessPercentage(ITestResult iTestResult) {
    }

    /**
     * {@inheritDoc}
     */
    public void onStart(ITestContext iTestContext) {
    }

    /**
     * {@inheritDoc}
     */
    public void onFinish(ITestContext iTestContext) {
    }
}
