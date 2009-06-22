package com.flexive.tests.embedded.persistence;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.Assert;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.*;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import com.flexive.core.flatstorage.FxFlatStorageInfo;

/**
 * Tests for the Flat Storage
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"ejb", "content", "flatstorage"})
public class FlatStorageTest {
    private boolean testsEnabled = false;

    private ContentEngine co;
    private ACLEngine acl;
    private TypeEngine type;
    private AssignmentEngine ass;
    private DivisionConfigurationEngine dce;

    final static String TEST_STORAGE = "FX_CONTENT_FLAT_TEST";
    final static String TEST_STORAGE_DESCRIPTION = "Test storage";

    /**
     * setup...
     *
     * @throws Exception on errors
     */
    @BeforeClass
    public void beforeClass() throws Exception {
        co = EJBLookup.getContentEngine();
        acl = EJBLookup.getAclEngine();
        type = EJBLookup.getTypeEngine();
        ass = EJBLookup.getAssignmentEngine();
        dce = EJBLookup.getDivisionConfigurationEngine();
        login(TestUsers.SUPERVISOR);
        testsEnabled = dce.isFlatStorageEnabled();
        setupStorage();
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        dce.removeFlatStorage(TEST_STORAGE);
        logout();
    }

    private void setupStorage() throws FxApplicationException {
        if(!testsEnabled) return;
        dce.createFlatStorage(TEST_STORAGE, TEST_STORAGE_DESCRIPTION, 20, 20, 10, 10, 10);
    }

    @Test
    public void storageCreateRemove() throws Exception {
        if(!testsEnabled) return;
        dce.createFlatStorage(TEST_STORAGE+"_CR", TEST_STORAGE_DESCRIPTION, 10, 20, 30, 40, 50);
        boolean found = false;
        for(FxFlatStorageInfo info: dce.getFlatStorageInfos()) {
            if(info.getName().equals(TEST_STORAGE+"_CR")) {
                found = true;
                Assert.assertEquals(info.getColumnsString(), 10);
                Assert.assertEquals(info.getColumnsText(), 20);
                Assert.assertEquals(info.getColumnsBigInt(), 30);
                Assert.assertEquals(info.getColumnsDouble(), 40);
                Assert.assertEquals(info.getColumnsSelect(), 50);
            }
        }
        Assert.assertTrue(found, "Test storage not found!");
        dce.removeFlatStorage(TEST_STORAGE+"_CR");
        found = false;
        for(FxFlatStorageInfo info: dce.getFlatStorageInfos()) {
            if(info.getName().equals(TEST_STORAGE+"_CR"))
                found = true;
        }
        Assert.assertFalse(found, "Test storage should no longer exist!");
    }

}
