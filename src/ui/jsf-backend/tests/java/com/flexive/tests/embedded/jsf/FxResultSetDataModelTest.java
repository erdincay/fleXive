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
package com.flexive.tests.embedded.jsf;

import com.flexive.faces.model.FxResultSetDataModel;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxAccountInUseException;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxLoginFailedException;
import com.flexive.shared.exceptions.FxLogoutFailedException;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.SqlQueryBuilder;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Tests for the FxResultSetDataModel class, including basic SQL searches.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = {"jsf", "search", "resultset"})
public class FxResultSetDataModelTest extends AbstractSqlQueryTest {

    @Override
    @BeforeClass
    public void beforeClass() throws FxLoginFailedException, FxAccountInUseException, FxApplicationException {
        super.beforeClass();
    }


    @Override
    @AfterClass
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        super.afterClass();
    }

    /**
     * Checks if the test content can be found as expected.
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void findTestData() throws FxApplicationException {
        FxResultSet rs = EJBLookup.getSearchEngine().search(SELECT_ALL);
        Assert.assertTrue(rs.getRowCount() == TOTALROWS, "Unexpected number of results: " + rs.getRowCount());
        assertTotalRowCount(rs);
    }

    /**
     * Tests a partial result set model with start index 0.
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void preloadedPartialResult() throws FxApplicationException {
        testPreloadedPartialResult(0);
        testPreloadedPartialResult(10);
        testPreloadedPartialResult(TOTALROWS - 11);
    }

    /**
     * Tests partial results retrieved using a query builder.
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void lazyPartialResult() throws FxApplicationException {
        SqlQueryBuilder builder = getSelectAllBuilder();

        testLazyPartialResult(builder, 0, 10);
        testLazyPartialResult(builder, 10, 10);
        testLazyPartialResult(builder, 0, 1);
        testLazyPartialResult(builder, 10, 1);
    }

    private void testLazyPartialResult(SqlQueryBuilder builder, final int startIndex, final int maxRows) throws FxApplicationException {
        FxResultSetDataModel model = new FxResultSetDataModel(builder, startIndex, maxRows);
        FxResultSet refResult = getRows(startIndex, maxRows);
        // trigger load
//		model.setRowIndex(startIndex - 1);
//		Assert.assertTrue(startIndex == 0 || model.isRowAvailable(), "Row before first row should be available");
        model.setRowIndex(startIndex);
        Assert.assertTrue(model.isRowAvailable(), "First row should be available");
        model.setRowIndex(startIndex + maxRows - 1);
        Assert.assertTrue(model.isRowAvailable(), "Last row should be available");
        model.setRowIndex(startIndex + maxRows);
        Assert.assertTrue(!model.isRowAvailable(), "Row after last row should not be available");
        Assert.assertTrue(model.getRowCount() == TOTALROWS, "Total row count should be " + TOTALROWS + ", got: " + model.getRowCount());
        Assert.assertNotNull(model.getResult());
        Assert.assertTrue(model.getWrappedData() == model.getResult(), "Result set should be wrapped data");
        Assert.assertTrue(model.getFetchRows() == maxRows, "FetchRows should be " + maxRows + ", got: " + model.getFetchRows());
        assertEqualResults(model, refResult, startIndex, maxRows);
    }

    /**
     * Tests a query that returns only the last row.
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void oneRowResult() throws FxApplicationException {
        FxResultSetDataModel model = new FxResultSetDataModel(getRows(TOTALROWS - 1, 1));
        model.setRowIndex(TOTALROWS - 1);
        Assert.assertTrue(model.isRowAvailable(), "Row should be available");
        model.getRowData();
    }

    /**
     * Sets a custom result as data source.
     *
     * @throws FxApplicationException on errors
     */
    @Test
    public void testWrappedData() throws FxApplicationException {
        FxResultSet refResult = getRows(0, 10);
        FxResultSetDataModel model = new FxResultSetDataModel(refResult);
        Assert.assertTrue(model.getWrappedData() == refResult, "Original result set should not be wrapped.");
        assertEqualResults(model, refResult, 0, 10);
        model.setWrappedData(getRows(10, 10));
        assertEqualResults(model, (FxResultSet) model.getWrappedData(), 10, 10);
    }

    private void testPreloadedPartialResult(int startRow) throws FxApplicationException {
        FxResultSetDataModel model = new FxResultSetDataModel(getRows(startRow, 10));
        model.setRowIndex(startRow - 1);
        Assert.assertTrue(!model.isRowAvailable(), "No rows before startRow should be available.");
        for (int i = 0; i < 10; i++) {
            model.setRowIndex(startRow + i);
            Assert.assertTrue(model.isRowAvailable(), "Expected more rows, current row: " + (startRow + i + 1));
            model.getRowData();
        }
        model.setRowIndex(startRow + 10);
        Assert.assertTrue(!model.isRowAvailable(), "No more rows should be available");
	}
}
