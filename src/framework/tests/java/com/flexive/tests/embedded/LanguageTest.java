/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/copyleft/gpl.html.
 *  A copy is found in the textfile GPL.txt and important notices to the
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
import com.flexive.shared.FxLanguage;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.LanguageEngine;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.List;

/**
 * Tests for the language beans.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = "ejb")
public class LanguageTest {

    /**
     * Asserts that all language labels are different
     * 
     * @throws com.flexive.shared.exceptions.FxApplicationException on errors
     */
    @Test
    public void testLanguageLabels() throws FxApplicationException {
        LanguageEngine languageBean = EJBLookup.getLanguageEngine();
        List<FxLanguage> languages = languageBean.loadAvailable();
        Set<String> defaultTranslations = new HashSet<String>();
        for (FxLanguage language : languages) {
            String name = language.getLabel().getDefaultTranslation();
            assert !defaultTranslations.contains(name) : "Language name '" + name + "' occurs at least twice.";
            defaultTranslations.add(name);
        }
    }
}
