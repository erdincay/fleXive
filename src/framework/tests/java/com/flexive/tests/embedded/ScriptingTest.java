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

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.configuration.*;
import com.flexive.shared.configuration.parameters.ParameterFactory;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.ScriptingEngine;
import com.flexive.shared.scripting.*;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.ejb.beans.ScriptingEngineBean;
import org.testng.Assert;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests for the scripting engine (grooooooovy)
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "scripting"})
public class ScriptingTest {
    public static boolean allowTearDown = false;

    ScriptingEngine se;
    public static FxScriptInfo loadScript = null;
    public static FxScriptInfo removeScript = null;
    public static int testScriptExecution = 0;

    @BeforeSuite(dependsOnGroups = {"bootstrap"})
    public void suiteSetup() throws FxApplicationException, FxLoginFailedException, FxAccountInUseException, FxLogoutFailedException {
        login(TestUsers.SUPERVISOR);
        try {
            String codeLoad = "println \"[Groovy script]=== Loading Content \"+content.pk+\"(\"+environment.getType(content.getTypeId()).getName()+\") ===\"";
            String codeRemove = "println \"[Groovy script]=== Before removal of content \"+pk+\"(\"+environment.getType(securityInfo.getTypeId()).getName()+\") ===\"";
            loadScript = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.AfterContentLoad, "afterLoadTest.gy", "Test script", codeLoad);
            removeScript = EJBLookup.getScriptingEngine().createScript(FxScriptEvent.BeforeContentRemove, "beforeRemoveTest.gy", "Test script", codeRemove);
        } finally {
            logout();
        }
    }

    public static void suiteShutDown() throws Exception {
        if (!allowTearDown) //"hack" to prevent this method to be run as normal test
            return;
        login(TestUsers.SUPERVISOR);
        if (loadScript != null)
            EJBLookup.getScriptingEngine().remove(loadScript.getId());
        if (removeScript != null)
            EJBLookup.getScriptingEngine().remove(removeScript.getId());
        logout();
    }

    @BeforeClass
    public void setup() throws Exception {
        se = EJBLookup.getScriptingEngine();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void shutdown() throws Exception {
        logout();
    }

    /**
     * This method tests simple script ops: creating, running, loading and removing scripts;
     *
     * @throws Exception on errors
     */
    @Test
    public void simpleScript() throws Exception {
        String name = "unittest.gy";
        String desc = "unit test script";
        String code = "return \"hello world\";";
        String codeUpdate = "return \"hello universe\";";
        String wrongCode = "retur \"hello, my syntax is wrong\"";
        Object result = "hello world";

        FxScriptInfo si = se.createScript(FxScriptEvent.Manual, name, desc, code);
        Assert.assertTrue(si != null, "No FxScriptInfo returned!");
        Assert.assertTrue(si.getId() > 0, "Invalid script id");
        Assert.assertTrue(si.getName().equals(name), "Invalid name");
        Assert.assertTrue(si.getDescription().equals(desc), "Invalid description");
        Assert.assertTrue(si.getEvent().equals(FxScriptEvent.Manual), "Invalid script type");
        Assert.assertTrue(se.runScript(si.getId(), new FxScriptBinding()).getResult().equals(result), "Invalid result from script!");
        // test remaining #runScript methods
        assertEquals(se.runScript(si.getId()).getResult(), result);
        assertEquals(se.runScript(name, new FxScriptBinding()).getResult(), result);
        assertEquals(se.runScript(name, new FxScriptBinding(), code).getResult(), result);
        // test the GenericScriptException
        try {
            se.runScript(name, new FxScriptBinding(), wrongCode);
            fail("Executing a groovy script having faulty syntax should have failed");
        } catch (ScriptingEngineBean.GenericScriptException e) {
            // expected
        }

        // compare with getScriptInfos()
        for (FxScriptInfo sInfo : se.getScriptInfos()) {
            if (sInfo.getId() == si.getId()) {
                assertEquals(sInfo.getName(), si.getName());
                assertEquals(sInfo.getDescription(), si.getDescription());
                assertEquals(sInfo.getEvent(), si.getEvent());
                assertEquals(sInfo.getCode(), si.getCode());
            }
        }
        assertEquals(se.loadScriptCode(si.getId()), code); // compare code
        se.updateScriptCode(si.getId(), codeUpdate); // update and compare again
        assertEquals(se.loadScriptCode(si.getId()), codeUpdate);

        se.remove(si.getId());
    }

    /**
     * This method tests activation and deactivation of scripts
     *
     * @throws Exception on errors
     */
    @Test
    public void scriptActivation() throws Exception {
        FxScriptInfo si = se.createScript(FxScriptEvent.Manual, "manualTestScript.gy", "manualTestScript", "return \"done\";");
        try {
            se.runScript(si.getId());
            FxScriptInfoEdit inactive = si.asEditable();
            inactive.setActive(false);
            se.updateScriptInfo(inactive);
            Assert.assertEquals(se.runScript(si.getId()).getResult(), null, "Inactive scripts must not be runnable!");
        } finally {
            se.remove(si.getId());
        }
    }

    /**
     * This method tests all aspects of script assignments to type properties.
     *
     * @throws Exception on errors
     */
    @Test
    public void scriptAssignmentMapping() throws Exception {
        long typeId = EJBLookup.getTypeEngine().save(FxTypeEdit.createNew("AS_SCRIPTING", new FxString("Assignment scripting test type"),
                CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()), null));
        EJBLookup.getAssignmentEngine().createProperty(
                typeId,
                FxPropertyEdit.createNew(
                        "A",
                        new FxString(true, FxLanguage.ENGLISH, "A"),
                        new FxString(true, FxLanguage.ENGLISH, "A"),
                        new FxMultiplicity(0, 5),
                        CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                        FxDataType.String1024).setMultiLang(false),
                "/");
        EJBLookup.getAssignmentEngine().createProperty(
                typeId,
                FxPropertyEdit.createNew(
                        "B",
                        new FxString(true, FxLanguage.ENGLISH, "B"),
                        new FxString(true, FxLanguage.ENGLISH, "B"),
                        new FxMultiplicity(0, 5),
                        CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                        FxDataType.String1024).setMultiLang(false),
                "/");
        EJBLookup.getAssignmentEngine().createProperty(
                typeId,
                FxPropertyEdit.createNew(
                        "C",
                        new FxString(true, FxLanguage.ENGLISH, "C"),
                        new FxString(true, FxLanguage.ENGLISH, "C"),
                        new FxMultiplicity(0, 5),
                        CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                        FxDataType.String1024).setMultiLang(false),
                "/");
        // create a derived property
        EJBLookup.getAssignmentEngine().save(FxPropertyAssignmentEdit.reuse("AS_SCRIPTING/C", "AS_SCRIPTING", "/", "NEW_C"), false);

        FxType type = CacheAdmin.getEnvironment().getType(typeId);
        List<FxScriptInfo> scripts = new ArrayList<FxScriptInfo>(10);
        try {
            FxScriptEvent event = FxScriptEvent.AfterDataChangeDelete;
            FxScriptInfo testAssignmentScript = se.createScript(event, "Test2" + event.getName() + ".gy", "", "");
            final long assId = type.getAssignment("/C").getId();
            final long assIdDer = type.getAssignment("/NEW_C").getId();

            // test removal of assignment and all assignments for "/C"
            se.createAssignmentScriptMapping(testAssignmentScript.getId(), assId, true, true);
            FxScriptMapping sm = se.loadScriptMapping(testAssignmentScript.getId());
            List<FxScriptMappingEntry> sme = sm.getMappedAssignments();
            assertEquals(sme.get(0).getScriptEvent(), FxScriptEvent.AfterDataChangeDelete);

            se.removeAssignmentScriptMappingForEvent(testAssignmentScript.getId(), assId, FxScriptEvent.AfterDataChangeDelete);
            sm = se.loadScriptMapping(testAssignmentScript.getId());
            sme = sm.getMappedAssignments();
            assertEquals(sme.size(), 0);

            // reassign, test reassignment and remove all assignments for "/B"
            se.remove(testAssignmentScript.getId());
            testAssignmentScript = se.createScript(event, "TestNoTwo" + event.getName() + ".gy", "", "");
            se.createAssignmentScriptMapping(testAssignmentScript.getId(), assId, true, true);
            sm = se.loadScriptMapping(testAssignmentScript.getId());
            sme = sm.getMappedAssignments();
            assertEquals(sme.get(0).getScriptEvent(), FxScriptEvent.AfterDataChangeDelete);
            se.removeAssignmentScriptMapping(testAssignmentScript.getId(), assId);
            sm = se.loadScriptMapping(testAssignmentScript.getId());
            sme = sm.getMappedAssignments();
            assertEquals(sme.size(), 0);

            // check assignment consistency
            se.remove(testAssignmentScript.getId());
            testAssignmentScript = se.createScript(event, "TestNoTwo" + event.getName() + ".gy", "", "");
            try {
                se.createAssignmentScriptMapping(testAssignmentScript.getId(), assIdDer, true, true);
                se.createAssignmentScriptMapping(testAssignmentScript.getId(), assIdDer, true, true);
                fail("Assigning the same script event to the derived property should have failed.");
            } catch (FxEntryExistsException e) {
                // expected
            }
            se.remove(testAssignmentScript.getId());

            // test the assignment update
            se.remove(testAssignmentScript.getId());
            testAssignmentScript = se.createScript(event, "Test2" + event.getName() + ".gy", "", "");
            se.createAssignmentScriptMapping(testAssignmentScript.getId(), assId, true, true);
            se.updateAssignmentScriptMappingForEvent(testAssignmentScript.getId(), assId, event, false, false);
            sm = se.loadScriptMapping(testAssignmentScript.getId());
            sme = sm.getMappedAssignments();
            assertFalse(sme.get(0).isActive());
            assertFalse(sme.get(0).isDerivedUsage());
            se.remove(testAssignmentScript.getId()); // clean up

            // create various test assignments (will produce console output)
            scripts.add(createScript(type.getAssignment("/A"), FxScriptEvent.BeforeAssignmentDataCreate));
            scripts.add(createScript(type.getAssignment("/A"), FxScriptEvent.AfterAssignmentDataCreate));
            scripts.add(createScript(type.getAssignment("/A"), FxScriptEvent.BeforeAssignmentDataDelete));
            scripts.add(createScript(type.getAssignment("/A"), FxScriptEvent.AfterAssignmentDataDelete));
            scripts.add(createScript(type.getAssignment("/B"), FxScriptEvent.BeforeDataChangeAdd));
            scripts.add(createScript(type.getAssignment("/B"), FxScriptEvent.BeforeDataChangeUpdate));
            scripts.add(createScript(type.getAssignment("/B"), FxScriptEvent.BeforeDataChangeDelete));
            scripts.add(createScript(type.getAssignment("/B"), FxScriptEvent.AfterDataChangeAdd));
            scripts.add(createScript(type.getAssignment("/B"), FxScriptEvent.AfterDataChangeUpdate));
            scripts.add(createScript(type.getAssignment("/B"), FxScriptEvent.AfterDataChangeDelete));

            FxString data = new FxString(false, "data");
            FxString data2 = new FxString(false, "data2");
            ContentEngine ce = EJBLookup.getContentEngine();
            FxContent co = ce.initialize(typeId);
            FxPK pk = ce.save(co);
            co = ce.load(pk);
            co.setValue("/B[1]", data);
            System.out.println("=>add B[1]");
            ce.save(co);
            co = ce.load(pk);
            FxString tmp = (FxString) co.getValue("/B[1]");
            tmp.setTranslation(FxLanguage.GERMAN, "data de");
            System.out.println("=>update B[1]");
            ce.save(co);
            co = ce.load(pk);
            co.setValue("/B[2]", data2);
            System.out.println("=>add B[2]");
            ce.save(co);
            co = ce.load(pk);
            co.remove("/B[2]");
            System.out.println("=>delete B[2]");
            ce.save(co);
            EJBLookup.getContentEngine().remove(pk);
        } finally {
            EJBLookup.getTypeEngine().remove(typeId);
            for (FxScriptInfo si : scripts)
                se.remove(si.getId());
        }
    }

    /**
     * Helper method for creating script assignments to type properties
     *
     * @param assignment the property assignment
     * @param event      the scripting event
     * @return an instance of FxScriptInfo
     * @throws FxApplicationException on errors
     */
    private FxScriptInfo createScript(FxAssignment assignment, FxScriptEvent event) throws FxApplicationException {
        String data = "unknown";
        if (event.name().startsWith("BeforeAssignment"))
            data = "assignment";
        else if (event.name().startsWith("AfterAssignment"))
            data = "assignment";
        else if (event.name().startsWith("BeforeDataChange"))
            data = "change";
        else if (event.name().startsWith("AfterDataChange"))
            data = "change";
        FxScriptInfo si = se.createScript(event, "Test" + event.name() + ".gy",
                "",
                "println \"This is [" + event.name() + "] registered for Assignment [" + assignment.getXPath() + "] - I am called for ${" + data + "}\"");
        se.createAssignmentScriptMapping(si.getId(), assignment.getId(), true, true);
        return si;
    }

    /**
     * This method tests general info methods
     *
     * @throws Exception on errors
     */
    @Test
    public void scriptingEngines() throws Exception {
        List<String[]> scriptingEngines = se.getAvailableScriptEngines();
        assertTrue(scriptingEngines.size() >= 1);
        // check Groovy is present (with a max count of 2)
        int count = 0;
        for (String[] s : scriptingEngines) {
            if (s[0].equals("groovy") || s[0].equals("gy"))
                count++;
        }
        assertEquals(count, 2);

        // runonce scripts
        List<FxScriptRunInfo> runOnce = se.getRunOnceInformation();
        assertTrue(runOnce.size() > 0);
    }

    /**
     * This method tests if type script assignments are properly propagated to derived types
     *
     * @throws Exception on errors
     */
    @Test
    public void derivedTypeScriptMapping() throws Exception {
        FxScriptInfo si = null;
        long parent = -1;
        long derived = -1;
        try {
            //create parent
            parent = EJBLookup.getTypeEngine().save(FxTypeEdit.createNew("TY_SCRIPTING_PARENT", new FxString("Type scripting parent test type"),
                    CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()), null));
            FxType parentT = CacheAdmin.getEnvironment().getType(parent);
            FxScriptEvent event = FxScriptEvent.PrepareContentCreate;
            //assign script
            si= createScriptForType(event, parentT);
            //create derived type
            derived = EJBLookup.getTypeEngine().save(FxTypeEdit.createNew("TY_SCRIPTING_DERIVED", new FxString(true, "TY_SCRIPTING_DERIVED"), parentT.getACL(), parentT));
            FxType derivedT = CacheAdmin.getEnvironment().getType(derived);
            //check if same script is assigned to parent and derived type
            assertTrue(derivedT.getScriptMapping(event)[0] == si.getId() && si.getId() == parentT.getScriptMapping(event)[0]);

            //cancel derived usage
            se.updateTypeScriptMappingForEvent(si.getId(), parentT.getId(), FxScriptEvent.PrepareContentCreate, true, false);
            //check if script is still assigned
            assertTrue(CacheAdmin.getEnvironment().getType(derived).getScriptMapping(event)==null);
        }
        finally {
            if (derived != -1)
                EJBLookup.getTypeEngine().remove(derived);
            if (parent != -1)
                EJBLookup.getTypeEngine().remove(parent);
            if (si != null)
                se.remove(si.getId());
        }
    }

    /**
     * This method tests script assignments to types
     *
     * @throws Exception on errors
     */
    @Test
    public void typeScriptMapping() throws Exception {
        long typeId = EJBLookup.getTypeEngine().save(FxTypeEdit.createNew("TY_SCRIPTING", new FxString("Type scripting test type"),
                CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()), null));

        EJBLookup.getAssignmentEngine().createProperty(typeId, FxPropertyEdit.createNew(
                "A123321", new FxString(true, FxLanguage.ENGLISH, "A123321"), new FxString(true, FxLanguage.ENGLISH, "A"),
                new FxMultiplicity(0, 5), CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024).setMultiLang(false), "/");

        FxType type = CacheAdmin.getEnvironment().getType(typeId);

        List<FxScriptInfo> scripts = new ArrayList<FxScriptInfo>(8);
        try {
            // remove and update a type script event
            FxScriptEvent event = FxScriptEvent.PrepareContentCreate;
            FxScriptInfo si = createScriptForType(event, type);
            FxScriptMapping sm = se.loadScriptMapping(si.getId());
            List<FxScriptMappingEntry> sme = sm.getMappedTypes();
            assertEquals(sme.get(0).getScriptEvent(), FxScriptEvent.PrepareContentCreate); // check for event equality

            try { // check mapping consistency
                se.createTypeScriptMapping(si.getId(), type.getId(), true, true);
                fail("Reassigning the same script event to this type should have failed.");
            } catch (FxEntryExistsException e) {
                // expected
            }

            // update test
            se.updateTypeScriptMappingForEvent(si.getId(), type.getId(), FxScriptEvent.PrepareContentCreate, false, false);
            sm = se.loadScriptMapping(si.getId());
            sme = sm.getMappedTypes();
            assertFalse(sme.get(0).isActive()); // check that the changes have been made
            assertFalse(sme.get(0).isDerivedUsage());

            // remove script mapping test (for specific event)
            se.removeTypeScriptMappingForEvent(si.getId(), type.getId(), FxScriptEvent.PrepareContentCreate);
            sm = se.loadScriptMapping(si.getId());
            sme = sm.getMappedTypes();
            assertEquals(sme.size(), 0);

            // general remove test
            se.remove(si.getId());
            si = createScriptForType(FxScriptEvent.PrepareContentCreate, type);
            se.removeTypeScriptMapping(si.getId(), type.getId());
            sm = se.loadScriptMapping(si.getId());
            sme = sm.getMappedTypes();
            assertEquals(sme.size(), 0);
            se.remove(si.getId()); // clean up

            // create various scripts for content creation events
            scripts.add(createScriptForType(FxScriptEvent.BeforeContentCreate, type));
            scripts.add(createScriptForType(FxScriptEvent.AfterContentCreate, type));
            scripts.add(createScriptForType(FxScriptEvent.BeforeContentSave, type));
            scripts.add(createScriptForType(FxScriptEvent.AfterContentSave, type));
            scripts.add(createScriptForType(FxScriptEvent.BeforeContentRemove, type));
            scripts.add(createScriptForType(FxScriptEvent.AfterContentRemove, type));
            scripts.add(createScriptForType(FxScriptEvent.AfterContentLoad, type));
            scripts.add(createScriptForType(FxScriptEvent.AfterContentInitialize, type));

            // create content for testing
            FxString data = new FxString(false, "testdata, def lang");
            ContentEngine ce = EJBLookup.getContentEngine();
            FxContent co = ce.initialize(typeId);
            FxPK pk = ce.save(co);
            co = ce.load(pk);
            co.setValue("/A123321[1]", data);
            ce.save(co);
            co = ce.load(pk);
            FxString tmp = (FxString) co.getValue("/A123321[1]");
            tmp.setTranslation(FxLanguage.GERMAN, "testdata, de");
            ce.save(co);
            EJBLookup.getContentEngine().remove(pk);
        } finally {
            EJBLookup.getTypeEngine().remove(type.getId());
            for (FxScriptInfo si : scripts) {
                se.remove(si.getId());
            }
        }
    }

    /**
     * Helper method for type-script mappings and creation
     *
     * @param event the scripting event
     * @param type  the content type
     * @return returns an instance of FxScriptInfo
     * @throws FxApplicationException on errors
     */
    private FxScriptInfo createScriptForType(FxScriptEvent event, FxType type) throws FxApplicationException {
        FxScriptInfo si = se.createScript(event, "TestTypeMap" + event.getName() + ".gy", "", "println \"TYPE script mapping tests: fired for type "
                + type.getName() + "(id:" + type.getId() + ") for this event: " + event.getName() + "\"");
        se.createTypeScriptMapping(event, si.getId(), type.getId(), true, true);
        return si;
    }

    /**
     * This test verifies the following methods: #createScriptFromDropLibrary, #executeDropRunOnceScripts, #executeDropStartupScripts
     *
     * @throws Exception on errors
     */
    @Test
    public void dropScriptTest() throws Exception {
        if( ContainerBootstrap.runInEAR() )
            return; //drops can not be tested within an ear
        Parameter<Boolean> dropParameter = ParameterFactory.newInstance(Boolean.class, "/scripts/runonce/", ParameterScope.DIVISION, "booleanParam", false);
        FxScriptInfo si;
        String dropName = "flexiveDropTest";
        String runOnceScript = "TestDropRunOnceScript.gy";
        String libCode = "println \"Script library: This is a test script called from the [fleXive] test suite\"";

        try {
            testScriptExecution = 0; // need to reset, since script was called during startup of test suite
            se.executeDropStartupScripts(dropName);
            assertEquals(testScriptExecution, 1);
        } catch (FxInvalidParameterException e) {
            fail("Executing the drop startup scripts from " + dropName + " failed");
        }

        try {
            se.executeDropRunOnceScripts(dropParameter, dropName);
            final List<FxScriptRunInfo> divisionRunOnceInfos = EJBLookup.getDivisionConfigurationEngine().get(SystemParameters.DIVISION_RUNONCE_INFOS);
            int runCount = 0;
            for(FxScriptRunInfo info : divisionRunOnceInfos) {
                if(info.getName().endsWith(runOnceScript))
                    runCount++;
            }
            assertTrue(runCount > 0);
        } catch (FxInvalidParameterException e) {
            fail("Executing the drop runonce scripts from " + dropName + " failed");
        }

        try {
            si = se.createScriptFromDropLibrary("flexiveDropTest", FxScriptEvent.Manual, "TestDropLibScript.gy", "UniqueScriptName1010.gy", "description");
            assertEquals(si.getCode(), libCode);
            se.remove(si.getId()); // clean up
        } catch (FxNotFoundException e) {
            fail("Creating a script from the drop " + dropName + " failed");
        }
    }
}