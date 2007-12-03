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

import com.flexive.shared.search.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

/**
 * Basic unit tests for the ResultPreferences class.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = "shared")
public class ResultPreferencesTest {
    private static final ResultColumnInfo[][] INFOS_EQUAL = {
            {new ResultColumnInfo(Table.CONTENT, "type", ""), new ResultColumnInfo(Table.CONTENT, "type", "")},
            {new ResultColumnInfo(Table.CONTENT, "Type", ""), new ResultColumnInfo(Table.CONTENT, "type", "")},
            {new ResultColumnInfo(Table.CONTENT, "type", "name"), new ResultColumnInfo(Table.CONTENT, "type", "name")},
            {new ResultColumnInfo(Table.CONTENT, "type", "Name"), new ResultColumnInfo(Table.CONTENT, "type", "name")},
            {new ResultColumnInfo(Table.CONTENT, "type", null), new ResultColumnInfo(Table.CONTENT, "type", null)},
    };
    private static final ResultColumnInfo[][] INFOS_UNEQUAL = {
            {new ResultColumnInfo(Table.CONTENT, "type", ""), new ResultColumnInfo(Table.CONTENT, "type1", "")},
            {new ResultColumnInfo(Table.CONTENT, "type", "name"), new ResultColumnInfo(Table.CONTENT, "type", "")},
            {new ResultColumnInfo(Table.CONTENT, "type", "name"), new ResultColumnInfo(Table.CONTENT, "type", null)},
            {new ResultColumnInfo(Table.CONTENT, "type", "name"), new ResultColumnInfo(Table.CONTENT, "type", "id")},
    };
    private static final ResultOrderByInfo[][] ORDERBY_EQUAL = {
            {new ResultOrderByInfo(Table.CONTENT, "type", "", SortDirection.ASCENDING),
                    new ResultOrderByInfo(Table.CONTENT, "type", "", SortDirection.ASCENDING)},
            {new ResultOrderByInfo(Table.CONTENT, "type", "", SortDirection.DESCENDING),
                    new ResultOrderByInfo(Table.CONTENT, "type", "", SortDirection.DESCENDING)}
    };
    private static final ResultOrderByInfo[][] ORDERBY_UNEQUAL = {
            {new ResultOrderByInfo(Table.CONTENT, "type", "", SortDirection.ASCENDING),
                    new ResultOrderByInfo(Table.CONTENT, "type", "", SortDirection.DESCENDING)}
    };

    @Test
    public void resultPreferencesDeepCopy() {
        ResultPreferences rp = new ResultPreferences();
        ResultPreferencesEdit rpe = rp.getEditObject();

        // add a column info, check if base object remains unchanged
        assert rp.getSelectedColumns().size() == 0;
        assert rpe.getSelectedColumns().size() == 0;
        final ResultColumnInfo ci = INFOS_EQUAL[0][0];
        rpe.addSelectedColumn(ci);
        assert rp.getSelectedColumns().size() == 0;
        assert rpe.getSelectedColumns().size() == 1;
        assert rpe.getSelectedColumns().get(0).equals(ci);

        // add a order by info, check if base object remains unchanged
        assert rp.getOrderByColumns().size() == 0;
        assert rpe.getOrderByColumns().size() == 0;
        final ResultOrderByInfo obi = ORDERBY_EQUAL[0][0];
        rpe.addOrderByColumn(obi);
        assert rpe.getOrderByColumns().size() == 1;
        assert rp.getOrderByColumns().size() == 0;
        assert rpe.getOrderByColumns().get(0).equals(obi);
    }

    @Test
    public void resultPreferencesCopy() {
        final List<ResultColumnInfo> selectedColumns = Arrays.asList(INFOS_UNEQUAL[0]);
        final List<ResultOrderByInfo> orderByColumns = Arrays.asList(ORDERBY_UNEQUAL[0]);
        ResultPreferences rp = new ResultPreferences(selectedColumns, orderByColumns, 15, 150);
        assert rp.getSelectedColumns().size() == 2;
        assert rp.getOrderByColumns().size() == 2;
        assert rp.getSelectedColumns().equals(selectedColumns);
        assert rp.getOrderByColumns().equals(orderByColumns);
        ResultPreferencesEdit rpe = rp.getEditObject();
        assert rp.equals(rpe);

        // remove column, assert unequal, then restore previous state
        final ResultColumnInfo ci = rpe.removeSelectedColumn(1);
        assert !rp.equals(rpe);
        rpe.addSelectedColumn(ci);
        assert rp.equals(rpe);

        // remove order by, assert unequal, then restore previous state
        final ResultOrderByInfo obi = rpe.removeOrderByColumn(1);
        assert (!rp.equals(rpe));
        rpe.addOrderByColumn(obi);
        assert rp.equals(rpe);

        // other tests
        rpe.setThumbBoxSize(10);
        assert !rp.equals(rpe);
        rpe.setThumbBoxSize(rp.getThumbBoxSize());
        assert rp.equals(rpe);
        rpe.setRowsPerPage(100);
        assert !rp.equals(rpe);
        rpe.setRowsPerPage(rp.getRowsPerPage());
        assert rp.equals(rpe);
    }

    @Test
    public void resultPreferencesAddEmpty() {
        ResultPreferences rp = new ResultPreferences();
        ResultPreferencesEdit rpe = rp.getEditObject();
        try {
            rpe.addSelectedColumn(null);
            assert false : "It should not be possible to add null column infos";
        } catch (RuntimeException e) {
            // pass
        }
        try {
            rpe.addOrderByColumn(null);
            assert false : "It should not be possible to add null order by column infos";
        } catch (RuntimeException e) {
            // pass
        }
    }

    @Test(dataProvider = "equalColumnInfos")
    public void columnInfoEqual(ResultColumnInfo ci1, ResultColumnInfo ci2) {
        assert ci1.equals(ci2) && ci2.equals(ci1) : ci1 + " should be equal to " + ci2;
    }

    @Test(dataProvider = "unequalColumnInfos")
    public void columnInfoUnequal(ResultColumnInfo ci1, ResultColumnInfo ci2) {
        assert !ci1.equals(ci2) && !ci2.equals(ci1) : ci1 + " should not be equal to " + ci2;
    }

    @Test(dataProvider = "equalOrderByInfos")
    public void orderByEqual(ResultOrderByInfo obi1, ResultOrderByInfo obi2) {
        assert obi1.equals(obi2) && obi2.equals(obi1) : obi1 + " should be equal to " + obi2;
    }

    @Test(dataProvider = "unequalOrderByInfos")
    public void orderByUnequal(ResultOrderByInfo obi1, ResultOrderByInfo obi2) {
        assert !obi1.equals(obi2) && !obi2.equals(obi1) : obi1 + " should not be equal to " + obi2;
    }

    @DataProvider(name = "equalColumnInfos")
    private Object[][] getEqualColumnInfos() {
        return INFOS_EQUAL;
    }

    @DataProvider(name = "unequalColumnInfos")
    private Object[][] getUnequalColumnInfos() {
        return INFOS_UNEQUAL;
    }

    @DataProvider(name = "equalOrderByInfos")
    private Object[][] getEqualOrderByInfos() {
        return ORDERBY_EQUAL;
    }

    @DataProvider(name = "unequalOrderByInfos")
    private Object[][] getUnequalOrderByInfos() {
        return ORDERBY_UNEQUAL;
    }
}
