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
package com.flexive.core.search.cmis.parser;

import com.flexive.core.search.cmis.model.*;
import com.flexive.core.storage.ContentStorage;
import com.flexive.core.storage.TreeStorage;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.cmis.CmisVirtualProperty;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxCmisSqlParseException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.search.SortDirection;
import com.flexive.shared.search.query.PropertyValueComparator;
import com.flexive.shared.structure.FxDataType;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import org.antlr.runtime.tree.Tree;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.flexive.shared.exceptions.FxCmisSqlParseException.ErrorCause;

/**
 * Translates a CMIS SQL AST to a {@link Statement}.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 * @since 3.1
 */
class StatementBuilder {
    private static final Log LOG = LogFactory.getLog(StatementBuilder.class);

    private final Tree root;
    private final Statement stmt = new Statement();
    private final Connection con;
    private final ContentStorage storage;
    private final TreeStorage treeStorage;
    private final FxEnvironment environment;

    StatementBuilder(Connection con, ContentStorage storage, TreeStorage treeStorage, Tree root) {
        this.root = root;
        this.con = con;
        this.storage = storage;
        this.treeStorage = treeStorage;
        this.environment = CacheAdmin.getEnvironment();
    }

    public Statement getStatement() {
        return stmt;
    }

    public Statement build() throws FxCmisSqlParseException {
        if (LOG.isTraceEnabled()) {
            LOG.trace("Evaluating tree: " + root.toStringTree());
        }
        assert root.getType() == CmisSqlLexer.STATEMENT;

        try {
            processFrom(getFirstChildWithType(root, CmisSqlLexer.FROM));
            processSelect(getFirstChildWithType(root, CmisSqlLexer.SELECT));
            processWhere(getFirstChildWithType(root, CmisSqlLexer.WHERE));
            processOrderBy(getFirstChildWithType(root, CmisSqlLexer.ORDER));
        } catch (FxRuntimeException e) {
            throw new FxCmisSqlParseException(LOG, e.getConverted());
        }

        return stmt;
    }

    protected void processSelect(Tree selectNode) throws FxCmisSqlParseException {
        assert selectNode.getType() == CmisSqlLexer.SELECT;
        assert !stmt.getTables().isEmpty() : "No tables selected, or FROM clause not processed.";

        for (Tree child : children(selectNode)) {
            switch (child.getType()) {
                case CmisSqlLexer.ANYREF:
                    for (ValueExpression valueExpression : processAnyRef(child)) {
                        stmt.addSelectedColumn(valueExpression);
                    }
                    break;
                default:
                    stmt.addSelectedColumn(processValueExpression(child));
                    break;
            }

        }
    }

    protected void processFrom(Tree fromNode) throws FxCmisSqlParseException {
        assert fromNode.getType() == CmisSqlLexer.FROM;
        for (Tree child : children(fromNode)) {
            stmt.addTable(decodeFromTableReference(child));
        }
    }

    private TableReference decodeFromTableReference(Tree node) throws FxCmisSqlParseException {
        switch (node.getType()) {
            case CmisSqlLexer.TREF:
                return decodeTableReference(node);
            case CmisSqlLexer.JOIN:
                // add table references
                final Tree onNode = getFirstChildWithType(node, CmisSqlLexer.ON);
                final TableReference table1 = decodeFromTableReference(node.getChild(0));
                final TableReference table2 = decodeFromTableReference(node.getChild(1));
                // decode "ON" node, which will reference the tables just added, so we temporarily
                // add them to the statement to avoid replicating the column reference decoder
                stmt.addTable(table1);
                stmt.addTable(table2);
                final JoinedTableReference tableJoin = new JoinedTableReference(
                        table1, table2,
                        decodeColumnReference(onNode.getChild(0), false),
                        decodeColumnReference(onNode.getChild(1), false)
                );
                stmt.removeTable(table1);
                stmt.removeTable(table2);
                return tableJoin;
            default:
                throw new UnsupportedOperationException("Invalid node type for 'FROM' table reference: " + node.getType());
        }
    }

    private SingleTableReference decodeTableReference(Tree node) {
        assert node.getType() == CmisSqlLexer.TREF;
        final Tree aliasDef = getFirstChildWithType(node, CmisSqlLexer.TALIASDEF);
        final String tableName = decodeIdent(node.getChild(0));
        return aliasDef == null
                ? new SingleTableReference(environment, tableName)
                : new SingleTableReference(environment, tableName, decodeIdent(aliasDef.getChild(0)));
    }

