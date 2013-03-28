/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2010
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.tests.embedded.jsf.util;

import com.flexive.faces.FxJsfUtils;
import com.google.common.collect.Lists;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import java.util.Arrays;
import java.util.List;

/**
 * Tests for the FxJsfUtils utility class.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"jsf"})
public class JsfUtilTest {
    @Test
    public void testEvalInt() {
        Assert.assertTrue(FxJsfUtils.evalInt("3") == 3);
    }

    @Test
    public void testSelectItemSorter() {
        final SelectItem[] items = {
                new SelectItem(null, null), // null value and label
                new SelectItem(-1, null),
                new SelectItem(-1, "b"),
                new SelectItem(-1, "a")
        };
        Arrays.sort(items, new FxJsfUtils.SelectItemSorter());
        Assert.assertNull(items[0].getLabel());
        Assert.assertNull(items[1].getLabel());
        Assert.assertEquals("a", items[2].getLabel());
        Assert.assertEquals("b", items[3].getLabel());
    }

    @Test
    public void testGroupsSelectItemSort() {
        final List<SelectItem> items = Lists.newArrayList(
                new SelectItem(2, "Item 2"),
                new SelectItem(1, "Item 1"),
                new SelectItemGroup("Group 1", null, false, new SelectItem[] {
                        new SelectItem(3, "Item 1.2"),
                        new SelectItem(4, "Item 1.1")
                })
        );
        FxJsfUtils.sortSelectItems(items);
        Assert.assertEquals(items.get(0).getLabel(), "Group 1");
        Assert.assertEquals(items.get(1).getLabel(), "Item 1");
        Assert.assertEquals(items.get(2).getLabel(), "Item 2");

        final SelectItem[] groupItems = ((SelectItemGroup) items.get(0)).getSelectItems();
        Assert.assertEquals(groupItems[0].getLabel(), "Item 1.1");
        Assert.assertEquals(groupItems[1].getLabel(), "Item 1.2");
    }
}
