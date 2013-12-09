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
package com.flexive.tests.embedded.persistence;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxMultiplicity;
import com.flexive.shared.structure.FxPropertyEdit;
import com.flexive.shared.structure.FxTypeEdit;
import com.flexive.shared.value.FxReference;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.ReferencedContent;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;

/**
 * Tests for referenced contents
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "reference"})
public class ReferenceTest {

    public static final String TEST_TYPE_A = "TEST_TYPE_A_" + RandomStringUtils.random(16, true, true);
    public static final String TEST_TYPE_B = "TEST_TYPE_B_" + RandomStringUtils.random(16, true, true);

    private ContentEngine co;
    private TypeEngine type;

    private long typeA;
    private long typeB;

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        login(TestUsers.SUPERVISOR);

        co = EJBLookup.getContentEngine();
        type = EJBLookup.getTypeEngine();

        FxTypeEdit teA = FxTypeEdit.createNew(TEST_TYPE_A);
        FxTypeEdit teB = FxTypeEdit.createNew(TEST_TYPE_B);
        typeA = type.save(teA);
        typeB = type.save(teB);

        FxPropertyEdit peRefB = FxPropertyEdit.createNew("REFB", new FxString(true, "Reference to B"),
                new FxString(true, "Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE),
                FxDataType.Reference).setAutoUniquePropertyName(true);
        peRefB.setReferencedType(CacheAdmin.getEnvironment().getType(typeB));
        EJBLookup.getAssignmentEngine().createProperty(typeA, peRefB, "/"); //A -> B
        EJBLookup.getAssignmentEngine().createProperty(typeB, peRefB, "/"); //B -> B
    }

    /**
     * teardown
     *
     * @throws FxApplicationException  on errors
     * @throws FxLogoutFailedException on errors
     */
    @AfterClass
    public void afterClass() throws FxApplicationException, FxLogoutFailedException {
        co.removeForType(typeA);
        type.remove(typeA);
        co.removeForType(typeB);
        type.remove(typeB);
        logout();
    }

    /**
     * A references B
     * Reference should not be removeable
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void referenceInstanceRemove() throws FxApplicationException {
        FxPK pkB = co.save(co.initialize(typeB));

        FxContent coA = co.initialize(typeA);
        coA.setValue("/REFB", new FxReference(false, new ReferencedContent(pkB.getId())));
        FxPK pkA = co.save(coA);
        try {
            co.remove(pkB);
            Assert.fail("Removing referenced content B should not be possible! Probably no referential integrity checks in place!");
        } catch (FxApplicationException e) {
            //expected
        }
        //cleanup
        co.remove(pkA);
        co.remove(pkB);
    }

    /**
     * B references B
     * Reference should be removeable (instance)
     *
     * @throws FxApplicationException on errors
     */
    private void selfReferenceInstanceRemove() throws FxApplicationException {
        FxContent coB = co.initialize(typeB);
        coB = co.load(co.save(coB));
        coB.setValue("/REFB", new FxReference(false, new ReferencedContent(coB.getId())));
        coB = co.load(co.save(coB));
        try {
            co.remove(coB.getPk());
        } catch (FxApplicationException e) {
            Assert.fail("Removing self-referenced content B should be possible!", e);
        }
    }

    /**
     * B references B
     * Reference should be removeable (all for type)
     *
     * @throws FxApplicationException on errors
     */
    private void selfReferenceTypeRemove() throws FxApplicationException {
        FxContent coB = co.initialize(typeB);
        coB = co.load(co.save(coB));
        coB.setValue("/REFB", new FxReference(false, new ReferencedContent(coB.getId())));
        co.save(coB);
        try {
            co.removeForType(typeB);
        } catch (FxApplicationException e) {
            Assert.fail("Removing self-referenced content B should be possible!", e);
        }
    }

    /**
     * Test self referenced removal for hierarchical storage
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void selfReferenceHierarchicalRemove() throws FxApplicationException {
        type.unflatten(typeA);
        type.unflatten(typeB);
        selfReferenceInstanceRemove();
        selfReferenceTypeRemove();
    }

    /**
     * Test self referenced removal for flat storage
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void selfReferenceFlatstorageRemove() throws FxApplicationException {
        type.flatten(typeA);
        type.flatten(typeB);
        selfReferenceInstanceRemove();
        selfReferenceTypeRemove();
    }

    /**
     * A references B
     * Referenced type B should not be removeable
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void referenceTypeRemove() throws FxApplicationException {
        FxPK pkB = co.save(co.initialize(typeB));

        FxContent coA = co.initialize(typeA);
        coA.setValue("/REFB", new FxReference(false, new ReferencedContent(pkB.getId())));
        FxPK pkA = co.save(coA);
        try {
            co.removeForType(typeB);
            Assert.fail("Removing referenced type B should not be possible! Probably no referential integrity checks in place!");
        } catch (FxApplicationException e) {
            //expected
        }
        //cleanup
        co.remove(pkA);
        co.remove(pkB);
    }

    /**
     * A references B
     * 2 versions of B exist, one will be removed after being referenced
     * Reference should still exist/throw no errors
     * see issue FX-502
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void referenceVersionRemove() throws FxApplicationException {
        FxPK pkB = co.save(co.initialize(typeB));
        co.createNewVersion(co.load(pkB));

        FxContent coA = co.initialize(typeA);
        coA.setValue("/REFB", new FxReference(false, new ReferencedContent(pkB.getId())));
        FxPK pkA = co.save(coA);
        try {
            co.removeVersion(pkB);
        } catch (FxApplicationException e) {
            Assert.fail("Removing first version of B should be possible! Error: " + e.getMessage(), e);
        }
        //cleanup
        co.remove(pkA);
        co.remove(pkB);
    }
}
