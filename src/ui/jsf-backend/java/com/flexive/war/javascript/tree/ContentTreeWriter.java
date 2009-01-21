/***************************************************************
 *  This file is part of the [fleXive](R) backend application.
 *
 *  Copyright (c) 1999-2008
 *  UCS - unique computing solutions gmbh (http://www.ucs.at)
 *  All rights reserved
 *
 *  The [fleXive](R) backend application is free software; you can redistribute
 *  it and/or modify it under the terms of the GNU General Public
 *  License as published by the Free Software Foundation;
 *  either version 2 of the License, or (at your option) any
 *  later version.
 *
 *  The GNU General Public License can be found at
 *  http://www.gnu.org/licenses/gpl.html.
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
package com.flexive.war.javascript.tree;

import com.flexive.faces.RequestRelativeUriMapper;
import com.flexive.faces.components.tree.dojo.DojoTreeRenderer;
import com.flexive.faces.javascript.tree.TreeNodeWriter;
import static com.flexive.faces.javascript.tree.TreeNodeWriter.Node;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.configuration.SystemParameters;
import com.flexive.shared.exceptions.FxApplicationException;
import com.flexive.shared.tree.FxTreeMode;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.war.JsonWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.*;

/**
 * Renders the content tree for the current user, either in the live
 * or edit version.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class ContentTreeWriter implements Serializable {
    private static final long serialVersionUID = -8810546022515615892L;
    private static final Log LOG = LogFactory.getLog(ContentTreeWriter.class);

    private static final String DOCTYPE_NODE = "ContentNode";
    private static final String DOCTYPE_CONTENT = "ContentObject";

    /**
     * Render the content tree beginning at the given node up to maxDepth levels deep.
     * To be called via JSON-RPC-Java.
     *
     * @param request     the current request, auto-variable set by JSON-RPC-Java
     * @param startNodeId the start node for the content tree (1 or null to get the whole tree)
     * @param maxDepth    the maximum depth to be rendered
     * @param liveTree    true if the live tree should be rendered, false for the edit tree
     * @param pathMode    true if node labels should be paths instead of content captions
     * @return the resulting tree
     */
    public String renderContentTree(HttpServletRequest request, Long startNodeId, int maxDepth, boolean liveTree, boolean pathMode) {
        StringWriter localWriter = null;
        try {
            // if embedded in a tree component, use the component's tree writer
            TreeNodeWriter writer = (TreeNodeWriter) request.getAttribute(DojoTreeRenderer.PROP_NODEWRITER);
            if (writer == null) {
                // otherwise return the tree nodes in the response
                localWriter = new StringWriter();
                writer = new TreeNodeWriter(localWriter, new RequestRelativeUriMapper(request), TreeNodeWriter.FORMAT_CONTENTTREE);
            }
            writeContentTree(writer, startNodeId != null ? startNodeId : FxTreeNode.ROOT_NODE, maxDepth, liveTree, pathMode);
            if (localWriter != null) {
                writer.finishResponse();
            }
        } catch (Throwable e) {
            LOG.error("Failed to render content tree: " + e.getMessage(), e);
        }
        return localWriter != null ? localWriter.toString() : "";
    }

    /**
     * Get the chain of node id's "leading" to the given node
     *
     * @param nodeId   requested node
     * @param liveTree live or edit tree?
     * @return chain of node id's "leading" to the given node
     */
    public String getIdChain(Long nodeId, boolean liveTree) {
        try {
            long[] chain = EJBLookup.getTreeEngine().getIdChain(liveTree ? FxTreeMode.Live : FxTreeMode.Edit, nodeId);
            StringWriter out = new StringWriter();
            JsonWriter writer = new JsonWriter(out);
            writer.startMap();
            writer.startAttribute("nodes");
            writer.startArray();
            for (long node : chain)
                writer.writeLiteral("node_"+node);
            writer.closeArray();
            writer.closeMap();
            writer.finishResponse();
            return out.toString();
        } catch (FxApplicationException e) {
            LOG.error("Failed to get idChain for node #" + nodeId + ", live tree:" + liveTree, e);
            return "[]";
        } catch (IOException e) {
            LOG.error("Failed to instantiate JSON writer", e);
            return "[]";
        }
    }

    /**
     * Render the content tree beginning at the given node up to maxDepth levels deep.
     *
     * @param writer      the tree node writer to be used
     * @param startNodeId the start node for the content tree (1 to get the whole tree)
     * @param maxDepth    the maximum depth to be rendered
     * @param liveTree    true if the live tree should be rendered, false for the edit tree
     * @param pathMode    true if node labels should be paths instead of content captions
     * @throws java.io.IOException    if an I/O error occured
     * @throws FxApplicationException if an tree error occured while accessing the tree
     */
    private void writeContentTree(TreeNodeWriter writer, long startNodeId, int maxDepth, boolean liveTree, boolean pathMode) throws
            FxApplicationException, IOException {
        FxTreeNode root = EJBLookup.getTreeEngine().getTree(liveTree ? FxTreeMode.Live : FxTreeMode.Edit,
                startNodeId, maxDepth);
        writeContentNode(writer, root, new HashMap<String, Object>(), new ArrayList<String>(), pathMode);
    }

    private void writeContentNode(TreeNodeWriter writer, FxTreeNode node, Map<String, Object> properties,
                                  List<String> actionsDisabled, boolean pathMode) throws IOException {
        final boolean liveTreeEnabled;
        try {
            liveTreeEnabled = EJBLookup.getConfigurationEngine().get(SystemParameters.TREE_LIVE_ENABLED);
        } catch (FxApplicationException e) {
            throw e.asRuntimeException();
        }
        properties.clear();
        actionsDisabled.clear();
        properties.put("objectId", node.getId());
        properties.put("widgetId", "node_" + node.getId());
        properties.put("isDirty", liveTreeEnabled && node.isDirty());
        properties.put("mayEdit", node.isMayEdit());
        if (node.hasReference())
            properties.put("referenceId", node.getReference().getId());

        setAllowedActions(actionsDisabled, node, liveTreeEnabled);
        if (actionsDisabled.size() > 0)
            properties.put("actionsDisabled", actionsDisabled);

        final String docType = node.hasReference() ? DOCTYPE_CONTENT : DOCTYPE_NODE;
        final String label = pathMode ? node.getName() : node.getLabel().getBestTranslation();
        properties.put("nodeText", label);
        if (node.isLeaf()) {
            writer.writeNode(new Node(String.valueOf(node.getId()), label, docType, properties));
        } else {
            if (node.getChildren().size() == 0 && node.getDirectChildCount() > 0) {
                properties.put("isFolder", true);
            }
            writer.startNode(new Node(String.valueOf(node.getId()), label + " [" + node.getTotalChildCount() + "]",
                    docType, properties));
            writer.startChildren();
            for (FxTreeNode child : node.getChildren())
                writeContentNode(writer, child, properties, actionsDisabled, pathMode);
            writer.closeChildren();
            writer.closeNode();
        }
    }

    /**
     * Set the allowed context menu actions depending on the given node.
     *
     * @param actionsDisabled list of disabled actions
     * @param node            the node to be processed
     * @param liveTreeEnabled if the live/edit tree switch is available (all treemode-related actions will be disabled
     *                        if this parameter is false)
     */
    private void setAllowedActions(List<String> actionsDisabled, FxTreeNode node, boolean liveTreeEnabled) {
        final boolean contentAvailable = node.hasReference();
        enableAction(actionsDisabled, contentAvailable && node.isMayDelete(), "removeContent");
        enableAction(actionsDisabled, contentAvailable && node.isMayDelete() && node.getDirectChildCount() > 0, "removeContentAndChildren");
        enableAction(actionsDisabled, contentAvailable && node.isMayEdit(), "editContent", "rename");
        enableAction(actionsDisabled, node.isMayEdit(), "editNode", "cutNode");
        enableAction(actionsDisabled, node.getDirectChildCount() > 0, "searchSubtree");
        final boolean editNodeActions =
                !node.isLive()
                        && node.isMayEdit()
                        && liveTreeEnabled;
        enableAction(actionsDisabled, editNodeActions, "activateNode", "activateNodeAndChildren", "removeNode");
        enableAction(actionsDisabled, editNodeActions && node.getDirectChildCount() > 0, "removeNodeAndChildren");
        enableAction(actionsDisabled, !node.isLive(), "createContent", "createFolder");
        final boolean liveNodeActions =
                node.isLive()
                        && liveTreeEnabled
                        && node.isMayEdit()
                        && node.getId() != FxTreeNode.ROOT_NODE;
        enableAction(actionsDisabled, liveNodeActions, "deactivateNode");
        enableAction(actionsDisabled, liveNodeActions && node.getDirectChildCount() > 0, "deactivateNodeAndChildren");
    }

    /**
     * Shortcut for enabling/disabling a tree action.
     *
     * @param actionsDisabled list of disabled actions
     * @param enable          true to enable the action, false to disable it
     * @param actions         the action(s) to be enabled or disabled
     */
    private void enableAction(List<String> actionsDisabled, boolean enable, String... actions) {
        if (!enable) {
            actionsDisabled.addAll(Arrays.asList(actions));
        }
    }
}