    protected void processWhere(Tree whereNode) throws FxCmisSqlParseException {
        if (whereNode == null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Empty where node.");
            }
            return;
        }
        assert whereNode.getType() == CmisSqlLexer.WHERE;
        stmt.setRootCondition(processCondition(null, whereNode.getChild(0)));
    }

    protected Condition processCondition(ConditionList parent, Tree root) throws FxCmisSqlParseException {
        switch (root.getType()) {
            case CmisSqlLexer.AND:
            case CmisSqlLexer.OR:
                final ConditionList.Connective type = root.getType() == CmisSqlLexer.AND
                        ? ConditionList.Connective.AND
                        : ConditionList.Connective.OR;
                // re-use parent list to avoid nesting expressions with the same operator
                final ConditionList list = parent != null && parent.getConnective().equals(type)
                        ? parent
                        : new ConditionList(parent, type);
                // add nested conditions to the list
                for (Tree node : children(root)) {
                    final Condition cond = processCondition(list, node);
                    if (cond != list) {
                        list.addCondition(cond);
                    }
                }
                return list;

            case CmisSqlLexer.COMPOP:
                final Tree compNode = root.getChild(0);
                final String compOp = compNode.getText();
                final ValueExpression lhs = processValueExpression(compNode.getChild(0));
                return new ComparisonCondition(
                        parent,
                        lhs,
                        PropertyValueComparator.forComparison(compOp),
                        processLiteral(
                                compNode.getChild(1),
                                lhs instanceof ColumnReference ? (ColumnReference) lhs : null
                        )
                );

            case CmisSqlLexer.CONTAINS:
                assert root.getChild(0).getType() == CmisSqlLexer.CHARLIT;  // contains search expression
                final String qualifier = root.getChildCount() > 1 ? decodeIdent(root.getChild(1).getChild(0)) : null;
                final String expression = decodeCharLiteral(root.getChild(0));
                if (qualifier == null && stmt.getTables().size() > 1) {
                    throw new FxCmisSqlParseException(LOG, ErrorCause.AMBIGUOUS_CONTAINS, expression);
                }

                return new ContainsCondition(
                        parent,
                        qualifier != null ? stmt.getTable(qualifier) : stmt.getTables().get(0),
                        qualifier,
                        expression
                );

            case CmisSqlLexer.LIKE:
                return new LikeCondition(
                        parent,
                        decodeColumnReference(getFirstChildWithType(root, CmisSqlLexer.CREF), false),
                        decodeCharLiteral(getFirstChildWithType(root, CmisSqlLexer.CHARLIT)),
                        getFirstChildWithType(root, CmisSqlLexer.NOT) != null
                );

            case CmisSqlLexer.IN:
                final Tree inCref = getFirstChildWithType(root, CmisSqlLexer.CREF);
                final ColumnReference inColumnReference = decodeColumnReference(inCref, false);
                final List<Literal> inValues = new ArrayList<Literal>(root.getChildCount() - 1);
                for (int i = inCref.getChildIndex() + 1; i < root.getChildCount(); i++) {
                    inValues.add(processLiteral(root.getChild(i), inColumnReference));
                }
                return new InCondition(
                        parent,
                        inColumnReference,
                        inValues,
                        getFirstChildWithType(root, CmisSqlLexer.NOT) != null
                );

            case CmisSqlLexer.NULL:
                return new NullCondition(
                        parent,
                        decodeColumnReference(getFirstChildWithType(root, CmisSqlLexer.CREF), false),
                        getFirstChildWithType(root, CmisSqlLexer.NOT) != null
                );

            case CmisSqlLexer.IN_FOLDER:
            case CmisSqlLexer.IN_TREE:
                // optional table qualifier
                final TableReference folderQualifier = root.getChildCount() > 1 
                        ? stmt.getTable(decodeIdent(root.getChild(1).getChild(0)))
                        : null;
                if (folderQualifier == null && stmt.getTableCount() > 1) {
                    // table must be specified for JOINs
                    throw new FxCmisSqlParseException(
                            LOG,
                            ErrorCause.AMBIGUOUS_TABLE_REF,
                            (root.getType() == CmisSqlLexer.IN_FOLDER ? "IN_FOLDER" : "IN_TREE")
                    );
                }
                final TableReference sourceTable = folderQualifier == null ? stmt.getTables().get(0) : folderQualifier;

                // find folder - TODO: Edit/Live tree selection?
                final String folderIdent = decodeCharLiteral(root.getChild(0));
                final long folderId;
                try {
                    if (StringUtils.isNumeric(folderIdent)) {
                        // folder ID as used by the CMIS interfaces
                        folderId = Long.parseLong(folderIdent);
                        // check if the folder is actually valid
                        treeStorage.getTreeNodeInfo(con, FxTreeMode.Edit, folderId);
                    } else if (folderIdent != null && folderIdent.startsWith("/")) {
                       // lookup folder path
                        folderId = treeStorage.getIdByFQNPath(con, FxTreeMode.Edit, FxTreeNode.ROOT_NODE, folderIdent);
                        if (folderId == -1) {
                            throw new FxCmisSqlParseException(
                                    LOG,
                                    ErrorCause.INVALID_NODE_PATH,
                                    folderIdent
                            );
                        }
                    } else {
                        throw new IllegalArgumentException("No folder ID or path for tree condition");
                    }

                    return root.getType() == CmisSqlLexer.IN_FOLDER
                            // IN_FOLDER
                            ? new FolderCondition(parent, sourceTable, folderId)
                            // IN_TREE (needs node info for bounds checks)
                            : new TreeCondition(parent, sourceTable, treeStorage.getTreeNodeInfo(con, FxTreeMode.Edit, folderId));
                    
                } catch (FxApplicationException e) {
                    throw new FxCmisSqlParseException(LOG, e);
                }
            default:
                throw new UnsupportedOperationException("Unsupported condition: " + root.toStringTree());
        }
    }

    protected ValueExpression processValueExpression(Tree node) throws FxCmisSqlParseException {
        switch (node.getType()) {
            case CmisSqlLexer.CREF:
                return decodeColumnReference(node, false);
            case CmisSqlLexer.FUNCTION:
                return processFunction(node);
            default:
                throw new UnsupportedOperationException("Unsupported value expression: " + node.toStringTree());
        }
    }

    protected List<ValueExpression> processAnyRef(Tree node) {
        assert node.getType() == CmisSqlLexer.ANYREF;

        // find out the target tables of "*"
        final List<TableReference> tables = new ArrayList<TableReference>();
        final Tree tableReferenceNode = getFirstChildWithType(node, CmisSqlLexer.TREF);
        if (tableReferenceNode != null) {
            tables.add(
                    stmt.getTable(decodeIdent(tableReferenceNode.getChild(0)))
            );
        } else {
            if (stmt.getTables().size() > 1) {
                throw new IllegalArgumentException("'*' without table alias, but with more than one source table");
            }
            tables.addAll(stmt.getTables());
        }

        final List<ValueExpression> result = new ArrayList<ValueExpression>();
        for (TableReference rootTable : tables) {
            for (TableReference table : rootTable.getLeafTables()) {
                final FxType type = table.getRootType();

                // add all properties assigned to the root type
                for (FxPropertyAssignment assignment : type.getAssignedProperties()) {
                    if (!assignment.isSystemInternal() && !FxDataType.Binary.equals(assignment.getProperty().getDataType())) {
                        result.add(
                                new ColumnReference(
                                        environment, storage,
                                        table,
                                        assignment.getAlias().toLowerCase(),
                                        null,
                                        false
                                )
                        );
                    }
                }

                // add the main binary property
                if (type.getMainBinaryAssignment() != null) {
                    result.add(
                            new ColumnReference(
                                    environment, storage,
                                    table,
                                    type.getMainBinaryAssignment().getAlias().toLowerCase(),
                                    null,
                                    false
                            )
                    );
                }
            }
        }

        // add all CMIS system properties
        for (CmisVirtualProperty property : CmisVirtualProperty.values()) {
            if (property.isSupportsQuery()) {
                result.add(
                        new ColumnReference(
                            environment, storage,
                            tables.get(0),
                            property.getCmisPropertyName(),
                            null,
                            false
                        )
                );
            }
        }

        return result;
    }

    protected ValueFunction processFunction(Tree node) throws FxCmisSqlParseException {
        assert node.getType() == CmisSqlLexer.FUNCTION;
        final Tree funNode = node.getChild(0);
        final String alias = getOptionalTextNode(node, CmisSqlLexer.CALIAS);
        switch (funNode.getType()) {
            case CmisSqlLexer.UPPER:
                return new StringValueFunction(decodeColumnReference(node.getChild(1), true), "upper", alias);
            case CmisSqlLexer.LOWER:
                return new StringValueFunction(decodeColumnReference(node.getChild(1), true), "lower", alias);
            case CmisSqlLexer.SCORE:
                return new NumericValueFunction("score", alias);
            default:
                throw new UnsupportedOperationException("Unsupported function: " + funNode.toStringTree());
        }
    }

    protected ColumnReference decodeColumnReference(Tree node, boolean useUpperCaseFilter) throws FxCmisSqlParseException {
        assert node.getType() == CmisSqlLexer.CREF;

        // a normal column (property) reference
        final Tree tableRef = getFirstChildWithType(node, CmisSqlLexer.TREF);
        final boolean multiValued = node.getChild(0).getType() == CmisSqlLexer.ANY;
        final String column = decodeIdent(node.getChild(multiValued ? 1 : 0));

        // table alias must be set unless only one table was selected
        if (tableRef == null && stmt.getTables().size() > 1) {
            throw new FxCmisSqlParseException(LOG, ErrorCause.AMBIGUOUS_COLUMN_REF, column);
        }

        final ColumnReference result = new ColumnReference(
                environment,
                storage,
                tableRef != null
                        ? stmt.getTable(decodeIdent(tableRef.getChild(0)))
                        : stmt.getTables().get(0),
                column,
                getOptionalTextNode(node, CmisSqlLexer.CALIAS),
                useUpperCaseFilter
        );
        if (multiValued && !result.isMultivalued()) {
            throw new FxCmisSqlParseException(LOG, ErrorCause.EXPECTED_MVREF, result.getAlias());
        }
        return result;
    }


    /**
     * Process a literal node. The parser separates literals in numeric literals
     * ({@link com.flexive.core.search.cmis.parser.CmisSqlLexer#CHARLIT})
     * and character literals
     * ({@link com.flexive.core.search.cmis.parser.CmisSqlLexer#NUMLIT}).
     * The optional column reference is used for determining the value range of numeric literals,
     * otherwise Double is used.
     *
     * @param node      the literal node
     * @param forColumn an optional column reference that specifies the value range for numeric literals
     * @return a literal instance
     */
    protected Literal processLiteral(Tree node, ColumnReference forColumn) {
        final String text = node.getChild(0).getText();
        switch (node.getType()) {
            case CmisSqlLexer.CHARLIT:
                // strip quotes
                return new Literal<String>(decodeCharLiteral(node));
            case CmisSqlLexer.NUMLIT:
                if (forColumn == null) {
                    return new Literal<Number>(Double.parseDouble(text));
                } else {
                    // determine numeric type of column
                    switch (forColumn.getPropertyEntry().getProperty().getDataType()) {
                        case Number:
                            return new Literal<Integer>(Integer.parseInt(text));
                        case LargeNumber:
                        case Reference:
                        case SelectOne:
                        case SelectMany:
                            return new Literal<Long>(Long.parseLong(text));
                        case Double:
                            return new Literal<Double>(Double.parseDouble(text));
                        case Float:
                            return new Literal<Float>(Float.parseFloat(text));
                        default:
                            throw new UnsupportedOperationException("Unsupported numeric literal type: "
                                    + forColumn.getPropertyEntry().getProperty().getDataType()
                            );
                    }
                }

            default:
                throw new UnsupportedOperationException("Unsupported literal node: " + node.toStringTree());
        }
    }

    protected String decodeIdent(Tree node) {
        assert node.getType() == CmisSqlLexer.IDENTIFIER;
        final String text = node.getChild(0).getText();
        return text.startsWith("\"") && text.endsWith("\"")
                ? text.substring(1, text.length() - 1)
                : text;
    }

    protected String decodeCharLiteral(Tree node) {
        assert node.getType() == CmisSqlLexer.CHARLIT;
        final String text = node.getChild(0).getText();
        return text.substring(1, text.length() - 1);
    }


    protected void processOrderBy(Tree orderByNode) {
        if (orderByNode == null) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Empty order by node.");
            }
            return;
        }
        assert orderByNode.getType() == CmisSqlLexer.ORDER;
        for (Tree sortSpec : children(orderByNode)) {
            assert sortSpec.getType() == CmisSqlLexer.SORTSPEC;
            assert sortSpec.getChildCount() >= 1;
            final String columnName = decodeIdent(sortSpec.getChild(0));
            final SortDirection direction =
                    sortSpec.getChildCount() > 1
                            && (sortSpec.getChild(1).getType() == CmisSqlLexer.DESC)
                            ? SortDirection.DESCENDING : SortDirection.ASCENDING;
            stmt.addOrderByColumn(columnName, direction);
        }
    }

    private String getOptionalTextNode(Tree node, int type) {
        final Tree child = getFirstChildWithType(node, type);
        if (child != null && child.getChildCount() == 0) {
            throw new IllegalArgumentException("Text node of type " + type + " exists, but has no children.");
        }
        return child != null
                ? (child.getChild(0).getType() == CmisSqlLexer.IDENTIFIER
                ? decodeIdent(child.getChild(0))
                : child.getChild(0).getText())
                : null;
    }


    private static Iterable<Tree> children(Tree node) {
        return new ChildIterable(node);
    }

    private static Tree getFirstChildWithType(Tree node, int type) {
        for (Tree child : children(node)) {
            if (child.getType() == type) {
                return child;
            }
        }
        return null;
    }

    private static final class ChildIterable implements Iterable<Tree> {
        private final Tree tree;

        private ChildIterable(Tree tree) {
            this.tree = tree;
        }

        public Iterator<Tree> iterator() {
            return new Iterator<Tree>() {
                int index = 0;

                public boolean hasNext() {
                    return index < tree.getChildCount();
                }

                public Tree next() {
                    return tree.getChild(index++);
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
