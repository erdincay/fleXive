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
package com.flexive.tests.shared;

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxRuntimeException;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Test for the localized flexive exception messages
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"shared"})
public class ExceptionTest {

    /**
     * Quick check that the exception hierarchy works
     */
    @Test
    public void exceptionHierarchy() {
        try {
            FxApplicationException e = new FxApplicationException("ex.test.params.1");
            throw e;
        } catch (FxApplicationException e) {
            // pass
        }
        try {
            FxRuntimeException e = new FxApplicationException("ex.test.params.1").asRuntimeException();
            throw e;
        } catch (RuntimeException e) {
            // pass
        }
    }

    /**
     * Checks if the stack trace of a FxRuntimeException points to the original
     * throw, not the .asRuntimeException method.
     */
    @Test
    public void runtimeExceptionStackTrace() {
        try {
            throwsRuntimeException();
        } catch (RuntimeException e) {
            StackTraceElement[] stackTrace = e.getStackTrace();
            Assert.assertTrue("throwsRuntimeException".equals(stackTrace[0].getMethodName()), "First stack trace element of wrapped application exception should be the original method call.");
        }
    }

    private void throwsRuntimeException() {
        throw new FxApplicationException("ex.test.params.1").asRuntimeException();
    }
}
