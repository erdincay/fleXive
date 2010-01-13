/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
package com.flexive.tests.embedded;

import com.flexive.core.Database;
import com.flexive.core.storage.genericSQL.GenericTreeStorageSpreaded;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.interfaces.TreeEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.FxPropertyAssignmentEdit;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxTypeEdit;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.tree.FxTreeNodeEdit;
import com.flexive.shared.tree.FxTreeRemoveOp;
import com.flexive.shared.value.FxReference;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.ReferencedContent;
import com.flexive.shared.workflow.StepDefinition;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.util.List;

import static com.flexive.shared.CacheAdmin.getEnvironment;
import static com.flexive.shared.EJBLookup.getContentEngine;
import static com.flexive.shared.EJBLookup.getTreeEngine;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import static org.testng.Assert.*;

/**
 * Tree engine tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "tree"})
public class FxTreeTest {
    TreeEngine tree;
    ScriptingEngine scripting;
    ContentEngine ce;
    TypeEngine ty;

    @BeforeClass
    public void beforeClass() throws Exception {
        login(TestUsers.SUPERVISOR);
        tree = getTreeEngine();
        scripting = EJBLookup.getScriptingEngine();
        ce = getContentEngine();
        ty = EJBLookup.getTypeEngine();
        enableTreeChecks();
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        try {
            clearTrees();
            disableTreeChecks();
        } catch (FxApplicationException e) {
            //ignore
        }
        logout();
    }

    private void enableTreeChecks() throws FxApplicationException {
        EJBLookup.getDivisionConfigurationEngine().put(SystemParameters.TREE_CHECKS_ENABLED, true);
    }

    private void disableTreeChecks() throws FxApplicationException {
        EJBLookup.getDivisionConfigurationEngine().put(SystemParameters.TREE_CHECKS_ENABLED, false);
    }

    /**
     * Helper to build a node name
     *
     * @param number running number
     * @return generated node name
     */
    private static String getNodeName(int number) {
        return "Node" + number;
    }

    /**
     * Helper to build a node label
     *
     * @param number running number
     * @return generated node label
     */
    private static FxString getNodeLabel(int number) {
        return new FxString(true, "Node" + number + "Description");
    }

    /**
     * Clear live and edit trees
     *
     * @throws FxApplicationException on errors
     */
    private void clearTrees() throws FxApplicationException {
        tree.clear(FxTreeMode.Live);
        tree.clear(FxTreeMode.Edit);
    }

    /**
     * Edit tests
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void treeTestEdit() throws FxApplicationException {
        treeCRUD(FxTreeMode.Edit);
        createPath(FxTreeMode.Edit);
    }

    /**
     * Test path creation
     *
     * @param mode FxTreeMode
     * @throws FxApplicationException on errors
     */
    private void createPath(FxTreeMode mode) throws FxApplicationException {
        final long[] nodes = tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "my/virtual/directory");
        assertEquals(nodes.length, 3, "Should have created 3 nodes");
        assertEquals(tree.getNode(mode, nodes[0]).getName(), "my");
        assertEquals(tree.getNode(mode, nodes[1]).getName(), "virtual");
        assertEquals(tree.getNode(mode, nodes[2]).getName(), "directory");

        // create subfolder, reuse two folders
        final long[] tmpNodes = tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "my/virtual/tmp");
        assertEquals(tmpNodes.length, 3, "Should have returned 3 nodes");
        assertEquals(tree.getNode(mode, tmpNodes[0]).getName(), "my");
        assertEquals(tree.getNode(mode, tmpNodes[1]).getName(), "virtual");
        assertEquals(tree.getNode(mode, tmpNodes[2]).getName(), "tmp");
        assertEquals(nodes[0], tmpNodes[0]);
        assertEquals(nodes[1], tmpNodes[1]);

        // use /my/virtual as root node
        final long[] nestedNodes = tree.createNodes(mode, nodes[1], 0, "/directory/tmp2");
        assertEquals(nestedNodes.length, 2);
        assertEquals(tree.getNode(mode, nestedNodes[0]).getName(), "directory");
        assertEquals(tree.getNode(mode, nestedNodes[1]).getName(), "tmp2");
        assertEquals(nestedNodes[0], nodes[2]);  // check "directory" node
    }

    /**
     * Test most methods of the simplified API
     *
     * @param mode FxTreeMode to test
     * @throws FxApplicationException on errors
     */
    private void treeCRUD(FxTreeMode mode) throws FxApplicationException {
        //clear the tree
        clearTrees();
        assertTrue(tree.getNode(mode, FxTreeNode.ROOT_NODE).getDirectChildCount() == 0,
                "Expected to have 0 children, got: [" + tree.getNode(mode, FxTreeNode.ROOT_NODE).getDirectChildCount() + "]");
        //create new node
        FxTreeNodeEdit node1 = FxTreeNodeEdit.createNew(getNodeName(1));
        node1.setLabel(getNodeLabel(1));
        node1.setMode(mode);
        long id1 = tree.save(node1);
        assertTrue(tree.exist(mode, id1));
        assertTrue(tree.getNode(mode, FxTreeNode.ROOT_NODE).getDirectChildCount() == 1,
                "Expected to have 1 child, got: [" + tree.getNode(mode, FxTreeNode.ROOT_NODE).getDirectChildCount() + "]");
        //load and check if all well
        FxTreeNode node1_loaded = tree.getNode(mode, id1);
        assertTrue(node1_loaded.getName().equals(node1.getName()));
        assertEquals(node1_loaded.getLabel(), node1.getLabel());
        //rename name
        tree.save(new FxTreeNodeEdit(node1_loaded).setName("abcd"));
        node1_loaded = tree.getNode(mode, id1);
        assertTrue(node1_loaded.getName().equals("abcd"), "Expected [abcd] - got [" + node1_loaded.getName() + "]");
        assertEquals(node1_loaded.getLabel(), node1.getLabel());
        //rename label
        tree.save(new FxTreeNodeEdit(node1_loaded).setLabel(getNodeLabel(42)));
        node1_loaded = tree.getNode(mode, id1);
        assertTrue(node1_loaded.getName().equals("abcd"));
        assertTrue(node1_loaded.getLabel().equals(getNodeLabel(42)));
        //create child
        FxTreeNodeEdit node1_1 = FxTreeNodeEdit.createNewChildNode(node1_loaded).setName("1").setLabel(getNodeLabel(1)).setMode(mode);
        long id1_1 = tree.save(node1_1);
        FxTreeNode node1_1_loaded = tree.getNode(mode, id1_1);
        assertTrue(node1_1_loaded.getParentNodeId() == node1_loaded.getId());
        //verify path
        assertTrue(node1_1_loaded.getPath().equals("/abcd/1"));
        //verify label
        assertEquals(tree.getLabels(mode, node1_1_loaded.getId()).get(0), "/" + getNodeLabel(42).getBestTranslation() + "/" + getNodeLabel(1).getBestTranslation());
        //create 2 other children for positioning tests
        FxTreeNodeEdit node1_2 = FxTreeNodeEdit.createNewChildNode(node1_loaded).setName("2").setLabel(getNodeLabel(2)).setMode(mode);
        long id1_2 = tree.save(node1_2);
        FxTreeNode node1_2_loaded = tree.getNode(mode, id1_2);
        //verify path
        assertTrue(node1_2_loaded.getPath().equals("/abcd/2"), "Expected [/abcd/2] got: [" + node1_2_loaded.getPath() + "]");
        //verify label
        assertTrue(("/" + getNodeLabel(42).getBestTranslation() + "/" + getNodeLabel(2).getBestTranslation()).
                equals(tree.getLabels(mode, node1_2_loaded.getId()).get(0)),
                "Expected [/" + getNodeLabel(42).getBestTranslation() + "/" + getNodeLabel(2).getBestTranslation() + "] got: [" +
                        tree.getLabels(mode, node1_2_loaded.getId()).get(0) + "]");
        // verify unknown id return value
        final int invalidId = 999999;
        final String expectedOutcome = "/<unknown:" + invalidId + ">";
        assertEquals(tree.getLabels(mode, invalidId).get(0), expectedOutcome);
        FxTreeNodeEdit node1_3 = FxTreeNodeEdit.createNewChildNode(node1_loaded).setName("3").setLabel(getNodeLabel(3)).setMode(mode);
        long id1_3 = tree.save(node1_3);
        FxTreeNode node1_3_loaded = tree.getNode(mode, id1_3);

        // TODO: check total child count with TreeEngine#getTotalChildCount (FX-690)

        //verify positions - should be 1-2-3
        assertTrue(node1_1_loaded.getPosition() == 0, "Expected [0] got: [" + node1_1_loaded.getPosition() + "]");
        assertTrue(node1_2_loaded.getPosition() == 1, "Expected [1] got: [" + node1_2_loaded.getPosition() + "]");
        assertTrue(node1_3_loaded.getPosition() == 2, "Expected [2] got: [" + node1_3_loaded.getPosition() + "]");

        //swap positions of 1 and 3 to net 3-2-1
        tree.save(new FxTreeNodeEdit(node1_3_loaded).setPosition(-1));
        tree.save(new FxTreeNodeEdit(node1_1_loaded).setPosition(100));
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_2_loaded = tree.getNode(mode, id1_2);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assertTrue(node1_1_loaded.getPosition() == 2, "Expected [2] got: [" + node1_1_loaded.getPosition() + "]");
        assertTrue(node1_2_loaded.getPosition() == 1, "Expected [1] got: [" + node1_2_loaded.getPosition() + "]");
        assertTrue(node1_3_loaded.getPosition() == 0, "Expected [0] got: [" + node1_3_loaded.getPosition() + "]");
        //3-2-1 => 3-1-2
        tree.save(new FxTreeNodeEdit(node1_1_loaded).setPosition(1));
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_2_loaded = tree.getNode(mode, id1_2);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assertTrue(node1_1_loaded.getPosition() == 1, "Expected [1] got: [" + node1_1_loaded.getPosition() + "]");
        assertTrue(node1_2_loaded.getPosition() == 2, "Expected [2] got: [" + node1_2_loaded.getPosition() + "]");
        assertTrue(node1_3_loaded.getPosition() == 0, "Expected [0] got: [" + node1_3_loaded.getPosition() + "]");
        //3-1-2 => 1-2-3
        tree.save(new FxTreeNodeEdit(node1_3_loaded).setPosition(4));
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_2_loaded = tree.getNode(mode, id1_2);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assertTrue(node1_1_loaded.getPosition() == 0, "Expected [0] got: [" + node1_1_loaded.getPosition() + "]");
        assertTrue(node1_2_loaded.getPosition() == 1, "Expected [1] got: [" + node1_2_loaded.getPosition() + "]");
        assertTrue(node1_3_loaded.getPosition() == 2, "Expected [2] got: [" + node1_3_loaded.getPosition() + "]");
        //delete 1_2 and check positions
        tree.remove(new FxTreeNodeEdit(node1_2_loaded), FxTreeRemoveOp.Remove, false);
        assertTrue(!tree.exist(mode, id1_2));
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assertTrue(node1_1_loaded.getPosition() == 0, "Expected [0] got: [" + node1_1_loaded.getPosition() + "]");
        assertTrue(node1_3_loaded.getPosition() == 1, "Expected [1] got: [" + node1_3_loaded.getPosition() + "]");

        // TODO: check total child count with TreeEngine#getTotalChildCount (FX-690)

        if (mode == FxTreeMode.Live)
            return; //children are to be removed in live mode
        //delete parent but not children and check if they moved up in hierarchy
        tree.remove(new FxTreeNodeEdit(node1_loaded), FxTreeRemoveOp.Remove, false);
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assertTrue(node1_1_loaded.getParentNodeId() == FxTreeNode.ROOT_NODE);
        assertTrue(node1_3_loaded.getParentNodeId() == FxTreeNode.ROOT_NODE);
        //attach 1_3 as child to 1_2
        tree.save(new FxTreeNodeEdit(node1_3_loaded).setParentNodeId(node1_1_loaded.getId()));
        node1_1_loaded = tree.getTree(mode, id1_1, 3);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assertTrue(node1_1_loaded.getChildren().size() == 1);
        assertTrue(node1_1_loaded.getChildren().get(0).getId() == node1_3_loaded.getId());
        assertTrue(node1_3_loaded.getPath().equals("/1/3"), "Expected [/1/3] got: [" + node1_3_loaded.getPath() + "]");
        //delete 1_1 with children and check that 1_3 is gone too
        tree.remove(new FxTreeNodeEdit(node1_1_loaded), FxTreeRemoveOp.Remove, true);
        assertTrue(!tree.exist(mode, id1_3));
        assertTrue(tree.getNode(mode, FxTreeNode.ROOT_NODE).getDirectChildCount() == 0,
                "Expected to have 0 children, got: [" + tree.getNode(mode, FxTreeNode.ROOT_NODE).getDirectChildCount() + "]");
        clearTrees();

        //test changing a referenced content
        ContentEngine co = getContentEngine();
        FxPK testFolder = co.save(co.initialize(FxType.FOLDER));
        try {
            long nodeId = tree.save(FxTreeNodeEdit.createNew("Test").setParentNodeId(FxTreeNode.ROOT_NODE).setMode(mode));
            FxTreeNode node = tree.getNode(mode, nodeId);
            assertNotSame(node.getReference(), testFolder, "Expected to have a different folder pk!");
            tree.save(node.asEditable().setReference(testFolder));
            node = tree.getNode(mode, nodeId);
            assertEquals(node.getReference(), testFolder, "Node reference should have been updated!");
        } finally {
            co.remove(testFolder);
        }
    }

    /**
     * Test node and subtree activation
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void activationTest() throws FxApplicationException {
        //clear live and edit tree
        clearTrees();
        //test activation without subtree
        long[] ids = tree.createNodes(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, 0, "/Test1/Test2/Test3");
        tree.activate(FxTreeMode.Edit, ids[0], false, true);
        checkTreeExists(FxTreeMode.Live, ids, new boolean[] { true, false, false });

        //test activation with subtree
        ids = tree.createNodes(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, 0, "/ATest1/ATest2/ATest3");
        tree.activate(FxTreeMode.Edit, ids[0], true, true);
        checkTreeExists(FxTreeMode.Live, ids, new boolean[] { true, true, true });

        // activate tree that has activated parts
        tree.activate(FxTreeMode.Edit, ids[0], true, true);
        checkTreeExists(FxTreeMode.Live, ids, new boolean[] { true, true, true });

        // set content without live step
        final long nodeId = ids[2];
        final FxContent content = getContentEngine().load(tree.getNode(
                FxTreeMode.Edit,
                ids[2]
        ).getReference());

        content.setStepByDefinition(StepDefinition.EDIT_STEP_ID);
        getContentEngine().save(content);

        tree.activate(FxTreeMode.Edit, ids[0], true, false);
        checkTreeExists(FxTreeMode.Edit, nodeId, true);
        checkTreeExists(FxTreeMode.Live, nodeId, false);
        // activate content indirectly
        tree.activate(FxTreeMode.Edit, ids[1], true, true);
        checkTreeExists(FxTreeMode.Live, nodeId, true);

        //there should be 2 nodes not active (Test2 and Test3), see if they get activated after activating all
        tree.activate(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, true, true);
        //assertEquals(6, tree.getTree(FxTreeMode.Live, FxTreeNode.ROOT_NODE, 3).getTotalChildCount());

        // remove an activated node
        tree.remove(FxTreeMode.Live, ids[1], FxTreeRemoveOp.Unfile, false);
        tree.remove(FxTreeMode.Edit, ids[2], FxTreeRemoveOp.Unfile, true);
        checkTreeExists(FxTreeMode.Edit, new long[] { ids[1], ids[2] }, new boolean[] { true, false });
        //checkTreeExists(FxTreeMode.Live, new long[] { ids[1], ids[2] }, new boolean[] { false, true });

        // activate changes through parent node
        tree.activate(FxTreeMode.Edit, ids[1], false, false);
        checkTreeExists(FxTreeMode.Live, new long[] { ids[1], ids[2] }, new boolean[] { true, false });
    }

    private void checkTreeExists(FxTreeMode treeMode, long id, boolean exists) throws FxApplicationException {
        checkTreeExists(treeMode, new long[] { id } , new boolean[] { exists });
    }

    private void checkTreeExists(FxTreeMode treeMode, long[] ids, boolean[] exists) throws FxApplicationException {
        assertEquals(
                exists.length, ids.length,
                "Programming error: exists has " + exists.length + " elements, but there are " + ids.length + " ids."
        );
        for (int i = 0; i < ids.length; i++) {
            assertEquals(tree.exist(treeMode, ids[i]), exists[i], "TreeEngine#exist returns unexpected result (i=" + i + ")");
        }
    }

    /**
     * Test behaviour when contents that are references of a tree node are removed
     * Expected behaviour: every tree node is a contentinstance itself. Removing the respective
     * content instance should therefore remove the relevant entry from the tree;
     * Live tree: childNodes are always removed!
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void contentRemoval() throws FxApplicationException {
        //clear live and edit tree
        clearTrees();
        FxPK contentPK;
        FxTreeMode mode = FxTreeMode.Edit;
        long[] nodes = createTestTree(mode);

        FxTreeNode n1 = tree.getNode(mode, nodes[0]);
        FxTreeNode n2 = tree.getNode(mode, nodes[1]);
        FxTreeNode n3 = tree.getNode(mode, nodes[2]);
        FxTreeNode childNode = tree.findChild(mode, nodes[1], "node_2_1");

        assertEquals(tree.getPathById(mode, n1.getId()), "/node_1");
        assertEquals(tree.getPathById(mode, n2.getId()), "/node_1/node_2");
        assertEquals(tree.getPathById(mode, n3.getId()), "/node_1/node_2/node_3");
        assertEquals(tree.getPathById(mode, childNode.getId()), "/node_1/node_2/node_2_1");

        // get the content reference of node2 and remove the content
        contentPK = n2.getReference();
        ce.remove(contentPK);

        // get the child node of nodes[0] which now must be "node_2_1"
        FxTreeNode n4 = tree.findChild(mode, nodes[0], "node_2_1");
        assertEquals(n4.getId(), childNode.getId());
        // check the new paths
        assertEquals(tree.getPathById(mode, n1.getId()), "/node_1");
        assertEquals(tree.getPathById(mode, n3.getId()), "/node_1/node_3");
        assertEquals(tree.getPathById(mode, childNode.getId()), "/node_1/node_2_1");
        assertEquals(tree.getPathById(mode, n2.getId()), "_"); // no path for the removed node

        try {
            tree.getNode(mode, nodes[1]);
            fail("Retrieving node " + nodes[1] + " should have thrown an exception");
        } catch (FxLoadException e) {
            // expected
        }

        // clean up
        tree.remove(mode, nodes[0], FxTreeRemoveOp.Remove, true);

        // perform tests in live tree: Removing a node from the live tree removes all its children (to avoid
        // inconsistencies with the edit tree)
        mode = FxTreeMode.Live;
        nodes = createTestTree(mode);

        n1 = tree.getNode(mode, nodes[0]);
        n2 = tree.getNode(mode, nodes[1]);
        n3 = tree.getNode(mode, nodes[2]);
        childNode = tree.findChild(mode, nodes[1], "node_2_1");

        assertEquals(tree.getPathById(mode, n1.getId()), "/node_1");
        assertEquals(tree.getPathById(mode, n2.getId()), "/node_1/node_2");
        assertEquals(tree.getPathById(mode, n3.getId()), "/node_1/node_2/node_3");
        assertEquals(tree.getPathById(mode, childNode.getId()), "/node_1/node_2/node_2_1");

        contentPK = n2.getReference();
        ce.remove(contentPK);

        try { // trying to find the same child node as in the edit tree should throw an exception
            tree.findChild(mode, nodes[0], "node_2_1");
            fail("Trying to retrieve node_2_1 should have thrown an exception");
        } catch (FxNotFoundException e) {
            // expected
        }
        assertEquals(tree.getPathById(mode, n1.getId()), "/node_1");
        assertEquals(tree.getPathById(mode, childNode.getId()), "_");

        // clean up
        tree.remove(mode, nodes[0], FxTreeRemoveOp.Remove, true);
    }

    /**
     * Create a lot of nodes trying to provoke errors
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void massCreateTest() throws FxApplicationException {
        clearTrees();
        disableTreeChecks();    // disable to improve performance
        try {
            FxTreeMode mode = FxTreeMode.Edit;
            String path = "/t1/t2/t3/t4/t5/t6/t7/t8/t9/t10";
            for (int i = 0; i < 3; i++) {
                path = path + "/f" + (i + 1);
                tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, path);
                FxTreeNode parent = tree.getNode(mode, tree.getIdByPath(mode, path));
                createSubNodes(parent, 40);
            }
            // check consistency
            enableTreeChecks();
            triggerTreeCheck(mode);
        } finally {
            enableTreeChecks();
        }
    }

    private void triggerTreeCheck(FxTreeMode mode) throws FxApplicationException {
        final long testId = tree.save(FxTreeNodeEdit.createNew("test").setParentNodeId(FxTreeNode.ROOT_NODE));
        if (FxTreeMode.Live == mode) {
            tree.activate(FxTreeMode.Edit, testId, false, true);
        }
        tree.remove(FxTreeMode.Edit, testId, FxTreeRemoveOp.Remove, false);
    }

    /**
     * Create count subnodes attached to parent
     *
     * @param parent parent node
     * @param count  number of subnodes to create
     * @throws FxApplicationException on errors
     */
    private void createSubNodes(FxTreeNode parent, int count) throws FxApplicationException {
        FxTreeNodeEdit node;
        for (int i = 0; i < count; i++) {
            node = FxTreeNodeEdit.createNewChildNode(parent).setName("Node" + i);
            tree.save(node);
        }
    }

    public static long scriptCounter = 0;

    /**
     * Test add/remove node scripting
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void scriptingAddRemoveTest() throws FxApplicationException {
        clearTrees();
        FxTreeMode mode = FxTreeMode.Edit;
        scriptCounter = 0;
        String code = "println \"[Groovy script]=== After adding node ${node.id}: ${node.path} ===\"\n" +
                "com.flexive.tests.embedded.FxTreeTest.scriptCounter++";
        FxScriptInfo siAdd = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.AfterTreeNodeAdded,
                "afterNodeAdded.gy", "Test script", code);
        code = "println \"[Groovy script]=== Before removing node ${node.id}: ${node.path} ===\"\n" +
                "com.flexive.tests.embedded.FxTreeTest.scriptCounter -= 2";
        FxScriptInfo siBeforeRemove = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.BeforeTreeNodeRemoved,
                "beforeNodeRemoved.gy", "Test script", code);
        code = "println \"[Groovy script]=== After removing node ${node.id}: ${node.path} ===\"\n" +
                "com.flexive.tests.embedded.FxTreeTest.scriptCounter++";
        FxScriptInfo siAfterRemove = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.AfterTreeNodeRemoved,
                "afterNodeRemoved.gy", "Test script", code);
        try {
            assertEquals(scriptCounter, 0);
            long nodeId = tree.save(FxTreeNodeEdit.createNew("Test1").setMode(mode).setParentNodeId(FxTreeNode.ROOT_NODE));
            assertEquals(scriptCounter, 1);
            long topNode = tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "/A/B/C")[0];
            assertEquals(scriptCounter, 4);
            tree.copy(mode, topNode, nodeId, 0);
            assertEquals(scriptCounter, 7);
            tree.remove(tree.getNode(mode, topNode), FxTreeRemoveOp.Unfile, true);
            assertEquals(scriptCounter, 4);
            tree.remove(tree.getNode(mode, nodeId), FxTreeRemoveOp.Unfile, true);
            assertEquals(scriptCounter, 0);
        } finally {
            scripting.remove(siAdd.getId());
            scripting.remove(siBeforeRemove.getId());
            scripting.remove(siAfterRemove.getId());
        }
    }

    /**
     * Test activate node scripting
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void scriptingActivateTest() throws FxApplicationException {
        clearTrees();
        FxTreeMode mode = FxTreeMode.Edit;
        scriptCounter = 0;
        String code = "println \"[Groovy script]=== Before activating node ${node.id}: ${node.path} ===\"\n" +
                "com.flexive.tests.embedded.FxTreeTest.scriptCounter+=2";
        FxScriptInfo siBeforeActivate = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.BeforeTreeNodeActivated,
                "beforeNodeActivate.gy", "Test script", code);
        code = "println \"[Groovy script]=== After activating node ${node.id}: ${node.path} ===\"\n" +
                "com.flexive.tests.embedded.FxTreeTest.scriptCounter--";
        FxScriptInfo siAfterActivate = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.AfterTreeNodeActivated,
                "afterNodeActivate.gy", "Test script", code);
        try {
            assertEquals(scriptCounter, 0);
            long nodeId = tree.save(FxTreeNodeEdit.createNew("Test1").setMode(mode).setParentNodeId(FxTreeNode.ROOT_NODE));
            tree.activate(FxTreeMode.Edit, nodeId, false, true);
            assertEquals(scriptCounter, 1);

            long topNode = tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "/A/B/C")[0];
            tree.activate(FxTreeMode.Edit, topNode, true, true);
            assertEquals(scriptCounter, 4);
        } finally {
            scripting.remove(siBeforeActivate.getId());
            scripting.remove(siAfterActivate.getId());
        }
    }

    /**
     * Test replacing a content with a folder scripting
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void scriptingFolderReplacementTest() throws FxApplicationException {
        clearTrees();
        FxTreeMode mode = FxTreeMode.Edit;
        scriptCounter = 0;
        ContentEngine co = getContentEngine();
        FxPK testContent = co.save(co.initialize(FxType.ROOT_ID));
        String code = "println \"[Groovy script]=== AfterTreeNodeFolderReplacement node ${node.id}: ${node.path}. co-pk: ${content}, folder-pk: ${node.reference} ===\"\n" +
                "com.flexive.tests.embedded.FxTreeTest.scriptCounter=42\n" +
                "if(content.id == node.reference.id) com.flexive.tests.embedded.FxTreeTest.scriptCounter=-1";
        FxScriptInfo siAfterTreeNodeFolderReplacement = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.AfterTreeNodeFolderReplacement,
                "treeNodeFolderReplacement.gy", "Test script", code);
        try {
            assertEquals(scriptCounter, 0);
            tree.save(FxTreeNodeEdit.createNew("Test1").
                    setMode(mode).
                    setParentNodeId(FxTreeNode.ROOT_NODE).
                    setReference(testContent));
            tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "/Test1/A");
            co.remove(testContent);
            assertEquals(scriptCounter, 42);
        } finally {
            scripting.remove(siAfterTreeNodeFolderReplacement.getId());
            try {
                co.remove(testContent);
            } catch (FxApplicationException e) {
                //ignore
            }
        }
    }

    @Test
    public void treeIteratorTest() throws FxApplicationException {
        clearTrees();
        final String[] names = {"my", "virtual", "directory"};
        final long[] nodes = tree.createNodes(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, 0, StringUtils.join(names, "/"));
        int index = 0;
        for (FxTreeNode node : tree.getTree(FxTreeMode.Edit, nodes[0], 5)) {
            assertTrue(node.getName().equals(names[index]), "Expected node name: " + names[index] + ", got: " + node.getName());
            index++;
        }

        // test mixed folder/node access
        tree.createNodes(FxTreeMode.Edit, nodes[1], 999, "new/directory");
        final String[] expected = {"my", "virtual", "directory", "new", "directory"};
        /* new folder structure:
            my/
                virtual/
                    directory/
                    new/
                        directory/
         */
        index = 0;
        for (FxTreeNode node : tree.getTree(FxTreeMode.Edit, nodes[0], 5)) {
            assertTrue(node.getName().equals(expected[index]), "Expected node name: " + expected[index] + ", got: " + node.getName());
            index++;
        }
    }

    @Test
    public void treeCaptionPathToIdTest() throws FxApplicationException {
        clearTrees();
        FxTreeNodeEdit tn = FxTreeNodeEdit.createNew("NodeName");
        tn.setLabel(new FxString(false, "NodeLabel"));
        tn.setParentNodeId(FxTreeNode.ROOT_NODE);
        long nodeId = tree.save(tn);
        assertTrue(tree.getIdByLabelPath(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, "/NodeLabel") == nodeId);
        FxTreeNodeEdit tn2 = FxTreeNodeEdit.createNew("NodeName2");
        tn2.setLabel(new FxString(false, "NodeLabel2"));
        tn2.setParentNodeId(nodeId);
        long nodeId2 = tree.save(tn2);
        assertTrue(tree.getIdByLabelPath(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, "/NodeLabel/NodeLabel2") == nodeId2);
    }

    /**
     * Tests #findChild and #getNodesWithReference methods
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void findChildrenTest() throws FxApplicationException {
        clearTrees();
        final String node2 = "node_2";
        FxTreeMode mode1, mode2;
        FxTreeNode result;
        List<FxTreeNode> nodesWithRefResults;
        FxPK pk;
        final String contentXPath = "TREETESTTYPE/TESTPROP";
        final String testData = "testdata, default lang 1234567890";
        mode1 = FxTreeMode.Edit;
        mode2 = FxTreeMode.Live;
        long[] nodes = createTestTree(mode1);

        // find by name
        result = tree.findChild(mode1, nodes[0], node2);
        assertEquals(result.getName(), node2);
        try { // let's cause an exception
            tree.findChild(mode2, nodes[0], node2);
            fail("Should not be able to find " + node2 + " in Live tree");
        } catch (FxNotFoundException e) {
            // expected
        }

        pk = result.getReference();
        // find by reference id
        result = tree.findChild(mode1, nodes[0], pk.getId());
        assertEquals(result.getName(), node2);
        try { // let's cause an exception
            tree.findChild(mode2, nodes[0], pk.getId());
            fail("Should not be able to find " + node2 + " in Live tree");
        } catch (FxNotFoundException e) {
            // expected
        }

        // find by pk (reference)
        result = tree.findChild(mode1, nodes[0], pk);
        assertEquals(result.getName(), node2);
        try { // let's cause an exception
            tree.findChild(mode2, nodes[0], pk);
            fail("Should not be able to find " + node2 + " in Live tree");
        } catch (FxNotFoundException e) {
            // expected
        }

        // find by FxReference
        // this also tests creating a content instance of a type and using it as a tree node
        FxTreeNode parent = tree.getNode(mode1, nodes[0]);
        long typeId = createTestType();
        FxPK contentPK = createTestContent(parent, typeId, testData);
        FxReference ref = new FxReference(false, new ReferencedContent(contentPK));
        result = tree.findChild(mode1, nodes[0], ref);
        assertEquals(result.getReference(), contentPK);
        try { // let's cause an exception
            tree.findChild(mode2, nodes[0], ref);
            fail("Should not be able to find " + contentPK + " in Live tree");
        } catch (FxNotFoundException e) {
            // expected
        }

        // retrieve all nodes with references
        nodesWithRefResults = tree.getNodesWithReference(mode1, ref.getBestTranslation().getId());
        int resultOK = 0;
        FxTreeNode tmp = null;
        for (FxTreeNode node : nodesWithRefResults) {
            if (node.getParentNodeId() == nodes[0]) {
                resultOK++;
                tmp = node;
            }
        }
        assertEquals(resultOK, 1); // should find exactly one node with a content reference
        try {
            FxContent co = ce.load(tmp.getReference());
            assertEquals(co.getValue(contentXPath).toString(), testData);
        } catch (NullPointerException e) { // Idea satisfaction guaranteed
            fail("The FxContent should have been found");
        }

        // clean up
        ce.remove(contentPK);
        ty.remove(typeId);
        tree.remove(mode1, nodes[0], FxTreeRemoveOp.Remove, true);
    }

    /**
     * Tests the #getPaths() and #getPathById methods
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void getPathsTest() throws FxApplicationException {
        clearTrees();
        FxTreeMode mode = FxTreeMode.Edit;
        long[] nodes = createTestTree(mode);
        final String node2_2 = "node_2_2";
        final String expectedPath1 = "/node_1/node_2/node_2_2";
        final String expectedPath2 = "/node_1/node_2/node_3";
        final long invalidNodeId = 1234567890;
        int pathsOK = 0;

        FxTreeNode node = tree.findChild(mode, nodes[1], node2_2);

        // retrieve the paths for node_2.2 and node_3
        for (String path : tree.getPaths(mode, nodes[2], node.getId())) {
            if (path.equals(expectedPath1) || path.equals(expectedPath2))
                pathsOK++;
        }
        assertEquals(pathsOK, 2); // should find 2 correct paths
        pathsOK = 0; // reset

        // assert that an invalid node id will result in an empty path (represented by "_")
        String returnedPath = "";
        for (String path : tree.getPaths(mode, invalidNodeId)) {
            if (path == null || path.equals("_")) // returns an underscore if no path was found
                returnedPath = path;
            pathsOK++;
        }
        assertTrue(pathsOK > 0);
        assertEquals(returnedPath, "_");

        // test getPathById()
        String path = tree.getPathById(mode, nodes[2]);
        assertEquals(path, expectedPath2);

        // clean up
        tree.remove(mode, nodes[0], FxTreeRemoveOp.Remove, true);
    }

    /**
     * STUB TEST: #setData and #getData are not implemented yet!!
     * Tests the #setData and #getData methods
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void setAndGetDataTest() throws FxApplicationException {
        clearTrees();
        FxTreeMode mode = FxTreeMode.Edit;
        final String data1 = "Template A";
        final String data2 = "Template B";
        long nodes[] = createTestTree(mode);

        try {
            tree.setData(mode, nodes[0], data1);
            tree.setData(mode, nodes[1], data2);
        } catch (RuntimeException e) {
            // expected, method throws UnsupportedOperationException
        }

        try {
            tree.getDatas(mode, nodes[2]);
        } catch (RuntimeException e) {
            // expected, method throws UnsupportedOperationException
        }
        // clean up
        tree.remove(mode, nodes[0], FxTreeRemoveOp.Remove, true);
    }

    /**
     * Tests the #getIdChain and #getReverseIdChainmethods
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void getIdsTest() throws FxApplicationException {
        clearTrees();
        FxTreeMode mode = FxTreeMode.Edit;
        long nodes[] = createTestTree(mode);
        long invalidId = 1234567890;

        long[] ids = tree.getIdChain(mode, nodes[2]); // should return 1, nodes[0], nodes[1], nodes[2]
        assertEquals(ids[0], 1);
        assertEquals(ids[1], nodes[0]);
        assertEquals(ids[2], nodes[1]);
        assertEquals(ids[3], nodes[2]);

        assertEquals(tree.getIdChain(mode, invalidId), null); // should be null

        ids = tree.getReverseIdChain(mode, nodes[2]); // should return nodes[2], nodes[1], nodes[0], 1
        assertEquals(ids[0], nodes[2]);
        assertEquals(ids[1], nodes[1]);
        assertEquals(ids[2], nodes[0]);
        assertEquals(ids[3], 1);

        // clean up
        tree.remove(mode, nodes[0], FxTreeRemoveOp.Remove, true);
    }

    /**
     * This tests the #populate method
     *
     * @throws FxApplicationException on errors
     */
    @Test(enabled = false)
    public void populateTest() throws FxApplicationException {
        clearTrees();
        disableTreeChecks();
        try {
            // create the testdata in the live tree, assert that names of the root nodes
            FxTreeMode mode = FxTreeMode.Live;
            FxTreeNode n1, n2, n3;
            int maxLevel = 1;
            final String node1 = "Level2_0_";
            final String node2 = "Level3_0_9_";
            final String node3 = "Level4_0_9_99_";

            try {
                tree.populate(mode, maxLevel);
            } catch (FxApplicationException e) {
                fail(e.getMessage());
            }
            n1 = tree.findChild(mode, 1, node1);
            n2 = tree.findChild(mode, n1.getId(), node2);
            n3 = tree.findChild(mode, n2.getId(), node3);

            assertEquals(n1.getName(), node1);
            assertEquals(n2.getName(), node2);
            assertEquals(n3.getName(), node3);

            //clean up
            tree.remove(mode, n1.getId(), FxTreeRemoveOp.Remove, true);

            enableTreeChecks();
            triggerTreeCheck(mode);
        } finally {
            enableTreeChecks();
        }
    }


    /**
     * GenericTreeStorage: #setData method
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void genericTreeStorageSetDataTest() throws FxApplicationException {
        clearTrees();
        FxTreeMode mode = FxTreeMode.Edit;
        long[] nodes = createTestTree(mode);
        String data = "TEMPLATE A";
        TreeStorage treeStorage = new TreeStorage();

        Connection con = null;
        try {
            con = Database.getDbConnection();
            treeStorage.setData(con, mode, nodes[0], data);

            FxTreeNode n = tree.getNode(mode, nodes[0]);
            assertEquals(n.getData(), data);

        } catch (Throwable t) {
            fail("GenericTreeStorage #setData shouldn't throw an exception. Exception: " + t.getMessage());
            t.printStackTrace();
        } finally {
            Database.closeObjects(FxTreeTest.class, con, null);
            // clean up
            tree.remove(mode, nodes[0], FxTreeRemoveOp.Remove, true);
        }
    }

    /**
     * GenericTreeStorage: #copy & #getCopyOfCount (explicitly)
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void genericTreeStorageGetCopyOfCountTest() throws FxApplicationException {
        clearTrees();
        FxTreeMode mode = FxTreeMode.Edit;
        long[] nodes = createTestTree(mode);
        long typeId = createTestType();
        FxTreeNode node = tree.getNode(mode, nodes[0]);
        final String testData = "TESTDATA TO BE COPIED";
        final int expectedCopyCount = 1; // expected copy count result - we will copy a node exactly once
        TreeStorage treeStorage = new TreeStorage();
        FxPK contentPK = createTestContent(node, typeId, testData);
        FxTreeNode contentNode1 = tree.findChild(mode, nodes[0], contentPK);

        long copyId = tree.copy(mode, contentNode1.getId(), nodes[2], 0);
        FxTreeNode contentNode2 = tree.findChild(mode, nodes[2], contentPK); // get the new tree node

        assertEquals(copyId, contentNode2.getId());
        assertEquals(contentNode2.getName(), contentNode1.getName());
        assertEquals(contentNode1.getReference(), contentNode2.getReference());

        Connection con = null;
        try {
            con = Database.getDbConnection();
            int actualCopyCount = treeStorage.getCopyOfCount(con, mode, "", nodes[2], copyId);
            assertEquals(actualCopyCount, expectedCopyCount);
        } catch (Exception e) {
            fail("GenericTreeStorage #getCopyOfCount shouldn't throw an exception. Exception: " + e.getMessage());
        } finally {
            Database.closeObjects(FxTreeTest.class, con, null);
            // clean up
            ce.remove(contentPK);
            ty.remove(typeId);
            tree.remove(mode, nodes[0], FxTreeRemoveOp.Remove, true);
        }
    }

    /**
     * GenericTreeStorage: #flagDirty method
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void genericTreeStorageFlagDirtyTest() throws FxApplicationException {
        clearTrees();
        TreeStorage treeStorage = new TreeStorage();
        FxTreeMode mode = FxTreeMode.Edit;
        long nodeId = createTestTree(mode)[0];

        Connection con = null;
        try {
            con = Database.getDbConnection();
            treeStorage.flagDirty(con, mode, nodeId);
            FxTreeNode n = tree.getNode(mode, nodeId);
            assertTrue(n.isDirty());
        } catch (Exception e) {
            fail("Failed to flag node " + nodeId + " dirty. Exception: " + e.getMessage());
        } finally {
            Database.closeObjects(FxTreeTest.class, con, null);
            // clean up
            tree.remove(mode, nodeId, FxTreeRemoveOp.Remove, true);
        }
    }

    @Test
    public void manyMovesTest() throws FxApplicationException {
        // stress tree engine moves somewhat in reaction to bug FX-724, however, I haven't been able
        // to reproduce it yet.

        // create nodes in the root node and in one subfolder
        final List<Long> rootNodeIds = Lists.newArrayList();    // root node IDs
        final List<Long> folderNodeIds = Lists.newArrayList();

        try {
            disableTreeChecks();
            final long folderId = getTreeEngine().save(FxTreeNodeEdit.createNew("FX-724").setParentNodeId(FxTreeNode.ROOT_NODE));
            final int numNodes = 30;
            for (int i = 0; i < numNodes; i++) {
                rootNodeIds.add(
                        getTreeEngine().save(FxTreeNodeEdit.createNew("test" + i).setParentNodeId(FxTreeNode.ROOT_NODE))
                );
                folderNodeIds.add(
                        getTreeEngine().save(FxTreeNodeEdit.createNew("nested" + i).setParentNodeId(folderId))
                );
            }
            try {
                randomMoves(FxTreeNode.ROOT_NODE, numNodes / 2);
                randomMoves(folderId, numNodes / 2);
            } finally {
                enableTreeChecks();
                getTreeEngine().remove(FxTreeMode.Edit, folderId, FxTreeRemoveOp.Remove, true);
                for (long nodeId : rootNodeIds) {
                    getTreeEngine().remove(FxTreeMode.Edit, nodeId, FxTreeRemoveOp.Remove, false);
                }
            }
        } finally {
            enableTreeChecks();
        }
    }

    private void randomMoves(long parentNodeId, int times) throws FxApplicationException {
        // create client-side copy to check if the moves are implemented correctly
        final List<FxTreeNode> children = Lists.newArrayList(
                getTreeEngine().getTree(FxTreeMode.Edit, parentNodeId, 1).getChildren()
        );
        assertTrue(children.size() > 0);

        for (int i = 0; i < times; i++) {
            final FxTreeNode child = children.get(RandomUtils.nextInt(children.size()));
            final int oldPosition = children.indexOf(child);
            int newPosition = oldPosition;
            while (newPosition == oldPosition) {
                newPosition = RandomUtils.nextInt(children.size());
            }

            assertEquals(
                    getTreeEngine().getNode(FxTreeMode.Edit, child.getId()).getPosition(), oldPosition,
                    "Invalid old node position."
            );

            getTreeEngine().move(FxTreeMode.Edit, child.getId(), parentNodeId, newPosition);

            children.remove(oldPosition);
            children.add(newPosition, child);

            assertEquals(
                FxSharedUtils.getSelectableObjectIdList(
                        getTreeEngine().getTree(FxTreeMode.Edit, parentNodeId, 1).getChildren()
                ),
                FxSharedUtils.getSelectableObjectIdList(
                        children
                ),
                "Tree children order mixed up."
            );
        }
    }

    /**
     * Helper class for testing GenericTreeStorage methods
     */
    class TreeStorage extends GenericTreeStorageSpreaded {
        public TreeStorage() {
            super();
        }

        public int getCopyOfCount(Connection con, FxTreeMode mode, String copyOfPrefix, long parentNodeId, long nodeId) throws FxTreeException {
            return super.getCopyOfCount(con, mode, copyOfPrefix, parentNodeId, nodeId);
        }

        public void setData(Connection con, FxTreeMode mode, long nodeId, String data) throws FxApplicationException {
            super.setData(con, mode, nodeId, data);
        }

        public void flagDirty(Connection con, FxTreeMode mode, long nodeId) throws FxUpdateException {
            super.flagDirty(con, mode, nodeId);
        }
    }


    /**
     * Helper method creates a test tree
     *
     * @param mode FxTreeMode (e.g. .Edit)
     * @return long[] array of tree node ids
     * @throws FxApplicationException on errors
     */
    private long[] createTestTree(FxTreeMode mode) throws FxApplicationException {
        FxTreeMode _mode = mode;
        if (mode == FxTreeMode.Live)
            _mode = FxTreeMode.Edit;
        final long[] nodes = tree.createNodes(_mode, FxTreeNode.ROOT_NODE, 0, "node_1/node_2/node_3");
        tree.createNodes(_mode, (int) nodes[1], 0, "node_2_1");
        tree.createNodes(_mode, (int) nodes[1], 1, "node_2_2");
        tree.createNodes(_mode, (int) nodes[2], 0, "node_3_1");
        if (mode == FxTreeMode.Live)
            tree.activate(_mode, nodes[0], true, true);
        return nodes;
    }

    /**
     * Creates a test type
     *
     * @return the id of the test type
     * @throws FxApplicationException on errors
     */
    private long createTestType() throws FxApplicationException {
        long typeId = EJBLookup.getTypeEngine().save(FxTypeEdit.createNew("TREETESTTYPE", new FxString("Tree test type"),
                getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()), null));

        /*EJBLookup.getAssignmentEngine().createProperty(typeId, FxPropertyEdit.createNew(
                "TESTPROP", new FxString(true, FxLanguage.ENGLISH, "TESTPROP"), new FxString(true, FxLanguage.ENGLISH, "TESTPROP"),
                new FxMultiplicity(0, 5), CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024).setMultiLang(false), "/");*/
        EJBLookup.getAssignmentEngine().save(FxPropertyAssignmentEdit.reuse("ROOT/FQN", "TREETESTTYPE", "/", "TESTPROP"), false);
        return typeId;
    }

    /**
     * Creates a test content instance for the reference search and attaches the content to a given FxTreeNode
     *
     * @param parentNode the parent FxTreeNode to which the content should be attached
     * @param typeId     the id of the type for which the content instance will be created
     * @param testData   a String value for test data
     * @return FxPK the content instance PK
     * @throws FxApplicationException on errors
     */
    private FxPK createTestContent(FxTreeNode parentNode, long typeId, String testData) throws FxApplicationException {
        // create content for testing - will produce console output
        FxString data = new FxString(false, testData);

        FxContent co = ce.initialize(typeId);
        FxPK contentPK = ce.save(co);
        co = ce.load(contentPK);
        co.setValue("/TESTPROP[1]", data);
        contentPK = ce.save(co);
        tree.save(FxTreeNodeEdit.createNew(String.valueOf(contentPK)).setParentNodeId(parentNode.getId()).setReference(contentPK));

        return contentPK;
    }

    @Test
    public void removeOpTest() throws FxApplicationException {
        long typeId = createTestType();

        try {
            clearTrees();
            TreeStorage treeStorage = new TreeStorage();
            FxTreeMode mode = FxTreeMode.Edit;
            FxTreeNodeEdit tne = FxTreeNodeEdit.createNew("TestRemove").setMode(mode);

            FxTreeNode rootNode = tree.getNode(mode, FxTreeNode.ROOT_NODE);

            FxPK pk = createTestContent(rootNode, typeId, "TestData123");
            long nodeId = tree.getIdByPath(mode, "/TestData123");
            assertEquals(tree.getNode(mode, nodeId).getReference().getId(), pk.getId());

            //Unfile: remove node but keep content
            tree.remove(tree.getNode(mode, nodeId), FxTreeRemoveOp.Unfile, false);
            assertFalse(tree.exist(mode, nodeId), "Node should have been removed!");
            try {
                ce.getContentVersionInfo(pk);
            } catch (FxApplicationException e) {
                fail("Expected pk to exist!", e);
            }

            nodeId = tree.save(FxTreeNodeEdit.createNew("TestData4").setReference(pk).setMode(mode).setParentNodeId(FxTreeNode.ROOT_NODE));
            assertEquals(tree.getNode(mode, nodeId).getReference().getId(), pk.getId()); //check exists and pk is correct

            //Remove: remove node AND content
            tree.remove(tree.getNode(mode, nodeId), FxTreeRemoveOp.Remove, false);
            assertFalse(tree.exist(mode, nodeId), "Node should have been removed!");
            try {
                ce.getContentVersionInfo(pk);
                fail("Content should no longer exist!");
            } catch (FxApplicationException e) {
                //expected
            }

            pk = createTestContent(rootNode, typeId, "TestData123");
            nodeId = tree.getIdByPath(mode, "/TestData123");
            assertEquals(tree.getNode(mode, nodeId).getReference().getId(), pk.getId()); //check exists and pk is correct

            //RemoveSingleFiled: remove node AND content if content is not referenced from other nodes
            //test1: like Remove the content should be removed
            tree.remove(tree.getNode(mode, nodeId), FxTreeRemoveOp.RemoveSingleFiled, false);
            assertFalse(tree.exist(mode, nodeId), "Node should have been removed!");
            try {
                ce.getContentVersionInfo(pk);
                fail("Content should no longer exist!");
            } catch (FxApplicationException e) {
                //expected
            }

            pk = createTestContent(rootNode, typeId, "TestData123");
            nodeId = tree.getIdByPath(mode, "/TestData123");
            long nodeId2 = tree.save(FxTreeNodeEdit.createNew("TestData5").setReference(pk).setMode(mode).setParentNodeId(FxTreeNode.ROOT_NODE));
            assertEquals(tree.getNode(mode, nodeId).getReference().getId(), pk.getId()); //check exists and pk is correct

            //RemoveSingleFiled: remove node AND content if content is not referenced from other nodes
            //test2: content should still exist since it is referenced by 2 nodes
            tree.remove(tree.getNode(mode, nodeId), FxTreeRemoveOp.RemoveSingleFiled, false);
            assertFalse(tree.exist(mode, nodeId), "Node should have been removed!");
            assertTrue(tree.exist(mode, nodeId2), "Node2 should still exist!");
            try {
                ce.getContentVersionInfo(pk);
            } catch (FxApplicationException e) {
                fail("Expected pk to exist!", e);
            }

            nodeId = tree.save(FxTreeNodeEdit.createNew("TestData6").setReference(pk).setMode(mode).setParentNodeId(FxTreeNode.ROOT_NODE));
            assertEquals(tree.getTree(mode, FxTreeNode.ROOT_NODE, 10).getDirectChildCount(), 2, "");

            //UnfileAll: remove both nodes but not the content
            tree.remove(tree.getNode(mode, nodeId), FxTreeRemoveOp.UnfileAll, false);
            assertFalse(tree.exist(mode, nodeId), "Node should have been removed!");
            assertFalse(tree.exist(mode, nodeId2), "Node2 should have been removed!");
            try {
                ce.getContentVersionInfo(pk);
            } catch (FxApplicationException e) {
                fail("Expected pk to exist!", e);
            }
        } finally {
            // clean up
            ce.removeForType(typeId);
            ty.remove(typeId);
        }
    }
}
