/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2014
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

package com.flexive.tests.embedded.persistence;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidStateException;
import com.flexive.shared.exceptions.FxRemoveException;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.structure.export.StructureExporter;
import com.flexive.shared.structure.export.StructureExporterCallback;
import com.flexive.shared.structure.export.StructureExporterTools;
import com.flexive.shared.value.FxString;
import com.flexive.tests.embedded.FxTestUtils;
import com.flexive.tests.embedded.TestUsers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Test all aspects of the ScriptExporter
 *
 * @author Christopher Blasnik (c.blasnik@flexive.com) UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "structure", "importexport"})
public class StructureExporterTest {

    private TypeEngine te;
    private AssignmentEngine ae;

    @BeforeClass
    public void beforeClass() throws Exception {
        te = EJBLookup.getTypeEngine();
        ae = EJBLookup.getAssignmentEngine();
        FxTestUtils.login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws Exception {
        FxTestUtils.logout();
    }

    /**
     * Tests the export of a "simple" structure without resolving dependencies to other types
     * types: Export1 & Export2 (with derived assignments from Export1)
     *
     * @throws FxApplicationException on errors
     */
    public void simpleStructureExportTestNoDeps() throws Exception {
        final long[] typeIdArr = createSimpleStructure();
        final FxType export1 = CacheAdmin.getEnvironment().getType(typeIdArr[0]);

        try {
            // no dependencies
            final StructureExporterCallback sec = StructureExporter.newInstance(export1.getId(), true);

            final long initialExportId = sec.getTypeId();
            final Map<FxType, List<FxAssignment>> typeAssignmentsMap = sec.getTypeAssignments();
            final Map<FxGroupAssignment, List<FxAssignment>> groupAssignmentsMap = sec.getGroupAssignments();

            Assert.assertEquals(initialExportId, typeIdArr[0]);
            Assert.assertFalse(sec.getHasDependencies());
            Assert.assertTrue(sec.getDependencies() == null, "The Map of dependencies should be NULL for ignoreDependencies = true");

            try {
                final List<StructureExporterCallback> seList = sec.getDependencyStructures();
            } catch (FxInvalidStateException e) {
                Assert.assertTrue(e != null, "Calling a list of exported structures should have failed");
            }

            final List<FxAssignment> e1 = typeAssignmentsMap.get(export1);
            // get the ids only
            final List<Long> exportedTypeAssignmentsForEXPORT1 = StructureExporterTools.convertAssignmentsToIds(e1);
            // get a list of ids from the actualy type w/o any ids, acl, etc. assignments
            final List<Long> export1PropsAssignments = new ArrayList<Long>();
            for (FxPropertyAssignment propAss : export1.getAssignedProperties()) {
                if (!StructureExporterTools.isSystemProperty(propAss.getXPath()))
                    export1PropsAssignments.add(propAss.getId());
            }

            // the length of the exported type assignments should be two, the other one 1
            Assert.assertEquals(export1PropsAssignments.size(), 1);
            Assert.assertEquals(exportedTypeAssignmentsForEXPORT1.size(), 2);
            Assert.assertTrue(exportedTypeAssignmentsForEXPORT1.contains(export1PropsAssignments.get(0)), "Couldn't find the prop assignment with id " + export1PropsAssignments.get(0) + " in the exported structure");

            for (FxGroupAssignment groupAss : export1.getAssignedGroups()) {
                Assert.assertTrue(exportedTypeAssignmentsForEXPORT1.contains(groupAss.getId()), "Couldn't find the group assignment with id " + export1PropsAssignments.get(0) + " in the exported structure");

                final List<FxAssignment> c = groupAss.getAllChildAssignments();
                final List<Long> childAssignments = StructureExporterTools.convertAssignmentsToIds(c);

                Assert.assertTrue(childAssignments.size() == 1); // just to make sure
                Assert.assertTrue(groupAssignmentsMap.containsKey(groupAss));
                Assert.assertTrue(groupAssignmentsMap.get(groupAss).size() == 1, "The Group with id " + groupAss.getId() + " should have one assignment");

                final List<FxAssignment> g = groupAssignmentsMap.get(groupAss);
                final List<Long> exportedGroupAssignmentsForEXPORT1 = StructureExporterTools.convertAssignmentsToIds(g);

                Assert.assertTrue(exportedGroupAssignmentsForEXPORT1.contains(childAssignments.get(0)));
            }


        } finally {
            deleteStructure(typeIdArr);
        }
    }

    /**
     * Tests the export of a "simple" structure also resolving the dependencies to other types
     * --> Test of Export1 w/ deps
     *
     * @throws FxApplicationException on errors
     */
    public void simpleStructureExportTestWithDepsPart1() throws FxApplicationException {
        final long[] typeIdArr = createSimpleStructure();
        final FxType export1 = CacheAdmin.getEnvironment().getType(typeIdArr[0]);
//        final FxType export2 = CacheAdmin.getEnvironment().getType(typeIdArr[1]);
        try {
            // with dependencies
            // expected outcome: same as "#simpleStructureExportTestNoDeps"
            final StructureExporterCallback sec = StructureExporter.newInstance(export1.getId(), false);

            final long initialExportId = sec.getTypeId();
            final Map<FxType, List<FxAssignment>> typeAssignmentsMap = sec.getTypeAssignments();
            final Map<FxGroupAssignment, List<FxAssignment>> groupAssignmentsMap = sec.getGroupAssignments();

            Assert.assertEquals(initialExportId, typeIdArr[0]);
            Assert.assertFalse(sec.getHasDependencies());
            Assert.assertTrue(sec.getDependencies() == null, "The Map of dependencies should be null when exporting EXPORT1 w/ deps");

            try {
                sec.getDependencyStructures();
            } catch (FxInvalidStateException e) {
                Assert.assertTrue(e != null, "Calling a list of exported structures should have failed");
            }

            final List<FxAssignment> e1 = typeAssignmentsMap.get(export1);
            // get the ids only
            final List<Long> exportedTypeAssignmentsForEXPORT1 = StructureExporterTools.convertAssignmentsToIds(e1);
            // get a list of ids from the actualy type w/o any ids, acl, etc. assignments
            final List<Long> export1PropsAssignments = new ArrayList<Long>();
            for (FxPropertyAssignment propAss : export1.getAssignedProperties()) {
                if (!StructureExporterTools.isSystemProperty(propAss.getXPath()))
                    export1PropsAssignments.add(propAss.getId());
            }

            // the length of the exported type assignments should be two, the other one 1
            Assert.assertEquals(export1PropsAssignments.size(), 1);
            Assert.assertEquals(exportedTypeAssignmentsForEXPORT1.size(), 2);
            Assert.assertTrue(exportedTypeAssignmentsForEXPORT1.contains(export1PropsAssignments.get(0)), "Couldn't find the prop assignment with id " + export1PropsAssignments.get(0) + " in the exported structure");

            for (FxGroupAssignment groupAss : export1.getAssignedGroups()) {
                Assert.assertTrue(exportedTypeAssignmentsForEXPORT1.contains(groupAss.getId()), "Couldn't find the group assignment with id " + export1PropsAssignments.get(0) + " in the exported structure");

                final List<FxAssignment> c = groupAss.getAllChildAssignments();
                final List<Long> childAssignments = StructureExporterTools.convertAssignmentsToIds(c);

                Assert.assertTrue(childAssignments.size() == 1); // just to make sure
                Assert.assertTrue(groupAssignmentsMap.containsKey(groupAss));
                Assert.assertTrue(groupAssignmentsMap.get(groupAss).size() == 1, "The Group with id " + groupAss.getId() + " should have one assignment");

                final List<FxAssignment> g = groupAssignmentsMap.get(groupAss);
                final List<Long> exportedGroupAssignmentsForEXPORT1 = StructureExporterTools.convertAssignmentsToIds(g);

                Assert.assertTrue(exportedGroupAssignmentsForEXPORT1.contains(childAssignments.get(0)));
            }

        } finally {
            deleteStructure(typeIdArr);
        }
    }

    /**
     * Tests the export of a "simple" structure also resolving the dependencies to other types
     * --> Test of Export2 w/ deps
     *
     * @throws FxApplicationException on errors
     */
    public void simpleStructureExportTestWithDepsPart2() throws FxApplicationException {
        final long[] typeIdArr = createSimpleStructure();
        final FxType EXPORT_1 = CacheAdmin.getEnvironment().getType(typeIdArr[0]);
        final FxType EXPORT_2 = CacheAdmin.getEnvironment().getType(typeIdArr[1]);
        final FxPropertyAssignment PROP1_Orig = CacheAdmin.getEnvironment().getPropertyAssignment("EXPORT1/PROP1");
        final FxPropertyAssignment PROP1_Der = CacheAdmin.getEnvironment().getPropertyAssignment("EXPORT2/PROP1");
        final FxGroupAssignment GROUP1_Orig = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("EXPORT1/GROUP1");
        final FxGroupAssignment GROUP1_Der = (FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment("EXPORT2/GROUP1");
        final FxPropertyAssignment PROP2_Orig = CacheAdmin.getEnvironment().getPropertyAssignment("EXPORT1/GROUP1/PROP2");
        final FxPropertyAssignment PROP2_Der = CacheAdmin.getEnvironment().getPropertyAssignment("EXPORT2/GROUP1/PROP2");


        try {
            // with dependencies
            // starting with EXPORT2!
            final StructureExporterCallback sec = StructureExporter.newInstance(EXPORT_2.getId(), false);

            final long initialExportId = sec.getTypeId();
            final Map<FxType, List<FxAssignment>> typeAssignments = sec.getTypeAssignments();
            final Map<FxGroupAssignment, List<FxAssignment>> groupAssignments = sec.getGroupAssignments();
            final Map<FxType, List<FxAssignment>> dependencies = sec.getDependencies();

            Assert.assertEquals(initialExportId, typeIdArr[1]);
            Assert.assertTrue(sec.getHasDependencies());
            Assert.assertTrue(dependencies.size() > 0, "The Map of dependencies should have a size() > 0");

            /**
             * Expected outcomes:
             * sec.getTypeAssignments():
             * size() == 2
             * EXPORT1 --> PROP1, GROUP1
             * EXPORT2 --> size() = 0
             *
             * sec.getGroupAssignments():
             * size() == 1
             * GROUP1 --> PROP2
             *
             * sec.getDependencyStructure():
             * size() == 1
             * StructureExporterCallback secDep1 = sec.getDependencyStructure().get(0):
             * secDep1.getTypeAssignments():
             * EXPORT2 --> PROP1 (derived), GROUP1 (derived)
             *
             * secDep1.getGroupAssignments():
             * GROUP1 --> PROP2 (derived)
             *
             */

            // getTypeAssignments
            Assert.assertEquals(typeAssignments.size(), 2);
            Assert.assertTrue(typeAssignments.containsKey(EXPORT_1));
            Assert.assertTrue(typeAssignments.containsKey(EXPORT_2));

            List<FxAssignment> exportTypeAssignments = typeAssignments.get(EXPORT_1);
            final List<FxAssignment> allOrigEXPORT_1Assignments = StructureExporterTools.getCombinedAssignments(EXPORT_1.getAssignedProperties(), EXPORT_1.getAssignedGroups());
            for (FxGroupAssignment ga : EXPORT_1.getAssignedGroups()) {
                if (ga.getAllChildAssignments().size() > 0) {
                    allOrigEXPORT_1Assignments.addAll(ga.getAllChildAssignments());
                }
            }

            Assert.assertEquals(exportTypeAssignments.size(), 2);
            Assert.assertTrue(isAssignmentInList(allOrigEXPORT_1Assignments, PROP1_Orig), "Could not find the property assignment in the exported type");
            Assert.assertTrue(isAssignmentInList(allOrigEXPORT_1Assignments, GROUP1_Orig), "Could not find the group assignment in the exported type");
            Assert.assertTrue(isAssignmentInList(allOrigEXPORT_1Assignments, PROP2_Orig));
            Assert.assertFalse(isAssignmentInList(allOrigEXPORT_1Assignments, PROP2_Der));
            Assert.assertFalse(isAssignmentInList(allOrigEXPORT_1Assignments, PROP1_Der));
            Assert.assertFalse(isAssignmentInList(allOrigEXPORT_1Assignments, GROUP1_Der));

            List<FxAssignment> EXPORT_2TypeAssignments = typeAssignments.get(EXPORT_2);
            // EXPORT 2 data will be available from getDependencyStructures()
            Assert.assertEquals(EXPORT_2TypeAssignments.size(), 0);

            // getGroupAssignments
            Assert.assertEquals(groupAssignments.size(), 1);
            Assert.assertTrue(groupAssignments.containsKey(GROUP1_Orig));
            Assert.assertEquals(groupAssignments.size(), 1, "There should be exactly one property assigned to the exported group");

            // getDependencyStructures
            final List<StructureExporterCallback> seList = sec.getDependencyStructures();

            Assert.assertEquals(seList.size(), 1);

            final StructureExporterCallback secDEP1 = seList.get(0);
            final Map<FxType, List<FxAssignment>> typeAssignmentsDEP1 = secDEP1.getTypeAssignments();
            final Map<FxGroupAssignment, List<FxAssignment>> groupAssignmentsDEP1 = secDEP1.getGroupAssignments();

            Assert.assertFalse(secDEP1.getHasDependencies());

            // getDependencyStructures --> getTypeAssignments()
            Assert.assertEquals(typeAssignmentsDEP1.size(), 1);
            Assert.assertTrue(typeAssignmentsDEP1.containsKey(EXPORT_2));

            final List<FxAssignment> EXPORT_2_DEP1_TypeAssignments = typeAssignmentsDEP1.get(EXPORT_2);
            final List<FxAssignment> origExport2Assignments = StructureExporterTools.getCombinedAssignments(EXPORT_2.getAssignedProperties(), EXPORT_2.getAssignedGroups());

            Assert.assertEquals(EXPORT_2_DEP1_TypeAssignments.size(), 2);
            Assert.assertTrue(isAssignmentInList(origExport2Assignments, PROP1_Der), "Could not find the property assignment in the exported type");
            Assert.assertTrue(isAssignmentInList(origExport2Assignments, GROUP1_Der), "Could not find the group assignment in the exported type");
            Assert.assertFalse(isAssignmentInList(origExport2Assignments, PROP1_Orig));
            Assert.assertFalse(isAssignmentInList(origExport2Assignments, GROUP1_Orig));

            // getDependencyStructures --> getGroupAssignments()
            Assert.assertEquals(groupAssignmentsDEP1.size(), 1);
            Assert.assertTrue(groupAssignmentsDEP1.containsKey(GROUP1_Der));

            Assert.assertEquals(groupAssignmentsDEP1.get(GROUP1_Der).size(), 1, "There should be exactly one property assigned to the exported group");
            Assert.assertTrue(groupAssignmentsDEP1.get(GROUP1_Der).contains(PROP2_Der));

        } finally {
            deleteStructure(typeIdArr);
        }
    }

    /**
     * Tests the export of an advanced structure also resolving the dependencies to other types
     *
     * @throws FxApplicationException on errors
     */
    public void advancedStructureExportTestWithDeps() throws FxApplicationException {
        final long[] typeIds = createAdvancedStructure();
        final FxType EXPORT_1 = CacheAdmin.getEnvironment().getType(typeIds[0]);
        final FxType EXPORT_2 = CacheAdmin.getEnvironment().getType(typeIds[1]);
        final FxType EXPORT_3 = CacheAdmin.getEnvironment().getType(typeIds[2]);

        try {
            /**
             * No matter which Type is used as the starting point, the outcome should always be an export containing all Assignments
             * of the original types
             */
            StructureExporterCallback sec = StructureExporter.newInstance(EXPORT_1.getId(), false);
            Map<FxType, List<FxAssignment>> typeAssignments = sec.getTypeAssignments();
            Map<FxGroupAssignment, List<FxAssignment>> groupAssignments = sec.getGroupAssignments();

            Assert.assertTrue(sec.getHasDependencies());
            Assert.assertEquals(sec.getDependencyStructures().size(), 3);

            // assert map / list sizes
            Assert.assertEquals(typeAssignments.size(), 3);
            Assert.assertEquals(groupAssignments.size(), 2);

            advancedStructureAssertionSubTask(EXPORT_1, EXPORT_2, EXPORT_3, typeAssignments, groupAssignments,
                    sec);

            // now let's try EXPORT_3 as the starting point:
            sec = StructureExporter.newInstance(EXPORT_3.getId(), false);
            typeAssignments = sec.getTypeAssignments();
            groupAssignments = sec.getGroupAssignments();

            Assert.assertTrue(sec.getHasDependencies());
            Assert.assertEquals(sec.getDependencyStructures().size(), 3);

            // assert map / list sizes
            Assert.assertEquals(typeAssignments.size(), 3);
            Assert.assertEquals(groupAssignments.size(), 2);

            advancedStructureAssertionSubTask(EXPORT_1, EXPORT_2, EXPORT_3, typeAssignments, groupAssignments,
                    sec);

            // and now EXPORT_2:
            sec = StructureExporter.newInstance(EXPORT_2.getId(), false);
            typeAssignments = sec.getTypeAssignments();
            groupAssignments = sec.getGroupAssignments();

            Assert.assertTrue(sec.getHasDependencies());
            Assert.assertEquals(sec.getDependencyStructures().size(), 3);

            // assert map / list sizes
            Assert.assertEquals(typeAssignments.size(), 3);
            Assert.assertEquals(groupAssignments.size(), 2);

            advancedStructureAssertionSubTask(EXPORT_1, EXPORT_2, EXPORT_3, typeAssignments, groupAssignments,
                    sec);
        } finally {
            deleteStructure(typeIds);
        }
    }

    /**
     * A subtask for the advanced structure tests
     *
     * @param EXPORT_1         FxType EXPORT1
     * @param EXPORT_2         FxType EXPORT2
     * @param EXPORT_3         FxType EXPORT3
     * @param typeAssignments  StructureExporterCallback type assignments
     * @param groupAssignments StructureExporterCallback group assignments
     * @param sec              instance of StructureExporterCallback
     * @throws FxApplicationException on errors
     */
    private void advancedStructureAssertionSubTask(FxType EXPORT_1, FxType EXPORT_2, FxType EXPORT_3, Map<FxType, List<FxAssignment>> typeAssignments,
                                                   Map<FxGroupAssignment, List<FxAssignment>> groupAssignments, StructureExporterCallback sec)
            throws FxApplicationException {
        // create Lists of all assignments ids for each ORIGINAL FxType
        final List<Long> EXPORT_1_ids = getAllAssignmentsOfType(EXPORT_1);
        final List<Long> EXPORT_2_ids = getAllAssignmentsOfType(EXPORT_2);
        final List<Long> EXPORT_3_ids = getAllAssignmentsOfType(EXPORT_3);
        Collections.sort(EXPORT_1_ids);
        Collections.sort(EXPORT_2_ids);
        Collections.sort(EXPORT_3_ids);

        // internal: just check if we've got all ids for our types
        Assert.assertEquals(EXPORT_1_ids.size(), 7);
        Assert.assertEquals(EXPORT_2_ids.size(), 3);
        Assert.assertEquals(EXPORT_3_ids.size(), 3);
        // EXPORT 1:
        final List<Long> EXPORT_1_exportedIds = getAllExportedIdsForAType(EXPORT_1, typeAssignments, groupAssignments);
        final StructureExporterCallback EXPORT_1_dependendencyStructure = sec.getDependencyStructure(EXPORT_1.getId());
        EXPORT_1_exportedIds.addAll(getAllExportedIdsForAType(EXPORT_1, EXPORT_1_dependendencyStructure.getTypeAssignments(),
                EXPORT_1_dependendencyStructure.getGroupAssignments()));
        Collections.sort(EXPORT_1_exportedIds);
        // remove duplicates from EXPORT_1_exportedIds
        Set<Long> uniqueIds = new HashSet<Long>();
        uniqueIds.addAll(EXPORT_1_exportedIds);

        Assert.assertTrue(EXPORT_1_ids.containsAll(uniqueIds), "The exported type should contain the same FxAssignment ids as the original one");

        // EXPORT 2:
        final List<Long> EXPORT_2_exportedIds = getAllExportedIdsForAType(EXPORT_2, typeAssignments, groupAssignments);
        final StructureExporterCallback EXPORT_2_dependendencyStructure = sec.getDependencyStructure(EXPORT_2.getId());
        EXPORT_2_exportedIds.addAll(getAllExportedIdsForAType(EXPORT_2, EXPORT_2_dependendencyStructure.getTypeAssignments(),
                EXPORT_2_dependendencyStructure.getGroupAssignments()));
        Collections.sort(EXPORT_2_exportedIds);
        // remove duplicates from EXPORT_1_exportedIds
        uniqueIds = new HashSet<Long>();
        uniqueIds.addAll(EXPORT_2_exportedIds);

        Assert.assertTrue(EXPORT_2_ids.containsAll(uniqueIds), "The exported type should contain the same FxAssignment ids as the original one");

        // EXPORT 3:
        final List<Long> EXPORT_3_exportedIds = getAllExportedIdsForAType(EXPORT_3, typeAssignments, groupAssignments);
        final StructureExporterCallback EXPORT_3_dependendencyStructure = sec.getDependencyStructure(EXPORT_3.getId());
        EXPORT_3_exportedIds.addAll(getAllExportedIdsForAType(EXPORT_3, EXPORT_3_dependendencyStructure.getTypeAssignments(),
                EXPORT_3_dependendencyStructure.getGroupAssignments()));
        Collections.sort(EXPORT_3_exportedIds);
        // remove duplicates from EXPORT_3_exportedIds
        uniqueIds = new HashSet<Long>();
        uniqueIds.addAll(EXPORT_3_exportedIds);

        Assert.assertTrue(EXPORT_3_ids.containsAll(uniqueIds), "The exported type should contain the same FxAssignment ids as the original one");
    }

    /**
     * Test if differing derived assignments are recognised by the structure exporter (for derived types)
     *
     * @throws FxApplicationException on errors
     */
    public void derivedAssignmentDiffTest() throws FxApplicationException {
        FxTypeEdit parent = null;
        FxTypeEdit derived = null;
        long parentId = -1;
        long derivedId = -1;
        try {
            parent = FxTypeEdit.createNew("PARENTTYPE01");
            parent.save();
            parent = CacheAdmin.getEnvironment().getType("PARENTTYPE01").asEditable();
            parent.addProperty("prop1", FxDataType.String1024);
            parentId = parent.getId();

            derived = FxTypeEdit.createNew("DERIVEDTYPE01", "PARENTTYPE01");
            derived.save();
            derived = CacheAdmin.getEnvironment().getType("DERIVEDTYPE01").asEditable();
            derivedId = derived.getId();

            // change the assignment's multiplicity
            FxPropertyAssignmentEdit ed = ((FxPropertyAssignment)CacheAdmin.getEnvironment().getAssignment("DERIVEDTYPE01/PROP1")).asEditable();
            ed.setMultiplicity(FxMultiplicity.of(0, 3));
            ae.save(ed, false);

            final StructureExporterCallback callback = StructureExporter.newInstance(derived.getId(), false);

            Assert.assertTrue(callback.getHasDependencies());
            final List<StructureExporterCallback> dependencyStructures = callback.getDependencyStructures();
            Assert.assertEquals(dependencyStructures.size(), 1);
            final List<Long> differingDerivedAssignments = callback.getDifferingDerivedAssignments();
            Assert.assertEquals(differingDerivedAssignments.size(), 1);

            final StructureExporterCallback cb = dependencyStructures.get(0);
            FxType derivedType = CacheAdmin.getEnvironment().getType("DERIVEDTYPE01");
            final List<FxAssignment> l = cb.getTypeAssignments().get(derivedType);
            
            Assert.assertTrue(l.size() == 1);
            long fromTypeAssignment = l.get(0).getId();
            long fromDiffAssignment = differingDerivedAssignments.get(0);
            Assert.assertEquals(fromTypeAssignment, fromDiffAssignment);

        } finally {
            if (derivedId != -1)
                te.remove(derivedId);
            if(parentId != -1)
                te.remove(parentId);
        }
    }

    /**
     * Tests the export of multiple types
     *
     * @throws FxApplicationException on errors
     */
    public void multipleTypesExportTest() throws FxApplicationException {
        // TODO:
    }

    /**
     * Helper method to return all Assignment is for an exported type from the given export maps / assignments
     *
     * @param exportedType     the exported FxType
     * @param typeAssignments  the Map of typeAssignments
     * @param groupAssignments the Map of groupAssignments
     * @return returns a List of long ids containing ALL exported assignment ids
     */
    private List<Long> getAllExportedIdsForAType(FxType exportedType, Map<FxType, List<FxAssignment>> typeAssignments,
                                                 Map<FxGroupAssignment, List<FxAssignment>> groupAssignments) {

        final List<Long> exportedIds = StructureExporterTools.convertAssignmentsToIds(typeAssignments.get(exportedType));
        // groups
        final List<FxAssignment> EXPORT_1_groups = new ArrayList<FxAssignment>();
        for (FxAssignment a : typeAssignments.get(exportedType)) {
            if (a instanceof FxGroupAssignment) {
                EXPORT_1_groups.add(a);
            }
        }

        if (groupAssignments != null) {
            for (FxGroupAssignment ga : groupAssignments.keySet()) {
                for (FxAssignment a : EXPORT_1_groups) {
                    if (ga.getId() == a.getId()) {
                        exportedIds.add(ga.getId());
                        if (groupAssignments.get(ga) != null)
                            exportedIds.addAll(StructureExporterTools.convertAssignmentsToIds(groupAssignments.get(ga)));
                    }
                }
            }
        }
        return exportedIds;
    }

    /**
     * Create a "simple" structure of two types
     * Layout:
     * EXPORT1
     * |
     * --- PROP1
     * |
     * --- GROUP1
     * |
     * --- GROUP1/PROP2
     * <p/>
     * EXPORT2
     * |
     * --- PROP1[der. f. EXPORT1]
     * |
     * --- GROUP1[der. f. EXPORT1]
     * |
     * --- GROUP1/PROP2[der. f. EXPORT1]
     *
     * @return long[] returns an array of type ids
     * @throws FxApplicationException on errors
     */
    private long[] createSimpleStructure() throws FxApplicationException {
        // EXPORT1
        final long typeId1 = te.save(FxTypeEdit.createNew("Export1"));
        final FxPropertyEdit prop1 = FxPropertyEdit.createNew("Prop1", new FxString(true, "Prop1_Description"), new FxString(true, "Prop1_Hint"),
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.String1024);
        final FxPropertyEdit prop2 = FxPropertyEdit.createNew("Prop2", new FxString(true, "Prop2_Description"), new FxString(true, "Prop2_Hint"),
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.String1024);
        final FxGroupEdit gEd = FxGroupEdit.createNew("Group1", new FxString(true, "Group1_Description"), new FxString(true, "Group1_Hint"), true, FxMultiplicity.MULT_0_1);

        final long prop1AssId = ae.createProperty(typeId1, prop1, "/");
        final long groupAssId = ae.createGroup(typeId1, gEd, "/");
        final long prop2AssId = ae.createProperty(typeId1, prop2, CacheAdmin.getEnvironment().getAssignment(groupAssId).getXPath());

        // EXPORT2
        final long typeId2 = te.save(FxTypeEdit.createNew("Export2"));
        FxType t2 = CacheAdmin.getEnvironment().getType(typeId2);
        final FxPropertyAssignmentEdit pa1 = FxPropertyAssignmentEdit.reuse(
                CacheAdmin.getEnvironment().getAssignment(prop1AssId).getXPath(), "EXPORT2", "/");
        ae.save(pa1, false);
        final FxGroupAssignmentEdit ga1 = FxGroupAssignmentEdit.createNew((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(groupAssId), t2, "Group1", "/");
        final long groupId = ae.save(ga1, false);

        FxAssignment groupAssignment = CacheAdmin.getEnvironment().getAssignment(groupId);
        final FxPropertyAssignmentEdit pa2 = FxPropertyAssignmentEdit.createNew((FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(prop2AssId), t2, "Prop2", "GROUP1", groupAssignment);
        ae.save(pa2, false);

        return new long[]{typeId1, typeId2};
    }

    /**
     * Create a derived type
     *
     * @param name   the name for the derived type
     * @param parent the parent FxType
     * @return long the created type's id
     * @throws FxApplicationException on errors
     *                                // TODO: finish / check!
     */
    private long createSimpleDerived(String name, FxType parent) throws FxApplicationException {
        final long typeId = te.save(FxTypeEdit.createNew(name, parent));
        // add a property
        final FxPropertyEdit prop1 = FxPropertyEdit.createNew("AdditionalProp1", new FxString(true, "Add_Prop1_Description"), new FxString(true, "Add_Prop1_Hint"),
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.String1024);

        return typeId;
    }

    /**
     * Create an advanced structure of three types and mutual dependencies
     * Layout:
     * EXPORT1
     * |
     * -- PROP1
     * |
     * -- PROP2 (der. f. EXPORT2)
     * |
     * -- PROP3 (der. f. EXPORT3)
     * |
     * -- GROUP1
     * |
     * -- GROUP1/PROP1 (der. f. EXPORT1/PROP1)
     * |
     * -- GROUP2 (der. f. EXPORT2/GROUP2)
     * |
     * -- GROUP2/PROP2 (der. f. EXPORT2/GROUP2/PROP2)
     * <p/>
     * EXPORT2
     * |
     * -- GROUP2
     * |
     * -- GROUP2/PROP2
     * |
     * -- PROP3 (der. f. EXPORT3/PROP3)
     * <p/>
     * EXPORT3
     * |
     * -- PROP3
     * |
     * -- PROP1 (der. f. EXPORT1/PROP1)
     * |
     * -- PROP2 (der. f. EXPORT2/GROUP2/PROP2)
     *
     * @return long[] returns an array of type ids
     * @throws FxApplicationException on errors
     */
    private long[] createAdvancedStructure() throws FxApplicationException {
        // TYPES
        final long[] typeIds = new long[3];
        typeIds[0] = te.save(FxTypeEdit.createNew("Export1"));
        typeIds[1] = te.save(FxTypeEdit.createNew("Export2"));
        typeIds[2] = te.save(FxTypeEdit.createNew("Export3"));
        final FxType type1 = CacheAdmin.getEnvironment().getType(typeIds[0]);

        // PROPS
        final FxPropertyEdit prop1 = FxPropertyEdit.createNew("Prop1", new FxString(true, "Prop1_Label"), new FxString(true, "Prop1_Hint"),
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.String1024);
        final FxPropertyEdit prop2 = FxPropertyEdit.createNew("Prop2", new FxString(true, "Prop2_Label"), new FxString(true, "Prop2_Hint"),
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.String1024);
        final FxPropertyEdit prop3 = FxPropertyEdit.createNew("Prop3", new FxString(true, "Prop3_Label"), new FxString(true, "Prop3_Hint"),
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.String1024);

        // GROUPS
        final FxGroupEdit group1 = FxGroupEdit.createNew("Group1", new FxString(true, "Group1_Label"), new FxString(true, "Group1_Hint"), true, FxMultiplicity.MULT_0_1);
        final FxGroupEdit group2 = FxGroupEdit.createNew("Group2", new FxString(true, "Group2_Label"), new FxString(true, "Group2_Hint"), true, FxMultiplicity.MULT_0_1);

        // ASSIGNMENTS
        final long prop1AssId = ae.createProperty(typeIds[0], prop1, "/"); // EXPORT1/PROP1
        /*final long group1AssId = */
        ae.createGroup(typeIds[0], group1, "/"); // EXPORT1/GROUP1
        final long prop3AssId = ae.createProperty(typeIds[2], prop3, "/"); // EXPORT3/PROP3
        final long group2AssId = ae.createGroup(typeIds[1], group2, "/"); // EXPORT2/GROUP2
        final long prop2AssId = ae.createProperty(typeIds[1], prop2,
                CacheAdmin.getEnvironment().getAssignment(group2AssId).getXPath()); //EXPORT2/GROUP2/PROP2

        ae.save(FxPropertyAssignmentEdit.reuse(CacheAdmin.getEnvironment().getAssignment(prop3AssId).getXPath(),
                "EXPORT1", "/"), false);// EXPORT1/PROP3

        ae.save(FxPropertyAssignmentEdit.reuse(CacheAdmin.getEnvironment().getAssignment(prop2AssId).getXPath(),
                "EXPORT1", "/"), false);// EXPORT1/PROP2

        final FxAssignment exp1gr1Ass = CacheAdmin.getEnvironment().getAssignment("EXPORT1/GROUP1");
        ae.save(FxPropertyAssignmentEdit.createNew((FxPropertyAssignment) (CacheAdmin.getEnvironment().getAssignment(prop1AssId)),
                type1, "Prop1", "GROUP1", exp1gr1Ass), false); // EXPORT1/GROUP1/PROP1

        ae.save(FxGroupAssignmentEdit.createNew((FxGroupAssignment) CacheAdmin.getEnvironment().getAssignment(group2AssId),
                type1, "Group2", "/"), false); // EXPORT1/GROUP2
        final FxAssignment exp1gr2Ass = CacheAdmin.getEnvironment().getAssignment("EXPORT1/GROUP2");
        ae.save(FxPropertyAssignmentEdit.createNew((FxPropertyAssignment) (CacheAdmin.getEnvironment().getAssignment(prop2AssId)),
                type1, "Prop2", "GROUP2", exp1gr2Ass), false); // EXPORT1/GROUP2/PROP2

        ae.save(FxPropertyAssignmentEdit.reuse(CacheAdmin.getEnvironment().getAssignment(prop1AssId).getXPath(),
                "EXPORT3", "/"), false);// EXPORT3/PROP1

        ae.save(FxPropertyAssignmentEdit.reuse(CacheAdmin.getEnvironment().getAssignment(prop2AssId).getXPath(),
                "EXPORT3", "/"), false);// EXPORT3/PROP2

        ae.save(FxPropertyAssignmentEdit.reuse(CacheAdmin.getEnvironment().getAssignment(prop3AssId).getXPath(),
                "EXPORT2", "/"), false);// EXPORT2/PROP3

        return typeIds;
    }

    /**
     * Create a type containing properties with all possible FxDataTypes
     *
     * @return long returns the type's id
     * @throws FxApplicationException on errors
     */
    private long createTypeWithAllDataTypes() throws FxApplicationException {
        final long typeId = te.save(FxTypeEdit.createNew("Export1"));

        final FxPropertyEdit[] properties = new FxPropertyEdit[16];
        properties[0] = FxPropertyEdit.createNew("Prop1", new FxString(true, "Prop1_Description"),
                new FxString(true, "Prop1_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.String1024);
        properties[1] = FxPropertyEdit.createNew("Prop2", new FxString(true, "Prop2_Description"),
                new FxString(true, "Prop2_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.Boolean);
        properties[2] = FxPropertyEdit.createNew("Prop3", new FxString(true, "Prop3_Description"),
                new FxString(true, "Prop3_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.Double);
        properties[3] = FxPropertyEdit.createNew("Prop4", new FxString(true, "Prop4_Description"),
                new FxString(true, "Prop4_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.Float);
        properties[4] = FxPropertyEdit.createNew("Prop5", new FxString(true, "Prop5_Description"),
                new FxString(true, "Prop5_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.Binary);
        properties[5] = FxPropertyEdit.createNew("Prop6", new FxString(true, "Prop6_Description"),
                new FxString(true, "Prop6_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.Date);
        properties[6] = FxPropertyEdit.createNew("Prop7", new FxString(true, "Prop7_Description"),
                new FxString(true, "Prop7_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.DateRange);
        properties[7] = FxPropertyEdit.createNew("Prop8", new FxString(true, "Prop8_Description"),
                new FxString(true, "Prop8_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.DateTime);
        properties[8] = FxPropertyEdit.createNew("Prop9", new FxString(true, "Prop9_Description"),
                new FxString(true, "Prop9_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.DateTimeRange);
        properties[9] = FxPropertyEdit.createNew("Prop10", new FxString(true, "Prop10_Description"),
                new FxString(true, "Prop10_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.HTML);
        properties[10] = FxPropertyEdit.createNew("Prop11", new FxString(true, "Prop11_Description"),
                new FxString(true, "Prop11_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.LargeNumber);
        properties[11] = FxPropertyEdit.createNew("Prop12", new FxString(true, "Prop12_Description"),
                new FxString(true, "Prop12_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.Number);
        properties[12] = FxPropertyEdit.createNew("Prop13", new FxString(true, "Prop13_Description"),
                new FxString(true, "Prop13_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.Reference);
        properties[12].setReferencedType(CacheAdmin.getEnvironment().getType("ROOT"));

        properties[13] = FxPropertyEdit.createNew("Prop14", new FxString(true, "Prop14_Description"),
                new FxString(true, "Prop14_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.Text);
        properties[14] = FxPropertyEdit.createNew("Prop15", new FxString(true, "Prop15_Description"),
                new FxString(true, "Prop15_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.SelectMany);
        properties[14].setReferencedList(CacheAdmin.getEnvironment().getSelectList("COUNTRIES"));

        properties[15] = FxPropertyEdit.createNew("Prop16", new FxString(true, "Prop16_Description"),
                new FxString(true, "Prop16_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.SelectOne);
        properties[15].setReferencedList(CacheAdmin.getEnvironment().getSelectList("COUNTRIES"));

        /* Will be activated as soon as the InlineReference is a properly implemented FxValue
        properties[16] = FxPropertyEdit.createNew("Prop17", new FxString(true, "Prop17_Description"),
                new FxString(true, "Prop9_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.InlineReference);
                */

        for (FxPropertyEdit pEd : properties) {
            ae.createProperty(typeId, pEd, "/");
        }

        return typeId;
    }


    /**
     * Creates a type with 1 property whose assignment is different from the base property
     *
     * @return long the type's id
     * @throws FxApplicationException on errors
     */
    private long createDifferingTypeAssignments() throws FxApplicationException {
        final long typeId = te.save(FxTypeEdit.createNew("Export1"));
        // the PROPERTY
        final FxPropertyEdit prop = FxPropertyEdit.createNew("Prop1", new FxString(true, "Prop1_Description"),
                new FxString(true, "Prop1_Hint"), FxMultiplicity.MULT_0_1,
                CacheAdmin.getEnvironment().getDefaultACL(ACLCategory.STRUCTURE), FxDataType.String1024);
        prop.setMultiLang(true);
        prop.setInOverview(true);
        prop.setMaxLength(30);
        prop.setMultiLine(false);
        prop.setSearchable(true);
        prop.setOverrideHTMLEditor(true);
        prop.setOverrideACL(true);
        prop.setOverrideMaxLength(true);
        prop.setOverrideMultiLang(true);
        prop.setOverrideMultiLine(true);
        prop.setOverrideMultiplicity(true);
        prop.setOverrideOverview(true);
        prop.setOverrideSearchable(true);

        final long propAssId = ae.createProperty(typeId, prop, "/");

        // change the assignment
        final FxPropertyAssignment propAss = (FxPropertyAssignment) (CacheAdmin.getEnvironment().getAssignment(propAssId));
        FxPropertyAssignmentEdit pEdit = propAss.asEditable();

        pEdit.setLabel(new FxString(true, "Prop1_new_label"));
        pEdit.setMultiLang(false);
        pEdit.setInOverview(false);
        pEdit.setMaxLength(40);
        pEdit.setMultiLine(true);
        pEdit.setSearchable(false);

        ae.save(pEdit, false);

        return typeId;
    }

    /**
     * Delete type(s)
     *
     * @param id the FxTypes' ids to delete
     * @throws FxApplicationException on errors
     */
    private void deleteStructure(long... id) throws FxApplicationException {
        for (long i : id) {
            try {
                final FxType t = CacheAdmin.getEnvironment().getType(i);
                if (i != -1 && CacheAdmin.getEnvironment().typeExists(t.getName()))
                    te.remove(i);
            } catch (FxApplicationException e) {
                if (e instanceof FxRemoveException) {
                    final FxType t = CacheAdmin.getEnvironment().getType(i);
                    for (FxPropertyAssignment pa : t.getAssignedProperties())
                        ae.removeAssignment(pa.getId());
                    for (FxGroupAssignment ga : t.getAssignedGroups())
                        ae.removeAssignment(ga.getId());
                    te.remove(i);
                }
            }
        }
    }

    /**
     * @param l List&lt;FxAssignment&gt;
     * @param a FxAssignment
     * @return returns true if the given FxAssignment is contained in the List l
     */
    private boolean isAssignmentInList(List<FxAssignment> l, FxAssignment a) {
        boolean isSame = false;
        for (FxAssignment ass : l) {
            if (!StructureExporterTools.isSystemProperty(ass.getXPath()))
                if (ass.getId() == a.getId())
                    isSame = true;
        }
        return isSame;
    }

    /**
     * Retrieves all assignments for a given type!
     * Caution: no recursive search, only goes down 1 level per group, subgroups are not checked!
     * Automatically removes all system - generated properties
     *
     * @param t the FxType
     * @return returns a list of Long ids for the given type t
     */
    private List<Long> getAllAssignmentsOfType(FxType t) {
        final List<Long> out = new ArrayList<Long>();
        final List<FxAssignment> ass = StructureExporterTools.getCombinedAssignments(t.getAssignedProperties(), t.getAssignedGroups());
        for (FxAssignment a : ass) {
            if (!StructureExporterTools.isSystemProperty(a.getXPath()))
                out.add(a.getId());
        }
//        out.addAll(StructureExporterTools.convertAssignmentsToIds(ass));

        if (t.getAssignedGroups().size() > 0) {
            for (FxGroupAssignment ga : t.getAssignedGroups()) {
                if (ga.getAllProperties().size() > 0) {
                    out.addAll(StructureExporterTools.convertAssignmentsToIds(ga.getAllProperties()));
                }
            }
        }
        return out;
    }

    /**
     * GroovyScriptExporterTests follow here
     */
    // TODO: GroovyScriptExporterTests
}
