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

import com.flexive.shared.value.FxString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A node in a search query expression.
 * 
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public abstract class QueryNode implements Serializable {
    private static final long serialVersionUID = 3912814336085148874L;
    protected int id = -1;
    protected QueryNode parent = null;
    protected List<QueryNode> children = new ArrayList<QueryNode>();

    /**
     * Default constructor
     */
    protected QueryNode() {
    }
    
    /**
     * Protected constructor.
     * @param id    the node ID
     */
    protected QueryNode(int id) {
        this.id = id;
    }
    
    /**
     * Return true if the node's value is valid, or false if it is not.
     *    
     * @return  true if the node's value is valid, or false if it is not.
     */
    public abstract boolean isValid();

    /**
     * Build the query represented by this node and its children.
     *
     * @param builder	an sql query builder
     */
    protected abstract void buildSqlQuery(SqlQueryBuilder builder);
    
    /**
     * Visit this node.
     * 
     * @param visitor	the visitor 
     */
    public abstract void visit(QueryNodeVisitor visitor);
    
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public QueryNode getParent() {
        return parent;
    }

    /**
     * Return the nesting level of the given node in the query tree,
     * i.e. the number of parent links one has to traverse to reach the root node.
     * A node attached to the root node has a level of 1.
     *
     * @return  the node level
     */
    public int getLevel() {
        return parent != null ? 1 + parent.getLevel() : 0;
    }

    /**
     * Set the parent of this node. If the node is not included
     * in the parent's children list, it is added automatically.
     * 
     * @param parent    the parent to be sed
     */
    public void setParent(QueryNode parent) {
        if (this.parent != null) {
            // detach from previous parent
            this.parent.getChildren().remove(this);
        }
        this.parent = parent;
        if (parent != null) {
            if (parent.getChildren().indexOf(this) == -1) {
                // add as child to new parent
                parent.addChild(this);
            }
        }
    }
    
    public List<QueryNode> getChildren() {
        return children;
    }
    
    public void setChildren(List<QueryNode> children) {
        this.children = children;
    }
    
    /**
     * Add a new child node after the last child. 
     * @param child The child node to be added
     */
    public void addChild(QueryNode child) {
        throw new UnsupportedOperationException("Child nodes not supported for this node type: " + getClass().getCanonicalName());
    }

    /**
     * Add a new child to the given parent node. The parent node may be
     * the current node, or any of its immediate or transitive child nodes.
     * 
     * @param parentNode The parent node where the child should be inserted
     * @param child The child node to be added
     */
    public void addChild(QueryNode parentNode, QueryNode child) {
    	throw new UnsupportedOperationException("Child nodes not supported for this node type: " + getClass().getCanonicalName());
    }

    /**
     * Add a child after the given node. The after node may be
     * an immediate or transitive child of the current node.
     * 
     * @param afterNode		node after which the child should be inserted
     * @param child		child node to be added
     */
    public void addChildAfter(QueryNode afterNode, QueryNode child) {
    	throw new UnsupportedOperationException("Child nodes not supported for this node type: " + getClass().getCanonicalName());
    }

    /**
     * Remove a child node. Also searches all subtrees if the node
     * is node an immediate child.
     * @param child the child to be deleted
     * @return the parent node of the deleted child, or null if the child does not exist
     */
    public QueryNode removeChild(QueryNode child) {
        throw new UnsupportedOperationException("Child nodes not supported for this node type: " + getClass().getCanonicalName());
    }

    /**
     * Return the child node at the given index.
     * 
     * @param index	the child index in this parent's children list
     * @return	the child node at the given index.
     */
    public QueryNode getChild(int index) {
    	throw new UnsupportedOperationException("Child nodes not supported for this node type: " + getClass().getCanonicalName());
    }

    /**
     * Return the child node with the given ID.
     * 
     * @param childId	the requested child node ID
     * @return		the child node with the given ID
     */
    public QueryNode findChild(int childId) {
    	throw new UnsupportedOperationException("Child nodes not supported for this node type: " + getClass().getCanonicalName());
    }

    /*
    protected void _writeTreeString(Appendable out) throws IOException, FxInvalidParameterException {
        if (this.getClass().isAnonymousClass() || this.getClass().getEnclosingClass() != null) {
            throw new FxInvalidParameterException("NODE", "ex.search.query.invalidClass=", 
                    this.getClass().getCanonicalName());
        }
        out.append("<node id=\"").append(String.valueOf(id))
            .append("\" class=\"").append(this.getClass().getCanonicalName()).append("\"");
        if (children.isEmpty()) {
            out.append("/>");
        } else {
            for (QueryNode child: children) {
                child._writeTreeString(out);
            }
            out.append("</node>");
        }
    }
    */
    
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object anotherNode) {
        return anotherNode instanceof QueryNode && ((QueryNode) anotherNode).getId() == this.id;
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.id;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        if (children.size() == 0) {
            return this.getNodeName();
        } else {
            StringBuilder out = new StringBuilder();
            out.append(getNodeName()).append("[");
            int ctr = 0;
            for (QueryNode child: children) {
                if (ctr++ > 0) {
                    out.append(',');
                }
                out.append(child.toString());
            }
            out.append("]");
            return out.toString();
        }
    }
    
    /**
     * Return the informal representation of a node to be used in toString.
     * 
     * @return  the informal representation of a node to be used in toString.
     */
    protected String getNodeName() {
        return String.valueOf(id);
    }

    /**
     * Return the label to be displayed for this query node.
     *
     * @return  the label to be displayed for this query node.
     */
    public FxString getLabel() {
        return new FxString(getNodeName());
    }
    
    /**
     * Return true if this node is a "value" node, i.e. is used to represent
     * a scalar value in the query. Used for JSTL node handling.
     * 
     * @return	true for scalar value nodes
     */
    public boolean isValueNode() {
    	return false;
    }
    
    /**
     * Returns true if the node and its children should not be displayed in a
     * query editor or report.
     *  
     * @return		true if the node and its children should not be displayed 
     */
    public boolean isHidden() {
    	return false;
    }

    /**
     * Returns true if the node needs a wide input field, e.g. a datepicker
     * or file download field.
     *
     * @return  true if the node needs a wide input field  
     */
    public boolean isWideInput() {
        return false;
    }
    
    /**
     * Returns the complete node path of the current node, i.e. the node ids
     * traversed to the root node separated by dashes ("-").
     * e.g.
     * 1.path --> 1
     *  |- 2.path --> 1-2
     *    |- 3.path --> 1-2-3
     * 
     * @return  the complete node path of the current node
     */
    public String getPath() {
    	if (parent == null) {
    		return String.valueOf(id);
    	} else {
    		return parent.getPath() + "-" + String.valueOf(id);
    	}
    }

}
