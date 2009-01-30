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
import com.flexive.shared.exceptions.FxCreateException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for the unique mode
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "structure"})
public class UniqueModeTest extends StructureTestBase {

    // TYPE_A > TYPE_B > TYPE_C
    private final static String TYPE_A = "UNIQUEMODETEST_A_" + RandomStringUtils.random(16, true, true);
    private final static String TYPE_B = "UNIQUEMODETEST_B_" + RandomStringUtils.random(16, true, true);
    private final static String TYPE_C = "UNIQUEMODETEST_C_" + RandomStringUtils.random(16, true, true);
    private final static String TYPE_D = "UNIQUEMODETEST_D_" + RandomStringUtils.random(16, true, true);

    private static final String PROP_NAME = "UniqueProperty";
    private static final String XPATH = "/" + PROP_NAME;
    private static final FxString V1A = new FxString(false, "Abc123");
    private static final FxString V1B = new FxString(false, "AbC123");
    private static final FxString V2 = new FxString(false, "def456");
    private static final FxString V3 = new FxString(false, "ghi789");

    final int A = 0;
    final int B = 1;
    final int C = 2;
    final int D = 3;

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        super.init();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        logout();
    }

    private void createProperty(long typeId, ACL acl, String name, String XPath, UniqueMode mode) throws FxApplicationException {
        ass.createProperty(
                typeId,
                FxPropertyEdit.createNew(name, new FxString("UniqueMode." + mode + " UnitTest property " + name),
                        new FxString("hint..."), new FxMultiplicity(1, 2), acl, FxDataType.String1024).setUniqueMode(mode),
                XPath);
    }

    private long[] createTypes(UniqueMode mode) throws FxApplicationException {
        ACL structACL = CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId());
        long typeA = type.save(FxTypeEdit.createNew(TYPE_A, new FxString("Test type " + TYPE_A),
                structACL, null));
        createProperty(typeA, structACL, PROP_NAME, "/", mode);
        long typeB = type.save(FxTypeEdit.createNew(TYPE_B, new FxString("Test type " + TYPE_B),
                structACL,
                CacheAdmin.getEnvironment().getType(typeA)));
        long typeC = type.save(FxTypeEdit.createNew(TYPE_C, new FxString("Test type " + TYPE_C), structACL,
                CacheAdmin.getEnvironment().getType(typeB)));
        long typeD = type.save(FxTypeEdit.createNew(TYPE_D, new FxString("Test type " + TYPE_D), structACL, null));
        ass.save(FxPropertyAssignmentEdit.createNew((FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(TYPE_A + "/" + PROP_NAME),
                CacheAdmin.getEnvironment().getType(TYPE_D),
                PROP_NAME, "/").setEnabled(true), false);
        return new long[]{typeA, typeB, typeC, typeD};
    }

    private void dropTypes() throws FxApplicationException {
        try {
            long id = CacheAdmin.getEnvironment().getType(TYPE_D).getId();
            co.removeForType(id);
            type.remove(id);
        } catch (FxApplicationException e) {
            //ignore
        }
        try {
            long id = CacheAdmin.getEnvironment().getType(TYPE_C).getId();
            co.removeForType(id);
            type.remove(id);
        } catch (FxApplicationException e) {
            //ignore
        }
        try {
            long id = CacheAdmin.getEnvironment().getType(TYPE_B).getId();
            co.removeForType(id);
            type.remove(id);
        } catch (FxApplicationException e) {
            //ignore
        }
        try {
            long id = CacheAdmin.getEnvironment().getType(TYPE_A).getId();
            co.removeForType(id);
            type.remove(id);
        } catch (FxApplicationException e) {
            //ignore
        }
    }

    @Test
    public void uniqueGlobal() throws FxApplicationException {
        long[] types = createTypes(UniqueMode.Global);
        try {
            FxContent c = co.initialize(types[A]);
            c.setValue(XPATH, V1A);
            FxPK pk = co.save(c);
            c = co.load(pk);
            co.createNewVersion(c); //new version has to work!
            c = co.initialize(types[B]);
            c.setValue(XPATH, V2);
            co.save(c);
            c = co.initialize(types[C]);
            c.setValue(XPATH, V1B);
            try {
                //save on C violates A
                co.save(c);
                assert false : "Constraint violation C on A expected!";
            } catch (FxCreateException e) {
                //ok
            }
            c = co.initialize(types[A]);
            c.setValue(XPATH, V1A);
            try {
                //save on A violates A
                co.save(c);
                assert false : "Constraint violation A on A expected!";
            } catch (FxCreateException e) {
                //ok
            }
            c = co.initialize(types[D]);
            c.setValue(XPATH, V1A);
            try {
                //save on D violates A
                co.save(c);
                assert false : "Constraint violation D on A expected!";
            } catch (FxCreateException e) {
                //ok
            }
            c = co.initialize(types[D]);
            c.setValue(XPATH, V3);
            co.save(c); //new value in D => ok
        } finally {
            dropTypes();
        }
    }

    @Test
    public void uniqueType() throws FxApplicationException {
        long[] types = createTypes(UniqueMode.Type);
        try {
            FxContent c = co.initialize(types[A]);
            c.setValue(XPATH, V1A);
            FxPK pk = co.save(c);
            c = co.load(pk);
            co.createNewVersion(c); //new version has to work!
            c = co.initialize(types[B]);
            c.setValue(XPATH, V2);
            co.save(c);
            c = co.initialize(types[C]);
            c.setValue(XPATH, V1A);
            co.save(c); //C and A same => ok
            c = co.initialize(types[D]);
            c.setValue(XPATH, V1A);
            co.save(c); //D, C and A same => ok
            c = co.initialize(types[A]);
            c.setValue(XPATH, V1B);
            try {
                //save on A violates A
                co.save(c);
                assert false : "Constraint violation expected!";
            } catch (FxCreateException e) {
                //ok
            }
        } finally {
            dropTypes();
        }
        uniqueWithinInstance(UniqueMode.Type);
    }

    @Test
    public void uniqueDerivedTypes() throws FxApplicationException {
        long[] types = createTypes(UniqueMode.DerivedTypes);
        try {
            FxContent c = co.initialize(types[B]);
            c.setValue(XPATH, V2);
            FxPK pk = co.save(c);
            c = co.load(pk);
            co.createNewVersion(c); //new version has to work!
            c = co.initialize(types[C]);
            c.setValue(XPATH, V2);
            try {
                //save on C violates B
                co.save(c);
                assert false : "Constraint violation C on B expected!";
            } catch (FxCreateException e) {
                //ok
            }
            c = co.initialize(types[A]);
            c.setValue(XPATH, V2);
            try {
                //save on C violates A
                co.save(c);
                assert false : "Constraint violation C on A expected!";
            } catch (FxCreateException e) {
                //ok
            }
            c = co.initialize(types[D]);
            c.setValue(XPATH, V2);
            co.save(c); //D is not derived or parent => ok
        } finally {
            dropTypes();
        }
        uniqueWithinInstance(UniqueMode.DerivedTypes);
    }

    @Test
    public void uniqueInstance() throws FxApplicationException {
        uniqueWithinInstance(UniqueMode.Instance);
    }

    private void uniqueWithinInstance(UniqueMode mode) throws FxApplicationException {
        long[] types = createTypes(mode);
        try {
            FxContent c = co.initialize(types[A]);
            c.setValue(XPATH, V2);
            FxPK pk = co.save(c);
            c = co.load(pk);
            co.createNewVersion(c); //new version has to work!
            c = co.load(pk);
            c.setValue(XPATH + "[2]", V3);
            co.save(c); //different value at [2] => ok
            c = co.load(pk);
            c.setValue(XPATH + "[2]", V2);
            try {
                //save on [2] violates [1]
                co.save(c);
                assert false : "Constraint violation [2] on [1] expected!";
            } catch (FxCreateException e) {
                //ok
            }
            c = co.initialize(types[D]);
            c.setValue(XPATH, V2);
            co.save(c); //different type => ok
        } finally {
            dropTypes();
        }
    }

}
