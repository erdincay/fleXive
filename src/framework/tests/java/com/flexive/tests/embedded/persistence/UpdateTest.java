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
package com.flexive.tests.embedded.persistence;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests to updates of structural information
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "structure"})
public class UpdateTest extends StructureTestBase {
    private static final String TEST_TYPE = "UPDATETYPETEST_" + RandomStringUtils.random(16, true, true);
    private long typeId = -1;

    private void createProperty(long typeId, ACL acl, String name, String XPath) throws FxApplicationException {
        ass.createProperty(
                typeId,
                FxPropertyEdit.createNew(name, new FxString("UpdateTest UnitTest property " + name),
                        new FxString("hint..."), new FxMultiplicity(0, 5), acl, FxDataType.String1024).
                        setMultiLang(true).setAutoUniquePropertyName(true),
                XPath);
    }

    private void createGroup(long typeId, String name, String XPath) throws FxApplicationException {
        ass.createGroup(
                typeId,
                FxGroupEdit.createNew(name, new FxString("UpdateTest UnitTest group " + name),
                        new FxString("hint..."), true, FxMultiplicity.MULT_0_N),
                XPath);
    }

    private void createStructure() throws FxApplicationException {
        ACL structACL = CacheAdmin.getEnvironment().getACL(ACL.Category.STRUCTURE.getDefaultId());
        typeId = type.save(FxTypeEdit.createNew(TEST_TYPE, new FxString("Test type"),
                structACL, null));
        createProperty(typeId, structACL, "P0", "/");
        createGroup(typeId, "G1", "/");
        createGroup(typeId, "G2", "/G1");
        createProperty(typeId, structACL, "P1", "/G1/G2");
        createProperty(typeId, structACL, "P2", "/G1/G2");
        createGroup(typeId, "T1", "/");
        createProperty(typeId, structACL, "P1", "/T1");
        createProperty(typeId, structACL, "P2", "/T1");
    }

