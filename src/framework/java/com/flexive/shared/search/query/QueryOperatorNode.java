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

import com.flexive.shared.FxSharedUtils;
import com.flexive.shared.exceptions.FxInvalidParameterException;
import com.flexive.shared.exceptions.FxNotFoundException;
import com.flexive.shared.exceptions.FxRuntimeException;

/**
 * A "operator" node representing a conjunctive/disjunctive union of its children. 
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class QueryOperatorNode extends QueryNode {
    private static final long serialVersionUID = -6443190344195963157L;
    private SqlQueryBuilder queryBuilder;

    /**
     * Node operators
     */
    public static enum Operator { 
        /** And operator */ 
        AND("AND"), 
        /** Or operator */ 
        OR("OR");

        private String sqlRepresentation;
        
        /**
         * Create a new operator.
         * 
         * @param sqlRepresentation the SQL representation of the operator
         */
        private Operator(String sqlRepresentation) {
            this.sqlRepresentation = sqlRepresentation;
        }

        public String getSqlRepresentation() {
            return sqlRepresentation;
        }
        
        public String getName() {
        	return name();
        }
        
        public Operator[] getValues() {
        	return values();
        }
    }
    
    private Operator operator = Operator.AND;

    /**
     * Default constructor.
     */
    public QueryOperatorNode() {
    }
    
    /**
     * Constructor.
     * @param id    the node ID
     */
    public QueryOperatorNode(int id) {
        super(id);
    }
    
    /**
     * Constructor.
     * @param id    the node ID.
     * @param operator  the operator value.
     */
    public QueryOperatorNode(int id, Operator operator) {
        FxSharedUtils.checkParameterEmpty(operator, "OPERATOR");
        this.id = id;
        this.operator = operator;
    }

    public Operator getOperator() {
        return operator;
    }
    
    public void setOperator(Operator operator) {
        FxSharedUtils.checkParameterEmpty(operator, "OPERATOR");
        this.operator = operator;
    }
    
    /** {@inheritDoc} */    
    @Override
    public void addChild(QueryNode child) {
        children.add(child);
        child.parent = this;
    }

    /** {@inheritDoc} */
    @Override
	public void addChild(QueryNode parentNode, QueryNode child) {
        parentNode.addChild(child);
	}


    /** {@inheritDoc} */
    @Override
    public void addChildAfter(QueryNode afterNode, QueryNode child) {
        for (int i = 0; i < children.size(); i++) {
            QueryNode queryNode = children.get(i);
            if (queryNode.equals(afterNode)) {
                children.add(i + 1, child);
                child.parent = this;
                return;
            }
        }
        throw new FxInvalidParameterException("CHILD", "ex.queryNode.child.notFound", child.getId()).asRuntimeException();
    }

    /** {@inheritDoc} */
    @Override
    public QueryNode removeChild(QueryNode child) {
    	QueryNode result = null;
        if (children.indexOf(child) != -1) {
            children.remove(child);
            result = this;
        } else {
            for (QueryNode node: children) {
                if (node instanceof QueryOperatorNode) {
                    QueryNode removedParent = node.removeChild(child);
                    result = removedParent != null ? removedParent : null;
                    if (result != null) {
                    	break;
                    }
                }
            }
        }
        if (children.isEmpty() && parent != null) {
        	// remove empty operator nodes
        	result = parent.removeChild(this);
        } else if (children.size() == 1 && parent != null) {
        	// only one child - attach it to the parent
        	parent.addChild(children.get(0));
        	children.remove(0);
        	result = parent.removeChild(this);
        }
        compactOperatorNodes();
        return result;
    }
    
    /**
     * Remove nested operator nodes without leaves. 
     * E.g. AND[OR[1,2]] --> OR[1,2]
     *
     * @return  true if the tree has been compacted
     */
    private boolean compactOperatorNodes() {
		if (children.size() == 1 && !children.get(0).isValueNode()) {
        	// we got only one operator node as a child - take its children and attach it to our node
	        QueryNode operatorChild = children.get(0);
        	for (QueryNode child: operatorChild.getChildren()) {
        		addChild(child);
        	}
        	// keep our operator
        	setOperator(((QueryOperatorNode) operatorChild).getOperator());
        	children.remove(operatorChild);
            return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
	public QueryNode getChild(int index) {
		return children.get(index);
	}
    
   
    /** {@inheritDoc} */
	@Override
	public QueryNode findChild(int childId) {
		if (this.id == childId) {
			return this;
		}
		for (QueryNode node: children) {
			if (node.getId() == childId) {
				return node;
			}
			if (node.getChildren().size() > 0) {
				try {
					return node.findChild(childId);
				} catch (FxRuntimeException e) {
					// try other children first
				}
			}
		}
		throw new FxNotFoundException("ex.queryNode.child.notFound", childId).asRuntimeException();
	}

	/** {@inheritDoc} */
    @Override
    protected String getNodeName() {
        return operator.name();
    }
    
    
	/** {@inheritDoc} */
    @Override
    public boolean isValid() {
        // the node is valid if all of its children are valid
        for (QueryNode child: children) {
            if (!child.isValid()) {
                return false;
            }
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void buildSqlQuery(SqlQueryBuilder builder) {
        if (builder.isFrozen()) {
            // do nothing if query already exists
            return;
        }
        if (children.isEmpty()) {
            return;
        }
        builder.enterSub(operator);
    	for (QueryNode child: children) {
    		child.buildSqlQuery(builder);
    	}
    	builder.closeSub();
    }

    /**
     * Return the SQL (sub-)query represented by this node and its children.
     * 
     * @return	the SQL (sub-)query represented by this node and its children.
     */
    public String getSqlQuery() {
    	SqlQueryBuilder builder = queryBuilder == null ? new SqlQueryBuilder() : queryBuilder;
    	buildSqlQuery(builder);
    	return builder.getQuery();
    }

    /** {@inheritDoc} */
	@Override
	public void visit(QueryNodeVisitor visitor) {
		visitor.visit(this);
		for (QueryNode child: children) {
			visitor.setCurrentParent(this);
			child.visit(visitor);
		}
	}

    public SqlQueryBuilder getQueryBuilder() {
        return queryBuilder;
    }

    public void setQueryBuilder(SqlQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }
}
