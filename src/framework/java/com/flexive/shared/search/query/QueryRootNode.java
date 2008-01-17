/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.shared.search.query;

import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.search.AdminResultLocations;
import com.flexive.shared.search.ResultLocation;

import java.util.List;

/**
 * The root node of a query, containing additional information
 * like its name or its type.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class QueryRootNode extends QueryOperatorNode {
    private static final long serialVersionUID = 4606502650296335222L;

    /**
     * There are substantally different query types.
     * Currently only content searches are implemented.
     */
    public static enum Type {
        /** Content search query node */
        CONTENTSEARCH
    }

    private String name;
    private final Type type;
    private final ResultLocation location;

    /**
     * Create a new, empty query with the given parameters.
     *
     * @param id    the node ID
     * @param type  the query type
     * @param location  the query form location (usually matches the search result location)
     */
    public QueryRootNode(int id, Type type, ResultLocation location) {
        super(id, Operator.AND);
        this.type = type;
        this.location = location;
    }

    /**
     * Create a new, empty query of the given type.
     * 
     * @param type  the query type
     */
    public QueryRootNode(Type type) {
        this(-1, type, AdminResultLocations.DEFAULT);
    }

    /**
     * Create a new, empty query with the given parameters.
     *
     * @param type  the query type
     * @param location  the query form location (usually matches the search result location)
     */
    public QueryRootNode(Type type, ResultLocation location) {
        this(-1, type, location);
    }

    /**
     * Create a new, empty query of the given type with the given root node id.
     * 
     * @param id    the node ID
     * @param type  the query type
     */
    public QueryRootNode(int id, Type type) {
        this(id, type, AdminResultLocations.DEFAULT);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public ResultLocation getLocation() {
        return location;
    }

    /**
     * Return a fresh node ID not used in the tree.
     *  
     * @return	a new node ID that can be used for attaching a new node to the tree
     */
    public int getNewId() {
        MaxNodeIdVisitor visitor = new MaxNodeIdVisitor();
        visit(visitor);
        return visitor.getMaxId() + 1;
    }

    /**
     * Join the given nodes in a new sub hierarchy with the given operator.
     * The new subquery is inserted in the tree level of the highest node.
     *
     * @param nodeIds   the nodes to be joined
     * @param operator  the operator used for joining (and/or)
     * @return          the new operator node
     */
    public QueryOperatorNode joinNodes(List<Integer> nodeIds, Operator operator) {
        if (nodeIds == null || nodeIds.size() <= 1) {
            throw new FxInvalidParameterException("NODEIDS", "ex.queryNode.join.nodeCount").asRuntimeException();
        }
        QueryNode parent = null;
        QueryOperatorNode joinNode = new QueryOperatorNode(getNewId(), operator);
        // find common parent node
        for (int nodeId: nodeIds) {
            final QueryNode node = findChild(nodeId);
            if (!node.isValueNode()) {
                throw new FxInvalidParameterException("NODEIDS", "ex.queryNode.join.value").asRuntimeException();
            }
            if (parent == null) {
                parent = node.getParent();
            } else {
                // find lowest common ancestor (LCA)
                QueryNode nodeParent = node.getParent();
                // reach equal depth for both parent node paths
                while (nodeParent.getLevel() > parent.getLevel()) {
                    nodeParent = nodeParent.getParent();
                }
                while (parent.getLevel() > nodeParent.getLevel()) {
                    parent = parent.getParent();
                }
                // move upwards until we reach the same node
                while (nodeParent != parent) {
                    nodeParent = nodeParent.getParent();
                    parent = parent.getParent();
                }
                // parent now points to the LCA for the nodes processed so far
            }
        }
        parent.addChild(joinNode);
        // move children
        QueryNode currentJoinRoot = null;
        for (int nodeId: nodeIds) {
            final QueryNode child = findChild(nodeId);
            removeChild(child);
            currentJoinRoot = currentJoinRoot != null ? findChild(nodeIds.get(0)).getParent() : joinNode;
            currentJoinRoot.addChild(child);
        }
        // nodes may have been reattached to another operator node than joinNode, so get the parent from our first child
        return (QueryOperatorNode) findChild(nodeIds.get(0)).getParent();
    }


}
