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

import com.flexive.shared.XPathElement;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxRuntimeException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;

/**
 * XPathElement test
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"shared"})
public class XPathElementTest {

    @Test
    public void toElement() throws FxInvalidParameterException {
        XPathElement xpe = XPathElement.toElement("/Test/A/B", "B[3]");
        Assert.assertTrue(("B".equals(xpe.getAlias()) && xpe.getIndex() == 3), "expected B[3]");
        xpe = XPathElement.toElement("/Test/A/B", "B");
        Assert.assertTrue(("B".equals(xpe.getAlias()) && xpe.getIndex() == 1), "expected B[1]");
    }

    @Test
    public void lastElement() throws FxInvalidParameterException {
        Assert.assertEquals("C[1]", XPathElement.lastElement("/A/B/C").toString());
        Assert.assertEquals("C[123]", XPathElement.lastElement("/A/B/C[123]").toString());
        Assert.assertEquals("C[2]", XPathElement.lastElement("/C[2]").toString());
        try {
            XPathElement.lastElement("/A/B/C[123]s");
            Assert.fail("expected exception");
        } catch (FxRuntimeException e) {
            //ok
        }
        Assert.assertEquals("C[123]", XPathElement.lastElement("A/B/C[123]").toString());
        try {
            XPathElement.lastElement("/A/B/C[a]");
            Assert.fail("expected exception");
        } catch (FxRuntimeException e) {
            //ok
        }
    }

    @Test
    public void stripLastElement() throws FxInvalidParameterException {
        try {
            XPathElement.stripLastElement(null);
            Assert.fail("expected exception");
        } catch (FxRuntimeException e) {
            //expected
        }
        try {
            XPathElement.stripLastElement("foobar");
            Assert.fail("expected exception");
        } catch (FxRuntimeException e) {
            //expected
        }
        Assert.assertEquals("/", XPathElement.stripLastElement("/"));
        Assert.assertEquals("/", XPathElement.stripLastElement("/abc"));
        Assert.assertEquals("/ABC", XPathElement.stripLastElement("/abc/def"));
        Assert.assertEquals("/ABC/DEF", XPathElement.stripLastElement("/abc/def/ghi"));
    }

    @Test
    public void isValid() {
        String[] validPatterns = {
                "/A[1]/B[1]/C[1]",
                "A[@pk=123.46]/A/BC",
                "A_B[@pk=123.46]/A/BC",
                "/A[1]",
                "A[@pk=NEW]/A",
                "A[@pk=5.LIVE]/A",
                "A[@pk=7.MAX]/A",
                "A[@pK=7.MAX]/A",
                "A[@PK=7.MAX]/A",
                "A[@pK=7.MAX]/A",
                "/A",
                "/A_A[1]/B",
                "/A___A_[1]/B_/C[42]",
                "A/B/C",
        };
        String[] invalidPatterns = {
                "FOO",
                "",
                "/A[",
                "/AA[1]]/B",
                "/AA[1]/B]",
                "/AA[[1]/B",
                "/AA[[1]]/B",
                "/A/",
                "/1",
                "1/A",
                "/1A",
                "/a",
                "A[@pK=7.MAXX]/A",
                "A[@pK=7.Max]/A",
                "A[@pk=NEW.42]/A",
                "A[@pk=LIVE]/A",
                "A[@pK=7.LIVEE]/A",
                "A[@pK=.MAX]/A",
                "/A[a]",
                "/AA[1]/B/",
                "A[@pk=]/A",
                "A[@pk]/A",
                "A[]/A",
        };
        for (String valid : validPatterns)
            Assert.assertTrue(XPathElement.isValidXPath(valid), "Pattern " + valid + " was expected to be valid!");
        for (String valid : invalidPatterns)
            Assert.assertTrue(!XPathElement.isValidXPath(valid), "Pattern " + valid + " was expected to be invalid!");
    }

    @Test
    public void split() throws FxInvalidParameterException {
        String[][] validPatterns = {
                {"/A[1]/B[2]/C[1]", "/A/B[2]/C"},
                {"/A[1]/B[1]", "/A/B"},
                {"/A[1]/B[1]/C[3]", "/A/B/C[3]"},
                {"/A[1]", "/A"}
        };
        for (String[] pattern : validPatterns)
            Assert.assertTrue(pattern[0].equals(XPathElement.toXPath(XPathElement.split(pattern[1]))));
    }

    @Test
    public void cardinalities() throws FxInvalidParameterException {
        int[] card = XPathElement.getIndices("/A/B[2]/C[3]/D");
        Assert.assertTrue(card.length == 4 && card[0] == 1 && card[1] == 2 && card[2] == 3 && card[3] == 1, "multiplicity didn't match!");
        Assert.assertTrue("/A[1]/B[2]/C[1]".equals(XPathElement.toXPathMult("/A/B[2]/C")));
        Assert.assertTrue("/A/B/C".equals(XPathElement.toXPathNoMult("/A[2]/B/C[1234]")));
        Assert.assertTrue("XY/A/B/C".equals(XPathElement.toXPathNoMult("XY[@pk=NEW]/A[2]/B/C[1234]")), "Expected XY/A/B/C but got " + XPathElement.toXPathNoMult("XY[@pk=NEW]/A[2]/B/C[1234]"));
        List<XPathElement> xp = XPathElement.split("/A/B[1]/C/D[2]");
        Assert.assertTrue(!xp.get(0).isIndexDefined());
        Assert.assertTrue(xp.get(1).isIndexDefined());
        Assert.assertTrue(!xp.get(2).isIndexDefined());
        Assert.assertTrue(xp.get(3).isIndexDefined());
    }

    @Test
    public void buildXPath() {
        String[][][] testsNoSlash = {
                {{"12/23"}, {null, "12", "23"}},
                {{"ROOT/A/B/C"}, {"/ROOT", "/A", "B/", "//C/"}},
                {{"A/B/C"}, {null, "A", "B", "/C"}}
        };
        String[][][] testsSlash = {
                {{"/12/23"}, {null, "12", "23"}},
                {{"/ROOT/A/B/C"}, {"/ROOT", "/A", "B/", "//C/"}},
                {{"/A/B/C"}, {null, "A", null, "B", "/C"}}
        };
        for (String[][] test : testsNoSlash)
            Assert.assertTrue(test[0][0].equals(XPathElement.buildXPath(false, test[1])));
        for (String[][] test : testsSlash)
            Assert.assertTrue(test[0][0].equals(XPathElement.buildXPath(true, test[1])));
    }

    @Test
    public void xpathPk() {
        Assert.assertEquals(XPathElement.getPK("TYPE[@pk=5.1]/property"), new FxPK(5, 1));
        Assert.assertEquals(XPathElement.getPK("TYPE[@pk=NEW]/property"), new FxPK());
        Assert.assertEquals(XPathElement.getPK("TYPE[@pk=5.LIVE]/property"), new FxPK(5, FxPK.LIVE));
        Assert.assertEquals(XPathElement.getPK("TYPE[@pk=5.MAX]/property"), new FxPK(5, FxPK.MAX));
    }

    @Test
    public void changeIndex() {
        Assert.assertEquals(XPathElement.changeIndex("/Test[1]/a[1]/b[3]", 0, 5), "/Test[5]/a[1]/b[3]");
        Assert.assertEquals(XPathElement.changeIndex("/Test[1]/a[1]/b[3]", 1, 5), "/Test[1]/a[5]/b[3]");
        Assert.assertEquals(XPathElement.changeIndex("type/Test[1]/a[1]/b[3]", 1, 5), "type/Test[1]/a[5]/b[3]");
    }

    @Test
    public void getIndices() {
        checkIntArray(XPathElement.getIndices("/TEST[1]/A[1]/B[3]"), new int[]{1, 1, 3});
        checkIntArray(XPathElement.getIndices("/TEST[1]/A/B[3]"), new int[]{1, 1, 3});
        checkIntArray(XPathElement.getIndices("/TEST/A/B[3]"), new int[]{1, 1, 3});
        checkIntArray(XPathElement.getIndices("B/TEST[1]/A/B[3]"), new int[]{1, 1, 3});
        checkIntArray(XPathElement.getIndices("/B/TEST[1]/A/B[3]"), new int[]{1, 1, 1, 3});
    }

    private void checkIntArray(int[] a, int[] b) {
        if( a.length != b.length )
            Assert.fail("Invalid length!");
        for(int i=0; i<a.length; i++)
            Assert.assertEquals(a[i], b[i]);
    }


}
