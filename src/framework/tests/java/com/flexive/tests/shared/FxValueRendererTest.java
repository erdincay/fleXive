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
package com.flexive.tests.shared;

import com.flexive.shared.FxContext;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.*;
import com.flexive.shared.value.renderer.FxValueRenderer;
import com.flexive.shared.value.renderer.FxValueRendererFactory;
import org.testng.annotations.Test;
import org.testng.Assert;

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
        switchEN();
        String englishDate = FORMAT_EN.format(date);
        switchDE();
        String germanDate = FORMAT_DE.format(date);
        Assert.assertTrue(!englishDate.equals(germanDate));
    }

    private void switchEN() {
        FxContext.get().setDateFormatOverride("MM/dd/yyyy");
        FxContext.get().setDecimalSeparatorOverride('.');
        FxContext.get().setGroupingSeparatorOverride(',');
        FxContext.get().setUseGroupingSeparatorOverride(true);
    }

    private void switchDE() {
        FxContext.get().setDateFormatOverride("dd.MM.yyyy");
        FxContext.get().setDecimalSeparatorOverride(',');
        FxContext.get().setGroupingSeparatorOverride('.');
        FxContext.get().setUseGroupingSeparatorOverride(true);
    }

    @Test
    public void floatFormatTest() {
        FxFloat value = new FxFloat(1234.56f);
        switchEN();
        Assert.assertTrue("1,234.56".equals(FORMAT_EN.format(value)));
        switchDE();
        Assert.assertTrue("1.234,56".equals(FORMAT_DE.format(value)));
    }

    @Test
    public void doubleFormatTest() {
        FxDouble value = new FxDouble(1234.56);
        switchEN();
        Assert.assertTrue("1,234.56".equals(FORMAT_EN.format(value)));
        switchDE();
        Assert.assertTrue("1.234,56".equals(FORMAT_DE.format(value)));
    }

    @Test
    public void stringFormatTest() {
        FxString value = new FxString(true, "default");
        value.setTranslation(FxLanguage.ENGLISH, "english");
        value.setTranslation(FxLanguage.GERMAN, "deutsch");
        switchEN();
        Assert.assertTrue("english".equals(FORMAT_EN.format(value)), "Expected english translation, got: " + FORMAT_EN.format(value));
        switchDE();
        Assert.assertTrue("deutsch".equals(FORMAT_DE.format(value)), "Expected german translation, got: " + FORMAT_DE.format(value));
    }

    @Test
    public void renderFormatTest() throws IOException {
        Writer de = new StringWriter();
        Writer en = new StringWriter();
        FxDouble value = new FxDouble(1234.56);
        switchEN();
        FORMAT_EN.render(en, value);
        switchDE();
        FORMAT_DE.render(de, value);
        Assert.assertTrue("1,234.56".equals(en.toString()), "Expected 1,234.56, got: " + en);
        Assert.assertTrue("1.234,56".equals(de.toString()), "Expected 1.234,56, got: " + de);
    }

    @Test
    public void renderEmptyTest() {
        final FxString empty = new FxString(false, "");
        Assert.assertTrue(FORMAT_EN.format(empty).equals(""), "Empty string translation should be empty");
        Assert.assertTrue(FORMAT_DE.format(empty).equals(""), "Empty string translation should be empty");
        Assert.assertTrue(empty.toString().equals(""), "Empty string translation should be empty");
    }

    @Test
    @SuppressWarnings({"unchecked"})
    public void invalidInputTest() {
        // form inputs must be returned in their string representation
        final String input = "invalid value";
        for (FxValue fxValue: new FxValue[] {
                ((FxValue) new FxNumber(false, 21)).setDefaultTranslation(input),
                ((FxValue) new FxFloat(false, 21f)).setDefaultTranslation(input),
                ((FxValue) new FxDouble(false, 21.)).setDefaultTranslation(input),
                ((FxValue) new FxDate(false, new Date())).setDefaultTranslation(input),
                ((FxValue) new FxDateRange(false, new DateRange(new Date(), new Date()))).setDefaultTranslation(input),
                ((FxValue) new FxDateTime(false, new Date())).setDefaultTranslation(input),
                ((FxValue) new FxDateTimeRange(false, new DateRange(new Date(), new Date()))).setDefaultTranslation(input),
        }) {
            try {
                final String output = FxValueRendererFactory.getInstance().format(fxValue);
                Assert.assertEquals(output, input, "Expected output '" + input + "', got: '" + output + "'"
                        + ", class: " + fxValue.getClass().getName());
            } catch (Exception e) {
                Assert.fail("Failed to format invalid input value in "
                        + fxValue.getClass().getName() + ": " + e.getMessage());
            }
        }

    }
}
