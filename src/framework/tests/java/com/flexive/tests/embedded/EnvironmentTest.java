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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACL.Category;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.structure.*;
import com.flexive.shared.workflow.Step;
import com.flexive.shared.workflow.StepDefinition;
import com.flexive.shared.workflow.Workflow;
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
        testUnmodifiableList("environment.getDataTypes()", dataTypes, FxDataType.String1024);
        assert dataTypes.equals(getEnvironment().getDataTypes()) : "Environment list modified.";
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetACLs() {
        List<ACL> acls = getEnvironment().getACLs();
        testUnmodifiableList("environment.getACLs()", acls, new ACL(1, "test", null, -1, null, null, null, ACL.Category.INSTANCE, null));
        assert acls.equals(getEnvironment().getACLs());
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetACLsByCategory() {
        for (Category category : Category.values()) {
            List<ACL> acls = getEnvironment().getACLs(category);
            testUnmodifiableList("environment.getACLs()", acls, new ACL(1, "test", null, -1, null, null, null, ACL.Category.INSTANCE, null));
            assert acls.equals(getEnvironment().getACLs(category));
        }
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetWorkflows() {
        List<Workflow> workflows = getEnvironment().getWorkflows();
        testUnmodifiableList("environment.getWorkflows()", workflows, new Workflow(-1, "test", null, null, null));
        assert workflows.equals(getEnvironment().getWorkflows());
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetSteps() {
        List<Step> steps = getEnvironment().getSteps();
        testUnmodifiableList("environment.getSteps()", steps, new Step(-1, -1, -1, -1));
        assert steps.equals(getEnvironment().getSteps());
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetStepDefinitions() {
        List<StepDefinition> stepDefinitions = getEnvironment().getStepDefinitions();
        testUnmodifiableList("environment.getStepDefinitions()", stepDefinitions, new StepDefinition(-1, null, null, -1));
        assert stepDefinitions.equals(getEnvironment().getStepDefinitions());
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
        testUnmodifiableList("environment.getMandators()", mandators, new Mandator(-1, "test", -1, true, null));
        assert mandators.equals(getEnvironment().getMandators(true, true));
    }


    @Test(groups = {"ejb", "environment"})
    public void testGetGroups() {
        for (int i = 0; i < 16; i++) {
            List<FxGroup> groups = getEnvironment().getGroups((i & 1) > 0, (i & 2) > 0, (i & 4) > 0, (i & 8) > 0);
            testUnmodifiableList("environment.getGroups()", groups, new FxGroup(1, "test", null, null, true, null, null));
            assert groups.equals(getEnvironment().getGroups((i & 1) > 0, (i & 2) > 0, (i & 4) > 0, (i & 8) > 0));
        }
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetProperties() {
        List<FxProperty> properties = getEnvironment().getProperties(true, true);
        testUnmodifiableList("environment.getProperties()", properties, new FxProperty(-1, "test", null, null, null));
        assert properties.equals(getEnvironment().getProperties(true, true));
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetPropertyAssignments() {
        List<FxPropertyAssignment> assignments = getEnvironment().getPropertyAssignments();
        testUnmodifiableList("environment.getPropertyAssignments()", assignments, null);
        assert assignments.equals(getEnvironment().getPropertyAssignments());
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetPropertyAssignments2() {
        List<FxPropertyAssignment> assignments = getEnvironment().getPropertyAssignments(true);
        testUnmodifiableList("environment.getPropertyAssignments(true)", assignments, null);
        assert assignments.equals(getEnvironment().getPropertyAssignments(true));
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetGroupAssignments() {
        List<FxGroupAssignment> assignments = getEnvironment().getGroupAssignments();
        testUnmodifiableList("environment.getGroupAssignments()", assignments, new FxGroupAssignment(-1, false, null, "test", null, -1,
                new FxMultiplicity(0, 1), 0, null, -1,
                null, null, null, GroupMode.AnyOf, null));
        assert assignments.equals(getEnvironment().getGroupAssignments());
    }

    @Test(groups = {"ejb", "environment"})
    public void testGetTypes() {
        for (int i = 0; i < 16; i++) {
            List<FxType> types = getEnvironment().getTypes((i & 1) > 0, (i & 2) > 0, (i & 4) > 0, (i & 8) > 0);
            testUnmodifiableList("environment.getTypes(...)", types, null);
            assert types.equals(getEnvironment().getTypes((i & 1) > 0, (i & 2) > 0, (i & 4) > 0, (i & 8) > 0));
        }
    }


    private <T> void testUnmodifiableList(String source, List<T> list, T dummyInstance) {
        try {
            list.clear();
            assert false : "List can be cleared: " + source;
        } catch (UnsupportedOperationException e) {
            // continue
        }
        try {
            list.add(dummyInstance);
            assert false : "List can be extended: " + source;
        } catch (UnsupportedOperationException e) {
            // continue
        }
        try {
            if (list.size() > 0) {
                list.remove(0);
                assert false : "List elements can be removed: " + source;
            }
        } catch (UnsupportedOperationException e) {
            // continue
        }
    }
}

