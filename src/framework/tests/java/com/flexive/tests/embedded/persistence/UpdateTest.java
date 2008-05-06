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
                        new FxString("hint..."), new FxMultiplicity(0, 5), acl, FxDataType.String1024).setMultiLang(true),
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
    public void assignmentReposition() throws FxApplicationException {
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
