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
package com.flexive.tests.browser;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;

import static org.testng.Assert.fail;

/**
 * Tests related to Mandators
 *
 * @author Laszlo Hernadi (laszlo.hernadi@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev: 462 $
 */
public class AdmMandatorTest extends AbstractBackendBrowserTest {

    private final static boolean SKIP_CLASS = !AbstractBackendBrowserTest.isForceAll();
    private final static Log LOG = LogFactory.getLog(AdmMandatorTest.class);

    private final static String [] MANDATORS = {"mand01", "mand02", "mand03"};

    @Override
    protected boolean doSkip() {
        return SKIP_CLASS;
    }

    /*
     * Mandators	AM_1
     *      Log in as supervisor.	                        Logged out.	                Logged in as supervisor.
     *      Create first mandator "mand01". Check active.	Logged in as supervisor.	Mandator created
     *      Create second mandator "mand02". Check active.	Logged in as supervisor.	Mandator created
     *      Rename mandator "mand01" to "mand02".	        Logged in as supervisor.	Error
     *      Rename first mandator to "mand03".	            Logged in as supervisor.	Mandator renamed
     *      Rename first mandator to "mand01".	            Logged in as supervisor.	Mandator renamed
     *      Create mandator named "mand01".	                Logged in as supervisor.	Error
    /**/
    public void mandators_AM_1() {
        if (SKIP_CLASS) {
            skipMe();
            return;
        }
        createMandators();
        try {
            loginSupervisor();
            Assert.assertTrue (!renameMandator(MANDATORS[0], MANDATORS[1]));
            Assert.assertTrue (renameMandator(MANDATORS[0], MANDATORS[2]));
            Assert.assertTrue (renameMandator(MANDATORS[2], MANDATORS[0]));
            Assert.assertTrue (!createMandator(MANDATORS[0], true));
        } catch (Throwable t) {
//            t.printStackTrace();
            LOG.error(t.getMessage(), t);
            fail();
        } finally {
            // clean up
            try {
                deleteMandator(MANDATORS[2]);
            } catch (RuntimeException re) {/* if the rename don't work --> don't leave mand03*/}
            logout();
        }
    }

    /**
     * creates the needed mandators
     * @see AbstractBackendBrowserTest#createMandators()
     */
    protected void createMandators_() {
        try {
            loginSupervisor();
//            navigateTo(NavigationTab.Administration);
            for (int i = 0; i < 2; i++) {
                createMandator(MANDATORS[i], true);
            }
        } catch (Throwable t) {
//            t.printStackTrace();
            LOG.error(t.getMessage(), t);
            fail();
        } finally {
            // clean up
            try {
                deleteMandator(MANDATORS[2]);
            } catch (RuntimeException re) {/* if the rename don't work --> don't leave mand03*/}
            logout();
        }
    }

    /**
     * renames a given mandator
     * @param fromName the name to search
     * @param toName the name to rename
     * @return <code>true</code> if successfull
     */
    private boolean renameMandator(String fromName, String toName) {
        selectFrame(Frame.Content);
        selenium.click(getButtonFromTRContainingName_(getResultTable(MANDATOR_OVERVIEW_PAGE), fromName, ":editButton_"));
        selenium.waitForPageToLoad("30000");
        selenium.type("frm:name", toName);
        selenium.click("link=Save");
        selenium.waitForPageToLoad("30000");

        return selenium.isTextPresent("Mandator " + toName + " was successfully updated.");
    }

    /**
     * deletes a given mandator
     * @param name the name of a mandator
     * @return <code>true</code> if successfull
     */
    private boolean deleteMandator(String name) {
        selectFrame(Frame.Content);
        selenium.click(getButtonFromTRContainingName_(getResultTable(MANDATOR_OVERVIEW_PAGE), name, "deleteButton_"));
        selenium.waitForPageToLoad("30000");
        return selenium.isTextPresent("Mandator " + name + " was successfully deleted.");
    }
}
