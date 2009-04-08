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

import com.flexive.shared.FxContext;
import static com.flexive.shared.EJBLookup.*;
import com.flexive.shared.content.FxPK;
import static com.flexive.shared.CacheAdmin.getEnvironment;
import com.flexive.shared.structure.FxTypeEdit;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ResultPreferencesEngine;
import com.flexive.shared.search.*;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.security.UserTicket;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Basic unit tests for the result preferences EJB.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "search"})
public class ResultPreferencesEngineTest {
    private TestUser user;

    public ResultPreferencesEngineTest() {
    }

    public ResultPreferencesEngineTest(TestUser user) {
        this.user = user;
    }

    @Factory
    public Object[] createTestInstances() throws FxApplicationException {
        TestUsers.getConfiguredTestUsers(); // invoke initialization -  might be needed
        return new Object[]{
                new ResultPreferencesEngineTest(TestUsers.MANDATOR_SUPERVISOR),
                new ResultPreferencesEngineTest(TestUsers.SUPERVISOR)
        };
    }

    @BeforeMethod
    public void beforeMethod() throws FxLoginFailedException, FxAccountInUseException {
        FxTestUtils.login(user);
    }

    @AfterMethod
    public void afterMethod() throws FxLogoutFailedException {
        FxTestUtils.logout();
    }

    /**
     * Basic load/save test
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          if an application error occured
     */
    @Test
    public void resultPreferencesSaveLoad() throws FxApplicationException {
        ResultPreferences rp = createResultPreferences();
        final ResultPreferencesEngine rpi = getResultPreferencesEngine();

        final long typeId = -2;
        final ResultViewType viewType = ResultViewType.LIST;
        final AdminResultLocations location = AdminResultLocations.ADMIN;
        try {
            Assert.assertTrue(!rpi.isCustomized(typeId, viewType, location));
            rpi.load(typeId, viewType, location);
            Assert.fail("Should not be able to load configuration for typeId=" + typeId);
        } catch (FxNotFoundException e) {
            // pass
        }
        try {
            rpi.save(rp, typeId, viewType, location);
            final ResultPreferences rpdb = rpi.load(typeId, viewType, location);
            Assert.assertTrue(rp.equals(rpdb) && rpdb.equals(rp), "ResultPreferences should be equal: " + rp + " / " + rpdb);
            Assert.assertTrue(rpi.isCustomized(typeId, viewType, location));
        } finally {
            rpi.remove(typeId, viewType, location);
            Assert.assertTrue(!rpi.isCustomized(typeId, viewType, location));
        }
    }

    @Test
    public void resultPreferencesTimestamp() throws FxApplicationException {
        ResultPreferences rp = createResultPreferences();
        final ResultPreferencesEngine rpi = getResultPreferencesEngine();
        Assert.assertTrue(rp.getLastChecked() == -1);
        final ResultViewType viewType = ResultViewType.LIST;
        final AdminResultLocations location = AdminResultLocations.ADMIN;
        final int typeId = -2;
        try {
            rpi.save(rp, typeId, viewType, location);
            // assert that check has been performed at least once
            Assert.assertTrue(rpi.load(typeId, viewType, location).getLastChecked() > 0);
            // TODO add test for environment timestamp
        } finally {
            rpi.remove(typeId, viewType, location);
            Assert.assertTrue(!rpi.isCustomized(typeId, viewType, location));
        }
    }

    @Test
    public void saveDefaultPreferences() throws FxApplicationException {
        final ResultPreferences rp = createResultPreferences();
        final UserTicket ticket = FxContext.getUserTicket();
        try {
            getResultPreferencesEngine().saveSystemDefault(rp, -10,
                    ResultViewType.LIST, AdminResultLocations.ADMIN);
            Assert.assertTrue(ticket.isGlobalSupervisor(), "Only global supervisors might update the default result preferences.");
        } catch (FxNoAccessException e) {
            Assert.assertTrue(!ticket.isGlobalSupervisor(), "Global supervisors should be able to update the default result preferences.");
        }
    }

