/***************************************************************
 *  This file is part of the [fleXive](R) project.
 *
 *  Copyright (c) 1999-2007
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
package com.flexive.war.javascript.tree;

import com.flexive.faces.RequestRelativeUriMapper;
import com.flexive.faces.components.TreeRenderer;
import com.flexive.faces.javascript.tree.TreeNodeWriter;
import com.flexive.faces.javascript.tree.TreeNodeWriter.Node;
import com.flexive.shared.CacheAdmin;
import com.flexive.shared.FxContext;
import com.flexive.shared.security.UserTicket;
import com.flexive.shared.structure.FxEnvironment;
import com.flexive.shared.structure.FxGroupAssignment;
import com.flexive.shared.structure.FxPropertyAssignment;
import com.flexive.shared.structure.FxType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Renders the structure tree for the current user.
 *
 * @author Daniel Lichtenberger (daniel.lichtenberger@flexive.com), UCS - unique computing solutions gmbh (http://www.ucs.at)
 * @version $Rev$
 */
public class StructureTreeWriter implements Serializable {
    private static final long serialVersionUID = 7930729560359590725L;
    private static final Log LOG = LogFactory.getLog(StructureTreeWriter.class);
    private UserTicket ticket;

    //public static final String DOC_TYPE_ROOT="Root";
    public static final String DOC_TYPE_GROUP = "Group";
    public static final String DOC_TYPE_TYPE = "Type";
    public static final String DOC_TYPE_TYPE_RELATION = "TypeRelation";
    public static final String DOC_TYPE_ASSIGNMENT = "Assignment";
    public static final String DOC_TYPE_ASSIGNMENT_SYSTEMINTERNAL = "AssignmentSystemInternal";

    /**
     * Render the structure tree to the node writer set in the current request.
     * Called through the JSON-RPC wrapper.
     *
     * @param request the current request
     * @param typeId  the type to be rendered (or -1 for all structures)
     * @return nothing
     */
    public String renderStructureTree(HttpServletRequest request, long typeId) {
        StringWriter localWriter = null;
        try {
            ticket = FxContext.get().getTicket();
            // if embedded in a tree component, use the component's tree writer
            TreeNodeWriter writer = (TreeNodeWriter) request.getAttribute(TreeRenderer.PROP_NODEWRITER);
            if (writer == null) {
                // otherwise return the tree nodes in the response
                localWriter = new StringWriter();
                writer = new TreeNodeWriter(localWriter, new RequestRelativeUriMapper(request), TreeNodeWriter.FORMAT_PLAIN);
            }
            writeStructureTree(writer, typeId, request);
            if (localWriter != null) {
                writer.finishResponse();
            }
        } catch (Throwable e) {
            LOG.error("Failed to render structure tree: " + e.getMessage(), e);
        }
        return localWriter != null ? localWriter.toString() : "";
    }

    /**
     * Render the structure tree from the given start node (use -1 to print
     * the whole tree.)
     *
     * @param writer  the tree node writer
     * @param typeId  the start type ID (-1 for all types)
     * @param request the current servlet request
     * @throws IOException if the tree could not be written
     */
    public void writeStructureTree(TreeNodeWriter writer, long typeId, HttpServletRequest request) throws IOException {
        final FxEnvironment environment = CacheAdmin.getFilteredEnvironment();
        Map<String, Object> nodeProperties = new HashMap<String, Object>();
        if (typeId == -1) {
            // print whole tree

            // print root types
            for (FxType type : environment.getTypes(true, false, true, false)) {
                writeType(writer, nodeProperties, type);
            }
            // print root properties

            for (FxPropertyAssignment property : environment.getSystemInternalRootPropertyAssignments()) {
                writePropertyAssignment(writer, nodeProperties, property);
            }

        } else {
            // print chosen node
            writeType(writer, nodeProperties, environment.getType(typeId));
        }
    }

    /**
     * populates nodeProperties with permissions of the current user for the given ACL
     *
     * @param nodeProperties an existing hashmap for storing additional JS properties
     * @param ACLId          the Id of the ACL
     */
    private void setPermissions(Map<String, Object> nodeProperties, long ACLId) {
        nodeProperties.put("mayEdit", String.valueOf(ticket.mayEditACL(ACLId)));
        nodeProperties.put("mayDelete", String.valueOf(ticket.mayDeleteACL(ACLId)));
    }

    /**
     * Render a type including its derived types, assigned groups and properties.
     *
     * @param writer         the tree node writer
     * @param type           the type to be rendered
     * @param nodeProperties an existing hashmap for storing additional JS properties (cleared on entry)
     * @throws IOException if the tree could not be written
     */
    private void writeType(TreeNodeWriter writer, Map<String, Object> nodeProperties, FxType type) throws IOException {
        nodeProperties.clear();
        nodeProperties.put("propertyId", String.valueOf(type.getId()));
        setPermissions(nodeProperties, type.getACL().getId());

        writer.startNode(new Node(String.valueOf(type.getId()), type.getDisplayName(), type.isRelation() ? DOC_TYPE_TYPE_RELATION : DOC_TYPE_TYPE, nodeProperties));
        writer.startChildren();
        // write derived types
        for (FxType child : type.getDerivedTypes()) {
            writeType(writer, nodeProperties, child);
        }
        // write assigned groups
        for (FxGroupAssignment group : type.getAssignedGroups()) {
            writeGroup(writer, nodeProperties, group);
        }
        // add assigned properties
        for (FxPropertyAssignment property : type.getAssignedProperties()) {
            if (!property.isSystemInternal()) {
                writePropertyAssignment(writer, nodeProperties, property);
            }
        }
        writer.closeChildren();
        writer.closeNode();
    }

    /**
     * Render an assigned property.
     *
     * @param writer         the tree node writer
     * @param nodeProperties an existing hashmap for storing additional JS properties (cleared on entry)
     * @param property       the property to be rendered
     * @throws IOException if the tree could not be written
     */
    private void writePropertyAssignment(TreeNodeWriter writer, Map<String, Object> nodeProperties, FxPropertyAssignment property) throws IOException {
        nodeProperties.clear();
        nodeProperties.put("propertyId", String.valueOf(property.getId()));
        setPermissions(nodeProperties, property.getACL().getId());
        writer.writeNode(new Node(String.valueOf(property.getId()), property.getDisplayName(),
                property.isSystemInternal() ? DOC_TYPE_ASSIGNMENT_SYSTEMINTERNAL : DOC_TYPE_ASSIGNMENT, nodeProperties));
    }

    /**
     * Render an assigned group, included assigned subgroups and properties.
     *
     * @param writer         the tree node writer
     * @param nodeProperties an existing hashmap for storing additional JS properties (cleared on entry)
     * @param group          the group to be rendered
     * @throws IOException if the tree could not be written
     */
    private void writeGroup(TreeNodeWriter writer, Map<String, Object> nodeProperties, FxGroupAssignment group) throws IOException {
        nodeProperties.clear();
        nodeProperties.put("propertyId", String.valueOf(group.getId()));
        writer.startNode(new Node(String.valueOf(group.getId()), group.getDisplayName(), DOC_TYPE_GROUP, nodeProperties));
        writer.startChildren();
        // write nested groups
        for (FxGroupAssignment nestedGroup : group.getAssignedGroups()) {
            writeGroup(writer, nodeProperties, nestedGroup);
        }
        // write properties
        for (FxPropertyAssignment property : group.getAssignedProperties()) {
            if (!property.isSystemInternal()) {
                writePropertyAssignment(writer, nodeProperties, property);
            }
        }
		// add assigned properties
		writer.closeChildren();
		writer.closeNode();
	}
	
}
