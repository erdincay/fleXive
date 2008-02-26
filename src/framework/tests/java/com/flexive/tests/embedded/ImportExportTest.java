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
import com.flexive.shared.content.FxContent;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.value.FxValue;
import com.flexive.core.conversion.ConversionEngine;
import com.thoughtworks.xstream.XStream;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Random;

/**
 * Tests for importing and exporting data
 *
 * @author Markus Plesser (markus.plesser@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev
 */
@Test(groups = {"ejb", "importexport"})
public class ImportExportTest {

    /**
     * Test values
     */
    @Test
    public void valueMarshalling() {
        Random rnd = new Random(System.currentTimeMillis() + 42);
        XStream xs = ConversionEngine.getXStream();
        for (FxDataType dt : FxDataType.values()) {
            switch (dt) {
                case SelectOne:
                case SelectMany:
                case InlineReference:
                    //these datatypes are not implemented for random values
                    continue;
                default:
                    FxValue ml = dt.getRandomValue(rnd, true);
                    Assert.assertEquals(ml, xs.fromXML(xs.toXML(ml)), "Testing: " + dt.name());
                    FxValue sl = dt.getRandomValue(rnd, false);
                    Assert.assertEquals(sl, xs.fromXML(xs.toXML(sl)), "Testing: " + dt.name());
                    Assert.assertNotSame(xs.toXML(sl), xs.toXML(ml), "Testing: " + dt.name());
            }
        }
    }

    /**
     * Test FxContent
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void contentMarshalling() throws FxApplicationException {
        ContentEngine ce = EJBLookup.getContentEngine();
        FxContent co = ce.initialize(FxType.CONTACTDATA);
        XStream xs = ConversionEngine.getXStream();
        final String xml = xs.toXML(co);
        System.out.print("Content:\n" + xml);
        System.out.println("Reverted:\n" + xs.toXML(xs.fromXML(xml)));

    }

}
