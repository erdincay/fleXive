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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.media.FxMimeTypeWrapper;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.structure.*;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.Workflow;
import com.google.common.collect.ImmutableList;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Environment beans tests. Includes tests that verify the un-modifiability of
 * environment lists by the client.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class EnvironmentTest {

    protected FxEnvironment getEnvironment() {
        return CacheAdmin.getEnvironment();
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetDataTypes() {
        List<FxDataType> dataTypes = getEnvironment().getDataTypes();
        tryModifyList("environment.getDataTypes()", dataTypes);
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetACLs() {
        List<ACL> acls = getEnvironment().getACLs();
        tryModifyList("environment.getACLs()", acls);
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetACLsByCategory() {
        for (ACLCategory category : ACLCategory.values()) {
            List<ACL> acls = getEnvironment().getACLs(category);
            tryModifyList("environment.getACLs()", acls);
        }
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetWorkflows() {
        List<Workflow> workflows = getEnvironment().getWorkflows();
        tryModifyList("environment.getWorkflows()", workflows);
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetSteps() {
        List<Step> steps = getEnvironment().getSteps();
        tryModifyList("environment.getSteps()", steps);
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetStepDefinitions() {
        List<StepDefinition> stepDefinitions = getEnvironment().getStepDefinitions();
        tryModifyList("environment.getStepDefinitions()", stepDefinitions);
    }

    public void testGetStepsByDefinition() {
        // TODO
    }

    public void testGetStepsByWorkflow() {
        // TODO
    }


    @Test(groups = {"ejb", "environment"})
    public void testGetMandators() {
        List<Mandator> mandators = getEnvironment().getMandators(true, true);
        tryModifyList("environment.getMandators()", mandators);
    }


    @Test(groups = {"ejb", "environment"})
    public void testGetGroups() {
        for (int i = 0; i < 16; i++) {
            List<FxGroup> groups = getEnvironment().getGroups((i & 1) > 0, (i & 2) > 0, (i & 4) > 0, (i & 8) > 0);
            tryModifyList("environment.getGroups()", groups);
        }
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetProperties() {
        List<FxProperty> properties = getEnvironment().getProperties(true, true);
        tryModifyList("environment.getProperties()", properties);
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetPropertyAssignments() {
        List<FxPropertyAssignment> assignments = getEnvironment().getPropertyAssignments();
        tryModifyList("environment.getPropertyAssignments()", assignments);
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetPropertyAssignments2() {
        List<FxPropertyAssignment> assignments = getEnvironment().getPropertyAssignments(true);
        tryModifyList("environment.getPropertyAssignments(true)", assignments);
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetGroupAssignments() {
        List<FxGroupAssignment> assignments = getEnvironment().getGroupAssignments();
        tryModifyList("environment.getGroupAssignments()", assignments);
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetTypes() {
        for (int i = 0; i < 16; i++) {
            List<FxType> types = getEnvironment().getTypes((i & 1) > 0, (i & 2) > 0, (i & 4) > 0, (i & 8) > 0);
            tryModifyList("environment.getTypes(...)", types);
        }
    }

    /**
     * Tests #propertyExistsInType and #groupExistsInType
     */
    @Test(groups= {"ejb", "environment"})
    public void testPropAndGroupExistWithinTypes() {
        final String typeName = "CONTACTDATA";
        final String prop1 = "NAME";
        final String group1 = "ADDRESS";
        final String assignment = "DISPLAYNAME";
        final String nonExistingType = "IDONTEXIST01234";
        Assert.assertTrue(getEnvironment().propertyExistsInType(typeName, prop1));
        Assert.assertTrue(getEnvironment().propertyExistsInType(typeName, prop1.toLowerCase()));
        Assert.assertTrue(getEnvironment().groupExistsInType(typeName, group1));
        Assert.assertTrue(getEnvironment().groupExistsInType(typeName, group1.toLowerCase()));
        Assert.assertFalse(getEnvironment().groupExistsInType(typeName, assignment));
        Assert.assertFalse(getEnvironment().groupExistsInType(typeName, assignment.toLowerCase()));

        try {
            getEnvironment().propertyExistsInType(nonExistingType, prop1);
        } catch(Exception e){
            // expected
        }

        try {
            getEnvironment().groupExistsInType(nonExistingType, prop1);
        } catch(Exception e) {
            // expected
        }
    }

    private <T> void tryModifyList(String source, List<T> list) {
        final List<T> referenceList = ImmutableList.copyOf(list);
        try {
            //noinspection unchecked
            ((List) list).add(new Object());    // add garbage

            // continue - test will check whether the original list is still untouched
            Assert.assertFalse(list.equals(referenceList), "Environment list modified: " + source);
        } catch (UnsupportedOperationException e) {
            // modification not possible - also OK
            Assert.assertTrue(list.equals(referenceList));
        }
    }

    /**
     * Test the getMimeTypeMatch method
     *
     * @since 3.1
     */
    @Test(groups = {"ejb", "environment"})
    public void mimeTypeMatchTest() throws FxApplicationException {
        /*
        basic setup provided the following mime types
        unknown/unknown --> DOCUMENTFILE
        application/unknown --> DOCUMENT
        image/unknown --> IMAGE
         */
        long id1 = -1;
        long id2 = -1;
        long id3 = -1;
        long id4 = -1;

        Assert.assertEquals(getEnvironment().getMimeTypeMatch(null).getName(), FxType.DOCUMENTFILE);
        Assert.assertEquals(getEnvironment().getMimeTypeMatch("unknown/unknown").getName(), FxType.DOCUMENTFILE);
        Assert.assertEquals(getEnvironment().getMimeTypeMatch("foo/bar").getName(), FxType.DOCUMENTFILE);
        Assert.assertEquals(getEnvironment().getMimeTypeMatch("application/unknown").getName(), FxType.DOCUMENT);
        Assert.assertEquals(getEnvironment().getMimeTypeMatch("application/foobar").getName(), FxType.DOCUMENT);
        Assert.assertEquals(getEnvironment().getMimeTypeMatch("image/unknown").getName(), "IMAGE");
        Assert.assertEquals(getEnvironment().getMimeTypeMatch("image/foobar").getName(), "IMAGE");

        // let's create some derived types and check if the respective FxTypes are recognised
        TypeEngine te = EJBLookup.getTypeEngine();
        try {
            id1 = te.save(FxTypeEdit.createNew("PDF_TYPE", FxType.DOCUMENT).setMimeType(new FxMimeTypeWrapper("application/pdf")));
            id2 = te.save(FxTypeEdit.createNew("JPG_TYPE", FxType.IMAGE).setMimeType(new FxMimeTypeWrapper("image/jpeg")));
            id3 = te.save(FxTypeEdit.createNew("PNG_TYPE", FxType.IMAGE).setMimeType(new FxMimeTypeWrapper("image/png")));
            id4 = te.save(FxTypeEdit.createNew("JPG_TYPE_CHILD", "JPG_TYPE").setMimeType(new FxMimeTypeWrapper("image/foobar")));

            Assert.assertEquals(getEnvironment().getMimeTypeMatch("application/pdf").getName(), "PDF_TYPE");
            Assert.assertEquals(getEnvironment().getMimeTypeMatch("image/jpeg").getName(), "JPG_TYPE");
            Assert.assertEquals(getEnvironment().getMimeTypeMatch("image/png").getName(), "PNG_TYPE");
            Assert.assertEquals(getEnvironment().getMimeTypeMatch("image/foobar").getName(), "JPG_TYPE_CHILD");

            // child types override their parents' settings
            FxType childTest = getEnvironment().getType("JPG_TYPE_CHILD");
            FxMimeTypeWrapper wrapper = childTest.getMimeType();
            wrapper.addMimeTypes("image/jpeg");
            te.save(childTest.asEditable().setMimeType(wrapper));

            // the original JPG_TYPE still has image/jpg
            Assert.assertEquals(getEnvironment().getType("JPG_TYPE").getMimeType().toString(), "image/jpeg");
            // asking for the matching type will return the bottom of the hierarchy
            Assert.assertEquals(getEnvironment().getMimeTypeMatch("image/jpeg").getName(), "JPG_TYPE_CHILD");

        } catch (FxApplicationException ex) {
            // silent death throes
        } finally { // clean up test structures
            if (id1 != -1)
                te.remove(id1);
            if (id2 != -1)
                te.remove(id1);
            if (id3 != -1)
                te.remove(id1);
            if (id4 != -1)
                te.remove(id1);
        }
    }
}