    @Test
    public void resultPreferencesWithAssignment() throws FxApplicationException {
        final long folderTypeId = getEnvironment().getType("folder").getId();
        try {
            createFolderPreferences(folderTypeId);
            final FxResultSet result = new SqlQueryBuilder().select("@*").type("folder").getResult();
            Assert.assertTrue(result.getColumnCount() == 1, "Expected one column, got: " + Arrays.toString(result.getColumnNames()));
            Assert.assertTrue(result.getColumnName(1).equals("folder/caption"), "Invalid column name: " + result.getColumnName(1));
        } finally {
            getResultPreferencesEngine().remove(folderTypeId, ResultViewType.LIST, AdminResultLocations.DEFAULT);
        }
    }

    @Test
    public void resultPreferencesAssignmentChecked_FX430() throws FxApplicationException {
        final long folderTypeId = getEnvironment().getType("folder").getId();
        try {
            createFolderPreferences(folderTypeId);
            Assert.assertTrue(getResultPreferencesEngine()
                    .load(folderTypeId, ResultViewType.LIST, AdminResultLocations.DEFAULT)
                    .getOrderByColumns().size() > 0, "OrderBy with assignment removed by internal check routine");
        } finally {
            getResultPreferencesEngine().remove(folderTypeId, ResultViewType.LIST, AdminResultLocations.DEFAULT);
        }
    }

    @Test
    public void resultPreferencesFallback_FX482() throws FxApplicationException {
        long baseTypeId = -1;
        long derivedTypeId = -1;
        final List<FxPK> pks = new ArrayList<FxPK>();
        try {
            try {
                FxContext.get().runAsSystem();
                baseTypeId = getTypeEngine().save(FxTypeEdit.createNew("TEST_FX482"));
                derivedTypeId = getTypeEngine().save(
                        FxTypeEdit.createNew("TEST_FX482_DERIVED", baseTypeId)
                );
            } finally {
                FxContext.get().stopRunAsSystem();
            }
            pks.add(getContentEngine().initialize(baseTypeId).save().getPk());
            pks.add(getContentEngine().initialize(derivedTypeId).save().getPk());

            // store result preferences for base type
            getResultPreferencesEngine().save(
                    new ResultPreferences(
                            Arrays.asList(
                                    new ResultColumnInfo("@pk"),
                                    new ResultColumnInfo("acl"),
                                    new ResultColumnInfo("created_at")
                            ),
                            new ArrayList<ResultOrderByInfo>(0),
                            10,
                            100
                    ),
                    baseTypeId,
                    ResultViewType.LIST,
                    AdminResultLocations.DEFAULT
            );

            // precondition: the result preferences are used for a result of the base type
            assertBasePreferencesPresent(new SqlQueryBuilder().select("@*").type(baseTypeId).getResult());
            // check if the result preferences are also used for a result of the derived type
            assertBasePreferencesPresent(new SqlQueryBuilder().select("@*").type(derivedTypeId).getResult());
        } finally {
            try {
                FxContext.get().runAsSystem();
                for (FxPK pk : pks) {
                    getContentEngine().remove(pk);
                }
                if (derivedTypeId != -1) {
                    getTypeEngine().remove(derivedTypeId);
                }
                if (baseTypeId != -1) {
                    getTypeEngine().remove(baseTypeId);
                }
            } finally {
                FxContext.get().stopRunAsSystem();
            }
        }
    }

    private void assertBasePreferencesPresent(FxResultSet result) {
        assertEquals(result.getRowCount(), 1);
        assertEquals(result.getColumnCount(), 3);
        assertEquals(result.getColumnName(1), "@pk");
        assertEquals(result.getColumnName(2), "acl");
        assertEquals(result.getColumnName(3), "created_at");
    }

    private void createFolderPreferences(long folderTypeId) throws FxApplicationException {
        getResultPreferencesEngine().save(new ResultPreferences(
                Arrays.asList(new ResultColumnInfo("#folder/caption")),
                Arrays.asList(new ResultOrderByInfo("#folder/caption", SortDirection.ASCENDING)),
                25,
                75),
                folderTypeId,
                ResultViewType.LIST, AdminResultLocations.DEFAULT);
    }


    private ResultPreferences createResultPreferences() {
        final List<ResultColumnInfo> selectedColumns = Arrays.asList(
                new ResultColumnInfo(Table.CONTENT, "id", null),
                new ResultColumnInfo(Table.CONTENT, "@pk", null));
        final List<ResultOrderByInfo> orderBy = Arrays.asList(new ResultOrderByInfo(Table.CONTENT, "id", null, SortDirection.ASCENDING));
        return new ResultPreferences(selectedColumns, orderBy, 25, 10);
    }

}