    /**
     * Change the alias of a group assignment that has instances
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void groupAssignmentRename() throws FxApplicationException {
        FxContent c = co.initialize(typeId);
        c.setValue("/G1/G2/P1", new FxString(true, "Test1"));
        c.setValue("/G1/G2/P2", new FxString(true, "Test2"));
        FxPK cpk = co.save(c);
        FxGroupAssignmentEdit ga = ((FxGroupAssignment) CacheAdmin.getEnvironment().getType(typeId).getAssignment("/G1")).asEditable();
        ga.setAlias("X1");
        ass.save(ga, false);
        FxContent x = co.load(cpk);
        Assert.assertEquals(c.getValue("/G1/G2/P1"), x.getValue("/X1/G2/P1"));
        //set it back
        ga = ((FxGroupAssignment) CacheAdmin.getEnvironment().getType(typeId).getAssignment("/X1")).asEditable();
        ga.setAlias("G1");
        ass.save(ga, false);
        FxContent y = co.load(cpk);
        Assert.assertEquals(y.getValue("/G1/G2/P1"), x.getValue("/X1/G2/P1"));
    }

    /**
     * Test position changes
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void simplePositioning() throws FxApplicationException {
        FxPropertyAssignmentEdit pa1 = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getType(typeId).getAssignment("/G1/G2/P1")).asEditable();
        FxPropertyAssignmentEdit pa2 = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getType(typeId).getAssignment("/G1/G2/P2")).asEditable();
        Assert.assertEquals(pa1.getPosition(), 0);
        Assert.assertEquals(pa2.getPosition(), 1);
        pa1.setPosition(123);
        ass.save(pa1, false);
        pa1 = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getType(typeId).getAssignment("/G1/G2/P1")).asEditable();
        pa2 = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getType(typeId).getAssignment("/G1/G2/P2")).asEditable();
        Assert.assertEquals(pa1.getPosition(), 1);
        Assert.assertEquals(pa2.getPosition(), 0);
    }

    /**
     * Test position changes for groups and properties
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void complexPositioning() throws FxApplicationException {
        FxGroupAssignment t1 = (FxGroupAssignment) CacheAdmin.getEnvironment().getType(typeId).getAssignment("/T1");
        //create a derived property assignment, reset position and save it into existing group
        FxPropertyAssignment p1 = (FxPropertyAssignment) CacheAdmin.getEnvironment().getType(typeId).getAssignment("/T1/P1");
        FxPropertyAssignmentEdit p1_derived = FxPropertyAssignmentEdit.createNew(p1,
                CacheAdmin.getEnvironment().getType(typeId), "P1_Derived", TEST_TYPE + "/T1").setPosition(0);
        long p1_derivedId = ass.save(p1_derived, false);
        //newly created property assignment should be positioned before the other (non system internal) assignments
        int derived_pos = CacheAdmin.getEnvironment().getAssignment(p1_derivedId).getPosition();
        p1 = (FxPropertyAssignment) CacheAdmin.getEnvironment().getType(typeId).getAssignment("/T1/P1");
        Assert.assertTrue(derived_pos < p1.getPosition(), "Expected derived (" + derived_pos + ") to be lower than p1 (" +
                p1.getPosition() + ")");
        Assert.assertTrue(derived_pos < CacheAdmin.getEnvironment().getType(typeId).getAssignment("/T1/P2").getPosition(),
                "Expected derived_pos (" + derived_pos + ") < /T1/P2 pos (" +
                        CacheAdmin.getEnvironment().getType(typeId).getAssignment("/T1/P2").getPosition() + ")");
        p1_derived = ((FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(p1_derivedId)).asEditable();

        //move after p1, save and check positions
        //P1D, P1, P2 => P1, P1D, P2
        int p1_old_pos = CacheAdmin.getEnvironment().getAssignment(p1.getId()).getPosition();
        p1_derived.setPosition(p1_old_pos);
        ass.save(p1_derived, false);
        Assert.assertEquals(CacheAdmin.getEnvironment().getAssignment(p1_derivedId).getPosition(), p1_old_pos);
        Assert.assertEquals(CacheAdmin.getEnvironment().getAssignment(p1.getId()).getPosition(), p1_old_pos - 1);

        //create a derived property assignment, explicitly set position before saving and check position after saving
        p1_old_pos = CacheAdmin.getEnvironment().getAssignment(p1.getId()).getPosition();
        long p1_derived2 = ass.save(FxPropertyAssignmentEdit.createNew(p1, CacheAdmin.getEnvironment().getType(typeId),
                "P1_Derived2", TEST_TYPE + "/T1").setPosition(p1_old_pos), false);
        Assert.assertEquals(CacheAdmin.getEnvironment().getAssignment(p1_derived2).getPosition(), p1_old_pos);
        Assert.assertEquals(CacheAdmin.getEnvironment().getAssignment(p1.getId()).getPosition(), p1_old_pos + 1);

        //create new derived group assignment, reset position and save it into existing group
        long g1_g2Id = ass.save(FxGroupAssignmentEdit.createNew(t1, CacheAdmin.getEnvironment().getType(typeId), "T1_G2",
                TEST_TYPE + "/T1").setPosition(0), true);
        //newly created group assignment should be positioned before the other (non system internal) assignments
        Assert.assertTrue(CacheAdmin.getEnvironment().getAssignment(g1_g2Id).getPosition() < t1.getPosition());
        FxGroupAssignmentEdit t1_g2 = ((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(g1_g2Id)).asEditable();
        //move after p1, save and check positions
        p1_old_pos = CacheAdmin.getEnvironment().getAssignment(p1.getId()).getPosition();
        t1_g2.setPosition(p1_old_pos);
        ass.save(t1_g2, true);
        Assert.assertEquals(CacheAdmin.getEnvironment().getAssignment(g1_g2Id).getPosition(), p1_old_pos);
        Assert.assertEquals(CacheAdmin.getEnvironment().getAssignment(p1.getId()).getPosition(), p1_old_pos - 1);

        //create a derived group assignment, explicitly set position before saving and check position after saving
        p1_old_pos = CacheAdmin.getEnvironment().getAssignment(p1.getId()).getPosition();
        long g1_g3Id = ass.save(FxGroupAssignmentEdit.createNew(t1, CacheAdmin.getEnvironment().getType(typeId), "T1_G3",
                TEST_TYPE + "/T1").setPosition(p1_old_pos), true);
        Assert.assertEquals(CacheAdmin.getEnvironment().getAssignment(g1_g3Id).getPosition(), p1_old_pos);
        Assert.assertEquals(CacheAdmin.getEnvironment().getAssignment(p1.getId()).getPosition(), p1_old_pos + 1);
    }

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        super.init();
        login(TestUsers.SUPERVISOR);
        createStructure();
    }

    @AfterClass(dependsOnMethods = {"tearDownStructures"})
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        logout();
    }

    @AfterClass
    public void tearDownStructures() throws Exception {
        if (typeId == -1)
            return;
        co.removeForType(typeId);
        type.remove(typeId);
    }
}
