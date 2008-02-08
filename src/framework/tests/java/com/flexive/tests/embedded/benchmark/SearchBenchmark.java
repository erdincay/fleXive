package com.flexive.tests.embedded.benchmark;

import com.flexive.shared.EJBLookup;
import com.flexive.shared.FxLanguage;
import com.flexive.shared.search.query.SqlQueryBuilder;
import com.flexive.shared.search.FxResultSet;
import com.flexive.shared.value.FxString;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.tree.FxTreeNodeEdit;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Some benchmarks for the {@link com.flexive.shared.interfaces.SearchEngine}.
 *
 * @author Daniel Lichtenberger, UCS
 * @version $Rev$
 */
@Test(groups = "benchmark")
public class SearchBenchmark {

    public void selectTreePathsBenchmark() throws FxApplicationException {
        final int numNodes = 2000;
        long rootNode = -1;
        try {
            // create a lot of nodes
            long startCreateNode = System.currentTimeMillis();
            rootNode = EJBLookup.getTreeEngine().save(FxTreeNodeEdit.createNew("selectTreePathsBenchmark"));
            for (int i = 0; i < numNodes; i++) {
                final FxString label = new FxString(FxLanguage.ENGLISH, "English label " + i).setTranslation(FxLanguage.GERMAN, "Deutsches Label " + i);
                EJBLookup.getTreeEngine().save(FxTreeNodeEdit.createNew("test test test " + i)
                        .setParentNodeId(rootNode).setLabel(label));
                if (i % 100 == 99) {
                    FxBenchmarkUtils.logExecutionTime("createTreeNodes[" + (i - 99) + "-" + i + "]",
                            startCreateNode, 100, "tree node");
                    startCreateNode = System.currentTimeMillis();
                }
            }

            final List<FxTreeNode> children = EJBLookup.getTreeEngine().getTree(FxTreeMode.Edit, rootNode, 1).getChildren();
            assert children.size() == numNodes : "Expected " + numNodes + " children of our root node, got: " + children.size();

            // select the tree paths of all linked contents
            final SqlQueryBuilder builder = new SqlQueryBuilder().select("@pk", "@path").maxRows(numNodes).isChild(rootNode);
            final long startSearch = System.currentTimeMillis();
            final FxResultSet result = builder.getResult();
            FxBenchmarkUtils.logExecutionTime("selectTreePath", startSearch, numNodes, "row");
            assert result.getRowCount() == numNodes : "Expected " + numNodes + " rows, got: " + result.getRowCount();
        } finally {
            if (rootNode != -1) {
                EJBLookup.getTreeEngine().remove(FxTreeNodeEdit.createNew("").setId(rootNode), true, true);
            }
        }

    }
}
