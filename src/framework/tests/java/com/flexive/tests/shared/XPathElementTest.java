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

import com.flexive.shared.XPathElement;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxInvalidParameterException;
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
        assert ("B".equals(xpe.getAlias()) && xpe.getIndex() == 3) : "expected B[3]";
        xpe = XPathElement.toElement("/Test/A/B", "B");
        assert ("B".equals(xpe.getAlias()) && xpe.getIndex() == 1) : "expected B[1]";
    }

    @Test
    public void lastElement() throws FxInvalidParameterException {
        assert "C[1]".equals(XPathElement.lastElement("/A/B/C").toString());
        assert "C[123]".equals(XPathElement.lastElement("/A/B/C[123]").toString());
        assert "C[2]".equals(XPathElement.lastElement("/C[2]").toString());
        try {
            XPathElement.lastElement("/A/B/C[123]s");
            assert false : "expected exception";
        } catch (FxInvalidParameterException e) {
            //ok
        }
        try {
            XPathElement.lastElement("A/B/C[123]");
            assert false : "expected exception";
        } catch (FxInvalidParameterException e) {
            //ok
        }
        try {
            XPathElement.lastElement("/A/B/C[a]");
            assert false : "expected exception";
        } catch (FxInvalidParameterException e) {
            //ok
        }
    }

    @Test
    public void isValid() {
        String[] validPatterns = {
                "/A",
                "/A[1]",
                "/A[1]/B[1]/C[1]",
                "/AA[1]/B",
                "A[@pk=123.46]/A/BC",
                "A[@pk=NEW]/A",
                "A[@pk=5.LIVE]/A",
                "A[@pk=7.MAX]/A",
        };
        String[] invalidPatterns = {
                "/A/",
                "/A[a]",
                "A/B[1]",
                "/AA[1]/B/",
                "A[@pk=NEW.42]/A",
                "A[@pk=LIVE]/A",
                "A[@pk=]/A",
                "A[@pk]/A",
                "A[]/A",
        };
        for (String valid : validPatterns)
            assert XPathElement.isValidXPath(valid) : "Pattern " + valid + " was expected to be valid!";
        for (String valid : invalidPatterns)
            assert !XPathElement.isValidXPath(valid) : "Pattern " + valid + " was expected to be invalid!";
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
            assert pattern[0].equals(XPathElement.toXPath(XPathElement.split(pattern[1])));
    }

    @Test
    public void cardinalities() throws FxInvalidParameterException {
        int[] card = XPathElement.getIndices("/A/B[2]/C[3]/D");
        assert card.length == 4 && card[0] == 1 && card[1] == 2 && card[2] == 3 && card[3] == 1 : "multiplicity didn't match!";
        assert "/A[1]/B[2]/C[1]".equals(XPathElement.toXPathMult("/A/B[2]/C"));
        assert "/A/B/C".equals(XPathElement.toXPathNoMult("/A[2]/B/C[1234]"));
        assert "XY/A/B/C".equals(XPathElement.toXPathNoMult("XY[@pk=NEW]/A[2]/B/C[1234]")) : "Expected XY/A/B/C but got " + XPathElement.toXPathNoMult("XY[@pk=NEW]/A[2]/B/C[1234]");
        List<XPathElement> xp = XPathElement.split("/A/B[1]/C/D[2]");
        assert !xp.get(0).isIndexDefined();
        assert xp.get(1).isIndexDefined();
        assert !xp.get(2).isIndexDefined();
        assert xp.get(3).isIndexDefined();
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
            assert test[0][0].equals(XPathElement.buildXPath(false, test[1]));
        for (String[][] test : testsSlash)
            assert test[0][0].equals(XPathElement.buildXPath(true, test[1]));
    }

    @Test
    public void xpathPk() {
        Assert.assertEquals(XPathElement.getPK("TYPE[@pk=5.1]/property"), new FxPK(5, 1));
        Assert.assertEquals(XPathElement.getPK("TYPE[@pk=NEW]/property"), new FxPK());
        Assert.assertEquals(XPathElement.getPK("TYPE[@pk=5.LIVE]/property"), new FxPK(5, FxPK.LIVE));
        Assert.assertEquals(XPathElement.getPK("TYPE[@pk=5.MAX]/property"), new FxPK(5, FxPK.MAX));
    }

}
