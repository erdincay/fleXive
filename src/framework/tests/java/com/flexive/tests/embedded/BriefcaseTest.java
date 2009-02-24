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
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.BriefcaseEngine;
import com.flexive.shared.search.Briefcase;
import com.flexive.shared.search.FxResultRow;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.SqlQueryBuilder;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import org.apache.commons.lang.ArrayUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Briefcase engine tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "search"})
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
        final BriefcaseEngine be = EJBLookup.getBriefcaseEngine();
        final long briefcaseId = be.create("test briefcase", "test description", null);
        try {
            final Briefcase briefcase = be.load(briefcaseId);
            Assert.assertEquals(briefcase.getName(), "test briefcase");
            Assert.assertEquals(briefcase.getDescription(), "test description");
            Assert.assertEquals(be.getItems(briefcaseId).length, 0, "Briefcase should be empty");
        } finally {
            be.remove(briefcaseId);
        }
    }

    @Test
    public void addBriefcaseItems() throws FxApplicationException {
        final BriefcaseEngine be = EJBLookup.getBriefcaseEngine();
        final long briefcaseId = be.create("test briefcase", "test description", null);
        try {
            // get some objects
            final FxResultSet result = new SqlQueryBuilder().select("@pk").filterType("FOLDER").condition("id", PropertyValueComparator.NE, 0).getResult();
            final List<Long> ids = new ArrayList<Long>(result.getRowCount());
            for (FxResultRow row : result.getResultRows()) {
                ids.add(row.getPk(1).getId());
            }
            Assert.assertTrue(!ids.isEmpty(), "No objects found for testing the briefcase engine");
            be.addItems(briefcaseId, ArrayUtils.toPrimitive(ids.toArray(new Long[ids.size()])));
            Assert.assertEquals(be.getItems(briefcaseId).length, ids.size());
            // add them again - shouldn't change briefcase
            be.addItems(briefcaseId, ArrayUtils.toPrimitive(ids.toArray(new Long[ids.size()])));
            Assert.assertEquals(be.getItems(briefcaseId).length, ids.size());

            // query briefcase
            final FxResultSet briefcaseResult = new SqlQueryBuilder().filterBriefcase(briefcaseId).select("@pk").getResult();
            Assert.assertEquals(briefcaseResult.getRowCount(), ids.size(), "Briefcase does not contain all items added previously");
            Assert.assertEquals(be.getItems(briefcaseId).length, ids.size(), "Briefcase does not contain all items added previously");
            // query briefcase with a condition
            final FxResultSet briefcaseResult2 = new SqlQueryBuilder().filterBriefcase(briefcaseId).select("@pk").condition("id", PropertyValueComparator.NE, 0).getResult();
            Assert.assertEquals(briefcaseResult2.getRowCount(), ids.size(), "Briefcase does not contain all items added previously");


            // replace briefcase content with first row only
            final long addId = result.getResultRow(0).getPk(1).getId();
            be.setItems(briefcaseId, new long[]{addId});
            testSingleItemBriefcase(be, briefcaseId, addId);

            // remove first row item, add second row item
            final long secondAddId = result.getResultRow(1).getPk(1).getId();
            be.updateItems(briefcaseId, new long[]{secondAddId}, new long[]{addId});
            testSingleItemBriefcase(be, briefcaseId, secondAddId);

            // clear briefcase
            be.clear(briefcaseId);
            Assert.assertEquals(new SqlQueryBuilder().filterBriefcase(briefcaseId).getResult().getRowCount(), 0);
        } finally {
            be.remove(briefcaseId);
        }
    }

    private void testSingleItemBriefcase(BriefcaseEngine be, long briefcaseId, long itemId) throws FxApplicationException {
        final FxResultSet oneResult = new SqlQueryBuilder().filterBriefcase(briefcaseId).select("@pk").getResult();
        Assert.assertEquals(oneResult.getRowCount(), 1, "Briefcase should contain only one item");
        Assert.assertEquals(be.getItems(briefcaseId).length, 1, "Briefcase should contain only one item");
        Assert.assertEquals(be.getItems(briefcaseId)[0], itemId, "Briefcase should contain item " + itemId);
        Assert.assertEquals(oneResult.getResultRow(0).getPk(1).getId(), itemId, "Briefcase item ID should be " + itemId);
    }
}
