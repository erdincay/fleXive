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
package com.flexive.tests.embedded.structure;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.structure.FxTypeEdit;
import com.flexive.shared.structure.TypeState;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.tree.FxTreeNodeEdit;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for FxType's TypeState (Available, Locked, Unavailable)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
@Test(groups = {"ejb", "structure"})
public class TypeStateTest extends StructureTestBase {
    private static transient Log LOG = LogFactory.getLog(TypeStateTest.class);

    private long testType = -1;

    @BeforeClass
    public void beforeClass() throws Exception {
        super.init();
        login(TestUsers.SUPERVISOR);
        try {
            testType = type.save(FxTypeEdit.createNew("TEST_" + RandomStringUtils.randomAlphanumeric(10)));
        } catch (FxApplicationException e) {
            LOG.error(e);
        }
    }

    @AfterClass
    public void afterClass() throws Exception {
        if (testType != -1) {
            //make sure the type is active or this will fail
            type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
            co.removeForType(testType);
        }
        logout();
    }

    /**
     * Test changing the state
     *
     * @throws Exception on errors
     */
    public void changeState() throws Exception {
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Locked));
        Assert.assertTrue(CacheAdmin.getEnvironment().getType(testType).getState() == TypeState.Locked, "Expected type to be locked!");
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Unavailable));
        Assert.assertTrue(CacheAdmin.getEnvironment().getType(testType).getState() == TypeState.Unavailable, "Expected type to be unavailable!");
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
        Assert.assertTrue(CacheAdmin.getEnvironment().getType(testType).getState() == TypeState.Available, "Expected type to be available!");
    }

    /**
     * Test locked types with contents
     *
     * @throws FxApplicationException on errors
     */
    public void lockedTypeContent() throws FxApplicationException {
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
        FxContent co_ok = co.initialize(testType);
        FxPK pk1 = co.save(co_ok);
        FxContent co_pk1 = co.load(pk1);
        FxContent co_pk1v2 = co.load(co.createNewVersion(co_pk1));
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Locked));
        try {
            co.initialize(testType);
            Assert.fail("Initialization on locked types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.save(co_ok);
            Assert.fail("Create on locked types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.save(co_pk1);
            Assert.fail("Save on locked types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.createNewVersion(co_pk1);
            Assert.fail("CreateNewVersion on locked types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.removeVersion(co_pk1v2.getPk());
            Assert.fail("RemoveVersion on locked types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.load(pk1);
        } catch (FxApplicationException e) {
            Assert.fail("Loading locked types should be allowed!");
        }
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
        co.remove(pk1);
    }

    /**
     * Test unavailable types with contents
     *
     * @throws FxApplicationException on errors
     */
    public void unavailableTypeContent() throws FxApplicationException {
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
        FxContent co_ok = co.initialize(testType);
        FxPK pk1 = co.save(co_ok);
        FxContent co_pk1 = co.load(pk1);
        FxContent co_pk1v2 = co.load(co.createNewVersion(co_pk1));
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Unavailable));
        try {
            co.initialize(testType);
            Assert.fail("Initialization on unavailable types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.save(co_ok);
            Assert.fail("Create on unavailable types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.save(co_pk1);
            Assert.fail("Save on unavailable types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.createNewVersion(co_pk1);
            Assert.fail("CreateNewVersion on unavailable types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.removeVersion(co_pk1v2.getPk());
            Assert.fail("RemoveVersion on unavailable types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.load(pk1);
            Assert.fail("Loading unavailable types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        try {
            co.removeForType(testType);
            Assert.fail("RemoveForType for unavailable types should be prevented!");
        } catch (FxApplicationException e) {
            //expected
        }
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
        co.remove(pk1);
    }

    /**
     * Test locked/unavailable types concerning queries
     *
     * @throws Exception on errors
     */
    public void typeStateQuery() throws Exception {
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
        co.removeForType(testType);
        FxContent co_ok = co.initialize(testType);
        FxPK pk = co.save(co_ok);
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Locked));
        long cnt = new SqlQueryBuilder().type(testType).getResult().getRowCount();
        Assert.assertEquals(cnt, 1, "Expected one result!");
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Unavailable));
        cnt = new SqlQueryBuilder().type(testType).getResult().getRowCount();
        Assert.assertEquals(cnt, 0, "Expected no results!");
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
        cnt = new SqlQueryBuilder().type(testType).getResult().getRowCount();
        Assert.assertEquals(cnt, 1, "Expected one result!");
        co.remove(pk);
    }

    /**
     * Test active/inactive mandators with the tree
     *
     * @throws FxApplicationException on errors
     */
    public void typeStateTree() throws Exception {
        EJBLookup.getTreeEngine().clear(FxTreeMode.Edit);
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
        FxContent co_act1 = co.initialize(testType);
        FxPK pk = co.save(co_act1);
        EJBLookup.getTreeEngine().getNode(FxTreeMode.Edit, FxTreeNode.ROOT_NODE).getTotalChildCount();
        long folder = EJBLookup.getTreeEngine().save(FxTreeNodeEdit.createNew("TypeStateTestFolder"));
        long node_root = EJBLookup.getTreeEngine().save(FxTreeNodeEdit.createNew("TypeStateTestRoot").setReference(pk));
        EJBLookup.getTreeEngine().save(FxTreeNodeEdit.createNew("TypeStateTestChild").setReference(pk).setParentNodeId(folder));
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Locked));
        FxTreeNode node = EJBLookup.getTreeEngine().getTree(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, 100);
        Assert.assertEquals(node.getTotalChildCount(), 3);
        Assert.assertEquals(node.getDirectChildCount(), 2);
        Assert.assertEquals(node.getChildren().size(), 2);
        Assert.assertEquals(node.getChildren().get(0).getChildren().size(), 1);
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Unavailable));
        node = EJBLookup.getTreeEngine().getTree(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, 100);
        Assert.assertEquals(node.getChildren().size(), 1);
        Assert.assertEquals(node.getChildren().get(0).getChildren().size(), 0);
        try {
            EJBLookup.getTreeEngine().getNode(FxTreeMode.Edit, node_root);
            Assert.fail("getNode on an unavailable type should fail!");
        } catch (FxNotFoundException e) {
            //expected
        } catch (Exception ex) {
            Assert.fail("FxNotFoundException expected! Got: " + ex.getClass().getCanonicalName());
        }
        type.save(CacheAdmin.getEnvironment().getType(testType).asEditable().setState(TypeState.Available));
        node = EJBLookup.getTreeEngine().getTree(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, 100);
        Assert.assertEquals(node.getChildren().size(), 2);
        Assert.assertEquals(node.getChildren().get(0).getChildren().size(), 1);
        EJBLookup.getTreeEngine().getNode(FxTreeMode.Edit, node_root);
        EJBLookup.getTreeEngine().clear(FxTreeMode.Edit);
    }
}
