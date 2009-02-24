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
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.tests.embedded.FxTestUtils;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Test for relations
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "structure", "relation"})
public class RelationTest extends StructureTestBase {
    private final static String REL_NAME = "RELTEST_REL_" + RandomStringUtils.random(16, true, true);
    private final static String SRC_NAME = "RELTEST_SRC_" + RandomStringUtils.random(16, true, true);
    private final static String DST_NAME = "RELTEST_DST_" + RandomStringUtils.random(16, true, true);

    private ACL aclStructure, aclWorkflow, aclContent;
    private long relId, srcId[], dstId[];
    private int src_dst_count = 6;

    private void createProperty(long typeId, String name, String XPath) throws FxApplicationException {
        ass.createProperty(
                typeId,
                FxPropertyEdit.createNew(name, new FxString("Relation UnitTest property " + name),
                        new FxString("hint..."), FxMultiplicity.MULT_0_1, aclStructure, FxDataType.String1024).setAutoUniquePropertyName(true),
                XPath);
    }

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        super.init();
        login(TestUsers.SUPERVISOR);
        //create the base type
        ACL[] tmp = FxTestUtils.createACLs(
                new String[]{
                        "STRUCTURE_" + RandomStringUtils.random(16, true, true),
                        "WORKFLOW_" + RandomStringUtils.random(16, true, true),
                        "CONTENT_" + RandomStringUtils.random(16, true, true)

                },
                new ACLCategory[]{
                        ACLCategory.STRUCTURE,
                        ACLCategory.WORKFLOW,
                        ACLCategory.INSTANCE
                },
                TestUsers.getTestMandator()
        );
        aclStructure = tmp[0];
        aclWorkflow = tmp[1];
        aclContent = tmp[2];
        relId = type.save(FxTypeEdit.createNew(REL_NAME, new FxString("Test data"), aclStructure, null).setMaxRelSource(0).setMaxRelDestination(0).setMode(TypeMode.Relation));
        createProperty(relId, "R", "/");
        srcId = new long[src_dst_count];
        dstId = new long[src_dst_count];
        for (int i = 0; i < src_dst_count; i++) {
            srcId[i] = type.save(FxTypeEdit.createNew(SRC_NAME + "_" + (i + 1), new FxString("Test data"), aclStructure, null));
            createProperty(srcId[i], "S" + i, "/");
            dstId[i] = type.save(FxTypeEdit.createNew(DST_NAME + "_" + (i + 1), new FxString("Test data"), aclStructure, null));
            createProperty(dstId[i], "D" + i, "/");
        }
    }

    @Test
    public void verifyRelationMode() {
        Assert.assertTrue(CacheAdmin.getEnvironment().getType(relId).isRelation(), "Expected a relation but found a type");
    }

    @Test
    public void checkTypeRelation() throws FxApplicationException {
        //make relation
        FxTypeEdit rel = CacheAdmin.getEnvironment().getType(relId).asEditable();
        rel.addRelation(FxTypeRelationEdit.createNew(CacheAdmin.getEnvironment().getType(srcId[0]), CacheAdmin.getEnvironment().getType(dstId[0])));
        type.save(rel);

        rel = CacheAdmin.getEnvironment().getType(relId).asEditable();
        Assert.assertTrue(rel.getRelations().size() == 1, "One relation expected!");
        Assert.assertTrue(rel.getRemovedRelations().size() == 0);
        Assert.assertTrue(rel.getUpdatedRelations().size() == 0);
        Assert.assertTrue(rel.getAddedRelations().size() == 0);

        String assMsg = "No changes expected";
        rel.addRelation(buildRel(0, 0, 0, 0));
        Assert.assertTrue(rel.getRemovedRelations().size() == 0, assMsg);
        Assert.assertTrue(rel.getUpdatedRelations().size() == 0, assMsg);
        Assert.assertTrue(rel.getAddedRelations().size() == 0, assMsg);

        assMsg = "One update expected";
        rel.updateRelation(buildRel(0, 0, 1, 0));
        Assert.assertTrue(rel.getRemovedRelations().size() == 0, assMsg);
        Assert.assertTrue(rel.getUpdatedRelations().size() == 1, assMsg);
        Assert.assertTrue(rel.getAddedRelations().size() == 0, assMsg);

        assMsg = "One update, one added expected";
        rel.addRelation(buildRel(0, 1, 0, 10));
        Assert.assertTrue(rel.getRemovedRelations().size() == 0, assMsg);
        Assert.assertTrue(rel.getUpdatedRelations().size() == 1, assMsg);
        Assert.assertTrue(rel.getAddedRelations().size() == 1, assMsg);

        assMsg = "One removed, one added expected";
        rel.removeRelation(buildRel(0, 0, 1, 0));
        Assert.assertTrue(rel.getRemovedRelations().size() == 1, assMsg);
        Assert.assertTrue(rel.getUpdatedRelations().size() == 0, assMsg);
        Assert.assertTrue(rel.getAddedRelations().size() == 1, assMsg);

        type.save(rel);
        rel = CacheAdmin.getEnvironment().getType(relId).asEditable();
        Assert.assertTrue(rel.getRelations().size() == 1);
        FxTypeRelation check = rel.getRelations().get(0);
        Assert.assertTrue(check.getSource().getId() == srcId[0]);
        Assert.assertTrue(!check.isSourceLimited());
        Assert.assertTrue(check.getDestination().getId() == dstId[1]);
        Assert.assertTrue(check.isDestinationLimited());
        Assert.assertTrue(check.getMaxDestination() == 10);

        rel.removeRelation(buildRel(0, 1, 0, 0));
        rel.setRemoveInstancesWithRelationTypes(true);
        type.save(rel);
        Assert.assertTrue(CacheAdmin.getEnvironment().getType(relId).getRelations().size() == 0);
    }

    @Test
    public void checkRelationInstancesSimple() throws FxApplicationException {
        FxTypeEdit rel = CacheAdmin.getEnvironment().getType(relId).asEditable();
        rel.addRelation(buildRel(0, 0, 0, 0));
        type.save(rel);
        rel = CacheAdmin.getEnvironment().getType(relId).asEditable();

        FxPK src = co.save(co.initialize(srcId[0]));
        FxPK dst = co.save(co.initialize(dstId[0]));
        FxContent r = co.initialize(relId);
        r.setRelatedSource(src).setRelatedDestination(dst);
        FxPK pkr = co.save(r);
        r = co.load(pkr);
        Assert.assertTrue(r.getRelatedSource().equals(src), "Source does not match!");
        Assert.assertTrue(r.getRelatedDestination().equals(dst), "Destination does not match!");
        rel.removeRelation(rel.getRelations().get(0));
        rel.setRemoveInstancesWithRelationTypes(false);
        try {
            type.save(rel);
            Assert.fail("Exception expected!");
        } catch (FxApplicationException e) {
            //ok
        }
        rel.setRemoveInstancesWithRelationTypes(true);
        try {
            type.save(rel);
        } catch (FxApplicationException e) {
            Assert.fail("No exception expected, relation instances should have been removed. msg: " + e.getMessage());
        }
        rel = CacheAdmin.getEnvironment().getType(relId).asEditable();
        Assert.assertTrue(rel.getRelations().size() == 0, "Expected 0 relations for rel!");
    }

    private FxTypeRelation buildRel(int src, int dst, int maxSrc, int maxDst) {
        return FxTypeRelationEdit.createNew(CacheAdmin.getEnvironment().getType(srcId[src]),
                CacheAdmin.getEnvironment().getType(dstId[dst]), maxSrc, maxDst);
    }

    @AfterClass(dependsOnMethods = {"tearDownStructures"})
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        logout();
    }

    @AfterClass
    public void tearDownStructures() throws Exception {
        co.removeForType(relId);
        type.remove(relId);
        for (int i = 0; i < src_dst_count; i++) {
            co.removeForType(srcId[i]);
            type.remove(srcId[i]);
            co.removeForType(dstId[i]);
            type.remove(dstId[i]);
        }
        FxTestUtils.removeACL(aclStructure, aclWorkflow, aclContent);
    }

}
