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
package com.flexive.tests.shared;

import com.flexive.shared.FxLanguage;
import com.flexive.shared.value.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.testng.Assert.*;

/**
 * Generic FxValue tests
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = "shared")
public class FxValueTest {
    private static Object[][] testInstances = new Object[][]{
            {new ValueTestBean<String, FxString>(new FxString(false, "Test string"), "Element",
                    new String[]{"String value"}, new String[0])},
            {new ValueTestBean<Integer, FxNumber>(new FxNumber(false, 42), 1234,
                    new String[]{"5678", "0", "-23124"},
                    new String[]{"6789x", "5.6", "-", "-0.1"})},
            {new ValueTestBean<Long, FxLargeNumber>(new FxLargeNumber(false, (long) 42), (long) 1234,
                    new String[]{"5678", "0", "-23124"},
                    new String[]{"6789x", "5.6", "-", "-0.1"})},
            {new ValueTestBean<Double, FxDouble>(new FxDouble(false, 1979.1111), 1234.5678,
                    new String[]{"5678", "0", "-23124", "0.0", "-5.3", "213341.314231", "321,432", "123,456,000", "123,456.000"},
                    new String[]{"6789x", "abc"})},
            {new ValueTestBean<Float, FxFloat>(new FxFloat(false, 1979.1111f), 1234.5678f,
                    new String[]{"5678", "0", "-23124", "0.0", "-5.3", "213341.314231", "321,432", "123,456,000", "123,456.000"},
                    new String[]{"6789x", "abc"})},
            {new ValueTestBean<Boolean, FxBoolean>(new FxBoolean(false, true), false,
                    new String[]{"true", "false"},
                    new String[]{})}
            // TODO add test instances for FxDate, FxDateRange
    };

    @Test(dataProvider = "testInstances")
    public <T, TDerived extends FxValue<T, TDerived>> void testIsValid(ValueTestBean<T, TDerived> testBean) {
        FxValue<T, TDerived> value = testBean.getValue().copy();
        assertTrue(value.isValid(), "Test value should be valid: " + value);
        assertTrue(!value.isMultiLanguage(), "Value must not be multilingual: " + value);

        // test valid string values
        for (String validValue : testBean.getValidStringValues()) {
            //noinspection unchecked
            ((FxValue) value).setValue(validValue);
            assertTrue(value.isValid(), "String value should be valid: " + validValue);
        }

        // test invalid string values
        for (String invalidValue : testBean.getInvalidStringValues()) {
            //noinspection unchecked
            ((FxValue) value).setValue(invalidValue);
            assertTrue(!value.isValid(), "String value should not be valid: " + invalidValue);
        }
        value.setValue(testBean.getValueElement());
        assertTrue(value.isValid(), "Value must be valid: " + testBean.getValueElement());
    }

    @Test(dataProvider = "testInstances")
    public <T, TDerived extends FxValue<T, TDerived>> void testValidContract(ValueTestBean<T, TDerived> testBean) {
        // checks the contract of FxValue#isValid() and FxValue#getErrorValue():
        FxValue<T, TDerived> value = testBean.getValue().copy();
        // if !isValid(), then getErrorValue() must return an object.
        for (String validValue : testBean.getValidStringValues()) {
            //noinspection unchecked
            ((FxValue) value).setValue(validValue);
            assertTrue(value.isValid());
            try {
                value.getErrorValue();
                Assert.fail("Valid values cannot return error values.");
            } catch (IllegalStateException e) {
                // pass
            }
        }
        // check empty values
        value.setEmpty();
        if (value.isValid()) {
            try {
                value.getErrorValue();
                Assert.fail("Valid values cannot return error values.");
            } catch (IllegalStateException e) {
                // pass
            }
        } else {
            assertTrue(value.getErrorValue() != null, "Invalid values must return an error value.");
        }
        // if isValid(), then getErrorValue() must throw an IllegalStateException.
        for (String invalidValue : testBean.getInvalidStringValues()) {
            //noinspection unchecked
            ((FxValue) value).setValue(invalidValue);
            assertTrue(!value.isValid());
            assertTrue(value.getErrorValue() != null, "Invalid value must return an error value.");
        }
    }

    /**
     * Bug check: if a previously empty number property is updated with the same
     * "empty" number, the isEmpty flag is not set correctly.
     */
    @Test
    public void testUpdateEmptyValue() {
        FxNumber number = new FxNumber(0).setEmpty();
        assertTrue(number.isEmpty());
        number.setValue(0);
        assertTrue(!number.isEmpty(), "FxNumber still empty after explicit value update.");
    }

    /**
     * Bug check: FxNoAccess should override equals, otherwise it will throw an NPE
     */
    @Test
    public void testNoAccessEquals() {
        final FxString value = new FxString(false, "test");
        final FxNoAccess noAccess = new FxNoAccess(null, value);
        assertFalse(noAccess.equals(new FxNoAccess(null, value)));
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void multiLanguageValidTest() {
        // create a FxValue with valid and invalid entries
        final FxNumber value = new FxNumber(true, 41).setTranslation(FxLanguage.ENGLISH, 21);
        // force string value into content - this will usually happen for user input from an input component
        ((FxValue) value).setTranslation(FxLanguage.GERMAN, "abc");
        assertFalse(value.isValid());
        assertTrue(value.isValid(FxLanguage.SYSTEM_ID));
        assertTrue(value.isValid(FxLanguage.ENGLISH));
        assertFalse(value.isValid(FxLanguage.GERMAN));
    }
    
    @Test
    public void emptyCheck() {
        final FxString val = new FxString(true, FxLanguage.ENGLISH, "a value");
        val.setEmpty(FxLanguage.GERMAN);
        assertFalse(val.isEmpty());
    }

    @Test
    public void defaultTranslationTest() {
        final FxString val = new FxString(true, FxLanguage.ENGLISH, "en");
        assertEquals(val.getBestTranslation(), "en");
        val.setTranslation(FxLanguage.GERMAN, "de");
        assertEquals(val.getBestTranslation(FxLanguage.GERMAN), "de");
        val.setEmpty(FxLanguage.GERMAN);
        assertEquals(val.getBestTranslation(FxLanguage.GERMAN), "en");
        assertEquals(val.getBestTranslation(), "en");
    }

    @Test
    public void emptyValueEqualsTest() {
        final FxString val = new FxString(true, FxLanguage.ENGLISH, "en");
        final FxString val2 = val.copy();
        assertEquals(val, val2);
        val2.setEmpty(FxLanguage.GERMAN);
        assertEquals(val, val2);
    }

    @Test
    public void valueDataTest() {
        final FxString val = new FxString(false, "");
        assertFalse(val.hasValueData());
        val.setValueData(21);
        assertEquals(val.getValueData(), (Integer) 21);
        val.clearValueData();
        assertFalse(val.hasValueData());
    }

    @Test
    public void valueDataTestML() {
        final FxString val = new FxString(true, FxLanguage.ENGLISH, "en");
        assertFalse(val.hasValueData());
        assertFalse(val.hasValueData(FxLanguage.ENGLISH));

        val.setValueData(FxLanguage.ENGLISH, 1, true);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 1);

        // this seems unintuitive at first, but setting a language-specific value in a content that had no value data
        // before causes this value to be used for all other languages as well
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 1);
        assertTrue(val.hasValueData(FxLanguage.GERMAN));

        val.clearValueData(FxLanguage.ENGLISH);
        assertFalse(val.hasValueData());
        assertFalse(val.hasValueData(FxLanguage.ENGLISH));
    }

    @Test
    public void valueDataTestMLDefault() {
        final FxString val = new FxString(true, FxLanguage.ENGLISH, "en");
        val.setValueData(0);

        val.setValueData(FxLanguage.ENGLISH, 1);

        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 1);

        // fallback value
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 0);

        val.clearValueData(FxLanguage.GERMAN);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 0);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 1);

        val.clearValueData(FxLanguage.ENGLISH);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 0);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 0);

        // clearing a value when no specific value was set
        val.clearValueData(FxLanguage.ENGLISH);
        assertNull(val.getValueDataRaw(FxLanguage.ENGLISH));
        assertNull(val.getValueDataRaw(FxLanguage.GERMAN));

        val.setValueData(FxLanguage.GERMAN, 1, true);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 1);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 1);

        val.setValueData(FxLanguage.ENGLISH, null);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 1);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 1);

        val.setValueData(FxLanguage.ENGLISH, 0);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 1);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 0);
    }

    @Test
    public void valueDataTestMLDefaultCleanup() {
        final FxString val = new FxString(true, FxLanguage.ENGLISH, "en");

        val.setValueData(FxLanguage.ENGLISH, 1, true);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 1);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 1);  // fallback

        val.setValueData(FxLanguage.ENGLISH, 2, true);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 2);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 2);  // fallback

        val.setValueData(FxLanguage.ENGLISH, null);
        assertNull(val.getValueDataRaw(FxLanguage.ENGLISH));
        assertNull(val.getValueDataRaw(FxLanguage.GERMAN));
    }

    @Test
    public void valueDataTestMLNoFallback() {
        final FxString val = new FxString(true, FxLanguage.ENGLISH, "en");

        val.setValueData(FxLanguage.ENGLISH, 1, false);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 1);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), null);  // no fallback

        val.setValueData(FxLanguage.GERMAN, 2, true);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 1);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 2);
        assertEquals(val.getValueDataRaw(FxLanguage.ITALIAN), (Integer) 2);

        val.clearValueData(FxLanguage.ENGLISH);
        assertEquals(val.getValueDataRaw(FxLanguage.ENGLISH), (Integer) 2);
        assertEquals(val.getValueDataRaw(FxLanguage.GERMAN), (Integer) 2);
        assertEquals(val.getValueDataRaw(FxLanguage.ITALIAN), (Integer) 2);
    }

    @DataProvider(name = "testInstances")
    private Object[][] getTestInstances() {
        return testInstances;
    }

    /**
     * Test instance for a FxValue element.
     */
    private static class ValueTestBean<T, TDerived extends FxValue<T, TDerived>> {
        private FxValue<T, TDerived> value;
        private T valueElement;
        private List<String> validStringValues;
        private List<String> invalidStringValues;

        public ValueTestBean(FxValue<T, TDerived> value, T valueElement, String[] validStringValues, String[] invalidStringValues) {
            super();
            this.value = value;
            this.valueElement = valueElement;
            this.validStringValues = Collections.unmodifiableList(Arrays.asList(validStringValues));
            this.invalidStringValues = Collections.unmodifiableList(Arrays.asList(invalidStringValues));
        }

        public List<String> getInvalidStringValues() {
            return invalidStringValues;
        }

        public List<String> getValidStringValues() {
            return validStringValues;
        }

        public FxValue<T, TDerived> getValue() {
            return value;
        }

        public T getValueElement() {
            return valueElement;
        }
    }
}
