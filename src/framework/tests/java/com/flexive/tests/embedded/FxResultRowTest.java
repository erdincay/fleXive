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

import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.FxResultRow;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.SqlQueryBuilder;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for the {@link com.flexive.shared.search.FxResultRow FxResultRow} wrapper object.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "search"})
public class FxResultRowTest {

    @BeforeClass
    public void setup() throws Exception {
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void shutdown() throws Exception {
        logout();
    }

    @Test
    public void rowIteratorTest() throws FxApplicationException {
        FxResultSet result = new SqlQueryBuilder().select("@pk", "caption")
                .condition("caption", PropertyValueComparator.LIKE, "test caption%")
                .getResult();
        Assert.assertEquals(result.getRows().size(), 25, "Expected 25 result rows");
        int index = 0;
        for (FxResultRow row : result.getResultRows()) {
            Assert.assertEquals(row.getPk(1), row.getPk("@pk"));
            Assert.assertEquals(row.getFxValue(2), row.getFxValue("caption"));
            Assert.assertEquals(row.getValue(1), row.getData()[0]);
            Assert.assertEquals(row.getValue(2), row.getData()[1]);
            Assert.assertEquals(row.getData(), result.getRows().get(index));
            index++;
        }
    }
}
