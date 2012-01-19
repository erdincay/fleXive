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

import com.flexive.shared.FxFormatUtils;
import com.flexive.shared.content.FxPK;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.search.query.VersionFilter;
import com.flexive.shared.structure.FxSelectList;
import com.flexive.shared.structure.FxSelectListEdit;
import com.flexive.shared.structure.FxSelectListItem;
import com.flexive.shared.value.*;
import com.flexive.sqlParser.*;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Date;

import static org.testng.Assert.*;

/**
 * Tests for the FxSQL parser. Checks various syntax features, but does not
 * execute real queries.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class SqlParserTest {

    @Test(groups = {"search"})
    public void emptyWhereClause() throws SqlParserException {
        parse("SELECT co.id FROM content co", "id");
        parse("SELECT id", "id");
        try {
            parse("SELECT co.id FROM content co WHERE", "id");
            Assert.fail("WHERE specified, but no conditions - expected failure");
        } catch (SqlParserException e) {
            // pass
        }
        try {
            parse("SELECT id WHERE", "id");
            Assert.fail("WHERE specified, but no conditions - expected failure");
        } catch (SqlParserException e) {
            // pass
        }
    }

    @Test(groups = {"shared", "search"})
    public void queryWithoutAliasTest() throws SqlParserException {
        parse("SELECT id", "id");
        parse("SELECT id WHERE id < 10", "id");
        parse("SELECT id WHERE id < 10 AND caption IS NOT NULL ORDER BY id", "id");
        parse("SELECT #type/property", "type/property");
        parse("SELECT #type/property WHERE #type/property > 0", "type/property");
    }

    @Test(groups = {"shared", "search"})
    public void nestedAssignmentTest() throws SqlParserException {
        parse("SELECT co.@pk, #co.ARTICLE/TEASER/TEASER_TITLE\n" +
                "FROM content co\n" +
                "WHERE #co.ARTICLE/TITLE LIKE 'Earthquake%'",
                "@pk",
                "ARTICLE/TEASER/TEASER_TITLE"
        );
    }

    @Test(groups = {"shared", "search"})
    public void contentTypeFilter() throws SqlParserException {
        final FxStatement stmt1 = parse("SELECT co.id FROM content co FILTER co.TYPE=21", "id");
        assertTrue(stmt1.getContentTypeFilter().equals("21"), "Content type filter was " + stmt1.getContentTypeFilter() + ", expected: 21");

        final FxStatement stmt2 = parse("SELECT co.id FROM content co FILTER co.TYPE=mytype", "id");
        assertTrue(stmt2.getContentTypeFilter().equalsIgnoreCase("mytype"), "Content type filter was " + stmt2.getContentTypeFilter() + ", expected: mytype");
    }

    @Test(groups = {"search"})
    public void versionFilter() throws SqlParserException {
        assertTrue(parse("SELECT co.id FROM content co FILTER co.VERSION=max").getVersionFilter().equals(VersionFilter.MAX));
        assertTrue(parse("SELECT co.id FROM content co FILTER co.VERSION=LIVE").getVersionFilter().equals(VersionFilter.LIVE));
        assertTrue(parse("SELECT co.id FROM content co FILTER co.VERSION=ALL").getVersionFilter().equals(VersionFilter.ALL));
        // auto gets the version through some user session magic, but it should definitely not return auto
        assertTrue(!parse("SELECT co.id FROM content co FILTER co.VERSION=AUTO").getVersionFilter().equals(VersionFilter.AUTO));
        try {
            parse("SELECT co.id FROM content co FILTER co.VERSION=15");
            Assert.fail("Specific versions cannot be selected.");
        } catch (SqlParserException e) {
            // pass
        }
    }

    @Test(groups = {"shared", "search"})
    public void ignoreCaseFilter() throws SqlParserException {
        assertTrue(parse("SELECT co.id FROM content co FILTER IGNORE_CASE=T").getIgnoreCase());
        assertTrue(parse("SELECT co.id FROM content co FILTER IGNORE_CASE=t").getIgnoreCase());
        assertTrue(parse("SELECT co.id FROM content co FILTER IGNORE_CASE=true").getIgnoreCase());
        assertTrue(!parse("SELECT co.id FROM content co FILTER IGNORE_CASE=F").getIgnoreCase());
        assertTrue(!parse("SELECT co.id FROM content co FILTER IGNORE_CASE=f").getIgnoreCase());
        assertTrue(!parse("SELECT co.id FROM content co FILTER IGNORE_CASE=false").getIgnoreCase());
    }

    @Test(groups = {"search"})
    public void maxResultRowsFilter() throws SqlParserException {
        assertTrue(parse("SELECT co.id FROM content co FILTER MAX_RESULTROWS=21").getMaxResultRows() == 21);
        assertTrue(parse("SELECT co.id FROM content co FILTER MAX_RESULTROWS=0").getMaxResultRows() == 0);
        try {
            parse("SELECT co.id FROM content co FILTER MAX_RESULTROWS=-1");
            Assert.fail("Negative values should not be allowed for filter value MAX_RESULTROWS.");
        } catch (SqlParserException e) {
            // pass
        }
    }

    @Test(groups = {"shared", "search"})
    public void searchLanguagesFilter() throws SqlParserException {
        assertTrue(parse("SELECT co.id FROM content co").getTableByAlias("co").getSearchLanguages().length == 0);
        assertTrue(parse("SELECT co.id FROM content co FILTER co.SEARCH_LANGUAGES=de").getTableByAlias("co").getSearchLanguages()[0].equals("de"));
        assertTrue(parse("SELECT co.id FROM content co FILTER co.SEARCH_LANGUAGES=de|en").getTableByAlias("co").getSearchLanguages()[0].equals("de"));
        assertTrue(parse("SELECT co.id FROM content co FILTER co.SEARCH_LANGUAGES=de|en").getTableByAlias("co").getSearchLanguages()[1].equals("en"));
    }

    @Test(groups = {"shared", "search"})
    public void briefcaseFilter() throws SqlParserException {
        assertTrue(parse("SELECT co.id FROM content co").getBriefcaseFilter().length == 0);
        assertTrue(parse("SELECT co.id FROM content co FILTER briefcase=1").getBriefcaseFilter()[0] == 1);
        assertTrue(parse("SELECT co.id FROM content co FILTER briefcase=1|21").getBriefcaseFilter()[0] == 1);
        assertTrue(parse("SELECT co.id FROM content co FILTER briefcase=1|21").getBriefcaseFilter()[1] == 21);
    }

    @Test(groups = {"shared", "search"})
    public void combinedBriefcaseFilters() throws SqlParserException {
        final FxStatement stmt = parse("SELECT co.id FROM content co \n" +
                "FILTER IGNORE_CASE=false, co.SEARCH_LANGUAGES=fr|it, briefcase=2|3,\n" +
                "       co.version=max, co.type=mine\n");
        assertTrue(!stmt.getIgnoreCase());
        assertTrue(stmt.getTableByAlias("co").getSearchLanguages()[0].equals("fr"));
        assertTrue(stmt.getTableByAlias("co").getSearchLanguages()[1].equals("it"));
        assertTrue(stmt.getBriefcaseFilter()[0] == 2);
        assertTrue(stmt.getBriefcaseFilter()[1] == 3);
    }

    @Test(groups = {"shared", "search"})
    public void basicConditionComparators() throws SqlParserException {
        for (PropertyValueComparator comp : PropertyValueComparator.values()) {
            final String query = "SELECT co.id FROM content co WHERE " + comp.getSql("co.property", "myvalue");
            try {
                parse(query);
            } catch (SqlParserException e) {
                Assert.fail("Failed to submit query with comparator " + comp + ":\n"
                        + query + "\n\nError message: " + e.getMessage());
            }
        }
    }

    @Test(groups = {"shared", "search"})
    public void nestedConditionComparators() {
        for (PropertyValueComparator comp : PropertyValueComparator.values()) {
            final Date date = new Date();
            final String query = "SELECT co.id FROM content co WHERE " + comp.getSql("co.p1", 1) + " AND ("
                    + comp.getSql("co.p2", "stringval") + " OR ("
                    + comp.getSql("co.p3", 2) + " AND " + comp.getSql("co.p4", date)
                    + "))";
            try {
                final FxStatement stmt = parse(query);

                // check root expression
                final Brace root = stmt.getRootBrace();
                assertTrue(root.isAnd(), "Root expression should be AND");
                assertTrue(root.getElements().length == 2, "Root should have two children, has: " + Arrays.asList(root.getElements()));
                final boolean inComp = comp == PropertyValueComparator.IN || comp == PropertyValueComparator.NOT_IN;
                checkStatementCondition(root.getElementAt(0), comp, "p1", makeTuple("1", inComp));

                // check first nested level
                final Brace level1 = (Brace) root.getElementAt(1);
                assertTrue(level1.isOr(), "Level 1 expression should be 'or'");
                assertTrue(level1.getElements().length == 2, "Level1 should have two children, has: " + Arrays.asList(level1.getElements()));
                checkStatementCondition(level1.getElementAt(0), comp, "p2", makeTuple(FxFormatUtils.escapeForSql("stringval"), inComp));

                // check innermost level
                final Brace level2 = (Brace) level1.getElementAt(1);
                assertTrue(level2.isAnd(), "Level 2 expression should be 'and'");
                assertTrue(level2.getElements().length == 2, "Level2 should have two children, has: " + Arrays.asList(level2.getElements()));
                checkStatementCondition(level2.getElementAt(0), comp, "p3", makeTuple("2", inComp));
                checkStatementCondition(level2.getElementAt(1), comp, "p4", makeTuple(FxFormatUtils.escapeForSql(date), inComp));
            } catch (Exception e) {
                Assert.fail("Failed to submit query with comparator " + comp + ":\n"
                        + query + "\n\nError message: " + e.getMessage());
            }
        }
    }

    private String makeTuple(String input, boolean enabled) {
        return enabled ? "(" + input + ")" : input;
    }

    @Test(groups = {"shared", "search"})
    public void dataTypeSupport() throws SqlParserException {
        final FxSelectListEdit selectList = new FxSelectList("test").asEditable();
        final FxSelectListItem item1 = new FxSelectListItem(25, "item1", selectList, -1, new FxString("label1"));
        final FxSelectListItem item2 = new FxSelectListItem(28, "item2", selectList, -1, new FxString("label2"));
        final FxValue[] testData = {
                new FxString("som'e test string"),
                new FxHTML("<h1>\"HTML c'aption\"</h1>"),
                new FxFloat(1.21f),
                new FxDouble(1.21),
                new FxDate(new Date()),
                new FxSelectOne(item1),
                new FxSelectMany(new SelectMany(selectList).select(item1.getId()).select(item2.getId())),
                new FxBoolean(false),
                new FxBoolean(true),
                new FxLargeNumber(2741824312312L),
                new FxNumber(21),
                new FxReference(new ReferencedContent(21, FxPK.MAX)),
                new FxReference(new ReferencedContent(21, FxPK.LIVE)),
                new FxReference(new ReferencedContent(21, 4))
        };
        for (FxValue value : testData) {
            final FxStatement stmt = parse("SELECT co.id FROM content co WHERE co.value = " + FxFormatUtils.escapeForSql(value));
            final Object conditionValue = ((Condition) stmt.getRootBrace().getElementAt(0)).getRValueInfo().getValue();
            assertTrue(StringUtils.isNotBlank(value.getSqlValue()));
            assertTrue(conditionValue.equals(value.getSqlValue()), "SQL condition value should be " + value.getSqlValue() + ", is: " + conditionValue);
        }
    }

    @Test(groups = {"shared", "search"})
    public void selectFunctions() throws SqlParserException {
        assertTrue(parse("SELECT minute(co.created_at) FROM content co").getSelectedValues().get(0)
                .getValue().getSqlFunctions().get(0).equalsIgnoreCase("minute"));
        final Value val1 = parse("SELECT co.id, year(month(day(co.created_at))) FROM content co").getSelectedValues().get(1).getValue();
        assertEquals(val1.getSqlFunctions(), Arrays.asList("YEAR", "MONTH", "DAY"),
                "Expected functions year, month, day; got: " + Arrays.asList(val1.getSqlFunctions()));
    }

    @Test(groups = {"search"})
    public void orderBy() throws SqlParserException {
        for (String valid : new String[]{
                "SELECT co.id FROM content co ORDER BY co.id, 1",
                "SELECT co.id FROM content co ORDER BY co.id ASC",
                "SELECT co.id FROM content co ORDER BY co.id",
                "SELECT co.id FROM content co ORDER BY 1",
                "SELECT co.id FROM content co ORDER BY 1 ASC, 1 DESC",
                "SELECT co.id FROM content co ORDER BY 1 DESC, 1 DESC"
        }) {
            final FxStatement stmt = parse(valid);
            assertEquals(stmt.getOrderByValues().get(0).getColumnIndex(), 0,
                    "Order by column index should be 0, was: " + stmt.getOrderByValues().get(0).getColumnIndex());
        }
        // some invalid queries
        for (String invalid : new String[]{
                "SELECT co.id FROM content co ORDER BY co.id2", // order by value not selected
                "SELECT co.id, co.ver FROM content co ORDER BY 3",
                "SELECT co.id, co.ver FROM content co ORDER BY co.1",
                "SELECT co.id, co.ver FROM content co ORDER BY co.id DESCC",
        }) {
            try {
                parse(invalid);
                Assert.fail("Query " + invalid + " is invalid.");
            } catch (SqlParserException e) {
                // pass
            }
        }
    }

    @Test(groups = {"shared", "search"})
    public void groupBy() throws SqlParserException {
        for (String valid : new String[]{
                "SELECT co.id FROM content co GROUP BY co.id",
                "SELECT co.id FROM content co GROUP BY co.id, co.id",
                "SELECT co.id FROM content co GROUP BY 1",
        }) {
            parse(valid);
            // TODO: add checks, since group by are recognized by the parser, but not evaluated
        }

        /*for (String invalid : new String[] {
                "SELECT co.id FROM content co GROUP BY co.id2",
                "SELECT co.id FROM content co GROUP BY 2",
        }) {
            try {
                parse(invalid);
                Assert.fail("Query " + invalid + " is invalid.");
            } catch (SqlParserException e) {
                // pass
            }
        }*/
    }

    @Test(groups = {"shared", "search"})
    public void queryComments() throws SqlParserException {
        parse("SELECT co.ID /* some comment */ FROM content -- line-comment\nco");
        parse("SELECT co.ID /* some \n multiline -- nested \n comment */ FROM content co");
        parse("SELECT co.ID FROM content co WHERE co.property /* some comment */ = /* some comment */ 21");
        parse("SELECT /* some \ncomment */ co.ID FROM /* some \n\ncomment */ content co -- another comment");
    }

    @Test(groups = {"shared", "search"})
    public void groupBraceNoReorg() throws SqlParserException {
        checkNoReorg("id > 0 AND id < 0", new Brace.GroupFunction() {
            public Object apply(Condition cond) {
                return 1;   //
            }
        });
        checkNoReorg("id > 0 AND (id < 0 OR id > 0)", new Brace.GroupFunction() {
            int ctr = 0;
            public Object apply(Condition cond) {
                return ctr++;   // all conditions will be in different groups
            }
        });
    }

    @Test(groups = {"shared", "search"})
    public void groupBraceSimple() throws SqlParserException {
        final FxStatement stmt = parse("SELECT id WHERE id > 0 and s01 = 'abc' AND s02 = 'def'");
        assertEquals(stmt.getRootBrace().getElements().length, 3);
        final Condition s01 = (Condition) stmt.getRootBrace().getElements()[1];
        final Condition s02 = (Condition) stmt.getRootBrace().getElements()[2];
        assertEquals(s01.getLValueInfo().getValue().toString(), "s01");
        assertEquals(s02.getLValueInfo().getValue().toString(), "s02");

        final Brace reorg = stmt.getRootBrace().groupConditions(new Brace.GroupFunction() {
            public Object apply(Condition cond) {
                // group s01 and s02
                return cond.getLValueInfo().getValue().toString().charAt(0) == 's' ? 0 : 1;
            }
        });
        assertNotSame(reorg, stmt.getRootBrace());
        assertEquals(reorg.getElements().length, 2);
        assertTrue("and".equalsIgnoreCase(reorg.getType()), "New condition type should be 'and':" + reorg.getType());

        final Brace sub = (Brace) reorg.getElements()[reorg.getElements()[0] instanceof Condition ? 1 : 0];
        assertEquals(sub.getElements().length, 2);
        assertTrue("and".equalsIgnoreCase(sub.getType()), "Subcondition type should be 'and' too:" + sub.getType());
        assertTrue(ArrayUtils.indexOf(sub.getElements(), s01) != -1, "s01 not found in subquery");
        assertTrue(ArrayUtils.indexOf(sub.getElements(), s02) != -1, "s02 not found in subquery");
    }

    private void checkNoReorg(String conditions, Brace.GroupFunction fun) throws SqlParserException {
        final FxStatement stmt = parse("SELECT id WHERE " + conditions);
        assertEquals(
                stmt.getRootBrace().groupConditions(fun),
                stmt.getRootBrace(),
                "No reorg should happend because all statements are in the same group"
        );
    }

    private void checkStatementCondition(BraceElement element, PropertyValueComparator comp, String lvalue, String rvalue) {
        assertTrue(element instanceof Condition, "First root child should be a condition, is: " + element);
        final Condition condition = (Condition) element;
        assertEquals(condition.getLValueInfo().getValue().toString().toUpperCase(), lvalue.toUpperCase());
        assertTrue(!comp.isNeedsInput() || !condition.getRValueInfo().isNull());
        assertTrue(!comp.isNeedsInput() || condition.getRValueInfo().getValue().equals(rvalue),
                "RValue should be " + rvalue + ", is: " + condition.getRValueInfo().getValue());
        assertTrue(!comp.isNeedsInput() || condition.getConstant().getValue().equals(rvalue));
    }

    /**
     * Parses the given query, performs basic validity checks based on the additional parameters
     * like selected columns, and returns the parsed statement.
     *
     * @param query           the FxSQL query
     * @param selectedColumns the selected columns, the returned statement will be checked to contain these columns
     * @return the parsed FxSQL statement
     * @throws com.flexive.sqlParser.SqlParserException
     *          on parser errors
     */
    private FxStatement parse(String query, String... selectedColumns) throws SqlParserException {
        final FxStatement stmt2 = FxStatement.parseSql(query);
        checkStatement(stmt2, selectedColumns);
        return stmt2;
    }

    /**
     * Parses the given query, performs basic validity checks and returns the parsed statement.
     *
     * @param query the FxSQL query
     * @return the parsed FxSQL statement
     * @throws SqlParserException on parser errors
     */
    private FxStatement parse(String query) throws SqlParserException {
        return parse(query, (String[]) null);
    }


    /**
     * Perform basic validity checks of common queries.
     *
     * @param statement       the statement to be checked
     * @param selectedColumns the selected column(s). If null, the corresponding tests will be skipped.
     * @return the statement
     */
    private FxStatement checkStatement(FxStatement statement, String[] selectedColumns) {
        assertTrue(statement.getTables().length == 1, "One table should be selected, got: " + statement.getTables().length);
        assertTrue(statement.getTableByType(Table.TYPE.CONTENT) != null, "No content table selected");
        assertTrue(statement.getParserExecutionTime() >= 0, "Parser execution time not set.");
        assertTrue(statement.getTableByAlias("co") != null);
        assertTrue(statement.getTableByAlias("co").getType().equals(Table.TYPE.CONTENT));
        if (selectedColumns != null) {
            for (int i = 0; i < statement.getSelectedValues().size(); i++) {
                final SelectedValue value = statement.getSelectedValues().get(i);
                assertEquals(value.getAlias().toUpperCase(), selectedColumns[i].toUpperCase(), "Unexpected column selected: " + value);
            }
        }
        return statement;
    }
}
