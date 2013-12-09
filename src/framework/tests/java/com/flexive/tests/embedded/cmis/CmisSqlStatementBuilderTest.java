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
package com.flexive.tests.embedded.cmis;

import com.flexive.core.Database;
import com.flexive.core.search.cmis.model.*;
import com.flexive.core.storage.ContentStorage;
import com.flexive.core.storage.StorageManager;
import com.flexive.core.storage.TreeStorage;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxCmisSqlParseException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.TypeStorageMode;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.tree.FxTreeRemoveOp;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static com.flexive.core.search.cmis.model.ConditionList.Connective;
import static com.flexive.core.search.cmis.parser.CmisSqlUtils.buildStatement;
import static org.testng.Assert.*;

/**
 * Tests for the CMIS SQL statement builder. Disabled tests are expected to fail because they
 * test for features that have not been implemented yet.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
public class CmisSqlStatementBuilderTest {
    private ContentStorage storage;
    private TreeStorage treeStorage;
    private Connection con;

    @BeforeClass(groups = {"search", "cmis"})
    public void beforeClass() throws FxNotFoundException, SQLException {
        storage = StorageManager.getContentStorage(TypeStorageMode.Hierarchical);
        treeStorage = StorageManager.getTreeStorage();
        con = Database.getDbConnection();
    }

    @AfterClass(groups = {"search", "cmis"})
    public void afterClass() throws SQLException {
        if (con != null) {
            con.close();
        }
    }

    @Test(groups = {"search", "cmis"})
    public void singleTableSingleColumn() throws FxCmisSqlParseException {
        for (String query : new String[] {
                "SELECT d.name FROM contactData AS d",
                "SELECT name FROM contactData AS d",
        }) {
            final Statement stmt = build(query);
            assertEquals(stmt.getTables().get(0).getAlias(), "d", "Invalid table alias");
            assertEquals(stmt.getTables().get(0).getReferencedTypes().get(0).getName(), "CONTACTDATA");

            final Selectable col = stmt.getSelectedColumns().get(0);
            final FxPropertyAssignment ass = col.getBaseAssignment();
            assertTrue(ass.getXPath().endsWith("/NAME"), "Invalid assignment: " + ass.getXPath());
            assertEquals(col.getTableReference(), stmt.getTables().get(0));
        }
    }

    @Test(groups = {"search", "cmis"})
    public void singleCondition() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT d.name FROM contactData AS d WHERE d.name = 'Pete'");
        final ComparisonCondition cond = (ComparisonCondition) stmt.getRootCondition();
        assertEquals(cond.getRhs().getValue(), "Pete");
    }

    @Test(groups = {"search", "cmis"})
    public void singleDisjunction() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT d.name FROM contactData AS d WHERE d.name = 'Pete' OR d.name = 'Pope'");
        new CheckConditionList(
                Connective.OR,
                new CheckComparisonCondition(PropertyValueComparator.EQ, new Literal<String>("Pete")),
                new CheckComparisonCondition(PropertyValueComparator.EQ, new Literal<String>("Pope"))
        ).check(getRootConditionList(stmt));
    }

    @Test(groups = {"search", "cmis"})
    public void multiDisjunction() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT d.name FROM contactData AS d " +
                "WHERE d.name = 'Pete' OR d.name = 'Pope' OR d.name = 'Jack'");
        new CheckConditionList(
                Connective.OR,
                new CheckComparisonCondition(PropertyValueComparator.EQ, new Literal<String>("Pete")),
                new CheckComparisonCondition(PropertyValueComparator.EQ, new Literal<String>("Pope")),
                new CheckComparisonCondition(PropertyValueComparator.EQ, new Literal<String>("Jack"))
        ).check(getRootConditionList(stmt));
    }

    @Test(groups = {"search", "cmis"})
    public void nestedConditions() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT s.stringSearchProp FROM SearchTest AS s " +
                "WHERE s.stringSearchProp = 'Pete' AND (s.numberSearchProp > 10" +
                " OR (s.largeNumberSearchProp > 25 AND s.doubleSearchProp < 15))");
        new CheckConditionList(
                Connective.AND,
                new CheckComparisonCondition(PropertyValueComparator.EQ, new Literal<String>("Pete")),
                new CheckConditionList(
                        ConditionList.Connective.OR,
                        new CheckComparisonCondition(PropertyValueComparator.GT, new Literal<Integer>(10)),
                        new CheckConditionList(
                                ConditionList.Connective.AND,
                                new CheckComparisonCondition(PropertyValueComparator.GT, new Literal<Long>(25L)),
                                new CheckComparisonCondition(PropertyValueComparator.LT, new Literal<Double>(15.0))
                        )
                )
        ).check(getRootConditionList(stmt));
    }

    @Test(groups = {"search", "cmis"})
    public void simpleStringFunction() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT UPPER(d.name) FROM contactData AS d");
        final Selectable col1 = stmt.getSelectedColumns().get(0);
        assertEquals(col1.getClass(), StringValueFunction.class);
        final StringValueFunction fun = (StringValueFunction) col1;
        assertEquals(fun.getFunction(), ValueFunction.Functions.UPPER);
        final String xpath = fun.getColumnReference().getBaseAssignment().getXPath();
        assertTrue(xpath.endsWith("/NAME"), "Invalid XPath: " + xpath);
    }

    @Test(groups = {"search", "cmis"})
    public void scoreFunction() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT SCORE() AS x FROM contactData WHERE CONTAINS('x')");
        final NumericValueFunction col1 = (NumericValueFunction) stmt.getSelectedColumns().get(0);
        assertEquals(col1.getFunction(), ValueFunction.Functions.SCORE);
        assertEquals(col1.getAlias(), "x");
    }

    @Test(groups = {"search", "cmis"})
    public void scoreFunctionDefaultAlias() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT SCORE() FROM contactData WHERE CONTAINS('peter')");
        final NumericValueFunction col1 = (NumericValueFunction) stmt.getSelectedColumns().get(0);
        assertEquals(col1.getFunction(), ValueFunction.Functions.SCORE);
        assertEquals(col1.getAlias(), "SEARCH_SCORE");  // default alias from CMIS spec
    }

    @Test(groups = {"search", "cmis"}, enabled = false)
    public void anyCond() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT d.name FROM contactData AS d WHERE 'Loewengasse' = ANY d.street");
    }

    @Test(groups = {"search", "cmis"})
    public void basicJoin() throws FxCmisSqlParseException {
        final Statement stmt = buildStatement(con, storage, treeStorage,
                "SELECT p1.name, p2.name FROM contactData AS p1 JOIN contactData AS p2 ON (p1.surname=p2.surname)"
        );
        assertTrue(stmt.getTables().size() == 1, "JOINed table should be the only root table");
        final Iterator<TableReference> iter = stmt.getTables().get(0).getLeafTables().iterator();
        assertEquals(iter.next().getAlias(), "p1");
        assertEquals(iter.next().getAlias(), "p2");
    }

    @Test(groups = {"search", "cmis"})
    public void columnAlias() throws FxCmisSqlParseException {
        final Statement stmt = buildStatement(con, storage, treeStorage,
                "SELECT p1.name AS myname FROM contactData AS p1"
        );
        assertEquals(stmt.getSelectedColumns().get(0).getAlias(), "myname", "Column alias not set correctly");
    }

    @Test(groups = {"search", "cmis"})
    public void implicitColumnAlias() throws FxCmisSqlParseException {
        final Statement stmt = buildStatement(con, storage, treeStorage,
                "SELECT p1.name FROM contactData AS p1"
        );
        assertEquals(stmt.getSelectedColumns().get(0).getAlias(), "name", "Implicit column alias not set correctly");
    }

    @Test(groups = {"search", "cmis"})
    public void fulltextCondition() throws FxCmisSqlParseException {
        final Statement stmt = buildStatement(con, storage, treeStorage,
                "SELECT longtext FROM article WHERE CONTAINS('summer')"
        );
        assertTrue(stmt.getRootCondition() != null);
    }

    @Test(groups = {"search", "cmis"})
    public void basicOrderBy() throws FxCmisSqlParseException {
        testOrderByColumn("name", "name", SortDirection.ASCENDING);
        testOrderByColumn("name ASC", "name", SortDirection.ASCENDING);
        testOrderByColumn("name DESC", "name", SortDirection.DESCENDING);
        try {
            buildStatement(con, storage, treeStorage,
                    "SELECT c.name FROM contactData AS c ORDER BY c.notSelected"
            );
            assertTrue(false, "Order by unselected column should not be allowed");
        } catch (FxCmisSqlParseException e) {
            // pass
        }
    }

    @Test(groups = {"search", "cmis"})
    public void likeCondition() throws FxCmisSqlParseException {
        final Condition cond = build("SELECT name FROM contactData WHERE name LIKE 'Supervisor%'").getRootCondition();
        checkLikeCondition(cond, "Supervisor%", false);
    }

    @Test(groups = {"search", "cmis"})
    public void notLikeCondition() throws FxCmisSqlParseException {
        final Condition cond = build("SELECT name FROM contactData WHERE name NOT LIKE 'Supervisor%'").getRootCondition();
        checkLikeCondition(cond, "Supervisor%", true);
    }

    @Test(groups = {"search", "cmis"})
    public void numericIn() throws FxCmisSqlParseException {
        final Condition root = build("SELECT name FROM contactData WHERE id IN (5, 10, 25)").getRootCondition();
        checkInCondition(root, false, new Literal<Long>(5L), new Literal<Long>(10L), new Literal<Long>(25L));
    }

    @Test(groups = {"search", "cmis"})
    public void stringIn() throws FxCmisSqlParseException {
        final Condition root = build("SELECT name FROM contactData WHERE surname IN ('Allen', 'Jobs')").getRootCondition();
        checkInCondition(root, false, new Literal<String>("Allen"), new Literal<String>("Jobs"));
    }

    @Test(groups = {"search", "cmis"})
    public void notIn() throws FxCmisSqlParseException {
        final Condition root = build("SELECT name FROM contactData WHERE id NOT IN (5, 10, 25)").getRootCondition();
        checkInCondition(root, true, new Literal<Long>(5L), new Literal<Long>(10L), new Literal<Long>(25L));
    }

    @Test(groups = {"search", "cmis"})
    public void quantifiedIn() throws FxCmisSqlParseException {
        final Condition root = build("SELECT name FROM cmis_person WHERE ANY email IN ('root@localhost', 'daniel@localhost')").getRootCondition();
        checkInCondition(root, false, new Literal<String>("root@localhost"), new Literal<String>("daniel@localhost"));
    }

    @Test(groups = {"search", "cmis"})
    public void quantifiedNotIn() throws FxCmisSqlParseException {
        final Condition root = build("SELECT name FROM cmis_person WHERE ANY email NOT IN ('root@localhost', 'daniel@localhost')").getRootCondition();
        checkInCondition(root, true, new Literal<String>("root@localhost"), new Literal<String>("daniel@localhost"));
    }

    @Test(groups = {"search", "cmis"})
    public void isNull() throws FxCmisSqlParseException {
        final Condition root = build("SELECT id FROM contactData WHERE name IS NULL").getRootCondition();
        checkNullCondition(root, false);
    }

    @Test(groups = {"search", "cmis"})
    public void isNotNull() throws FxCmisSqlParseException {
        final Condition root = build("SELECT id FROM contactData WHERE name IS NOT NULL").getRootCondition();
        checkNullCondition(root, true);
    }

    @Test(groups = {"search", "cmis"})
    public void selectInvalidDerivedAssignment() throws FxCmisSqlParseException {
        build("SELECT astring FROM cmis_type_a");       // pass
        build("SELECT astring, bstring FROM cmis_type_b");  // pass
        try {
            build("SELECT astring, bstring FROM cmis_type_a");
            assertTrue(false, "Cannot select derived assignment from base type");
        } catch (FxCmisSqlParseException e) {
            // pass
        }
    }

    @Test(groups = {"search", "cmis"})
    public void selectMultivalued() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT email FROM cmis_person");
        assertTrue(stmt.getSelectedColumns().get(0).isMultivalued(), "Expected multivalued column: " + stmt.getSelectedColumns().get(0));
    }

    @Test(groups = {"search", "cmis"})
    public void selectStar() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT * from cmis_person");
        assertNamePropertiesSelected(stmt);
    }

    @Test(groups = {"search", "cmis"})
    public void selectQualifiedStar() throws FxCmisSqlParseException {
        final Statement stmt = build("SELECT p.* from cmis_person AS p");
        assertNamePropertiesSelected(stmt);
    }

    @Test(groups = {"search", "cmis"})
    public void folderCondition() throws FxCmisSqlParseException, FxApplicationException {
        final long nodeId = EJBLookup.getTreeEngine().createNodes(FxTreeMode.Edit, FxTreeNode.ROOT_NODE, -1, "/test")[0];
        try {
            for (String fun : new String[] { "IN_FOLDER", "IN_TREE" }) {
                build("SELECT * FROM cmis_person WHERE " + fun + "('/test')");
                build("SELECT * FROM cmis_person WHERE " + fun + "('" + nodeId + "')");
                build("SELECT * FROM cmis_person WHERE " + fun + "(cmis_person, '/test')");
                build("SELECT * FROM cmis_person AS p WHERE " + fun + "(p, '/test')");
                build("SELECT p1.* FROM cmis_person AS p1 JOIN cmis_person AS p2 ON (p1.name=p2.name) WHERE " + fun + "(p1, '/test')");
                try {
                    build("SELECT * FROM cmis_person WHERE " + fun + "(p, '/test')");
                    fail("Invalid table reference not caught");
                } catch (FxCmisSqlParseException e) {
                    // pass
                }
                try {
                    build("SELECT p1.* FROM cmis_person AS p1 JOIN cmis_person AS p2 ON (p1.name=p2.name) WHERE " + fun + "('/test')");
                    fail("Queries with join must specify table reference for " + fun);
                } catch (FxCmisSqlParseException e) {
                    // pass
                }
            }
        } finally {
            EJBLookup.getTreeEngine().remove(FxTreeMode.Edit, nodeId, FxTreeRemoveOp.Remove, true);
        }
    }

    private void assertNamePropertiesSelected(Statement stmt) {
        assertTrue(stmt.getSelectedColumns().size() >= 3);
        assertEquals(stmt.getSelectedColumns().get(0).getAlias(), "name");
        assertEquals(stmt.getSelectedColumns().get(1).getAlias(), "ssn");
        assertEquals(stmt.getSelectedColumns().get(2).getAlias(), "email");
    }


    private Statement build(String query) throws FxCmisSqlParseException {
        return buildStatement(con, storage, treeStorage, query);
    }

    private void testOrderByColumn(String orderBy, String expectedName, SortDirection expectedSortDirection) throws FxCmisSqlParseException {
        final Statement stmt = buildStatement(con, storage, treeStorage,
                "SELECT c.name FROM contactData AS c ORDER BY " + orderBy
        );
        assertEquals(stmt.getOrderByColumns().size(), 1, "Expected one ORDER BY column");
        assertEquals(stmt.getOrderByColumns().get(0).getColumn().getAlias(), expectedName);
        assertEquals(stmt.getOrderByColumns().get(0).getDirection(), expectedSortDirection);
    }

    private ConditionList getRootConditionList(Statement stmt) {
        assertEquals(stmt.getRootCondition().getClass(), ConditionList.class);
        return (ConditionList) stmt.getRootCondition();
    }

    private void checkLikeCondition(Condition cond, String text, boolean negated) {
        assertTrue(cond != null);
        assertEquals(cond.getClass(), LikeCondition.class);
        final LikeCondition like = (LikeCondition) cond;
        assertEquals(like.getValue(), text);
        assertEquals(like.isNegated(), negated);
    }

    private void checkInCondition(Condition root, boolean negated, Literal<?>... values) {
        assertTrue(root != null);
        assertEquals(root.getClass(), InCondition.class);
        final InCondition cond = (InCondition) root;
        assertEquals(cond.isNegated(), negated);
        assertEquals(cond.getValues(), Arrays.asList(values));
    }

    private void checkNullCondition(Condition root, boolean negated) {
        assertEquals(root.getClass(), NullCondition.class);
        final NullCondition condition = (NullCondition) root;
        assertEquals(condition.isNegated(), negated);
    }


    private abstract static class ConditionChecker<T extends Condition> {
        protected final ConditionChecker[] children;
        private final Class<T> expectedClass;

        protected ConditionChecker(Class<T> expectedClass, ConditionChecker... children) {
            this.children = children;
            this.expectedClass = expectedClass;
        }

        public void check(T condition) {
            assertEquals(condition.getClass(), expectedClass);
        }
    }

    private static class CheckConditionList extends ConditionChecker<ConditionList> {
        private final ConditionList.Connective type;

        private CheckConditionList(ConditionList.Connective type, ConditionChecker... children) {
            super(ConditionList.class, children);
            this.type = type;
        }

        @SuppressWarnings({"unchecked"})
        @Override
        public void check(ConditionList condition) {
            super.check(condition);
            assertEquals(condition.getConnective(), type);
            final List<Condition> subConditions = condition.getConditions();
            assertEquals(subConditions.size(), children.length, "Child checkers do not match conditions");
            for (int i = 0; i < children.length; i++) {
                children[i].check(subConditions.get(i));
            }
        }
    }

    private static class CheckComparisonCondition extends ConditionChecker<ComparisonCondition> {
        private final PropertyValueComparator comparator;
        private final Literal value;

        private CheckComparisonCondition(PropertyValueComparator comparator,
                                         Literal value) {
            super(ComparisonCondition.class);
            this.comparator = comparator;
            this.value = value;
        }

        @Override
        public void check(ComparisonCondition condition) {
            super.check(condition);
            assertEquals(condition.getComparator(), comparator);
            assertEquals(condition.getRhs(), value);
        }
    }
}
