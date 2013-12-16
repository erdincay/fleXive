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

import com.flexive.shared.AbstractSelectableObjectWithName;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Selectable object tests, especially equality and comparable tests.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = "shared")
public class SelectableObjectTest {
    private static class SelectableFromAbstract extends AbstractSelectableObjectWithName {
        private long id;
        private String name;

        public SelectableFromAbstract(long id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private static class SubSelectableFromAbstract extends SelectableFromAbstract {
        public SubSelectableFromAbstract(long id, String name) {
            super(id, name);
        }
    }

    @Test
    public void selectableFromAbstractEquals() {
        SelectableFromAbstract sfa1 = new SelectableFromAbstract(1, "test");
        SelectableFromAbstract sfa2 = new SelectableFromAbstract(2, "test");
        Assert.assertTrue(!sfa1.equals(sfa2));
        Assert.assertTrue(!sfa2.equals(sfa1));
        Assert.assertTrue(!sfa1.equals(null));  // check for NPE
        SelectableFromAbstract sfa2_2 = new SelectableFromAbstract(2, "test");
        Assert.assertTrue(sfa2.equals(sfa2_2));
    }

    /**
     * Checks that objects of different types, but the same ID are considered unequal.
     */
    @Test
    public void selectableFromAbstractSubclassEquals() {
        SelectableFromAbstract sfa = new SelectableFromAbstract(1, "test");
        SubSelectableFromAbstract ssfa = new SubSelectableFromAbstract(1, "test");
        Assert.assertTrue(!sfa.equals(ssfa));
    }
}
