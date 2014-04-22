package com.flexive.tests.embedded.persistence;

import com.flexive.core.flatstorage.FxFlatStorageInfo;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxDelta;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.DivisionConfigurationEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.security.ACLCategory;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxValue;
import com.flexive.tests.embedded.TestUsers;
import org.apache.commons.lang.RandomStringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.flexive.core.flatstorage.FxFlatStorage.FxFlatColumnType;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;

/**
 * Tests for the Flat Storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "flatstorage"}, sequential = true)
public class FlatStorageTest {
    private boolean testsEnabled = false;

    private ContentEngine co;
    private TypeEngine type;
    private AssignmentEngine ass;
    private DivisionConfigurationEngine dce;

    final static String TEST_STORAGE = "FX_CONTENT_FLAT_TEST";
    final static String TEST_STORAGE_DESCRIPTION = "Test storage";
    final static int VALUEDATA_TEST = 815;

    public static final String TEST_TYPE = "TEST_TYPE_" + RandomStringUtils.random(16, true, true);

    private boolean autoFS_State = false;

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void beforeFlat() throws Exception {
        co = EJBLookup.getContentEngine();
        type = EJBLookup.getTypeEngine();
        ass = EJBLookup.getAssignmentEngine();
        dce = EJBLookup.getDivisionConfigurationEngine();
        login(TestUsers.SUPERVISOR);
        testsEnabled = dce.isFlatStorageEnabled();
        if (testsEnabled) {
            autoFS_State = EJBLookup.getConfigurationEngine().get(SystemParameters.FLATSTORAGE_AUTO);
            EJBLookup.getConfigurationEngine().put(SystemParameters.FLATSTORAGE_AUTO, false);
            setupStorage();
            setupStructures();
        }
    }

    @AfterClass
    public void afterFlat() throws FxLogoutFailedException, FxApplicationException {
        if (testsEnabled) {
            long typeId = CacheAdmin.getEnvironment().getType(TEST_TYPE).getId();
            co.removeForType(typeId);
            type.remove(typeId);
            dce.removeFlatStorage(TEST_STORAGE);
            EJBLookup.getConfigurationEngine().put(SystemParameters.FLATSTORAGE_AUTO, autoFS_State);
        }
        logout();
    }

    /**
     * Setup testing structure.
     * <p/>
     * Hierarchy looks like this:
     * * TestProperty1 (String 1024)[0..1] * {F}
     * * TestProperty2 (Text) [0..1] *       {F}
     * * TestProperty3 (String 1024) [0..5]
     * * TestProperty4 (String 1024) [0..N]
     * * TestProperty5 (Double) [0..1] *     {F}
     * * TestProperty6 (Number) [0..1] *     {F}
     * * TestProperty7 (Text) [1..1] *       {F}
     * * TestGroup1[0..2]
     * * * TestProperty1_1 (String 1024) [0..1]
     * * TestGroup2[0..1]
     * * * TestProperty2_1 (Boolean) [0..1] *   {F}
     * * * TestProperty2_2 (String 1024) [0..2]
     *
     * @throws Exception on errors
     */
    private void setupStructures() throws Exception {
        try {
            if (CacheAdmin.getEnvironment().getType(TEST_TYPE) != null)
                return;
        } catch (FxRuntimeException e) {
            //ignore and create
        }
        long typeId = type.save(FxTypeEdit.createNew(TEST_TYPE, new FxString("Test data"), CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()), null));
        //check if loadable
        CacheAdmin.getEnvironment().getType(TEST_TYPE);
        FxString hint = new FxString(true, "hint");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty1", new FxString(true, "TestProperty1"), hint,
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024).setMultiLang(true), "/");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty2", new FxString(true, "TestProperty2"), hint,
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.Text), "/");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty3", new FxString(true, "TestProperty3"), hint,
                FxMultiplicity.of(0, 5), CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024), "/");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty4", new FxString(true, "TestProperty4"), hint,
                FxMultiplicity.MULT_0_N, CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024), "/");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty5", new FxString(true, "TestProperty5"), hint,
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.Float), "/");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty6", new FxString(true, "TestProperty6"), hint,
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.Number), "/");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty7", new FxString(true, "TestProperty7"), hint,
                FxMultiplicity.MULT_1_1, CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.Text), "/");
        ass.createGroup(typeId, FxGroupEdit.createNew("TestGroup1", new FxString(true, "TestGroup1"), hint, false,
                FxMultiplicity.of(0, 2)), "/");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty1_1", new FxString(true, "TestProperty1_1"), hint,
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024), "/TestGroup1");
        ass.createGroup(typeId, FxGroupEdit.createNew("TestGroup2", new FxString(true, "TestGroup2"), hint, false,
                FxMultiplicity.MULT_0_1), "/");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty2_1", new FxString(true, "TestProperty2_1"), hint,
                FxMultiplicity.MULT_0_1, CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.Boolean), "/TestGroup2");
        ass.createProperty(typeId, FxPropertyEdit.createNew("TestProperty2_2", new FxString(true, "TestProperty2_2"), hint,
                FxMultiplicity.of(0, 2), CacheAdmin.getEnvironment().getACL(ACLCategory.STRUCTURE.getDefaultId()),
                FxDataType.String1024), "/TestGroup2");

    }

    private void setupStorage() throws FxApplicationException {
        if (!testsEnabled) return;
        dce.createFlatStorage(TEST_STORAGE, TEST_STORAGE_DESCRIPTION, 3, 3, 3, 3, 3);
    }

    @Test
    public void storageCreateRemove() throws Exception {
        if (!testsEnabled) return;
        dce.createFlatStorage(TEST_STORAGE + "_CR", TEST_STORAGE_DESCRIPTION, 10, 11, 12, 13, 14);
        boolean found = false;
        for (FxFlatStorageInfo info : dce.getFlatStorageInfos()) {
            if (info.getName().equals(TEST_STORAGE + "_CR")) {
                found = true;
                Assert.assertEquals(info.getColumnsString(), 10);
                Assert.assertEquals(info.getColumnsText(), 11);
                Assert.assertEquals(info.getColumnsBigInt(), 12);
                Assert.assertEquals(info.getColumnsDouble(), 13);
                Assert.assertEquals(info.getColumnsSelect(), 14);
            }
        }
        Assert.assertTrue(found, "Test storage not found!");
        dce.removeFlatStorage(TEST_STORAGE + "_CR");
        found = false;
        for (FxFlatStorageInfo info : dce.getFlatStorageInfos()) {
            if (info.getName().equals(TEST_STORAGE + "_CR"))
                found = true;
        }
        Assert.assertFalse(found, "Test storage should no longer exist!");
    }

    @Test
    public void flatAnalyze() throws Exception {
        if (!testsEnabled) return;

        Map<String, List<FxPropertyAssignment>> pot = ass.getPotentialFlatAssignments(CacheAdmin.getEnvironment().getType(TEST_TYPE));
        Assert.assertEquals(pot.size(), 5, "Expected 5 flat mappings");
        Assert.assertEquals(pot.get(FxFlatColumnType.STRING.name()).size(), 2);
        Assert.assertEquals(pot.get(FxFlatColumnType.TEXT.name()).size(), 2);
        Assert.assertEquals(pot.get(FxFlatColumnType.BIGINT.name()).size(), 2);
        Assert.assertEquals(pot.get(FxFlatColumnType.DOUBLE.name()).size(), 1);
        Assert.assertEquals(pot.get(FxFlatColumnType.SELECT.name()).size(), 0);
        //2 should be boosted since its required
        Assert.assertEquals(pot.get(FxFlatColumnType.TEXT.name()).get(0), CacheAdmin.getEnvironment().getAssignment(TEST_TYPE + "/TESTPROPERTY7"));
        Assert.assertEquals(pot.get(FxFlatColumnType.TEXT.name()).get(1), CacheAdmin.getEnvironment().getAssignment(TEST_TYPE + "/TESTPROPERTY2"));
        Assert.assertEquals(pot.get(FxFlatColumnType.STRING.name()).get(0), CacheAdmin.getEnvironment().getAssignment(TEST_TYPE + "/TESTPROPERTY1"));
        Assert.assertEquals(pot.get(FxFlatColumnType.DOUBLE.name()).get(0), CacheAdmin.getEnvironment().getAssignment(TEST_TYPE + "/TESTPROPERTY5"));
        Assert.assertEquals(pot.get(FxFlatColumnType.BIGINT.name()).get(0), CacheAdmin.getEnvironment().getAssignment(TEST_TYPE + "/TESTPROPERTY6"));
        Assert.assertEquals(pot.get(FxFlatColumnType.BIGINT.name()).get(1), CacheAdmin.getEnvironment().getAssignment(TEST_TYPE + "/TESTGROUP2/TESTPROPERTY2_1"));
    }

    @Test(dependsOnMethods = {"flatAnalyze"})
    public void flattenAssignments() throws Exception {
        if (!testsEnabled) return;
        //create a test instance which will be migrated by the flat storage
        final FxContent test = co.initialize(TEST_TYPE);
        test.randomize();
        co.save(test);

        Map<String, List<FxPropertyAssignment>> pot = ass.getPotentialFlatAssignments(CacheAdmin.getEnvironment().getType(TEST_TYPE));
        for (String col : pot.keySet()) {
            for (FxPropertyAssignment pa : pot.get(col)) {
                Assert.assertFalse(pa.isFlatStorageEntry(), "Assignment " + pa.getXPath() + " is not expected to be a flat storage entry!");
                ass.flattenAssignment(TEST_STORAGE, pa);
                Assert.assertTrue(CacheAdmin.getEnvironment().getPropertyAssignment(pa.getXPath()).isFlatStorageEntry(),
                        "Assignment " + pa.getXPath() + " is expected to be a flat storage entry!");
            }
        }
    }

    @Test(dependsOnMethods = {"flattenAssignments"})
    public void flatCreateContent() throws Exception {
        if (!testsEnabled) return;
        //create a test instance which will be migrated by the flat storage
        final FxContent test = co.initialize(TEST_TYPE);
        test.randomize();
        test.getValue("/TestProperty7").setValueData(VALUEDATA_TEST);
        checkValueData(test);
        FxPK pk = co.save(test);
        FxContent loaded = co.load(pk);
        checkValueData(loaded);
        FxDelta delta = FxDelta.processDelta(test, loaded);
        System.out.println("delta: " + delta.dump());
        Assert.assertTrue(delta.isOnlyInternalPropertyChanges(), "Only system internal properties are expected to be changed!");

        loaded.remove("/TestProperty1");
        loaded.setValue("/TestProperty2", new FxString(false, "TEST123"));
        co.save(loaded);
        FxContent loaded2 = co.load(pk);
        checkValueData(loaded2);
        Assert.assertFalse(loaded2.containsValue("/TestProperty1"), "/TestProperty1 should have been removed!");
        Assert.assertEquals(loaded2.getValue("/TestProperty2").getDefaultTranslation(), "TEST123", "/TestProperty should have been updated!");

        ass.unflattenAssignment(CacheAdmin.getEnvironment().getPropertyAssignment(TEST_TYPE + "/TestProperty1"));
        ass.unflattenAssignment(CacheAdmin.getEnvironment().getPropertyAssignment(TEST_TYPE + "/TestProperty2"));
        ass.unflattenAssignment(CacheAdmin.getEnvironment().getPropertyAssignment(TEST_TYPE + "/TestProperty5"));
        ass.unflattenAssignment(CacheAdmin.getEnvironment().getPropertyAssignment(TEST_TYPE + "/TestProperty6"));
        ass.unflattenAssignment(CacheAdmin.getEnvironment().getPropertyAssignment(TEST_TYPE + "/TestProperty7"));
        ass.unflattenAssignment(CacheAdmin.getEnvironment().getPropertyAssignment(TEST_TYPE + "/TESTGROUP2/TESTPROPERTY2_1"));
        CacheAdmin.expireCachedContents(); //force a cache expiry
        /* //optional test code for maintenance:
        Connection con = null;
        try {
            con = Database.getDbConnection();
            FxFlatStorageManager.getInstance().maintenance(con);
        } finally {
            con.close();
        }*/

        FxContent loaded3 = co.load(pk);
        checkValueData(loaded3);
        delta = FxDelta.processDelta(loaded2, loaded3);
        System.out.println("unflatten-delta: " + delta.dump());
        Assert.assertFalse(delta.changes(), "No changes expected after unflatten!");
        co.remove(pk);
    }

    /**
     * Check if value data is still the same
     *
     * @param test content to test
     */
    private void checkValueData(FxContent test) {
        final FxValue value = test.getValue("/TestProperty7");
        Assert.assertNotNull(value);
        Assert.assertTrue(value.hasValueData());
        Assert.assertEquals((int) value.getValueData(), VALUEDATA_TEST);
    }

}
