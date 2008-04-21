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
package com.flexive.war.beans.cms;

import com.flexive.faces.FxJsfUtils;
import com.flexive.shared.EJBLookup;
import com.flexive.shared.interfaces.TreeEngine;
import static com.flexive.shared.tree.FxTreeMode.Edit;
import com.flexive.shared.tree.FxTreeNode;
import com.flexive.war.FxRequest;

import java.util.*;


/**
 * Request based beans, providing access to the tree.
 *
 * @author Gregor Schober (gregor.schober@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 */
public class TreeProviderBean extends Hashtable<String, List<FxTreeNode>> {

    private TreeEngine tree;
    private Map<Long, List<FxTreeNode>> cachedNodes = new HashMap<Long, List<FxTreeNode>>(25);     // cache nodes during request
    private Map<Object, List<FxTreeNode>> cachedResults = new HashMap<Object, List<FxTreeNode>>(25); // cache results during request

    public TreeProviderBean() {
        tree = EJBLookup.getTreeEngine();
    }

    @SuppressWarnings("unchecked")
    public List<FxTreeNode> get(Object key) {

        String path = String.valueOf(key);
        if (cachedResults.containsKey(key)) {
            return cachedResults.get(key);
        }

        FxRequest request = FxJsfUtils.getRequest();
        try {
            long nodeId = -1;
            if (path.indexOf(":+") > 0) {
                String split[] = path.split(":\\+");
                nodeId = Long.valueOf(split[0]);
                int level = Integer.valueOf(split[1]);
                int idx = 0;
                for (long tmp : request.getIdChain()) {
                    if (tmp == nodeId) {
                        break;
                    }
                    idx++;
                }
                idx += level;
                nodeId = request.getIdChain()[idx];
            } else if (path.equalsIgnoreCase("..")) {
                nodeId = request.getTemplateInfo().getParentNode();
            } else if (path.equalsIgnoreCase(".")) {
                nodeId = request.getNodeId();
            } else if (path.equalsIgnoreCase("/")) {
                nodeId = FxTreeNode.ROOT_NODE;
            } else {
                try {
                    nodeId = Long.parseLong(path);
                } catch (Throwable t) {/*ignore*/}
                if (nodeId == -1) nodeId = tree.getIdByPath(Edit, path);
            }
            List<FxTreeNode> result = null;
            if (!cachedNodes.containsKey(nodeId)) {
                result = tree.getTree(Edit, nodeId, 3).getChildren();
                for (FxTreeNode node : result)
                    processActiveFlag(request, node);
                cachedNodes.put(nodeId, result);
            }
            if (result != null) cachedResults.put(key, result);
            return result;
        } catch (Throwable t) {
            List<FxTreeNode> errorResult = new ArrayList<FxTreeNode>(1);
            String msg = t.getLocalizedMessage() == null ? "Null Exception" : t.getLocalizedMessage();
            errorResult.add(FxTreeNode.createErrorNode(request.getNodeId(), msg));
            return errorResult;
        }
    }

    /**
     * Set the active flag of the given node and all its children.
     *
     * @param request the request
     * @param node    the node
     */
    private void processActiveFlag(final FxRequest request, final FxTreeNode node) {
        node.setActivate(request.treeNodeIsActive(node.getId()));
        if (node.getChildren() != null) {
            for (FxTreeNode child : node.getChildren()) {
                processActiveFlag(request, child);
            }
        }
    }

}
