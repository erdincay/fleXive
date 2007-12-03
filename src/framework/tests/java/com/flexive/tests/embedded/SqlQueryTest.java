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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxResultRow;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.FxSQLSearchParams;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.QueryOperatorNode;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.security.Account;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxLargeNumber;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * FxSQL search query engine tests.
 * <p/>
 * Test data is created in init1201_testcontent.gy !
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "search"})
public class SqlQueryTest {

    @BeforeClass
    public void setup() throws Exception {
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void shutdown() throws Exception {
        logout();
    }

    @Test
    public void simpleSelectTest() throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select("caption", "comment").enterSub(QueryOperatorNode.Operator.AND)
                .condition("caption", PropertyValueComparator.LIKE, "test caption%")
                .condition("comment", PropertyValueComparator.LIKE, "folder comment%")
                .closeSub().getResult();
        assert result.getRowCount() == 25 : "Expected to fetch 25 rows, got: " + result.getRowCount();
        assert result.getColumnIndex("co.caption") == 1 : "Unexpected column index for co.caption: " + result.getColumnIndex("co.caption");
        assert result.getColumnIndex("co.comment") == 2 : "Unexpected column index for co.comment: " + result.getColumnIndex("co.comment");
        for (int i = 1; i <= result.getRowCount(); i++) {
            assert result.getString(i, 1).startsWith("test caption") : "Unexpected column value: " + result.getString(i, 1);
            assert result.getString(i, 2).startsWith("folder comment") : "Unexpected column value: " + result.getString(i, 2);
        }
    }

    @Test
    public void simpleNestedQueryTest() throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select("caption").andSub()
                .condition("caption", PropertyValueComparator.LIKE, "test caption%")
                .orSub().condition("comment", PropertyValueComparator.LIKE, "folder comment 1")
                .condition("comment", PropertyValueComparator.LIKE, "folder comment 2").closeSub().getResult();
        assert result.getRowCount() == 2 : "Expected to fetch 2 rows, got: " + result.getRowCount();
        for (int i = 1; i <= 2; i++) {
            assert result.getString(i, 1).matches("test caption [12]") : "Unexpected column value: " + result.getString(i, 1);
        }
    }

    /**
     * Check if the SQL search for empty string properties works.
     *
     * @throws FxApplicationException if the search failed
     */
    @Test
    public void stringEmptyQuery() throws FxApplicationException {
        new SqlQueryBuilder().condition("caption", PropertyValueComparator.EMPTY, null).getResult();
        new SqlQueryBuilder().condition("caption", PropertyValueComparator.NOT_EMPTY, null).getResult();
        new SqlQueryBuilder().condition("caption", PropertyValueComparator.EMPTY, null)
                .orSub().condition("caption", PropertyValueComparator.EMPTY, null).condition("caption", PropertyValueComparator.NOT_EMPTY, null)
                .closeSub().getResult();
    }

    @Test
    public void selectUserTest() throws FxApplicationException {
        for (FxResultRow row : new SqlQueryBuilder().select("created_by", "created_by.username").getResult().getResultRows()) {
            final Account account = EJBLookup.getAccountEngine().load(((FxLargeNumber) row.getFxValue(1)).getDefaultTranslation());
            Assert.assertEquals(row.getFxValue(2).getDefaultTranslation(), account.getName());
        }
    }

    @Test
    public void filterByTypeTest() throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select("typedef").filterType("FOLDER").getResult();
        assert result.getRowCount() > 0;
        final FxType folderType = CacheAdmin.getEnvironment().getType("FOLDER");
        for (FxResultRow row : result.getResultRows()) {
            assert folderType.getId() == ((FxLargeNumber) row.getValue(1)).getBestTranslation()
                    : "Unexpected result type: " + row.getValue(1) + ", expected: " + folderType.getId();
        }
    }

    @Test
    public void briefcaseQueryTest() throws FxApplicationException {
        // create briefcase
        final String selectFolders = new SqlQueryBuilder().filterType("FOLDER").getQuery();
        final FxSQLSearchParams params = new FxSQLSearchParams().saveResultInBriefcase("test briefcase", "description", (Long) null);
        final FxResultSet result = EJBLookup.getSearchEngine().search(selectFolders, 0, Integer.MAX_VALUE, params);
        long bcId = result.getCreatedBriefcaseId();
        try {
            assert result.getRowCount() > 0;
            assert result.getCreatedBriefcaseId() != -1 : "Briefcase should have been created, but no ID returned.";

            // select briefcase
            final FxResultSet briefcase = new SqlQueryBuilder().filterBriefcase(result.getCreatedBriefcaseId()).getResult();
            assert briefcase.getRowCount() > 0 : "Empty briefcase returned, but getResult returned " + result.getRowCount() + " rows.";
        } finally {
            EJBLookup.getBriefcaseEngine().remove(bcId);
        }
    }

    @Test
    public void typeConditionTest() throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select("typedef").type("CONTACTDATA").getResult();
        final FxType cdType = CacheAdmin.getEnvironment().getType("CONTACTDATA");
        assert result.getRowCount() > 0;
        for (FxResultRow row : result.getResultRows()) {
            assert ((FxLargeNumber) row.getFxValue(1)).getDefaultTranslation() == cdType.getId()
                    : "Unexpected type in result, expected " + cdType.getId() + ", was: " + row.getFxValue(1);
        }
    }
}
