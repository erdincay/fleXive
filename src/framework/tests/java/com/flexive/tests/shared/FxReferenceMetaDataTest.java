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

import org.testng.annotations.Test;
import org.testng.Assert;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import com.flexive.shared.FxReferenceMetaData;

/**
 * Tests for the FxReferenceMetaData class.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Test(groups = "shared")
public class FxReferenceMetaDataTest {

    public void basicMetaDataOperations() {
        final FxReferenceMetaData meta = new FxReferenceMetaData();
        meta.put("akey", "avalue");
        assertEquals(meta.get("akey"), "avalue");

        meta.put("int", 21);
        assertEquals(meta.get("int"), "21");
        assertEquals(meta.getInt("int", -1), 21);
                           
        meta.put("long", 100L + Integer.MAX_VALUE);
        assertEquals(meta.getLong("long", -1), 100L + Integer.MAX_VALUE);
        assertTrue(meta.getLong("long", -1L) > 0, "Long overflow detected");

        meta.put("double", 21.95);
        assertEquals(meta.getDouble("double", -1), 21.95);
    }

    public void serializeMetaData() {
        final FxReferenceMetaData meta = new FxReferenceMetaData();
        final StringBuilder expected = new StringBuilder();

        testSerialization(meta, expected, "akey", "avalue", "akey=avalue;");
        testSerialization(meta, expected, "bkey", "bvalue", "bkey=bvalue;");
        testSerialization(meta, expected, "escaped", "so;many;;separators;;;;;", "escaped=so;;many;;;;separators;;;;;;;;;;;");
        testSerialization(meta, expected, "a=type;id", "a=1;b=10", "a==type;;id=a==1;;b==10;");

        meta.put("int", 21);
        expected.append("int=21;");
        assertEquals(meta.getSerializedForm(), expected.toString());
        assertEquals(FxReferenceMetaData.fromSerializedForm(null, meta.getSerializedForm()).getInt("int", -1), 21);
    }

    public void mergeMetaData() {
        final FxReferenceMetaData meta = new FxReferenceMetaData();
        meta.put("akey", "123");
        meta.put("toberemoved", "456");
        assertEquals(meta.size(), 2);
        assertEquals(meta.get("toberemoved"), "456");

        final FxReferenceMetaData other = new FxReferenceMetaData();
        other.put("toberemoved", "");
        other.put("bkey", "789");

        meta.merge(other);
        assertEquals(meta.size(), 2);
        assertEquals(meta.get("toberemoved"), null);
        assertFalse(meta.containsKey("toberemoved"));
        assertEquals(meta.get("bkey"), "789");
        assertEquals(meta.get("akey"), "123");
    }

    private void testSerialization(FxReferenceMetaData meta, StringBuilder expected, String key, String value, String serialized) {
        meta.put(key, value);
        expected.append(serialized);
        assertEquals(meta.getSerializedForm(), expected.toString());
        final FxReferenceMetaData<Integer> restored = FxReferenceMetaData.fromSerializedForm(21, meta.getSerializedForm());
        assertEquals(restored.get(key), value, "Value not restored from serialized form (" + restored + ")");
    }
}
