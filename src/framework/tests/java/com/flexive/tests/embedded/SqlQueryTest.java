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
package com.flexive.tests.embedded;

import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.search.*;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.QueryOperatorNode;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.security.Account;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.value.*;
import static com.flexive.tests.embedded.FxTestUtils.login;
import static com.flexive.tests.embedded.FxTestUtils.logout;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * FxSQL search query engine tests.
 * <p/>
 * Test data is created in init1201_testcontent.gy !
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
@Test(groups = {"ejb", "search"})
public class SqlQueryTest {
    private static final String TEST_SUFFIX = "SearchProp";
    private static final String TEST_TYPE = "SearchTest";
    private static final Map<String, FxDataType> TEST_PROPS = new HashMap<String, FxDataType>();

    static {
        TEST_PROPS.put("string", FxDataType.String1024);
        TEST_PROPS.put("text", FxDataType.Text);
        TEST_PROPS.put("html", FxDataType.HTML);
        TEST_PROPS.put("number", FxDataType.Number);
        TEST_PROPS.put("largeNumber", FxDataType.LargeNumber);
        TEST_PROPS.put("float", FxDataType.Float);
        TEST_PROPS.put("double", FxDataType.Double);
        TEST_PROPS.put("date", FxDataType.Date);
        TEST_PROPS.put("dateTime", FxDataType.DateTime);
        TEST_PROPS.put("boolean", FxDataType.Boolean);
        TEST_PROPS.put("binary", FxDataType.Binary);
        TEST_PROPS.put("reference", FxDataType.Reference);
        TEST_PROPS.put("selectOne", FxDataType.SelectOne);
        TEST_PROPS.put("selectMany", FxDataType.SelectMany);
        TEST_PROPS.put("dateRange", FxDataType.DateRange);
        TEST_PROPS.put("dateTimeRange", FxDataType.DateTimeRange);
    }

    private int testInstanceCount;  // number of instances for the SearchTest type

    @BeforeClass
    public void setup() throws Exception {
        login(TestUsers.SUPERVISOR);
        testInstanceCount = new SqlQueryBuilder().type(TEST_TYPE).getResult().getRowCount();
        assert testInstanceCount > 0 : "No instances of test type " + TEST_TYPE + " found.";
    }

    @AfterClass
    public void shutdown() throws Exception {
        logout();
    }

