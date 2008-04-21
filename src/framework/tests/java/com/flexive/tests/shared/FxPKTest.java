/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) project is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation.
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

import com.flexive.shared.content.FxPK;
import com.flexive.shared.value.FxReference;
import com.flexive.shared.value.ReferencedContent;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.Test;

/**
 * Basic FxPK tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"shared"})
public class FxPKTest {
    @Test
    public void testFromString() {
        assertEquals(FxPK.fromString("NEW"), new FxPK());
        assertEquals(FxPK.fromString("10.1"), new FxPK(10, 1));
        assertEquals(FxPK.fromString("10.MAX"), new FxPK(10, FxPK.MAX));
        assertEquals(FxPK.fromString("10.LIVE"), new FxPK(10, FxPK.LIVE));
        assertEquals(FxPK.fromString("10"), new FxPK(10));
    }

    @Test
    public void testFromObject() {
        assertEquals(FxPK.fromObject("10.1"), new FxPK(10, 1));
        assertEquals(FxPK.fromObject(new FxPK(10, 2)), new FxPK(10, 2));
        assertEquals(FxPK.fromObject(new FxReference(false, new ReferencedContent(10))), new FxPK(10));
    }
}
