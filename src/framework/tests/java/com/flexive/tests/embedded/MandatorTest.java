/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxEntryInUseException;
import com.flexive.shared.interfaces.MandatorEngine;
import com.flexive.shared.security.Mandator;
import static com.flexive.tests.embedded.FxTestUtils.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Mandator tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb"})
public class MandatorTest {

    private MandatorEngine me = null;

    @BeforeClass
    public void beforeClass() throws Exception {
        me = EJBLookup.getMandatorEngine();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws Exception {
        logout();
    }

    /**
     * Tests if the current mandator (= the test user's mandator) is active.
     * If it wasn't active, the test user may not have been permitted to log in
     * at all.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void currentMandatorActive() throws Exception {
        Mandator mandator = CacheAdmin.getEnvironment().getMandator(getUserTicket().getMandatorId());
        assert mandator.isActive() : "Current mandator is inactive.";
    }

    /**
     * Tests the creation/removal of new mandators.
     *
     * @throws Exception if an error occured
     */
    @Test
    public void createRemoveMandator() throws Exception {
        int mandatorId = me.create("TestMandator", false);
        Mandator test = null;
        try {
            test = CacheAdmin.getEnvironment().getMandator(mandatorId);
        } catch (Exception e) {
            assert false : "Failed to get created mandator from cache: " + e.getMessage();
        }
        assert test != null : "Loaded mandator is null!";
        assert test.getName().equals("TestMandator") : "Name mismatch!";
        assert !test.hasMetadata() : "Mandator should have no meta data attached!";
        assert !test.isActive() : "Mandator should be inactive!";
        me.activate(test.getId());
        try {
            test = CacheAdmin.getEnvironment().getMandator(mandatorId);
        } catch (Exception e) {
            assert false : "Failed to get activated mandator from cache: " + e.getMessage();
        }
        assert test.isActive() : "Mandator should be active!";
        me.deactivate(test.getId());
        try {
            test = CacheAdmin.getEnvironment().getMandator(mandatorId);
        } catch (Exception e) {
            assert false : "Failed to get deactivated mandator from cache: " + e.getMessage();
        }
        assert !test.isActive() : "Mandator should be deactivated!";

        me.remove(test.getId());
        try {
            CacheAdmin.getEnvironment().getMandator(mandatorId);
            assert false : "Removed mandator could be retrieved from cache!";
        } catch (Exception e) {
            //ignore
        }
        try {
            me.remove(Mandator.MANDATOR_FLEXIVE); //try to remove the public mandator -> got to fail
            assert false : "Removing the public mandator should have failed!";
        } catch (FxEntryInUseException e) {
            //ok
        } catch (Exception e) {
            assert false : "Unexpected exception: " + e.getMessage();
        }
    }

}
