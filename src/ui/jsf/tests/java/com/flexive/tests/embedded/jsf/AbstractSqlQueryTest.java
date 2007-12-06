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
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxContent;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.*;
import com.flexive.shared.interfaces.AssignmentEngine;
import com.flexive.shared.interfaces.ContentEngine;
import com.flexive.shared.interfaces.TypeEngine;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.QueryOperatorNode;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.security.ACL;
import com.flexive.shared.security.Mandator;
import com.flexive.shared.structure.*;
import com.flexive.shared.value.FxString;
import com.flexive.tests.embedded.FxTestUtils;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import com.flexive.tests.embedded.TestUsers;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * Base class for SQL query tests, including test data generation.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class AbstractSqlQueryTest {
    protected static final int TOTALROWS = 25;
    protected static final String TEST_TYPE = "FXRESULTSET_TYPE";
    protected final static String TEST_PROPERTY = "FXRESULTSET_TESTPROPERTY";
    protected static final String SELECT_ALL = "SELECT co.@PK FROM content co WHERE co." + TEST_PROPERTY + " like 'testValue%'";

    protected ACL structureAcl;
    protected ACL contentAcl;
    protected long typeId = -1;
    protected long assignmentId = -1;

    @BeforeClass
    public void beforeClass() throws FxLoginFailedException, FxAccountInUseException, FxApplicationException {
        final AssignmentEngine assignmentEngine = EJBLookup.getAssignmentEngine();
        final TypeEngine typeEngine = EJBLookup.getTypeEngine();
        final ContentEngine contentEngine = EJBLookup.getContentEngine();

        login(TestUsers.SUPERVISOR);

        // create structure
        tearDownStructures();
        this.structureAcl = FxTestUtils.createACL("Test:" + FxResultSetDataModelTest.class,
                ACL.Category.STRUCTURE, Mandator.MANDATOR_FLEXIVE);
        this.contentAcl = FxTestUtils.createACL("Test_Content:" + FxResultSetDataModelTest.class,
                ACL.Category.INSTANCE, Mandator.MANDATOR_FLEXIVE);
        FxPropertyEdit pe = FxPropertyEdit.createNew(TEST_PROPERTY, new FxString(""),
                new FxString(""), FxMultiplicity.MULT_0_1, structureAcl, FxDataType.String1024);
        pe.setAutoUniquePropertyName(true);
        typeId = typeEngine.save(FxTypeEdit.createNew(TEST_TYPE, new FxString(""), structureAcl, null));
        try {
            assignmentId = assignmentEngine.createProperty(typeId, pe, "/");
        } catch (FxEntryExistsException e) {
            // ignore
        }

        // create some test data
        for (int i = 0; i < TOTALROWS; i++) {
            FxContent co = contentEngine.initialize(typeId, Mandator.MANDATOR_FLEXIVE, contentAcl.getId(), -1, -1);
            co.setValue("/" + TEST_PROPERTY, new FxString(false, "testValue" + i));
            contentEngine.save(co);
        }
    }

    @AfterClass
    public void afterClass() throws FxLogoutFailedException, FxApplicationException {
        tearDownStructures();
        EJBLookup.getACLEngine().remove(structureAcl.getId());
        EJBLookup.getACLEngine().remove(contentAcl.getId());
        logout();
    }

    private void tearDownStructures() {
        final TypeEngine typeEngine = EJBLookup.getTypeEngine();
        // remove contents
        try {
            if (typeId != -1) {
                EJBLookup.getContentEngine().removeForType(typeId);
            }
        } catch (Exception e) {
            // ignore
        }

        // remove type
        try {
            FxType type = CacheAdmin.getEnvironment().getType(TEST_TYPE);
            typeEngine.remove(type.getId());
        } catch (Exception e) {
            // ignore
        }
    }

    protected FxResultSet getRows(int startRow, int maxRows) throws FxApplicationException {
        FxResultSet rs = EJBLookup.getSearchEngine().search(SELECT_ALL, startRow, maxRows, null);
        assert rs.getRowCount() == maxRows : "Unexpected number of results: " + rs.getRowCount()
                + ", expected: " + maxRows;
        assertTotalRowCount(rs);
        return rs;
    }


    protected void assertEqualResults(FxResultSetDataModel model, FxResultSet refResult, int startIndex, int maxRows) {
        for (int i = startIndex; i < startIndex + maxRows; i++) {
			model.setRowIndex(i);
			final Object[] refRow = refResult.getRows().get(i - startIndex);
			final FxPK refPk = (FxPK) refRow[0];
			final FxPK pk = (FxPK) ((Object[]) model.getRowData())[0];
			assert refPk.equals(pk) :
				"Rows should be equal:\n" + "Lazy-load: " + pk + "\nPreload: " + refPk;
            assert model.getRowIndex() == i : "Expected row index: " + i + ", got: " + model.getRowIndex();
        }
    }

	protected void assertTotalRowCount(FxResultSet rs) {
		assert rs.getTotalRowCount() == TOTALROWS : "Unexpected total rows: " + rs.getTotalRowCount();
	}


    protected SqlQueryBuilder getSelectAllBuilder() {
        SqlQueryBuilder builder = new SqlQueryBuilder();
        builder.enterSub(QueryOperatorNode.Operator.AND);
        builder.condition(CacheAdmin.getEnvironment().getAssignment(assignmentId),
                    PropertyValueComparator.LIKE, new FxString("testValue%"));
        builder.closeSub();
        return builder;
    }
}