    @Test
    public void simpleSelectTest() throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select("caption", "comment").enterSub(QueryOperatorNode.Operator.AND)
                .condition("caption", PropertyValueComparator.LIKE, "test caption%")
                .condition("comment", PropertyValueComparator.LIKE, "folder comment%")
                .closeSub().getResult();
        assert result.getRowCount() == 25 : "Expected to fetch 25 rows, got: " + result.getRowCount();
        assert result.getColumnIndex("co.caption") == 1 : "Unexpected column index for co.caption: " + result.getColumnIndex("co.caption");
        assert result.getColumnIndex("co.comment") == 2 : "Unexpected column index for co.comment: " + result.getColumnIndex("co.comment");
        for (int i = 1; i <= result.getRowCount(); i++) {
            assert result.getString(i, 1).startsWith("test caption") : "Unexpected column value: " + result.getString(i, 1);
            assert result.getString(i, 2).startsWith("folder comment") : "Unexpected column value: " + result.getString(i, 2);
        }
    }

    @Test
    public void simpleNestedQueryTest() throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select("caption").andSub()
                .condition("caption", PropertyValueComparator.LIKE, "test caption%")
                .orSub().condition("comment", PropertyValueComparator.LIKE, "folder comment 1")
                .condition("comment", PropertyValueComparator.LIKE, "folder comment 2").closeSub().getResult();
        assert result.getRowCount() == 2 : "Expected to fetch 2 rows, got: " + result.getRowCount();
        for (int i = 1; i <= 2; i++) {
            assert result.getString(i, 1).matches("test caption [12]") : "Unexpected column value: " + result.getString(i, 1);
        }
    }

    /**
     * Check if the SQL search for empty string properties works.
     *
     * @throws FxApplicationException if the search failed
     */
    @Test
    public void stringEmptyQuery() throws FxApplicationException {
        new SqlQueryBuilder().condition("caption", PropertyValueComparator.EMPTY, null).getResult();
        new SqlQueryBuilder().condition("caption", PropertyValueComparator.NOT_EMPTY, null).getResult();
        new SqlQueryBuilder().condition("caption", PropertyValueComparator.EMPTY, null)
                .orSub().condition("caption", PropertyValueComparator.EMPTY, null).condition("caption", PropertyValueComparator.NOT_EMPTY, null)
                .closeSub().getResult();
    }

    @Test
    public void selectUserTest() throws FxApplicationException {
        for (FxResultRow row : new SqlQueryBuilder().select("created_by", "created_by.username").getResult().getResultRows()) {
            final Account account = EJBLookup.getAccountEngine().load(((FxLargeNumber) row.getFxValue(1)).getDefaultTranslation());
            Assert.assertEquals(row.getFxValue(2).getDefaultTranslation(), account.getName());
        }
    }

    @Test
    public void filterByTypeTest() throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select("typedef").filterType("FOLDER").getResult();
        assert result.getRowCount() > 0;
        final FxType folderType = CacheAdmin.getEnvironment().getType("FOLDER");
        for (FxResultRow row : result.getResultRows()) {
            assert folderType.getId() == ((FxLargeNumber) row.getValue(1)).getBestTranslation()
                    : "Unexpected result type: " + row.getValue(1) + ", expected: " + folderType.getId();
        }
    }

    @Test
    public void briefcaseQueryTest() throws FxApplicationException {
        // create briefcase
        final String selectFolders = new SqlQueryBuilder().filterType("FOLDER").getQuery();
        final FxSQLSearchParams params = new FxSQLSearchParams().saveResultInBriefcase("test briefcase", "description", (Long) null);
        final FxResultSet result = EJBLookup.getSearchEngine().search(selectFolders, 0, Integer.MAX_VALUE, params);
        long bcId = result.getCreatedBriefcaseId();
        try {
            assert result.getRowCount() > 0;
            assert result.getCreatedBriefcaseId() != -1 : "Briefcase should have been created, but no ID returned.";

            // select briefcase
            final FxResultSet briefcase = new SqlQueryBuilder().filterBriefcase(result.getCreatedBriefcaseId()).getResult();
            assert briefcase.getRowCount() > 0 : "Empty briefcase returned, but getResult returned " + result.getRowCount() + " rows.";
        } finally {
            EJBLookup.getBriefcaseEngine().remove(bcId);
        }
    }

    @Test
    public void typeConditionTest() throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select("typedef").type("CONTACTDATA").getResult();
        final FxType cdType = CacheAdmin.getEnvironment().getType("CONTACTDATA");
        assert result.getRowCount() > 0;
        for (FxResultRow row : result.getResultRows()) {
            assert ((FxLargeNumber) row.getFxValue(1)).getDefaultTranslation() == cdType.getId()
                    : "Unexpected type in result, expected " + cdType.getId() + ", was: " + row.getFxValue(1);
        }
    }

    /**
     * Generic tests on the SearchTest type.
     *
     * @param name     the base property name
     * @param dataType the datatype of the property
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on search engine errors
     */
    @Test(dataProvider = "testProperties")
    public void genericSelectTest(String name, FxDataType dataType) throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select(getTestPropertyName(name)).type(TEST_TYPE).getResult();
        assert result.getRowCount() == testInstanceCount : "Expected all test instances to be returned, got "
                + result.getRowCount() + " instead of " + testInstanceCount;
        for (FxResultRow row : result.getResultRows()) {
            assert dataType.getValueClass().isAssignableFrom(row.getFxValue(1).getClass())
                    : "Invalid class returned for datatype " + dataType + ": " + row.getFxValue(1).getClass() + " instead of " + dataType.getValueClass();
        }
    }

    @Test(dataProvider = "testProperties")
    public void orderByTest(String name, FxDataType dataType) throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select("@pk", getTestPropertyName(name)).type(TEST_TYPE)
                .orderBy(2, SortDirection.ASCENDING).getResult();
        assert result.getRowCount() > 0;
        assertAscendingOrder(result, 2);
    }

    @Test
    public void orderByResultPreferencesTest() throws FxApplicationException {
        setResultPreferences(SortDirection.ASCENDING);
        final FxResultSet result = new SqlQueryBuilder().select("@pk", getTestPropertyName("string")).filterType(TEST_TYPE).getResult();
        assertAscendingOrder(result, 2);

        setResultPreferences(SortDirection.DESCENDING);
        final FxResultSet descendingResult = new SqlQueryBuilder().select("@pk", getTestPropertyName("string")).filterType(TEST_TYPE).getResult();
        assertDescendingOrder(descendingResult, 2);
    }

    private void setResultPreferences(SortDirection sortDirection) throws FxApplicationException {
        final ResultPreferencesEdit prefs = new ResultPreferencesEdit(new ArrayList<ResultColumnInfo>(0), Arrays.asList(
                new ResultOrderByInfo(Table.CONTENT, getTestPropertyName("string"), "", sortDirection)),
                25, 100);
        EJBLookup.getResultPreferencesEngine().save(prefs, CacheAdmin.getEnvironment().getType(TEST_TYPE).getId(), ResultViewType.LIST, AdminResultLocations.DEFAULT);
    }

    /**
     * Tests all available value comparators for the given datatype. Note that no semantic
     * tests are performed, each comparator is executed with a random value.
     *
     * @param name     the base property name
     * @param dataType the datatype of the property
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on search engine errors
     */
    @Test(dataProvider = "testProperties")
    public void genericConditionTest(String name, FxDataType dataType) throws FxApplicationException {
        final FxPropertyAssignment assignment = getTestPropertyAssignment(name);
        final Random random = new Random(0);

        // need to get some folder IDs for the reference property
        final List<FxPK> folderPks = new SqlQueryBuilder().select("@pk").type("folder").getResult().collectColumn(1);

        for (PropertyValueComparator comparator : PropertyValueComparator.getAvailable(dataType)) {
            for (String prefix : new String[]{
                    TEST_TYPE + "/",
                    TEST_TYPE + "/groupTop/",
                    TEST_TYPE + "/groupTop/groupNested/"
            }) {
                final String assignmentName = prefix + getTestPropertyName(name);
                try {
                    // submit a query with the given property/comparator combination
                    final FxValue value;
                    switch (dataType) {
                        case Reference:
                            value = new FxReference(new ReferencedContent(folderPks.get(random.nextInt(folderPks.size()))));
                            break;
                        case DateRange:
                            // a query is always performed against a particular date, but not a date range
                            value = new FxDate(new Date());
                            break;
                        case DateTimeRange:
                            value = new FxDateTime(new Date());
                            break;
                        default:
                            value = dataType.getRandomValue(random, assignment);
                    }
                    new SqlQueryBuilder().condition(assignmentName, comparator, value).getResult();
                    // no exception thrown, consider it a success
                } catch (Exception e) {
                    assert false : "Failed to submit for property " + dataType + " with comparator " + comparator
                            + ":\n" + e.getMessage() + ", thrown at:\n"
                            + StringUtils.join(e.getStackTrace(), '\n');
                }
            }
        }
    }

    /**
     * Tests relative comparators like &lt; and == for all datatypes that support them.
     *
     * @param name     the base property name
     * @param dataType the datatype of the property
     * @throws com.flexive.shared.exceptions.FxApplicationException
     *          on search engine errors
     */
    @Test(dataProvider = "testProperties")
    public void genericRelativeComparatorsTest(String name, FxDataType dataType) throws FxApplicationException {
        final String propertyName = getTestPropertyName(name);

        for (PropertyValueComparator comparator : PropertyValueComparator.getAvailable(dataType)) {
            if (!(comparator.equals(PropertyValueComparator.EQ) || comparator.equals(PropertyValueComparator.GE)
               || comparator.equals(PropertyValueComparator.GT) || comparator.equals(PropertyValueComparator.LE)
               || comparator.equals(PropertyValueComparator.LT))) {
                continue;
            }
            final FxValue value = getTestValue(name, comparator);
            final SqlQueryBuilder builder = new SqlQueryBuilder().select("@pk", propertyName).condition(propertyName, comparator, value);
            final FxResultSet result = builder.getResult();
            assert result.getRowCount() > 0 : "Cannot test on empty result sets, query=\n" + builder.getQuery();
            for (FxResultRow row: result.getResultRows()) {
                final FxValue rowValue = row.getFxValue(2);
                switch(comparator) {
                    case EQ:
                        assert rowValue.getBestTranslation().equals(value.getBestTranslation())
                                : "Result value " + rowValue + " is not equal to " + value;
                        break;
                    case LT:
                        assert rowValue.compareTo(value) < 0 : "Result value " + rowValue + " is not less than " + value;
                        break;
                    case LE:
                        assert rowValue.compareTo(value) <= 0 : "Result value " + rowValue + " is not less or equal to " + value;
                        break;
                    case GT:
                        assert rowValue.compareTo(value) > 0 : "Result value " + rowValue + " is not greater than " + value;
                        break;
                    case GE:
                        assert rowValue.compareTo(value) >= 0 : "Result value " + rowValue + " is not greater or equal to " + value;
                        break;
                    default:
                        assert false : "Invalid comparator: " + comparator;
                }
            }
        }
    }

    /**
     * Finds a FxValue of the test instances that matches some of the result rows
     * for the given comparator, but not all or none.
     *
     * @param name  the test property name
     * @param comparator    the comparator
     * @return  a value that matches some rows
     * @throws FxApplicationException   on search engine errors
     */
    private FxValue getTestValue(String name, PropertyValueComparator comparator) throws FxApplicationException {
        final FxResultSet result = new SqlQueryBuilder().select(getTestPropertyName(name)).type(TEST_TYPE).getResult();
        final List<FxValue> values = result.collectColumn(1);
        assert values.size() == testInstanceCount : "Expected " + testInstanceCount + " rows, got: " + values.size();
        for (FxValue value: values) {
            if (value == null || value.isEmpty()) {
                continue;
            }
            int match = 0;  // number of matched values for the given comparator
            int count = 0;  // number of values checked so far
            for (FxValue value2: values) {
                if (value2 == null || value2.isEmpty()) {
                    continue;
                }
                count++;
                switch(comparator) {
                    case EQ:
                        if (value.getBestTranslation().equals(value2.getBestTranslation())) {
                            match++;
                        }
                        break;
                    case LT:
                        if (value2.compareTo(value) < 0) {
                            match++;
                        }
                        break;
                    case LE:
                        if (value2.compareTo(value) <= 0) {
                            match++;
                        }
                        break;
                    case GT:
                        if (value2.compareTo(value) > 0) {
                            match++;
                        }
                        break;
                    case GE:
                        if (value2.compareTo(value) >= 0) {
                            match++;
                        }
                        break;
                    default:
                        assert false : "Cannot check relative ordering for comparator " + comparator;
                }
                if (match > 0 && count > match) {
                    // this value is matched by _some_ other row values, so it's suitable as test input
                    if (value instanceof FxDateRange) {
                        // daterange checks are performed against an actual date, not another range
                        return new FxDate(((FxDateRange) value).getBestTranslation().getLower());
                    } else if (value instanceof FxDateTimeRange) {
                        // see above
                        return new FxDateTime(((FxDateTimeRange) value).getBestTranslation().getLower());
                    }
                    return value;
                }
            }
        }
        throw new IllegalArgumentException("Failed to find a suitable test value for property " + getTestPropertyName(name)
                + " and comparator " + comparator);
    }


    @DataProvider(name = "testProperties")
    public Object[][] getTestProperties() {
        final Object[][] result = new Object[TEST_PROPS.size()][];
        int ctr = 0;
        for (Map.Entry<String, FxDataType> entry : TEST_PROPS.entrySet()) {
            result[ctr++] = new Object[]{entry.getKey(), entry.getValue()};
        }
        return result;
    }

    private String getTestPropertyName(String baseName) {
        return baseName + TEST_SUFFIX;
    }

    private FxPropertyAssignment getTestPropertyAssignment(String baseName) {
        return (FxPropertyAssignment) CacheAdmin.getEnvironment().getAssignment(TEST_TYPE + "/" + getTestPropertyName(baseName));
    }

    private void assertAscendingOrder(FxResultSet result, int column) {
        assertOrder(result, column, true);
    }

    private void assertDescendingOrder(FxResultSet result, int column) {
        assertOrder(result, column, false);
    }

    private void assertOrder(FxResultSet result, int column, boolean ascending) {
        FxValue oldValue = null;
        for (FxResultRow row : result.getResultRows()) {
            // check order
            assert oldValue == null || (ascending
                    ? row.getFxValue(column).compareTo(oldValue) >= 0
                    : row.getFxValue(column).compareTo(oldValue) <= 0)
                    : row.getFxValue(column) + " is not "
                    + (ascending ? "greater" : "less") + " than " + oldValue;
            oldValue = row.getFxValue(column);
        }
    }

}
