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

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.interfaces.ACLEngine;
import static com.flexive.shared.security.Role.ACLManagement;
import com.flexive.shared.security.*;
import com.flexive.shared.value.FxString;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.util.Arrays;
import java.util.List;

/**
 * ACL management role tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "security", "roles"})
public class ACLManagementTest extends AbstractRoleTest {
    private long aclId = -1;

    @AfterMethod(groups = {"ejb", "security", "roles"})
    public void cleanup() throws FxApplicationException {
        if (aclId != -1) {
            EJBLookup.getAclEngine().remove(aclId);
            aclId = -1;
        }
    }

    @Test
    public void createAclTest() throws FxApplicationException {
        try {
            createAcl(ticket.getMandatorId());
        } catch (FxNoAccessException e) {
            assertNoAccess(e, ACLManagement, ticket.getMandatorId());
        }
    }

    @Test
    public void createForeignAclTest() throws FxApplicationException {
        Assert.assertTrue(ticket.getMandatorId() != Mandator.MANDATOR_FLEXIVE);
        try {
            createAcl(Mandator.MANDATOR_FLEXIVE);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, ACLManagement, Mandator.MANDATOR_FLEXIVE);
        }
    }

    @Test
    public void updateAclTest() throws FxApplicationException {
        updateAcl(ticket.getMandatorId());
    }

    @Test
    public void updateForeignAclTest() throws FxApplicationException {
        updateAcl(Mandator.MANDATOR_FLEXIVE);
    }

    @Test
    public void loadGroupAssignmentsTest() throws FxApplicationException {
        long foreignUserGroupId = -1;
        try {
            FxContext.get().runAsSystem();
            final List<UserGroup> groups = EJBLookup.getUserGroupEngine().loadAll(Mandator.MANDATOR_FLEXIVE);
            for (final UserGroup group : groups) {
                if (group.getId() != UserGroup.GROUP_EVERYONE && group.getId() != UserGroup.GROUP_OWNER) {
                    foreignUserGroupId = group.getId();
                    break;
                }
            }
            Assert.assertTrue(foreignUserGroupId != -1, "No suitable user group found");
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        final ACLEngine ae = EJBLookup.getAclEngine();
        ae.loadGroupAssignments(ticket.getGroups()[0]);
        try {
            ae.loadGroupAssignments(foreignUserGroupId);
            assertSuccess(null, Mandator.MANDATOR_FLEXIVE);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, null, Mandator.MANDATOR_FLEXIVE);
        }
    }


    private void updateAcl(long mandatorId) throws FxApplicationException {
        final ACLEngine ae = EJBLookup.getAclEngine();
        try {
            createAcl(mandatorId);
            assertSuccess(ACLManagement, mandatorId);
            ae.update(aclId, "new name", new FxString(true, "new label"), "#000000", "descr",
                    Arrays.asList(new ACLAssignment(aclId, UserGroup.GROUP_EVERYONE, true, true, false,
                            true, true, false, ACLCategory.INSTANCE, null)));
            assertSuccess(ACLManagement, mandatorId);
            Assert.assertTrue(ae.load(aclId).getName().equals("new name"));
            Assert.assertTrue(ae.loadAssignments(aclId, UserGroup.GROUP_EVERYONE).size() == 1);
            assertSuccess(ACLManagement, mandatorId);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, ACLManagement, mandatorId);
        }
    }

    private void createAcl(long mandatorId) throws FxApplicationException {
        aclId = EJBLookup.getAclEngine().create("acl management test", new FxString(true, "label"),
                mandatorId, "", "", ACLCategory.INSTANCE);
        assertSuccess(ACLManagement, mandatorId);
    }
}
