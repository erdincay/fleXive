/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
import com.flexive.shared.FxLanguage;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.MandatorEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxTypeEdit;
import static com.flexive.tests.embedded.FxTestUtils.*;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Mandator tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "mandator"})
public class MandatorTest {

    private static transient Log LOG = LogFactory.getLog(MandatorTest.class);

    private MandatorEngine me = null;
    private TypeEngine te = null;
    private ContentEngine ce = null;
    private long testMandator = -1;
    private long testType = -1;

    @BeforeClass
    public void beforeClass() throws Exception {
        me = EJBLookup.getMandatorEngine();
        te = EJBLookup.getTypeEngine();
        ce = EJBLookup.getContentEngine();
        login(TestUsers.SUPERVISOR);
        try {
            testMandator = me.create("MANDATOR_" + RandomStringUtils.randomAlphanumeric(10), true);
            testType = te.save(FxTypeEdit.createNew("TEST_" + RandomStringUtils.randomAlphanumeric(10)));
        } catch (FxApplicationException e) {
            LOG.error(e);
        }
    }

    @AfterClass
    public void afterClass() throws Exception {
        if (testMandator != -1) {
            if (testType != -1)
                ce.removeForType(testType);
            me.remove(testMandator);
        }
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

    /**
     * Test active/inactive mandators with contents
     *
     * @throws FxApplicationException on errors
     */
    public void activeContent() throws FxApplicationException {
        me.activate(testMandator); //make sure the mandator is active
        FxType type = CacheAdmin.getEnvironment().getType(testType);
        Assert.assertTrue(CacheAdmin.getEnvironment().getMandator(testMandator).isActive(), "Expected mandator to be active!");
        FxContent co_act1 = ce.initialize(testType, testMandator, -1, type.getWorkflow().getSteps().get(0).getId(), FxLanguage.DEFAULT_ID);
        me.deactivate(testMandator);
        Assert.assertFalse(CacheAdmin.getEnvironment().getMandator(testMandator).isActive(), "Expected mandator to be inactive!");
        try {
            ce.initialize(testType, testMandator, -1, type.getWorkflow().getSteps().get(0).getId(), FxLanguage.DEFAULT_ID);
            Assert.fail("Initialize on a deactivated mandator should fail!");
        } catch (FxNotFoundException e) {
            //expected
        } catch (Exception ex) {
            Assert.fail("FxNotFoundException expected! Got: " + ex.getClass().getCanonicalName());
        }
        try {
            ce.save(co_act1);
            Assert.fail("Save on a deactivated mandator should fail!");
        } catch (FxCreateException e) {
            //expected
        } catch (Exception ex) {
            Assert.fail("FxCreateException expected! Got: " + ex.getClass().getCanonicalName());
        }
        me.activate(testMandator);
        FxPK pk = ce.save(co_act1);
        FxContent co_loaded = ce.load(pk);
        FxPK pk_ver = ce.createNewVersion(co_loaded);
        ce.removeVersion(pk_ver);
        ce.remove(pk);
        pk = ce.save(co_act1);
        me.deactivate(testMandator);
        try {
            ce.load(pk);
            Assert.fail("Load on a deactivated mandator should fail!");
        } catch (FxNotFoundException e) {
            //expected
        } catch (Exception ex) {
            Assert.fail("FxNotFoundException expected! Got: " + ex.getClass().getCanonicalName());
        }
        me.activate(testMandator);
        co_loaded = ce.load(pk);
        me.deactivate(testMandator);
        try {
            ce.createNewVersion(co_loaded);
            Assert.fail("CreateNewVersion on a deactivated mandator should fail!");
        } catch (FxNotFoundException e) {
            //expected
        } catch (Exception ex) {
            Assert.fail("FxNotFoundException expected! Got: " + ex.getClass().getCanonicalName());
        }
        me.activate(testMandator);
        pk_ver = ce.createNewVersion(co_loaded);
        me.deactivate(testMandator);
        try {
            ce.removeVersion(pk_ver);
            Assert.fail("RemoveVersion on a deactivated mandator should fail!");
        } catch (FxNotFoundException e) {
            //expected
        } catch (Exception ex) {
            Assert.fail("FxNotFoundException expected! Got: " + ex.getClass().getCanonicalName());
        }
        try {
            ce.remove(pk);
            Assert.fail("Remove on a deactivated mandator should fail!");
        } catch (FxRemoveException e) {
            //expected
        } catch (Exception ex) {
            Assert.fail("FxRemoveException expected! Got: " + ex.getClass().getCanonicalName());
        }
        me.activate(testMandator);
    }

}
