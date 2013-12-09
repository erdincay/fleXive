/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2014
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
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.interfaces.LanguageEngine;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import static org.testng.Assert.*;

/**
 * Tests for the language beans.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = "ejb")
public class LanguageTest {

    private LanguageEngine le;

    @BeforeClass
    public void beforeClass() throws Exception {
        le = EJBLookup.getLanguageEngine();
        login(TestUsers.SUPERVISOR);
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException {
        logout();
    }

    /**
     * Asserts that all language labels are different
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on errors
     */
    @Test
    public void testLanguageLabels() throws FxApplicationException {
        LanguageEngine languageBean = EJBLookup.getLanguageEngine();
        List<FxLanguage> languages = languageBean.loadAvailable();
        Set<String> defaultTranslations = new HashSet<String>();
        for (FxLanguage language : languages) {
            String name = language.getLabel().getDefaultTranslation();
            Assert.assertTrue(!defaultTranslations.contains(name), "Language name '" + name + "' occurs at least twice.");
            defaultTranslations.add(name);
        }
    }

    /**
     * Asserts setting and loading available languages, removal of used languages
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on errors
     */
    @Test
    public void testSetAvailable() throws FxApplicationException {
        List<FxLanguage> origLanguages = le.loadAvailable(false);

        List<FxLanguage> testLanguages = new ArrayList<FxLanguage>();

        // reset the db langs
        le.setAvailable(origLanguages, true);
        // test by ignoring the languages in use
        testLanguages.add(new FxLanguage("it"));
        testLanguages.add(new FxLanguage("fr"));
        le.setAvailable(testLanguages, true);
        testLanguages = le.loadAvailable();

        // assert available language and the new default lang set to "it"
        assertEquals(testLanguages.size(), 2);
        assertEquals(testLanguages.get(0).getIso2digit(), "it");

        // check environment
        assertEquals(CacheAdmin.getEnvironment().getLanguages(), testLanguages);

        // reset the db langs
        le.setAvailable(origLanguages, true);
        testLanguages.add(new FxLanguage("fr"));
        testLanguages.add(new FxLanguage("it"));
        // test by retaining the usage of the system default "en"
        try {
            le.setAvailable(testLanguages, false);
            fail("\"setAvailable(testLanguages, false)\" should have failed");
        } catch (FxInvalidParameterException e) {
            assertNotNull(e);
        }

        // now assert that "en" is one of the ~136 disabled languages
        for(FxLanguage l : le.loadDisabled()) {
            assertEquals(false, l.getIso2digit().equals("en"));
        }

        // reset the db langs
        le.setAvailable(origLanguages, true);
        assertEquals(CacheAdmin.getEnvironment().getLanguages(), origLanguages);
    }

    /**
     * Asserts that an ArrayList of FxLanguage is returned and that the elements are FxLanguage Objects
     * For loadAvailable(false) this test asserts that none of the returned testLanguages is the system language.
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException on errors
     */
    @Test
    public void testLoadAvailable() throws FxApplicationException {
        List<FxLanguage> languages = le.loadAvailable(true);
        assertTrue(languages instanceof ArrayList);
        for (FxLanguage l : languages) {
            assertTrue(l != null);
        }
        // assert that none of the returned languages is the systemlanguage
        languages = le.loadAvailable(false);
        for (FxLanguage l : languages) {
            assertTrue(l.getId() != 0);
        }
    }

    /**
     * Asserts that an invalid ID returns "false" for LanguageBean#isValid(id)
     *
     * @throws com.flexive.shared.exceptions.FxApplicationException on errors
     */
    @Test public void testValidException() throws FxApplicationException {
        assertTrue(!le.isValid(5000));
    }
}
