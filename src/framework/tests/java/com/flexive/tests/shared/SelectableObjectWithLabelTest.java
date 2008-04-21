/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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

import com.flexive.shared.AbstractSelectableObjectWithLabel;
import com.flexive.shared.value.FxString;
import org.testng.annotations.Test;

/**
 * Selectable object tests, especially equality and comparable tests.
 * NOTE: currently copied from SelectableObjectTest, it's OK to delete this test file entirely
 * when the SelectableObjectWithName and SelectableObjectWithLabel interfaces are combined.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = "shared")
public class SelectableObjectWithLabelTest {
    private static class SelectableFromAbstract extends AbstractSelectableObjectWithLabel {
        private long id;
        private FxString label;

        public SelectableFromAbstract(long id, FxString label) {
            this.id = id;
            this.label = label;
        }

        public long getId() {
            return id;
        }

        public FxString getLabel() {
            return label;
        }
    }

    private static class SubSelectableFromAbstract extends SelectableFromAbstract {
        public SubSelectableFromAbstract(long id, FxString label) {
            super(id, label);
        }
    }

    @Test
    public void selectableFromAbstractEquals() {
        SelectableFromAbstract sfa1 = new SelectableFromAbstract(1, new FxString("test"));
        SelectableFromAbstract sfa2 = new SelectableFromAbstract(2, new FxString("test"));
        assert !sfa1.equals(sfa2);
        assert !sfa2.equals(sfa1);
        //noinspection ObjectEqualsNull
        assert !sfa1.equals(null);  // check for NPE
        SelectableFromAbstract sfa2_2 = new SelectableFromAbstract(2, new FxString("test"));
        assert sfa2.equals(sfa2_2);
    }

    /**
     * Checks that objects of different types, but the same ID are considered unequal.
     */
    @Test
    public void selectableFromAbstractSubclassEquals() {
        SelectableFromAbstract sfa = new SelectableFromAbstract(1, new FxString("test"));
        SubSelectableFromAbstract ssfa = new SubSelectableFromAbstract(1, new FxString("test"));
        assert !sfa.equals(ssfa);
    }
}
