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
package com.flexive.tests.embedded.benchmark;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.scripting.FxScriptInfo;
import com.flexive.shared.scripting.FxScriptEvent;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxString;
import com.flexive.tests.embedded.TestUsers;
import com.flexive.tests.embedded.FxTestUtils;
import static com.flexive.tests.embedded.benchmark.FxBenchmarkUtils.getResultLogger;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmarks content instances.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = "benchmark", enabled = true)
public class ContentBenchmark {
    public static int scriptCtr1 = 0;
    public static int scriptCtr2 = 0;
    private static final String SCRIPT1 = "ContentBenchmark.test1.gy";
    private static final String SCRIPT2 = "ContentBenchmark.test2.gy";

    @BeforeClass(groups = "benchmark")
    public void init() throws FxLoginFailedException, FxAccountInUseException {
        FxTestUtils.login(TestUsers.SUPERVISOR);
    }

    @AfterClass(groups = "benchmark")
    public void cleanup() throws FxLogoutFailedException {
        FxTestUtils.logout();
    }

    @Test(groups = "benchmark")
    public void createContactDataBenchmark() throws FxApplicationException {
        // register some scripts
        final FxScriptInfo script1 = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.AfterContentCreate, SCRIPT1, "",
                "com.flexive.tests.embedded.benchmark.ContentBenchmark.scriptCtr1++");
        final FxScriptInfo script2 = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.AfterContentCreate, SCRIPT2, "",
                "com.flexive.tests.embedded.benchmark.ContentBenchmark.scriptCtr2++");
        final long contactDataId = CacheAdmin.getEnvironment().getType(FxType.CONTACTDATA).getId();
        EJBLookup.getScriptingEngine().createTypeScriptMapping(script1.getId(), contactDataId, true, true);
        EJBLookup.getScriptingEngine().createTypeScriptMapping(script2.getId(), contactDataId, true, true);

        final List<FxPK> result = new ArrayList<FxPK>();
        final ContentEngine ci = EJBLookup.getContentEngine();
        final long startTime = System.currentTimeMillis();
        final int runs = 100;
        try {
            for (int i = 0; i < runs; i++) {
                final FxContent content = createContactData(ci);
                result.add(ci.save(content));
            }
            assertEquals(scriptCtr1, runs);
            assertEquals(scriptCtr2, runs);
            getResultLogger().logTime("createContactData", startTime, 100, "instance");

            // perform a basic contact data query
            final long resultStart = System.currentTimeMillis();
            final FxResultSet rs = new SqlQueryBuilder().select("@pk").condition("name", PropertyValueComparator.EQ, "Peter").getResult();
            Assert.assertTrue(rs.getRowCount() > 0);
            getResultLogger().logTime("queryContactData", resultStart, rs.getRowCount(), "row");
        } finally {
            final long deleteStart = System.currentTimeMillis();
            for (FxPK pk : result) {
                try {
                    ci.remove(pk);
                } catch (Exception e) {
                    System.err.println("Failed to remove content " + pk + ": " + e.getMessage());
                }
            }
            getResultLogger().logTime("deleteContactData", deleteStart, 100, "instance");
            EJBLookup.getScriptingEngine().remove(script1.getId());
            EJBLookup.getScriptingEngine().remove(script2.getId());
        }
    }

    private FxContent createContactData(ContentEngine ci) throws FxApplicationException {
        final FxContent content = ci.initialize(FxType.CONTACTDATA);
        content.setValue("/name", new FxString(false, "Peter"));
        content.setValue("/surname", new FxString(false, "Bones"));
        content.setValue("/email", new FxString(false, "root@localhost"));
        content.setValue("/address/street", new FxString(false, "Loewengasse 2/8"));
        content.setValue("/address/zipCode", new FxString(false, "1030"));
        content.setValue("/address/city", new FxString(false, "Vienna"));
        return content;
    }

    @Test(groups = "benchmark")
    public void getContentData() throws FxApplicationException {
        final FxContent content = createContactData(EJBLookup.getContentEngine());
        final long startTime = System.currentTimeMillis();
        for (int i = 0; i < 100000; i++) {
            content.getData("/name");
            content.getData("/email");
            content.getData("/address/zipCode");
            content.getData("/address/street");
        }
        getResultLogger().logTime("getContentData", startTime, 100000 * 4, "instance property lookup");
    }
}
