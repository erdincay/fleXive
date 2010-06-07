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

        DivisionConfigurationEngine divisionConfiguration = EJBLookup.getDivisionConfigurationEngine();

        //single language
        Assert.assertNull(divisionConfiguration.getResourceValue(key1, FxLanguage.SYSTEM_ID));
        FxString test = new FxString(false, SINGLE_TEST);
        divisionConfiguration.setResourceValue(key1, test);
        FxString comp = divisionConfiguration.getResourceValue(key1same, FxLanguage.SYSTEM_ID);
        Assert.assertEquals(test, comp);
        comp = divisionConfiguration.getResourceValue(key1same, FxLanguage.ENGLISH);
        Assert.assertEquals(test, comp);
        comp.setEmpty();
        divisionConfiguration.setResourceValue(key1, comp);
        Assert.assertNull(divisionConfiguration.getResourceValue(key1, FxLanguage.SYSTEM_ID));
        divisionConfiguration.setResourceValue(key1, test);
        Assert.assertNotNull(divisionConfiguration.getResourceValue(key1, FxLanguage.SYSTEM_ID));
        divisionConfiguration.setResourceValue(key1, null);
        Assert.assertNull(divisionConfiguration.getResourceValue(key1, FxLanguage.SYSTEM_ID));

        //multi language
        test = new FxString(true, FxLanguage.ENGLISH, "en");
        divisionConfiguration.setResourceValue(key1, test);
        comp = divisionConfiguration.getResourceValue(key1same, FxLanguage.SYSTEM_ID);
        Assert.assertTrue(comp.isMultiLanguage());
        Assert.assertNotNull(comp);
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.ENGLISH);
        comp = divisionConfiguration.getResourceValue(key1, FxLanguage.ENGLISH);
        Assert.assertNotNull(comp);
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.ENGLISH);
        test.setTranslation(FxLanguage.GERMAN, "de");
        divisionConfiguration.setResourceValue(key1same, test);
        comp = divisionConfiguration.getResourceValue(key1, FxLanguage.ENGLISH);
        Assert.assertTrue(comp.translationExists(FxLanguage.ENGLISH));
        Assert.assertTrue(comp.translationExists(FxLanguage.GERMAN));
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.ENGLISH);
        Assert.assertEquals(comp.getTranslation(FxLanguage.GERMAN), "de");
        Assert.assertEquals(comp.getTranslation(FxLanguage.ENGLISH), "en");
        comp = divisionConfiguration.getResourceValue(key1, FxLanguage.GERMAN);
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.GERMAN);
        Assert.assertEquals(comp.getTranslation(FxLanguage.GERMAN), "de");
        Assert.assertEquals(comp.getTranslation(FxLanguage.ENGLISH), "en");
        comp.setTranslation(FxLanguage.GERMAN, "de1");
        divisionConfiguration.setResourceValue(key1, comp);
        comp = divisionConfiguration.getResourceValue(key1, FxLanguage.ENGLISH);
        Assert.assertTrue(comp.getDefaultLanguage() == FxLanguage.ENGLISH);
        Assert.assertEquals(comp.getTranslation(FxLanguage.GERMAN), "de1");
        Assert.assertEquals(comp.getTranslation(FxLanguage.ENGLISH), "en");
        divisionConfiguration.setResourceValue(key1, null);
        Assert.assertNull(divisionConfiguration.getResourceValue(key1, FxLanguage.ENGLISH));
    }
}
