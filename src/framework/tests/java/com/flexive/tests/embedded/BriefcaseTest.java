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

import com.flexive.shared.FxContext;
import com.flexive.shared.FxReferenceMetaData;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.interfaces.BriefcaseEngine;
import com.flexive.shared.search.Briefcase;
import com.flexive.shared.search.BriefcaseItemData;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.security.ACL;
import com.google.common.collect.Lists;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;

import static com.flexive.shared.EJBLookup.getBriefcaseEngine;
import static com.flexive.shared.EJBLookup.getSearchEngine;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import static java.util.Arrays.asList;
import static org.testng.Assert.*;

/**
 * Briefcase engine tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "search", "briefcase"})
public class BriefcaseTest {
    @BeforeClass
    public void beforeClass() throws Exception {
        login(TestUsers.REGULAR);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        logout();
    }

    @Test
    public void createDeleteBriefcase() throws FxApplicationException {
        final BriefcaseEngine be = getBriefcaseEngine();
        final long briefcaseId = be.create("test briefcase", "test description", null);
        try {
            final Briefcase briefcase = be.load(briefcaseId);
            assertEquals(briefcase.getName(), "test briefcase");
            assertEquals(briefcase.getDescription(), "test description");
            assertEquals(be.getItems(briefcaseId).length, 0, "Briefcase should be empty");
        } finally {
            be.remove(briefcaseId);
        }
    }

    @Test
    public void addBriefcaseItems() throws FxApplicationException {
        final BriefcaseEngine be = getBriefcaseEngine();
        final long briefcaseId = be.create("test briefcase", "test description", null);
        try {
            // get some objects
            final List<FxPK> ids = getFolders();
            assertTrue(!ids.isEmpty(), "No objects found for testing the briefcase engine");
            be.addItems(briefcaseId, ids);
            assertEquals(be.getItems(briefcaseId).length, ids.size());
            // add them again - shouldn't change briefcase
            be.addItems(briefcaseId, ids);
            assertEquals(be.getItems(briefcaseId).length, ids.size());

            // query briefcase
            final FxResultSet briefcaseResult = new SqlQueryBuilder().filterBriefcase(briefcaseId).select("@pk").getResult();
            assertEquals(briefcaseResult.getRowCount(), ids.size(), "Briefcase does not contain all items added previously");
            assertEquals(be.getItems(briefcaseId).length, ids.size(), "Briefcase does not contain all items added previously");
            // query briefcase with a condition
            final FxResultSet briefcaseResult2 = new SqlQueryBuilder().filterBriefcase(briefcaseId).select("@pk").condition("id", PropertyValueComparator.NE, 0).getResult();
            assertEquals(briefcaseResult2.getRowCount(), ids.size(), "Briefcase does not contain all items added previously");


            // replace briefcase content with first row only
            final FxPK addId = ids.get(0);
            be.setItems(briefcaseId, asList(addId));
            testSingleItemBriefcase(be, briefcaseId, addId.getId());

            // remove first row item, add second row item
            final FxPK secondAddId = ids.get(1);
            be.updateItems(briefcaseId, asList(secondAddId), asList(addId));
            testSingleItemBriefcase(be, briefcaseId, secondAddId.getId());

            // clear briefcase
            be.clear(briefcaseId);
            assertEquals(new SqlQueryBuilder().filterBriefcase(briefcaseId).getResult().getRowCount(), 0);
        } finally {
            be.remove(briefcaseId);
        }
    }

    @Test
    public void itemDataTest() throws FxApplicationException {
        final BriefcaseEngine be = getBriefcaseEngine();
        final long briefcaseId = be.create("test briefcase", "test description", null);
        try {
            // get some objects
            final List<FxPK> ids = getFolders();
            assertTrue(!ids.isEmpty(), "No objects found for testing the briefcase engine");
            be.addItems(briefcaseId, ids);
            assertEquals(be.getItems(briefcaseId).length, ids.size());

            long itemId = ids.get(0).getId();
            long itemId2 = ids.get(1).getId();
            BriefcaseItemData data1 = BriefcaseItemData.createBriefCaseItemData(briefcaseId, itemId, "TEST1");
            data1.setIntFlag1(1);
            data1.setIntFlag2(20);
            data1.setLongFlag1(1000L);
            data1.setLongFlag2(1001L);
            be.addItemData(briefcaseId, data1);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, null, null, null, null, null), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId, null, null, null, null, null, null), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId, "TEST1", null, null, null, null, null), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId, "TEST1", 1, null, null, null, null), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId, "TEST1", 1, 20, null, null, null), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId, "TEST1", 1, 20, null, 1000L, 1001L), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId, "TEST1", 1, 20, 0, 1000L, 1001L), 0);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId, "TEST2", 1, 20, null, 1000L, 1001L), 0);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId + 1, null, null, null, null, null, null), 0);
            be.removeItemData(briefcaseId, itemId);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, null, null, null, null, null), 0);

            BriefcaseItemData data2 = BriefcaseItemData.createBriefCaseItemData(briefcaseId, itemId, "TEST2");
            data2.setIntFlag1(1);
            data2.setIntFlag2(2);
            data2.setLongFlag1(1000L);
            data2.setLongFlag2(1002L);
            BriefcaseItemData data3 = BriefcaseItemData.createBriefCaseItemData(briefcaseId, itemId2, "TEST3");
            data3.setIntFlag1(1);
            data3.setIntFlag2(2);
            data3.setLongFlag1(1000L);
            data3.setLongFlag2(1002L);
            List<BriefcaseItemData> datas = Lists.newArrayList(data1, data2, data3);
            be.addItemData(briefcaseId, datas);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, null, null, null, null, null), 3);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId, null, null, null, null, null, null), 2);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId2, null, null, null, null, null, null), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, 1, null, null, null, null), 3);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, 1, null, null, 1000L, null), 3);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, null, null, null, 1000L, null), 3);
            assertEquals(be.queryItemDataCount(briefcaseId, null, "TEST2", null, null, null, 1000L, null), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, null, 2, null, null, null), 2);
            List<BriefcaseItemData> res = be.queryItemData(briefcaseId, null, "TEST2", null, null, null, null, null, BriefcaseItemData.SortField.INTFLAG1, BriefcaseItemData.SortOrder.ASC);
            assertEquals(res.size(), 1);
            BriefcaseItemData check = res.get(0);
            assertEquals(check.getBriefcaseId(), briefcaseId);
            assertEquals(check.getId(), itemId);
            assertEquals((int)check.getIntFlag1(), 1);
            assertEquals((int)check.getIntFlag2(), 2);
            assertFalse(check.isIntFlagSet(3));
            assertEquals((long)check.getLongFlag1(), 1000L);
            assertEquals((long)check.getLongFlag2(), 1002L);
            assertEquals(check.getName(), "TEST2");
            check.setIntFlag1(11);
            check.setIntFlag3(3);
            be.updateItemData(briefcaseId, check);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, null, null, null, null, null), 3);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId, null, null, null, null, null, null), 2);
            assertEquals(be.queryItemDataCount(briefcaseId, itemId2, null, null, null, null, null, null), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, 1, null, null, null, null), 2);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, 11, null, null, null, null), 1);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, null, null, 3, null, null), 1);


            // clear briefcase
            be.clear(briefcaseId);
            assertEquals(be.queryItemDataCount(briefcaseId, null, null, null, null, null, null, null), 0);
            assertEquals(new SqlQueryBuilder().filterBriefcase(briefcaseId).getResult().getRowCount(), 0);
        } finally {
            be.remove(briefcaseId);
        }
    }

    @Test
    public void moveBriefcaseItems() throws FxApplicationException {
        final BriefcaseEngine be = getBriefcaseEngine();
        final long briefcase1 = be.create("move items briefcase 1", "test", null);
        final long briefcase2 = be.create("move items briefcase 2", "test", null);
        try {
            final List<FxPK> ids = getFolders();
            be.addItems(briefcase1, ids);
            assertEquals(be.load(briefcase1).getSize(), ids.size());

            // move to second briefcase
            assertEquals(be.load(briefcase2).getSize(), 0);
            be.moveItems(briefcase1, briefcase2, ids);

            assertEquals(be.load(briefcase1).getSize(), 0);
            assertEquals(be.load(briefcase2).getSize(), ids.size());
        } finally {
            be.remove(briefcase1);
            be.remove(briefcase2);
        }
    }

    @Test
    public void replaceMetaData() throws FxApplicationException {
        final long briefcaseId = getBriefcaseEngine().create("replaceMetaData", "test", null);
        try {
            final List<FxPK> ids = getFolders();
            getBriefcaseEngine().addItems(briefcaseId, ids);

            final List<FxReferenceMetaData<FxPK>> emptyMeta = getBriefcaseEngine().getMetaData(briefcaseId);
            assertEquals(emptyMeta.size(), ids.size());
            final FxPK reference = emptyMeta.get(0).getReference();
            assertTrue(emptyMeta.get(0).isEmpty(), "Empty metadata should be empty");

            final FxReferenceMetaData<FxPK> meta = FxReferenceMetaData.createNew(ids.get(0));
            meta.put("akey", "avalue");
            meta.put("bkey", "bvalue");
            //noinspection unchecked
            getBriefcaseEngine().setMetaData(briefcaseId, asList(meta));

            final List<FxReferenceMetaData<FxPK>> newMeta = getBriefcaseEngine().getMetaData(briefcaseId);
            // check BriefcaseEngine#getMetaData(long, FxPK)
            assertFalse(newMeta.isEmpty());
            for (FxReferenceMetaData<FxPK> metaData : newMeta) {
                assertEquals(getBriefcaseEngine().getMetaData(briefcaseId, metaData.getReference()), metaData);
            }

            assertEquals(newMeta.size(), ids.size());
            assertMetaDataSize(newMeta, reference, 2);
            assertMetadata(newMeta, reference, "akey", "avalue");
            assertMetadata(newMeta, reference, "bkey", "bvalue");

            // merge some updates
            final FxReferenceMetaData<FxPK> update = FxReferenceMetaData.createNew(reference);
            update.put("akey", "newvalue");
            update.put("newkey", 123);
            //noinspection unchecked
            getBriefcaseEngine().mergeMetaData(briefcaseId, asList(update));
            final List<FxReferenceMetaData<FxPK>> updatedMeta = getBriefcaseEngine().getMetaData(briefcaseId);
            assertEquals(updatedMeta.size(), ids.size());
            assertMetaDataSize(updatedMeta, reference, 3);
            assertMetadata(updatedMeta, reference, "akey", "newvalue");
            assertMetadata(updatedMeta, reference, "bkey", "bvalue");
            assertMetadata(updatedMeta, reference, "newkey", "123");

            // try to remove a key
            final FxReferenceMetaData<FxPK> removeUpdate = FxReferenceMetaData.createNew(reference);
            removeUpdate.put("newkey", "");
            //noinspection unchecked
            getBriefcaseEngine().mergeMetaData(briefcaseId, asList(removeUpdate));
            final List<FxReferenceMetaData<FxPK>> updatedMeta2 = getBriefcaseEngine().getMetaData(briefcaseId);
            assertEquals(updatedMeta2.size(), ids.size());
            assertMetaDataSize(updatedMeta2, reference, 2);
            assertMetadata(updatedMeta2, reference, "akey", "newvalue");
            assertMetadata(updatedMeta2, reference, "bkey", "bvalue");
        } finally {
            getBriefcaseEngine().remove(briefcaseId);
        }
    }

    @Test
    public void systemInternalBriefcase() throws FxApplicationException {
        // use the NULL ACL to create a briefcase that can be read and queried only by the global supervisor
        // or with FxContext.get().runAsSystem()
        final List<FxPK> pks = getSearchEngine().search("SELECT @pk").<FxPK>collectColumn(1).subList(0, 10);
        assertFalse(pks.isEmpty());

        try {
            final long id = getBriefcaseEngine().create("systemInternalBriefcase", "", ACL.NULL_ACL_ID);
            fail("Normal user shouldn't be able to create a system internal briefcase");
            getBriefcaseEngine().remove(id);
        } catch (FxNoAccessException e) {
            // pass
        }

        final long id;
        FxContext.get().runAsSystem();
        try {
            id = getBriefcaseEngine().create("systemInternalBriefcase", "", ACL.NULL_ACL_ID);
        } finally {
            FxContext.get().stopRunAsSystem();
        }
        try {
            // try to read as a normal user
            try {
                getBriefcaseEngine().load(id);
                fail("Normal user shouldn't be able to load the system internal briefcase.");
            } catch (FxApplicationException e) {
                // pass
            }

            // try to update as a normal user
            try {
                getBriefcaseEngine().addItems(id, pks);
                fail("Normal user shouldn't be able to add items to a system internal briefcase.");
            } catch (FxApplicationException e) {
                // pass
            }

            // try to read/update as a system user
            FxContext.get().runAsSystem();
            try {
                getBriefcaseEngine().load(id);
                getBriefcaseEngine().removeItems(id, pks);
                assertEquals(getBriefcaseEngine().load(id).getSize(), 0, "Items should have been removed");
            } finally {
                FxContext.get().stopRunAsSystem();
            }

        } finally {
            FxContext.get().runAsSystem();
            try {
                getBriefcaseEngine().remove(id);
            } finally {
                FxContext.get().stopRunAsSystem();
            }
        }
    }


    private void assertMetaDataSize(List<FxReferenceMetaData<FxPK>> updatedMeta2, FxPK reference, int size) {
        assertEquals(FxReferenceMetaData.find(updatedMeta2, reference).size(), size);
    }

    private void assertMetadata(List<FxReferenceMetaData<FxPK>> updatedMeta2, FxPK reference, String key, String value) {
        assertEquals(FxReferenceMetaData.find(updatedMeta2, reference).get(key), value);
    }

    private void testSingleItemBriefcase(BriefcaseEngine be, long briefcaseId, long itemId) throws FxApplicationException {
        final FxResultSet oneResult = new SqlQueryBuilder().filterBriefcase(briefcaseId).select("@pk").getResult();
        assertEquals(oneResult.getRowCount(), 1, "Briefcase should contain only one item");
        assertEquals(be.getItems(briefcaseId).length, 1, "Briefcase should contain only one item");
        assertEquals(be.getItems(briefcaseId)[0], itemId, "Briefcase should contain item " + itemId);
        assertEquals(oneResult.getResultRow(0).getPk(1).getId(), itemId, "Briefcase item ID should be " + itemId);
    }

    private List<FxPK> getFolders() throws FxApplicationException {
        return new SqlQueryBuilder().select("@pk").filterType("FOLDER").getResult().collectColumn(1);
    }

}
