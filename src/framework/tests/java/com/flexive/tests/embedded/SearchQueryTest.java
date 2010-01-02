/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2010
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
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.SearchEngine;
import com.flexive.shared.search.AdminResultLocations;
import com.flexive.shared.search.query.QueryRootNode;
import com.flexive.shared.search.query.QueryRootNode.Type;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.QueryNodeTreeTests;
import com.flexive.tests.embedded.QueryNodeTreeTests.InnerNodeGenerator;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "search"})
public class SearchQueryTest {
    private static final String QUERYNAME = "AUTOMATED_TEST_QUERY";
    private SearchEngine searchEngine;

    @BeforeClass
    public void beforeClass() throws FxLookupException, FxLoginFailedException, FxAccountInUseException {
        searchEngine = EJBLookup.getSearchEngine();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        logout();
    }

    @Test(dataProvider = "sampleQueries")
    public void loadSaveQuery(QueryRootNode query) throws FxApplicationException {
        try {
            query.setName(QUERYNAME);
            searchEngine.save(query);
            QueryNodeTreeTests.assertEqualTrees(query, searchEngine.load(AdminResultLocations.DEFAULT, QUERYNAME));
        } finally {
            searchEngine.remove(AdminResultLocations.DEFAULT, QUERYNAME);
        }
    }

    @Test
    public void createDeleteQuery() throws FxApplicationException {
        QueryRootNode query = QueryNodeTreeTests.buildNestedTree(5, new InnerNodeGenerator(), 5);
        query.setName(QUERYNAME);
        searchEngine.save(query);
        // check that the query actually is stored in the db
        searchEngine.load(AdminResultLocations.DEFAULT, QUERYNAME);

        searchEngine.remove(AdminResultLocations.DEFAULT, QUERYNAME);
        try {
            searchEngine.load(AdminResultLocations.DEFAULT, QUERYNAME);
            Assert.fail("Query " + QUERYNAME + " should have been deleted.");
        } catch (FxNotFoundException e) {
            // pass
        }
    }

    @Test
    public void setDefaultQuery() throws FxApplicationException {
        QueryRootNode query = new QueryRootNode(Type.CONTENTSEARCH, AdminResultLocations.ADMIN);
        query.addChild(new QueryNodeTreeTests.InnerTestNode(42));
        searchEngine.saveSystemDefault(query);
        Assert.assertEquals(searchEngine.loadDefault(AdminResultLocations.ADMIN).getChild(0).getId(), 42, "First child should have ID 42");

        // now save user default
        query.getChild(0).setId(43);
        searchEngine.saveDefault(query);
        Assert.assertEquals(searchEngine.loadDefault(AdminResultLocations.ADMIN).getChild(0).getId(), 43, "First child should have ID 43");
    }

    @DataProvider(name = "sampleQueries")
    public Object[][] getSampleQueries() {
        return new Object[][]{
                {QueryNodeTreeTests.buildFlatTree(new InnerNodeGenerator(), 5)},
                {QueryNodeTreeTests.buildNestedTree(5, new InnerNodeGenerator(), 5)},
                {QueryNodeTreeTests.buildNestedTree(25, new InnerNodeGenerator(), 5)},
        };
    }
}
