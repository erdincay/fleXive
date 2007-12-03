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

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import org.testng.annotations.Test;

/**
 * Unit tests for com.flexive.shared.FxFormatUtils
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see com.flexive.shared.FxFormatUtils
 */
@Test(groups = {"shared"})
public class FormatUtilTest {
    /**
     * Tests for com.flexive.shared.FxFormatUtils.isRGBCode.
     */
    @Test
    public void isRGBCode() {
        checkRgb("#000000", true);
        checkRgb("#123456", true);
        checkRgb("#FFFFFF", true);
        checkRgb("#ABCDEFF", false);
        checkRgb("#ABCDEG", false);
        checkRgb("##ABCDE", false);
        checkRgb("123456", true);
        checkRgb("99999A", true);
        checkRgb(null, false);
        checkRgb("", false);
    }

    /**
     * Check the given RGB code
     *
     * @param code           the RGB code to check
     * @param expectedResult if the code is expected to be valid
     */
    private void checkRgb(String code, boolean expectedResult) {
        assert expectedResult == FxFormatUtils.isRGBCode(code) : "Failed to check RGB code " + code;
    }

    /**
     * Tests for com.flexive.shared.FxFormatUtils.colorCodeToStyle
     */
    @Test
    public void colorCodeToStyle() {
        assert "style=\"color:#F0F0F0\"".equals(FxFormatUtils.colorCodeToStyle("#F0F0F0").trim()) : "Failed to convert color code";
        assert "style=\"color:#000000\"".equals(FxFormatUtils.colorCodeToStyle("000000").trim()) : "Failed to convert color code";
        assert "class=\"test123\"".equals(FxFormatUtils.colorCodeToStyle("test123").trim()) : "Failed to convert color code";
    }

    /**
     * Tests for com.flexive.shared.FxFormatUtils.isEmail
     */
    @Test
    public void isEmail() {
        checkEmail("abc@abc.com", true);
        checkEmail("a@b", false);
        checkEmail("a@b.com", true);
        checkEmail("  a@b.com  ", true);
        checkEmail("test.point@match.point.com", true);
        checkEmail("test@@test.com", false);
    }

    /**
     * Check the given email address.
     *
     * @param eMail the email address to be checked
     * @param valid if the address is expected to be valid
     */
    private void checkEmail(String eMail, boolean valid) {
        try {
            assert FxFormatUtils.isEmail(eMail).equals((eMail != null) ? eMail.trim() : "INV") : "Failed to return valid email address: " + eMail;
        } catch (FxInvalidParameterException e) {
            if (valid) {
                assert false : "Failed to recognize valid email address: " + eMail;
            }
        }
    }

    /**
     * Tests for com.flexive.shared.FxFormatUtils.processColorString
     */
    @Test
    public void processColorString() {
        checkColorString("#ABCDEF", true);
        checkColorString("cssClassName", true);
        checkColorString("#0X0X0X", false);
        checkColorString("#AAAAAG", false);
        checkColorString("#123ABC", true);
        checkColorString("anotherCss123", true);
    }

    /**
     * Check the given color string
     *
     * @param value the string to be checked
     * @param valid if the given value is expected to be valid
     */
    private void checkColorString(String value, boolean valid) {
        try {
            assert value.equals(FxFormatUtils.processColorString("TEST", value)) : "Failed to recognize valid color string: " + value;
        } catch (FxInvalidParameterException e) {
            if (valid) {
                assert false : "Failed to recognize valid color string " + value;
            }
        }
    }
}
