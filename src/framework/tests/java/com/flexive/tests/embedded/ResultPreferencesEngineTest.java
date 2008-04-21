/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ResultPreferencesEngine;
import com.flexive.shared.search.*;
import com.flexive.shared.security.UserTicket;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

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
        final ResultPreferencesEngine rpi = EJBLookup.getResultPreferencesEngine();

        final long typeId = -2;
        final ResultViewType viewType = ResultViewType.LIST;
        final AdminResultLocations location = AdminResultLocations.ADMIN;
        try {
            assert !rpi.isCustomized(typeId, viewType, location);
            rpi.load(typeId, viewType, location);
            assert false : "Should not be able to load configuration for typeId=" + typeId;
        } catch (FxNotFoundException e) {
            // pass
        }
        try {
            rpi.save(rp, typeId, viewType, location);
            final ResultPreferences rpdb = rpi.load(typeId, viewType, location);
            assert rp.equals(rpdb) && rpdb.equals(rp) : "ResultPreferences should be equal: " + rp + " / " + rpdb;
            assert rpi.isCustomized(typeId, viewType, location);
        } finally {
            rpi.remove(typeId, viewType, location);
            assert !rpi.isCustomized(typeId, viewType, location);
        }
    }

    @Test
    public void resultPreferencesTimestamp() throws FxApplicationException {
        ResultPreferences rp = createResultPreferences();
        final ResultPreferencesEngine rpi = EJBLookup.getResultPreferencesEngine();
        assert rp.getLastChecked() == -1;
        final ResultViewType viewType = ResultViewType.LIST;
        final AdminResultLocations location = AdminResultLocations.ADMIN;
        final int typeId = -2;
        try {
            rpi.save(rp, typeId, viewType, location);
            // assert that check has been performed at least once
            assert rpi.load(typeId, viewType, location).getLastChecked() > 0;
            // TODO add test for environment timestamp
        } finally {
            rpi.remove(typeId, viewType, location);
            assert !rpi.isCustomized(typeId, viewType, location);
        }
    }

    @Test
    public void saveDefaultPreferences() throws FxApplicationException {
        final ResultPreferences rp = createResultPreferences();
        final UserTicket ticket = FxContext.get().getTicket();
        try {
            EJBLookup.getResultPreferencesEngine().saveSystemDefault(rp, -10,
                    ResultViewType.LIST, AdminResultLocations.ADMIN);
            assert ticket.isGlobalSupervisor() : "Only global supervisors might update the default result preferences.";
        } catch (FxNoAccessException e) {
            assert !ticket.isGlobalSupervisor() : "Global supervisors should be able to update the default result preferences.";
        }
    }

    private ResultPreferences createResultPreferences() {
        final List<ResultColumnInfo> selectedColumns = Arrays.asList(
                new ResultColumnInfo(Table.CONTENT, "id", null),
                new ResultColumnInfo(Table.CONTENT, "@pk", null));
        final List<ResultOrderByInfo> orderBy = Arrays.asList(new ResultOrderByInfo(Table.CONTENT, "id", null, SortDirection.ASCENDING));
        return new ResultPreferences(selectedColumns, orderBy, 25, 10);
    }

}
