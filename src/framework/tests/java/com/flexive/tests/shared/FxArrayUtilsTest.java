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
        String a = FxArrayUtils.createEmptyStringArray(6, ",");
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
        a = FxArrayUtils.replaceElement(a, ',', 5, "Y");
        Assert.assertEquals(a, "10,11,12,,X,Y");
    }
}
