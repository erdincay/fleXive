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
package com.flexive.tests.embedded.roles;

import static com.flexive.shared.EJBLookup.getScriptingEngine;
import com.flexive.shared.FxContext;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxNoAccessException;
import com.flexive.shared.scripting.FxScriptEvent;
import static com.flexive.shared.security.Role.ScriptExecution;
import static com.flexive.shared.security.Role.ScriptManagement;

import com.flexive.shared.scripting.FxScriptInfoEdit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 * Script management role tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "security", "roles"})
public class ScriptManagementTest extends AbstractRoleTest {
    private long scriptId = -1;
    private boolean forced = false;

    @AfterMethod(groups = {"ejb", "security", "roles"})
    public void cleanup() throws FxApplicationException {
        if (scriptId != -1) {
            if (forced) {
                FxContext.get().runAsSystem();
                getScriptingEngine().remove(scriptId);
                FxContext.get().stopRunAsSystem();
            } else {
                getScriptingEngine().remove(scriptId);
            }
            scriptId = -1;
            forced = false;
        }
    }

    @Test
    public void createScriptTest() throws FxApplicationException {
        try {
            createScript(false);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, ScriptManagement,  -1);
        }
    }

    @Test
    public void updateScriptTest() throws FxApplicationException {
        createScript(true);
        try {
            getScriptingEngine().updateScriptCode(scriptId, "new code");
            assertSuccess(ScriptManagement, -1);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, ScriptManagement, -1);
        }
    }

    @Test
    public void runScriptTest() throws FxApplicationException {
        createScript(true);
        try {
            getScriptingEngine().runScript(scriptId);
            assertSuccess(ScriptExecution, -1);
        } catch (FxNoAccessException e) {
            assertNoAccess(e, ScriptExecution, -1);
        }
    }

    private void createScript(boolean force) throws FxApplicationException {
        if (force) {
            FxContext.get().runAsSystem();
        }
        try {
            scriptId = getScriptingEngine().createScript(
                    new FxScriptInfoEdit(-1,FxScriptEvent.Manual, "script management test.gy", "", "1+1",true,true)).getId();
            forced = force;
            if (!force) {
                assertSuccess(ScriptManagement, -1);
            }
        } catch (FxNoAccessException e) {
            assertNoAccess(e, ScriptManagement, -1);
            throw e;
        } finally {
            if (force) {
                FxContext.get().stopRunAsSystem();
            }
        }
    }
}
