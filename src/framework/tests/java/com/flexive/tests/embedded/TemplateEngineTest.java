/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.tree.*;
import static com.flexive.shared.tree.FxTreeMode.Edit;
import static com.flexive.shared.tree.FxTreeMode.Live;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.TemplateEngine;
import static com.flexive.shared.interfaces.TemplateEngine.Type;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Template engine tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "templates"})
public class TemplateEngineTest {
    private static final String T_CONTENT = "TemplateEngineTest.HTML";
    private static final String T_MASTER = "TemplateEngineTest.MASTER";
    private static final String T_TAG = "TemplateEngineTest.TAG";

    private List<Long> templateIds = new ArrayList<Long>();

    @BeforeClass
    public void beforeClass() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException {
        FxTestUtils.login(TestUsers.REGULAR);
        final TemplateEngine te = EJBLookup.getTemplateEngine();
        templateIds.add(te.create(T_CONTENT, Type.CONTENT, "text/html", "html template"));
        templateIds.add(te.create(T_MASTER, Type.MASTER, "text/html", "master template"));
        templateIds.add(te.create(T_TAG, Type.TAG, "text/html", "tag template"));
    }

    @AfterClass
    public void afterClass() throws FxApplicationException, FxLogoutFailedException {
        for (Long id : templateIds) {
            EJBLookup.getTemplateEngine().remove(id);
        }
        FxTestUtils.logout();
    }

    @Test
    public void templateContentTest() throws FxApplicationException {
        final TemplateEngine te = EJBLookup.getTemplateEngine();
        final long id = te.create("test", Type.MASTER, "text/html", "initial content");
        try {
            assertEquals(te.getContent(id, Edit), "initial content");
            assertEquals(te.getContent(id, Live), null);
            assertEquals(te.getFinalContent(id, null, Edit), "initial content");
            assertEquals(te.getContent("test", Edit), "initial content");
            assertEquals(te.getContent("test", Live), null);
            assertEquals(te.getFinalContent("test", null, Edit), "initial content");

            te.setContent(id, "edit content", "text/plain", Edit);
            assertEquals(te.getContent(id, Edit), "edit content");
            assertEquals(te.getContent(id, Live), null);
            assertEquals(te.getContent("test", Edit), "edit content");
            assertEquals(te.getContent("test", Live), null);

            te.activate(id);
            te.setContent(id, "live content", "text/plain", Live);
            assertEquals(te.getContent(id, Edit), "edit content");
            assertEquals(te.getContent(id, Live), "live content");
            assertEquals(te.getContent("test", Edit), "edit content");
            assertEquals(te.getContent("test", Live), "live content");
        } finally {
            te.remove(id);
        }
    }

    @Test
    public void treeTemplateTest() throws FxApplicationException {
        final TemplateEngine te = EJBLookup.getTemplateEngine();
        final long id = te.create("test", Type.MASTER, "text/html", "initial content");
        final long treeNodeId = EJBLookup.getTreeEngine().save(FxTreeNodeEdit.createNew("test").setParentNodeId(FxTreeNode.ROOT_NODE));
        try {
            assertEquals(te.templateIsReferenced(id), false, "New template cannot be referenced");
            assertEquals(te.getTemplateMappings(treeNodeId, FxTreeMode.Edit).size(), 0);

            te.setTemplateMappings(treeNodeId, Arrays.asList(new FxTemplateMapping(null, id)));
            assertEquals(te.getTemplateMappings(treeNodeId, FxTreeMode.Edit).size(), 1);
            try {
                te.getTemplateMappings(treeNodeId, FxTreeMode.Live);
                assert false : "Tree node not available in live step, but getTemplateMappings did not throw an exception";
            } catch (FxApplicationException e) {
                // pass
            }
            final FxTemplateInfo editTemplate = te.getTemplate(treeNodeId, Edit);
            assertEquals(editTemplate.getName(), "test");

            EJBLookup.getTreeEngine().activate(FxTreeMode.Edit, treeNodeId, false);
            assertEquals(te.getTemplateMappings(treeNodeId, FxTreeMode.Edit).size(), 1);
            assertEquals(te.getTemplateMappings(treeNodeId, FxTreeMode.Live).size(), 1);
            try {
                te.getTemplate(treeNodeId, Live);
                assert false : "Template does not exist in live mode, but getTemplate(,Live) returned: "
                        + te.getTemplate(treeNodeId, Live);
            } catch (FxApplicationException e) {
                // pass
            }
            te.activate(id);
            final FxTemplateInfo liveTemplate = te.getTemplate(treeNodeId, Live);
            assertEquals(liveTemplate.getName(), "test");

            te.setTemplateMappings(treeNodeId, new ArrayList<FxTemplateMapping>(0));
            assertEquals(te.getTemplateMappings(treeNodeId, FxTreeMode.Edit).size(), 0);
        } finally {
            EJBLookup.getTreeEngine().remove(FxTreeNodeEdit.createNew("").setMode(FxTreeMode.Live).setId(treeNodeId), false, true);
            EJBLookup.getTreeEngine().remove(FxTreeNodeEdit.createNew("").setMode(FxTreeMode.Edit).setId(treeNodeId), false, true);
            te.remove(id);
        }
    }

    @Test
    public void templateRenameTest() throws FxApplicationException {
        final TemplateEngine te = EJBLookup.getTemplateEngine();
        final long id = te.create("test", Type.MASTER, "text/html", "content");
        try {
            assertEquals(te.getInfo(id, Edit).getName(), "test");
            te.setName(id, "new name");
            assertEquals(te.getInfo(id, Edit).getName(), "new name");
        } finally {
            te.remove(id);
        }
    }

    @Test
    public void templateReferenceTest() throws FxApplicationException {
        final TemplateEngine te = EJBLookup.getTemplateEngine();
        final long master = te.create("test", Type.MASTER, "text/html", "master template");
        final String content = "<ui:composition template=\"test\"> </ui:composition>";
        final long client = te.create("client", Type.CONTENT, "text/html", content);
        try {
            assert !te.templateIsReferenced(client) : "Client template should not be referenced";
            assert te.templateIsReferenced(master) : "Master template is referenced by client";
            assertEquals(te.getFinalContent(client, null, Edit), content);
            assertEquals(te.getFinalContent(client, "newtemplate", Edit), content.replace("test", "newtemplate"));
        } finally {
            te.remove(client);
            te.remove(master);
        }
    }

    @Test
    public void listTemplatesTest() throws FxApplicationException {
        // test three default templates created in beforeClass
        final List<FxTemplateInfo> contentTemplates = EJBLookup.getTemplateEngine().list(Type.CONTENT);
        assertEquals(contentTemplates.size(), 1, "Expected one content template");
        assertEquals(contentTemplates.get(0).getName(), T_CONTENT);

        final List<FxTemplateInfo> masterTemplates = EJBLookup.getTemplateEngine().list(Type.MASTER);
        assertEquals(masterTemplates.size(), 1, "Expected one master template");
        assertEquals(masterTemplates.get(0).getName(), T_MASTER);

        final List<FxTemplateInfo> tagTemplates = EJBLookup.getTemplateEngine().list(Type.TAG);
        assertEquals(tagTemplates.size(), 1, "Expected one content template");
        assertEquals(tagTemplates.get(0).getName(), T_TAG);
    }

}
