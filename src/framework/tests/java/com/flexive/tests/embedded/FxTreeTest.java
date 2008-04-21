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
package com.flexive.tests.embedded;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.interfaces.TreeEngine;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.tree.FxTreeNodeEdit;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tree engine tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "tree"})
public class FxTreeTest {
    TreeEngine tree;
    ScriptingEngine scripting;

    @BeforeClass
    public void beforeClass() throws Exception {
        login(TestUsers.SUPERVISOR);
        tree = EJBLookup.getTreeEngine();
        scripting = EJBLookup.getScriptingEngine();
        EJBLookup.getDivisionConfigurationEngine().put(SystemParameters.TREE_CHECKS_ENABLED, true);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        try {
            tree.clear(FxTreeMode.Live);
            tree.clear(FxTreeMode.Edit);
            EJBLookup.getDivisionConfigurationEngine().put(SystemParameters.TREE_CHECKS_ENABLED, false);
        } catch (FxApplicationException e) {
            //ignore
        }
        logout();
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
     * Live tests
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void treeTestLive() throws FxApplicationException {
        treeCRUD(FxTreeMode.Live);
        createPath(FxTreeMode.Live);
    }

    /**
     * Test path creation
     *
     * @param mode FxTreeMode
     * @throws FxApplicationException on errors
     */
    private void createPath(FxTreeMode mode) throws FxApplicationException {
        final long[] nodes = tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "my/virtual/directory");
        Assert.assertEquals(nodes.length, 3, "Should have created 3 nodes");
        Assert.assertEquals(tree.getNode(mode, nodes[0]).getName(), "my");
        Assert.assertEquals(tree.getNode(mode, nodes[1]).getName(), "virtual");
        Assert.assertEquals(tree.getNode(mode, nodes[2]).getName(), "directory");

        // create subfolder, reuse two folders
        final long[] tmpNodes = tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "my/virtual/tmp");
        Assert.assertEquals(tmpNodes.length, 3, "Should have returned 3 nodes");
        Assert.assertEquals(tree.getNode(mode, tmpNodes[0]).getName(), "my");
        Assert.assertEquals(tree.getNode(mode, tmpNodes[1]).getName(), "virtual");
        Assert.assertEquals(tree.getNode(mode, tmpNodes[2]).getName(), "tmp");
        Assert.assertEquals(nodes[0], tmpNodes[0]);
        Assert.assertEquals(nodes[1], tmpNodes[1]);

