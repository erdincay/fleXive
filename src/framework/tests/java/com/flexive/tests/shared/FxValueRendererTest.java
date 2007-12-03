/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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
package com.flexive.tests.shared;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.FxDate;
import com.flexive.shared.value.FxDouble;
import com.flexive.shared.value.FxFloat;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.renderer.FxValueRenderer;
import com.flexive.shared.value.renderer.FxValueRendererFactory;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Date;

/**
 * FxValueRenderer tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = "shared")
public class FxValueRendererTest {
    private static final FxValueRenderer FORMAT_EN = FxValueRendererFactory.getInstance(new FxLanguage("en"));
    private static final FxValueRenderer FORMAT_DE = FxValueRendererFactory.getInstance(new FxLanguage("de"));

    @Test
    public void dateFormatTest() {
        FxDate date = new FxDate(new Date(0));
        String englishDate = FORMAT_EN.format(date);
        String germanDate = FORMAT_DE.format(date);
        assert !englishDate.equals(germanDate);
    }

    @Test
    public void floatFormatTest() {
        FxFloat value = new FxFloat(1234.56f);
        assert "1,234.56".equals(FORMAT_EN.format(value));
        assert "1.234,56".equals(FORMAT_DE.format(value));
    }

    @Test
    public void doubleFormatTest() {
        FxDouble value = new FxDouble(1234.56);
        assert "1,234.56".equals(FORMAT_EN.format(value));
        assert "1.234,56".equals(FORMAT_DE.format(value));
    }

    @Test
    public void stringFormatTest() {
        FxString value = new FxString(true, "default");
        value.setTranslation(FxLanguage.ENGLISH, "english");
        value.setTranslation(FxLanguage.GERMAN, "deutsch");
        assert "english".equals(FORMAT_EN.format(value)) : "Expected english translation, got: " + FORMAT_EN.format(value);
        assert "deutsch".equals(FORMAT_DE.format(value)) : "Expected german translation, got: " + FORMAT_DE.format(value);
    }

    @Test
    public void renderFormatTest() throws IOException {
        Writer de = new StringWriter();
        Writer en = new StringWriter();
        FxDouble value = new FxDouble(1234.56);
        FORMAT_EN.render(en, value);
        FORMAT_DE.render(de, value);
        assert "1,234.56".equals(en.toString()) : "Expected 1,234.56, got: " + en;
        assert "1.234,56".equals(de.toString()) : "Expected 1.234,56, got: " + de;
    }

    @Test
    public void renderEmptyTest() {
        final FxString empty = new FxString(false, "");
        assert FORMAT_EN.format(empty).equals("") : "Empty string translation should be empty";
        assert FORMAT_DE.format(empty).equals("") : "Empty string translation should be empty";
        assert empty.toString().equals("") : "Empty string translation should be empty";
    }
}
