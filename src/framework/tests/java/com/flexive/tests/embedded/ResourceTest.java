package com.flexive.tests.embedded;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.DivisionConfigurationEngine;
import com.flexive.shared.value.FxString;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Map;
import java.util.Set;

import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;

/**
 * Test for resource handling
 *
 * @author Markus Plesser (markus.plesser@ucs.at), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = { "ejb", "resource" })
public class ResourceTest {

    @BeforeClass
    public void beforeClass() throws Exception {
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        logout();
    }

    @Test
    public void resourceTest() throws FxApplicationException {
        final String key1 = "res.Test.AbC-123";
        final String key1same = "res.test.abc-123";
        final String SINGLE_TEST = "Single lang test";

        DivisionConfigurationEngine dc = EJBLookup.getDivisionConfigurationEngine();

        //single language
        Assert.assertNull(dc.getResourceValue(key1, FxLanguage.SYSTEM_ID));
        FxString test = new FxString(false, SINGLE_TEST);
        dc.setResourceValue(key1, test);
        FxString comp = dc.getResourceValue(key1same, FxLanguage.SYSTEM_ID);
        Assert.assertEquals(test, comp);
        comp = dc.getResourceValue(key1same, FxLanguage.ENGLISH);
        Assert.assertEquals(test, comp);
        comp.setEmpty();
        dc.setResourceValue(key1, comp);
        Assert.assertNull(dc.getResourceValue(key1, FxLanguage.SYSTEM_ID));
        dc.setResourceValue(key1, test);
        Assert.assertNotNull(dc.getResourceValue(key1, FxLanguage.SYSTEM_ID));
        dc.setResourceValue(key1, null);
        Assert.assertNull(dc.getResourceValue(key1, FxLanguage.SYSTEM_ID));

        //multi language
        test = new FxString(true, FxLanguage.ENGLISH, "en");
        dc.setResourceValue(key1, test);
        comp = dc.getResourceValue(key1same, FxLanguage.SYSTEM_ID);
        Assert.assertTrue(comp.isMultiLanguage());
        Assert.assertNotNull(comp);
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.ENGLISH);
        comp = dc.getResourceValue(key1, FxLanguage.ENGLISH);
        Assert.assertNotNull(comp);
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.ENGLISH);
        test.setTranslation(FxLanguage.GERMAN, "de");
        dc.setResourceValue(key1same, test);
        comp = dc.getResourceValue(key1, FxLanguage.ENGLISH);
        Assert.assertTrue(comp.translationExists(FxLanguage.ENGLISH));
        Assert.assertTrue(comp.translationExists(FxLanguage.GERMAN));
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.ENGLISH);
        Assert.assertEquals(comp.getTranslation(FxLanguage.GERMAN), "de");
        Assert.assertEquals(comp.getTranslation(FxLanguage.ENGLISH), "en");
        comp = dc.getResourceValue(key1, FxLanguage.GERMAN);
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.GERMAN);
        Assert.assertEquals(comp.getTranslation(FxLanguage.GERMAN), "de");
        Assert.assertEquals(comp.getTranslation(FxLanguage.ENGLISH), "en");
        comp.setTranslation(FxLanguage.GERMAN, "de1");
        dc.setResourceValue(key1, comp);
        comp = dc.getResourceValue(key1, FxLanguage.ENGLISH);
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.ENGLISH);
        Assert.assertEquals(comp.getTranslation(FxLanguage.GERMAN), "de1");
        Assert.assertEquals(comp.getTranslation(FxLanguage.ENGLISH), "en");
        dc.setResourceValue(key1, null);
        Assert.assertNull(dc.getResourceValue(key1, FxLanguage.ENGLISH));
    }

    @Test
    public void multipleResourcesTest() throws FxApplicationException {
        DivisionConfigurationEngine dc = EJBLookup.getDivisionConfigurationEngine();
        FxString valMult1 = new FxString(true, FxLanguage.ENGLISH, "en1").setTranslation(FxLanguage.GERMAN, "de1");
        FxString valMult2 = new FxString(true, FxLanguage.ENGLISH, "en2").setTranslation(FxLanguage.GERMAN, "de2");
        FxString valSingle1 = new FxString(false, "single1");
        FxString valSingle2 = new FxString(false, "single2");

        dc.removeResourceValues("test.mr.");
        Assert.assertEquals(dc.getResourceValues("test.mr.", FxLanguage.ENGLISH).size(), 0);
        dc.setResourceValue("test.mr.s2", valSingle2);
        dc.setResourceValue("test.mr.m2", valMult2);
        dc.setResourceValue("test.mr.s1", valSingle1);
        dc.setResourceValue("test.mr.m1", valMult1);
        Assert.assertEquals(dc.getResourceValues("test.mr.", FxLanguage.ENGLISH).size(), 4);
        Assert.assertEquals(dc.getResourceValues("test.mr.m", FxLanguage.ENGLISH).size(), 2);
        Assert.assertEquals(dc.getResourceValues("test.mr.s", FxLanguage.ENGLISH).size(), 2);
        Map<String, FxString> test = dc.getResourceValues("test.mr.", FxLanguage.ENGLISH);
        Assert.assertTrue(test.containsKey("test.mr.m1"));
        Assert.assertTrue(test.containsKey("test.mr.m2"));
        Assert.assertTrue(test.containsKey("test.mr.s1"));
        Assert.assertTrue(test.containsKey("test.mr.s2"));
        Set<String> testSet = test.keySet();
        //check order
        String[] keys = testSet.toArray(new String[testSet.size()]);
        Assert.assertEquals(keys[0], "test.mr.m1");
        Assert.assertEquals(keys[1], "test.mr.m2");
        Assert.assertEquals(keys[2], "test.mr.s1");
        Assert.assertEquals(keys[3], "test.mr.s2");
        dc.removeResourceValues("test.mr.s");
        test = dc.getResourceValues("test.mr.", FxLanguage.ENGLISH);
        Assert.assertEquals(test.size(), 2);
        Assert.assertTrue(test.containsKey("test.mr.m1"));
        Assert.assertTrue(test.containsKey("test.mr.m2"));
        Assert.assertFalse(test.containsKey("test.mr.s1"));
        Assert.assertFalse(test.containsKey("test.mr.s2"));
        dc.removeResourceValues("test.mr.");
        test = dc.getResourceValues("test.mr.", FxLanguage.ENGLISH);
        Assert.assertEquals(test.size(), 0);
    }
}
