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

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import com.flexive.shared.search.cmis.CmisResultSet;
import com.flexive.shared.search.cmis.CmisResultRow;
import com.flexive.shared.search.cmis.CmisResultValue;
import com.flexive.shared.value.FxString;
import com.flexive.shared.value.FxNumber;
import com.flexive.shared.exceptions.FxRuntimeException;

import java.util.Arrays;

/**
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class CmisResultSetTest {
    @Test(groups = {"shared", "search", "cmis"})
    public void resultSetConstruction() {
        final CmisResultSet result = new CmisResultSet(2);
        final CmisResultRow row = result.newRow();
        row.setValue(1, 21);
        row.setValue(2, new FxString("String value"));

        result.addRow(row);
        assert result.getRowCount() == 1;

        // check immutability
        try {
            result.getRows().get(0).setValue(1, 22);
            assert false : "Row should be frozen";
        } catch (FxRuntimeException e) {
            assert result.getRows().get(0).getColumn(1).getValue().equals(21);
        }

        try {
            result.freeze().addRow(row);
            assert false : "Result set should be frozen";
        } catch (FxRuntimeException e) {
            result.addRow(result.newRow());    // old result set should remain mutable 
        }
    }

    @Test(groups = {"shared", "search", "cmis"})
    public void resultRowIterator() {
        final CmisResultSet resultSet = new CmisResultSet(1);
        resultSet.addRow(resultSet.newRow().setValue(1, 1));
        resultSet.addRow(resultSet.newRow().setValue(1, 2));
        assert resultSet.getRowCount() == 2;
        int index = 0;
        for (CmisResultRow row : resultSet) {
            assert row.getColumn(1).getValue().equals(++index) : "Unexpected value: " + row.getColumn(1);
        }
        assert index == 2 : "Expected two rows";
    }

    @Test(groups = {"shared", "search", "cmis"})
    public void resultRowColumnsIterator() {
        final CmisResultSet resultSet = new CmisResultSet(2);
        resultSet.addRow(resultSet.newRow().setValue(1, 1).setValue(2, "Row 1 Column 2"));
        resultSet.addRow(resultSet.newRow().setValue(1, 2).setValue(2, "Row 2 Column 2"));
        assert resultSet.getRowCount() == 2;
        int index = 0;
        for (CmisResultRow row : resultSet) {
            assert row.getColumn(1).getValue().equals(++index) : "Unexpected value: " + row.getColumn(1);
            int columns = 0;
            for (CmisResultValue col : row) {
                assert col.getValue().equals(columns == 0 ? index : "Row " + index + " Column 2");
                columns++;
            }
            assert columns == 2 : "Expected two columns";
        }
        assert index == 2 : "Expected two rows";
    }

    @Test(groups = {"shared", "search", "cmis"})
    public void multivaluedResult() {
        final CmisResultSet rs = new CmisResultSet(1);
        rs.addRow(rs.newRow().setValue(1, Arrays.asList(1, 2, 3)));
        rs.addRow(rs.newRow().setValue(1, Arrays.asList(4, 5, 6)));
        assertEquals(rs.getColumn(0, 1).getValues(), Arrays.asList(1, 2, 3));
        assertEquals(rs.getColumn(1, 1).getValues(), Arrays.asList(4, 5, 6));
        assertEquals(rs.getColumn(0, 1).toString(), "[1, 2, 3]"); // List#toString
    }
}
