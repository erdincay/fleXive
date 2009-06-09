/***************************************************************
 *  This file is part of the [fleXive](R) framework.
 *
 *  Copyright (c) 1999-2008
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
package com.flexive.faces.beans;

import com.flexive.faces.FxJsfUtils;
import com.flexive.faces.messages.FxFacesMsgErr;
import com.flexive.faces.messages.FxFacesMsgInfo;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.interfaces.SearchEngine;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.exceptions.FxInvalidQueryNodeException;
import com.flexive.shared.exceptions.FxRuntimeException;
import com.flexive.shared.search.AdminResultLocations;
import com.flexive.shared.search.ResultLocation;
import com.flexive.shared.search.ResultViewType;
import com.flexive.shared.search.query.*;
import static com.flexive.shared.search.query.QueryRootNode.Type.CONTENTSEARCH;
import com.flexive.shared.structure.FxAssignment;
import com.flexive.shared.structure.FxType;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.shared.value.FxLargeNumber;
import com.flexive.shared.value.FxString;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.faces.event.ActionEvent;
import javax.faces.context.FacesContext;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Search query editor beans.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class QueryEditorBean implements Serializable {
    private static final Log LOG = LogFactory.getLog(QueryEditorBean.class);
    private static final long serialVersionUID = -7734399826904382438L;
    private static final String SESSION_QUERY = "query";
    private static final String SESSION_TIMEOUT = "timeout";
    private static final String SESSION_MAXRESULTS = "maxResults";

    /**
     * JSF root component containing the query editor
     */
    private static final String RESET_COMPONENT_ID = "frm:queryEditor";

    private List<FxAssignment> properties = null;
    private QueryRootNode rootNode = null;
    private long addAssignmentId;
    private int addAssignmentNodeId = -1;
    private boolean addNodeLive = false;
    private int removeNodeId = -1;
    private String nodeSelection = null;
    private boolean reloadSearchPanel = false;
    private int queryTimeout = SearchEngine.DEFAULT_QUERY_TIMEOUT;
    private int maxResults = 2000;

    private SqlQueryBuilder queryBuilder;
    private ResultLocation location = AdminResultLocations.ADMIN;
    private String filterTypeName;

    public QueryEditorBean() {
        parseRequestParameters();
    }

    /**
     * Parse the request parameters and perform actions as requested.
     * Works only if the QueryEditorBean remains request-scoped!
     */
    private void parseRequestParameters() {
        try {
            String action = FxJsfUtils.getParameter("action");
            if (StringUtils.isBlank(action)) {
                // no action requested
                return;
            }
            if ("nodeSearch".equals(action)) {
                // create a new query with the node set as "nodeId"
                setRootNode(new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH, location));
                addAssignmentId = FxJsfUtils.getLongParameter("nodeId", FxTreeNode.ROOT_NODE);
                addNodeLive = FxJsfUtils.getBooleanParameter("liveMode", false);
                addTreeNode(null);
            } else if ("typeSearch".equals(action)) {
                setRootNode(new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH, location));
                addAssignmentId = FxJsfUtils.getLongParameter("typeId", -1);
                if (addAssignmentId != -1) {
                    addTypeQuery(null);
                }
            } else if ("assignmentSearch".equals(action)) {
                setRootNode(new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH, location));
                addAssignmentId = FxJsfUtils.getLongParameter("assignmentId", -1);
                if (addAssignmentId != -1) {
                    addAssignment(null);
                }
            } else if ("propertySearch".equals(action)) {
                setRootNode(new QueryRootNode(QueryRootNode.Type.CONTENTSEARCH, location));
                addAssignmentId = FxJsfUtils.getLongParameter("propertyId", -1);
                if (addAssignmentId != -1) {
                    addProperty(null);
                }
            } else if ("new".equals(action)) {
                // create a new search query
                getRootNode().getChildren().clear();
                getRootNode().setName(null);
            } else if ("load".equals(action)) {
                setRootNode(EJBLookup.getSearchEngine().load(AdminResultLocations.ADMIN,
                        FxJsfUtils.getParameter("name")));
            } else if ("loadSystemDefault".equals(action)) {
                setRootNode(EJBLookup.getSearchEngine().loadSystemDefault(AdminResultLocations.ADMIN));
            }
            updateQueryStore();
            FxJsfUtils.resetFaceletsComponent(RESET_COMPONENT_ID);
        } catch (Exception e) {
            LOG.error("Failed to parse request parameters: " + e.getMessage(), e);
        }
    }

    /**
     * Show the query editor, restore last query (TODO).
     *
     * @return the final page
     */
    public String show() {
        return "contentQuery";
    }

    /**
     * Execute the current search query.
     *
     * @return the outcome
     */
    public String executeSearch() {
        final SearchResultBean resultBean = (SearchResultBean) FxJsfUtils.getManagedBean("fxSearchResultBean");
        final SqlQueryBuilder builder = getQueryBuilder();
        if (!buildQuery(builder)) {
            return null;
        }
        resultBean.setQueryBuilder(builder);
        resultBean.resetFilters();
        updateQueryStore();
        return resultBean.show();
    }

    private boolean buildQuery(SqlQueryBuilder builder) {
        if (StringUtils.isNotBlank(filterTypeName)) {
            builder.filterType(filterTypeName);
        }
        if (rootNode == null || rootNode.getChildren().size() == 0) {
            new FxFacesMsgErr("QueryEditor.err.emptyQuery").addToContext();
            return false;
        }
        try {
            rootNode.buildSqlQuery(builder);
        } catch (FxRuntimeException e) {
            if (e.getConverted() instanceof FxInvalidQueryNodeException) {
                final FxInvalidQueryNodeException queryNodeException = (FxInvalidQueryNodeException) e.getConverted();
                // add error message for node component
                final FxFacesMsgErr msg = new FxFacesMsgErr(queryNodeException);
                msg.setId(String.valueOf(queryNodeException.getTreeNodeId()));
                msg.addToContext();
            } else {
                new FxFacesMsgErr("QueryEditor.err.buildQuery", e).addToContext();
            }
            return false;
        } catch (Exception e) {
            new FxFacesMsgErr("QueryEditor.err.buildQuery", e).addToContext();
            return false;
        }
        builder.timeout(queryTimeout);
        builder.maxRows(maxResults);
        return true;
    }

    /**
     * Sets the current user's default query for the current location.
     */
    public void saveDefault() {
        /*if (!buildQuery(new SqlQueryBuilder())) {
            return;
        }*/
        try {
            EJBLookup.getSearchEngine().saveDefault(rootNode);
            new FxFacesMsgInfo("QueryEditor.nfo.saveDefault").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("QueryEditor.err.saveSystemDefault", e).addToContext();
        }
    }

    /**
     * Saves the current query.
     */
    public void saveQuery() {
        if (StringUtils.isBlank(rootNode.getName())) {
            new FxFacesMsgErr("QueryEditor.err.saveQuery.empty").addToContext("queryName");
            return;
        }
        /*if (!buildQuery(new SqlQueryBuilder())) {
            return;
        }*/
        try {
            getRootNode().setName(rootNode.getName());
            EJBLookup.getSearchEngine().save(getRootNode());
            reloadSearchPanel = true;
            new FxFacesMsgInfo("QueryEditor.nfo.saveQuery", rootNode.getName()).addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr(e).addToContext();
        }
    }

    /**
     * Save the system default query (only global supervisors may call this).
     */
    public void saveSystemDefault() {
        if (!buildQuery(new SqlQueryBuilder())) {
            return;
        }
        try {
            EJBLookup.getSearchEngine().saveSystemDefault(rootNode);
            new FxFacesMsgInfo("QueryEditor.nfo.saveSystemDefault").addToContext();
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("QueryEditor.err.saveSystemDefault", e).addToContext();
        }
    }

    /**
     * Loads the system-wide default query.
     */
    public void loadSystemDefault() {
        try {
            setRootNode(EJBLookup.getSearchEngine().loadSystemDefault(location));
        } catch (FxApplicationException e) {
            new FxFacesMsgErr("QueryEditor.err.loadSystemDefault", e).addToContext();
        }
    }

    /**
     * Add the (property) assignment stored in addAssignmentId
     * to the node identified by addAssignmentNodeId.
     *
     * @param event the action event
     */
    public void addAssignment(ActionEvent event) {
        addQueryNode(new AssignmentValueNode(getRootNode().getNewId(), addAssignmentId));
    }

    /**
     * Add the property stored in addAssignmentId to the node identified
     * by addAssignmentNodeId.
     * @param event the action event
     */
    public void addProperty(ActionEvent event) {
        addQueryNode(new PropertyValueNode(getRootNode().getNewId(), addAssignmentId));
    }

    /**
     * Add the tree node stored in addAssignmentId to the node identified by
     * addAssignmentNodeId.
     *
     * @param event the action event
     * @throws FxApplicationException on errors
     */
    public void addTreeNode(ActionEvent event) throws FxApplicationException {
        final FxTreeNode treeNode = EJBLookup.getTreeEngine().getNode(FxTreeMode.Edit, addAssignmentId);
        final TreeValueNode newNode = new TreeValueNode(getRootNode().getNewId(), treeNode.getId(),
                addNodeLive ? FxTreeMode.Live : FxTreeMode.Edit,
                treeNode.getLabel() != null && !treeNode.getLabel().isEmpty()
                        ? treeNode.getLabel() : new FxString(false, treeNode.getName()));
        addQueryNode(newNode);
    }

    /**
     * Add a query node that searches only for types of the given type ID.
     *
     * @param event the action event
     */
    public void addTypeQuery(ActionEvent event) {
        final FxType type = CacheAdmin.getEnvironment().getType(addAssignmentId);
        final AssignmentValueNode node = new AssignmentValueNode(getRootNode().getNewId(), CacheAdmin.getEnvironment().getAssignment("ROOT/TYPEDEF").getId());
        node.setValue(new FxLargeNumber(false, type.getId()));
        addQueryNode(node);
    }

    public void addQueryNode(QueryNode newNode) {
        QueryRootNode root = getRootNode();
        QueryNode targetNode;
        QueryNode addAssignmentNode = null;
        try {
            targetNode = addAssignmentNodeId != -1 ? root.findChild(addAssignmentNodeId).getParent() : root;
            addAssignmentNode = addAssignmentNodeId != -1 && addAssignmentNodeId != root.getId()
                    ? root.findChild(addAssignmentNodeId) : null;
        } catch (FxRuntimeException e) {
            // node not found
            targetNode = root;
        }

        if (addAssignmentNode != null) {
            targetNode.addChildAfter(addAssignmentNode, newNode);
        } else {
            targetNode.addChild(newNode);
        }
        //}
        addAssignmentNodeId = newNode.getId();
        FxJsfUtils.resetFaceletsComponent(RESET_COMPONENT_ID);
        updateQueryStore();
    }

    /**
     * Remove the node identified by removeNodeId from the current query.
     *
     * @param event the action event
     */
    public void removeNode(ActionEvent event) {
        QueryNode removeNode = getRootNode().findChild(removeNodeId);
        if (removeNode != null && removeNode.getParent() != null) {
            removeNode.getParent().removeChild(removeNode);
            FxJsfUtils.resetFaceletsComponent(RESET_COMPONENT_ID);
            updateQueryStore();
        }
    }

    /**
     * Join the selected nodes with 'and'.
     *
     * @param event the action event
     */
    public void createAndSubquery(ActionEvent event) {
        joinSelectedNodes(QueryOperatorNode.Operator.AND);
    }

    /**
     * Join the selected nodes with 'or'.
     *
     * @param event the action event
     */
    public void createOrSubquery(ActionEvent event) {
        joinSelectedNodes(QueryOperatorNode.Operator.OR);
    }

    private void joinSelectedNodes(QueryOperatorNode.Operator operator) {
        Scanner scanner = new Scanner(nodeSelection).useDelimiter(",");
        List<Integer> nodeIds = new ArrayList<Integer>();
        while (scanner.hasNextInt()) {
            nodeIds.add(scanner.nextInt());
        }
        if (nodeIds.size() > 1) {
            getRootNode().joinNodes(nodeIds, operator);
        }
        FxJsfUtils.resetFaceletsComponent(RESET_COMPONENT_ID);
        updateQueryStore();
    }

    public List<FxAssignment> getProperties() {
        if (properties == null) {
            properties = new ArrayList<FxAssignment>();
        }
        return properties;
    }

    public void setProperties(List<FxAssignment> properties) {
        this.properties = properties;
    }

    public long getAddAssignmentId() {
        return addAssignmentId;
    }

    public void setAddAssignmentId(long addPropertyId) {
        this.addAssignmentId = addPropertyId;
    }

    public QueryRootNode getRootNode() {
        if (rootNode == null) {
            rootNode = (QueryRootNode) sessionGet(SESSION_QUERY);
        }
        if (rootNode == null) {
            try {
                rootNode = EJBLookup.getSearchEngine().loadDefault(location);
            } catch (FxApplicationException e) {
                rootNode = new QueryRootNode(CONTENTSEARCH, location);
            }
        }
        return rootNode;
    }

    public void setRootNode(QueryRootNode rootNode) {
        this.rootNode = rootNode;
        updateQueryStore();
    }

    public int getAddAssignmentNodeId() {
        return addAssignmentNodeId;
    }

    public void setAddAssignmentNodeId(int addAssignmentNodeId) {
        this.addAssignmentNodeId = addAssignmentNodeId;
    }

    public int getRemoveNodeId() {
        return removeNodeId;
    }

    public void setRemoveNodeId(int removeNodeId) {
        this.removeNodeId = removeNodeId;
    }

    public String getNodeSelection() {
        return nodeSelection;
    }

    public void setNodeSelection(String nodeSelection) {
        this.nodeSelection = nodeSelection;
    }

    /**
     * Stores the current query in the user session.
     */
    private void updateQueryStore() {
        if (rootNode != null) {
            sessionPut(SESSION_QUERY, rootNode);
        }
    }

    public ResultLocation getLocation() {
        return location;
    }

    public void setLocation(ResultLocation location) {
        this.location = location;
    }

    public String getFilterTypeName() {
        return filterTypeName;
    }

    public void setFilterTypeName(String filterTypeName) {
        this.filterTypeName = filterTypeName;
    }

    public SqlQueryBuilder getQueryBuilder() {
        if (queryBuilder == null) {
            queryBuilder = new SqlQueryBuilder(location, ResultViewType.LIST);
        }
        return queryBuilder;
    }

    public void setQueryBuilder(SqlQueryBuilder queryBuilder) {
        this.queryBuilder = queryBuilder;
    }

    public boolean isAddNodeLive() {
        return addNodeLive;
    }

    public void setAddNodeLive(boolean addNodeLive) {
        this.addNodeLive = addNodeLive;
    }

    public int getQueryTimeout() {
        final Object sessionTimeout = sessionGet(SESSION_TIMEOUT);
        if (sessionTimeout != null) {
            queryTimeout = (Integer) sessionTimeout;
        }
        return queryTimeout;
    }

    public void setQueryTimeout(int queryTimeout) {
        this.queryTimeout = queryTimeout;
        sessionPut(SESSION_TIMEOUT, queryTimeout);
    }

    public int getMaxResults() {
        final Object sessionMaxResults = sessionGet(SESSION_MAXRESULTS);
        if (sessionMaxResults != null) {
            maxResults = (Integer) sessionMaxResults;
        }
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
        sessionPut(SESSION_MAXRESULTS, maxResults);
    }

    /**
     * Return the current tab title of the query tab.
     *
     * @return  the current tab title of the query tab.
     */
    public String getTabTitle() {
        if (StringUtils.isBlank(getRootNode().getName())) {
            return MessageBean.getInstance().getMessage("QueryEditor.tabtitle.query");
        }
        return MessageBean.getInstance().getMessage("QueryEditor.tabtitle.loadedQuery", getRootNode().getName());
    }
    
    /**
     * Returns the session attribute key for storing the current query
     *
     * @return the session attribute key for storing the current query
     */
    private String getQueryTreeStore(String key) {
        return "FlexiveSearchQueryTree/" + location + "/" + QueryRootNode.Type.CONTENTSEARCH
                + "/" + key;
    }

    private Object sessionGet(String key) {
        return FxJsfUtils.getSessionAttribute(getQueryTreeStore(key));
    }

    private void sessionPut(String key, Object value) {
        FxJsfUtils.setSessionAttribute(getQueryTreeStore(key), value);
    }


    public boolean isReloadSearchPanel() {
        return reloadSearchPanel;
    }
}
