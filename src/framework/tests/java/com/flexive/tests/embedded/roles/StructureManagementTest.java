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
package com.flexive.tests.embedded.roles;

import com.flexive.shared.CacheAdmin;
import static com.flexive.shared.EJBLookup.getAssignmentEngine;
import static com.flexive.shared.EJBLookup.getTypeEngine;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNoAccessException;
import static com.flexive.shared.security.Role.StructureManagement;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxPropertyAssignmentEdit;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxTypeEdit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Structure management role tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"security", "roles"})
public class StructureManagementTest extends AbstractRoleTest {
    private long typeId = -1;
    private FxType type;
    private long assignmentId = -1;
    private boolean forced = false;

    @AfterMethod(groups = {"security", "roles"})
    public void cleanup() throws FxApplicationException {
        if (forced) {
            FxContext.get().runAsSystem();
        }
        try {
            if (assignmentId != -1) {
                getAssignmentEngine().removeAssignment(assignmentId, false, false);
            }
            if (typeId != -1) {
                getTypeEngine().remove(typeId);
            }
            typeId = assignmentId = -1;
            type = null;
        } finally {
            if (forced) {
                FxContext.get().stopRunAsSystem();
            }
            forced = false;
        }
    }

    @Test
    public void createTypeTest() throws FxApplicationException {
        try {
            createType(false);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, StructureManagement, -1);
        }
    }

    private void createType(boolean force) throws FxApplicationException {
        if (force) {
            FxContext.get().runAsSystem();
        }
        try {
            typeId = getTypeEngine().save(FxTypeEdit.createNew("structure management test type"));
            type = CacheAdmin.getEnvironment().getType(typeId);
            if (!force) {
                assertSuccess(StructureManagement, -1);
            }
            forced = force;
        } catch (FxNoAccessException e) {
            assertNoAccess(e, StructureManagement, -1);
            throw e;
        } finally {
            if (force) {
                FxContext.get().stopRunAsSystem();
            }
        }
    }

    @Test
    public void createAssignmentTest() throws FxApplicationException {
        createType(true);
        try {
            assignmentId = getAssignmentEngine().save(FxPropertyAssignmentEdit.createNew(
                    (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment("root/caption"),
                    type, "testAssignment", null),
                    false);
            assertSuccess(StructureManagement, -1);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, StructureManagement, -1);
        }
    }
}
