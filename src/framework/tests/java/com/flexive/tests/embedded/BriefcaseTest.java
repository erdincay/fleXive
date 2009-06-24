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

import static com.flexive.shared.EJBLookup.getBriefcaseEngine;
import com.flexive.shared.FxReferenceMetaData;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.BriefcaseEngine;
import com.flexive.shared.search.Briefcase;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.SqlQueryBuilder;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import java.util.List;

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
        login(TestUsers.SUPERVISOR);
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

            final List<FxReferenceMetaData<FxPK>> emptyMeta = getBriefcaseEngine().loadMetaData(briefcaseId);
            assertEquals(emptyMeta.size(), ids.size());
            final FxPK reference = emptyMeta.get(0).getReference();
            assertTrue(emptyMeta.get(0).isEmpty(), "Empty metadata should be empty");

            final FxReferenceMetaData<FxPK> meta = FxReferenceMetaData.createNew(ids.get(0));
            meta.put("akey", "avalue");
            meta.put("bkey", "bvalue");
            getBriefcaseEngine().replaceMetaData(briefcaseId, asList(meta));

            final List<FxReferenceMetaData<FxPK>> newMeta = getBriefcaseEngine().loadMetaData(briefcaseId);
            assertEquals(newMeta.size(), ids.size());
            assertMetaDataSize(newMeta, reference, 2);
            assertMetadata(newMeta, reference, "akey", "avalue");
            assertMetadata(newMeta, reference, "bkey", "bvalue");

            // merge some updates
            final FxReferenceMetaData<FxPK> update = FxReferenceMetaData.createNew(reference);
            update.put("akey", "newvalue");
            update.put("newkey", 123);
            getBriefcaseEngine().mergeMetaData(briefcaseId, asList(update));
            final List<FxReferenceMetaData<FxPK>> updatedMeta = getBriefcaseEngine().loadMetaData(briefcaseId);
            assertEquals(updatedMeta.size(), ids.size());
            assertMetaDataSize(updatedMeta, reference, 3);
            assertMetadata(updatedMeta, reference, "akey", "newvalue");
            assertMetadata(updatedMeta, reference, "bkey", "bvalue");
            assertMetadata(updatedMeta, reference, "newkey", "123");

            // try to remove a key
            final FxReferenceMetaData<FxPK> removeUpdate = FxReferenceMetaData.createNew(reference);
            removeUpdate.put("newkey", "");
            getBriefcaseEngine().mergeMetaData(briefcaseId, asList(removeUpdate));
            final List<FxReferenceMetaData<FxPK>> updatedMeta2 = getBriefcaseEngine().loadMetaData(briefcaseId);
            assertEquals(updatedMeta2.size(), ids.size());
            assertMetaDataSize(updatedMeta2, reference, 2);
            assertMetadata(updatedMeta2, reference, "akey", "newvalue");
            assertMetadata(updatedMeta2, reference, "bkey", "bvalue");
        } finally {
            getBriefcaseEngine().remove(briefcaseId);
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