        // use /my/virtual as root node
        final long[] nestedNodes = tree.createNodes(mode, nodes[1], 0, "/directory/tmp2");
        Assert.assertEquals(nestedNodes.length, 2);
        Assert.assertEquals(tree.getNode(mode, nestedNodes[0]).getName(), "directory");
        Assert.assertEquals(tree.getNode(mode, nestedNodes[1]).getName(), "tmp2");
        Assert.assertEquals(nestedNodes[0], nodes[2]);  // check "directory" node
    }

    /**
     * Test most methods of the simplified API
     *
     * @param mode FxTreeMode to test
     * @throws FxApplicationException on errors
     */
    private void treeCRUD(FxTreeMode mode) throws FxApplicationException {
        //clear the tree
        tree.clear(mode);
        assert tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() == 0 :
                "Expected to have 0 children, got: [" + tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() + "]";
        //create new node
        FxTreeNodeEdit node1 = FxTreeNodeEdit.createNew(getNodeName(1));
        node1.setLabel(getNodeLabel(1));
        node1.setMode(mode);
        long id1 = tree.save(node1);
        assert tree.exist(mode, id1);
        assert tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() == 1 :
                "Expected to have 1 child, got: [" + tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() + "]";
        //load and check if all well
        FxTreeNode node1_loaded = tree.getNode(mode, id1);
        assert node1_loaded.getName().equals(node1.getName());
        Assert.assertEquals(node1_loaded.getLabel(), node1.getLabel());
        //rename name
        tree.save(new FxTreeNodeEdit(node1_loaded).setName("abcd"));
        node1_loaded = tree.getNode(mode, id1);
        assert node1_loaded.getName().equals("abcd") : "Expected [abcd] - got [" + node1_loaded.getName() + "]";
        Assert.assertEquals(node1_loaded.getLabel(), node1.getLabel());
        //rename label
        tree.save(new FxTreeNodeEdit(node1_loaded).setLabel(getNodeLabel(42)));
        node1_loaded = tree.getNode(mode, id1);
        assert node1_loaded.getName().equals("abcd");
        assert node1_loaded.getLabel().equals(getNodeLabel(42));
        //create child
        FxTreeNodeEdit node1_1 = FxTreeNodeEdit.createNewChildNode(node1_loaded).setName("1").setLabel(getNodeLabel(1)).setMode(mode);
        long id1_1 = tree.save(node1_1);
        FxTreeNode node1_1_loaded = tree.getNode(mode, id1_1);
        assert node1_1_loaded.getParentNodeId() == node1_loaded.getId();
        //verify path
        assert node1_1_loaded.getPath().equals("/abcd/1");
        //verify label
        Assert.assertEquals(tree.getLabels(mode, node1_1_loaded.getId()).get(0), "/" + getNodeLabel(42).getBestTranslation() + "/" + getNodeLabel(1).getBestTranslation());
        //create 2 other children for positioning tests
        FxTreeNodeEdit node1_2 = FxTreeNodeEdit.createNewChildNode(node1_loaded).setName("2").setLabel(getNodeLabel(2)).setMode(mode);
        long id1_2 = tree.save(node1_2);
        FxTreeNode node1_2_loaded = tree.getNode(mode, id1_2);
        //verify path
        assert node1_2_loaded.getPath().equals("/abcd/2") : "Expected [/abcd/2] got: [" + node1_2_loaded.getPath() + "]";
        //verify label
        assert ("/" + getNodeLabel(42).getBestTranslation() + "/" + getNodeLabel(2).getBestTranslation()).
                equals(tree.getLabels(mode, node1_2_loaded.getId()).get(0)) :
                "Expected [/" + getNodeLabel(42).getBestTranslation() + "/" + getNodeLabel(2).getBestTranslation() + "] got: [" +
                        tree.getLabels(mode, node1_2_loaded.getId()).get(0) + "]";
        FxTreeNodeEdit node1_3 = FxTreeNodeEdit.createNewChildNode(node1_loaded).setName("3").setLabel(getNodeLabel(3)).setMode(mode);
        long id1_3 = tree.save(node1_3);
        FxTreeNode node1_3_loaded = tree.getNode(mode, id1_3);
        assert tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() == 4 :
                "Expected to have 4 children, got: [" + tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() + "]";
        //verify positions - should be 1-2-3
        assert node1_1_loaded.getPosition() == 0 : "Expected [0] got: [" + node1_1_loaded.getPosition() + "]";
        assert node1_2_loaded.getPosition() == 1 : "Expected [1] got: [" + node1_2_loaded.getPosition() + "]";
        assert node1_3_loaded.getPosition() == 2 : "Expected [2] got: [" + node1_3_loaded.getPosition() + "]";

        //swap positions of 1 and 3 to net 3-2-1
        tree.save(new FxTreeNodeEdit(node1_3_loaded).setPosition(-1));
        tree.save(new FxTreeNodeEdit(node1_1_loaded).setPosition(100));
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_2_loaded = tree.getNode(mode, id1_2);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assert node1_1_loaded.getPosition() == 2 : "Expected [2] got: [" + node1_1_loaded.getPosition() + "]";
        assert node1_2_loaded.getPosition() == 1 : "Expected [1] got: [" + node1_2_loaded.getPosition() + "]";
        assert node1_3_loaded.getPosition() == 0 : "Expected [0] got: [" + node1_3_loaded.getPosition() + "]";
        //3-2-1 => 3-1-2
        tree.save(new FxTreeNodeEdit(node1_1_loaded).setPosition(1));
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_2_loaded = tree.getNode(mode, id1_2);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assert node1_1_loaded.getPosition() == 1 : "Expected [1] got: [" + node1_1_loaded.getPosition() + "]";
        assert node1_2_loaded.getPosition() == 2 : "Expected [2] got: [" + node1_2_loaded.getPosition() + "]";
        assert node1_3_loaded.getPosition() == 0 : "Expected [0] got: [" + node1_3_loaded.getPosition() + "]";
        //3-1-2 => 1-2-3
        tree.save(new FxTreeNodeEdit(node1_3_loaded).setPosition(4));
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_2_loaded = tree.getNode(mode, id1_2);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assert node1_1_loaded.getPosition() == 0 : "Expected [0] got: [" + node1_1_loaded.getPosition() + "]";
        assert node1_2_loaded.getPosition() == 1 : "Expected [1] got: [" + node1_2_loaded.getPosition() + "]";
        assert node1_3_loaded.getPosition() == 2 : "Expected [2] got: [" + node1_3_loaded.getPosition() + "]";
        //delete 1_2 and check positions
        tree.remove(new FxTreeNodeEdit(node1_2_loaded), true, false);
        assert !tree.exist(mode, id1_2);
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assert node1_1_loaded.getPosition() == 0 : "Expected [0] got: [" + node1_1_loaded.getPosition() + "]";
        assert node1_3_loaded.getPosition() == 1 : "Expected [1] got: [" + node1_3_loaded.getPosition() + "]";
        assert tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() == 3 :
                "Expected to have 3 children, got: [" + tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() + "]";
        if (mode == FxTreeMode.Live)
            return; //children are to be removed in live mode
        //delete parent but not children and check if they moved up in hierarchy
        tree.remove(new FxTreeNodeEdit(node1_loaded), true, false);
        node1_1_loaded = tree.getNode(mode, id1_1);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assert node1_1_loaded.getParentNodeId() == FxTreeNode.ROOT_NODE;
        assert node1_3_loaded.getParentNodeId() == FxTreeNode.ROOT_NODE;
        //attach 1_3 as child to 1_2
        tree.save(new FxTreeNodeEdit(node1_3_loaded).setParentNodeId(node1_1_loaded.getId()));
        node1_1_loaded = tree.getTree(mode, id1_1, 3);
        node1_3_loaded = tree.getNode(mode, id1_3);
        assert node1_1_loaded.getChildren().size() == 1;
        assert node1_1_loaded.getChildren().get(0).getId() == node1_3_loaded.getId();
        assert node1_3_loaded.getPath().equals("/1/3") : "Expected [/1/3] got: [" + node1_3_loaded.getPath() + "]";
        //delete 1_1 with children and check that 1_3 is gone too
        tree.remove(new FxTreeNodeEdit(node1_1_loaded), true, true);
        assert !tree.exist(mode, id1_3);
        assert tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() == 0 :
                "Expected to have 0 children, got: [" + tree.getNode(mode, FxTreeNode.ROOT_NODE).getTotalChildCount() + "]";
        tree.clear(mode);

        //test changing a referenced content
        ContentEngine co = EJBLookup.getContentEngine();
        FxPK testFolder = co.save(co.initialize(FxType.FOLDER));
        try {
            long nodeId = tree.save(FxTreeNodeEdit.createNew("Test").setParentNodeId(FxTreeNode.ROOT_NODE).setMode(mode));
            FxTreeNode node = tree.getNode(mode, nodeId);
            Assert.assertNotSame(node.getReference(), testFolder, "Expected to have a different folder pk!");
            tree.save(node.asEditable().setReference(testFolder));
            node = tree.getNode(mode, nodeId);
            Assert.assertEquals(node.getReference(), testFolder, "Node reference should have been updated!");
        } finally {
            co.remove(testFolder);
        }
        tree.clear(mode);
    }

    /**
     * Test node and subtree activation
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void activationTest() throws FxApplicationException {
        //clear live and edit tree
        tree.clear(FxTreeMode.Edit);
        tree.clear(FxTreeMode.Live);
        //test activation without subtree
        long[] ids = tree.createNodes(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, 0, "/Test1/Test2/Test3");
        tree.activate(FxTreeMode.Edit, ids[0], false);
        Assert.assertEquals(true, tree.exist(FxTreeMode.Live, ids[0]));
        Assert.assertEquals(false, tree.exist(FxTreeMode.Live, ids[1]));
        Assert.assertEquals(false, tree.exist(FxTreeMode.Live, ids[2]));
        //test activation with subtree
        ids = tree.createNodes(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, 0, "/ATest1/ATest2/ATest3");
        tree.activate(FxTreeMode.Edit, ids[0], true);
        Assert.assertEquals(true, tree.exist(FxTreeMode.Live, ids[0]));
        Assert.assertEquals(true, tree.exist(FxTreeMode.Live, ids[1]));
        Assert.assertEquals(true, tree.exist(FxTreeMode.Live, ids[2]));
        //there should be 2 nodes not active (Test2 and Test3), see if they get activated after activating all
        tree.activate(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, true);
        Assert.assertEquals(6, tree.getTree(FxTreeMode.Live, FxTreeNode.ROOT_NODE, 3).getTotalChildCount());
    }

    /**
     * Test behaviour when contents that are references of a tree node are removed
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void contentRemoval() throws FxApplicationException {
        //clear live and edit tree
        tree.clear(FxTreeMode.Edit);
        tree.clear(FxTreeMode.Live);
        //TODO: code me!
    }

    /**
     * Create a lot of nodes trying to provoke errors
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void massCreateTest() throws FxApplicationException {
        FxTreeMode mode = FxTreeMode.Edit;
        tree.clear(mode);
        String path = "/t1/t2/t3/t4/t5/t6/t7/t8/t9/t10";
        for (int i = 0; i < 3; i++) {
            path = path + "/f" + (i + 1);
            tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, path);
            FxTreeNode parent = tree.getNode(mode, tree.getIdByPath(mode, path));
            createSubNodes(parent, 40);
        }
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
//            System.out.println("Creating "+parent.getPath()+"/Node"+i);
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
        FxTreeMode mode = FxTreeMode.Edit;
        tree.clear(mode);
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
            Assert.assertEquals(scriptCounter, 0);
            long nodeId = tree.save(FxTreeNodeEdit.createNew("Test1").setMode(mode).setParentNodeId(FxTreeNode.ROOT_NODE));
            Assert.assertEquals(scriptCounter, 1);
            long topNode = tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "/A/B/C")[0];
            Assert.assertEquals(scriptCounter, 4);
            tree.copy(mode, topNode, nodeId, 0);
            Assert.assertEquals(scriptCounter, 7);
            tree.remove(tree.getNode(mode, topNode), true, true);
            Assert.assertEquals(scriptCounter, 4);
            tree.remove(tree.getNode(mode, nodeId), true, true);
            Assert.assertEquals(scriptCounter, 0);
        } finally {
            scripting.removeScript(siAdd.getId());
            scripting.removeScript(siBeforeRemove.getId());
            scripting.removeScript(siAfterRemove.getId());
        }
    }

    /**
     * Test activate node scripting
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void scriptingActivateTest() throws FxApplicationException {
        FxTreeMode mode = FxTreeMode.Edit;
        tree.clear(mode);
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
            Assert.assertEquals(scriptCounter, 0);
            long nodeId = tree.save(FxTreeNodeEdit.createNew("Test1").setMode(mode).setParentNodeId(FxTreeNode.ROOT_NODE));
            tree.activate(FxTreeMode.Edit, nodeId, false);
            Assert.assertEquals(scriptCounter, 1);

            long topNode = tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "/A/B/C")[0];
            tree.activate(FxTreeMode.Edit, topNode, true);
            Assert.assertEquals(scriptCounter, 4);
        } finally {
            scripting.removeScript(siBeforeActivate.getId());
            scripting.removeScript(siAfterActivate.getId());
            tree.clear(FxTreeMode.Edit);
            tree.clear(FxTreeMode.Live);
        }
    }

    /**
     * Test replacing a content with a folder scripting
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void scriptingFolderReplacementTest() throws FxApplicationException {
        FxTreeMode mode = FxTreeMode.Edit;
        tree.clear(mode);
        scriptCounter = 0;
        ContentEngine co = EJBLookup.getContentEngine();
        FxPK testContent = co.save(co.initialize(FxType.ROOT_ID));
        String code = "println \"[Groovy script]=== AfterTreeNodeFolderReplacement node ${node.id}: ${node.path}. co-pk: ${content}, folder-pk: ${node.reference} ===\"\n" +
                "com.flexive.tests.embedded.FxTreeTest.scriptCounter=42\n"+
                "if(content.id == node.reference.id) com.flexive.tests.embedded.FxTreeTest.scriptCounter=-1";
        FxScriptInfo siAfterTreeNodeFolderReplacement = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.AfterTreeNodeFolderReplacement,
                "treeNodeFolderReplacement.gy", "Test script", code);
        try {
            Assert.assertEquals(scriptCounter, 0);
            long nodeId = tree.save(FxTreeNodeEdit.createNew("Test1").
                    setMode(mode).
                    setParentNodeId(FxTreeNode.ROOT_NODE).
                    setReference(testContent));
            tree.createNodes(mode, FxTreeNode.ROOT_NODE, 0, "/Test1/A");
            co.remove(testContent);
            Assert.assertEquals(scriptCounter, 42);
        } finally {
            scripting.removeScript(siAfterTreeNodeFolderReplacement.getId());
            tree.clear(mode);
            try {
                co.remove(testContent);
            } catch (FxApplicationException e) {
                //ignore
            }
        }
    }
}
