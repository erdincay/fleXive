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

/**
 * Tests for the FxResultSetDataModel class, including basic SQL searches.
 *  
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
@Test(groups = "jsf")
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
	 * @throws FxApplicationException 
	 */
	@Test
	public void findTestData() throws FxApplicationException {
		FxResultSet rs = EJBLookup.getSearchEngine().search(SELECT_ALL, 1, null, null);
		assert rs.getRowCount() == TOTALROWS : "Unexpected number of results: " + rs.getRowCount();
		assertTotalRowCount(rs);
	}

	/**
	 * Tests a partial result set model with start index 0.
	 * @throws FxApplicationException 
	 */
	@Test
	public void preloadedPartialResult() throws FxApplicationException {
		testPreloadedPartialResult(0);
		testPreloadedPartialResult(10);
		testPreloadedPartialResult(TOTALROWS - 11);
	}
	
	/**
	 * Tests partial results retrieved using a query builder.
	 * @throws FxApplicationException 
	 *
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
		FxResultSetDataModel model = new FxResultSetDataModel(EJBLookup.getSearchEngine(), builder, startIndex, maxRows);
		FxResultSet refResult = getRows(startIndex, maxRows);
        // trigger load
//		model.setRowIndex(startIndex - 1);
//		assert startIndex == 0 || model.isRowAvailable() : "Row before first row should be available";
        model.setRowIndex(startIndex);
        assert model.isRowAvailable() : "First row should be available";
		model.setRowIndex(startIndex + maxRows - 1);
		assert model.isRowAvailable() : "Last row should be available";
		model.setRowIndex(startIndex + maxRows);
		assert !model.isRowAvailable() : "Row after last row should not be available";
        assert model.getWrappedData() == model.getResult() : "Result set should be wrapped data";
        assert model.getRowCount() == TOTALROWS : "Total row count should be " + TOTALROWS + ", got: " + model.getRowCount();
        assert model.getFetchRows() == maxRows : "FetchRows should be " + maxRows + ", got: " + model.getFetchRows();
        assertEqualResults(model, refResult, startIndex, maxRows);
    }

    /**
	 * Tests a query that returns only the last row.
	 * @throws FxApplicationException 
	 */
	@Test
	public void oneRowResult() throws FxApplicationException {
		FxResultSetDataModel model = new FxResultSetDataModel(getRows(TOTALROWS - 1, 1));
		model.setRowIndex(TOTALROWS - 1);
		assert model.isRowAvailable() : "Row should be available";
		model.getRowData();
	}

    /**
     * Sets a custom result as data source.
     */
    @Test
    public void testWrappedData() throws FxApplicationException {
        FxResultSet refResult = getRows(0, 10);
        FxResultSetDataModel model = new FxResultSetDataModel(refResult);
        assert model.getWrappedData() == refResult : "Original result set should not be wrapped.";
        assertEqualResults(model, refResult, 0, 10);
        model.setWrappedData(getRows(10, 10));
        assertEqualResults(model, (FxResultSet) model.getWrappedData(), 10, 10);
    }

    private void testPreloadedPartialResult(int startRow) throws FxApplicationException {
		FxResultSetDataModel model = new FxResultSetDataModel(getRows(startRow, 10));
		model.setRowIndex(startRow - 1);
		assert !model.isRowAvailable() : "No rows before startRow should be available.";
		for (int i = 0; i < 10; i++) {
			model.setRowIndex(startRow + i);
			assert model.isRowAvailable() : "Expected more rows, current row: " + (startRow+i+1);
			model.getRowData();
		}
		model.setRowIndex(startRow + 10);
		assert !model.isRowAvailable() : "No more rows should be available";
	}
}
