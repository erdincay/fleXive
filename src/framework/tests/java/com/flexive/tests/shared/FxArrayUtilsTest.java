package com.flexive.tests.shared;

import com.flexive.shared.FxArrayUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for com.flexive.shared.FxArrayUtils.
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @see com.flexive.shared.FxArrayUtils
 */
@Test(groups = {"shared"})
public class FxArrayUtilsTest {
    @Test
    public void stringArrayTest() {
        String a = FxArrayUtils.createEmptyStringArray(6, ',');
        Assert.assertEquals(a, ",,,,,");
        a = FxArrayUtils.replaceElement(a, ',', 0, "10");
        Assert.assertEquals(a, "10,,,,,");
        a = FxArrayUtils.replaceElement(a, ',', 2, "12");
        Assert.assertEquals(a, "10,,12,,,");
        a = FxArrayUtils.replaceElement(a, ',', 1, "11");
        Assert.assertEquals(a, "10,11,12,,,");
        a = FxArrayUtils.replaceElement(a, ',', 5, "15");
        Assert.assertEquals(a, "10,11,12,,,15");
        a = FxArrayUtils.replaceElement(a, ',', 4, "14");
        Assert.assertEquals(a, "10,11,12,,14,15");
        a = FxArrayUtils.replaceElement(a, ',', 3, "13");
        Assert.assertEquals(a, "10,11,12,13,14,15");
        a = FxArrayUtils.replaceElement(a, ',', 3, "");
        Assert.assertEquals(a, "10,11,12,,14,15");
        a = FxArrayUtils.replaceElement(a, ',', 4, "X");
        Assert.assertEquals(a, "10,11,12,,X,15");
        a = FxArrayUtils.replaceElement(a, ',', 5, "42");
        Assert.assertEquals(a, "10,11,12,,X,42");
        Assert.assertEquals(FxArrayUtils.getIntElementAt(a, ',', 0), 10);
        Assert.assertEquals(FxArrayUtils.getIntElementAt(a, ',', 1), 11);
        Assert.assertEquals(FxArrayUtils.getIntElementAt(a, ',', 2), 12);
        Assert.assertEquals(FxArrayUtils.getIntElementAt(a, ',', 5), 42);
        Assert.assertEquals(FxArrayUtils.getIntElementAt("", ',', 0), Integer.MIN_VALUE);
        Assert.assertEquals(FxArrayUtils.getIntElementAt("25", ',', 0), 25);

        try {
            FxArrayUtils.getIntElementAt(a, ',', 6);
            Assert.fail("Wrong index should throw exception!");
        } catch (ArrayIndexOutOfBoundsException e) {
            //expected
        }

        // tests for single-element arrays
        Assert.assertEquals(FxArrayUtils.getHexIntElementAt("", ',', 0), null);
        Assert.assertEquals(FxArrayUtils.getHexIntElementAt("25", ',', 0), Integer.valueOf(0x25));
        Assert.assertEquals(FxArrayUtils.getHexIntElementAt(",1", ',', 1), Integer.valueOf(1));

        Assert.assertEquals(FxArrayUtils.replaceElement("", ',', 0, "1"), "1");
        Assert.assertEquals(FxArrayUtils.replaceElement("25", ',', 0, "1"), "1");
    }
}
